package com.xiaomanjun.sleepdownschedule.feature.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AgentCreationPlanRunnerTest {
    private val success = AgentPlanExecutionResult(true, null, true, "已完成")
    private val failure = AgentPlanExecutionResult(false, null, false, "后续批次失败")

    @Test
    fun retryAfterLaterBatchFailureSkipsPreviouslyCreatedTodo() {
        val ledger = AgentCreationReceiptLedger()
        val key = "schedule:message:creation"
        val plan = AgentPlan(listOf(
            action(AgentValidatedActionType.CREATE_TODO, "待办"),
            action(AgentValidatedActionType.ADD, "课程")
        ))
        var todoSaves = 0
        var courseAttempts = 0
        var completed: AgentPlanExecutionResult? = null
        val handler: AgentActionHandler = { batch, onResult ->
            when (batch.actions.first().type) {
                AgentValidatedActionType.CREATE_TODO -> {
                    todoSaves++
                    onResult(success)
                }
                AgentValidatedActionType.ADD -> {
                    courseAttempts++
                    onResult(if (courseAttempts == 1) failure else success)
                }
                else -> error("Unexpected creation batch")
            }
        }

        assertTrue(ledger.begin(key, alreadyApplied = false))
        AgentCreationPlanRunner(ledger).run(key, plan, handler) { completed = it }
        assertFalse(completed!!.success)
        assertEquals(1, todoSaves)
        assertTrue(completed!!.message.contains("此前批次已完成"))
        assertTrue(ledger.finish(key, completed!!))

        completed = null
        assertTrue(ledger.begin(key, alreadyApplied = false))
        AgentCreationPlanRunner(ledger).run(key, plan, handler) { completed = it }

        assertTrue(completed!!.success)
        assertTrue(completed!!.verified)
        assertEquals("A retry must not insert the committed todo again", 1, todoSaves)
        assertEquals(2, courseAttempts)
        assertTrue(ledger.finish(key, completed!!))
        assertFalse(ledger.begin(key, alreadyApplied = false))
    }

    @Test
    fun homogeneousTodoAndCoursePlansKeepTheirAtomicBatchBoundaries() {
        val todos = AgentPlan(listOf(
            action(AgentValidatedActionType.CREATE_TODO, "待办一"),
            action(AgentValidatedActionType.CREATE_TODO, "待办二")
        ))
        val courses = AgentPlan(listOf(
            action(AgentValidatedActionType.ADD, "课程一"),
            action(AgentValidatedActionType.ADD, "课程二")
        ))

        assertEquals(listOf(2), agentCreationBatches(todos).map { it.plan.actions.size })
        assertEquals(listOf(2), agentCreationBatches(courses).map { it.plan.actions.size })
    }

    @Test
    fun singleBatchPreservesExecutorResultIncludingUndo() {
        val ledger = AgentCreationReceiptLedger()
        val key = "schedule:single-batch"
        val undo: (((AgentPlanExecutionResult) -> Unit) -> Unit) = { _ -> }
        val executorResult = AgentPlanExecutionResult(true, null, true, "课程已创建", undo)
        var observed: AgentPlanExecutionResult? = null

        assertTrue(ledger.begin(key, alreadyApplied = false))
        AgentCreationPlanRunner(ledger).run(
            key,
            AgentPlan(listOf(action(AgentValidatedActionType.ADD, "课程"))),
            handler = { _, onResult -> onResult(executorResult) },
            onResult = { observed = it }
        )

        assertSame(executorResult, observed)
    }

    @Test
    fun emptyPlanPreservesHandlerFailure() {
        val ledger = AgentCreationReceiptLedger()
        val key = "schedule:empty"
        val executorResult = AgentPlanExecutionResult(false, null, false, "没有可执行操作")
        var observed: AgentPlanExecutionResult? = null

        assertTrue(ledger.begin(key, alreadyApplied = false))
        AgentCreationPlanRunner(ledger).run(
            key,
            AgentPlan(emptyList()),
            handler = { _, onResult -> onResult(executorResult) },
            onResult = { observed = it }
        )

        assertSame(executorResult, observed)
    }

    @Test
    fun duplicateHandlerCallbackDoesNotRepeatBatchOrCompletion() {
        val ledger = AgentCreationReceiptLedger()
        val key = "schedule:duplicate"
        val plan = AgentPlan(listOf(action(AgentValidatedActionType.CREATE_TODO, "待办")))
        var saves = 0
        var completions = 0
        val handler: AgentActionHandler = { _, onResult ->
            saves++
            val gate = CountDownLatch(1)
            val first = Thread { gate.await(); onResult(success) }
            val second = Thread { gate.await(); onResult(success) }
            first.start()
            second.start()
            gate.countDown()
            first.join()
            second.join()
        }

        assertTrue(ledger.begin(key, alreadyApplied = false))
        AgentCreationPlanRunner(ledger).run(key, plan, handler) { completions++ }

        assertEquals(1, saves)
        assertEquals(1, completions)
    }

    @Test
    fun finalCompletionRunsOnlyAfterReceiptIsAcceptedAndOnlyOnce() {
        val ledger = AgentCreationReceiptLedger()
        val key = "schedule:final-completion"
        var appliedMarkerWritten = false
        var completions = 0
        assertTrue(ledger.begin(key, alreadyApplied = false))

        val first = finishAgentCreationOnce(
            ledger = ledger,
            key = key,
            result = success,
            onVerifiedSuccess = {
                assertSame(success, ledger.receipts.value[key])
                appliedMarkerWritten = true
            },
            onComplete = { result ->
                assertTrue(appliedMarkerWritten)
                assertSame(success, result)
                completions++
            }
        )
        val duplicate = finishAgentCreationOnce(
            ledger = ledger,
            key = key,
            result = success,
            onComplete = { completions++ }
        )

        assertTrue(first)
        assertFalse(duplicate)
        assertEquals(1, completions)
    }

    private fun action(type: AgentValidatedActionType, summary: String) =
        AgentValidatedAction(type = type, summary = summary)
}
