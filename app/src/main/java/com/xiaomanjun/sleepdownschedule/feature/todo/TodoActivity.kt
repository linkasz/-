package com.xiaomanjun.sleepdownschedule.feature.todo

import com.xiaomanjun.sleepdownschedule.feature.home.HomePageEntrance
import com.xiaomanjun.sleepdownschedule.feature.home.homeSwitchGroup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.model.AppState
import com.xiaomanjun.sleepdownschedule.model.CourseEntity
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.app.ui.LiquidControlToggle
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.CenterLiquidDialog
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogCapsuleField
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogButtonRole
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogLiquidButton
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidDialogHeader
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.QuickSheetLiquidAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.quickSheetBackdropModifier
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.sleepDownGlassForegroundColor
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidCascadingPopup
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidMenuItem
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassTokens
import com.xiaomanjun.sleepdownschedule.glass.ui.CourseGlassCard
import com.xiaomanjun.sleepdownschedule.glass.ui.LocalCourseCardPalette
import com.xiaomanjun.sleepdownschedule.glass.ui.DefaultCourseCardPalette
import com.xiaomanjun.sleepdownschedule.glass.ui.courseCardBaseColor
import com.xiaomanjun.sleepdownschedule.feature.home.LocalPersonalizationPreview
import com.xiaomanjun.sleepdownschedule.feature.home.adaptiveWeekCardCornerRadius
import com.xiaomanjun.sleepdownschedule.feature.home.currentWindowSizeDp
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.Capsule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardToMain(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardToMain(intent)
    }

    private fun forwardToMain(incoming: Intent?) {
        val forwarded = Intent(incoming ?: Intent()).setClass(this, com.xiaomanjun.sleepdownschedule.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(forwarded)
        finish()
    }
}

internal class TodoChromeActions {
    var onConfigureAi: () -> Unit = {}
    var onAddTodo: () -> Unit = {}
}

private data class TodoEditState(
    val source: TodoItemEntity? = null,
    val sourceId: Long? = source?.id,
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val priority: Int = 1,
    val pinned: Boolean = false,
    val groupId: Long? = null,
    val courseId: Long? = null,
    val parentId: Long? = null,
    val repeatRule: String = "NONE",
    val extractedSubtasks: List<String> = emptyList(),
    val endTime: String = "",
    val reminderMode: String = "NONE",
    val reminderOffset: Int = 0,
    val reminderTime: String = "08:00",
    val persistentReminder: Boolean = false,
    val strongReminder: Boolean = false
)

private fun TodoEditState.toSavedValue(): List<Any?> = listOf(
        sourceId,
    title,
    description,
    date,
    time,
    priority,
    pinned,
    groupId,
    courseId,
    parentId,
    repeatRule,
    extractedSubtasks, endTime, reminderMode, reminderOffset, reminderTime, persistentReminder, strongReminder
)

private fun restoreTodoEditState(values: List<*>): TodoEditState {
    val title = values.getOrNull(1) as? String ?: ""
    val sourceId = (values.getOrNull(0) as? Number)?.toLong()
    return TodoEditState(
        sourceId = sourceId,
        title = title,
        description = values.getOrNull(2) as? String ?: "",
        date = values.getOrNull(3) as? String ?: "",
        time = values.getOrNull(4) as? String ?: "",
        priority = (values.getOrNull(5) as? Number)?.toInt() ?: 1,
        pinned = values.getOrNull(6) as? Boolean ?: false,
        groupId = (values.getOrNull(7) as? Number)?.toLong(),
        courseId = (values.getOrNull(8) as? Number)?.toLong(),
        parentId = (values.getOrNull(9) as? Number)?.toLong(),
        repeatRule = values.getOrNull(10) as? String ?: "NONE",
        extractedSubtasks = (values.getOrNull(11) as? List<*>)?.filterIsInstance<String>().orEmpty(),
        endTime = values.getOrNull(12) as? String ?: "",
        reminderMode = values.getOrNull(13) as? String ?: "NONE",
        reminderOffset = (values.getOrNull(14) as? Number)?.toInt() ?: 0,
        reminderTime = values.getOrNull(15) as? String ?: "08:00",
        persistentReminder = values.getOrNull(16) as? Boolean ?: false,
        strongReminder = values.getOrNull(17) as? Boolean ?: false
    )
}

private val TodoEditStateSaver = Saver<TodoEditState, List<Any?>>(save = { it.toSavedValue() }, restore = ::restoreTodoEditState)
private val NullableTodoEditStateSaver = Saver<TodoEditState?, List<Any?>>(save = { it?.toSavedValue() }, restore = { values ->
    values?.let(::restoreTodoEditState)
})

internal val TodoEntryRequestSaver = Saver<TodoEntryRequest?, List<Any?>>(save = { request ->
    request?.let {
        listOf(
            it.id,
            it.destination.name,
            it.todoId,
            it.courseId,
            it.startWithNewTask,
            it.sharedText,
            it.imageUris.map(Uri::toString),
            it.screenshotPath,
            it.requestShizukuPermission,
            it.returnToCourse
        )
    }
}, restore = { values ->
    values?.let {
        TodoEntryRequest(
            id = (it[0] as? Number)?.toLong() ?: 0L,
            destination = runCatching { TodoPage.valueOf(it[1] as? String ?: "Tasks") }
                .getOrDefault(TodoPage.Tasks),
            todoId = (it[2] as? Number)?.toLong(),
            courseId = (it[3] as? Number)?.toLong(),
            startWithNewTask = it[4] as? Boolean ?: false,
            sharedText = it[5] as? String ?: "",
            imageUris = (it[6] as? List<*>)?.filterIsInstance<String>()?.map(Uri::parse).orEmpty(),
            screenshotPath = it[7] as? String,
            requestShizukuPermission = it[8] as? Boolean ?: false,
            returnToCourse = it[9] as? Boolean ?: false
        )
    }
})

