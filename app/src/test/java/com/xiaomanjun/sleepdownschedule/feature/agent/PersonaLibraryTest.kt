package com.xiaomanjun.sleepdownschedule.feature.agent

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PersonaLibraryTest {
    private val cards = listOf(PersonaCard(PersonaMeta("builtin-friendly", "自然亲切"), "亲切"),
        PersonaCard(PersonaMeta("builtin-concise", "简洁直接"), "简洁", enabled = false),
        PersonaCard(PersonaMeta("custom", "用户助理"), "自定义业务风格"))
    @Test fun disablingDefaultChoosesAnEnabledBuiltInAndLeavesOtherPreferencesIntact() {
        val lib = PersonaLibrary(cards, "custom").validated().toggle("custom")
        assertEquals("builtin-friendly", lib.defaultId)
        assertFalse(lib.cards.single { it.meta.id == "custom" }.enabled)
        val next = lib.toggle("builtin-friendly")
        assertEquals("builtin-concise", next.defaultId)
        assertEquals(1, next.cards.count { it.isDefault })
        assertTrue(next.cards.single { it.isDefault }.enabled)
    }
    @Test fun previewAndSerializedBackupHaveNoPrivateFields() {
        val lib = PersonaLibrary(cards, "custom").validated()
        assertTrue(AgentPersonaRepository.preview(lib.cards.last()).startsWith("自定义业务风格"))
        val objectValue = Json.parseToJsonElement(Json.encodeToString(lib)).jsonObject
        assertEquals(setOf("cards", "defaultId"), objectValue.keys)
        assertTrue(objectValue["cards"]!!.jsonArray.all { "corePrompt" !in it.jsonObject })
    }
    @Test fun malformedLibrariesFailRatherThanResettingUserConfiguration() {
        assertThrows(IllegalArgumentException::class.java) { PersonaLibrary(cards, "absent").validated() }
        assertThrows(IllegalArgumentException::class.java) { PersonaLibrary(cards + cards.first(), "custom").validated() }
        assertThrows(IllegalArgumentException::class.java) { PersonaLibrary(cards.map { it.copy(visiblePrompt = "") }, "custom").validated() }
    }
    @Test fun companionIdentitySurvivesBackupAndOldLibrariesKeepDefaults() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val card = cards.last().copy(companion = AgentCompanionProfile("小序", "陪我整理每天安排的伙伴", "阿林"))
        val library = PersonaLibrary(cards.dropLast(1) + card, "custom").validated()
        val restored = json.decodeFromString<PersonaLibrary>(json.encodeToString(library)).validated()
        assertEquals(card.companion, restored.cards.last().companion)
        val old = json.decodeFromString<PersonaCard>("""{"meta":{"id":"builtin-friendly","name":"自然亲切"},"visiblePrompt":"亲切"}""")
        assertEquals("小序", old.companionProfile().name)
        assertTrue(old.companionProfile().identity.contains("智能伴侣"))
        assertEquals("", old.companionProfile().userAddress)
    }
    @Test fun previewIncludesEditableIdentityAndWhitelistedBackupFieldsOnly() {
        val card = cards.last().copy(companion = AgentCompanionProfile("小序", "学习搭子", "同学"))
        val preview = AgentPersonaRepository.preview(card)
        assertTrue(preview.contains("小序")); assertTrue(preview.contains("学习搭子")); assertTrue(preview.contains("同学"))
        assertTrue(preview.contains("课程")); assertTrue(preview.contains("天气")); assertTrue(preview.contains("用户确认"))
        val encoded = Json { encodeDefaults = true }.encodeToString(card)
        assertFalse(encoded.contains("corePrompt"))
        assertEquals(setOf("name", "identity", "userAddress"),
            Json.parseToJsonElement(encoded).jsonObject.getValue("companion").jsonObject.keys)
    }
    @Test fun identityFieldsAreBoundedAndKeepExistingCardLabelSeparate() {
        val p = AgentCompanionProfile("  小序\n ", "a".repeat(1500), "x".repeat(40)).normalized()
        assertEquals("小序", p.name)
        assertEquals(1000, p.identity.length); assertEquals(32, p.userAddress.length)
        val card = cards.first().copy(companion = p)
        assertEquals("自然亲切", card.meta.name)
        assertEquals("小序", card.companionProfile().name)
    }
    @Test fun companionKeepsCourseAgendaLocationTimeAndWeatherFunctionsRegistered() {
        val names = agentToolDefinitions().map {
            it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content.lowercase()
        }
        assertTrue(names.containsAll(listOf("get_my_courses", "get_my_schedule", "get_current_location", "get_current_time", "weather_quick")))
    }
    @Test fun everySourceHasOneMappingAndCharactersRetainDistinctBusinessPrompts() {
        val base = if (File("src/main/assets").exists()) File(".") else File("app")
        val catalog = Json.parseToJsonElement(File(base, "src/main/assets/personas/catalog.json").readText()).jsonArray
        val map = Json.parseToJsonElement(File(base, "../docs/qa/2026-10-01-persona-source-map.json").readText()).jsonArray
        assertEquals(38, catalog.size); assertEquals(42, map.size)
        assertEquals(42, map.map { it.jsonObject["file"]!!.jsonPrimitive.content }.distinct().size)
        assertEquals(38, catalog.map { it.jsonObject["meta"]!!.jsonObject["id"] }.distinct().size)
        catalog.forEach { item ->
            assertEquals("", item.jsonObject["corePrompt"]!!.jsonPrimitive.content)
            val prompt = item.jsonObject["visiblePrompt"]!!.jsonPrimitive.content
            assertTrue(prompt.contains("角色定位")); assertTrue(prompt.contains("回复长度"))
            assertTrue(prompt.contains("课程表")); assertTrue(prompt.contains("当前位置")); assertTrue(prompt.contains("用户偏好"))
            assertFalse(prompt.contains("核心指令")); assertFalse(prompt.contains("<think>"))
        }
    }
}
