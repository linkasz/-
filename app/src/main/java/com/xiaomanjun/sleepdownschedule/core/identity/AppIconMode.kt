package com.xiaomanjun.sleepdownschedule.core.identity

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import androidx.core.content.edit
import com.xiaomanjun.sleepdownschedule.feature.backup.BackupAppIconPreferences

enum class AppIconMode(val label: String) {
    LIGHT("浅色"),
    DARK("深色"),
    FOLLOW_DARK_MODE("跟随")
}

/** Legacy preference value retained for backup compatibility. */
enum class AppIconStyle(val label: String) {
    MINIMAL("简约"),
    KANBAN("玻璃")
}

enum class AppIconPalette(val label: String, val icon: Int, val aliasSuffix: String) {
    SKY("晴空蓝", com.xiaomanjun.sleepdownschedule.R.mipmap.ic_palette_sky, ".LauncherFollow"),
    GREEN("青芽绿", com.xiaomanjun.sleepdownschedule.R.mipmap.ic_palette_green, ".LauncherGreen"),
    TEAL("碧海青", com.xiaomanjun.sleepdownschedule.R.mipmap.ic_palette_teal, ".LauncherTeal"),
    PURPLE("流光紫", com.xiaomanjun.sleepdownschedule.R.mipmap.ic_palette_purple, ".LauncherPurple"),
    ORANGE("暖霞橙", com.xiaomanjun.sleepdownschedule.R.mipmap.ic_palette_orange, ".LauncherOrange")
}

internal fun resolveAppIconPalette(value: String?): AppIconPalette =
    AppIconPalette.entries.firstOrNull { it.name == value } ?: AppIconPalette.SKY

internal enum class LauncherAlias(val classSuffix: String) {
    MINIMAL_FOLLOW(".LauncherFollow"),
    MINIMAL_LIGHT(".LauncherLight"),
    MINIMAL_DARK(".LauncherDark"),
    KANBAN_FOLLOW(".LauncherKanbanFollow"),
    KANBAN_LIGHT(".LauncherKanbanLight"),
    KANBAN_DARK(".LauncherKanbanDark")
}

private const val LauncherAliasNamespace = "com.xiaomanjun.sleepdownschedule"

private fun styleAlias(style: AppIconStyle): (AppIconMode) -> LauncherAlias = when (style) {
    AppIconStyle.MINIMAL -> { mode ->
        when (mode) {
            AppIconMode.LIGHT -> LauncherAlias.MINIMAL_LIGHT
            AppIconMode.DARK -> LauncherAlias.MINIMAL_DARK
            AppIconMode.FOLLOW_DARK_MODE -> LauncherAlias.MINIMAL_FOLLOW
        }
    }
    AppIconStyle.KANBAN -> { mode ->
        when (mode) {
            AppIconMode.LIGHT -> LauncherAlias.KANBAN_LIGHT
            AppIconMode.DARK -> LauncherAlias.KANBAN_DARK
            AppIconMode.FOLLOW_DARK_MODE -> LauncherAlias.KANBAN_FOLLOW
        }
    }
}

internal fun resolveLauncherAlias(
    mode: AppIconMode,
    style: AppIconStyle,
    followsSystemDarkMode: Boolean,
    darkTheme: Boolean
): LauncherAlias {
    val resolvedMode = when (mode) {
        AppIconMode.LIGHT, AppIconMode.DARK -> mode
        AppIconMode.FOLLOW_DARK_MODE -> when {
            followsSystemDarkMode -> AppIconMode.FOLLOW_DARK_MODE
            darkTheme -> AppIconMode.DARK
            else -> AppIconMode.LIGHT
        }
    }
    // FOLLOW 模式下跟随系统深浅时，仍走对应风格的 FOLLOW alias（图标带 night 变体自动切换）。
    return styleAlias(style)(resolvedMode)
}

internal fun launcherAliasClassName(alias: LauncherAlias): String =
    LauncherAliasNamespace + alias.classSuffix

/**
 * 根据当前选择的图标风格和模式，返回对应的 mipmap 资源 ID。
 * 用于在应用内显示当前使用的图标（如关于页）。
 */
fun currentIconResId(
    context: Context,
    darkTheme: Boolean = AppIconManager.currentDarkTheme(context)
): Int {
    return AppIconManager.currentPalette(context).icon
}

/** Fixed full-color drawable aliases for SystemUI, sharing each explicit light/dark PNG. */
fun currentLiveUpdateIconResId(context: Context): Int {
    return when (AppIconManager.currentPalette(context)) {
        AppIconPalette.SKY -> com.xiaomanjun.sleepdownschedule.R.drawable.ic_palette_sky_foreground
        AppIconPalette.GREEN -> com.xiaomanjun.sleepdownschedule.R.drawable.ic_palette_green_foreground
        AppIconPalette.TEAL -> com.xiaomanjun.sleepdownschedule.R.drawable.ic_palette_teal_foreground
        AppIconPalette.PURPLE -> com.xiaomanjun.sleepdownschedule.R.drawable.ic_palette_purple_foreground
        AppIconPalette.ORANGE -> com.xiaomanjun.sleepdownschedule.R.drawable.ic_palette_orange_foreground
    }
}

