package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.feature.importing.*
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill
import com.xiaomanjun.sleepdownschedule.glass.ui.rememberGlassMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
internal class AiModelDiscoveryState {
    var request by mutableStateOf<Job?>(null)
    var loading by mutableStateOf(false)
    var models by mutableStateOf<List<DiscoveredAiModel>>(emptyList())
    var message by mutableStateOf<String?>(null)
    var failed by mutableStateOf(false)
    var showPicker by mutableStateOf(false)
}

/** State and coroutine scope belong to the settings session, not a recyclable lazy-list item. */
@Composable
internal fun AiModelDiscoveryControl(
    state: AiModelDiscoveryState,
    scope: CoroutineScope,
    settings: AiImportSettings,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    enabled: Boolean = true,
    fetchModels: suspend () -> List<DiscoveredAiModel> = { discoverAiModels(settings) }
) = with(state) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickSheetLiquidAction(
            label = if (loading) "取消获取" else "获取可用模型",
            enabled = enabled && (loading || (settings.profile.baseUrl.isNotBlank() && settings.apiKey.isNotBlank())),
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxWidth(),
            height = SleepDownDesignTokens.CenteredDialog.ActionHeight
        ) {
            if (loading) {
                request?.cancel()
                message = "已取消获取，原有模型保留"
            } else {
                loading = true
                failed = false
                message = null
                request = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        models = fetchModels()
                        message = if (models.isEmpty()) "平台返回了空列表，可继续手动添加模型"
                            else "已获取 ${models.size} 个模型，选择需要的模型后添加"
                        showPicker = models.isNotEmpty()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        failed = true
                        message = error.message ?: "获取失败，请重试或手动添加模型"
                    } finally {
                        loading = false
                    }
                }
            }
        }
        ModelDiscoveryProgress(loading)
        Text(
            message ?: "填写接口地址与 API Key 后获取；也可使用本机已保存的 Key。不会自动替换当前模型。",
            style = MaterialTheme.typography.bodySmall,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!loading && models.isNotEmpty()) {
            TextButton(onClick = { showPicker = true }) { Text("选择已获取的模型（${models.size}）") }
        }
    }
}

@Composable
private fun ModelDiscoveryProgress(loading: Boolean) {
    val motion = rememberGlassMotionEnabled()
    val progress = remember { Animatable(0f) }
    var longWait by remember { mutableStateOf(false) }
    LaunchedEffect(loading, motion) {
        longWait = false
        progress.snapTo(0f)
        if (loading) {
            delay(3_000)
            longWait = true
            if (motion) progress.animateTo(1f, infiniteRepeatable(tween(1_500, easing = LinearEasing)))
        }
    }
    if (!loading) return
    val accent = MaterialTheme.colorScheme.primary
    val track = accent.copy(alpha = .14f)
    Canvas(Modifier.fillMaxWidth().height(3.dp).semantics { contentDescription = "正在获取模型" }) {
        drawLine(track, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = size.height)
        if (longWait && motion) {
            val center = (-.3f + progress.value * 1.6f) * size.width
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent),
                startX = center - size.width * .3f, endX = center + size.width * .3f))
        } else if (!motion) {
            drawLine(accent, Offset.Zero, Offset(size.width / 3f, 0f), strokeWidth = size.height * 2f)
        }
    }
}

@Composable
internal fun AiModelDiscoveryDialog(
    state: AiModelDiscoveryState,
    existingIds: List<String>,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onAddModels: (List<String>) -> Unit
) {
    if (state.showPicker) {
        AiDiscoveredModelsDialog(
            models = state.models,
            existingIds = existingIds,
            backdrop = backdrop,
            config = config,
            onDismiss = { state.showPicker = false },
            onAdd = { ids ->
                onAddModels(ids)
                state.showPicker = false
                state.message = "已添加 ${ids.size} 个模型，点击当前模型即可切换"
            }
        )
    }
}

