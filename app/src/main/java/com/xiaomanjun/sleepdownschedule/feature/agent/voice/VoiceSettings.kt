package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.content.Context
import com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettingsStore

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
    val ttsVoice: String = "Serena"
) {
    val transcriptionOnly get() = platform == VoicePlatform.ALIYUN && AliyunVoiceCatalog.transcriptionOnly(model)
    val availableVoices get() = if (platform == VoicePlatform.ALIYUN) AliyunVoiceCatalog.voices(model) else platform.voices
    fun normalizedVoice(): VoiceSettings = if (voice in availableVoices) this else copy(voice = availableVoices.first())
    fun withModel(next: String): VoiceSettings = copy(model = next).normalizedVoice()
    val configured get() = platform == VoicePlatform.SYSTEM || apiKey.isNotBlank()
    fun validated(): VoiceSettings {
        require(model in platform.models) { "请选择该平台支持的语音模型" }
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
            prefs.getString("${platform.name}.model", null)?.takeIf { it in platform.models } ?: platform.models.first(),
            prefs.getString("${platform.name}.voice", null) ?: platform.voices.first(),
            prefs.getString("${platform.name}.key", null)?.let { AiImportSettingsStore.decrypt(context, it) }.orEmpty(),
            prefs.getString("${platform.name}.region", "cn-beijing").orEmpty(),
            prefs.getString("${platform.name}.workspace", "").orEmpty(), prefs.getBoolean("read_aloud", true),
            prefs.getString("ALIYUN.tts_model", "qwen3-tts-flash").orEmpty(), prefs.getString("ALIYUN.tts_voice", "Serena").orEmpty()).normalizedVoice()
    }

    fun profile(context: Context, platform: VoicePlatform): VoiceSettings {
        val prefs = context.getSharedPreferences("scheduleplus_voice", Context.MODE_PRIVATE)
        return VoiceSettings(platform,
            prefs.getString("${platform.name}.model", null)?.takeIf { it in platform.models } ?: platform.models.first(),
            prefs.getString("${platform.name}.voice", null) ?: platform.voices.first(),
            prefs.getString("${platform.name}.key", null)?.let { AiImportSettingsStore.decrypt(context, it) }.orEmpty(),
            prefs.getString("${platform.name}.region", "cn-beijing").orEmpty(),
            prefs.getString("${platform.name}.workspace", "").orEmpty(), prefs.getBoolean("read_aloud", true),
            prefs.getString("ALIYUN.tts_model", "qwen3-tts-flash").orEmpty(), prefs.getString("ALIYUN.tts_voice", "Serena").orEmpty()).normalizedVoice()
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
            .putString("${settings.platform.name}.workspace", settings.workspace.trim()).commit()) { "语音配置保存失败" }
    }
}
