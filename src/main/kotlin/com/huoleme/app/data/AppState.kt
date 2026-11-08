package com.huoleme.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.huoleme.app.Strings
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AppState(
    private val store: RoamingStore = RoamingStore()
) {
    private val initialData: RoamingData = store.load()

    var records by mutableStateOf(initialData.records.sortedByDescending { it.timestamp })
        private set

    var settings by mutableStateOf(initialData.settings.toAppSettings())
        private set

    var firstLaunchDone by mutableStateOf(initialData.firstLaunchDone)
        private set

    private var lastNotifiedDateValue: String = initialData.lastNotifiedDate

    val dataFile: Path
        get() = store.file

    val isCheckedInToday: Boolean
        get() = records.any { it.date == LocalDate.now().toString() }

    val currentStreak: Int
        get() = calculateStreak(records)

    val lastCheckInTime: String
        get() = records.firstOrNull()?.let { record ->
            Instant.ofEpochMilli(record.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } ?: Strings.noRecords

    val livedAge: Triple<Int, Int, Int>?
        get() = calculateLivedAge(settings.birthDate)

    val lastNotifiedDate: String
        get() = lastNotifiedDateValue

    fun completeFirstLaunch() {
        firstLaunchDone = true
        persist()
    }

    fun markNotifiedToday() {
        lastNotifiedDateValue = LocalDate.now().toString()
        persist()
    }

    fun checkIn(): Boolean {
        if (isCheckedInToday) return false
        val record = CheckInRecord(
            date = LocalDate.now().toString(),
            timestamp = System.currentTimeMillis()
        )
        records = (records + record).sortedByDescending { it.timestamp }
        persist()
        return true
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        persist()
    }

    fun injectPastCheckIns(days: Int) {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val existingDates = records.mapTo(mutableSetOf()) { it.date }
        val injected = (1..days).mapNotNull { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            if (existingDates.add(key)) {
                CheckInRecord(
                    date = key,
                    timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli()
                )
            } else {
                null
            }
        }
        if (injected.isNotEmpty()) {
            records = (records + injected).sortedByDescending { it.timestamp }
            persist()
        }
    }

    fun undoTodayCheckIn() {
        val today = LocalDate.now().toString()
        val updated = records.filterNot { it.date == today }
        if (updated.size != records.size) {
            records = updated
            persist()
        }
    }

    fun clearAllRecords() {
        records = emptyList()
        persist()
    }

    fun exportBackup(): String = store.exportJson(snapshot())

    fun importBackup(text: String): ImportSummary {
        val data = BackupService.parse(text)
        val existingDates = records.mapTo(mutableSetOf()) { it.date }
        val newRecords = data.records
            .filter { it.date !in existingDates }
            .distinctBy { it.date }
        if (newRecords.isNotEmpty()) {
            records = (records + newRecords).sortedByDescending { it.timestamp }
        }
        applyImportedSettings(data.settings)
        data.firstLaunchDone?.let { firstLaunchDone = it }
        persist()
        return ImportSummary(
            importedCount = newRecords.size,
            totalCount = data.records.size
        )
    }

    private fun applyImportedSettings(patch: ImportedSettings) {
        var updated = settings
        patch.themeMode?.let { updated = updated.copy(themeMode = it) }
        patch.customColor?.let { updated = updated.copy(customColor = it) }
        patch.darkMode?.let { updated = updated.copy(darkMode = it) }
        patch.fontSizeLevel?.let { updated = updated.copy(fontSizeLevel = it) }
        patch.birthDate?.let { updated = updated.copy(birthDate = it) }
        patch.testament?.let { updated = updated.copy(testament = it) }
        patch.notificationHour?.let { updated = updated.copy(notificationHour = it) }
        patch.notificationMinute?.let { updated = updated.copy(notificationMinute = it) }
        patch.animationsEnabled?.let { updated = updated.copy(animationsEnabled = it) }
        patch.minimizeToTray?.let { updated = updated.copy(minimizeToTray = it) }
        patch.autoStartEnabled?.let {
            updated = updated.copy(autoStartEnabled = it)
        }
        settings = updated
    }

    private fun snapshot(): RoamingData = RoamingData(
        firstLaunchDone = firstLaunchDone,
        settings = settings.toRoamingSettings(),
        records = records.sortedByDescending { it.timestamp },
        lastNotifiedDate = lastNotifiedDateValue
    )

    private fun persist() {
        runCatching { store.save(snapshot()) }
    }

    private fun calculateStreak(allRecords: List<CheckInRecord>): Int {
        val today = LocalDate.now()
        val dates = allRecords
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .distinct()
            .sortedDescending()
        if (dates.isEmpty()) return 0

        val latest = dates.first()
        if (latest != today && latest != today.minusDays(1)) return 0

        var streak = 0
        var expected = latest
        for (date in dates) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateLivedAge(birthDate: String): Triple<Int, Int, Int>? {
        if (birthDate.isBlank()) return null
        val birth = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
        val today = LocalDate.now()
        if (birth.isAfter(today)) return null
        val period = Period.between(birth, today)
        return Triple(period.years, period.months, period.days)
    }
}
