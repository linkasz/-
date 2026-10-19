package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.content.Context
import com.xiaomanjun.sleepdownschedule.feature.importing.mergeAiModelIds
import com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettingsStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal enum class VoicePlatform(val label: String, val models: List<String>, val voices: List<String>) {
    SILICON_FLOW("硅基流动（分段语音）", SiliconVoicePreset.entries.map { it.id }, listOf("claire", "anna", "bella", "diana", "alex", "benjamin", "charles", "david")),
    ALIYUN("阿里云百炼", listOf("qwen3-asr-flash-realtime", "qwen3-asr-flash-realtime-2026-02-10", "qwen3-asr-flash-realtime-2025-10-27", "qwen3.8-omni-flash-realtime", "qwen3.5-omni-flash-realtime", "qwen3.5-omni-plus-realtime"), listOf("系统默认")),
    OPENAI("OpenAI", listOf("gpt-realtime-2.1", "gpt-realtime-mini"), listOf("marin", "cedar", "alloy", "coral", "sage")),
    GLM("智谱", listOf("glm-realtime-flash", "glm-realtime-air"), listOf("tongtong", "xiaochen", "female-tianmei", "female-shaonv", "male-qn-daxuesheng", "male-qn-jingying", "lovely_girl")),
    SYSTEM("系统语音（降级）", listOf("Android 语音识别与朗读"), listOf("系统默认"))
}

internal data class VoiceSettings(
    val platform: VoicePlatform = VoicePlatform.ALIYUN,
    val model: String = platform.models.first(),
    val voice: String = if (platform == VoicePlatform.ALIYUN) AliyunVoiceCatalog.voices(model).first() else platform.voices.first(),
    val apiKey: String = "",
    val region: String = "cn-beijing",
    val workspace: String = "",
    val readAloud: Boolean = true,
    val ttsModel: String = "qwen3-tts-flash",
    val ttsVoice: String = "Serena",
    val availableModels: List<String> = emptyList()
) {
    val modelOptions get() = (platform.models + availableModels).distinctBy(String::lowercase)
    val transcriptionOnly get() = platform == VoicePlatform.ALIYUN && AliyunVoiceCatalog.transcriptionOnly(model)
    val availableVoices get() = if (platform == VoicePlatform.ALIYUN) AliyunVoiceCatalog.voices(model) else platform.voices
    fun normalizedVoice(): VoiceSettings = if (voice in availableVoices) this else copy(voice = availableVoices.first())
    fun withModel(next: String): VoiceSettings {
        require(isSupportedVoiceModel(platform, next)) { "该模型不支持当前语音协议" }
        return copy(model = next.trim()).normalizedVoice()
    }
    fun withAddedModels(models: List<String>): VoiceSettings = copy(
        availableModels = mergeAiModelIds(availableModels, models).filter { isSupportedVoiceModel(platform, it) }
    )
    val configured get() = platform == VoicePlatform.SYSTEM || apiKey.isNotBlank()
    fun validated(): VoiceSettings {
        require(model in modelOptions && isSupportedVoiceModel(platform, model)) { "请选择该平台支持的语音模型" }
        require(availableModels.all { isSupportedVoiceModel(platform, it) }) { "已保存的模型与当前语音协议不兼容" }
        require(voice in availableVoices) { "请选择当前模型支持的音色" }
        require(configured) { "请先填写语音平台密钥" }
        if (platform == VoicePlatform.ALIYUN) {
            require(ttsModel == "qwen3-tts-flash" && ttsVoice.isNotBlank() && ttsVoice.length <= 160) { "朗读模型或音色不正确" }
            require(region in listOf("cn-beijing", "ap-southeast-1")) { "请选择有效地域" }
            require(workspace.matches(Regex("[a-zA-Z0-9-]+"))) { "请填写百炼业务空间 ID" }
        }
        return this
    }
}

