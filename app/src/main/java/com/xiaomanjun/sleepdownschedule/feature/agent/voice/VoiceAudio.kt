package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.math.sqrt

/** Owns only active-session hardware; never records or plays after release. */
internal class VoiceAudio(private val context: Context, private val scope: CoroutineScope) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var recorder: AudioRecord? = null
    private var echo: AcousticEchoCanceler? = null
    private var recordJob: Job? = null
    private var playJob: Job? = null
    @Volatile private var track: AudioTrack? = null
    private var queue = Channel<ByteArray>(64)
    private var focus: AudioFocusRequest? = null
    @Volatile private var playbackGeneration = 0
    val playbackToken get() = playbackGeneration
    @Volatile private var capturedGeneration = 0
    @Volatile var playedBytes = 0L; private set
    private val attributes = assistantPlaybackAttributes()

    @SuppressLint("MissingPermission")
    fun start(rate: Int, onFrame: (ByteArray, Float) -> Unit, onLostFocus: () -> Unit, onFailure: () -> Unit) {
        release()
        acquireFocus(onLostFocus)
        val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minimum > 0) { "设备不支持该语音采样率" }
        val recording = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, rate / 5))
        recorder = recording
        check(recording.state == AudioRecord.STATE_INITIALIZED) { "麦克风不可用" }
        if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(recording.audioSessionId)?.apply { enabled = true }
        recording.startRecording()
        val generation = ++capturedGeneration
        recordJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(rate / 25 * 2)
            try {
                while (isActive && capturedGeneration == generation) {
                    val count = recording.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) error("Microphone read failed")
                    if (count > 0 && capturedGeneration == generation) {
                        val frame = buffer.copyOf(count)
                        onFrame(frame, pcmLevel(frame))
                    }
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (capturedGeneration == generation) withContext(Dispatchers.Main) { onFailure() } }
        }
    }

    fun acquireFocus(onLostFocus: () -> Unit) {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
                if (change != AudioManager.AUDIOFOCUS_GAIN) onLostFocus()
            }, Handler(Looper.getMainLooper())).build()
        check(manager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "无法获得音频焦点" }
        focus = focusRequest
    }

    suspend fun play(pcm: ByteArray, onFailure: () -> Unit, expectedToken: Int = playbackGeneration) =
        withContext(Dispatchers.Main.immediate) {
            if (expectedToken != playbackGeneration) return@withContext
            if (playJob?.isActive != true) {
                queue = Channel(64)
                val current = queue
                val generation = playbackGeneration
                playJob = scope.launch(Dispatchers.IO) {
                    var output: AudioTrack? = null
                    var outputBytes = 0L
                    try {
                        val minimum = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                        check(minimum > 0) { "设备不支持语音播放采样率" }
                        val owned = AudioTrack.Builder().setAudioAttributes(attributes)
                            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                            .setBufferSizeInBytes(maxOf(minimum, 9600)).setTransferMode(AudioTrack.MODE_STREAM).build()
                        output = owned
                        check(owned.state == AudioTrack.STATE_INITIALIZED) { "音频播放设备不可用" }
                        manager.assistantSpeaker()?.let { speaker ->
                            if (!owned.setPreferredDevice(speaker)) android.util.Log.w("ShixuAudio", "speaker_preference_rejected")
                        }
                        ensureActive()
                        if (generation != playbackGeneration) return@launch
                        track = owned
                        owned.play()
                        for (chunk in current) {
                            if (generation != playbackGeneration) break
                            var offset = 0
                            while (offset < chunk.size && isActive && generation == playbackGeneration) {
                                val written = owned.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                                check(written > 0) { "Audio output failed" }
                                offset += written; outputBytes += written
                                if (generation == playbackGeneration) playedBytes = outputBytes
                            }
                        }
                        if (generation == playbackGeneration) withTimeoutOrNull(60_000) {
                            while (isActive && owned.playbackHeadPosition.toLong() < outputBytes / 2) delay(30)
                        }
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) {
                        android.util.Log.w("ShixuAudio", "playback_failure ${error.javaClass.simpleName}")
                        if (generation == playbackGeneration) withContext(Dispatchers.Main) { onFailure() }
                    } finally {
                        runCatching { output?.stop() }; runCatching { output?.release() }
                        if (track === output) track = null
                    }
                }
            }
            val current = queue
            try { current.send(pcm) }
            catch (error: CancellationException) { throw error }
            catch (error: kotlinx.coroutines.channels.ClosedSendChannelException) {
                if (expectedToken == playbackGeneration) {
                    android.util.Log.w("ShixuAudio", "playback_queue_closed")
                    onFailure()
                }
            }
        }

    suspend fun finishPlayback(expectedToken: Int = playbackGeneration) {
        val job = withContext(Dispatchers.Main.immediate) {
            if (expectedToken != playbackGeneration) return@withContext null
            queue.close()
            playJob
        }
        job?.join()
    }

    fun interrupt() {
        playbackGeneration++; queue.close(); playJob?.cancel(); playJob = null
        runCatching { track?.pause(); track?.flush() }; playedBytes = 0
    }

    fun release() {
        capturedGeneration++; recordJob?.cancel(); recordJob = null
        runCatching { recorder?.stop() }; runCatching { recorder?.release() }; recorder = null
        runCatching { echo?.release() }.onFailure { android.util.Log.w("ShixuAudio", "echo_release ${it.javaClass.simpleName}") }; echo = null
        interrupt()
        focus?.let { request -> runCatching { manager.abandonAudioFocusRequest(request) } }; focus = null
    }
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
