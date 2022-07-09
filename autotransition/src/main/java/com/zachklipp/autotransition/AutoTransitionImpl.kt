package com.zachklipp.autotransition

import androidx.compose.animation.core.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.util.*

internal class AutoTransitionImpl(
    private val scope: CoroutineScope,
    private val state: AutoTransitionState,
    private val converterRegistry: AutoTransitionConverterRegistry,
    private val defaultAnimationSpec: AnimationSpec<Any?>
) : AutoTransition {

    override fun withAnimation(animationSpec: AnimationSpec<Any?>?, block: () -> Unit) {
        val statesToAnimate = mutableMapOf<Any, AutoTransitionConverter>()
        val snapshot = Snapshot.takeMutableSnapshot(
            writeObserver = { changedState ->
                statesToAnimate[changedState] =
                    checkNotNull(converterRegistry.getConverterFor(changedState)) {
                        "No AutoTransitionConverter found for state object: $changedState"
                    }
            }
        )

        val targetValues = try {
            snapshot.enter {
                block()

                // Read the values of all the state objects that were written to by block while
                // still inside the snapshot, to get their target values.
                buildMap(capacity = statesToAnimate.size) {
                    statesToAnimate.forEach { (stateObject, converter) ->
                        val targetValue = converter.getStateValue(stateObject)
                        put(stateObject, Pair(converter, targetValue))
                    }
                }
            }
        } finally {
            // Always dispose of the snapshot, we only run the block initially to get the target
            // values.
            snapshot.dispose()
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            state.animateValues(targetValues, animationSpec ?: defaultAnimationSpec)
        }
    }
}

internal class AutoTransitionState {

    private data class StateAnimation(
        val converter: AutoTransitionConverter,
        var animation: TargetBasedAnimation<*, *>,
        var startTimeNanos: Long = AnimationConstants.UnspecifiedTime,
        var lastFrameNanos: Long = AnimationConstants.UnspecifiedTime,
    )

    /**
     * Tracks all ongoing animations from all ongoing calls to [animateValues] so that we can
     * interrupt animations and continue their velocity and frame times.
     *
     * A [WeakHashMap] to avoid memory leaks, since there is a global singleton instance of this
     * class.
     */
    private val ongoingAnimations = WeakHashMap<Any, StateAnimation>()

    suspend fun animateValues(
        statesToTargetValues: Map<Any, Pair<AutoTransitionConverter, Any?>>,
        animationSpec: AnimationSpec<Any?>
    ) {
        val animations = initializeAnimations(statesToTargetValues, animationSpec)
        try {
            val newValues = mutableMapOf<Any, Pair<AutoTransitionConverter, Any?>>()
            while (animations.isNotEmpty()) {
                newValues.clear()
                withFrameNanos { frameNanos ->
                    val it = animations.iterator()
                    while (it.hasNext()) {
                        val (stateObject, stateAnimation) = it.next()
                        if (ongoingAnimations[stateObject]?.animation !== stateAnimation.animation) {
                            // Stop running any animations that were interrupted.
                            it.remove()
                        } else {
                            // Record all new values for running animations – but don't actually set
                            // them yet, we'll do that later in a snapshot.
                            if (stateAnimation.startTimeNanos == AnimationConstants.UnspecifiedTime) {
                                stateAnimation.startTimeNanos = frameNanos
                            }
                            val playTime = frameNanos - stateAnimation.startTimeNanos
                            if (stateAnimation.animation.isFinishedFromNanos(playTime)) {
                                // Ensure the final value is exactly the target value.
                                newValues[stateObject] = Pair(
                                    stateAnimation.converter,
                                    stateAnimation.animation.targetValue
                                )
                                it.remove()
                                ongoingAnimations -= stateObject
                            } else {
                                stateAnimation.lastFrameNanos = frameNanos
                                val newValue = stateAnimation.animation.getValueFromNanos(playTime)
                                newValues[stateObject] = Pair(stateAnimation.converter, newValue)
                            }
                        }
                    }

                    // Atomically update all animated values inside a snapshot.
                    Snapshot.withMutableSnapshot {
                        newValues.forEach { (stateObject, converterAndNewValue) ->
                            val (converter, newValue) = converterAndNewValue
                            converter.setStateValue(stateObject, newValue)
                        }
                    }
                }
            }
        } finally {
            // Remove all animations we're still running from the shared list.
            animations.forEach { (stateObject, stateAnimation) ->
                if (ongoingAnimations[stateObject]?.animation === stateAnimation.animation) {
                    ongoingAnimations -= stateObject
                }
            }
        }
    }

    private fun initializeAnimations(
        statesToTargetValues: Map<Any, Pair<AutoTransitionConverter, Any?>>,
        animationSpec: AnimationSpec<Any?>
    ): MutableMap<Any, StateAnimation> {
        val animations = mutableMapOf<Any, StateAnimation>()
        statesToTargetValues.forEach { (stateObject, converterAndValue) ->
            val (converter, targetValue) = converterAndValue
            val initialValue = converter.getStateValue(stateObject)
            val oldStateAnimation = ongoingAnimations[stateObject]
            val initialVelocity = oldStateAnimation?.run {
                animation.getVelocityVectorFromNanos(lastFrameNanos - startTimeNanos)
            }
            val newAnimation = converter.createTargetBasedAnimation(
                animationSpec = animationSpec,
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocityVector = initialVelocity
            )
            if (oldStateAnimation != null) {
                oldStateAnimation.animation = newAnimation
                oldStateAnimation.startTimeNanos = oldStateAnimation.lastFrameNanos
                animations[stateObject] = oldStateAnimation
            } else {
                val stateAnimation = StateAnimation(converter, newAnimation)
                ongoingAnimations[stateObject] = stateAnimation
                animations[stateObject] = stateAnimation
            }
        }
        return animations
    }
}

@Suppress("UNCHECKED_CAST")
private fun AutoTransitionConverter.createTargetBasedAnimation(
    animationSpec: AnimationSpec<Any?>,
    initialValue: Any?,
    targetValue: Any?,
    initialVelocityVector: AnimationVector?
): TargetBasedAnimation<*, *> = TargetBasedAnimation(
    animationSpec = animationSpec,
    typeConverter = vectorConverterFor(initialValue) as TwoWayConverter<Any?, AnimationVector>,
    initialValue = initialValue,
    targetValue = targetValue,
    initialVelocityVector = initialVelocityVector
)