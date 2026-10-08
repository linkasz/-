package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.Context
import androidx.work.*
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import java.util.concurrent.TimeUnit

class TodoTrashWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val repository = TodoRepository((applicationContext as CourseScheduleApp).database.todoDao())
        TodoCalendarSync(applicationContext, repository).cleanupTrash()
        Result.success()
    } catch (error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        fun schedule(context: Context) {
            val work = WorkManager.getInstance(context)
            work.enqueueUniquePeriodicWork("todo-trash-retention", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TodoTrashWorker>(1, TimeUnit.DAYS).build())
            work.enqueueUniqueWork("todo-trash-startup", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<TodoTrashWorker>().build())
        }
    }
}
