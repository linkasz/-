package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager

/** Assistant replies use media volume and loudspeaker playback, not telephone earpiece routing. */
internal fun assistantPlaybackAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()

// Prefer the phone speaker on our own players without changing global call mode or other apps' routes.
internal fun AudioManager.assistantSpeaker(): AudioDeviceInfo? =
    getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
