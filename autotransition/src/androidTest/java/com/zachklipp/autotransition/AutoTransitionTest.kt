package com.zachklipp.autotransition

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class AutoTransitionTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var autoTransition: AutoTransition

    @After
    fun tearDown() {
        assertEquals(0, OngoingAnimationCache.size)
    }

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
    fun animationInterrupted_whenValueChangedInWithAnimation() {
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

    @Test
    fun animationInterrupted_whenValueChangedOutsideWithAnimation() {
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

        value = 200

        // New animation should start immediately on the next frame, not wait one.
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(200, value)
        }

        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            assertEquals(200, value)
        }
    }

    @Test
    fun throws_whenNoAdapterForMutableStateType() {
        var value by mutableStateOf("Hello")
        var innerError: Exception? = null
        var outerError: Throwable? = null

        rule.setContent {
            val scope = rememberCoroutineScope {
                // Since the withAnimation block runs inside a coroutine, we need to add a handler
                // to the context to prevent it from crashing the test.
                CoroutineExceptionHandler { _, throwable ->
                    outerError = throwable
                }
            }
            val factory = LocalAutoTransitionFactory.current
            autoTransition = remember(scope, factory) {
                factory.createAutoTransition(scope)
            }
        }

        rule.runOnIdle {
            autoTransition.withAnimation {
                try {
                    value = "World"
                } catch (e: Exception) {
                    innerError = e
                    throw e
                }
            }
        }

        assertSame(innerError, outerError)
        assertIs<IllegalStateException>(innerError)
        val message = innerError!!.message!!
        assertContains(message, "Could not find StateObjectRegistry for state object")
        assertContains(message, "MutableState(value=World)")
        assertEquals("Hello", value)
    }

    @Test
    fun doesntAnimateAnything_whenNoAdapterForMutableStateType() {
        var invalidValue by mutableStateOf("Hello")
        var validValue by mutableStateOf(1)

        rule.setContent {
            val scope = rememberCoroutineScope {
                // Since the withAnimation block runs inside a coroutine, we need to add a handler
                // to the context to prevent it from crashing the test.
                CoroutineExceptionHandler { _, _ -> }
            }
            val factory = LocalAutoTransitionFactory.current
            autoTransition = remember(scope, factory) {
                factory.createAutoTransition(scope)
            }
        }

        rule.runOnIdle {
            autoTransition.withAnimation {
                validValue = 100
                invalidValue = "World"
            }
        }

        rule.runOnIdle {
            assertEquals("Hello", invalidValue)
            assertEquals(1, validValue)
        }
    }

    @Test
    fun stressTest_doesntLeakOldAnimations() {
        var value1 by mutableStateOf(1)
        var value2 by mutableStateOf(1f)
        val random = Random(0)

        fun updateValues() {
            when (random.nextInt(3)) {
                0 -> {
                    value1 += 1
                }
                1 -> {
                    value2 += 0.5f
                }
                2 -> {
                    value1 += 2
                    value2 += 0.2f
                }
            }
        }

        rule.setAutoTransitionContent {
            LaunchedEffect(Unit) {
                repeat(100) {
                    launch {
                        autoTransition.withAnimation {
                            updateValues()
                        }
                        repeat(100) {
                            delayFrames(random.nextInt(1, 4))
                            autoTransition.withAnimation {
                                updateValues()
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, OngoingAnimationCache.size)
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

    private suspend fun delayFrames(count: Int) {
        repeat(count) {
            withFrameMillis {}
        }
    }
}