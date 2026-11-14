package com.huoleme.app.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.StdCallLibrary.StdCallCallback
import java.io.File
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Windows 触摸键盘（屏幕键盘）的自动弹出与收起。
 *
 * 为什么需要它：Windows 只会为「实现了 UI Automation 文本模式」的应用自动弹
 * 触摸键盘。Compose Desktop 没有 UIA TextPattern，系统因此不知道输入框什么
 * 时候获得焦点，也就在任何情况下都不会自动弹键盘——这正是未接键盘时点输入框
 * 毫无反应的原因。
 *
 * 这里的做法：
 *  - 输入框获得焦点 → 用 ShellExecute 拉起系统触摸键盘（TabTip.exe）。
 *  - 输入框失焦 / 窗口隐藏 / 退出 → 向键盘宿主进程（TextInputHost.exe，旧版
 *    为 TabTip.exe）的窗口发送 WM_CLOSE 收起。
 *  - 只在「触摸设备且当前没有可用键盘」时自动弹（与 Windows 自己「未连接键盘
 *    时显示触摸键盘」的策略一致），接上实体键盘后不再打扰。
 *
 * 所有调用都做了容错：JNA 或系统组件不可用时静默降级，绝不影响主流程。
 */
object VirtualKeyboard {

    /** 延迟收起，避免输入框之间切换时键盘闪一下。 */
    private const val HIDE_DELAY_MS = 600L

    /** 按下非输入区域后延迟收起的时间，留给输入框先报告自己接管了这次按下。 */
    private const val PRESS_HIDE_DELAY_MS = 320L

    private const val SM_DIGITIZER = 94
    private const val SM_MAXIMUMTOUCHES = 95
    private const val SM_CONVERTIBLESLATEMODE = 0x2003
    private const val RIM_TYPEKEYBOARD = 1
    private const val RIDI_DEVICENAME = 0x20000007
    private const val WM_CLOSE = 0x0010
    private const val PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
    private const val RAW_INPUT_DEVICE_LIST_SIZE = 16 // sizeof(RAWINPUTDEVICELIST) on x64

    /** 按顺序尝试的触摸键盘可执行文件。 */
    private val TABTIP_PATHS = listOf(
        "C:\\Program Files\\Common Files\\microsoft shared\\ink\\TabTip.exe",
        "C:\\Program Files (x86)\\Common Files\\microsoft shared\\ink\\TabTip.exe"
    )

    /** 承载触摸键盘界面的进程（Win11 为 TextInputHost，Win10 为 TabTip）。 */
    private val KEYBOARD_PROCESSES = setOf("textinputhost.exe", "tabtip.exe")

    private val user32: User32Lib? = loadLibrary("user32", User32Lib::class.java)
    private val kernel32: Kernel32Lib? = loadLibrary("kernel32", Kernel32Lib::class.java)
    private val shell32: Shell32Lib? = loadLibrary("shell32", Shell32Lib::class.java)

