package com.zachklipp.autotransition

import androidx.compose.animation.core.TwoWayConverter

/**
 * Providers [getter][getValue], [setter][setValue], and [vector converter][vectorConverterFor] for
 * a snapshot state object of an arbitrary type.
 */
interface StateObjectAdapter {
    /** Returns the value of the state object for animation. */
    fun getValue(stateObject: Any): Any?

    /** Sets the value of the state object from the animation. */
    fun setValue(stateObject: Any, value: Any?)

    /**
     * Creates a [TwoWayConverter] to animate values.
     * @param initialValue The starting value of the animation, as returned by [getValue], because
     * some types need an actual value to create a converter.
     */
    fun vectorConverterFor(initialValue: Any?): TwoWayConverter<*, *>
}

/**
 * Something that knows how to create [StateObjectAdapter]s for one or more state object types.
 */
fun interface StateObjectAdapterRegistry {
    /**
     * If this registry knows how to handle the given [stateObject], returns a [StateObjectAdapter]
     * for it. Otherwise, returns null.
     */
    fun getAdapterFor(stateObject: Any): StateObjectAdapter?
}

/** Presents a list of [StateObjectAdapterRegistry]s as a single registry. */
internal class MultiAdapterRegistry(
    private val registries: List<StateObjectAdapterRegistry>
) : StateObjectAdapterRegistry {

    override fun getAdapterFor(stateObject: Any): StateObjectAdapter? {
        registries.forEach { registry ->
            registry.getAdapterFor(stateObject)?.let { return it }
        }
        return null
    }
}