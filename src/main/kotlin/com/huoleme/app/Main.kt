package com.huoleme.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.huoleme.app.data.AppState
import com.huoleme.app.desktop.AutoStart
import com.huoleme.app.desktop.DesktopTray
import com.huoleme.app.ui.SwingFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.time.LocalDate
import java.time.LocalTime
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    SwingFonts.configureTextRendering()
    SwingFonts.install()

    // Windows Run entry launches the exe with the plain word "autostart".
    // In that mode only the tray is installed; the main window stays hidden.
    val startHidden = AutoStart.ARG in args

    if (!SingleInstance.acquire()) {
        JOptionPane.showMessageDialog(
            null,
            "活了么已经在运行，请查看 Windows 系统托盘。",
            AppInfo.NAME,
            JOptionPane.INFORMATION_MESSAGE
        )
        return
    }

    try {
        application {
            val screenConfig = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration
            val windowWidth = (screenConfig.bounds.width.toFloat() - 48f).coerceIn(390f, 430f)
            val windowHeight = (screenConfig.bounds.height.toFloat() - 72f).coerceIn(480f, 900f)

            val windowState = rememberWindowState(
                width = windowWidth.dp,
                height = windowHeight.dp,
                position = WindowPosition(Alignment.Center)
            )
            val appState = remember { AppState() }
            val trayReady = remember { mutableStateOf(false) }
            val appWindow = remember { mutableStateOf<java.awt.Window?>(null) }
            var windowVisible by remember { mutableStateOf(!startHidden) }
            var trayHintShown by remember { mutableStateOf(false) }
            var autoStartStatus by remember { mutableStateOf(Strings.autoStartChecking) }

            val tray = remember {
                DesktopTray(
                    appState = appState,
                    onShowWindow = {
                        windowVisible = true
                        SwingUtilities.invokeLater {
                            appWindow.value?.let { window ->
                                window.isVisible = true
                                window.toFront()
                                window.requestFocus()
                            }
                        }
                    },
                    onExit = {
                        // Let the tray menu finish dispatching the click first.
                        SwingUtilities.invokeLater {
                            runCatching { exitApplication() }
                            // Fallback: with the window hidden in the tray, some
                            // runtimes keep the AWT loop alive. Never ignore 退出.
                            Thread {
                                Thread.sleep(2_500L)
                                kotlin.system.exitProcess(0)
                            }.apply { isDaemon = true }.start()
                        }
                    }
                )
            }

            val appIcon = remember {
                runCatching {
                    val stream = AppInfo::class.java.getResourceAsStream("/icon.png")
                        ?: return@runCatching null
                    BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
                }.getOrNull()
            }

            // Tray, auto-start and the reminder loop live at application scope so
            // they keep working even when the window is hidden at boot.
            LaunchedEffect(Unit) {
                val installed = tray.install()
                trayReady.value = installed
                if (startHidden && !installed) windowVisible = true
            }
            DisposableEffect(Unit) {
                onDispose { tray.dispose() }
            }

            LaunchedEffect(appState.settings.autoStartEnabled) {
                val enabled = appState.settings.autoStartEnabled
                val result = withContext(Dispatchers.IO) {
                    if (enabled) AutoStart.install() else AutoStart.uninstall()
                }
                autoStartStatus = result.message
            }

            LaunchedEffect(
                appState.settings.notificationHour,
                appState.settings.notificationMinute,
                appState.isCheckedInToday,
                tray
            ) {
                while (true) {
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
                        if (tray.notifyMissedCheckIn()) {
                            appState.markNotifiedToday()
                        }
                    }
                    // A minute is enough and avoids waking the JVM every 20 s.
                    delay(60_000L)
                }
            }

            Window(
                visible = windowVisible,
                onCloseRequest = {
                    if (appState.settings.minimizeToTray && trayReady.value) {
                        windowVisible = false
                        if (!trayHintShown) {
                            tray.notifyMinimizedToTray()
                            trayHintShown = true
                        }
                    } else {
                        exitApplication()
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
                }
                HuolemeApp(
                    appState = appState,
                    tray = tray,
                    autoStartStatus = autoStartStatus,
                    windowWidth = windowState.size.width
                )
            }
        }
    } finally {
        SingleInstance.release()
    }
}
