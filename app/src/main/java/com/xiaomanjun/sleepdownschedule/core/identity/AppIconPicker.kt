package com.xiaomanjun.sleepdownschedule.core.identity

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.CenterLiquidDialog
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidDialogHeader
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownFormDialog
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassSurface
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

@Composable
fun AppIconPicker(backdrop: Backdrop?, config: ScheduleConfigEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val revision by AppIconManager.changes.collectAsState()
    val selected = remember(revision) { AppIconManager.currentPalette(context) }
    var error by remember { mutableStateOf<String?>(null) }
    SleepDownFormDialog(onDismiss) {
        CenterLiquidDialog(backdrop, config) {
            LiquidDialogHeader("应用图标", onDismiss, backdrop, config)
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AppIconPalette.entries, key = { it.name }) { palette ->
                    GlassSurface(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        selected = palette == selected,
                        onClick = { runCatching { AppIconManager.setPalette(context, palette) }
                            .onFailure { error = "无法切换图标，请重试" } }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                                update = { it.setImageDrawable(context.getDrawable(palette.icon)); it.contentDescription = palette.label },
                                modifier = Modifier.size(56.dp))
                            Text(palette.label, Modifier.weight(1f).padding(start = 14.dp))
                            if (palette == selected) Text("✓", color = androidx.compose.ui.graphics.Color(0xFF008BFF))
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
            Text("所选配色会保持保存", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
