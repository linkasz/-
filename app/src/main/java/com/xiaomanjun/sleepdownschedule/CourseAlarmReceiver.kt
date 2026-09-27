package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoActivity
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoQuickCaptureContract

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

class CourseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationScheduler.ACTION_TODO_REMINDER) {
            val pending = goAsync()
            val app = context.applicationContext as CourseScheduleApp
            app.applicationScope.launch(Dispatchers.IO) {
                try {
                    val todoId = intent.getLongExtra("todoId", 0L)
                    val scheduledDueAt = intent.getLongExtra("todoDueAt", Long.MIN_VALUE)
                    val item = app.database.todoDao().getById(todoId) ?: return@launch
                    if (item.isCompleted || item.parentId != null || item.dueAt != scheduledDueAt) return@launch
                    NotificationScheduler.withShortWakeLock(context, "todo_reminder") {
                        NotificationScheduler.createChannel(context)
                        if (!NotificationScheduler.canPostNotifications(context)) return@withShortWakeLock
                        val notification = NotificationCompat.Builder(context, NotificationScheduler.channelId())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("待办即将截止：${item.title}")
                            .setContentText(item.description.ifBlank { "请查看待办事项" })
                            .setContentIntent(
                                PendingIntent.getActivity(
                                    context,
                                    todoId.hashCode(),
                                    Intent(context, TodoActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        .putExtra(TodoQuickCaptureContract.EXTRA_OPEN_TODO_ID, todoId)
                                        .setData(Uri.parse("scheduleplus://todo-reminder/$todoId/$scheduledDueAt")),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                            .setAutoCancel(true)
                            .build()
                        runCatching {
                            NotificationManagerCompat.from(context).notify(todoId.hashCode(), notification)
                        }.onFailure { error ->
                            Log.w("SchedulePlusReminder", "skip todo notification", error)
                        }
                    }
                } catch (error: Exception) {
                    Log.w("SchedulePlusReminder", "could not verify todo reminder", error)
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action == NotificationScheduler.ACTION_REFRESH_COURSE_ALARMS ||
            intent.action == Intent.ACTION_SCREEN_ON || intent.action == Intent.ACTION_USER_PRESENT
        ) {
            val pending = goAsync()
            NotificationScheduler.requestRefresh(
                context,
                forceReschedule = intent.action == NotificationScheduler.ACTION_REFRESH_COURSE_ALARMS
            ) {
                pending.finish()
            }
            return
        }
        NotificationScheduler.withShortWakeLock(context, "course_alarm") {
            NotificationScheduler.createChannel(context)
            val payload = NotificationScheduler.payloadFromIntent(intent)
            val name = payload?.name ?: intent.getStringExtra("courseName") ?: "课程"
            val location = payload?.location ?: intent.getStringExtra("location").orEmpty()
            val timeText = payload?.timeText ?: intent.getStringExtra("timeText").orEmpty()
            val mode = runCatching { NotificationMode.valueOf(intent.getStringExtra("notificationMode") ?: NotificationMode.STANDARD.name) }.getOrDefault(NotificationMode.STANDARD)
            if (mode == NotificationMode.LIVE_UPDATE && payload != null) {
                // Alarm delivery order is not guaranteed. Re-select from current data so an older
                // session's end/retry cannot replace or cancel the course that is now in class.
                val pending = goAsync()
                NotificationScheduler.requestRefresh(context) {
                    pending.finish()
                }
                return@withShortWakeLock
            }
            val startTime = runCatching { LocalTime.parse(timeText.substringBefore("-").trim()) }.getOrNull()
            if (
                mode == NotificationMode.LIVE_UPDATE &&
                payload?.segments.isNullOrEmpty() &&
                startTime != null &&
                !LocalTime.now().isBefore(startTime)
            ) {
                Log.d("SleepDownLiveUpdate", "skip alarm live update: course already started name=$name, start=$startTime")
                return@withShortWakeLock
            }
            if (!NotificationScheduler.canPostNotifications(context)) {
                Log.w("SleepDownLiveUpdate", "skip alarm: notification delivery unavailable")
                return@withShortWakeLock
            }
            val notification = if (mode == NotificationMode.LIVE_UPDATE) {
                null
            } else {
                NotificationCompat.Builder(context, NotificationScheduler.channelId())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("快上课了：$name")
                    .setContentText(if (location.isBlank()) "请准备上课" else "地点：$location")
                    .setAutoCancel(true)
                    .build()
            }
            if (mode == NotificationMode.LIVE_UPDATE) {
                val livePayload = payload ?: return@withShortWakeLock
                if (livePayload.shouldStop()) {
                    Log.d("SleepDownLiveUpdate", "alarm boundary reached payload expiry key=${livePayload.muteKey}")
                    NotificationScheduler.cancelLiveUpdateNotifications(context)
                    NotificationScheduler.stopLiveUpdateService(context)
                    return@withShortWakeLock
                }
                if (NotificationScheduler.isPayloadMuted(context, livePayload)) {
                    Log.d("SleepDownLiveUpdate", "skip muted alarm payload key=${livePayload.muteKey}")
                    return@withShortWakeLock
                }
                Log.d("SleepDownLiveUpdate", "alarm receiver live update: kind=${livePayload.kind}, name=$name")
                NotificationScheduler.startLiveUpdateService(context, livePayload)
            } else {
                val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                try {
                    NotificationManagerCompat.from(context).notify(notificationId, requireNotNull(notification))
                } catch (securityException: SecurityException) {
                    Log.w("SleepDownLiveUpdate", "skip course notification: permission revoked", securityException)
                }
            }
        }
    }
}
