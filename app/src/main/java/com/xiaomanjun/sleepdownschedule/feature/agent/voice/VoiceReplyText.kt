package com.xiaomanjun.sleepdownschedule.feature.agent.voice

/** Action-only replies still expose the existing confirmation UI; never read operation JSON. */
internal fun voiceReplyText(displayText: String, hasActions: Boolean): String =
    if (hasActions && displayText.isBlank()) "已拟好内容，请查看并确认。"
    else displayText

/** Strip presentation syntax only; keep words/numbers from the displayed message unchanged. */
internal fun speechPlainText(displayText: String): String =
    com.xiaomanjun.sleepdownschedule.feature.agent.agentMarkdownPlainText(displayText)
