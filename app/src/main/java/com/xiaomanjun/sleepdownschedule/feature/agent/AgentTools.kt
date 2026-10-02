package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI

/**
 * Provider-independent read-tool protocol.
 *
 * Providers with native function calling can map their calls to these objects later. Text-only
 * providers receive the same deterministic results as compact system messages, so the rest of the
 * Agent never depends on one vendor's wire format.
 */
enum class AgentToolName {
    GET_MY_COURSES,
    GET_MY_SCHEDULE,
    GET_CURRENT_LOCATION,
    GET_CURRENT_TIME,
    GET_CURRENT_OVERVIEW,
    GET_DATE_AGENDA,
    WEATHER_QUICK,
    SEARCH_COURSES,
    GET_WEEK_SCHEDULE,
    GET_SEMESTER_SCHEDULE,
    GET_PERIODS,
    GET_SETTINGS,
    GET_SCHEDULE_ADJUSTMENTS,
    GET_SCHEDULES,
    UPDATE_MEMORY
}

data class AgentToolCall(
    val id: String,
    val name: AgentToolName,
    val arguments: Map<String, String> = emptyMap()
)

data class AgentToolResult(
    val callId: String,
    val name: AgentToolName,
    val success: Boolean,
    val content: String
)

@Serializable
data class AgentRunStatus(
    val icon: AgentRunStatusIcon,
    val text: String,
    val detail: String? = null
)

@Serializable
enum class AgentRunStatusIcon {
    OVERVIEW,
    SEARCH,
    SCHEDULE,
    PERIOD,
    SETTINGS,
    THINKING
}

internal fun AgentToolName.runStatus(): AgentRunStatus = when (this) {
    AgentToolName.GET_MY_COURSES, AgentToolName.GET_MY_SCHEDULE -> AgentRunStatus(AgentRunStatusIcon.SCHEDULE, "读取安排")
    AgentToolName.GET_CURRENT_LOCATION -> AgentRunStatus(AgentRunStatusIcon.SEARCH, "读取已授权位置")
    AgentToolName.GET_CURRENT_TIME -> AgentRunStatus(AgentRunStatusIcon.OVERVIEW, "读取当前时间")
    AgentToolName.WEATHER_QUICK -> AgentRunStatus(AgentRunStatusIcon.SEARCH, "查询天气")
    AgentToolName.GET_DATE_AGENDA -> AgentRunStatus(AgentRunStatusIcon.SCHEDULE, "按日期查询课程与待办")
    AgentToolName.GET_CURRENT_OVERVIEW ->
        AgentRunStatus(AgentRunStatusIcon.OVERVIEW, "读取当前日程")
    AgentToolName.SEARCH_COURSES ->
        AgentRunStatus(AgentRunStatusIcon.SEARCH, "查找课程")
    AgentToolName.GET_WEEK_SCHEDULE ->
        AgentRunStatus(AgentRunStatusIcon.SCHEDULE, "读取本周课表")
    AgentToolName.GET_SEMESTER_SCHEDULE ->
        AgentRunStatus(AgentRunStatusIcon.SCHEDULE, "读取学期课表")
    AgentToolName.GET_PERIODS ->
        AgentRunStatus(AgentRunStatusIcon.PERIOD, "读取节次时间")
    AgentToolName.GET_SETTINGS ->
        AgentRunStatus(AgentRunStatusIcon.SETTINGS, "读取应用设置")
    AgentToolName.GET_SCHEDULE_ADJUSTMENTS ->
        AgentRunStatus(AgentRunStatusIcon.SCHEDULE, "读取调休安排")
    AgentToolName.GET_SCHEDULES ->
        AgentRunStatus(AgentRunStatusIcon.SETTINGS, "读取课表列表")
    AgentToolName.UPDATE_MEMORY ->
        AgentRunStatus(AgentRunStatusIcon.SETTINGS, "更新助手记忆")
}

/** Immutable snapshot reads only need to be exposed once per user turn. */
internal val AgentToolName.isOneShotPerTurn: Boolean
    get() = this !in setOf(AgentToolName.SEARCH_COURSES, AgentToolName.GET_DATE_AGENDA, AgentToolName.WEATHER_QUICK)

internal fun AgentToolCall.cacheKey(): String = buildString {
    append(name.name)
    arguments.toSortedMap().forEach { (key, value) ->
        append('\u0000').append(key).append('=').append(value.trim())
    }
}

