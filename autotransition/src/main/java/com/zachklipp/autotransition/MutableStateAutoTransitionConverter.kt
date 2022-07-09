package com.zachklipp.autotransition

import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.MutableState
import kotlin.reflect.KClass

@Suppress("FunctionName")
fun <T : Any> MutableStateAutoTransitionConverter(
    valueType: KClass<T>,
    vectorConverter: TwoWayConverter<T, *>
): MutableStateAutoTransitionConverter<T> =
    object : MutableStateAutoTransitionConverter<T>(valueType) {
        override fun vectorConverterForTyped(value: T): TwoWayConverter<T, *> = vectorConverter
    }

abstract class MutableStateAutoTransitionConverter<T : Any>(
    private val valueType: KClass<T>
) : AutoTransitionConverter,
    AutoTransitionConverterRegistry {

    final override fun getConverterFor(stateObject: Any): AutoTransitionConverter? {
        if (stateObject is MutableState<*> && valueType.isInstance(stateObject.value)) {
            return this
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    final override fun getStateValue(stateObject: Any): T {
        return (stateObject as MutableState<T>).value
    }

    @Suppress("UNCHECKED_CAST")
    final override fun setStateValue(stateObject: Any, value: Any?) {
        (stateObject as MutableState<T>).value = value as T
    }

    @Suppress("UNCHECKED_CAST")
    final override fun vectorConverterFor(value: Any?): TwoWayConverter<*, *> {
        return vectorConverterForTyped(value as T)
    }

    protected abstract fun vectorConverterForTyped(value: T): TwoWayConverter<T, *>
}