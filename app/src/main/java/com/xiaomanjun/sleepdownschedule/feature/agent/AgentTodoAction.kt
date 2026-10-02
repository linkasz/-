package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import androidx.room.withTransaction
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.feature.todo.*
import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class AgentTodoDraft(
    val title: String,
    val description: String = "",
    val date: String? = null,
    val time: String? = null,
    val priority: Int = 1,
    val courseId: Long? = null,
    val remind: Boolean = false
)

internal fun validateAgentTodo(draft: AgentTodoDraft?, facts: DayAgentFacts): TodoDraft? {
    draft ?: return null
    val title = draft.title.trim().takeIf { it.isNotEmpty() && it.length <= 500 } ?: return null
    if (draft.description.length > 10_000 || draft.priority !in 0..3) return null
    if (draft.courseId != null && facts.semesterCourses.none { it.id == draft.courseId && it.scheduleId == facts.scheduleId }) return null
    val date = draft.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    val time = draft.time?.let { if (!Regex("\\d{2}:\\d{2}").matches(it)) return null
        runCatching { LocalTime.parse(it) }.getOrNull() ?: return null }
    if (time != null && date == null || draft.remind && time == null) return null
    val zone = runCatching { ZoneId.of(facts.timeZoneId) }.getOrNull() ?: return null
    val local = date?.atTime(time ?: LocalTime.MIDNIGHT)
    // A nonexistent or ambiguous DST wall time needs clarification, never a silent shift.
    val offsets = local?.let { zone.rules.getValidOffsets(it) }
    if (offsets != null && offsets.size != 1) return null
    return TodoDraft(title = title, description = draft.description.trim(),
        dueAt = local?.toInstant(offsets!!.single())?.toEpochMilli(), allDay = date != null && time == null,
        priority = draft.priority, courseId = draft.courseId,
        reminderMode = if (draft.remind) "BEFORE" else "NONE", reminderOffsetMinutes = 0)
}

internal suspend fun executeConfirmedAgentTodos(context: Context, plan: AgentPlan): AgentPlanExecutionResult {
    val db = (context.applicationContext as CourseScheduleApp).database
    val repository = TodoRepository(db.todoDao())
    return try {
        require(plan.actions.isNotEmpty() && plan.actions.all { it.type == AgentValidatedActionType.CREATE_TODO }) { "待办计划不能混合其他操作" }
        val ids = db.withTransaction {
            plan.actions.map { action ->
                val draft = requireNotNull(action.todo) { "待办信息不完整" }
                require(draft.id == 0L && draft.parentId == null && draft.groupId == null) { "此操作只能新建未分组待办" }
                draft.courseId?.let { id ->
                    require(db.courseDao().getAllCourses().any { it.id == id && it.scheduleId == action.sourceScheduleId }) { "关联课程已变化，请重新确认" }
                }
                val id = repository.save(draft)
                val stored = requireNotNull(repository.getById(id))
                check(stored.title == draft.title.trim() && stored.dueAt == draft.dueAt && stored.reminderMode == draft.reminderMode) { "待办保存校验失败" }
                id
            }
        }
        val followUp = mutableListOf<String>()
        try { NotificationScheduler.requestReschedule(context); TodoTasksWidgetProvider.refreshAll(context) }
        catch (error: Exception) { followUp += "提醒或组件刷新尚未完成，请重新进入应用检查" }
        val sync = TodoCalendarSync(context, repository)
        if (sync.autoSyncEnabled()) {
            try { if (sync.syncAll().issues.isNotEmpty()) followUp += "系统日历关联需在日历页确认" }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { followUp += "本地待办已保存，系统日历同步尚未完成" }
        }
        AgentPlanExecutionResult(true, null, true, (listOf("已创建 ${ids.size} 项待办") + followUp).joinToString("；"))
    } catch (error: CancellationException) { throw error }
    catch (error: Exception) { AgentPlanExecutionResult(false, null, false, error.message ?: "创建待办失败") }
}
