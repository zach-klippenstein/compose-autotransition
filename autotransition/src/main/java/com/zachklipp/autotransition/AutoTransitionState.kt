package com.zachklipp.autotransition

import androidx.compose.animation.core.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import java.util.*

/**
 * Stores all state about an ongoing animation for a state object. Used to continue velocity and
 * frame timing when an animation is interrupted, as well as to detect when a state object's value
 * is changed outside the animation.
 *
 * @param adapter The [StateObjectAdapter] for the state object this animation animating.
 * @param latestAnimatedValue The value that the animation expects the state object to contain at
 * the start of each frame. Should be initially set to the state object's value before the animation
 * and then it will be updated to the animated value on every frame. If the state object's value
 * does not match at the start of a frame, then the animation for that object will be cancelled.
 * @param startTimeNanos The timestamp in nanoseconds of the first frame of this animation. Used
 * with [lastFrameNanos] to calculate play time. When interrupting an ongoing animation, this value
 * should be updated to [lastFrameNanos] so the new animation can start immediately instead of
 * waiting for the next frame.
 * @param lastFrameNanos The timestamp in nanoseconds of the last frame that this animation ran in.
 */
internal data class StateAnimation(
    val adapter: StateObjectAdapter,
    var animation: TargetBasedAnimation<*, *>,
    var latestAnimatedValue: Any?,
    var startTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    var lastFrameNanos: Long = AnimationConstants.UnspecifiedTime,
)

/**
 * Global singleton that stores information about all the state objects that are being animated and
 * their [StateAnimation]s. Used to preserve velocity and frame timing when a [withAnimation]
 * interrupts another running [withAnimation].
 */
internal object OngoingAnimationCache {

    /**
     * Tracks all ongoing animations from all ongoing calls to [animateValues] so that we can
     * interrupt animations and continue their velocity and frame times.
     *
     * A [WeakHashMap] to avoid memory leaks, since there is a global singleton instance of this
     * class.
     */
    private val ongoingAnimations = WeakHashMap<Any, StateAnimation>()

    val size: Int get() = ongoingAnimations.size

    operator fun get(stateObject: Any): StateAnimation? = ongoingAnimations[stateObject]

    operator fun set(stateObject: Any, animation: StateAnimation) {
        ongoingAnimations[stateObject] = animation
    }

    operator fun minusAssign(stateObject: Any) {
        ongoingAnimations -= stateObject
    }
}

/**
 * Doesn't return until all the animations triggered by [block] are finished or interrupted.
 */
internal suspend fun withAnimation(
    adapterRegistry: StateObjectAdapterRegistry,
    animationSpec: AnimationSpec<Any?>,
    block: () -> Unit
) {
    val statesToAnimate = mutableMapOf<Any, StateObjectAdapter>()
    val snapshot = Snapshot.takeMutableSnapshot(
        writeObserver = { changedState ->
            statesToAnimate[changedState] =
                checkNotNull(adapterRegistry.getAdapterFor(changedState)) {
                    "Could not find StateObjectRegistry for state object: $changedState. " +
                            "Did you forget to add it to your AutoTransitionFactory.Builder?"
                }
        }
    )

    val targetValues = try {
        snapshot.enter {
            block()

            // Read the values of all the state objects that were written to by block while
            // still inside the snapshot, to get their target values.
            buildMap(capacity = statesToAnimate.size) {
                statesToAnimate.forEach { (stateObject, adapter) ->
                    val targetValue = adapter.getValue(stateObject)
                    put(stateObject, Pair(adapter, targetValue))
                }
            }
        }
    } finally {
        // Always dispose of the snapshot, we only run the block initially to get the target
        // values. This must be done before suspending to avoid leaking the snapshot.
        snapshot.dispose()
    }

    animateValues(targetValues, animationSpec)
}

