package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import java.util.UUID
import kotlinx.serialization.json.*

/** Protocol differences belong here, not in a URL-only provider switch. No credentials in frames. */
internal class RealtimeVoiceProtocol(val settings: VoiceSettings) {
    val nativeAssistant get() = settings.platform == VoicePlatform.ALIYUN && !transcriptionOnly
    val inputRate = if (settings.platform == VoicePlatform.OPENAI) 24000 else 16000
    val outputRate = 24000
    val transcriptionOnly get() = settings.transcriptionOnly
    // Manual commit permits context refresh before Omni creates its own answer.
    val manualInput get() = settings.platform == VoicePlatform.ALIYUN && !transcriptionOnly
    val url: String get() = when (settings.platform) {
        VoicePlatform.OPENAI -> "wss://api.openai.com/v1/realtime?model=${settings.model}"
        VoicePlatform.ALIYUN -> "wss://${settings.workspace}.${settings.region}.maas.aliyuncs.com/api-ws/v1/realtime?model=${settings.model}"
        VoicePlatform.GLM -> "wss://open.bigmodel.cn/api/paas/v4/realtime"
        else -> error("此平台不使用实时 WebSocket")
    }

    fun event(type: String, fields: JsonObjectBuilder.() -> Unit = {}): String = buildJsonObject {
        put("type", type); put("event_id", UUID.randomUUID().toString())
        if (settings.platform == VoicePlatform.GLM) put("client_timestamp", System.currentTimeMillis())
        fields()
    }.toString()

    fun session(instructions: String = "你是时序清单语音转写接口，仅转录用户音频，不回答或执行操作。"): String = event("session.update") {
        putJsonObject("session") {
            if (transcriptionOnly) {
                putJsonArray("modalities") { add("text") }
                put("input_audio_format", "pcm"); put("sample_rate", inputRate)
                putJsonObject("turn_detection") { put("type", "server_vad"); put("threshold", .2); put("silence_duration_ms", 600) }
                return@putJsonObject
            }
            if (settings.platform != VoicePlatform.ALIYUN) put("model", settings.model)
            put("instructions", instructions)
            // Aliyun uses nested function definitions; other realtime protocols use flat definitions.
            val tools = com.xiaomanjun.sleepdownschedule.feature.agent.agentToolDefinitions()
            put("tools", if (settings.platform == VoicePlatform.ALIYUN) tools else JsonArray(tools.map {
                buildJsonObject { put("type", "function"); (it.jsonObject["function"] as JsonObject).forEach { (k, v) -> put(k, v) } }
            }))
            if (settings.platform != VoicePlatform.ALIYUN) put("tool_choice", "none")
            when (settings.platform) {
                VoicePlatform.OPENAI -> {
                    put("type", "realtime"); putJsonArray("output_modalities") { add("audio") }
                    putJsonObject("audio") {
                        putJsonObject("input") {
                            putJsonObject("format") { put("type", "audio/pcm"); put("rate", inputRate) }
                            putJsonObject("transcription") { put("model", "gpt-4o-mini-transcribe"); put("language", "zh") }
                            putJsonObject("turn_detection") { put("type", "server_vad"); put("silence_duration_ms", 600); put("create_response", false) }
                        }
                        putJsonObject("output") { put("voice", settings.voice)
                            putJsonObject("format") { put("type", "audio/pcm"); put("rate", outputRate) } }
                    }
                }
                VoicePlatform.ALIYUN -> {
                    // Omni answers as text; a dedicated TTS reads the persisted message later.
                    putJsonArray("modalities") { add("text") }
                    putJsonObject("input_audio_transcription") { put("model", "qwen3-asr-flash-realtime") }
                    put("turn_detection", JsonNull)
                    putJsonObject("audio") {
                        putJsonObject("input") { putJsonObject("format") { put("type", "pcm"); put("sample_rate", inputRate) } }
                        putJsonObject("output") {
                            if (settings.model.startsWith("qwen3.8")) put("voice", settings.voice)
                            putJsonObject("format") { put("type", "pcm"); put("sample_rate", outputRate) }
                        }
                    }
                }
                VoicePlatform.GLM -> {
                    put("voice", settings.voice); putJsonArray("modalities") { add("text"); add("audio") }
                    put("input_audio_format", "pcm16"); put("output_audio_format", "pcm")
                    putJsonObject("beta_fields") { put("chat_mode", "audio"); put("tts_source", "e2e"); put("auto_search", false) }
                    putJsonObject("turn_detection") { put("type", "server_vad"); put("create_response", false); put("interrupt_response", true) }
                }
                else -> error("Unsupported native protocol")
            }
        }
    }

