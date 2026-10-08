package com.xiaomanjun.sleepdownschedule.feature.agent.voice

/** Consume responses once while retaining cancelled request order until response.created arrives. */
internal class NativeResponseTracker {
    private val requests = ArrayDeque<Long>()
    private val turns = LinkedHashMap<String, Long>()
    private var activeId = ""
    fun enqueue(turn: Long) { requests.addLast(turn) }
    fun seen(id: String) = id in turns
    fun started(id: String, currentTurn: Long): Boolean {
        if (id.isBlank() || seen(id)) return false
        val expected = requests.removeFirstOrNull()
        if (expected != currentTurn) return false
        turns[id] = currentTurn
        if (turns.size > 64) turns.remove(turns.keys.first())
        activeId = id
        return true
    }
    fun accepts(id: String, turn: Long) = id.isNotBlank() && id == activeId && turns[id] == turn
    fun consume(id: String, turn: Long): Boolean {
        if (!accepts(id, turn)) return false
        activeId = ""
        return true
    }
    fun interrupt() { activeId = "" }
    fun reset() { requests.clear(); turns.clear(); activeId = "" }
}
