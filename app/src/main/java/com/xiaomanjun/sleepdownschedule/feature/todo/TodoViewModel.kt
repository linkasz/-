package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodoUiState(
    val items: List<TodoItemEntity> = emptyList(),
    val groups: List<TodoGroupEntity> = emptyList(),
    val deletedItems: List<TodoItemEntity> = emptyList(),
    val loading: Boolean = true,
    val loadError: String? = null
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TodoRepository,
    private val aiExtractor: TodoAiExtractor,
    private val calendarSync: TodoCalendarSync
) : ViewModel() {
    private val loadRevision = MutableStateFlow(0)
    // A failed Room read is distinct from an empty list and can be subscribed again explicitly.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = loadRevision.flatMapLatest {
        combine(repository.items, repository.groups, repository.deletedItems) { items, groups, deleted ->
            TodoUiState(items, groups, deleted, loading = false)
        }.onStart { emit(TodoUiState()) }.catch { error ->
            if (error is CancellationException) throw error
            emit(TodoUiState(loading = false, loadError = "读取待办失败，请重试"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())
    fun retryLoad() { loadRevision.value++ }
    private val mutableAiState = kotlinx.coroutines.flow.MutableStateFlow(TodoAiState())
    val aiState = mutableAiState
    private val mutableCalendarMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val calendarMessage = mutableCalendarMessage
    private val mutableCalendarReview = kotlinx.coroutines.flow.MutableStateFlow<List<TodoCalendarSyncIssue>>(emptyList())
    val calendarReview = mutableCalendarReview
    private val mutableOperationError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val operationError = mutableOperationError
    private val mutableSaving = MutableStateFlow(false)
    val saving = mutableSaving.asStateFlow()
    val autoCalendarSync = kotlinx.coroutines.flow.MutableStateFlow(calendarSync.autoSyncEnabled())

    init {
        TodoTrashWorker.schedule(context)
        viewModelScope.launch {
            runCatching { repository.ensureDefaultGroup() }
                .onFailure { mutableOperationError.value = it.message ?: "加载默认分组失败" }
        }
    }

    fun save(draft: TodoDraft, subtaskTitles: List<String> = emptyList(), onSaved: (Long) -> Unit = {}) {
        // Claim synchronously, before launch/recomposition can allow another confirmation.
        if (!mutableSaving.compareAndSet(false, true)) return
        val applicationScope = (context.applicationContext as com.xiaomanjun.sleepdownschedule.CourseScheduleApp).applicationScope
        applicationScope.launch {
            try {
                val existing: TodoItemEntity?
                val id: Long
                try {
                    existing = if (draft.id > 0L) repository.getById(draft.id) else null
                    id = repository.saveWithSubtasks(draft, subtaskTitles)
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    mutableOperationError.value = error.message ?: "保存待办失败"
                    return@launch
                }
                // Local persistence is complete. Later failures must never invite another save.
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onSaved(id) }
                // Provider latency must not block confirming a different draft.
                applicationScope.launch { refreshAfterSave(existing, draft, id) }
            } finally { mutableSaving.value = false }
        }
    }

    private suspend fun refreshAfterSave(existing: TodoItemEntity?, draft: TodoDraft, id: Long) {
        suspend fun followUp(message: String, action: suspend () -> Unit) {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { mutableCalendarMessage.value = "待办已保存；$message" }
        }
        followUp("旧提醒清理尚未完成") {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(id.hashCode())
        }
        if (existing != null && (existing.calendarEventId != null || existing.calendarSyncToken != null) &&
            (draft.dueAt == null || draft.parentId != null)) {
            followUp("旧日历事件清理尚未完成") { calendarSync.removeIfNoLongerScheduled(id) }
        }
        followUp("提醒更新尚未完成") { NotificationScheduler.requestReschedule(context) }
        followUp("组件刷新尚未完成") { TodoTasksWidgetProvider.refreshAll(context) }
        followUp("系统日历同步尚未完成") {
            if (calendarSync.autoSyncEnabled() && calendarSync.hasCalendarPermission()) {
                val report = calendarSync.syncAll()
                mutableCalendarReview.value = report.issues
                if (report.issues.isNotEmpty()) mutableCalendarMessage.value = "待办已保存；系统日历关联需要确认"
            }
        }
    }

    fun toggle(item: TodoItemEntity, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.toggle(item)
                androidx.core.app.NotificationManagerCompat.from(context).cancel(item.id.hashCode())
                // Commit and acknowledge the local action before waiting on the external
                // calendar provider. Provider latency must not stall the completion animation
                // or make the checkbox feel unresponsive.
                if (!item.isCompleted) onCompleted()
                NotificationScheduler.requestReschedule(context)
                TodoTasksWidgetProvider.refreshAll(context)
                if (!item.isCompleted) runCatching { calendarSync.remove(item) }
                    .onFailure { mutableCalendarMessage.value = it.message ?: "清理旧日历事件失败" }
                if (calendarSync.autoSyncEnabled()) autoSyncAfterChange()
            }.onFailure { mutableOperationError.value = it.message ?: "更新待办失败" }
        }
    }

    fun togglePin(item: TodoItemEntity) = viewModelScope.launch {
        runCatching { repository.pin(item) }
            .onFailure { mutableOperationError.value = it.message ?: "更新置顶状态失败" }
    }
    fun delete(item: TodoItemEntity) = viewModelScope.launch {
        runCatching {
            calendarSync.archive(item)
        }.onFailure { mutableOperationError.value = it.message ?: "删除待办失败" }
        // Archiving is committed before external calendar cleanup. Refresh local consumers
        // even if that cleanup fails; a removed task must not keep an ongoing reminder.
        if (repository.getById(item.id)?.deletedAt != null) {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(item.id.hashCode())
            NotificationScheduler.requestReschedule(context)
            TodoTasksWidgetProvider.refreshAll(context)
        }
    }
    fun restore(item: TodoItemEntity) = viewModelScope.launch {
        runCatching {
            calendarSync.restore(item)
            NotificationScheduler.requestReschedule(context)
            TodoTasksWidgetProvider.refreshAll(context)
            if (calendarSync.autoSyncEnabled()) autoSyncAfterChange()
        }.onFailure { mutableOperationError.value = it.message ?: "恢复失败" }
    }
    fun purge(item: TodoItemEntity) = viewModelScope.launch {
        runCatching { calendarSync.purge(item) }
            .onFailure { mutableOperationError.value = it.message ?: "永久删除失败" }
    }
    fun clearTrash() = viewModelScope.launch {
        // Visit each archived row: deleting a parent may detach children archived earlier
        // under another batch, and those children must also be included in an explicit clear.
        runCatching { state.value.deletedItems.toList().forEach { calendarSync.purge(it) } }
            .onFailure { mutableOperationError.value = it.message ?: "清空失败" }
    }
    fun addGroup(name: String) = viewModelScope.launch {
        runCatching { repository.saveGroup(name) }
            .onFailure { mutableOperationError.value = it.message ?: "创建分组失败" }
    }
    fun deleteGroup(group: TodoGroupEntity) = viewModelScope.launch {
        runCatching { repository.deleteGroup(group) }
            .onFailure { mutableOperationError.value = it.message ?: "删除分组失败" }
    }

    fun extractFromText(text: String) = extract(text, emptyList())
    fun extractFromImages(uris: List<Uri>, caption: String = "", onComplete: () -> Unit = {}) =
        extract(caption, uris, onComplete)

    private fun extract(text: String, images: List<Uri>, onComplete: () -> Unit = {}) = viewModelScope.launch {
        mutableAiState.value = TodoAiState(isLoading = true)
        runCatching { aiExtractor.extract(text, images) }
            .onSuccess { mutableAiState.value = TodoAiState(result = it) }
            .onFailure { mutableAiState.value = TodoAiState(error = it.message ?: "AI 提取失败") }
        onComplete()
    }

    fun clearAiState() { mutableAiState.value = TodoAiState() }

    fun saveAiConfig(config: TodoAiConfig) {
        aiExtractor.saveConfig(config)
    }

    fun readAiConfig(): TodoAiConfig = aiExtractor.readConfig()

    fun syncCalendar() = viewModelScope.launch {
        runCatching { calendarSync.syncAll() }
            .onSuccess { report ->
                mutableCalendarReview.value = report.issues
                mutableCalendarMessage.value = if (report.issues.isEmpty()) {
                    "已同步 ${report.syncedCount} 项待办到系统日历"
                } else {
                    "已同步 ${report.syncedCount} 项；${report.issues.size} 项需要确认日历关联"
                }
            }
            .onFailure { mutableCalendarMessage.value = it.message ?: "日历同步失败" }
    }

    fun resolveCalendarReview(todoId: Long, createNew: Boolean, keepLocalEvent: Boolean = false) {
        viewModelScope.launch {
            runCatching { calendarSync.resolveReview(todoId, createNew, keepLocalEvent) }
                .onSuccess {
                    mutableCalendarReview.value = mutableCalendarReview.value.filterNot { it.todoId == todoId }
                    if (createNew) syncCalendar()
                }
                .onFailure { mutableOperationError.value = it.message ?: "更新日历关联失败" }
        }
    }

    fun dismissCalendarReview(todoId: Long) {
        mutableCalendarReview.value = mutableCalendarReview.value.filterNot { it.todoId == todoId }
    }

    fun clearCalendarMessage() { mutableCalendarMessage.value = null }
    fun clearOperationError() { mutableOperationError.value = null }

    fun setAutoCalendarSync(enabled: Boolean) {
        calendarSync.setAutoSync(enabled)
        autoCalendarSync.value = enabled
        if (enabled && calendarSync.hasCalendarPermission()) syncCalendar()
    }

    fun autoSyncAfterChange() {
        if (calendarSync.autoSyncEnabled() && calendarSync.hasCalendarPermission()) syncCalendar()
    }
}

data class TodoAiState(
    val isLoading: Boolean = false,
    val result: TodoExtraction? = null,
    val error: String? = null
)
