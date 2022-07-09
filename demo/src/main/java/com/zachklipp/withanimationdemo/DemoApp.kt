package com.zachklipp.withanimationdemo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DemoApp() {
    val state = remember { DemoState() }

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = spacedBy(4.dp)
    ) {
        Controls(state)
        Divider()
        Display(state)
    }
}