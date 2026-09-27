package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.Context
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TodoHiltModule {
    @Provides
    @Singleton
    fun provideTodoDao(@ApplicationContext context: Context): TodoDao =
        (context.applicationContext as CourseScheduleApp).database.todoDao()
}
