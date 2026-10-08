package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One preview owns its focus/player/HTTP call and releases them when the settings leave. */
internal class VoicePreviewPlayer(context: Context, scope: CoroutineScope) {
    private val audio = VoiceAudio(context, scope)
    private val manager = context.getSystemService(android.media.AudioManager::class.java)
    private var player: MediaPlayer? = null
    private var call: Call? = null
    private var response: Response? = null
    private var epoch = 0L
    private var owner: Job? = null
    suspend fun play(voice: TtsVoice, settings: VoiceSettings) {
        stop()
        val token = epoch
        owner = currentCoroutineContext()[Job]
        try {
            audio.acquireFocus { stop() }
            if (voice.sampleUrl != null) withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val p = MediaPlayer(); player = p
                    p.setAudioAttributes(assistantPlaybackAttributes())
                    manager.assistantSpeaker()?.let { speaker ->
                        if (!p.setPreferredDevice(speaker)) android.util.Log.w("ShixuAudio", "preview_speaker_preference_rejected")
                    }
                    p.setOnPreparedListener { if (continuation.isActive) it.start() }
                    p.setOnCompletionListener { if (continuation.isActive) continuation.resume(Unit) }
                    p.setOnErrorListener { _, _, _ ->
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException("试听音频读取失败，请重试")); true
                    }
                    continuation.invokeOnCancellation { runCatching { p.release() }; if (player === p) player = null }
                    p.setDataSource(voice.sampleUrl); p.prepareAsync()
                }
            } else {
                settings.validated()
                AliyunTtsClient.stream(settings.copy(ttsVoice = voice.id), "你好，我是时序清单的智能助理。现在为你试听这个音色。",
                    { call = it }, { response = it }, { audio.play(it, { stop() }) })
                audio.finishPlayback()
            }
        } finally { if (epoch == token) { owner = null; stop() } }
    }
    fun stop() {
        // Cancelling the owner also resumes a suspended sample when audio focus is lost.
        epoch++; owner?.cancel(); owner = null
        call?.cancel(); call = null; runCatching { response?.close() }; response = null
        player?.let { runCatching { it.release() } }; player = null; audio.release()
    }
}
