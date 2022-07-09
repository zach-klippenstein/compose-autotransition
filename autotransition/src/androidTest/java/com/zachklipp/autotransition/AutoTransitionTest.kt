package com.zachklipp.autotransition

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
class AutoTransitionTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var autoTransition: AutoTransition

    @Test
    fun stateWritesInWithAnimationAreNotImmediatelyApplied() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }
        rule.mainClock.autoAdvance = false

        autoTransition.withAnimation {
            value = 100
        }

        rule.runOnIdle {
            assertEquals(0, value)
        }
    }

    @Test
    fun valuesArentChangedOnFirstFrame() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }
        rule.mainClock.autoAdvance = false

        autoTransition.withAnimation {
            value = 100
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertEquals(0, value)
        }
    }

    @Test
    fun valuesAreChangedOnSecondFrame() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }
        rule.mainClock.autoAdvance = false

        autoTransition.withAnimation {
            value = 100
        }

        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertNotEquals(0, value)
        }
    }

    @Test
    fun animatesToFinalValue() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }

        autoTransition.withAnimation {
            value = 100
        }

        rule.runOnIdle {
            assertEquals(100, value)
        }
    }

    @Test
    fun animatesMultipleValues() {
        var value1 by mutableStateOf(0)
        var value2 by mutableStateOf(0f)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value1)
            assertEquals(0f, value2)
        }

        autoTransition.withAnimation {
            value1 = 100
            value2 = 1f
        }

        rule.runOnIdle {
            assertEquals(100, value1)
            assertEquals(1f, value2)
        }
    }

    @Test
    fun animationIntermediateValuesComeFromSpec() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }
        rule.mainClock.autoAdvance = false

        autoTransition.withAnimation(tween(durationMillis = 1000, easing = LinearEasing)) {
            value = 100
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            assertEquals(24, value)
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            assertEquals(49, value)
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            assertEquals(75, value)
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            assertEquals(100, value)
        }
    }

    @Test
    fun withAnimationInterruptsOngoingAnimation() {
        var value by mutableStateOf(0)
        rule.setAutoTransitionContent()
        rule.runOnIdle {
            assertEquals(0, value)
        }
        rule.mainClock.autoAdvance = false
        autoTransition.withAnimation(tween(durationMillis = 1000, easing = LinearEasing)) {
            value = 100
        }
        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertEquals(49, value)
        }

        autoTransition.withAnimation {
            value = 200
        }

        // New animation should start immediately on the next frame, not wait one.
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(69, value)
        }

        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            assertEquals(200, value)
        }
    }

    private fun ComposeContentTestRule.setAutoTransitionContent(
        content: @Composable AutoTransition.() -> Unit = {}
    ) {
        setContent {
            autoTransition = rememberAutoTransition()
            content(autoTransition)
        }
    }
}