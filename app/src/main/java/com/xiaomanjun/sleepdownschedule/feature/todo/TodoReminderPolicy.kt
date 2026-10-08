package com.xiaomanjun.sleepdownschedule.feature.todo

import java.time.Instant
import java.time.ZoneId

internal fun todoReminderAt(item: TodoItemEntity, legacyLeadMinutes: Int, zone: ZoneId): Long? {
    val due = item.dueAt ?: return null
    if (item.deletedAt != null || item.isCompleted || item.parentId != null) return null
    return when (item.reminderMode) {
        "NONE" -> null
        "LEGACY" -> (if (item.allDay) Instant.ofEpochMilli(due).atZone(zone).toLocalDate().atTime(9, 0).atZone(zone).toInstant().toEpochMilli() else due) - legacyLeadMinutes.coerceAtLeast(0) * 60_000L
        "BEFORE" -> due - item.reminderOffsetMinutes.coerceAtLeast(0) * 60_000L
        "ALL_DAY" -> Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
            .minusDays((item.reminderOffsetMinutes / 1440).coerceAtLeast(0).toLong())
            .atStartOfDay().plusMinutes(item.reminderTimeMinutes.toLong()).atZone(zone).toInstant().toEpochMilli()
        else -> null
    }
}

internal fun todoReminderSignature(item: TodoItemEntity): String =
    listOf(item.dueAt, item.endAt, item.allDay, item.reminderMode, item.reminderOffsetMinutes,
        item.reminderTimeMinutes, item.persistentReminder, item.strongReminder, item.title, item.description).joinToString("|")

internal fun todoNotificationChannel(context: android.content.Context, strong: Boolean): String {
    if (!strong) return com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler.channelId()
    val id = "scheduleplus_todo_strong"
    val manager = context.getSystemService(android.app.NotificationManager::class.java)
    manager.createNotificationChannel(android.app.NotificationChannel(id, "待办强提醒", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
        enableVibration(true)
        setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
            android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
    })
    return id
}
