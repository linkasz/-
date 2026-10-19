package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import android.util.Log
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.model.AgentMessageEntity
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** An overlay can disappear while a confirmed save finishes. Its receipt outlives that overlay. */
internal object AgentCreationDispatch {
    private val ledger = AgentCreationReceiptLedger()
    val receipts = ledger.receipts
    fun key(scheduleId: Int, actionKey: String) = "$scheduleId:$actionKey"

    @Synchronized
    fun submit(
        context: Context,
        scheduleId: Int,
        date: LocalDate,
        actionKey: String,
        plan: AgentPlan,
        onComplete: ((AgentPlanExecutionResult) -> Unit)? = null,
        handler: AgentActionHandler
    ) {
        val key = key(scheduleId, actionKey)
        if (!ledger.begin(key, actionKey in DayAgentPreferences.getAppliedActions(context, scheduleId))) return
        val appContext = context.applicationContext
        val conversationKey = AgentConversationStore.selected(appContext, scheduleId, date)
        fun finish(result: AgentPlanExecutionResult) {
            finishAgentCreationOnce(ledger, key, result, onVerifiedSuccess = {
                DayAgentPreferences.markActionApplied(appContext, scheduleId, actionKey)
                val app = appContext as CourseScheduleApp
                app.applicationScope.launch(Dispatchers.IO) {
                    try {
                        app.database.agentDao().insertMessage(AgentMessageEntity(scheduleId = scheduleId,
                            sessionDate = conversationKey, role = "assistant", content = "已创建。${result.message}",
                            createdAt = System.currentTimeMillis(), status = "READY"))
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) { Log.w("ShixuAgent", "Save receipt history failed: ${error.javaClass.simpleName}") }
                }
            }, onComplete = onComplete)
        }
        try {
            AgentCreationPlanRunner(ledger).run(key, plan, handler, ::finish)
        } catch (error: CancellationException) { ledger.abandon(key); throw error }
        catch (error: Exception) {
            val failure = AgentPlanExecutionResult(false, null, false, "保存失败，请重试；尚未确认创建成功")
            if (!finishAgentCreationOnce(ledger, key, failure, onComplete = onComplete)) {
                Log.w("ShixuAgent", "Save completion callback failed: ${error.javaClass.simpleName}")
            }
        }
    }
}

internal fun finishAgentCreationOnce(
    ledger: AgentCreationReceiptLedger,
    key: String,
    result: AgentPlanExecutionResult,
    onVerifiedSuccess: () -> Unit = {},
    onComplete: ((AgentPlanExecutionResult) -> Unit)? = null
): Boolean {
    if (!ledger.finish(key, result)) return false
    if (result.success && result.verified) onVerifiedSuccess()
    onComplete?.invoke(result)
    return true
}

internal data class AgentCreationBatch(
    val startIndex: Int,
    val endExclusive: Int,
    val plan: AgentPlan
) {
    val key: String = "${plan.actions.firstOrNull()?.type}:$startIndex:$endExclusive"
}

/** Mirrors the home action handler's transaction boundaries for creation plans. */
internal fun agentCreationBatches(plan: AgentPlan): List<AgentCreationBatch> {
    val actions = plan.actions
    if (actions.isEmpty()) return listOf(AgentCreationBatch(0, 0, plan))
    if (actions.all { it.type == AgentValidatedActionType.ADD } ||
        actions.all { it.type == AgentValidatedActionType.CREATE_TODO }) {
        return listOf(AgentCreationBatch(0, actions.size, plan))
    }

    val batches = mutableListOf<AgentCreationBatch>()
    var index = 0
    while (index < actions.size) {
        val type = actions[index].type
        val endExclusive = when {
            type == AgentValidatedActionType.ADD -> {
                var end = index + 1
                while (end < actions.size && actions[end].type == AgentValidatedActionType.ADD) end++
                end
            }
            type == AgentValidatedActionType.CREATE_TODO &&
                actions.drop(index).all { it.type == AgentValidatedActionType.CREATE_TODO } -> actions.size
            else -> index + 1
        }
        batches += AgentCreationBatch(index, endExclusive, AgentPlan(actions.subList(index, endExclusive)))
        index = endExclusive
    }
    return batches
}

/** Retries only unverified batches from a partially completed confirmed creation plan. */
internal class AgentCreationPlanRunner(private val ledger: AgentCreationReceiptLedger) {
    fun run(
        key: String,
        plan: AgentPlan,
        handler: AgentActionHandler,
        onResult: (AgentPlanExecutionResult) -> Unit
    ) {
        val batches = agentCreationBatches(plan)
        val completed = ledger.successfulBatches(key)
        val messages = mutableListOf<String>()
        var index = 0

        fun runNext() {
            while (index < batches.size) {
                val batch = batches[index++]
                val previous = completed[batch.key]
                if (previous != null) {
                    messages += previous.message
                    continue
                }
                val callbackConsumed = AtomicBoolean(false)
                handler(batch.plan) resultCallback@{ result ->
                    if (!callbackConsumed.compareAndSet(false, true)) return@resultCallback
                    if (!result.success || !result.verified) {
                        if (batches.size == 1 && messages.isEmpty()) {
                            onResult(result)
                            return@resultCallback
                        }
                        val prior = messages.filter(String::isNotBlank)
                        val prefix = if (prior.isEmpty()) "" else "此前批次已完成，重试时将跳过。\n${prior.joinToString("\n")}\n"
                        onResult(result.copy(message = prefix + result.message, undo = null))
                        return@resultCallback
                    }
                    ledger.recordBatchSuccess(key, batch.key, result)
                    if (batches.size == 1) {
                        onResult(result)
                        return@resultCallback
                    }
                    messages += result.message
                    runNext()
                }
                return
            }
            onResult(AgentPlanExecutionResult(
                success = true,
                preview = null,
                verified = true,
                message = messages.filter(String::isNotBlank).joinToString("\n").ifBlank { "已完成" }
            ))
        }

        runNext()
    }
}
