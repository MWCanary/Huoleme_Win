package com.huoleme.app.desktop

import com.huoleme.app.AppInfo
import com.huoleme.app.data.AppState
import com.huoleme.app.ui.SwingFonts
import java.awt.Color
import java.awt.Image
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * Windows tray integration.
 *
 * The tray menu is a Swing JPopupMenu instead of AWT PopupMenu: AWT's native
 * Windows menu ignores MenuItem.font in this runtime, which made every Chinese
 * label render as empty square glyphs. Swing renders through Java2D and uses
 * the explicit CJK font reliably.
 */
class DesktopTray(
    private val appState: AppState,
    private val onShowWindow: () -> Unit,
    private val onExit: () -> Unit
) {
    private var trayIcon: TrayIcon? = null
    private var openMenu: JPopupMenu? = null
    private var menuOwner: JWindow? = null

    val isInstalled: Boolean
        get() = trayIcon != null

    fun install(): Boolean {
        if (!SystemTray.isSupported() || trayIcon != null) return trayIcon != null
        val image = loadImage() ?: return false

        val icon = TrayIcon(image, "${AppInfo.NAME} - 生命体征守护").apply {
            isImageAutoSize = true
            addActionListener { onShowWindow() }
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.button == MouseEvent.BUTTON1) {
                        onShowWindow()
                    }
                }

                override fun mousePressed(event: MouseEvent) {
                    if (event.isPopupTrigger || event.button == MouseEvent.BUTTON3) {
                        showSwingMenu()
                    }
                }

                override fun mouseReleased(event: MouseEvent) {
                    if (event.isPopupTrigger) {
                        showSwingMenu()
                    }
                }
            })
        }

        return runCatching {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            true
        }.getOrDefault(false)
    }

    fun notifyMissedCheckIn(): Boolean = display(
        title = "生命状态确认 ⚠️",
        body = "你还活着么？请打开「${AppInfo.NAME}」确认今日状态！",
        type = TrayIcon.MessageType.WARNING
    )

    fun notifyCheckedIn(): Boolean = display(
        title = "生存判定更新",
        body = "打卡成功！您目前状态为 [正常存活中]，连续守护时间已递增。",
        type = TrayIcon.MessageType.INFO
    )

    fun notifyTest(): Boolean = display(
        title = "守护测试通知",
        body = "Windows 通知通道可用，请及时确认今日生命状态。",
        type = TrayIcon.MessageType.INFO
    )

    fun notifyMinimizedToTray(): Boolean = display(
        title = "活了么仍在守护",
        body = "应用已最小化到系统托盘。点击托盘图标可重新打开；右键托盘图标可退出。",
        type = TrayIcon.MessageType.INFO
    )

    fun dispose() {
        closeMenu()
        val current = trayIcon ?: return
        runCatching { SystemTray.getSystemTray().remove(current) }
        trayIcon = null
    }

    private fun showSwingMenu() {
        if (openMenu?.isVisible == true) return
        val location = MouseInfo.getPointerInfo()?.location ?: return
        val font = cjkMenuFont()

        val menu = JPopupMenu().apply {
            this.font = font
            isLightWeightPopupEnabled = false
        }
        val openItem = JMenuItem("打开${AppInfo.NAME}").apply {
            this.font = font
            addActionListener { postAction { onShowWindow() } }
        }
        val checkInItem = JMenuItem("快速打卡").apply {
            this.font = font
            isEnabled = !appState.isCheckedInToday
            addActionListener {
                postAction {
                    val success = appState.checkIn()
                    if (success) {
                        display("打卡成功", "今日生存记录已更新。", TrayIcon.MessageType.INFO)
                    } else {
                        display("今日已打卡", "无需重复打卡。", TrayIcon.MessageType.INFO)
                    }
                }
            }
        }
        val exitItem = JMenuItem("退出${AppInfo.NAME}").apply {
            this.font = font
            addActionListener {
                postAction {
                    dispose()
                    onExit()
                }
            }
        }

        menu.add(openItem)
        menu.add(checkInItem)
        menu.addSeparator()
        menu.add(exitItem)
        menu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(event: PopupMenuEvent?) = Unit
            override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent?) = closeMenu()
            override fun popupMenuCanceled(event: PopupMenuEvent?) = closeMenu()
        })

        // Heavyweight owner keeps the popup above the taskbar. It must NOT be
        // always-on-top: a leftover transparent always-on-top window makes the
        // whole app look like it forces itself above every other window.
        val owner = JWindow().apply {
            setBounds(location.x, location.y, 1, 1)
            background = Color(0, 0, 0, 0)
            isVisible = true
        }
        menuOwner = owner
        openMenu = menu
        menu.show(owner, 0, 0)
    }

    /**
     * Delay the callback until the popup finished dispatching the click.
     * Disposing the owner synchronously inside popupMenuWillBecomeInvisible used
     * to cancel the pending ActionEvent, so 打开 / 退出 appeared to do nothing.
     */
    private fun postAction(action: () -> Unit) {
        SwingUtilities.invokeLater { runCatching { action() } }
    }

    private fun closeMenu() {
        val menu = openMenu
        val owner = menuOwner
        openMenu = null
        menuOwner = null
        SwingUtilities.invokeLater {
            runCatching { menu?.isVisible = false }
            runCatching { owner?.dispose() }
        }
    }

    private fun display(title: String, body: String, type: TrayIcon.MessageType): Boolean {
        val current = trayIcon ?: return false
        return runCatching {
            current.displayMessage(title, body, type)
            true
        }.getOrDefault(false)
    }

    private fun loadImage(): Image? = runCatching {
        AppInfo::class.java.getResourceAsStream("/icon.png")?.use { stream ->
            ImageIO.read(stream)
        }
    }.getOrNull()

    private fun cjkMenuFont(): java.awt.Font = SwingFonts.menuFont()
}
