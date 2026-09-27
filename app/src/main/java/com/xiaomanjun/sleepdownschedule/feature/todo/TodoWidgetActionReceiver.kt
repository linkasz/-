package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, 0L)
        if (todoId <= 0L) return
        val pendingResult = goAsync()
        WidgetActionScope.launch {
            try {
                val app = context.applicationContext as CourseScheduleApp
                val dao = app.database.todoDao()
                val item = dao.getById(todoId) ?: return@launch
                if (item.isCompleted || item.parentId != null) return@launch

                val repository = TodoRepository(dao)
                val calendarSync = TodoCalendarSync(context.applicationContext, repository)
                runCatching { calendarSync.remove(item) }
                    .onFailure { Log.w(TAG, "Could not remove completed task from system calendar", it) }
                dao.toggleCompleted(item, System.currentTimeMillis())
                NotificationScheduler.requestReschedule(context.applicationContext)
                if (calendarSync.autoSyncEnabled() && calendarSync.hasCalendarPermission()) {
                    runCatching { calendarSync.syncAll() }
                        .onFailure { Log.w(TAG, "Could not refresh system calendar after widget action", it) }
                }
                TodoTasksWidgetProvider.refreshAll(context.applicationContext)
            } catch (error: Exception) {
                Log.e(TAG, "Todo widget action failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "com.scheduleplus.student.todo.widget.TODO_ID"
        private const val TAG = "TodoTasksWidget"
        private val WidgetActionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
