package com.zachklipp.autotransition

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

/**
 * TODO kdoc
 */
interface AutoTransition {

    /**
     * TODO kdoc
     */
    fun withAnimation(animationSpec: AnimationSpec<Any?>? = null, block: () -> Unit)
}

/**
 * TODO kdoc
 */
@Composable
fun rememberAutoTransition(): AutoTransition {
    val scope = rememberCoroutineScope()
    val factory = LocalAutoTransitionFactory.current
    return remember(scope, factory) {
        factory.createAutoTransition(scope)
    }
}