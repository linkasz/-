package com.xiaomanjun.sleepdownschedule

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.components.*
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.core.ui.settings.*
import com.xiaomanjun.sleepdownschedule.feature.home.day.HomeReadabilityContext
import com.xiaomanjun.sleepdownschedule.feature.home.day.LocalHomeReadability
import com.xiaomanjun.sleepdownschedule.glass.*
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import com.xiaomanjun.sleepdownschedule.model.defaultConfig
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

/** Synthetic native preview. Debug manifest only; no user database, AI, or private assets. */
class GlassPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dark = intent.getBooleanExtra("dark", false)
        val pattern = intent.getStringExtra("background") ?: "detail"
        val page = intent.getIntExtra("page", 0)
        val openMenu = intent.getBooleanExtra("menu", false)
        setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                GlassMiuixSettingsTheme(defaultConfig().copy(darkMode = dark, followSystemDarkMode = false)) {
                    Preview(dark, pattern, page, openMenu)
                }
            }
        }
    }
}

private fun previewBitmap(pattern: String): Bitmap {
    val image = Bitmap.createBitmap(360, 720, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(image)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (y in 0 until 720) {
        val color = when (pattern) {
            "light" -> 0xFFEAF1FA.toInt()
            "dark" -> 0xFF172238.toInt()
            "mixed" -> if (y < 360) 0xFFEAF1FA.toInt() else 0xFF172238.toInt()
            else -> android.graphics.Color.HSVToColor(floatArrayOf((y * .4f) % 360, .48f, .82f))
        }
        paint.color = color; canvas.drawRect(0f, y.toFloat(), 360f, y + 1f, paint)
    }
    if (pattern == "detail") {
        paint.strokeWidth = 1f; paint.color = 0x55606060
        for (x in 0..360 step 18) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), 720f, paint)
        for (y in 0..720 step 18) canvas.drawLine(0f, y.toFloat(), 360f, y.toFloat(), paint)
        paint.color = 0xAA172331.toInt(); paint.textSize = 12f
        for (y in 30..720 step 54) canvas.drawText("SYNTHETIC 012345 / ABC", 10f, y.toFloat(), paint)
    }
    return image
}

