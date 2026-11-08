package com.huoleme.app.data

import com.huoleme.app.AppInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Single source of truth under %APPDATA%\Huoleme\data.json.
 *
 * It contains every user setting, the first-launch flag, the last notification
 * date and all check-in records. Export uses exactly the content of this file.
 */
class RoamingStore(
    val file: Path = defaultFile()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): RoamingData {
        Files.createDirectories(file.parent)

        if (Files.exists(file)) {
            val decoded = runCatching {
                json.decodeFromString<RoamingData>(Files.readString(file))
            }.getOrNull()
            if (decoded != null) return decoded

            backupCorruptedFile()
            val fallback = RoamingData()
            save(fallback)
            return fallback
        }

        // First launch with this data format: create the folder/file and migrate
        // legacy records.json + Preferences settings when they exist.
        val migrated = migrateLegacy()
        save(migrated)
        return migrated
    }

    fun save(data: RoamingData) {
        Files.createDirectories(file.parent)
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temporary, exportJson(data))
        try {
            Files.move(
                temporary,
                file,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun exportJson(data: RoamingData): String =
        json.encodeToString(RoamingData.serializer(), data)

    private fun migrateLegacy(): RoamingData {
        val legacySettings = runCatching { SettingsStore().load() }
            .getOrDefault(AppSettings())
        val legacyFirstLaunch = runCatching { SettingsStore().getFirstLaunchDone() }
            .getOrDefault(false)
        val legacyRecords = runCatching { RecordStore().load() }
            .getOrDefault(emptyList())

        return RoamingData(
            firstLaunchDone = legacyFirstLaunch,
            settings = legacySettings.toRoamingSettings(),
            records = legacyRecords.sortedByDescending { it.timestamp }
        )
    }

    private fun backupCorruptedFile() {
        val backup = file.resolveSibling(
            "data.corrupt-${System.currentTimeMillis()}.json"
        )
        runCatching { Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING) }
    }

    companion object {
        fun defaultFile(): Path {
            val appData = System.getenv("APPDATA")
            val base = if (!appData.isNullOrBlank()) {
                Path.of(appData)
            } else {
                Path.of(System.getProperty("user.home"))
            }
            return base.resolve(AppInfo.DATA_DIR_NAME).resolve("data.json")
        }
    }
}