/**
 * OpenAI-compatible function declarations. The model, not Kotlin keyword rules, chooses which
 * tools to call. Local code only validates the selected name/arguments and executes it against the
 * active schedule-scoped fact snapshot.
 */
internal fun agentToolDefinitions(
    includeMiMoWebSearch: Boolean = false,
    forceMiMoWebSearch: Boolean = false,
    includeMemoryTool: Boolean = false,
    strictFunctions: Boolean = false,
    excludedTools: Set<AgentToolName> = emptySet()
): JsonArray = buildJsonArray {
    listOf(AgentToolName.GET_MY_COURSES to "查询指定日期课程，date为yyyy-MM-dd，省略表示本地今天。活动安排需同时查询get_my_schedule；无活动时段时询问起止时间。",
        AgentToolName.GET_MY_SCHEDULE to "查询指定日期的日程与未完成待办，含时间段和全天状态，排除最近删除。date为yyyy-MM-dd，省略表示本地今天。",
        AgentToolName.GET_CURRENT_LOCATION to "读取用户已授权的当前位置；缺少权限时返回明确错误，不猜测城市。",
        AgentToolName.GET_CURRENT_TIME to "读取设备当前日期、时间及时区，解析明天等相对时间前使用。"
    ).forEach { (name, description) ->
        if (name !in excludedTools) add(agentToolDefinition(name, description,
            fields = if (name in setOf(AgentToolName.GET_MY_COURSES, AgentToolName.GET_MY_SCHEDULE)) mapOf("date" to "本地ISO日期，可为空表示今天", "offset" to "翻页偏移，默认0；结果有nextOffset时续读",
                "activityStart" to "可选：活动开始HH:mm，与activityEnd一起提供", "activityEnd" to "可选：同日活动结束HH:mm，必须晚于开始") else emptyMap(),
            strict = strictFunctions))
    }
    if (AgentToolName.WEATHER_QUICK !in excludedTools) add(agentToolDefinition(
        AgentToolName.WEATHER_QUICK,
        "weather.quick：快速查询真实天气。city 为明确城市，可为空表示设备当前位置；date 为 yyyy-MM-dd，可为空表示本地今天。结果 ok/text/temperature 来自 Open-Meteo。成功后直接回复真实结果，不再重复查询或只说正在查询；失败时说明原因，不猜测天气。",
        fields = linkedMapOf("city" to "明确城市或空值使用已授权定位", "date" to "ISO 日期或空值使用本地今天"),
        strict = strictFunctions
    ))
    if (AgentToolName.GET_DATE_AGENDA !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_DATE_AGENDA,
        "按设备本地日期查询课程与待办。startDate/endDate 为 yyyy-MM-dd 且包含首尾，最多366天。kind=ALL/COURSES/TODOS，默认ALL；includeCompleted 默认false；includeUndated 默认false，问近期/所有待办时可为true。返回完整计数、按时间排序的明细及 nextOffset；有 nextOffset 必须续读才能声称已列出全部。课程使用真实学期、单双周与调休规则。",
        fields = linkedMapOf("startDate" to "起始本地日期，必填", "endDate" to "结束本地日期，必填",
            "kind" to "ALL、COURSES 或 TODOS", "includeCompleted" to "true/false：是否包含已完成待办",
            "includeUndated" to "true/false：是否包含没有日期的待办", "offset" to "翻页偏移，默认0"),
        strict = strictFunctions
    ))
    if (AgentToolName.GET_CURRENT_OVERVIEW !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_CURRENT_OVERVIEW,
        "当前日期、时间、学期状态、有效教学周、今天/明天的完整课程记录与实际发生时间、原教学周和天气。",
        strict = strictFunctions
    ))
    if (AgentToolName.SEARCH_COURSES !in excludedTools) add(agentToolDefinition(
        AgentToolName.SEARCH_COURSES,
        "一次组合条件定位当前课表的课程，返回完整记录和真实 ID。query 模糊检索，其余条件精确匹配且同时满足；至少提供一项。定位后直接组合字段修改，无需按字段重复查询。最多展示 24 条，截断时用完整学期快照处理批量目标。",
        courseSearch = true,
        strict = strictFunctions
    ))
    if (AgentToolName.GET_WEEK_SCHEDULE !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_WEEK_SCHEDULE,
        "当前教学周完整日程，用于空档、冲突和本周安排。",
        strict = strictFunctions
    ))
    if (AgentToolName.GET_SEMESTER_SCHEDULE !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_SEMESTER_SCHEDULE,
        "当前课表的全部课程记录，用于课程总览、批量或跨周修改。",
        strict = strictFunctions
    ))
    if (AgentToolName.GET_PERIODS !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_PERIODS,
        "当前节次拓扑、准确时间和全部作息方案；调整节次或时间前读取。",
        strict = strictFunctions
    ))
    if (AgentToolName.GET_SETTINGS !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_SETTINGS,
        "可访问设置的键、类型、范围和当前值；回答或修改设置前读取。",
        strict = strictFunctions
    ))
    if (AgentToolName.GET_SCHEDULE_ADJUSTMENTS !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_SCHEDULE_ADJUSTMENTS,
        "读取当前课表已保存的停课日期、补课日期及原课程日期。用户问调休是哪几天、哪天补哪天时，用结果直接回答；也供修改前读取完整调休表。查询无需打开设置。",
        strict = strictFunctions
    ))
    if (AgentToolName.GET_SCHEDULES !in excludedTools) add(agentToolDefinition(
        AgentToolName.GET_SCHEDULES,
        "全部课表（含 ID、名称、是否当前使用）；多课表切换、删除或创建后读取。",
        strict = strictFunctions
    ))
    if (includeMemoryTool && AgentToolName.UPDATE_MEMORY !in excludedTools) {
        add(agentMemoryToolDefinition(strictFunctions))
    }
    /*
     * Only fact acquisition is exposed as a model tool. Write plans deliberately remain the
     * generic <agent_actions> JSON protocol in the final answer: turning every write primitive
     * into a function tool makes the schema look like a capability allow-list and causes models
     * to refuse perfectly representable composite tasks.
     */
    if (includeMiMoWebSearch) {
        /*
         * This is MiMo's server-side tool, not a locally executed function. With force_search
         * disabled and tool_choice=auto in the request, MiMo decides whether fresh public web
         * information is needed and returns the summarized answer itself.
         */
        add(buildJsonObject {
            put("type", "web_search")
            put("max_keyword", 3)
            put("force_search", forceMiMoWebSearch)
            put("limit", 1)
        })
    }
}

