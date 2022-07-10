package com.zachklipp.autotransition

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import com.zachklipp.autotransition.AutoTransitionFactory.Builder
import kotlinx.coroutines.CoroutineScope

/**
 * Specifies the [AutoTransitionFactory] for [rememberAutoTransition] to use.
 */
val LocalAutoTransitionFactory = staticCompositionLocalOf { AutoTransitionFactory.Default }

/**
 * A factory that creates [AutoTransition] instances scoped to a particular [CoroutineScope].
 * To customize the features of the [AutoTransition], create your own factory using [Builder] and
 * provide it as the [LocalAutoTransitionFactory] composition local.
 */
interface AutoTransitionFactory {
    /**
     * Returns an [AutoTransition] instance that will run animations as long as [scope] is active.
     * When [scope] is cancelled, any animations started by the [AutoTransition] will be cancelled.
     */
    fun createAutoTransition(scope: CoroutineScope): AutoTransition

    /**
     * Builder for [AutoTransitionFactory]s that allow you to specify additional
     * [StateObjectAdapterRegistry]s and custom [AnimationSpec]s.
     */
    class Builder {

        private val registries = DefaultStateObjectAdapters.toMutableList()
        private var defaultAnimationSpec: AnimationSpec<Any?> = spring()

        /**
         * Registers a [StateObjectAdapterRegistry] to be checked for state object types not handled
         * by the built-in adapters.
         */
        fun addAdapterRegistry(registry: StateObjectAdapterRegistry): Builder = apply {
            registries += registry
        }

        /**
         * Sets the [AnimationSpec] to be used by [AutoTransition.withAnimation] calls that don't
         * specify their own spec.
         */
        fun setDefaultAnimationSpec(animationSpec: AnimationSpec<Any?>): Builder = apply {
            defaultAnimationSpec = animationSpec
        }

        fun build(): AutoTransitionFactory = AutoTransitionFactoryImpl(
            adapterRegistry = MultiAdapterRegistry(registries.toList()),
            defaultAnimationSpec = defaultAnimationSpec
        )
    }

    companion object {
        /**
         * An [AutoTransitionFactory] that only knows how to animate some primitive types that
         * Compose ships with vector converters for.
         */
        val Default: AutoTransitionFactory = Builder().build()
    }
}

internal class AutoTransitionFactoryImpl(
    private val adapterRegistry: StateObjectAdapterRegistry,
    private val defaultAnimationSpec: AnimationSpec<Any?>
) : AutoTransitionFactory {

    override fun createAutoTransition(
        scope: CoroutineScope
    ): AutoTransition = AutoTransitionImpl(
        scope = scope,
        adapterRegistry = adapterRegistry,
        defaultAnimationSpec = defaultAnimationSpec
    )
}