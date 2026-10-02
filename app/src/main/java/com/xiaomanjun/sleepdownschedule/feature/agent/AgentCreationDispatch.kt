package com.xiaomanjun.sleepdownschedule.feature.agent

import android.content.Context
import android.util.Log
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.model.AgentMessageEntity
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** An overlay can disappear while a confirmed save finishes. Its receipt outlives that overlay. */
internal object AgentCreationDispatch {
    private val ledger = AgentCreationReceiptLedger()
    val receipts = ledger.receipts
    fun key(scheduleId: Int, actionKey: String) = "$scheduleId:$actionKey"

    @Synchronized
    fun submit(context: Context, scheduleId: Int, date: LocalDate, actionKey: String, plan: AgentPlan, handler: AgentActionHandler) {
        val key = key(scheduleId, actionKey)
        if (!ledger.begin(key, actionKey in DayAgentPreferences.getAppliedActions(context, scheduleId))) return
        val appContext = context.applicationContext
        val conversationKey = AgentConversationStore.selected(appContext, scheduleId, date)
        try {
            handler(plan) { result ->
                if (!ledger.finish(key, result)) return@handler
                if (result.success && result.verified) {
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
                }
            }
        } catch (error: CancellationException) { ledger.abandon(key); throw error }
        catch (error: Exception) {
            ledger.finish(key, AgentPlanExecutionResult(false, null, false, "保存失败，请重试；尚未确认创建成功"))
        }
    }
}