private val AgentRunTraceJson = Json { ignoreUnknownKeys = true }
private val AgentRunTraceMarker = Regex(
    "<agent_run_trace>([\\s\\S]*?)</agent_run_trace>",
    RegexOption.IGNORE_CASE
)

internal data class AgentStoredMessage(
    val content: String,
    val statuses: List<AgentRunStatus>
)

/**
 * Execution summaries are persisted with the visible answer so a completed turn remains
 * inspectable after the in-memory runner is released. They are application metadata, not model
 * reasoning, and are removed again before conversation history is sent to a provider.
 */
internal fun agentMessageWithRunTrace(
    content: String,
    statuses: List<AgentRunStatus>
): String {
    val usefulStatuses = statuses.fold(mutableListOf<AgentRunStatus>()) { result, status ->
        if (result.lastOrNull() != status) result += status
        result
    }
    if (usefulStatuses.isEmpty()) return content
    return "<agent_run_trace>${AgentRunTraceJson.encodeToString(usefulStatuses)}</agent_run_trace>\n$content"
}

internal fun parseAgentStoredMessage(content: String): AgentStoredMessage {
    val marker = AgentRunTraceMarker.find(content)
    val statuses = marker?.groupValues?.getOrNull(1)?.let { json ->
        runCatching { AgentRunTraceJson.decodeFromString<List<AgentRunStatus>>(json) }
            .getOrDefault(emptyList())
    }.orEmpty()
    return AgentStoredMessage(
        content = content.replace(AgentRunTraceMarker, "").trimStart(),
        statuses = statuses
    )
}

/**
 * Responses API function tools use a flattened declaration while Chat Completions nests the
 * same fields under `function`. Keep one schema source and adapt only the wire shape.
 */
internal fun agentResponsesToolDefinitions(
    includeMemoryTool: Boolean = false,
    excludedTools: Set<AgentToolName> = emptySet()
): JsonArray = buildJsonArray {
    agentToolDefinitions(
        includeMemoryTool = includeMemoryTool,
        strictFunctions = true,
        excludedTools = excludedTools
    ).forEach { declaration ->
        val function = declaration.jsonObject["function"]?.jsonObject ?: return@forEach
        add(buildJsonObject {
            put("type", "function")
            function["name"]?.let { put("name", it) }
            function["description"]?.let { put("description", it) }
            function["parameters"]?.let { put("parameters", it) }
            function["strict"]?.let { put("strict", it) }
        })
    }
}