/** One current value on the form; alternatives and manual entry stay inside its picker. */
@Composable
internal fun AiModelSelector(
    title: String,
    model: String,
    models: List<String>,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    enabled: Boolean = true,
    label: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf<String?>(null) }
    val motion = rememberGlassMotionEnabled()
    val arrow by animateFloatAsState(if (expanded) 180f else 0f,
        if (motion) tween(180) else snap(), label = "model-picker-arrow")
    val choices = mergeAiModelIds(listOf(model), models)
    fun select(id: String) {
        try {
            require(id.isNotBlank() && id.none(Char::isISOControl)) { "请输入有效的模型 ID" }
            onSelect(id.trim())
            expanded = false
        } catch (error: IllegalArgumentException) {
            failure = error.message ?: "此模型暂不支持，请选择其他模型"
        }
    }
    GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
        contentDescription = "$title，${label(model).ifBlank { "未选择" }}"
        if (!enabled) disabled()
    }, onClick = if (enabled) ({ query = ""; failure = null; expanded = true }) else null) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(label(model).ifBlank { "选择模型" }, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, "展开$title", Modifier.graphicsLayer { rotationZ = arrow })
        }
    }
    SleepDownPickerDialog(expanded, "选择$title", { expanded = false }, backdrop, config, scrollableContent = true) {
        DialogCapsuleField(query, { query = it; failure = null }, "搜索或输入模型 ID", config, Modifier.fillMaxWidth())
        failure?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(choices.filter { it.contains(query.trim(), true) || label(it).contains(query.trim(), true) }, key = { it }) { id ->
                GlassPill(backdrop, config, Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .heightIn(min = 48.dp).semantics { selected = id == model }, onClick = { select(id) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label(id), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (id == model) Icon(Icons.Default.Check, "当前模型", tint = LocalContentColor.current)
                    }
                }
            }
        }
        if (query.isNotBlank() && choices.none { it.equals(query.trim(), true) }) {
            QuickSheetLiquidAction("添加并使用此模型", enabled = true, backdrop = backdrop, config = config,
                modifier = Modifier.fillMaxWidth()) { select(query) }
        }
    }
}

@Composable
private fun AiDiscoveredModelsDialog(
    models: List<DiscoveredAiModel>,
    existingIds: List<String>,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val existing = remember(existingIds) { existingIds.map(String::lowercase).toSet() }
    val visible = remember(models, query) {
        val term = query.trim()
        models.filter { it.id.contains(term, true) || it.name.contains(term, true) }
    }
    val additions = models.filter { it.id in selected && it.id.lowercase() !in existing }.map { it.id }
    SleepDownFormDialog(onDismiss) {
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader("添加模型", onDismiss, backdrop, config)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = SleepDownDesignTokens.Dialog.HorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogCapsuleField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "搜索模型名称或 ID",
                    config = config,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "搜索模型" }
                )
                Text(
                    "列表由当前平台返回。对话、图片与工具调用能力以平台说明为准，添加后可测试连接。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("${visible.size} 个结果 · 已选 ${additions.size}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth()) {
                    TextButton(modifier = Modifier.weight(1f), enabled = visible.any { it.id.lowercase() !in existing }, onClick = {
                        selected = selected + visible.filter { it.id.lowercase() !in existing }.map { it.id }
                    }) { Text("全选结果") }
                    TextButton(modifier = Modifier.weight(1f), enabled = selected.isNotEmpty(),
                        onClick = { selected = emptySet() }) { Text("清空选择") }
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = SleepDownDesignTokens.Dialog.HorizontalPadding)
            ) {
                if (visible.isEmpty()) {
                    item { Text("没有匹配的模型，试试其他关键词", Modifier.padding(vertical = 16.dp)) }
                }
                items(visible, key = { it.id }) { model ->
                    val added = model.id.lowercase() in existing
                    val checked = added || model.id in selected
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(checked, enabled = !added, role = Role.Checkbox) { next ->
                                selected = if (next) selected + model.id else selected - model.id
                            }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = !added)
                        Column(Modifier.weight(1f)) {
                            Text(model.name, style = MaterialTheme.typography.bodyMedium)
                            if (model.name != model.id) {
                                Text(model.id, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (added) {
                                Text("已添加", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            QuickSheetLiquidAction(
                label = "添加所选（${additions.size}）",
                enabled = additions.isNotEmpty(),
                backdrop = backdrop,
                config = config,
                primary = true,
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = SleepDownDesignTokens.Dialog.HorizontalPadding,
                    vertical = SleepDownDesignTokens.Dialog.ContentSpacing
                ),
                height = SleepDownDesignTokens.CenteredDialog.ActionHeight
            ) { onAdd(additions) }
        }
    }
}
