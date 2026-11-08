package com.huoleme.app.data

import com.huoleme.app.Strings
import com.huoleme.app.theme.AppThemeMode
import java.util.prefs.Preferences

enum class DarkModePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.CLASSIC_BLUE,
    val customColor: Int = 0xFF00897B.toInt(),
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val fontSizeLevel: Int = 2,
    val birthDate: String = "",
    val testament: String = Strings.testamentDefault,
    val notificationHour: Int = 20,
    val notificationMinute: Int = 0,
    val animationsEnabled: Boolean = true,
    val minimizeToTray: Boolean = true,
    val autoStartEnabled: Boolean = true
)

class SettingsStore {
    private val prefs: Preferences = Preferences.userRoot().node("com/huoleme/app")

    fun load(): AppSettings {
        val themeMode = runCatching {
            AppThemeMode.valueOf(prefs.get(KEY_THEME_MODE, AppThemeMode.CLASSIC_BLUE.name))
        }.getOrDefault(AppThemeMode.CLASSIC_BLUE)

        val darkMode = runCatching {
            DarkModePreference.valueOf(prefs.get(KEY_DARK_MODE, DarkModePreference.SYSTEM.name))
        }.getOrDefault(DarkModePreference.SYSTEM)

        return AppSettings(
            themeMode = themeMode,
            customColor = prefs.getInt(KEY_CUSTOM_COLOR, 0xFF00897B.toInt()),
            darkMode = darkMode,
            fontSizeLevel = prefs.getInt(KEY_FONT_SIZE, 2).coerceIn(0, 4),
            birthDate = prefs.get(KEY_BIRTH_DATE, ""),
            testament = prefs.get(KEY_TESTAMENT, Strings.testamentDefault),
            notificationHour = prefs.getInt(KEY_NOTIFICATION_HOUR, 20).coerceIn(0, 23),
            notificationMinute = prefs.getInt(KEY_NOTIFICATION_MINUTE, 0).coerceIn(0, 59),
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, true),
            minimizeToTray = prefs.getBoolean(KEY_MINIMIZE_TO_TRAY, true),
            autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, true)
        )
    }

    fun save(settings: AppSettings) {
        runCatching {
            prefs.put(KEY_THEME_MODE, settings.themeMode.name)
            prefs.putInt(KEY_CUSTOM_COLOR, settings.customColor)
            prefs.put(KEY_DARK_MODE, settings.darkMode.name)
            prefs.putInt(KEY_FONT_SIZE, settings.fontSizeLevel)
            prefs.put(KEY_BIRTH_DATE, settings.birthDate)
            prefs.put(KEY_TESTAMENT, settings.testament)
            prefs.putInt(KEY_NOTIFICATION_HOUR, settings.notificationHour)
            prefs.putInt(KEY_NOTIFICATION_MINUTE, settings.notificationMinute)
            prefs.putBoolean(KEY_ANIMATIONS, settings.animationsEnabled)
            prefs.putBoolean(KEY_MINIMIZE_TO_TRAY, settings.minimizeToTray)
            prefs.putBoolean(KEY_AUTO_START, settings.autoStartEnabled)
            prefs.flush()
        }
    }

    fun getFirstLaunchDone(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun getLastNotifiedDate(): String = prefs.get(KEY_LAST_NOTIFIED_DATE, "")

    fun setLastNotifiedDate(date: String) {
        runCatching {
            prefs.put(KEY_LAST_NOTIFIED_DATE, date)
            prefs.flush()
        }
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CUSTOM_COLOR = "custom_color"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_FONT_SIZE = "font_size_level"
        const val KEY_BIRTH_DATE = "birth_date"
        const val KEY_TESTAMENT = "survival_testament"
        const val KEY_NOTIFICATION_HOUR = "notification_hour"
        const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        const val KEY_ANIMATIONS = "animations_enabled"
        const val KEY_MINIMIZE_TO_TRAY = "minimize_to_tray"
        const val KEY_AUTO_START = "auto_start_enabled"
        const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
        const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"
    }
}
