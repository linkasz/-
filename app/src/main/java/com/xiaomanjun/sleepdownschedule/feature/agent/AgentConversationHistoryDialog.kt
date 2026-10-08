package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.withTransaction
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.MainActivity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.model.AgentMessageEntity
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

internal data class ConversationListItem(val scheduleId: Int, val key: String, val title: String,
    val messages: List<AgentMessageEntity>, val updatedAt: Long)

internal fun conversationItems(messages: List<AgentMessageEntity>, metadata: Map<Int, List<AgentConversationMeta>>): List<ConversationListItem> {
    val grouped = messages.groupBy { it.scheduleId to it.sessionDate }
    val keys = grouped.keys + metadata.flatMap { (scheduleId, list) -> list.map { scheduleId to it.key } }
    return keys.distinct().map { (scheduleId, key) ->
        val turns = grouped[scheduleId to key].orEmpty().sortedWith(compareBy<AgentMessageEntity> { it.createdAt }.thenBy { it.id })
        val meta = metadata[scheduleId]?.firstOrNull { it.key == key }
        val title = meta?.title ?: turns.firstOrNull { it.role == "user" }?.let { parseAgentMessageContent(it.content).text.take(40) }?.ifBlank { null } ?: "对话"
        ConversationListItem(scheduleId, key, title, turns, turns.lastOrNull()?.createdAt ?: meta?.createdAt ?: 0L)
    }.sortedByDescending { it.updatedAt }
}

