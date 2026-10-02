package com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRefreshRetryPolicyTest {
    @Test
    fun transientNetworkFailuresRetryWithABoundedAttemptCount() {
        assertTrue(autoRefreshErrorIsRetryable(IOException("network unavailable")))
        assertTrue(autoRefreshErrorIsRetryable(IllegalStateException("教务页面加载失败，请检查校园网或 VPN 连接")))

        val retryable = AutoRefreshOutcome(success = false, message = "网络暂不可用", retryable = true)
        assertTrue(shouldRetryAutoRefresh(retryable, runAttemptCount = 0))
        assertTrue(shouldRetryAutoRefresh(retryable, runAttemptCount = AutoRefreshMaxRetryAttempts - 1))
        assertFalse(shouldRetryAutoRefresh(retryable, runAttemptCount = AutoRefreshMaxRetryAttempts))
    }

    @Test
    fun successfulAndAuthenticationFailuresDoNotRetry() {
        assertFalse(autoRefreshErrorIsRetryable(IllegalStateException("教务会话已失效，请重新登录")))
        assertFalse(
            shouldRetryAutoRefresh(
                AutoRefreshOutcome(success = true, message = "刷新成功", retryable = true),
                runAttemptCount = 0
            )
        )
        assertFalse(
            shouldRetryAutoRefresh(
                AutoRefreshOutcome(success = false, message = "会话已失效", retryable = false),
                runAttemptCount = 0
            )
        )
    }
}
