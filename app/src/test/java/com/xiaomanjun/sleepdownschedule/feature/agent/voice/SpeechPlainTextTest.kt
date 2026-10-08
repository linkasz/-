package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SpeechPlainTextTest {
    @Test fun removesOnlyPresentationAndKeepsFactsInDisplayedOrder() {
        assertEquals("明天的安排\n会议 10:00～11:00\n教室A\n91%", speechPlainText("# 明天的安排\n- **会议** 10:00～11:00\n[教室A](https://example.org)\n`91%`"))
        assertEquals("已创建。开会", speechPlainText("已创建。开会"))
        assertEquals("val x = 10\n", speechPlainText("```kotlin\nval x = 10\n```") + "\n")
    }
    @Test fun officialCatalogHasCompleteCompatibleMetadataAndSeparateTtsDefaults() {
        val base = if (File("src/main/assets").exists()) File(".") else File("app")
        val voices = Json.parseToJsonElement(File(base, "src/main/assets/voices/aliyun-tts.json").readText()).jsonArray
        assertTrue(voices.size >= 40)
        assertEquals(voices.size, voices.map { it.jsonObject["id"] }.distinct().size)
        assertTrue(voices.all { "qwen3-tts-flash" in it.jsonObject["compatibleModels"]!!.jsonArray.map { m -> m.jsonPrimitive.content } })
        assertTrue(voices.any { it.jsonObject["id"]!!.jsonPrimitive.content == "Serena" })
        assertEquals("qwen3-tts-flash", VoiceSettings().ttsModel)
        assertEquals("Serena", VoiceSettings().ttsVoice)
    }
    @Test fun tableSpeechUsesTheSameCellOrderAsTheBubble() {
        assertEquals("事项，时间\n会议，10:00\n作业，12:00", speechPlainText("|事项|时间|\n|---|---|\n|**会议**|10:00|\n|作业|12:00|"))
    }
    @Test fun officialRefreshParsesNonRealtimeSectionInsteadOfTheTocOrOmniTable() {
        val rows = (1..12).joinToString("") { "<tr><td>V$it</td><td>音色名：声音$it 描述：女性<audio src=\"https://help-static-aliyun-doc.aliyuncs.com/a$it.wav\"></audio></td><td>中文、英文</td><td>qwen3-tts-flash</td></tr>" }
        val html = "<a>非实时目录</a><h2>实时</h2><table><tr><td>NotTts</td></tr></table><h2>非实时音色</h2><table>$rows</table>"
        val result = AliyunTtsVoices.parseOfficialTable(html)
        assertEquals(12, result.size); assertEquals("V1", result.first().id)
        assertEquals("女", result.first().gender); assertEquals(listOf("中文", "英文"), result.first().language)
        assertThrows(IllegalArgumentException::class.java) { AliyunTtsVoices.parseOfficialTable("<h2>非实时</h2><table></table>") }
    }
}
