package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException

internal val ContextToolNames = setOf(AgentToolName.GET_MY_COURSES, AgentToolName.GET_MY_SCHEDULE,
    AgentToolName.GET_CURRENT_LOCATION, AgentToolName.GET_CURRENT_TIME)

internal fun agentContextAgenda(arguments: Map<String, String>, facts: DayAgentFacts, kind: String): String {
    val date = arguments["date"]?.takeIf(String::isNotBlank)?.let(LocalDate::parse) ?: facts.date
    val base = Json.parseToJsonElement(agentDateAgendaResult(mapOf("startDate" to date.toString(), "endDate" to date.toString(), "kind" to kind,
        "offset" to arguments["offset"].orEmpty().ifBlank { "0" }), facts)).jsonObject
    val start = arguments["activityStart"]?.takeIf(String::isNotBlank)
    val end = arguments["activityEnd"]?.takeIf(String::isNotBlank)
    if (start == null && end == null) return base.toString()
    require(start != null && end != null) { "请提供活动起止时间" }
    val from = java.time.LocalTime.parse(start); val until = java.time.LocalTime.parse(end)
    require(until > from) { "同日活动结束必须晚于开始" }
    // Conflict calculation reads all pages of both sources, not just a truncated visible list.
    val all = mutableListOf<JsonElement>()
    var offset = 0
    while (true) {
        val page = Json.parseToJsonElement(agentDateAgendaResult(mapOf("startDate" to date.toString(), "endDate" to date.toString(),
            "kind" to "ALL", "offset" to offset.toString()), facts)).jsonObject
        all.addAll(page["items"]!!.jsonArray)
        offset = page["nextOffset"]?.jsonPrimitive?.intOrNull ?: break
    }
    val unknown = all.filter { row ->
        val obj = row.jsonObject
        obj["allDay"]?.jsonPrimitive?.booleanOrNull == true ||
            (obj["start"]?.jsonPrimitive?.contentOrNull ?: obj["time"]?.jsonPrimitive?.contentOrNull) == null
    }
    val conflicts = all.filter { row ->
        val obj = row.jsonObject
        val rawStart = obj["start"]?.jsonPrimitive?.contentOrNull ?: obj["time"]?.jsonPrimitive?.contentOrNull
        if (rawStart == null || row in unknown) false else {
            val a = java.time.LocalTime.parse(rawStart)
            val b = obj["end"]?.jsonPrimitive?.contentOrNull?.let(java.time.LocalTime::parse)
            if (b == null) a >= from && a < until else a < until && b > from
        }
    }
    return buildJsonObject {
        base.forEach { (k, v) -> put(k, v) }
        put("activityStart", start); put("activityEnd", end); put("conflicts", JsonArray(conflicts))
        put("timeUncertain", JsonArray(unknown)); put("canConfirmNoConflict", conflicts.isEmpty() && unknown.isEmpty())
    }.toString()
}

internal fun agentCurrentTimeResult(): String = ZonedDateTime.now().let { now -> buildJsonObject {
    put("ok", true); put("date", now.toLocalDate().toString()); put("time", now.toLocalTime().toString()); put("timeZone", now.zone.id)
}.toString() }

/** Text and realtime providers share the same permission and read boundaries. */
internal suspend fun executeContextTool(context: Context, call: AgentToolCall, facts: DayAgentFacts,
    traceId: String): AgentToolResult {
    try {
        if (call.name == AgentToolName.GET_CURRENT_LOCATION) {
            val location = currentWeatherLocation(context)
            val payload = buildJsonObject {
                put("ok", location != null)
                if (location != null) {
                    put("latitude", location.latitude); put("longitude", location.longitude)
                    put("accuracyMeters", location.accuracy); put("timestamp", location.time)
                    put("text", "这是已授权定位结果；经纬度不是城市名称，不能据此猜测城市。")
                } else put("text", "暂时无法取得当前位置，请开启定位权限与定位服务，或告诉我城市。")
            }
            return AgentToolResult(call.id, call.name, location != null, payload.toString())
        }
        if (call.name == AgentToolName.WEATHER_QUICK) {
            val date = call.arguments["date"]?.takeIf(String::isNotBlank)?.let(LocalDate::parse) ?: facts.date
            val result = QuickWeather.query(context, call.arguments["city"], date, traceId)
            return AgentToolResult(call.id, call.name, result["ok"]?.jsonPrimitive?.booleanOrNull == true, result.toString())
        }
        return executeAgentReadTools(listOf(call), facts).single()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { return AgentToolResult(call.id, call.name, false, "无法读取，请检查查询日期、权限或网络；不要编造结果。") }
}