private fun agentMemoryToolDefinition(strict: Boolean) = buildJsonObject {
    put("type", "function")
    put("function", buildJsonObject {
        put("name", AgentToolName.UPDATE_MEMORY.name)
        put(
            "description",
            "完整替换用户已授权保存的简短长期记忆。仅保存跨天仍有价值的稳定偏好或背景；" +
                "不得保存临时任务、当天安排、聊天复述或敏感凭据。没有值得更新的内容时不要调用。"
        )
        put("parameters", buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("memory", buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "完整的新记忆文本，不是增量。用简短条目表达；明确忘记全部内容时传空字符串。"
                    )
                    put("maxLength", 1200)
                })
            })
            put("required", buildJsonArray { add(JsonPrimitive("memory")) })
            put("additionalProperties", false)
        })
        if (strict) put("strict", true)
    })
}

/**
 * MiMo's web_search extension is intentionally sent only to the documented official endpoint and
 * supported model names. Custom OpenAI-compatible services frequently reject unknown tool types.
 */
internal fun supportsMiMoOfficialWebSearch(
    providerId: String,
    baseUrl: String,
    model: String
): Boolean {
    @Suppress("UNUSED_VARIABLE")
    val configuredProviderId = providerId
    val host = runCatching { URI(baseUrl.trim()).host.orEmpty().lowercase() }.getOrDefault("")
    // Xiaomi currently documents the server-side web_search plugin only on the pay-as-you-go
    // Chat Completions endpoint. Token Plan is deliberately not treated as equivalent here.
    if (host != "api.xiaomimimo.com") return false
    return model.trim().lowercase() in setOf("mimo-v2.5-pro", "mimo-v2.5")
}

private val AgentCourseSearchFields = linkedMapOf(
    "query" to "课程名、教师或地点的模糊检索词；已有精确条件时可省略",
    "courseId" to "已知真实课程记录 ID，十进制整数",
    "name" to "完整课程名称，精确匹配",
    "teacher" to "当前教师姓名，精确匹配",
    "location" to "当前上课地点，精确匹配",
    "weekday" to "原始课表星期，整数 1（周一）至 7（周日）；实际补课日期请使用日程工具定位",
    "period" to "课程包含的节次，正整数",
    "week" to "课程实际开课的教学周，正整数，考虑单双周"
)

private fun agentToolDefinition(
    name: AgentToolName,
    description: String,
    courseSearch: Boolean = false,
    fields: Map<String, String> = emptyMap(),
    strict: Boolean = false
) = buildJsonObject {
    put("type", "function")
    put("function", buildJsonObject {
        // Dots are not legal function names for several compatible providers.
        // weather.quick is the service API; WEATHER_QUICK is its portable model wire name.
        put("name", if (name in ContextToolNames) name.name.lowercase() else name.name)
        put("description", description)
        put("parameters", buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                (if (courseSearch) AgentCourseSearchFields else fields).forEach { (key, description) ->
                        put(key, buildJsonObject {
                            put("type", buildJsonArray { add(JsonPrimitive("string")); add(JsonPrimitive("null")) })
                            put("description", description)
                        })
                    }
            })
            put("required", buildJsonArray {
                // Strict providers require all declared fields; unused filters are JSON null.
                if (courseSearch && strict) AgentCourseSearchFields.keys.forEach { add(JsonPrimitive(it)) }
                if (strict) fields.keys.forEach { add(JsonPrimitive(it)) }
                else if (name == AgentToolName.GET_DATE_AGENDA) {
                    add(JsonPrimitive("startDate")); add(JsonPrimitive("endDate"))
                }
            })
            put("additionalProperties", false)
        })
        if (strict) put("strict", true)
    })
}

