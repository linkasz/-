package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoItemEntity
import java.time.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AgentDateTodoTest {
    private val date = LocalDate.of(2026, 10, 1)
    private fun facts(todos: List<TodoItemEntity> = emptyList()) = DayAgentFacts(date, date.atTime(7, 0),
        emptyList(), emptyList(), emptyList(), null, "snapshot", timeZoneId = "Asia/Shanghai",
        scheduleConfig = ScheduleConfigEntity(totalWeeks = 20, currentWeek = 1, notificationLeadMinutes = 10), todos = todos)
    private fun query(facts: DayAgentFacts, extra: Map<String, String> = emptyMap()) = Json.parseToJsonElement(
        agentDateAgendaResult(mapOf("startDate" to "2026-10-01", "endDate" to "2026-10-31", "kind" to "TODOS") + extra, facts)).jsonObject
    private fun due(day: LocalDate, time: LocalTime = LocalTime.of(8, 0)) = day.atTime(time).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

    @Test fun dateQueriesRespectLocalTimezoneAndHideArchivedAndCompleted() {
        val rows = listOf(TodoItemEntity(1, "月初", dueAt = due(date, LocalTime.MIDNIGHT)),
            TodoItemEntity(2, "月末", dueAt = due(date.withDayOfMonth(31), LocalTime.of(23, 59))),
            TodoItemEntity(3, "下月", dueAt = due(date.plusMonths(1))),
            TodoItemEntity(4, "已完成", dueAt = due(date), isCompleted = true),
            TodoItemEntity(5, "已归档", dueAt = due(date), deletedAt = 1), TodoItemEntity(6, "未定日期"))
        assertEquals(2, query(facts(rows))["todoCount"]!!.jsonPrimitive.int)
        assertEquals(4, query(facts(rows), mapOf("includeCompleted" to "true", "includeUndated" to "true"))["todoCount"]!!.jsonPrimitive.int)
    }
    @Test fun sharedContextToolsIncludeActualEndTimesAndCalculateActivityOverlap() {
        val row = TodoItemEntity(10, "会议", dueAt = due(date, LocalTime.of(10, 0)), endAt = due(date, LocalTime.of(11, 0)))
        val f = facts(listOf(row, TodoItemEntity(11, "截止任务", dueAt = due(date, LocalTime.of(12, 0))),
            TodoItemEntity(12, "已删除", dueAt = due(date), deletedAt = 1)))
        val result = Json.parseToJsonElement(agentContextAgenda(mapOf("date" to date.toString(),
            "activityStart" to "10:30", "activityEnd" to "11:30"), f, "TODOS")).jsonObject
        assertEquals("11:00", result["items"]!!.jsonArray.first().jsonObject["end"]!!.jsonPrimitive.content)
        assertEquals(1, result["conflicts"]!!.jsonArray.size)
        assertFalse(result["canConfirmNoConflict"]!!.jsonPrimitive.boolean)
        val touching = Json.parseToJsonElement(agentContextAgenda(mapOf("activityStart" to "11:00", "activityEnd" to "12:00"), f, "TODOS")).jsonObject
        assertTrue(touching["conflicts"]!!.jsonArray.isEmpty())
        assertTrue(touching["canConfirmNoConflict"]!!.jsonPrimitive.boolean)
        val allDay = Json.parseToJsonElement(agentContextAgenda(mapOf("activityStart" to "11:00", "activityEnd" to "12:00"),
            f.copy(todos = f.todos + TodoItemEntity(13, "全天", dueAt = due(date), allDay = true)), "TODOS")).jsonObject
        assertFalse(allDay["canConfirmNoConflict"]!!.jsonPrimitive.boolean)
        assertEquals(1, allDay["timeUncertain"]!!.jsonArray.size)
        assertThrows(IllegalArgumentException::class.java) { agentContextAgenda(mapOf("activityStart" to "12:00", "activityEnd" to "11:00"), f, "TODOS") }
    }
    @Test fun paginationKeepsFullCountsAndCompleteSecondPage() {
        val f = facts((1L..105L).map { TodoItemEntity(it, "待办$it", dueAt = due(date)) })
        assertEquals(105, query(f)["total"]!!.jsonPrimitive.int)
        assertEquals(100, query(f)["items"]!!.jsonArray.size)
        assertEquals(100, query(f)["nextOffset"]!!.jsonPrimitive.int)
        assertEquals(5, query(f, mapOf("offset" to "100"))["items"]!!.jsonArray.size)
        assertEquals(JsonNull, query(f, mapOf("offset" to "100"))["nextOffset"])
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsReversedDateRange() {
        query(facts(), mapOf("endDate" to "2026-09-30"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownBooleanInsteadOfGuessing() {
        query(facts(), mapOf("includeCompleted" to "maybe"))
    }
    @Test fun courseOccurrencesAndPeriodsUseCalendarProjection() {
        val course = CourseEntity(id = 8, name = "课程", teacher = null, location = "教室", weekday = 4,
            periods = listOf(1, 2), weeks = listOf(1), weekParity = WeekParity.ALL, note = null, scheduleId = 1)
        val f = buildDayAgentFacts(listOf(course), listOf(PeriodEntity(periodIndex = 1, startTime = "08:00", endTime = "08:45"),
            PeriodEntity(periodIndex = 2, startTime = "08:55", endTime = "09:40")), ScheduleConfigEntity(totalWeeks = 20, currentWeek = 1, notificationLeadMinutes = 10), date, null)
        val result = Json.parseToJsonElement(agentDateAgendaResult(mapOf("startDate" to date.toString(),
            "endDate" to date.plusDays(6).toString(), "kind" to "COURSES"), f)).jsonObject
        assertEquals(1, result["courseOccurrences"]!!.jsonPrimitive.int)
        assertEquals(2, result["coursePeriods"]!!.jsonPrimitive.int)
    }
    @Test fun creationUsesExplicitDateTimeAndDefaultsToNoReminder() {
        val draft = validateAgentTodo(AgentTodoDraft("开会", date = "2026-10-02", time = "08:00"), facts())!!
        assertEquals(due(date.plusDays(1)), draft.dueAt)
        assertFalse(draft.allDay)
        assertEquals("NONE", draft.reminderMode)
        assertNull(draft.groupId)
    }
    @Test fun creationSupportsAllDayAndExplicitReminder() {
        assertTrue(validateAgentTodo(AgentTodoDraft("作业", date = "2026-10-02"), facts())!!.allDay)
        assertEquals("BEFORE", validateAgentTodo(AgentTodoDraft("开会", date = "2026-10-02", time = "08:00", remind = true), facts())!!.reminderMode)
        assertNull(validateAgentTodo(AgentTodoDraft("开会", time = "08:00"), facts()))
        assertNull(validateAgentTodo(AgentTodoDraft("开会", date = "2026-10-02", remind = true), facts()))
    }
    @Test fun ambiguousOrNonexistentDstTimeIsRejected() {
        val f = facts().copy(timeZoneId = "America/New_York")
        assertNull(validateAgentTodo(AgentTodoDraft("会议", date = "2026-03-08", time = "02:30"), f))
        assertNull(validateAgentTodo(AgentTodoDraft("会议", date = "2026-11-01", time = "01:30"), f))
    }
    @Test fun todoActionIsValidatedAndInvalidBatchCannotPartiallyExecute() {
        val valid = """请确认。<agent_actions>[{"type":"CREATE_TODO","todo":{"title":"开会","date":"2026-10-02","time":"08:00"}}]</agent_actions>"""
        assertEquals(AgentValidatedActionType.CREATE_TODO, parseAgentActions(valid, facts()).actions.single().type)
        val invalid = valid.replace("08:00", "99:00")
        assertTrue(parseAgentActions(invalid, facts()).actions.isEmpty())
    }
    @Test fun changedTodosInvalidateCachedReadResults() {
        val cache = AgentToolFactCache()
        val call = AgentToolCall("date", AgentToolName.GET_DATE_AGENDA, mapOf("startDate" to date.toString()))
        cache.put(facts(), call, AgentToolResult(call.id, call.name, true, "empty"), 1_000)
        assertTrue(cache.read(facts(), 1_001).isNotEmpty())
        assertTrue(cache.read(facts(listOf(TodoItemEntity(title = "new"))), 1_001).isEmpty())
    }
    @Test fun liveClockAndLocationNeverBecomeCrossTurnCachedFacts() {
        val cache = AgentToolFactCache()
        listOf(AgentToolName.GET_CURRENT_TIME, AgentToolName.GET_CURRENT_LOCATION).forEach { name ->
            cache.put(facts(), AgentToolCall("live", name), AgentToolResult("live", name, true, "current"), 1_000)
        }
        assertTrue(cache.read(facts(), 1_001).isEmpty())
    }
}
