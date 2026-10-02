package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class RealtimeVoiceProtocolTest {
    @Test fun compactMicContinuesListeningAndCanRetryAfterPauseOrFailure() {
        assertEquals(CompactVoiceMicAction.KEEP_LISTENING, compactVoiceMicAction(VoicePhase.LISTENING))
        assertEquals(CompactVoiceMicAction.KEEP_LISTENING, compactVoiceMicAction(VoicePhase.CONNECTING))
        assertEquals(CompactVoiceMicAction.INTERRUPT, compactVoiceMicAction(VoicePhase.SPEAKING))
        assertEquals(CompactVoiceMicAction.INTERRUPT, compactVoiceMicAction(VoicePhase.THINKING))
        assertEquals(CompactVoiceMicAction.START, compactVoiceMicAction(VoicePhase.OFF))
        assertEquals(CompactVoiceMicAction.START, compactVoiceMicAction(VoicePhase.ERROR))
    }
    private fun settings(platform: VoicePlatform) = VoiceSettings(platform, apiKey = "not-a-real-key", workspace = "example")
    @Test fun allNativePlatformsUseTheirOwnSessionProtocol() {
        val open = RealtimeVoiceProtocol(settings(VoicePlatform.OPENAI))
        val ali = RealtimeVoiceProtocol(settings(VoicePlatform.ALIYUN))
        val glm = RealtimeVoiceProtocol(settings(VoicePlatform.GLM))
        assertEquals(24000, open.inputRate); assertEquals(16000, ali.inputRate)
        val openSession = Json.parseToJsonElement(open.session()).jsonObject["session"]!!.jsonObject
        assertEquals("realtime", openSession["type"]!!.jsonPrimitive.content)
        assertTrue(openSession.containsKey("audio")); assertFalse(openSession.containsKey("input_audio_format"))
        val glmFrame = Json.parseToJsonElement(glm.session()).jsonObject
        assertTrue(glmFrame.containsKey("client_timestamp"))
        assertEquals("pcm", glmFrame["session"]!!.jsonObject["output_audio_format"]!!.jsonPrimitive.content)
        assertTrue(glmFrame["session"]!!.jsonObject.containsKey("beta_fields"))
        assertTrue(ali.url.contains("example.cn-beijing.maas.aliyuncs.com"))
        listOf(open, ali, glm).forEach { assertFalse(it.session().contains("not-a-real-key")) }
    }
    @Test fun legacyAndGaAudioEventsAndToolsAreNormalized() {
        val p = RealtimeVoiceProtocol(settings(VoicePlatform.OPENAI))
        assertEquals(VoiceEvent.Audio("abc"), p.parse("""{"type":"response.output_audio.delta","delta":"abc"}"""))
        assertEquals(VoiceEvent.Audio("abc"), p.parse("""{"type":"response.audio.delta","delta":"abc"}"""))
        assertEquals(VoiceEvent.Transcript("今天有课吗"), p.parse("""{"type":"conversation.item.input_audio_transcription.completed","transcript":"今天有课吗"}"""))
        val frame = buildJsonObject { put("type", "response.function_call_arguments.done"); put("call_id", "a"); put("name", "ask_schedule_assistant"); put("arguments", """{"question":"查询课程"}""") }
        assertEquals(VoiceEvent.Tool("a", "ask_schedule_assistant", """{"question":"查询课程"}""", ""), p.parse(frame.toString()))
        assertEquals("response.cancel", Json.parseToJsonElement(p.cancel(true)!!).jsonObject["type"]!!.jsonPrimitive.content)
        assertNull(p.cancel(false))
        assertEquals("input_audio_buffer.clear", Json.parseToJsonElement(p.clear()).jsonObject["type"]!!.jsonPrimitive.content)
    }
    @Test fun waveEncodingAndLevelUseRealPcm() {
        val pcm = byteArrayOf(0, 0, 0, 64)
        val wav = pcmWav(pcm, 16000)
        assertEquals("RIFF", wav.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals(48, wav.size); assertEquals(0f, pcmLevel(byteArrayOf(0, 0)), 0f)
        assertTrue(pcmLevel(pcm) > 0f)
    }
    @Test fun invalidAliWorkspaceFailsBeforeConnecting() {
        assertThrows(IllegalArgumentException::class.java) { settings(VoicePlatform.ALIYUN).copy(workspace = "evil/path").validated() }
        assertFalse(VoiceSettings().configured)
        assertTrue(VoiceSettings(VoicePlatform.SYSTEM).configured)
    }
    @Test fun dedicatedRealtimeAsrNeverCreatesAnAssistantResponse() {
        val p = RealtimeVoiceProtocol(settings(VoicePlatform.ALIYUN))
        val session = Json.parseToJsonElement(p.session()).jsonObject["session"]!!.jsonObject
        assertTrue(p.transcriptionOnly)
        assertEquals("pcm", session["input_audio_format"]!!.jsonPrimitive.content)
        assertEquals(16000, session["sample_rate"]!!.jsonPrimitive.int)
        assertEquals(listOf("text"), session["modalities"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertFalse(session.containsKey("tools")); assertFalse(session.containsKey("voice"))
        assertNull(p.cancel(true)); assertNull(p.cancel(false)); assertFalse(p.manualInput)
        assertEquals(VoiceEvent.PartialTranscript("今天有课吗"),
            p.parse("""{"type":"conversation.item.input_audio_transcription.text","text":"今天有课","stash":"吗"}"""))
        assertEquals(VoiceEvent.Transcript("今天有课吗", "turn-1"),
            p.parse("""{"type":"conversation.item.input_audio_transcription.completed","transcript":"今天有课吗","item_id":"turn-1"}"""))
    }
    @Test fun allThreeAsrNamesUseTextOnlySessionsInBothRegions() {
        VoicePlatform.ALIYUN.models.filter(AliyunVoiceCatalog::transcriptionOnly).forEach { model ->
            listOf("cn-beijing", "ap-southeast-1").forEach { region ->
                val config = settings(VoicePlatform.ALIYUN).withModel(model).copy(region = region).validated()
                val p = RealtimeVoiceProtocol(config)
                assertTrue(p.url.endsWith("model=$model")); assertTrue(p.url.contains(".$region."))
                assertFalse(p.session().contains("instructions")); assertFalse(p.session().contains("voice"))
                assertNull(p.cancel(true))
            }
        }
    }
    @Test fun omniAnswersTextAndRegistersAllContextToolsWithoutGeneratingReadback() {
        VoicePlatform.ALIYUN.models.filterNot(AliyunVoiceCatalog::transcriptionOnly).forEach { model ->
            val p = RealtimeVoiceProtocol(settings(VoicePlatform.ALIYUN).withModel(model))
            val session = Json.parseToJsonElement(p.session()).jsonObject["session"]!!.jsonObject
            assertTrue(p.manualInput); assertFalse(p.transcriptionOnly)
            assertEquals(JsonNull, session["turn_detection"])
            assertEquals("qwen3-asr-flash-realtime", session["input_audio_transcription"]!!.jsonObject["model"]!!.jsonPrimitive.content)
            assertFalse(session.containsKey("tool_choice")); assertFalse(session.containsKey("input_audio_format"))
            val audio = session["audio"]!!.jsonObject
            assertEquals(16000, audio["input"]!!.jsonObject["format"]!!.jsonObject["sample_rate"]!!.jsonPrimitive.int)
            assertEquals(24000, audio["output"]!!.jsonObject["format"]!!.jsonObject["sample_rate"]!!.jsonPrimitive.int)
            assertEquals("input_audio_buffer.commit", Json.parseToJsonElement(p.commit()).jsonObject["type"]!!.jsonPrimitive.content)
            assertEquals(listOf("text"), session["modalities"]!!.jsonArray.map { it.jsonPrimitive.content })
            val names = session["tools"]!!.jsonArray.map { it.jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content }
            assertTrue(names.containsAll(listOf("get_my_courses", "get_my_schedule", "get_current_time", "get_current_location")))
            assertEquals(VoiceEvent.ResponseStarted("r1"), p.parse("""{"type":"response.created","response":{"id":"r1"}}"""))
            assertEquals(VoiceEvent.ResponseDone("r1"), p.parse("""{"type":"response.done","response":{"id":"r1","status":"completed"}}"""))
        }
    }
    @Test fun finalTextAndToolCallKeepResponseIdentityAndRawArguments() {
        val p = RealtimeVoiceProtocol(settings(VoicePlatform.ALIYUN).withModel("qwen3.8-omni-flash-realtime"))
        assertEquals(VoiceEvent.TextDelta("明天", "r1"), p.parse("""{"type":"response.text.delta","response_id":"r1","delta":"明天"}"""))
        assertEquals(VoiceEvent.TextDone("明天有课。", "r1"), p.parse("""{"type":"response.text.done","response_id":"r1","text":"明天有课。"}"""))
        assertEquals(VoiceEvent.Tool("c1", "get_my_courses", "{\"date\":\"2026-10-02\"}", "r1"),
            p.parse("""{"type":"response.function_call_arguments.done","response_id":"r1","call_id":"c1","name":"get_my_courses","arguments":"{\"date\":\"2026-10-02\"}"}"""))
        assertEquals(VoiceEvent.ResponseDone("r1", true), p.parse("""{"type":"response.done","response":{"id":"r1","status":"completed","output":[{"type":"function_call"}]}}"""))
        val tools = Json.parseToJsonElement(RealtimeVoiceProtocol(settings(VoicePlatform.OPENAI)).session()).jsonObject["session"]!!.jsonObject["tools"]!!.jsonArray
        assertTrue(tools.all { it.jsonObject["name"] != null && it.jsonObject["function"] == null })
        assertEquals(VoiceEvent.Failure("model_not_found", "", "old-response"), p.parse(
            """{"type":"response.done","response":{"id":"old-response","status":"failed","status_details":{"error":{"code":"model_not_found"}}}}"""))
    }
    @Test fun modelSwitchNormalizesOnlyIncompatibleVoicesAndPreservesCredentials() {
        val original = settings(VoicePlatform.ALIYUN).withModel("qwen3.5-omni-flash-realtime").copy(voice = "Harvey", readAloud = false)
        assertEquals(21, original.availableVoices.size)
        assertEquals("Harvey", original.normalizedVoice().voice)
        val updated = original.withModel("qwen3.8-omni-flash-realtime")
        assertEquals(27, updated.availableVoices.size); assertEquals("Tina", updated.voice)
        assertEquals(original.apiKey, updated.apiKey); assertEquals(original.workspace, updated.workspace)
        assertFalse(updated.readAloud)
        assertEquals("系统默认", original.withModel("qwen3-asr-flash-realtime").voice)
        assertEquals("Tina", original.copy(voice = "Chelsie").normalizedVoice().voice)
        assertThrows(IllegalArgumentException::class.java) { original.copy(voice = "Chelsie").validated() }
        assertTrue(AliyunVoiceCatalog.label("Harvey").contains("厚"))
    }
    @Test fun serviceErrorsHaveActionableContextWithoutEchoingSecretsOrTranscripts() {
        val settings = settings(VoicePlatform.ALIYUN)
        val p = RealtimeVoiceProtocol(settings)
        val event = p.parse("""{"type":"error","error":{"code":"invalid_request_error","message":"Unsupported voice; secret not-a-real-key; 用户内容"}}""") as VoiceEvent.Failure
        val message = voiceFailureMessage(event, settings)
        assertTrue(message.contains("音色")); assertTrue(message.contains(settings.model))
        assertTrue(message.contains("北京")); assertTrue(message.contains("invalid_request_error"))
        assertFalse(message.contains(settings.apiKey)); assertFalse(message.contains("用户内容"))
        assertTrue(voiceFailureMessage(VoiceEvent.Failure("http_403"), settings).contains("无权"))
        assertTrue(voiceFailureMessage(VoiceEvent.Failure("http_429"), settings).contains("额度"))
        assertFalse(voiceFailureMessage(VoiceEvent.Failure("sk-secret-key"), settings).contains("sk-secret-key"))
        assertEquals(VoiceEvent.Failure("model_not_found", "Unknown model"),
            p.parse("""{"type":"response.done","response":{"status":"failed","status_details":{"error":{"code":"model_not_found","message":"Unknown model"}}}}"""))
    }
    @Test fun readAloudIsAnIndependentDefaultEnabledOption() {
        assertTrue(VoiceSettings().readAloud)
        assertFalse(VoiceSettings().copy(readAloud = false).readAloud)
        assertEquals(4, SiliconVoicePreset.entries.size)
        SiliconVoicePreset.entries.forEach { preset ->
            val payload = siliconSpeechPayload(VoiceSettings(VoicePlatform.SILICON_FLOW, model = preset.id), "今天没有课程")
            assertEquals(preset.tts, payload["model"]!!.jsonPrimitive.content)
            assertEquals(preset.id, SiliconVoicePreset.resolve(preset.id).id)
            assertEquals(if (preset.tts.startsWith("fnlp/MOSS")) "[S1]今天没有课程" else "今天没有课程",
                payload["input"]!!.jsonPrimitive.content)
        }
    }
    @Test fun actionOnlyAnswersKeepConfirmationReachable() {
        assertEquals("今天没有课程", voiceReplyText("今天没有课程", false))
        assertEquals("已拟好内容，请查看并确认。", voiceReplyText("", true))
        assertEquals("建议调整时间", voiceReplyText("建议调整时间", true))
    }
}