internal fun executeAgentReadTools(
    calls: List<AgentToolCall>,
    facts: DayAgentFacts
): List<AgentToolResult> {
    /*
     * Every tool must read the same schedule-scoped fact snapshot. Upstream UI transitions may
     * briefly retain slots from the previous pager, so enforce the boundary here rather than
     * trusting any caller. This also makes tool results mutually consistent by construction.
     */
    val scopedSemesterCourses = (
        facts.semesterCourses.asSequence() +
            facts.week.asSequence().map { it.course } +
            facts.today.asSequence().map { it.course } +
            facts.tomorrow.asSequence().map { it.course }
        )
        .filter { it.scheduleId == facts.scheduleId }
        .distinctBy { it.id }
        .toList()
    val courseIds = scopedSemesterCourses.mapTo(hashSetOf()) { it.id }
    val scopedFacts = facts.copy(
        today = facts.today.filter {
            it.course.scheduleId == facts.scheduleId && it.course.id in courseIds
        },
        tomorrow = facts.tomorrow.filter {
            it.course.scheduleId == facts.scheduleId && it.course.id in courseIds
        },
        week = facts.week.filter {
            it.course.scheduleId == facts.scheduleId && it.course.id in courseIds
        },
        periodDefinitions = facts.periodDefinitions.filter { it.scheduleId == facts.scheduleId },
        semesterCourses = scopedSemesterCourses
    )
    return calls.map { call ->
        val content = try {
            when (call.name) {
                AgentToolName.GET_MY_COURSES -> agentContextAgenda(call.arguments, scopedFacts, "COURSES")
                AgentToolName.GET_MY_SCHEDULE -> agentContextAgenda(call.arguments, scopedFacts, "TODOS")
                AgentToolName.GET_CURRENT_TIME -> agentCurrentTimeResult()
                AgentToolName.GET_CURRENT_LOCATION -> throw IllegalArgumentException("位置工具需要会话权限上下文")
                AgentToolName.WEATHER_QUICK -> throw IllegalArgumentException("天气必须通过会话层的 weather.quick 联网接口读取，当前离线执行器没有天气结果")
                AgentToolName.GET_DATE_AGENDA -> agentDateAgendaResult(call.arguments, scopedFacts)
                AgentToolName.GET_CURRENT_OVERVIEW -> agentOverviewResult(scopedFacts)
                AgentToolName.SEARCH_COURSES -> agentCourseSearchResult(call.arguments, scopedFacts)
                AgentToolName.GET_WEEK_SCHEDULE -> agentWeekResult(scopedFacts)
                AgentToolName.GET_SEMESTER_SCHEDULE -> agentSemesterResult(scopedFacts)
                AgentToolName.GET_PERIODS -> agentPeriodResult(scopedFacts)
                AgentToolName.GET_SETTINGS -> AgentSettingRegistry.promptCatalog(currentValues = scopedFacts.settingSnapshot)
                AgentToolName.GET_SCHEDULE_ADJUSTMENTS -> agentAdjustmentsResult(scopedFacts)
                AgentToolName.GET_SCHEDULES -> agentSchedulesResult(scopedFacts)
                AgentToolName.UPDATE_MEMORY -> "记忆更新只能由助手会话层处理"
            }
        } catch (error: IllegalArgumentException) {
            return@map AgentToolResult(call.id, call.name, false, "读取失败：${error.message}；请修正查询条件后重试。")
        }
        AgentToolResult(
            callId = call.id,
            name = call.name,
            success = true,
            content = "读取成功；当前课表ID=${scopedFacts.scheduleId}；事实版本=${scopedFacts.sourceHash}\n$content"
        )
    }
}

private val CompleteAgentToolResult = Regex(
    "<\\s*tool_(?:result|call)\\b[^>]*>[\\s\\S]*?<\\s*/\\s*tool_(?:result|call)\\s*>",
    RegexOption.IGNORE_CASE
)

private val IncompleteAgentToolResult = Regex(
    "<\\s*tool_(?:result|call)\\b[^>]*(?:>|$)[\\s\\S]*$",
    RegexOption.IGNORE_CASE
)