object AppIconManager {
    private const val PreferencesName = "app_icon_preferences"
    private const val ModeKey = "mode"
    private const val StyleKey = "style"
    private const val PaletteKey = "palette"
    private val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val changes: kotlinx.coroutines.flow.StateFlow<Long> = revision

    fun currentPalette(context: Context): AppIconPalette = resolveAppIconPalette(preferences(context).getString(PaletteKey, null))

    fun setPalette(context: Context, palette: AppIconPalette) {
        val previous = currentPalette(context)
        check(preferences(context).edit().putString(PaletteKey, palette.name).commit())
        try { applyStoredMode(context) } catch (error: Exception) {
            preferences(context).edit().putString(PaletteKey, previous.name).commit()
            applyStoredMode(context)
            throw error
        }
        revision.value += 1
    }
    private const val FollowsSystemDarkModeKey = "follows_system_dark_mode"
    private const val DarkThemeKey = "dark_theme"
    private var lastAppliedIconResId: Int? = null
    internal var onIconChanged: (() -> Unit)? = null

    fun currentDarkTheme(context: Context): Boolean = if (
        preferences(context).getBoolean(FollowsSystemDarkModeKey, true)
    ) {
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    } else preferences(context).getBoolean(DarkThemeKey, false)

    fun currentMode(context: Context): AppIconMode {
        val stored = preferences(context).getString(
            ModeKey,
            AppIconMode.FOLLOW_DARK_MODE.name
        )
        return runCatching { AppIconMode.valueOf(stored.orEmpty()) }
            .getOrDefault(AppIconMode.FOLLOW_DARK_MODE)
    }

    fun currentStyle(context: Context): AppIconStyle {
        val stored = preferences(context).getString(
            StyleKey,
            AppIconStyle.KANBAN.name
        )
        return runCatching { AppIconStyle.valueOf(stored.orEmpty()) }
            .getOrDefault(AppIconStyle.KANBAN)
    }

    fun backupPreferences(context: Context): BackupAppIconPreferences {
        val storage = preferences(context)
        return BackupAppIconPreferences(
            palette = currentPalette(context).name,
            mode = currentMode(context).name,
            style = currentStyle(context).name,
            followsSystemDarkMode = storage.getBoolean(FollowsSystemDarkModeKey, true),
            darkTheme = storage.getBoolean(DarkThemeKey, false)
        )
    }

    fun applyBackupPreferences(context: Context, backup: BackupAppIconPreferences) {
        val mode = runCatching { AppIconMode.valueOf(backup.mode) }
            .getOrElse { throw IllegalArgumentException("未知 app icon mode: ${backup.mode}") }
        val style = runCatching { AppIconStyle.valueOf(backup.style) }
            .getOrDefault(AppIconStyle.KANBAN)
        val committed = preferences(context).edit()
            .putString(PaletteKey, resolveAppIconPalette(backup.palette).name)
            .putString(ModeKey, mode.name)
            .putString(StyleKey, style.name)
            .putBoolean(FollowsSystemDarkModeKey, backup.followsSystemDarkMode)
            .putBoolean(DarkThemeKey, backup.darkTheme)
            .commit()
        check(committed) { "无法提交 app icon preferences" }
        applyStoredMode(context)
        revision.value += 1
    }

    fun setMode(context: Context, mode: AppIconMode) {
        preferences(context).edit {
            putString(ModeKey, mode.name)
        }
        applyStoredMode(context)
    }

    fun setStyle(context: Context, style: AppIconStyle) {
        preferences(context).edit {
            putString(StyleKey, style.name)
        }
        applyStoredMode(context)
    }

    fun syncAppearance(
        context: Context,
        followsSystemDarkMode: Boolean,
        darkTheme: Boolean
    ) {
        preferences(context).edit {
            putBoolean(FollowsSystemDarkModeKey, followsSystemDarkMode)
            putBoolean(DarkThemeKey, darkTheme)
        }
    }

    fun applyStoredMode(context: Context) {
        val desired = currentPalette(context).aliasSuffix
        val packageManager = context.packageManager
        val suffixes = (LauncherAlias.entries.map { it.classSuffix } + AppIconPalette.entries.map { it.aliasSuffix }).distinct()
        fun component(suffix: String) = ComponentName(context.packageName, LauncherAliasNamespace + suffix)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            packageManager.setComponentEnabledSettings(suffixes.map { suffix ->
                PackageManager.ComponentEnabledSetting(
                    component(suffix),
                    if (suffix == desired) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            })
        } else {
            // Keep a valid entry while older PackageManagers apply the individual changes.
            packageManager.setComponentEnabledSetting(component(desired), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            suffixes.filterNot { it == desired }.forEach { suffix ->
                packageManager.setComponentEnabledSetting(component(suffix), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
        refreshAppNotificationIcons(context)
        val iconResId = currentIconResId(context)
        if (lastAppliedIconResId != iconResId) {
            lastAppliedIconResId = iconResId
            onIconChanged?.invoke()
        }
    }

    private fun setAliasEnabled(
        packageManager: PackageManager,
        context: Context,
        alias: LauncherAlias,
        enabled: Boolean
    ) {
        val component = ComponentName(
            context.packageName,
            launcherAliasClassName(alias)
        )
        val desiredState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (packageManager.getComponentEnabledSetting(component) != desiredState) {
            packageManager.setComponentEnabledSetting(
                component,
                desiredState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