private suspend fun animateValues(
    statesToTargetValues: Map<Any, Pair<StateObjectAdapter, Any?>>,
    animationSpec: AnimationSpec<Any?>
) {
    val animationCache = OngoingAnimationCache
    val animations = initializeAnimations(animationCache, statesToTargetValues, animationSpec)

    /**
     * Contains all the state objects and animations that need to be updated on each frame.
     * The actual state objects are then updated atomically in a single snapshot after all new
     * values for the frame have been calculated.
     * Keep the map around to avoid re-allocating it on every frame.
     */
    val newValues = mutableMapOf<Any, StateAnimation>()
    fun updateAnimations(frameNanos: Long) {
        newValues.clear()
        val it = animations.iterator()
        while (it.hasNext()) {
            val (stateObject, stateAnimation) = it.next()

            // Stop running any animations that were interrupted by other withAnimation calls.
            if (animationCache[stateObject]?.animation !== stateAnimation.animation) {
                it.remove()
                continue
            }

            // Stop running any animations that were interrupted because their state objects were
            // changed since the last frame.
            if (stateAnimation.adapter.getValue(stateObject) !=
                stateAnimation.latestAnimatedValue
            ) {
                it.remove()
                animationCache -= stateObject
                continue
            }

            // Record all new values for running animations – but don't actually set
            // them yet, we'll do that later in a snapshot.
            if (stateAnimation.startTimeNanos == AnimationConstants.UnspecifiedTime) {
                stateAnimation.startTimeNanos = frameNanos
            }
            val playTime = frameNanos - stateAnimation.startTimeNanos
            if (stateAnimation.animation.isFinishedFromNanos(playTime)) {
                // Ensure the final value is exactly the target value.
                stateAnimation.latestAnimatedValue = stateAnimation.animation.targetValue
                newValues[stateObject] = stateAnimation
                // Stop running animations when completed.
                it.remove()
                animationCache -= stateObject
            } else {
                val newValue = stateAnimation.animation.getValueFromNanos(playTime)
                stateAnimation.latestAnimatedValue = newValue
                stateAnimation.lastFrameNanos = frameNanos
                newValues[stateObject] = stateAnimation
            }
        }
    }

    try {
        while (animations.isNotEmpty()) {
            withFrameNanos { frameNanos ->
                updateAnimations(frameNanos)

                // Atomically update all animated values inside a snapshot.
                // If this snapshot fails to be applied, this function will throw, and we'll remove
                // all known animations from the cache in the finally block below.
                Snapshot.withMutableSnapshot {
                    newValues.forEach { (stateObject, stateAnimation) ->
                        stateAnimation.adapter.setValue(
                            stateObject,
                            stateAnimation.latestAnimatedValue
                        )
                    }
                }
            }
        }
    } finally {
        // Remove all animations we're still running from the shared list.
        animations.forEach { (stateObject, stateAnimation) ->
            if (animationCache[stateObject]?.animation === stateAnimation.animation) {
                animationCache -= stateObject
            }
        }
    }
}

/**
 * Ensures that [animationCache] is populated with all the animations specified by
 * [statesToTargetValues], either updating animations that were already in the cache or adding new
 * [StateAnimation] entries.
 */
private fun initializeAnimations(
    animationCache: OngoingAnimationCache,
    statesToTargetValues: Map<Any, Pair<StateObjectAdapter, Any?>>,
    animationSpec: AnimationSpec<Any?>
): MutableMap<Any, StateAnimation> {
    val animations = mutableMapOf<Any, StateAnimation>()
    statesToTargetValues.forEach { (stateObject, adapterAndValue) ->
        val (adapter, targetValue) = adapterAndValue
        val initialValue = adapter.getValue(stateObject)
        val oldStateAnimation = animationCache[stateObject]
        val initialVelocity = oldStateAnimation?.run {
            animation.getVelocityVectorFromNanos(lastFrameNanos - startTimeNanos)
        }

        @Suppress("UNCHECKED_CAST")
        val newAnimation = TargetBasedAnimation(
            animationSpec = animationSpec,
            typeConverter =
            adapter.vectorConverterFor(initialValue) as TwoWayConverter<Any?, AnimationVector>,
            initialValue = initialValue,
            targetValue = targetValue,
            initialVelocityVector = initialVelocity
        )

        if (oldStateAnimation != null) {
            oldStateAnimation.animation = newAnimation
            oldStateAnimation.startTimeNanos = oldStateAnimation.lastFrameNanos
            oldStateAnimation.latestAnimatedValue = initialValue
            animations[stateObject] = oldStateAnimation
        } else {
            val stateAnimation = StateAnimation(
                adapter = adapter,
                animation = newAnimation,
                latestAnimatedValue = initialValue
            )
            animationCache[stateObject] = stateAnimation
            animations[stateObject] = stateAnimation
        }
    }
    return animations
}