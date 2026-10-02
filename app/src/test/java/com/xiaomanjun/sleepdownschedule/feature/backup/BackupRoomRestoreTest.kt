package com.xiaomanjun.sleepdownschedule.feature.backup

import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.feature.agent.*
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoCalendarSyncState

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRoomRestoreTest {
    @Test
    fun archivedRangeAndReminderFieldsRoundTripWithoutDeviceCalendarIds() {
        val item = com.xiaomanjun.sleepdownschedule.feature.todo.TodoItemEntity(
            id = 92, title = "归档任务", dueAt = 1_800_000_000_000L, endAt = 1_800_003_600_000L,
            reminderMode = "BEFORE", reminderOffsetMinutes = 30, persistentReminder = true, strongReminder = true,
            deletedAt = 1_800_000_000_000L, deletionBatch = "batch-a", calendarEventId = 456,
            calendarSyncToken = "11111111-1111-4111-8111-111111111111", calendarSyncState = TodoCalendarSyncState.LINKED
        )
        val archive = BackupExportMapper.toArchive(metadata(), BackupRoomSnapshot(todoItems = listOf(item)),
            BackupPreferences(BackupFormatV1.PREFERENCES_VERSION))
        val decoded = BackupCodec.decode(BackupCodec.encode(archive))
        val restored = BackupRoomRestoreMapper.map(decoded, BackupImportPlanBuilder.build(decoded, "trash-reminder-roundtrip")).todoItems.single()
        assertEquals(item.endAt, restored.endAt)
        assertEquals("BEFORE", restored.reminderMode)
        assertEquals(30, restored.reminderOffsetMinutes)
        assertTrue(restored.persistentReminder); assertTrue(restored.strongReminder)
        assertEquals(item.deletedAt, restored.deletedAt); assertEquals("batch-a", restored.deletionBatch)
        assertNull(restored.calendarEventId)
    }

    @Test
    fun legacyScheduledTodoRequiresCalendarConfirmationAfterRestore() {
        val item = BackupTodoItem(
            id = BackupStableId.new(BackupStableId.TODO_PREFIX),
            title = "旧备份任务",
            description = "",
            dueAt = 1_800_000_000_000L,
            allDay = false,
            priority = 1,
            isCompleted = false,
            isPinned = false,
            groupId = null,
            parentId = null,
            courseId = null,
            repeatRule = "NONE",
            createdAt = 1L,
            updatedAt = 1L,
            completedAt = null
        )
        val archive = BackupArchive(
            metadata = metadata(),
            data = BackupData(dataVersion = BackupFormatV1.DATA_VERSION, todoItems = listOf(item)),
            preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION)
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(archive))
        val plan = BackupImportPlanBuilder.build(decoded, "legacy-calendar-review")
        val restored = BackupRoomRestoreMapper.map(decoded, plan).todoItems.single()

        assertNull(restored.calendarSyncToken)
        assertNull(restored.calendarEventId)
        assertEquals(TodoCalendarSyncState.NEEDS_CONFIRMATION, restored.calendarSyncState)
        assertEquals("LEGACY", restored.reminderMode)
        assertNull(restored.deletedAt)
    }

    @Test
    fun calendarMarkerRoundTripsButExternalProviderRowIdDoesNot() {
        val item = com.xiaomanjun.sleepdownschedule.feature.todo.TodoItemEntity(
            id = 91L,
            title = "跨设备日历任务",
            description = "",
            dueAt = 1_800_000_000_000L,
            calendarEventId = 456L,
            calendarSyncToken = "11111111-1111-4111-8111-111111111111",
            calendarSyncState = TodoCalendarSyncState.LINKED
        )
        val archive = BackupExportMapper.toArchive(
            metadata = metadata(),
            snapshot = BackupRoomSnapshot(todoItems = listOf(item)),
            preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION)
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(archive))
        val todo = decoded.data.todoItems.single()
        val plan = BackupImportPlanBuilder.build(decoded, "calendar-token-roundtrip")
        val restored = BackupRoomRestoreMapper.map(decoded, plan).todoItems.single()

        assertEquals(item.calendarSyncToken, todo.calendarSyncToken)
        assertEquals(TodoCalendarSyncState.LINKED, todo.calendarSyncState)
        assertEquals(item.calendarSyncToken, restored.calendarSyncToken)
        assertEquals(TodoCalendarSyncState.LINKED, restored.calendarSyncState)
        assertNull(restored.calendarEventId)
    }

    @Test
    fun restoreMapperUsesPlanIdsAndRebuildsPrivateAgentMarker() {
        val archive = BackupCodec.decode(
            BackupCodec.encode(
                BackupExportMapper.toArchive(
                    metadata = metadata(),
                    snapshot = BackupRoomSnapshot(
                        schedules = listOf(ScheduleProfileEntity(7, "主课表", true)),
                        configs = listOf(defaultConfig(7).copy(wallpaperUri = "content://wallpaper")),
                        periods = listOf(PeriodEntity(1, "08:00", "08:45", 7)),
                        courses = listOf(
                            CourseEntity(
                                id = 101,
                                name = "课程",
                                teacher = null,
                                location = null,
                                weekday = 1,
                                periods = listOf(1),
                                weeks = listOf(1),
                                weekParity = WeekParity.ALL,
                                note = null,
                                customStartTime = "08:12",
                                customEndTime = "08:38",
                                customPeriodTimes = "1,08:12-08:38",
                                customColorArgb = 0xFF6688AAL,
                                scheduleId = 7
                            )
                        ),
                        agentMessages = listOf(
                            AgentMessageEntity(
                                id = 601,
                                scheduleId = 7,
                                sessionDate = "2026-08-10",
                                role = "user",
                                content = "[[agent_image:11111111-1111-1111-1111-111111111111.jpg]]\n看图",
                                createdAt = 1L,
                                status = "READY"
                            )
                        )
                    ),
                    preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION),
                    assetInputs = listOf(
                        BackupExportAssetInput(
                            sourceKey = "schedule-wallpaper:7:content://wallpaper",
                            bytes = byteArrayOf(1, 2, 3),
                            mediaType = "image/png"
                        ),
                        BackupExportAssetInput(
                            sourceKey = "agent-attachment:7:601:11111111-1111-1111-1111-111111111111.jpg",
                            bytes = byteArrayOf(4, 5, 6),
                            mediaType = "image/jpeg"
                        )
                    )
                )
            )
        )
        val plan = BackupImportPlanBuilder.build(
            archive = archive,
            operationId = "restore-mapper",
            existing = BackupImportTargetSnapshot(scheduleIds = setOf(7), courseIds = setOf(101))
        )
        val scheduleId = plan.scheduleIds.values.single()
        val wallpaperId = archive.data.schedules.single().config.wallpaperAssetId!!
        val attachmentId = archive.data.schedules.single().agentMessages.single().attachmentAssetIds.single()

        val rows = BackupRoomRestoreMapper.map(
            archive = archive,
            plan = plan,
            assetUrisById = mapOf(wallpaperId to "file:///data/wallpaper", attachmentId to "file:///data/agent"),
            attachmentFileNamesByAssetId = mapOf(
                attachmentId to "11111111-1111-1111-1111-111111111111.jpg"
            )
        )

        assertEquals(scheduleId, rows.schedules.single().id)
        assertEquals(scheduleId, rows.configs.single().id)
        assertEquals("file:///data/wallpaper", rows.configs.single().wallpaperUri)
        assertEquals(scheduleId, rows.courses.single().scheduleId)
        assertEquals("08:12", rows.courses.single().customStartTime)
        assertEquals("08:38", rows.courses.single().customEndTime)
        assertEquals("1,08:12-08:38", rows.courses.single().customPeriodTimes)
        assertEquals(0xFF6688AAL, rows.courses.single().customColorArgb)
        assertEquals(
            "[[agent_image:11111111-1111-1111-1111-111111111111.jpg]]\n看图",
            rows.agentMessages.single().content
        )
    }

    @Test
    fun missingWallpaperIsNotReplacedWithAnImplicitDefault() {
        val archive = BackupCodec.decode(
            BackupCodec.encode(
                BackupExportMapper.toArchive(
                    metadata = metadata(),
                    snapshot = BackupRoomSnapshot(
                        schedules = listOf(ScheduleProfileEntity(7, "主课表", true)),
                        configs = listOf(defaultConfig(7).copy(wallpaperUri = "content://lost"))
                    ),
                    preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION)
                )
            )
        )
        val plan = BackupImportPlanBuilder.build(archive, "restore-missing-wallpaper")
        val wallpaperId = archive.data.schedules.single().config.wallpaperAssetId!!
        val rows = BackupRoomRestoreMapper.map(archive, plan)

        assertNull(rows.configs.single().wallpaperUri)
        assertEquals(BackupImportAssetAction.WARN_MISSING, plan.assetTargets.getValue(wallpaperId).action)
        assertTrue(plan.warnings.any { it.code == BackupImportWarningCode.MISSING_ASSET })
    }

    @Test
    fun noActiveScheduleExplicitlySelectsFirstSchedule() {
        val archive = BackupCodec.decode(
            BackupCodec.encode(
                BackupExportMapper.toArchive(
                    metadata = metadata(),
                    snapshot = BackupRoomSnapshot(
                        schedules = listOf(
                            ScheduleProfileEntity(7, "第一", false),
                            ScheduleProfileEntity(9, "第二", false)
                        ),
                        configs = listOf(defaultConfig(7), defaultConfig(9))
                    ),
                    preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION)
                )
            )
        )
        val plan = BackupImportPlanBuilder.build(archive, "restore-no-active")
        val rows = BackupRoomRestoreMapper.map(archive, plan)

        assertTrue(plan.warnings.any { it.code == BackupImportWarningCode.NO_ACTIVE_SCHEDULE_SELECTED })
        assertTrue(rows.schedules.first().isActive)
        assertFalse(rows.schedules.drop(1).any { it.isActive })
    }

    @Test
    fun restoreJournalRoundTripsMarkerPayloadWithoutWritingLiveData() {
        val temporaryRoot = Files.createTempDirectory("sleepdown-restore-journal").toFile()
        try {
            val archive = BackupCodec.decode(
                BackupCodec.encode(
                    BackupExportMapper.toArchive(
                        metadata = metadata(),
                        snapshot = BackupRoomSnapshot(),
                        preferences = BackupPreferences(BackupFormatV1.PREFERENCES_VERSION)
                    )
                )
            )
            val plan = BackupImportPlanBuilder.build(archive, "restore-journal")
            val journal = BackupRestoreJournal(temporaryRoot, "restore-journal")
            val marker = BackupRestoreMarker(
                operationId = "restore-journal",
                archiveFingerprint = "a".repeat(64),
                state = BackupRestoreState.STAGED,
                plan = plan,
                finalAssetPathsById = mapOf("asset" to FilePathSentinel),
                dbCommitStarted = true
            )
            journal.writeMarker(marker)
            journal.writePreferences(archive.preferences)
            journal.writePayload(BackupRestorePayload.fromArchive(archive))

            assertEquals(marker, journal.readMarker())
            assertEquals(archive.preferences, journal.readPreferences())
            assertEquals(archive.data, journal.readPayload().data)
        } finally {
            temporaryRoot.deleteRecursively()
        }
    }

    private fun metadata() = BackupSourceMetadata(
        createdAt = "2026-08-10T12:00:00Z",
        sourceAppVersionName = "1.1.4",
        sourceVersionCode = 24,
        sourcePackageName = "com.example.courseschedule",
        sourceDatabaseVersion = APP_DATABASE_VERSION,
        devicePlatform = "Android"
    )

    private companion object {
        const val FilePathSentinel = "D:/sleepdown-test/asset"
    }
}
