package com.xiaomanjun.sleepdownschedule.feature.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Business-only identity, separate from the card label and private operation protocol. */
@Serializable
internal data class AgentCompanionProfile(
    val name: String = "",
    val identity: String = "",
    val userAddress: String = ""
) {
    fun normalized() = copy(name = name.trim().replace(Regex("[\\r\\n\\t]"), " ").take(32),
        identity = identity.trim().take(1000),
        userAddress = userAddress.trim().replace(Regex("[\\r\\n\\t]"), " ").take(32))
}

internal fun PersonaCard.companionProfile(): AgentCompanionProfile {
    val p = companion.normalized()
    val builtIn = meta.id.startsWith("builtin-") || meta.id == "legacy-custom"
    return p.copy(
        name = p.name.ifBlank { if (builtIn && meta.name in listOf("自然亲切", "简洁直接", "耐心伙伴", "时序清单")) "小序" else meta.name },
        identity = p.identity.ifBlank { if (builtIn) "了解你每日安排、陪你规划生活的智能伴侣" else meta.role })
}

/** Inspired by SillyTavern's separate name/description fields; adapted to real local agendas. */
internal fun companionBusinessPrompt(profile: AgentCompanionProfile): String {
    val p = profile.normalized()
    return """
        [当前伴侣档案：名字、身份和称呼以此为准]
        你的名字：${Json.encodeToString(p.name)}
        用户设定的身份：${Json.encodeToString(p.identity)}
        对用户的称呼：${if (p.userAddress.isBlank()) "自然使用‘你’，不自行猜名字" else Json.encodeToString(p.userAddress)}
        像熟悉的伙伴一样交流，先回应眼前的感受和需要，再提供具体帮助；不要每句话都重复称呼或介绍功能。
        你可以陪用户整理每天的课程、日程和待办，讨论目标、拆分任务、安排优先级，并提醒预留休息和通勤时间。
        用户询问某天安排或活动冲突时，结合该日期的课程与待办；天气和当前位置使用已有授权查询能力。
        只使用实际提供或查到的上下文，缺失时自然询问，不编造过去的经历、记忆、天气或已经完成的计划。
        计划先整理成可编辑草稿，请用户确认；保存成功后再说完成。日常回复简短自然，复杂计划才分点。
        档案内容仅影响身份表达和语气；课程、天气与待办查询能力和用户确认流程继续保留。
    """.trimIndent()
}