private enum class TodoEditorMenu {
    Group,
    Course,
    Repeat
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TodoPlusScreen(
    viewModel: TodoViewModel,
    config: ScheduleConfigEntity,
    schedule: AppState,
    backdrop: Backdrop?,
    page: TodoPage,
    onPageChange: (TodoPage) -> Unit,
    outerTransitionActive: Boolean = false,
    chromeActions: TodoChromeActions,
    entryRequest: TodoEntryRequest? = null,
    onEntryRequestConsumed: (Long) -> Unit = {},
    onRequestShizukuPermission: () -> Unit = {},
    initialCourseId: Long? = null,
    startWithNewTask: Boolean = false,
    initialTodoId: Long? = null,
    onInitialCourseConsumed: () -> Unit = {},
    onInitialTodoConsumed: () -> Unit = {},
    requestCalendarPermission: () -> Unit,
    requestAutoCalendarPermission: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var previousPage by remember { mutableStateOf(page) }
    // Keep the direction stable through the whole destination composition, including reversals.
    val pageEntrance = remember(page) { (page != previousPage) to if (page.ordinal >= previousPage.ordinal) 1f else -1f }
    SideEffect { previousPage = page }
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val calendarMessage by viewModel.calendarMessage.collectAsStateWithLifecycle()
    val calendarReview by viewModel.calendarReview.collectAsStateWithLifecycle()
    val operationError by viewModel.operationError.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val autoSync by viewModel.autoCalendarSync.collectAsStateWithLifecycle()
    val glassForeground = sleepDownGlassForegroundColor(config)
    val coursePalette = LocalCourseCardPalette.current.ifEmpty { DefaultCourseCardPalette }
    val groupColorArgb = remember(state.groups, coursePalette) {
        buildTodoGroupColorAssignments(state.groups, coursePalette)
    }
    val groupColors = remember(groupColorArgb) {
        groupColorArgb.mapValues { Color(it.value.toInt()) }
    }
    val courseColors = schedule.courses.associate { course ->
        course.id to courseCardBaseColor(config, course)
    }
    val defaultTodoColor = groupColors[state.groups.firstOrNull()?.id]
        ?: Color(coursePalette.first().toInt())
    val todoItemColor: (TodoItemEntity) -> Color = remember(courseColors, groupColors, defaultTodoColor) {
        { item ->
            item.courseId?.let(courseColors::get)
                ?: item.groupId?.let(groupColors::get)
                ?: defaultTodoColor
        }
    }
    val themedPrimary = Color(coursePalette.first().toInt())
    val themedSecondary = Color(coursePalette.getOrElse(1) { coursePalette.first() }.toInt())
    val themedTertiary = Color(coursePalette.getOrElse(2) { coursePalette.first() }.toInt())
    val todoColorScheme = MaterialTheme.colorScheme.copy(
        primary = themedPrimary,
        secondary = themedSecondary,
        tertiary = themedTertiary,
        onSurface = glassForeground,
        onSurfaceVariant = glassForeground.copy(alpha = 0.72f),
        surfaceVariant = glassForeground.copy(alpha = 0.10f)
    )
    val currentDensity = LocalDensity.current
    val previewFontScale = LocalPersonalizationPreview.current?.cardFontScale
        ?: config.courseCardFontScale
    val todoDensity = remember(currentDensity, previewFontScale) {
        Density(currentDensity.density, currentDensity.fontScale * previewFontScale.coerceIn(0.75f, 1.5f))
    }
    var editor by rememberSaveable(stateSaver = NullableTodoEditStateSaver) {
        mutableStateOf(initialCourseId?.takeIf { startWithNewTask }?.let { TodoEditState(courseId = it) })
    }
    var editorVisible by remember { mutableStateOf(editor != null) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable(stateSaver = Saver<TodoFilter, List<Any?>>(
        save = { listOf(it.kind.name, it.groupId) },
        restore = { TodoFilter(TodoFilterKind.valueOf(it[0] as String), (it[1] as? Number)?.toLong()) }
    )) { mutableStateOf(TodoFilter()) }
    var filterCourse by rememberSaveable { mutableStateOf(initialCourseId) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val context = LocalContext.current
    var completionPulse by remember { mutableIntStateOf(0) }
    var soundEnabled by remember(context) {
        mutableStateOf(context.getSharedPreferences("schedule_plus_todo", android.content.Context.MODE_PRIVATE)
            .getBoolean("completion_sound", true))
    }
    val haptics = LocalHapticFeedback.current
    fun openEditor(value: TodoEditState) {
        if (viewModel.saving.value) return
        editor = value
        editorVisible = true
    }
    fun closeEditor() {
        editorVisible = false
    }
    SideEffect {
        chromeActions.onConfigureAi = { showAiSettings = true }
        chromeActions.onAddTodo = {
            openEditor(TodoEditState(
                groupId = filter.groupId.takeIf { filter.kind == TodoFilterKind.GROUP },
                courseId = filterCourse
            ))
        }
    }
    fun celebrateCompletion() {
        completionPulse++
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (soundEnabled) {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 250)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        if (uris.isNotEmpty()) viewModel.extractFromImages(uris)
    }
    val aiConfig = remember(showAiSettings) { viewModel.readAiConfig() }
    var handledShareRequestId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(entryRequest?.id) {
        val request = entryRequest ?: return@LaunchedEffect
        if (handledShareRequestId == request.id) return@LaunchedEffect
        handledShareRequestId = request.id

        val screenshotPath = request.screenshotPath
        if (!screenshotPath.isNullOrBlank()) {
            val screenshot = runCatching { File(screenshotPath).canonicalFile }.getOrNull()
            val captureDirectory = context.getExternalFilesDir(null)
                ?.let { File(it, "quick_capture").canonicalFile }
            if (
                screenshot != null && captureDirectory != null &&
                screenshot.path.startsWith(captureDirectory.path + File.separator) && screenshot.isFile
            ) {
                viewModel.extractFromImages(listOf(Uri.fromFile(screenshot)), "从当前屏幕提取待办") {
                    screenshot.delete()
                }
            } else {
                Toast.makeText(context, "无法读取当前屏幕截图", Toast.LENGTH_SHORT).show()
            }
        } else if (request.imageUris.isNotEmpty()) {
            viewModel.extractFromImages(request.imageUris, request.sharedText)
        } else if (request.sharedText.isNotBlank()) {
            viewModel.extractFromText(request.sharedText)
        }
        if (request.requestShizukuPermission) onRequestShizukuPermission()
        if (request.todoId == null && request.courseId == null && !request.startWithNewTask) {
            onEntryRequestConsumed(request.id)
        }
    }

    LaunchedEffect(initialCourseId, startWithNewTask) {
        when {
            initialCourseId != null -> {
                val courseId = requireNotNull(initialCourseId)
                onPageChange(TodoPage.Tasks)
                filterCourse = courseId
                if (startWithNewTask) openEditor(TodoEditState(courseId = courseId))
                onInitialCourseConsumed()
            }
            startWithNewTask -> {
                onPageChange(TodoPage.Tasks)
                openEditor(TodoEditState())
                onInitialCourseConsumed()
            }
        }
    }

    LaunchedEffect(initialTodoId, state.items) {
        val todoId = initialTodoId ?: return@LaunchedEffect
        val item = state.items.firstOrNull { it.id == todoId } ?: return@LaunchedEffect
        onPageChange(TodoPage.Tasks)
        filterCourse = item.courseId
        openEditor(item.toEditState())
        onInitialTodoConsumed()
    }

    LaunchedEffect(aiState.result) {
        aiState.result?.let { result ->
            val target = editor ?: TodoEditState()
            openEditor(target.copy(
                title = result.title,
                description = result.description,
                date = result.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }.orEmpty(),
                time = if (result.allDay) "" else result.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty(),
                priority = result.priority,
                repeatRule = result.repeatRule,
                extractedSubtasks = result.subtasks
            ))
            viewModel.clearAiState()
        }
    }
    LaunchedEffect(aiState.error) {
        aiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearAiState() }
    }
    LaunchedEffect(calendarMessage) {
        calendarMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearCalendarMessage() }
    }
    LaunchedEffect(operationError) {
        operationError?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearOperationError() }
    }

    CompositionLocalProvider(LocalDensity provides todoDensity) {
    MaterialTheme(colorScheme = todoColorScheme) {
        CompositionLocalProvider(LocalContentColor provides glassForeground) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .padding(top = detailContentTopPadding())
                .padding(horizontal = 16.dp)
                .padding(bottom = 112.dp)
                .imePadding()
        ) {
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            if (outerTransitionActive) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                // The incoming pane now owns the same staggered springs as Courses.
                                EnterTransition.None.togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 540f)
                                    ) { if (targetState.ordinal > initialState.ordinal) -it else it } + fadeOut(animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f))
                                )
                            }
                        },
                        label = "todo-page-transition"
                    ) { selectedPage ->
                        HomePageEntrance(animateOnMount = pageEntrance.first && !outerTransitionActive, entranceDirection = pageEntrance.second) {
                        when (selectedPage) {
                            TodoPage.Tasks -> if (filter.kind == TodoFilterKind.DELETED) TodoTrashPage(
                                state = state, config = config, backdrop = backdrop,
                                onBack = { filter = TodoFilter() }, onRestore = viewModel::restore,
                                onPurge = viewModel::purge, onClear = viewModel::clearTrash, onRetry = viewModel::retryLoad
                            ) else TodoTasksPage(
                                state = state,
                                schedule = schedule,
                                onRetry = viewModel::retryLoad,
                                config = config,
                                backdrop = backdrop,
                                showCompleted = showCompleted,
                                onShowCompleted = { showCompleted = it },
                                soundEnabled = soundEnabled,
                                itemColor = todoItemColor,
                                onSoundEnabled = { enabled ->
                                    soundEnabled = enabled
                                    context.getSharedPreferences("schedule_plus_todo", android.content.Context.MODE_PRIVATE)
                                        .edit().putBoolean("completion_sound", enabled).apply()
                                },
                                filter = filter,
                                filterCourse = filterCourse,
                                onFilter = { filter = it; filterCourse = null },
                                onFilterCourse = { filterCourse = it },
                                onEdit = { item -> openEditor(item.toEditState()) },
                                onToggle = { item -> viewModel.toggle(item, ::celebrateCompletion) },
                                onPin = viewModel::togglePin,
                                onDelete = viewModel::delete,
                                onAdd = {
                                    openEditor(TodoEditState(
                                        groupId = filter.groupId.takeIf { filter.kind == TodoFilterKind.GROUP },
                                        courseId = filterCourse
                                    ))
                                },
                                onAddSubtask = { item ->
                                    openEditor(TodoEditState(groupId = item.groupId, courseId = item.courseId, parentId = item.id))
                                }
                            )
                            TodoPage.Calendar -> TodoCalendarPage(
                                state = state,
                                schedule = schedule,
                                config = config,
                                backdrop = backdrop,
                                courseColors = courseColors,
                                itemColor = todoItemColor,
                                selectedDate = selectedDate,
                                month = month,
                                autoSync = autoSync,
                                onMonth = {
                                    month = it
                                    selectedDate = todoCalendarDateForMonth(it, selectedDate)
                                },
                                onDate = { selectedDate = it },
                                onAutoSync = { enabled ->
                                    if (enabled) requestAutoCalendarPermission()
                                    else viewModel.setAutoCalendarSync(false)
                                },
                                onSync = requestCalendarPermission,
                                onToggle = { item -> viewModel.toggle(item, ::celebrateCompletion) },
                                onEdit = { item -> openEditor(item.toEditState()) }
                            )
                            TodoPage.Insights -> TodoInsightsPage(
                                state = state,
                                config = config,
                                backdrop = backdrop,
                                groupColors = groupColors,
                                courseColors = courseColors,
                                itemColor = todoItemColor
                            )
    }
    }
    }
    }
        ConfettiOverlay(completionPulse)

            if (editor != null) {
                TodoEditSheet(
                    current = requireNotNull(editor),
                    show = editorVisible,
                    groups = state.groups,
                    courses = schedule.courses,
                    backdrop = backdrop,
                    config = config,
                    aiLoading = aiState.isLoading,
                    saving = saving,
                    onChange = { editor = it },
                    onAiExtract = { text ->
                        if (viewModel.readAiConfig().apiKey.isBlank()) showAiSettings = true
                        else viewModel.extractFromText(text)
                    },
                    onPhotoExtract = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSave = { form ->
                        if (!editorVisible || viewModel.saving.value) return@TodoEditSheet
                        val parsedDue = runCatching { parseDue(form.date, form.time) }
                        if (parsedDue.isFailure) {
                            Toast.makeText(
                                context,
                                parsedDue.exceptionOrNull()?.message ?: "截止日期或时间格式无效",
                                Toast.LENGTH_LONG
                            ).show()
                            return@TodoEditSheet
                        }
                        val due = parsedDue.getOrThrow()
                        val draft = TodoDraft(
                            id = form.sourceId ?: 0,
                            title = form.title,
                            description = form.description,
                            dueAt = due?.first,
                            allDay = due?.second ?: false,
                            priority = form.priority,
                            isPinned = form.pinned,
                            groupId = form.groupId,
                            parentId = form.parentId,
                            courseId = form.courseId,
                            repeatRule = form.repeatRule,
                            endAt = form.endTime.takeIf { it.isNotBlank() }?.let { parseDue(form.date, it)?.first },
                            reminderMode = form.reminderMode,
                            reminderOffsetMinutes = form.reminderOffset,
                            reminderTimeMinutes = runCatching { java.time.LocalTime.parse(form.reminderTime).let { it.hour * 60 + it.minute } }.getOrDefault(480),
                            persistentReminder = form.persistentReminder,
                            strongReminder = form.strongReminder
                        )
                        viewModel.save(draft, form.extractedSubtasks.toList()) {
                            closeEditor()
                        }
                    },
                    onDelete = {
                        editor?.sourceId?.let { id -> state.items.firstOrNull { it.id == id }?.let(viewModel::delete) }
                        closeEditor()
                    },
                    onDismiss = ::closeEditor,
                    onDismissFinished = { if (!editorVisible) editor = null }
                )
            }
            if (showAiSettings) {
                LaunchedEffect(Unit) {
                    context.startActivity(android.content.Intent(context, com.xiaomanjun.sleepdownschedule.SettingsDetailActivity::class.java)
                        .putExtra(com.xiaomanjun.sleepdownschedule.app.ui.SettingsDetailPageExtra,
                            com.xiaomanjun.sleepdownschedule.app.ui.SettingsPage.AiImport.name))
                    showAiSettings = false
                }
            }
            calendarReview.firstOrNull()?.let { issue ->
                CalendarSyncReviewDialog(
                    issue = issue,
                    backdrop = backdrop,
                    config = config,
                    onDismiss = { viewModel.dismissCalendarReview(issue.todoId) },
                    onSkip = { viewModel.resolveCalendarReview(issue.todoId, createNew = false) },
                    onCreateOrRetry = { viewModel.resolveCalendarReview(issue.todoId, createNew = true) },
                    onKeepLocal = { viewModel.resolveCalendarReview(issue.todoId, createNew = true, keepLocalEvent = true) }
                )
            }
    }
        }
    }
}
}

