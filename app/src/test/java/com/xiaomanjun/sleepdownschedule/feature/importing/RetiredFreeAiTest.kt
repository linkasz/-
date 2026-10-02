package com.xiaomanjun.sleepdownschedule.feature.importing

import com.xiaomanjun.sleepdownschedule.feature.backup.*
import org.junit.Assert.*
import org.junit.Test

class RetiredFreeAiTest {
    @Test fun oldBackupBecomesUnconfiguredAndKeepsSelfConfiguredProfiles() {
        fun provider(id: String) = BackupAiProvider(id, id, "OPENAI_COMPATIBLE", "https://example.com/v1", "model", "Bearer",
            false, false, false, false, false, false, false, false, "CHAT_COMPLETIONS", "NONE", "AUTO", reasoningEffort = "MEDIUM")
        val backup = BackupAiImportPreferences(AiProviderPresets.dailyFree.id, "enabled",
            listOf(provider(AiProviderPresets.dailyFree.id), provider(AiProviderPresets.openAI.id)))
        val migrated = AiImportSettingsStore.prepareBackupForRestore(backup)
        assertEquals(AiProviderPresets.none.id, migrated.selectedProviderId)
        assertNull(migrated.managedFreeOfferDecision)
        assertFalse(migrated.providers.any { AiProviderPresets.isManagedFreeId(it.id) })
        assertEquals(backup.providers.last(), migrated.providers.first { it.id == AiProviderPresets.openAI.id })
        assertTrue(migrated.providers.any { it.id == AiProviderPresets.none.id })
    }
    @Test fun selectableProvidersNeverOfferRetiredFreeService() {
        assertFalse(AiProviderPresets.selectable.any { AiProviderPresets.isManagedFreeId(it.id) })
        assertEquals(AiProviderPresets.dailyFree.id, AiProviderPresets.byId(AiProviderPresets.dailyFree.id).id)
    }
}
