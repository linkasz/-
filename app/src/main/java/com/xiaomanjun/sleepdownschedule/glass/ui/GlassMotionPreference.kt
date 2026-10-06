package com.xiaomanjun.sleepdownschedule.glass.ui

import android.database.ContentObserver
import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** A live system preference, not another persisted application setting. */
private object GlassMotionPreference {
    var enabled by mutableStateOf(true)
        private set
    private var users = 0
    private var observer: ContentObserver? = null
    fun acquire(resolver: ContentResolver) {
        if (users++ != 0) return
        fun refresh() { enabled = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
        refresh()
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { refresh() }
        }.also { resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, it) }
    }
    fun release(resolver: ContentResolver) {
        if (--users == 0) {
            observer?.let(resolver::unregisterContentObserver)
            observer = null
        }
    }
}

@Composable
internal fun rememberGlassMotionEnabled(): Boolean {
    val resolver = LocalContext.current.applicationContext.contentResolver
    DisposableEffect(resolver) {
        GlassMotionPreference.acquire(resolver)
        onDispose { GlassMotionPreference.release(resolver) }
    }
    return GlassMotionPreference.enabled
}
