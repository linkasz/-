package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

internal fun constrainedAssistantHeight(height: Float, minimum: Float, maximum: Float): Float =
    height.coerceIn(minOf(minimum, maximum).coerceAtLeast(1f), maximum.coerceAtLeast(1f))

/** Only a deliberate swipe in the destination's direction switches modes. */
internal fun assistantHandleSwipeCommits(distance: Float, expanded: Boolean, threshold: Float): Boolean =
    if (expanded) distance <= -threshold else distance >= threshold

/** The shared handle owns its swipe; the conversation keeps its existing two-state morph. */
@Composable
internal fun AssistantExpandHandle(
    foreground: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "展开完整对话",
    expanded: Boolean = false,
    onResizeStart: (() -> Unit)? = null,
    onResize: ((Float) -> Unit)? = null,
    onResizeEnd: ((Boolean) -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val latestAction by rememberUpdatedState(onExpand)
    val latestStart by rememberUpdatedState(onResizeStart)
    val latestResize by rememberUpdatedState(onResize)
    val latestEnd by rememberUpdatedState(onResizeEnd)
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var distance by remember { mutableFloatStateOf(0f) }
    val rootY = remember { floatArrayOf(0f) }
    val pointerRootY = remember { floatArrayOf(0f) }
    val offset by animateFloatAsState(if (dragging) distance.coerceIn(-threshold * 2, threshold * 2) * .15f else 0f,
        spring(dampingRatio = .72f, stiffness = 420f), label = "assistantHandleRebound")
    val scale by animateFloatAsState(if (dragging) 1.12f else if (pressed) .88f else 1f,
        spring(dampingRatio = .72f, stiffness = 420f), label = "assistantHandlePress")
    Box(modifier.fillMaxWidth().height(48.dp).onGloballyPositioned { rootY[0] = it.positionInRoot().y }
        .semantics { contentDescription = actionLabel }.pointerInput(expanded, threshold) {
        detectVerticalDragGestures(
            onDragStart = { position -> dragging = true; distance = 0f; pointerRootY[0] = position.y + rootY[0]; latestStart?.invoke() },
            onDragCancel = { dragging = false; distance = 0f; latestEnd?.invoke(true) },
            onDragEnd = {
                val commit = assistantHandleSwipeCommits(distance, expanded, threshold)
                dragging = false; distance = 0f
                if (latestResize != null) latestEnd?.invoke(false) else if (commit) latestAction()
            },
            onVerticalDrag = { change, _ ->
                change.consume()
                // The handle itself moves during resize. Local deltas include that movement and lose travel.
                val absoluteY = change.position.y + rootY[0]
                val delta = absoluteY - pointerRootY[0]
                pointerRootY[0] = absoluteY
                distance += delta; latestResize?.invoke(delta)
            }
        )
    }.clickable(interactionSource = interaction,
        indication = null, role = Role.Button, onClickLabel = actionLabel, onClick = onExpand),
        contentAlignment = Alignment.BottomCenter) {
        // Keep the 48dp touch target, while the visible 4dp bar sits two bar heights above the edge.
        Box(Modifier.padding(bottom = 8.dp).size(36.dp, 4.dp).graphicsLayer { scaleX = scale; scaleY = scale; translationY = offset }
            .clip(CircleShape).background(foreground.copy(alpha = .65f)))
    }
}