    fun append(audio: String) = event("input_audio_buffer.append") { put("audio", audio) }
    fun cancel(responseActive: Boolean): String? = if (!transcriptionOnly && responseActive) event("response.cancel") else null
    fun clear() = event("input_audio_buffer.clear")
    fun commit() = event("input_audio_buffer.commit")
    fun createResponse() = event("response.create")
    fun toolResult(callId: String, text: String) = event("conversation.item.create") { putJsonObject("item") {
        put("type", "function_call_output"); put("call_id", callId); put("output", text)
    } }

    fun parse(raw: String): VoiceEvent {
        val json = Json.parseToJsonElement(raw).jsonObject
        fun value(key: String) = (json[key] as? JsonPrimitive)?.content.orEmpty()
        return when (value("type")) {
            "session.updated" -> VoiceEvent.Ready
            "input_audio_buffer.speech_started" -> VoiceEvent.SpeechStarted
            "input_audio_buffer.speech_stopped" -> VoiceEvent.SpeechStopped
            "conversation.item.input_audio_transcription.text" -> VoiceEvent.PartialTranscript(value("text") + value("stash"))
            "conversation.item.input_audio_transcription.delta" -> VoiceEvent.TranscriptDelta(value("delta"))
            "conversation.item.input_audio_transcription.completed" -> VoiceEvent.Transcript(value("transcript"), value("item_id"))
            "response.audio.delta", "response.output_audio.delta" -> VoiceEvent.Audio(value("delta"))
            "response.audio.done", "response.output_audio.done" -> VoiceEvent.AudioDone
            "response.text.delta", "response.output_text.delta" -> VoiceEvent.TextDelta(value("delta"), value("response_id"))
            "response.text.done", "response.output_text.done" -> VoiceEvent.TextDone(value("text"), value("response_id"))
            "response.audio_transcript.done", "response.output_audio_transcript.done" -> VoiceEvent.TextDone(value("transcript"), value("response_id"))
            "response.created" -> VoiceEvent.ResponseStarted((json["response"] as? JsonObject)?.get("id")?.jsonPrimitive?.content.orEmpty())
            "response.done" -> {
                val response = json["response"] as? JsonObject
                if (response?.get("status")?.jsonPrimitive?.content == "failed") {
                    val failure = (response["status_details"] as? JsonObject)?.get("error") as? JsonObject
                    VoiceEvent.Failure(failure?.get("code")?.jsonPrimitive?.content.orEmpty(),
                        failure?.get("message")?.jsonPrimitive?.content.orEmpty(), response["id"]?.jsonPrimitive?.content.orEmpty())
                } else VoiceEvent.ResponseDone(response?.get("id")?.jsonPrimitive?.content.orEmpty(),
                    (response?.get("output") as? JsonArray)?.any { (it as? JsonObject)?.get("type")?.jsonPrimitive?.content == "function_call" } == true,
                    response?.get("status")?.jsonPrimitive?.content.orEmpty())
            }
            "response.function_call_arguments.done" -> {
                VoiceEvent.Tool(value("call_id"), value("name"), value("arguments"), value("response_id"))
            }
            "error", "conversation.item.input_audio_transcription.failed" -> {
                val failure = json["error"] as? JsonObject
                VoiceEvent.Failure(failure?.get("code")?.jsonPrimitive?.content.orEmpty(),
                    failure?.get("message")?.jsonPrimitive?.content.orEmpty())
            }
            else -> VoiceEvent.Unknown
        }
    }
}

internal sealed interface VoiceEvent {
    data object Ready : VoiceEvent
    data object SpeechStarted : VoiceEvent
    data object SpeechStopped : VoiceEvent
    data class Transcript(val text: String, val itemId: String = "") : VoiceEvent
    data class PartialTranscript(val text: String) : VoiceEvent
    data class TranscriptDelta(val text: String) : VoiceEvent
    data class Audio(val base64: String) : VoiceEvent
    data object AudioDone : VoiceEvent
    data class TextDelta(val text: String, val responseId: String) : VoiceEvent
    data class TextDone(val text: String, val responseId: String) : VoiceEvent
    data class ResponseStarted(val responseId: String) : VoiceEvent
    data class ResponseDone(val responseId: String, val hasTools: Boolean = false, val status: String = "completed") : VoiceEvent
    data class Tool(val callId: String, val name: String, val arguments: String, val responseId: String) : VoiceEvent
    data class Failure(val code: String, val detail: String = "", val responseId: String = "") : VoiceEvent
    data object Unknown : VoiceEvent
}
