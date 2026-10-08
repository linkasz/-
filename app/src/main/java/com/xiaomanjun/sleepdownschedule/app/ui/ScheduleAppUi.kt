package com.xiaomanjun.sleepdownschedule.app.ui
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath

import androidx.compose.animation.core.LinearEasing

import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.app.state.*
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import com.xiaomanjun.sleepdownschedule.transition.legacy.*
import com.xiaomanjun.sleepdownschedule.core.ui.settings.*
import com.xiaomanjun.sleepdownschedule.feature.settings.*
import com.xiaomanjun.sleepdownschedule.feature.course.management.HomeMenuActivitySourceFallback
import com.xiaomanjun.sleepdownschedule.feature.course.management.putCourseManagementInitialState
import com.xiaomanjun.sleepdownschedule.feature.schedule.*
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshWebSession
import com.xiaomanjun.sleepdownschedule.feature.schedule.manager.*
import com.xiaomanjun.sleepdownschedule.feature.schedule.picker.*
import com.xiaomanjun.sleepdownschedule.feature.home.*
import com.xiaomanjun.sleepdownschedule.feature.home.day.*
import com.xiaomanjun.sleepdownschedule.feature.home.week.*
import com.xiaomanjun.sleepdownschedule.feature.home.overlay.*

import com.xiaomanjun.sleepdownschedule.app.config.SleepDownRemoteConfig
import com.xiaomanjun.sleepdownschedule.core.performance.*
import com.xiaomanjun.sleepdownschedule.core.remoteconfig.*
import com.xiaomanjun.sleepdownschedule.core.wallpaper.*
import com.xiaomanjun.sleepdownschedule.domain.course.*
import com.xiaomanjun.sleepdownschedule.feature.course.editor.*
import com.xiaomanjun.sleepdownschedule.feature.importing.*
import com.xiaomanjun.sleepdownschedule.feature.importing.shiguang.ShiguangWarehouseUpdater
import com.xiaomanjun.sleepdownschedule.feature.importing.shiguang.describeAdapterRefresh
import com.xiaomanjun.sleepdownschedule.feature.importing.shiguang.uninstallShiguangRuntime

import com.xiaomanjun.sleepdownschedule.core.identity.AppDistribution
import com.xiaomanjun.sleepdownschedule.core.identity.currentIconResId
import com.xiaomanjun.sleepdownschedule.feature.backup.*
import com.xiaomanjun.sleepdownschedule.feature.widget.*
import com.xiaomanjun.sleepdownschedule.feature.agent.*
import com.xiaomanjun.sleepdownschedule.feature.agent.background.*
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoChromeActions
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoEntryRequest
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoEntryRequestSaver
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoPage
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoPlusScreen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.DatePickerDialog
import android.app.Application
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.net.toUri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.os.Build
import android.view.WindowManager
import android.view.PixelCopy
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.view.HapticFeedbackConstants
import android.view.WindowInsetsController
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.content.FileProvider
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import com.kyant.shapes.Capsule
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField as MaterialOutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.palette.graphics.Palette
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidButtonPressSnapshot
import com.kyant.backdrop.catalog.components.LiquidPanel
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.GlassRenderPhase
import com.xiaomanjun.sleepdownschedule.glass.GlassSamplingLink
import com.xiaomanjun.sleepdownschedule.glass.GlassTopologyNode
import com.xiaomanjun.sleepdownschedule.glass.GlassTopologyNodeRole
import com.xiaomanjun.sleepdownschedule.glass.LocalGlassSceneState
import com.xiaomanjun.sleepdownschedule.glass.glassBackdropProducer
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassCombinedBackdrop
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassLayerBackdrop
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassSceneState
import com.xiaomanjun.sleepdownschedule.glass.GlassBackendPolicy
import com.xiaomanjun.sleepdownschedule.transition.ActivityTransitionCoordinator
import com.xiaomanjun.sleepdownschedule.transition.CrossActivityTransitionHost
import com.xiaomanjun.sleepdownschedule.transition.LegacyTransitionProfile
import com.xiaomanjun.sleepdownschedule.transition.StaticTransitionAnchorProvider
import com.xiaomanjun.sleepdownschedule.transition.TransitionAnchorFrame
import com.xiaomanjun.sleepdownschedule.transition.TransitionLaunchResult
import com.xiaomanjun.sleepdownschedule.transition.TransitionPayload
import com.xiaomanjun.sleepdownschedule.transition.TransitionRouteCatalog
import com.xiaomanjun.sleepdownschedule.transition.TransitionRouteId
import com.xiaomanjun.sleepdownschedule.transition.openRegisteredActivity
import com.xiaomanjun.sleepdownschedule.transition.transitionRouteIdOrNull
import com.xiaomanjun.sleepdownschedule.domain.schedule.formatScheduleDate
import com.xiaomanjun.sleepdownschedule.domain.schedule.scheduleAdjustmentForDate
import com.xiaomanjun.sleepdownschedule.domain.schedule.adjustedTeachingWeekForDate
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.DisposableEffect
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.ceil
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun performButtonHaptic(view: android.view.View) {
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

private fun android.view.Window.setStatusBarDarkIcons(darkIcons: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        insetsController?.setSystemBarsAppearance(
            if (darkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = if (darkIcons) {
            decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data object TodoTasks : Screen
    data object TodoCalendar : Screen
    data object TodoInsights : Screen
    data object Config : Screen
}

private val ScreenSaver = Saver<Screen, String>(
    save = { screen -> when (screen) {
        Screen.Home -> "home"
        Screen.TodoTasks -> "todo_tasks"
        Screen.TodoCalendar -> "todo_calendar"
        Screen.TodoInsights -> "todo_insights"
        Screen.Config -> "settings"
    } },
    restore = { value -> when (value) {
        "todo_tasks" -> Screen.TodoTasks
        "todo_calendar" -> Screen.TodoCalendar
        "todo_insights" -> Screen.TodoInsights
        "settings" -> Screen.Config
        else -> Screen.Home
    } }
)

private fun Screen.isTodoDestination(): Boolean =
    this is Screen.TodoTasks || this is Screen.TodoCalendar || this is Screen.TodoInsights

internal fun Screen.dockIndex(): Int = when (this) {
    Screen.Home -> 0
    Screen.TodoTasks -> 1
    Screen.TodoCalendar -> 2
    Screen.TodoInsights -> 3
    Screen.Config -> 4
}

private fun Screen.todoPage(): TodoPage? = when (this) {
    Screen.TodoTasks -> TodoPage.Tasks
    Screen.TodoCalendar -> TodoPage.Calendar
    Screen.TodoInsights -> TodoPage.Insights
    else -> null
}

private fun TodoPage.toScreen(): Screen = when (this) {
    TodoPage.Tasks -> Screen.TodoTasks
    TodoPage.Calendar -> Screen.TodoCalendar
    TodoPage.Insights -> Screen.TodoInsights
}

enum class HomeMode { Day, Week }
internal fun homeModeAtFraction(fraction: Float): HomeMode =
    HomeMode.entries[((fraction.coerceIn(0f, 1f) * HomeMode.entries.size).toInt())
        .coerceIn(HomeMode.entries.indices)]
internal fun HomeStartMode.toHomeMode(): HomeMode = when (this) {
    HomeStartMode.DAY,
    HomeStartMode.TWO_DAY -> HomeMode.Day
    HomeStartMode.WEEK -> HomeMode.Week
}
enum class SettingsSection { Schedule, Notifications }
enum class SettingsPage { Root, General, LiquidGlass, Widgets, AiImport, DayAgent, Schedule, AutoRefreshSchedule, Notifications, ScheduleManager, BackupRestore, BackupPreview, About, Changelog, PrivacyPolicy }

/** Matches the navigation motion used by the bundled Miuix system-style navigator. */
private class MiuixSettingsNavigationEasing(
    response: Float = 0.8f,
    damping: Float = 0.95f
) : Easing {
    private val decay: Float
    private val frequency: Float
    private val phase: Float

    init {
        val omega = 2.0 * PI / response
        val stiffness = omega * omega
        val friction = damping * 4.0 * PI / response
        frequency = (sqrt(4.0 * stiffness - friction * friction) / 2.0).toFloat()
        decay = (-friction / 2.0).toFloat()
        phase = decay / frequency
    }

    override fun transform(fraction: Float): Float {
        val time = fraction.toDouble()
        val attenuation = exp(decay * time)
        return (attenuation * (-cos(frequency * time) + phase * sin(frequency * time)) + 1.0).toFloat()
    }
}

private val MiuixSettingsNavigationMotion = MiuixSettingsNavigationEasing()

/** Follow-through state for the three-dot button after a morph surface hands off on close. */
private data class SourceButtonFollowThrough(
    val startCenter: Offset,
    val startScale: Float
)

private fun agentSettingsPage(value: String?): SettingsPage? = when (value) {
    "GENERAL" -> SettingsPage.General
    "LIQUID_GLASS" -> SettingsPage.LiquidGlass
    "WIDGETS" -> SettingsPage.Widgets
    "AI_IMPORT" -> SettingsPage.AiImport
    "DAY_AGENT" -> SettingsPage.DayAgent
    "SCHEDULE" -> SettingsPage.Schedule
    "AUTO_REFRESH_SCHEDULE" -> SettingsPage.AutoRefreshSchedule
    "NOTIFICATIONS" -> SettingsPage.Notifications
    "SCHEDULE_MANAGER" -> SettingsPage.ScheduleManager
    "BACKUP_RESTORE" -> SettingsPage.BackupRestore
    // ABOUT / CHANGELOG / DOWNLOAD are separate entries in the settings root but all land on the
    // same "关于应用" screen, so the agent protocol keeps three names and maps them to one page.
    "ABOUT" -> SettingsPage.Changelog
    "CHANGELOG" -> SettingsPage.Changelog
    "DOWNLOAD" -> SettingsPage.Changelog
    "PRIVACY_POLICY" -> SettingsPage.PrivacyPolicy
    else -> null
}

internal const val SettingsDetailPageExtra = "settings_page"
private const val BackupPreviewUriExtra = "backup_preview_uri"
private const val EduAdapterExtra = "edu_adapter"

private fun SettingsPage.title(): String = when (this) {
    SettingsPage.Root -> "设置"
    SettingsPage.General -> "通用设置"
    SettingsPage.LiquidGlass -> "液态玻璃"
    SettingsPage.Widgets -> "小组件设置"
    SettingsPage.AiImport -> "AI 设置"
    SettingsPage.DayAgent -> "AI助理"
    SettingsPage.Schedule -> "课表详细设置"
    SettingsPage.AutoRefreshSchedule -> "自动刷新课表"
    SettingsPage.Notifications -> "通知设置"
    SettingsPage.ScheduleManager -> "课表设置"
    SettingsPage.BackupRestore -> "备份与恢复"
    SettingsPage.BackupPreview -> "恢复预览"
    SettingsPage.About -> "关于应用"
    SettingsPage.Changelog -> "关于应用"
	SettingsPage.PrivacyPolicy -> "隐私政策"
}

internal fun SettingsPage.usesPersistentCenteredSettingsTitle(): Boolean = when (this) {
    SettingsPage.LiquidGlass,
    SettingsPage.Widgets,
    SettingsPage.AutoRefreshSchedule,
    SettingsPage.About,
	SettingsPage.Changelog,
	SettingsPage.PrivacyPolicy -> true
    else -> false
}

internal data class TabletSettingsNavigationState(
    val rootPage: SettingsPage = SettingsPage.General,
    val detailPages: List<SettingsPage> = emptyList()
) {
    val displayedPage: SettingsPage
        get() = detailPages.lastOrNull() ?: rootPage

    fun selectRoot(page: SettingsPage): TabletSettingsNavigationState = copy(
        rootPage = page.takeUnless { it == SettingsPage.Root } ?: SettingsPage.General,
        detailPages = emptyList()
    )

    fun pushDetail(page: SettingsPage): TabletSettingsNavigationState =
        if (page == displayedPage) this else copy(detailPages = detailPages + page)

    fun popDetail(): TabletSettingsNavigationState =
        if (detailPages.isEmpty()) this else copy(detailPages = detailPages.dropLast(1))
}

private val TabletSettingsNavigationStateSaver = listSaver<TabletSettingsNavigationState, String>(
    save = { state -> listOf(state.rootPage.name) + state.detailPages.map(SettingsPage::name) },
    restore = { names ->
        val root = names.firstOrNull()
            ?.let { runCatching { SettingsPage.valueOf(it) }.getOrNull() }
            ?.takeUnless { it == SettingsPage.Root }
            ?: SettingsPage.General
        val details = names.drop(1).mapNotNull { name ->
            runCatching { SettingsPage.valueOf(name) }.getOrNull()
        }
        TabletSettingsNavigationState(rootPage = root, detailPages = details)
    }
)

internal val DayDockScrollPadding = 104.dp
internal val WeekDockScrollPadding = 132.dp
internal val DockScrollPadding = 132.dp
internal val HomeHeaderGlassBlur = 2.dp
internal val HomeHeaderGlassLensHeight = 12.dp
internal val HomeHeaderGlassLensAmount = 24.dp
internal val HomeLightGlassSurfaceColor = ComposeColor(0xFFF2F4F8)
internal val HomeLightGlassGradientColor = ComposeColor(0xFFF7F8FB)
internal val HomeLightGlassAccentColor = ComposeColor(0xFF0A84FF)
internal val HomeLightGlassSelectedAccentColor = ComposeColor(0xFF006FD6)
internal fun dockSelectedAccentColor(lightGlass: Boolean): ComposeColor =
    if (lightGlass) HomeLightGlassSelectedAccentColor else ComposeColor(0xFF0091FF)
internal const val HomeLightGlassChromeTintAlpha = 0.54f
internal const val HomeLightGlassAccentTintAlpha = 0.24f
internal const val HomeLightGlassPanelTintAlpha = 0.15f
internal const val HomeLightGlassMenuTintAlpha = 0.14f
internal fun homeChromeGlassSurfaceAlpha(lightGlass: Boolean): Float =
    if (lightGlass) HomeLightGlassChromeTintAlpha else 0.45f
internal fun dockImeCompensationPx(imeBottomPx: Int, systemBottomPx: Int): Int =
    (imeBottomPx - systemBottomPx).coerceAtLeast(0)
internal const val HomeHeaderGlassHighlightAlpha = 0.09f
internal const val HomeHeaderGlassShadowAlpha = 0.05f
internal const val HomeHeaderGlassOuterShadowAlpha = 0.018f
internal const val HomeHeaderGlassInnerShadowAlpha = 0.08f
internal fun homeChromeBlur(base: Dp, config: ScheduleConfigEntity): Dp =
    base * normalizedHomeChromeBlurScale(config.homeChromeBlurScale)
internal fun homeHeaderGlassTokens(lightGlass: Boolean, blurScale: Float = DefaultHomeChromeBlurScale): GlassTokens {
    val base = GlassTokens.pill(intensity = 0.95f).copy(
        surfaceAlpha = homeChromeGlassSurfaceAlpha(lightGlass)
    )
    return base.copy(blur = base.blur * normalizedHomeChromeBlurScale(blurScale))
}

sealed interface HomeDialog {
    data object ImportSchedule : HomeDialog
    data object EduImport : HomeDialog
    data class ConfirmImport(val draft: ImportDraft, val returnDialog: HomeDialog? = ImportSchedule) : HomeDialog
    data class EditWallpaper(val uri: Uri, val entrySnapshot: Bitmap?) : HomeDialog
    data class EditCourse(val course: CourseEntity?, val targetWeek: Int? = null) : HomeDialog
    data class ApplyCourseEdit(val original: CourseEntity, val edited: CourseEntity, val targetWeek: Int) : HomeDialog
    data class ConfirmCourseConflicts(
        val original: CourseEntity,
        val edited: CourseEntity,
        val targetWeek: Int,
        val conflictWeeks: List<Int>,
        val singleWeekOnly: Boolean = false
    ) : HomeDialog
    data class ApplyCourseDelete(val course: CourseEntity, val targetWeek: Int) : HomeDialog
}

private data class PendingCourseGroupEdit(
    val originals: List<CourseEntity>,
    val edited: List<CourseEntity>
)

private fun composeDetailMorphSnapshot(underlay: Bitmap, popup: Bitmap): Bitmap? = runCatching {
    val width = underlay.width
    val height = underlay.height
    require(width > 0 && height > 0)
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { result ->
        val target = android.graphics.Rect(0, 0, width, height)
        AndroidCanvas(result).apply {
            drawBitmap(underlay, null, target, null)
            drawBitmap(popup, null, target, null)
        }
    }
}.getOrNull()

private data class DetailMorphWindowSnapshot(
    val bitmap: Bitmap,
    val originInWindow: Offset
)

/**
 * Captures the final pixels of the special schedule-picker scene.
 *
 * That scene is assembled from a nested PickerScene producer, bitmap-backed schedule cards and a
 * root Miuix popup-host sibling. Recording an ancestor GraphicsLayer is still useful as a fallback,
 * but it is not guaranteed to flatten every nested RenderNode on ColorOS. PixelCopy reads the
 * already-composited source window once, before the destination Activity exists.
 */
private suspend fun captureDetailMorphWindowSnapshot(
    activity: Activity,
    rootPositionInWindow: Offset,
    rootSize: IntSize
): DetailMorphWindowSnapshot? {
    val decor = activity.window.decorView
    if (decor.width <= 0 || decor.height <= 0 || rootSize.width <= 0 || rootSize.height <= 0) {
        return null
    }
    val left = rootPositionInWindow.x.roundToInt().coerceIn(0, decor.width - 1)
    val top = rootPositionInWindow.y.roundToInt().coerceIn(0, decor.height - 1)
    val right = (rootPositionInWindow.x + rootSize.width)
        .roundToInt()
        .coerceIn(left + 1, decor.width)
    val bottom = (rootPositionInWindow.y + rootSize.height)
        .roundToInt()
        .coerceIn(top + 1, decor.height)
    val sourceRect = android.graphics.Rect(left, top, right, bottom)
    val bitmap = Bitmap.createBitmap(
        sourceRect.width(),
        sourceRect.height(),
        Bitmap.Config.ARGB_8888
    )
    return suspendCancellableCoroutine { continuation ->
        runCatching {
            PixelCopy.request(
                activity.window,
                sourceRect,
                bitmap,
                { result ->
                    if (!continuation.isActive) {
                        bitmap.recycle()
                    } else if (result == PixelCopy.SUCCESS) {
                        continuation.resume(
                            DetailMorphWindowSnapshot(
                                bitmap = bitmap,
                                originInWindow = Offset(left.toFloat(), top.toFloat())
                            )
                        )
                    } else {
                        bitmap.recycle()
                        continuation.resume(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }.onFailure {
            bitmap.recycle()
            if (continuation.isActive) continuation.resume(null)
        }
    }
}

internal val LocalEditingCourseId = compositionLocalOf<Long?> { null }
internal val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
@Volatile
internal var hideFromRecentsEnabled = false

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CourseScheduleAppUi(
    viewModel: ScheduleViewModel,
    todoViewModel: com.xiaomanjun.sleepdownschedule.feature.todo.TodoViewModel,
    todoEntryRequest: com.xiaomanjun.sleepdownschedule.feature.todo.TodoEntryRequest? = null,
    onTodoEntryRequestConsumed: (Long) -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onRequestAutoCalendarPermission: () -> Unit = {},
    onRequestShizukuPermission: () -> Unit = {},
    externalIcsUri: Uri? = null,
    onExternalIcsConsumed: (Uri) -> Unit = {},
    onStartupContentReady: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val todoItems by viewModel.todoItems.collectAsStateWithLifecycle()
    val allSchedulesState by viewModel.allSchedulesState.collectAsStateWithLifecycle()
    val message by viewModel.snackbar.collectAsStateWithLifecycle()
    val pickerState = rememberSchedulePickerState()
    var previewScheduleId by remember { mutableStateOf<Int?>(null) }
    var pendingPickerEditorScheduleId by remember { mutableStateOf<Int?>(null) }
    var quickScheduleDraft by remember { mutableStateOf<QuickScheduleDraft?>(null) }
    val dayAgentBackgroundMotionState = rememberDayAgentBackgroundMotionState()
    var dayAgentPagerSettled by remember { mutableStateOf(false) }
    var detailMorphState by remember { mutableStateOf<DetailMorphState>(DetailMorphState.Idle) }
    var detailMorphRequest by remember { mutableStateOf<DetailMorphRequest?>(null) }
    var detailCaptureCoverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detailCaptureRecordCleanFrame by remember { mutableStateOf(false) }
    val detailCaptureMaskActive = remember { AtomicBoolean(false) }
    val baseVisualState = previewScheduleId?.let(allSchedulesState::forSchedule) ?: state
    var personalizationDraftConfig by remember { mutableStateOf<ScheduleConfigEntity?>(null) }
    var personalizationPendingCommitConfig by remember { mutableStateOf<ScheduleConfigEntity?>(null) }
    var personalizationSliderPreviewKey by remember { mutableStateOf<String?>(null) }
    val personalizationPreviewState = remember { PersonalizationPreviewState() }
    val personalizationPreviewProgress by animateFloatAsState(
        targetValue = if (personalizationSliderPreviewKey != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = CubicBezierEasing(0.22f, 0f, 0.2f, 1f)),
        label = "personalization-preview-transition"
    )
    LaunchedEffect(baseVisualState.config, personalizationPendingCommitConfig) {
        val pending = personalizationPendingCommitConfig
        if (pending != null && baseVisualState.config == pending) {
            personalizationPendingCommitConfig = null
        }
    }
    LaunchedEffect(baseVisualState.config.id) {
        if (personalizationDraftConfig?.id != baseVisualState.config.id) {
            personalizationDraftConfig = null
        }
        if (personalizationPendingCommitConfig?.id != baseVisualState.config.id) {
            personalizationPendingCommitConfig = null
        }
    }
    val personalizationConfig = personalizationDraftConfig ?: personalizationPendingCommitConfig
    val pendingVisualState = baseVisualState.withPersonalizationConfig(personalizationConfig)
    val glassBackendPolicy = remember { GlassBackendPolicy.LargeGlass }
    val glassSceneState = rememberGlassSceneState(
        sceneId = "home",
        backendPolicy = glassBackendPolicy
    )
    val screenGraphicsLayer = rememberGraphicsLayer()
    // The week grid is already recorded into this layer for Morph motion. Reuse that GPU layer
    // as the personalization backdrop producer so the glass does not traverse every live
    // week-card RenderNode again on each animation frame.
    val cachedWeekHomeBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Content,
        providerId = "home-cached-week-scene",
        sceneState = glassSceneState,
        graphicsLayer = screenGraphicsLayer
    )
    val detailScreenGraphicsLayer = rememberGraphicsLayer()
    val detailPopupGraphicsLayer = rememberGraphicsLayer()
    var detailPopupCaptureActive by remember { mutableStateOf(false) }
    var detailPopupCaptureToken by remember { mutableIntStateOf(0) }
    val detailPopupCapturedToken = remember { AtomicInteger(-1) }
    val recordedScheduleId = remember { AtomicInteger(-1) }
    val recordedHomeGeneration = remember { AtomicLong(0L) }
    val lastRecordedHomeFrameKey = remember { AtomicReference<String?>(null) }
    val weekHomeSurfaceUsesOffscreenCache = remember { AtomicBoolean(false) }
    var captureRenderToken by remember { mutableIntStateOf(0) }
    var snapshotGeneration by remember { mutableIntStateOf(0) }
    var snapshotJob by remember { mutableStateOf<Job?>(null) }
    var cacheHydrationJob by remember { mutableStateOf<Job?>(null) }
    var entryPrewarmJob by remember { mutableStateOf<Job?>(null) }
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    var lastTodoPage by rememberSaveable { mutableStateOf(TodoPage.Tasks) }
    var todoReturnsToCourse by rememberSaveable { mutableStateOf(true) }
    val todoChromeActions = remember { com.xiaomanjun.sleepdownschedule.feature.todo.TodoChromeActions() }
    var localTodoRequestId by remember { mutableLongStateOf(0L) }
    var pendingLocalTodoRequest by rememberSaveable(stateSaver = TodoEntryRequestSaver) {
        mutableStateOf<TodoEntryRequest?>(null)
    }
    val activeTodoRequest = pendingLocalTodoRequest ?: todoEntryRequest
    fun consumeTodoRequest(id: Long) {
        if (pendingLocalTodoRequest?.id == id) pendingLocalTodoRequest = null
        else onTodoEntryRequestConsumed(id)
    }
    LaunchedEffect(activeTodoRequest?.id) {
        val request = activeTodoRequest ?: return@LaunchedEffect
        screen = request.destination.toScreen()
        todoReturnsToCourse = request.returnToCourse
        val handledInTodoContent = request.todoId != null || request.courseId != null ||
            request.startWithNewTask || request.sharedText.isNotBlank() || request.imageUris.isNotEmpty() ||
            !request.screenshotPath.isNullOrBlank() || request.requestShizukuPermission
        if (!handledInTodoContent) {
            consumeTodoRequest(request.id)
        }
    }
    // Commit the visible destination before another dock tap can cancel an effect.
    SideEffect { screen.todoPage()?.let { lastTodoPage = it } }
    BackHandler(enabled = screen.isTodoDestination() && todoReturnsToCourse) {
        screen = Screen.Home
        todoReturnsToCourse = true
    }
    var settingsExitInterceptionRequired by remember { mutableStateOf(false) }
    var settingsExitRequest by remember { mutableIntStateOf(0) }
    var pendingSettingsExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var homeMode by remember(state.loaded, state.config.defaultHomeMode) {
        mutableStateOf(state.config.defaultHomeMode.toHomeMode())
    }
    val rootPageMotion = rememberHomeSwitchMotion(screen !is Screen.Home, "home-settings")
    var previousDockIndex by remember { mutableIntStateOf(screen.dockIndex()) }
    val dockEntranceDirection = remember(screen) { if (screen.dockIndex() >= previousDockIndex) 1f else -1f }
    SideEffect { previousDockIndex = screen.dockIndex() }
    var lastSecondaryWasTodo by remember { mutableStateOf(screen.isTodoDestination()) }
    // Home is not a secondary destination: retain the real outgoing pane until its spring settles.
    val secondaryPageShowsTodo = if (screen is Screen.Home) lastSecondaryWasTodo else screen.isTodoDestination()
    SideEffect { if (screen !is Screen.Home) lastSecondaryWasTodo = screen.isTodoDestination() }
    var secondaryPageUsesHomeWallpaper by remember {
        mutableStateOf(screen.isTodoDestination())
    }
    SideEffect {
        when {
            screen.isTodoDestination() -> secondaryPageUsesHomeWallpaper = true
            screen !is Screen.Home -> secondaryPageUsesHomeWallpaper = false
            !rootPageMotion.moving -> secondaryPageUsesHomeWallpaper = false
        }
    }
    val homeModeMotion = key(state.loaded) {
        rememberHomeSwitchMotion(homeMode != HomeMode.Day, "day-week-month")
    }
    val rootPageStateHolder = rememberSaveableStateHolder()
    var homeDialog by remember { mutableStateOf<HomeDialog?>(null) }
    val aiHistorySelection by AiEduImportProgressSession.historySelection.collectAsStateWithLifecycle()
    val aiFinalImportRequest by AiEduImportProgressSession.finalImportRequest.collectAsStateWithLifecycle()
    LaunchedEffect(aiHistorySelection) {
        aiHistorySelection?.let { restored ->
            homeDialog = HomeDialog.ConfirmImport(restored, returnDialog = HomeDialog.ImportSchedule)
            AiEduImportProgressSession.consumeHistoryDraft()
        }
    }
    var renderedHomeDialog by remember { mutableStateOf<HomeDialog?>(null) }
    var homeDialogVisible by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()
    val homeAssistant = remember(appScope) { HomeAssistantState(appScope) }
    val conversationOpen by AgentConversationNavigator.pending.collectAsStateWithLifecycle()
    LaunchedEffect(conversationOpen, state.loaded, state.config.id) {
        val request = conversationOpen ?: return@LaunchedEffect
        if (!state.loaded) return@LaunchedEffect
        if (state.config.id != request.scheduleId) {
            if (state.schedules.any { it.id == request.scheduleId }) viewModel.activateSchedule(request.scheduleId)
            else AgentConversationNavigator.pending.value = null
            return@LaunchedEffect
        }
        screen = Screen.Home
        AgentConversationStore.select(viewModel.getApplication<android.app.Application>(), request.scheduleId, request.key)
        homeAssistant.openConversation(fullScreen = true, independentPage = true)
        AgentConversationNavigator.pending.value = null
    }
    var assistantImportUri by remember { mutableStateOf<Uri?>(null) }
    var assistantImportText by remember { mutableStateOf<String?>(null) }
    val assistantHaptic = LocalHapticFeedback.current
    var assistantHapticSent by remember { mutableStateOf(false) }

    var courseEditorRequest by remember { mutableStateOf<CourseEditorOverlayRequest?>(null) }
    var pendingCourseGroupEdit by remember { mutableStateOf<PendingCourseGroupEdit?>(null) }
    var pendingCourseGroupDelete by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
    var courseEditorRenderedCourseId by remember { mutableStateOf<Long?>(null) }
    val courseEditorMotionState = rememberCourseEditorMotionState()
    val courseEditorFlightRegistry = remember { CourseEditorFlightRegistry() }
    val courseEditorOverlayPhase = courseEditorMotionState.phase
    fun openCourseEditor(course: CourseEntity, targetWeek: Int?, sourceBounds: Rect?, copyDraft: CourseEntity? = null, contextMessage: String? = null) {
        if (courseEditorRequest != null) return
        val sourceGrid = targetWeek?.let(courseEditorFlightRegistry::grid)
        courseEditorFlightRegistry.frozen = true
        courseEditorRequest = CourseEditorOverlayRequest(
            course = course,
            targetWeek = targetWeek,
            sourceBoundsInRoot = sourceBounds,
            sourceIsDayCard = homeMode != HomeMode.Week && sourceBounds != null,
            copyDraft = copyDraft,
            sourceGrid = sourceGrid,
            contextMessage = contextMessage
        )
    }
    fun openAdjustedCourseEditor(courseId: Long, date: LocalDate, sourceBounds: Rect?) {
        val original = state.courses.firstOrNull { it.id == courseId } ?: return
        val adjustment = scheduleAdjustmentForDate(state.config, date)
        val originalDate = adjustment?.sourceDate?.let(LocalDate::parse) ?: date
        val originalWeek = adjustedTeachingWeekForDate(state.config, originalDate) ?: return
        val message = if (adjustment?.sourceDate != null) {
            "$date 补 $originalDate（第 $originalWeek 周）的课。这里编辑原课程，修改也会同步到对应补课安排。"
        } else if (adjustment != null) {
            "$date 已停课。这里编辑保留的原课程；当天是否上课由调休安排决定。"
        } else null
        openCourseEditor(original, originalWeek, sourceBounds, contextMessage = message)
    }
    fun closeCourseEditor() {
        courseEditorRequest = null
    }
    fun dismissHomeDialog() {
        homeDialogVisible = false
        homeDialog = null
        pendingCourseGroupEdit = null
        pendingCourseGroupDelete = emptyList()
    }
    var courseRemoval by remember { mutableStateOf<CourseRemovalMotion?>(null) }
    fun deleteHomeCourses(courses: List<CourseEntity>, week: Int?) {
        val removal = CourseRemovalMotion(courses, week)
        courseRemoval = removal
        val onFailure = { if (courseRemoval === removal) courseRemoval = null }
        if (week == null) viewModel.deleteCourses(courses, onFailure)
        else viewModel.deleteCoursesSingleWeek(courses, week, onFailure)
        dismissHomeDialog()
        closeCourseEditor()
    }
    LaunchedEffect(screen, homeMode, state.config.id) { courseRemoval = null }
    LaunchedEffect(homeDialog) {
        if (homeDialog != null) {
            renderedHomeDialog = homeDialog
            homeDialogVisible = true
        } else if (renderedHomeDialog != null) {
            homeDialogVisible = false
            delay(320)
            renderedHomeDialog = null
        }
    }
    val homeAnchoredMorphState = rememberHomeAnchoredMorphState()
    val courseShortcuts = remember(appScope) { CourseShortcutController(appScope) }
    val latestCopyState = rememberUpdatedState(state)
    val courseCopy = remember(appScope, viewModel) {
        CourseCopyController(
            scope = appScope,
            validate = { candidate ->
                val latest = latestCopyState.value
                when {
                    latest.config.id != candidate.scheduleId -> "课表已切换，请重新选择"
                    else -> conflictWeeksForAddedCourses(listOf(candidate), latest.courses, latest.periods)
                        .takeIf { it.isNotEmpty() }
                        ?.let { "课程冲突，请换个位置" }
                }
            },
            save = { candidate, result -> viewModel.copyCourses(listOf(candidate), result); Unit }
        )
    }
    val homeMenuDestinationMotionState = rememberHomeMenuDestinationMotionState()
    var homeMenuDestinationRequest by remember { mutableStateOf<HomeMenuDestinationRequest?>(null) }
    var homeMenuSourceHidden by remember { mutableStateOf(false) }
    var homeAddMenuBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var homeAnchoredOverlayRequest by remember { mutableStateOf<HomeAnchoredOverlayRequest?>(null) }
    var jumpWeekDialogMounted by remember { mutableStateOf(false) }
    var jumpWeekDialogVisible by remember { mutableStateOf(false) }
    var pendingJumpWeekDialog by remember { mutableStateOf(false) }
    var pendingOpenScheduleSettings by remember { mutableStateOf(false) }
    var homeMenuActivityLaunched by remember { mutableStateOf(false) }
    var destinationOwnsButtonReturn by remember { mutableStateOf(false) }
    var destinationCollapseHandedOff by remember { mutableStateOf(false) }
    var addButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var personalizeButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var sourceButtonFollowThrough by remember { mutableStateOf<SourceButtonFollowThrough?>(null) }
    val sourceButtonFollowProgress = remember { Animatable(0f) }
    val latestSourceFollowThrough = rememberUpdatedState<(Rect) -> Unit> { rect ->
        if (sourceButtonFollowThrough == null) {
            val rest = addButtonBounds ?: return@rememberUpdatedState
            sourceButtonFollowThrough = SourceButtonFollowThrough(
                startCenter = rect.center,
                startScale = (rect.width / rest.width.coerceAtLeast(1f)).coerceIn(0.30f, 1.05f)
            )
        }
    }
    var pendingHomeAnchoredOverlay by remember { mutableStateOf<HomeAnchoredOverlayKind?>(null) }
    var pendingHomeAnchoredSourceScale by remember { mutableFloatStateOf(1f) }
    var showScheduleEntryPill by remember { mutableStateOf(false) }
    val landingCourse = courseEditorMotionState.closingCourseOverride
    val editingCourseId: Long? = if (landingCourse != null) {
        state.courses.firstOrNull {
            it.copy(id = 0, weeks = landingCourse.weeks, weekParity = landingCourse.weekParity) == landingCourse.copy(id = 0)
        }?.id
    } else courseEditorRequest?.course?.id ?: courseEditorRenderedCourseId
    val activeHomeAnchoredOverlay =
        homeAnchoredOverlayRequest?.kind ?: homeAnchoredMorphState.renderedKind
    val destinationTransitionActive =
        homeMenuDestinationRequest != null ||
            homeMenuDestinationMotionState.phase != HomeAnchoredOverlayPhase.Idle
    val addButtonHidden = activeHomeAnchoredOverlay == HomeAnchoredOverlayKind.Add ||
        sourceButtonFollowThrough != null ||
        homeMenuActivityLaunched ||
        (destinationTransitionActive && !destinationCollapseHandedOff)

    val personalizationPreviewActive by remember {
        derivedStateOf {
            personalizationSliderPreviewKey != null || personalizationPreviewProgress > 0.001f
        }
    }
    val homeBackgroundOverlayActive =
        homeAnchoredOverlayRequest != null || homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle ||
            destinationTransitionActive || courseEditorRequest != null ||
            courseEditorOverlayPhase != CourseEditorOverlayPhase.Idle || courseShortcuts.request != null ||
            homeAssistant.stage == HomeAssistantStage.Conversation
    val homeBackgroundFreezeActive = !rootPageMotion.moving && !homeModeMotion.moving && shouldUseFrozenHomeMorphBlur(
        screenIsHome = screen is Screen.Home,
        previewActive = personalizationPreviewActive,
        overlayActive = homeBackgroundOverlayActive
    )
    // Retain data inputs as well as the drawn scene. Foreground editing still uses the live
    // ViewModel state. A copied course must enter layout before its existing landing animation.
    val retainedHomeState = remember(baseVisualState.config.id) { RetainedHomeValue(baseVisualState) }
    val retainedVisualState = retainedHomeState.update(
        baseVisualState,
        frozen = homeBackgroundFreezeActive && landingCourse == null
    ).withPersonalizationConfig(personalizationConfig)
    val removal = courseRemoval
    val visualState = remember(retainedVisualState, removal) {
        if (removal == null) retainedVisualState
        else retainedVisualState.copy(courses = removal.retainIn(retainedVisualState.courses))
    }
    val removalReady = removal != null && removal.appliedTo(state.courses) &&
        !homeBackgroundFreezeActive && renderedHomeDialog == null && editingCourseId == null
    LaunchedEffect(removal, removalReady) {
        if (removal != null && removalReady) {
            removal.progress.animateTo(1f, tween(CourseRemovalDurationMillis, easing = LinearEasing))
            if (courseRemoval === removal) courseRemoval = null
        }
    }
    val retainedAgentState = remember(state.config.id) { RetainedHomeValue(state) }
    val agentVisualState = retainedAgentState.update(state, frozen = homeBackgroundFreezeActive)
    val homeBackgroundSession = remember(homeBackgroundFreezeActive) { Any() }

    fun openHomeAnchoredOverlay(kind: HomeAnchoredOverlayKind, sourcePressedScale: Float = 1f) {
        if (homeAnchoredOverlayRequest != null ||
            homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle
        ) return
        val bounds = when (kind) {
            HomeAnchoredOverlayKind.Add -> addButtonBounds
            HomeAnchoredOverlayKind.Personalize -> personalizeButtonBounds
        }
        if (bounds == null || bounds.width <= 2f || bounds.height <= 2f) {
            pendingHomeAnchoredOverlay = kind
            pendingHomeAnchoredSourceScale = sourcePressedScale
            return
        }
        pendingHomeAnchoredOverlay = null
        homeAnchoredOverlayRequest = HomeAnchoredOverlayRequest(kind, bounds, sourcePressedScale)
    }

    fun toggleHomeAnchoredOverlay(kind: HomeAnchoredOverlayKind, sourcePressedScale: Float = 1f) {
        if (homeAnchoredOverlayRequest?.kind == kind) {
            homeAnchoredOverlayRequest = null
        } else {
            openHomeAnchoredOverlay(kind, sourcePressedScale)
        }
    }

    val windowContainerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val homeAdaptiveMetrics = rememberHomeAdaptiveMetrics()
    val homeDeviceCornerPx = com.xiaomanjun.sleepdownschedule.core.ui.interaction.deviceScreenCornerRadiusPx()

    LaunchedEffect(
        pendingHomeAnchoredOverlay,
        addButtonBounds,
        personalizeButtonBounds,
        homeAnchoredMorphState.phase,
        screen
    ) {
        val pending = pendingHomeAnchoredOverlay ?: return@LaunchedEffect
        if (screen !is Screen.Home || homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle) {
            return@LaunchedEffect
        }
        openHomeAnchoredOverlay(pending, pendingHomeAnchoredSourceScale)
    }
    LaunchedEffect(screen) {
        if (screen !is Screen.Home) {
            pendingHomeAnchoredOverlay = null
            homeAnchoredOverlayRequest = null
            personalizationSliderPreviewKey = null
            personalizationDraftConfig = null
            personalizationPreviewState.clearAll()
        }
        if (screen !is Screen.Config) {
            settingsExitInterceptionRequired = false
            pendingSettingsExitAction = null
        }
    }
    LaunchedEffect(homeAnchoredOverlayRequest) {
        if (homeAnchoredOverlayRequest?.kind != HomeAnchoredOverlayKind.Personalize) {
            personalizationSliderPreviewKey = null
            personalizationDraftConfig = null
            personalizationPreviewState.clearAll()
        }
    }
    var homeContentUnderTopBar by remember { mutableStateOf(false) }
    val adaptiveWeekCardHeight = remember(
        windowContainerSize.height,
        density.density,
        density.fontScale,
        homeAdaptiveMetrics.weekTopSpacerHeight,
        homeAdaptiveMetrics.safeBottom,
        visualState.periods.size
    ) {
        adaptiveWeekRowHeightDp(
            viewportHeightDp = windowContainerSize.height / density.density.coerceAtLeast(0.001f),
            topSpacerDp = homeAdaptiveMetrics.weekTopSpacerHeight.value,
            periodCount = visualState.periods.size,
            bottomInsetDp = homeAdaptiveMetrics.safeBottom.value + 8f,
            fontScale = density.fontScale
        )
    }
    val weekCardHeightScaleFloor = remember(adaptiveWeekCardHeight, density.fontScale) {
        minimumWeekCardHeightScale(
            adaptiveRowHeightDp = adaptiveWeekCardHeight,
            fontScale = density.fontScale
        )
    }
    var weekCardHeightScale by remember(
        visualState.config.id,
        visualState.config.weekCardHeightScale,
        weekCardHeightScaleFloor
    ) {
        mutableFloatStateOf(
            normalizedWeekCardHeightScale(
                visualState.config.weekCardHeightScale,
                weekCardHeightScaleFloor
            )
        )
    }
    val weekCardHeight = adaptiveWeekCardHeight * weekCardHeightScale
    val context = LocalContext.current
    val dayViewMode = remember(screen, state.config.defaultHomeMode, context) {
        DayViewPreferences.mode(
            context = context,
            legacyTwoDay = state.config.defaultHomeMode == HomeStartMode.TWO_DAY
        )
    }
    var weekViewStyle by remember(screen, context) {
        mutableStateOf(WeekViewPreferences.style(context))
    }
    val remoteExperience by SleepDownRemoteConfig.experience.collectAsStateWithLifecycle()
    fun requestSettingsExit(action: () -> Unit) {
        if (screen is Screen.Config && settingsExitInterceptionRequired) {
            pendingSettingsExitAction = action
            settingsExitRequest++
        } else {
            action()
        }
    }
    BackHandler(enabled = screen is Screen.Config && settingsExitInterceptionRequired) {
        requestSettingsExit { context.findActivity()?.finish() }
    }
    var pendingImportedSetupId by remember(context) {
        mutableStateOf(PendingImportSetupStore.consume(context))
    }
    // Daily "a holiday is coming" prompt. It asks at most once a day, stays quiet for holidays the
    // user already planned, and never competes with the agreement/notice surfaces.
    var upcomingHoliday by remember { mutableStateOf<UpcomingHoliday?>(null) }
    val latestHomeConfig by rememberUpdatedState(state.config)
    val latestHomePeriods by rememberUpdatedState(state.periods)
    val holidayAdjustmentsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(ScheduleAdjustmentsActivity.ArrangementsExtra)?.let { value ->
                val scheduleId = result.data?.getIntExtra(ScheduleAdjustmentsActivity.ScheduleIdExtra, -1) ?: -1
                val original = result.data?.getStringExtra(ScheduleAdjustmentsActivity.OriginalArrangementsExtra)
                if (scheduleId > 0 && original != null) viewModel.saveScheduleAdjustments(scheduleId, original, value)
            }
        }
    }
    LaunchedEffect(remoteExperience, state.loaded, state.config.id, state.config.scheduleAdjustmentsJson) {
        if (!state.loaded) return@LaunchedEffect
        if (remoteExperience.agreement != null || remoteExperience.notice != null) return@LaunchedEffect
        val holiday = HolidayReminder.upcomingHoliday(context, state.config) ?: return@LaunchedEffect
        if (HolidayReminder.claimDailyPrompt(context)) upcomingHoliday = holiday
    }
    LaunchedEffect(aiFinalImportRequest) {
        aiFinalImportRequest?.let { request ->
            viewModel.importDraft(request.draft, request.createNewSchedule) {
                dismissHomeDialog()
                screen = Screen.Home
                pendingImportedSetupId = null
            }
            AiEduImportProgressSession.consumeFinalImportRequest()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(externalIcsUri, state.loaded) {
        val uri = externalIcsUri ?: return@LaunchedEffect
        if (!state.loaded) return@LaunchedEffect
        onExternalIcsConsumed(uri)
        screen = Screen.Home
        pendingHomeAnchoredOverlay = null
        homeAnchoredOverlayRequest = null
        loadAiImportFile(context, uri)
            .onSuccess { file ->
                if (!file.isIcs) {
                    Toast.makeText(context, "分享的文件不是有效的 ICS 日历", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                IcsScheduleCodec.parse(file.bytes, state.config)
                    .onSuccess { draft ->
                        homeDialog = HomeDialog.ConfirmImport(draft, returnDialog = null)
                    }
                    .onFailure { error ->
                        Toast.makeText(context, error.message ?: "ICS 解析失败", Toast.LENGTH_SHORT).show()
                    }
            }
            .onFailure { error ->
                Toast.makeText(context, error.message ?: "ICS 文件读取失败", Toast.LENGTH_SHORT).show()
            }
    }
    val backgroundBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Background,
        providerId = "home-background",
        sceneState = glassSceneState
    )
    val contentBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Content,
        providerId = "home-content",
        sceneState = glassSceneState
    )
    val pickerSceneBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.PickerScene,
        providerId = "home-picker-scene",
        sceneState = glassSceneState
    )
    val centeredDialogSceneBackdrop = rememberCenteredDialogSceneBackdrop(
        providerId = "home-centered-dialog-scene"
    )
    val chromeBackdrop = rememberGlassCombinedBackdrop(backgroundBackdrop, contentBackdrop)
    remember(glassSceneState) {
        glassSceneState.requireValidTopology(
            nodes = listOf(
                GlassTopologyNode("home-background", GlassBackdropDomain.Background, GlassTopologyNodeRole.Producer),
                GlassTopologyNode("home-content", GlassBackdropDomain.Content, GlassTopologyNodeRole.Producer),
                GlassTopologyNode("home-chrome", GlassBackdropDomain.ChromeCombined, GlassTopologyNodeRole.Consumer),
                GlassTopologyNode("home-picker", GlassBackdropDomain.PickerScene, GlassTopologyNodeRole.Producer),
                GlassTopologyNode("home-dialog", GlassBackdropDomain.DialogBridge, GlassTopologyNodeRole.Consumer),
                GlassTopologyNode("home-cached-week", GlassBackdropDomain.Content, GlassTopologyNodeRole.Producer),
                GlassTopologyNode(
                    "home-personalization",
                    GlassBackdropDomain.ChromeCombined,
                    GlassTopologyNodeRole.Consumer
                )
            ),
            links = listOf(
                GlassSamplingLink("home-background", "home-chrome"),
                GlassSamplingLink("home-content", "home-chrome"),
                GlassSamplingLink("home-background", "home-dialog"),
                GlassSamplingLink("home-content", "home-dialog"),
                GlassSamplingLink("home-cached-week", "home-personalization")
            )
        )
    }
    var homeReadabilityRootSize by remember { mutableStateOf(IntSize.Zero) }
    var homeRootPositionOnScreen by remember { mutableStateOf(Offset.Zero) }
    var homeRootPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    fun currentHomeMenuDestinationRequest(
        kind: HomeMenuDestinationKind
    ): HomeMenuDestinationRequest? {
        val sourceButton = addButtonBounds ?: return null
        if (homeReadabilityRootSize.width <= 0 || homeReadabilityRootSize.height <= 0) return null
        val menuBounds = homeAddMenuBoundsInRoot ?: homeAddMenuTargetRect(
            source = sourceButton,
            rootSize = homeReadabilityRootSize,
            density = density.density,
            actionCount = 6,
            adaptiveMetrics = homeAdaptiveMetrics
        )
        return HomeMenuDestinationRequest(
            kind = kind,
            sourceBoundsInRoot = menuBounds,
            collapseBoundsInRoot = sourceButton
        )
    }
    fun openHomeMenuDestination(kind: HomeMenuDestinationKind) {
        val request = currentHomeMenuDestinationRequest(kind) ?: return
        destinationOwnsButtonReturn = false
        destinationCollapseHandedOff = false
        homeMenuDestinationRequest = request
    }
    val wallpaperImages by rememberHomeWallpaperImages(visualState.config)
    val homeWallpaperRecordKey = remember { mutableStateOf<Any?>(null) }
    val updateHomeWallpaperRecordKey: (Any?) -> Unit = remember {
        { homeWallpaperRecordKey.value = it }
    }
    val todoWallpaperRecordKey = remember { mutableStateOf<Any?>(null) }
    val updateTodoWallpaperRecordKey: (Any?) -> Unit = remember {
        { todoWallpaperRecordKey.value = it }
    }
    val homeReadabilityContext = remember(
        wallpaperImages.readabilityBitmap,
        visualState.config,
        homeReadabilityRootSize,
        homeRootPositionInWindow
    ) {
        HomeReadabilityContext(
            bitmap = wallpaperImages.readabilityBitmap,
            config = visualState.config,
            rootSize = homeReadabilityRootSize,
            rootOffsetInWindow = homeRootPositionInWindow
        )
    }
    val appDarkTheme = appUsesDarkTheme(visualState.config)
    // Todos, calendar and insights share the wallpaper and course-card appearance used by Home.
    // Settings continues to use settingsVisualConfig only inside its own neutral page stack.
    val todoVisualConfig = visualState.config
    val todoAdaptiveGlassState = rememberFallbackAdaptiveGlassState(todoVisualConfig)
    val hasUserWallpaper = !visualState.config.wallpaperUri.isNullOrBlank()
    val statusBarWallpaperLuminance = remember(homeReadabilityContext) {
        homeStatusBarWallpaperLuminance(homeReadabilityContext)
    }
    var homeStatusBarDarkIcons by remember(hasUserWallpaper, appDarkTheme) {
        mutableStateOf(!hasUserWallpaper || !appDarkTheme)
    }
    LaunchedEffect(statusBarWallpaperLuminance, appDarkTheme, hasUserWallpaper) {
        homeStatusBarDarkIcons = if (!hasUserWallpaper) {
            true
        } else {
            statusBarWallpaperLuminance?.let { luminance ->
                // A small dead band prevents icon polarity from flickering while brightness is dragged.
                if (homeStatusBarDarkIcons) luminance >= 0.46f else luminance >= 0.56f
            } ?: !appDarkTheme
        }
    }
    LaunchedEffect(screen, homeStatusBarDarkIcons, appDarkTheme, context, homeAssistant.visible) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        window.setStatusBarDarkIcons(
            darkIcons = if (screen is Screen.Home && homeAssistant.visible) false
                else if (screen is Screen.Home) homeStatusBarDarkIcons else !appDarkTheme
        )
    }
    val expectedWallpaperRenderKey = homeWallpaperRenderKey(
        visualState.config,
        appUsesDarkTheme(visualState.config)
    )
    val wallpaperLoadFinished = !visualState.config.hasAnyWallpaper() ||
        wallpaperImages.renderKey == expectedWallpaperRenderKey
    val noWallpaperResolved = wallpaperImages.renderKey == expectedWallpaperRenderKey &&
        wallpaperImages.source == null
    LaunchedEffect(wallpaperImages.renderKey, expectedWallpaperRenderKey) {
        if (wallpaperImages.renderKey == expectedWallpaperRenderKey) {
            // Keep the cheap GPU blur preview visible until the one final CPU-cached bitmap is
            // ready, then hand over without exposing the previously committed blur for a frame.
            personalizationPreviewState.clear(PersonalizeWallpaperBlurSlider)
        }
    }
    val latestOnStartupContentReady = rememberUpdatedState(onStartupContentReady)
    var startupContentReported by remember { mutableStateOf(false) }
    val latestVisualState = rememberUpdatedState(visualState)
    val latestWallpaperImages = rememberUpdatedState(wallpaperImages)
    val latestAllSchedulesState = rememberUpdatedState(allSchedulesState)
    val startupPhase = StartupPhase.FullQuality
    LaunchedEffect(state.loaded, wallpaperLoadFinished) {
        if (!state.loaded || !wallpaperLoadFinished) return@LaunchedEffect
        // Release the first-draw gate only after the wallpaper and stationary home share a frame.
        withFrameNanos { }
        if (!startupContentReported) {
            startupContentReported = true
            latestOnStartupContentReady.value()
        }
    }
    val glassQuality = animatedGlassQuality(startupPhase)
    val adaptiveGlassState = rememberFallbackAdaptiveGlassState(visualState.config)
    val homeAnchoredKindLabel = when (homeAnchoredMorphState.renderedKind) {
        HomeAnchoredOverlayKind.Add -> "AddMenu"
        HomeAnchoredOverlayKind.Personalize -> "Personalize"
        null -> "Unknown"
    }
    val homeMenuDestinationKindLabel = when (homeMenuDestinationMotionState.kind) {
        HomeMenuDestinationKind.AddCourse -> "AddCourse"
        HomeMenuDestinationKind.ManualImport -> "ManualImport"
        HomeMenuDestinationKind.EduImport -> "EduImport"
        null -> "Unknown"
    }
    val startupAnimation = when {
        personalizationSliderPreviewKey != null -> "PersonalizeSliderDrag"
        homeMenuDestinationMotionState.phase != HomeAnchoredOverlayPhase.Idle -> when (homeMenuDestinationMotionState.phase) {
            HomeAnchoredOverlayPhase.Preparing -> "HomeMenuDestinationPrepare:$homeMenuDestinationKindLabel"
            HomeAnchoredOverlayPhase.Opening -> "HomeMenuDestinationOpen:$homeMenuDestinationKindLabel"
            HomeAnchoredOverlayPhase.Closing,
            HomeAnchoredOverlayPhase.Disposing -> "HomeMenuDestinationClose:$homeMenuDestinationKindLabel"
            HomeAnchoredOverlayPhase.Open -> "Idle"
            HomeAnchoredOverlayPhase.Idle -> "Idle"
        }
        homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle -> when (homeAnchoredMorphState.phase) {
            HomeAnchoredOverlayPhase.Preparing -> if (homeAnchoredKindLabel == "Personalize") {
                "PersonalizePrepare"
            } else "HomeAnchoredPrepare:$homeAnchoredKindLabel"
            HomeAnchoredOverlayPhase.Opening -> if (homeAnchoredKindLabel == "Personalize") {
                "PersonalizeOpen"
            } else "HomeAnchoredOpen:$homeAnchoredKindLabel"
            HomeAnchoredOverlayPhase.Closing,
            HomeAnchoredOverlayPhase.Disposing -> if (homeAnchoredKindLabel == "Personalize") {
                "PersonalizeClose"
            } else "HomeAnchoredClose:$homeAnchoredKindLabel"
            HomeAnchoredOverlayPhase.Open -> "Idle"
            HomeAnchoredOverlayPhase.Idle -> "Idle"
        }
        courseEditorOverlayPhase == CourseEditorOverlayPhase.Preparing -> "CourseEditorPrepare"
        courseEditorOverlayPhase == CourseEditorOverlayPhase.Opening -> "CourseEditorOpen"
        courseEditorOverlayPhase == CourseEditorOverlayPhase.Closing ||
            courseEditorOverlayPhase == CourseEditorOverlayPhase.Disposing -> "CourseEditorClose"
        homeDialogVisible -> "DialogOpen"
        else -> "Idle"
    }
    val glassRenderPhase = when {
        startupAnimation.contains("Prepare", ignoreCase = true) -> GlassRenderPhase.Preparing
        startupAnimation.contains("Close", ignoreCase = true) -> GlassRenderPhase.Closing
        startupAnimation != "Idle" -> GlassRenderPhase.Moving
        else -> GlassRenderPhase.Live
    }
    LaunchedEffect(glassRenderPhase) {
        glassSceneState.synchronizePhase(glassRenderPhase)
    }
    StartupPerformanceBoost(homeDialogVisible)
    PerformanceAnimationState(startupAnimation, startupAnimation != "Idle")
    GlassPerformanceDiagnostics(glassSceneState, startupAnimation)
    RefreshCadenceDiagnostics(
        enabled = BuildConfig.SLEEPDOWN_LARGE_GLASS_EXPERIMENT && startupAnimation != "Idle"
    )
    StartupJankStats(
        phase = startupPhase,
        screen = if (screen is Screen.Home) "Home" else if (screen is Screen.Config) "Settings" else "Other",
        animation = startupAnimation,
        personalizeMode = if (activeHomeAnchoredOverlay == HomeAnchoredOverlayKind.Personalize) {
            homeMode.name
        } else {
            "Idle"
        },
        personalizeSlider = when (personalizationSliderPreviewKey) {
            PersonalizeWallpaperBlurSlider -> "WallpaperBlur"
            PersonalizeWallpaperBrightnessSlider -> "WallpaperBrightness"
            PersonalizeWeekHeightSlider -> "WeekHeight"
            PersonalizeCardAlphaSlider -> "CardAlpha"
            PersonalizeCardBlurSlider -> "CardBlur"
            PersonalizeCardRefractionSlider -> "CardRefraction"
            PersonalizeCardFontSlider -> "CardFont"
            else -> "Idle"
        }
    )
    // Sample the window-origin recorded home scene so the morph shell and the wallpaper
    // behind it stay aligned (see homeAnchoredOverlayBackdrop).
    val courseEditorBackdrop = rememberScreenScaledBackdrop(
        backdrop = cachedWeekHomeBackdrop,
        scale = { courseEditorMotionState.backgroundZoom.value },
        rootPositionOnScreen = { homeRootPositionOnScreen },
        rootSize = { homeReadabilityRootSize }
    )
    // The week grid is recorded into screenGraphicsLayer at the window origin (the whole
    // readable root including the top bar). Sampling that same recorded scene keeps the
    // source-anchored overlays aligned in both day and week modes; the live chromeBackdrop
    // producers live below the top bar, so drawing them at the root origin shifts the
    // sampled wallpaper upward by the top-bar height.
    val homeAnchoredOverlayBackdrop = rememberScreenScaledBackdrop(
        backdrop = cachedWeekHomeBackdrop,
        scale = {
            val zoom = homeAnchoredMorphState.backgroundZoom.value
            1f + (zoom - 1f) * (1f - personalizationPreviewProgress)
        },
        rootPositionOnScreen = { homeRootPositionOnScreen },
        rootSize = { homeReadabilityRootSize }
    )
    // Android Dialog content owns a separate Compose root/window. Wrap each recorded layer in
    // screen coordinates before combining them, otherwise LayerBackdrop mixes the dialog and
    // activity window origins and samples with a decor/status-bar offset on ColorOS.
    val homeDialogBackdrop = rememberScreenScaledBackdrop(
        backdrop = cachedWeekHomeBackdrop,
        scale = { 1f },
        rootPositionOnScreen = { homeRootPositionOnScreen },
        rootSize = { homeReadabilityRootSize }
    ) ?: cachedWeekHomeBackdrop
    val homeMenuDestinationBackdrop = rememberScreenScaledBackdrop(
        backdrop = cachedWeekHomeBackdrop,
        scale = { homeMenuDestinationMotionState.backgroundZoom.value },
        rootPositionOnScreen = { homeRootPositionOnScreen },
        rootSize = { homeReadabilityRootSize }
    )
    val fullScreenMenuDestinationActive =
        homeMenuDestinationRequest?.kind == HomeMenuDestinationKind.EduImport ||
            (homeMenuDestinationMotionState.phase != HomeAnchoredOverlayPhase.Idle &&
                homeMenuDestinationMotionState.kind == HomeMenuDestinationKind.EduImport)
    val homeOverlayBackgroundZoom: () -> Float = {
        if (personalizationPreviewProgress > 0.001f) {
            val zoom = homeAnchoredMorphState.backgroundZoom.value
            1f + (zoom - 1f) * (1f - personalizationPreviewProgress)
        } else if (fullScreenMenuDestinationActive) {
            homeMenuDestinationMotionState.backgroundZoom.value
        } else {
            maxOf(
                courseEditorMotionState.backgroundZoom.value,
                homeAnchoredMorphState.backgroundZoom.value,
                homeMenuDestinationMotionState.backgroundZoom.value
            )
        }
    }
    val dayAgentBackdrop = rememberScreenScaledBackdrop(
        // TodayAgentHost is inside screenGraphicsLayer: sampling that layer here would
        // make its RenderNode graph recursive. The wallpaper producer excludes the agent.
        backdrop = backgroundBackdrop,
        scale = { dayAgentBackgroundMotionState.backgroundZoom.value },
        rootPositionOnScreen = { homeRootPositionOnScreen },
        rootSize = { homeReadabilityRootSize }
    )
    val systemDark = isSystemInDarkTheme()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            appScope.launch {
                val entrySnapshot = runCatching {
                    captureRenderToken += 1
                    withFrameNanos { }
                    screenGraphicsLayer.toImageBitmap().asAndroidBitmap()
                }.getOrNull()
                homeDialog = HomeDialog.EditWallpaper(uri, entrySnapshot)
            }
        }
    }
    val todayDate = LocalDate.now()
    val homeCurrentWeek = effectiveCurrentWeek(visualState.config)
    val beforeScheduleTerm = isBeforeScheduleTerm(visualState.config, todayDate)
    val afterScheduleTerm = isAfterScheduleTerm(visualState.config, todayDate)
    val homeWeekHeaderPreview = remember(visualState.config.id) { mutableStateOf<Int?>(null) }
    var homeDisplayWeek by remember(visualState.config.id, visualState.loaded) {
        mutableIntStateOf(if (beforeScheduleTerm) 1 else homeCurrentWeek)
    }
    var pendingConflictCourseId by remember(visualState.config.id) { mutableStateOf<Long?>(null) }
    var pendingConflictCourseKey by remember(visualState.config.id) { mutableStateOf<String?>(null) }
    var pendingConflictWeeks by remember(visualState.config.id) { mutableStateOf<List<Int>>(emptyList()) }
    var homeDisplayDate by remember(visualState.config.id) { mutableStateOf(todayDate) }
    LaunchedEffect(
        state.loaded,
        visualState.config.id,
        pickerState.overlayVisible,
        homeMode,
        homeDisplayDate
    ) {
        // The Agent background is transformed live now, so there is no frozen snapshot to
        // invalidate when leaving the day page or opening the picker.
    }
    LaunchedEffect(visualState.loaded, visualState.config.id, visualState.config.totalWeeks, homeCurrentWeek, visualState.config.autoCurrentWeek, beforeScheduleTerm) {
        if (!visualState.loaded) return@LaunchedEffect
        val currentTargetWeek = if (beforeScheduleTerm) 1 else homeCurrentWeek
        homeDisplayWeek = homeDisplayWeek.coerceIn(1, visualState.config.totalWeeks.coerceAtLeast(1))
        if (visualState.config.autoCurrentWeek) homeDisplayWeek = currentTargetWeek
    }
    val homeTitleWeek = if (homeMode != HomeMode.Week) {
        effectiveCurrentWeek(visualState.config, homeDisplayDate)
    } else {
        homeDisplayWeek
    }
    val homeTitleDate = if (homeMode != HomeMode.Week) {
        homeDisplayDate
    } else {
        todayDate
    }
    val homeReturnTargetWeek = if (beforeScheduleTerm) 1 else homeCurrentWeek
    val homeShowingAnotherWeek = homeMode == HomeMode.Week && homeDisplayWeek != homeReturnTargetWeek
    val homeCourseColorSignature = remember(visualState.config.id, visualState.courses) {
        visualState.courses
            .map(::courseCardColorKey)
            .distinct()
            .sorted()
    }
    val homeCoursePalette = remember(
        visualState.config.courseCardColorMode,
        visualState.config.cardColorArgb,
        visualState.config.courseCardPalette,
        wallpaperImages.representativeColors
    ) {
        resolvedCourseCardPalette(visualState.config, wallpaperImages.representativeColors)
    }
    val homeCourseColorAssignments = remember(
        visualState.config.id,
        visualState.config.courseCardColorMode,
        visualState.config.cardColorArgb,
        visualState.config.courseCardPalette,
        homeCourseColorSignature,
        homeCoursePalette
    ) {
        // Card movement and resizing must not recolor the whole grid.
        // The semantic color-key set changes only when the actual course identities do.
        buildCourseCardColorAssignments(
            visualState.courses,
            homeCoursePalette,
            tonalFamily = visualState.config.courseCardColorMode == CourseCardColorMode.GRADIENT
        )
    }
    val homeCaptureFrameKey = remember(
        homeBackgroundSession,
        homeReadabilityRootSize,
        density.density,
        density.fontScale,
        captureRenderToken,
        visualState.config,
        visualState.courses,
        visualState.periods,
        wallpaperImages.renderKey,
        wallpaperImages.source,
        homeWallpaperRecordKey.value,
        homeCourseColorSignature,
        homeMode,
        homeDisplayWeek,
        homeDisplayDate,
        editingCourseId,
        activeHomeAnchoredOverlay,
        addButtonHidden,
        rootPageMotion.moving,
        homeModeMotion.moving
    ) {
        buildString {
            append(System.identityHashCode(homeBackgroundSession)).append('|')
            append(homeReadabilityRootSize).append('|').append(density.density).append('|')
                .append(density.fontScale).append('|')
            append(captureRenderToken).append('|')
            append(visualState.config.hashCode()).append('|')
            append(visualState.courses.hashCode()).append('|')
            append(visualState.periods.hashCode()).append('|')
            append(wallpaperImages.renderKey).append('|')
            append(wallpaperImages.source != null).append('|')
            // The drawable becomes recordable after its source bitmap is first presented.
            // Re-capture the home scene then; the bitmap selection may be unchanged.
            append(homeWallpaperRecordKey.value?.hashCode()).append('|')
            append(homeCourseColorSignature.hashCode()).append('|')
            append(homeMode).append('|').append(homeDisplayWeek).append('|').append(homeDisplayDate)
                .append('|').append(editingCourseId)
                .append('|').append(activeHomeAnchoredOverlay)
                .append('|').append(addButtonHidden)
                .append('|').append(rootPageMotion.moving).append('|').append(homeModeMotion.moving)
            // Menus are sibling consumers outside this recorder. Hiding the source menu during
            // handoff changes no Home pixels and must not recapture the scene during Opening.
        }
    }
    val useFrozenHomeMorphBlur: () -> Boolean = {
        homeBackgroundFreezeActive
    }
    val useCachedHomeSurface: () -> Boolean = {
        shouldReuseHomeSurface(
            screenIsHome = screen is Screen.Home,
            previewActive = personalizationPreviewActive,
            overlayActive = homeBackgroundOverlayActive,
            cachedScheduleId = recordedScheduleId.get(),
            currentScheduleId = visualState.config.id,
            cachedFrameKey = lastRecordedHomeFrameKey.get(),
            currentFrameKey = homeCaptureFrameKey
        )
    }
    val currentHomeCoordinatesFreeze = rememberUpdatedState(homeBackgroundFreezeActive)
    val currentHomeCaptureFrameKey = rememberUpdatedState(homeCaptureFrameKey)
    val homeGlassMotionKey = remember(rootPageMotion, homeModeMotion) {
        derivedStateOf {
            Triple(currentHomeCaptureFrameKey.value, rootPageMotion.sampleKey, homeModeMotion.sampleKey)
        }
    }
    // Observe page movement in glass draw nodes, even when their layout coordinate object is reused.
    val homeGlassSampleRecordKey = remember(homeGlassMotionKey) { { homeGlassMotionKey.value } }
    val freezeHomeGlassCoordinates = remember {
        {
            currentHomeCoordinatesFreeze.value &&
                lastRecordedHomeFrameKey.get() == currentHomeCaptureFrameKey.value
        }
    }
    val substantialHomeAnchoredCoverage =
        activeHomeAnchoredOverlay == HomeAnchoredOverlayKind.Personalize &&
            (
                homeAnchoredOverlayRequest?.kind == HomeAnchoredOverlayKind.Personalize ||
                    homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle
                )
    val substantialMenuDestinationCoverage =
        homeMenuDestinationRequest != null ||
            homeMenuDestinationMotionState.phase != HomeAnchoredOverlayPhase.Idle
    val substantialCourseEditorCoverage =
        courseEditorRequest != null || courseEditorOverlayPhase != CourseEditorOverlayPhase.Idle
    val homeBackgroundBlurClosing =
        homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Closing ||
            homeMenuDestinationMotionState.phase == HomeAnchoredOverlayPhase.Closing ||
            courseEditorOverlayPhase == CourseEditorOverlayPhase.Closing
    val homeOverlayBackgroundBlurProgress: () -> Float = {
        val legacyDepth = homeOverlayDepthProgress(homeOverlayBackgroundZoom())
        val previewActive = personalizationSliderPreviewKey != null ||
            personalizationPreviewProgress > 0.001f
        val substantialOverlayActive = substantialCourseEditorCoverage ||
            substantialMenuDestinationCoverage || substantialHomeAnchoredCoverage
        if (!shouldUseStagedHomeOverlayBlur(previewActive, substantialOverlayActive)) {
            legacyDepth
        } else when {
            substantialCourseEditorCoverage &&
                courseEditorOverlayPhase != CourseEditorOverlayPhase.Idle ->
                stagedHomeOverlayBlurProgress(
                    legacyDepthProgress = legacyDepth,
                    morphProgress = courseEditorMotionState.progress.value,
                    closing = courseEditorOverlayPhase == CourseEditorOverlayPhase.Closing
                )
            substantialMenuDestinationCoverage &&
                homeMenuDestinationMotionState.phase != HomeAnchoredOverlayPhase.Idle ->
                stagedHomeOverlayBlurProgress(
                    legacyDepthProgress = legacyDepth,
                    morphProgress = homeMenuDestinationMotionState.progress.value,
                    closing = homeMenuDestinationMotionState.phase == HomeAnchoredOverlayPhase.Closing
                )
            substantialHomeAnchoredCoverage &&
                homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle ->
                stagedHomeOverlayBlurProgress(
                    legacyDepthProgress = legacyDepth,
                    morphProgress = homeAnchoredMorphState.progress.value,
                    closing = homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Closing
                )
            else -> legacyDepth
        }
    }
    LaunchedEffect(screen, homeMode, visualState.config.id, homeDisplayWeek,
        homeAdaptiveMetrics.screenWidth, homeAdaptiveMetrics.screenHeight,
        homeAnchoredOverlayRequest, pickerState.overlayVisible) {
        courseShortcuts.reset()
    }
    LaunchedEffect(screen, homeMode, visualState.config.id, homeAnchoredOverlayRequest, pickerState.overlayVisible) {
        courseCopy.reset()
    }
    LaunchedEffect(homeDisplayWeek, homeAdaptiveMetrics.screenWidth, homeAdaptiveMetrics.screenHeight) {
        if (courseCopy.busy) courseCopy.reset() else courseCopy.clearSelection()
    }
    suspend fun awaitHomeBackgroundFrame(routeEligible: Boolean) {
        if (!routeEligible || !homeBackgroundFreezeActive) return
        // The existing preparation phase supplies the clean source-hidden frame. Keep every
        // course node mounted; there is no flat-material capture or post-close rebuild.
        repeat(3) {
            if (useCachedHomeSurface() && weekHomeSurfaceUsesOffscreenCache.get()) return
            withFrameNanos { }
        }
    }

    suspend fun awaitRenderedSchedule(scheduleId: Int): Boolean {
        val generationBeforeSwitch = recordedHomeGeneration.get()
        Log.d("SchedulePicker", "await start requested=$scheduleId recorded=${recordedScheduleId.get()} generation=$generationBeforeSwitch")
        previewScheduleId = scheduleId
        captureRenderToken += 1
        snapshotFlow { latestVisualState.value.config.id }.first { it == scheduleId }
        Log.d("SchedulePicker", "visual state ready requested=$scheduleId token=$captureRenderToken")
        val target = latestAllSchedulesState.value.allConfigs.firstOrNull { it.id == scheduleId }
            ?: defaultConfig(scheduleId)
        if (target.hasAnyWallpaper()) {
            // The wallpaper loader currently exposes no terminal error state. Bound this wait
            // so an unreadable URI degrades to the same loading/fallback layer as the real home.
            withTimeoutOrNull(2_500L) {
                snapshotFlow {
                    latestVisualState.value.config.id to (latestWallpaperImages.value.source != null)
                }.first { (renderedId, wallpaperReady) -> renderedId == scheduleId && wallpaperReady }
            }
        }
        val ready = withTimeoutOrNull(900L) {
            while (
                recordedScheduleId.get() != scheduleId ||
                recordedHomeGeneration.get() == generationBeforeSwitch
            ) {
                withFrameNanos { }
            }
            // The marker is written after graphicsLayer.record completes. Cross one more
            // frame boundary so capture never races the frame that produced the marker.
            withFrameNanos { }
            true
        } ?: false
        Log.d(
            "SchedulePicker",
            "await end requested=$scheduleId ready=$ready recorded=${recordedScheduleId.get()} generation=${recordedHomeGeneration.get()}"
        )
        return ready
    }

    suspend fun captureRenderedSchedule(scheduleId: Int): Bitmap? {
        if (!awaitRenderedSchedule(scheduleId)) return null
        val bitmap = runCatching { screenGraphicsLayer.toImageBitmap().asAndroidBitmap() }
            .onFailure { Log.e("SchedulePicker", "capture failed for schedule=$scheduleId", it) }
            .getOrNull()
        Log.d("SchedulePicker", "capture result requested=$scheduleId bitmap=${bitmap?.width}x${bitmap?.height}")
        if (bitmap != null) {
            appScope.launch { ScheduleSnapshotStore.save(context, scheduleId, bitmap) }
        }
        return bitmap
    }

    fun hydratePersistedSnapshots(generation: Int) {
        cacheHydrationJob?.cancel()
        cacheHydrationJob = appScope.launch {
            val selectedId = pickerState.selectedScheduleId ?: return@launch
            val selectedIndex = pickerState.orderIds.indexOf(selectedId)
            val neighborIds = listOf(selectedIndex - 1, selectedIndex + 1)
                .mapNotNull(pickerState.orderIds::getOrNull)
            val orderedTargets = (neighborIds + pickerState.orderIds)
                .distinct()
                .filter { it != selectedId && pickerState.snapshots[it] == null }
            for (targetId in orderedTargets) {
                if (generation != snapshotGeneration || !pickerState.overlayVisible) break
                ScheduleSnapshotStore.load(context, targetId)?.let { persisted ->
                    pickerState.snapshots[targetId] = persisted
                }
            }
        }
    }

    fun prewarmCurrentScheduleSnapshot() {
        entryPrewarmJob?.cancel()
        val requestedId = state.config.id
        entryPrewarmJob = appScope.launch {
            val snapshot = captureRenderedSchedule(requestedId) ?: return@launch
            if (
                pickerState.phase is CustomizeUiState.ShowingEntryButton &&
                state.config.id == requestedId
            ) {
                pickerState.currentSnapshot = snapshot
                pickerState.currentSnapshotScheduleId = requestedId
                pickerState.snapshots[requestedId] = snapshot
            }
        }
    }

    fun enterCustomizePage() {
        if (pickerState.phase !is CustomizeUiState.Home && pickerState.phase !is CustomizeUiState.ShowingEntryButton) return
        snapshotJob?.cancel()
        val generation = ++snapshotGeneration
        appScope.launch {
            showScheduleEntryPill = false
            pickerState.phase = CustomizeUiState.CapturingSnapshots
            val entryExitStartedAt = withFrameNanos { it }
            var frameTime: Long
            do {
                frameTime = withFrameNanos { it }
            } while (frameTime - entryExitStartedAt < 120_000_000L)
            val currentId = state.config.id
            pickerState.selectedScheduleId = currentId
            pickerState.originalScheduleId = currentId
            pickerState.orderIds.clear()
            pickerState.orderIds += currentId
            pickerState.orderIds += allSchedulesState.schedules.map { it.id }.filter { it != currentId }
            val prewarmed = pickerState.currentSnapshot
                ?.takeIf { pickerState.currentSnapshotScheduleId == currentId }
            val recordedNow = if (prewarmed == null && recordedScheduleId.get() == currentId) {
                runCatching { screenGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            } else null
            val currentSnapshot = prewarmed
                ?: recordedNow
                ?: ScheduleSnapshotStore.load(context, currentId)
                ?: ScheduleSnapshotStore.createEmptySchedulePlaceholder(
                    windowContainerSize.width,
                    windowContainerSize.height
                )
            pickerState.currentSnapshot = currentSnapshot
            pickerState.snapshots[currentId] = currentSnapshot
            pickerState.currentSnapshotScheduleId = currentId
            if (recordedNow != null) {
                appScope.launch { ScheduleSnapshotStore.save(context, currentId, recordedNow) }
            }
            pickerState.enterProgress.snapTo(0f)
            pickerState.chromeProgress.snapTo(0f)
            pickerState.pageSpacingProgress.snapTo(0f)
            pickerState.cornerProgress.snapTo(0f)
            pickerState.realHomeRevealProgress.snapTo(0f)
            pickerState.phase = CustomizeUiState.EnteringPicker
            withFrameNanos { }
            coroutineScope {
                launch { pickerState.enterProgress.animateTo(1f, tween(450, easing = CubicBezierEasing(0.3f, 0.72f, 0.2f, 1f))) }
                launch { pickerState.cornerProgress.animateTo(1f, tween(450, easing = CubicBezierEasing(0.3f, 0.72f, 0.2f, 1f))) }
                launch {
                    val titleDelayStart = withFrameNanos { it }
                    var titleFrameTime: Long
                    do {
                        titleFrameTime = withFrameNanos { it }
                    } while (titleFrameTime - titleDelayStart < 100_000_000L)
                    pickerState.chromeProgress.animateTo(1f, tween(350))
                }
                launch { pickerState.pageSpacingProgress.animateTo(1f, tween(500)) }
            }
            if (generation != snapshotGeneration) return@launch
            pickerState.phase = CustomizeUiState.Picker
            hydratePersistedSnapshots(generation)
        }
    }

    fun switchPickerSchedule(scheduleId: Int) {
        if (pickerState.phase !is CustomizeUiState.Picker || scheduleId == pickerState.selectedScheduleId) return
        snapshotJob?.cancel()
        val generation = ++snapshotGeneration
        pickerState.selectedScheduleId = scheduleId
        pickerState.snapshots[scheduleId]?.let {
            pickerState.currentSnapshot = it
            pickerState.currentSnapshotScheduleId = scheduleId
        }
        snapshotJob = appScope.launch {
            val requestedId = scheduleId
            val cached = pickerState.snapshots[requestedId]
                ?: ScheduleSnapshotStore.load(context, requestedId)
            if (cached != null && generation == snapshotGeneration && pickerState.selectedScheduleId == requestedId) {
                pickerState.snapshots[requestedId] = cached
                pickerState.currentSnapshot = cached
                pickerState.currentSnapshotScheduleId = requestedId
            }
            // Pager navigation is visual-only. Do not switch or recapture the hidden real
            // homepage here; doing so rebuilt the entire schedule on every settled swipe and
            // competed with the Pager animation for both main-thread and GPU time.
            if (generation == snapshotGeneration && pickerState.selectedScheduleId == requestedId) {
                hydratePersistedSnapshots(generation)
            }
        }
    }

    fun quickDraftFor(scheduleId: Int): QuickScheduleDraft {
        val config = latestAllSchedulesState.value.allConfigs.firstOrNull { it.id == scheduleId }
            ?: defaultConfig(scheduleId)
        val totalWeeks = config.totalWeeks.coerceIn(1, 60)
        val resolvedWeek = resolveScheduleCurrentWeek(
            config,
            totalWeeks,
            config.currentWeek,
            config.termStartDate,
            config.autoCurrentWeek
        )
        return QuickScheduleDraft(
            scheduleId = scheduleId,
            totalWeeks = totalWeeks,
            currentWeek = resolvedWeek,
            autoCurrentWeek = config.autoCurrentWeek,
            hideEmptyWeekends = config.hideEmptyWeekends,
            termStartDate = config.termStartDate.orEmpty()
        )
    }

    LaunchedEffect(pendingImportedSetupId) {
        val scheduleId = pendingImportedSetupId ?: return@LaunchedEffect
        screen = Screen.Home
        dismissHomeDialog()
        snapshotFlow {
            val latest = latestAllSchedulesState.value
            latest.schedules.any { it.id == scheduleId } &&
                latest.allConfigs.any { it.id == scheduleId } &&
                latestVisualState.value.config.id == scheduleId
        }.first { it }
        if (pickerState.overlayVisible) pickerState.reset()
        withFrameNanos { }
        enterCustomizePage()
        snapshotFlow {
            pickerState.phase is CustomizeUiState.Picker &&
                pickerState.selectedScheduleId == scheduleId
        }.first { it }
        quickScheduleDraft = quickDraftFor(scheduleId)
        pendingImportedSetupId = null
    }

    fun exitPicker(
        apply: Boolean,
        targetOverride: Int? = null,
        commitTarget: Boolean = apply,
        crossfadeToTarget: Boolean = !apply,
        onFinished: (() -> Unit)? = null
    ) {
        if (pickerState.interactionsLocked) return
        snapshotJob?.cancel()
        ++snapshotGeneration
        val targetId = targetOverride
            ?: if (apply) pickerState.selectedScheduleId ?: state.config.id else state.config.id
        appScope.launch {
            // Always start from exactly what the centered card is currently showing, even when
            // Cancel is returning to a different, previously-applied schedule.
            val centeredId = pickerState.selectedScheduleId
            val cachedCardSnapshot = centeredId?.let { id ->
                pickerState.currentSnapshot
                    ?.takeIf { pickerState.currentSnapshotScheduleId == id }
                    ?: pickerState.snapshots[id]
            }
            pickerState.phase = if (commitTarget) CustomizeUiState.Applying else CustomizeUiState.ExitingPicker
            pickerState.preparingExit = true
            val cancelTargetSnapshot = if (crossfadeToTarget) {
                pickerState.snapshots[targetId] ?: ScheduleSnapshotStore.load(context, targetId)
            } else {
                null
            }
            // Prepare the actual live destination, but do not turn it into the second visual
            // layer. The cached centered card will later fade away to reveal this real home.
            awaitRenderedSchedule(targetId)
            pickerState.currentSnapshot = cachedCardSnapshot ?: pickerState.currentSnapshot
            // Cancel visually dissolves the original/applied schedule's persisted card directly
            // over the currently centered card while it grows. Apply continues to enlarge the
            // selected card and hands it directly to the live destination.
            pickerState.transitionFromSnapshot = cancelTargetSnapshot
            pickerState.snapshotCoverBitmap = null
            if (commitTarget) {
                val activationFinished = kotlinx.coroutines.CompletableDeferred<Unit>()
                viewModel.activateSchedule(targetId) { activationFinished.complete(Unit) }
                activationFinished.await()
                withTimeoutOrNull(1_200L) {
                    snapshotFlow { latestVisualState.value.config.id }.first { it == targetId }
                }
            }
            // Force the fully loaded destination through two additional draw/vsync boundaries.
            // The loading indicator remains visible throughout this preparation window.
            captureRenderToken += 1
            withFrameNanos { }
            withFrameNanos { }
            pickerState.preparingExit = false
            coroutineScope {
                launch { pickerState.enterProgress.animateTo(0f, tween(420, easing = CubicBezierEasing(0.3f, 0.72f, 0.2f, 1f))) }
                launch { pickerState.chromeProgress.animateTo(0f, tween(220)) }
                launch { pickerState.pageSpacingProgress.animateTo(0f, tween(320)) }
            }
            // Geometry is now fully screen-sized. Only now flatten the corner radius so no
            // square card edge can be exposed while the card is still floating over the home.
            pickerState.cornerProgress.animateTo(0f, tween(90))
            // The cached card is now fully screen-sized and square. Reveal the already-rendered
            // real home underneath instead of crossfading to another bitmap.
            pickerState.realHomeRevealProgress.animateTo(1f, tween(250))
            val handoffHoldStart = withFrameNanos { it }
            var handoffFrame: Long
            do {
                handoffFrame = withFrameNanos { it }
            } while (handoffFrame - handoffHoldStart < 64_000_000L)
            val temporaryToDelete = pickerState.temporaryIds.filter { !commitTarget || it != targetId }
            previewScheduleId = null
            pickerState.reset()
            temporaryToDelete.forEach(viewModel::deleteSchedule)
            withFrameNanos { }
            onFinished?.invoke()
        }
    }

    val latestPickerBackAction by rememberUpdatedState(newValue = {
        if (pickerState.phase is CustomizeUiState.Picker) {
            if (pickerState.deletingScheduleId != null) {
                pickerState.deletingScheduleId = null
                pickerState.deleteReveal = 0f
            } else {
                // System Back is deliberately identical to the visible Cancel action.
                exitPicker(apply = false)
            }
        }
    })
    BackHandler(enabled = pickerState.overlayVisible) {
        latestPickerBackAction()
    }
    var returnHomeWeekRequest by remember { mutableIntStateOf(0) }
    val returnHomeToCurrentDateAndWeek = {
        returnHomeWeekRequest++
        if (beforeScheduleTerm) {
            homeDisplayDate = parseScheduleDate(visualState.config.termStartDate) ?: LocalDate.now()
            homeDisplayWeek = 1
        } else {
            homeDisplayDate = LocalDate.now()
            homeDisplayWeek = homeCurrentWeek
        }
    }

    LaunchedEffect(state.config.hideFromRecents) {
        hideFromRecentsEnabled = state.config.hideFromRecents
    }

    var initialLifecycleStartSeen by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                PendingImportSetupStore.consume(context)?.let { pendingImportedSetupId = it }
                if (initialLifecycleStartSeen) {
                    viewModel.refreshNotifications()
                    pendingPickerEditorScheduleId?.let { scheduleId ->
                        pendingPickerEditorScheduleId = null
                        val generation = ++snapshotGeneration
                        appScope.launch {
                            pickerState.phase = CustomizeUiState.ExitingEditor
                            val snap = captureRenderedSchedule(scheduleId)
                            if (generation == snapshotGeneration && pickerState.selectedScheduleId == scheduleId) {
                                if (snap != null) {
                                    pickerState.snapshots[scheduleId] = snap
                                    pickerState.currentSnapshot = snap
                                    pickerState.currentSnapshotScheduleId = scheduleId
                                }
                                pickerState.phase = CustomizeUiState.Picker
                                hydratePersistedSnapshots(generation)
                            }
                        }
                    }
                } else {
                    initialLifecycleStartSeen = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(1800)
            viewModel.clearSnackbar()
        }
    }
    // Root wrapper — ensures splash covers all content including Scaffold internals
    @OptIn(ExperimentalSharedTransitionApi::class)
    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize()
    ) {
    val sharedTransitionScope = this
    val activeSharedTransitionScope = if (startupPhase == StartupPhase.FullQuality) sharedTransitionScope else null
    val sharedCourseFrame = courseCardGlassEffectFrame(
        tokens = GlassTokens.courseCard(visualState.config.courseCardBlur),
        liveBlur = personalizationPreviewState.cardBlur ?: visualState.config.courseCardBlur,
        quality = glassQuality,
        hasWallpaper = visualState.config.hasAnyWallpaper()
    )
    val sharedCourseRadiusPx = with(LocalDensity.current) { (sharedCourseFrame.blur ?: 0.dp).toPx() }
    val sharedCourseBackdrop = remember(backgroundBackdrop, sharedCourseRadiusPx, sharedCourseFrame.useVibrancy) {
        com.kyant.backdrop.backdrops.SharedBlurBackdrop(backgroundBackdrop, sharedCourseRadiusPx, sharedCourseFrame.useVibrancy)
    }
    val sharedCourseBackdropExpected = rootPageMotion.retains(false) &&
        visualState.config.courseCardGlassEnabled && visualState.config.hasAnyWallpaper() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useSharedCourseBackdrop = sharedCourseBackdropExpected && wallpaperImages.source != null
    lateinit var handleHomeAgentAction: AgentActionHandler
    handleHomeAgentAction = {
        plan: AgentPlan,
        onResult: (AgentPlanExecutionResult) -> Unit ->
        val actions = plan.actions
        val action = actions.singleOrNull()
        val courseActions = actions.filter {
            it.type == AgentValidatedActionType.ADD ||
                it.type == AgentValidatedActionType.UPDATE ||
                it.type == AgentValidatedActionType.REPLACE ||
                it.type == AgentValidatedActionType.DELETE
        }
         val settingActions = actions.filter {
             it.type == AgentValidatedActionType.SET_SETTING ||
                 it.type == AgentValidatedActionType.SET_PERIOD_SETTINGS ||
                 it.type == AgentValidatedActionType.SET_ADJUSTMENTS
        }
        when {
            courseActions.size == actions.size && actions.isNotEmpty() ->
                viewModel.executeAgentPlan(actions, onResult)
            actions.isNotEmpty() && actions.all { it.type == AgentValidatedActionType.CREATE_TODO } ->
                (context.applicationContext as CourseScheduleApp).applicationScope.launch(Dispatchers.Main.immediate) {
                    onResult(executeConfirmedAgentTodos(context.applicationContext, plan))
                }
            settingActions.isNotEmpty() && settingActions.size + courseActions.size == actions.size ->
                viewModel.executeAgentSettingPlan(actions, onResult)
            actions.isEmpty() -> onResult(AgentPlanExecutionResult(false, null, false, "没有可执行操作"))
            action == null -> {
                // Keep dependent operations ordered. Adjacent course/settings steps share one
                // transaction; navigation/context changes occur only after that batch succeeds.
                val batch = actions.takeWhile { it in courseActions || it in settingActions }
                    .ifEmpty { listOf(actions.first()) }
                handleHomeAgentAction(AgentPlan(batch)) { firstResult ->
                    if (!firstResult.success || !firstResult.verified) onResult(firstResult)
                    else handleHomeAgentAction(AgentPlan(actions.drop(batch.size))) { remaining ->
                        onResult(remaining.copy(verified = firstResult.verified && remaining.verified,
                            message = firstResult.message + "\n" + remaining.message, undo = null))
                    }
                }
            }
            else -> when (action.type) {
            AgentValidatedActionType.CREATE_TODO -> (context.applicationContext as CourseScheduleApp).applicationScope.launch(Dispatchers.Main.immediate) {
                onResult(executeConfirmedAgentTodos(context.applicationContext, plan))
            }
            AgentValidatedActionType.ADD,
            AgentValidatedActionType.UPDATE,
            AgentValidatedActionType.REPLACE,
            AgentValidatedActionType.DELETE ->
                viewModel.executeAgentPlan(actions, onResult)
            AgentValidatedActionType.OPEN_SETTINGS -> viewModel.openAgentPage(onResult) { current ->
                if (action.settingsPage == "SCHEDULE_ADJUSTMENTS") {
                    context.openRegisteredActivity(TransitionRouteId.SettingsToScheduleAdjustments,
                        ScheduleAdjustmentsActivity.intent(context, current.config, current.config.scheduleAdjustmentsJson),
                        launchActivity = { holidayAdjustmentsLauncher.launch(it) })
                } else if (action.settingsPage == "PERSONALIZATION") {
                    openHomeAnchoredOverlay(HomeAnchoredOverlayKind.Personalize)
                } else agentSettingsPage(action.settingsPage)?.let { page ->
                    val intent = Intent(context, SettingsDetailActivity::class.java)
                        .putExtra(SettingsDetailPageExtra, page.name)
                    if (page == SettingsPage.Schedule) {
                        intent.putExtra(ScheduleCustomizeIdExtra, current.config.id)
                    }
                    context.openRegisteredActivity(
                        TransitionRouteId.HomeToSettingsDetail,
                        intent
                    )
                }
            }
            AgentValidatedActionType.OPEN_IMPORT -> {
                assistantImportText = action.importText
                assistantImportUri = action.importAttachmentUri?.let(Uri::parse)
                homeDialog = HomeDialog.ImportSchedule
                onResult(
                    AgentPlanExecutionResult(
                        success = true,
                        preview = null,
                        verified = true,
                        message = "已打开 AI 导入"
                    )
                )
            }
            AgentValidatedActionType.SET_SETTING,
            AgentValidatedActionType.SET_PERIOD_SETTINGS,
            AgentValidatedActionType.SET_ADJUSTMENTS ->
                viewModel.executeAgentSettingPlan(actions, onResult)
            AgentValidatedActionType.CREATE_SCHEDULE ->
                viewModel.createScheduleFromAgent(action.scheduleName.orEmpty(), onResult)
            AgentValidatedActionType.ACTIVATE_SCHEDULE ->
                action.scheduleId?.let { viewModel.activateScheduleFromAgent(it, onResult) }
                    ?: onResult(
                        AgentPlanExecutionResult(false, null, false, "目标课表不存在，请重新读取课表列表")
                    )
            AgentValidatedActionType.DELETE_SCHEDULE ->
                action.scheduleId?.let { viewModel.deleteScheduleFromAgent(it, onResult) }
                    ?: onResult(
                        AgentPlanExecutionResult(false, null, false, "目标课表不存在，请重新读取课表列表")
                    )
        }
        }
    }
    CompositionLocalProvider(
        LocalHomeAssistant provides homeAssistant,
        LocalAdjustedCourseEditor provides ::openAdjustedCourseEditor,
        LocalCourseShortcuts provides courseShortcuts,
        LocalCourseCopy provides courseCopy,
        LocalCourseRemoval provides courseRemoval,
        com.xiaomanjun.sleepdownschedule.glass.LocalSharedCourseBackdrop provides
            sharedCourseBackdrop.takeIf { sharedCourseBackdropExpected },
        LocalSharedTransitionScope provides activeSharedTransitionScope,
        LocalEditingCourseId provides editingCourseId,
        LocalCourseEditorFlightRegistry provides courseEditorFlightRegistry,
        LocalStartupPhase provides startupPhase,
        LocalGlassQuality provides glassQuality,
        LocalAdaptiveGlass provides adaptiveGlassState,
        LocalHomeReadability provides homeReadabilityContext,
        LocalCourseCardPalette provides homeCoursePalette,
        LocalCourseCardColorAssignments provides homeCourseColorAssignments,
        com.xiaomanjun.sleepdownschedule.feature.home.LocalPersonalizationPreview provides
            personalizationPreviewState,
        LocalGlassSceneState provides glassSceneState,
        LocalCenteredDialogSceneBackdrop provides centeredDialogSceneBackdrop
    ) {
    // Home now owns an explicit Miuix root scaffold, matching the already-fixed secondary-page
    // topology. Its complete business stack is the underlay; Miuix renders every second-level
    // popup in the scaffold's later sibling host, so a dialog consumer can never be recorded by
    // the LayerBackdrop it samples.
    top.yukonga.miuix.kmp.basic.Scaffold(
        modifier = Modifier.fillMaxSize().assistantPullGesture(
            enabled = screen is Screen.Home && homeMode == HomeMode.Week && state.loaded && !homeAssistant.visible,
            canStart = { position ->
                position.y > with(density) { homeAdaptiveMetrics.safeTop.toPx() } &&
                    homeAssistant.canPullAt(position) &&
                    !homeContentUnderTopBar && !homeAssistant.editing && !homeBackgroundOverlayActive && !courseCopy.active &&
                    renderedHomeDialog == null && !jumpWeekDialogMounted &&
                    pickerState.phase is CustomizeUiState.Home &&
                    dayAgentBackgroundMotionState.progress.value < 0.001f &&
                    DayAgentPreferences.isWeekAssistantEnabled(context) &&
                    (!DayAgentPreferences.hasDecision(context) || DayAgentPreferences.isEnabled(context))
            },
            onStart = { assistantHapticSent = false; homeAssistant.beginPull() },
            onDistance = {
                if (homeAssistant.pull(it, density.density) && !assistantHapticSent) {
                    assistantHapticSent = true
                    assistantHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onRelease = { homeAssistant.release(it && homeAssistant.armed) }
        ),
        underlayModifier = Modifier
            .fillMaxSize()
            .then(if (courseShortcuts.request != null || courseCopy.active || homeAssistant.visible) {
                Modifier.glassBackdropProducer(centeredDialogSceneBackdrop)
            } else {
                Modifier.centeredDialogSceneProducer(centeredDialogSceneBackdrop)
            }),
        popupHost = {
            Box(Modifier.fillMaxSize()) {
                HomeAssistantHost(
                    controller = homeAssistant,
                    state = state,
                    available = screen is Screen.Home && state.loaded &&
                        (homeMode == HomeMode.Week || homeAssistant.stage == HomeAssistantStage.Conversation),
                    remindersAllowed = homeMode == HomeMode.Week && !homeAssistant.editing &&
                        !homeBackgroundOverlayActive && !courseCopy.active && renderedHomeDialog == null &&
                        !jumpWeekDialogMounted && pickerState.phase is CustomizeUiState.Home &&
                        dayAgentBackgroundMotionState.progress.value < 0.001f,
                    backdrop = centeredDialogSceneBackdrop,
                    backgroundMotion = dayAgentBackgroundMotionState,
                    onAgentAction = handleHomeAgentAction,
                    onImportFile = { uri -> assistantImportUri = uri; homeDialog = HomeDialog.ImportSchedule }
                )
                CourseCopyOverlay(courseCopy, visualState.config, centeredDialogSceneBackdrop, backgroundBackdrop)
                CourseShortcutOverlay(
                    controller = courseShortcuts,
                    config = visualState.config,
                    backdrop = centeredDialogSceneBackdrop,
                    cardBackdrop = backgroundBackdrop,
                    onEdit = { source -> openCourseEditor(source.course, source.week, source.bounds) },
                    onCopy = { source, draft ->
                        courseCopy.begin(source, draft)
                    },
                    onRemove = { course, week ->
                        pendingCourseGroupDelete = emptyList()
                        homeDialog = HomeDialog.ApplyCourseDelete(course, week)
                    }
                )
                // Overlay content is subcomposed by the host, not at its call site. Preserve the
                // same Miuix/Glass CompositionLocals that wrapped the 1.1.5 host; otherwise sheet
                // rows fall back to the taller non-Miuix implementation and their dividers become
                // visible. The host remains a sibling of the backdrop producer, so this does not
                // reintroduce the former producer/consumer recursion.
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (detailPopupCaptureActive) {
                                val requestedToken = detailPopupCaptureToken
                                if (detailPopupCapturedToken.get() != requestedToken) {
                                    detailPopupGraphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    detailPopupCapturedToken.set(requestedToken)
                                }
                                drawLayer(detailPopupGraphicsLayer)
                            } else {
                                drawContent()
                            }
                        }
                ) {
                    GlassMiuixSettingsTheme(settingsVisualConfig(state.config)) {
                        MiuixPopupHost()
                    }
                }

                // Detailed settings deliberately overlays the still-mounted quick sheet, matching
                // the 1.2.0 handoff. Keeping it after the Miuix host prevents the retained sheet
                // from covering the destination without closing/reopening either surface.
                detailCaptureCoverBitmap?.let { cover ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(259f)
                            .drawWithContent {
                                if (!detailCaptureMaskActive.get()) drawContent()
                            }
                            .pointerInput(cover) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent().changes.forEach { it.consume() }
                                    }
                                }
                            }
                    ) {
                        Image(
                            bitmap = cover.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                detailMorphRequest?.let { request ->
                    val detailState = allSchedulesState.forSchedule(request.scheduleId)
                    DetailScheduleMorphOverlay(
                        request = request,
                        detailState = detailState,
                        onMorphStateChange = { detailMorphState = it },
                        onSave = { config, periods ->
                            viewModel.saveConfigForSchedule(request.scheduleId, config, periods)
                        },
                        onPreviewLiveUpdate = viewModel::previewLiveUpdate,
                        onFinished = {
                            detailMorphRequest = null
                            detailCaptureCoverBitmap = null
                            detailCaptureRecordCleanFrame = false
                            detailPopupCaptureActive = false
                            detailMorphState = DetailMorphState.Idle
                        },
                        modifier = Modifier.zIndex(260f)
                    )
                }
            }
        },
        containerColor = ComposeColor.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned {
                homeReadabilityRootSize = it.size
                homeRootPositionOnScreen = it.positionOnScreen()
                homeRootPositionInWindow = it.positionInWindow()
            }
            .drawWithContent {
                val recordCleanFrame =
                    detailMorphState is DetailMorphState.Capturing && detailCaptureRecordCleanFrame
                val courseEditorOwnsFrame =
                    courseEditorRequest != null ||
                        courseEditorOverlayPhase != CourseEditorOverlayPhase.Idle
                // Never record the course editor into the detail-page capture layer. It contains
                // several backdrop consumers and recording that entire overlay on every animation
                // frame causes an explosive offscreen-render workload after the first use.
                // HomeAnchored/HomeMenuDestination own similarly heavy live backdrop consumers;
                // pause this unrelated capture while they move. Their settled Open frame remains
                // available to flows such as manual-import history detail background capture.
                // Keep the detailed-settings source frame live while the quick sheet animates.
                // The detailed-settings handoff needs consecutive fully composed source and
                // clean-background frames, as in the 1.1.5 implementation.
                val shouldRecordStableDetailFrame =
                    detailMorphState is DetailMorphState.Idle &&
                        !rootPageMotion.moving && !homeModeMotion.moving &&
                        !courseEditorOwnsFrame &&
                        (
                            homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Open ||
                                (homeAnchoredOverlayRequest == null &&
                                    homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Idle)
                            ) &&
                        (
                            homeMenuDestinationMotionState.phase == HomeAnchoredOverlayPhase.Open ||
                                (homeMenuDestinationRequest == null &&
                                    homeMenuDestinationMotionState.phase == HomeAnchoredOverlayPhase.Idle)
                            )
                if (shouldRecordStableDetailFrame || recordCleanFrame) {
                    detailCaptureMaskActive.set(recordCleanFrame)
                    try {
                        detailScreenGraphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                    } finally {
                        detailCaptureMaskActive.set(false)
                    }
                    if (!recordCleanFrame) {
                        // Recording already traversed this same visible frame. Present that
                        // display list instead of drawing every backdrop consumer a second time.
                        // Masked transition captures still need their separate unmasked draw.
                        drawLayer(detailScreenGraphicsLayer)
                        return@drawWithContent
                    }
                }
                drawContent()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    translationY = if (homeAssistant.pullingHome) homeAssistant.pullPixels else 0f
                    shape = RoundedRectangle(homeDeviceCornerPx.toDp())
                    clip = translationY != 0f
                    val zoom = dayAgentBackgroundMotionState.backgroundZoom.value
                    val depthProgress =
                        ((1f - zoom) / (1f - DayAgentBackgroundZoomRestScale)).coerceIn(0f, 1f)
                    scaleX = zoom
                    scaleY = zoom
                    val blurPx = if (homeAssistant.visible) 0f else with(density) { 12.dp.toPx() } * depthProgress
                    renderEffect = if (blurPx > 0.001f) {
                        BlurEffect(blurPx, blurPx, TileMode.Clamp)
                    } else null
                }
        ) {
        // The live home surface owns shared depth for source-anchored overlays. Consumers render
        // outside this layer and compensate through rememberScreenScaledBackdrop, keeping the
        // liquid sampling coordinates aligned without blurring the foreground panel itself.
        HomeBackgroundZoomLayer(
            zoom = homeOverlayBackgroundZoom,
            dimProgress = { maxOf(homeOverlayBackgroundBlurProgress(), courseShortcuts.progress.value) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        HomeBackgroundBlurLayer(
            blurProgress = { maxOf(homeOverlayBackgroundBlurProgress(), courseShortcuts.progress.value) },
            useFrozenHomeScene = useFrozenHomeMorphBlur,
            closing = { homeBackgroundBlurClosing || courseShortcuts.closing },
            sceneKey = homeCaptureFrameKey,
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val freeze = useFrozenHomeMorphBlur()
                    // A moving pair of pages is not a stable overlay source. Recording it on
                    // the switch's first frame adds a full tree traversal to page construction.
                    val needsCapture = !rootPageMotion.moving && !homeModeMotion.moving &&
                        lastRecordedHomeFrameKey.get() != homeCaptureFrameKey
                    screenGraphicsLayer.alpha = 1f
                    if (needsCapture) {
                        screenGraphicsLayer.record { this@drawWithContent.drawContent() }
                        recordedScheduleId.set(visualState.config.id)
                        recordedHomeGeneration.incrementAndGet()
                        lastRecordedHomeFrameKey.set(homeCaptureFrameKey)
                    }
                    if (freeze) {
                        if (weekHomeSurfaceUsesOffscreenCache.compareAndSet(false, true)) {
                            screenGraphicsLayer.compositingStrategy =
                                androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                        }
                        // Replay the neutral scene. Zoom and blur belong to the two outer
                        // layers; the retained course tree is neither drawn nor re-recorded.
                        drawLayer(screenGraphicsLayer)
                    } else {
                        if (weekHomeSurfaceUsesOffscreenCache.compareAndSet(true, false)) {
                            screenGraphicsLayer.compositingStrategy =
                                androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
                        }
                        // A changed model/window has already been drawn by record above. Replay
                        // that frame once instead of traversing the home tree twice on resume.
                        if (needsCapture) drawLayer(screenGraphicsLayer) else drawContent()
                    }
                }
        ) {
        CompositionLocalProvider(
            LocalHomeBackgroundFrozen provides homeBackgroundFreezeActive,
            LocalHomeTextContrastFrozen provides (rootPageMotion.moving || homeModeMotion.moving),
            com.xiaomanjun.sleepdownschedule.glass.LocalGlassCoordinatesFrozen provides
                freezeHomeGlassCoordinates,
            com.xiaomanjun.sleepdownschedule.glass.LocalGlassSampleRecordKey provides homeGlassSampleRecordKey
        ) {
        Scaffold(
            containerColor = ComposeColor.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Box(Modifier.fillMaxWidth()) {
                HomeSwitchPane(rootPageMotion, secondary = false, modifier = Modifier.fillMaxWidth(),
                    pageClip = HomeSwitchClip.TopBar) {
                TopBarEntranceContainer(
                    phase = startupPhase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            rootTopBarLayoutHeight(
                                Screen.Home,
                                homeMode == HomeMode.Week && weekViewStyle == WeekViewStyle.BOUNDLESS
                            )
                        )
                ) {
                    if (rootPageMotion.retains(false)) {
                        AnimatedVisibility(
                            visible = homeContentUnderTopBar,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(10f),
                            enter = fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f)),
                            exit = fadeOut(animationSpec = spring(dampingRatio = 0.95f, stiffness = 560f))
                        ) {
                            HomeTopGradientBlur(
                                config = visualState.config,
                                backdrop = chromeBackdrop,
                                height = if (
                                    homeMode == HomeMode.Week &&
                                    weekViewStyle == WeekViewStyle.BOUNDLESS
                                ) {
                                    boundlessHomeTopGradientHeight(homeAdaptiveMetrics)
                                } else if (homeMode == HomeMode.Week) {
                                    // 普通周视图用独立的顶栏模糊：不包含无界模式表头那段高度
                                    (homeAdaptiveMetrics.topOverlayHeight -
                                        BoundlessWeekHeaderRowHeight).coerceAtLeast(110.dp)
                                } else {
                                    homeAdaptiveMetrics.topOverlayHeight
                                },
                                fullBlurHeight = if (homeMode == HomeMode.Week && weekViewStyle == WeekViewStyle.BOUNDLESS) {
                                    homeAdaptiveMetrics.safeTop + 66.dp + BoundlessWeekHeaderRowHeight + 4.dp
                                } else 0.dp,
                                modifier = Modifier
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(11f)
                    ) {
                        AppTopBar(
                            screen = Screen.Home,
                            state = visualState,
                            settingsPage = SettingsPage.Root,
                            backdrop = chromeBackdrop,
                            homeMode = homeMode,
                            onHomeModeChange = { homeMode = it },
                            homeDisplayDate = homeDisplayDate,
                            homeDisplayWeek = if (weekViewStyle == WeekViewStyle.BOUNDLESS) {
                                homeWeekHeaderPreview.value ?: homeDisplayWeek
                            } else homeDisplayWeek,
                            beforeScheduleTerm = beforeScheduleTerm,
                            afterScheduleTerm = afterScheduleTerm,
                            homeShowingAnotherWeek = homeShowingAnotherWeek,
                            onReturnHomeToCurrentWeek = returnHomeToCurrentDateAndWeek,
                            activeHomeOverlay = activeHomeAnchoredOverlay,
                            addButtonHidden = addButtonHidden,
                            onAddButtonPositioned = { addButtonBounds = it },
                            onPersonalizeButtonPositioned = { personalizeButtonBounds = it },
                            onToggleAddMenu = { sourceScale ->
                                toggleHomeAnchoredOverlay(HomeAnchoredOverlayKind.Add, sourceScale)
                            },
                            onTogglePersonalize = { sourceScale ->
                                toggleHomeAnchoredOverlay(HomeAnchoredOverlayKind.Personalize, sourceScale)
                            },
                            onBackHome = { screen = Screen.Home }
                        )
                    }
                    if (homeModeMotion.retains(true) && weekViewStyle == WeekViewStyle.BOUNDLESS) {
                        // Boundless week header lives on the top bar layer (above the gradient
                        // blur) so weekday labels are never covered; geometry mirrors the course
                        // grid (rowHeaderWidth slot + equal columns + weekGridEndPadding).
                        BoundlessWeekdayHeaderRow(
                            displayWeek = homeWeekHeaderPreview.value ?: homeDisplayWeek,
                            courses = visualState.courses,
                            config = visualState.config,
                            today = todayDate,
                            textColor = homeForegroundColor(visualState.config),
                            backdrop = chromeBackdrop,
                            rowHeaderWidth = BoundlessWeekRowHeaderWidth,
                            weekGridEndPadding = if (homeAdaptiveMetrics.isLargeScreen) {
                                0.dp
                            } else {
                                BoundlessWeekGridEndPadding
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(12f)
                                .fillMaxWidth()
                                .padding(top = homeAdaptiveMetrics.safeTop + 66.dp)
                                .padding(
                                    start = if (homeAdaptiveMetrics.isLargeScreen) homeAdaptiveMetrics.tabletContentMargin else 0.dp,
                                    end = if (homeAdaptiveMetrics.isLargeScreen) homeAdaptiveMetrics.tabletContentMargin else 0.dp
                                )
                                .homeSwitchLayer(homeModeMotion, secondary = true)
                        )
                    }
                }
                }
                HomeSwitchPane(
                    rootPageMotion,
                    secondary = true,
                    modifier = Modifier.fillMaxWidth(),
                    pageClip = HomeSwitchClip.TopBar
                ) {
                    TopBarEntranceContainer(
                        phase = startupPhase,
                        modifier = Modifier.fillMaxWidth()
                            .height(rootTopBarLayoutHeight(screen))
                    ) {
                        if (secondaryPageShowsTodo) {
                            CompositionLocalProvider(LocalAdaptiveGlass provides todoAdaptiveGlassState) {
                                key(screen.todoPage() ?: lastTodoPage) {
                                HomePageEntrance(animateOnMount = !rootPageMotion.moving, entranceDirection = dockEntranceDirection) {
                                TodoRootTopBar(
                                    page = screen.todoPage() ?: lastTodoPage,
                                    config = todoVisualConfig,
                                    backdrop = chromeBackdrop,
                                    actions = todoChromeActions,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                                }
                                }
                            }
                        }
                    }
                }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glassBackdropProducer(backgroundBackdrop, recordKey = {
                            if (visualState.loaded && wallpaperImages.source != null) {
                                val imageKeys = buildList {
                                    if (rootPageMotion.retains(false)) {
                                        homeWallpaperRecordKey.value?.let(::add)
                                    }
                                    if (rootPageMotion.retains(true) && secondaryPageUsesHomeWallpaper) {
                                        todoWallpaperRecordKey.value?.let(::add)
                                    }
                                }
                                imageKeys.takeIf { it.isNotEmpty() }?.let { keys ->
                                    listOf(
                                        keys,
                                        personalizationPreviewState.wallpaperBrightness
                                            ?: visualState.config.wallpaperBrightness,
                                        rootPageMotion.pageSampleKey
                                    )
                                }
                            } else null
                        })
                        .background(ComposeColor.White)
                ) {
                    if (rootPageMotion.retains(false)) {
                        Box(Modifier.fillMaxSize().homeSwitchLayer(rootPageMotion, secondary = false,
                            pageClip = HomeSwitchClip.Page)) {
                        if (!visualState.loaded) {
                            HomeBackdropFallback()
                        } else {
                            if (wallpaperImages.source != null) {
                                HomeWallpaper(
                                    visualState.config,
                                    wallpaperImages,
                                    startupPhase,
                                    previewState = personalizationPreviewState,
                                    onRecordKeyChanged = updateHomeWallpaperRecordKey,
                                    isActive = screen is Screen.Home && rootPageMotion.retains(false) && visualState.loaded
                                )
                                WallpaperGlassSamplingToneOverlay(
                                    visualState.config,
                                    personalizationPreviewState
                                )
                            } else {
                                HomeBackdropFallback(noWallpaper = noWallpaperResolved)
                            }
                        }
                        }
                    }
                    if (rootPageMotion.retains(true)) {
                        Box(
                            Modifier.fillMaxSize()
                                .homeSwitchLayer(rootPageMotion, secondary = true, pageClip = HomeSwitchClip.Page)
                        ) {
                            if (secondaryPageUsesHomeWallpaper) {
                                if (!visualState.loaded) {
                                    HomeBackdropFallback()
                                } else if (wallpaperImages.source != null) {
                    HomeWallpaper(
                        visualState.config,
                        wallpaperImages,
                        startupPhase,
                        previewState = personalizationPreviewState,
                        onRecordKeyChanged = updateTodoWallpaperRecordKey,
                        isActive = screen.isTodoDestination() && visualState.loaded
                    )
                                    WallpaperGlassSamplingToneOverlay(
                                        visualState.config,
                                        personalizationPreviewState
                                    )
                                } else {
                                    HomeBackdropFallback(noWallpaper = noWallpaperResolved)
                                }
                            } else {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(settingsPageBackground(settingsVisualConfig(state.config)))
                                )
                            }
                        }
                    }
                }
                /*
                 * Wallpaper brightness is a visual treatment for the wallpaper only. Keep it
                 * outside backgroundBackdrop's provider node: liquid course cards should sample
                 * the original wallpaper, otherwise lowering wallpaper brightness also darkens
                 * every glass surface that consumes this backdrop.
                 */
                if (
                    rootPageMotion.retains(false) &&
                    visualState.loaded &&
                    visualState.config.hasAnyWallpaper() &&
                    wallpaperImages.source != null
                ) {
                    Box(Modifier.fillMaxSize().homeSwitchLayer(rootPageMotion, secondary = false,
                        pageClip = HomeSwitchClip.Page)) {
                        WallpaperToneOverlay(visualState.config, personalizationPreviewState)
                    }
                }
                if (
                    rootPageMotion.retains(true) &&
                    secondaryPageUsesHomeWallpaper &&
                    visualState.loaded &&
                    visualState.config.hasAnyWallpaper() &&
                    wallpaperImages.source != null
                ) {
                    Box(Modifier.fillMaxSize().homeSwitchLayer(rootPageMotion, secondary = true,
                        pageClip = HomeSwitchClip.Page)) {
                        WallpaperToneOverlay(visualState.config, personalizationPreviewState)
                    }
                }
                if (useSharedCourseBackdrop) {
                    Box(Modifier.fillMaxSize().then(sharedCourseBackdrop.preRenderModifier {
                        // Include the corners: they follow the tail after the faster page lands.
                        homeWallpaperRecordKey.value?.let { imageKey ->
                            listOf(imageKey, personalizationPreviewState.wallpaperBrightness
                                ?: visualState.config.wallpaperBrightness, rootPageMotion.pageSampleKey)
                        }
                    }))
                }
                val contentModifier = Modifier
                    .fillMaxSize()
                    .glassBackdropProducer(contentBackdrop)
                Column(modifier = contentModifier) {
                    if (screen !is Screen.Home) {
                        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    }
                    ContentEntranceContainer(phase = startupPhase, modifier = Modifier.weight(1f)) {
                        HomeSwitchPane(rootPageMotion, secondary = false, modifier = Modifier.fillMaxSize(),
                            pageClip = HomeSwitchClip.Page, retainContent = true) {
                            rootPageStateHolder.SaveableStateProvider("home") {
                                 if (visualState.loaded) HomeScreen(
                                     state = visualState,
                                     todoItems = todoItems,
                                     agentState = agentVisualState,
                                     personalizationPreviewState = personalizationPreviewState,
                                     mode = homeMode,
                                     modeMotion = homeModeMotion,
                                     dayViewMode = dayViewMode,
                                     weekViewStyle = weekViewStyle,
                                     adaptiveMetrics = homeAdaptiveMetrics,
                                    weekCardHeight = weekCardHeight.dp,
                                    displayWeek = homeDisplayWeek,
                                    returnToCurrentWeekRequest = returnHomeWeekRequest,
                                    displayDate = homeDisplayDate,
                                    backdrop = backgroundBackdrop,
                                    dayAgentBackdrop = dayAgentBackdrop,
                                    // Dragged week cards must not sample contentBackdrop/chromeBackdrop:
                                    // they live inside contentBackdrop, so sampling it can recursively include themselves.
                                    floatingCourseBackdrop = backgroundBackdrop,
                                     weekHeaderBackdrop = backgroundBackdrop,
                                    onSwipeWeek = { delta -> homeDisplayWeek = (homeDisplayWeek + delta).coerceIn(1, visualState.config.totalWeeks.coerceAtLeast(1)) },
                                    onWeekHeaderPreview = { homeWeekHeaderPreview.value = it },
                                    onWeekJumpSettled = { settledWeek ->
                                        if (settledWeek == homeDisplayWeek) captureRenderToken++
                                    },
                                    onSwipeDay = { delta ->
                                        val requested = homeDisplayDate.plusDays(delta.toLong())
                                        val range = scheduleDayNavigationRange(visualState.config, todayDate)
                                        homeDisplayDate = if (range == null) requested
                                        else requested.coerceIn(range.start, range.endInclusive)
                                    },
                                    onContentUnderTopBarChange = { if (screen is Screen.Home) homeContentUnderTopBar = it },
                                    dayAgentBackgroundMotionState = dayAgentBackgroundMotionState,
                                    onAgentPagerSettledChange = { settled ->
                                        dayAgentPagerSettled = settled
                                    },
                                    onAgentPrepareOpen = {
                                        // Never open over a partially swiped day page. The background is now
                                        // transformed live (blur + zoom), so no frozen snapshot capture is
                                        // needed — only the pager-settled gate remains.
                                        if (!dayAgentPagerSettled) {
                                            snapshotFlow { dayAgentPagerSettled }.first { it }
                                        }
                                    },
                                    onAgentDismissed = {},
                                    onCourseClick = { course, week, sourceBounds ->
                                        openCourseEditor(course, week, sourceBounds)
                                    },
                                    onOpenTodos = {
                                        todoReturnsToCourse = true
                                        screen = Screen.TodoTasks
                                    },
                                    onAddCourse = viewModel::addCourse,
                                    onAgentAction = handleHomeAgentAction,
                                    onUpdateCourseSingleWeek = viewModel::updateCourseSingleWeek,
                                    conflictFocusCourseId = pendingConflictCourseId
                                        ?.takeIf { homeDisplayWeek in pendingConflictWeeks },
                                    conflictFocusCourseKey = pendingConflictCourseKey
                                        ?.takeIf { homeDisplayWeek in pendingConflictWeeks },
                                    onResolveCourseConflict = { course, moved, week ->
                                            viewModel.updateCourseSingleWeek(course, moved, week)
                                        appScope.launch {
                                            delay(180)
                                            pendingConflictCourseId = null
                                            pendingConflictCourseKey = null
                                            pendingConflictWeeks = emptyList()
                                        }
                                    },
                                     onDeleteCourseSingleWeek = { course, week ->
                                         pendingCourseGroupDelete = emptyList()
                                         homeDialog = HomeDialog.ApplyCourseDelete(course, week)
                                     },
                                      weekEditInteractionEnabled = pickerState.phase is CustomizeUiState.Home,
                                      onScheduleLongPress = {
                                        if (pickerState.phase is CustomizeUiState.Home) {
                                            pendingHomeAnchoredOverlay = null
                                            homeAnchoredOverlayRequest = null
                                            pickerState.phase = CustomizeUiState.ShowingEntryButton
                                            showScheduleEntryPill = true
                                            prewarmCurrentScheduleSnapshot()
                                        }
                                    }
                                )
                            }
                            DockBackdropContinuityPatch(
                                config = visualState.config,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                        HomeSwitchPane(rootPageMotion, secondary = true, modifier = Modifier.fillMaxSize(),
                            pageClip = HomeSwitchClip.Page) {
                            AnimatedContent(
                                targetState = secondaryPageShowsTodo,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    if (rootPageMotion.moving) {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    } else {
                                        val motion = spring<Float>(dampingRatio = 0.88f, stiffness = 520f)
                                        EnterTransition.None togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 540f)
                                            ) { if (targetState) it else -it } + fadeOut(animationSpec = motion))
                                    }
                                },
                                label = "secondary-root-destination"
                            ) { showTodoPage ->
                            HomePageEntrance(animateOnMount = !rootPageMotion.moving, entranceDirection = if (showTodoPage) -1f else 1f) {
                            if (showTodoPage) {
                                rootPageStateHolder.SaveableStateProvider("todo") {
                                    val request = activeTodoRequest
                                    CompositionLocalProvider(LocalAdaptiveGlass provides todoAdaptiveGlassState) {
                                        TodoPlusScreen(
                                            viewModel = todoViewModel,
                                            config = todoVisualConfig,
                                            schedule = state,
                                            backdrop = backgroundBackdrop,
                                            page = screen.todoPage() ?: lastTodoPage,
                                            onPageChange = { screen = it.toScreen() },
                                            outerTransitionActive = rootPageMotion.moving,
                                            chromeActions = todoChromeActions,
                                            entryRequest = request,
                                            onEntryRequestConsumed = ::consumeTodoRequest,
                                            onRequestShizukuPermission = onRequestShizukuPermission,
                                            initialCourseId = request?.courseId,
                                            startWithNewTask = request?.startWithNewTask == true,
                                            initialTodoId = request?.todoId,
                                            onInitialCourseConsumed = {
                                                request?.let { consumeTodoRequest(it.id) }
                                            },
                                            onInitialTodoConsumed = {
                                                request?.let { consumeTodoRequest(it.id) }
                                            },
                                            requestCalendarPermission = onRequestCalendarPermission,
                                            requestAutoCalendarPermission = onRequestAutoCalendarPermission
                                        )
                                    }
                                }
                            } else rootPageStateHolder.SaveableStateProvider("settings") {
                                SettingsScreen(
                                        page = SettingsPage.Root,
                                        state = state,
                                        backdrop = backgroundBackdrop,
                                        onPageChange = {
                                            val intent = Intent(context, SettingsDetailActivity::class.java)
                                                .putExtra(SettingsDetailPageExtra, it.name)
                                            if (it == SettingsPage.Schedule) {
                                                intent.putExtra(ScheduleCustomizeIdExtra, state.config.id)
                                            }
                                            context.openRegisteredActivity(
                                                TransitionRouteId.HomeToSettingsDetail,
                                                intent
                                            )
                                        },
                                        onSave = viewModel::saveConfig,
                                        onUpdateConfig = viewModel::saveNotificationSettings,
                                        onUpdateGeneralConfig = viewModel::saveGeneralSettings,
                                        onUpdateHomeChromeBlurScale = viewModel::saveHomeChromeBlurScale,
                                        onPreviewLiveUpdate = viewModel::previewLiveUpdate,
                                        exitCommitRequest = settingsExitRequest,
                                        onExitCommitFinished = { completed ->
                                            val action = pendingSettingsExitAction
                                            pendingSettingsExitAction = null
                                            if (completed) action?.invoke()
                                        },
                                        onExitInterceptionChange = {
                                            settingsExitInterceptionRequired = it
                                        }
                                    )
                            }
                            }
                            }
                        }
                    }
                }
            if (screen !is Screen.Home) {
                showScheduleEntryPill = false
                if (pickerState.phase is CustomizeUiState.ShowingEntryButton) pickerState.phase = CustomizeUiState.Home
            }
            if (screen is Screen.Home || screen.isTodoDestination() || screen is Screen.Config) {
                DockEntranceContainer(
                    phase = startupPhase,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .zIndex(100f)
                ) {
                    FloatingDock(
                        selected = screen,
                        backdrop = chromeBackdrop,
                        config = if (screen is Screen.Home) visualState.config else state.config,
                        onHome = { requestSettingsExit { screen = Screen.Home } },
                        onConfig = {
                            screen = Screen.Config
                        },
                        onTodos = { screen = Screen.TodoTasks; todoReturnsToCourse = true },
                        onCalendar = { screen = Screen.TodoCalendar; todoReturnsToCourse = true },
                        onInsights = { screen = Screen.TodoInsights; todoReturnsToCourse = true }
                    )
                }
            }
        }
        }

        // This control intentionally lives outside the recorded home layer. It can be visible
        // while the real home is pre-captured without becoming part of its own preview bitmap.
        if (screen is Screen.Home) {
            ScheduleManagerEntryPill(
                visible = showScheduleEntryPill,
                backdrop = chromeBackdrop,
                config = visualState.config,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(89f),
                onClick = ::enterCustomizePage,
                onDismiss = {
                    entryPrewarmJob?.cancel()
                    showScheduleEntryPill = false
                    if (pickerState.phase is CustomizeUiState.ShowingEntryButton) {
                        pickerState.phase = CustomizeUiState.Home
                    }
                }
            )
        }

        pickerState.snapshotCoverBitmap?.let { cover ->
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().zIndex(90f)
            )
        }

        SchedulePickerOverlay(
            pickerState = pickerState,
            allState = allSchedulesState,
            backdrop = chromeBackdrop,
            dialogBackdrop = pickerSceneBackdrop,
            onPageSelected = ::switchPickerSchedule,
            onApply = { exitPicker(apply = true) },
            onClose = { exitPicker(apply = false) },
            onBack = { centeredId ->
                exitPicker(apply = false)
            },
            onCreate = {
                if (pickerState.phase is CustomizeUiState.Picker) {
                    snapshotJob?.cancel()
                    pickerState.phase = CustomizeUiState.CreatingCombination
                    viewModel.createSchedule(activate = false) { newId ->
                        appScope.launch {
                            snapshotFlow {
                                val latest = latestAllSchedulesState.value
                                latest.schedules.any { it.id == newId } &&
                                    latest.allConfigs.any { it.id == newId } &&
                                    latest.allPeriods.any { it.scheduleId == newId }
                            }.first { it }
                            pickerState.temporaryIds += newId
                            // Keep the currently centered card as the transition source. The new
                            // schedule is rendered underneath and revealed by the same crossfade
                            // hand-off used by Cancel, so no fake card or Pager insertion is needed.
                            pickerState.phase = CustomizeUiState.Picker
                            exitPicker(
                                apply = true,
                                targetOverride = newId,
                                commitTarget = true,
                                crossfadeToTarget = true,
                                onFinished = {
                                    quickScheduleDraft = quickDraftFor(newId)
                                }
                            )
                        }
                    }
                }
            },
            onShare = { scheduleId, shareType ->
                val profile = allSchedulesState.schedules.firstOrNull { it.id == scheduleId }
                val config = allSchedulesState.allConfigs.firstOrNull { it.id == scheduleId } ?: defaultConfig(scheduleId)
                val periods = allSchedulesState.allPeriods.filter { it.scheduleId == scheduleId }.ifEmpty { defaultPeriods(scheduleId) }
                val courses = allSchedulesState.allCourses.filter { it.scheduleId == scheduleId }
                val scheduleName = profile?.name ?: "课表"
                when (shareType) {
                    ScheduleShareType.TOKEN -> shareScheduleToken(
                        context,
                        scheduleName,
                        buildSleepDownScheduleToken(config, periods, courses)
                    )
                    ScheduleShareType.ICS -> appScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                IcsScheduleCodec.writeShareFile(context, scheduleName, config, periods, courses)
                            }
                        }.onSuccess { shareScheduleIcs(context, scheduleName, it) }
                            .onFailure { Toast.makeText(context, it.message ?: "ICS 文件生成失败", Toast.LENGTH_SHORT).show() }
                    }
                }
            },
            onCustomize = { scheduleId ->
                if (pickerState.phase is CustomizeUiState.Picker) {
                    quickScheduleDraft = quickDraftFor(scheduleId)
                }
            },
            onRename = viewModel::renameSchedule,
            onDeleteRequest = { scheduleId ->
                if (pickerState.phase is CustomizeUiState.Picker && pickerState.orderIds.size > 1) {
                    snapshotJob?.cancel()
                    pickerState.phase = CustomizeUiState.DeletingCombination
                    appScope.launch {
                        val animation = Animatable(0f)
                        animation.animateTo(1f, tween(280)) {
                            pickerState.deleteScale = 1f - value * 0.4f
                            pickerState.deleteAlpha = 1f - value
                        }
                        val deletedIndex = pickerState.orderIds.indexOf(scheduleId)
                        pickerState.orderIds.remove(scheduleId)
                        pickerState.temporaryIds.remove(scheduleId)
                        pickerState.snapshots.remove(scheduleId)
                        appScope.launch { ScheduleSnapshotStore.delete(context, scheduleId) }
                        viewModel.deleteSchedule(scheduleId)
                        val nextId = pickerState.orderIds.getOrNull(deletedIndex.coerceAtMost(pickerState.orderIds.lastIndex))
                            ?: pickerState.orderIds.firstOrNull()
                        pickerState.deletingScheduleId = null
                        pickerState.deleteReveal = 0f
                        pickerState.deleteScale = 1f
                        pickerState.deleteAlpha = 1f
                        pickerState.phase = CustomizeUiState.Picker
                        nextId?.let(::switchPickerSchedule)
                    }
                }
            },
            // Capture the complete manager (cards, title, indicators and actions) in a producer
            // that does not contain the later quick-settings sheet consumer.
            modifier = Modifier.glassBackdropProducer(pickerSceneBackdrop)
        )
    }
    } // end frozen background locals
    } // end cached home content
    } // end HomeBackgroundBlurLayer
    } // end HomeBackgroundZoomLayer

    val latestOpenHomeMenuDestination = rememberUpdatedState<(HomeMenuDestinationKind) -> Unit> {
        kind -> openHomeMenuDestination(kind)
    }
    fun releaseHomeMenuActivitySource() {
        homeMenuActivityLaunched = false
        destinationOwnsButtonReturn = false
        destinationCollapseHandedOff = false
        homeMenuSourceHidden = false
    }
    val latestOpenHomeActivityDestination =
        rememberUpdatedState<(TransitionRouteId, Intent) -> Unit> { routeId, targetIntent ->
            if (homeMenuActivityLaunched) return@rememberUpdatedState
            val activity = context.findActivity() ?: return@rememberUpdatedState
            if (homeAdaptiveMetrics.isLargeScreen) {
                // Large screens skip the heavy home morph: first retract the three-dot menu,
                // then open with the system default cross-activity transition. Returning lands
                // on the plain home with the menu already closed.
                homeMenuActivityLaunched = true
                homeAnchoredOverlayRequest = null
                appScope.launch {
                    try {
                        snapshotFlow {
                            homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Idle &&
                                homeMenuDestinationMotionState.phase == HomeAnchoredOverlayPhase.Idle
                        }.first { it }
                        if (!activity.isFinishing) {
                            ActivityTransitionCoordinator.openImmediate(
                                activity,
                                if (routeId == TransitionRouteId.HomeToCourseManagement) {
                                    TransitionRouteId.TabletHomeToCourseManagement
                                } else {
                                    TransitionRouteId.TabletHomeToEduImport
                                },
                                targetIntent
                            )
                        }
                    } finally {
                        releaseHomeMenuActivitySource()
                    }
                }
                return@rememberUpdatedState
            }
            val sharedDestinationRequest =
                currentHomeMenuDestinationRequest(HomeMenuDestinationKind.EduImport)
                    ?: return@rememberUpdatedState
            val sourceBoundsInRoot = sharedDestinationRequest.sourceBoundsInRoot
            val collapseBoundsInRoot = sharedDestinationRequest.collapseBoundsInRoot
            if (sourceBoundsInRoot.width <= 2f || sourceBoundsInRoot.height <= 2f) {
                return@rememberUpdatedState
            }
            homeMenuActivityLaunched = true
            appScope.launch {
            var openingSourceHandoffOwnedBySession = false
            var openingSourcePlaceholder: BitmapDrawable? = null
            val openingSourceOverlay = activity.window.decorView.overlay
            try {
                val fullFrame = runCatching {
                    detailScreenGraphicsLayer.toImageBitmap().asAndroidBitmap()
                }.getOrNull()
                val sourceSnapshot = fullFrame?.cropToAnchoredBounds(sourceBoundsInRoot)
                val collapseSnapshot = fullFrame?.cropToAnchoredBounds(collapseBoundsInRoot)
                // Never hide the accepted glass menu unless both the complete opening source and
                // the real top-right return button have been captured successfully.
                if (sourceSnapshot == null || collapseSnapshot == null) {
                    releaseHomeMenuActivitySource()
                    return@launch
                }
                val sourceBoundsInWindow = Rect(
                    left = sourceBoundsInRoot.left + homeRootPositionInWindow.x,
                    top = sourceBoundsInRoot.top + homeRootPositionInWindow.y,
                    right = sourceBoundsInRoot.right + homeRootPositionInWindow.x,
                    bottom = sourceBoundsInRoot.bottom + homeRootPositionInWindow.y
                )
                val collapseBoundsInWindow = Rect(
                    left = collapseBoundsInRoot.left + homeRootPositionInWindow.x,
                    top = collapseBoundsInRoot.top + homeRootPositionInWindow.y,
                    right = collapseBoundsInRoot.right + homeRootPositionInWindow.x,
                    bottom = collapseBoundsInRoot.bottom + homeRootPositionInWindow.y
                )
                openingSourcePlaceholder = BitmapDrawable(context.resources, sourceSnapshot).apply {
                    setBounds(
                        sourceBoundsInWindow.left.roundToInt(),
                        sourceBoundsInWindow.top.roundToInt(),
                        sourceBoundsInWindow.right.roundToInt(),
                        sourceBoundsInWindow.bottom.roundToInt()
                    )
                }
                val sourceHandoffAttached = runCatching {
                    openingSourceOverlay.add(checkNotNull(openingSourcePlaceholder))
                    true
                }.getOrDefault(false)
                if (!sourceHandoffAttached) {
                    releaseHomeMenuActivitySource()
                    return@launch
                }
                // Preserve the exact clicked menu until either ColorOS actually starts its spring
                // or the Legacy backend synchronously installs its own placeholder. The hidden
                // Compose frame below is still what the destination backdrop records.
                homeMenuSourceHidden = true
                withFrameNanos { }
                withFrameNanos { }
                val cleanBackground = runCatching {
                    detailScreenGraphicsLayer.toImageBitmap().asAndroidBitmap()
                }.getOrNull()
                if (cleanBackground == null) {
                    releaseHomeMenuActivitySource()
                    return@launch
                }
                val openingAnchor = TransitionAnchorFrame(
                    boundsInWindow = sourceBoundsInWindow,
                    cornerRadiusPx = with(density) { HomeAddMenuTargetCornerDp.dp.toPx() },
                    bitmap = sourceSnapshot
                )
                val returnAnchor = TransitionAnchorFrame(
                    boundsInWindow = collapseBoundsInWindow,
                    cornerRadiusPx = with(density) { 21.dp.toPx() },
                    bitmap = collapseSnapshot
                )
                val launchResult = ActivityTransitionCoordinator.open(
                    activity = activity,
                    routeId = routeId,
                    intent = targetIntent,
                    payload = TransitionPayload(
                        openingAnchor = openingAnchor,
                        returnAnchorProvider = StaticTransitionAnchorProvider(returnAnchor),
                        backgroundBitmap = cleanBackground,
                        onOpeningSourceHandoff = {
                            activity.runOnUiThread {
                                openingSourcePlaceholder?.let(openingSourceOverlay::remove)
                                openingSourcePlaceholder = null
                            }
                        },
                        // The source placeholder intentionally remains visible across the async
                        // Activity launch. Ask the public ColorOS API to alpha the source Activity
                        // leash when its native animation starts, preventing a stationary menu
                        // from remaining below the moving system bitmap.
                        nativeSourceLeashAlphaOutOnOpen = true,
                        // Source onResume happens before ColorOS finishes its CLOSE spring. Let
                        // the session-scoped backend release the hidden menu/button only after the
                        // matching native end callback (or Legacy completion/watchdog).
                        onSourceReleased = {
                            activity.runOnUiThread { releaseHomeMenuActivitySource() }
                        }
                    )
                )
                if (launchResult is TransitionLaunchResult.Failed) {
                    releaseHomeMenuActivitySource()
                    return@launch
                }
                openingSourceHandoffOwnedBySession = true
                // The Activity now owns the exact Edu-import return to the real three-dot button.
                // Silently dispose the first-level menu underneath it instead of restoring that
                // menu as a false return anchor.
                destinationOwnsButtonReturn = true
                destinationCollapseHandedOff = false
                homeAnchoredOverlayRequest = null
            } catch (_: Throwable) {
                releaseHomeMenuActivitySource()
            } finally {
                if (!openingSourceHandoffOwnedBySession) {
                    openingSourcePlaceholder?.let(openingSourceOverlay::remove)
                    openingSourcePlaceholder = null
                }
            }
        }
        }
    val latestOpenCourseManagement = rememberUpdatedState<() -> Unit> {
        latestOpenHomeActivityDestination.value(
            TransitionRouteId.HomeToCourseManagement,
            Intent(context, CourseManagementActivity::class.java)
                .putCourseManagementInitialState(state)
        )
    }
    val latestOpenEduSchoolSelect = rememberUpdatedState<() -> Unit> {
        latestOpenHomeActivityDestination.value(
            TransitionRouteId.HomeToEduImport,
            Intent(context, EduSchoolSelectActivity::class.java)
        )
    }
    val latestOpenJumpWeekDialog = rememberUpdatedState<() -> Unit> {
        pendingJumpWeekDialog = true
        homeAnchoredOverlayRequest = null
    }
    val latestOpenScheduleSettings = rememberUpdatedState<() -> Unit> {
        pendingOpenScheduleSettings = true
        homeAnchoredOverlayRequest = null
    }
    val homeAddActions = remember {
        listOf(
            AddMenuAction(R.drawable.ic_add_course, "添加单节课") {
                latestOpenHomeMenuDestination.value(HomeMenuDestinationKind.AddCourse)
            },
            AddMenuAction(R.drawable.ic_ai_import, "手动导入课表") {
                latestOpenHomeMenuDestination.value(HomeMenuDestinationKind.ManualImport)
            },
            AddMenuAction(R.drawable.ic_school_import, "教务系统导入") {
                latestOpenEduSchoolSelect.value()
            },
            AddMenuAction(R.drawable.ic_courses, "课程管理") {
                latestOpenCourseManagement.value()
            },
            AddMenuAction(R.drawable.ic_material_event, "跳转周数") {
                latestOpenJumpWeekDialog.value()
            },
            AddMenuAction(R.drawable.ic_settings, "课表设置") {
                latestOpenScheduleSettings.value()
            }
        )
    }

    LaunchedEffect(
        pendingOpenScheduleSettings,
        homeAnchoredMorphState.phase,
        screen,
        state.config.id
    ) {
        if (!pendingOpenScheduleSettings || screen !is Screen.Home) return@LaunchedEffect
        if (homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle) return@LaunchedEffect
        pendingOpenScheduleSettings = false
        if (pickerState.phase is CustomizeUiState.Home) {
            // Equivalent to long-pressing the schedule homepage and tapping the 课表设置
            // entry pill: open the in-app multi-schedule customization page directly.
            pickerState.phase = CustomizeUiState.ShowingEntryButton
            showScheduleEntryPill = true
            prewarmCurrentScheduleSnapshot()
            enterCustomizePage()
        }
    }

    LaunchedEffect(
        pendingJumpWeekDialog,
        homeAnchoredMorphState.phase,
        screen,
        state.config.id
    ) {
        if (!pendingJumpWeekDialog || screen !is Screen.Home) return@LaunchedEffect
        if (homeAnchoredMorphState.phase != HomeAnchoredOverlayPhase.Idle) return@LaunchedEffect
        pendingJumpWeekDialog = false
        // Open the jump-week dialog only after the three-dot menu has fully retracted, so the
        // dialog never overlaps the closing menu.
        jumpWeekDialogMounted = true
        jumpWeekDialogVisible = true
    }

    LaunchedEffect(sourceButtonFollowThrough) {
        val follow = sourceButtonFollowThrough ?: return@LaunchedEffect
        sourceButtonFollowProgress.snapTo(0f)
        sourceButtonFollowProgress.animateTo(
            1f,
            spring(dampingRatio = 0.86f, stiffness = 460f)
        )
        sourceButtonFollowThrough = null
    }

    fun closeHomeMenuDestination() {
        // The destination owns the legacy liquid return to the real button. Dispose the hidden
        // first-level menu without replaying its independent Closing or button follow-through.
        destinationOwnsButtonReturn = true
        destinationCollapseHandedOff = false
        homeAnchoredOverlayRequest = null
        homeMenuDestinationRequest = null
    }

    HomeAnchoredMorphOverlayHost(
        request = if (screen is Screen.Home) homeAnchoredOverlayRequest else null,
        motionState = homeAnchoredMorphState,
        backdrop = homeAnchoredOverlayBackdrop,
        config = state.config,
        addActions = homeAddActions,
        homeMode = homeMode,
        onHomeModeChange = { homeMode = it },
        adaptiveMetrics = homeAdaptiveMetrics,
        modifier = Modifier
            .zIndex(24f)
            .graphicsLayer { alpha = if (homeMenuSourceHidden) 0f else 1f },
        awaitOpeningGate = {
            awaitHomeBackgroundFrame(routeEligible = true)
        },
        onDismissRequest = {
            homeAnchoredOverlayRequest = null
        },
        onAddMenuBoundsChanged = { homeAddMenuBoundsInRoot = it },
        onSourceFollowThrough = { rect -> latestSourceFollowThrough.value(rect) },
        suppressClose = destinationOwnsButtonReturn,
        personalizePreviewProgress = personalizationPreviewProgress,
        sourceContent = { kind, sourceModifier ->
            HomeIconButtonVisual(
                backdrop = homeAnchoredOverlayBackdrop,
                config = state.config,
                iconRes = if (kind == HomeAnchoredOverlayKind.Personalize) {
                    R.drawable.ic_edit
                } else {
                    R.drawable.ic_more_horizontal
                },
                contentDescription = if (kind == HomeAnchoredOverlayKind.Personalize) {
                    "benchmark_personalize_button"
                } else {
                    "添加菜单"
                },
                modifier = sourceModifier,
                isInteractive = false
            )
        },
        personalizeContent = { panelModifier ->
            PersonalizePanel(
                modifier = panelModifier.semantics { testTag = "benchmark_personalize_panel" },
                drawSurface = false,
                rowEntranceActive = homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Opening ||
                    homeAnchoredMorphState.phase == HomeAnchoredOverlayPhase.Open,
                state = pendingVisualState,
                backdrop = homeAnchoredOverlayBackdrop,
                mode = homeMode,
                weekCardHeightScale = weekCardHeightScale,
                weekCardHeightScaleFloor = weekCardHeightScaleFloor,
                onWeekCardHeightScale = {
                    val safeScale = normalizedWeekCardHeightScale(it, weekCardHeightScaleFloor)
                    val committedConfig = mergePersonalizationCandidate(
                        current = personalizationDraftConfig ?: visualState.config,
                        candidate = visualState.config.copy(
                            weekCardHeightDp = null,
                            weekCardHeightScale = safeScale
                        ),
                        changeKey = PersonalizeWeekHeightSlider
                    )
                    weekCardHeightScale = safeScale
                    personalizationDraftConfig = committedConfig
                    personalizationPendingCommitConfig = committedConfig
                    viewModel.savePersonalization(committedConfig)
                },
                onWeekCardHeightScalePreview = {
                    weekCardHeightScale = normalizedWeekCardHeightScale(it, weekCardHeightScaleFloor)
                },
                onPickWallpaper = {
                    wallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onUpdateConfig = { changeKey, candidate ->
                    val merged = mergePersonalizationCandidate(
                        current = personalizationDraftConfig ?: visualState.config,
                        candidate = candidate,
                        changeKey = changeKey
                    )
                    personalizationDraftConfig = merged
                    personalizationPendingCommitConfig = merged
                    if (changeKey != PersonalizeWallpaperBlurSlider) {
                        personalizationPreviewState.clear(changeKey)
                    }
                    viewModel.savePersonalization(merged)
                },
                onPreviewConfig = { sliderKey, candidate ->
                    personalizationPreviewState.update(sliderKey, candidate)
                },
                previewSliderKey = personalizationSliderPreviewKey,
                previewProgress = personalizationPreviewProgress,
                onSliderPreviewActiveChange = { key, active ->
                    personalizationSliderPreviewKey = resolveActivePersonalizationSlider(
                        currentKey = personalizationSliderPreviewKey,
                        eventKey = key,
                        active = active
                    )
                }
            )
        }
    )

    // The first-level menu alone owns this final button follow-through. Destinations restore the
    // real button directly at their legacy pinch handoff and never enter this spring choreography.
    sourceButtonFollowThrough?.let { follow ->
        val rest = addButtonBounds ?: return@let
        val density = LocalDensity.current
        val buttonSizePx = with(density) { 42.dp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    val progress = sourceButtonFollowProgress.value
                    val center = Offset(
                        x = follow.startCenter.x + (rest.center.x - follow.startCenter.x) * progress,
                        y = follow.startCenter.y + (rest.center.y - follow.startCenter.y) * progress
                    )
                    IntOffset(
                        (center.x - buttonSizePx / 2f).roundToInt(),
                        (center.y - buttonSizePx / 2f).roundToInt()
                    )
                }
                .requiredSize(42.dp)
                .zIndex(300f)
        ) {
            HomeIconButtonVisual(
                backdrop = homeAnchoredOverlayBackdrop,
                config = state.config,
                iconRes = R.drawable.ic_more_horizontal,
                contentDescription = "添加菜单",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = follow.startScale + (1f - follow.startScale) * sourceButtonFollowProgress.value
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }

    if (jumpWeekDialogMounted) {
        HomeJumpWeekDialog(
            show = jumpWeekDialogVisible,
            initialWeek = homeDisplayWeek,
            totalWeeks = visualState.config.totalWeeks,
            backdrop = homeDialogBackdrop,
            config = visualState.config,
            onDismissRequest = { jumpWeekDialogVisible = false },
            onDismissFinished = { targetWeek ->
                jumpWeekDialogVisible = false
                jumpWeekDialogMounted = false
                targetWeek?.let { requestedWeek ->
                    val boundedWeek = requestedWeek.coerceIn(
                        1,
                        visualState.config.totalWeeks.coerceAtLeast(1)
                    )
                    appScope.launch {
                        if (homeMode != HomeMode.Week) {
                            homeMode = HomeMode.Week
                            // Let the current week page become the settled week surface first;
                            // the selected target then enters through the normal week transition.
                            delay(220)
                        } else {
                            withFrameNanos { }
                        }
                        homeDisplayWeek = boundedWeek
                    }
                }
            }
        )
    }

    HomeMenuDestinationOverlayHost(
        request = homeMenuDestinationRequest,
        motionState = homeMenuDestinationMotionState,
        state = state,
        backdrop = homeMenuDestinationBackdrop,
        adaptiveMetrics = homeAdaptiveMetrics,
        homeMode = homeMode,
        modifier = Modifier.zIndex(90f),
        awaitOpeningGate = { awaitHomeBackgroundFrame(routeEligible = true) },
        onDismissRequest = ::closeHomeMenuDestination,
        sourceActions = homeAddActions,
        onSourceHandoff = { homeMenuSourceHidden = true },
        onCollapseHandoff = {
            destinationCollapseHandedOff = true
        },
        onClosed = {
            homeMenuSourceHidden = false
            destinationCollapseHandedOff = false
            destinationOwnsButtonReturn = false
        },
        onAddCourses = { courses ->
            val conflictWeeks = conflictWeeksForAddedCourses(courses, state.courses, state.periods)
            if (conflictWeeks.isEmpty()) {
                viewModel.addCourses(courses)
                closeHomeMenuDestination()
            } else {
                pendingCourseGroupEdit = PendingCourseGroupEdit(emptyList(), courses)
                homeDialog = HomeDialog.ConfirmCourseConflicts(
                    original = courses.first(),
                    edited = courses.first(),
                    targetWeek = effectiveCurrentWeek(state.config),
                    conflictWeeks = conflictWeeks
                )
            }
        },
        onManualImportParsed = { draft ->
            closeHomeMenuDestination()
            appScope.launch {
                delay(HomeMenuDestinationCloseDurationMillis.toLong())
                homeDialog = HomeDialog.ConfirmImport(draft, returnDialog = null)
            }
        },
        captureHistoryBackground = {
            // History handoff is a rare explicit capture. Advance the frame key so the optimized
            // stable-frame recorder refreshes both the visible source frame and, after the source
            // is hidden, the clean background frame. Without this the callback can return the
            // previous cached menu texture even though Compose has already changed the source.
            captureRenderToken += 1
            withFrameNanos { }
            withFrameNanos { }
            runCatching {
                AiImportHistoryBackgroundCapture(
                    bitmap = detailScreenGraphicsLayer.toImageBitmap().asAndroidBitmap(),
                    rootLeftInWindow = homeRootPositionInWindow.x,
                    rootTopInWindow = homeRootPositionInWindow.y
                )
            }.getOrNull()
        },
        onEduAdapterSelected = { adapter ->
            context.openRegisteredActivity(
                TransitionRouteId.SchoolSelectToEduImport,
                Intent(context, EduImportActivity::class.java)
                    .putExtra(EduAdapterExtra, adapter.toIntentKey())
            )
        }
    )
    GlassMiuixSettingsTheme(settingsVisualConfig(state.config)) {
        QuickScheduleSettingsSheets(
            draft = quickScheduleDraft,
            config = state.config,
            // The home/chrome producers are siblings below the sheet, so this remains a real
            // liquid backdrop without ever recording the dialog that consumes it. In particular,
            // do not restore the former root-level quickSheetBackdrop: it caused the native
            // RenderThread recursion when the new-schedule sheet opened after Picker exit.
            backdrop = if (pickerState.overlayVisible) pickerSceneBackdrop else chromeBackdrop,
            onDraftChange = { quickScheduleDraft = it },
            onDismiss = { quickScheduleDraft = null },
            onDismissFinished = {
                // Direct customization leaves the manager below the sheet. New-schedule setup
                // still returns through the existing home-to-picker Morph after its sheet closes.
                if (!pickerState.overlayVisible) {
                    pickerState.phase = CustomizeUiState.Home
                    enterCustomizePage()
                }
            },
            onSave = { draft, onSaved ->
                val latest = latestAllSchedulesState.value
                val baseConfig = latest.allConfigs.firstOrNull { it.id == draft.scheduleId }
                    ?: return@QuickScheduleSettingsSheets
                val totalWeeks = draft.totalWeeks.coerceIn(1, 60)
                val manualWeek = draft.currentWeek.coerceIn(1, totalWeeks)
                val datedConfig = baseConfig.copy(
                    totalWeeks = totalWeeks,
                    currentWeek = manualWeek,
                    termStartDate = draft.termStartDate.ifBlank { null },
                    autoCurrentWeek = draft.autoCurrentWeek,
                    hideEmptyWeekends = draft.hideEmptyWeekends
                )
                val periods = latest.allPeriods.filter { it.scheduleId == draft.scheduleId }
                    .ifEmpty { defaultPeriods(draft.scheduleId) }
                viewModel.saveConfigForSchedule(
                    draft.scheduleId,
                    // The automatic display week is derived at runtime. Persist the
                    // selected fallback instead of writing a clamped pre-term week 1.
                    datedConfig.copy(currentWeek = manualWeek),
                    periods,
                    onSaved
                )
            },
            suppressDetailedButton = detailMorphState !is DetailMorphState.Idle,
            onDetailedSettings = { scheduleId, sourceBoundsInWindow, saveBeforeOpening ->
                // The detailed page is the real cross-activity SettingsDetailActivity opened
                // through the shared transition framework (QuickSheetToSettingsDetail route).
                if (detailMorphState !is DetailMorphState.Idle) {
                    return@QuickScheduleSettingsSheets
                }
                val activity = context.findActivity() ?: return@QuickScheduleSettingsSheets
                val detailInputNanos = System.nanoTime()
                appScope.launch {
                    suspend fun capturePopupFrame(): Bitmap? {
                        detailPopupCaptureActive = true
                        val requestedToken = detailPopupCaptureToken + 1
                        detailPopupCaptureToken = requestedToken
                        var waitedFrames = 0
                        while (
                            detailPopupCapturedToken.get() != requestedToken &&
                            waitedFrames < 4
                        ) {
                            withFrameNanos { }
                            waitedFrames += 1
                        }
                        if (detailPopupCapturedToken.get() != requestedToken) return null
                        return runCatching {
                            detailPopupGraphicsLayer.toImageBitmap().asAndroidBitmap()
                        }.getOrNull()
                    }

                    // Let the released press state reach the source window before copying its
                    // final compositor output. This is the authoritative path for the custom
                    // schedule picker, whose nested RenderNodes cannot be flattened reliably by
                    // recording only the outer Compose layer.
                    withFrameNanos { }
                    val windowSnapshot = captureDetailMorphWindowSnapshot(
                        activity = activity,
                        rootPositionInWindow = homeRootPositionInWindow,
                        rootSize = homeReadabilityRootSize
                    )
                    val layeredSnapshot = if (windowSnapshot == null) {
                        val sourceUnderlaySnapshot = runCatching {
                            detailScreenGraphicsLayer.toImageBitmap().asAndroidBitmap()
                        }.getOrNull()
                        val sourcePopupSnapshot = try {
                            capturePopupFrame()
                        } finally {
                            detailPopupCaptureActive = false
                        }
                        if (sourceUnderlaySnapshot != null && sourcePopupSnapshot != null) {
                            composeDetailMorphSnapshot(sourceUnderlaySnapshot, sourcePopupSnapshot)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    val fullSnapshot = windowSnapshot?.bitmap ?: layeredSnapshot
                    // This source is not a normal page: the home underlay and QuickSheet are two
                    // sibling draw layers in the root Miuix Scaffold. Preserve their already
                    // composed frame for the destination-side depth/blur renderer; cross-window
                    // blur cannot reconstruct that Compose backdrop topology on ColorOS.
                    val snapshotOriginInWindow = windowSnapshot?.originInWindow
                        ?: homeRootPositionInWindow
                    val sourceBoundsInSnapshot = Rect(
                        left = sourceBoundsInWindow.left - snapshotOriginInWindow.x,
                        top = sourceBoundsInWindow.top - snapshotOriginInWindow.y,
                        right = sourceBoundsInWindow.right - snapshotOriginInWindow.x,
                        bottom = sourceBoundsInWindow.bottom - snapshotOriginInWindow.y
                    )
                    val buttonSnapshot = fullSnapshot?.cropToAnchoredBounds(sourceBoundsInSnapshot)
                    com.xiaomanjun.sleepdownschedule.glass.GlassBackendTrace.durationSince(
                        "Detail.InputThroughCapture", detailInputNanos
                    )
                    val openingAnchor = TransitionAnchorFrame(
                        boundsInWindow = sourceBoundsInWindow,
                        cornerRadiusPx = with(density) { 25.dp.toPx() },
                        bitmap = buttonSnapshot
                    )
                    // The destination must not race the draft write. Keeping the real button in
                    // place while saving gives the translucent destination an unchanged live
                    // underlay and ensures the first detailed-settings composition sees the
                    // committed values.
                    val detailSaveNanos = System.nanoTime()
                    saveBeforeOpening {
                        com.xiaomanjun.sleepdownschedule.glass.GlassBackendTrace.durationSince(
                            "Detail.Save", detailSaveNanos
                        )
                        appScope.launch {
                            com.xiaomanjun.sleepdownschedule.glass.GlassBackendTrace.durationSince(
                                "Detail.InputToOpenDispatch", detailInputNanos
                            )
                            ActivityTransitionCoordinator.open(
                                activity = activity,
                                routeId = TransitionRouteId.QuickSheetToSettingsDetail,
                                intent = Intent(activity, QuickSheetSettingsDetailActivity::class.java)
                                    .putExtra(SettingsDetailPageExtra, SettingsPage.Schedule.name)
                                    .putExtra(ScheduleCustomizeIdExtra, scheduleId),
                                payload = TransitionPayload(
                                    openingAnchor = openingAnchor,
                                    returnAnchorProvider = StaticTransitionAnchorProvider(openingAnchor),
                                    backgroundBitmap = fullSnapshot
                                )
                            )
                        }
                    }
                }
            }
        )

        // Keep the course editor inside the same root stack as the stock MIUIX popup
        // host. The host stays in its 1.0.6 position and therefore preserves the manager
        // quick sheet's original backdrop, colors and animation, while its later draw
        // order still places picker dialogs above the editor shell.
        CourseEditorContainerOverlayHost(
            request = courseEditorRequest,
            state = state,
            backdrop = courseEditorBackdrop,
            config = state.config,
            adaptiveMetrics = homeAdaptiveMetrics,
            modifier = Modifier.zIndex(100f),
            suspended = screen !is Screen.Home,
            awaitOpeningGate = { awaitHomeBackgroundFrame(routeEligible = true) },
            onManageCourseTodos = { courseId, startNewTask ->
                localTodoRequestId -= 1L
                pendingLocalTodoRequest = TodoEntryRequest(
                    id = localTodoRequestId,
                    courseId = courseId,
                    startWithNewTask = startNewTask,
                    returnToCourse = true
                )
                todoReturnsToCourse = true
                screen = Screen.TodoTasks
            },
            onDismissRequest = { closeCourseEditor() },
            onCopy = { courses, onResult ->
                val sourceRequest = courseEditorRequest
                viewModel.copyCourses(courses) { success ->
                    onResult(success)
                    if (success && courseEditorRequest === sourceRequest) {
                        val destination = courses.first()
                        appScope.launch {
                            val visibleWeeks = destination.weeks.filter {
                                weekCourseBuckets(listOf(destination), it).visibleCourses.isNotEmpty()
                            }
                            val destinationWeek = sourceRequest?.targetWeek?.takeIf { it in visibleWeeks }
                                ?: visibleWeeks.firstOrNull() ?: homeDisplayWeek
                            val targetBuckets = weekCourseBuckets(state.courses + courses, destinationWeek)
                            val weekdays = visibleWeekdaysForBuckets(targetBuckets, state.config.hideEmptyWeekends)
                            val destinationBounds = sourceRequest?.sourceGrid?.reveal(destination, weekdays)
                                ?: sourceRequest?.sourceBoundsInRoot?.let { source ->
                                    courseEditorWeekLandingBounds(
                                        source, sourceRequest.course, destination,
                                        state.periods.map { it.periodIndex },
                                        with(density) { weekCardHeight.dp.toPx() },
                                        with(density) { 4.dp.toPx() }
                                    )
                                }
                            if (courseEditorRequest === sourceRequest) {
                                homeDisplayWeek = destinationWeek
                                courseEditorMotionState.retractTo(destinationBounds, destination)
                                closeCourseEditor()
                            }
                        }
                    }
                }
            },
            onSave = { originals, editedCourses, targetWeek ->
                val original = originals.singleOrNull()
                val edited = editedCourses.singleOrNull()
                if (original != null && edited != null && courseWeeksChanged(original, edited)) {
                    val conflictWeeks = conflictWeeksForEditedCourse(
                        original,
                        edited,
                        state.courses,
                        state.periods
                    )
                    if (conflictWeeks.isEmpty()) {
                        viewModel.updateCourse(edited)
                        closeCourseEditor()
                    } else {
                        homeDialog = HomeDialog.ConfirmCourseConflicts(
                            original,
                            edited,
                            targetWeek ?: effectiveCurrentWeek(state.config),
                            conflictWeeks
                        )
                    }
                } else if (original != null && edited != null) {
                    // Keep the editor fully mounted behind the choice dialog. Closing it first
                    // lets its Morph/interaction shield cover and stall the confirmation.
                    homeDialog = HomeDialog.ApplyCourseEdit(original, edited, targetWeek ?: effectiveCurrentWeek(state.config))
                } else {
                    val conflictWeeks = conflictWeeksForEditedCourseGroup(
                        originals,
                        editedCourses,
                        state.courses,
                        state.periods
                    )
                    if (conflictWeeks.isEmpty()) {
                        viewModel.replaceCourseGroup(originals, editedCourses)
                        closeCourseEditor()
                    } else {
                        pendingCourseGroupEdit = PendingCourseGroupEdit(originals, editedCourses)
                        homeDialog = HomeDialog.ConfirmCourseConflicts(
                            originals.first(),
                            editedCourses.first(),
                            targetWeek ?: effectiveCurrentWeek(state.config),
                            conflictWeeks
                        )
                    }
                }
            },
            onDelete = { courses, targetWeek ->
                pendingCourseGroupDelete = courses.takeIf { it.size > 1 }.orEmpty()
                homeDialog = HomeDialog.ApplyCourseDelete(
                    courses.first(),
                    targetWeek ?: effectiveCurrentWeek(state.config)
                )
            },
            motionState = courseEditorMotionState,
            onRenderedCourseIdChange = {
                courseEditorRenderedCourseId = it
                if (it == null) courseEditorFlightRegistry.frozen = false
            },
            onPhaseChange = {}
        )

    }

    remoteExperience.agreement?.let { agreement ->
        LiquidAlertDialog(
            title = agreement.title,
            message = if (agreement.forceReaccept) {
                "协议或政策已经更新，请查看新版本并确认同意后继续使用。"
            } else {
                "首次使用前，请查看并同意此协议或政策。"
            },
            actions = listOf(
                LiquidAlertAction("查看正文", LiquidAlertActionStyle.Secondary) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, agreement.url.toUri()))
                    }
                },
                LiquidAlertAction("同意", LiquidAlertActionStyle.Primary) {
                    SleepDownRemoteConfig.acceptAgreement(context, agreement)
                }
            ),
            backdrop = chromeBackdrop,
            config = state.config,
            onDismissRequest = {}
        )
    } ?: remoteExperience.notice?.let { notice ->
        LiquidAlertDialog(
            title = notice.title,
            message = notice.content,
            actions = listOf(
                LiquidAlertAction("知道了", LiquidAlertActionStyle.Primary) {
                    SleepDownRemoteConfig.markNoticeShown(context, notice)
                }
            ),
            backdrop = chromeBackdrop,
            config = state.config,
            onDismissRequest = {
                SleepDownRemoteConfig.markNoticeShown(context, notice)
            }
        )
    }

    if (remoteExperience.agreement == null && remoteExperience.notice == null) {
        upcomingHoliday?.let { holiday ->
            LiquidAlertDialog(
                title = "临近${holiday.name}",
                message = buildString {
                    append("${holiday.name}假期 ")
                    append(formatScheduleDate(holiday.start))
                    if (holiday.end != holiday.start) append(" – ${formatScheduleDate(holiday.end)}")
                    append("，是否现在设置调休课表？")
                    append("停课当天的课程会保留为灰色卡片，补课日按原课程日期上课。")
                },
                actions = listOf(
                    LiquidAlertAction("稍后再说", LiquidAlertActionStyle.Secondary) {
                        upcomingHoliday = null
                    },
                    LiquidAlertAction("去设置", LiquidAlertActionStyle.Primary) {
                        upcomingHoliday = null
                        context.openRegisteredActivity(
                            TransitionRouteId.SettingsToScheduleAdjustments,
                            ScheduleAdjustmentsActivity.intent(
                                context,
                                latestHomeConfig,
                                latestHomeConfig.scheduleAdjustmentsJson
                            ),
                            launchActivity = { holidayAdjustmentsLauncher.launch(it) }
                        )
                    }
                ),
                backdrop = chromeBackdrop,
                config = state.config,
                onDismissRequest = { upcomingHoliday = null }
            )
        }
    }

    (renderedHomeDialog as? HomeDialog.EditWallpaper)?.let { dialog ->
        WallpaperEditorOverlay(
            uri = dialog.uri,
            entrySnapshot = dialog.entrySnapshot,
            config = visualState.config,
            backdrop = chromeBackdrop,
            visible = homeDialogVisible,
            adaptiveMetrics = homeAdaptiveMetrics,
            onCancel = { dismissHomeDialog() },
            onApply = { nextConfig ->
                viewModel.savePersonalization(nextConfig)
                dismissHomeDialog()
            },
            modifier = Modifier.zIndex(280f)
        )
    }

    // Dialog-based dialogs for all other types (including EditCourse without a source card)
    renderedHomeDialog?.let { dialog ->
        if (dialog !is HomeDialog.EditWallpaper && (dialog !is HomeDialog.EditCourse || dialog.course == null)) {
        if (dialog is HomeDialog.ApplyCourseEdit) {
            ApplyCourseEditDialog(
                original = dialog.original,
                edited = dialog.edited,
                backdrop = homeDialogBackdrop,
                config = state.config,
                onSingle = {
                    val conflictWeeks = conflictWeeksForSingleWeekEdit(
                        dialog.original,
                        dialog.edited,
                        dialog.targetWeek,
                        state.courses,
                        state.periods
                    )
                    if (conflictWeeks.isEmpty()) {
                        viewModel.updateCourseSingleWeek(
                            dialog.original,
                            dialog.edited,
                            dialog.targetWeek
                        )
                        dismissHomeDialog()
                        closeCourseEditor()
                    } else {
                        homeDialog = HomeDialog.ConfirmCourseConflicts(
                            dialog.original,
                            dialog.edited,
                            dialog.targetWeek,
                            conflictWeeks,
                            singleWeekOnly = true
                        )
                    }
                },
                onAll = {
                    val scope = courseApplyAllScope(dialog.original, dialog.edited, state.courses)
                    val conflictWeeks = if (scope.originals.size == 1) {
                        conflictWeeksForEditedCourse(dialog.original, scope.edited, state.courses, state.periods)
                    } else {
                        conflictWeeksForEditedCourseGroup(scope.originals, listOf(scope.edited), state.courses, state.periods)
                    }
                    if (conflictWeeks.isEmpty()) {
                        if (scope.originals.size == 1) viewModel.updateCourse(scope.edited)
                        else viewModel.replaceCourseGroup(scope.originals, listOf(scope.edited))
                        dismissHomeDialog()
                        closeCourseEditor()
                    } else {
                        pendingCourseGroupEdit = scope.takeIf { it.originals.size > 1 }?.let {
                            PendingCourseGroupEdit(it.originals, listOf(it.edited))
                        }
                        homeDialog = HomeDialog.ConfirmCourseConflicts(
                            dialog.original,
                            scope.edited,
                            dialog.targetWeek,
                            conflictWeeks
                        )
                    }
                },
                onCancel = { dismissHomeDialog() }
            )
        } else if (dialog is HomeDialog.ConfirmCourseConflicts) {
            CourseConflictRetentionDialog(
                course = dialog.edited,
                conflictWeeks = dialog.conflictWeeks,
                backdrop = homeDialogBackdrop,
                config = state.config,
                onKeepTemporarily = keepConflict@{
                    pendingCourseGroupEdit?.let { groupEdit ->
                        if (groupEdit.originals.isEmpty()) {
                            viewModel.addCourses(groupEdit.edited)
                            pendingCourseGroupEdit = null
                            pendingConflictCourseId = null
                            pendingConflictCourseKey = groupEdit.edited.firstOrNull()?.occurrenceOverrideKey()
                            pendingConflictWeeks = dialog.conflictWeeks
                            dismissHomeDialog()
                            closeHomeMenuDestination()
                            val conflictWeek = dialog.conflictWeeks.first()
                            appScope.launch {
                                delay(HomeMenuDestinationCloseDurationMillis.toLong() + 32L)
                                homeDisplayWeek = conflictWeek
                                homeMode = HomeMode.Week
                            }
                            return@keepConflict
                        }
                        val editorWasOpen = courseEditorRequest != null
                        viewModel.replaceCourseGroup(groupEdit.originals, groupEdit.edited)
                        pendingCourseGroupEdit = null
                        pendingConflictCourseId = groupEdit.edited.firstOrNull()?.id?.takeIf { it > 0 }
                        pendingConflictCourseKey = groupEdit.edited.firstOrNull()?.occurrenceOverrideKey()
                        pendingConflictWeeks = dialog.conflictWeeks
                        dismissHomeDialog()
                        closeCourseEditor()
                        val conflictWeek = dialog.conflictWeeks.first()
                        if (editorWasOpen) {
                            appScope.launch {
                                delay(CourseEditorCloseDurationMillis.toLong() + 32L)
                                homeDisplayWeek = conflictWeek
                                homeMode = HomeMode.Week
                            }
                        } else {
                            homeDisplayWeek = conflictWeek
                            homeMode = HomeMode.Week
                        }
                        return@keepConflict
                    }
                    val editorWasOpen = courseEditorRequest != null
                    val editedRetractBounds = courseEditorRequest
                        ?.sourceBoundsInRoot
                        ?.takeIf { homeMode == HomeMode.Week }
                        ?.let { source ->
                            val periodIndexes = state.periods.map { it.periodIndex }
                            val originalStart = dialog.original.periods
                                .map(periodIndexes::indexOf)
                                .filter { it >= 0 }
                                .minOrNull()
                            val editedPositions = dialog.edited.periods
                                .map(periodIndexes::indexOf)
                                .filter { it >= 0 }
                                .distinct()
                                .sorted()
                            val editedStart = editedPositions.firstOrNull()
                            if (originalStart == null || editedStart == null) {
                                null
                            } else {
                                val cardHeightPx = with(density) { weekCardHeight.dp.toPx() }
                                val cardGapPx = with(density) { 4.dp.toPx() }
                                val left = source.left +
                                    (dialog.edited.weekday - dialog.original.weekday) *
                                    (source.width + cardGapPx)
                                val top = source.top +
                                    (editedStart - originalStart) * cardHeightPx
                                val targetHeight = (
                                    cardHeightPx * editedPositions.size.coerceAtLeast(1) -
                                        cardGapPx
                                    ).coerceAtLeast(with(density) { 18.dp.toPx() })
                                Rect(left, top, left + source.width, top + targetHeight)
                            }
                        }
                    courseEditorMotionState.retractTo(editedRetractBounds)
                    if (dialog.singleWeekOnly) {
                        viewModel.updateCourseSingleWeek(
                            dialog.original,
                            dialog.edited,
                            dialog.targetWeek
                        )
                    } else {
                        viewModel.updateCourse(dialog.edited)
                    }
                    pendingConflictCourseId =
                        dialog.edited.id.takeUnless { dialog.singleWeekOnly }
                    pendingConflictCourseKey = dialog.edited.occurrenceOverrideKey()
                    pendingConflictWeeks = dialog.conflictWeeks
                    dismissHomeDialog()
                    closeCourseEditor()
                    val conflictWeek = dialog.conflictWeeks.first()
                    if (editorWasOpen) {
                        appScope.launch {
                            delay(CourseEditorCloseDurationMillis.toLong() + 32L)
                            homeDisplayWeek = conflictWeek
                            homeMode = HomeMode.Week
                        }
                    } else {
                        homeDisplayWeek = conflictWeek
                        homeMode = HomeMode.Week
                    }
                },
                onReturn = {
                    if (pendingCourseGroupEdit != null) {
                        pendingCourseGroupEdit = null
                        dismissHomeDialog()
                    } else {
                        homeDialog = HomeDialog.ApplyCourseEdit(
                            dialog.original,
                            dialog.edited,
                            dialog.targetWeek
                        )
                    }
                }
            )
        } else if (dialog is HomeDialog.ApplyCourseDelete) {
            ApplyCourseDeleteDialog(
                course = dialog.course,
                backdrop = homeDialogBackdrop,
                config = state.config,
                onSingle = {
                    deleteHomeCourses(pendingCourseGroupDelete.ifEmpty { listOf(dialog.course) }, dialog.targetWeek)
                },
                onAll = {
                    deleteHomeCourses(pendingCourseGroupDelete.ifEmpty { listOf(dialog.course) }, null)
                },
                onCancel = {
                    pendingCourseGroupDelete = emptyList()
                    dismissHomeDialog()
                }
            )
        } else {
        Dialog(onDismissRequest = { dismissHomeDialog() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            if (dialog is HomeDialog.ImportSchedule) {
                val dialogView = LocalView.current
                DisposableEffect(dialogView) {
                    val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                    val previousSoftInputMode = dialogWindow?.attributes?.softInputMode
                    dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    onDispose {
                        previousSoftInputMode?.let { dialogWindow.setSoftInputMode(it) }
                    }
                }
            }
            val animatedDialogContent: @Composable () -> Unit = {
                AnimatedVisibility(
                    visible = homeDialogVisible,
                    enter = popEnterTransition(),
                    exit = popExitTransition()
                ) {
                val dialogContent: @Composable () -> Unit = {
                    when (dialog) {
                    is HomeDialog.EditCourse -> {
                        val editor: @Composable () -> Unit = {
                            NormalizedCourseEditorScreen(
                                state = state,
                                initialCourse = dialog.course,
                                onCancel = { dismissHomeDialog() },
                                onSave = {
                                    if (dialog.course == null) {
                                        viewModel.addCourse(it)
                                        dismissHomeDialog()
                                    } else if (courseWeeksChanged(dialog.course, it)) {
                                        viewModel.updateCourse(it)
                                        dismissHomeDialog()
                                    } else {
                                        homeDialog = HomeDialog.ApplyCourseEdit(dialog.course, it, dialog.targetWeek ?: effectiveCurrentWeek(state.config))
                                    }
                                },
                                onDelete = {
                                    homeDialog = HomeDialog.ApplyCourseDelete(it, dialog.targetWeek ?: effectiveCurrentWeek(state.config))
                                },
                                backdrop = homeDialogBackdrop,
                                pickerRenderInRootScaffold = dialog.course != null
                            )
                        }
                        if (dialog.course == null) {
                            // Keep the Android dialog itself under its original centered constraints.
                            // Only the editor content owns a local overlay host, so wheel pickers can
                            // cover the form without changing the glass shell's size or position.
                            top.yukonga.miuix.kmp.basic.Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = ComposeColor.Transparent,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0)
                            ) {
                                editor()
                            }
                        } else {
                            editor()
                        }
                    }
                    HomeDialog.ImportSchedule -> NormalizedAiManualImportScreen(
                        state = state,
                        backdrop = homeDialogBackdrop,
                        initialFileUri = assistantImportUri,
                        onInitialFileConsumed = { assistantImportUri = null },
                        initialText = assistantImportText,
                        onInitialTextConsumed = { assistantImportText = null },
                        onCancel = { dismissHomeDialog() },
                        onParsed = { homeDialog = HomeDialog.ConfirmImport(it, returnDialog = null) }
                    )
                    HomeDialog.EduImport -> Unit
                    is HomeDialog.EditWallpaper -> Unit
                    is HomeDialog.ConfirmImport -> ConfirmScheduleScreen(
                        draft = dialog.draft,
                        backdrop = homeDialogBackdrop,
                        onCancel = { homeDialog = dialog.returnDialog },
                        onDraftChanged = { revised ->
                            homeDialog = dialog.copy(draft = revised)
                        },
                        onConfirm = { createNewSchedule ->
                            viewModel.importDraft(dialog.draft, createNewSchedule) { scheduleId ->
                                dismissHomeDialog()
                                if (dialog.draft.source == ImportDraftSource.AI_EDU) {
                                    screen = Screen.Home
                                    pendingImportedSetupId = null
                                } else {
                                    pendingImportedSetupId = scheduleId
                                }
                            }
                        }
                    )
                            is HomeDialog.ApplyCourseEdit -> ApplyCourseEditDialog(
                        original = dialog.original,
                        edited = dialog.edited,
                        backdrop = homeDialogBackdrop,
                        config = state.config,
                        onSingle = {
                            val conflictWeeks = conflictWeeksForSingleWeekEdit(
                                dialog.original,
                                dialog.edited,
                                dialog.targetWeek,
                                state.courses,
                                state.periods
                            )
                            if (conflictWeeks.isEmpty()) {
                                viewModel.updateCourseSingleWeek(
                                    dialog.original,
                                    dialog.edited,
                                    dialog.targetWeek
                                )
                                dismissHomeDialog()
                            } else {
                                homeDialog = HomeDialog.ConfirmCourseConflicts(
                                    dialog.original,
                                    dialog.edited,
                                    dialog.targetWeek,
                                    conflictWeeks,
                                    singleWeekOnly = true
                                )
                            }
                        },
                            onAll = {
                            val conflictWeeks = conflictWeeksForEditedCourse(
                                dialog.original,
                                dialog.edited,
                                state.courses,
                                state.periods
                            )
                            if (conflictWeeks.isEmpty()) {
                                viewModel.updateCourse(dialog.edited)
                                dismissHomeDialog()
                            } else {
                                homeDialog = HomeDialog.ConfirmCourseConflicts(
                                    dialog.original,
                                    dialog.edited,
                                    dialog.targetWeek,
                                    conflictWeeks
                                )
                            }
                        },
                        onCancel = {
                            dismissHomeDialog()
                        }
                    )
                    is HomeDialog.ConfirmCourseConflicts -> Unit
                    is HomeDialog.ApplyCourseDelete -> ApplyCourseDeleteDialog(
                        course = dialog.course,
                        backdrop = homeDialogBackdrop,
                        config = state.config,
                        onSingle = {
                            deleteHomeCourses(listOf(dialog.course), dialog.targetWeek)
                        },
                        onAll = {
                            deleteHomeCourses(listOf(dialog.course), null)
                        },
                        onCancel = {
                            dismissHomeDialog()
                            openCourseEditor(dialog.course, dialog.targetWeek, null)
                        }
                    )
                    }
                }
                CenterLiquidDialog(
                        backdrop = homeDialogBackdrop,
                        config = state.config,
                        modifier = Modifier,
                        size = if (dialog is HomeDialog.ImportSchedule) {
                            LiquidDialogSize.Compact
                        } else {
                            LiquidDialogSize.Standard
                        },
                        followGlassContrast = dialog is HomeDialog.ImportSchedule
                    ) {
                        dialogContent()
                    }
                }
            }
            animatedDialogContent()
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
private fun HomeBackgroundZoomLayer(
    zoom: () -> Float,
    dimProgress: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .drawWithContent {
                drawContent()
                // Draw after the blurred/zoomed home, outside its cached sampling layers.
                // One shared scrim avoids stacking darkness when an overlay hands off to another.
                val dimAlpha = 0.08f * dimProgress().coerceIn(0f, 1f)
                if (dimAlpha > 0f) drawRect(ComposeColor.Black.copy(alpha = dimAlpha))
            }
            .graphicsLayer {
                val currentZoom = zoom()
                scaleX = currentZoom
                scaleY = currentZoom
            },
        content = content
    )
}

internal val HomeTopOverlayHeight = 178.dp
private val DetailTopBarHeight = SleepDownDesignTokens.SecondaryPage.CompactTopBarHeight
private val DetailTopOverlayExtra = SleepDownDesignTokens.SecondaryPage.TopOverlayExtra
private val DetailContentTopGap = SleepDownDesignTokens.SecondaryPage.ContentTopGap
internal val LightTopBarButtonShadow = Shadow(
    radius = 7.dp,
    offset = DpOffset(0.dp, 1.dp),
    color = ComposeColor.Black.copy(alpha = 0.10f)
)
internal val HomeInitialTopInset = 122.dp

@Composable
private fun HomeBackgroundBlurLayer(
    blurProgress: () -> Float,
    useFrozenHomeScene: () -> Boolean,
    closing: () -> Boolean,
    sceneKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    // Auto composition: this is just a display-list wrapper, never another full-screen texture.
    // Its alpha can hide the sharp pass without changing the scene shared by foreground glass.
    val clearSceneLayer = rememberGraphicsLayer()
    val frozenBlurLayer = rememberGraphicsLayer()
    val frozenRecordKey = remember { AtomicReference<Any?>(null) }
    val frozenRecordSize = remember { AtomicReference(IntSize.Zero) }
    val sampleScale = HomeFrozenBlurSampleScale
    val maximumBlurPx = with(density) { 22.dp.toPx() }
    val frozenBlurEffects = remember(maximumBlurPx, sampleScale) {
        List(HomeLiveBlurStepCount + 1) { index ->
            if (index == 0) null else {
                val radius = maximumBlurPx * sampleScale * index / HomeLiveBlurStepCount.toFloat()
                BlurEffect(radius, radius, TileMode.Clamp)
            }
        }
    }
    // The live path is kept for idle Home and per-frame personalization preview. Reuse a small
    // bounded set of RenderEffect instances so those exceptional paths no longer compile a new
    // full-screen blur effect for every animation frame.
    val liveBlurEffects = remember(maximumBlurPx) {
        List(HomeLiveBlurStepCount + 1) { index ->
            if (index == 0) null else {
                val radius = maximumBlurPx * index / HomeLiveBlurStepCount.toFloat()
                BlurEffect(radius, radius, TileMode.Clamp)
            }
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                val frozenHomeScene = useFrozenHomeScene()
                val progress = blurProgress().coerceIn(0f, 1f)
                val isClosing = closing()
                val step = quantizeHomeBackgroundBlurStep(progress, isClosing)
                if (frozenHomeScene) {
                    renderEffect = null
                    val blurAlpha = homeFrozenBlurAlpha(progress)
                    clearSceneLayer.alpha = if (blurAlpha < 1f) 1f else 0f
                    frozenBlurLayer.alpha = blurAlpha
                    frozenBlurLayer.renderEffect =
                        frozenBlurEffects[step.coerceAtLeast(HomeFrozenBlurMinimumStep)]
                } else {
                    renderEffect = liveBlurEffects[step]
                    clearSceneLayer.alpha = 0f
                    frozenBlurLayer.alpha = 0f
                    frozenBlurLayer.renderEffect = null
                }
            }
            .drawWithContent {
                // Animation state is read only by graphicsLayer above. A zoom/blur/alpha tick
                // changes RenderNode properties without invalidating this display list.
                val frozenHomeScene = useFrozenHomeScene()
                if (!frozenHomeScene) {
                    frozenRecordKey.set(null)
                    drawContent()
                    return@drawWithContent
                }

                val fullSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                val reducedSize = IntSize(
                    width = (size.width * sampleScale).roundToInt().coerceAtLeast(1),
                    height = (size.height * sampleScale).roundToInt().coerceAtLeast(1)
                )
                // Both wrappers refer to the same retained scene. Record in Preparing, including
                // at zero blur, rather than doing this work on the first moving frame. The blur
                // has one fixed allocation from Opening through Closing; no bitmap readback or
                // blur pyramid is created. Rounded sample dimensions map back to exact source
                // dimensions so the sharp/blurred layers stay aligned on odd-sized windows.
                if (
                    frozenRecordKey.get() != sceneKey ||
                    frozenRecordSize.get() != fullSize
                ) {
                    val scaleX = reducedSize.width / size.width
                    val scaleY = reducedSize.height / size.height
                    clearSceneLayer.record(size = fullSize) {
                        this@drawWithContent.drawContent()
                    }
                    frozenBlurLayer.record(size = reducedSize) {
                        withTransform({
                            scale(
                                scaleX = scaleX,
                                scaleY = scaleY,
                                pivot = Offset.Zero
                            )
                        }) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    frozenBlurLayer.pivotOffset = Offset.Zero
                    frozenBlurLayer.scaleX = 1f / scaleX
                    frozenBlurLayer.scaleY = 1f / scaleY
                    frozenRecordKey.set(sceneKey)
                    frozenRecordSize.set(fullSize)
                }

                drawLayer(clearSceneLayer)
                drawLayer(frozenBlurLayer)
            },
        content = content
    )
}

@Composable
private fun rootTopBarLayoutHeight(
    screen: Screen,
    boundlessWeekHeader: Boolean = false
): Dp {
    return when (screen) {
        Screen.Home -> {
            val metrics = rememberHomeAdaptiveMetrics()
            if (boundlessWeekHeader) {
                // Measure the whole visual gradient envelope, not just the header: otherwise
                // the parent's maximum height truncates the blur before its fade-out tail.
                boundlessHomeTopGradientHeight(metrics)
            } else {
                metrics.topOverlayHeight
            }
        }
        Screen.TodoTasks,
        Screen.TodoCalendar,
        Screen.TodoInsights,
        Screen.Config -> detailTopOverlayHeight()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TodoRootTopBar(
    page: TodoPage,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    actions: TodoChromeActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .height(66.dp)
            .graphicsLayer { clip = false }
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxSize(),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor.Transparent),
            title = {}
        )
        Text(
            text = when (page) {
                TodoPage.Tasks -> "待办事项"
                TodoPage.Calendar -> "日历"
                TodoPage.Insights -> "效率洞察"
            },
            modifier = Modifier.align(Alignment.CenterStart)
                .padding(start = 20.dp, end = 112.dp).homeSwitchGroup(0f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = LocalAdaptiveGlass.current.contentColor,
            maxLines = 1
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(top = 2.dp, end = 8.dp).homeSwitchGroup(0f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group management was removed; retain only the existing AI and create actions.
            HomeIconButton(
                backdrop = backdrop,
                config = config,
                iconRes = R.drawable.ic_ai_import,
                contentDescription = "AI 配置",
                selected = false,
                onClick = { _ -> actions.onConfigureAi() }
            )
            HomeIconButton(
                backdrop = backdrop,
                config = config,
                iconRes = R.drawable.ic_add_course,
                contentDescription = "添加待办",
                selected = false,
                onClick = { _ -> actions.onAddTodo() }
            )
        }
    }
}

@Composable
internal fun detailTopOverlayHeight(): Dp {
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.safeDrawing.only(WindowInsetsSides.Top).getTop(this).toDp() }
    return statusTop + DetailTopBarHeight + DetailTopOverlayExtra
}

@Composable
internal fun detailContentTopPadding(): Dp {
    LocalGlassSettingsContentTopPadding.current?.let { return it }
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.safeDrawing.only(WindowInsetsSides.Top).getTop(this).toDp() }
    return statusTop + DetailTopBarHeight + DetailContentTopGap
}

@Composable
fun DetailActivityScaffold(
    title: String,
    config: ScheduleConfigEntity,
    onBack: () -> Unit,
    useSettingsBackground: Boolean = false,
    showTopGradientBlur: Boolean = true,
    isolateContentFromBackdrop: Boolean = false,
    compactTopBar: Boolean = false,
    centerCompactTitle: Boolean = false,
    compactTitleMatchesSettings: Boolean = false,
    topBarVisible: Boolean = true,
    preserveStatusBarSpace: Boolean = false,
    topBarBackdropOverride: Backdrop? = null,
    topBarActions: @Composable (Backdrop?) -> Unit = {},
    content: @Composable (Backdrop?) -> Unit
) {
    val pageConfig = if (useSettingsBackground) settingsVisualConfig(config) else config
    val wallpaperImages by rememberHomeWallpaperImages(pageConfig)
    val expectedWallpaperKey = homeWallpaperRenderKey(pageConfig, appUsesDarkTheme(pageConfig))
    val noWallpaperResolved = wallpaperImages.renderKey == expectedWallpaperKey && wallpaperImages.source == null
    GlassMiuixDetailActivityScaffold(
        title = title,
        config = pageConfig,
        useSettingsBackground = useSettingsBackground,
        backgroundContent = {
            if (useSettingsBackground) {
                Box(
                    Modifier.fillMaxSize()
                        .background(settingsPageBackground(pageConfig))
                )
            } else if (wallpaperImages.source != null) {
                HomeWallpaper(
                    config = pageConfig,
                    images = wallpaperImages,
                    phase = StartupPhase.FullQuality,
                    isActive = true
                )
                WallpaperGlassSamplingToneOverlay(pageConfig)
            } else {
                HomeBackdropFallback(
                    noWallpaper = !pageConfig.hasAnyWallpaper() || noWallpaperResolved
                )
            }
        },
        backgroundOverlayContent = {
            if (!useSettingsBackground && wallpaperImages.source != null) {
                WallpaperToneOverlay(pageConfig)
            }
        },
        onBack = onBack,
        showTopGradientBlur = showTopGradientBlur,
        isolateContentFromBackdrop = isolateContentFromBackdrop,
        compactTopBar = compactTopBar,
        centerCompactTitle = centerCompactTitle,
        compactTitleMatchesSettings = compactTitleMatchesSettings,
        topBarVisible = topBarVisible,
        preserveStatusBarSpace = preserveStatusBarSpace,
        topBarBackdropOverride = topBarBackdropOverride,
        topBarActions = topBarActions,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    onBack: () -> Unit,
    centerTitle: Boolean = false,
    useMiuixCollapsedTitleStyle: Boolean = false,
    showBackButton: Boolean = true,
    backButtonStartPadding: Dp = 16.dp,
    statusTopOverride: Dp? = null,
    actions: @Composable () -> Unit = {}
) {
    val density = LocalDensity.current
    val statusTop = statusTopOverride
        ?: with(density) { WindowInsets.safeDrawing.only(WindowInsetsSides.Top).getTop(this).toDp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusTop + DetailTopBarHeight)
            .graphicsLayer { clip = false }
    ) {
        if (centerTitle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DetailTopBarHeight)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { clip = false }
            ) {
                if (showBackButton) {
                    TopBackButton(
                        backdrop = backdrop,
                        config = config,
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = backButtonStartPadding)
                            .size(SleepDownDesignTokens.SecondaryPage.BackButtonSize)
                    )
                }
                Text(
                    title,
                    style = if (useMiuixCollapsedTitleStyle) {
                        MiuixTheme.textStyles.title3
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = if (useMiuixCollapsedTitleStyle) {
                        FontWeight.Medium
                    } else {
                        FontWeight.SemiBold
                    },
                    color = if (useMiuixCollapsedTitleStyle) {
                        MiuixTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    lineHeight = if (useMiuixCollapsedTitleStyle) {
                        MiuixTheme.textStyles.title3.fontSize
                    } else {
                        MaterialTheme.typography.titleLarge.fontSize
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center).padding(
                        horizontal = SleepDownDesignTokens.SecondaryPage.CenterTitleSideClearance
                    )
                )
            }
        } else Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DetailTopBarHeight)
                .align(Alignment.BottomCenter)
                .padding(start = backButtonStartPadding, end = 16.dp)
                .graphicsLayer { clip = false },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                TopBackButton(
                    backdrop = backdrop,
                    config = config,
                    onClick = onBack,
                    modifier = Modifier.size(SleepDownDesignTokens.SecondaryPage.BackButtonSize)
                )
            }
            Box(
                modifier = Modifier
                    .height(SleepDownDesignTokens.SecondaryPage.BackButtonSize)
                    .padding(start = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = MaterialTheme.typography.titleLarge.fontSize,
                    textAlign = TextAlign.Center
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(DetailTopBarHeight)
                .padding(end = backButtonStartPadding)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.CenterEnd
        ) { actions() }
    }
}

@Composable
fun settingsPageBackground(config: ScheduleConfigEntity): ComposeColor {
    return if (appUsesDarkTheme(config)) ComposeColor.Black else ComposeColor(0xFFEDEEF3)
}

@Composable
fun settingsVisualConfig(config: ScheduleConfigEntity): ScheduleConfigEntity {
    return config.copy(
        wallpaperUri = null,
        wallpaperBrightness = 1f,
        homeTextLight = appUsesDarkTheme(config),
        defaultWallpaperStyle = DefaultWallpaperStyle.NONE
    )
}

// Size the tail from the actual header, rather than keeping the old 230dp minimum that
// spreads full-strength header blur too far into the first courses on compact windows.
private fun boundlessHomeTopGradientHeight(metrics: HomeAdaptiveMetrics): Dp =
    metrics.safeTop + 66.dp + BoundlessWeekHeaderRowHeight + 36.dp

@Composable
fun HomeTopGradientBlur(
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    height: Dp = HomeTopOverlayHeight,
    fullBlurHeight: Dp = 0.dp
) {
    val lightGlass = glassUsesLightStyle(config)
    val tintColor = if (lightGlass) HomeLightGlassGradientColor else ComposeColor(0xFF111111)
    Box(modifier = modifier) {
        ProgressiveBackdropBlur(
            backdrop = backdrop,
            tintColor = tintColor,
            height = height,
            blurRadius = 7.dp,
            // Pure blur: no white/dark tint base. A faint tint remains only as the API<33 fallback
            // brush (the runtime shader path is driven by tintIntensity = 0).
            tintIntensity = 0f,
            direction = ProgressiveBlurDirection.TopToBottom,
            // The Nexio radius curve needs its own plateau; alpha-mask parameters alone do not
            // stop the blur radius from shrinking before the boundless weekday/date header.
            radiusFadeStart = (fullBlurHeight.value / height.value).coerceIn(0f, 0.95f),
            topMaskFadeStart = 0.85f,
            topMaskFadeEnd = 1f,
            fallbackTintStops = listOf(
                0f to tintColor.copy(alpha = if (lightGlass) 0.12f else 0.16f),
                0.6f to tintColor.copy(alpha = if (lightGlass) 0.05f else 0.07f),
                1f to ComposeColor.Transparent
            )
        )
        // Wallpaper brightness: the backdrop sampled above already carries the glass sampling dim,
        // but the remainder dim (WallpaperToneOverlay) is kept outside the backdrop producers so
        // course cards stay bright. Re-apply that exact remainder inside the gradient zone so the
        // chrome does not read brighter ("发白") than the dimmed wallpaper around it.
        val brightness = config.wallpaperBrightness.coerceIn(0.35f, 1f)
        val totalDim = (1f - brightness).coerceIn(0f, 0.65f)
        val samplingDim = wallpaperGlassSamplingDim(brightness)
        val remainderDim = if (samplingDim >= 0.999f) {
            0f
        } else {
            ((totalDim - samplingDim) / (1f - samplingDim)).coerceIn(0f, 0.65f)
        }
        if (remainderDim > 0.001f) {
            val dimColor = ComposeColor.Black.copy(alpha = remainderDim)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to dimColor,
                                0.85f to dimColor.copy(alpha = dimColor.alpha * 0.6f),
                                1f to ComposeColor.Transparent
                            )
                        )
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppTopBar(
    screen: Screen,
    state: AppState,
    settingsPage: SettingsPage,
    backdrop: Backdrop?,
    homeMode: HomeMode,
    onHomeModeChange: (HomeMode) -> Unit,
    homeDisplayDate: LocalDate,
    homeDisplayWeek: Int,
    beforeScheduleTerm: Boolean,
    afterScheduleTerm: Boolean,
    homeShowingAnotherWeek: Boolean,
    onReturnHomeToCurrentWeek: () -> Unit,
    activeHomeOverlay: HomeAnchoredOverlayKind?,
    addButtonHidden: Boolean,
    onAddButtonPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onPersonalizeButtonPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onToggleAddMenu: (Float) -> Unit,
    onTogglePersonalize: (Float) -> Unit,
    onBackHome: () -> Unit
) {
    val adaptiveTopBarColor = LocalAdaptiveGlass.current.contentColor
    val homeTextColor = adaptiveTopBarColor
    if (screen is Screen.Home) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                .height(66.dp)
                .graphicsLayer { clip = false }
        ) {
            // Keep Material's bar shell, but render both the title and animated liquid controls as
            // siblings so neither is clipped by TopAppBar's internal single-line title layout.
            TopAppBar(
                modifier = Modifier.fillMaxSize(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor.Transparent,
                    scrolledContainerColor = ComposeColor.Transparent,
                    titleContentColor = homeTextColor,
                    actionIconContentColor = homeTextColor,
                    navigationIconContentColor = homeTextColor
                ),
                title = {}
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 120.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                HomeDateTitle(
                    state = state,
                    displayDate = if (homeMode != HomeMode.Week) homeDisplayDate else LocalDate.now(),
                    displayWeek = if (homeMode != HomeMode.Week) effectiveCurrentWeek(state.config, homeDisplayDate) else homeDisplayWeek,
                    showTwoDays = false,
                    beforeScheduleTerm = beforeScheduleTerm,
                    afterScheduleTerm = afterScheduleTerm,
                    showReturnToCurrentWeekHint = homeMode == HomeMode.Week && homeDisplayWeek != effectiveCurrentWeek(state.config),
                    showWeather = homeMode == HomeMode.Week,
                    onReturnCurrent = onReturnHomeToCurrentWeek
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    // Match Material TopAppBar's 4dp action inset plus the existing row inset.
                    .padding(top = 2.dp, end = 8.dp)
                    .excludeHomeAssistantPull()
                    .graphicsLayer { clip = false },
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeIconButton(
                    backdrop = backdrop,
                    config = state.config,
                    iconRes = R.drawable.ic_edit,
                    contentDescription = "benchmark_personalize_button",
                    selected = false,
                    visible = activeHomeOverlay != HomeAnchoredOverlayKind.Personalize,
                    onClick = onTogglePersonalize,
                    onButtonPositioned = onPersonalizeButtonPositioned
                )
                HomeIconButton(
                    backdrop = backdrop,
                    config = state.config,
                    iconRes = R.drawable.ic_more_horizontal,
                    contentDescription = "添加菜单",
                    selected = false,
                    visible = !addButtonHidden,
                    onClick = onToggleAddMenu,
                    onButtonPositioned = onAddButtonPositioned
                )
            }
        }
        return
    }
    TopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).height(66.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeColor.Transparent,
            scrolledContainerColor = ComposeColor.Transparent,
            titleContentColor = if (screen is Screen.Home) homeTextColor else MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = if (screen is Screen.Home) homeTextColor else MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = if (screen is Screen.Home) homeTextColor else MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    when (settingsPage) {
                        SettingsPage.Root -> "设置"
                        SettingsPage.General -> "通用设置"
                        SettingsPage.LiquidGlass -> "液态玻璃"
                        SettingsPage.Widgets -> "小组件设置"
                        SettingsPage.AiImport -> "AI 设置"
                        SettingsPage.DayAgent -> "AI助理"
                        SettingsPage.Schedule -> "课表详细设置"
                        SettingsPage.AutoRefreshSchedule -> "自动刷新课表"
                        SettingsPage.Notifications -> "通知设置"
                        SettingsPage.ScheduleManager -> "课表设置"
                        SettingsPage.BackupRestore -> "备份与恢复"
                        SettingsPage.BackupPreview -> "恢复预览"
                        SettingsPage.About -> "关于应用"
                        SettingsPage.Changelog -> "关于应用"
						SettingsPage.PrivacyPolicy -> "隐私政策"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp
                )
            }
        },
        navigationIcon = {
            if (screen is Screen.Config && settingsPage != SettingsPage.Root) {
                TopBackButton(
                    backdrop = backdrop,
                    config = state.config,
                    onClick = onBackHome,
                    modifier = Modifier.padding(start = 16.dp).size(42.dp)
                )
            }
        },
        actions = {}
    )
}

private fun popEnterTransition(): EnterTransition =
    fadeIn(animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)) +
        scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f))

private fun popExitTransition(): ExitTransition =
    fadeOut(animationSpec = spring(dampingRatio = 0.92f, stiffness = 560f)) +
        scaleOut(targetScale = 0.94f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 560f))

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun CourseBoundsSource(
    courseId: Long,
    visible: Boolean,
    sharedScope: SharedTransitionScope?,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    content(modifier.then(if (visible) Modifier else Modifier.graphicsLayer { alpha = 0f }))
}

@Composable
fun TopBackButton(backdrop: Backdrop?, config: ScheduleConfigEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TopGlassIconButton(
        backdrop = backdrop,
        config = config,
        iconRes = R.drawable.ic_arrow_back,
        contentDescription = "返回",
        onClick = onClick,
        modifier = modifier,
        buttonHeight = 42.dp
    )
}

@Composable
fun TopGlassIconButton(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceColorOverride: ComposeColor? = null,
    buttonHeight: Dp = 42.dp
) {
    AppGlassIconButton(backdrop, config, contentDescription, onClick, modifier,
        size = buttonHeight, surfaceOverride = surfaceColorOverride) {
        Icon(painterResource(iconRes), contentDescription = null,
            modifier = Modifier.size(SleepDownDesignTokens.Button.IconSize))
    }
}

@Composable
private fun HomeActionCapsule(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    homeMode: HomeMode,
    visible: Boolean,
    onPersonalize: (Float) -> Unit,
    onMore: (Float) -> Unit,
    onCapsulePositioned: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    val view = LocalView.current
    val pressSnapshot = remember { LiquidButtonPressSnapshot() }
    fun capturedScale(): Float = 1f + (3f / 42f) * pressSnapshot.progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            // Keep the visual right edge on the same inset as the Today Agent surface. Width
            // changes therefore trim only the leading side instead of shifting the shared edge.
            .padding(end = if (homeMode == HomeMode.Week) 4.dp else 7.dp)
            .size(width = 100.dp, height = 42.dp)
            .onGloballyPositioned { onCapsulePositioned(it.boundsInRoot()) }
            .graphicsLayer { alpha = if (visible) 1f else 0f }
    ) {
        HomeActionCapsuleVisual(
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxSize(),
            isInteractive = visible,
            pressSnapshot = pressSnapshot,
            onPersonalize = {
                performButtonHaptic(view)
                onPersonalize(capturedScale())
            },
            onMore = {
                performButtonHaptic(view)
                onMore(capturedScale())
            }
        )
    }
}

@Composable
internal fun HomeActionCapsuleVisual(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    pressSnapshot: LiquidButtonPressSnapshot? = null,
    onPersonalize: () -> Unit = {},
    onMore: () -> Unit = {}
) {
    val adaptiveGlass = LocalAdaptiveGlass.current
    val lightGlass = adaptiveGlass.lightGlass
    val personalizeInteraction = remember { MutableInteractionSource() }
    val moreInteraction = remember { MutableInteractionSource() }
    val interactionModifier: (String, MutableInteractionSource, () -> Unit) -> Modifier =
        { description, interaction, action ->
        Modifier
            .fillMaxHeight()
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = isInteractive,
                onClick = action
            )
    }
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = interactionModifier(
                    "benchmark_personalize_button",
                    personalizeInteraction,
                    onPersonalize
                ).let { Modifier.weight(1f).then(it) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = adaptiveGlass.contentColor
                )
            }
            Box(
                modifier = interactionModifier("添加菜单", moreInteraction, onMore)
                    .let { Modifier.weight(1f).then(it) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_more_horizontal),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = adaptiveGlass.contentColor
                )
            }
        }
    }
    if (backdrop != null) {
        LiquidButton(
            onClick = {},
            backdrop = backdrop,
            modifier = modifier,
            isInteractive = isInteractive,
            clickTargetEnabled = false,
            height = 42.dp,
            contentPadding = PaddingValues(0.dp),
            blurRadius = homeChromeBlur(HomeHeaderGlassBlur, config),
            lensHeight = HomeHeaderGlassLensHeight,
            lensAmount = HomeHeaderGlassLensAmount,
            chromaticAberration = false,
            shadowEnabled = lightGlass,
            shadowStyle = LightTopBarButtonShadow,
            pressExpansion = 3.dp,
            highlightRadiusMultiplier = 0.9f,
            pressSnapshot = pressSnapshot,
            surfaceColor = (
                if (lightGlass) HomeLightGlassSurfaceColor else ComposeColor(0xFF121212)
                ).copy(alpha = homeChromeGlassSurfaceAlpha(lightGlass))
        ) { content() }
    } else {
        GlassPill(backdrop = null, config = config, modifier = modifier) { content() }
    }
}

@Composable
fun HomeIconButton(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accentColor: ComposeColor = ComposeColor.Unspecified,
    visible: Boolean = true,
    onClick: (Float) -> Unit,
    onButtonPositioned: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val view = LocalView.current
    val pressSnapshot = remember { LiquidButtonPressSnapshot() }
    fun capturedScale(): Float = 1f + (3f / 42f) * pressSnapshot.progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .padding(end = 7.dp)
            .size(42.dp)
            .then(
                if (onButtonPositioned != null) {
                    Modifier.onGloballyPositioned { onButtonPositioned(it.boundsInRoot()) }
                } else {
                    Modifier
                }
            )
            .graphicsLayer { alpha = if (visible) 1f else 0f }
    ) {
        HomeIconButtonVisual(
            backdrop = backdrop,
            config = config,
            iconRes = iconRes,
            contentDescription = contentDescription,
            selected = selected,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize(),
            isInteractive = visible,
            pressSnapshot = pressSnapshot,
            onClick = {
                performButtonHaptic(view)
                onClick(capturedScale())
            }
        )
    }
}

@Composable
internal fun HomeIconButtonVisual(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: ComposeColor = ComposeColor.Unspecified,
    isInteractive: Boolean = true,
    pressSnapshot: LiquidButtonPressSnapshot? = null,
    onClick: () -> Unit = {}
) {
    val adaptiveGlass = LocalAdaptiveGlass.current
    val lightGlass = adaptiveGlass.lightGlass
    val baseSurfaceColor = if (lightGlass) HomeLightGlassSurfaceColor else ComposeColor(0xFF121212)
    val buttonSurfaceColor = if (accentColor.isSpecified) {
        accentColor.copy(alpha = if (lightGlass) HomeLightGlassAccentTintAlpha else 0.32f)
    } else {
        baseSurfaceColor.copy(alpha = homeChromeGlassSurfaceAlpha(lightGlass))
    }
    if (backdrop != null) {
        LiquidButton(
            onClick = if (isInteractive) onClick else ({}),
            backdrop = backdrop,
            modifier = modifier,
            height = 42.dp,
            tint = if (accentColor.isSpecified) accentColor else ComposeColor.Unspecified,
            surfaceColor = buttonSurfaceColor,
            contentPadding = PaddingValues(0.dp),
            blurRadius = homeChromeBlur(HomeHeaderGlassBlur, config),
            lensHeight = HomeHeaderGlassLensHeight,
            lensAmount = HomeHeaderGlassLensAmount,
            chromaticAberration = false,
            shadowEnabled = lightGlass,
            shadowStyle = LightTopBarButtonShadow,
            pressExpansion = 3.dp,
            pressSnapshot = pressSnapshot
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp),
                    tint = adaptiveGlass.contentColor
                )
            }
        }
    } else {
        GlassPill(
            backdrop = null,
            config = config,
            modifier = modifier,
            selected = selected,
            onClick = if (isInteractive) onClick else ({})
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), contentDescription = contentDescription, modifier = Modifier.size(20.dp))
            }
        }
    }
}

data class AddMenuAction(
    val iconRes: Int? = null,
    val label: String,
    val imageVector: ImageVector? = null,
    val iconTint: ComposeColor? = null,
    val textTint: ComposeColor? = null,
    val onClick: () -> Unit
)

private fun Bitmap.cropToAnchoredBounds(bounds: Rect): Bitmap? = runCatching {
    val left = bounds.left.roundToInt().coerceIn(0, width - 1)
    val top = bounds.top.roundToInt().coerceIn(0, height - 1)
    val cropWidth = bounds.width.roundToInt().coerceIn(1, width - left)
    val cropHeight = bounds.height.roundToInt().coerceIn(1, height - top)
    Bitmap.createBitmap(this, left, top, cropWidth, cropHeight)
}.getOrNull()

@Composable
fun AddMenuLiquidItem(
    config: ScheduleConfigEntity,
    action: AddMenuAction,
    itemHeight: Dp,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    compactCapsule: Boolean = false
) {
    val baseText = glassForegroundColor(config)
    val selectedColor = ComposeColor(0xFF8E8E93).copy(
        alpha = if (glassUsesLightStyle(config)) 0.20f else 0.28f
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight - 2.dp)
                .clip(if (compactCapsule) Capsule() else RoundedRectangle(HomeAddMenuSelectionCornerDp.dp))
                .background(if (highlighted) selectedColor else ComposeColor.Transparent)
                .padding(horizontal = if (compactCapsule) 14.dp else 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactCapsule) 12.dp else 10.dp)
            ) {
                Icon(
                    action.imageVector?.let { rememberVectorPainter(it) }
                        ?: painterResource(requireNotNull(action.iconRes)),
                    contentDescription = null,
                    modifier = Modifier.size(if (compactCapsule) 20.dp else 21.dp),
                    tint = action.iconTint ?: baseText
                )
                Text(
                    action.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = action.textTint ?: baseText,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingDock(
    selected: Screen,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
    onHome: () -> Unit,
    onConfig: () -> Unit,
    onTodos: () -> Unit,
    onCalendar: () -> Unit = {},
    onInsights: () -> Unit = {}
) {
    val adaptiveGlass = LocalAdaptiveGlass.current
    val settingsDestination = selected is Screen.Config
    val lightGlass = if (settingsDestination) !appUsesDarkTheme(config) else adaptiveGlass.lightGlass
    val selectedAccent = dockSelectedAccentColor(lightGlass)
    val foreground = if (settingsDestination) {
        if (appUsesDarkTheme(config)) ComposeColor.White else ComposeColor.Black
    } else {
        adaptiveGlass.contentColor
    }
    val density = LocalDensity.current
    // MIUI reports the IME-sized bottom edge through safeDrawing for the underlying Activity
    // while a transparent Compose Dialog owns the keyboard. That makes a bottom-aligned Dock jump
    // above the keyboard even though the Activity itself uses adjustNothing. Use the stable
    // navigation-bar inset as the physical screen edge; the separate IME compensation still
    // handles devices that genuinely resize the Activity window.
    val systemBottomPx = if (previewMode) {
        0
    } else {
        WindowInsets.navigationBarsIgnoringVisibility
            .only(WindowInsetsSides.Bottom)
            .getBottom(density)
    }
    val bottomInset = with(density) { systemBottomPx.toDp() }
    val imeCompensationPx = dockImeCompensationPx(
        imeBottomPx = if (previewMode) 0 else WindowInsets.ime.getBottom(density),
        systemBottomPx = systemBottomPx
    )
    val bottomOffset = (bottomInset + 8.dp).coerceAtLeast(8.dp)
    val dockTextColor by animateColorAsState(
        targetValue = foreground,
        animationSpec = tween(220),
        label = "FloatingDockContentTheme"
    )
    val dockAlignment = when (config.dockAlignment) {
        DockAlignment.LEFT -> Alignment.BottomStart
        DockAlignment.CENTER -> Alignment.BottomCenter
        DockAlignment.RIGHT -> Alignment.BottomEnd
    }
    val horizontalPadding = when (config.dockAlignment) {
        DockAlignment.LEFT -> PaddingValues(start = 18.dp, bottom = bottomOffset)
        DockAlignment.CENTER -> PaddingValues(start = 18.dp, end = 18.dp, bottom = bottomOffset)
        DockAlignment.RIGHT -> PaddingValues(end = 18.dp, bottom = bottomOffset)
    }
    val dockWidthModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().excludeHomeAssistantPull()
    val selectedTab = selected.dockIndex()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontalPadding)
            // MainActivity keeps adjustResize for text fields. Counter only the IME portion
            // here so the home dock stays at its physical bottom position behind the keyboard.
            .graphicsLayer { translationY = imeCompensationPx.toFloat() },
        contentAlignment = dockAlignment
    ) {
        if (backdrop != null) {
            CompositionLocalProvider(LocalContentColor provides dockTextColor) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = { index -> when (index) {
                        0 -> onHome()
                        1 -> onTodos()
                        2 -> onCalendar()
                        3 -> onInsights()
                        else -> onConfig()
                    } },
                    backdrop = backdrop,
                    tabsCount = 5,
                    modifier = dockWidthModifier.semantics { testTag = "benchmark_home_dock" },
                    containerHeight = 54.dp,
                    indicatorHeight = 46.dp,
                    blurRadius = homeChromeBlur(1.3.dp, config),
                    containerAlpha = homeChromeGlassSurfaceAlpha(lightGlass),
                    lensHeight = 10.dp,
                    lensAmount = 40.dp,
                    indicatorWidthOverflow = 8.dp,
                    indicatorHeightOverflow = 4.dp,
                    indicatorLensHeight = 12.dp,
                    indicatorLensAmount = 17.dp,
                    officialHighlightAlpha = 0.07f,
                    officialShadowAlpha = 0.05f,
                    officialInnerShadowAlpha = 0.08f,
                    chromaticAberrationEnabled = true,
                    isLightThemeOverride = lightGlass,
                    lightContainerColor = HomeLightGlassSurfaceColor,
                    lightAccentColor = HomeLightGlassSelectedAccentColor,
                    darkAccentColor = dockSelectedAccentColor(lightGlass = false),
                    useOfficialGlassParameters = true
                ) {
                    LiquidBottomTab(onClick = onHome) {
                        DockTabContent(R.drawable.ic_courses, "课程", iconSize = 23.dp)
                    }
                    LiquidBottomTab(onClick = onTodos) {
                        DockTabContent(R.drawable.ic_check, "待办", iconSize = 22.dp)
                    }
                    LiquidBottomTab(onClick = onCalendar) {
                        DockVectorTabContent(Icons.Default.CalendarMonth, "日历")
                    }
                    LiquidBottomTab(onClick = onInsights) {
                        DockVectorTabContent(Icons.Default.Insights, "洞察")
                    }
                    LiquidBottomTab(onClick = onConfig) {
                        DockTabContent(R.drawable.ic_settings, "设置", iconSize = 24.dp)
                    }
                }
            }
        } else {
            GlassPill(backdrop = null, config = config, modifier = dockWidthModifier) {
                Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    DockFallbackItem(selectedTab == 0, "课程", dockTextColor, selectedAccent, R.drawable.ic_courses, onClick = onHome)
                    DockFallbackItem(selectedTab == 1, "待办", dockTextColor, selectedAccent, R.drawable.ic_check, onClick = onTodos)
                    DockFallbackItem(selectedTab == 2, "日历", dockTextColor, selectedAccent, icon = Icons.Default.CalendarMonth, onClick = onCalendar)
                    DockFallbackItem(selectedTab == 3, "洞察", dockTextColor, selectedAccent, icon = Icons.Default.Insights, onClick = onInsights)
                    DockFallbackItem(selectedTab == 4, "设置", dockTextColor, selectedAccent, R.drawable.ic_settings, onClick = onConfig)
                }
            }
        }
    }
}

@Composable
private fun DockVectorTabContent(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun RowScope.DockFallbackItem(
    selected: Boolean,
    label: String,
    foreground: ComposeColor,
    selectedAccent: ComposeColor,
    iconRes: Int? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 720f),
        label = "dock-tab-press"
    )
    Column(
        Modifier.weight(1f).fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(Capsule())
            .background(if (selected) selectedAccent.copy(alpha = 0.15f) else ComposeColor.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(21.dp), tint = foreground)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = foreground)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = foreground, maxLines = 1, softWrap = false)
    }
}

@Composable
fun ScheduleManagerEntryPill(
    visible: Boolean,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }
    LaunchedEffect(visible) {
        if (visible) {
            delay(5_000)
            onDismiss()
        }
    }
    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).getBottom(this).toDp() }
    val dockBottomOffset = (bottomInset + 8.dp).coerceAtLeast(8.dp)
    val pillBottomOffset = dockBottomOffset + 68.dp
    val dockAlignment = when (config.dockAlignment) {
        DockAlignment.LEFT -> Alignment.BottomStart
        DockAlignment.CENTER -> Alignment.BottomCenter
        DockAlignment.RIGHT -> Alignment.BottomEnd
    }
    val horizontalPadding = when (config.dockAlignment) {
        DockAlignment.LEFT -> PaddingValues(start = 18.dp, bottom = pillBottomOffset)
        DockAlignment.CENTER -> PaddingValues(start = 18.dp, end = 18.dp, bottom = pillBottomOffset)
        DockAlignment.RIGHT -> PaddingValues(end = 18.dp, bottom = pillBottomOffset)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontalPadding),
        contentAlignment = dockAlignment
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(320)) +
                scaleIn(initialScale = 0.4f, animationSpec = tween(320, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f))),
            exit = fadeOut(animationSpec = tween(120)) +
                scaleOut(targetScale = 0.4f, animationSpec = tween(120))
        ) {
            val lightGlass = glassUsesLightStyle(config)
            if (backdrop != null) {
                LiquidButton(
                    onClick = onClick,
                    backdrop = backdrop,
                    modifier = Modifier.width(140.dp),
                    height = 48.dp,
                    blurRadius = 2.dp,
                    lensHeight = 12.dp,
                    lensAmount = 24.dp,
                        surfaceColor = if (lightGlass) HomeLightGlassSurfaceColor.copy(alpha = 0.075f) else ComposeColor(0xFF121212).copy(alpha = 0.075f),
                        chromaticAberration = false,
                        shadowEnabled = false,
                        highlightEnabled = true,
                        contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Text(
                        "课表设置",
                        color = glassForegroundColor(config),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            } else {
                GlassPill(
                    backdrop = null,
                    config = config,
                    modifier = Modifier
                        .width(140.dp)
                        .height(48.dp),
                    onClick = onClick
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "课表设置",
                            color = glassForegroundColor(config),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DockBackdropContinuityPatch(config: ScheduleConfigEntity, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).getBottom(this).toDp() }
    val height = (bottomInset + 92.dp).coerceAtLeast(92.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(ComposeColor.Transparent)
    )
}

@Composable
private fun DockTabContent(iconRes: Int, label: String, iconSize: Dp) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(ComposeColor.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(iconSize))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

internal const val PersonalizeWallpaperBlurSlider = "wallpaper-blur"
internal const val PersonalizeWallpaperBrightnessSlider = "wallpaper-brightness"
private const val PersonalizeWallpaperContentChange = "wallpaper-content"
private const val PersonalizeWeekHeightSlider = "week-height"
internal const val PersonalizeWeekCornerSlider = "week-corner"
private const val PersonalizeCardColorChange = "card-color"
internal const val PersonalizeCardAlphaSlider = "card-alpha"
internal const val PersonalizeCardBlurSlider = "card-blur"
internal const val PersonalizeCardRefractionSlider = "card-refraction"
internal const val PersonalizeCardFontSlider = "card-font"
private const val PersonalizeCardGlassChange = "card-glass"
private const val PersonalizeCardOutlineLightChange = "card-outline-light"
private const val PersonalizeCardGaussianBlurChange = "card-gaussian-blur"
internal const val PersonalizeCardColoredTextChange = "card-colored-text"

internal fun resolveActivePersonalizationSlider(
    currentKey: String?,
    eventKey: String,
    active: Boolean
): String? = when {
    active -> eventKey
    currentKey == eventKey -> null
    else -> currentKey
}

internal fun mergePersonalizationCandidate(
    current: ScheduleConfigEntity,
    candidate: ScheduleConfigEntity,
    changeKey: String
): ScheduleConfigEntity = when (changeKey) {
    PersonalizeWallpaperContentChange -> current.copy(
        wallpaperUri = candidate.wallpaperUri,
        wallpaperPortraitCenterX = candidate.wallpaperPortraitCenterX,
        wallpaperPortraitCenterY = candidate.wallpaperPortraitCenterY,
        wallpaperPortraitScale = candidate.wallpaperPortraitScale,
        wallpaperLandscapeCenterX = candidate.wallpaperLandscapeCenterX,
        wallpaperLandscapeCenterY = candidate.wallpaperLandscapeCenterY,
        wallpaperLandscapeScale = candidate.wallpaperLandscapeScale,
        wallpaperSourceWidth = candidate.wallpaperSourceWidth,
        wallpaperSourceHeight = candidate.wallpaperSourceHeight,
        homeTextLight = candidate.homeTextLight
    )
    PersonalizeWallpaperBlurSlider -> current.copy(wallpaperBlur = candidate.wallpaperBlur)
    PersonalizeWallpaperBrightnessSlider -> current.copy(
        wallpaperBrightness = candidate.wallpaperBrightness
    )
    PersonalizeWeekHeightSlider -> current.copy(
        weekCardHeightDp = null,
        weekCardHeightScale = candidate.weekCardHeightScale
    )
    PersonalizeWeekCornerSlider -> current.copy(
        weekCardCornerProgress = candidate.weekCardCornerProgress
    )
    PersonalizeCardColorChange -> current.copy(
        cardColorArgb = candidate.cardColorArgb,
        courseCardColorMode = candidate.courseCardColorMode,
        courseCardPalette = if (candidate.courseCardColorMode == CourseCardColorMode.COLORFUL) {
            candidate.courseCardPalette
        } else {
            current.courseCardPalette
        }
    )
    PersonalizeCardAlphaSlider -> current.copy(cardAlpha = candidate.cardAlpha)
    PersonalizeCardBlurSlider -> current.copy(courseCardBlur = candidate.courseCardBlur)
    PersonalizeCardRefractionSlider -> current.copy(
        courseCardRefractionStrength = candidate.courseCardRefractionStrength
    )
    PersonalizeCardFontSlider -> current.copy(courseCardFontScale = candidate.courseCardFontScale)
    PersonalizeCardGlassChange -> current.switchCourseCardGlassMode(
        candidate.courseCardGlassEnabled
    )
    PersonalizeCardOutlineLightChange -> current.copy(
        courseCardOutlineLightEnabled = candidate.courseCardOutlineLightEnabled
    )
    PersonalizeCardGaussianBlurChange -> current.copy(
        courseCardGaussianBlurEnabled = candidate.courseCardGaussianBlurEnabled
    )
    PersonalizeCardColoredTextChange -> current.copy(
        courseCardColoredTextEnabled = candidate.courseCardColoredTextEnabled
    )
    else -> current
}

private fun Modifier.personalizePreviewVisibility(
    activeKey: String?,
    previewProgress: Float,
    ownKey: String? = null
): Modifier {
    val selectedSlider = activeKey != null && activeKey == ownKey
    val effectiveAlpha = if (selectedSlider) 1f else 1f - previewProgress.coerceIn(0f, 1f)
    return graphicsLayer { alpha = effectiveAlpha }
        .drawWithContent {
            // Alpha alone does not guarantee nested Liquid controls skip backdrop work. Keep the
            // measured slot intact, but stop their draw traversal once they are truly invisible.
            if (effectiveAlpha > 0.005f) drawContent()
        }
}

@Composable
private fun PersonalizeSliderLabel(
    value: State<Float>,
    label: (Float) -> String
) {
    Text(label(value.value), style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun PersonalizeValueSlider(
    sliderKey: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop?,
    label: (Float) -> String,
    onCommit: (Float) -> Unit,
    onPreviewValueChange: (Float) -> Unit,
    previewSliderKey: String?,
    previewProgress: Float,
    onSliderPreviewActiveChange: (String, Boolean) -> Unit,
    snapValue: Float? = null,
    benchmarkDescription: String? = null,
    onTouchActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayValue = remember(valueRange) { mutableFloatStateOf(value.coerceIn(valueRange)) }
    LaunchedEffect(value, valueRange) {
        displayValue.floatValue = value.coerceIn(valueRange)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .personalizePreviewVisibility(previewSliderKey, previewProgress, sliderKey),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PersonalizeSliderLabel(displayValue, label)
        LiquidControlSlider(
            compactThumb = true,
            value = value,
            onValueChange = onCommit,
            valueRange = valueRange,
            backdrop = backdrop,
            onLiveValueChange = {
                displayValue.floatValue = it
                onPreviewValueChange(it)
            },
            snapValue = snapValue,
            modifier = if (benchmarkDescription == null) Modifier else Modifier.semantics {
                contentDescription = benchmarkDescription
            },
            onSliderTouchActiveChange = { active ->
                onTouchActiveChange(active)
                onSliderPreviewActiveChange(sliderKey, active)
            }
        )
    }
}

@Composable
private fun WallpaperBlurControl(
    blurDp: Float,
    backdrop: Backdrop?,
    onCommit: (Float) -> Unit,
    onPreviewValueChange: (Float) -> Unit,
    previewSliderKey: String?,
    previewProgress: Float,
    onSliderPreviewActiveChange: (String, Boolean) -> Unit,
    onTouchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizeValueSlider(
        sliderKey = PersonalizeWallpaperBlurSlider,
        modifier = modifier,
        value = wallpaperBlurPercent(blurDp),
        valueRange = 0f..100f,
        backdrop = backdrop,
        label = { "壁纸模糊 ${it.roundToInt()}%" },
        onCommit = onCommit,
        onPreviewValueChange = onPreviewValueChange,
        previewSliderKey = previewSliderKey,
        previewProgress = previewProgress,
        onSliderPreviewActiveChange = onSliderPreviewActiveChange,
        snapValue = 0f,
        benchmarkDescription = "benchmark_personalization_blur_slider",
        onTouchActiveChange = onTouchActiveChange
    )
}

private enum class CourseColorDialogStage { Picker, Wallpaper }

private val SolidCourseColorPresets = listOf(
    0xFFD6E9FFL,
    0xFFFFC4D6L,
    0xFFB7E4C7L,
    0xFFFFD166L
)

private val GradientCourseColorPresets = listOf(
    0xFF79BDF2L,
    0xFFA99AE8L,
    0xFFF09AB6L,
    0xFF71C8A1L
)

/** One affordance for the original wallpaper-driven automatic colourful mode. */
private val AutomaticColorfulCoursePreview = DefaultCourseCardPalette.take(4)

private fun vividPreviewColor(argb: Long, minimumValue: Float = 0.72f): ComposeColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb.toInt(), hsv)
    hsv[1] = hsv[1].coerceIn(0.34f, 0.82f)
    hsv[2] = hsv[2].coerceIn(minimumValue, 0.96f)
    return ComposeColor(android.graphics.Color.HSVToColor(hsv))
}

private fun tonalPreviewColors(seed: Long): List<ComposeColor> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toInt(), hsv)
    val hue = hsv[0]
    val saturation = hsv[1].coerceAtLeast(0.40f)
    return listOf(
        ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation * 0.34f, 0.98f))),
        ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation * 0.58f, 0.90f))),
        ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation * 0.78f, 0.80f))),
        ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation.coerceAtMost(0.88f), 0.70f)))
    )
}

@Composable
private fun CourseColorModeRow(
    title: String,
    iconRes: Int,
    mode: CourseCardColorMode,
    selectedMode: CourseCardColorMode,
    presets: List<List<Long>>,
    selectedPreset: (List<Long>) -> Boolean,
    customSelected: Boolean,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    onPresetSelected: (List<Long>) -> Unit,
    onOpenPalette: () -> Unit
) {
    val foreground = LocalContentColor.current
    val labelColor = if (selectedMode == mode) MaterialTheme.colorScheme.primary else foreground
    @Composable
    fun PresetButton(colors: List<Long>) {
        val selected = selectedMode == mode && selectedPreset(colors)
        val previewColors = when (mode) {
            CourseCardColorMode.SOLID -> colors.map { ComposeColor(it.toInt()) }
            CourseCardColorMode.GRADIENT -> tonalPreviewColors(colors.first())
            CourseCardColorMode.COLORFUL -> colors.map { vividPreviewColor(it) }
        }
        Surface(
            modifier = Modifier.size(32.dp),
            shape = Capsule(),
            color = if (mode == CourseCardColorMode.COLORFUL) {
                ComposeColor.White.copy(alpha = 0.88f)
            } else {
                ComposeColor.Transparent
            },
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            onClick = { onPresetSelected(colors) }
        ) {
            if (mode == CourseCardColorMode.COLORFUL) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // A four-color dot cluster reads instantly as "colorful"; a themed glyph
                    // cannot express the multi-palette idea on its own.
                    Canvas(
                        Modifier
                            .size(19.dp)
                            .semantics { contentDescription = "自动彩色课程卡" }
                    ) {
                        val dotRadius = size.minDimension * 0.165f
                        val gap = size.minDimension * 0.235f
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dotColors = listOf(
                            ComposeColor(0xFFFF5A52),
                            ComposeColor(0xFFFFB300),
                            ComposeColor(0xFF30C851),
                            ComposeColor(0xFF0A84FF)
                        )
                        val dotCenters = listOf(
                            Offset(centerX - gap, centerY - gap),
                            Offset(centerX + gap, centerY - gap),
                            Offset(centerX - gap, centerY + gap),
                            Offset(centerX + gap, centerY + gap)
                        )
                        dotCenters.forEachIndexed { index, center ->
                            drawCircle(color = dotColors[index], radius = dotRadius, center = center)
                        }
                    }
                }
            } else {
                Box(
                    Modifier.background(
                        if (previewColors.size == 1) {
                            Brush.linearGradient(listOf(previewColors.first(), previewColors.first()))
                        } else {
                            Brush.linearGradient(previewColors)
                        }
                    )
                )
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.width(42.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                color = labelColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode == CourseCardColorMode.COLORFUL) {
                repeat(5) { index ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (index) {
                            0 -> PresetButton(presets.first())
                            4 -> CourseColorPaletteButton(
                                backdrop = backdrop,
                                selected = customSelected,
                                onClick = onOpenPalette,
                                size = 32.dp
                            )
                        }
                    }
                }
            } else {
                presets.forEach { colors ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PresetButton(colors)
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CourseColorPaletteButton(
                        backdrop = backdrop,
                        selected = customSelected,
                        onClick = onOpenPalette,
                        size = 32.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseColorEditorDialog(
    show: Boolean,
    stage: CourseColorDialogStage,
    mode: CourseCardColorMode,
    colors: List<Long>,
    selectedIndex: Int,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onStageChange: (CourseColorDialogStage) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onConfirm: () -> Unit
) {
    val foreground = appPanelForegroundColor(config)
    SleepDownPickerDialog(
        show = show,
        title = if (stage == CourseColorDialogStage.Picker) "选择颜色" else "从壁纸取色",
        onDismissRequest = onDismiss,
        backdrop = backdrop,
        config = config,
        contentSpacing = 14.dp,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        blurRadius = 10.dp,
        contentTransitionKey = stage,
        titleAction = {
            if (stage == CourseColorDialogStage.Picker) {
                IconButton(
                    onClick = { onStageChange(CourseColorDialogStage.Wallpaper) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_color_eyedropper),
                        contentDescription = "从壁纸吸色",
                        tint = foreground,
                        modifier = Modifier.size(21.dp)
                    )
                }
            } else {
                // Keep the title row's geometry stable while its content crossfades.
                Spacer(Modifier.size(38.dp))
            }
        }
    ) {
        if (stage == CourseColorDialogStage.Picker) {
            if (mode == CourseCardColorMode.COLORFUL && colors.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    items(colors.size) { index ->
                        val color = colors[index]
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = Capsule(),
                            color = ComposeColor(color.toInt()),
                            border = BorderStroke(
                                if (index == selectedIndex) 3.dp else 1.dp,
                                if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                else foreground.copy(alpha = 0.34f)
                            ),
                            onClick = { onSelectedIndexChange(index) }
                        ) {}
                    }
                    item {
                        val addColor: () -> Unit = {
                            val addIndex = colors.size
                            onColorChange(
                                addIndex,
                                DefaultCourseCardPalette[addIndex % DefaultCourseCardPalette.size]
                            )
                            onSelectedIndexChange(addIndex)
                        }
                        // 外层 padding 与 clip=false 保证按压放大不被列表/弹窗裁切
                        Box(
                            // 与色块保持同尺寸(34dp)让加号对齐；按压放大外扩交给
                            // LazyRow 的 contentPadding 与 clip=false 处理，
                            // 项内 padding 会导致加号相对色块错位。
                            modifier = Modifier
                                .size(34.dp)
                                .graphicsLayer { clip = false },
                            contentAlignment = Alignment.Center
                        ) {
                            if (backdrop != null) {
                                LiquidButton(
                                    onClick = addColor,
                                    backdrop = backdrop,
                                    modifier = Modifier.size(34.dp),
                                    height = 34.dp,
                                    contentPadding = PaddingValues(0.dp),
                                    surfaceColor = ComposeColor(0xFF0A84FF),
                                    blurRadius = 7.dp,
                                    lensHeight = 11.dp,
                                    lensAmount = 15.dp,
                                    pressExpansion = 3.dp
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "添加颜色",
                                        tint = ComposeColor.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = Capsule(),
                                    color = ComposeColor(0xFF0A84FF),
                                    onClick = addColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "添加颜色",
                                            tint = ComposeColor.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            top.yukonga.miuix.kmp.basic.ColorPalette(
                color = ComposeColor(colors.getOrElse(selectedIndex) { colors.firstOrNull() ?: 0xFFD6E9FFL }.toInt()),
                onColorChanged = { color ->
                    onColorChange(selectedIndex, color.copy(alpha = 1f).toArgb().toLong() and 0xFFFFFFFFL)
                },
                modifier = Modifier.fillMaxWidth(),
                rows = 7,
                hueColumns = 12,
                includeGrayColumn = true,
                showPreview = true,
                cornerRadius = 16.dp,
                indicatorRadius = 10.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickSheetLiquidAction(
                    label = "取消",
                    enabled = true,
                    backdrop = backdrop,
                    config = config,
                    modifier = Modifier.weight(1f),
                    height = SleepDownDesignTokens.CenteredDialog.ActionHeight,
                    onClick = onDismiss
                )
                QuickSheetLiquidAction(
                    label = "确认",
                    enabled = colors.isNotEmpty(),
                    backdrop = backdrop,
                    config = config,
                    modifier = Modifier.weight(1f),
                    primary = true,
                    height = SleepDownDesignTokens.CenteredDialog.ActionHeight,
                    onClick = onConfirm
                )
            }
        } else {
            WallpaperPaletteSampler(
                mode = mode,
                colors = colors,
                config = config,
                onColorChange = onColorChange,
                onCancel = { onStageChange(CourseColorDialogStage.Picker) },
                onConfirm = onConfirm,
                backdrop = backdrop
            )
        }
    }
}

@Composable
private fun WallpaperPaletteSampler(
    mode: CourseCardColorMode,
    colors: List<Long>,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    onColorChange: (Int, Long) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    // Miuix caps a centered dialog at two thirds of the current window. Reserve the measured
    // title, copy, actions, shell insets and compact gaps first, then let the wallpaper preview
    // consume only the remaining space. This keeps both actions inside the shell on phones with
    // large display or font scaling while retaining a useful sampling area on taller screens.
    val previewHeight = (windowHeight * (2f / 3f) - 210.dp).coerceIn(180.dp, 300.dp)
    val foreground = appPanelForegroundColor(config)
    val useDarkDefaultWallpaper = appUsesDarkTheme(config)
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        config.wallpaperUri,
        config.defaultWallpaperStyle,
        useDarkDefaultWallpaper
    ) {
        value = withContext(Dispatchers.IO) {
            loadWallpaperBitmap(context, config, useDarkDefaultWallpaper)
        }
    }
    val pointCount = if (mode == CourseCardColorMode.COLORFUL) 4 else 1
    var samplePoints by remember(mode) {
        mutableStateOf(
            if (pointCount == 1) {
                listOf(Offset(0.50f, 0.50f))
            } else {
                listOf(
                    Offset(0.28f, 0.28f),
                    Offset(0.72f, 0.30f),
                    Offset(0.32f, 0.72f),
                    Offset(0.72f, 0.74f)
                )
            }
        )
    }
    var selectedPoint by remember(mode) { mutableIntStateOf(0) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    fun updatePoint(index: Int, point: Offset) {
        val safePoint = Offset(point.x.coerceIn(0.04f, 0.96f), point.y.coerceIn(0.04f, 0.96f))
        samplePoints = samplePoints.toMutableList().also { it[index] = safePoint }
        val source = bitmap ?: return
        val cropState = if (!config.wallpaperUri.isNullOrBlank()) {
            config.wallpaperCropState(WallpaperPreviewOrientation.Portrait)
        } else {
            WallpaperCropState()
        }
        sampleCroppedBitmapColor(
            source,
            previewSize,
            safePoint.x * previewSize.width,
            safePoint.y * previewSize.height,
            cropState
        )?.let { onColorChange(index, it) }
    }

    LaunchedEffect(bitmap, previewSize, mode) {
        val source = bitmap ?: return@LaunchedEffect
        if (previewSize.width <= 0 || previewSize.height <= 0) return@LaunchedEffect
        val cropState = if (!config.wallpaperUri.isNullOrBlank()) {
            config.wallpaperCropState(WallpaperPreviewOrientation.Portrait)
        } else {
            WallpaperCropState()
        }
        samplePoints.take(pointCount).forEachIndexed { index, point ->
            sampleCroppedBitmapColor(
                source,
                previewSize,
                point.x * previewSize.width,
                point.y * previewSize.height,
                cropState
            )?.let { onColorChange(index, it) }
        }
    }

    Text(
        text = if (mode == CourseCardColorMode.COLORFUL) {
            "拖动圆点，为彩色组合提取四个颜色"
        } else if (mode == CourseCardColorMode.GRADIENT) {
            "拖动圆点，提取渐变色系的基准色"
        } else {
            "拖动圆点，提取课程卡片颜色"
        },
        color = foreground.copy(alpha = 0.70f),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(210.dp)
                .height(previewHeight)
                .clip(RoundedRectangle(24.dp))
                .background(if (appUsesDarkTheme(config)) ComposeColor(0xFF15171C) else ComposeColor(0xFFF1F3F8))
                .onSizeChanged { previewSize = it }
                .pointerInput(bitmap, previewSize, selectedPoint) {
                    detectTapGestures { tap ->
                        if (previewSize.width > 0 && previewSize.height > 0) {
                            updatePoint(
                                selectedPoint,
                                Offset(tap.x / previewSize.width, tap.y / previewSize.height)
                            )
                        }
                    }
                }
        ) {
            bitmap?.let { wallpaper ->
                FocusCroppedWallpaper(
                    bitmap = wallpaper,
                    config = config,
                    modifier = Modifier.fillMaxSize(),
                    useSavedCrop = !config.wallpaperUri.isNullOrBlank()
                )
            } ?: Text(
                text = "当前没有可取色的壁纸",
                color = foreground,
                modifier = Modifier.align(Alignment.Center)
            )
            samplePoints.forEachIndexed { index, point ->
                val markerColor = ComposeColor(colors.getOrElse(index) { colors.firstOrNull() ?: 0xFFD6E9FFL }.toInt())
                Surface(
                    modifier = Modifier
                        .offset {
                            val radiusPx = with(density) { 18.dp.roundToPx() }
                            IntOffset(
                                (point.x * previewSize.width).roundToInt() - radiusPx,
                                (point.y * previewSize.height).roundToInt() - radiusPx
                            )
                        }
                        .size(36.dp)
                        .pointerInput(index, previewSize) {
                            detectDragGestures(
                                onDragStart = { selectedPoint = index },
                                onDrag = { _, dragAmount ->
                                    if (previewSize.width > 0 && previewSize.height > 0) {
                                        selectedPoint = index
                                        val current = samplePoints[index]
                                        updatePoint(
                                            index,
                                            Offset(
                                                current.x + dragAmount.x / previewSize.width,
                                                current.y + dragAmount.y / previewSize.height
                                            )
                                        )
                                    }
                                }
                            )
                        },
                    shape = Capsule(),
                    color = markerColor,
                    border = BorderStroke(
                        if (selectedPoint == index) 3.dp else 2.dp,
                        ComposeColor.White.copy(alpha = 0.94f)
                    ),
                    onClick = { selectedPoint = index }
                ) {}
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickSheetLiquidAction(
            label = "取消",
            enabled = true,
            backdrop = backdrop,
            config = config,
            modifier = Modifier.weight(1f),
            height = SleepDownDesignTokens.CenteredDialog.ActionHeight,
            onClick = onCancel
        )
        QuickSheetLiquidAction(
            label = "确认",
            enabled = bitmap != null,
            backdrop = backdrop,
            config = config,
            modifier = Modifier.weight(1f),
            primary = true,
            height = SleepDownDesignTokens.CenteredDialog.ActionHeight,
            onClick = onConfirm
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizePanel(
    modifier: Modifier = Modifier,
    drawSurface: Boolean = true,
    rowEntranceActive: Boolean = true,
    state: AppState,
    backdrop: Backdrop?,
    mode: HomeMode,
    weekCardHeightScale: Float,
    weekCardHeightScaleFloor: Float,
    onWeekCardHeightScale: (Float) -> Unit,
    onWeekCardHeightScalePreview: (Float) -> Unit,
    onPickWallpaper: () -> Unit,
    onUpdateConfig: (String, ScheduleConfigEntity) -> Unit,
    onPreviewConfig: (String, ScheduleConfigEntity) -> Unit,
    previewSliderKey: String?,
    previewProgress: Float,
    onSliderPreviewActiveChange: (String, Boolean) -> Unit
) {
    val rowReveal = remember { Animatable(0f) }
    val rowDensity = LocalDensity.current
    val rowEasing = remember { CubicBezierEasing(0.22f, 0f, 0.30f, 1f) }
    LaunchedEffect(rowEntranceActive) {
        if (rowEntranceActive) {
            withFrameNanos { }
            rowReveal.animateTo(1f, tween(580, easing = LinearEasing))
        }
    }
    fun Modifier.rowEntrance(index: Int): Modifier = graphicsLayer {
        val t = ((rowReveal.value * 580f - index * 15f) / 340f).coerceIn(0f, 1f)
        val progress = rowEasing.transform(t)
        alpha = progress
        scaleX = 0.86f + 0.14f * progress
        scaleY = scaleX
        translationY = (1f - progress) * with(rowDensity) { 32.dp.toPx() }
    }
    val inheritedCoursePalette = LocalCourseCardPalette.current
    var showCourseColorDialog by remember { mutableStateOf(false) }
    var courseColorDialogMode by remember { mutableStateOf(CourseCardColorMode.SOLID) }
    var courseColorDialogStage by remember { mutableStateOf(CourseColorDialogStage.Picker) }
    var courseColorEditorColors by remember { mutableStateOf(listOf(0xFFD6E9FFL)) }
    var courseColorEditorIndex by remember { mutableIntStateOf(0) }

    fun openCourseColorDialog(colorMode: CourseCardColorMode) {
        val seed = state.config.cardColorArgb
            .takeUnless { it == MulticolorCourseCardArgb }
            ?: 0xFFD6E9FFL
        val initial = when (colorMode) {
            CourseCardColorMode.SOLID,
            CourseCardColorMode.GRADIENT -> listOf(seed)
            CourseCardColorMode.COLORFUL -> {
                val configured = decodeCourseCardPalette(state.config.courseCardPalette)
                    .ifEmpty { inheritedCoursePalette }
                    .ifEmpty { DefaultCourseCardPalette }
                    .take(4)
                buildList {
                    addAll(configured)
                    DefaultCourseCardPalette.forEach { fallback ->
                        if (size < 4 && fallback !in this) add(fallback)
                    }
                }.take(4)
            }
        }
        courseColorDialogMode = colorMode
        courseColorDialogStage = CourseColorDialogStage.Picker
        courseColorEditorColors = initial
        courseColorEditorIndex = 0
        showCourseColorDialog = true
    }

    fun updateCourseColorEditor(index: Int, color: Long) {
        val next = courseColorEditorColors.toMutableList()
        while (next.size <= index) {
            next += DefaultCourseCardPalette[next.size % DefaultCourseCardPalette.size]
        }
        next[index] = color and 0xFFFFFFFFL
        courseColorEditorColors = next
    }

    fun commitCourseColorEditor() {
        val primary = courseColorEditorColors.firstOrNull() ?: 0xFFD6E9FFL
        val candidate = when (courseColorDialogMode) {
            CourseCardColorMode.SOLID,
            CourseCardColorMode.GRADIENT -> state.config.copy(
                cardColorArgb = primary,
                courseCardColorMode = courseColorDialogMode
            )
            CourseCardColorMode.COLORFUL -> state.config.copy(
                cardColorArgb = primary,
                courseCardColorMode = CourseCardColorMode.COLORFUL,
                courseCardPalette = encodeCourseCardPalette(courseColorEditorColors)
            )
        }
        onUpdateConfig(PersonalizeCardColorChange, candidate)
        showCourseColorDialog = false
    }

    var activeTouchSliderKey by remember { mutableStateOf<String?>(null) }
    fun updateSliderTouchOwner(key: String, active: Boolean) {
        activeTouchSliderKey = resolveActivePersonalizationSlider(
            currentKey = activeTouchSliderKey,
            eventKey = key,
            active = active
        )
    }
    @Composable
    fun PersonalizeSection(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    ComposeColor.Black.copy(
                        alpha = 0.10f * (1f - previewProgress.coerceIn(0f, 1f))
                    ),
                    shape = RoundedRectangle(18.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
    @Composable
    fun PanelContent(modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonalizeSection {
                Row(
                    modifier = Modifier.rowEntrance(0)
                        .fillMaxWidth()
                        .personalizePreviewVisibility(previewSliderKey, previewProgress),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("首页壁纸", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LiquidMenuButton(
                            backdrop,
                            "\u9009\u62E9\u58C1\u7EB8",
                            onClick = onPickWallpaper,
                            textColorOverride = ComposeColor.White,
                            surfaceColorOverride = ComposeColor(0xFF0A84FF).copy(alpha = 0.72f)
                        )
                        if (state.config.wallpaperUri != null) {
                            LiquidMenuButton(
                                backdrop,
                                "\u6E05\u9664",
                                onClick = {
                                    onUpdateConfig(
                                        PersonalizeWallpaperContentChange,
                                        state.config.copy(
                                            wallpaperUri = null,
                                            wallpaperPortraitCenterX = 0.5f,
                                            wallpaperPortraitCenterY = 0.5f,
                                            wallpaperPortraitScale = 1f,
                                            wallpaperLandscapeCenterX = 0.5f,
                                            wallpaperLandscapeCenterY = 0.5f,
                                            wallpaperLandscapeScale = 1f,
                                            wallpaperSourceWidth = null,
                                            wallpaperSourceHeight = null,
                                            homeTextLight = false
                                        )
                                    )
                                },
                                textColorOverride = ComposeColor.White,
                                surfaceColorOverride = ComposeColor(0xFFFF453A).copy(alpha = 0.66f)
                            )
                        }
                    }
                }
                WallpaperBlurControl(
                    modifier = Modifier.rowEntrance(1),
                    blurDp = state.config.wallpaperBlur,
                    backdrop = backdrop,
                    onCommit = { percent ->
                        onUpdateConfig(
                            PersonalizeWallpaperBlurSlider,
                            state.config.copy(wallpaperBlur = wallpaperBlurDp(percent))
                        )
                    },
                    onPreviewValueChange = { percent ->
                        onPreviewConfig(
                            PersonalizeWallpaperBlurSlider,
                            state.config.copy(wallpaperBlur = wallpaperBlurDp(percent))
                        )
                    },
                    previewSliderKey = previewSliderKey,
                    previewProgress = previewProgress,
                    onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                    onTouchActiveChange = {
                        updateSliderTouchOwner(PersonalizeWallpaperBlurSlider, it)
                    }
                )
                PersonalizeValueSlider(
                    sliderKey = PersonalizeWallpaperBrightnessSlider,
                    modifier = Modifier.rowEntrance(2),
                    value = state.config.wallpaperBrightness.coerceIn(0.35f, 1f),
                    valueRange = 0.35f..1f,
                    backdrop = backdrop,
                    label = { "壁纸亮度 ${(it * 100).toInt()}%" },
                    onCommit = {
                        onUpdateConfig(
                            PersonalizeWallpaperBrightnessSlider,
                            state.config.copy(wallpaperBrightness = it)
                        )
                    },
                    onPreviewValueChange = {
                        onPreviewConfig(
                            PersonalizeWallpaperBrightnessSlider,
                            state.config.copy(wallpaperBrightness = it)
                        )
                    },
                    previewSliderKey = previewSliderKey,
                    previewProgress = previewProgress,
                    onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                    snapValue = 1f,
                    onTouchActiveChange = {
                        updateSliderTouchOwner(PersonalizeWallpaperBrightnessSlider, it)
                    }
                )
            }
            PersonalizeSection {
                if (mode == HomeMode.Week) {
                    Text(
                        text = "周视图行高",
                        modifier = Modifier.rowEntrance(3)
                            .fillMaxWidth()
                            .personalizePreviewVisibility(previewSliderKey, previewProgress),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(
                        modifier = Modifier.rowEntrance(4).personalizePreviewVisibility(
                            previewSliderKey,
                            previewProgress,
                            PersonalizeWeekHeightSlider
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Box {
                            LiquidControlSlider(
                                value = weekCardHeightSliderFromScale(
                                    scale = weekCardHeightScale,
                                    minimumScale = weekCardHeightScaleFloor
                                ),
                                compactThumb = true,
                                onValueChange = {
                                    onWeekCardHeightScale(
                                        weekCardHeightScaleFromSlider(
                                            progress = it,
                                            minimumScale = weekCardHeightScaleFloor
                                        )
                                    )
                                },
                                valueRange = 0f..1f,
                                backdrop = backdrop,
                                onLiveValueChange = {
                                    onWeekCardHeightScalePreview(
                                        weekCardHeightScaleFromSlider(
                                            progress = it,
                                            minimumScale = weekCardHeightScaleFloor
                                        )
                                    )
                                },
                                snapValue = 0.5f,
                                onSliderTouchActiveChange = { active ->
                                    updateSliderTouchOwner(PersonalizeWeekHeightSlider, active)
                                    onSliderPreviewActiveChange(PersonalizeWeekHeightSlider, active)
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-2).dp)
                        ) {
                            Text(
                                "紧凑",
                                modifier = Modifier.align(Alignment.CenterStart),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "自适应",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "宽松",
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.rowEntrance(5)
                        .fillMaxWidth()
                        .personalizePreviewVisibility(previewSliderKey, previewProgress),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("课程卡片颜色", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                CourseColorModeRow(
                    title = "纯色",
                    iconRes = R.drawable.ic_color_solid,
                    mode = CourseCardColorMode.SOLID,
                    selectedMode = state.config.courseCardColorMode,
                    presets = SolidCourseColorPresets.map { listOf(it) },
                    selectedPreset = { it.firstOrNull() == state.config.cardColorArgb },
                    customSelected = state.config.courseCardColorMode == CourseCardColorMode.SOLID &&
                        SolidCourseColorPresets.none { it == state.config.cardColorArgb },
                    backdrop = backdrop,
                    modifier = Modifier.rowEntrance(6).personalizePreviewVisibility(previewSliderKey, previewProgress),
                    onPresetSelected = { colors ->
                        onUpdateConfig(
                            PersonalizeCardColorChange,
                            state.config.copy(
                                cardColorArgb = colors.first(),
                                courseCardColorMode = CourseCardColorMode.SOLID
                            )
                        )
                    },
                    onOpenPalette = { openCourseColorDialog(CourseCardColorMode.SOLID) }
                )
                CourseColorModeRow(
                    title = "渐变",
                    iconRes = R.drawable.ic_color_gradient,
                    mode = CourseCardColorMode.GRADIENT,
                    selectedMode = state.config.courseCardColorMode,
                    presets = GradientCourseColorPresets.map { listOf(it) },
                    selectedPreset = { it.firstOrNull() == state.config.cardColorArgb },
                    customSelected = state.config.courseCardColorMode == CourseCardColorMode.GRADIENT &&
                        GradientCourseColorPresets.none { it == state.config.cardColorArgb },
                    backdrop = backdrop,
                    modifier = Modifier.rowEntrance(7).personalizePreviewVisibility(previewSliderKey, previewProgress),
                    onPresetSelected = { colors ->
                        onUpdateConfig(
                            PersonalizeCardColorChange,
                            state.config.copy(
                                cardColorArgb = colors.first(),
                                courseCardColorMode = CourseCardColorMode.GRADIENT
                            )
                        )
                    },
                    onOpenPalette = { openCourseColorDialog(CourseCardColorMode.GRADIENT) }
                )
                CourseColorModeRow(
                    title = "彩色",
                    iconRes = R.drawable.ic_color_colorful,
                    mode = CourseCardColorMode.COLORFUL,
                    selectedMode = state.config.courseCardColorMode,
                    presets = listOf(AutomaticColorfulCoursePreview),
                    selectedPreset = { state.config.courseCardPalette.isBlank() },
                    customSelected = state.config.courseCardColorMode == CourseCardColorMode.COLORFUL &&
                        state.config.courseCardPalette.isNotBlank(),
                    backdrop = backdrop,
                    modifier = Modifier.rowEntrance(8).personalizePreviewVisibility(previewSliderKey, previewProgress),
                    onPresetSelected = {
                        onUpdateConfig(
                            PersonalizeCardColorChange,
                            state.config.copy(
                                courseCardColorMode = CourseCardColorMode.COLORFUL,
                                courseCardPalette = ""
                            )
                        )
                    },
                    onOpenPalette = { openCourseColorDialog(CourseCardColorMode.COLORFUL) }
                )
                val glassLocked = !state.config.hasAnyWallpaper()
                val alphaLabel = when {
                    !glassLocked &&
                        state.config.courseCardGlassEnabled &&
                        state.config.courseCardOutlineLightEnabled -> "轮廓光强度"
                    !glassLocked && state.config.courseCardGlassEnabled -> "课程卡片着色强度"
                    else -> "课程卡片不透明度"
                }
                PersonalizeValueSlider(
                    sliderKey = PersonalizeCardAlphaSlider,
                    modifier = Modifier.rowEntrance(9),
                    value = state.config.cardAlpha.coerceIn(0f, 1f),
                    valueRange = 0f..1f,
                    backdrop = backdrop,
                    label = { "$alphaLabel ${(it * 100).toInt()}%" },
                    onCommit = {
                        onUpdateConfig(
                            PersonalizeCardAlphaSlider,
                            state.config.copy(cardAlpha = it)
                        )
                    },
                    onPreviewValueChange = {
                        onPreviewConfig(
                            PersonalizeCardAlphaSlider,
                            state.config.copy(cardAlpha = it)
                        )
                    },
                    previewSliderKey = previewSliderKey,
                    previewProgress = previewProgress,
                    onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                    snapValue = 0.5f,
                    onTouchActiveChange = {
                        updateSliderTouchOwner(PersonalizeCardAlphaSlider, it)
                    }
                )
                if (
                    (state.config.courseCardGlassEnabled || state.config.courseCardGaussianBlurEnabled) &&
                    !glassLocked
                ) {
                    val maxCourseCardBlur = courseCardBlurMaximum(state.config.courseCardGlassEnabled)
                    PersonalizeValueSlider(
                        sliderKey = PersonalizeCardBlurSlider,
                        modifier = Modifier.rowEntrance(10),
                        value = state.config.courseCardBlur.coerceIn(0f, maxCourseCardBlur) /
                            maxCourseCardBlur * 100f,
                        valueRange = 0f..100f,
                        backdrop = backdrop,
                        label = { "课程卡片模糊 ${it.toInt()}%" },
                        onCommit = {
                            onUpdateConfig(
                                PersonalizeCardBlurSlider,
                                state.config.copy(courseCardBlur = it / 100f * maxCourseCardBlur)
                            )
                        },
                        onPreviewValueChange = {
                            onPreviewConfig(
                                PersonalizeCardBlurSlider,
                                state.config.copy(courseCardBlur = it / 100f * maxCourseCardBlur)
                            )
                        },
                        previewSliderKey = previewSliderKey,
                        previewProgress = previewProgress,
                        onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                        snapValue = 35f,
                        onTouchActiveChange = {
                            updateSliderTouchOwner(PersonalizeCardBlurSlider, it)
                        }
                    )
                }
                PersonalizeValueSlider(
                    sliderKey = PersonalizeCardFontSlider,
                    modifier = Modifier.rowEntrance(11),
                    value = state.config.courseCardFontScale,
                    valueRange = 0.80f..1.35f,
                    backdrop = backdrop,
                    label = { "课程卡片字体 ${(it * 100).toInt()}%" },
                    onCommit = {
                        onUpdateConfig(
                            PersonalizeCardFontSlider,
                            state.config.copy(courseCardFontScale = it)
                        )
                    },
                    onPreviewValueChange = {
                        onPreviewConfig(
                            PersonalizeCardFontSlider,
                            state.config.copy(courseCardFontScale = it)
                        )
                    },
                    previewSliderKey = previewSliderKey,
                    previewProgress = previewProgress,
                    onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                    snapValue = 1f,
                    onTouchActiveChange = {
                        updateSliderTouchOwner(PersonalizeCardFontSlider, it)
                    }
                )
                if (mode == HomeMode.Week) {
                    PersonalizeValueSlider(
                        sliderKey = PersonalizeWeekCornerSlider,
                        modifier = Modifier.rowEntrance(12),
                        value = state.config.weekCardCornerProgress.coerceIn(0f, 1f),
                        valueRange = 0f..1f,
                        backdrop = backdrop,
                        label = { "课程卡片圆角" },
                        onCommit = {
                            onUpdateConfig(
                                PersonalizeWeekCornerSlider,
                                state.config.copy(weekCardCornerProgress = it)
                            )
                        },
                        onPreviewValueChange = {
                            onPreviewConfig(
                                PersonalizeWeekCornerSlider,
                                state.config.copy(weekCardCornerProgress = it)
                            )
                        },
                        previewSliderKey = previewSliderKey,
                        previewProgress = previewProgress,
                        onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                        snapValue = 0.5f,
                        onTouchActiveChange = {
                            updateSliderTouchOwner(PersonalizeWeekCornerSlider, it)
                        }
                    )
                }
                if (state.config.courseCardGlassEnabled && !glassLocked) {
                    PersonalizeValueSlider(
                        sliderKey = PersonalizeCardRefractionSlider,
                        modifier = Modifier.rowEntrance(13),
                        value = state.config.courseCardRefractionStrength.coerceIn(0f, 1f),
                        valueRange = 0f..1f,
                        backdrop = backdrop,
                        label = { "折射强度 ${(it * 100f).toInt()}%" },
                        onCommit = {
                            onUpdateConfig(
                                PersonalizeCardRefractionSlider,
                                state.config.copy(courseCardRefractionStrength = it)
                            )
                        },
                        onPreviewValueChange = {
                            onPreviewConfig(
                                PersonalizeCardRefractionSlider,
                                state.config.copy(courseCardRefractionStrength = it)
                            )
                        },
                        previewSliderKey = previewSliderKey,
                        previewProgress = previewProgress,
                        onSliderPreviewActiveChange = onSliderPreviewActiveChange,
                        snapValue = 0.5f,
                        onTouchActiveChange = {
                            updateSliderTouchOwner(PersonalizeCardRefractionSlider, it)
                        }
                    )
                }
                Spacer(
                    Modifier
                        .height(4.dp)
                        .personalizePreviewVisibility(previewSliderKey, previewProgress)
                )
                Row(
                    modifier = Modifier.rowEntrance(14)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .personalizePreviewVisibility(previewSliderKey, previewProgress),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("课程卡片液态玻璃", style = MaterialTheme.typography.labelLarge)
                    LiquidControlToggle(
                        checked = state.config.courseCardGlassEnabled && !glassLocked,
                        compact = true,
                        onCheckedChange = {
                            onUpdateConfig(
                                PersonalizeCardGlassChange,
                                state.config.copy(courseCardGlassEnabled = it)
                            )
                        },
                        enabled = !glassLocked,
                        backdrop = backdrop
                    )
                }
                if (glassLocked) {
                    Text(
                        "设置壁纸以启用课程卡片液态玻璃效果",
                        style = MaterialTheme.typography.bodySmall,
                        color = personalizePanelForegroundColor(state.config).copy(alpha = 0.60f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 6.dp)
                            .personalizePreviewVisibility(previewSliderKey, previewProgress)
                    )
                }
                if (state.config.courseCardGlassEnabled && !glassLocked) {
                    Row(
                        modifier = Modifier.rowEntrance(15)
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .personalizePreviewVisibility(previewSliderKey, previewProgress),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("质感轮廓光", style = MaterialTheme.typography.labelLarge)
                        LiquidControlToggle(
                            checked = state.config.courseCardOutlineLightEnabled,
                            compact = true,
                            onCheckedChange = {
                                onUpdateConfig(
                                    PersonalizeCardOutlineLightChange,
                                    state.config.copy(courseCardOutlineLightEnabled = it)
                                )
                            },
                            backdrop = backdrop
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.rowEntrance(15)
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .personalizePreviewVisibility(previewSliderKey, previewProgress),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课程卡片高斯模糊", style = MaterialTheme.typography.labelLarge)
                        LiquidControlToggle(
                            checked = state.config.courseCardGaussianBlurEnabled && !glassLocked,
                            compact = true,
                            onCheckedChange = {
                                onUpdateConfig(
                                    PersonalizeCardGaussianBlurChange,
                                    state.config.copy(courseCardGaussianBlurEnabled = it)
                                )
                            },
                            enabled = !glassLocked,
                            backdrop = backdrop
                        )
                    }
                }
                Row(
                    modifier = Modifier.rowEntrance(16)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .personalizePreviewVisibility(previewSliderKey, previewProgress),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("彩色文字", style = MaterialTheme.typography.labelLarge)
                    LiquidControlToggle(
                        checked = state.config.courseCardColoredTextEnabled,
                        compact = true,
                        onCheckedChange = {
                            onUpdateConfig(
                                PersonalizeCardColoredTextChange,
                                state.config.copy(courseCardColoredTextEnabled = it)
                            )
                        },
                        backdrop = backdrop
                    )
                }
            }
        }
    }
    val scrollState = rememberScrollState()
    val contentModifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState, enabled = activeTouchSliderKey == null)
    val panelTextColor = personalizePanelForegroundColor(state.config)
    if (!drawSurface) {
        Box(modifier = modifier) {
            CompositionLocalProvider(LocalContentColor provides panelTextColor) {
                PanelContent(contentModifier)
            }
        }
    } else if (backdrop != null) {
        val lightGlass = glassUsesLightStyle(state.config)
        LiquidPanel(
            backdrop = backdrop,
            modifier = modifier,
            surfaceColor = if (lightGlass) ComposeColor.White.copy(alpha = 0.18f) else ComposeColor(0xFF121212).copy(alpha = 0.30f)
        ) {
            CompositionLocalProvider(LocalContentColor provides panelTextColor) {
                PanelContent(contentModifier)
            }
        }
    } else {
        GlassDialogSurface(
            backdrop = null,
            config = state.config,
            modifier = modifier
        ) {
            CompositionLocalProvider(LocalContentColor provides panelTextColor) {
                PanelContent(contentModifier)
            }
        }
    }
    CourseColorEditorDialog(
        show = showCourseColorDialog,
        stage = courseColorDialogStage,
        mode = courseColorDialogMode,
        colors = courseColorEditorColors,
        selectedIndex = courseColorEditorIndex,
        backdrop = backdrop,
        config = state.config,
        onDismiss = { showCourseColorDialog = false },
        onStageChange = { courseColorDialogStage = it },
        onSelectedIndexChange = { courseColorEditorIndex = it },
        onColorChange = ::updateCourseColorEditor,
        onConfirm = ::commitCourseColorEditor
    )
}

@Composable
private fun personalizePanelForegroundColor(config: ScheduleConfigEntity): ComposeColor {
    return homeForegroundColor(config)
}

@Composable
fun LiquidMenuButton(
    backdrop: Backdrop?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    textColorOverride: ComposeColor? = null,
    surfaceColorOverride: ComposeColor? = null
) {
    val textColor = textColorOverride ?: if (destructive) ComposeColor(0xFFFF453A) else MaterialTheme.colorScheme.primary
    val surfaceColor = surfaceColorOverride ?: if (destructive) ComposeColor(0xFFFF453A).copy(alpha = 0.16f) else ComposeColor.White.copy(alpha = 0.10f)
    if (backdrop != null) {
        LiquidButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = modifier,
            height = 40.dp,
            surfaceColor = surfaceColor,
            contentPadding = PaddingValues(horizontal = 14.dp),
            blurRadius = 8.dp,
            lensHeight = 24.dp,
            lensAmount = 28.dp,
            chromaticAberration = false
        ) {
            Text(label, color = textColor, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
    } else {
        Text(
            label,
            modifier = modifier
                .clip(Capsule())
                .background(surfaceColor.copy(alpha = surfaceColor.alpha.coerceAtLeast(0.14f)))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false
        )
    }
}

open class SettingsDetailActivityHost : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The coordinator selects a translucent destination for snapshot-free anchored routes
        // before super.onCreate. The QuickSheet route also declares this policy in the manifest so
        // its captured underlay can be installed without an opaque preview frame.
        ActivityTransitionCoordinator.prepareDestinationBeforeOnCreate(this)
        super.onCreate(savedInstanceState)
        ActivityTransitionCoordinator.installDestinationWindowBackground(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val externalBackupPreviewSource = intent.data?.takeIf {
            intent.action == Intent.ACTION_VIEW
        }
        val customizeScheduleId = intent.getIntExtra(ScheduleCustomizeIdExtra, -1).takeIf { it > 0 }
        if (intent.getStringExtra(SettingsDetailPageExtra) == SettingsPage.AutoRefreshSchedule.name) {
            // Match the education picker before the first window layout, not after composition.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
        val useEntrySnapshot = intent.getBooleanExtra(ScheduleEntrySnapshotExtra, false)
        setContent {
            val app = application as CourseScheduleApp
            val viewModel: ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ScheduleViewModelFactory(app, app.repository)
            )
            val section = if (externalBackupPreviewSource != null) {
                SettingsPage.BackupPreview
            } else {
                SettingsPage.valueOf(
                    intent.getStringExtra(SettingsDetailPageExtra) ?: SettingsPage.General.name
                )
            }
            val backupPreviewSource = externalBackupPreviewSource
                ?: intent.getStringExtra(BackupPreviewUriExtra)?.toUri()
            val stateFlow = if (customizeScheduleId != null || section == SettingsPage.ScheduleManager) {
                viewModel.allSchedulesState
            } else {
                viewModel.state
            }
            val state by stateFlow.collectAsStateWithLifecycle()
            // Anchored entries keep the destination window transparent during the Morph. The
            // special QuickSheet route supplies its root-composited underlay to the shared
            // snapshot blur chain instead of relying on OEM cross-window blur.
            val isAnchoredSettingsEntry = remember {
                intent.transitionRouteIdOrNull()
                    ?.let(TransitionRouteCatalog::get)
                    ?.legacyProfile is LegacyTransitionProfile.Anchored
            }
            val isQuickSheetSettingsEntry = remember {
                intent.transitionRouteIdOrNull() == TransitionRouteId.QuickSheetToSettingsDetail
            }
            CourseScheduleTheme(config = state.config) {
                val darkWindowBackground = appUsesDarkTheme(state.config)
                LaunchedEffect(darkWindowBackground, isAnchoredSettingsEntry) {
                    // Keep the Activity surface consistent with the Compose page while it is
                    // leaving the screen. Some Android 13/ColorOS back animations otherwise
                    // reveal the light theme's static window background for one frame.
                    if (!isAnchoredSettingsEntry) {
                        window.applyAppThemeSurface(darkWindowBackground)
                    }
                }
                val scheduleEditState = remember(state, customizeScheduleId) {
                    if (customizeScheduleId != null) scheduleConfigStateForEdit(state, customizeScheduleId) else state
                }
                val scheduleEditReady = customizeScheduleId == null ||
                    (
                        state.allConfigs.any { it.id == customizeScheduleId } &&
                            state.allPeriods.any { it.scheduleId == customizeScheduleId }
                    )
                val editEntrySnapshot = remember(customizeScheduleId, useEntrySnapshot) {
                    customizeScheduleId
                        ?.takeIf { useEntrySnapshot }
                        ?.let { scheduleId ->
                            BitmapFactory.decodeFile(
                                ScheduleSnapshotStore.file(this, scheduleId).absolutePath
                            )
                        }
                }
                var editEntrySnapshotVisible by remember(editEntrySnapshot) {
                    mutableStateOf(editEntrySnapshot != null)
                }
                LaunchedEffect(editEntrySnapshot) {
                    if (editEntrySnapshot != null) {
                        withFrameNanos { }
                        withFrameNanos { }
                        editEntrySnapshotVisible = false
                    }
                }
                var scheduleExitRequest by remember { mutableIntStateOf(0) }
                var autoRefreshWarehouseRequest by remember { mutableIntStateOf(0) }
                val autoRefreshProfile = if (section == SettingsPage.AutoRefreshSchedule) {
                    AutoRefreshScheduleStore.observe(this@SettingsDetailActivityHost)
                        .collectAsStateWithLifecycle().value
                } else null
                var widgetEditorVisible by remember { mutableStateOf(false) }
                var interceptSystemBack by remember(section) { mutableStateOf(false) }
                // Anchored entries share the same destination-side Morph host; ordinary settings
                // entries retain their platform/depth transition behavior.
                var transitionRequestClose by remember { mutableStateOf<(() -> Unit)?>(null) }
                val closeSettings: () -> Unit = { (transitionRequestClose ?: { finish() })() }
                val requestExit: () -> Unit = {
                    when (section) {
                        SettingsPage.Schedule -> scheduleExitRequest++
                        else -> closeSettings()
                    }
                }
                // Anchored schedule pages need the Activity host to route Back through the page
                // before asking the Morph to close. Ordinary clean pages keep the platform Back
                // path unregistered so predictive Back remains available.
                val anchoredScheduleBackRequired =
                    section == SettingsPage.Schedule && isAnchoredSettingsEntry
                BackHandler(
                    enabled = interceptSystemBack || anchoredScheduleBackRequired,
                    onBack = requestExit
                )
                Box(Modifier.fillMaxSize()) {
                val settingsDetailContent: @Composable () -> Unit = {
                DetailActivityScaffold(
                    title = section.title(),
                    config = state.config,
                    useSettingsBackground = true,
                    compactTopBar = section.usesPersistentCenteredSettingsTitle(),
                    centerCompactTitle = section.usesPersistentCenteredSettingsTitle(),
                    compactTitleMatchesSettings = section.usesPersistentCenteredSettingsTitle(),
                    topBarVisible = !widgetEditorVisible,
                    onBack = requestExit,
                    topBarActions = { topBackdrop ->
                        if (section == SettingsPage.AutoRefreshSchedule && autoRefreshProfile == null) {
                            TopGlassIconButton(
                                backdrop = topBackdrop,
                                config = settingsVisualConfig(state.config),
                                iconRes = R.drawable.ic_refresh,
                                contentDescription = "更新自动刷新适配列表",
                                onClick = { autoRefreshWarehouseRequest++ },
                                modifier = Modifier.size(SleepDownDesignTokens.SecondaryPage.BackButtonSize),
                                buttonHeight = 42.dp
                            )
                        }
                    }
                ) { backdrop ->
                    when (section) {
                        SettingsPage.General -> GeneralSettingsScreen(
                            state = state,
                            backdrop = backdrop,
                            onUpdateConfig = viewModel::saveGeneralSettings,
                            onOpenLiquidGlass = {
                                ActivityTransitionCoordinator.openImmediate(
                                    this@SettingsDetailActivityHost,
                                    TransitionRouteId.SettingsToSettingsDetail,
                                    Intent(this@SettingsDetailActivityHost, SettingsDetailActivity::class.java)
                                        .putExtra(SettingsDetailPageExtra, SettingsPage.LiquidGlass.name)
                                )
                            }
                        )
                        SettingsPage.LiquidGlass -> LiquidGlassSettingsScreen(
                            state = state,
                            backdrop = backdrop,
                            onUpdateBlurScale = viewModel::saveHomeChromeBlurScale
                        )
                        SettingsPage.Widgets -> WidgetCustomizationScreen(
                            state = state,
                            backdrop = backdrop,
                            onEditorVisibilityChange = { widgetEditorVisible = it }
                        )
                        SettingsPage.AiImport -> AiImportSettingsScreen(
                            state = state,
                            backdrop = backdrop
                        )
                        SettingsPage.DayAgent -> DayAgentSettingsScreen(state, backdrop)
                        SettingsPage.Schedule -> if (!scheduleEditReady) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        } else {
                            ScheduleConfigScreen(
                                scheduleEditState,
                                backdrop,
                                SettingsSection.Schedule,
                                onSave = { config, periods ->
                                    val targetId = customizeScheduleId
                                    if (targetId != null) {
                                        viewModel.saveConfigForSchedule(targetId, config, periods)
                                    } else {
                                        viewModel.saveConfig(config, periods)
                                    }
                                },
                                onPreviewLiveUpdate = viewModel::previewLiveUpdate,
                                exitCommitRequest = scheduleExitRequest,
                                onExitCommitFinished = { saved -> if (saved) closeSettings() },
                                onExitInterceptionChange = { interceptSystemBack = it }
                            )
                        }
                        SettingsPage.AutoRefreshSchedule -> AutoRefreshScheduleSettingsScreen(
                            state = state,
                            backdrop = backdrop,
                            warehouseRefreshRequest = autoRefreshWarehouseRequest
                        )
                        SettingsPage.Notifications -> ScheduleConfigScreen(
                            state = state,
                            backdrop = backdrop,
                            section = SettingsSection.Notifications,
                            onSave = { config, _ -> viewModel.saveNotificationSettings(config) },
                            onPreviewLiveUpdate = viewModel::previewLiveUpdate
                        )
                        SettingsPage.About -> ChangelogSettingsScreen(
                            state = state,
                            backdrop = backdrop,
							onPrivacyPolicy = {
								ActivityTransitionCoordinator.openImmediate(
                                    this@SettingsDetailActivityHost,
                                    TransitionRouteId.SettingsToSettingsDetail,
                                    Intent(this@SettingsDetailActivityHost, SettingsDetailActivity::class.java)
                                        .putExtra(SettingsDetailPageExtra, SettingsPage.PrivacyPolicy.name)
                                )
							}
                        )
                        SettingsPage.BackupRestore -> BackupRestoreSettingsScreen(
                            state = state,
                            backdrop = backdrop,
                            onOpenPreview = { source ->
                                ActivityTransitionCoordinator.openImmediate(
                                    this@SettingsDetailActivityHost,
                                    TransitionRouteId.SettingsToSettingsDetail,
                                    Intent(this@SettingsDetailActivityHost, SettingsDetailActivity::class.java)
                                        .setData(source)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        .putExtra(SettingsDetailPageExtra, SettingsPage.BackupPreview.name)
                                        .putExtra(BackupPreviewUriExtra, source.toString())
                                )
                            }
                        )
                        SettingsPage.BackupPreview -> if (backupPreviewSource != null) {
                            BackupRestorePreviewScreen(state, backdrop, backupPreviewSource)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "没有找到要预览的备份文件，请返回后重新选择。",
                                    modifier = Modifier.padding(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SettingsPage.Changelog -> ChangelogSettingsScreen(
                            state = state,
                            backdrop = backdrop,
							onPrivacyPolicy = {
								ActivityTransitionCoordinator.openImmediate(
                                    this@SettingsDetailActivityHost,
                                    TransitionRouteId.SettingsToSettingsDetail,
                                    Intent(this@SettingsDetailActivityHost, SettingsDetailActivity::class.java)
                                        .putExtra(SettingsDetailPageExtra, SettingsPage.PrivacyPolicy.name)
                                )
							}
                        )
						SettingsPage.PrivacyPolicy -> PrivacyPolicySettingsScreen(state, backdrop)
                        SettingsPage.ScheduleManager -> ScheduleManagerScreen(
                            state = state, backdrop = backdrop,
                            onCreateSchedule = { viewModel.createSchedule(it) },
                            onActivateSchedule = { id, finish -> viewModel.activateSchedule(id, finish) },
                            onRenameSchedule = { id, name -> viewModel.renameSchedule(id, name) },
                            onDeleteSchedule = { viewModel.deleteSchedule(it) }
                        )
                        SettingsPage.Root -> SettingsRootScreen(state, backdrop) {}
                    }
                }
                }
                if (isAnchoredSettingsEntry) {
                    CrossActivityTransitionHost(
                        activity = this@SettingsDetailActivityHost,
                        // A new Activity-local ViewModel starts from AppState(). Starting the
                        // Morph against that placeholder consumes the whole opening duration
                        // before the selected schedule arrives, which looks like a skipped
                        // transition. Precompose the real page, then release the existing motion.
                        openingReady = !isQuickSheetSettingsEntry || scheduleEditReady,
                        handleSystemBack = section != SettingsPage.Schedule,
                        sourceContent = {
                            // The QuickSheet route already supplies the exact composed button and
                            // full background frame. Painting a synthetic source card here would
                            // stretch a flat color across the expanding shell, so leave this layer
                            // transparent; other anchored settings entries retain their fallback.
                            if (!isQuickSheetSettingsEntry) {
                                val sourceDark = appUsesDarkTheme(state.config)
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (sourceDark) {
                                                ComposeColor(0xFF25272D)
                                            } else {
                                                ComposeColor(0xFFE9ECF2)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "详细设置",
                                        color = if (sourceDark) ComposeColor.White else ComposeColor(0xFF111111),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    ) { requestClose ->
                        if (transitionRequestClose == null) transitionRequestClose = requestClose
                        settingsDetailContent()
                    }
                } else {
                    settingsDetailContent()
                }
                AnimatedVisibility(
                    visible = editEntrySnapshotVisible,
                    enter = EnterTransition.None,
                    exit = fadeOut(animationSpec = tween(220)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(200f)
                ) {
                    editEntrySnapshot?.let { snapshot ->
                        Image(
                            bitmap = snapshot.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                }
            }
        }
    }
}

private fun scheduleConfigStateForEdit(state: AppState, scheduleId: Int): AppState {
    val targetConfig = state.allConfigs.firstOrNull { it.id == scheduleId }
        ?: state.config.takeIf { it.id == scheduleId }
        ?: defaultConfig(scheduleId)
    val targetPeriods = state.allPeriods.filter { it.scheduleId == scheduleId }
        .ifEmpty { state.periods.takeIf { targetConfig.id == state.config.id } ?: defaultPeriods(scheduleId) }
    return state.copy(config = targetConfig.copy(id = scheduleId), periods = targetPeriods)
}

open class EduSchoolSelectActivityHost : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityTransitionCoordinator.prepareDestinationBeforeOnCreate(this)
        super.onCreate(savedInstanceState)
        ActivityTransitionCoordinator.installDestinationWindowBackground(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContent {
            val app = application as CourseScheduleApp
            val viewModel: ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ScheduleViewModelFactory(app, app.repository)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val refreshScope = rememberCoroutineScope()
            var warehouseGeneration by remember { mutableIntStateOf(0) }
            var warehouseRefreshing by remember { mutableStateOf(false) }
            var warehouseManualRefreshing by remember { mutableStateOf(false) }
            var warehouseRefreshResultMessage by remember { mutableStateOf<String?>(null) }
            fun refreshWarehouse(manual: Boolean) {
                if (warehouseRefreshing) return
                warehouseRefreshing = true
                warehouseManualRefreshing = manual
                refreshScope.launch {
                    val beforeAdapters = if (manual) ShiguangWarehouse.loadAdapters(this@EduSchoolSelectActivityHost)
                        else emptyList()
                    runCatching {
                        if (manual) {
                            ShiguangWarehouseUpdater.refresh(this@EduSchoolSelectActivityHost)
                        } else {
                            ShiguangWarehouseUpdater.refreshIfStale(this@EduSchoolSelectActivityHost)
                        }
                    }.onSuccess { result ->
                        if (result == null) return@onSuccess
                        if (result.changed) warehouseGeneration += 1
                        if (manual) {
                            val afterAdapters = ShiguangWarehouse.loadAdapters(this@EduSchoolSelectActivityHost)
                            warehouseRefreshResultMessage = describeAdapterRefresh(
                                beforeAdapters, afterAdapters, result.changed
                            )
                        }
                    }.onFailure { error ->
                        if (manual) {
                            warehouseRefreshResultMessage =
                                "更新失败，继续使用当前适配列表：${error.message ?: "网络请求失败"}"
                        }
                    }
                    warehouseRefreshing = false
                    warehouseManualRefreshing = false
                }
            }
            LaunchedEffect(Unit) {
                if (ShiguangWarehouseUpdater.isRefreshStale(this@EduSchoolSelectActivityHost)) {
                    refreshWarehouse(manual = false)
                }
            }
            CourseScheduleTheme(config = state.config) {
                CrossActivityTransitionHost(
                    activity = this@EduSchoolSelectActivityHost,
                    sourceContent = {
                        HomeMenuActivitySourceFallback(
                            config = state.config,
                            highlightedRowIndex = 4
                        )
                    }
                ) { requestClose ->
                    DetailActivityScaffold(
                        title = "选择学校",
                        config = state.config,
                        onBack = requestClose,
                        topBarActions = { topBackdrop ->
                            TopGlassIconButton(
                                backdrop = topBackdrop,
                                config = settingsVisualConfig(state.config),
                                iconRes = R.drawable.ic_refresh,
                                contentDescription = if (warehouseRefreshing) "正在更新适配器" else "更新适配器",
                                onClick = { refreshWarehouse(manual = true) },
                                modifier = Modifier.size(SleepDownDesignTokens.SecondaryPage.BackButtonSize),
                                buttonHeight = 42.dp
                            )
                        }
                    ) { backdrop ->
                        EduSchoolPickerScreen(
                            state = state,
                            backdrop = backdrop,
                            warehouseGeneration = warehouseGeneration,
                            onSelect = { adapter ->
                                ActivityTransitionCoordinator.openImmediate(
                                    this@EduSchoolSelectActivityHost,
                                    TransitionRouteId.SchoolSelectToEduImport,
                                    Intent(this, EduImportActivity::class.java)
                                        .putExtra(EduAdapterExtra, adapter.toIntentKey())
                                )
                            }
                        )
                        if (warehouseManualRefreshing) {
                            LiquidAlertDialog(
                                title = "正在更新适配器",
                                message = "正在获取最新适配列表。现在可以返回或退到桌面，更新会在后台继续。",
                                actions = listOf(
                                    LiquidAlertAction(
                                        "后台继续",
                                        LiquidAlertActionStyle.Secondary,
                                        onClick = { warehouseManualRefreshing = false }
                                    )
                                ),
                                backdrop = backdrop,
                                config = state.config,
                                onDismissRequest = { warehouseManualRefreshing = false }
                            )
                        }
                        warehouseRefreshResultMessage?.let { result ->
                            LiquidAlertDialog(
                                title = "适配器刷新结果",
                                message = result,
                                actions = listOf(LiquidAlertAction("知道了", LiquidAlertActionStyle.Primary) {
                                    warehouseRefreshResultMessage = null
                                }),
                                backdrop = backdrop,
                                config = state.config,
                                onDismissRequest = { warehouseRefreshResultMessage = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
open class EduImportActivityHost : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val app = application as CourseScheduleApp
            val viewModel: ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ScheduleViewModelFactory(app, app.repository)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val adapter = remember { eduAdapterFromIntentKey(intent.getStringExtra(EduAdapterExtra)) }
            val retainedSession = remember(adapter) {
                if (adapter != null && intent.getBooleanExtra(AutoRefreshWebSession.RestoreSessionExtra, false)) {
                    AutoRefreshScheduleStore.load(app)?.takeIf {
                        it.schoolId == adapter.school.id && it.adapterId == adapter.adapterId
                    }
                } else null
            }
            val browserAdapter = remember(adapter, retainedSession) {
                adapter?.copy(importUrl = retainedSession?.authenticatedUrl ?: adapter.importUrl)
            }
            var pendingDraft by remember { mutableStateOf<ImportDraft?>(null) }
            val eduWebContentBackdrop = rememberGlassLayerBackdrop(
                domain = GlassBackdropDomain.Content,
                providerId = "edu-import-web-content"
            )
            CourseScheduleTheme(config = state.config) {
                if (pendingDraft == null) {
                    CompositionLocalProvider(LocalLegacyProgressiveBlur provides true) {
                    DetailActivityScaffold(
                        title = adapter?.school?.name ?: "教务导入",
                        config = state.config,
                        onBack = { finish() },
                        isolateContentFromBackdrop = true,
                        compactTopBar = true,
                        centerCompactTitle = true,
                        compactTitleMatchesSettings = true,
                        preserveStatusBarSpace = true,
                        topBarBackdropOverride = eduWebContentBackdrop
                    ) { backdrop ->
                        if (adapter == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = detailContentTopPadding())
                            ) {
                                MissingCourseScreen(onBack = { finish() })
                            }
                        } else {
                            EduImportActivityScreen(
                                state = state,
                                adapter = checkNotNull(browserAdapter),
                                backdrop = backdrop,
                                webContentBackdrop = eduWebContentBackdrop,
                                useDetailTopPadding = true,
                                prepareWebView = { webView ->
                                    retainedSession?.let {
                                        AutoRefreshWebSession.restore(webView, it)
                                    }
                                },
                                initialDesktopMode = retainedSession?.desktopMode ?: false,
                                onParsed = { draft -> pendingDraft = draft }
                            )
                        }
                    }
                    }
                } else {
                    val previewDraft = checkNotNull(pendingDraft)
                    DetailActivityScaffold(
                        title = "导入预览",
                        config = state.config,
                        onBack = { pendingDraft = null },
                        isolateContentFromBackdrop = true,
                        compactTopBar = true,
                        centerCompactTitle = true,
                        compactTitleMatchesSettings = true,
                        preserveStatusBarSpace = true
                    ) { backdrop ->
                    if (adapter == null) {
                        MissingCourseScreen(onBack = { finish() })
                    } else {
                        Box(modifier = Modifier.padding(top = detailContentTopPadding())) {
                            ConfirmScheduleScreen(
                                draft = previewDraft,
                                warning = if (adapter.isGeneralEduTool()) "可能部分节次信息会有误，请自行检查修改。" else null,
                                backdrop = backdrop,
                                onCancel = { pendingDraft = null },
                                onConfirm = { createNewSchedule ->
                                    viewModel.importDraft(previewDraft, createNewSchedule) {
                                        returnToScheduleHome()
                                    }
                                }
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidControlSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    onLiveValueChange: ((Float) -> Unit)? = null,
    snapValue: Float? = null,
    onSliderTouchActiveChange: (Boolean) -> Unit = {},
    visibilityThreshold: Float = 0.01f,
    enabled: Boolean = true,
    compactThumb: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val previewDispatchScope = rememberCoroutineScope()
    val latestLiveValueChange by rememberUpdatedState(onLiveValueChange)
    var pendingLiveValue by remember { mutableStateOf<Float?>(null) }
    var previewDispatchJob by remember { mutableStateOf<Job?>(null) }
    val safeSnapValue = snapValue?.coerceIn(valueRange)
    var localValue by remember(valueRange) { mutableFloatStateOf(value.coerceIn(valueRange)) }
    var previewModeActive by remember(valueRange) { mutableStateOf(false) }
    var lastSnapSide by remember(valueRange, safeSnapValue) {
        mutableIntStateOf(snapSide(value.coerceIn(valueRange), safeSnapValue, visibilityThreshold))
    }
    var snapZoneActive by remember(valueRange, safeSnapValue) {
        mutableStateOf(lastSnapSide == 0 && safeSnapValue != null)
    }

    fun updatePreview(candidate: Float) {
        val next = candidate.coerceIn(valueRange)
        val nextSide = snapSide(next, safeSnapValue, visibilityThreshold)
        val enteredSnapZone = safeSnapValue != null && nextSide == 0 && !snapZoneActive
        val crossedSnap = nextSide != 0 && !snapZoneActive &&
            lastSnapSide != 0 && nextSide != lastSnapSide
        if (enteredSnapZone || crossedSnap) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        snapZoneActive = safeSnapValue != null && nextSide == 0
        if (nextSide != 0) lastSnapSide = nextSide
        localValue = next
        pendingLiveValue = next
        if (previewDispatchJob?.isActive != true) {
            previewDispatchJob = previewDispatchScope.launch {
                withFrameNanos { }
                val latest = pendingLiveValue
                pendingLiveValue = null
                previewDispatchJob = null
                if (latest != null) latestLiveValueChange?.invoke(latest)
            }
        }
    }

    fun commit(candidate: Float) {
        val finalValue = candidate.coerceIn(valueRange)
        localValue = finalValue
        previewDispatchJob?.cancel()
        previewDispatchJob = null
        pendingLiveValue = null
        // The exact release value is never delayed behind frame conflation.
        latestLiveValueChange?.invoke(finalValue)
        onValueChange(finalValue)
    }

    LaunchedEffect(value, valueRange) {
        val externalValue = value.coerceIn(valueRange)
        if (!previewModeActive && abs(localValue - externalValue) > visibilityThreshold) {
            localValue = externalValue
            lastSnapSide = snapSide(externalValue, safeSnapValue, visibilityThreshold)
            snapZoneActive = safeSnapValue != null && lastSnapSide == 0
        }
    }

    SliderWithSnapMarker(
        modifier = modifier,
        snapValue = safeSnapValue,
        valueRange = valueRange
    ) {
        if (backdrop != null && enabled) {
            LiquidSlider(
                value = { localValue },
                onPreviewValueChange = ::updatePreview,
                onPreviewModeChange = { active ->
                    previewModeActive = active
                    onSliderTouchActiveChange(active)
                },
                onCommit = ::commit,
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                backdrop = backdrop,
                snapValue = safeSnapValue,
                compactThumb = compactThumb,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Slider(
                value = localValue,
                enabled = enabled,
                onValueChange = {
                    if (!previewModeActive) {
                        previewModeActive = true
                        onSliderTouchActiveChange(true)
                    }
                    updatePreview(it)
                },
                onValueChangeFinished = {
                    commit(localValue)
                    if (previewModeActive) {
                        previewModeActive = false
                        onSliderTouchActiveChange(false)
                    }
                },
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun snapSide(candidate: Float, snapValue: Float?, threshold: Float): Int {
    val snap = snapValue ?: return 0
    return when {
        candidate < snap - threshold -> -1
        candidate > snap + threshold -> 1
        else -> 0
    }
}

@Composable
private fun SliderWithSnapMarker(
    modifier: Modifier,
    snapValue: Float?,
    valueRange: ClosedFloatingPointRange<Float>,
    content: @Composable () -> Unit
) {
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        content()
        val snap = snapValue ?: return@BoxWithConstraints
        val fraction = ((snap - valueRange.start) /
            (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val radius = 2.5.dp.toPx()
            val visualFraction = if (isLtr) fraction else 1f - fraction
            val centerX = radius +
                (size.width - radius * 2f).coerceAtLeast(0f) * visualFraction
            drawCircle(
                color = ComposeColor.White.copy(alpha = 0.78f),
                radius = radius,
                center = Offset(centerX, size.height / 2f)
            )
        }
    }
}
@Composable
fun LiquidControlToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val alpha = if (enabled) 1f else 0.40f
    if (backdrop != null) {
        LiquidToggle(
            selected = { checked },
            onSelect = { if (enabled) onCheckedChange(it) },
            backdrop = backdrop,
            compact = compact,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp).graphicsLayer { this.alpha = alpha }
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            modifier = modifier.heightIn(min = 48.dp).graphicsLayer {
                this.alpha = alpha
                scaleX = if (compact) 0.8f else 1f
                scaleY = if (compact) 0.8f else 1f
            }
        )
    }
}

@Composable
fun DockItem(selected: Boolean, backdrop: Backdrop?, config: ScheduleConfigEntity, iconRes: Int, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .clip(Capsule())
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .height(60.dp)
            .width(78.dp),
        contentAlignment = Alignment.Center
    ) {
        if (pressed) {
            GlassLens(
                backdrop = backdrop,
                config = config,
                pressProgress = 1f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    state: AppState,
    backdrop: Backdrop?,
    onPageChange: (SettingsPage) -> Unit,
    onSave: (ScheduleConfigEntity, List<PeriodEntity>) -> Unit,
    onUpdateConfig: (ScheduleConfigEntity) -> Unit,
    onUpdateGeneralConfig: (ScheduleConfigEntity) -> Unit = onUpdateConfig,
    onUpdateHomeChromeBlurScale: (Float) -> Unit,
    onPreviewLiveUpdate: (ScheduleConfigEntity) -> Unit,
    onCreateSchedule: (String) -> Unit = {},
    onActivateSchedule: (Int, (() -> Unit)?) -> Unit = { _, _ -> },
    onRenameSchedule: (Int, String) -> Unit = { _, _ -> },
    onDeleteSchedule: (Int) -> Unit = {},
    exitCommitRequest: Int = 0,
    onExitCommitFinished: (Boolean) -> Unit = {},
    onExitInterceptionChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val pageConfig = settingsVisualConfig(state.config)
    val pageState = state.copy(config = pageConfig)
    val adaptiveMetrics = rememberHomeAdaptiveMetrics()
    var backupPreviewUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var autoRefreshWarehouseRequest by remember { mutableIntStateOf(0) }
    val autoRefreshProfile = AutoRefreshScheduleStore.observe(context).collectAsStateWithLifecycle().value
    GlassMiuixSettingsTheme(pageConfig) {
        if (page == SettingsPage.Root && adaptiveMetrics.isLargeScreen) {
            var tabletNavigation by rememberSaveable(
                stateSaver = TabletSettingsNavigationStateSaver
            ) {
                mutableStateOf(TabletSettingsNavigationState())
            }
            var detailNavigationDirection by remember { mutableIntStateOf(0) }
            var tabletWidgetEditorVisible by remember { mutableStateOf(false) }
            var pendingPageName by remember { mutableStateOf<String?>(null) }
            var scheduleExitInFlight by remember { mutableStateOf(false) }
            var scheduleExitRequest by remember { mutableIntStateOf(0) }
            var externalExitInFlight by remember { mutableStateOf(false) }
            var lastHandledExternalExitRequest by remember { mutableIntStateOf(exitCommitRequest) }
            var exitInterceptionByPage by remember {
                mutableStateOf<Map<SettingsPage, Boolean>>(emptyMap())
            }
            val selectedPage = tabletNavigation.rootPage
            val displayedPage = tabletNavigation.displayedPage
            val portrait = adaptiveMetrics.screenHeight > adaptiveMetrics.screenWidth
            val navigationWidth = if (portrait) {
                (adaptiveMetrics.screenWidth * 0.38f).coerceIn(280.dp, 328.dp)
            } else {
                (adaptiveMetrics.screenWidth * 0.31f).coerceIn(336.dp, 408.dp)
            }
            LaunchedEffect(displayedPage) {
                if (displayedPage != SettingsPage.Widgets) tabletWidgetEditorVisible = false
            }
            val interceptRootExit = tabletNavigation.detailPages.isEmpty() &&
                exitInterceptionByPage[selectedPage] == true
            LaunchedEffect(interceptRootExit) {
                onExitInterceptionChange(interceptRootExit)
            }
            DisposableEffect(onExitInterceptionChange) {
                onDispose { onExitInterceptionChange(false) }
            }
            fun applyTabletRootPage(nextPage: SettingsPage) {
                detailNavigationDirection = 0
                tabletNavigation = tabletNavigation.selectRoot(nextPage)
                backupPreviewUriString = null
                pendingPageName = null
            }
            fun requestTabletRootPage(nextPage: SettingsPage) {
                if (
                    tabletNavigation.detailPages.isEmpty() &&
                    exitInterceptionByPage[selectedPage] == true
                ) {
                    pendingPageName = nextPage.name
                    if (!scheduleExitInFlight) {
                        scheduleExitInFlight = true
                        scheduleExitRequest++
                    }
                } else {
                    applyTabletRootPage(nextPage)
                }
            }
            fun pushTabletDetailPage(nextPage: SettingsPage) {
                val nextNavigation = tabletNavigation.pushDetail(nextPage)
                if (nextNavigation == tabletNavigation) return
                detailNavigationDirection = 1
                tabletNavigation = nextNavigation
            }
            fun popTabletDetailPage() {
                val nextNavigation = tabletNavigation.popDetail()
                if (nextNavigation == tabletNavigation) return
                detailNavigationDirection = -1
                tabletNavigation = nextNavigation
            }
            LaunchedEffect(exitCommitRequest) {
                if (exitCommitRequest == lastHandledExternalExitRequest) return@LaunchedEffect
                lastHandledExternalExitRequest = exitCommitRequest
                if (
                    tabletNavigation.detailPages.isEmpty() &&
                    exitInterceptionByPage[selectedPage] == true
                ) {
                    externalExitInFlight = true
                    if (!scheduleExitInFlight) {
                        scheduleExitInFlight = true
                        scheduleExitRequest++
                    }
                } else {
                    onExitCommitFinished(true)
                }
            }
            BackHandler(enabled = tabletNavigation.detailPages.isNotEmpty()) {
                popTabletDetailPage()
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(settingsPageBackground(pageConfig))
            ) {
                Box(Modifier.width(navigationWidth).fillMaxHeight()) {
                    SettingsRootScreen(
                        state = pageState,
                        backdrop = backdrop,
                        selectedPage = selectedPage,
                        onPageChange = ::requestTabletRootPage
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f))
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    AnimatedContent(
                        targetState = displayedPage,
                        transitionSpec = {
                            when {
                                detailNavigationDirection > 0 -> {
                                    slideInHorizontally(
                                        tween(500, easing = MiuixSettingsNavigationMotion)
                                    ) { width -> width } togetherWith
                                        slideOutHorizontally(
                                            tween(500, easing = MiuixSettingsNavigationMotion)
                                        ) { width -> -width / 4 }
                                }
                                detailNavigationDirection < 0 -> {
                                    slideInHorizontally(
                                        tween(500, easing = MiuixSettingsNavigationMotion)
                                    ) { width -> -width / 4 } togetherWith
                                        slideOutHorizontally(
                                            tween(500, easing = MiuixSettingsNavigationMotion)
                                        ) { width -> width }
                                }
                                else -> EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "TabletSettingsDetailNavigation"
                    ) { targetPage ->
                        GlassMiuixTabletDetailPaneScaffold(
                            title = targetPage.title(),
                            config = pageConfig,
                            topBarVisible = !(targetPage == SettingsPage.Widgets && tabletWidgetEditorVisible),
                            showBackButton = targetPage != selectedPage,
                            useMiuixCollapsedTitleStyle =
                                targetPage.usesPersistentCenteredSettingsTitle(),
                            onBack = ::popTabletDetailPage,
                            topBarActions = { topBackdrop ->
                                if (targetPage == SettingsPage.AutoRefreshSchedule && autoRefreshProfile == null) {
                                    TopGlassIconButton(
                                        backdrop = topBackdrop,
                                        config = pageConfig,
                                        iconRes = R.drawable.ic_refresh,
                                        contentDescription = "更新自动刷新适配列表",
                                        onClick = { autoRefreshWarehouseRequest++ },
                                        modifier = Modifier.size(SleepDownDesignTokens.SecondaryPage.BackButtonSize),
                                        buttonHeight = 42.dp
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { paneBackdrop ->
                            SettingsPageContent(
                                page = targetPage,
                                state = state,
                                pageState = pageState,
                                backdrop = paneBackdrop,
                                onPageChange = ::pushTabletDetailPage,
                                onSave = onSave,
                                onUpdateConfig = onUpdateConfig,
                                onUpdateGeneralConfig = onUpdateGeneralConfig,
                                onUpdateHomeChromeBlurScale = onUpdateHomeChromeBlurScale,
                                onPreviewLiveUpdate = onPreviewLiveUpdate,
                                onCreateSchedule = onCreateSchedule,
                                onActivateSchedule = onActivateSchedule,
                                onRenameSchedule = onRenameSchedule,
                                onDeleteSchedule = onDeleteSchedule,
                                onWidgetEditorVisibilityChange = { tabletWidgetEditorVisible = it },
                                backupPreviewUri = backupPreviewUriString?.toUri(),
                                onOpenBackupPreview = { source ->
                                    backupPreviewUriString = source.toString()
                                    pushTabletDetailPage(SettingsPage.BackupPreview)
                                },
                                exitCommitRequest = scheduleExitRequest,
                                onExitCommitFinished = { completed ->
                                    scheduleExitInFlight = false
                                    scheduleExitRequest = 0
                                    if (externalExitInFlight) {
                                        externalExitInFlight = false
                                        pendingPageName = null
                                        onExitCommitFinished(completed)
                                    } else {
                                        val destination = pendingPageName
                                        pendingPageName = null
                                        if (completed && destination != null) {
                                            applyTabletRootPage(
                                                runCatching { SettingsPage.valueOf(destination) }
                                                    .getOrDefault(SettingsPage.General)
                                            )
                                        }
                                    }
                                },
                                onExitInterceptionChange = { needsInterception ->
                                    exitInterceptionByPage = exitInterceptionByPage.toMutableMap().apply {
                                        if (needsInterception) put(targetPage, true) else remove(targetPage)
                                    }
                                },
                                autoRefreshWarehouseRequest = autoRefreshWarehouseRequest
                            )
                        }
                    }
                }
            }
        } else {
            SettingsPageContent(
                page = page,
                state = state,
                pageState = pageState,
                backdrop = backdrop,
                onPageChange = onPageChange,
                onSave = onSave,
                onUpdateConfig = onUpdateConfig,
                onUpdateGeneralConfig = onUpdateGeneralConfig,
                onUpdateHomeChromeBlurScale = onUpdateHomeChromeBlurScale,
                onPreviewLiveUpdate = onPreviewLiveUpdate,
                onCreateSchedule = onCreateSchedule,
                onActivateSchedule = onActivateSchedule,
                onRenameSchedule = onRenameSchedule,
                onDeleteSchedule = onDeleteSchedule,
                backupPreviewUri = backupPreviewUriString?.toUri(),
                onOpenBackupPreview = { source ->
                    backupPreviewUriString = source.toString()
                    onPageChange(SettingsPage.BackupPreview)
                },
                exitCommitRequest = exitCommitRequest,
                onExitCommitFinished = onExitCommitFinished,
                onExitInterceptionChange = onExitInterceptionChange
            )
        }
    }
}

@Composable
private fun SettingsPageContent(
    page: SettingsPage,
    state: AppState,
    pageState: AppState,
    backdrop: Backdrop?,
    onPageChange: (SettingsPage) -> Unit,
    onSave: (ScheduleConfigEntity, List<PeriodEntity>) -> Unit,
    onUpdateConfig: (ScheduleConfigEntity) -> Unit,
    onUpdateGeneralConfig: (ScheduleConfigEntity) -> Unit,
    onUpdateHomeChromeBlurScale: (Float) -> Unit,
    onPreviewLiveUpdate: (ScheduleConfigEntity) -> Unit,
    onCreateSchedule: (String) -> Unit,
    onActivateSchedule: (Int, (() -> Unit)?) -> Unit,
    onRenameSchedule: (Int, String) -> Unit,
    onDeleteSchedule: (Int) -> Unit,
    onWidgetEditorVisibilityChange: (Boolean) -> Unit = {},
    backupPreviewUri: Uri? = null,
    onOpenBackupPreview: (Uri) -> Unit = {},
    exitCommitRequest: Int = 0,
    onExitCommitFinished: (Boolean) -> Unit = {},
    onExitInterceptionChange: (Boolean) -> Unit = {},
    autoRefreshWarehouseRequest: Int = 0
) {
    when (page) {
        SettingsPage.Root -> SettingsRootScreen(pageState, backdrop, onPageChange = onPageChange)
        SettingsPage.General -> GeneralSettingsScreen(
            state = state,
            backdrop = backdrop,
            onUpdateConfig = onUpdateGeneralConfig,
            onOpenLiquidGlass = { onPageChange(SettingsPage.LiquidGlass) },
            exitCommitRequest = exitCommitRequest,
            onExitCommitFinished = onExitCommitFinished
        )
        SettingsPage.LiquidGlass -> LiquidGlassSettingsScreen(
            state = state,
            backdrop = backdrop,
            onUpdateBlurScale = onUpdateHomeChromeBlurScale
        )
        SettingsPage.Widgets -> WidgetCustomizationScreen(
            state,
            backdrop,
            onEditorVisibilityChange = onWidgetEditorVisibilityChange
        )
        SettingsPage.AiImport -> AiImportSettingsScreen(
            state = state,
            backdrop = backdrop,
            exitCommitRequest = exitCommitRequest,
            onExitCommitFinished = onExitCommitFinished
        )
        SettingsPage.DayAgent -> DayAgentSettingsScreen(state, backdrop)
        SettingsPage.Schedule -> ScheduleConfigScreen(
            state = state,
            backdrop = backdrop,
            section = SettingsSection.Schedule,
            onSave = onSave,
            onPreviewLiveUpdate = onPreviewLiveUpdate,
            exitCommitRequest = exitCommitRequest,
            onExitCommitFinished = onExitCommitFinished,
            onExitInterceptionChange = onExitInterceptionChange
        )
        SettingsPage.AutoRefreshSchedule -> AutoRefreshScheduleSettingsScreen(
            state = state,
            backdrop = backdrop,
            warehouseRefreshRequest = autoRefreshWarehouseRequest
        )
        SettingsPage.Notifications -> ScheduleConfigScreen(
            state = state,
            backdrop = backdrop,
            section = SettingsSection.Notifications,
            onSave = { config, _ -> onUpdateConfig(config) },
            onPreviewLiveUpdate = onPreviewLiveUpdate,
            exitCommitRequest = exitCommitRequest,
            onExitCommitFinished = onExitCommitFinished
        )
        SettingsPage.ScheduleManager -> ScheduleManagerScreen(
            state,
            backdrop,
            onCreateSchedule,
            onActivateSchedule,
            onRenameSchedule,
            onDeleteSchedule
        )
        SettingsPage.BackupRestore -> BackupRestoreSettingsScreen(
            state = pageState,
            backdrop = backdrop,
            onOpenPreview = onOpenBackupPreview
        )
        SettingsPage.BackupPreview -> if (backupPreviewUri != null) {
            BackupRestorePreviewScreen(pageState, backdrop, backupPreviewUri)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "没有找到要预览的备份文件，请返回后重新选择。",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SettingsPage.About -> ChangelogSettingsScreen(
            pageState,
            backdrop,
			onPrivacyPolicy = { onPageChange(SettingsPage.PrivacyPolicy) }
        )
        SettingsPage.Changelog -> ChangelogSettingsScreen(
            pageState,
            backdrop,
			onPrivacyPolicy = { onPageChange(SettingsPage.PrivacyPolicy) }
        )
		SettingsPage.PrivacyPolicy -> PrivacyPolicySettingsScreen(pageState, backdrop)
    }
}

@Composable
fun SettingsRootScreen(
    state: AppState,
    backdrop: Backdrop?,
    selectedPage: SettingsPage? = null,
    onPageChange: (SettingsPage) -> Unit
) {
    val context = LocalContext.current
    val darkTheme = appUsesDarkTheme(state.config)
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }
    val appName = remember {
        runCatching {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        }.getOrDefault(context.getString(R.string.app_name))
    }
    val iconRevision by com.xiaomanjun.sleepdownschedule.core.identity.AppIconManager.changes.collectAsState()
    val appIconRes = remember(darkTheme, iconRevision) { currentIconResId(context, darkTheme) }
    val appIconDrawable = remember(context, appIconRes) {
        runCatching { context.getDrawable(appIconRes) }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
    }
    GlassMiuixRootSettingsScaffold(
        title = "设置",
        config = state.config
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = DockScrollPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth().homeSwitchGroup()) {
                top.yukonga.miuix.kmp.preference.ArrowPreference(
                    title = appName,
                    summary = "版本、来源与许可信息",
                    startAction = {
                        AndroidView(
                            factory = { viewContext ->
                                ImageView(viewContext).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    contentDescription = context.getString(R.string.app_icon_content_description)
                                }
                            },
                            update = { imageView -> imageView.setImageDrawable(appIconDrawable) },
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(56.dp)
                                .clip(RoundedRectangle(16.dp))
                        )
                    },
                    endActions = {
                        Text(
                            "版本 $versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            softWrap = true,
                            textAlign = TextAlign.End
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (selectedPage == SettingsPage.Changelog) 0.10f else 0f
                            )
                        ),
                    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = { onPageChange(SettingsPage.Changelog) }
                )
            }
        }
        item {
            GlassPreferenceSection("应用", modifier = Modifier.homeSwitchGroup()) {
                SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        "通用设置",
                        "深色模式与系统外观",
                        selected = selectedPage == SettingsPage.General,
                        onClick = { onPageChange(SettingsPage.General) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        "小组件设置",
                        "自定义小组件背景",
                        selected = selectedPage == SettingsPage.Widgets,
                        onClick = { onPageChange(SettingsPage.Widgets) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        "当前课表详细设置",
                        "编辑当前课表的周数、节次与显示规则",
                        selected = selectedPage == SettingsPage.Schedule,
                        onClick = { onPageChange(SettingsPage.Schedule) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        "自动刷新课表",
                        "连接教务系统，手动或定时同步课程",
                        selected = selectedPage == SettingsPage.AutoRefreshSchedule,
                        onClick = { onPageChange(SettingsPage.AutoRefreshSchedule) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        "通知设置",
                        "上课提醒与实时活动",
                        selected = selectedPage == SettingsPage.Notifications,
                        onClick = { onPageChange(SettingsPage.Notifications) }
                    )
                }
            }
        }
        item {
            GlassPreferenceSection("智能助手", modifier = Modifier.homeSwitchGroup()) {
                SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        "AI 设置",
                        "配置智能功能共用的服务商、模型和 API Key。",
                        selected = selectedPage == SettingsPage.AiImport,
                        onClick = { onPageChange(SettingsPage.AiImport) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        "AI助理",
                        "管理日视图助手、天气与预警。",
                        selected = selectedPage == SettingsPage.DayAgent,
                        onClick = { onPageChange(SettingsPage.DayAgent) }
                    )
                }
            }
        }
        item {
            GlassPreferenceSection("其他", modifier = Modifier.homeSwitchGroup()) {
                SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        "备份与恢复",
                        "保存课表和设置，或从备份恢复",
                        selected = selectedPage == SettingsPage.BackupRestore,
                        onClick = { onPageChange(SettingsPage.BackupRestore) }
                    )
                    SettingsDivider()
                }
            }
        }
        }
    }

}

@Composable
fun ScheduleManagerScreen(
    state: AppState,
    backdrop: Backdrop?,
    onCreateSchedule: (String) -> Unit,
    onActivateSchedule: (Int, (() -> Unit)?) -> Unit,
    onRenameSchedule: (Int, String) -> Unit,
    onDeleteSchedule: (Int) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val topPadding = detailContentTopPadding()
    val coursesPerSchedule = remember(state.allCourses) {
        state.allCourses.groupBy { it.scheduleId }.mapValues { it.value.size }
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = DockScrollPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "课表设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                SettingsActionButton("新建课表", backdrop, onClick = { showCreateDialog = true })
            }
        }
        item {
            SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                state.schedules.forEachIndexed { idx, profile ->
                    val courseCount = coursesPerSchedule[profile.id] ?: 0
                    val isActive = profile.isActive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                context.openRegisteredActivity(
                                    TransitionRouteId.SettingsToSettingsDetail,
                                    Intent(context, SettingsDetailActivity::class.java)
                                        .putExtra(SettingsDetailPageExtra, SettingsPage.Schedule.name)
                                        .putExtra(ScheduleCustomizeIdExtra, profile.id)
                                )
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isActive) {
                                    Text(
                                        "当前",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedRectangle(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "$courseCount 门课程",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isActive) {
                                SettingsActionButton("设为", backdrop, onClick = {
                                    onActivateSchedule(profile.id, null)
                                })
                            }
                            SettingsActionButton("重命名", backdrop, onClick = {
                                renameTarget = Pair(profile.id, profile.name)
                            })
                            if (!isActive && state.schedules.size > 1) {
                                SettingsActionButton("删除", backdrop, onClick = {
                                    deleteTarget = Pair(profile.id, profile.name)
                                }, destructive = true)
                            }
                        }
                    }
                    if (idx != state.schedules.lastIndex) SettingsDivider()
                }
            }
        }
    }

    if (showCreateDialog) {
        ScheduleNameDialog(
            title = "新建课表",
            initialName = "",
            backdrop = backdrop,
            config = state.config,
            onConfirm = { name ->
                showCreateDialog = false
                if (name.isNotBlank()) onCreateSchedule(name)
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    renameTarget?.let { (id, name) ->
        ScheduleNameDialog(
            title = "重命名课表",
            initialName = name,
            backdrop = backdrop,


            config = state.config,
            onConfirm = { newName ->
                renameTarget = null
                if (newName.isNotBlank()) onRenameSchedule(id, newName)
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { (id, name) ->
        LiquidAlertDialog(
            title = "删除课表",
            message = "确定要删除「$name」吗？该课表下的所有课程都会被删除，且无法恢复。",
            actions = listOf(
                LiquidAlertAction("取消", LiquidAlertActionStyle.Secondary) { deleteTarget = null },
                LiquidAlertAction("确认删除", LiquidAlertActionStyle.Destructive) {
                    val targetId = id
                    deleteTarget = null
                    onDeleteSchedule(targetId)
                }
            ),
            backdrop = backdrop,
            config = state.config,
            onDismissRequest = { deleteTarget = null }
        )
    }
}

@Composable
fun ScheduleNameDialog(
    title: String,
    initialName: String,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val textColor = glassForegroundColor(config)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CenterLiquidDialog(backdrop = backdrop, config = config) {
            LiquidDialogHeader(title, onDismiss, backdrop, config, onConfirm = { onConfirm(name.trim()) })
            DialogCapsuleField(name, { name = it }, "课表名称", config, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun AboutSettingsScreen(state: AppState, backdrop: Backdrop?) {
    ChangelogSettingsScreen(state = state, backdrop = backdrop)
}

@Composable
private fun AboutGlassPanel(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    longContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedRectangle(28.dp)
    val panelGradient = if (darkTheme) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            )
        )
    }
    Column(
        modifier = modifier
            .then(if (longContent) Modifier.drawWithCache {
                val path = androidx.compose.ui.graphics.Path().apply {
                    addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
                }
                onDrawWithContent {
                    clipPath(path) { this@onDrawWithContent.drawContent() }
                }
            } else Modifier.clip(shape))
            .background(panelGradient)
            .border(
                width = 1.dp,
                color = ComposeColor.White.copy(alpha = if (darkTheme) 0.22f else 0.88f),
                shape = shape
            ),
        content = content
    )
}

@Composable
private fun AboutSectionHeading(
    title: String,
    summary: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun AboutFeatureCard(
    imageRes: Int,
    eyebrow: String,
    title: String
) {
    val imageBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Content,
        providerId = "about-feature-image"
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideCard = maxWidth >= 560.dp
        val cardRatio = if (wideCard) 16f / 8.5f else 1.08f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardRatio)
                .squircleClip(28.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropProducer(imageBackdrop),
                contentScale = ContentScale.Crop
            )
            ProgressiveBackdropBlur(
                backdrop = imageBackdrop,
                modifier = Modifier.align(Alignment.BottomCenter),
                tintColor = ComposeColor.Black,
                height = if (wideCard) 108.dp else 120.dp,
                blurRadius = 7.dp,
                tintIntensity = 0.07f,
                direction = ProgressiveBlurDirection.BottomToTop,
                topMaskFadeStart = 0.18f,
                topMaskFadeEnd = 0.96f,
                topTintFadeStart = 0.14f,
                topTintFadeEnd = 0.98f,
                fallbackTintStops = listOf(
                    0f to ComposeColor.Black.copy(alpha = 0.52f),
                    0.56f to ComposeColor.Black.copy(alpha = 0.20f),
                    1f to ComposeColor.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to ComposeColor.Black.copy(alpha = 0.03f),
                                0.62f to ComposeColor.Black.copy(alpha = 0.05f),
                                1f to ComposeColor.Black.copy(alpha = 0.50f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = ComposeColor(0xFF8CCBFF)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ComposeColor.White
                )
            }
        }
    }
}

@Composable
private fun AboutHero(
    appName: String,
    versionName: String,
    iconResId: Int,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val appIconDrawable = remember(context, iconResId) {
        runCatching { context.getDrawable(iconResId) }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 380.dp
        AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                if (compact) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AboutAppIcon(
                            appName = appName,
                            drawable = appIconDrawable,
                            modifier = Modifier.size(82.dp).clip(RoundedRectangle(24.dp))
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "课程、待办与日程，一处安排",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AboutAppIcon(
                            appName = appName,
                            drawable = appIconDrawable,
                            modifier = Modifier.size(82.dp).clip(RoundedRectangle(24.dp))
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "课程、待办与日程，一处安排",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前版本", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v$versionName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("最低支持", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Android 8.0+", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutAppIcon(
    appName: String,
    drawable: android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "$appName 应用图标"
            }
        },
        update = { imageView -> imageView.setImageDrawable(drawable) },
        modifier = modifier
    )
}

@Composable
fun ChangelogSettingsScreen(
    state: AppState,
    backdrop: Backdrop?,
    onPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val darkTheme = appUsesDarkTheme(state.config)
    val iconRevision by com.xiaomanjun.sleepdownschedule.core.identity.AppIconManager.changes.collectAsState()
    val appIconRes = remember(context, darkTheme, iconRevision) { currentIconResId(context, darkTheme) }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = detailContentTopPadding(),
            bottom = DockScrollPadding
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AboutHero(
                appName = context.getString(R.string.app_name),
                versionName = BuildConfig.VERSION_NAME,
                iconResId = appIconRes,
                darkTheme = darkTheme
            )
        }
        if (BuildConfig.DISTRIBUTION_CHANNEL == "github") {
            item {
                SchedulePlusUpdatePanel(darkTheme = darkTheme)
            }
        }
        item {
            AboutSectionHeading(title = "本次更新 · 1.0.2", summary = "确认保存更可靠。")
            AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow("待办保存", "主任务和子任务一起保存，保存过程中避免重复提交；离开页面后仍会完成已确认的保存。")
                SettingsDivider()
                SettingsInfoRow("AI 确认创建", "区分任务已保存与提醒、日历同步结果，避免同步失败后重复创建。")
                SettingsDivider()
                SettingsInfoRow("日历同步", "清理前核对最新任务状态，避免误删刚重新安排的事件。")
            }
        }
        item {
            AboutSectionHeading(title = "应用能力", summary = "围绕课程、任务和日常时间安排。")
            AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow("课程表", "多课表、教务导入、手动编辑、课程提醒、备份恢复与桌面小组件。")
                SettingsDivider()
                SettingsInfoRow("待办事项", "支持分组、子任务、优先级、重复规则、置顶、课程关联、日历视图与桌面小组件。")
                SettingsDivider()
                SettingsInfoRow("界面体验", "液态玻璃视觉体系，适配浅色、深色、壁纸背景与不同屏幕尺寸。")
            }
        }
        item {
            AboutSectionHeading(title = "数据与隐私", summary = "本机优先，联网功能由你主动启用。")
            AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    "本机优先",
                    "课程、待办、分组与设置默认只保存在设备本地。AI 提取和系统日历同步仅在你主动启用后使用。"
                )
                SettingsDivider()
                SettingsNavigationRow(
                    "隐私说明",
                    "查看 AI、日历权限与本机数据的处理方式",
                    onClick = onPrivacyPolicy
                )
            }
        }
        item {
            AboutSectionHeading(title = "界面与开源组件", summary = "液态玻璃材质与组件信息。")
            AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    "液态玻璃",
                    "时序清单使用 AndroidLiquidGlass / Backdrop 构建背景采样、折射与高光效果。"
                )
                SettingsDivider()
                SettingsNavigationRow(
                    "玻璃组件",
                    "查看 AndroidLiquidGlass 项目页面",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Kyant0/AndroidLiquidGlass")
                                )
                            )
                        }
                    }
                )
                SettingsDivider()
                SettingsInfoRow("许可证", "完整许可与第三方声明见项目仓库中的 LICENSE.md 和 THIRD_PARTY_NOTICES.md。")
                SettingsDivider()
                SettingsNavigationRow(
                    "项目来源",
                    "基于 SleepDown课程表修改；原作者 xiaomanjun233；非官方修改版。",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xiaomanjun233/SleepDown-Schedule")))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SchedulePlusUpdatePanel(darkTheme: Boolean) {
    val context = LocalContext.current
    val viewModel: com.xiaomanjun.sleepdownschedule.feature.update.SchedulePlusUpdateViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val update = state.update
    val downloadedApk = state.downloadedApkPath?.let { path -> java.io.File(path) }

    AboutSectionHeading(title = "版本更新")
    AboutGlassPanel(darkTheme = darkTheme, modifier = Modifier.fillMaxWidth()) {
        SettingsInfoRow("更新来源", "只从时序清单的 GitHub Release 获取更新。")
        SettingsDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("接收 Beta 版本", style = MaterialTheme.typography.titleSmall)
                Text(
                    "关闭时只检查正式版",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.channel == com.xiaomanjun.sleepdownschedule.feature.update.SchedulePlusUpdateChannel.BETA,
                onCheckedChange = viewModel::setBetaChannel
            )
        }
        SettingsDivider()
        SettingsInfoRow("更新状态", state.message)
        update?.let { available ->
            SettingsDivider()
            SettingsInfoRow(
                "${if (available.isPrerelease) "Beta" else "正式版"} ${available.versionName}",
                available.releaseNotes.ifBlank { "此版本没有附带更新说明。" }
            )
            SettingsDivider()
            SettingsNavigationRow(
                "下载并校验更新",
                if (state.isDownloading) "正在下载……" else "下载后先校验 SHA-256，不会静默安装",
                onClick = viewModel::downloadUpdate
            )
        }
        downloadedApk?.let { file ->
            SettingsDivider()
            SettingsNavigationRow(
                "安装已校验的更新",
                "Android 会显示系统安装确认；首次使用可能需要授权此来源安装应用",
                onClick = {
                    val available = state.update ?: return@SettingsNavigationRow
                    runCatching {
                        com.xiaomanjun.sleepdownschedule.feature.update.SchedulePlusUpdateInstaller
                            .install(context, available, file)
                    }.onSuccess { result ->
                        if (result == com.xiaomanjun.sleepdownschedule.feature.update.InstallRequestResult.PERMISSION_REQUIRED) {
                            Toast.makeText(context, "授权后返回此页面，再点一次“安装已校验的更新”。", Toast.LENGTH_LONG).show()
                        }
                    }.onFailure { error ->
                        Toast.makeText(context, error.message ?: "无法启动系统安装器", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
        SettingsDivider()
        SettingsNavigationRow(
            "检查更新",
            if (state.isChecking) "正在检查……" else "${state.message} · 当前 ${BuildConfig.VERSION_NAME}",
            onClick = viewModel::checkForUpdates
        )
    }
}

private fun Context.resolveSleepDownCustomTabsPackage(): String? {
    return CustomTabsClient.getPackageName(
        this,
        listOf("com.microsoft.emmx"),
        true
    ) ?: CustomTabsClient.getPackageName(this, emptyList())
}

private fun Context.openSleepDownCustomTab(
    url: String,
    providerPackage: String?,
    initialWidthPx: Int,
    darkTheme: Boolean
) {
    val chromeColor = if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    runCatching {
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setColorScheme(
                if (darkTheme) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT
            )
            .setToolbarColor(chromeColor)
            .setNavigationBarColor(chromeColor)
            .setInitialActivityWidthPx(initialWidthPx)
            .setActivitySideSheetBreakpointDp(600)
            .setActivitySideSheetPosition(CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_END)
            .setActivitySideSheetDecorationType(CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER)
            .setActivitySideSheetRoundedCornersPosition(CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP)
            .setActivitySideSheetMaximizationEnabled(false)
            // AndroidX Browser accepts toolbar radii in the inclusive 0..16 dp range.
            .setToolbarCornerRadiusDp(16)
            .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_START)
            .setBackgroundInteractionEnabled(false)
            .build()
            .also { intent -> providerPackage?.let(intent.intent::setPackage) }
        customTab.launchUrl(this, url.toUri())
    }.onFailure {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }.onFailure {
            Toast.makeText(this, "未找到可打开此链接的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun WebView.releaseSleepDownWebView(clearResourceCache: Boolean = true) {
    runCatching { stopLoading() }
    runCatching { clearHistory() }
    // Resource cache can grow into tens of megabytes after repeated school-site imports.
    // Cookies and DOM storage are intentionally retained so login state is not lost.
    if (clearResourceCache) runCatching { clearCache(true) }
    runCatching { uninstallShiguangRuntime() }
    runCatching { destroy() }
}

internal fun WebView.enableSleepDownDownloads() {
    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        handleSleepDownDownload(url, userAgent, contentDisposition, mimeType)
    }
}

private fun WebView.handleSleepDownDownload(
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {
    when {
        url.startsWith("blob:", ignoreCase = true) -> Toast.makeText(context, "请用浏览器下载此文件", Toast.LENGTH_SHORT).show()
        url.startsWith("data:", ignoreCase = true) -> {
            if (context.ensureLegacyDownloadPermission()) {
                context.handleSleepDownDataUrl(url)
            }
        }
        else -> {
            if (context.ensureLegacyDownloadPermission()) {
                context.handleSleepDownWebDownload(url, userAgent, contentDisposition, mimeType, this.url)
            }
        }
    }
}

internal fun shouldRequestLegacyDownloadPermission(
    sdkInt: Int,
    permissionGranted: Boolean
): Boolean = sdkInt < Build.VERSION_CODES.Q && !permissionGranted

private fun Context.ensureLegacyDownloadPermission(): Boolean {
    val granted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    if (!shouldRequestLegacyDownloadPermission(Build.VERSION.SDK_INT, granted)) {
        return true
    }
    val activity = findActivity()
    if (activity == null) {
        Toast.makeText(this, "无法申请存储权限，请使用系统浏览器下载", Toast.LENGTH_SHORT).show()
        return false
    }
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        LegacyDownloadPermissionRequestCode
    )
    Toast.makeText(this, "授权存储权限后，请再次点击下载", Toast.LENGTH_SHORT).show()
    return false
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val LegacyDownloadPermissionRequestCode = 60730

private fun Context.handleSleepDownWebDownload(
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
    referer: String? = null
) {
    val uri = runCatching { url.toUri() }.getOrNull()
    if (uri == null || (uri.scheme != "http" && uri.scheme != "https")) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(this, "无法处理下载链接", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val appContext = applicationContext
    val cookie = CookieManager.getInstance().getCookie(url)
    Toast.makeText(appContext, "已开始下载", Toast.LENGTH_SHORT).show()
    Thread {
        runCatching {
            appContext.downloadWebFile(
                url = url,
                fallbackContentDisposition = contentDisposition,
                userAgent = userAgent,
                cookie = cookie,
                referer = referer,
                mimeType = mimeType
            )
        }.onSuccess {
            appContext.showToastOnMain("已保存到下载目录")
        }.onFailure {
            appContext.showToastOnMain("下载失败：${it.message ?: "未知错误"}")
        }
    }.start()
}

private fun Context.downloadWebFile(
    url: String,
    fallbackContentDisposition: String?,
    userAgent: String?,
    cookie: String?,
    referer: String?,
    mimeType: String?
) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15000
        readTimeout = 30000
        requestMethod = "GET"
        if (!userAgent.isNullOrBlank()) setRequestProperty("User-Agent", userAgent)
        if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
        if (!referer.isNullOrBlank()) setRequestProperty("Referer", referer)
    }
    connection.inputStream.use { input ->
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) error("HTTP $responseCode")
        val resolvedMimeType = connection.contentType?.substringBefore(';') ?: mimeType ?: "application/octet-stream"
        val resolvedFileName = resolveWebDownloadFileName(
            originalUrl = url,
            finalUrl = connection.url?.toString(),
            contentDisposition = connection.getHeaderField("Content-Disposition") ?: fallbackContentDisposition,
            mimeType = resolvedMimeType
        )
        openDownloadOutput(resolvedFileName, resolvedMimeType).use { output ->
            input.copyTo(output)
        }
    }
    connection.disconnect()
}

private fun Context.openDownloadOutput(fileName: String, mimeType: String): java.io.OutputStream {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        openScopedDownloadOutput(fileName, mimeType)
    } else {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        File(downloads, fileName).outputStream()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.openScopedDownloadOutput(fileName: String, mimeType: String): java.io.OutputStream {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val targetUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("无法创建文件")
    return contentResolver.openOutputStream(targetUri) ?: error("无法写入文件")
}

private fun resolveWebDownloadFileName(
    originalUrl: String,
    finalUrl: String?,
    contentDisposition: String?,
    mimeType: String?
): String {
    val fromDisposition = contentDisposition?.let { disposition ->
        Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)
            ?: Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)
    }?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    val guessed = URLUtil.guessFileName(finalUrl ?: originalUrl, contentDisposition, mimeType)
    val name = fromDisposition?.takeIf { it.isNotBlank() } ?: guessed
    if (!name.endsWith(".bin", ignoreCase = true) && name.substringAfterLast('.', "").isNotBlank()) return name
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.orEmpty())
        ?.takeIf { it.isNotBlank() }
        ?: when {
            mimeType?.contains("android", ignoreCase = true) == true -> "apk"
            mimeType?.contains("zip", ignoreCase = true) == true -> "zip"
            mimeType?.contains("pdf", ignoreCase = true) == true -> "pdf"
            else -> null
        }
    val base = name.removeSuffix(".bin").ifBlank { "download" }
    return if (extension != null) "$base.$extension" else name
}

private fun Context.showToastOnMain(message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private fun Context.handleSleepDownDataUrl(dataUrl: String) {
    val commaIndex = dataUrl.indexOf(',')
    if (commaIndex <= 0) {
        Toast.makeText(this, "无法处理下载链接", Toast.LENGTH_SHORT).show()
        return
    }
    val header = dataUrl.substring(0, commaIndex)
    val payload = dataUrl.substring(commaIndex + 1)
    val mimeType = header.substringAfter("data:", "application/octet-stream").substringBefore(';')
    handleSleepDownBase64Download(payload, mimeType, null)
}

private fun Context.handleSleepDownBase64Download(base64: String, mimeType: String?, title: String?) {
    runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val safeTitle = title?.takeIf { it.isNotBlank() }?.replace(Regex("[\\\\/:*?\"<>|]"), "_") ?: "download"
        val extension = when {
            mimeType?.contains("pdf", ignoreCase = true) == true -> ".pdf"
            mimeType?.contains("zip", ignoreCase = true) == true -> ".zip"
            mimeType?.contains("excel", ignoreCase = true) == true || mimeType?.contains("spreadsheet", ignoreCase = true) == true -> ".xlsx"
            mimeType?.contains("word", ignoreCase = true) == true -> ".docx"
            mimeType?.contains("image/png", ignoreCase = true) == true -> ".png"
            mimeType?.contains("image/jpeg", ignoreCase = true) == true -> ".jpg"
            else -> ""
        }
        val fileName = "$safeTitle$extension"
        openDownloadOutput(fileName, mimeType ?: "application/octet-stream").use { it.write(bytes) }
        Toast.makeText(this, "已保存到下载目录", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show()
    }
}

