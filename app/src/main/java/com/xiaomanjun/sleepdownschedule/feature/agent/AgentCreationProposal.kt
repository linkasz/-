package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

internal fun AgentPlan.isCreation() = actions.isNotEmpty() && actions.all {
    it.type in setOf(AgentValidatedActionType.CREATE_TODO, AgentValidatedActionType.ADD, AgentValidatedActionType.CREATE_SCHEDULE)
}

internal data class AgentCreationFields(val title: String, val note: String = "", val date: String = "", val time: String = "",
    val weekday: String = "", val periods: String = "", val weeks: String = "", val teacher: String = "", val location: String = "")

internal fun creationFields(action: AgentValidatedAction, facts: DayAgentFacts): AgentCreationFields {
    action.todo?.let { todo ->
        val due = todo.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of(facts.timeZoneId)) }
        return AgentCreationFields(todo.title, todo.description, due?.toLocalDate()?.toString().orEmpty(),
            if (todo.allDay) "" else due?.toLocalTime()?.toString().orEmpty())
    }
    action.edited?.let { course -> return AgentCreationFields(course.name, course.note.orEmpty(),
        weekday = course.weekday.toString(), periods = course.periods.joinToString(","), weeks = course.weeks.joinToString(","),
        teacher = course.teacher.orEmpty(), location = course.location.orEmpty()) }
    return AgentCreationFields(action.scheduleName.orEmpty())
}

internal fun editedCreationPlan(plan: AgentPlan, fields: List<AgentCreationFields>, facts: DayAgentFacts): ParsedAgentActions {
    if (fields.size != plan.actions.size) return ParsedAgentActions("", emptyList(), listOf("草稿已变化，请重新打开。"))
    fun indices(text: String) = text.split(',', '，').map { it.trim().toIntOrNull() ?: -1 }
    val drafts = plan.actions.zip(fields).map { (action, field) -> when (action.type) {
        AgentValidatedActionType.CREATE_TODO -> AgentActionDraft(AgentActionType.CREATE_TODO,
            todo = AgentTodoDraft(field.title, field.note, field.date.trim().ifBlank { null }, field.time.trim().ifBlank { null },
                action.todo!!.priority, action.todo.courseId, action.todo.reminderMode != "NONE"))
        AgentValidatedActionType.ADD -> AgentActionDraft(AgentActionType.ADD_COURSE, scope = action.scope,
            sourceWeeks = action.sourceWeeks.takeIf { it.isNotEmpty() }, course = action.edited!!.let { course ->
                AgentCoursePatch(field.title, field.teacher.ifBlank { null }, field.location.ifBlank { null },
                    field.weekday.trim().toIntOrNull() ?: -1, indices(field.periods), indices(field.weeks), course.weekParity.name,
                    field.note.ifBlank { null }, course.customStartTime, course.customEndTime,
                    course.customColorArgb?.let { "#" + (it and 0xFFFFFFFFL).toString(16).padStart(8, '0') })
            })
        else -> AgentActionDraft(AgentActionType.CREATE_SCHEDULE, name = field.title)
    } }
    return parseAgentActions("<agent_actions>${Json.encodeToString(drafts)}</agent_actions>", facts)
}

/** Shared draft UI: editing returns a checked proposal; no repository call happens here. */
@Composable
internal fun AgentCreationProposal(plan: AgentPlan, facts: DayAgentFacts, backdrop: Backdrop?, config: ScheduleConfigEntity,
    applied: Boolean, executing: Boolean, cancelled: Boolean, feedback: String?,
    onEdit: (AgentPlan) -> Unit, onCancel: () -> Unit, onConfirm: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val palette = rememberAssistantGlassPalette(config)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plan.actions.forEach { action ->
            Text(action.summary, color = palette.foreground, style = MaterialTheme.typography.bodyMedium)
            action.todo?.let { todo ->
                Text(if (todo.reminderMode == "NONE") "未分组 · 无提醒" else "未分组 · 到时提醒",
                    color = palette.foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium)
            }
            action.edited?.let { course -> Text("周${course.weekday} · 第${course.periods.joinToString("、")}节 · 第${course.weeks.joinToString("、")}周",
                color = palette.foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium) }
        }
        feedback?.let { Text(it, color = palette.foreground, style = MaterialTheme.typography.bodySmall) }
        if (applied || cancelled) Text(if (applied) "已创建" else "已取消，未创建", color = palette.accent)
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickSheetLiquidAction("编辑", !executing, backdrop, config, modifier = Modifier.weight(1f), height = 48.dp) { editing = true }
                QuickSheetLiquidAction("取消", !executing, backdrop, config, modifier = Modifier.weight(1f), height = 48.dp, onClick = onCancel)
            }
            QuickSheetLiquidAction(if (executing) "正在保存…" else "确认创建", !executing,
                backdrop, config, primary = true, modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = onConfirm)
        }
    }
    if (editing) Dialog({ editing = false }, DialogProperties(usePlatformDefaultWidth = false)) {
        var values by remember(plan) { mutableStateOf(plan.actions.map { creationFields(it, facts) }) }
        var issue by remember { mutableStateOf<String?>(null) }
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader("编辑待确认内容", { editing = false }, backdrop, config)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                values.forEachIndexed { index, field ->
                    fun update(next: AgentCreationFields) { values = values.toMutableList().also { it[index] = next }; issue = null }
                    DialogCapsuleField(field.title, { update(field.copy(title = it)) }, "名称", config)
                    when (plan.actions[index].type) {
                        AgentValidatedActionType.CREATE_TODO -> {
                            DialogCapsuleField(field.date, { update(field.copy(date = it)) }, "日期 YYYY-MM-DD（可留空）", config)
                            DialogCapsuleField(field.time, { update(field.copy(time = it)) }, "时间 HH:mm（留空为全天）", config)
                            DialogCapsuleField(field.note, { update(field.copy(note = it)) }, "备注", config)
                        }
                        AgentValidatedActionType.ADD -> {
                            DialogCapsuleField(field.weekday, { update(field.copy(weekday = it)) }, "星期（1–7）", config)
                            DialogCapsuleField(field.periods, { update(field.copy(periods = it)) }, "节次（逗号分隔）", config)
                            DialogCapsuleField(field.weeks, { update(field.copy(weeks = it)) }, "周次（逗号分隔）", config)
                            DialogCapsuleField(field.teacher, { update(field.copy(teacher = it)) }, "教师", config)
                            DialogCapsuleField(field.location, { update(field.copy(location = it)) }, "地点", config)
                            DialogCapsuleField(field.note, { update(field.copy(note = it)) }, "备注", config)
                        }
                        else -> Unit
                    }
                }
                issue?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            QuickSheetLiquidAction("更新草稿", true, backdrop, config, primary = true, modifier = Modifier.fillMaxWidth().padding(16.dp), height = 50.dp) {
                val checked = editedCreationPlan(plan, values, facts)
                if (checked.validationIssues.isNotEmpty()) issue = checked.validationIssues.joinToString("\n")
                else { onEdit(AgentPlan(checked.actions)); editing = false }
            }
        }
    }
}
