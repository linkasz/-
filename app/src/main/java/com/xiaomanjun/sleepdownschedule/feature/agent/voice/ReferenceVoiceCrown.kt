package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.glass.ui.TopAssistantSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.rememberAssistantGlassPalette
import com.xiaomanjun.sleepdownschedule.glass.ui.fixedAssistantContentWidth
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.xiaomanjun.sleepdownschedule.feature.agent.AssistantExpandHandle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Video geometry is proportional to the usable app window, never the OEM status-bar island. */
@Composable
internal fun ReferenceVoiceCrown(
    session: VoiceSession, phase: VoicePhase, backdrop: Backdrop?, config: ScheduleConfigEntity,
    availableWidth: Dp, maxHeight: Dp, question: String, reply: String,
    assistantName: String = "时序清单",
    sessionStartedAt: Long = 0L,
    input: String, onInput: (String) -> Unit, onSend: () -> Unit,
    onMic: () -> Unit, onExpand: () -> Unit,
    onAdd: () -> Unit, onSettings: () -> Unit, onBounds: (Rect) -> Unit,
    heightOverride: () -> Float = { Float.NaN },
    onResizeStart: (() -> Unit)? = null, onResize: ((Float) -> Unit)? = null,
    onResizeEnd: ((Boolean) -> Unit)? = null,
    proposalContent: @Composable ColumnScope.() -> Unit = {}, modifier: Modifier = Modifier
) {
    // RMS is observed in Canvas only. The surrounding conversation never recomposes per sample.
    val audioState = session.state.collectAsState()
    val palette = rememberAssistantGlassPalette(config)
    val foreground = palette.foreground
    var loadingHeld by remember(sessionStartedAt) { mutableStateOf(sessionStartedAt > 0 && android.os.SystemClock.elapsedRealtime() - sessionStartedAt < 240) }
    LaunchedEffect(sessionStartedAt, phase) {
        if (phase in listOf(VoicePhase.OFF, VoicePhase.ERROR)) { loadingHeld = false; return@LaunchedEffect }
        val remaining = (240 - (android.os.SystemClock.elapsedRealtime() - sessionStartedAt)).coerceAtLeast(0)
        if (remaining > 0) { loadingHeld = true; delay(remaining) }
        loadingHeld = false
    }
    val thinking = loadingHeld || phase in listOf(VoicePhase.CONNECTING, VoicePhase.THINKING, VoicePhase.QUERYING)
    var replyHidden by remember { mutableStateOf(false) }
    LaunchedEffect(reply) { replyHidden = false }
    LaunchedEffect(phase) { if (phase in listOf(VoicePhase.THINKING, VoicePhase.QUERYING)) replyHidden = true }
    val showReply = reply.isNotBlank() && phase != VoicePhase.CONNECTING && !loadingHeld && !replyHidden
    val showCard = showReply || !loadingHeld && phase in listOf(VoicePhase.THINKING, VoicePhase.QUERYING) && question.isNotBlank()
    val geometry = updateTransition(if (showCard) 2 else if (thinking) 0 else 1, label = "voiceCrownMorph")
    val waitingMix = geometry.animateFloat(transitionSpec = { spring(dampingRatio = .78f, stiffness = 320f) }, label = "contentMorph") {
        if (it == 0) 1f else 0f
    }
    val width by geometry.animateDp(transitionSpec = { spring(dampingRatio = .78f, stiffness = 320f) }, label = "width") { stage -> when (stage) {
        2 -> availableWidth
        0 -> minOf(228.dp, availableWidth * .58f)
        else -> minOf(178.dp, availableWidth * .45f)
    } }
    val orbHeight by geometry.animateDp(transitionSpec = { spring(dampingRatio = .78f, stiffness = 320f) }, label = "height") { stage -> when (stage) {
        0 -> 56.dp
        else -> minOf(178.dp, availableWidth * .45f) * .82f
    } }
    val corner by geometry.animateDp(transitionSpec = { spring(dampingRatio = .78f, stiffness = 320f) }, label = "corner") { if (it == 2) 32.dp else 100.dp }
    val shape = RoundedCornerShape(corner)
    var measuredHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val resized by remember { derivedStateOf { !heightOverride().isNaN() } }
    val cardHeight = minOf(maxHeight, 440.dp)
    val shellHeight by geometry.animateDp(transitionSpec = { spring(dampingRatio = .78f, stiffness = 320f) }, label = "shellHeight") {
        // Measure the real reply + composer instead of reserving a blank 440dp card for every answer.
        when (it) { 2 -> (if (measuredHeight > 0.dp) measuredHeight else cardHeight).coerceAtMost(cardHeight)
            0 -> 56.dp; else -> minOf(178.dp, availableWidth * .45f) * .82f }
    }
    val height = shellHeight
    // Pointer height is read in measurement, avoiding recomposing text/persona/tool UI per sample.
    val viewport = Modifier.layout { measurable, constraints ->
        val requested = heightOverride()
        val pixels = (if (requested.isNaN()) height.toPx() else requested)
            .toInt().coerceIn(1, maxHeight.roundToPx().coerceAtLeast(1))
        val child = measurable.measure(Constraints.fixed(constraints.maxWidth, pixels))
        layout(child.width, child.height) { child.place(0, 0) }
    }
    Box(modifier.width(width.coerceAtMost(availableWidth)).then(viewport).clip(shape)
        .onGloballyPositioned { onBounds(it.boundsInRoot()) }
        .combinedClickable(onClick = if (showCard) ({}) else onMic, onLongClick = onSettings,
            onLongClickLabel = "语音设置")
        .semantics { contentDescription = if (showCard) "语音助手回复，点击展开完整对话" else phase.label }
        ) {
        TopAssistantSurface(backdrop, config, shape, Modifier.matchParentSize(),
            opaqueHeaderHeight = if (showCard) (measuredHeight - 72.dp).coerceAtLeast(0.dp) else 0.dp,
            bottomShadeAlpha = if (thinking && !showCard && !palette.light) .3f else .04f,
            lightSurface = palette.light, frostedConversation = true,
            audioLevel = { audioState.value.level })
        if (showCard) {
            // Reveal the shell while keeping paragraph width fixed, like the existing expanded route.
            Column(Modifier.fillMaxWidth().layout { measurable, constraints ->
                val requested = heightOverride()
                val widthPx = availableWidth.roundToPx()
                val heightPx = (if (requested.isNaN()) cardHeight.toPx() else requested)
                    .toInt().coerceIn(1, maxHeight.roundToPx().coerceAtLeast(1))
                val child = measurable.measure(Constraints(widthPx, widthPx,
                    minHeight = if (requested.isNaN()) 0 else heightPx, maxHeight = heightPx))
                if (requested.isNaN()) measuredHeight = with(density) { child.height.toDp() }
                layout(constraints.constrainWidth(child.width), constraints.constrainHeight(child.height)) { child.place(0, 0) }
            }) {
                // Keep the composer and resize handle reachable when IME/rotation shortens the window.
                Column(Modifier.fillMaxWidth().weight(1f, fill = resized).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(question, color = foreground.copy(alpha = .9f), style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4, modifier = Modifier.weight(1f))
                }
                // Listening resumes after every answer. Keep its waveform visible alongside the reply.
                CrownVoiceStatus(audioState, phase, foreground, onSettings)
                if (showReply) Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(palette.base.copy(alpha = .48f)).padding(14.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFB6DFFF), modifier = Modifier.size(18.dp))
                            Text(assistantName, color = foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium,
                                maxLines = 2, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(reply, color = foreground, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp))
                        proposalContent()
                    }
                }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrownIcon(Icons.Default.Add, "附件", foreground, onAdd, backdrop, config)
                    Box(Modifier.weight(1f)) {
                    GlassSurface(backdrop, config, Modifier.matchParentSize()
                        .border(1.dp, foreground.copy(alpha = .32f), RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp), tokens = palette.composerTokens,
                        baseSurfaceColorOverride = palette.base, restingDecorations = true) {}
                    BasicTextField(input, onInput, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = foreground),
                        cursorBrush = SolidColor(Color(0xFF57C9FF)), maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) {
                            if (input.isEmpty()) Text("询问时序清单", color = foreground.copy(alpha = .55f), style = MaterialTheme.typography.bodyMedium)
                            inner()
                        } })
                    }
                    VoiceMicrophoneButton(VoiceSessionState(phase), backdrop, config, palette,
                        onClick = onMic, onSettings = onSettings)
                    if (input.isNotBlank()) CrownIcon(Icons.Default.ArrowUpward, "发送文字", foreground, onSend, backdrop, config)
                }
                // Row bottom inset (8dp) + footer's visible-bar top (12dp) gives the shared 20dp gap.
                AssistantExpandHandle(foreground, onExpand, modifier = Modifier.height(24.dp), onResizeStart = onResizeStart,
                    onResize = onResize, onResizeEnd = onResizeEnd)
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(orbHeight)) {
                    VoiceRibbon(audioState, thinking, Modifier.fillMaxSize(), active = phase != VoicePhase.OFF && phase != VoicePhase.ERROR,
                        waitingMix = waitingMix)
                }
                if (phase == VoicePhase.ERROR || phase == VoicePhase.OFF) {
                    Text(audioState.value.error ?: "语音已暂停，点击继续", color = foreground,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CrownVoiceStatus(state: State<VoiceSessionState>, phase: VoicePhase, foreground: Color,
    onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        val active = phase != VoicePhase.OFF && phase != VoicePhase.ERROR
        VoiceRibbon(state, phase in listOf(VoicePhase.CONNECTING, VoicePhase.THINKING, VoicePhase.QUERYING), Modifier.size(52.dp, 36.dp), active)
        Text(if (phase == VoicePhase.ERROR) state.value.error ?: phase.label else phase.label, color = foreground.copy(alpha = .85f),
            style = MaterialTheme.typography.labelSmall, maxLines = 2, modifier = Modifier.weight(1f))
        // The composer owns the only microphone action, including connection-error states.
        if (phase == VoicePhase.ERROR) CrownIcon(Icons.Default.Settings, "检查语音配置", foreground, onSettings)
    }
}

@Composable
private fun CrownIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, foreground: Color, onClick: () -> Unit,
    backdrop: Backdrop? = null, config: ScheduleConfigEntity? = null) {
    if (config != null) {
        val palette = rememberAssistantGlassPalette(config)
        GlassSurface(backdrop, config, Modifier.size(48.dp).border(1.dp, foreground.copy(alpha = .32f), CircleShape),
            shape = CircleShape, tokens = palette.composerTokens, baseSurfaceColorOverride = palette.base,
            restingDecorations = true, onClick = onClick) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = foreground, modifier = Modifier.size(23.dp))
            }
        }
        return
    }
    IconButton(onClick, Modifier.size(48.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background((if (foreground == Color.White) Color.Black else Color.White).copy(alpha = .44f))
            .border(.8.dp, foreground.copy(alpha = .4f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = foreground, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
internal fun VoiceRibbon(state: State<VoiceSessionState>, thinking: Boolean, modifier: Modifier, active: Boolean = true,
    waitingMix: State<Float>? = null) {
    val time = remember { mutableFloatStateOf(0f) }
    val smoothed = remember { mutableFloatStateOf(0f) }
    val paths = remember { Array(3) { Path() } }
    val palette = remember { listOf(Color(0xFF198BFF), Color(0xFFAB62F4), Color(0xFF18BCB7)) }
    // Phase is supplied by the caller's distinct status flow. Read RMS only during drawing.
    val processing = thinking
    LaunchedEffect(active, processing) {
        if (!active) { smoothed.floatValue = 0f; return@LaunchedEffect }
        var previous = withFrameNanos { it }
        val start = previous
        while (isActive) withFrameNanos { frame ->
            val delta = ((frame - previous) / 1_000_000_000f).coerceIn(0f, .1f)
            previous = frame
            val target = if (processing) 0f else state.value.level.coerceIn(0f, 1f)
            val speed = if (target > smoothed.floatValue) 25f else 10f
            smoothed.floatValue += (target - smoothed.floatValue) * (1f - kotlin.math.exp(-speed * delta))
            time.floatValue = (frame - start) / 1_000_000_000f
        }
    }
    Canvas(modifier) {
        val center = size.height * .5f
        val waiting = (waitingMix?.value ?: if (processing) 1f else 0f).coerceIn(0f, 1f)
        if (waiting > 0f) {
            // Pending network state, deliberately distinct from the measured speech waveform.
            for (i in 0..2) drawCircle(palette[i].copy(alpha = waiting * (.55f + .35f * sin(time.floatValue * 4f - i).coerceAtLeast(0f))),
                3.dp.toPx(), Offset(size.width * .5f + (i - 1) * 13.dp.toPx(), center))
        }
        if (waiting < 1f) {
            val level = if (active) smoothed.floatValue else 0f
            drawLine(palette[0].copy(alpha = .25f * (1f - waiting)), Offset(size.width * .12f, center),
                Offset(size.width * .88f, center), 1.5.dp.toPx(), StrokeCap.Round)
            if (level > .002f) palette.forEachIndexed { lane, color ->
                val path = paths[lane]; path.reset()
                for (direction in listOf(1f, -1f)) for (sample in if (direction > 0) 0..64 else 64 downTo 0) {
                    val position = sample / 64f
                    val y = siriWaveHeight((position - .5f) * 4f, time.floatValue, lane, level) * size.height * .45f
                    val x = size.width * (.1f + .8f * position)
                    if (direction > 0 && sample == 0) path.moveTo(x, center - y) else path.lineTo(x, center - direction * y)
                }
                path.close()
                drawPath(path, color.copy(alpha = .3f * (1f - waiting)))
                drawPath(path, color.copy(alpha = .85f * (1f - waiting)), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
internal fun VoiceWindowRim(modifier: Modifier = Modifier) {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) withFrameNanos { clock.floatValue = ((it - start) / 9_000_000_000.0).toFloat() % 1f }
    }
    Canvas(modifier) {
        val palette = listOf(Color(0xFF78DFFF), Color(0xFF8C7FFF), Color(0xFFFF91CB), Color(0xFFFFDA79))
        val shift = clock.floatValue
        val stops = palette.mapIndexed { i, color -> ((i / 4f + shift) % 1f) to color }.sortedBy { it.first }
        val boundary = lerp(stops.last().second, stops.first().second,
            (1f - stops.last().first) / (1f + stops.first().first - stops.last().first))
        val rim = Brush.sweepGradient(*(listOf(0f to boundary) + stops + (1f to boundary)).toTypedArray())
        val corner = CornerRadius(28.dp.toPx())
        listOf(18f to .045f, 10f to .1f, 5f to .22f, 1.5f to .9f).forEach { (width, alpha) ->
            val inset = width.dp.toPx() / 2f
            drawRoundRect(rim, topLeft = Offset(inset, inset), size = size.copy(
                width = (size.width - inset * 2).coerceAtLeast(0f), height = (size.height - inset * 2).coerceAtLeast(0f)),
                cornerRadius = corner, style = Stroke(width.dp.toPx()), alpha = alpha)
        }
    }
}
