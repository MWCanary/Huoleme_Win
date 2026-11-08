package com.huoleme.app.ui

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import javax.swing.UIManager
import kotlin.math.roundToInt

object SwingFonts {
    private val preferred = listOf("Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans SC", "SimSun")

    /**
     * Must run before AWT/Skia initialization. LCD (ClearType) text AA and DPI
     * awareness stop Windows from bitmap-scaling the whole window, which is the
     * classic cause of blurry text on 125% / 150% displays.
     * The same flags are also passed as -D options by the packaged launcher.
     */
    fun configureTextRendering() {
        System.setProperty("sun.java2d.dpiaware", "true")
        System.setProperty("sun.java2d.uiScale.enabled", "true")
        System.setProperty("awt.useSystemAAFontSettings", "lcd")
        System.setProperty("swing.aatext", "true")
    }

    fun install() {
        val font = uiFont(12)
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        listOf(
            "Label.font",
            "Button.font",
            "TextField.font",
            "PasswordField.font",
            "ComboBox.font",
            "Menu.font",
            "MenuItem.font",
            "OptionPane.messageFont",
            "OptionPane.buttonFont",
            "FileChooser.font"
        ).forEach { key -> UIManager.put(key, font) }
    }

    /** Tray menu font, scaled with the display DPI so it never renders tiny/fuzzy. */
    fun menuFont(): Font = uiFont(13)

    private fun uiFont(base: Int): Font {
        val available = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .toSet()
        }.getOrDefault(emptySet())
        val family = preferred.firstOrNull { it in available } ?: Font.SANS_SERIF
        val scale = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
        }.getOrDefault(1.0).coerceIn(1.0, 2.0)
        return Font(family, Font.PLAIN, (base * scale).roundToInt().coerceIn(base, 26))
    }
}
