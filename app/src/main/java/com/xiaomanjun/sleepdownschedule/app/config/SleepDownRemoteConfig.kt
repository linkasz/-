package com.xiaomanjun.sleepdownschedule.app.config

import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.core.remoteconfig.*

import com.xiaomanjun.sleepdownschedule.feature.importing.*

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SleepDownRemoteConfig {
    private const val ExperiencePrefs = "sleepdown_remote_experience"
    private const val ShownNoticesKey = "shown_notice_ids_v1"
    private const val AcceptedPrivacyKey = "accepted_privacy_version"
    private const val AcceptedTermsKey = "accepted_terms_version"

    private val dialogNoticesShownThisProcess = ConcurrentHashMap.newKeySet<Long>()
    private val mutableState = MutableStateFlow(RemoteConfigState())
    val state = mutableState.asStateFlow()
    private val mutableExperience = MutableStateFlow(RemoteExperienceState())
    val experience = mutableExperience.asStateFlow()

    fun refresh(scope: CoroutineScope, force: Boolean = true) = Unit

    fun markNoticeShown(context: Context, notice: RemoteNotice) {
        if (notice.displayMode == "dialog") {
            dialogNoticesShownThisProcess += notice.id
        } else {
            val prefs = context.getSharedPreferences(ExperiencePrefs, Context.MODE_PRIVATE)
            val shown = prefs.getStringSet(ShownNoticesKey, emptySet()).orEmpty().toMutableSet()
            shown += notice.id.toString()
            prefs.edit { putStringSet(ShownNoticesKey, shown) }
        }
        recomputeExperience(context.applicationContext)
    }

    fun acceptAgreement(context: Context, agreement: RemoteAgreementSummary) {
        val type = mutableState.value.bootstrap?.agreements?.let { set ->
            when (agreement) { set.privacy -> "privacy"; set.terms -> "terms"; else -> null }
        } ?: return
        context.getSharedPreferences(ExperiencePrefs, Context.MODE_PRIVATE).edit(commit = true) {
            putLong(if (type == "privacy") AcceptedPrivacyKey else AcceptedTermsKey, agreement.version)
        }
        recomputeExperience(context.applicationContext)
    }

    private fun recomputeExperience(context: Context) {
        val bootstrap = mutableState.value.bootstrap
        if (bootstrap == null) { mutableExperience.value = RemoteExperienceState(); return }
        val prefs = context.getSharedPreferences(ExperiencePrefs, Context.MODE_PRIVATE)
        val agreement = listOfNotNull(bootstrap.agreements.privacy, bootstrap.agreements.terms).firstOrNull { item ->
            val accepted = prefs.getLong(
                if (item === bootstrap.agreements.privacy) AcceptedPrivacyKey else AcceptedTermsKey,
                0L
            )
            item.required && item.version > accepted && (accepted == 0L || item.forceReaccept)
        }
        val shown = prefs.getStringSet(ShownNoticesKey, emptySet()).orEmpty()
        val now = estimatedServerTimeSeconds()
        val notice = bootstrap.notices.firstOrNull { item ->
            item.enabled && item.startAt <= now && (item.endAt == null || item.endAt > now) &&
                if (item.displayMode == "dialog") item.id !in dialogNoticesShownThisProcess
                else item.id.toString() !in shown
        }
        mutableExperience.value = RemoteExperienceState(agreement, notice)
    }

    internal fun estimatedServerTimeSeconds(): Long = System.currentTimeMillis() / 1_000L
}
