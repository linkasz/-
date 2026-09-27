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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodoUiState(
    val items: List<TodoItemEntity> = emptyList(),
    val groups: List<TodoGroupEntity> = emptyList()
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TodoRepository,
    private val aiExtractor: TodoAiExtractor,
    private val calendarSync: TodoCalendarSync
) : ViewModel() {
    val state = combine(repository.items, repository.groups) { items, groups ->
        TodoUiState(items, groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())
    private val mutableAiState = kotlinx.coroutines.flow.MutableStateFlow(TodoAiState())
    val aiState = mutableAiState
    private val mutableCalendarMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val calendarMessage = mutableCalendarMessage
    val autoCalendarSync = kotlinx.coroutines.flow.MutableStateFlow(calendarSync.autoSyncEnabled())

    init {
        viewModelScope.launch { repository.ensureDefaultGroup() }
    }

    fun save(draft: TodoDraft, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val existing = if (draft.id > 0L) repository.getById(draft.id) else null
            runCatching { repository.save(draft) }.onSuccess { id ->
                if (
                    existing != null && existing.calendarEventId != null &&
                    (draft.dueAt == null || draft.parentId != null)
                ) {
                    calendarSync.remove(existing)
                }
                NotificationScheduler.requestReschedule(context)
                TodoTasksWidgetProvider.refreshAll(context)
                if (calendarSync.autoSyncEnabled()) autoSyncAfterChange()
                onSaved(id)
            }
        }
    }

    fun toggle(item: TodoItemEntity, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            if (!item.isCompleted) calendarSync.remove(item)
            repository.toggle(item)
            NotificationScheduler.requestReschedule(context)
            TodoTasksWidgetProvider.refreshAll(context)
            if (calendarSync.autoSyncEnabled()) autoSyncAfterChange()
            if (!item.isCompleted) onCompleted()
        }
    }

    fun togglePin(item: TodoItemEntity) = viewModelScope.launch { repository.pin(item) }
    fun delete(item: TodoItemEntity) = viewModelScope.launch {
        calendarSync.remove(item)
        repository.delete(item)
        NotificationScheduler.requestReschedule(context)
        TodoTasksWidgetProvider.refreshAll(context)
    }
    fun addGroup(name: String) = viewModelScope.launch { runCatching { repository.saveGroup(name) } }
    fun deleteGroup(group: TodoGroupEntity) = viewModelScope.launch { repository.deleteGroup(group) }

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
            .onSuccess { mutableCalendarMessage.value = "已同步 $it 项待办到系统日历" }
            .onFailure { mutableCalendarMessage.value = it.message ?: "日历同步失败" }
    }

    fun clearCalendarMessage() { mutableCalendarMessage.value = null }

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
