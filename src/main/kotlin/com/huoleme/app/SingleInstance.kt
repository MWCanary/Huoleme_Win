package com.huoleme.app

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * One app instance per Windows user. Multiple instances would create multiple
 * tray icons and could race on the JSON record file.
 *
 * The scheduled-task reminder process uses [isAnotherInstanceRunning] as a
 * read-only probe: it must not take the lock away from the main app.
 */
object SingleInstance {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    fun acquire(): Boolean {
        if (lock != null) return true
        val opened = FileChannel.open(
            lockFile(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )
        val acquired = runCatching { opened.tryLock() }.getOrNull()
        if (acquired == null) {
            runCatching { opened.close() }
            return false
        }
        channel = opened
        lock = acquired
        return true
    }

    /**
     * True when another live process currently owns the instance lock.
     * Never keeps the lock: the probe is released immediately.
     */
    fun isAnotherInstanceRunning(): Boolean {
        if (lock != null) return false
        return runCatching {
            FileChannel.open(
                lockFile(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            ).use { probeChannel ->
                val probeLock = probeChannel.tryLock()
                if (probeLock == null) {
                    true
                } else {
                    probeLock.release()
                    false
                }
            }
        }.getOrDefault(false)
    }

    fun release() {
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        lock = null
        channel = null
    }

    private fun lockFile(): Path {
        val appData = System.getenv("LOCALAPPDATA")
        val base = if (!appData.isNullOrBlank()) {
            Path.of(appData)
        } else {
            Path.of(System.getProperty("java.io.tmpdir"))
        }
        val directory = base.resolve(AppInfo.DATA_DIR_NAME)
        Files.createDirectories(directory)
        return directory.resolve("app-instance.lock")
    }
}