@Composable
private fun Preview(dark: Boolean, pattern: String, page: Int, initiallyOpen: Boolean) {
    val bitmap = remember(pattern) { previewBitmap(pattern) }
    val config = remember(dark) { defaultConfig().copy(darkMode = dark, followSystemDarkMode = false,
        wallpaperUri = "preview://synthetic", homeTextLight = dark) }
    val backdrop = rememberGlassLayerBackdrop(GlassBackdropDomain.Background, "debug-glass-background")
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var tab by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(false) }
    var value by remember { mutableFloatStateOf(.45f) }
    var menu by remember { mutableStateOf(initiallyOpen) }
    var dialog by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<Rect?>(null) }
    var text by remember { mutableStateOf("") }
    var clicks by remember { mutableIntStateOf(0) }
    MiuixScaffold {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned { rootSize = it.size; rootOffset = it.positionInWindow() }) {
            Canvas(Modifier.fillMaxSize().glassBackdropProducer(backdrop)) {
                drawImage(bitmap.asImageBitmap(), dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            }
            CompositionLocalProvider(
                LocalHomeReadability provides HomeReadabilityContext(bitmap, config, rootSize, rootOffset),
                LocalAdaptiveGlass provides adaptiveGlassStateFromLuminance(if (dark) .12f else .85f),
                LocalSettingsPopupBackdrop provides backdrop
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("原生玻璃组件 · $page", color = if (dark) Color.White else Color(0xFF172331))
                    Text("点击计数 $clicks", color = if (dark) Color.White else Color(0xFF172331))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppGlassIconButton(backdrop, config, "预览按钮", { clicks++ }) { Text("＋") }
                        AppGlassIconButton(backdrop, config, "选中按钮", { clicks++ }, selected = true) { Text("✓") }
                        AppGlassIconButton(backdrop, config, "禁用按钮", {}, enabled = false) { Text("×") }
                        AppGlassIconButton(backdrop, config, "打开菜单", { menu = true },
                            modifier = Modifier.onGloballyPositioned { anchor = it.boundsInWindow() }) { Text("≡") }
                    }
                    GlassPill(backdrop, config, Modifier.fillMaxWidth().height(48.dp), onClick = { clicks++ }) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("玻璃胶囊", color = sleepDownGlassForegroundColor(config))
                        }
                    }
                    if (page == 0) {
                        LiquidToggle({ checked }, { checked = it }, backdrop)
                        LiquidSlider(value = { value }, onPreviewValueChange = { value = it },
                            onPreviewModeChange = {}, onCommit = { value = it }, valueRange = 0f..1f,
                            visibilityThreshold = .001f, backdrop = backdrop, modifier = Modifier.fillMaxWidth())
                        LiquidBottomTabs({ tab }, { tab = it }, backdrop, 4, Modifier.fillMaxWidth(),
                            isLightThemeOverride = !dark) {
                            listOf("课程", "待办", "日历", "设置").forEach { label ->
                                Box(Modifier.weight(1f).height(56.dp), contentAlignment = Alignment.Center) {
                                    Text(label, color = if (dark) Color.White else Color(0xFF172331))
                                }
                            }
                        }
                        Text("开关 $checked · 滑块 ${"%.2f".format(value)} · 导航 $tab", color = if (dark) Color.White else Color.Black)
                    } else if (page == 1) {
                        QuickSheetLiquidAction("普通操作", true, backdrop, config, Modifier.fillMaxWidth()) { clicks++ }
                        QuickSheetLiquidAction("确认", true, backdrop, config, Modifier.fillMaxWidth(), primary = true) { clicks++ }
                        GlassSurface(backdrop, config, Modifier.fillMaxWidth(), shape = RoundedRectangle(24.dp), tokens = GlassTokens.dialog()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("阅读面板", color = LocalContentColor.current)
                                CompositionLocalProvider(LocalReadablePanelControls provides true) {
                                    OutlinedTextField(text, { text = it }, label = { Text("合成输入") }, modifier = Modifier.fillMaxWidth())
                                    QuickSheetLiquidAction("面板内操作", true, backdrop, config, Modifier.fillMaxWidth()) { dialog = true }
                                }
                            }
                        }
                        GlassSurface(backdrop, config, Modifier.fillMaxWidth().height(90.dp), shape = RoundedRectangle(18.dp), tokens = GlassTokens.courseCard(12f)) {
                            Column(Modifier.padding(14.dp)) { Text("合成课程卡"); Text("保留课程色和文字") }
                        }
                    } else {
                        TopAssistantSurface(backdrop, config, RoundedRectangle(28.dp),
                            Modifier.fillMaxWidth().height(300.dp), lightSurface = !dark,
                            frostedConversation = true, opaqueHeaderHeight = 76.dp) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Text("助手展开面板", color = if (dark) Color.White else Color(0xFF172331))
                                Text("合成会话：上方阅读柔化，下方液态玻璃。", color = if (dark) Color.White else Color(0xFF172331))
                                Spacer(Modifier.weight(1f))
                                GlassPill(backdrop, config, Modifier.fillMaxWidth().height(48.dp)) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("输入测试内容") }
                                }
                            }
                        }
                    }
                }
                SleepDownLiquidCascadingPopup(menu, anchor, listOf(
                    SleepDownLiquidMenuItem("one", "菜单项目", onClick = { clicks++; menu = false }),
                    SleepDownLiquidMenuItem("child", "二级菜单", children = listOf(SleepDownLiquidMenuItem("nested", "子菜单项目", onClick = { clicks++; menu = false }))),
                    SleepDownLiquidMenuItem("disabled", "禁用项目", enabled = false)
                ), { menu = false }, backdrop, config)
                SleepDownPickerDialog(dialog, "合成选择器", { dialog = false }, backdrop, config) {
                    Text("测试内容，未读取用户资料")
                    QuickSheetLiquidAction("完成", true, backdrop, config) { dialog = false }
                }
            }
        }
    }
}
