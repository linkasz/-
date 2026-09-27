package com.xiaomanjun.sleepdownschedule.feature.todo

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.model.AppState
import com.xiaomanjun.sleepdownschedule.model.CourseEntity
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.glassBackdropProducer
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassLayerBackdrop
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassTokens
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TodoActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels()
    private var incomingCourseId by mutableStateOf<Long?>(null)
    private var incomingStartNewTask by mutableStateOf(false)
    private var incomingTodoId by mutableStateOf<Long?>(null)
    private var enableAutoCalendarSyncAfterPermission = false
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val enablingAutoSync = enableAutoCalendarSyncAfterPermission
        enableAutoCalendarSyncAfterPermission = false
        if (result.values.all { it }) {
            if (enablingAutoSync) viewModel.setAutoCalendarSync(true)
            else viewModel.syncCalendar()
        } else {
            if (enablingAutoSync) viewModel.setAutoCalendarSync(false)
            Toast.makeText(this, "未授予日历权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        incomingCourseId = intent.getLongExtra(TodoQuickCaptureContract.EXTRA_INITIAL_COURSE_ID, 0L).takeIf { it > 0L }
        incomingStartNewTask = intent.getBooleanExtra(TodoQuickCaptureContract.EXTRA_START_NEW_TASK, false)
        incomingTodoId = intent.getLongExtra(TodoQuickCaptureContract.EXTRA_OPEN_TODO_ID, 0L).takeIf { it > 0L }
        consumeSharedIntent(intent)
        setContent {
            val app = application as CourseScheduleApp
            var scheduleState by remember { mutableStateOf(AppState()) }
            LaunchedEffect(Unit) {
                scheduleState = withContext(Dispatchers.IO) { app.repository.activeSnapshot() }
            }
            CourseScheduleTheme(config = scheduleState.config) {
                TodoPlusScreen(
                    viewModel = viewModel,
                    config = scheduleState.config,
                    schedule = scheduleState,
                    initialCourseId = incomingCourseId,
                    startWithNewTask = incomingStartNewTask,
                    initialTodoId = incomingTodoId,
                    onInitialCourseConsumed = {
                        incomingCourseId = null
                        incomingStartNewTask = false
                        intent.removeExtra(TodoQuickCaptureContract.EXTRA_INITIAL_COURSE_ID)
                        intent.removeExtra(TodoQuickCaptureContract.EXTRA_START_NEW_TASK)
                    },
                    onInitialTodoConsumed = {
                        incomingTodoId = null
                        intent.removeExtra(TodoQuickCaptureContract.EXTRA_OPEN_TODO_ID)
                    },
                    requestCalendarPermission = {
                        calendarPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    },
                    requestAutoCalendarPermission = {
                        enableAutoCalendarSyncAfterPermission = true
                        calendarPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingCourseId = intent.getLongExtra(TodoQuickCaptureContract.EXTRA_INITIAL_COURSE_ID, 0L).takeIf { it > 0L }
        incomingStartNewTask = intent.getBooleanExtra(TodoQuickCaptureContract.EXTRA_START_NEW_TASK, false)
        incomingTodoId = intent.getLongExtra(TodoQuickCaptureContract.EXTRA_OPEN_TODO_ID, 0L).takeIf { it > 0L }
        consumeSharedIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun consumeSharedIntent(intent: Intent?) {
        val screenshotPath = intent?.getStringExtra(TodoQuickCaptureContract.EXTRA_SCREENSHOT_PATH)
        if (!screenshotPath.isNullOrBlank()) {
            val screenshot = runCatching { File(screenshotPath).canonicalFile }.getOrNull()
            val captureDirectory = getExternalFilesDir(null)
                ?.let { File(it, "quick_capture").canonicalFile }
            if (
                screenshot != null &&
                captureDirectory != null &&
                screenshot.path.startsWith(captureDirectory.path + File.separator) &&
                screenshot.isFile
            ) {
                viewModel.extractFromImages(
                    listOf(Uri.fromFile(screenshot)),
                    "从当前屏幕提取待办",
                    onComplete = { screenshot.delete() }
                )
            }
            return
        }
        if (intent?.getBooleanExtra(TodoQuickCaptureContract.EXTRA_REQUEST_SHIZUKU, false) == true) {
            com.xiaomanjun.sleepdownschedule.feature.experimental.XiaomiSuperIsland.requestShizukuPermission { granted ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (granted) "Shizuku 已授权。再次点按快捷设置磁贴即可提取当前屏幕。"
                        else "请先安装并启动 Shizuku，再返回授权。",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
                val image = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (image != null) viewModel.extractFromImages(listOf(image), text)
                else if (text.isNotBlank()) viewModel.extractFromText(text)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
                val images = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                if (images.isNotEmpty()) viewModel.extractFromImages(images, text)
                else if (text.isNotBlank()) viewModel.extractFromText(text)
            }
        }
    }
}

private data class TodoEditState(
    val source: TodoItemEntity? = null,
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
    val extractedSubtasks: List<String> = emptyList()
)

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TodoPlusScreen(
    viewModel: TodoViewModel,
    config: ScheduleConfigEntity,
    schedule: AppState,
    initialCourseId: Long? = null,
    startWithNewTask: Boolean = false,
    initialTodoId: Long? = null,
    onInitialCourseConsumed: () -> Unit = {},
    onInitialTodoConsumed: () -> Unit = {},
    requestCalendarPermission: () -> Unit,
    requestAutoCalendarPermission: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val calendarMessage by viewModel.calendarMessage.collectAsStateWithLifecycle()
    val autoSync by viewModel.autoCalendarSync.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(TodoPage.Tasks) }
    var editor by remember { mutableStateOf(initialCourseId?.takeIf { startWithNewTask }?.let { TodoEditState(courseId = it) }) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showGroupManage by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var filterGroup by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterCourse by rememberSaveable { mutableStateOf(initialCourseId) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var calendarWeekMode by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var completionPulse by remember { mutableIntStateOf(0) }
    var soundEnabled by remember(context) {
        mutableStateOf(context.getSharedPreferences("schedule_plus_todo", android.content.Context.MODE_PRIVATE)
            .getBoolean("completion_sound", true))
    }
    val haptics = LocalHapticFeedback.current
    fun celebrateCompletion() {
        completionPulse++
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (soundEnabled) {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 250)
        }
    }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        if (uris.isNotEmpty()) viewModel.extractFromImages(uris)
    }
    val aiConfig = remember(showAiSettings) { viewModel.readAiConfig() }
    val backgroundBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Background,
        providerId = "todo-background"
    )

    LaunchedEffect(initialCourseId, startWithNewTask) {
        when {
            initialCourseId != null -> {
                val courseId = requireNotNull(initialCourseId)
                page = TodoPage.Tasks
                filterCourse = courseId
                if (startWithNewTask) editor = TodoEditState(courseId = courseId)
                onInitialCourseConsumed()
            }
            startWithNewTask -> {
                page = TodoPage.Tasks
                editor = TodoEditState()
                onInitialCourseConsumed()
            }
        }
    }

    LaunchedEffect(initialTodoId, state.items) {
        val todoId = initialTodoId ?: return@LaunchedEffect
        val item = state.items.firstOrNull { it.id == todoId } ?: return@LaunchedEffect
        page = TodoPage.Tasks
        filterCourse = item.courseId
        editor = item.toEditState()
        onInitialTodoConsumed()
    }

    LaunchedEffect(aiState.result) {
        aiState.result?.let { result ->
            val target = editor ?: TodoEditState()
            editor = target.copy(
                title = result.title,
                description = result.description,
                date = result.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }.orEmpty(),
                time = if (result.allDay) "" else result.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty(),
                priority = result.priority,
                repeatRule = result.repeatRule,
                extractedSubtasks = result.subtasks
            )
            viewModel.clearAiState()
        }
    }
    LaunchedEffect(aiState.error) {
        aiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearAiState() }
    }
    LaunchedEffect(calendarMessage) {
        calendarMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearCalendarMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().glassBackdropProducer(backgroundBackdrop)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f)
                        )
                    )
                )
        )
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                TodoDock(page = page, onSelect = { page = it }, config = config, backdrop = backgroundBackdrop)
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .imePadding()
            ) {
                TodoHeader(
                    page = page,
                    onBack = onBack,
                    onAdd = { editor = TodoEditState(groupId = filterGroup ?: state.groups.firstOrNull()?.id, courseId = filterCourse) },
                    onAi = { showAiSettings = true },
                    onPhoto = { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onGroups = { showGroupManage = true }
                )
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (slideInHorizontally { it / 7 } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it / 7 } + fadeOut()
                        )
                    },
                    label = "todo-page-transition"
                ) { selectedPage ->
                    when (selectedPage) {
                        TodoPage.Tasks -> TodoTasksPage(
                            state = state,
                            schedule = schedule,
                            backdrop = backgroundBackdrop,
                            showCompleted = showCompleted,
                            onShowCompleted = { showCompleted = it },
                            soundEnabled = soundEnabled,
                            onSoundEnabled = { enabled ->
                                soundEnabled = enabled
                                context.getSharedPreferences("schedule_plus_todo", android.content.Context.MODE_PRIVATE)
                                    .edit().putBoolean("completion_sound", enabled).apply()
                            },
                            filterGroup = filterGroup,
                            filterCourse = filterCourse,
                            onFilterGroup = { filterGroup = it },
                            onFilterCourse = { filterCourse = it },
                            onEdit = { item -> editor = item.toEditState() },
                            onToggle = { item -> viewModel.toggle(item, ::celebrateCompletion) },
                            onPin = viewModel::togglePin,
                            onDelete = viewModel::delete,
                            onAdd = { editor = TodoEditState(groupId = filterGroup ?: state.groups.firstOrNull()?.id, courseId = filterCourse) },
                            onAddSubtask = { item -> editor = TodoEditState(groupId = item.groupId, courseId = item.courseId, parentId = item.id) }
                        )
                        TodoPage.Calendar -> TodoCalendarPage(
                            state = state,
                            config = config,
                            backdrop = backgroundBackdrop,
                            selectedDate = selectedDate,
                            month = month,
                            weekMode = calendarWeekMode,
                            autoSync = autoSync,
                            onMonth = { month = it },
                            onDate = { selectedDate = it },
                            onWeekMode = { calendarWeekMode = it },
                            onAutoSync = { enabled ->
                                if (enabled) {
                                    requestAutoCalendarPermission()
                                } else {
                                    viewModel.setAutoCalendarSync(false)
                                }
                            },
                            onSync = requestCalendarPermission,
                            onToggle = { item -> viewModel.toggle(item, ::celebrateCompletion) },
                            onEdit = { item -> editor = item.toEditState() }
                        )
                        TodoPage.Insights -> TodoInsightsPage(state.items, config, backgroundBackdrop)
                    }
                }
            }
        }
        ConfettiOverlay(completionPulse)
    }

    if (editor != null) {
        TodoEditSheet(
            current = requireNotNull(editor),
            groups = state.groups,
            courses = schedule.courses,
            backdrop = backgroundBackdrop,
            config = config,
            aiLoading = aiState.isLoading,
            onChange = { editor = it },
            onAiExtract = { text ->
                if (viewModel.readAiConfig().apiKey.isBlank()) showAiSettings = true
                else viewModel.extractFromText(text)
            },
            onSave = { form ->
                val due = parseDue(form.date, form.time)
                val draft = TodoDraft(
                    id = form.source?.id ?: 0,
                    title = form.title,
                    description = form.description,
                    dueAt = due?.first,
                    allDay = due?.second ?: false,
                    priority = form.priority,
                    isPinned = form.pinned,
                    groupId = form.groupId,
                    parentId = form.parentId,
                    courseId = form.courseId,
                    repeatRule = form.repeatRule
                )
                viewModel.save(draft) { parentId ->
                    form.extractedSubtasks.forEach { title ->
                        viewModel.save(TodoDraft(title = title, groupId = form.groupId, courseId = form.courseId, parentId = parentId))
                    }
                    viewModel.autoSyncAfterChange()
                    editor = null
                }
            },
            onDelete = { editor?.source?.let(viewModel::delete); editor = null },
            onDismiss = { editor = null }
        )
    }
    if (showAiSettings) {
        AiSettingsDialog(
            initial = aiConfig,
            backdrop = backgroundBackdrop,
            config = config,
            onDismiss = { showAiSettings = false },
            onSave = { configValue ->
                runCatching { viewModel.saveAiConfig(configValue) }
                    .onSuccess { showAiSettings = false; Toast.makeText(context, "AI 配置已保存", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            }
        )
    }
    if (showGroupManage) {
        Dialog(onDismissRequest = { showGroupManage = false }) {
            GlassSurface(
                backdrop = backgroundBackdrop,
                config = config,
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                tokens = GlassTokens.dialog(),
                debugLabel = "todo-groups-dialog"
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("任务分组", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    state.groups.forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(group.name, Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteGroup(group) }) { Icon(Icons.Default.Delete, "删除分组") }
                        }
                    }
                    OutlinedTextField(groupName, { groupName = it }, label = { Text("新分组名称") }, singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showGroupManage = false }) { Text("完成") }
                        TextButton(onClick = { viewModel.addGroup(groupName); groupName = "" }) { Text("添加") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoHeader(
    page: TodoPage,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onAi: () -> Unit,
    onPhoto: () -> Unit,
    onGroups: () -> Unit
) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回课程表") }
        Column(Modifier.weight(1f)) {
            Text("Schedule+", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                when (page) { TodoPage.Tasks -> "待办事项"; TodoPage.Calendar -> "日历"; TodoPage.Insights -> "效率洞察" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onGroups) { Icon(Icons.Default.MoreVert, "分组管理") }
        IconButton(onClick = onAi) { Icon(Icons.Default.AutoAwesome, "AI 配置") }
        IconButton(onClick = onPhoto) { Icon(Icons.Default.Share, "从图片提取") }
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "添加待办") }
    }
}

@Composable
private fun TodoTasksPage(
    state: TodoUiState,
    schedule: AppState,
    backdrop: Backdrop,
    showCompleted: Boolean,
    onShowCompleted: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabled: (Boolean) -> Unit,
    filterGroup: Long?,
    onFilterGroup: (Long?) -> Unit,
    filterCourse: Long?,
    onFilterCourse: (Long?) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onToggle: (TodoItemEntity) -> Unit,
    onPin: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit,
    onAdd: () -> Unit,
    onAddSubtask: (TodoItemEntity) -> Unit
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val tasks = state.items.filter {
        it.parentId == null &&
            (showCompleted || !it.isCompleted) &&
            (filterGroup == null || it.groupId == filterGroup) &&
            (filterCourse == null || it.courseId == filterCourse)
    }
    val filterCourseName = filterCourse?.let { id -> schedule.courses.firstOrNull { it.id == id }?.name }
    val todayCourses = schedule.courses.filter { course -> courseHappensToday(course, schedule, today) }
        .sortedBy { course -> schedule.periods.firstOrNull { it.periodIndex == course.periods.minOrNull() }?.startTime ?: "99:99" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("今天 · ${today.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE))}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${tasks.count { it.dueAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() == today } == true }} 项到期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("显示已完成", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Switch(checked = showCompleted, onCheckedChange = onShowCompleted)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("完成提示音", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Switch(checked = soundEnabled, onCheckedChange = onSoundEnabled)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filterGroup == null, onClick = { onFilterGroup(null) }, label = { Text("全部") })
                state.groups.forEach { group ->
                    FilterChip(selected = filterGroup == group.id, onClick = { onFilterGroup(group.id) }, label = { Text(group.name) })
                }
            }
            if (filterCourse != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("课程关联：${filterCourseName ?: "已关联课程"}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { onFilterCourse(null) }) { Text("清除") }
                }
            }
        }
        item {
                TodoGlassCard(config = schedule.config, backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今日时间轴", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val dueToday = state.items.filter { it.parentId == null && !it.isCompleted && it.dueAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() == today } == true }
                    val rows = buildList {
                        todayCourses.forEach { course ->
                            val start = schedule.periods.firstOrNull { it.periodIndex == course.periods.minOrNull() }?.startTime ?: "课表"
                            add(AgendaLine(start, "课程", course.name))
                        }
                        dueToday.forEach { item ->
                            val dateTime = item.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                            add(AgendaLine(if (item.allDay) "全天" else dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "今天", "待办", item.title))
                        }
                    }.sortedBy { it.sortTime }
                    if (rows.isEmpty()) Text("今天没有课程或到期待办", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    rows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.time, Modifier.width(58.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Box(Modifier.width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .38f)))
                            Text(row.kind, Modifier.padding(start = 10.dp).width(42.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(row.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (tasks.isEmpty()) {
            item {
                TodoGlassCard(schedule.config, backdrop, Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("还没有待办", style = MaterialTheme.typography.titleMedium)
                        Text("添加一项任务，开始安排今天。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onAdd) { Text("创建待办") }
                    }
                }
            }
        }
        items(tasks, key = { it.id }) { item ->
            val children = state.items.filter { it.parentId == item.id }
            TodoItemCard(
                item = item,
                children = children,
                groups = state.groups,
                config = schedule.config,
                backdrop = backdrop,
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
}

private data class AgendaLine(val time: String, val kind: String, val title: String) {
    val sortTime: String get() = if (time == "全天" || time == "课表") "00:00" else time
}

private fun courseHappensToday(course: CourseEntity, schedule: AppState, today: LocalDate): Boolean {
    if (course.weekday != today.dayOfWeek.value) return false
    val week = schedule.config.currentWeek
    if (course.weeks.isNotEmpty() && week !in course.weeks) return false
    return when (course.weekParity.name) {
        "ODD" -> week % 2 == 1
        "EVEN" -> week % 2 == 0
        else -> true
    }
}

@Composable
private fun TodoItemCard(
    item: TodoItemEntity,
    children: List<TodoItemEntity>,
    groups: List<TodoGroupEntity>,
    config: ScheduleConfigEntity,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onToggleChild: (TodoItemEntity) -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TodoCheckmark(checked = item.isCompleted, onClick = onToggle)
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
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (item.isPinned) "取消置顶" else "置顶") }, onClick = { onPin(); menuOpen = false })
                        DropdownMenuItem(text = { Text("添加子任务") }, onClick = { onAddSubtask(); menuOpen = false })
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { onClick(); menuOpen = false })
                        DropdownMenuItem(text = { Text("删除") }, onClick = { onDelete(); menuOpen = false })
                    }
                }
            }
            if (item.description.isNotBlank()) Text(item.description, Modifier.padding(start = 54.dp, bottom = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            if (children.isNotEmpty()) {
                Divider(Modifier.padding(start = 50.dp, top = 4.dp, bottom = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                children.forEach { child ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TodoCheckmark(checked = child.isCompleted, onClick = { onToggleChild(child) })
                        Text(child.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = if (child.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoCheckmark(checked: Boolean, onClick: () -> Unit) {
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
        Modifier.size(26.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, primary.copy(alpha = if (checked) 0.95f else 0.72f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Default.Check, contentDescription = "已完成", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TodoCalendarPage(
    state: TodoUiState,
    config: ScheduleConfigEntity,
    backdrop: Backdrop,
    selectedDate: LocalDate,
    month: YearMonth,
    weekMode: Boolean,
    autoSync: Boolean,
    onMonth: (YearMonth) -> Unit,
    onDate: (LocalDate) -> Unit,
    onWeekMode: (Boolean) -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onSync: () -> Unit,
    onToggle: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit
) {
    val itemsOnDay = state.items.filter { item -> item.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate } == true }
    Column(Modifier.fillMaxSize()) {
        TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMonth(month.minusMonths(1)) }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                    Text(month.format(DateTimeFormatter.ofPattern("yyyy年 M月")), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onMonth(month.plusMonths(1)) }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
                    FilterChip(selected = weekMode, onClick = { onWeekMode(!weekMode) }, label = { Text(if (weekMode) "周" else "月") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                val first = month.atDay(1)
                val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
                val dates = if (weekMode) {
                    val start = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                    (0L..6L).map(start::plusDays)
                } else (0L..41L).map(gridStart::plusDays)
                LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(if (weekMode) 56.dp else 270.dp), userScrollEnabled = false) {
                    items(dates) { date ->
                        val hasTask = state.items.any { it.dueAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() == date } == true }
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(CircleShape)
                                .background(if (date == selectedDate) MaterialTheme.colorScheme.primary.copy(alpha = .22f) else Color.Transparent)
                                .clickable { onDate(date); if (date.month != month.month) onMonth(YearMonth.from(date)) }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(date.dayOfMonth.toString(), color = if (date.month == month.month) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f), style = MaterialTheme.typography.bodySmall)
                            Box(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(if (hasTask) MaterialTheme.colorScheme.primary else Color.Transparent))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${itemsOnDay.size} 项待办", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("自动同步", style = MaterialTheme.typography.labelMedium)
            Switch(checked = autoSync, onCheckedChange = onAutoSync)
            IconButton(onClick = onSync) { Icon(Icons.Default.CalendarMonth, "同步到系统日历") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)) {
            if (itemsOnDay.isEmpty()) item { Text("这一天没有待办", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(itemsOnDay, key = { it.id }) { item ->
                TodoItemCard(item, emptyList(), state.groups, config, backdrop, onClick = { onEdit(item) }, onToggle = { onToggle(item) }, onToggleChild = onToggle, onPin = {}, onDelete = {}, onAddSubtask = {})
            }
        }
    }
}

@Composable
private fun TodoInsightsPage(items: List<TodoItemEntity>, config: ScheduleConfigEntity, backdrop: Backdrop) {
    val rootItems = items.filter { it.parentId == null }
    val activeItems = rootItems.filterNot { it.isCompleted }
    val total = rootItems.size
    val completed = rootItems.count { it.isCompleted }
    val completion = if (total == 0) 0f else completed.toFloat() / total
    val now = System.currentTimeMillis()
    val weekAhead = now + 7 * 86_400_000L
    val highPriority = activeItems.count { it.priority >= 2 }
    val overdue = activeItems.count { it.dueAt?.let { dueAt -> dueAt < now } == true }
    val upcoming = activeItems.count { it.dueAt?.let { dueAt -> dueAt >= now && dueAt < weekAhead } == true }
    val unscheduled = activeItems.count { it.dueAt == null }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("完成率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${(completion * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Slider(value = completion, onValueChange = {}, enabled = false)
                    Text("已完成 $completed / $total 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("任务压力分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("仅统计未完成的主任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    InsightBar("高优先级", highPriority, activeItems.size, MaterialTheme.colorScheme.error)
                    InsightBar("已逾期", overdue, activeItems.size, MaterialTheme.colorScheme.error)
                    InsightBar("未来 7 天到期", upcoming, activeItems.size, MaterialTheme.colorScheme.tertiary)
                    InsightBar("未安排日期", unscheduled, activeItems.size, MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            TodoGlassCard(config, backdrop, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("近 7 天完成趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val counts = (6 downTo 0).map { offset ->
                        val date = LocalDate.now().minusDays(offset.toLong())
                        date to rootItems.count { it.completedAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() == date } == true }
                    }
                    Row(Modifier.fillMaxWidth().height(110.dp).padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        val max = (counts.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
                        counts.forEach { (date, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                                Box(Modifier.padding(vertical = 4.dp).width(22.dp).height((14 + 62f * count / max).dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .72f)))
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
private fun TodoDock(page: TodoPage, onSelect: (TodoPage) -> Unit, config: ScheduleConfigEntity, backdrop: Backdrop) {
    val bottomInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    Box(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 8.dp + bottomInset)) {
        GlassSurface(
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tokens = GlassTokens.pill(),
            debugLabel = "todo-dock"
        ) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                DockPage(page == TodoPage.Tasks, "待办", Icons.Default.CheckCircle) { onSelect(TodoPage.Tasks) }
                DockPage(page == TodoPage.Calendar, "日历", Icons.Default.CalendarMonth) { onSelect(TodoPage.Calendar) }
                DockPage(page == TodoPage.Insights, "洞察", Icons.Default.Insights) { onSelect(TodoPage.Insights) }
            }
        }
    }
}

@Composable
private fun RowScope.DockPage(selected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.58f, stiffness = 720f),
        label = "todo-dock-press"
    )
    Column(
        Modifier.weight(1f).fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodoGlassCard(config: ScheduleConfigEntity, backdrop: Backdrop?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        backdrop = backdrop,
        config = config,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        tokens = GlassTokens.courseCard(config.courseCardBlur),
        debugLabel = "todo-card",
        content = { Column(content = content) }
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun TodoEditSheet(
    current: TodoEditState,
    groups: List<TodoGroupEntity>,
    courses: List<CourseEntity>,
    backdrop: Backdrop,
    config: ScheduleConfigEntity,
    aiLoading: Boolean,
    onChange: (TodoEditState) -> Unit,
    onAiExtract: (String) -> Unit,
    onSave: (TodoEditState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var edit by remember(current.source?.id, current.parentId) { mutableStateOf(current) }
    var datePicker by remember { mutableStateOf(false) }
    var dueDate by remember(edit.date) { mutableStateOf<LocalDate?>(runCatching { LocalDate.parse(edit.date) }.getOrNull()) }
    var aiText by remember { mutableStateOf("") }
    var groupMenu by remember { mutableStateOf(false) }
    var courseMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        dragHandle = null
    ) {
        GlassSurface(
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tokens = GlassTokens.dialog(intensity = 1.05f),
            debugLabel = "todo-editor-sheet"
        ) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 18.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (edit.source == null) "添加待办" else "编辑待办", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (edit.source != null) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除待办") }
                }
            }
            item { OutlinedTextField(edit.title, { edit = edit.copy(title = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("标题") }, singleLine = true) }
            item { OutlinedTextField(edit.description, { edit = edit.copy(description = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("描述") }, minLines = 2, maxLines = 4) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(edit.date, { edit = edit.copy(date = it) }, modifier = Modifier.weight(1f).clickable { datePicker = true }, label = { Text("截止日期") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    OutlinedTextField(edit.time, { edit = edit.copy(time = it) }, modifier = Modifier.weight(1f), label = { Text("时间（可选）") }, placeholder = { Text("HH:mm") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                }
                TextButton(onClick = { datePicker = true }) { Text("选择日期") }
            }
            item {
                Text("优先级", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { priority -> FilterChip(selected = edit.priority == priority, onClick = { edit = edit.copy(priority = priority) }, label = { Text(priorityLabel(priority)) }) }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("分组", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { groupMenu = true }) { Text(groups.firstOrNull { it.id == edit.groupId }?.name ?: "选择分组") }
                        DropdownMenu(groupMenu, { groupMenu = false }) {
                            groups.forEach { group -> DropdownMenuItem(text = { Text(group.name) }, onClick = { edit = edit.copy(groupId = group.id); groupMenu = false }) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("关联课程", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { courseMenu = true }) { Text(courses.firstOrNull { it.id == edit.courseId }?.name ?: "无") }
                        DropdownMenu(courseMenu, { courseMenu = false }) {
                            DropdownMenuItem(text = { Text("无") }, onClick = { edit = edit.copy(courseId = null); courseMenu = false })
                            courses.forEach { course -> DropdownMenuItem(text = { Text(course.name) }, onClick = { edit = edit.copy(courseId = course.id); courseMenu = false }) }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("重复", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { repeatMenu = true }) { Text(repeatLabel(edit.repeatRule)) }
                        DropdownMenu(repeatMenu, { repeatMenu = false }) {
                            listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { rule -> DropdownMenuItem(text = { Text(repeatLabel(rule)) }, onClick = { edit = edit.copy(repeatRule = rule); repeatMenu = false }) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("置顶", Modifier.weight(1f))
                    Switch(checked = edit.pinned, onCheckedChange = { edit = edit.copy(pinned = it) })
                }
            }
            item {
                OutlinedTextField(aiText, { aiText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("粘贴文本，让 AI 提取待办") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI 仅在你配置模型服务后发送所选文字或图片。", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onAiExtract(aiText) }, enabled = aiText.isNotBlank() && !aiLoading) {
                        if (aiLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        Text("提取")
                    }
                }
                if (edit.extractedSubtasks.isNotEmpty()) Text("将同时创建 ${edit.extractedSubtasks.size} 个子任务", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Button(onClick = { onChange(edit); onSave(edit) }, modifier = Modifier.fillMaxWidth(), enabled = edit.title.isNotBlank()) { Text("保存待办") }
            }
        }
        }
    }
    if (datePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli())
        Dialog(onDismissRequest = { datePicker = false }) {
            GlassSurface(
                backdrop = backdrop,
                config = config,
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                tokens = GlassTokens.dialog(),
                debugLabel = "todo-date-picker"
            ) {
                Column(Modifier.padding(12.dp)) {
                    DatePicker(picker)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { datePicker = false }) { Text("取消") }
                        TextButton(onClick = {
                            picker.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                                edit = edit.copy(date = date.toString())
                                dueDate = date
                            }
                            datePicker = false
                        }) { Text("确定") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSettingsDialog(
    initial: TodoAiConfig,
    backdrop: Backdrop,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onSave: (TodoAiConfig) -> Unit
) {
    var url by remember { mutableStateOf(initial.apiUrl) }
    var key by remember { mutableStateOf(initial.apiKey) }
    var textModel by remember { mutableStateOf(initial.textModel) }
    var visionModel by remember { mutableStateOf(initial.visionModel) }
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            tokens = GlassTokens.dialog(),
            debugLabel = "todo-ai-settings-dialog"
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI 提取设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("文本和图片只发送到你配置的模型服务。API Key 保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API URL") }, singleLine = true)
                OutlinedTextField(key, { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(textModel, { textModel = it }, modifier = Modifier.fillMaxWidth(), label = { Text("文本模型") }, singleLine = true)
                OutlinedTextField(visionModel, { visionModel = it }, modifier = Modifier.fillMaxWidth(), label = { Text("多模态模型") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { onSave(TodoAiConfig(url, key, textModel, visionModel)) }) { Text("保存") }
                }
            }
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
        repeatRule = repeatRule
    )
}

private fun parseDue(dateText: String, timeText: String): Pair<Long, Boolean>? {
    val date = dateText.trim().takeIf(String::isNotBlank)?.let { LocalDate.parse(it) } ?: return null
    val zone = ZoneId.systemDefault()
    return if (timeText.isBlank()) date.atStartOfDay(zone).toInstant().toEpochMilli() to true
    else LocalDateTime.parse("${date}T${timeText.trim()}", DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zone).toInstant().toEpochMilli() to false
}
