package com.xiaomanjun.sleepdownschedule.data.local

import android.content.Context

/**
 * Calendar provider row IDs are device-local. This preference file is excluded from both cloud
 * backup and device transfer by the app's backup rules.
 */
internal object CalendarEventLocalIdStore {
    private const val PREFERENCES = "schedule_plus_calendar_pending"
    private const val KEY_EVENT_IDS = "local_event_ids_by_token"

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun get(token: String): Long? = values().firstNotNullOfOrNull { value ->
        val separator = value.lastIndexOf(':')
        if (separator <= 0 || value.substring(0, separator) != token) null
        else value.substring(separator + 1).toLongOrNull()
    }

    fun put(token: String, eventId: Long) {
        require(token.isNotBlank() && eventId > 0)
        val next = values().filterNot { it.substringBeforeLast(':', "") == token }.toMutableSet()
        next += "$token:$eventId"
        check(preferences().edit().putStringSet(KEY_EVENT_IDS, next).commit()) {
            "无法保存本机日历关联，请稍后重试"
        }
    }

    fun remove(token: String) {
        val next = values().filterNot { it.substringBeforeLast(':', "") == token }.toSet()
        check(preferences().edit().putStringSet(KEY_EVENT_IDS, next).commit()) {
            "无法清除本机日历关联，请稍后重试"
        }
    }

    private fun values(): Set<String> = preferences().getStringSet(KEY_EVENT_IDS, emptySet()).orEmpty()

    private fun preferences() = requireNotNull(appContext) {
        "CalendarEventLocalIdStore must be initialized before use"
    }.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
