package com.huoleme.app.data

import com.huoleme.app.AppInfo
import com.huoleme.app.Strings
import com.huoleme.app.theme.AppThemeMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoamingSettings(
    @SerialName("theme_mode")
    val themeMode: String = AppThemeMode.CLASSIC_BLUE.name,
    @SerialName("custom_color")
    val customColor: Int = 0xFF00897B.toInt(),
    @SerialName("dark_mode")
    val darkMode: String = DarkModePreference.SYSTEM.name,
    @SerialName("font_size_level")
    val fontSizeLevel: Int = 2,
    @SerialName("birth_date")
    val birthDate: String = "",
    @SerialName("survival_testament")
    val testament: String = Strings.testamentDefault,
    @SerialName("notification_hour")
    val notificationHour: Int = 20,
    @SerialName("notification_minute")
    val notificationMinute: Int = 0,
    @SerialName("animations_enabled")
    val animationsEnabled: Boolean = true,
    @SerialName("minimize_to_tray")
    val minimizeToTray: Boolean = true,
    @SerialName("auto_start_enabled")
    val autoStartEnabled: Boolean = true
)

@Serializable
data class RoamingData(
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    @SerialName("app")
    val app: String = AppInfo.NAME,
    @SerialName("version")
    val version: String = AppInfo.VERSION,
    @SerialName("first_launch_done")
    val firstLaunchDone: Boolean = false,
    @SerialName("settings")
    val settings: RoamingSettings = RoamingSettings(),
    @SerialName("records")
    val records: List<CheckInRecord> = emptyList(),
    @SerialName("last_notified_date")
    val lastNotifiedDate: String = ""
)

fun AppSettings.toRoamingSettings(): RoamingSettings = RoamingSettings(
    themeMode = themeMode.name,
    customColor = customColor,
    darkMode = darkMode.name,
    fontSizeLevel = fontSizeLevel,
    birthDate = birthDate,
    testament = testament,
    notificationHour = notificationHour,
    notificationMinute = notificationMinute,
    animationsEnabled = animationsEnabled,
    minimizeToTray = minimizeToTray,
    autoStartEnabled = autoStartEnabled
)

fun RoamingSettings.toAppSettings(): AppSettings = AppSettings(
    themeMode = runCatching { AppThemeMode.valueOf(themeMode) }.getOrDefault(AppThemeMode.CLASSIC_BLUE),
    customColor = customColor,
    darkMode = runCatching { DarkModePreference.valueOf(darkMode.uppercase()) }
        .getOrDefault(DarkModePreference.SYSTEM),
    fontSizeLevel = fontSizeLevel.coerceIn(0, 4),
    birthDate = birthDate,
    testament = testament,
    notificationHour = notificationHour.coerceIn(0, 23),
    notificationMinute = notificationMinute.coerceIn(0, 59),
    animationsEnabled = animationsEnabled,
    minimizeToTray = minimizeToTray,
    autoStartEnabled = autoStartEnabled
)