internal fun isSupportedVoiceModel(platform: VoicePlatform, model: String): Boolean {
    val id = model.trim()
    if (id.isBlank() || id.any(Char::isISOControl)) return false
    return when (platform) {
        VoicePlatform.ALIYUN -> Regex("(?:qwen3-asr-[a-z0-9.-]+-realtime(?:-[0-9]{4}-[0-9]{2}-[0-9]{2})?|qwen3\\.(?:5|8)-omni-[a-z0-9.-]+-realtime(?:-[0-9]{4}-[0-9]{2}-[0-9]{2})?)").matches(id)
        VoicePlatform.OPENAI -> Regex("gpt-realtime(?:-[a-z0-9.-]+)?").matches(id)
        VoicePlatform.GLM -> Regex("glm-realtime(?:-[a-z0-9.-]+)?").matches(id)
        VoicePlatform.SILICON_FLOW -> SiliconVoicePreset.entries.any { it.id == id }
        VoicePlatform.SYSTEM -> id == VoicePlatform.SYSTEM.models.single()
    }
}

private fun readVoiceModels(raw: String?): List<String> = runCatching {
    Json.decodeFromString<List<String>>(raw ?: "[]")
}.getOrDefault(emptyList())

/** Separate preferences deliberately excluded from backups. Keys use the existing Keystore. */
internal object VoiceSettingsStore {
    fun setReadAloud(context: Context, enabled: Boolean) {
        check(context.getSharedPreferences("scheduleplus_voice", Context.MODE_PRIVATE).edit()
            .putBoolean("read_aloud", enabled).commit()) { "朗读设置保存失败" }
    }
    fun load(context: Context): VoiceSettings {
        val prefs = context.getSharedPreferences("scheduleplus_voice", Context.MODE_PRIVATE)
        val platform = VoicePlatform.entries.firstOrNull { it.name == prefs.getString("platform", null) } ?: VoicePlatform.ALIYUN
        return VoiceSettings(platform,
            prefs.getString("${platform.name}.model", null)?.takeIf { isSupportedVoiceModel(platform, it) } ?: platform.models.first(),
            prefs.getString("${platform.name}.voice", null) ?: platform.voices.first(),
            prefs.getString("${platform.name}.key", null)?.let { AiImportSettingsStore.decrypt(context, it) }.orEmpty(),
            prefs.getString("${platform.name}.region", "cn-beijing").orEmpty(),
            prefs.getString("${platform.name}.workspace", "").orEmpty(), prefs.getBoolean("read_aloud", true),
            prefs.getString("ALIYUN.tts_model", "qwen3-tts-flash").orEmpty(), prefs.getString("ALIYUN.tts_voice", "Serena").orEmpty(),
            readVoiceModels(prefs.getString("${platform.name}.models", null))).normalizedVoice()
    }

    fun profile(context: Context, platform: VoicePlatform): VoiceSettings {
        val prefs = context.getSharedPreferences("scheduleplus_voice", Context.MODE_PRIVATE)
        return VoiceSettings(platform,
            prefs.getString("${platform.name}.model", null)?.takeIf { isSupportedVoiceModel(platform, it) } ?: platform.models.first(),
            prefs.getString("${platform.name}.voice", null) ?: platform.voices.first(),
            prefs.getString("${platform.name}.key", null)?.let { AiImportSettingsStore.decrypt(context, it) }.orEmpty(),
            prefs.getString("${platform.name}.region", "cn-beijing").orEmpty(),
            prefs.getString("${platform.name}.workspace", "").orEmpty(), prefs.getBoolean("read_aloud", true),
            prefs.getString("ALIYUN.tts_model", "qwen3-tts-flash").orEmpty(), prefs.getString("ALIYUN.tts_voice", "Serena").orEmpty(),
            readVoiceModels(prefs.getString("${platform.name}.models", null))).normalizedVoice()
    }

    fun save(context: Context, settings: VoiceSettings) {
        settings.validated()
        val encrypted = AiImportSettingsStore.encrypt(context, settings.apiKey.trim())
        check(context.getSharedPreferences("scheduleplus_voice", Context.MODE_PRIVATE).edit()
            .putString("platform", settings.platform.name)
            .putBoolean("read_aloud", settings.readAloud)
            .putString("ALIYUN.tts_model", settings.ttsModel)
            .putString("ALIYUN.tts_voice", settings.ttsVoice)
            .putString("${settings.platform.name}.model", settings.model)
            .putString("${settings.platform.name}.voice", settings.voice)
            .putString("${settings.platform.name}.key", encrypted)
            .putString("${settings.platform.name}.region", settings.region)
            .putString("${settings.platform.name}.workspace", settings.workspace.trim())
            .putString("${settings.platform.name}.models", Json.encodeToString(settings.availableModels)).commit()) { "语音配置保存失败" }
    }
}
