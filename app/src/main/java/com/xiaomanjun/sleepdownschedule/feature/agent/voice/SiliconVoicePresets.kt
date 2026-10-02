package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.serialization.json.*

/** Official file-ASR + streaming-PCM pairs; these are not native realtime WebSockets. */
internal enum class SiliconVoicePreset(val asr: String, val tts: String, val label: String) {
    SENSE_COSY("FunAudioLLM/SenseVoiceSmall", "FunAudioLLM/CosyVoice2-0.5B", "SenseVoice · CosyVoice"),
    TELE_COSY("TeleAI/TeleSpeechASR", "FunAudioLLM/CosyVoice2-0.5B", "TeleSpeech · CosyVoice"),
    SENSE_MOSS("FunAudioLLM/SenseVoiceSmall", "fnlp/MOSS-TTSD-v0.5", "SenseVoice · MOSS"),
    TELE_MOSS("TeleAI/TeleSpeechASR", "fnlp/MOSS-TTSD-v0.5", "TeleSpeech · MOSS");

    val id get() = "$asr + $tts"
    companion object {
        fun resolve(id: String) = entries.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("请选择有效的硅基流动语音组合")
    }
}

internal fun siliconSpeechPayload(settings: VoiceSettings, text: String): JsonObject {
    val preset = SiliconVoicePreset.resolve(settings.model)
    return buildJsonObject {
        put("model", preset.tts)
        // MOSS accepts a dialogue script; one S1 turn reads the validated assistant answer.
        put("input", if (preset.tts.startsWith("fnlp/MOSS")) "[S1]$text" else text)
        put("voice", "${preset.tts}:${settings.voice}")
        put("response_format", "pcm"); put("sample_rate", 24000); put("stream", true)
    }
}
