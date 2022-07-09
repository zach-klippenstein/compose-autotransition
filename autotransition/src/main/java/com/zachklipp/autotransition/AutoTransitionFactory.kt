package com.zachklipp.autotransition

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope

/**
 * TODO kdoc
 */
val LocalAutoTransitionFactory = staticCompositionLocalOf { AutoTransitionFactory.Default }

/**
 * TODO kdoc
 */
interface AutoTransitionFactory {
    /**
     * TODO kdoc
     */
    fun createAutoTransition(scope: CoroutineScope): AutoTransition

    /**
     * TODO kdoc
     */
    class Builder {

        private val registries = DefaultConverters.toMutableList()
        private var defaultAnimationSpec: AnimationSpec<Any?> = spring()

        /**
         * TODO kdoc
         */
        fun addConverterRegistry(registry: AutoTransitionConverterRegistry): Builder = apply {
            registries += registry
        }

        /** TODO */
        fun setDefaultAnimationSpec(animationSpec: AnimationSpec<Any?>): Builder = apply {
            defaultAnimationSpec = animationSpec
        }

        fun build(): AutoTransitionFactory = AutoTransitionFactoryImpl(
            converterRegistry = MultiConverterRegistry(registries.toList()),
            defaultAnimationSpec = defaultAnimationSpec
        )
    }

    companion object {
        /**
         * TODO kdoc
         */
        val Default: AutoTransitionFactory = Builder().build()
    }
}

internal class AutoTransitionFactoryImpl(
    private val converterRegistry: AutoTransitionConverterRegistry,
    private val defaultAnimationSpec: AnimationSpec<Any?>
) : AutoTransitionFactory {

    private val state = AutoTransitionState()

    override fun createAutoTransition(
        scope: CoroutineScope
    ): AutoTransition = AutoTransitionImpl(
        scope = scope,
        state = state,
        converterRegistry = converterRegistry,
        defaultAnimationSpec = defaultAnimationSpec
    )
}