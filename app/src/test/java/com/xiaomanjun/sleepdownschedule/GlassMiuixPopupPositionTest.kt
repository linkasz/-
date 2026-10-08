package com.xiaomanjun.sleepdownschedule

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.xiaomanjun.sleepdownschedule.core.ui.settings.calculateUpwardDropdownPosition
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

class GlassMiuixPopupPositionTest {
    @Test
    fun explicitWindowAnchorControlsPopupPosition() {
        val position = calculateUpwardDropdownPosition(
            layoutAnchorBounds = IntRect(10, 10, 40, 40),
            explicitAnchorBounds = Rect(220f, 300f, 260f, 340f),
            windowBounds = IntRect(0, 0, 360, 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(160, 120),
            popupMargin = IntRect(8, 8, 8, 8),
            alignment = PopupPositionProvider.Align.End
        )

        assertEquals(IntOffset(92, 172), position)
    }

    @Test
    fun popupFlipsBelowAnchorNearTopAndStaysInsideWindow() {
        val position = calculateUpwardDropdownPosition(
            layoutAnchorBounds = IntRect(0, 0, 20, 20),
            explicitAnchorBounds = Rect(0f, 5f, 32f, 35f),
            windowBounds = IntRect(0, 0, 360, 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(160, 120),
            popupMargin = IntRect(8, 8, 8, 8),
            alignment = PopupPositionProvider.Align.End
        )

        assertEquals(IntOffset(8, 43), position)
    }

    @Test
    fun invalidExplicitAnchorFallsBackToPopupLayoutAnchor() {
        val position = calculateUpwardDropdownPosition(
            layoutAnchorBounds = IntRect(200, 300, 240, 340),
            explicitAnchorBounds = Rect.Zero,
            windowBounds = IntRect(0, 0, 360, 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(160, 120),
            popupMargin = IntRect(8, 8, 8, 8),
            alignment = PopupPositionProvider.Align.End
        )

        assertEquals(IntOffset(72, 172), position)
    }
}