@Composable
internal fun AgentConversationHistoryDialog(backdrop: Backdrop?, config: ScheduleConfigEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { (context.applicationContext as CourseScheduleApp).database }
    val dao = remember(database) { database.agentDao() }
    val profiles by remember(database) { database.scheduleProfileDao().observeProfiles() }.collectAsStateWithLifecycle(emptyList())
    val messages by remember(dao) { dao.observeConversationHistory() }.collectAsStateWithLifecycle<List<AgentMessageEntity>?>(null)
    val revision by AgentConversationStore.revision.collectAsStateWithLifecycle()
    val receipts by AgentCreationDispatch.receipts.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var editMessage by remember { mutableStateOf<AgentMessageEntity?>(null) }
    var draft by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Long?>(null) } // -1 deletes the selected conversation
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val threads by produceState(emptyList<ConversationListItem>(), messages, profiles, revision) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            conversationItems(messages.orEmpty(), profiles.associate { it.id to AgentConversationStore.all(context, it.id) })
        }
    }
    val current = threads.firstOrNull { it.scheduleId to it.key == selected }
    val filtered by produceState(threads, threads, search) {
        if (search.isNotEmpty()) kotlinx.coroutines.delay(150)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            threads.filter { it.title.contains(search, true) || it.messages.any { m ->
                val text = parseAgentMessageContent(m.content).text
                (if (m.role == "assistant") agentVisibleAnswer(text) else text).contains(search, true)
            } }
        }
    }
    fun change(block: suspend () -> Unit) {
        if (busy) return
        if (receipts.any { it.value == null } || !AgentConversationStore.mutationLock.tryLock()) {
            feedback = "请等当前回复或保存完成后再修改记录。"; return
        }
        busy = true
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try { block(); feedback = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { feedback = error.message ?: "操作未完成，请重试" }
            finally { busy = false; AgentConversationStore.mutationLock.unlock() }
        }
    }
    fun open(thread: ConversationListItem) = change {
        check(profiles.any { it.id == thread.scheduleId }) { "这份课表已不存在，仍可查看或删除历史。" }
        AgentConversationStore.select(context, thread.scheduleId, thread.key)
        DayAgentPreferences.setEnabled(context, true)
        DayAgentPreferences.setWeekAssistantEnabled(context, true)
        AgentConversationNavigator.open(thread.scheduleId, thread.key)
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        onDismiss()
    }
    SleepDownFormDialog(onDismiss) {
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader(if (current == null) "对话记录" else current.title,
                { if (selected != null) selected = null else onDismiss() }, backdrop, config)
            if (current == null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    DialogCapsuleField(search, { search = it }, "搜索对话或消息", config, Modifier.weight(1f))
                    IconButton(enabled = !busy, onClick = { change {
                        val active = profiles.firstOrNull { it.isActive } ?: error("请先创建课表")
                        val item = AgentConversationStore.create(context, active.id)
                        selected = active.id to item.key
                    } }) { Icon(Icons.Default.Add, "新建对话") }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (messages == null) item { Text("正在读取对话…") }
                    else if (filtered.isEmpty()) item { Text(if (search.isEmpty()) "还没有对话，点击 + 开始。" else "没有找到匹配记录。") }
                    items(filtered, key = { "${it.scheduleId}:${it.key}" }) { thread ->
                        val previousDay = LocalDate.now().minusDays(1).toString()
                        val dateLabel = when (thread.key.take(10)) { LocalDate.now().toString() -> "今天"; previousDay -> "昨天"; else -> thread.key.take(10) }
                        GlassSurface(backdrop, config, Modifier.fillMaxWidth(), onClick = { selected = thread.scheduleId to thread.key }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(thread.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text("$dateLabel · ${profiles.firstOrNull { it.id == thread.scheduleId }?.name ?: "历史课表"} · ${thread.messages.size} 条消息",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    thread.messages.lastOrNull()?.let { last ->
                                        val text = parseAgentMessageContent(last.content).text
                                        Text(if (last.role == "assistant") agentVisibleAnswer(text) else text,
                                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${current.messages.size} 条消息", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(enabled = !busy, onClick = { draft = current.title; renaming = true }) { Text("重命名") }
                    IconButton(enabled = !busy, onClick = { deletion = -1 }) { Icon(Icons.Default.DeleteOutline, "删除对话", tint = MaterialTheme.colorScheme.error) }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (current.messages.isEmpty()) item { Text("这是一段新对话，点击下方按钮开始。") }
                    items(current.messages, key = { it.id }) { message ->
                        GlassSurface(backdrop, config, Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (message.role == "user") "你" else "时序清单的智能助理", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                    IconButton(enabled = !busy && message.status != "PENDING", onClick = {
                                        editMessage = message
                                        val body = parseAgentMessageContent(message.content).text
                                        draft = if (message.role == "assistant") agentVisibleAnswer(body) else body
                                    }) { Icon(Icons.Default.Edit, "编辑消息", Modifier.size(18.dp)) }
                                    IconButton(enabled = !busy && message.status != "PENDING", onClick = { deletion = message.id }) {
                                        Icon(Icons.Default.DeleteOutline, "删除消息", Modifier.size(18.dp))
                                    }
                                }
                                val body = parseAgentMessageContent(message.content)
                                Text(if (message.role == "assistant") agentVisibleAnswer(body.text) else body.text)
                                if (body.attachmentFileName != null) Text("包含图片", style = MaterialTheme.typography.labelSmall)
                                if (message.status != "READY") Text(if (message.status == "PENDING") "正在回复…" else "这一轮未完成", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                QuickSheetLiquidAction("打开并继续对话", enabled = !busy, backdrop = backdrop, config = config, primary = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = { open(current) })
            }
            feedback?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (editMessage != null || renaming) SleepDownFormDialog({ editMessage = null; renaming = false }) {
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader(if (renaming) "重命名对话" else "编辑消息", { editMessage = null; renaming = false }, backdrop, config)
            Column(Modifier.weight(1f).padding(16.dp)) {
                DialogCapsuleField(draft, { draft = it.take(16000) }, "输入内容", config, Modifier.fillMaxWidth(), minLines = if (renaming) 1 else 5)
                if (!renaming) Text("只修改历史正文，不自动重发，不修改已创建的待办或课程；原操作草稿将移除。", style = MaterialTheme.typography.bodySmall)
            }
            QuickSheetLiquidAction("保存", enabled = !busy && draft.isNotBlank(), backdrop = backdrop, config = config, primary = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp), onClick = { change {
                require(draft.isNotBlank()) { "内容不能为空" }
                if (renaming && current != null) AgentConversationStore.rename(context, current.scheduleId, current.key, draft)
                else editMessage?.let { message ->
                    val plain = sanitizeAgentToolOutput(draft)
                    require(!containsAgentContextEcho(plain) && !plain.contains("<agent_actions", true)) { "请只填写聊天正文" }
                    val attachment = parseAgentMessageContent(message.content).attachmentFileName
                    check(dao.editConversationMessage(message.id, agentMessageContent(plain, attachment)) == 1) { "消息已变化，请重新打开" }
                }
                editMessage = null; renaming = false
            } })
            feedback?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (deletion != null) AlertDialog(onDismissRequest = { deletion = null }, title = { Text("删除${if (deletion == -1L) "这段对话" else "这条消息"}？") },
        text = { Text("仅删除聊天记录，不影响已创建的课程或待办。删除后无法撤销。") },
        dismissButton = { TextButton({ deletion = null }) { Text("取消") } },
        confirmButton = { TextButton(enabled = !busy, onClick = { change {
            val id = deletion
            database.withTransaction {
                if (id == -1L && current != null) {
                    dao.deleteConversation(current.scheduleId, current.key)
                } else if (id != null) check(dao.deleteConversationMessage(id) == 1) { "消息已变化，请重新打开" }
            }
            if (id == -1L && current != null) { AgentConversationStore.forget(context, current.scheduleId, current.key); selected = null }
            deletion = null
            DayAgentRepository(context).cleanup(LocalDate.now())
        } }) { Text("删除", color = MaterialTheme.colorScheme.error) } })
}
