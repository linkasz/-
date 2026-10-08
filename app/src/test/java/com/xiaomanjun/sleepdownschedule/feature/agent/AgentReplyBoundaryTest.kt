package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.model.PeriodEntity
import com.xiaomanjun.sleepdownschedule.feature.agent.voice.voiceReplyText
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.Instant

class AgentReplyBoundaryTest {
    private val date = LocalDate.of(2026, 10, 1)
    private val facts = DayAgentFacts(date, date.atTime(14, 36), emptyList(), emptyList(), emptyList(), null, "snapshot",
        periodDefinitions = listOf(PeriodEntity(1, "08:00", "08:45")), timeZoneId = "Asia/Shanghai")
    private val todo = """[{"type":"CREATE_TODO","todo":{"title":"开会","date":"2026-10-02","time":"10:00"}}]"""

    @Test fun naturalProposalHasCheckedTimeAndNoProtocol() {
        val parsed = parseAgentActions("调用 CREATE_TODO 工具。<agent_actions>$todo</agent_actions>", facts)
        assertEquals("已拟好明天 10:00的「开会」待办，请确认。", parsed.displayText)
        assertEquals(Instant.parse("2026-10-02T02:00:00Z").toEpochMilli(), parsed.actions.single().todo!!.dueAt)
        assertFalse(voiceReplyText(parsed.displayText, true).contains("展开"))
        assertEquals("NONE", parsed.actions.single().todo!!.reminderMode)
    }

    @Test fun reportedAfternoonTenNeedsClarificationWithoutAModelCall() {
        assertTrue(ambiguousAgentTimeQuestion("帮我新建待办。我明天下午十点要开会。")!!.contains("22"))
        assertNull(ambiguousAgentTimeQuestion("明天上午十点开会"))
        assertNull(ambiguousAgentTimeQuestion("明天晚上十点开会"))
    }

    @Test fun factBracketsDoNotReplaceTheOperationArray() {
        val text = """{"kind":"facts","results":[{"items":[]}]} 已拟好。 $todo"""
        assertEquals(todo, looseAgentActionPayload(text))
        assertEquals("开会", parseAgentActions(text, facts).actions.single().todo!!.title)
    }

    @Test fun quotedBracketsAreNotJsonBoundaries() {
        val payload = todo.replace("开会", "开会[准备材料]")
        assertEquals("开会[准备材料]", parseAgentActions("请确认。$payload", facts).actions.single().todo!!.title)
    }

    @Test fun emptyPlanIsAnAnswerAndNotARejectedOperation() {
        val result = parseAgentActions("请问你想安排什么？<agent_actions>[]</agent_actions>", facts)
        assertTrue(result.validationIssues.isEmpty())
        assertTrue(result.actions.isEmpty())
    }

    @Test fun incompleteOrRepeatedPlanNeverCrossesTheReplyBoundary() {
        listOf("请确认。<agent_actions>$todo", "<agent_actions>$todo</agent_actions><agent_actions>[]</agent_actions>")
            .forEach { assertTrue(runCatching { checkedAgentAnswer(it, facts) }.exceptionOrNull() is AgentProtocolViolationException) }
    }

    @Test fun invalidDateHasAnIssueAndNeverCreatesAPartialPlan() {
        val parsed = parseAgentActions("<agent_actions>${todo.replace("2026-10-02", "2026-99-02")}</agent_actions>", facts)
        assertTrue(parsed.actions.isEmpty())
        assertTrue(parsed.validationIssues.any { it.contains("日期") })
    }

    @Test fun reproducedClockAndFactEchoIsBlockedBeforeAnyDelta() {
        val raw = "计划为空时输出。[本轮可信时钟] 当前日期2026-10-01 verified_cached_local_facts $todo"
        val shown = mutableListOf<String>()
        val gate = AgentFinalOutputGate(shown::add, validate = { checkedAgentAnswer(it, facts) })
        raw.chunked(9).forEach(gate::accept)
        assertTrue(runCatching { gate.finish(raw) }.exceptionOrNull() is AgentProtocolViolationException)
        assertTrue(shown.isEmpty())
        assertEquals("", sanitizeAgentToolOutput(raw))
    }

    @Test fun validReplyAppearsOnlyAfterTheWholeDraftIsChecked() {
        val text = "已拟好，请确认。<agent_actions>$todo</agent_actions>"
        val shown = mutableListOf<String>()
        val gate = AgentFinalOutputGate(shown::add, validate = { checkedAgentAnswer(it, facts) })
        text.chunked(13).forEach(gate::accept)
        assertTrue(shown.isEmpty())
        assertEquals(text, gate.finish(text))
        assertEquals(listOf(text), shown)
    }

    @Test fun editedTodoReturnsOnlyAnotherDraftWithNoId() {
        val plan = AgentPlan(parseAgentActions("<agent_actions>$todo</agent_actions>", facts).actions)
        val fields = plan.actions.map { creationFields(it, facts).copy(title = "项目会议", time = "11:30") }
        val result = editedCreationPlan(plan, fields, facts)
        assertTrue(result.validationIssues.isEmpty())
        val draft = result.actions.single().todo!!
        assertEquals(0L, draft.id)
        assertNull(draft.groupId)
        assertEquals("项目会议", draft.title)
        assertEquals(Instant.parse("2026-10-02T03:30:00Z").toEpochMilli(), draft.dueAt)
    }

    @Test fun courseAndScheduleCreationAlsoHaveEditableCheckedDrafts() {
        listOf("""[{"type":"ADD_COURSE","course":{"name":"高数","weekday":5,"periods":[1],"weeks":[1]}}]""",
            """[{"type":"CREATE_SCHEDULE","name":"新学期"}]""").forEach { json ->
            val plan = AgentPlan(parseAgentActions("<agent_actions>$json</agent_actions>", facts).actions)
            assertTrue(plan.isCreation())
            val edited = editedCreationPlan(plan, plan.actions.map { creationFields(it, facts).copy(title = "新名称") }, facts)
            assertTrue(edited.validationIssues.isEmpty())
            assertEquals(1, edited.actions.size)
        }
    }

    @Test fun arbitraryResizeHeightStaysContinuousAndReclampsForKeyboard() {
        assertEquals(377.25f, constrainedAssistantHeight(377.25f, 220f, 800f))
        assertEquals(220f, constrainedAssistantHeight(100f, 220f, 800f))
        assertEquals(800f, constrainedAssistantHeight(900f, 220f, 800f))
        assertEquals(190f, constrainedAssistantHeight(377.25f, 220f, 190f))
    }
}
