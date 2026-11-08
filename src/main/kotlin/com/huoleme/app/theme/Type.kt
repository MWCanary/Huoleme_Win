package com.huoleme.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font as PlatformFont
import androidx.compose.ui.unit.sp
import java.awt.GraphicsEnvironment
import kotlin.math.roundToInt

private val preferredCjkFamilies = listOf(
    "Microsoft YaHei UI",
    "Microsoft YaHei",
    "Noto Sans SC",
    "SimHei"
)

private val systemFamilyName: String? = runCatching {
    val available = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .toSet()
    preferredCjkFamilies.firstOrNull { it in available }
}.getOrNull()

/**
 * Explicit CJK family with real Regular / Medium / SemiBold / Bold faces.
 *
 * FontFamily.Default resolves to a Latin face on Windows, so Chinese glyphs came
 * from a fallback font and bold titles were synthesized -> blurry, fuzzy text.
 */
val AppFontFamily: FontFamily = systemFamilyName?.let { name ->
    FontFamily(
        listOf(
            PlatformFont(name, FontWeight.Normal, FontStyle.Normal),
            PlatformFont(name, FontWeight.Medium, FontStyle.Normal),
            PlatformFont(name, FontWeight.SemiBold, FontStyle.Normal),
            PlatformFont(name, FontWeight.Bold, FontStyle.Normal)
        )
    )
} ?: FontFamily.SansSerif

val AppTypography: Typography = buildTypography(1f)

/**
 * Font sizes are always whole sp values. Fractional fontScale (0.85 / 0.92 / ...)
 * used to produce fractional pixel sizes, which made Skia render glyphs with
 * grey, soft edges on 125% / 150% DPI scaling.
 */
fun scaledTypography(scale: Float): Typography =
    if (scale == 1f) AppTypography else buildTypography(scale)

private fun buildTypography(scale: Float): Typography {
    fun size(base: Int) = (base * scale).roundToInt().coerceAtLeast(9).sp

    fun style(
        baseSize: Int,
        baseLine: Int,
        weight: FontWeight = FontWeight.Normal
    ) = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = weight,
        fontSize = size(baseSize),
        lineHeight = size(baseLine),
        letterSpacing = 0.sp
    )

    return Typography(
        displayLarge = style(57, 64),
        displayMedium = style(45, 52),
        displaySmall = style(36, 44),
        headlineLarge = style(32, 40),
        headlineMedium = style(28, 36),
        headlineSmall = style(24, 32),
        titleLarge = style(22, 28),
        titleMedium = style(16, 24, FontWeight.Medium),
        titleSmall = style(14, 20, FontWeight.Medium),
        bodyLarge = style(16, 24),
        bodyMedium = style(14, 20),
        bodySmall = style(12, 16),
        labelLarge = style(14, 20, FontWeight.Medium),
        labelMedium = style(12, 16, FontWeight.Medium),
        labelSmall = style(11, 16, FontWeight.Medium)
    )
}
