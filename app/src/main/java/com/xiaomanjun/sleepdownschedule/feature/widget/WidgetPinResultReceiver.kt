package com.xiaomanjun.sleepdownschedule.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit

/** Called only after the launcher confirms that a widget was pinned. */
class WidgetPinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PINNED) return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPinTracker.confirm(context, intent.getStringExtra(EXTRA_REQUEST_TOKEN), widgetId)
            Toast.makeText(context, "小组件已添加到桌面", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_PINNED = "com.scheduleplus.student.action.WIDGET_PINNED"
        const val EXTRA_REQUEST_TOKEN = "request_token"
    }
}

/** A launcher accepting a pin request is not proof that a widget was placed. */
internal object WidgetPinTracker {
    private const val PREFS = "widget_pin_request"
    private const val KEY_TOKEN = "token"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_BEFORE_IDS = "before_ids"
    private const val KEY_CONFIRMED_ID = "confirmed_id"
    private const val TAG = "WidgetPin"

    enum class Result { NONE, WAITING, ADDED }

    fun begin(context: Context, token: String, provider: ComponentName, beforeIds: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putString(KEY_TOKEN, token)
            putString(KEY_PROVIDER, provider.flattenToString())
            putString(KEY_BEFORE_IDS, beforeIds.joinToString(","))
            remove(KEY_CONFIRMED_ID)
        }
        Log.i(TAG, "pin requested provider=${provider.className} beforeCount=${beforeIds.size}")
    }

    fun clear(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_TOKEN, null) == token) prefs.edit { clear() }
    }

    fun currentToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun confirm(context: Context, token: String?, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (token == null || prefs.getString(KEY_TOKEN, null) != token) return
        prefs.edit { putInt(KEY_CONFIRMED_ID, widgetId) }
        Log.i(TAG, "pin success callback widgetId=$widgetId")
    }

    fun result(context: Context, manager: AppWidgetManager, token: String?): Result {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (token == null || prefs.getString(KEY_TOKEN, null) != token) return Result.NONE
        if (prefs.getInt(KEY_CONFIRMED_ID, AppWidgetManager.INVALID_APPWIDGET_ID) !=
            AppWidgetManager.INVALID_APPWIDGET_ID) return Result.ADDED
        val provider = ComponentName.unflattenFromString(prefs.getString(KEY_PROVIDER, null).orEmpty())
            ?: return Result.WAITING
        val before = prefs.getString(KEY_BEFORE_IDS, "").orEmpty().split(',')
            .mapNotNull(String::toIntOrNull).toSet()
        val current = runCatching { manager.getAppWidgetIds(provider).toSet() }.getOrElse {
            Log.w(TAG, "widget id query failed: ${it.javaClass.simpleName}")
            return Result.WAITING
        }
        val newId = (current - before).firstOrNull()
        if (newId != null) {
            prefs.edit { putInt(KEY_CONFIRMED_ID, newId) }
            Log.i(TAG, "pin confirmed by new widgetId=$newId")
            return Result.ADDED
        }
        return Result.WAITING
    }
}
