package com.zachklipp.autotransition

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.TwoWayConverter

/**
 * TODO kdoc
 */
interface AutoTransitionConverter {
    fun getStateValue(stateObject: Any): Any?
    fun setStateValue(stateObject: Any, value: Any?)
    fun vectorConverterFor(value: Any?): TwoWayConverter<*, *>
}

fun interface AutoTransitionConverterRegistry {
    fun getConverterFor(stateObject: Any): AutoTransitionConverter?
}

internal class MultiConverterRegistry(
    private val registries: List<AutoTransitionConverterRegistry>
) : AutoTransitionConverterRegistry {

    override fun getConverterFor(stateObject: Any): AutoTransitionConverter? {
        registries.forEach { registry ->
            registry.getConverterFor(stateObject)?.let { return it }
        }
        return null
    }
}