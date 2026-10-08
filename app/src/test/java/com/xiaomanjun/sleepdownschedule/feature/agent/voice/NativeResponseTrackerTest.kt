package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import org.junit.Assert.*
import org.junit.Test

class NativeResponseTrackerTest {
    @Test fun completionIsConsumedOnceAndOldTextCannotEnterTheNextTurn() {
        val t = NativeResponseTracker(); t.enqueue(1)
        assertTrue(t.started("r1", 1)); assertTrue(t.accepts("r1", 1))
        assertTrue(t.consume("r1", 1)); assertFalse(t.consume("r1", 1)); assertFalse(t.accepts("r1", 1))
        t.enqueue(2); assertTrue(t.started("r2", 2)); assertFalse(t.accepts("r1", 2)); assertTrue(t.accepts("r2", 2))
    }
    @Test fun lateCreationAfterInterruptKeepsRequestOrderButCannotCreateAReply() {
        val t = NativeResponseTracker(); t.enqueue(1); t.interrupt(); t.enqueue(2)
        assertFalse(t.started("late-r1", 2)); assertTrue(t.started("r2", 2))
        assertFalse(t.consume("late-r1", 2)); assertTrue(t.consume("r2", 2))
    }
    @Test fun toolContinuationAndShutdownDoNotReviveACompletedResponse() {
        val t = NativeResponseTracker(); t.enqueue(3); assertTrue(t.started("tool-response", 3))
        assertTrue(t.consume("tool-response", 3)); t.enqueue(3); assertTrue(t.started("final-response", 3))
        assertFalse(t.accepts("tool-response", 3)); t.reset(); assertFalse(t.accepts("final-response", 3))
        assertFalse(t.started("unrequested", 3))
    }
    @Test fun duplicateCreationDoesNotConsumeTheNextPendingRequest() {
        val t = NativeResponseTracker(); t.enqueue(1); assertTrue(t.started("r1", 1)); t.consume("r1", 1)
        t.enqueue(2); assertTrue(t.seen("r1")); assertFalse(t.started("r1", 2)); assertTrue(t.started("r2", 2))
    }
}
