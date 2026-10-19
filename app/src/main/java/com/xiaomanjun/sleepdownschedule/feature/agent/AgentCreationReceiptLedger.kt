package com.xiaomanjun.sleepdownschedule.feature.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One claim per confirmed proposal, independent of either presentation's lifetime. */
internal class AgentCreationReceiptLedger {
    private val mutable = MutableStateFlow<Map<String, AgentPlanExecutionResult?>>(emptyMap())
    private val completedBatches = mutableMapOf<String, Map<String, AgentPlanExecutionResult>>()
    val receipts = mutable.asStateFlow()

    @Synchronized
    fun begin(key: String, alreadyApplied: Boolean): Boolean {
        val previous = mutable.value[key]
        if (alreadyApplied || key in mutable.value && (previous == null || previous.success && previous.verified)) return false
        mutable.value = mutable.value + (key to null)
        return true
    }

    @Synchronized
    fun finish(key: String, result: AgentPlanExecutionResult): Boolean {
        if (key !in mutable.value || mutable.value[key] != null) return false
        mutable.value = mutable.value + (key to result)
        val completed = mutable.value.filterValues { it != null }.keys
        if (completed.size > 128) {
            val evicted = completed.take(completed.size - 128).toSet()
            mutable.value = mutable.value - evicted
            evicted.forEach(completedBatches::remove)
        }
        return true
    }

    @Synchronized
    fun successfulBatches(key: String): Map<String, AgentPlanExecutionResult> =
        completedBatches[key].orEmpty()

    @Synchronized
    fun recordBatchSuccess(key: String, batchKey: String, result: AgentPlanExecutionResult) {
        require(result.success && result.verified) { "Only verified successful batches can be checkpointed" }
        completedBatches[key] = completedBatches[key].orEmpty() + (batchKey to result)
    }

    @Synchronized
    fun abandon(key: String) {
        if (key in mutable.value && mutable.value[key] == null) mutable.value = mutable.value - key
    }
}
