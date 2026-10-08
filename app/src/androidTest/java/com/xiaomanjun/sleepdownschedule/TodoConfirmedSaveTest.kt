package com.xiaomanjun.sleepdownschedule

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoDraft
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoConfirmedSaveTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), AppDatabase::class.java
    ).build()
    private val dao = database.todoDao()
    private val repository = TodoRepository(dao)

    @After fun closeDatabase() { database.close() }

    @Test fun confirmedDraftSavesParentAndAllExtractedChildrenTogether() = runBlocking {
        val id = repository.saveWithSubtasks(TodoDraft(title = "会议", description = "材料", priority = 2),
            listOf("准备", "复核"))
        val items = dao.getAllItems()
        assertEquals(3, items.size)
        assertEquals("会议", items.single { it.id == id }.title)
        assertEquals(setOf("准备", "复核"), items.filter { it.parentId == id }.map { it.title }.toSet())
        assertTrue(items.all { it.reminderMode == "NONE" && it.groupId == null })
    }

    @Test fun failingChildWriteRollsBackParentAndEarlierChildren() = runBlocking {
        val failure = runCatching {
            dao.upsertWithSubtasks(TodoItemEntity(title = "父任务"),
                listOf(TodoItemEntity(title = "有效子任务"), TodoItemEntity(title = "失败子任务", groupId = 999)))
        }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(dao.getAllItems().isEmpty())
    }

    @Test fun failingChildWriteAlsoRollsBackAnExistingParentEdit() = runBlocking {
        val id = repository.save(TodoDraft(title = "原标题"))
        val original = requireNotNull(dao.getById(id))
        val failure = runCatching {
            dao.upsertWithSubtasks(original.copy(title = "新标题"), listOf(TodoItemEntity(title = "子任务", groupId = 999)))
        }.exceptionOrNull()
        assertNotNull(failure)
        assertEquals(original, dao.getById(id))
        assertEquals(1, dao.getAllItems().size)
    }

    @Test fun invalidExtractedChildDoesNotLeaveAPartiallySavedDraft() = runBlocking {
        val failure = runCatching {
            repository.saveWithSubtasks(TodoDraft(title = "父任务"), listOf("有效", " "))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(dao.getAllItems().isEmpty())
    }

    @Test fun draftArchivedBeforeTheWriteCannotBeRestoredByConfirmingIt() = runBlocking {
        val id = repository.save(TodoDraft(title = "旧草稿"))
        val draftSnapshot = requireNotNull(dao.getById(id))
        dao.archiveItems(listOf(id), "test-batch", 123L)
        val archived = dao.getById(id)
        val failure = runCatching { dao.upsertWithSubtasks(draftSnapshot, emptyList()) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(archived, dao.getById(id))
    }
}
