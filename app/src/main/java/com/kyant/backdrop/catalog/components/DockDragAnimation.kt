// Based on Kyant0/AndroidLiquidGlass catalog gesture behavior, Apache-2.0.
// The Dock uses a frame-coalesced spring so pointer samples do not restart animations.
package com.kyant.backdrop.catalog.components

import android.os.Trace
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.catalog.utils.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class DockDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedRange<Float>,
    private val visibilityThreshold: Float,
    initialScale: Float,
    private val pressedScale: Float,
    private val onDragStarted: DockDragAnimation.(position: Offset) -> Unit,
    private val onDragStopped: DockDragAnimation.() -> Unit,
    private val onDrag: DockDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit
) {
    private val valueState = mutableFloatStateOf(initialValue)
    private val targetState = mutableFloatStateOf(initialValue)
    private val velocityState = mutableFloatStateOf(0f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)
    private val pressProgressSpec = spring<Float>(dampingRatio = 1f, stiffness = 1000f, visibilityThreshold = 0.001f)
    private val scaleXSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 250f, visibilityThreshold = 0.001f)
    private val scaleYSpec = spring<Float>(dampingRatio = 0.7f, stiffness = 250f, visibilityThreshold = 0.001f)

    private var isDragging = false
    private var isPressed = false
    private var releasePending = false
    private var frameDriverGeneration = 0L
    private var frameDriver: Job? = null

    val value: Float get() = valueState.floatValue
    val targetValue: Float get() = targetState.floatValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float
        get() {
            val range = valueRange.endInclusive - valueRange.start
            return if (range > 0f) velocityState.floatValue / range else 0f
        }

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        var gestureInProgress = false
        var traceOpen = false
        fun closeTrace() {
            if (traceOpen) {
                Trace.endSection()
                traceOpen = false
            }
        }

        try {
            inspectDragGestures(
                onDragStart = { down ->
                    gestureInProgress = true
                    Trace.beginSection("SleepDown.LiquidTabs.Drag")
                    traceOpen = true
                    beginDrag()
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    gestureInProgress = false
                    isDragging = false
                    onDragStopped()
                    releaseAfterSettle()
                    closeTrace()
                },
                onDragCancel = {
                    gestureInProgress = false
                    isDragging = false
                    onDragStopped()
                    releaseAfterSettle()
                    closeTrace()
                }
            ) { change, dragAmount ->
                onDrag(size, dragAmount)
            }
        } finally {
            if (gestureInProgress) {
                isDragging = false
                onDragStopped()
                releaseAfterSettle()
            }
            closeTrace()
        }
    }

    private fun beginDrag() {
        isDragging = true
        releasePending = false
        targetState.floatValue = valueState.floatValue
        ensureFrameDriver()
    }

    fun updateValue(value: Float) {
        targetState.floatValue = value.coerceIn(valueRange)
    }

    fun animateToValue(value: Float, releaseAfterAnimation: Boolean = true) {
        targetState.floatValue = value.coerceIn(valueRange)
        isDragging = false
        if (!isPressed) press()
        releasePending = releaseAfterAnimation
        ensureFrameDriver()
    }

    private fun releaseAfterSettle() {
        releasePending = true
        ensureFrameDriver()
    }

    private fun ensureFrameDriver() {
        if (frameDriver?.isActive == true) return
        val generation = ++frameDriverGeneration
        frameDriver = animationScope.launch {
            var previousFrameNanos = System.nanoTime()
            try {
                while (isActive) {
                    val frameNanos = awaitFrame()
                    val deltaSeconds = ((frameNanos - previousFrameNanos).coerceAtLeast(0L) / 1_000_000_000f)
                    previousFrameNanos = frameNanos

                    val next = dockSpringStep(
                        value = valueState.floatValue,
                        velocity = velocityState.floatValue,
                        target = targetState.floatValue,
                        deltaSeconds = deltaSeconds
                    )
                    valueState.floatValue = next.value
                    velocityState.floatValue = next.velocity

                    val distance = abs(targetState.floatValue - valueState.floatValue)
                    val settleThreshold = maxOf(
                        visibilityThreshold,
                        (valueRange.endInclusive - valueRange.start) * 0.001f
                    )
                    if (releasePending && !isDragging && distance <= (valueRange.endInclusive - valueRange.start) * 0.025f) {
                        releasePending = false
                        releasePress()
                    }
                    if (!isDragging && distance <= settleThreshold && abs(velocityState.floatValue) <= settleThreshold * 60f) {
                        valueState.floatValue = targetState.floatValue
                        velocityState.floatValue = 0f
                        break
                    }
                }
            } finally {
                if (frameDriverGeneration == generation) frameDriver = null
            }
        }
    }

    private fun press() {
        isPressed = true
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYSpec) }
        }
    }

    private fun releasePress() {
        if (!isPressed) return
        isPressed = false
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(0f, pressProgressSpec) }
            launch { scaleXAnimation.animateTo(1f, scaleXSpec) }
            launch { scaleYAnimation.animateTo(1f, scaleYSpec) }
        }
    }
}
