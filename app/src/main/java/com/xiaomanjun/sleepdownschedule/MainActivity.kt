package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.app.ui.*
import com.xiaomanjun.sleepdownschedule.app.state.*
import com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoEntryRequest
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoPage
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoQuickCaptureContract
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.xiaomanjun.sleepdownschedule.core.identity.AppIconManager
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pendingExternalIcsUri = MutableStateFlow<Uri?>(null)
    private val pendingExternalIcsUriFlow = pendingExternalIcsUri.asStateFlow()
    private val pendingTodoEntry = MutableStateFlow<TodoEntryRequest?>(null)
    private val pendingTodoEntryFlow = pendingTodoEntry.asStateFlow()
    private val todoViewModel: TodoViewModel by viewModels()
    private var pendingTodoIntent: Intent? = null
    private var nextTodoEntryId = 0L
    private var enableAutoCalendarSyncAfterPermission = false
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val enablingAutoSync = enableAutoCalendarSyncAfterPermission
        enableAutoCalendarSyncAfterPermission = false
        if (result.values.all { it }) {
            if (enablingAutoSync) todoViewModel.setAutoCalendarSync(true)
            else todoViewModel.syncCalendar()
        } else {
            if (enablingAutoSync) todoViewModel.setAutoCalendarSync(false)
            android.widget.Toast.makeText(this, "未授予日历权限", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    private val startupContentReady = AtomicBoolean(false)
    private var startupPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptExternalIcsIntent(intent)
        @Suppress("DEPRECATION")
        val restoredTodoIntent = savedInstanceState?.getParcelable<Intent>(TodoRequestIntentStateKey)
        if (restoredTodoIntent != null) {
            acceptTodoIntent(restoredTodoIntent, savedInstanceState.getLong(TodoRequestIdStateKey))
        } else if (savedInstanceState == null) {
            acceptTodoIntent(intent)
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val contentRoot = findViewById<View>(android.R.id.content)
        startupPreDrawListener = ViewTreeObserver.OnPreDrawListener {
            if (startupContentReady.get()) {
                startupPreDrawListener?.let { listener ->
                    if (contentRoot.viewTreeObserver.isAlive) {
                        contentRoot.viewTreeObserver.removeOnPreDrawListener(listener)
                    }
                }
                startupPreDrawListener = null
                true
            } else {
                false
            }
        }.also(contentRoot.viewTreeObserver::addOnPreDrawListener)
        setContent {
            val app = application as CourseScheduleApp
            val viewModel: ScheduleViewModel = viewModel(
                factory = ScheduleViewModelFactory(app, app.repository)
            )
            val config by viewModel.themeConfig.collectAsStateWithLifecycle()
            val externalIcsUri by pendingExternalIcsUriFlow.collectAsStateWithLifecycle()
            val todoEntryRequest by pendingTodoEntryFlow.collectAsStateWithLifecycle()
            CourseScheduleTheme(config = config) {
                com.xiaomanjun.sleepdownschedule.feature.agent.WeatherLocationPermissionPrompt()
                CourseScheduleAppUi(
                    viewModel = viewModel,
                    todoViewModel = todoViewModel,
                    todoEntryRequest = todoEntryRequest,
                    onTodoEntryRequestConsumed = ::consumeTodoEntryRequest,
                    onRequestCalendarPermission = {
                        calendarPermissionLauncher.launch(
                            arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
                        )
                    },
                    onRequestAutoCalendarPermission = {
                        enableAutoCalendarSyncAfterPermission = true
                        calendarPermissionLauncher.launch(
                            arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
                        )
                    },
                    onRequestShizukuPermission = ::requestShizukuPermission,
                    externalIcsUri = externalIcsUri,
                    onExternalIcsConsumed = { consumed ->
                        pendingExternalIcsUri.compareAndSet(consumed, null)
                    },
                    onStartupContentReady = {
                        if (startupContentReady.compareAndSet(false, true)) {
                            contentRoot.postInvalidateOnAnimation()
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        val contentRoot = findViewById<View>(android.R.id.content)
        startupPreDrawListener?.let { listener ->
            if (contentRoot.viewTreeObserver.isAlive) {
                contentRoot.viewTreeObserver.removeOnPreDrawListener(listener)
            }
        }
        startupPreDrawListener = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptExternalIcsIntent(intent)
        acceptTodoIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingTodoEntry.value?.let { request ->
            pendingTodoIntent?.let { outState.putParcelable(TodoRequestIntentStateKey, it) }
            outState.putLong(TodoRequestIdStateKey, request.id)
        }
        super.onSaveInstanceState(outState)
    }

    private fun consumeTodoEntryRequest(id: Long) {
        if (pendingTodoEntry.value?.id == id) {
            pendingTodoEntry.value = null
            pendingTodoIntent = null
        }
    }

    @Suppress("DEPRECATION")
    private fun acceptTodoIntent(incoming: Intent?, restoredId: Long? = null) {
        incoming ?: return
        val action = incoming.action
        val mimeType = incoming.type.orEmpty().lowercase()
        val isCalendarShare = mimeType in setOf("text/calendar", "text/x-vcalendar", "application/ics")
        val todoId = incoming.getLongExtra(TodoQuickCaptureContract.EXTRA_OPEN_TODO_ID, 0L).takeIf { it > 0L }
        val courseId = incoming.getLongExtra(TodoQuickCaptureContract.EXTRA_INITIAL_COURSE_ID, 0L).takeIf { it > 0L }
        val startWithNewTask = incoming.getBooleanExtra(TodoQuickCaptureContract.EXTRA_START_NEW_TASK, false)
        val screenshotPath = incoming.getStringExtra(TodoQuickCaptureContract.EXTRA_SCREENSHOT_PATH)
        val requestShizuku = incoming.getBooleanExtra(TodoQuickCaptureContract.EXTRA_REQUEST_SHIZUKU, false)
        val sharedText = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val imageUris = buildList {
            when (action) {
                Intent.ACTION_SEND -> incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
                Intent.ACTION_SEND_MULTIPLE -> incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.let(::addAll)
            }
            if (isEmpty()) {
                val clip = incoming.clipData
                if (clip != null && (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE)) {
                    repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
                }
            }
        }.distinct()
        val isShare = !isCalendarShare && when (action) {
            Intent.ACTION_SEND -> mimeType.startsWith("text/") || mimeType.startsWith("image/") ||
                sharedText.isNotBlank() || imageUris.isNotEmpty()
            Intent.ACTION_SEND_MULTIPLE -> mimeType.startsWith("image/") || imageUris.isNotEmpty()
            else -> false
        }
        if (
            !isShare && todoId == null && courseId == null && !startWithNewTask &&
            screenshotPath.isNullOrBlank() && !requestShizuku
        ) return

        val id = restoredId?.takeIf { it > 0L } ?: ++nextTodoEntryId
        nextTodoEntryId = maxOf(nextTodoEntryId, id)
        pendingTodoIntent = Intent(incoming)
        pendingTodoEntry.value = TodoEntryRequest(
            id = id,
            destination = TodoPage.Tasks,
            todoId = todoId,
            courseId = courseId,
            startWithNewTask = startWithNewTask,
            sharedText = sharedText,
            imageUris = imageUris,
            screenshotPath = screenshotPath,
            requestShizukuPermission = requestShizuku,
            returnToCourse = false
        )
    }

    private fun requestShizukuPermission() {
        com.xiaomanjun.sleepdownschedule.feature.experimental.XiaomiSuperIsland.requestShizukuPermission { granted ->
            runOnUiThread {
                android.widget.Toast.makeText(
                    this,
                    if (granted) "Shizuku 已授权。再次点按快捷设置磁贴即可提取当前屏幕。"
                    else "请先安装并启动 Shizuku，再返回授权。",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun acceptExternalIcsIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                val mime = intent.type.orEmpty().lowercase()
                if (mime in setOf("text/calendar", "text/x-vcalendar", "application/ics") ||
                    intent.data?.lastPathSegment.orEmpty().endsWith(".ics", ignoreCase = true)
                ) intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri else null
            }
            else -> null
        } ?: return
        pendingExternalIcsUri.value = uri
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (hideFromRecentsEnabled) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.appTasks.forEach { it.setExcludeFromRecents(true) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hideFromRecentsEnabled) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.appTasks.forEach { it.setExcludeFromRecents(false) }
        }
    }

    private companion object {
        const val TodoRequestIntentStateKey = "todo_request_intent"
        const val TodoRequestIdStateKey = "todo_request_id"
    }
}

@Composable
fun CourseScheduleTheme(
    config: ScheduleConfigEntity = defaultConfig(),
    content: @Composable () -> Unit
) {
    val darkTheme = appUsesDarkTheme(config)
    val view = LocalView.current
    LaunchedEffect(config.followSystemDarkMode, darkTheme, view.context) {
        // Changing launcher aliases during the settings-to-home handoff can make ColorOS/Oplus
        // remove the visible task. Record the desired appearance here; CourseScheduleApp applies
        // the alias only after the whole process enters the background.
        if (view.context is MainActivity) {
            AppIconManager.syncAppearance(
                context = view.context.applicationContext,
                followsSystemDarkMode = config.followSystemDarkMode,
                darkTheme = darkTheme
            )
        }
    }
    LaunchedEffect(darkTheme, view) {
        val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
        window.applyAppThemeSurface(darkTheme)
        window.makeSystemBarsTransparent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (darkTheme) 0 else {
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                },
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                if (darkTheme) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
    val blue = Color(0xFF007AFF)
    val blueContainer = Color(0xFFD6E9FF)
    val darkBlueContainer = Color(0xFF003A66)
    MaterialTheme(
        shapes = com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownContinuousShapes,
        colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = blue,
                onPrimary = Color.White,
                primaryContainer = darkBlueContainer,
                onPrimaryContainer = Color(0xFFD6E9FF),
                secondary = blue,
                secondaryContainer = darkBlueContainer,
                tertiary = blue,
                tertiaryContainer = darkBlueContainer,
                background = Color.Black,
                surface = Color(0xFF111111),
                surfaceVariant = Color(0xFF1C1C1E),
                surfaceContainerHigh = Color(0xFF1C1C1E)
            )
        } else {
            lightColorScheme(
                primary = blue,
                onPrimary = Color.White,
                primaryContainer = blueContainer,
                onPrimaryContainer = Color(0xFF003A66),
                secondary = blue,
                secondaryContainer = blueContainer,
                tertiary = blue,
                tertiaryContainer = blueContainer,
                background = Color.White,
                surface = Color.White,
                surfaceVariant = Color(0xFFF2F2F7),
                surfaceContainerHigh = Color.White
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Suppress("DEPRECATION")
private fun android.view.Window.makeSystemBarsTransparent() {
    statusBarColor = android.graphics.Color.TRANSPARENT
    navigationBarColor = android.graphics.Color.TRANSPARENT
}

internal fun android.view.Window.applyAppThemeSurface(darkTheme: Boolean) {
    val backgroundColor = if (darkTheme) {
        android.graphics.Color.BLACK
    } else {
        android.graphics.Color.rgb(0xED, 0xEE, 0xF3)
    }
    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(backgroundColor))
    decorView.setBackgroundColor(backgroundColor)
}
