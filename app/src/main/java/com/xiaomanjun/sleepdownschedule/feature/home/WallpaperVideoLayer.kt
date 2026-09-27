package com.xiaomanjun.sleepdownschedule.feature.home

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xiaomanjun.sleepdownschedule.core.wallpaper.WallpaperCropState
import kotlin.math.max

@Composable
internal fun WallpaperVideoLayer(
    uri: Uri,
    cropState: WallpaperCropState,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val latestIsActive = rememberUpdatedState(isActive)
    val playerView = remember(context) { WallpaperVideoTextureView(context) }
    DisposableEffect(lifecycleOwner, playerView) {
        val observer = LifecycleEventObserver { _, event ->
            playerView.setPlaybackActive(latestIsActive.value && event == Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        playerView.setPlaybackActive(latestIsActive.value && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerView.setPlaybackActive(false)
            playerView.release()
        }
    }
    SideEffect {
        playerView.bind(uri, cropState)
        playerView.setPlaybackActive(latestIsActive.value && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    AndroidView(
        factory = { playerView },
        update = {
            it.bind(uri, cropState)
            it.setPlaybackActive(latestIsActive.value && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        },
        modifier = modifier
    )
}

private class WallpaperVideoTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private var source: Uri? = null
    private var crop = WallpaperCropState()
    private var player: MediaPlayer? = null
    private var playerSurface: Surface? = null
    private var playbackActive = false
    private var prepared = false
    private var videoWidth = 0
    private var videoHeight = 0

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    fun bind(uri: Uri, cropState: WallpaperCropState) {
        crop = cropState
        if (source != uri) {
            source = uri
            releasePlayer()
            surfaceTexture?.let(::preparePlayer)
        } else {
            updateCropTransform()
        }
    }

    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        val current = player ?: return
        if (active && prepared) {
            runCatching { if (!current.isPlaying) current.start() }
        } else if (!active && prepared) {
            runCatching { if (current.isPlaying) current.pause() }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        source?.let { preparePlayer(surface) }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateCropTransform()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun release() {
        releasePlayer()
    }

    private fun preparePlayer(texture: SurfaceTexture) {
        val selected = source ?: return
        releasePlayer()
        val next = MediaPlayer()
        player = next
        val surface = Surface(texture)
        playerSurface = surface
        try {
            next.setSurface(surface)
            next.setVolume(0f, 0f)
            next.isLooping = true
            next.setOnPreparedListener { ready ->
                if (player !== ready) return@setOnPreparedListener
                prepared = true
                videoWidth = ready.videoWidth
                videoHeight = ready.videoHeight
                updateCropTransform()
                setPlaybackActive(playbackActive)
            }
            next.setOnVideoSizeChangedListener { ready, width, height ->
                if (player === ready) {
                    videoWidth = width
                    videoHeight = height
                    updateCropTransform()
                }
            }
            next.setOnErrorListener { failed, _, _ ->
                if (player === failed) releasePlayer()
                true
            }
            next.setDataSource(context, selected)
            next.prepareAsync()
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    private fun updateCropTransform() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) return
        val baseScale = max(width / videoWidth.toFloat(), height / videoHeight.toFloat())
        val scale = baseScale * crop.scale.coerceIn(1f, 6f)
        val drawnWidth = videoWidth * scale
        val drawnHeight = videoHeight * scale
        val left = width / 2f - videoWidth * crop.centerX.coerceIn(0f, 1f) * scale
        val top = height / 2f - videoHeight * crop.centerY.coerceIn(0f, 1f) * scale
        val matrix = Matrix().apply {
            setScale(drawnWidth / width, drawnHeight / height)
            postTranslate(left, top)
        }
        setTransform(matrix)
    }

    private fun releasePlayer() {
        val old = player
        player = null
        prepared = false
        videoWidth = 0
        videoHeight = 0
        if (old != null) {
            runCatching { old.setOnPreparedListener(null) }
            runCatching { old.setOnVideoSizeChangedListener(null) }
            runCatching { old.setOnErrorListener(null) }
            runCatching { old.release() }
        }
        playerSurface?.let { runCatching { it.release() } }
        playerSurface = null
    }
}
