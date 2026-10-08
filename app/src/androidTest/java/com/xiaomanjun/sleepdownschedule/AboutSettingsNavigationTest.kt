package com.xiaomanjun.sleepdownschedule

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaomanjun.sleepdownschedule.app.ui.SettingsDetailPageExtra
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutSettingsNavigationTest {
    @Test
    fun aboutRouteSurvivesAdaptiveIconCompositionAndActivityRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, SettingsDetailActivity::class.java)
            .putExtra(SettingsDetailPageExtra, "Changelog")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<SettingsDetailActivity>(intent).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse("About Activity should stay open after first composition", activity.isFinishing)
                assertNotNull("Launcher icon drawable must be available on this API level", activity.getDrawable(R.mipmap.ic_launcher))
            }

            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse("About Activity should stay open after state restoration", activity.isFinishing)
            }
        }
    }
}
