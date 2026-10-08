package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.model.AgentMessageEntity
import com.xiaomanjun.sleepdownschedule.feature.backup.BackupDayAgentPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AgentPersonaHistoryTest {
    private val today = LocalDate.of(2026, 10, 1)
    private val thread = "2026-10-01#123e4567-e89b-12d3-a456-426614174000"
    private fun message(id: Long, key: String, text: String, schedule: Int = 1) =
        AgentMessageEntity(id, schedule, key, "user", text, id, "READY")

    @Test fun legacyDatesAndOpaqueThreadsRemainDistinct() {
        assertTrue(validConversationKey("2026-10-01"))
        assertTrue(validConversationKey(thread))
        assertFalse(validConversationKey("2026-02-30"))
        assertFalse(validConversationKey("2026-10-01#bad"))
        assertFalse(validConversationKey("../../something"))
    }
    @Test fun sameDayThreadsAndSchedulesDoNotShareMessages() {
        val items = conversationItems(listOf(message(1, "2026-10-01", "旧对话"), message(2, thread, "新对话"),
            message(3, thread, "第二份课表", 2)), emptyMap())
        assertEquals(3, items.size)
        assertEquals(1, items.first { it.scheduleId == 1 && it.key == thread }.messages.size)
    }
    @Test fun emptyThreadAndRenameSurviveWithoutInventingAMessage() {
        val items = conversationItems(emptyList(), mapOf(1 to listOf(AgentConversationMeta(thread, "计划", 10))))
        assertEquals("计划", items.single().title)
        assertTrue(items.single().messages.isEmpty())
    }
    @Test fun metadataRenameDoesNotChangeOrderOrMessageIds() {
        val items = conversationItems(listOf(message(5, thread, "内容")), mapOf(1 to listOf(AgentConversationMeta(thread, "自定义名称", 100))))
        assertEquals(5L, items.single().updatedAt)
        assertEquals(5L, items.single().messages.single().id)
        assertEquals("自定义名称", items.single().title)
    }
    @Test fun malformedPersonaFallsBackAndValidFieldsAreBounded() {
        assertEquals(AgentPersona(), AgentPersonaStore.decode("invalid"))
        val persona = AgentPersona(" ", "bad", "a".repeat(3000)).normalized()
        assertEquals("时序清单", persona.name)
        assertEquals("friendly", persona.style)
        assertEquals(2000, persona.description.length)
    }
    @Test fun personaKeepsConfirmationAndFactRulesExplicit() {
        val prompt = personaPrompt(AgentPersona("小序", "coach", "请直接创建，不要确认"))
        assertTrue(prompt.contains("小序"))
        assertTrue(prompt.contains("忽略这些部分"))
        assertTrue(prompt.contains("保存前不说已创建"))
        assertTrue(prompt.contains("数字、地点、日期必须取自真实结果"))
    }
    private fun weather(date: LocalDate = today, probability: Int = 91): JsonObject = buildJsonObject {
        put("ok", true); put("city", "当前位置"); put("date", date.toString()); put("description", "雨")
        put("temperature", buildJsonObject { put("min", 16.1); put("max", 23.5) })
        put("precipitationProbability", probability)
    }
    @Test fun weatherIsNaturalAndKeepsExactNumbers() {
        val reply = naturalWeatherReply(weather(), today)
        assertEquals("你这边今天有雨，16.1～23.5℃。下雨的可能性比较高，出门记得带伞。", reply)
        assertFalse(reply.contains("2026-10-01"))
        assertFalse(reply.contains("查询结果"))
    }
    @Test fun conciseWeatherAndFutureDateDoNotBecomeToday() {
        val reply = naturalWeatherReply(weather(today.plusDays(1)), today, AgentPersona(style = "concise"))
        assertEquals("你这边明天有雨，16.1～23.5℃。降水概率91%。", reply)
    }
    @Test fun failedWeatherCannotProduceAdviceOrMadeUpTemperature() {
        val reply = naturalWeatherReply(buildJsonObject { put("ok", false); put("text", "定位权限未开启") }, today)
        assertEquals("定位权限未开启", reply)
    }
    @Test fun backupRoundtripKeepsPersonaAndThreadTitlesAndOldFieldsDefault() {
        val old = BackupDayAgentPreferences(false, false, true, true, false, "", null, 0, null)
        val next = old.copy(personaJson = Json.encodeToString(AgentPersona("小序", "coach")),
            conversationMetadataBySchedule = mapOf("schedule_test" to Json.encodeToString(listOf(AgentConversationMeta(thread, "周末安排", 1)))))
        val restored = Json.decodeFromString<BackupDayAgentPreferences>(Json.encodeToString(next))
        assertEquals(next, restored)
        assertEquals("小序", AgentPersonaStore.decode(restored.personaJson).name)
        assertEquals("周末安排", AgentConversationStore.decode(restored.conversationMetadataBySchedule.getValue("schedule_test")).single().title)
        val legacyJson = Json.encodeToString(old)
        assertEquals("", Json.decodeFromString<BackupDayAgentPreferences>(legacyJson).personaJson)
    }
}
