package com.huoleme.app.data

import com.huoleme.app.theme.AppThemeMode
import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStatePersistenceTest {
    @Test
    fun checkInAndSettingsArePersistedToRoamingFile() {
        val directory = Files.createTempDirectory("huoleme-state-test")
        val file = directory.resolve("data.json")
        val store = RoamingStore(file)
        store.save(RoamingData())

        val state = AppState(store)
        assertTrue(state.checkIn())
        state.updateSettings {
            it.copy(
                themeMode = AppThemeMode.CUSTOM,
                customColor = 0xFF123456.toInt(),
                birthDate = "2000-01-01",
                notificationHour = 21,
                notificationMinute = 30
            )
        }
        state.completeFirstLaunch()

        val loaded = RoamingStore(file).load()
        assertEquals(true, loaded.firstLaunchDone)
        assertEquals(1, loaded.records.size)
        assertEquals(LocalDate.now().toString(), loaded.records.single().date)
        assertTrue(loaded.records.single().timestamp > 0L)
        assertTrue(
            Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").matches(state.lastCheckInTime)
        )
        assertEquals("CUSTOM", loaded.settings.themeMode)
        assertEquals(0xFF123456.toInt(), loaded.settings.customColor)
        assertEquals("2000-01-01", loaded.settings.birthDate)
        assertEquals(21, loaded.settings.notificationHour)
    }
}
