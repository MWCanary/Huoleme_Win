package com.huoleme.app.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoamingStoreTest {
    @Test
    fun savesAndLoadsSingleRoamingFile() {
        val directory = Files.createTempDirectory("huoleme-roaming-test")
        val file = directory.resolve("data.json")
        val store = RoamingStore(file)
        val settings = AppSettings(
            themeMode = com.huoleme.app.theme.AppThemeMode.CUSTOM,
            customColor = 0xFF123456.toInt(),
            darkMode = DarkModePreference.DARK,
            fontSizeLevel = 3,
            birthDate = "2000-01-01",
            testament = "test",
            notificationHour = 21,
            notificationMinute = 15,
            animationsEnabled = false,
            minimizeToTray = false
        )
        val data = RoamingData(
            firstLaunchDone = true,
            settings = settings.toRoamingSettings(),
            records = listOf(
                CheckInRecord("2026-11-07", 123456L, "note", "")
            ),
            lastNotifiedDate = "2026-11-06"
        )

        store.save(data)
        assertTrue(Files.exists(file))
        val loaded = store.load()
        assertEquals(true, loaded.firstLaunchDone)
        assertEquals("2026-11-07", loaded.records.single().date)
        assertEquals(123456L, loaded.records.single().timestamp)
        assertEquals("2026-11-06", loaded.lastNotifiedDate)
        assertEquals(settings.themeMode, loaded.settings.toAppSettings().themeMode)
        assertEquals(0xFF123456.toInt(), loaded.settings.toAppSettings().customColor)
        assertEquals(settings, loaded.settings.toAppSettings())

        // Export must be exactly the same payload as the Roaming file.
        assertEquals(store.exportJson(data), Files.readString(file))
    }
}
