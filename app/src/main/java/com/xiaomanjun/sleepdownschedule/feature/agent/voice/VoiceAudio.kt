package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlin.math.sqrt

/** Owns only active-session hardware; never records or plays after release. */
internal class VoiceAudio(private val context: Context, private val scope: CoroutineScope) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var recorder: AudioRecord? = null
    private var echo: AcousticEchoCanceler? = null
    private var noise: NoiseSuppressor? = null
    @Volatile var echoCancellationActive = false; private set
    private var recordJob: Job? = null
    @Volatile private var recordCleanup: Job? = null
    private var focus: AudioFocusRequest? = null
    @Volatile private var capturedGeneration = 0
    private val attributes = assistantPlaybackAttributes()
    private val playback = VoicePlayback(scope, ::createPlaybackSink)
    val playbackToken get() = playback.playbackToken
    val playedBytes get() = playback.playedBytes

    @SuppressLint("MissingPermission")
    suspend fun start(rate: Int, onFrame: (ByteArray, Float) -> Unit, onLostFocus: () -> Unit, onFailure: () -> Unit) {
        release()
        recordCleanup?.join()
        currentCoroutineContext().ensureActive()
        acquireFocus(onLostFocus)
        val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minimum > 0) { "设备不支持该语音采样率" }
        val recording = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, rate / 5))
        recorder = recording
        check(recording.state == AudioRecord.STATE_INITIALIZED) { "麦克风不可用" }
        echo = captureEffect("echo") {
            if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(recording.audioSessionId) else null
        }
        noise = captureEffect("noise") {
            if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(recording.audioSessionId) else null
        }
        echoCancellationActive = echo != null
        val processor = VoiceCaptureProcessor(rate, platformNoiseSuppression = noise != null)
        android.util.Log.i("ShixuAudio", "capture_start rate=$rate aec=${echo != null} ns=${noise != null}")
        recording.startRecording()
        check(recording.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "麦克风未开始录音" }
        val generation = ++capturedGeneration
        recordJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(rate / 25 * 2)
            try {
                while (isActive && capturedGeneration == generation) {
                    val count = recording.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) error("Microphone read failed")
                    if (count > 0 && capturedGeneration == generation) {
                        val frame = buffer.copyOf(count)
                        processor.process(frame)
                        onFrame(frame, pcmLevel(frame))
                    }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                android.util.Log.w("ShixuAudio", "capture_failure ${error.javaClass.simpleName}")
                withContext(Dispatchers.Main) { if (capturedGeneration == generation) onFailure() }
            }
        }
    }

    fun acquireFocus(onLostFocus: () -> Unit) {
        lateinit var focusRequest: AudioFocusRequest
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
                if (focus === focusRequest && change != AudioManager.AUDIOFOCUS_GAIN) onLostFocus()
            }, Handler(Looper.getMainLooper())).build()
        check(manager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "无法获得音频焦点" }
        focus = focusRequest
    }

    suspend fun play(pcm: ByteArray, onFailure: () -> Unit, expectedToken: Int = playbackToken) =
        playback.play(pcm, onFailure, expectedToken)

    suspend fun finishPlayback(expectedToken: Int = playbackToken) = playback.finishPlayback(expectedToken)

    fun interrupt() = playback.interrupt()

    private fun createPlaybackSink(): VoicePlaybackSink {
        val minimum = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "设备不支持语音播放采样率" }
        val output = AudioTrack.Builder().setAudioAttributes(attributes)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimum, 9600)).setTransferMode(AudioTrack.MODE_STREAM).build()
        return object : VoicePlaybackSink {
            override fun start() {
                check(output.state == AudioTrack.STATE_INITIALIZED) { "音频播放设备不可用" }
                manager.assistantSpeaker()?.let { speaker ->
                    if (!output.setPreferredDevice(speaker)) android.util.Log.w("ShixuAudio", "speaker_preference_rejected")
                }
                output.play()
            }
            override fun write(bytes: ByteArray, offset: Int, length: Int): Int =
                output.write(bytes, offset, length, AudioTrack.WRITE_NON_BLOCKING)
            override val playbackHeadPositionSamples get() = output.playbackHeadPosition.toLong() and 0xffff_ffffL
            override fun close() {
                runCatching { output.pause() }
                runCatching { output.flush() }
                runCatching { output.stop() }
                runCatching { output.release() }
            }
        }
    }

    fun release() {
        capturedGeneration++
        val oldJob = recordJob
        val oldRecorder = recorder
        val oldEcho = echo
        val oldNoise = noise
        recordJob = null; recorder = null; echo = null; noise = null; echoCancellationActive = false
        oldJob?.cancel()
        runCatching { oldRecorder?.stop() }
        if (oldJob != null || oldRecorder != null || oldEcho != null || oldNoise != null) {
            recordCleanup = releaseAfterVoiceJob(oldJob) {
                runCatching { oldEcho?.release() }.onFailure {
                    android.util.Log.w("ShixuAudio", "echo_release ${it.javaClass.simpleName}")
                }
                runCatching { oldNoise?.release() }.onFailure {
                    android.util.Log.w("ShixuAudio", "noise_release ${it.javaClass.simpleName}")
                }
                runCatching { oldRecorder?.release() }
            }
        }
        interrupt()
        focus?.let { request -> runCatching { manager.abandonAudioFocusRequest(request) } }; focus = null
    }
}

/** Audio effects are optional vendor capabilities; a rejected effect must not tear down capture. */
private fun <T : AudioEffect> captureEffect(name: String, create: () -> T?): T? {
    var effect: T? = null
    try {
        effect = create() ?: return null
        if (effect.setEnabled(true) == AudioEffect.SUCCESS && effect.enabled) return effect
        android.util.Log.w("ShixuAudio", "${name}_unavailable")
    } catch (error: Exception) {
        android.util.Log.w("ShixuAudio", "${name}_unavailable ${error.javaClass.simpleName}")
    }
    runCatching { effect?.release() }
    return null
}

/** Keep a blocking native read's resource alive until its owning coroutine has exited. */
internal fun releaseAfterVoiceJob(job: Job?, release: () -> Unit): Job {
    val cleanup = CompletableDeferred<Unit>()
    val finish = {
        try { release() } finally { cleanup.complete(Unit) }
        Unit
    }
    if (job == null) finish() else job.invokeOnCompletion { finish() }
    return cleanup
}

internal fun pcmLevel(bytes: ByteArray): Float {
    if (bytes.size < 2) return 0f
    var total = 0.0
    for (index in 0 until bytes.size - 1 step 2) {
        val sample = ((bytes[index].toInt() and 255) or (bytes[index + 1].toInt() shl 8)).toShort().toDouble() / 32768
        total += sample * sample
    }
    return (sqrt(total / (bytes.size / 2)) * 7).toFloat().coerceIn(0f, 1f)
}

internal fun pcmWav(pcm: ByteArray, rate: Int): ByteArray {
    val out = java.nio.ByteBuffer.allocate(44 + pcm.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    out.put("RIFF".toByteArray()); out.putInt(pcm.size + 36); out.put("WAVEfmt ".toByteArray())
    out.putInt(16); out.putShort(1); out.putShort(1); out.putInt(rate); out.putInt(rate * 2)
    out.putShort(2); out.putShort(16); out.put("data".toByteArray()); out.putInt(pcm.size); out.put(pcm)
    return out.array()
}
