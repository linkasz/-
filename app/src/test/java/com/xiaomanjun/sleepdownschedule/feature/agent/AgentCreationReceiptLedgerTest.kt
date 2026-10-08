package com.xiaomanjun.sleepdownschedule.feature.agent

import org.junit.Assert.*
import org.junit.Test

class AgentCreationReceiptLedgerTest {
    private val success = AgentPlanExecutionResult(true, null, true, "已创建")
    private val failure = AgentPlanExecutionResult(false, null, false, "保存失败")

    @Test fun preparingAndCancellingDraftDoesNotClaimOrExecute() {
        val ledger = AgentCreationReceiptLedger()
        assertTrue(ledger.receipts.value.isEmpty())
        ledger.abandon("1:proposal")
        assertTrue(ledger.receipts.value.isEmpty())
    }

    @Test fun rapidConfirmAndReopenedPresentationOnlyExecuteOnce() {
        val ledger = AgentCreationReceiptLedger()
        var writes = 0
        repeat(20) { if (ledger.begin("1:proposal", false)) writes++ }
        assertEquals(1, writes)
        assertTrue(ledger.finish("1:proposal", success))
        assertFalse(ledger.begin("1:proposal", false))
        assertFalse(ledger.finish("1:proposal", success))
        assertEquals(success, ledger.receipts.value["1:proposal"])
    }

    @Test fun failedSaveCanRetryAndIndependentProposalsDoNotBlock() {
        val ledger = AgentCreationReceiptLedger()
        assertTrue(ledger.begin("1:a", false))
        assertTrue(ledger.begin("1:b", false))
        assertTrue(ledger.finish("1:a", failure))
        assertTrue(ledger.begin("1:a", false))
        assertTrue(ledger.finish("1:a", success))
        assertNull(ledger.receipts.value["1:b"])
    }

    @Test fun previouslyAppliedProposalCannotBeReplayedAfterProcessRestart() {
        assertFalse(AgentCreationReceiptLedger().begin("1:proposal", true))
    }
}
