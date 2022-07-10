package com.zachklipp.autotransition

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Provides methods for animation changes to snapshot state objects.
 *
 * To get an instance, call [rememberAutoTransition].
 */
interface AutoTransition {

    /**
     * Given a [block] that sets some snapshot state values (e.g. properties backed by
     * [mutableStateOf]), animates the changes to all those states over time.
     *
     * All the state objects written to must be of types that are known by the
     * [AutoTransitionFactory]'s configured [StateObjectAdapterRegistry]s, or an exception will be
     * thrown when the state object is written to. To animate additional state object types, specify
     * additional adapter registries via [AutoTransitionFactory.Builder.addAdapterRegistry].
     *
     * For example, to animate an elevation and scale simultaneously:
     *
     * ```kotlin
     * var elevation by mutableStateOf(0.dp)
     * var scale by mutableStateOf(1f)
     * …
     * autoTransition.withAnimation {
     *   elevation = 8.dp
     *   scale = 0.8f
     * }
     * ```
     *
     * @param animationSpec The [AnimationSpec] used to animate each state value. If null, the
     * default animation spec specified by the [AutoTransitionFactory] will be used. This can be
     * customized via [AutoTransitionFactory.Builder.setDefaultAnimationSpec].
     * @param block A side-effect-free function that writes to one or more snapshot state objects.
     * This function will be ran in a snapshot that will be immediately disposed without applying,
     * so it must not perform any side effects or update any state that is not in a snapshot state
     * object.
     */
    fun withAnimation(animationSpec: AnimationSpec<Any?>? = null, block: () -> Unit)
}

/**
 * Returns an [AutoTransition] that will cancel its animations when this function is removed from
 * the composition.
 */
@Composable
fun rememberAutoTransition(): AutoTransition {
    val scope = rememberCoroutineScope()
    val factory = LocalAutoTransitionFactory.current
    return remember(scope, factory) {
        factory.createAutoTransition(scope)
    }
}

internal class AutoTransitionImpl(
    private val scope: CoroutineScope,
    private val adapterRegistry: StateObjectAdapterRegistry,
    private val defaultAnimationSpec: AnimationSpec<Any?>
) : AutoTransition {

    override fun withAnimation(animationSpec: AnimationSpec<Any?>?, block: () -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withAnimation(adapterRegistry, animationSpec ?: defaultAnimationSpec, block)
        }
    }
}