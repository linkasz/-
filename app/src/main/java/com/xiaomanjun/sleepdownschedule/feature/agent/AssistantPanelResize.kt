package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.geometry.Rect
import com.xiaomanjun.sleepdownschedule.glass.ui.topAssistantMorphRect

/** Closing starts at the actual resized frame; a height override must not survive the return morph. */
internal fun assistantClosingFrame(anchor: Rect, current: Rect, drop: Float, spread: Float,
    initialDrop: Float, initialSpread: Float): Rect = topAssistantMorphRect(anchor, current,
    (drop / initialDrop.coerceAtLeast(.001f)).coerceIn(0f, 1f),
    (spread / initialSpread.coerceAtLeast(.001f)).coerceIn(0f, 1f))

internal fun assistantComposerTop(bottom: Float, inputHeight: Float, bottomGap: Float): Float =
    bottom - inputHeight - bottomGap

internal fun resistedAssistantHeight(raw: Float, minimum: Float, maximum: Float): Float {
    val min = minimum.coerceIn(1f, maximum.coerceAtLeast(1f))
    val max = maximum.coerceAtLeast(min)
    val bounded = raw.coerceIn(min, max)
    return bounded + ((raw - bounded) * .15f).coerceIn(-24f, 24f)
}

/** Direct pointer updates, read in layout/draw only; spring work happens once at release.
 * Uses the drag-target/release split from AndroidLiquidGlass's Apache-2.0 catalog.
 */
@Stable
internal class AssistantPanelResize(private val scope: CoroutineScope) {
    var height by mutableFloatStateOf(Float.NaN)
        private set
    var dragging by mutableStateOf(false)
        private set
    private var raw = 0f
    private var startHeight = 0f
    private var startOverride = Float.NaN
    private var settling: Job? = null

    fun begin(current: Float) {
        settling?.cancel()
        dragging = true
        startOverride = height
        startHeight = current
        raw = current
        height = current
    }

    fun drag(delta: Float, minimum: Float, maximum: Float) {
        if (!dragging) return // Ignore a cancelled pointer's late callback after the overlay begins closing.
        raw += delta
        height = resistedAssistantHeight(raw, minimum, maximum)
    }

    fun finish(cancelled: Boolean, minimum: Float, maximum: Float, edge: Float,
        onBoundary: (Boolean) -> Unit) {
        if (!dragging) return
        dragging = false
        val target = if (cancelled) startHeight.coerceAtLeast(1f)
            else height.coerceIn(minimum, maximum)
        // Ordinary release preserves any interior height. Only the window boundaries switch modes.
        val full = !cancelled && target >= maximum - edge
        val compact = !cancelled && target <= minimum + edge
        settling = scope.launch {
            val animation = Animatable(height)
            animation.animateTo(if (full) maximum else if (compact) minimum else target,
                spring(dampingRatio = .78f, stiffness = 380f, visibilityThreshold = .5f)) {
                height = value
            }
            if (cancelled) height = startOverride
            // Keep the measured frame through the presentation hand-off; resetting here caused a one-frame jump.
            if (full || compact) onBoundary(full)
        }
    }

    fun reset() { settling?.cancel(); dragging = false; height = Float.NaN }
}

/** Lazy conversation viewport resizes in layout; its text keeps a fixed reading width. */
internal fun Modifier.assistantResizableViewport(width: Dp, defaultHeight: Dp, height: () -> Float): Modifier =
    layout { measurable, constraints ->
        val requested = height()
        val h = (if (requested.isNaN()) defaultHeight.toPx() else requested).toInt().coerceAtLeast(1)
        val child = measurable.measure(Constraints.fixed(width.roundToPx().coerceAtLeast(1), h))
        layout(constraints.constrainWidth(child.width), constraints.constrainHeight(child.height)) { child.place(0, 0) }
    }

