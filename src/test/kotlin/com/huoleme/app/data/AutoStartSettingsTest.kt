package com.huoleme.app.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoStartSettingsTest {
    @Test
    fun roamingRoundTripKeepsAutoStartFlag() {
        val settings = AppSettings(autoStartEnabled = false)
        val roaming = settings.toRoamingSettings()
        assertEquals(false, roaming.autoStartEnabled)
        assertEquals(false, roaming.toAppSettings().autoStartEnabled)

        val file = Files.createTempDirectory("huoleme-autostart").resolve("data.json")
        val store = RoamingStore(file)
        val data = RoamingData(settings = roaming)
        store.save(data)
        val json = Files.readString(file)
        assertTrue(json.contains("\"auto_start_enabled\": false"))
        assertEquals(store.exportJson(data), json)
        assertEquals(false, store.load().settings.autoStartEnabled)
    }

    @Test
    fun oldDataFilesDefaultToEnabled() {
        val file = Files.createTempDirectory("huoleme-autostart-old").resolve("data.json")
        Files.writeString(
            file,
            """{"first_launch_done":true,"settings":{"notification_hour":20,"minimize_to_tray":true},"records":[]}"""
        )
        assertEquals(true, RoamingStore(file).load().settings.autoStartEnabled)
    }

    @Test
    fun legacyBackgroundReminderKeyIsIgnored() {
        val file = Files.createTempDirectory("huoleme-autostart-legacy").resolve("data.json")
        Files.writeString(
            file,
            """{"first_launch_done":true,"settings":{"background_reminder_enabled":true},"records":[]}"""
        )
        val loaded = RoamingStore(file).load()
        assertEquals(true, loaded.settings.autoStartEnabled)
        assertTrue(loaded.records.isEmpty())
    }

    @Test
    fun backupImportAndExportCarryTheFlag() {
        val exported = BackupService.export(
            records = emptyList(),
            settings = AppSettings(autoStartEnabled = false)
        )
        assertTrue(exported.contains("\"auto_start_enabled\": false"))
        assertEquals(false, BackupService.parse(exported).settings.autoStartEnabled)
        assertNull(
            BackupService.parse("""{"settings":{"theme_mode":"CLASSIC_BLUE"}}""")
                .settings.autoStartEnabled
        )
    }
}
