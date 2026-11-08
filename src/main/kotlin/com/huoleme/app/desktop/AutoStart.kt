package com.huoleme.app.desktop

import java.io.File
import java.util.concurrent.TimeUnit

data class AutoStartResult(val success: Boolean, val message: String)

private data class CommandResult(val success: Boolean, val output: String)

/**
 * Per-user Windows auto-start through HKCU\Software\Microsoft\Windows\CurrentVersion\Run.
 *
 * No scheduled task and no administrator rights: Windows launches the packaged
 * exe with the plain word `autostart`, and main() starts the app hidden in the
 * system tray instead of showing the main window.
 *
 * reg.exe is the fast path. Some managed PCs (and sandboxed launchers) block it,
 * so every operation falls back to a hidden PowerShell command using the same
 * per-user registry key.
 */
object AutoStart {
    const val ARG = "autostart"

    private const val VALUE_NAME = "Huoleme"
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val PS_PATH = "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

    /** Absolute path of the packaged exe, or null when running from a dev JVM. */
    fun launcher(): String? {
        val appPath = System.getProperty("jpackage.app-path")
        if (!appPath.isNullOrBlank()) {
            val candidate = File(appPath)
            if (candidate.isFile && candidate.name.endsWith(".exe", true)) {
                return candidate.absolutePath
            }
        }

        // jpackage app-image layout: <app>\活了么.exe + <app>\runtime\bin\java.exe
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            val parent = File(javaHome).parentFile
            val exe = parent?.listFiles()
                ?.firstOrNull { it.isFile && it.name.endsWith(".exe", true) }
            if (exe != null) return exe.absolutePath
        }

        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val file = File(command)
        val name = file.name.lowercase()
        if (!file.isFile || !name.endsWith(".exe")) return null
        if (name == "java.exe" || name == "javaw.exe") return null
        return file.absolutePath
    }

    /** Registry value: quoted exe path plus the plain `autostart` argument. */
    fun command(): String? = launcher()?.let { "\"$it\" $ARG" }

    fun isEnabled(): Boolean = readValue()?.contains(ARG, ignoreCase = true) == true

    fun install(): AutoStartResult {
        if (!isWindows()) {
            return AutoStartResult(false, "开机自启动仅支持 Windows")
        }
        val command = command()
            ?: return AutoStartResult(false, "当前为开发模式，打包后会自动注册开机自启动")
        val written = writeValue(command)
        if (!written.success) {
            return AutoStartResult(false, "注册自启动失败：${written.output.ifBlank { "注册表不可用" }}")
        }
        return if (isEnabled()) {
            AutoStartResult(true, "已开启开机自启动（登录后隐藏在托盘）")
        } else {
            AutoStartResult(false, "自启动项写入后校验失败，请检查注册表权限")
        }
    }

    fun uninstall(): AutoStartResult {
        if (!isWindows()) return AutoStartResult(false, "开机自启动仅支持 Windows")
        if (!isEnabled()) return AutoStartResult(true, "已关闭开机自启动")
        val removed = deleteValue()
        return if (removed.success) {
            AutoStartResult(true, "已关闭开机自启动")
        } else {
            AutoStartResult(false, "移除自启动失败：${removed.output.ifBlank { "注册表不可用" }}")
        }
    }

    private fun readValue(): String? {
        val reg = run("reg.exe", "query", RUN_KEY, "/v", VALUE_NAME)
        if (reg.success) return reg.output
        val ps = powershell(
            "(Get-ItemProperty -Path '$PS_PATH' -Name '$VALUE_NAME' " +
                "-ErrorAction SilentlyContinue).$VALUE_NAME"
        )
        return ps.output.takeIf { ps.success && it.isNotBlank() }
    }

    private fun writeValue(value: String): CommandResult {
        val reg = run(
            "reg.exe", "add", RUN_KEY,
            "/v", VALUE_NAME, "/t", "REG_SZ", "/d", value, "/f"
        )
        if (reg.success) return reg
        val escaped = value.replace("'", "''")
        return powershell(
            "New-ItemProperty -Path '$PS_PATH' -Name '$VALUE_NAME' " +
                "-Value '$escaped' -PropertyType String -Force | Out-Null"
        )
    }

    private fun deleteValue(): CommandResult {
        val reg = run("reg.exe", "delete", RUN_KEY, "/v", VALUE_NAME, "/f")
        if (reg.success) return reg
        return powershell(
            "Remove-ItemProperty -Path '$PS_PATH' -Name '$VALUE_NAME' " +
                "-ErrorAction SilentlyContinue"
        )
    }

    private fun powershell(script: String): CommandResult = run(
        "powershell.exe",
        "-NoProfile",
        "-NonInteractive",
        "-WindowStyle",
        "Hidden",
        "-Command",
        script
    )

    private fun run(vararg command: String): CommandResult = runCatching {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching CommandResult(false, "命令执行超时")
        }
        CommandResult(process.exitValue() == 0, output.trim())
    }.getOrElse { CommandResult(false, it.message ?: "无法执行系统命令") }
}
