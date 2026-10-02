package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable
internal fun AgentPersonaDialog(backdrop: Backdrop?, config: ScheduleConfigEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var library by remember { mutableStateOf<PersonaLibrary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<PersonaCard?>(null) }
    var copied by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonaCard?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(revision) {
        error = null
        try { library = withContext(Dispatchers.IO) { AgentPersonaRepository.load(context) } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "人格库读取失败，请重试" }
    }
    fun save(next: PersonaLibrary) {
        if (saving) return
        saving = true
        scope.launch {
            try { withContext(Dispatchers.IO) { AgentPersonaRepository.save(context, next) }; library = next.validated(); editing = null }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { error = "人格保存失败，请重试" }
            finally { saving = false }
        }
    }
    SleepDownFormDialog(onDismiss) {
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader("助理人格", onDismiss, backdrop, config)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("给陪你规划每一天的伴侣起个名字，设定身份和它对你的称呼。文字下次回复生效，语音下次开启生效；课程、天气和待办能力继续保留。") }
                if (error != null) item {
                    Text(error!!)
                    QuickSheetLiquidAction("重试", true, backdrop, config, onClick = { revision++ })
                }
                if (library == null && error == null) item { CircularProgressIndicator() }
                if (library?.cards?.isEmpty() == true) item { Text("暂无人格配置") }
                items(library?.cards.orEmpty(), key = { it.meta.id }) { card ->
                    // Multiline persona content needs a rectangular card, not a clipping capsule.
                    GlassSurface(backdrop, config, Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(card.meta.name + if (card.isDefault) " · 当前默认" else if (!card.enabled) " · 已停用" else "",
                                style = MaterialTheme.typography.titleMedium)
                            val profile = card.companionProfile()
                            Text("${profile.name} · ${profile.identity}", style = MaterialTheme.typography.bodySmall)
                            Text("称呼你：${profile.userAddress.ifBlank { "你" }}", style = MaterialTheme.typography.bodySmall)
                            AdaptiveGlassActions(listOf(
                                GlassAction("预览提示词", Icons.Default.Visibility, !saving) { copied = false; preview = card },
                                GlassAction("编辑", Icons.Default.Edit, !saving) { editing = card.copy(companion = card.companionProfile()) }
                            ), backdrop, config)
                            AdaptiveGlassActions(listOf(
                                GlassAction(if (card.enabled) "停用" else "启用", Icons.Default.PowerSettingsNew, !saving) {
                                    library?.let { save(it.toggle(card.meta.id)) }
                                },
                                GlassAction("设为默认", Icons.Default.Check, !saving && card.enabled && !card.isDefault, card.isDefault) {
                                    library?.let { save(it.copy(defaultId = card.meta.id)) }
                                }
                            ), backdrop, config)
                        }
                    }
                }
            }
        }
    }
    // The preview remains a business-only DTO and uses the common animated, readable picker.
    SleepDownPickerDialog(preview != null, "${preview?.meta?.name.orEmpty()} · 提示词", onDismissRequest = { preview = null }, backdrop = backdrop, config = config,
        titleAction = { IconButton({ preview = null }) { Icon(Icons.Default.Close, "关闭预览") } },
        scrollableContent = true, bottomActions = {
            AdaptiveGlassActions(listOf(
                GlassAction(if (copied) "已复制" else "复制全文", Icons.Default.ContentCopy, preview != null, true) {
                    preview?.let { clipboard.setText(AnnotatedString(AgentPersonaRepository.preview(it))); copied = true }
                },
                GlassAction("关闭", Icons.Default.Close) { preview = null }
            ), backdrop, config)
        }) {
            Text("业务层 · 可编辑与复制", style = MaterialTheme.typography.labelMedium)
            SelectionContainer { Text(preview?.let(AgentPersonaRepository::preview).orEmpty(), style = MaterialTheme.typography.bodyMedium) }
        }
    SleepDownPickerDialog(editing != null, "编辑人格", onDismissRequest = { if (!saving) editing = null }, backdrop = backdrop, config = config,
        scrollableContent = true, bottomActions = {
            QuickSheetLiquidAction(if (saving) "保存中…" else "保存", !saving && editing?.meta?.name?.isNotBlank() == true && editing?.visiblePrompt?.isNotBlank() == true,
                backdrop, config, primary = true, onClick = { library?.let { l -> editing?.let { card -> save(l.copy(cards = l.cards.map { if (it.meta.id == card.meta.id) card else it })) } } })
        }) {
        LabeledDialogCapsuleField("人格卡片名称：", editing?.meta?.name.orEmpty(), { editing = editing?.let { p -> p.copy(meta = p.meta.copy(name = it.take(32))) } }, "列表中显示的名称", config, enabled = !saving)
        LabeledDialogCapsuleField("助理的名字：", editing?.companion?.name.orEmpty(), { editing = editing?.let { p -> p.copy(companion = p.companion.copy(name = it.take(32))) } }, "如：小序", config, enabled = !saving)
        LabeledDialogCapsuleField("身份设定：", editing?.companion?.identity.orEmpty(), { editing = editing?.let { p -> p.copy(companion = p.companion.copy(identity = it.take(1000))) } }, "如：温柔的计划伙伴", config, minLines = 2, enabled = !saving)
        LabeledDialogCapsuleField("对你的称呼：", editing?.companion?.userAddress.orEmpty(), { editing = editing?.let { p -> p.copy(companion = p.companion.copy(userAddress = it.take(32))) } }, "留空使用‘你’", config, enabled = !saving)
        Text("档案与表达风格一起用于文字、语音和提示词预览。这里的编辑不会自动修改日程。", style = MaterialTheme.typography.bodySmall)
        LabeledDialogCapsuleField("表达风格与业务提示词：", editing?.visiblePrompt.orEmpty(), { editing = editing?.copy(visiblePrompt = it.take(12000)) }, "描述说话风格、回复长度和偏好", config, minLines = 6, enabled = !saving)
        if (error != null) Text(error!!)
    }
}