    private val executor = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "huoleme-touch-keyboard").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    private val lock = Any()
    private var pendingHide: ScheduledFuture<*>? = null
    private var pendingHideOnPress: ScheduledFuture<*>? = null

    @Volatile
    private var showing = false

    /** 系统里是否存在可用的触摸键盘程序。 */
    val isAvailable: Boolean by lazy { tabTipPath() != null && shell32 != null }

    // ---------------------------------------------------------------- Compose

    /** 某个输入框获得焦点。 */
    fun onFieldFocused() {
        if (!isAvailable) return
        cancelPendingHide()
        if (showing || !shouldAutoShow()) return
        executor.execute {
            if (showing) return@execute
            if (launch()) showing = true
        }
    }

    /** 某个输入框失去焦点（延迟收起，焦点转到另一个输入框时会被取消）。 */
    fun onFieldBlurred() {
        if (!showing) return
        synchronized(lock) {
            if (pendingHide != null) return
            pendingHide = executor.schedule({
                synchronized(lock) { pendingHide = null }
                hide()
            }, HIDE_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    /** 立即收起（窗口收进托盘、退出、窗口失去焦点时调用）。 */
    fun hide() {
        cancelPendingHide()
        if (!showing) return
        showing = false
        executor.execute { closeKeyboardWindows() }
    }

    /**
     * 用户按下了应用里的某个位置（窗口级监听，先于控件收到）。
     * 若紧接着没有落进输入框，就把键盘收起来——Compose 点击空白处不会自动
     * 清除输入框焦点，不这样做键盘会一直挂在屏幕上。
     */
    fun onPointerPressed() {
        if (!showing) return
        synchronized(lock) {
            pendingHideOnPress?.cancel(false)
            pendingHideOnPress = executor.schedule({
                synchronized(lock) { pendingHideOnPress = null }
                hide()
            }, PRESS_HIDE_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    /** 按下的位置在输入框内（重新定位光标等），取消上面那次收起。 */
    fun onTextFieldPressed() {
        synchronized(lock) {
            pendingHideOnPress?.cancel(false)
            pendingHideOnPress = null
        }
    }

    // ----------------------------------------------------------------- 策略

    /**
     * 是否应该自动弹出触摸键盘。
     *
     * 与 Windows 的「未连接键盘时显示触摸键盘」保持一致：触摸设备 + 没有可用
     * 键盘才弹。可通过 `-Dhuoleme.virtualKeyboard=always|never` 强制覆盖（供
     * 排障使用，界面上不暴露）。
     */
    private fun shouldAutoShow(): Boolean {
        when (System.getProperty("huoleme.virtualKeyboard")?.lowercase()) {
            "always", "on", "true", "1" -> return true
            "never", "off", "false", "0" -> return false
        }
        val u = user32 ?: return false
        if (!runCatching { isTouchDevice(u) }.getOrDefault(false)) return false
        // 键盘翻折/平板模式：系统自己判定为「当平板用」，直接弹。
        if (runCatching { u.GetSystemMetrics(SM_CONVERTIBLESLATEMODE) == 1 }.getOrDefault(false)) {
            return true
        }
        return !runCatching { hasTypingKeyboard(u) }.getOrDefault(true)
    }

    private fun isTouchDevice(u: User32Lib): Boolean =
        u.GetSystemMetrics(SM_MAXIMUMTOUCHES) > 0 || (u.GetSystemMetrics(SM_DIGITIZER) and 0x01) != 0

    /**
     * 是否接着能打字的实体键盘。
     *
     * 只统计真正的键盘接口，忽略 Windows 为音量键、屏幕快捷键合成的 HID 集合
     * （`&Col01`）与 `ConvertedDevice`，以及 Surface 键盘盖留下的 `ACPI#MSFT`
     * 连接器节点——键盘盖拆下后它们仍在，若算作键盘就永远不会弹虚拟键盘了。
     */
    private fun hasTypingKeyboard(u: User32Lib): Boolean {
        val count = IntByReference(0)
        if (u.GetRawInputDeviceList(null, count, RAW_INPUT_DEVICE_LIST_SIZE) != 0) return true
        if (count.value <= 0) return false
        val list = Memory(count.value.toLong() * RAW_INPUT_DEVICE_LIST_SIZE)
        if (u.GetRawInputDeviceList(list, count, RAW_INPUT_DEVICE_LIST_SIZE) == -1) return true
        val total = count.value
        for (i in 0 until total) {
            val base = i.toLong() * RAW_INPUT_DEVICE_LIST_SIZE
            val type = list.getInt(base + 8)
            if (type != RIM_TYPEKEYBOARD) continue
            val device = list.getLong(base)
            if (device == 0L) continue
            val name = readDeviceName(u, Pointer(device))
            if (looksLikeTypingKeyboard(name)) return true
        }
        return false
    }

    private fun readDeviceName(u: User32Lib, device: Pointer): String = runCatching {
        val size = IntByReference(0)
        u.GetRawInputDeviceInfoW(device, RIDI_DEVICENAME, null, size)
        if (size.value <= 0) return@runCatching ""
        val buffer = Memory((size.value + 1).toLong() * 2)
        u.GetRawInputDeviceInfoW(device, RIDI_DEVICENAME, buffer, size)
        buffer.getWideString(0)
    }.getOrDefault("")

    internal fun looksLikeTypingKeyboard(rawName: String): Boolean {
        if (rawName.isEmpty()) return false
        val name = rawName.uppercase()
        if (name.contains("&COL")) return false // HID 顶层集合：音量/媒体/快捷键
        if (name.contains("CONVERTEDDEVICE")) return false
        if (name.contains("BTHENUM") || name.contains("BTHHFENUM")) return true // 蓝牙键盘
        if (name.contains("MI_00")) return true // HID 键盘接口
        if (name.contains("PNP0303") || name.contains("PNP030B")) return true // PS/2 键盘
        if (name.contains("RDP_KBD")) return true // 远程桌面键盘
        return false
    }

    // ------------------------------------------------------------- Win32 操作

    private fun tabTipPath(): String? =
        TABTIP_PATHS.firstOrNull { runCatching { File(it).isFile }.getOrDefault(false) }

    /** 拉起系统触摸键盘。 */
    private fun launch(): Boolean {
        val path = tabTipPath() ?: return false
        val sh = shell32
        if (sh != null) {
            val result = runCatching {
                sh.ShellExecuteW(null, WString("open"), WString(path), null, null, 1)
            }.getOrNull()
            val code = result?.let { Pointer.nativeValue(it) } ?: 0L
            if (code > 32L) return true
        }
        // 兜底：交给 explorer 拉起（某些机器上 ShellExecute 会被策略拦掉）。
        return runCatching { ProcessBuilder("explorer.exe", path).start(); true }.getOrDefault(false)
    }

    /** 关闭所有触摸键盘窗口。 */
    private fun closeKeyboardWindows() {
        val u = user32 ?: return
        val k = kernel32 ?: return
        val targets = mutableListOf<Pointer>()
        runCatching {
            u.EnumWindows(object : WndEnumProc {
                override fun callback(hwnd: Pointer?, lParam: Pointer?): Boolean {
                    if (hwnd != null) {
                        val pid = IntByReference()
                        u.GetWindowThreadProcessId(hwnd, pid)
                        if (pid.value > 0 && processName(k, pid.value) in KEYBOARD_PROCESSES) {
                            targets.add(hwnd)
                        }
                    }
                    return true
                }
            }, null)
        }
        targets.forEach { hwnd -> runCatching { u.PostMessageW(hwnd, WM_CLOSE, null, null) } }
    }

    private fun processName(k: Kernel32Lib, pid: Int): String = runCatching {
        val handle = k.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid)
            ?: return@runCatching ""
        try {
            val buffer = Memory(2048L)
            val size = IntByReference(1024)
            if (!k.QueryFullProcessImageNameW(handle, 0, buffer, size)) return@runCatching ""
            File(buffer.getWideString(0)).name.lowercase()
        } finally {
            k.CloseHandle(handle)
        }
    }.getOrDefault("")

    private fun cancelPendingHide() {
        synchronized(lock) {
            pendingHide?.cancel(false)
            pendingHide = null
            pendingHideOnPress?.cancel(false)
            pendingHideOnPress = null
        }
    }

    private fun <T : Library> loadLibrary(name: String, type: Class<T>): T? =
        runCatching { Native.load(name, type) }.getOrNull()
}

internal interface User32Lib : StdCallLibrary {
    fun GetSystemMetrics(index: Int): Int
    fun GetRawInputDeviceList(list: Pointer?, count: IntByReference, size: Int): Int
    fun GetRawInputDeviceInfoW(device: Pointer?, command: Int, data: Pointer?, size: IntByReference): Int
    fun EnumWindows(callback: WndEnumProc, lParam: Pointer?): Boolean
    fun GetWindowThreadProcessId(hwnd: Pointer, pid: IntByReference): Int
    fun PostMessageW(hwnd: Pointer, message: Int, wParam: Pointer?, lParam: Pointer?): Boolean
}

internal interface Kernel32Lib : StdCallLibrary {
    fun OpenProcess(access: Int, inherit: Boolean, pid: Int): Pointer?
    fun QueryFullProcessImageNameW(process: Pointer, flags: Int, name: Memory, size: IntByReference): Boolean
    fun CloseHandle(handle: Pointer): Boolean
}

internal interface Shell32Lib : StdCallLibrary {
    fun ShellExecuteW(
        hwnd: Pointer?,
        operation: WString?,
        file: WString?,
        parameters: WString?,
        directory: WString?,
        show: Int
    ): Pointer?
}

internal interface WndEnumProc : StdCallCallback {
    fun callback(hwnd: Pointer?, lParam: Pointer?): Boolean
}
