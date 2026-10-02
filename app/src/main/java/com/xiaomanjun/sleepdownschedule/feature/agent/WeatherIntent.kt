package com.xiaomanjun.sleepdownschedule.feature.agent

import java.time.LocalDate

/** Conservative fast route: only a standalone weather question, never consume a compound task. */
internal fun quickWeatherIntent(question: String, today: LocalDate): Pair<String?, LocalDate>? {
    var text = question.trim().trimEnd('?', '？', '。', '!', '！')
    if (!text.contains("天气")) return null
    text = text.removePrefix("请").removePrefix("帮我").removePrefix("查一下").removePrefix("查询").removePrefix("看看").removePrefix("现在")
    val offset = when { text.contains("后天") -> 2L; text.contains("明天") -> 1L; else -> 0L }
    text = text.replace("今天", "").replace("明天", "").replace("后天", "")
    val ending = Regex("天气(?:怎么样|怎样|如何|情况|预报|好吗)?(?:呀|呢|啊|吧)?$")
    // Unknown question wording goes to the assistant, never into the city geocoder.
    if (!ending.containsMatchIn(text)) return null
    text = text.replace(ending, "").trim().trimEnd('的')
    if (text in setOf("这里", "我这里", "本地", "当前位置", "我这边", "这边")) text = ""
    if (text.contains("天气") || text.any { it in "你我什么怎么" }) return null
    if (text.isNotBlank() && !Regex("[\\p{IsHan}]{2,12}").matches(text)) return null
    if (text.any { it in "和还帮要会课" }) return null
    return text.takeIf { it.isNotBlank() } to today.plusDays(offset)
}
