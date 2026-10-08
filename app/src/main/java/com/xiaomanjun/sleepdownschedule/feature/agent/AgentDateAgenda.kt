package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.domain.schedule.coursesForDate
import com.xiaomanjun.sleepdownschedule.domain.schedule.courseStartTime
import com.xiaomanjun.sleepdownschedule.domain.schedule.courseEndTime
import com.xiaomanjun.sleepdownschedule.model.AppState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.*

internal fun agentDateAgendaResult(arguments: Map<String, String>, facts: DayAgentFacts): String {
    val start = runCatching { LocalDate.parse(arguments["startDate"]) }.getOrElse { throw IllegalArgumentException("startDate 必须为 yyyy-MM-dd") }
    val end = runCatching { LocalDate.parse(arguments["endDate"]) }.getOrElse { throw IllegalArgumentException("endDate 必须为 yyyy-MM-dd") }
    require(!end.isBefore(start) && ChronoUnit.DAYS.between(start, end) < 366) { "日期范围必须有序且不超过366天" }
    val kind = arguments["kind"]?.uppercase() ?: "ALL"
    require(kind in setOf("ALL", "COURSES", "TODOS")) { "kind 必须为 ALL/COURSES/TODOS" }
    fun flag(key: String): Boolean = arguments[key]?.toBooleanStrictOrNull() ?: false.also {
        require(arguments[key] == null) { "$key 必须为 true/false" }
    }
    val completed = flag("includeCompleted")
    val undated = flag("includeUndated")
    val offset = arguments["offset"]?.toIntOrNull() ?: 0.also { require(arguments["offset"] == null) { "offset 必须为整数" } }
    require(offset >= 0) { "offset 不得为负数" }
    val zone = ZoneId.of(facts.timeZoneId)
    val rows = mutableListOf<Pair<String, JsonObject>>()
    var courseCount = 0
    var periodCount = 0
    var todoCount = 0
    if (kind != "TODOS") {
        val config = requireNotNull(facts.scheduleConfig) { "课表日期配置尚未读取，不能推算课程" }
        val state = AppState(courses = facts.semesterCourses, periods = facts.periodDefinitions, config = config)
        var day = start
        while (!day.isAfter(end)) {
            coursesForDate(state, day, facts.date).forEach { course ->
                courseCount++
                periodCount += course.periods.distinct().size
                val time = courseStartTime(course, state.periods)?.toString()
                val date = day.toString()
                rows += "$date|${time ?: "99:99"}|0|${course.id}" to buildJsonObject {
                    put("type", "course"); put("id", course.id); put("date", date)
                    put("name", course.name); put("start", time?.let(::JsonPrimitive) ?: JsonNull)
                    put("end", courseEndTime(course, state.periods)?.toString()?.let(::JsonPrimitive) ?: JsonNull)
                    put("location", course.location.orEmpty()); put("teacher", course.teacher.orEmpty()); put("notes", course.note.orEmpty())
                    put("periodCount", course.periods.distinct().size)
                }
            }
            day = day.plusDays(1)
        }
    }
    if (kind != "COURSES") facts.todos.asSequence().filter { it.deletedAt == null && (completed || !it.isCompleted) }.forEach { todo ->
        val due = todo.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val date = due?.toLocalDate()
        if (date == null && !undated || date != null && (date.isBefore(start) || date.isAfter(end))) return@forEach
        todoCount++
        val time = due?.toLocalTime()?.toString()?.takeUnless { todo.allDay }
        rows += "${date ?: "9999-12-31"}|${time ?: "00:00"}|1|${todo.id}" to buildJsonObject {
            put("type", "todo"); put("id", todo.id); put("title", todo.title); put("description", todo.description)
            put("date", date?.toString()?.let(::JsonPrimitive) ?: JsonNull); put("time", time?.let(::JsonPrimitive) ?: JsonNull)
            put("allDay", todo.allDay); put("completed", todo.isCompleted); put("priority", todo.priority)
            put("end", todo.endAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime().toString() }?.let(::JsonPrimitive) ?: JsonNull)
            put("courseId", todo.courseId?.let(::JsonPrimitive) ?: JsonNull); put("groupId", todo.groupId?.let(::JsonPrimitive) ?: JsonNull)
        }
    }
    val sorted = rows.sortedBy { it.first }
    val page = sorted.drop(offset).take(100)
    return buildJsonObject {
        put("startDate", start.toString()); put("endDate", end.toString()); put("timeZone", zone.id)
        put("courseOccurrences", courseCount); put("coursePeriods", periodCount); put("todoCount", todoCount)
        put("total", rows.size); put("offset", offset)
        put("nextOffset", (offset + page.size).takeIf { it < rows.size }?.let(::JsonPrimitive) ?: JsonNull)
        put("items", JsonArray(page.map { it.second }))
    }.toString()
}
