package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogCapsuleField
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AdaptiveGlassTabs
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.GlassTab
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AdaptiveGlassActions
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.GlassAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import com.xiaomanjun.sleepdownschedule.app.ui.LiquidControlToggle
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.AssistantGlassPalette
import com.xiaomanjun.sleepdownschedule.glass.ui.rememberAssistantGlassPalette
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.feature.settings.AiModelSelector
import com.xiaomanjun.sleepdownschedule.feature.settings.AiModelDiscoveryState
import com.xiaomanjun.sleepdownschedule.feature.settings.AiModelDiscoveryControl
import com.xiaomanjun.sleepdownschedule.feature.settings.AiModelDiscoveryDialog
import com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettings
import com.xiaomanjun.sleepdownschedule.feature.importing.mergeAiModelIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun VoiceMicrophoneButton(state: VoiceSessionState, backdrop: Backdrop?, config: ScheduleConfigEntity,
    palette: AssistantGlassPalette, onClick: () -> Unit, onSettings: () -> Unit) {
    GlassSurface(backdrop, config, modifier = Modifier.size(48.dp)
        .border(1.dp, if (state.active) palette.accent else palette.foreground.copy(alpha = .32f), CircleShape)
        .semantics { selected = state.active }, shape = CircleShape,
        tokens = palette.composerTokens, baseSurfaceColorOverride = palette.base,
        restingDecorations = true,
        selected = state.active, selectedSurfaceColorOverride = Color(0xFF188FFF),
        onClick = onClick, onLongClick = onSettings) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.animation.Crossfade(state.phase to state.canRetryReadback, label = "voiceControlPhase") { (phase, retry) ->
                val interrupt = phase in listOf(VoicePhase.SPEAKING, VoicePhase.THINKING, VoicePhase.QUERYING)
                Icon(if (retry) Icons.Default.Replay else if (interrupt) Icons.Default.GraphicEq else if (state.active) Icons.Default.Pause else Icons.Default.Mic,
                    if (retry) "重试朗读，长按配置" else if (interrupt) "打断回复，长按配置" else if (state.active) "关闭语音，长按配置" else "开启语音，长按配置",
                    tint = if (state.active) Color.White else palette.foreground, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
internal fun VoiceInteractionStrip(session: VoiceSession, backdrop: Backdrop?, config: ScheduleConfigEntity,
    foreground: Color, onInterrupt: () -> Unit, onStop: () -> Unit, onSettings: () -> Unit) {
    val audioState = session.state.collectAsState()
    val statusFlow = remember(session) { session.state.map { it.copy(level = 0f) }.distinctUntilChanged() }
    val state by statusFlow.collectAsState(VoiceSessionState())
    if (state.phase == VoicePhase.OFF) return
    val palette = rememberAssistantGlassPalette(config)
    GlassSurface(backdrop, config, Modifier.fillMaxWidth().padding(bottom = 8.dp)
        .border(1.dp, Brush.linearGradient(listOf(Color(0xFFFFB8B9), Color(0xFFB5A4FF), Color(0xFF86DFFF))), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp), tokens = palette.controlTokens,
        baseSurfaceColorOverride = palette.base, onClick = if (state.canRetryReadback) ({ session.retryReadback() }) else if (state.active) onInterrupt else onSettings) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            VoiceRibbon(audioState, state.phase in listOf(VoicePhase.CONNECTING, VoicePhase.THINKING, VoicePhase.QUERYING), Modifier.size(52.dp, 36.dp), state.active)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(state.error ?: state.phase.label, color = foreground, style = MaterialTheme.typography.labelMedium)
                if (state.active) Text(if (state.phase == VoicePhase.SPEAKING) "点击打断 · 关闭停止录音" else "可直接继续说话 · 时序清单智能助理", color = foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
            }
            // Configuration is not a second microphone control; use the same settings glyph as the page.
            IconButton(onSettings, Modifier.size(48.dp)) { Icon(Icons.Default.Settings, "语音设置", tint = foreground) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VoiceSettingsDialog(backdrop: Backdrop?, config: ScheduleConfigEntity, onDismiss: () -> Unit, onSaved: () -> Unit = {}, show: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(VoiceSettingsStore.load(context)) }
    val editedProfiles = remember { mutableMapOf<VoicePlatform, VoiceSettings>() }
    var checking by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showVoicePicker by remember { mutableStateOf(false) }
    val discovery = remember(draft.platform, draft.apiKey, draft.region, draft.workspace) { AiModelDiscoveryState() }
    DisposableEffect(discovery) { onDispose { discovery.request?.cancel() } }
    SleepDownPickerDialog(
        show = show, title = "语音平台与模型", onDismissRequest = onDismiss,
        backdrop = backdrop, config = config, scrollableContent = true,
        bottomActions = {
            AdaptiveGlassActions(listOf(
                GlassAction("取消", Icons.Default.Close, onClick = onDismiss),
                GlassAction(if (checking) "检查中" else "连接检查", Icons.Default.Wifi, !checking) {
                    checking = true; feedback = null
                    scope.launch {
                        try { feedback = checkVoiceConnection(draft) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { feedback = error.message ?: "连接检查失败" }
                        finally { checking = false }
                    }
                },
                GlassAction("保存", Icons.Default.Check, !checking, true) {
                    runCatching { VoiceSettingsStore.save(context, draft) }.onSuccess { onSaved(); onDismiss() }
                        .onFailure { feedback = it.message ?: "保存失败" }
                }
            ), backdrop, config)
        }
    ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("百炼 Omni 实时模型直接回答并访问课程与日程；转写专用模型将文字交给已配置的 AI 助理。朗读使用最终显示的同一条回复。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("自动朗读回复")
                        Text("关闭后仅显示文字；百炼使用专用 TTS，其他平台使用合成或系统朗读。", style = MaterialTheme.typography.bodySmall)
                    }
                    LiquidControlToggle(draft.readAloud, { draft = draft.copy(readAloud = it) }, backdrop, enabled = !checking)
                }
                AdaptiveGlassTabs(VoicePlatform.entries.map { platform -> GlassTab(platform.name, platform.label,
                    when (platform) { VoicePlatform.ALIYUN -> Icons.Default.Cloud; VoicePlatform.OPENAI -> Icons.Default.AutoAwesome;
                        VoicePlatform.GLM -> Icons.Default.Psychology; VoicePlatform.SILICON_FLOW -> Icons.Default.GraphicEq;
                        VoicePlatform.SYSTEM -> Icons.Default.PhoneAndroid }) }, draft.platform.name, backdrop, config, enabled = !checking) { id ->
                            val platform = VoicePlatform.valueOf(id)
                            editedProfiles[draft.platform] = draft
                            val readAloud = draft.readAloud
                            draft = (editedProfiles[platform] ?: VoiceSettingsStore.profile(context, platform)).copy(readAloud = readAloud)
                            feedback = null
                }
                Text("模型", style = MaterialTheme.typography.labelLarge)
                if (draft.platform == VoicePlatform.SILICON_FLOW) Text(
                    "连续语音：分段识别 → 当前文字助手 → 流式合成。下列为识别与合成组合，非原生双向实时模型。",
                    style = MaterialTheme.typography.bodySmall)
                AiModelSelector("语音模型", draft.model, draft.modelOptions, backdrop, config, enabled = !checking,
                    label = { if (draft.platform == VoicePlatform.SILICON_FLOW) SiliconVoicePreset.resolve(it).label else it }) { model ->
                    require(isSupportedVoiceModel(draft.platform, model)) { "此模型不支持当前语音协议，请选择兼容的语音模型" }
                    draft = draft.withModel(model).copy(availableModels = mergeAiModelIds(draft.modelOptions, listOf(model)))
                    feedback = null
                }
                if (draft.platform == VoicePlatform.ALIYUN) {
                    AliyunTtsSettings(draft, !checking, backdrop, config) { draft = it }
                } else {
                Text("朗读方式", style = MaterialTheme.typography.labelLarge)
                if (draft.platform == VoicePlatform.SILICON_FLOW) GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    onClick = if (!checking) ({ showVoicePicker = true }) else null) {
                    Text("合成音色 · ${draft.voice} ▾", Modifier.padding(12.dp))
                } else Text("回复使用手机系统中文音色；实时模型不再生成另一份朗读内容。", style = MaterialTheme.typography.bodySmall)
                }
                if (draft.platform != VoicePlatform.SYSTEM) DialogCapsuleField(draft.apiKey, { draft = draft.copy(apiKey = it) },
                    "API Key（本机加密保存）", config, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(), enabled = !checking)
                if (draft.platform == VoicePlatform.ALIYUN) {
                    Text("API Key、地域与业务空间需相互对应；模型权限由百炼账号决定。连接检查仅检查所选模型。",
                        style = MaterialTheme.typography.bodySmall)
                    AdaptiveGlassTabs(listOf(GlassTab("cn-beijing", "北京", Icons.Default.LocationCity),
                        GlassTab("ap-southeast-1", "新加坡", Icons.Default.Public)), draft.region, backdrop, config, enabled = !checking) {
                        draft = draft.copy(region = it)
                    }
                    DialogCapsuleField(draft.workspace, { draft = draft.copy(workspace = it) }, "百炼业务空间 ID", config,
                        modifier = Modifier.fillMaxWidth(), enabled = !checking)
                }
                if (draft.platform != VoicePlatform.SYSTEM) {
                    AiModelDiscoveryControl(discovery, scope, AiImportSettings(voiceModelDiscoveryProfile(draft), draft.apiKey),
                        backdrop, config, enabled = !checking, fetchModels = { discoverVoiceModels(draft) })
                    Text("目录仅列出当前语音协议可接入的模型，实际权限需通过连接检查确认。", style = MaterialTheme.typography.bodySmall)
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
    }
    SleepDownPickerDialog(showVoicePicker, "选择合成音色", onDismissRequest = { showVoicePicker = false }, backdrop = backdrop, config = config,
        scrollableContent = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.availableVoices.forEach { voice ->
                VoiceSettingsChoice(voice, draft.voice == voice, !checking, backdrop, config) {
                    draft = draft.copy(voice = voice); showVoicePicker = false
                }
            }
        }
    }
    AiModelDiscoveryDialog(discovery, draft.modelOptions, backdrop, config) { ids ->
        draft = draft.copy(availableModels = mergeAiModelIds(draft.modelOptions, ids))
    }
}

@Composable
private fun VoiceSettingsChoice(label: String, selected: Boolean, enabled: Boolean, backdrop: Backdrop?,
    config: ScheduleConfigEntity, onClick: () -> Unit) {
    GlassPill(backdrop, config, modifier = Modifier.heightIn(min = 48.dp).semantics {
        this.selected = selected
        role = Role.Button
        if (!enabled) disabled()
    }, selected = selected, selectedSurfaceColorOverride = Color(0xFF168BFF),
        onClick = if (enabled) onClick else null) {
        Box(Modifier.defaultMinSize(minHeight = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center) {
            Text(label, color = (if (selected) Color.White else LocalContentColor.current).copy(alpha = if (enabled) 1f else .4f),
                style = MaterialTheme.typography.labelLarge)
        }
    }
}