@Composable
private fun TodoTasksPage(
    state: TodoUiState,
    schedule: AppState,
    onRetry: () -> Unit,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    showCompleted: Boolean,
    onShowCompleted: (Boolean) -> Unit,
    soundEnabled: Boolean,
    itemColor: (TodoItemEntity) -> Color,
    onSoundEnabled: (Boolean) -> Unit,
    filter: TodoFilter,
    onFilter: (TodoFilter) -> Unit,
    filterCourse: Long?,
    onFilterCourse: (Long?) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onToggle: (TodoItemEntity) -> Unit,
    onPin: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit,
    onAdd: () -> Unit,
    onAddSubtask: (TodoItemEntity) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val today = LocalDate.now()
    val childrenByParent = remember(state.items) {
        state.items.asSequence().filter { it.parentId != null }.groupBy { it.parentId }
    }
    val tasks = remember(state.items, showCompleted, filter, filterCourse) {
        todoListSelection(state, filter, showCompleted, filterCourse)
    }
    // Selection is only a draft: entering, changing filters or cancelling never deletes data.
    var deletingMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable(stateSaver = Saver<Set<Long>, List<Long>>(
        save = { it.toList() }, restore = { it.toSet() }
    )) { mutableStateOf(emptySet<Long>()) }
    var confirmDeletion by remember { mutableStateOf(false) }
    val selectedTasks = tasks.filter { it.id in selectedIds }
    LaunchedEffect(filter, filterCourse, showCompleted) {
        deletingMode = false; selectedIds = emptySet(); confirmDeletion = false
    }
    fun selectTask(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }
    androidx.activity.compose.BackHandler(deletingMode) {
        deletingMode = false; selectedIds = emptySet(); confirmDeletion = false
    }
    val filterCourseName = filterCourse?.let { id -> schedule.courses.firstOrNull { it.id == id }?.name }
    val todayCourses = remember(schedule.courses, schedule.periods, schedule.config, today) {
        com.xiaomanjun.sleepdownschedule.domain.schedule.coursesForDate(schedule, today)
    }
    val dueToday = remember(state.items, today) {
        state.items.filter {
            it.parentId == null && !it.isCompleted && it.dueAt?.let { ms ->
                Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() == today
            } == true
        }
    }
    val todayCourseColors = todayCourses.associate { course ->
        course.id to courseCardBaseColor(config, course)
    }
    val fallbackAgendaColor = MaterialTheme.colorScheme.primary
    val agendaRows = remember(todayCourses, dueToday, schedule.periods, itemColor, todayCourseColors, fallbackAgendaColor) {
        buildList {
            todayCourses.forEach { course ->
                val start = com.xiaomanjun.sleepdownschedule.domain.schedule.courseStartTime(course, schedule.periods)
                    ?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "课表"
                add(AgendaLine(start, "课程", course.name, todayCourseColors[course.id] ?: fallbackAgendaColor))
            }
            dueToday.forEach { item ->
                val dateTime = item.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                add(AgendaLine(
                    if (item.allDay) "全天" else dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "今天",
                    "待办",
                    item.title,
                    itemColor(item)
                ))
            }
        }.sortedBy { it.sortTime }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().homeSwitchGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    // Keep the three destinations in one row; the public tabs use icons on narrow screens.
                    com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AdaptiveGlassTabs(
                        tabs = listOf(
                            com.xiaomanjun.sleepdownschedule.core.ui.designsystem.GlassTab("ALL", "全部", Icons.Default.TaskAlt),
                            com.xiaomanjun.sleepdownschedule.core.ui.designsystem.GlassTab("UNGROUPED", "未分组", Icons.Default.Folder),
                            com.xiaomanjun.sleepdownschedule.core.ui.designsystem.GlassTab("DELETED", "最近删除", Icons.Default.Restore)
                        ), selectedId = filter.kind.name, backdrop = backdrop, config = config
                    ) { onFilter(TodoFilter(TodoFilterKind.valueOf(it))) }
                }
                TodoGlassIconButton(Icons.Default.DeleteOutline, "删除待办", backdrop, config,
                    destructive = true, onClick = { deletingMode = !deletingMode; selectedIds = emptySet() })
            }
        }
        if (deletingMode) item(key = "delete-selection") {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (tasks.isEmpty()) "当前列表没有可删除的待办" else "请选择要删除的待办 · 已选 ${selectedTasks.size} 项")
                    TodoDeleteActions(config, backdrop,
                        allSelected = selectedTasks.size == tasks.size && tasks.isNotEmpty(),
                        canSelect = tasks.isNotEmpty(), canDelete = selectedTasks.isNotEmpty(),
                        onSelect = {
                            selectedIds = if (selectedTasks.size == tasks.size) emptySet() else tasks.map { it.id }.toSet()
                        }, onCancel = { deletingMode = false; selectedIds = emptySet() },
                        onDelete = { confirmDeletion = true })
                }
            }
        }
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("今天 · ${today.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE))}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${dueToday.size} 项到期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = sleepDownGlassForegroundColor(config).copy(alpha = 0.12f))
                    if (filterCourse != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("课程关联：${filterCourseName ?: "已关联课程"}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            TodoGlassTextButton("清除", config, backdrop) { onFilterCourse(null) }
                        }
                    }
                    HorizontalDivider(color = sleepDownGlassForegroundColor(config).copy(alpha = 0.12f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "显示已完成",
                            modifier = Modifier.weight(1f).combinedClickable(
                                role = Role.Switch,
                                onClick = { onShowCompleted(!showCompleted) },
                                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                        LiquidControlToggle(
                            checked = showCompleted,
                            onCheckedChange = onShowCompleted,
                            backdrop = backdrop,
                            compact = true
                        )
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "完成提示音",
                            modifier = Modifier.weight(1f).combinedClickable(
                                role = Role.Switch,
                                onClick = { onSoundEnabled(!soundEnabled) },
                                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                        LiquidControlToggle(
                            checked = soundEnabled,
                            onCheckedChange = onSoundEnabled,
                            backdrop = backdrop,
                            compact = true
                        )
                    }
                }
            }
        }
        if (agendaRows.isNotEmpty()) item {
            TodoGlassCard(config = config, backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今日时间轴", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    agendaRows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.time, Modifier.width(58.dp), style = MaterialTheme.typography.labelMedium, color = row.color)
                            Box(Modifier.width(2.dp).height(24.dp).background(row.color.copy(alpha = .38f)))
                            Text(row.kind, Modifier.padding(start = 10.dp).width(42.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(row.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (state.loading || state.loadError != null) item { TodoListStatus(state, config, backdrop, onRetry) }
        else if (tasks.isEmpty()) {
            item {
                TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("还没有待办", style = MaterialTheme.typography.titleMedium)
                        Text("添加一项任务，开始安排今天。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TodoGlassTextButton("创建待办", config, backdrop, onClick = onAdd)
                    }
                }
            }
        }
        items(tasks, key = { it.id }) { item ->
            val children = childrenByParent[item.id].orEmpty()
            if (deletingMode) TodoGlassCard(config, backdrop,
                Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { selectTask(item.id) }, cardColor = itemColor(item)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.id in selectedIds, onCheckedChange = { selectTask(item.id) })
                    Text(item.title, Modifier.weight(1f), maxLines = 2)
                    if (children.isNotEmpty()) Text("含 ${children.size} 项子任务", style = MaterialTheme.typography.labelSmall)
                }
            } else TodoItemCard(
                item = item,
                children = children,
                groups = state.groups,
                config = config,
                backdrop = backdrop,
                cardColor = itemColor(item),
                onClick = { onEdit(item) },
                onToggle = { onToggle(item) },
                onToggleChild = onToggle,
                onPin = { onPin(item) },
                onDelete = { onDelete(item) },
                onAddSubtask = { onAddSubtask(item) }
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    SleepDownPickerDialog(show = confirmDeletion, title = "删除所选待办？", backdrop = backdrop, config = config,
        onDismissRequest = { confirmDeletion = false }, bottomActions = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TodoGlassTextButton("取消", config, backdrop) { confirmDeletion = false }
                TodoGlassTextButton("确认删除", config, backdrop, enabled = selectedTasks.isNotEmpty()) {
                    // Use the existing archive/reminder/calendar chain; children follow their parent batch.
                    if (!confirmDeletion) return@TodoGlassTextButton
                    val deletionBatch = selectedTasks.toList()
                    confirmDeletion = false; deletingMode = false; selectedIds = emptySet()
                    deletionBatch.forEach(onDelete)
                }
            }
        }) { Text("${selectedTasks.size} 项待办及其子任务将移到最近删除，30 天内可以恢复。") }
}


private data class AgendaLine(val time: String, val kind: String, val title: String, val color: Color) {
    val sortTime: String get() = if (time == "全天" || time == "课表") "00:00" else time
}

@Composable
private fun TodoItemCard(
    item: TodoItemEntity,
    children: List<TodoItemEntity>,
    groups: List<TodoGroupEntity>,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    cardColor: Color? = null,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onToggleChild: (TodoItemEntity) -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: () -> Unit,
    showSubtaskAction: Boolean = true,
    archived: Boolean = false,
    onRestore: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 640f),
        label = "todo-card-press"
    )
    TodoGlassCard(
        config,
        backdrop,
        Modifier.fillMaxWidth().graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .onGloballyPositioned { cardBounds = it.boundsInWindow() }
            .combinedClickable(
                enabled = !archived,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "编辑待办",
                onLongClickLabel = "打开待办操作菜单",
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuAnchor = cardBounds
                    menuOpen = true
                }
            ),
        cardColor = cardColor
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!archived) TodoCheckmark(checked = item.isCompleted, onClick = onToggle)
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    val due = item.dueAt?.let { instant ->
                        val date = Instant.ofEpochMilli(instant).atZone(ZoneId.systemDefault())
                        if (item.allDay) date.format(DateTimeFormatter.ofPattern("M月d日"))
                        else date.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
                    }
                    Text(
                        listOfNotNull(due, priorityLabel(item.priority), repeatLabel(item.repeatRule), groups.firstOrNull { it.id == item.groupId }?.name).joinToString(" · ").ifBlank { "未设截止日期" },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (item.priority) { 3 -> Color(0xFFE54D4D); 2 -> Color(0xFFE28A2B); else -> MaterialTheme.colorScheme.onSurfaceVariant }
                    )
                }
                if (item.isPinned) Icon(Icons.Default.PushPin, "已置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                // Direct deletion has its own hit target, so it never opens the editor.
                TodoGlassIconButton(Icons.Default.DeleteOutline, if (archived) "永久删除" else "删除待办",
                    backdrop, config, destructive = true, onClick = onDelete)
                if (archived) TodoGlassIconButton(Icons.Default.Restore, "恢复待办", backdrop, config, onClick = onRestore)
                else Box {
                    TodoGlassIconButton(
                        icon = Icons.Default.MoreVert,
                        label = "更多",
                        backdrop = backdrop,
                        config = config,
                        modifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInWindow() },
                        onClick = { menuOpen = true }
                    )
                }
            }
            if (item.description.isNotBlank()) Text(item.description, Modifier.padding(start = 54.dp, bottom = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            if (children.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(start = 50.dp, top = 4.dp, bottom = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                children.forEach { child ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!archived) TodoCheckmark(checked = child.isCompleted, onClick = { onToggleChild(child) })
                        Text(child.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = if (child.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
    SleepDownLiquidCascadingPopup(
        show = menuOpen,
        anchorBounds = menuAnchor,
        items = buildList {
            add(
            SleepDownLiquidMenuItem(
                key = "todo-pin-${item.id}",
                text = if (item.isPinned) "取消置顶" else "置顶",
                onClick = { onPin(); menuOpen = false }
            )
            )
            if (showSubtaskAction) add(SleepDownLiquidMenuItem(
                key = "todo-subtask-${item.id}",
                text = "添加子任务",
                onClick = { onAddSubtask(); menuOpen = false }
            ))
            add(SleepDownLiquidMenuItem(
                key = "todo-edit-${item.id}",
                text = "编辑",
                onClick = { onClick(); menuOpen = false }
            ))
            add(SleepDownLiquidMenuItem(
                key = "todo-delete-${item.id}",
                text = "删除",
                onClick = { onDelete(); menuOpen = false }
            ))
        },
        onDismissRequest = { menuOpen = false },
        backdrop = backdrop,
        config = config,
        panelMinWidth = 180.dp
    )
}

@Composable
private fun TodoCheckmark(checked: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val fill by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) primary else Color.Transparent,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.58f, stiffness = 560f),
        label = "todo-check-fill"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0.94f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 700f),
        label = "todo-check-scale"
    )
    Box(
        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                role = Role.Checkbox
                stateDescription = if (checked) "已完成" else "未完成"
            }
            .combinedClickable(
                role = Role.Checkbox,
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(26.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, primary.copy(alpha = if (checked) 0.95f else 0.72f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

internal fun buildTodoGroupColorAssignments(
    groups: List<TodoGroupEntity>,
    palette: List<Long>
): Map<Long, Long> {
    val colors = palette.ifEmpty { DefaultCourseCardPalette }
    return groups.associate { group ->
        val stableIndex = if (group.id <= 0L) 0L else group.id - 1L
        group.id to colors[Math.floorMod(stableIndex, colors.size.toLong()).toInt()]
    }
}

internal sealed interface TodoCalendarAgendaEntry {
    val sortMinute: Int
    val tieBreak: Int

    data class CourseEntry(
        val course: CourseEntity,
        val start: java.time.LocalTime?,
        val end: java.time.LocalTime?
    ) : TodoCalendarAgendaEntry {
        override val sortMinute: Int get() = start?.let { it.hour * 60 + it.minute } ?: 0
        override val tieBreak: Int = 0
    }

    data class TaskEntry(val item: TodoItemEntity) : TodoCalendarAgendaEntry {
        private val due = item.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
        override val sortMinute: Int get() = if (item.allDay) -1 else due?.let { it.hour * 60 + it.minute } ?: 0
        override val tieBreak: Int = 1
    }
}

internal fun buildTodoCalendarAgenda(
    schedule: AppState,
    items: List<TodoItemEntity>,
    date: LocalDate,
    today: LocalDate = LocalDate.now()
): List<TodoCalendarAgendaEntry> = buildList {
    com.xiaomanjun.sleepdownschedule.domain.schedule.coursesForDate(schedule, date, today).forEach { course ->
        add(
            TodoCalendarAgendaEntry.CourseEntry(
                course = course,
                start = com.xiaomanjun.sleepdownschedule.domain.schedule.courseStartTime(course, schedule.periods),
                end = com.xiaomanjun.sleepdownschedule.domain.schedule.courseEndTime(course, schedule.periods)
            )
        )
    }
    items.forEach { item ->
        val dueAt = item.dueAt ?: return@forEach
        if (Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate() == date) {
            add(TodoCalendarAgendaEntry.TaskEntry(item))
        }
    }
}.sortedWith(compareBy<TodoCalendarAgendaEntry> { it.sortMinute }.thenBy { it.tieBreak })

internal fun shouldShowTodoCalendarAgendaAfterDateTap(
    isCurrentlyOpen: Boolean,
    openDate: LocalDate,
    tappedDate: LocalDate
): Boolean = !(isCurrentlyOpen && openDate == tappedDate)

internal fun todoCalendarDateAtPosition(
    dateCellBounds: Map<LocalDate, Rect>,
    positionInRoot: Offset
): LocalDate? = dateCellBounds.entries.firstOrNull { (_, bounds) ->
    bounds.contains(positionInRoot)
}?.key

internal data class TodoCalendarPopupPlacement(
    val left: Int,
    val top: Int,
    val width: Int,
    val maxHeight: Int,
    val bottomSheet: Boolean
)

internal fun todoCalendarPopupPlacement(
    anchor: Rect,
    rootSize: IntSize,
    desiredWidth: Int,
    preferredHeight: Int,
    margin: Int,
    minimumPopupHeight: Int
): TodoCalendarPopupPlacement {
    val safeWidth = (rootSize.width - margin * 2).coerceAtLeast(1)
    val width = desiredWidth.coerceAtMost(safeWidth)
    val availableBottom = (rootSize.height - anchor.bottom.roundToInt() - margin).coerceAtLeast(0)
    val availableTop = (anchor.top.roundToInt() - margin).coerceAtLeast(0)
    val bestSpace = maxOf(availableBottom, availableTop)
    if (rootSize.height <= margin * 2 || bestSpace < minimumPopupHeight) {
        val height = preferredHeight.coerceAtMost((rootSize.height - margin * 2).coerceAtLeast(1))
        val panelWidth = minOf(safeWidth, (desiredWidth * 1.5f).toInt())
        return TodoCalendarPopupPlacement(
            left = ((rootSize.width - panelWidth) / 2).coerceAtLeast(0),
            top = (rootSize.height - margin - height).coerceAtLeast(margin),
            width = panelWidth,
            maxHeight = height,
            bottomSheet = true
        )
    }
    val useBelow = availableBottom >= minOf(preferredHeight, minimumPopupHeight) || availableBottom >= availableTop
    val available = if (useBelow) availableBottom else availableTop
    val height = preferredHeight.coerceAtMost(available.coerceAtLeast(1))
    val desiredLeft = (anchor.center.x.roundToInt() - width / 2).coerceIn(
        margin.coerceAtMost((rootSize.width - width).coerceAtLeast(0)),
        (rootSize.width - width - margin).coerceAtLeast(0)
    )
    val desiredTop = if (useBelow) anchor.bottom.roundToInt() + margin else anchor.top.roundToInt() - margin - height
    val safeTop = desiredTop.coerceIn(margin.coerceAtMost((rootSize.height - height).coerceAtLeast(0)),
        (rootSize.height - height - margin).coerceAtLeast(0))
    return TodoCalendarPopupPlacement(desiredLeft, safeTop, width, height, bottomSheet = false)
}

internal fun todoCalendarCourseStatus(
    schedule: AppState,
    date: LocalDate,
    today: LocalDate = LocalDate.now()
): String? {
    if (!schedule.loaded) return "课表加载中"
    val config = schedule.config
    if (config.autoCurrentWeek) {
        com.xiaomanjun.sleepdownschedule.domain.schedule.scheduleTermStatusLabel(config, date)
            ?.let { return it }
    }
    if (com.xiaomanjun.sleepdownschedule.domain.schedule.adjustedTeachingWeekForDate(config, date, today) == null) {
        return "所选日期不在当前课表的教学周内"
    }
    if (schedule.courses.isEmpty()) return "当前课表还没有课程"
    if (com.xiaomanjun.sleepdownschedule.domain.schedule.teachingDateForSchedule(config, date) == null) {
        return "当天已安排停课或休息"
    }
    return null
}

internal fun todoCalendarGridDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val visibleDays = first.dayOfWeek.value - 1 + month.lengthOfMonth()
    val cellCount = ((visibleDays + 6) / 7) * 7
    return List(cellCount) { index -> start.plusDays(index.toLong()) }
}

internal fun todoCalendarMonthSwipeTarget(
    month: YearMonth,
    horizontalDragPx: Float,
    thresholdPx: Float
): YearMonth? = when {
    kotlin.math.abs(horizontalDragPx) < thresholdPx -> null
    horizontalDragPx < 0f -> month.plusMonths(1)
    else -> month.minusMonths(1)
}

internal fun todoCalendarDateForMonth(month: YearMonth, selectedDate: LocalDate): LocalDate =
    month.atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))

/** Use the existing cached, tinted wallpaper sampler; selection keeps its blue/white contrast. */
@Composable
private fun CalendarText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    adaptive: Boolean = true
) {
    val background = com.xiaomanjun.sleepdownschedule.core.ui.text.LocalCourseTextBackground.current
        ?: com.xiaomanjun.sleepdownschedule.glass.ui.rememberCourseTextBackground(Color.Transparent, 0f, false, 0f, false, false, true) { null }
    CompositionLocalProvider(com.xiaomanjun.sleepdownschedule.core.ui.text.LocalCourseTextBackground provides background) {
        com.xiaomanjun.sleepdownschedule.core.ui.text.CourseCardText(
            text, modifier, color = color, themeColor = null, style = style,
            fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow,
            adaptiveContrast = adaptive, localMonochrome = adaptive
        )
    }
}

@Composable
private fun TodoCalendarPage(
    state: TodoUiState,
    schedule: AppState,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    courseColors: Map<Long, Color>,
    itemColor: (TodoItemEntity) -> Color,
    selectedDate: LocalDate,
    month: YearMonth,
    autoSync: Boolean,
    onMonth: (YearMonth) -> Unit,
    onDate: (LocalDate) -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onSync: () -> Unit,
    onToggle: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var agendaAnchor by remember { mutableStateOf(Rect.Zero) }
    var agendaPopupBounds by remember { mutableStateOf(Rect.Zero) }
    var showAgenda by remember { mutableStateOf(false) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    val dateCellBounds = remember { mutableMapOf<LocalDate, Rect>() }
    var agendaScrimCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val today = LocalDate.now()
    val courseStatus = todoCalendarCourseStatus(schedule, selectedDate, today)
    val agendaEntries = remember(schedule, state.items, selectedDate, today) {
        buildTodoCalendarAgenda(schedule, state.items, selectedDate, today)
    }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 64.dp.toPx() }
    fun onCalendarDateTapped(date: LocalDate, anchor: Rect) {
        val shouldOpen = shouldShowTodoCalendarAgendaAfterDateTap(
            isCurrentlyOpen = showAgenda,
            openDate = selectedDate,
            tappedDate = date
        )
        // Adjacent-month cells may open their agenda, but tapping them must never navigate.
        onDate(date)
        if (shouldOpen) {
            agendaAnchor = anchor
            showAgenda = true
        } else {
            showAgenda = false
        }
    }
    fun changeCalendarMonth(target: YearMonth) {
        // A month jump invalidates cell anchors and clamps the chosen day (e.g. Jan 31).
        showAgenda = false
        agendaAnchor = Rect.Zero
        dateCellBounds.clear()
        onMonth(target)
        onDate(todoCalendarDateForMonth(target, selectedDate))
    }
    Box(Modifier.fillMaxSize().onSizeChanged { rootSize = it }) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
    ) {
        item(key = "todo-calendar-grid") {
            // Move the calendar shell first, then its real rows; avoid applying the card lag twice.
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth(), switchGroup = false) {
                Column(
                    Modifier.padding(14.dp)
                        .pointerInput(month, swipeThresholdPx) {
                            var dragDistance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragDistance = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    dragDistance += dragAmount
                                    change.consume()
                                },
                                onDragEnd = {
                                    todoCalendarMonthSwipeTarget(month, dragDistance, swipeThresholdPx)
                                        ?.let(::changeCalendarMonth)
                                },
                                onDragCancel = { dragDistance = 0f }
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.homeSwitchGroup(0f), verticalAlignment = Alignment.CenterVertically) {
                        com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AppDirectionButton(
                            direction = -1,
                            label = "上个月",
                            backdrop = backdrop,
                            config = config,
                            onClick = { changeCalendarMonth(month.minusMonths(1)) }
                        )
                        CalendarText(
                            text = month.format(DateTimeFormatter.ofPattern("yyyy年 M月")),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable {
                                showAgenda = false; showMonthPicker = true
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AppDirectionButton(
                            direction = 1,
                            label = "下个月",
                            backdrop = backdrop,
                            config = config,
                            onClick = { changeCalendarMonth(month.plusMonths(1)) }
                        )
                    }
                    Row(Modifier.fillMaxWidth().homeSwitchGroup(0.1f)) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                            CalendarText(
                                label,
                                modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = {
                            // A single bounded spring owns each incoming/outgoing grid; rapid
                            // reversals retarget AnimatedContent rather than queueing animations.
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(spring(dampingRatio = .78f, stiffness = 360f)) { direction * it } +
                                fadeIn(spring(dampingRatio = .9f, stiffness = 520f))) togetherWith
                                (slideOutHorizontally(spring(dampingRatio = .86f, stiffness = 440f)) { -direction * it } +
                                    fadeOut(spring(dampingRatio = .9f, stiffness = 520f)))
                        },
                        label = "todo-calendar-month-swipe"
                    ) { displayedMonth ->
                        val dates = remember(displayedMonth) { todoCalendarGridDates(displayedMonth) }
                        val coursesByDate = remember(schedule, dates, today) {
                            dates.associateWith { date ->
                                com.xiaomanjun.sleepdownschedule.domain.schedule.coursesForDate(schedule, date, today)
                            }
                        }
                        val tasksByDate = remember(state.items, dates) {
                            state.items.mapNotNull { item ->
                                item.dueAt?.let { dueAt ->
                                    Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate() to item
                                }
                            }.groupBy({ it.first }, { it.second })
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        dates.chunked(7).forEachIndexed { weekIndex, weekDates ->
                            Row(Modifier.fillMaxWidth().homeSwitchGroup((weekIndex + 1) / 6f)) {
                            weekDates.forEach { date ->
                                val dateCourses = coursesByDate[date].orEmpty()
                                val dateTasks = tasksByDate[date].orEmpty()
                                val isSelected = date == selectedDate
                                val selectedProgress by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
                                    label = "todo-calendar-date-selection"
                                )
                                val dateTextColor = lerp(
                                    if (YearMonth.from(date) == displayedMonth) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f),
                                    MaterialTheme.colorScheme.onPrimary,
                                    selectedProgress
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                        .heightIn(min = 56.dp)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = date.format(
                                                DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)
                                            )
                                            selected = isSelected
                                            stateDescription = "${dateCourses.size} 门课程，${dateTasks.size} 项待办"
                                        }
                                        .onGloballyPositioned {
                                            // Outgoing grids remain drawn during the spring but cannot own hit targets.
                                            if (displayedMonth == month) dateCellBounds[date] = it.boundsInRoot()
                                        }
                                        .combinedClickable(
                                            role = Role.Button,
                                            onClick = {
                                                onCalendarDateTapped(date, dateCellBounds[date] ?: Rect.Zero)
                                            },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(vertical = 5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .graphicsLayer {
                                                    val scale = 0.88f + selectedProgress * 0.12f
                                                    scaleX = scale
                                                    scaleY = scale
                                                }
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = selectedProgress))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = selectedProgress * 0.42f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CalendarText(
                                                date.dayOfMonth.toString(),
                                                color = dateTextColor,
                                                adaptive = selectedProgress < .01f,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                        Row(
                                            Modifier.padding(top = 2.dp).height(5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            val markers = buildList {
                                                dateCourses.forEach { course -> courseColors[course.id]?.let(::add) }
                                                dateTasks.forEach { item -> add(itemColor(item)) }
                                            }.distinctBy(Color::toArgb).take(4)
                                            markers.forEach { color ->
                                                Box(Modifier.size(4.dp).clip(CircleShape).background(color))
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        }
                        }
                    }
                }
            }
        }
        item(key = "todo-calendar-selected-$selectedDate") {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CalendarText(selectedDate.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    CalendarText("${agendaEntries.count { it is TodoCalendarAgendaEntry.TaskEntry }} 项待办 · ${agendaEntries.count { it is TodoCalendarAgendaEntry.CourseEntry }} 节课程", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    courseStatus?.let { CalendarText(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                CalendarText(
                    "自动同步",
                    modifier = Modifier.padding(end = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                LiquidControlToggle(
                    checked = autoSync,
                    onCheckedChange = onAutoSync,
                    backdrop = backdrop,
                    compact = true
                )
                TodoGlassIconButton(
                    icon = Icons.Default.CalendarMonth,
                    label = "同步到系统日历",
                    backdrop = backdrop,
                    config = config,
                    onClick = onSync
                )
            }
        }
    }
    TodoMonthPicker(showMonthPicker, month, backdrop, config,
        onDismiss = { showMonthPicker = false }, onSelect = { changeCalendarMonth(it); showMonthPicker = false })
    if (showAgenda && rootSize.width > 0 && rootSize.height > 0) {
        val marginPx = with(density) { 12.dp.roundToPx() }
        val placement = todoCalendarPopupPlacement(
            anchor = agendaAnchor,
            rootSize = rootSize,
            desiredWidth = with(density) { 360.dp.roundToPx() },
            preferredHeight = with(density) { 430.dp.roundToPx() },
            margin = marginPx,
            minimumPopupHeight = with(density) { 250.dp.roundToPx() }
        )
        val popupMaxHeight = with(density) { placement.maxHeight.toDp() }
        Box(Modifier.fillMaxSize().zIndex(10f)) {
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { agendaScrimCoordinates = it }
                    .pointerInput(showAgenda, selectedDate, month, placement) {
                        detectTapGestures { tapPosition ->
                            val rootPosition = agendaScrimCoordinates?.localToRoot(tapPosition)
                            if (rootPosition != null && agendaPopupBounds.contains(rootPosition)) {
                                return@detectTapGestures
                            }
                            val tappedDate = rootPosition?.let {
                                todoCalendarDateAtPosition(dateCellBounds, it)
                            }
                            if (tappedDate != null) {
                                onCalendarDateTapped(tappedDate, dateCellBounds[tappedDate] ?: Rect.Zero)
                            } else {
                                showAgenda = false
                            }
                        }
                    }
            )
            TodoCalendarAgendaPopup(
                date = selectedDate,
                entries = agendaEntries,
                config = config,
                backdrop = backdrop,
                courseColors = courseColors,
                itemColor = itemColor,
                modifier = Modifier.offset { IntOffset(placement.left, placement.top) }
                    .width(with(density) { placement.width.toDp() })
                    .heightIn(max = popupMaxHeight)
                    .onGloballyPositioned { agendaPopupBounds = it.boundsInRoot() },
                listMaxHeight = (popupMaxHeight - 100.dp).coerceAtLeast(64.dp),
                courseStatus = courseStatus,
                onDismiss = { showAgenda = false },
                onToggle = onToggle,
                onEdit = { item -> showAgenda = false; onEdit(item) }
            )
        }
    }
    }
}

@Composable
private fun TodoCalendarAgendaPopup(
    date: LocalDate,
    entries: List<TodoCalendarAgendaEntry>,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    courseColors: Map<Long, Color>,
    itemColor: (TodoItemEntity) -> Color,
    modifier: Modifier,
    listMaxHeight: androidx.compose.ui.unit.Dp,
    courseStatus: String?,
    onDismiss: () -> Unit,
    onToggle: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit
) {
    TodoGlassCard(config, backdrop, modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CalendarText(date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    CalendarText(courseStatus ?: "当天安排", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TodoGlassIconButton(Icons.Default.Close, "关闭当天安排", backdrop, config, onClick = onDismiss)
            }
            if (entries.isEmpty()) {
                CalendarText(courseStatus ?: "这一天没有课程或待办", Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = listMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries, key = { entry ->
                        when (entry) {
                            is TodoCalendarAgendaEntry.CourseEntry -> "course-${entry.course.id}"
                            is TodoCalendarAgendaEntry.TaskEntry -> "todo-${entry.item.id}"
                        }
                    }) { entry ->
                        when (entry) {
                            is TodoCalendarAgendaEntry.CourseEntry -> {
                                val course = entry.course
                                TodoGlassCard(
                                    config = config,
                                    backdrop = backdrop,
                                    modifier = Modifier.fillMaxWidth(),
                                    cardColor = courseColors[course.id]
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.width(3.dp).height(40.dp).clip(CircleShape)
                                            .background(courseColors[course.id] ?: MaterialTheme.colorScheme.primary))
                                        CalendarText(
                                            entry.start?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "课表",
                                            Modifier.padding(horizontal = 8.dp).widthIn(min = 44.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Column(Modifier.weight(1f)) {
                                            CalendarText(course.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            val detail = listOfNotNull(
                                                entry.end?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                                    ?.let { "至 $it" },
                                                course.location?.takeIf(String::isNotBlank)?.let { "地点：$it" },
                                                course.teacher?.takeIf(String::isNotBlank)?.let { "教师：$it" },
                                                course.note?.takeIf(String::isNotBlank)?.let { "备注：$it" }
                                            ).joinToString(" · ")
                                            if (detail.isNotBlank()) CalendarText(
                                                detail,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 3,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            is TodoCalendarAgendaEntry.TaskEntry -> {
                                val item = entry.item
                                TodoGlassCard(
                                    config = config,
                                    backdrop = backdrop,
                                    modifier = Modifier.fillMaxWidth().clickable { onEdit(item) },
                                    cardColor = itemColor(item)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TodoCheckmark(checked = item.isCompleted) { onToggle(item) }
                                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                            CalendarText(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                            val due = item.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                                            CalendarText(
                                                if (item.allDay) "全天" else due?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (item.groupId != null || item.courseId != null) {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(itemColor(item)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoInsightsPage(
    state: TodoUiState,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    groupColors: Map<Long, Color>,
    courseColors: Map<Long, Color>,
    itemColor: (TodoItemEntity) -> Color
) {
    val items = state.items
    val rootItems = remember(items) { items.filter { it.parentId == null } }
    val activeItems = remember(rootItems) { rootItems.filterNot { it.isCompleted } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            delay((60_000L - current % 60_000L + 100L).coerceAtLeast(1_000L))
            now = System.currentTimeMillis()
        }
    }
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val metrics = remember(rootItems, now, zone, today) {
        val nextWeek = today.plusDays(7)
        val weekAhead = now + 7 * 86_400_000L
        var highPriority = 0
        var overdue = 0
        var upcoming = 0
        var unscheduled = 0
        var completed = 0
        rootItems.forEach { item ->
            if (item.isCompleted) completed++
            else {
                if (item.priority >= 2) highPriority++
                val dueAt = item.dueAt
                if (dueAt == null) unscheduled++
                else {
                    val dueDate = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
                    if (if (item.allDay) dueDate.isBefore(today) else dueAt < now) overdue++
                    if (if (item.allDay) !dueDate.isBefore(today) && dueDate.isBefore(nextWeek)
                        else dueAt >= now && dueAt < weekAhead
                    ) upcoming++
                }
            }
        }
        val total = rootItems.size
        TodoInsightMetrics(
            total = total,
            completed = completed,
            completion = if (total == 0) 0f else completed.toFloat() / total,
            highPriority = highPriority,
            overdue = overdue,
            upcoming = upcoming,
            unscheduled = unscheduled
        )
    }
    val trendCounts = remember(rootItems, today, zone, groupColors, courseColors, itemColor) {
        val trendDates = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
        val dateSet = trendDates.toSet()
        val counts = linkedMapOf<LocalDate, MutableMap<Int, Int>>()
        rootItems.forEach { item ->
            val date = item.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                ?.takeIf(dateSet::contains) ?: return@forEach
            val color = itemColor(item).toArgb()
            val byColor = counts.getOrPut(date) { linkedMapOf() }
            byColor[color] = (byColor[color] ?: 0) + 1
        }
        trendDates.map { date -> date to counts[date].orEmpty().toSortedMap() }
    }
    val palette = LocalCourseCardPalette.current.ifEmpty { DefaultCourseCardPalette }.map { Color(it.toInt()) }
    val trendMax = (trendCounts.maxOfOrNull { (_, colors) -> colors.values.sum() } ?: 0).coerceAtLeast(1)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("完成率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${(metrics.completion * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    InsightBar("完成", metrics.completed, metrics.total, palette[0])
                    Text("已完成 ${metrics.completed} / ${metrics.total} 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("任务压力分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("仅统计未完成的主任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    InsightBar("高优先级", metrics.highPriority, activeItems.size, palette.getOrElse(1) { palette[0] })
                    InsightBar("已逾期", metrics.overdue, activeItems.size, palette.getOrElse(2) { palette[0] })
                    InsightBar("未来 7 天到期", metrics.upcoming, activeItems.size, palette.getOrElse(3) { palette[0] })
                    InsightBar("未安排日期", metrics.unscheduled, activeItems.size, palette.getOrElse(4) { palette[0] })
                }
            }
        }
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("近 7 天完成趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().height(110.dp).padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        trendCounts.forEach { (date, colorCounts) ->
                            val count = colorCounts.values.sum()
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                                val barHeight = if (count == 0) 3.dp else (14 + 62f * count / trendMax).dp
                                Box(
                                    Modifier.padding(vertical = 4.dp).width(22.dp).height(barHeight)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    if (count == 0) {
                                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f)))
                                    } else {
                                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                                            colorCounts.forEach { (argb, segmentCount) ->
                                                Box(
                                                    Modifier.fillMaxWidth().weight(segmentCount.toFloat())
                                                        .background(Color(argb))
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TodoInsightMetrics(
    val total: Int,
    val completed: Int,
    val completion: Float,
    val highPriority: Int,
    val overdue: Int,
    val upcoming: Int,
    val unscheduled: Int
)

@Composable
private fun InsightBar(label: String, value: Int, total: Int, color: Color) {
    val fraction = if (total == 0) 0f else (value.toFloat() / total).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row { Text(label, Modifier.weight(1f)); Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun todoCardShape(config: ScheduleConfigEntity): androidx.compose.ui.graphics.Shape {
    val window = currentWindowSizeDp()
    val cornerProgress = LocalPersonalizationPreview.current?.weekCardCornerProgress
        ?: config.weekCardCornerProgress
    val radius = adaptiveWeekCardCornerRadius(
        cardWidth = (window.width - 32.dp).coerceAtLeast(1.dp),
        cardHeight = 132.dp,
        windowWidth = window.width,
        windowHeight = window.height,
        progress = cornerProgress
    )
    return remember(radius) { RoundedRectangle(radius) }
}

@Composable
private fun TodoGlassCard(
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    cardColor: Color? = null,
    shape: Shape = todoCardShape(config),
    switchGroup: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    CourseGlassCard(
        backdrop = backdrop,
        config = config,
        // Cards are the real visual groups: text and glass rebound together on every root/tab switch.
        modifier = if (switchGroup) modifier.homeSwitchGroup() else modifier,
        shape = shape,
        baseColorOverride = cardColor,
        neutralContainer = cardColor == null
    ) {
        Column(content = content)
    }
}

@Composable
private fun TodoSelectionPill(
    label: String,
    isSelected: Boolean,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val selectionProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "todo-filter-selection"
    )
    val labelColor = lerp(
        sleepDownGlassForegroundColor(config),
        Color.White,
        selectionProgress
    )
    GlassPill(
        backdrop = backdrop,
        config = config,
        modifier = Modifier
            .widthIn(min = 64.dp)
            .height(48.dp)
            .semantics {
                role = Role.Button
                selected = isSelected
                stateDescription = if (isSelected) "已选中" else "未选中"
            },
        selected = isSelected,
        selectedSurfaceColorOverride = Color(0xFF008BFF).copy(alpha = 0.88f),
        selectionProgress = selectionProgress,
        onClick = onClick,
        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    ) {
        Box(
            Modifier.widthIn(min = 64.dp).height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodoGlassIconButton(
    icon: ImageVector,
    label: String,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    GlassPill(
        backdrop = backdrop,
        config = config,
        modifier = modifier.size(48.dp).semantics { contentDescription = label },
        onClick = onClick,
        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (destructive) Color(0xFFE54D4D) else sleepDownGlassForegroundColor(config)
            )
        }
    }
}

@Composable
private fun TodoGlassActionPill(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    GlassPill(
        backdrop = backdrop,
        config = config,
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.52f }
            .semantics {
                label?.let { contentDescription = it }
                if (!enabled) disabled()
            },
        onClick = if (enabled) onClick else null,
        onLongClick = if (enabled) {
            { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp).heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun TodoDeleteActions(
    config: ScheduleConfigEntity, backdrop: Backdrop?, allSelected: Boolean,
    canSelect: Boolean, canDelete: Boolean, onSelect: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit
) {
    val labels = listOf(if (allSelected) "取消全选" else "全选", "取消", "删除所选")
    val icons = listOf(Icons.Default.TaskAlt, Icons.Default.Close, Icons.Default.DeleteOutline)
    val enabled = listOf(canSelect, true, canDelete)
    val actions = listOf(onSelect, onCancel, onDelete)
    val style = MaterialTheme.typography.labelLarge
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Share the page's polarity; the assistant samples a different wallpaper region.
    val light = com.xiaomanjun.sleepdownschedule.glass.ui.glassUsesLightStyle(config)
    val pageForeground = sleepDownGlassForegroundColor(config)
    val tokens = com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens.Button
    val material = GlassTokens.pill().copy(
        blur = 4.dp, lensHeight = 12.dp, lensAmount = 24.dp,
        surfaceAlpha = if (light) .52f else .56f,
        highlightAlpha = .24f, shadowAlpha = .12f, innerShadowAlpha = .10f
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = (maxWidth - 16.dp) / 3
        val iconOnly = labels.any { with(density) {
            measurer.measure(androidx.compose.ui.text.AnnotatedString(it), style).size.width.toDp() + 24.dp > cell
        } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                // Translucent adaptive tint reveals the lens; keep disabled text and outlines readable.
                val destructive = index == 2 && enabled[index]
                val foreground = if (destructive) Color(0xFFE54D4D)
                    else pageForeground.copy(alpha = if (enabled[index]) 1f else .80f)
                Box(Modifier.weight(1f)) {
                    androidx.compose.material3.TooltipBox(
                        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(label) } },
                        state = androidx.compose.material3.rememberTooltipState()
                    ) {
                        GlassSurface(backdrop, config,
                            modifier = Modifier.fillMaxWidth().height(tokens.ActionHeight)
                                .border(tokens.BorderWidth, foreground.copy(alpha = .28f), Capsule())
                                .semantics {
                                    contentDescription = label; role = Role.Button
                                    if (!enabled[index]) disabled()
                                }, shape = Capsule(), tokens = material,
                            restingDecorations = true,
                            onClick = if (enabled[index]) actions[index] else null
                        ) {
                            Box(Modifier.fillMaxSize().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                if (iconOnly) Icon(icons[index], null, Modifier.size(tokens.IconSize), tint = foreground)
                                else Text(label, color = foreground, style = style, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoGlassTextButton(
    label: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TodoGlassActionPill(
        backdrop = backdrop,
        config = config,
        modifier = modifier,
        enabled = enabled,
        label = label,
        onClick = onClick
    ) {
        Text(
            text = label,
            color = sleepDownGlassForegroundColor(config),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodoEditorChoicePill(
    label: String,
    isSelected: Boolean,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TodoEditorWhiteButton(
        label = label,
        config = config,
        backdrop = backdrop,
        modifier = modifier,
        selected = isSelected,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = TodoEditorAccent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodoEditorActionPill(
    label: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    primary: Boolean = false,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    TodoEditorWhiteButton(
        label = label,
        config = config,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        primary = primary,
        onClick = onClick
    ) {
        if (content != null) content() else Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TodoEditorAccent,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodoEditorIconButton(
    icon: ImageVector,
    label: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    destructive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TodoEditorWhiteButton(
        label = label,
        config = config,
        backdrop = backdrop,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (destructive) Color(0xFFE54D4D) else TodoEditorAccent
        )
    }
}

private val TodoEditorAccent = Color(0xFF0A84FF)
private val TodoEditorWhiteButtonTokens = GlassTokens.pill().copy(
    blur = 12.dp,
    lensHeight = 4.dp,
    lensAmount = 6.dp,
    surfaceAlpha = 0.90f
)

@Composable
private fun TodoEditorWhiteButton(
    label: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    primary: Boolean = false,
    shape: Shape = Capsule(),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val selectionProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "todo-editor-white-button-selection"
    )
    GlassSurface(
        backdrop = backdrop,
        config = config,
        modifier = modifier.heightIn(min = 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.52f }
            .border(1.25.dp, TodoEditorAccent.copy(alpha = selectionProgress), shape)
            .semantics {
                contentDescription = label
                role = Role.Button
                this.selected = selected
                stateDescription = if (selected) "已选中" else "未选中"
                if (!enabled) disabled()
            },
        shape = shape,
        tokens = TodoEditorWhiteButtonTokens,
        baseSurfaceColorOverride = if (primary) TodoEditorAccent else Color.White,
        selected = false,
        selectionProgress = 0f,
        onClick = if (enabled) onClick else null,
        onLongClick = if (enabled) {
            { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        } else null
    ) {
        CompositionLocalProvider(LocalContentColor provides TodoEditorAccent) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun TodoEditorSelectorRow(
    field: String,
    value: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    selected: Boolean = false,
    buttonModifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val foreground = sleepDownGlassForegroundColor(config)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(field, style = MaterialTheme.typography.bodyMedium, color = foreground)
                TodoEditorActionPill(
                    label = value,
                    config = config,
                    backdrop = backdrop,
                    modifier = Modifier.widthIn(min = 112.dp, max = 180.dp).then(buttonModifier),
                    selected = selected,
                    onClick = onClick
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(field, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = foreground)
                TodoEditorActionPill(
                    label = value,
                    config = config,
                    backdrop = backdrop,
                    modifier = Modifier.widthIn(min = 112.dp, max = 180.dp).then(buttonModifier),
                    selected = selected,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun TodoEditorDateTimeField(
    label: String,
    value: String,
    placeholder: String,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = sleepDownGlassForegroundColor(config).copy(alpha = 0.78f))
        DialogCapsuleField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            config = config,
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Ascii
        )
    }
}

@Composable
private fun TodoEditorSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun TodoEditSheet(
    current: TodoEditState,
    show: Boolean,
    groups: List<TodoGroupEntity>,
    courses: List<CourseEntity>,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    aiLoading: Boolean,
    saving: Boolean,
    onChange: (TodoEditState) -> Unit,
    onAiExtract: (String) -> Unit,
    onPhotoExtract: () -> Unit,
    onSave: (TodoEditState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit
) {
    val foreground = sleepDownGlassForegroundColor(config)
    var edit by rememberSaveable(current.sourceId, current.parentId, stateSaver = TodoEditStateSaver) { mutableStateOf(current) }
    LaunchedEffect(current) { edit = current }
    var advanced by rememberSaveable(current.sourceId) { mutableStateOf(false) }
    var priorities by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    var pickingReminderDate by remember { mutableStateOf(false) }
    val dueDate = remember(edit.date) { runCatching { LocalDate.parse(edit.date) }.getOrNull() }
    var aiText by rememberSaveable { mutableStateOf("") }
    var activeMenu by remember { mutableStateOf<TodoEditorMenu?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var validationError by remember { mutableStateOf<String?>(null) }
    fun updateEdit(value: TodoEditState) { edit = value; onChange(value); validationError = null }
    fun saveEdit() {
        if (!show || saving) return
        val result = runCatching {
            val due = parseDue(edit.date, edit.time)
            if (edit.endTime.isNotBlank()) {
                val end = parseDue(edit.date, edit.endTime)?.first
                require(due != null && !due.second && end != null && end > due.first) { "结束时间必须晚于开始时间" }
            }
            if (edit.reminderMode == "ALL_DAY") java.time.LocalTime.parse(edit.reminderTime)
            require(edit.reminderMode in setOf("NONE", "LEGACY") || due != null) { "请先选择截止日期" }
        }
        result.onSuccess { onChange(edit); onSave(edit) }.onFailure { validationError = it.message }
    }
    val picker = rememberDatePickerState(initialSelectedDateMillis = todoDatePickerMillis(dueDate))
    LaunchedEffect(dueDate, datePicker, pickingReminderDate) {
        picker.selectedDateMillis = todoDatePickerMillis(if (pickingReminderDate) dueDate?.minusDays((edit.reminderOffset / 1440).toLong()) else dueDate)
    }
    androidx.activity.compose.BackHandler(show && advanced) { advanced = false }
    top.yukonga.miuix.kmp.overlay.OverlayBottomSheet(
        show = show, title = null, onDismissRequest = onDismiss, onDismissFinished = onDismissFinished,
        backgroundColor = Color.Transparent, dragHandleColor = Color.Transparent,
        defaultWindowInsetsPadding = true,
        insideMargin = androidx.compose.ui.unit.DpSize(0.dp, 0.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!advanced) TodoAiGlowBar(aiText, { aiText = it }, aiLoading, backdrop, config,
                { onAiExtract(aiText) }, onPhotoExtract)
            Column(
                Modifier.fillMaxWidth().quickSheetBackdropModifier(backdrop = backdrop, config = config,
                    blurRadius = 12.dp, courseCardStyle = true).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    TodoEditorIconButton(if (advanced) Icons.Default.ChevronLeft else Icons.Default.Close,
                        if (advanced) "返回" else "取消", config = config, backdrop = backdrop,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = { if (advanced) advanced = false else onDismiss() })
                    Text(if (advanced) "高级" else if (edit.sourceId == null) "新建待办事项" else "编辑待办事项",
                        Modifier.align(Alignment.Center).padding(horizontal = 56.dp), color = foreground, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (!advanced) TodoEditorActionPill("确认保存", backdrop = backdrop, config = config,
                        modifier = Modifier.align(Alignment.CenterEnd).size(48.dp), primary = true,
                        enabled = edit.title.isNotBlank() && !aiLoading && !saving, onClick = ::saveEdit) {
                        Icon(Icons.Default.Check, "确认保存", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = if (advanced) 560.dp else 350.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (!advanced) {
                        item { DialogCapsuleField(edit.title, { updateEdit(edit.copy(title = it)) }, "输入待办标题", config,
                            Modifier.fillMaxWidth(), cornerRadius = 18.dp, fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true) }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TodoEditorActionPill(edit.date.ifBlank { "日期" }, backdrop = backdrop, config = config,
                                    modifier = Modifier.weight(1f), onClick = { pickingReminderDate = false; datePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp), tint = TodoEditorAccent)
                                    Text(edit.date.ifBlank { "日期" }, color = TodoEditorAccent)
                                }
                                TodoEditorActionPill(priorityLabel(edit.priority), backdrop = backdrop, config = config,
                                    modifier = Modifier.weight(1f), onClick = { priorities = !priorities }) {
                                    Icon(Icons.Default.Flag, null, Modifier.size(20.dp), tint = TodoEditorAccent)
                                    Text(priorityLabel(edit.priority), color = TodoEditorAccent)
                                }
                            }
                        }
                        if (priorities) item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                (0..3).forEach { priority -> TodoEditorChoicePill(priorityLabel(priority), edit.priority == priority,
                                    config, backdrop, Modifier.widthIn(min = 64.dp, max = 104.dp)) {
                                    updateEdit(edit.copy(priority = priority)); priorities = false
                                } }
                            }
                        }
                        item {
                            TodoEditorActionPill("高级", backdrop = backdrop, config = config,
                                modifier = Modifier.fillMaxWidth(), onClick = { advanced = true }) {
                                Icon(Icons.Default.Tune, null, Modifier.size(20.dp), tint = TodoEditorAccent)
                                Text("高级", Modifier.weight(1f), color = TodoEditorAccent)
                                Icon(Icons.Default.ChevronRight, null, tint = TodoEditorAccent)
                            }
                        }
                    } else {
                        item { DialogCapsuleField(edit.description, { updateEdit(edit.copy(description = it)) }, "备注", config,
                            Modifier.fillMaxWidth(), minLines = 2, cornerRadius = 18.dp, fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true) }
                        item {
                            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TodoEditorSelectorRow("截止日期", edit.date.ifBlank { "未设置" }, config, backdrop, selected = dueDate != null,
                                        onClick = { pickingReminderDate = false; datePicker = true })
                                    TodoEditorSelectorRow("分组", groups.firstOrNull { it.id == edit.groupId }?.name ?: "未分组",
                                        config, backdrop, selected = edit.groupId != null, buttonModifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInWindow() },
                                        onClick = { activeMenu = TodoEditorMenu.Group })
                                }
                            }
                        }
                        item {
                            TodoEditorSectionHeader("时间")
                            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TodoAdvancedToggle("全天", edit.time.isBlank(), backdrop, config) {
                                        updateEdit(edit.copy(time = if (it) "" else "23:00", endTime = "",
                                            reminderMode = if (edit.reminderMode == "NONE" || edit.reminderMode == "LEGACY") edit.reminderMode else if (it) "ALL_DAY" else "BEFORE"))
                                    }
                                    TodoAdvancedToggle("时间段", edit.endTime.isNotBlank(), backdrop, config) {
                                        val start = runCatching { java.time.LocalTime.parse(edit.time) }.getOrDefault(java.time.LocalTime.of(9, 0))
                                        updateEdit(edit.copy(time = start.toString(), endTime = if (it) start.plusHours(1).toString() else ""))
                                    }
                                    if (edit.time.isNotBlank()) DialogCapsuleField(edit.time, { updateEdit(edit.copy(time = it)) }, "开始时间 HH:mm", config,
                                        Modifier.fillMaxWidth(), fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true)
                                    if (edit.endTime.isNotBlank()) DialogCapsuleField(edit.endTime, { updateEdit(edit.copy(endTime = it)) }, "结束时间 HH:mm", config,
                                        Modifier.fillMaxWidth(), fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true)
                                }
                            }
                        }
                        item {
                            TodoEditorSectionHeader("提醒")
                            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("提醒时机", color = foreground)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TodoEditorChoicePill("无", edit.reminderMode == "NONE", config, backdrop, Modifier.widthIn(min = 56.dp, max = 100.dp)) {
                                            updateEdit(edit.copy(reminderMode = "NONE"))
                                        }
                                        if (edit.reminderMode == "LEGACY") Text("沿用原提醒设置", color = foreground)
                                        if (edit.time.isBlank()) TodoEditorChoicePill("指定时刻", edit.reminderMode == "ALL_DAY", config, backdrop,
                                            Modifier.widthIn(min = 96.dp, max = 160.dp)) { updateEdit(edit.copy(reminderMode = "ALL_DAY")) }
                                        else listOf(1, 5, 30, 60, 120).forEach { minutes ->
                                            TodoEditorChoicePill("提前${minutes}分", edit.reminderMode == "BEFORE" && edit.reminderOffset == minutes,
                                                config, backdrop, Modifier.widthIn(min = 84.dp, max = 124.dp)) {
                                                updateEdit(edit.copy(reminderMode = "BEFORE", reminderOffset = minutes))
                                            }
                                        }
                                    }
                                    if (edit.reminderMode !in listOf("NONE", "LEGACY")) {
                                        if (edit.reminderMode == "ALL_DAY") TodoEditorSelectorRow("提醒日期",
                                            dueDate?.minusDays((edit.reminderOffset / 1440).toLong())?.toString() ?: "先选择截止日期", config, backdrop,
                                            onClick = { if (dueDate != null) { pickingReminderDate = true; datePicker = true } else validationError = "请先选择截止日期" })
                                        else DialogCapsuleField(edit.reminderOffset.toString(), { value -> value.toIntOrNull()?.takeIf { it >= 0 }?.let { updateEdit(edit.copy(reminderOffset = it)) } },
                                            "自定义提前分钟", config,
                                            Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                            fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true)
                                        if (edit.reminderMode == "ALL_DAY") DialogCapsuleField(edit.reminderTime, { updateEdit(edit.copy(reminderTime = it)) }, "提醒时间 HH:mm", config,
                                            Modifier.fillMaxWidth(), fieldTextColor = Color(0xFF14253D), fieldLightStyleOverride = true)
                                    }
                                    TodoAdvancedToggle("持续提醒", edit.persistentReminder, backdrop, config) { updateEdit(edit.copy(persistentReminder = it)) }
                                    TodoAdvancedToggle("强提醒", edit.strongReminder, backdrop, config) { updateEdit(edit.copy(strongReminder = it)) }
                                }
                            }
                        }
                        item {
                            TodoEditorSectionHeader("重复与关联")
                            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TodoEditorSelectorRow("重复周期", repeatLabel(edit.repeatRule), config, backdrop, selected = edit.repeatRule != "NONE",
                                        buttonModifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInWindow() },
                                        onClick = { activeMenu = TodoEditorMenu.Repeat })
                                    TodoEditorSelectorRow("关联课程", courses.firstOrNull { it.id == edit.courseId }?.name ?: "无",
                                        config, backdrop, selected = edit.courseId != null, buttonModifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInWindow() },
                                        onClick = { activeMenu = TodoEditorMenu.Course })
                                    TodoAdvancedToggle("置顶", edit.pinned, backdrop, config) { updateEdit(edit.copy(pinned = it)) }
                                }
                            }
                        }
                        if (edit.extractedSubtasks.isNotEmpty()) item {
                            Text("子任务", color = foreground)
                            edit.extractedSubtasks.forEach { Text("• $it", color = foreground) }
                        }
                        if (edit.sourceId != null) item { TodoGlassTextButton("删除待办", config, backdrop, onClick = onDelete) }
                    }
                    validationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
                }
            }
        }
    }
    if (activeMenu != null) {
        val menuItems = when (requireNotNull(activeMenu)) {
            TodoEditorMenu.Group -> listOf(SleepDownLiquidMenuItem(
                key = "todo-ungrouped", text = "未分组", selected = edit.groupId == null,
                onClick = { updateEdit(edit.copy(groupId = null)); activeMenu = null }
            )) + groups.map { group ->
                SleepDownLiquidMenuItem(
                    key = "todo-group-${group.id}",
                    text = group.name,
                    selected = group.id == edit.groupId,
                    onClick = { updateEdit(edit.copy(groupId = group.id)); activeMenu = null }
                )
            }.ifEmpty {
                listOf(SleepDownLiquidMenuItem(key = "todo-no-groups", text = "暂无分组", enabled = false))
            }
            TodoEditorMenu.Course -> listOf(
                SleepDownLiquidMenuItem(
                    key = "todo-course-none",
                    text = "无",
                    selected = edit.courseId == null,
                    onClick = { updateEdit(edit.copy(courseId = null)); activeMenu = null }
                )
            ) + courses.map { course ->
                SleepDownLiquidMenuItem(
                    key = "todo-course-${course.id}",
                    text = course.name,
                    selected = edit.courseId == course.id,
                    onClick = { updateEdit(edit.copy(courseId = course.id)); activeMenu = null }
                )
            }
            TodoEditorMenu.Repeat -> listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").map { rule ->
                SleepDownLiquidMenuItem(
                    key = "todo-repeat-$rule",
                    text = repeatLabel(rule),
                    selected = edit.repeatRule == rule,
                    onClick = { updateEdit(edit.copy(repeatRule = rule)); activeMenu = null }
                )
            }
        }
        SleepDownLiquidCascadingPopup(
            show = true,
            anchorBounds = menuAnchor,
            items = menuItems,
            onDismissRequest = { activeMenu = null },
            backdrop = backdrop,
            config = config,
            panelMinWidth = 184.dp
        )
    }
    SleepDownPickerDialog(
        show = datePicker,
        title = if (pickingReminderDate) "选择提醒日期" else "选择截止日期",
        onDismissRequest = { datePicker = false },
        backdrop = backdrop,
        config = config,
        scrollableContent = true,
        bottomActions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                QuickSheetLiquidAction(
                    label = "取消",
                    enabled = true,
                    backdrop = backdrop,
                    config = config,
                    accented = true,
                    height = 48.dp,
                    onClick = { datePicker = false }
                )
                QuickSheetLiquidAction(
                    label = "确定",
                    enabled = true,
                    backdrop = backdrop,
                    config = config,
                    primary = true,
                    height = 48.dp,
                    onClick = {
                        picker.selectedDateMillis?.let { millis ->
                            val date = todoDatePickerDate(millis)
                            if (pickingReminderDate) {
                                val days = dueDate?.let { java.time.temporal.ChronoUnit.DAYS.between(date, it) }
                                if (days != null && days in 0..(Int.MAX_VALUE / 1440).toLong()) updateEdit(edit.copy(reminderOffset = days.toInt() * 1440))
                                else validationError = "提醒日期不能晚于截止日期"
                            } else updateEdit(edit.copy(date = date.toString()))
                        }
                        datePicker = false
                    }
                )
            }
        }
    ) {
        DatePicker(picker, colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = Color.Transparent))
    }
}

@Composable
private fun TodoAdvancedToggle(label: String, checked: Boolean, backdrop: Backdrop?, config: ScheduleConfigEntity, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).clickable { onChange(!checked) }, color = sleepDownGlassForegroundColor(config))
        LiquidControlToggle(checked = checked, onCheckedChange = onChange, backdrop = backdrop, compact = true)
    }
}

@Composable
private fun TodoAiGlowBar(
    text: String, onText: (String) -> Unit, loading: Boolean, backdrop: Backdrop?, config: ScheduleConfigEntity,
    onExtract: () -> Unit, onImage: () -> Unit
) {
    val colors = listOf(Color(0xFFFFC6CC), Color(0xFFFFE5BC), Color(0xFFE3D8FF), Color(0xFFBEE8FF))
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
        Box(Modifier.matchParentSize().offset(y = 2.dp)
            .then(if (android.os.Build.VERSION.SDK_INT >= 31) Modifier.blur(14.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)
            .background(Brush.linearGradient(colors.map { it.copy(alpha = .75f) }), Capsule()))
        GlassSurface(backdrop, config,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(Capsule())
                .background(Brush.linearGradient(colors)).border(1.5.dp, Color.White.copy(alpha = .95f), Capsule()),
            baseSurfaceColorOverride = Color.White,
            tokens = TodoEditorWhiteButtonTokens.copy(surfaceAlpha = 0.10f),
            debugLabel = "TodoAiGlow") {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.text.BasicTextField(
                    value = text, onValueChange = onText, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF34485B)),
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                    decorationBox = { inner -> Box { if (text.isBlank()) Text("从文本中提取…", color = Color(0xFF667581)); inner() } }
                )
                if (loading) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), color = TodoEditorAccent, strokeWidth = 2.dp)
                else TodoEditorIconButton(if (text.isBlank()) Icons.Default.Image else Icons.Default.AutoAwesome,
                    if (text.isBlank()) "从图片提取" else "提取待办", config, backdrop,
                    onClick = { if (text.isBlank()) onImage() else onExtract() })
            }
        }
    }
}


@Composable
private fun TodoListStatus(state: TodoUiState, config: ScheduleConfigEntity, backdrop: Backdrop?, onRetry: () -> Unit) {
    // Failed reads must remain distinguishable from a genuinely empty list.
    TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.loadError != null) {
                Text(state.loadError, color = sleepDownGlassForegroundColor(config))
                TodoGlassTextButton("重试", config, backdrop, onClick = onRetry)
            } else {
                Text("正在读取日程…", color = sleepDownGlassForegroundColor(config))
                repeat(3) { Box(Modifier.fillMaxWidth(if (it == 1) .65f else 1f).height(18.dp)
                    .clip(RoundedCornerShape(8.dp)).background(sleepDownGlassForegroundColor(config).copy(alpha = .10f))) }
            }
        }
    }
}

@Composable
private fun TodoTrashPage(state: TodoUiState, config: ScheduleConfigEntity, backdrop: Backdrop?,
    onBack: () -> Unit, onRestore: (TodoItemEntity) -> Unit, onPurge: (TodoItemEntity) -> Unit, onClear: () -> Unit,
    onRetry: () -> Unit) {
    var purging by remember { mutableStateOf<TodoItemEntity?>(null) }
    var clearing by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            TodoGlassIconButton(Icons.Default.ChevronLeft, "返回待办", backdrop, config, onClick = onBack)
            Text("最近删除", Modifier.weight(1f).padding(start = 12.dp))
            TodoGlassTextButton("清空", config, backdrop, enabled = state.deletedItems.isNotEmpty()) { clearing = true }
        } }
        item { Text("删除的任务保留 30 天，永久删除后无法恢复。", color = sleepDownGlassForegroundColor(config)) }
        if (state.loading || state.loadError != null) item { TodoListStatus(state, config, backdrop, onRetry) }
        else if (state.deletedItems.isEmpty()) item { Text("最近删除为空", color = sleepDownGlassForegroundColor(config)) }
        items(todoListSelection(state, TodoFilter(TodoFilterKind.DELETED), true, null), key = { it.id }) { item ->
            TodoItemCard(item, state.deletedItems.filter { it.parentId == item.id }, state.groups, config, backdrop,
                onClick = {}, onToggle = {}, onToggleChild = {}, onPin = {}, onDelete = { purging = item },
                onAddSubtask = {}, showSubtaskAction = false, archived = true, onRestore = { onRestore(item) })
        }
    }
    SleepDownPickerDialog(show = purging != null || clearing, title = if (clearing) "清空最近删除？" else "永久删除？",
        backdrop = backdrop, config = config, onDismissRequest = { purging = null; clearing = false },
        bottomActions = { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TodoGlassTextButton("取消", config, backdrop) { purging = null; clearing = false }
            TodoGlassTextButton("确认删除", config, backdrop) { if (clearing) onClear() else purging?.let(onPurge); purging = null; clearing = false }
        } }) { Text("此操作无法撤销。") }
}

@Composable
private fun CalendarSyncReviewDialog(
    issue: TodoCalendarSyncIssue,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onCreateOrRetry: () -> Unit,
    onKeepLocal: () -> Unit
) {
    val message = when (issue.type) {
        TodoCalendarIssueType.LEGACY_ARCHIVE -> "“${issue.title}”来自旧备份，备份没有保存日历关联信息。无法判断系统日历中是否已有对应事项。"
        TodoCalendarIssueType.MISSING_EVENT -> "找不到“${issue.title}”原先关联的日历事项。它可能已被删除，或日历账户尚未完成同步。"
        TodoCalendarIssueType.DUPLICATE_EVENTS -> "系统日历中发现多个带有“${issue.title}”关联标记的事项。请先在系统日历中手动处理重复项；应用不会替你删除事件。"
        TodoCalendarIssueType.INTERRUPTED_INSERT -> "上次写入“${issue.title}”时应用中断，当前无法确认是否已经创建。请选择是否重新创建。"
        TodoCalendarIssueType.MARKER_NOT_PERSISTED -> "日历提供方没有保留“${issue.title}”的跨设备标记。可以继续使用本机已关联的事件，但恢复到其他设备时仍需确认。"
    }
    val primaryLabel = when (issue.type) {
        TodoCalendarIssueType.DUPLICATE_EVENTS -> "已整理，重试"
        TodoCalendarIssueType.MARKER_NOT_PERSISTED -> "仅本机继续"
        else -> "确认新建"
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CenterLiquidDialog(backdrop = backdrop, config = config) {
            LiquidDialogHeader(
                title = "日历关联需确认",
                onDismiss = onDismiss,
                backdrop = backdrop,
                config = config,
                onConfirm = onDismiss
            )
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(issue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (issue.type == TodoCalendarIssueType.LEGACY_ARCHIVE ||
                    issue.type == TodoCalendarIssueType.MISSING_EVENT ||
                    issue.type == TodoCalendarIssueType.INTERRUPTED_INSERT
                ) {
                    Text("新建前请确认不会产生你不需要的重复事项。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogLiquidButton(
                    backdrop = backdrop,
                    label = "跳过",
                    onClick = onSkip,
                    role = DialogButtonRole.Cancel,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                DialogLiquidButton(
                    backdrop = backdrop,
                    label = primaryLabel,
                    onClick = if (issue.type == TodoCalendarIssueType.MARKER_NOT_PERSISTED) onKeepLocal else onCreateOrRetry,
                    role = DialogButtonRole.Confirm,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun TodoAiSettingsDialog(
    initial: TodoAiConfig,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onSave: (TodoAiConfig) -> Unit
) {
    var url by remember { mutableStateOf(initial.apiUrl) }
    var key by remember { mutableStateOf(initial.apiKey) }
    var textModel by remember { mutableStateOf(initial.textModel) }
    var visionModel by remember { mutableStateOf(initial.visionModel) }
    var saveError by remember { mutableStateOf<String?>(null) }
    com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownFormDialog(onDismiss) {
        CenterLiquidDialog(backdrop = backdrop, config = config) {
            LiquidDialogHeader(
                title = "AI 提取设置",
                onDismiss = onDismiss,
                backdrop = backdrop,
                config = config,
                onConfirm = {
                    try {
                        onSave(TodoAiConfig(url, key, textModel, visionModel, initial.credentialError))
                    } catch (error: Exception) {
                        saveError = error.message ?: "保存失败，请检查配置后重试"
                    }
                }
            )
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("文本和图片只发送到你配置的模型服务。API Key 保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (saveError ?: initial.credentialError)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                DialogCapsuleField(url, { url = it }, "API URL", config, Modifier.fillMaxWidth())
                DialogCapsuleField(key, { key = it }, "API Key", config, Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation())
                DialogCapsuleField(textModel, { textModel = it }, "文本模型", config, Modifier.fillMaxWidth())
                DialogCapsuleField(visionModel, { visionModel = it }, "多模态模型", config, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ConfettiOverlay(token: Int) {
    var progressTarget by remember { mutableStateOf(0f) }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = .58f, stiffness = 280f),
        label = "todo-confetti"
    )
    LaunchedEffect(token) {
        if (token > 0) {
            progressTarget = 1f
            delay(680)
            progressTarget = 0f
        }
    }
    if (progress > .02f) {
        Canvas(Modifier.fillMaxSize()) {
            val colors = listOf(Color(0xFF49C5B6), Color(0xFFFFC857), Color(0xFFFF6B81), Color(0xFF7D83FF))
            repeat(34) { index ->
                val seed = index * 37
                val startX = (seed % 101) / 100f * size.width
                val fall = progress * size.height * (0.25f + (index % 7) * .09f)
                val x = startX + kotlin.math.sin(progress * 9f + index) * 36f
                val y = -20f + fall
                drawCircle(colors[index % colors.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)), radius = 3f + (index % 3), center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}

private fun TodoItemEntity.toEditState(): TodoEditState {
    val local = dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    return TodoEditState(
        source = this,
        title = title,
        description = description,
        date = local?.toLocalDate()?.toString().orEmpty(),
        time = if (allDay) "" else local?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty(),
        priority = priority,
        pinned = isPinned,
        groupId = groupId,
        courseId = courseId,
        parentId = parentId,
        repeatRule = repeatRule,
        endTime = endAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty(),
        reminderMode = reminderMode,
        reminderOffset = reminderOffsetMinutes,
        reminderTime = java.time.LocalTime.of(reminderTimeMinutes / 60, reminderTimeMinutes % 60).toString(),
        persistentReminder = persistentReminder,
        strongReminder = strongReminder
    )
}

private fun parseDue(dateText: String, timeText: String): Pair<Long, Boolean>? {
    val normalizedDate = dateText.trim()
    if (normalizedDate.isBlank()) {
        require(timeText.isBlank()) { "请先选择截止日期" }
        return null
    }
    val date = runCatching { LocalDate.parse(normalizedDate) }
        .getOrElse { throw IllegalArgumentException("截止日期格式应为 YYYY-MM-DD") }
    val zone = ZoneId.systemDefault()
    if (timeText.isBlank()) return date.atStartOfDay(zone).toInstant().toEpochMilli() to true
    val time = runCatching { java.time.LocalTime.parse(timeText.trim(), DateTimeFormatter.ofPattern("HH:mm")) }
        .getOrElse { throw IllegalArgumentException("时间格式应为 HH:mm") }
    return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli() to false
}
