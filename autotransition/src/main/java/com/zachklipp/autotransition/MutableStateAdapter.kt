package com.zachklipp.autotransition

import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.MutableState
import kotlin.reflect.KClass

/**
 * Convenience function to create a [MutableStateAdapter] with a single [TwoWayConverter]
 * for all values the object can hold.
 */
@Suppress("FunctionName")
fun <T : Any> MutableStateAutoTransitionConverter(
    valueType: KClass<T>,
    vectorConverter: TwoWayConverter<T, *>
): MutableStateAdapter<T> = object : MutableStateAdapter<T>(valueType) {
    override fun vectorConverterForTyped(initialValue: T) = vectorConverter
}

/**
 * A [StateObjectAdapterRegistry] that knows how to creates [StateObjectAdapter]s for [MutableState]
 * objects with a particular value type [T].
 */
abstract class MutableStateAdapter<T : Any>(
    private val valueType: KClass<T>
) : StateObjectAdapter,
    StateObjectAdapterRegistry {

    final override fun getAdapterFor(stateObject: Any): StateObjectAdapter? {
        if (stateObject is MutableState<*> && valueType.isInstance(stateObject.value)) {
            return this
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    final override fun getValue(stateObject: Any): T {
        return (stateObject as MutableState<T>).value
    }

    @Suppress("UNCHECKED_CAST")
    final override fun setValue(stateObject: Any, value: Any?) {
        (stateObject as MutableState<T>).value = value as T
    }

    @Suppress("UNCHECKED_CAST")
    final override fun vectorConverterFor(initialValue: Any?): TwoWayConverter<*, *> {
        return vectorConverterForTyped(initialValue as T)
    }

    protected abstract fun vectorConverterForTyped(initialValue: T): TwoWayConverter<T, *>
}