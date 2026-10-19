package com.xiaomanjun.sleepdownschedule

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaomanjun.sleepdownschedule.feature.agent.background.DayAgentForegroundServiceHost
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises Android's real foreground-service start/stop ordering without a model account. */
@RunWith(AndroidJUnit4::class)
class AiInterruptLifecycleTest {
    @Test
    fun repeatedStopAndRestartKeepsTheLatestTurnAlive() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.waitForIdleSync()
            repeat(40) {
                instrumentation.runOnMainSync {
                    DayAgentForegroundServiceHost.startThinking(context)
                    DayAgentForegroundServiceHost.cancelThinking(context)
                    DayAgentForegroundServiceHost.startThinking(context)
                }
                SystemClock.sleep(50)
            }
            SystemClock.sleep(500)
            assertTrue("The newest run must retain foreground service", running(context))
            instrumentation.runOnMainSync { DayAgentForegroundServiceHost.cancelThinking(context) }
            SystemClock.sleep(500)
            assertFalse("Cancellation must release foreground service", running(context))
            // The platform's startForegroundService deadline is longer than intent dispatch.
            SystemClock.sleep(6_000)
        }
    }

    @Suppress("DEPRECATION")
    private fun running(context: Context): Boolean = context.getSystemService(ActivityManager::class.java)
        .getRunningServices(Int.MAX_VALUE).any {
            it.service.className == DayAgentForegroundService::class.java.name && it.foreground
        }
}
