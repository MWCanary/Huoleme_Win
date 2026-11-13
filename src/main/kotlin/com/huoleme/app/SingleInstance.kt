package com.huoleme.app

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * One app instance per Windows user, plus a loopback wake-up channel.
 *
 * A second launch must never initialize AWT/Swing/Compose: doing so (and showing
 * a modal "already running" dialog) used to leave a ~50 MB JVM alive forever on
 * every double-click. The secondary process now just sends one byte to the
 * primary instance and exits immediately.
 */
object SingleInstance {
    /** Set by the primary instance; invoked on its IPC thread. */
    @Volatile
    var onShowRequest: (() -> Unit)? = null

    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

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

    /** Primary instance: listen for wake-up requests from later launches. */
    fun startIpcServer() {
        if (server != null) return
        runCatching {
            val socket = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
            server = socket
            Files.writeString(portFile(), socket.localPort.toString())
            Thread({
                while (!socket.isClosed) {
                    runCatching {
                        socket.accept().use { peer ->
                            peer.soTimeout = 1_000
                            runCatching { peer.getInputStream().read() }
                            onShowRequest?.invoke()
                        }
                    }
                }
            }, "huoleme-single-instance").apply { isDaemon = true }.start()
        }
    }

    /** Secondary instance: ask the running app to show its window. Never blocks. */
    fun notifyRunning(): Boolean {
        val port = runCatching {
            Files.readString(portFile()).trim().toInt()
        }.getOrNull() ?: return false
        return runCatching {
            Socket(InetAddress.getLoopbackAddress(), port).use { peer ->
                peer.soTimeout = 1_000
                peer.getOutputStream().apply { write(1); flush() }
            }
            true
        }.getOrDefault(false)
    }

    fun release() {
        onShowRequest = null
        runCatching { server?.close() }
        server = null
        runCatching { Files.deleteIfExists(portFile()) }
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        lock = null
        channel = null
    }

    private fun baseDir(): Path {
        val local = System.getenv("LOCALAPPDATA")
        val base = if (!local.isNullOrBlank()) {
            Path.of(local)
        } else {
            Path.of(System.getProperty("java.io.tmpdir"))
        }
        val directory = base.resolve(AppInfo.DATA_DIR_NAME)
        Files.createDirectories(directory)
        return directory
    }

    private fun lockFile(): Path = baseDir().resolve("app-instance.lock")
    private fun portFile(): Path = baseDir().resolve("app-instance.port")
}
