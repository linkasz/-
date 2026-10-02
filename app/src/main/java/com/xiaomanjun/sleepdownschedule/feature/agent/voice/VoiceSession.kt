package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

internal enum class VoicePhase(val label: String) { OFF("语音已关闭"), CONNECTING("连接中"), LISTENING("正在聆听"), THINKING("正在思考"), QUERYING("正在查询"), SPEAKING("正在回答"), ERROR("语音未连接") }
internal data class VoiceSessionState(val phase: VoicePhase = VoicePhase.OFF, val level: Float = 0f, val transcript: String = "", val error: String? = null,
    val startedAtMillis: Long = 0L, val canRetryReadback: Boolean = false) {
    val active get() = phase !in listOf(VoicePhase.OFF, VoicePhase.ERROR)
}

private val voiceHttp = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS).build()

internal suspend fun Call.awaitVoice(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(IOException("语音网络连接失败")) }
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) { _, result, _ -> result.close() } else response.close()
        }
    })
}

/** Per-overlay session, no Activity retained by a process singleton. */
internal class VoiceSession(
    private val context: Context,
    private val scope: CoroutineScope,
    private val nativeBridge: NativeVoiceBridge? = null,
    private val onNativeQuestion: (String) -> Unit = {},
    private val askAssistant: suspend (String, String) -> String
) {
    private val mutable = MutableStateFlow(VoiceSessionState())
    val state = mutable.asStateFlow()
    private val audio = VoiceAudio(context, scope)
    private var sessionJob: Job? = null
    private var turnJob: Job? = null
    private var websocket: WebSocket? = null
    private var protocol: RealtimeVoiceProtocol? = null
    private var responseActive = false
    private var nativeReplyRequested = false
    private var manualTurn: ManualVoiceTurn? = null
    private var settings = VoiceSettings()
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    @Volatile private var synthesisCall: Call? = null
    @Volatile private var synthesisResponse: Response? = null
    private var ttsAnswer: String? = null
    private var lastReadback: String? = null
    @Volatile private var generation = 0L
    @Volatile private var turnGeneration = 0L
    private var pausedSettings: VoiceSettings? = null
    private var replyTraceId: String? = null
    private var seenInput = false
    private var transcriptWait: Job? = null
    private var finalWait: Job? = null
    private var pendingFinal: String? = null
    private val finalDeduplicator = VoiceFinalDeduplicator(android.os.SystemClock::elapsedRealtime)
    private val submittedItems = LinkedHashSet<String>()
    private var listeningFrameBytes = ByteArrayOutputStream()
    private var voicedFrames = 0
    private var quietFrames = 0
    private var preRoll = ArrayDeque<ByteArray>()
    private val nativeResponses = NativeResponseTracker()
    private val nativeTools = LinkedHashSet<String>()
    private val nativeText = StringBuilder()
    private var nativeRounds = 0
    private var nativeResponseHadTools = false
    private var nativeRepairAttempts = 0
    private var nativeInstructions = ""

    fun start(next: VoiceSettings) {
        stop()
        val current = ++generation
        settings = next
        mutable.value = VoiceSessionState(VoicePhase.CONNECTING, startedAtMillis = android.os.SystemClock.elapsedRealtime())
        Log.i("ShixuVoice", "session=$current connect_start")
        sessionJob = scope.launch {
            try {
                next.validated()
                when (next.platform) {
                    VoicePlatform.SYSTEM -> startSystem()
                    VoicePlatform.SILICON_FLOW -> {
                        audio.start(16000, { frame, level ->
                            scope.launch { if (generation == current) processSegment(frame, level) }
                        }, { if (current == generation) stop("音频焦点已转移，语音已停止") }, { if (current == generation) stop("麦克风读取失败") })
                        mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING)
                    }
                    else -> connectNative(current)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Log.w("ShixuVoice", "session=$current session_failure phase=${mutable.value.phase} type=${error.javaClass.simpleName}")
                if (current == generation) stop(error.message?.takeIf { !it.contains(settings.apiKey) || settings.apiKey.isBlank() } ?: "语音连接失败，请检查配置")
            }
        }
    }

    private suspend fun connectNative(current: Long) {
        val adapter = RealtimeVoiceProtocol(settings); protocol = adapter
        val nativeContext = if (adapter.nativeAssistant) requireNotNull(nativeBridge).prepare(newSession = true) else null
        nativeInstructions = nativeContext?.instructions.orEmpty()
        val detector = if (adapter.manualInput) ManualVoiceTurn(adapter.inputRate) else null
        manualTurn = detector
        val events = Channel<VoiceEvent>(64)
        websocket = voiceHttp.newWebSocket(Request.Builder().url(adapter.url)
            .apply { if (settings.platform == VoicePlatform.ALIYUN) header("OpenAI-Beta", "realtime=v1") }
            .header("Authorization", "Bearer ${settings.apiKey}").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (current == generation) {
                    webSocket.send(if (nativeContext == null) adapter.session() else adapter.session(nativeContext.instructions))
                } else webSocket.cancel()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = runCatching { adapter.parse(text) }.getOrElse { VoiceEvent.Failure("invalid_protocol") }
                if (!events.trySend(event).isSuccess) scope.launch { if (current == generation) stop("语音接收过快，请重试") }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                events.trySend(VoiceEvent.Failure(response?.code?.let { "http_$it" } ?: "network"))
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                events.trySend(VoiceEvent.Failure("closed_$code")); webSocket.close(code, "closed")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { events.close() }
        })
        try {
            val ready = withTimeoutOrNull(20_000) {
                while (true) when (val event = events.receive()) {
                    VoiceEvent.Ready -> return@withTimeoutOrNull true
                    is VoiceEvent.Failure -> error(voiceFailureMessage(event, settings))
                    else -> Unit
                }
            }
            check(ready == true) { "实时语音连接超时，请检查网络、模型及地域" }
            audio.start(adapter.inputRate, { frame, level ->
                if (current == generation) {
                    // A dedicated ASR session must not transcribe its own system TTS output.
                    if (mutable.value.phase == VoicePhase.SPEAKING) return@start
                    // During readback the microphone is muted to prevent self-transcription;
                    // the visible interrupt button can immediately restore listening.
                    mutable.update { if (it.phase == VoicePhase.SPEAKING) it else it.copy(level = level) }
                    val socket = websocket
                    if (socket != null && (socket.queueSize() > 512_000 || !socket.send(adapter.append(Base64.encodeToString(frame, Base64.NO_WRAP))))) {
                        scope.launch { if (current == generation) stop("语音网络发送受阻") }
                        return@start
                    }
                    // Commit a real voiced segment; context is refreshed before response.create.
                    detector?.feed(level, frame.size)?.let { action ->
                        if (action.started && !events.trySend(VoiceEvent.SpeechStarted).isSuccess)
                            scope.launch { if (current == generation) stop("语音接收过快，请重试") }
                        if (action.clear) socket?.send(adapter.clear())
                        if (action.commit) {
                            if (socket?.send(adapter.commit()) != true || !events.trySend(VoiceEvent.SpeechStopped).isSuccess)
                                scope.launch { if (current == generation) stop("语音提交失败，请重试") }
                        }
                    }
                }
            }, { if (current == generation) stop("音频焦点已转移，语音已停止") }, { if (current == generation) stop("麦克风不可用") })
            mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING)
            for (event in events) {
                if (current != generation) break
                when (event) {
                    VoiceEvent.SpeechStarted -> {
                        transcriptWait?.cancel()
                        seenInput = false
                        mutable.value = mutable.value.copy(transcript = "")
                        // Do not cancel a pending answer just because ASR emits another start.
                        // A distinct final transcript can replace the turn after debouncing.
                        if (mutable.value.phase == VoicePhase.SPEAKING || adapter.nativeAssistant && mutable.value.phase in listOf(VoicePhase.THINKING, VoicePhase.QUERYING)) interrupt()
                    }
                    VoiceEvent.SpeechStopped -> {
                        transcriptWait?.cancel()
                        transcriptWait = scope.launch {
                            delay(20_000)
                            if (current == generation && !seenInput) stop("平台未返回语音转录，请检查模型是否支持输入转写或网络是否正常")
                        }
                    }
                    is VoiceEvent.PartialTranscript -> mutable.value = mutable.value.copy(transcript = event.text)
                    is VoiceEvent.TranscriptDelta -> mutable.value = mutable.value.copy(transcript = mutable.value.transcript + event.text)
                    is VoiceEvent.Transcript -> if (event.text.isNotBlank() &&
                        (if (event.itemId.isBlank()) !seenInput else submittedItems.add(event.itemId))) {
                        transcriptWait?.cancel()
                        if (submittedItems.size > 64) submittedItems.remove(submittedItems.first())
                        seenInput = true; submit(event.text)
                    }
                    is VoiceEvent.Audio -> if (mutable.value.phase == VoicePhase.SPEAKING) {
                        val pcm = Base64.decode(event.base64, Base64.DEFAULT)
                        mutable.value = mutable.value.copy(level = pcmLevel(pcm))
                        audio.play(pcm, { if (current == generation) stop("音频播放失败") })
                    }
                    VoiceEvent.AudioDone -> if (mutable.value.phase == VoicePhase.SPEAKING) {
                        val turn = turnGeneration
                        val token = audio.playbackToken
                        scope.launch { audio.finishPlayback(token); completeSpeech(current, turn) }
                    }
                    is VoiceEvent.TextDelta -> if (acceptNativeEvent(event.responseId)) nativeText.append(event.text)
                    is VoiceEvent.TextDone -> if (acceptNativeEvent(event.responseId) && event.text.isNotBlank()) {
                        nativeText.setLength(0); nativeText.append(event.text)
                    }
                    is VoiceEvent.Tool -> if (adapter.nativeAssistant && acceptNativeEvent(event.responseId) && nativeTools.add(event.callId)) {
                        nativeResponseHadTools = true
                        require(nativeTools.size <= 24) { "本轮查询过多，请重新提问" }
                        mutable.value = mutable.value.copy(phase = VoicePhase.QUERYING)
                        val toolTurn = turnGeneration
                        val result = requireNotNull(nativeBridge).tool(event.name, event.arguments, event.callId, replyTraceId.orEmpty(), toolTurn)
                        if (toolTurn != turnGeneration || current != generation) continue
                        check(websocket?.send(adapter.toolResult(event.callId, result)) == true) { "工具结果发送失败" }
                    }
                    is VoiceEvent.ResponseStarted -> {
                        if (adapter.nativeAssistant && nativeResponses.seen(event.responseId)) continue
                        responseActive = true
                        if (adapter.nativeAssistant) {
                            if (!nativeResponses.started(event.responseId, turnGeneration)) {
                                websocket?.send(adapter.event("response.cancel")); responseActive = false
                            } else {
                                nativeText.setLength(0); nativeResponseHadTools = false
                            }
                            continue
                        }
                        // Also covers an interrupted request whose response.created arrived late.
                        if (!nativeReplyRequested) cancelNativeResponse()
                    }
                    is VoiceEvent.ResponseDone -> {
                        if (adapter.nativeAssistant) {
                            if (!nativeResponses.consume(event.responseId, turnGeneration)) continue
                            responseActive = false
                            if (event.status == "cancelled") { nativeText.setLength(0); continue }
                            if (event.hasTools || nativeResponseHadTools) {
                                require(++nativeRounds <= 8) { "查询未能收敛，请明确日期后重试" }
                                nativeResponses.enqueue(turnGeneration)
                                check(websocket?.send(adapter.createResponse()) == true) { "语音连接已关闭" }
                            } else {
                                val turn = turnGeneration
                                val text = nativeText.toString()
                                turnJob = scope.launch {
                                    try {
                                        val display = requireNotNull(nativeBridge).finish(text, turn)
                                        if (current == generation && turn == turnGeneration) {
                                            if (settings.readAloud) speakReply(display, current, turn)
                                            else mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING, level = 0f)
                                        }
                                    } catch (e: CancellationException) { throw e }
                                    catch (e: Exception) {
                                        if (current != generation || turn != turnGeneration) return@launch
                                        if (e is com.xiaomanjun.sleepdownschedule.feature.agent.AgentProtocolViolationException && nativeRepairAttempts++ == 0) {
                                            // Reformat once using the same read facts; no tool retry or execution is allowed.
                                            websocket?.send(adapter.event("session.update") { putJsonObject("session") {
                                                put("instructions", nativeInstructions + "\n上一条回复未通过本地校验。只修正用户正文与合法待确认草稿，不回显内部内容，不调用工具，不执行操作。")
                                                putJsonArray("tools") { }
                                            } })
                                            nativeResponses.enqueue(turn)
                                            if (websocket?.send(adapter.createResponse()) != true) stop("连接已关闭，请重试；尚未执行操作")
                                        } else stop("这次回复未通过校验或保存失败，请重试；没有执行任何操作")
                                    }
                                }
                            }
                            continue
                        }
                        responseActive = false
                        if (nativeReplyRequested && mutable.value.phase == VoicePhase.SPEAKING) {
                            val turn = turnGeneration
                            val token = audio.playbackToken
                            scope.launch { audio.finishPlayback(token); completeSpeech(current, turn) }
                        }
                        nativeReplyRequested = false
                    }
                    is VoiceEvent.Failure -> if (event.code !in listOf("response_cancel_not_active", "response_cancelled", "response_not_found")) {
                        if (adapter.nativeAssistant && event.responseId.isNotBlank() && !acceptNativeEvent(event.responseId)) continue
                        error(voiceFailureMessage(event, settings))
                    }
                    else -> Unit
                }
            }
            if (current == generation) stop("实时语音连接已结束")
        } finally { events.close() }
    }

    private fun processSegment(frame: ByteArray, level: Float) {
        if (mutable.value.phase != VoicePhase.LISTENING) return
        mutable.value = mutable.value.copy(level = level)
        if (level > 0.07f) {
            if (voicedFrames == 0) { preRoll.forEach { listeningFrameBytes.write(it) }; preRoll.clear() }
            listeningFrameBytes.write(frame); voicedFrames++; quietFrames = 0
        } else if (voicedFrames > 0) { listeningFrameBytes.write(frame); quietFrames++ }
        else { preRoll.addLast(frame); if (preRoll.size > 5) preRoll.removeFirst() }
        if (voicedFrames >= 5 && (quietFrames >= 15 || listeningFrameBytes.size() >= 16000 * 2 * 20)) {
            val pcm = listeningFrameBytes.toByteArray(); resetSegment()
            mutable.value = mutable.value.copy(phase = VoicePhase.THINKING, level = 0f)
            turnJob = scope.launch {
                try {
                    val text = recognize(pcm)
                    if (text.isBlank()) mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING)
                    else { delay(800); if (finalDeduplicator.accept(text)) answer(text)
                        else mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING) }
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { stop("语音识别或合成失败，请检查密钥和网络") }
            }
        } else if (quietFrames > 20) resetSegment()
    }

    private suspend fun recognize(pcm: ByteArray): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", SiliconVoicePreset.resolve(settings.model).asr)
            .addFormDataPart("file", "speech.wav", pcmWav(pcm, 16000).toRequestBody("audio/wav".toMediaType())).build()
        return voiceHttp.newCall(Request.Builder().url("https://api.siliconflow.cn/v1/audio/transcriptions")
            .header("Authorization", "Bearer ${settings.apiKey}").post(body).build()).awaitVoice().use { response ->
            check(response.isSuccessful) { "语音识别请求失败（${response.code}）" }
            Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
        }
    }

    private fun submit(question: String, callId: String? = null) {
        if (pendingFinal == question.trim()) return
        finalWait?.cancel()
        pendingFinal = question.trim()
        finalWait = scope.launch {
            delay(800)
            pendingFinal = null
            if (finalDeduplicator.accept(question)) submitFinal(question, callId)
            else if (settings.platform == VoicePlatform.SYSTEM && mutable.value.phase == VoicePhase.LISTENING) listenSystem()
        }
    }

    private fun submitFinal(question: String, callId: String? = null) {
        cancelReplyTrace()
        turnJob?.cancel(); synthesisCall?.cancel(); synthesisResponse?.close()
        audio.interrupt(); tts?.stop(); ttsAnswer = null; cancelNativeResponse()
        turnJob = scope.launch {
            try { answer(question, callId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { stop("时序清单智能助理处理失败，请检查文字 AI 配置或网络") }
        }
    }

    private suspend fun answer(question: String, callId: String? = null) {
        lastReadback = null
        mutable.value = mutable.value.copy(phase = VoicePhase.THINKING, transcript = question, level = 0f, error = null, canRetryReadback = false)
        val current = generation; val turn = ++turnGeneration
        val traceId = java.util.UUID.randomUUID().toString()
        replyTraceId = traceId
        Log.i("ShixuVoice", "traceId=$traceId request_start")
        if (protocol?.nativeAssistant == true) {
            nativeTools.clear(); nativeRounds = 0; nativeRepairAttempts = 0; nativeText.setLength(0)
            val instructions = requireNotNull(nativeBridge).begin(question, turn)
            if (current != generation || turn != turnGeneration) return
            nativeInstructions = instructions
            onNativeQuestion(question)
            check(websocket?.send(requireNotNull(protocol).session(instructions)) == true) { "语音上下文更新失败" }
            nativeResponses.enqueue(turn)
            check(websocket?.send(requireNotNull(protocol).createResponse()) == true) { "语音连接已关闭" }
            return // Omni supplies its own text answer, without invoking the configured text model.
        }
        if (com.xiaomanjun.sleepdownschedule.feature.agent.quickWeatherIntent(question, java.time.LocalDate.now()) != null) {
            mutable.value = mutable.value.copy(phase = VoicePhase.QUERYING)
        }
        val reply = askAssistant(question, traceId)
        currentCoroutineContext().ensureActive()
        if (current != generation || turn != turnGeneration) return
        if (!settings.readAloud) {
            Log.i("ShixuVoice", "traceId=$traceId reply_end channel=tts enabled=false")
            replyTraceId = null
            mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING, level = 0f)
            if (settings.platform == VoicePlatform.SYSTEM) listenSystem()
            return
        }
        speakReply(reply, current, turn, callId)
    }

    private suspend fun speakReply(reply: String, current: Long, turn: Long, callId: String? = null) {
        lastReadback = reply
        mutable.value = mutable.value.copy(phase = VoicePhase.SPEAKING, error = null, canRetryReadback = false)
        try {
        // The system recognizer must not turn our own readback into a second user request.
        if (settings.platform == VoicePlatform.SYSTEM) runCatching { recognizer?.cancel() }
        Log.i("ShixuVoice", "traceId=$replyTraceId reply_start channel=tts")
        when (settings.platform) {
            VoicePlatform.SILICON_FLOW -> {
                synthesize(speechPlainText(reply))
                audio.finishPlayback()
                completeSpeech(current, turn)
            }
            VoicePlatform.SYSTEM -> {
                initializeSystemTts()
                if (ttsReady) speakSystem(speechPlainText(reply)) else ttsAnswer = speechPlainText(reply)
            }
            VoicePlatform.ALIYUN -> {
                synthesizeAliyun(speechPlainText(reply))
                audio.finishPlayback()
                completeSpeech(current, turn)
            }
            else -> {
                initializeSystemTts()
                if (ttsReady) speakSystem(speechPlainText(reply)) else ttsAnswer = speechPlainText(reply)
            }
        }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { if (current == generation && turn == turnGeneration) readbackFailed() }
    }

    private fun readbackFailed() {
        synthesisCall?.cancel(); runCatching { synthesisResponse?.close() }; synthesisCall = null; synthesisResponse = null
        audio.interrupt(); tts?.stop(); ttsAnswer = null
        mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING, level = 0f,
            error = if (lastReadback != null) "回复已显示，朗读失败。点击此处或语音按钮重试朗读。" else "系统朗读暂时不可用，可继续语音或文字输入。",
            canRetryReadback = lastReadback != null)
        if (settings.platform == VoicePlatform.SYSTEM) runCatching { listenSystem() }
    }

    fun retryReadback() {
        val text = lastReadback ?: return
        if (!mutable.value.canRetryReadback || !mutable.value.active) return
        interrupt()
        val current = generation; val turn = ++turnGeneration
        // Retrying reads the saved reply; it never submits another model question.
        turnJob = scope.launch { speakReply(text, current, turn) }
    }

    fun readConfirmation(text: String) {
        if (!settings.readAloud || !mutable.value.active) return
        interrupt()
        val current = generation
        val turn = ++turnGeneration
        replyTraceId = java.util.UUID.randomUUID().toString()
        turnJob = scope.launch {
            try { speakReply(text, current, turn) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (current == generation) stop("内容已保存，朗读暂时不可用") }
        }
    }

    private suspend fun synthesize(text: String) = withContext(Dispatchers.IO) {
        val current = generation
        val turn = turnGeneration
        val token = audio.playbackToken
        val body = siliconSpeechPayload(settings, text).toString().toRequestBody("application/json".toMediaType())
        val call = voiceHttp.newCall(Request.Builder().url("https://api.siliconflow.cn/v1/audio/speech")
            .header("Authorization", "Bearer ${settings.apiKey}").post(body).build())
        synthesisCall = call
        try { call.awaitVoice().use { response ->
            synthesisResponse = response
            check(response.isSuccessful) { "语音合成失败（${response.code}）" }
            val source = requireNotNull(response.body).source()
            val buffer = okio.Buffer()
            while (isActive) {
                val read = source.read(buffer, 4096)
                if (read < 0) break
                val pcm = buffer.readByteArray()
                if (current != generation || turn != turnGeneration) return@withContext
                mutable.update { it.copy(level = pcmLevel(pcm)) }
                audio.play(pcm, { if (current == generation && turn == turnGeneration) readbackFailed() }, token)
            }
        } } finally { if (synthesisCall === call) { synthesisCall = null; synthesisResponse = null } }
    }

    private suspend fun synthesizeAliyun(text: String) {
        val current = generation; val turn = turnGeneration; val token = audio.playbackToken
        try {
            AliyunTtsClient.stream(settings, text, { synthesisCall = it }, { synthesisResponse = it }) { pcm ->
                if (current == generation && turn == turnGeneration) {
                    mutable.update { it.copy(level = pcmLevel(pcm)) }
                    audio.play(pcm, { if (current == generation && turn == turnGeneration) readbackFailed() }, token)
                }
            }
        } finally { if (current == generation && turn == turnGeneration) { synthesisCall = null; synthesisResponse = null } }
    }

    private fun startSystem() {
        val current = generation
        check(SpeechRecognizer.isRecognitionAvailable(context)) { "系统识别服务不可用，请使用文字输入或配置云端语音" }
        audio.acquireFocus { if (current == generation) stop("音频焦点已转移，语音已停止") }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (generation == current) mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING) }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) { if (generation == current) mutable.value = mutable.value.copy(level = (rmsdB / 12).coerceIn(0f, 1f)) }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                if (generation != current) return
                if (mutable.value.phase !in listOf(VoicePhase.CONNECTING, VoicePhase.LISTENING)) return
                // Silence ends a recognizer turn, not the ongoing voice conversation.
                if (error in listOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) scope.launch {
                    delay(600)
                    if (generation == current && mutable.value.phase == VoicePhase.LISTENING) listenSystem()
                }
                else stop(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "麦克风权限不可用" else "系统语音未识别到内容，请重试或使用文字")
            }
            override fun onResults(results: Bundle?) {
                if (generation != current) return
                if (mutable.value.phase != VoicePhase.LISTENING) return
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) submit(text) else listenSystem()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (generation == current) partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { text -> mutable.update { it.copy(transcript = text) } }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }) }
        if (settings.readAloud) initializeSystemTts()
        listenSystem()
    }

    private fun initializeSystemTts() {
        if (tts != null) return
        val current = generation
        tts = TextToSpeech(context) { status ->
            scope.launch {
                if (generation != current) return@launch
                if (status != TextToSpeech.SUCCESS) { tts?.shutdown(); tts = null; readbackFailed() }
                else {
                    val language = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                    if (language == null || language < TextToSpeech.LANG_AVAILABLE) {
                        tts?.shutdown(); tts = null; readbackFailed()
                        return@launch
                    }
                    ttsReady = true
                    // System TTS follows the same media/loudspeaker policy as cloud playback.
                    tts?.setAudioAttributes(assistantPlaybackAttributes())
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
                            if (generation == current && utteranceId == turnGeneration.toString() && audio != null) {
                                mutable.update { it.copy(level = pcmLevel(audio)) }
                            }
                        }
                        override fun onDone(utteranceId: String?) { scope.launch {
                            if (generation == current && utteranceId == turnGeneration.toString()) {
                                completeSpeech(current, turnGeneration)
                            }
                        } }
                        override fun onError(utteranceId: String?) { scope.launch {
                            if (generation == current && utteranceId == turnGeneration.toString()) readbackFailed()
                        } }
                    })
                    ttsAnswer?.let { speakSystem(it) }; ttsAnswer = null
                }
            }
        }
    }

    private fun speakSystem(answer: String) {
        check(tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, turnGeneration.toString()) == TextToSpeech.SUCCESS) { "系统朗读失败" }
    }

    private fun listenSystem() {
        if (settings.platform != VoicePlatform.SYSTEM || !mutable.value.active) return
        mutable.value = mutable.value.copy(phase = if (mutable.value.phase == VoicePhase.CONNECTING) VoicePhase.CONNECTING else VoicePhase.LISTENING, level = 0f)
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
    }

    private fun resetSegment() { listeningFrameBytes.reset(); voicedFrames = 0; quietFrames = 0; preRoll.clear() }
    private fun completeSpeech(current: Long, turn: Long) {
        if (current != generation || turn != turnGeneration || mutable.value.phase != VoicePhase.SPEAKING) return
        replyTraceId?.let { Log.i("ShixuVoice", "traceId=$it reply_end channel=tts") }
        replyTraceId = null
        mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING, level = 0f, error = null, canRetryReadback = false)
        if (settings.platform == VoicePlatform.SYSTEM) listenSystem()
    }
    private fun cancelReplyTrace() {
        replyTraceId?.let { Log.i("ShixuVoice", "traceId=$it reply_cancel") }
        replyTraceId = null
    }
    private fun cancelNativeResponse() {
        nativeReplyRequested = false
        protocol?.cancel(responseActive)?.let { websocket?.send(it) }
        responseActive = false
        nativeResponses.interrupt()
    }
    fun interrupt() {
        if (!mutable.value.active) return
        cancelReplyTrace()
        finalWait?.cancel(); finalWait = null; pendingFinal = null
        synthesisCall?.cancel(); runCatching { synthesisResponse?.close() }
        val abandoned = turnGeneration
        turnGeneration++; turnJob?.cancel(); turnJob = null
        nativeBridge?.let { scope.launch(NonCancellable) { it.abandon(abandoned) } }
        cancelNativeResponse()
        protocol?.takeUnless { it.transcriptionOnly }?.let { websocket?.send(it.clear()) }; manualTurn?.reset()
        audio.interrupt(); runCatching { tts?.stop() }; ttsAnswer = null; resetSegment()
        mutable.value = mutable.value.copy(phase = VoicePhase.LISTENING, level = 0f)
        if (settings.platform == VoicePlatform.SYSTEM) {
            try { recognizer?.cancel(); listenSystem() }
            catch (error: Exception) { stop("系统语音暂时不可用，可重新开启或使用文字输入") }
        }
    }
    fun stop(error: String? = null) {
        pausedSettings = null
        Log.i("ShixuVoice", "session=$generation stop phase=${mutable.value.phase}")
        cancelReplyTrace()
        finalWait?.cancel(); finalWait = null; pendingFinal = null; finalDeduplicator.clear()
        transcriptWait?.cancel(); transcriptWait = null; submittedItems.clear()
        synthesisCall?.cancel(); runCatching { synthesisResponse?.close() }; synthesisCall = null; synthesisResponse = null
        val abandoned = turnGeneration
        generation++; turnGeneration++; turnJob?.cancel(); sessionJob?.cancel(); turnJob = null; sessionJob = null
        nativeBridge?.let { scope.launch(NonCancellable) { it.abandon(abandoned) } }
        cancelNativeResponse(); websocket?.close(1000, "closed"); websocket?.cancel(); websocket = null; protocol = null
        manualTurn?.reset(); manualTurn = null
        nativeResponses.reset(); nativeTools.clear(); nativeText.setLength(0); nativeInstructions = ""
        audio.release(); runCatching { recognizer?.destroy() }; recognizer = null
        runCatching { tts?.stop() }; runCatching { tts?.shutdown() }; tts = null; ttsReady = false; ttsAnswer = null
        resetSegment(); seenInput = false
        mutable.value = VoiceSessionState(if (error == null) VoicePhase.OFF else VoicePhase.ERROR, error = error)
        lastReadback = null
    }

    private fun acceptNativeEvent(responseId: String): Boolean = nativeResponses.accepts(responseId, turnGeneration)

    fun pauseForBackground() {
        if (pausedSettings != null && !mutable.value.active) return
        val resume = settings.takeIf { mutable.value.active }
        stop()
        pausedSettings = resume
    }

    fun resumeFromBackground(microphoneAllowed: Boolean) {
        val next = pausedSettings ?: return
        pausedSettings = null
        if (microphoneAllowed) start(next) else stop("麦克风权限已撤回，可继续文字输入")
    }
}

