package com.xiaomanjun.sleepdownschedule.benchmark

import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun bidirectionalCourseTodoCalendarInsightsAndSettingsNavigation() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = ITERATIONS,
            setupBlock = {
                startBenchmarkHomeWithWallpaper()
                device.waitForIdle(BenchmarkIdleTimeoutMillis)
                SystemClock.sleep(SETTLE_BEFORE_CAPTURE_MILLIS)
            }
        ) {
            listOf("待办", "日历", "洞察", "设置", "洞察", "日历", "待办", "课程").forEach { label ->
                val tab = findDockLabel(device, label)
                device.click(tab.visibleCenter.x, tab.visibleCenter.y)
                SystemClock.sleep(SETTLE_AFTER_SWITCH_MILLIS)
                device.waitForIdle(BenchmarkIdleTimeoutMillis)
            }
        }

    private fun findDockLabel(device: UiDevice, label: String): UiObject2 {
        val dockTop = device.displayHeight - DOCK_SEARCH_HEIGHT_PX
        val candidate = device.findObjects(By.text(label))
            .firstOrNull { it.visibleBounds.centerY() >= dockTop }
        return requireNotNull(candidate) { "Dock tab '$label' was not visible" }
    }

    companion object {
        private const val ITERATIONS = 5
        private const val SETTLE_BEFORE_CAPTURE_MILLIS = 500L
        private const val SETTLE_AFTER_SWITCH_MILLIS = 720L
        private const val DOCK_SEARCH_HEIGHT_PX = 300
    }
}