private val LeakedAgentFunctionProtocol = Regex(
    "(?:<\\s*[|｜]\\s*DSML\\s*[|｜]\\s*(?:tool_calls?|invoke|parameter)\\b|" +
        "<\\s*(?:tool_calls|function_call|invoke|parameter)\\b|" +
        "[\"'](?:tool_calls|function_call)[\"']\\s*:)",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

internal fun containsLeakedAgentFunctionProtocol(content: String): Boolean =
    LeakedAgentFunctionProtocol.containsMatchIn(content)

internal class AgentProtocolViolationException(val clarification: String? = null) :
    IllegalStateException(clarification ?: "这次未能整理好内容，请补充信息或重试；尚未创建任何项目")

/**
 * Holds transport chunks until the complete answer and proposal have passed validation.
 * A repair never flashes internal syntax or persists unchecked assistant content.
 */
internal class AgentFinalOutputGate(
    private val onDelta: (String) -> Unit,
    private val holdBackCharacters: Int = 64,
    private val validate: (String) -> String = { it }
) {
    private val content = StringBuilder()

    fun accept(delta: String) {
        if (delta.isEmpty()) return
        val scanStart = (content.length - holdBackCharacters).coerceAtLeast(0)
        content.append(delta)
        if (containsLeakedAgentFunctionProtocol(content.substring(scanStart))) {
            throw AgentProtocolViolationException()
        }
        // Proposals and echoed fact JSON cannot be validated from a partial chunk. Keep this
        // transport buffer private until the complete reply is checked, for every provider.
    }

    fun finish(answer: String): String {
        if (answer.isBlank()) throw MissingAgentBodyException()
        if (containsLeakedAgentFunctionProtocol(answer)) throw AgentProtocolViolationException()
        if (containsAgentContextEcho(answer)) throw AgentProtocolViolationException()
        if (content.toString() != answer) {
            throw IllegalStateException("AI 流式响应内容不完整，请重试")
        }
        val checked = validate(answer)
        onDelta(checked)
        return checked
    }

}

/**
 * Tool payloads are an internal transport detail. Never persist or render them as assistant text.
 *
 * The incomplete form matters for streaming: from the first opening tag until its closing tag
 * arrives, the partial payload stays invisible. Once the close tag arrives, any natural-language
 * answer after it becomes visible immediately.
 */
internal fun sanitizeAgentToolOutput(content: String): String {
    if (containsLeakedAgentFunctionProtocol(content) || containsAgentContextEcho(content)) return ""
    val completeMatches = CompleteAgentToolResult.findAll(content).toList()
    if (completeMatches.isNotEmpty()) {
        return content
            .substring(completeMatches.last().range.last + 1)
            .replace(IncompleteAgentToolResult, "")
            .trim()
    }
    if (IncompleteAgentToolResult.containsMatchIn(content)) return ""
    return content.trim()
}

/**
 * Text emitted before a tool payload is orchestration narration ("让我查一下……"), not the
 * assistant's final answer. It may be shown as a subdued transient reasoning/status line while
 * streaming, but it must never be merged into the final body.
 */
internal fun extractAgentToolPrelude(content: String): String {
    val completeMatches = CompleteAgentToolResult.findAll(content).toList()
    if (completeMatches.isNotEmpty()) {
        return content
            .substring(0, completeMatches.last().range.last + 1)
            .replace(CompleteAgentToolResult, "")
            .trim()
    }
    val incomplete = IncompleteAgentToolResult.find(content) ?: return ""
    return content.substring(0, incomplete.range.first).trim()
}

private fun agentOverviewResult(facts: DayAgentFacts): String = buildString {
    val weekday = weekdayLabel(facts.date.dayOfWeek.toChineseWeekday())
    appendLine("日期=${facts.date} 星期$weekday；当前时间=${facts.now.toLocalTime()}")
    if (facts.todayIsAdjusted) appendLine(
        facts.currentTeachingDate?.let { "今日调休：补原 $it 第 ${facts.currentTeachingWeek} 周的课程；调整今日课程使用原课程日期和教学周。" }
            ?: "今日调休：停课。"
    )
    val teachingWeek = if (facts.termState in setOf(ScheduleTermState.MANUAL, ScheduleTermState.ACTIVE)) {
        facts.currentWeek.toString()
    } else {
        "无"
    }
    appendLine("课表ID=${facts.scheduleId}；学期状态=${facts.termState.name}（${facts.termStatus}）；" +
        "当前有效教学周=$teachingWeek；总周数=${facts.totalWeeks}")
    appendLine("天气=${facts.weather?.summary ?: "不可用"}")
    appendLine(
        "今日=" + facts.today.joinToString("；") {
            agentDayCourseLine(it)
        }.ifBlank { "无课" }
    )
    appendLine(
        "明日=" + facts.tomorrow.joinToString("；") {
            agentDayCourseLine(it)
        }.ifBlank { "无课" }
    )
}

private fun agentDayCourseLine(item: AgentCourseSlot): String =
    "${item.date} ${item.start}-${item.end} ${agentCourseLine(item.course)}" +
        "；teachingWeek=${item.teachingWeek}；originalDate=${item.originalDate ?: item.date}"

private fun agentCourseSearchResult(arguments: Map<String, String>, facts: DayAgentFacts): String {
    require(arguments.keys.all { it in AgentCourseSearchFields }) { "存在不支持的课程筛选字段" }
    val filters = arguments.mapValues { it.value.trim() }.filterValues(String::isNotEmpty)
    require(filters.isNotEmpty()) { "至少提供一个课程筛选条件" }
    fun positiveNumber(key: String, maximum: Long = Long.MAX_VALUE): Long? = filters[key]?.let { raw ->
        requireNotNull(raw.toLongOrNull()?.takeIf { it in 1..maximum }) { "$key 必须是 1 至 $maximum 之间的整数" }
    }
    val id = positiveNumber("courseId")
    val weekday = positiveNumber("weekday", 7)?.toInt()
    val period = positiveNumber("period", Int.MAX_VALUE.toLong())?.toInt()
    val week = positiveNumber("week", facts.totalWeeks.toLong())?.toInt()
    val unique = facts.semesterCourses.distinctBy { it.id }
    val needle = filters["query"].orEmpty()
    /*
     * Match in both directions: the model may pass either a fragment of the course name
     * ("数学" → "高等数学") or a whole user sentence that embeds the full name. One-directional
     * query.contains(name) silently failed the first, far more common case.
     */
    fun fieldMatches(field: String?): Boolean {
        val value = field?.takeIf(String::isNotBlank) ?: return false
        return value.contains(needle, ignoreCase = true) ||
            needle.contains(value, ignoreCase = true)
    }
    fun exact(key: String, value: String?): Boolean = filters[key]?.let { it.equals(value?.trim(), ignoreCase = true) } ?: true
    val matched = unique.filter { course ->
        (needle.isEmpty() || fieldMatches(course.name) || fieldMatches(course.teacher) || fieldMatches(course.location)) &&
            (id == null || course.id == id) && exact("name", course.name) &&
            exact("teacher", course.teacher) && exact("location", course.location) &&
            (weekday == null || course.weekday == weekday) &&
            (period == null || period in course.periods) &&
            (week == null || week in course.weeks && parityMatches(course.weekParity, week))
    }
    if (matched.isEmpty()) return "没有匹配课程"
    return buildString {
        append(matched.take(24).joinToString("\n", transform = ::agentCourseLine))
        if (matched.size > 24) {
            append("\n匹配共 ${matched.size} 条，仅展示前 24 条；批量处理前请读取 GET_SEMESTER_SCHEDULE 的完整记录后筛选，不能把当前结果当作全部目标。")
        }
    }
}

private fun agentWeekResult(facts: DayAgentFacts): String = buildString {
    appendLine("学期状态=${facts.termState.name}（${facts.termStatus}）")
    if (facts.termState !in setOf(ScheduleTermState.MANUAL, ScheduleTermState.ACTIVE)) {
        append("当前没有有效教学周")
    } else {
        append(
            facts.week.joinToString("\n") { item ->
                agentDayCourseLine(item) +
                    if (item.originalDate != null) "（调休：原 ${item.originalDate} 第 ${item.teachingWeek} 周）" else ""
            }.ifBlank { "本周无课" }
        )
    }
}

private fun agentSemesterResult(facts: DayAgentFacts): String = buildString {
    appendLine("学期状态=${facts.termState.name}（${facts.termStatus}）")
    appendLine("课程行=id|名称|星期|节次|周次；可选字段 p=单双周（默认 ALL）、l=地点、t=教师、n=备注（省略表示无备注）、x=自定义时间、c=自定义颜色（#AARRGGBB，省略表示默认）")
    append(
        facts.semesterCourses.distinctBy { it.id }
            .joinToString("\n", transform = ::agentCompactCourseLine)
            .ifBlank { "本学期无课程" }
    )
}

private fun agentPeriodResult(facts: DayAgentFacts): String =
    buildString {
        appendLine(
            "节次拓扑：上午=${facts.settingSnapshot["MORNING_PERIOD_COUNT"] ?: "UNKNOWN"}；" +
                "中午=${facts.settingSnapshot["NOON_PERIOD_COUNT"] ?: "UNKNOWN"}；" +
                "下午=${facts.settingSnapshot["AFTERNOON_PERIOD_COUNT"] ?: "UNKNOWN"}；" +
                "晚上=${facts.settingSnapshot["EVENING_PERIOD_COUNT"] ?: "UNKNOWN"}"
        )
        appendLine("当前生效的物化节次：")
        appendLine(
            facts.periodDefinitions.sortedBy { it.periodIndex }
                .joinToString("；") { "第${it.periodIndex}节 ${it.startTime}-${it.endTime}" }
                .ifBlank { "没有节次定义" }
        )
        if (facts.periodSchemes.isEmpty()) {
            appendLine("作息方案：数据库暂未返回方案")
        } else {
            appendLine("全部作息方案：")
            facts.periodSchemes.forEach { scheme ->
                appendLine(
                    "- ID=${scheme.id}；名称=${scheme.name}；当前=${scheme.isActive}；" +
                        "模式=${scheme.mode}；单节=${scheme.classDurationMinutes}分钟；" +
                        "普通课间=${scheme.breakDurationMinutes}分钟；" +
                        "时段起点=上午${scheme.morningStartTime}/中午${scheme.noonStartTime}/" +
                        "下午${scheme.afternoonStartTime}/晚上${scheme.eveningStartTime}；" +
                        "特殊课间=${scheme.specialBreaks.ifEmpty { mapOf<Int, Int>() }}；" +
                        "手动覆盖=${scheme.overriddenPeriods.sorted()}"
                )
                appendLine(
                    "  时间线=" + scheme.times.joinToString("；") {
                        "第${it.periodIndex}节 ${it.startTime}-${it.endTime}"
                    }
                )
            }
        }
    }.trim()

/** 调休表：date（调休/放假日），sourceDate（补课来源日，可为空），label。 */
private fun agentAdjustmentsResult(facts: DayAgentFacts): String = buildString {
    appendLine("当前课表已保存的调休安排。直接向用户说明停课日期、补课日期及原课程日期，无需导航：")
    if (facts.scheduleAdjustments.isEmpty()) {
        append("当前课表没有保存调休安排；这不代表学校或法定日历没有调休，不能编造日期。")
    } else {
        facts.scheduleAdjustments.forEach { adjustment ->
            appendLine(
                "- ${if (adjustment.sourceDate == null) "停课" else "补课"} date=${adjustment.date}" +
                    adjustment.sourceDate?.let { "；sourceDate=$it（上该日课程）" }.orEmpty() +
                    "；label=${adjustment.label.ifBlank { "（无说明）" }}"
            )
        }
    }
}

/** 全部课表；isActive 表示当前正在使用的课表。 */
private fun agentSchedulesResult(facts: DayAgentFacts): String = buildString {
    if (facts.schedules.isEmpty()) {
        append("课表ID=${facts.scheduleId}（名称未提供）")
    } else {
        facts.schedules.forEach { schedule ->
            appendLine(
                "- id=${schedule.id}；名称=${schedule.name}；当前使用=${schedule.isActive}"
            )
        }
        trimEnd()
    }
}

private fun agentCourseLine(course: CourseEntity): String =
    "ID=${course.id} ${course.name}；星期=${course.weekday}；节次=${course.periods.joinToString(",")}" +
        "；周次=${course.weeks.joinToString(",")}；单双周=${course.weekParity}" +
        "；地点=${course.location ?: "待确认"}；教师=${course.teacher ?: "待确认"}；备注=${course.note.orEmpty().toAgentCompactField()}" +
        course.customTimeRangeOrNull()?.let { (start, end) ->
            "；自定义时间=${start}-${end}（优先于节次默认时间）"
        }.orEmpty() + course.customColorArgb?.let { "；自定义颜色=${agentColorText(it)}" }.orEmpty()

private fun agentCompactCourseLine(course: CourseEntity): String = buildList {
    add(course.id.toString())
    add(course.name.toAgentCompactField())
    add(course.weekday.toString())
    add(course.periods.toAgentRanges())
    add(course.weeks.toAgentRanges())
    if (course.weekParity != WeekParity.ALL) add("p=${course.weekParity}")
    course.location?.takeIf(String::isNotBlank)?.let { add("l=${it.toAgentCompactField()}") }
    course.teacher?.takeIf(String::isNotBlank)?.let { add("t=${it.toAgentCompactField()}") }
    course.note?.takeIf(String::isNotBlank)?.let { add("n=${it.toAgentCompactField()}") }
    course.customTimeRangeOrNull()?.let { (start, end) -> add("x=$start-$end") }
    course.customColorArgb?.let { add("c=${agentColorText(it)}") }
}.joinToString("|")

private fun agentColorText(color: Long): String = "#" + (color and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

private fun String.toAgentCompactField(): String = trim()
    .replace('|', '／')
    .replace('\r', ' ')
    .replace('\n', ' ')

private fun List<Int>.toAgentRanges(): String {
    val values = distinct().sorted()
    if (values.isEmpty()) return "-"
    val ranges = mutableListOf<String>()
    var start = values.first()
    var end = start
    values.drop(1).forEach { value ->
        if (value == end + 1) {
            end = value
        } else {
            ranges += if (start == end) "$start" else "$start-$end"
            start = value
            end = value
        }
    }
    ranges += if (start == end) "$start" else "$start-$end"
    return ranges.joinToString(",")
}
