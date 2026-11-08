package com.huoleme.app.data

import com.huoleme.app.AppInfo
import com.huoleme.app.theme.AppThemeMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class ImportedSettings(
    val themeMode: AppThemeMode? = null,
    val customColor: Int? = null,
    val darkMode: DarkModePreference? = null,
    val fontSizeLevel: Int? = null,
    val birthDate: String? = null,
    val testament: String? = null,
    val notificationHour: Int? = null,
    val notificationMinute: Int? = null,
    val animationsEnabled: Boolean? = null,
    val minimizeToTray: Boolean? = null,
    val autoStartEnabled: Boolean? = null
)

data class BackupData(
    val records: List<CheckInRecord>,
    val settings: ImportedSettings,
    val firstLaunchDone: Boolean? = null
)

data class ImportSummary(
    val importedCount: Int,
    val totalCount: Int
)

object BackupService {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(records: List<CheckInRecord>, settings: AppSettings): String {
        val root = buildJsonObject {
            put("app", AppInfo.NAME)
            put("version", AppInfo.VERSION)
            put("exported_at", System.currentTimeMillis())
            put(
                "records",
                buildJsonArray {
                    records.forEach { record ->
                        add(
                            buildJsonObject {
                                put("date", record.date)
                                put("timestamp", record.timestamp)
                                put("note", record.note)
                            }
                        )
                    }
                }
            )
            put(
                "preferences",
                buildJsonObject {
                    put("theme_mode", settings.themeMode.name)
                    put("custom_color", settings.customColor)
                    put("dark_mode", settings.darkMode.name)
                    put("font_size_level", settings.fontSizeLevel)
                    put("birth_date", settings.birthDate)
                    put("survival_testament", settings.testament)
                    put("notification_hour", settings.notificationHour)
                    put("notification_minute", settings.notificationMinute)
                    put("animations_enabled", settings.animationsEnabled)
                    put("minimize_to_tray", settings.minimizeToTray)
                    put("auto_start_enabled", settings.autoStartEnabled)
                }
            )
        }
        return json.encodeToString(JsonElement.serializer(), root)
    }

    fun parse(text: String): BackupData {
        val element = runCatching { json.parseToJsonElement(text.trim()) }
            .getOrElse { throw IllegalArgumentException("JSON 格式无效") }

        val root = element as? JsonObject
        val recordsElement: JsonArray? = when {
            element is JsonArray -> element
            root != null -> root["records"] as? JsonArray
            else -> null
        }
        if (root == null && recordsElement == null) {
            throw IllegalArgumentException("不是有效的活了么备份文件")
        }

        val records = recordsElement?.mapNotNull { item ->
            val record = item as? JsonObject ?: return@mapNotNull null
            val date = record["date"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val timestamp = record["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L
            val note = record["note"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (date.isEmpty() || timestamp <= 0L) null
            else CheckInRecord(date = date, timestamp = timestamp, note = note)
        }.orEmpty()

        val preferences = (root?.get("settings") ?: root?.get("preferences")) as? JsonObject
        val firstLaunchDone = root?.get("first_launch_done")?.jsonPrimitive?.booleanOrNull
        return BackupData(
            records = records,
            settings = preferences?.let { parseSettings(it) } ?: ImportedSettings(),
            firstLaunchDone = firstLaunchDone
        )
    }

    private fun parseSettings(preferences: JsonObject): ImportedSettings {
        fun stringValue(key: String): String? =
            preferences[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        fun intValue(key: String): Int? =
            preferences[key]?.jsonPrimitive?.intOrNull

        fun booleanValue(key: String): Boolean? =
            preferences[key]?.jsonPrimitive?.booleanOrNull

        val themeMode = stringValue("theme_mode")?.let { raw ->
            runCatching { AppThemeMode.valueOf(raw) }.getOrNull()
        }
        val darkMode = stringValue("dark_mode")?.let { raw ->
            runCatching { DarkModePreference.valueOf(raw.uppercase()) }.getOrNull()
        }

        return ImportedSettings(
            themeMode = themeMode,
            customColor = intValue("custom_color"),
            darkMode = darkMode,
            fontSizeLevel = intValue("font_size_level")?.coerceIn(0, 4),
            birthDate = stringValue("birth_date"),
            testament = stringValue("survival_testament"),
            notificationHour = intValue("notification_hour")?.coerceIn(0, 23),
            notificationMinute = intValue("notification_minute")?.coerceIn(0, 59),
            animationsEnabled = booleanValue("animations_enabled"),
            minimizeToTray = booleanValue("minimize_to_tray"),
            autoStartEnabled = booleanValue("auto_start_enabled")
        )
    }
}
