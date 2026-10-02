package com.xiaomanjun.sleepdownschedule.glass.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Modal controls must not re-sample the page beneath their own frosted shell. */
internal val LocalReadablePanelControls = staticCompositionLocalOf { false }
