package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.sleepDownGlassForegroundColor
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import java.time.YearMonth

/** Selection is a draft until a month is tapped; dismissal never navigates the calendar. */
@Composable
internal fun TodoMonthPicker(show: Boolean, month: YearMonth, backdrop: Backdrop?, config: ScheduleConfigEntity,
    onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    if (!show) return
    var year by rememberSaveable(month) { mutableIntStateOf(month.year.coerceIn(1900, 2100)) }
    val focus = remember { FocusRequester() }
    SleepDownPickerDialog(show = true, title = "选择年月", backdrop = backdrop, config = config,
        // This is a page selector, not a nested form: keep its actual liquid glass material.
        onDismissRequest = onDismiss, readableSurface = false, enableWindowDim = true,
        scrollableContent = true) {
        Column(Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent {
            if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) { onDismiss(); true } else false
        }.focusable(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("年份 · $year", color = sleepDownGlassForegroundColor(config))
            LazyRow(state = rememberLazyListState((year - 1900 - 1).coerceAtLeast(0)),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1900..2100).toList(), key = { it }) { candidate ->
                    GlassPill(backdrop, config, Modifier.heightIn(min = 48.dp), selected = candidate == year,
                        selectedSurfaceColorOverride = com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens.Button.Primary,
                        onClick = { year = candidate }) {
                        Text(candidate.toString(), Modifier.padding(12.dp), color = if (candidate == year) androidx.compose.ui.graphics.Color.White else sleepDownGlassForegroundColor(config))
                    }
                }
            }
            repeat(4) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { column ->
                        val candidate = row * 3 + column + 1
                        GlassPill(backdrop, config, Modifier.weight(1f).heightIn(min = 48.dp),
                            selected = candidate == month.monthValue && year == month.year,
                            selectedSurfaceColorOverride = com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens.Button.Primary,
                            onClick = { onSelect(YearMonth.of(year, candidate)) }) {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text("${candidate}月", color = if (candidate == month.monthValue && year == month.year) androidx.compose.ui.graphics.Color.White else sleepDownGlassForegroundColor(config))
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}
