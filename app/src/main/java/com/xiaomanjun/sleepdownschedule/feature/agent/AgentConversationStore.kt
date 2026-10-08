package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

@Serializable
internal data class AgentConversationMeta(val key: String, val title: String, val createdAt: Long)

/** Legacy ISO-date keys remain valid. New sessions use a date-prefixed opaque key in the existing
 * message partition column; daily facts/caches continue to use actual ISO dates exclusively. */
internal fun validConversationKey(key: String): Boolean = runCatching {
    LocalDate.parse(key.take(10))
    key.length == 10 || key.length == 47 && key[10] == '#' && UUID.fromString(key.substring(11)).toString() == key.substring(11)
}.getOrDefault(false)

internal object AgentConversationStore {
    val mutationLock = kotlinx.coroutines.sync.Mutex()
    val revision = MutableStateFlow(0L)
    private val json = Json { ignoreUnknownKeys = true }
    private fun prefs(context: Context) = context.getSharedPreferences("agent_conversation_metadata", 0)
    fun all(context: Context, scheduleId: Int): List<AgentConversationMeta> = decode(raw(context, scheduleId))
    fun raw(context: Context, scheduleId: Int): String = prefs(context).getString("threads_$scheduleId", "[]").orEmpty()
    fun decode(raw: String): List<AgentConversationMeta> = runCatching {
        json.decodeFromString<List<AgentConversationMeta>>(raw).filter { validConversationKey(it.key) }
            .distinctBy { it.key }.map { it.copy(title = it.title.take(80)) }
    }.getOrDefault(emptyList())
    fun selected(context: Context, scheduleId: Int, date: LocalDate): String =
        prefs(context).getString("active_$scheduleId", null)?.takeIf(::validConversationKey) ?: date.toString()
    @Synchronized
    fun select(context: Context, scheduleId: Int, key: String) {
        require(validConversationKey(key)) { "对话标识无效" }
        prefs(context).edit { putString("active_$scheduleId", key) }
        revision.value++
    }
    @Synchronized
    fun create(context: Context, scheduleId: Int): AgentConversationMeta {
        val item = AgentConversationMeta("${LocalDate.now()}#${UUID.randomUUID()}", "新对话", System.currentTimeMillis())
        write(context, scheduleId, all(context, scheduleId) + item)
        select(context, scheduleId, item.key)
        return item
    }
    @Synchronized
    fun rename(context: Context, scheduleId: Int, key: String, title: String) {
        require(validConversationKey(key))
        val trimmed = title.trim().take(80)
        require(trimmed.isNotBlank()) { "请输入对话名称" }
        val items = all(context, scheduleId)
        val old = items.firstOrNull { it.key == key }
        write(context, scheduleId, items.filterNot { it.key == key } +
            (old ?: AgentConversationMeta(key, trimmed, System.currentTimeMillis())).copy(title = trimmed))
    }
    @Synchronized
    fun titleFromFirstMessage(context: Context, scheduleId: Int, key: String, question: String) {
        val title = all(context, scheduleId).firstOrNull { it.key == key }?.title
        if (title == null || title == "新对话") rename(context, scheduleId, key, question.take(40).ifBlank { "图片对话" })
    }
    @Synchronized
    fun forget(context: Context, scheduleId: Int, key: String) {
        write(context, scheduleId, all(context, scheduleId).filterNot { it.key == key })
        if (prefs(context).getString("active_$scheduleId", null) == key) {
            // Never reopen the just-deleted legacy day partition when the next turn arrives.
            create(context, scheduleId)
        }
    }
    fun restore(context: Context, scheduleId: Int, raw: String) {
        prefs(context).edit { remove("active_$scheduleId") }
        write(context, scheduleId, decode(raw))
    }
    private fun write(context: Context, scheduleId: Int, items: List<AgentConversationMeta>) {
        prefs(context).edit { putString("threads_$scheduleId", json.encodeToString(items)) }
        revision.value++
    }
}

internal data class AgentConversationOpenRequest(val scheduleId: Int, val key: String, val token: Long = System.nanoTime())
internal object AgentConversationNavigator {
    val pending = MutableStateFlow<AgentConversationOpenRequest?>(null)
    fun open(scheduleId: Int, key: String) { pending.value = AgentConversationOpenRequest(scheduleId, key) }
}
