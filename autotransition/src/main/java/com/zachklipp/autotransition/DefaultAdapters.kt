package com.zachklipp.autotransition

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal val DefaultStateObjectAdapters = listOf<StateObjectAdapterRegistry>(
    MutableStateAutoTransitionConverter(Float::class, Float.VectorConverter),
    MutableStateAutoTransitionConverter(Int::class, Int.VectorConverter),
    MutableStateAutoTransitionConverter(Rect::class, Rect.VectorConverter),
    MutableStateAutoTransitionConverter(Dp::class, Dp.VectorConverter),
    MutableStateAutoTransitionConverter(DpOffset::class, DpOffset.VectorConverter),
    MutableStateAutoTransitionConverter(Offset::class, Offset.VectorConverter),
    MutableStateAutoTransitionConverter(IntOffset::class, IntOffset.VectorConverter),
    MutableStateAutoTransitionConverter(Size::class, Size.VectorConverter),
    MutableStateAutoTransitionConverter(IntSize::class, IntSize.VectorConverter),
    object : MutableStateAdapter<Color>(Color::class) {
        override fun vectorConverterForTyped(initialValue: Color): TwoWayConverter<Color, *> {
            return Color.VectorConverter(initialValue.colorSpace)
        }
    }
)