package com.huoleme.app.data

import com.huoleme.app.theme.AppThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackupServiceTest {
    @Test
    fun roundTripPreservesRecordsAndSettings() {
        val settings = AppSettings(
            themeMode = AppThemeMode.CUSTOM,
            customColor = 0xFF123456.toInt(),
            darkMode = DarkModePreference.DARK,
            fontSizeLevel = 4,
            birthDate = "2000-01-01",
            testament = "test testament",
            notificationHour = 21,
            notificationMinute = 30,
            animationsEnabled = false,
            minimizeToTray = false
        )
        val json = BackupService.export(
            records = listOf(
                CheckInRecord(date = "2026-11-07", timestamp = 123456L, note = "first"),
                CheckInRecord(date = "2026-11-08", timestamp = 223456L, note = "")
            ),
            settings = settings
        )

        assertTrue(json.contains("\"records\""))
        val parsed = BackupService.parse(json)
        assertEquals(2, parsed.records.size)
        assertEquals("2026-11-07", parsed.records[0].date)
        assertEquals(123456L, parsed.records[0].timestamp)
        assertEquals("first", parsed.records[0].note)
        assertEquals(AppThemeMode.CUSTOM, parsed.settings.themeMode)
        assertEquals(0xFF123456.toInt(), parsed.settings.customColor)
        assertEquals(DarkModePreference.DARK, parsed.settings.darkMode)
        assertEquals(4, parsed.settings.fontSizeLevel)
        assertEquals("2000-01-01", parsed.settings.birthDate)
        assertEquals(21, parsed.settings.notificationHour)
        assertEquals(30, parsed.settings.notificationMinute)
        assertEquals(false, parsed.settings.minimizeToTray)
    }

    @Test
    fun parsesAndroidStyleRecordArray() {
        val json = """
            [
              {"date":"2026-11-07","timestamp":123,"note":"","location":"经度: 1.0, 纬度: 2.0"},
              {"date":"","timestamp":456,"note":"invalid"},
              {"date":"2026-11-08","timestamp":0,"note":"invalid"}
            ]
        """.trimIndent()

        val parsed = BackupService.parse(json)
        assertEquals(1, parsed.records.size)
        assertEquals("2026-11-07", parsed.records[0].date)
        assertNotNull(parsed.settings)
    }

    @Test
    fun parsesNewRoamingSettingsFormat() {
        val json = """
            {
              "first_launch_done": true,
              "settings": {
                "theme_mode": "CUSTOM",
                "custom_color": -16742021,
                "dark_mode": "DARK",
                "font_size_level": 3,
                "birth_date": "2000-01-01",
                "survival_testament": "hello",
                "notification_hour": 21,
                "notification_minute": 30,
                "animations_enabled": false,
                "minimize_to_tray": false
              },
              "records": [
                {"date":"2026-11-07","timestamp":123,"note":"n","location":""}
              ]
            }
        """.trimIndent()

        val parsed = BackupService.parse(json)
        assertEquals(1, parsed.records.size)
        assertEquals(true, parsed.firstLaunchDone)
        assertEquals(AppThemeMode.CUSTOM, parsed.settings.themeMode)
        assertEquals(DarkModePreference.DARK, parsed.settings.darkMode)
        assertEquals(3, parsed.settings.fontSizeLevel)
        assertEquals(21, parsed.settings.notificationHour)
        assertEquals(false, parsed.settings.minimizeToTray)
    }

    @Test
    fun rejectsMalformedJson() {
        assertFailsWith<IllegalArgumentException> {
            BackupService.parse("not a json document")
        }
    }
}
