package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class PersonaMeta(val id: String, val name: String, val role: String = "日常智能助理",
    val sourceFiles: List<String> = emptyList(), val preferredSource: String = "")

/** Only this business-only DTO crosses into Compose, clipboard or backup. */
@Serializable
internal data class PersonaCard(val meta: PersonaMeta, val visiblePrompt: String, val enabled: Boolean = true,
    val isDefault: Boolean = false, val companion: AgentCompanionProfile = AgentCompanionProfile())

@Serializable
private data class PersonaDefinition(val meta: PersonaMeta, val visiblePrompt: String, val corePrompt: String)

@Serializable
internal data class PersonaLibrary(val cards: List<PersonaCard>, val defaultId: String) {
    fun toggle(id: String): PersonaLibrary {
        val target = cards.single { it.meta.id == id }
        // Disabling the active persona restores the friendly built-in, never an invalid default.
        val fallback = target.enabled && id == defaultId
        val fallbackId = if (id != "builtin-friendly") "builtin-friendly" else "builtin-concise"
        return copy(defaultId = if (fallback) fallbackId else defaultId,
            cards = cards.map { when {
                it.meta.id == id -> it.copy(enabled = !target.enabled)
                fallback && it.meta.id == fallbackId -> it.copy(enabled = true)
                else -> it
            } }).validated()
    }
    fun validated(): PersonaLibrary {
        require(cards.isNotEmpty() && cards.size <= 100) { "人格数量不正确" }
        require(cards.map { it.meta.id }.distinct().size == cards.size) { "人格标识重复" }
        require(cards.all { it.meta.id.matches(Regex("[a-zA-Z0-9-]{1,64}")) && it.meta.name.isNotBlank() &&
            it.meta.name.length <= 32 && it.visiblePrompt.length in 1..12000 }) { "人格内容不正确" }
        require(cards.any { it.meta.id == defaultId && it.enabled }) { "默认人格必须启用" }
        return copy(cards = cards.map { it.copy(isDefault = it.meta.id == defaultId, companion = it.companion.normalized()) })
    }
}

internal object AgentPersonaRepository {
    private val revision = kotlinx.coroutines.flow.MutableStateFlow(0)
    val changes: kotlinx.coroutines.flow.StateFlow<Int> = revision
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private const val KEY = "persona_library_v1"
    // Private persona cores are not part of the public distribution.
    private const val CORE = ""


    private fun definitions(context: Context): List<PersonaDefinition> = context.assets.open("personas/catalog.json")
        .bufferedReader().use { json.decodeFromString(it.readText()) }

    fun load(context: Context): PersonaLibrary {
        val stored = context.getSharedPreferences("day_agent_preferences", 0).getString(KEY, null)
        if (stored != null) return json.decodeFromString<PersonaLibrary>(stored).validated()
        val legacy = AgentPersonaStore.load(context)
        val presets = AgentPersona.styles.map { (id, label) ->
            PersonaCard(PersonaMeta("builtin-$id", label), visiblePersonaPrompt(AgentPersona(style = id)))
        }
        val custom = PersonaCard(PersonaMeta("legacy-custom", legacy.name, "原有自定义人格"), visiblePersonaPrompt(legacy))
        val library = PersonaLibrary(presets + custom + definitions(context).map { PersonaCard(it.meta, it.visiblePrompt) }, custom.meta.id).validated()
        save(context, library)
        return library
    }

    fun save(context: Context, library: PersonaLibrary) {
        // Serialize an explicit whitelist, never the private definition or system prompt.
        val value = json.encodeToString(library.validated())
        check(context.getSharedPreferences("day_agent_preferences", 0).edit().putString(KEY, value).commit()) { "人格保存失败" }
        revision.value += 1
    }

    fun preview(card: PersonaCard): String = card.visiblePrompt + "\n\n" + companionBusinessPrompt(card.companionProfile())
    fun activeCard(context: Context): PersonaCard = load(context).let { library ->
        library.cards.single { it.meta.id == library.defaultId }
    }
    fun export(context: Context): String = json.encodeToString(load(context).validated())
    fun restore(context: Context, raw: String) {
        if (raw.isBlank()) {
            context.getSharedPreferences("day_agent_preferences", 0).edit { remove(KEY) }
            revision.value += 1
        } else save(context, json.decodeFromString<PersonaLibrary>(raw))
    }

    /** Provider-only assembly. Never return this from preview or preferences APIs. */
    fun systemPrompt(context: Context): String {
        val library = load(context)
        val card = library.cards.single { it.meta.id == library.defaultId }
        val privateCore = definitions(context).firstOrNull { it.meta.id == card.meta.id }?.corePrompt ?: CORE
        return "$privateCore\n$CORE\n[用户可编辑的表达风格]\n${preview(card)}"
    }
}

/** Visible adaptation contains expression and product context, no internal system protocol. */
internal fun visiblePersonaPrompt(persona: AgentPersona): String {
    val p = persona.normalized()
    val style = when (p.style) {
        "concise" -> "简洁直接，先说结论，减少寒暄。"
        "coach" -> "温和耐心，先回应问题，再给一个具体建议，不说教。"
        else -> "自然亲切，用短句，像熟悉的日常助理一样交谈。"
    }
    return "角色定位：${p.name}，时序清单的日常安排伙伴。\n语气：$style\n长度：日常1～3句，复杂安排才分点；可用简洁Markdown，不强行加emoji。\n" +
        "能力：查询课程、日程、待办、已授权位置和当前时间；整理操作草稿，由用户确认。缺少上下文时询问，不虚构事实。\n" +
        "活动查询同时查看课程和日程，未提供活动时段时询问，不断言无冲突。\n直接回应需求，不报字段或工具名，不使用动作旁白、内心独白和好感度。\n自定义表达描述：${p.description}"
}
