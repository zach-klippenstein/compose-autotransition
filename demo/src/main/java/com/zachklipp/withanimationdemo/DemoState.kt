package com.zachklipp.withanimationdemo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zachklipp.autotransition.AutoTransition
import com.zachklipp.autotransition.rememberAutoTransition
import kotlin.random.Random

class DemoState {
    var cornerRadius by mutableStateOf(0.dp)
    var color by mutableStateOf(Color.Blue)
    var padding by mutableStateOf(8.dp)
    var rotationDegrees by mutableStateOf(0f)
    var scale by mutableStateOf(1f)
    var elevation by mutableStateOf(0.dp)

    private val random = Random(0)

    fun randomize() {
        cornerRadius = random.nextDouble(10.0).dp
        color = Color(
            red = random.nextFloat(),
            green = random.nextFloat(),
            blue = random.nextFloat()
        )
        padding = random.nextDouble(32.0).dp
        rotationDegrees = random.nextDouble(-360.0, 360.0).toFloat()
        scale = random.nextDouble(1.0, 3.0).toFloat()
        elevation = random.nextDouble(16.0).dp
    }
}

@Composable
fun Controls(state: DemoState) {
    with(rememberAutoTransition()) {
        @Composable
        fun <T> ControlRow(
            label: String,
            values: List<T>,
            valueLabel: (T) -> String = { it.toString() },
            setter: (T) -> Unit
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label)
                values.forEach {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            withAnimation(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            ) {
                                setter(it)
                            }
                        }) {
                        Text(valueLabel(it))
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ControlRow(
                label = "Corner radius:",
                values = listOf(0.dp, 8.dp, 12.dp),
                setter = { state.cornerRadius = it }
            )

            ControlRow(
                label = "Color:",
                values = listOf(
                    "Blue" to Color.Blue,
                    "Red" to Color.Red,
                    "Green" to Color.Green
                ),
                valueLabel = { it.first },
                setter = { state.color = it.second }
            )

            ControlRow(
                label = "Padding:",
                values = listOf(0.dp, 8.dp, 24.dp),
                setter = { state.padding = it })

            ControlRow(
                label = "Rotation:",
                values = listOf(-75f, 0f, 270f),
                setter = { state.rotationDegrees = it }
            )

            ControlRow(
                label = "Scale:",
                values = listOf(1f, 1.5f, 3f),
                setter = { state.scale = it }
            )

            ControlRow(
                label = "Elevation:",
                values = listOf(0.dp, 2.dp, 8.dp),
                setter = { state.elevation = it }
            )

            Button(onClick = {
                withAnimation(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessVeryLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) {
                    state.randomize()
                }
            }) {
                Text("Randomize values")
            }
        }
    }
}

@Composable
fun Display(state: DemoState) {
    Surface(
        shape = RoundedCornerShape(state.cornerRadius.coerceAtLeast(0.dp)),
        color = state.color,
        elevation = state.elevation,
        modifier = Modifier
            .aspectRatio(1f)
            .wrapContentSize()
            .graphicsLayer {
                rotationZ = state.rotationDegrees
                scaleX = state.scale
                scaleY = state.scale
            }
    ) {
        Text(
            "Weee!",
            Modifier.padding(state.padding.coerceAtLeast(0.dp))
        )
    }
}