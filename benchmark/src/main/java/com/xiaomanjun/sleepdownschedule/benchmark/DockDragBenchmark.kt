package com.xiaomanjun.sleepdownschedule.benchmark

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DockDragBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun dockSlowHeldDragWithGlass() = measureDockDrag(
        steps = SLOW_DRAG_STEPS,
        dragDurationMillis = SLOW_DRAG_DURATION_MILLIS
    )

    @Test
    fun dockFastHeldDragWithGlass() = measureDockDrag(
        steps = FAST_DRAG_STEPS,
        dragDurationMillis = FAST_DRAG_DURATION_MILLIS
    )

    private fun measureDockDrag(steps: Int, dragDurationMillis: Long) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.WARM,
        iterations = ITERATIONS,
        setupBlock = {
            startBenchmarkHomeWithWallpaper()
            waitForDock(device)
            device.waitForIdle(BenchmarkIdleTimeoutMillis)
            SystemClock.sleep(SETTLE_BEFORE_CAPTURE_MILLIS)
        }
    ) {
        val dock = waitForDock(device)
        val bounds = dock.visibleBounds
        val startX = bounds.left + (bounds.width() * START_FRACTION).toInt()
        val endX = bounds.left + (bounds.width() * END_FRACTION).toInt()
        val centerY = bounds.centerY()

        performHeldDrag(device, startX, centerY, endX, centerY, steps, dragDurationMillis)
        SystemClock.sleep(SETTLE_AFTER_DRAG_MILLIS)
    }

    private fun performHeldDrag(
        device: UiDevice,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        steps: Int,
        dragDurationMillis: Long
    ) {
        val downTime = SystemClock.uptimeMillis()
        injectPointerEvent(device, downTime, MotionEvent.ACTION_DOWN, startX, startY)
        SystemClock.sleep(LONG_PRESS_HOLD_MILLIS)

        val moveDelayMillis = (dragDurationMillis / steps).coerceAtLeast(1L)
        for (step in 1..steps) {
            val fraction = step / steps.toFloat()
            val x = startX + ((endX - startX) * fraction).toInt()
            val y = startY + ((endY - startY) * fraction).toInt()
            injectPointerEvent(device, downTime, MotionEvent.ACTION_MOVE, x, y)
            if (step < steps) SystemClock.sleep(moveDelayMillis)
        }
        injectPointerEvent(device, downTime, MotionEvent.ACTION_UP, endX, endY)
    }

    private fun injectPointerEvent(device: UiDevice, downTime: Long, action: Int, x: Int, y: Int) {
        val event = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            action,
            x.toFloat(),
            y.toFloat(),
            0
        ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        try {
            InstrumentationRegistry.getInstrumentation().sendPointerSync(event)
        } finally {
            event.recycle()
        }
    }

    private fun waitForDock(device: UiDevice): UiObject2 = requireNotNull(
        device.wait(
            Until.findObject(By.res(PACKAGE_NAME, DOCK_RESOURCE_NAME)),
            UI_TIMEOUT_MILLIS
        )
    ) { "Main navigation Dock did not become visible" }

    companion object {
        private const val ITERATIONS = 5
        private const val UI_TIMEOUT_MILLIS = 10_000L
        private const val SETTLE_BEFORE_CAPTURE_MILLIS = 500L
        private const val SETTLE_AFTER_DRAG_MILLIS = 900L
        private const val LONG_PRESS_HOLD_MILLIS = 500L
        private const val SLOW_DRAG_STEPS = 90
        private const val FAST_DRAG_STEPS = 18
        private const val SLOW_DRAG_DURATION_MILLIS = 900L
        private const val FAST_DRAG_DURATION_MILLIS = 360L
        private const val START_FRACTION = 0.1f
        private const val END_FRACTION = 0.9f
        private const val DOCK_RESOURCE_NAME = "benchmark_home_dock"
    }
}
