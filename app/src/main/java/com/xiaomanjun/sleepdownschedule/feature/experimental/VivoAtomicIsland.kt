package com.xiaomanjun.sleepdownschedule.feature.experimental

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import com.xiaomanjun.sleepdownschedule.BuildConfig
import com.xiaomanjun.sleepdownschedule.MainActivity
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.core.identity.applyAppNotificationIcon
import com.xiaomanjun.sleepdownschedule.feature.reminder.LiveUpdateKind
import com.xiaomanjun.sleepdownschedule.feature.reminder.LiveUpdatePayload
import java.security.MessageDigest

/** Local vivo notification protocol. Both approval values are intentionally empty by default. */
internal object VivoAtomicIsland {
    private const val TAG = "VivoAtomicIsland"
    private const val PREFS = "vivo_atomic_course"
    private const val KEY_ACTIVE = "active_course"
    private const val KEY_TITLE = "active_title"
    private const val KEY_LAST_POST = "last_post"
    private const val MIN_UPDATE_INTERVAL_MS = 10_000L
    private const val ID = 20260525
    private val lock = Any()

    fun isAvailable(context: Context): Boolean {
        if (!BuildConfig.SLEEPDOWN_EXPERIMENTAL_FEATURES) return false
        val scene = BuildConfig.VIVO_COURSE_SCENE
        val approvedCert = BuildConfig.VIVO_APPROVED_CERT_SHA256
        if (scene.isBlank() || approvedCert.isBlank()) return false
        if (context.packageName != "com.scheduleplus.student") return false
        val brand = Build.BRAND.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        if (!brand.equals("vivo", true) && !brand.equals("iqoo", true) &&
            !manufacturer.equals("vivo", true) && !manufacturer.equals("iqoo", true)) return false
        return signingCertificates(context).any { it == approvedCert }
    }

    /** Returns true only when the one course notification was posted (or already up to date). */
    fun post(context: Context, payload: LiveUpdatePayload, channelId: String): Boolean = synchronized(lock) {
        if (payload.kind != LiveUpdateKind.COURSE || payload.segments.isEmpty() || !isAvailable(context)) {
            return@synchronized false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return@synchronized false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val identity = "${payload.muteKey}|${payload.muteUntil}"
        val recorded = prefs.getString(KEY_ACTIVE, null)
        val previous = recorded?.takeIf { manager.activeNotifications.any { notification -> notification.id == ID } }
        if (recorded != null && previous == null) prefs.edit { clear() }
        if (previous != null && previous != identity) endLocked(context, manager, channelId)
        val now = System.currentTimeMillis()
        if (previous == identity && now - prefs.getLong(KEY_LAST_POST, 0L) < MIN_UPDATE_INTERVAL_MS) {
            return@synchronized true
        }
        val operation = if (previous == identity) 1 else 0
        val description = "${payload.statusAt(now).detailText} · ${payload.timeText}"
        return@synchronized runCatching {
            manager.notify(ID, buildNotification(context, channelId, operation, payload.name, description))
            prefs.edit {
                putString(KEY_ACTIVE, identity)
                putString(KEY_TITLE, payload.name)
                putLong(KEY_LAST_POST, now)
            }
            true
        }.getOrElse { error ->
            Log.w(TAG, "course island post failed: ${error.javaClass.simpleName}")
            false
        }
    }

    fun end(context: Context, channelId: String) = synchronized(lock) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return@synchronized
        endLocked(context, manager, channelId)
    }

    private fun endLocked(context: Context, manager: NotificationManager, channelId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_ACTIVE, null) == null) return
        val title = prefs.getString(KEY_TITLE, "课程提醒").orEmpty()
        runCatching {
            manager.notify(ID, buildNotification(context, channelId, 2, title, "课程提醒已结束"))
        }.onFailure { error ->
            Log.w(TAG, "course island end failed: ${error.javaClass.simpleName}")
            manager.cancel(ID)
        }
        prefs.edit { clear() }
    }

    private fun buildNotification(
        context: Context,
        channelId: String,
        operation: Int,
        title: String,
        content: String
    ): Notification {
        val click = PendingIntent.getActivity(
            context,
            ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val baseInfo = Bundle().apply {
            putParcelable("notification.superx.baseInfos.icon", Icon.createWithResource(context, R.mipmap.ic_launcher))
            putCharSequence("notification.superx.baseInfos.title", title)
            putCharSequence("notification.superx.baseInfos.content", content)
        }
        val extras = Bundle().apply {
            putInt("notification.superx.operation", operation)
            putBoolean("notification.superx.showNotify", true)
            putInt("notification.superx.template", 1)
            putParcelable("notification.superx.clickResp", click)
            putString("notification.superx.scene", BuildConfig.VIVO_COURSE_SCENE)
            putBundle("notification.superx.baseInfos", baseInfo)
        }
        return Notification.Builder(context, channelId)
            .applyAppNotificationIcon(context)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(click)
            .setOnlyAlertOnce(true)
            .apply { if (operation == 2) setTimeoutAfter(1_500L) }
            .addExtras(extras)
            .build()
    }

    private fun signingCertificates(context: Context): List<String> = runCatching {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        val signatures: Array<out android.content.pm.Signature> = if (Build.VERSION.SDK_INT >= 28) {
            val signing = info.signingInfo ?: return@runCatching emptyList()
            (if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory)
                ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }
        signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02X".format(it) }
        }
    }.getOrElse { emptyList() }
}
