package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.app.ui.Screen
import com.xiaomanjun.sleepdownschedule.app.ui.HomeMode
import com.xiaomanjun.sleepdownschedule.app.ui.dockIndex
import com.xiaomanjun.sleepdownschedule.app.ui.homeModeAtFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationTest {
    @Test
    fun singleDockKeepsTheFiveMainDestinationsInProductOrder() {
        assertEquals(0, Screen.Home.dockIndex())
        assertEquals(1, Screen.TodoTasks.dockIndex())
        assertEquals(2, Screen.TodoCalendar.dockIndex())
        assertEquals(3, Screen.TodoInsights.dockIndex())
        assertEquals(4, Screen.Config.dockIndex())
    }

    @Test
    fun courseModeGestureMapsTheTwoEqualSegmentsToDayAndWeek() {
        assertEquals(HomeMode.Day, homeModeAtFraction(0f))
        assertEquals(HomeMode.Day, homeModeAtFraction(0.49f))
        assertEquals(HomeMode.Week, homeModeAtFraction(0.5f))
        assertEquals(HomeMode.Week, homeModeAtFraction(1f))
    }
}