/** Handshake only; never acquires microphone or emits a test conversation. */
internal suspend fun checkVoiceConnection(settings: VoiceSettings): String {
    settings.validated()
    if (settings.platform == VoicePlatform.SYSTEM) return "系统语音将在开启时检查设备服务"
    if (settings.platform == VoicePlatform.SILICON_FLOW) {
        return voiceHttp.newCall(Request.Builder().url("https://api.siliconflow.cn/v1/models")
            .header("Authorization", "Bearer ${settings.apiKey}").build()).awaitVoice().use { response ->
            check(response.isSuccessful) { "密钥或网络不可用（${response.code}）" }
            "平台鉴权成功；实际识别和合成需开启语音验收"
        }
    }
    val adapter = RealtimeVoiceProtocol(settings)
    return withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine<String> { continuation ->
            val socket = voiceHttp.newWebSocket(Request.Builder().url(adapter.url)
                .apply { if (settings.platform == VoicePlatform.ALIYUN) header("OpenAI-Beta", "realtime=v1") }
                .header("Authorization", "Bearer ${settings.apiKey}").build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) { webSocket.send(adapter.session()) }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = runCatching { adapter.parse(text) }.getOrNull()
                    if (event == VoiceEvent.Ready || event is VoiceEvent.Failure) {
                        if (continuation.isActive) {
                            if (event == VoiceEvent.Ready) continuation.resume("实时会话配置已确认；实际语音需开启后验收")
                            else continuation.resumeWithException(IOException(voiceFailureMessage(event as VoiceEvent.Failure, settings)))
                        }
                        webSocket.close(1000, "checked")
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (continuation.isActive) continuation.resumeWithException(IOException(
                        voiceFailureMessage(VoiceEvent.Failure(response?.code?.let { "http_$it" } ?: "network"), settings)))
                }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (continuation.isActive) continuation.resumeWithException(IOException(
                        voiceFailureMessage(VoiceEvent.Failure("closed_$code"), settings)))
                    webSocket.close(code, "closed")
                }
            })
            continuation.invokeOnCancellation { socket.cancel() }
        }
    } ?: error("实时语音连接检查超时，请检查网络、模型及地域")
}
