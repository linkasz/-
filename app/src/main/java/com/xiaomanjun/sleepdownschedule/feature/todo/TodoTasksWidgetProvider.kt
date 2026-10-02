package com.xiaomanjun.sleepdownschedule.feature.todo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoTasksWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        WidgetScope.launch {
            try {
                runCatching { updateWidgets(context.applicationContext, appWidgetManager, appWidgetIds) }
                    .onFailure { Log.w(TAG, "Could not refresh todo widget", it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        val pendingResult = goAsync()
        WidgetScope.launch {
            try {
                runCatching { updateWidgets(context.applicationContext, appWidgetManager, intArrayOf(appWidgetId)) }
                    .onFailure { Log.w(TAG, "Could not resize todo widget", it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        if (ids.isEmpty()) return
        val app = context.applicationContext as CourseScheduleApp
        val tasks = app.database.todoDao().getActiveItems()
            .asSequence()
            .filter { !it.isCompleted && it.parentId == null }
            .sortedWith(
                compareBy<TodoItemEntity> { !it.isPinned }
                    .thenBy { it.dueAt == null }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
            .toList()
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val titleColor = if (dark) 0xFFF4F3FA.toInt() else 0xFF282B36.toInt()
        val bodyColor = if (dark) 0xFFE5E4EB.toInt() else 0xFF30323A.toInt()
        val secondaryColor = if (dark) 0xFFB9B8C2.toInt() else 0xFF747783.toInt()
        val accentColor = if (dark) 0xFFAFC0FF.toInt() else 0xFF526AB0.toInt()
        val background = if (dark) R.drawable.widget_today_background_dark else R.drawable.widget_today_background

        ids.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_todo_tasks)
            views.setInt(R.id.widget_todo_root, "setBackgroundResource", background)
            views.setTextColor(R.id.widget_todo_header, titleColor)
            views.setTextColor(R.id.widget_todo_add, accentColor)
            views.setOnClickPendingIntent(R.id.widget_todo_root, activityIntent(context, widgetId, "open", false))
            views.setOnClickPendingIntent(R.id.widget_todo_add, activityIntent(context, widgetId, "add", true))

            val options = manager.getAppWidgetOptions(widgetId)
            val heightDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 108)
            )
            val visibleRows = when {
                heightDp >= 190 -> 4
                heightDp >= 145 -> 3
                else -> 2
            }

            repeat(WIDGET_ROW_COUNT) { index ->
                val row = index + 1
                val rowId = rowId(row)
                val titleId = titleId(row)
                val dueId = dueId(row)
                val checkId = checkId(row)
                val item = tasks.getOrNull(index)
                val show = row <= visibleRows && (item != null || (tasks.isEmpty() && row == 1))
                views.setViewVisibility(rowId, if (show) android.view.View.VISIBLE else android.view.View.GONE)
                if (!show) return@repeat

                views.setTextColor(titleId, bodyColor)
                views.setTextColor(dueId, secondaryColor)
                views.setTextColor(checkId, accentColor)
                if (item == null) {
                    views.setTextViewText(checkId, "–")
                    views.setTextViewText(titleId, context.getString(R.string.widget_todo_tasks_empty))
                    views.setViewVisibility(checkId, android.view.View.GONE)
                    views.setViewVisibility(dueId, android.view.View.GONE)
                } else {
                    views.setTextViewText(checkId, "○")
                    views.setContentDescription(checkId, context.getString(R.string.widget_todo_complete, item.title))
                    views.setTextViewText(titleId, item.title)
                    val due = item.dueAt?.let { timestamp ->
                        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
                        date.format(DateTimeFormatter.ofPattern(if (item.allDay) "M/d" else "M/d HH:mm"))
                    }
                    views.setTextViewText(dueId, due.orEmpty())
                    views.setViewVisibility(checkId, android.view.View.VISIBLE)
                    views.setViewVisibility(dueId, if (due == null) android.view.View.GONE else android.view.View.VISIBLE)
                    views.setOnClickPendingIntent(rowId, activityIntent(context, widgetId, "item-${item.id}", false))
                    views.setOnClickPendingIntent(checkId, completionIntent(context, widgetId, item.id))
                }
            }
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun activityIntent(
        context: Context,
        widgetId: Int,
        action: String,
        startNewTask: Boolean
    ): PendingIntent {
        val intent = Intent(context, com.xiaomanjun.sleepdownschedule.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(Uri.parse("scheduleplus://todo-widget/$widgetId/$action"))
        if (startNewTask) intent.putExtra(TodoQuickCaptureContract.EXTRA_START_NEW_TASK, true)
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun completionIntent(context: Context, widgetId: Int, todoId: Long): PendingIntent {
        val intent = Intent(context, TodoWidgetActionReceiver::class.java)
            .setAction("${context.packageName}.TODO_WIDGET_COMPLETE")
            .setData(Uri.parse("scheduleplus://todo-widget/$widgetId/complete/$todoId"))
            .putExtra(TodoWidgetActionReceiver.EXTRA_TODO_ID, todoId)
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun rowId(row: Int) = intArrayOf(
        R.id.widget_todo_row_1,
        R.id.widget_todo_row_2,
        R.id.widget_todo_row_3,
        R.id.widget_todo_row_4
    )[row - 1]

    private fun titleId(row: Int) = intArrayOf(
        R.id.widget_todo_title_1,
        R.id.widget_todo_title_2,
        R.id.widget_todo_title_3,
        R.id.widget_todo_title_4
    )[row - 1]

    private fun dueId(row: Int) = intArrayOf(
        R.id.widget_todo_due_1,
        R.id.widget_todo_due_2,
        R.id.widget_todo_due_3,
        R.id.widget_todo_due_4
    )[row - 1]

    private fun checkId(row: Int) = intArrayOf(
        R.id.widget_todo_check_1,
        R.id.widget_todo_check_2,
        R.id.widget_todo_check_3,
        R.id.widget_todo_check_4
    )[row - 1]

    companion object {
        private const val TAG = "TodoTasksWidget"
        private const val WIDGET_ROW_COUNT = 4
        private val WidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, TodoTasksWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            WidgetScope.launch {
                runCatching { TodoTasksWidgetProvider().updateWidgets(appContext, manager, ids) }
                    .onFailure { Log.w(TAG, "Could not refresh todo widget", it) }
            }
        }
    }
}
