// Based on Kyant0/AndroidLiquidGlass catalog components, Apache-2.0.
// Modified for SleepDown-Schedule.
package com.kyant.backdrop.catalog.utils

import android.os.Trace
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDragCancelled: DampedDragAnimation.() -> Unit = {},
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val dragTarget = mutableFloatStateOf(initialValue)
    private var dragTargetJob: Job? = null

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = dragTarget.floatValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float
        get() {
            val range = valueRange.endInclusive - valueRange.start
            return if (range > 0f) valueAnimation.velocity / range else 0f
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
                    startDragTracking()
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    gestureInProgress = false
                    stopDragTracking()
                    onDragStopped()
                    release()
                    closeTrace()
                },
                onDragCancel = {
                    gestureInProgress = false
                    stopDragTracking()
                    onDragCancelled()
                    release()
                    closeTrace()
                }
            ) { change, dragAmount ->
                onDrag(size, dragAmount)
            }
        } finally {
            if (gestureInProgress) {
                stopDragTracking()
                onDragCancelled()
                release()
            }
            closeTrace()
        }
    }

    fun press() {
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        dragTarget.floatValue = value.coerceIn(valueRange)
    }

    fun animateToValue(value: Float, releaseAfterAnimation: Boolean = true) {
        stopDragTracking()
        val target = value.coerceIn(valueRange)
        dragTarget.floatValue = target
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (releaseAfterAnimation) release()
            }
        }
    }

    fun animateToValueAndThen(value: Float, onFinished: () -> Unit) {
        stopDragTracking()
        val target = value.coerceIn(valueRange)
        dragTarget.floatValue = target
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                coroutineScope {
                    launch {
                        valueAnimation.animateTo(target, valueAnimationSpec)
                    }
                    launch {
                        awaitFrame()
                        val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                        if (abs(valueAnimation.value - target) >= threshold) {
                            snapshotFlow { valueAnimation.value }
                                .filter { abs(it - target) < threshold }
                                .first()
                        }
                        coroutineScope {
                            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
                            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
                        }
                    }
                }
                onFinished()
            }
        }
    }

    private fun startDragTracking() {
        stopDragTracking()
        dragTarget.floatValue = value
        dragTargetJob = animationScope.launch {
            // Pointer devices may deliver several movement samples before a display frame.
            // Retarget the spring once per frame instead of cancelling and recreating it for
            // every sample; this keeps the thumb responsive without queuing animation work.
            var animatedTarget = valueAnimation.targetValue
            var targetAnimation: Job? = null
            while (isActive) {
                awaitFrame()
                val target = dragTarget.floatValue
                if (target != animatedTarget) {
                    targetAnimation?.cancel()
                    targetAnimation = launch {
                        valueAnimation.animateTo(target, valueAnimationSpec)
                    }
                    animatedTarget = target
                }
            }
        }
    }

    private fun stopDragTracking() {
        dragTargetJob?.cancel()
        dragTargetJob = null
    }
}
