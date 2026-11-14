package com.huoleme.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.huoleme.app.data.AppState
import com.huoleme.app.desktop.AutoStart
import com.huoleme.app.desktop.DesktopTray
import com.huoleme.app.desktop.VirtualKeyboard
import com.huoleme.app.ui.SwingFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.GraphicsEnvironment
import java.time.LocalDate
import java.time.LocalTime
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/**
 * Two-tier startup.
 *
 *  - Background tier: a plain AWT tray icon + a reminder thread. No Compose, no
 *    Skia — this is all the resident process needs, so a never-opened auto-start
 *    run stays around 77 MB.
 *  - UI tier: Compose Desktop's `application {}`, entered only when a window is
 *    shown (normal launch, tray click, or a 2nd instance).
 *
 * Closing the window hides it (visible = false) so `application {}` keeps
 * running and the tray stays responsive with no reinitialisation delay.
 */
fun main(args: Array<String>) {
    // Must be first: a 2nd launch must not spin up AWT/Swing/Compose. It just
    // pokes the running instance and exits, leaving no orphan JVM behind.
    if (!SingleInstance.acquire()) {
        SingleInstance.notifyRunning()
        kotlin.system.exitProcess(0)
    }
    Runtime.getRuntime().addShutdownHook(Thread { SingleInstance.release() })

    SwingFonts.configureTextRendering()

    val startHidden = AutoStart.ARG in args
    val appState = AppState()

    // One tray instance, shared by the background service and the UI. It needs
    // only AWT, never Compose, so the background process can stay lightweight.
    val tray = DesktopTray(
        appState = appState,
        onShowWindow = { requestShow() },
        onExit = {
            // The tray icon is removed by the OS when the process exits.
            VirtualKeyboard.hide()
            kotlin.system.exitProcess(0)
        }
    )

    SingleInstance.onShowRequest = { requestShow() }
    SingleInstance.startIpcServer()

    val trayInstalled = tray.install()
    startReminderLoop(appState, tray)

    // Normal launch (or no usable tray) opens the UI immediately. Auto-start
    // with a working tray stays in the lightweight background instead.
    val openUiNow = !(startHidden && trayInstalled)
    if (!openUiNow) {
        // Lightweight tier: park the main thread. The moment a "show" request
        // arrives (tray click or another instance), we enter the UI tier.
        while (!requestedShow.compareAndSet(true, false)) {
            Thread.sleep(150)
        }
    }
    runUi(appState, tray)
}

/**
 * Set by the tray / IPC when the window should appear. While we are still in the
 * lightweight tier, main() polls [requestedShow] and enters the UI; once the UI
 * is up it calls [showWindowRef] directly so the same click is instant.
 */
private val requestedShow = java.util.concurrent.atomic.AtomicBoolean(false)

/** Touched by the AWT/tray thread and read here, so it must be volatile. */
@Volatile
private var inUi = false

@Volatile
private var showWindowRef: (() -> Unit)? = null

private fun requestShow() {
    // Prefer the live UI (instant raise). If the UI is not up yet, fall back to
    // the flag so main() enters the UI instead of losing the click.
    val show = showWindowRef
    if (inUi && show != null) {
        show()
    } else {
        requestedShow.set(true)
    }
}

/**
 * Reminder loop lives outside Compose so it runs identically in both tiers with
 * exactly one instance. It wakes once a minute and, if the user has not checked
 * in past the configured time today, raises a tray notification.
 */
private fun startReminderLoop(appState: AppState, tray: DesktopTray) {
    Thread({
        while (true) {
            runCatching {
                val now = LocalTime.now()
                val target = LocalTime.of(
                    appState.settings.notificationHour,
                    appState.settings.notificationMinute
                )
                val today = LocalDate.now().toString()
                if (
                    !appState.isCheckedInToday &&
                    !now.isBefore(target) &&
                    appState.lastNotifiedDate != today &&
                    tray.isInstalled
                ) {
                    if (tray.notifyMissedCheckIn()) appState.markNotifiedToday()
                }
            }
            Thread.sleep(60_000L)
        }
    }, "huoleme-reminder").apply { isDaemon = true }.start()
}

private fun runUi(appState: AppState, tray: DesktopTray) {
    requestedShow.set(false)
    application {
        inUi = true
        val appWindow = remember { mutableStateOf<java.awt.Window?>(null) }
        val windowVisible = remember { mutableStateOf(true) }
        var trayHintShown by remember { mutableStateOf(false) }
        var autoStartStatus by remember { mutableStateOf(Strings.autoStartChecking) }

        fun showWindow() {
            windowVisible.value = true
            SwingUtilities.invokeLater {
                appWindow.value?.let { window ->
                    window.isVisible = true
                    window.toFront()
                    window.requestFocus()
                }
            }
        }
        showWindowRef = { showWindow() }

        DisposableEffect(Unit) {
            onDispose { showWindowRef = null }
        }

        LaunchedEffect(appState.settings.autoStartEnabled) {
            val enabled = appState.settings.autoStartEnabled
            val result = withContext(Dispatchers.IO) {
                if (enabled) AutoStart.install() else AutoStart.uninstall()
            }
            autoStartStatus = result.message
        }

        // The Window is always composed, but its visibility is toggled. This
        // keeps `application {}` alive (so the process keeps running in the
        // tray) while still letting the user close the window to the tray.
        remember { SwingFonts.install() }

        val screenConfig = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration
        val width = (screenConfig.bounds.width.toFloat() - 48f).coerceIn(390f, 430f)
        val height = (screenConfig.bounds.height.toFloat() - 72f).coerceIn(480f, 900f)
        val windowState = rememberWindowState(
            width = width.dp,
            height = height.dp,
            position = WindowPosition(Alignment.Center)
        )
        val appIcon = remember {
            runCatching {
                val stream = AppInfo::class.java.getResourceAsStream("/icon.png")
                    ?: return@runCatching null
                BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
            }.getOrNull()
        }

        Window(
            visible = windowVisible.value,
            onCloseRequest = {
                VirtualKeyboard.hide()
                if (appState.settings.minimizeToTray && tray.isInstalled) {
                    windowVisible.value = false // minimize to tray, keep running
                    if (!trayHintShown) {
                        tray.notifyMinimizedToTray()
                        trayHintShown = true
                    }
                } else {
                    kotlin.system.exitProcess(0)
                }
            },
            title = AppInfo.NAME,
            icon = appIcon,
            state = windowState,
            alwaysOnTop = false
        ) {
            LaunchedEffect(Unit) {
                appWindow.value = window
                window.minimumSize = Dimension(390, 480)
                // 切换到别的应用时收起触摸键盘，免得它一直盖在屏幕上。
                window.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) = Unit
                    override fun windowLostFocus(e: WindowEvent?) = VirtualKeyboard.hide()
                })
            }
            DisposableEffect(Unit) {
                onDispose {
                    if (appWindow.value === window) appWindow.value = null
                }
            }
            // 窗口级监听：按下非输入框区域时收起触摸键盘（Compose 点空白处不会
            // 清除输入框焦点，否则键盘会一直挂在屏幕上）。
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) {
                                    VirtualKeyboard.onPointerPressed()
                                }
                            }
                        }
                    }
            ) {
                HuolemeApp(
                    appState = appState,
                    tray = tray,
                    autoStartStatus = autoStartStatus,
                    windowWidth = windowState.size.width
                )
            }
        }
    }
}
