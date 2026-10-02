package com.xiaomanjun.sleepdownschedule.feature.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AgentTodoCreationFollowUpTest {
    @Test fun calendarFailureAfterCommitDoesNotPermitCreatingTheSameProposalAgain() = runBlocking {
        val ledger = AgentCreationReceiptLedger()
        assertTrue(ledger.begin("1:confirmed", false))
        val result = agentTodoCreationFollowUp(2, {}, {}, { throw SecurityException("denied") })
        assertTrue(result.success)
        assertTrue(result.verified)
        assertTrue(result.message.contains("已创建 2 项待办"))
        assertTrue(result.message.contains("系统日历同步尚未完成"))
        assertTrue(ledger.finish("1:confirmed", result))
        assertFalse(ledger.begin("1:confirmed", false))
    }

    @Test fun failedReminderRefreshDoesNotSkipWidgetsOrCalendar() = runBlocking {
        val calls = mutableListOf<String>()
        val result = agentTodoCreationFollowUp(1,
            { calls += "reminders"; throw IllegalStateException("reminder failed") },
            { calls += "widgets"; throw IllegalStateException("widget failed") },
            { calls += "calendar"; false })
        assertEquals(listOf("reminders", "widgets", "calendar"), calls)
        assertTrue(result.success && result.verified)
        assertTrue(result.message.contains("提醒更新尚未完成"))
        assertTrue(result.message.contains("组件刷新尚未完成"))
        assertFalse(result.message.contains("reminder failed"))
    }

    @Test fun calendarReviewIsReportedAsSavedAndNeedsConfirmation() = runBlocking {
        val result = agentTodoCreationFollowUp(1, {}, {}, { true })
        assertTrue(result.success && result.verified)
        assertTrue(result.message.contains("日历页确认"))
        assertFalse(result.message.contains("同步尚未完成"))
    }

    @Test fun coroutineCancellationRetainsItsMeaning() = runBlocking {
        val cancelled = CancellationException("cancelled")
        try {
            agentTodoCreationFollowUp(1, {}, {}, { throw cancelled })
            fail("Cancellation must propagate")
        } catch (error: CancellationException) { assertSame(cancelled, error) }
    }
}
