package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput

internal fun assistantOutsideTapAllowed(outside: Boolean, moved: Boolean, consumed: Boolean): Boolean =
    outside && !moved && !consumed

/** A moving handle can leave its original hit box. Only a fresh, unhandled outside tap dismisses. */
internal fun Modifier.assistantOutsideTap(
    enabled: () -> Boolean,
    bounds: () -> Rect,
    dismiss: () -> Unit
): Modifier = composed {
    // Geometry changes every drag frame; keep the detector alive and read current callbacks instead.
    val latestEnabled by rememberUpdatedState(enabled)
    val latestBounds by rememberUpdatedState(bounds)
    val latestDismiss by rememberUpdatedState(dismiss)
    pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val outside = latestEnabled() && !latestBounds().contains(down.position)
        var moved = false
        var consumed = down.isConsumed
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            moved = moved || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            consumed = consumed || change.isConsumed || event.changes.any { it.id != down.id && it.pressed }
            if (!change.pressed) {
                if (latestEnabled() && assistantOutsideTapAllowed(outside, moved, consumed) &&
                    !latestBounds().contains(change.position)) {
                    change.consume()
                    latestDismiss()
                }
                break
            }
        }
    }
    }
}
