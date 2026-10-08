package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.*

/** Readback has its own model/voice catalog; Omni never generates a second readback script. */
@Composable
internal fun AliyunTtsSettings(draft: VoiceSettings, enabled: Boolean, backdrop: Backdrop?,
    config: ScheduleConfigEntity, onChange: (VoiceSettings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember { VoicePreviewPlayer(context, scope) }
    var voices by remember { mutableStateOf<List<TtsVoice>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var audition by remember { mutableStateOf<Job?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf("全部") }
    var gender by remember { mutableStateOf("全部") }
    var picker by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    fun stopPreview() { audition?.cancel(); audition = null; player.stop() }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) stopPreview() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); stopPreview() }
    }
    LaunchedEffect(draft.ttsModel, revision) {
        busy = true; feedback = null
        try { voices = withContext(Dispatchers.IO) { AliyunTtsVoices.load(context, draft.ttsModel) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { feedback = "音色目录读取失败，请重试" }
        finally { busy = false }
    }
    val selected = voices.firstOrNull { it.id == draft.ttsVoice }
    val filtered = voices.filter { (language == "全部" || language in it.language) && (gender == "全部" || gender == it.gender) }
    val active = enabled && !busy && audition == null
    Text("专用朗读 · ${draft.ttsModel}", style = MaterialTheme.typography.labelLarge)
    Text("只朗读聊天中已显示的回复；默认 Serena。选择音色不会改变回答模型。", style = MaterialTheme.typography.bodySmall)
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    AdaptiveGlassActions(listOf(
        GlassAction("语言：$language", Icons.Default.Language, active) { picker = "language" },
        GlassAction("性别：$gender", Icons.Default.Person, active) { picker = "gender" }
    ), backdrop, config)
    GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = if (active) ({ picker = "voice" }) else null) {
        Text("音色 · ${selected?.name ?: draft.ttsVoice} ▾", Modifier.padding(12.dp))
    }
    if (filtered.isEmpty() && !busy) Text("暂无符合筛选的音色，可切换为“全部”。", style = MaterialTheme.typography.bodySmall)
    AdaptiveGlassActions(listOf(
        GlassAction("刷新官方音色", Icons.Default.Refresh, active) {
            stopPreview(); busy = true; feedback = null
            scope.launch {
                try { voices = AliyunTtsVoices.refresh(context, draft.ttsModel); feedback = "已更新 ${voices.size} 款官方音色" }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { feedback = e.message ?: "刷新失败，保留现有目录" }
                finally { busy = false }
            }
        },
        GlassAction("账号音色", Icons.Default.CloudDownload, active) {
            busy = true; feedback = null
            scope.launch {
                try {
                    val custom = AliyunTtsVoices.accountVoices(draft)
                    voices = (voices.filter { it.source != "account" } + custom).distinctBy { it.id }
                    feedback = if (custom.isEmpty()) "该账号没有兼容此朗读模型的自定义音色，内置音色仍可使用。" else "已加载 ${custom.size} 款兼容的账号音色"
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { feedback = e.message ?: "账号音色读取失败" }
                finally { busy = false }
            }
        }
    ), backdrop, config)
    AdaptiveGlassActions(listOf(
        GlassAction(if (audition != null) "停止试听" else "试听", if (audition != null) Icons.Default.Stop else Icons.Default.PlayArrow,
            enabled && !busy && selected != null) {
            if (audition != null) stopPreview() else {
                feedback = null
                audition = scope.launch {
                    try { player.play(requireNotNull(selected), draft) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { feedback = e.message ?: "试听失败，请重试" }
                    finally { audition = null }
                }
            }
        },
        GlassAction("重新读取", Icons.Default.Replay, active) { revision++ }
    ), backdrop, config)
    if (audition != null) Text("正在试听…", style = MaterialTheme.typography.bodySmall)
    Text(if (selected?.sampleUrl != null) "试听播放官方样例，不调用模型。" else "此音色无官方样例，试听将调用百炼合成固定文本。", style = MaterialTheme.typography.bodySmall)
    feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    SleepDownPickerDialog(picker != null, when (picker) { "voice" -> "选择音色"; "language" -> "筛选语言"; else -> "筛选性别" },
        onDismissRequest = { picker = null }, backdrop = backdrop, config = config) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (picker == "voice") items(filtered, key = { it.id }) { voice ->
                GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp), selected = voice.id == draft.ttsVoice,
                    onClick = { stopPreview(); onChange(draft.copy(ttsVoice = voice.id)); picker = null }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${voice.name} · ${voice.gender}")
                        Text("${voice.id} · ${voice.style}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val options = if (picker == "language") listOf("全部") + voices.flatMap { it.language }.distinct().sorted()
                    else listOf("全部", "女", "男", "未知")
                items(options) { option ->
                    GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                        if (picker == "language") language = option else gender = option; picker = null
                    }) { Text(option, Modifier.padding(12.dp)) }
                }
            }
        }
    }
}
