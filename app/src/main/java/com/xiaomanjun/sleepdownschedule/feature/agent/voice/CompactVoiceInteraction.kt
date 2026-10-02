package com.xiaomanjun.sleepdownschedule.feature.agent.voice

internal enum class CompactVoiceMicAction { START, INTERRUPT, KEEP_LISTENING }

/** Continuing a compact conversation must never implicitly switch off the microphone. */
internal fun compactVoiceMicAction(phase: VoicePhase): CompactVoiceMicAction = when (phase) {
    VoicePhase.OFF, VoicePhase.ERROR -> CompactVoiceMicAction.START
    VoicePhase.THINKING, VoicePhase.QUERYING, VoicePhase.SPEAKING -> CompactVoiceMicAction.INTERRUPT
    VoicePhase.CONNECTING, VoicePhase.LISTENING -> CompactVoiceMicAction.KEEP_LISTENING
}
