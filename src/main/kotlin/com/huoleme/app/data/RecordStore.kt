package com.huoleme.app.data

import com.huoleme.app.AppInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class RecordStore(
    val file: Path = defaultFile()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class Backup(
        val records: List<CheckInRecord> = emptyList()
    )

    fun load(): List<CheckInRecord> {
        if (!Files.exists(file)) return emptyList()
        return runCatching {
            json.decodeFromString<Backup>(Files.readString(file))
                .records
                .filter { it.date.isNotBlank() && it.timestamp > 0L }
                .sortedByDescending { it.timestamp }
        }.getOrElse { emptyList() }
    }

    fun save(records: List<CheckInRecord>) {
        Files.createDirectories(file.parent)
        val backup = Backup(records.sortedByDescending { it.timestamp })
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temporary, json.encodeToString(backup))
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

    companion object {
        fun defaultFile(): Path {
            val appData = System.getenv("APPDATA")
            val base = if (!appData.isNullOrBlank()) {
                Path.of(appData)
            } else {
                Path.of(System.getProperty("user.home"))
            }
            return base.resolve(AppInfo.DATA_DIR_NAME).resolve("records.json")
        }
    }
}
