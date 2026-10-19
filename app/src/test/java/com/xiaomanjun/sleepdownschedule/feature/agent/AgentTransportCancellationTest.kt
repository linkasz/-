package com.xiaomanjun.sleepdownschedule.feature.agent

import java.util.concurrent.CancellationException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTransportCancellationTest {
    @Test
    fun cancellationIsNeverTreatedAsRecoverableFinalOutputFailure() {
        assertFalse(CancellationException("user stopped the turn").isFinalAnswerRepairable())
    }

    @Test
    fun missingOrInvalidFinalOutputStillUsesTheRepairPath() {
        assertTrue(MissingResponsesBodyException().isFinalAnswerRepairable())
        assertTrue(AgentProtocolViolationException().isFinalAnswerRepairable())
    }

    @Test
    fun cancellingRequestJobDisconnectsItsHttpConnectionImmediately() {
        val connection = TrackingHttpConnection()
        val job = Job()
        val registration = connection.disconnectAgentRequestOnCancellation(job)

        job.cancel(CancellationException("user stopped the turn"))

        assertTrue(connection.disconnected)
        registration.dispose()
    }

    @Test
    fun disconnectFailureDoesNotEscapeTheJobCancellationHandler() {
        val connection = TrackingHttpConnection(throwOnDisconnect = true)
        val job = Job()
        connection.disconnectAgentRequestOnCancellation(job)

        job.cancel(CancellationException("user stopped the turn"))
    }

    private class TrackingHttpConnection(
        private val throwOnDisconnect: Boolean = false
    ) : HttpURLConnection(URL("http://localhost/")) {
        var disconnected = false
            private set

        override fun disconnect() {
            disconnected = true
            if (throwOnDisconnect) error("close failure")
        }

        override fun usingProxy(): Boolean = false

        override fun connect() = Unit
    }
}
