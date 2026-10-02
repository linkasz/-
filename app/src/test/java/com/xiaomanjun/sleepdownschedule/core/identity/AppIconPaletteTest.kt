package com.xiaomanjun.sleepdownschedule.core.identity

import org.junit.Assert.*
import org.junit.Test

class AppIconPaletteTest {
    @Test fun fivePalettesHaveUniqueAliasesAndDefaultBlue() {
        assertEquals(listOf("晴空蓝", "青芽绿", "碧海青", "流光紫", "暖霞橙"), AppIconPalette.entries.map { it.label })
        assertEquals(5, AppIconPalette.entries.map { it.aliasSuffix }.toSet().size)
        assertEquals(5, AppIconPalette.entries.map { it.icon }.toSet().size)
        assertEquals(AppIconPalette.SKY, resolveAppIconPalette(null))
        assertEquals(AppIconPalette.SKY, resolveAppIconPalette("KANBAN"))
        AppIconPalette.entries.forEach { assertEquals(it, resolveAppIconPalette(it.name)) }
    }
}
