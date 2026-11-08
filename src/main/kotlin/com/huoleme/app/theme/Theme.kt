package com.huoleme.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

enum class AppThemeMode {
    CLASSIC_BLUE,
    SURVIVAL_RED,
    FOREST_GREEN,
    SUNSET_ORANGE,
    DYNAMIC,
    CUSTOM
}

private val BlueLightScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = Color(0xFFF0F4FA),
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F3F5),
    onSecondaryContainer = Color(0xFF202124),
    background = Color(0xFFF4F9FF),
    onBackground = BlueOnBackground,
    surface = Color(0xFFF4F9FF),
    onSurface = BlueOnSurface,
    surfaceVariant = Color(0xFFE0EFFF),
    onSurfaceVariant = Color(0xFF43474E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color.Transparent,
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    surfaceDim = Color(0xFFE5E7EB),
    surfaceBright = Color(0xFFFFFFFF)
)

private val BlueDarkScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = BlueDarkOnPrimary,
    primaryContainer = BlueDarkPrimaryContainer,
    onPrimaryContainer = BlueDarkOnPrimaryContainer,
    secondary = BlueDarkSecondary,
    onSecondary = Color(0xFF202124),
    secondaryContainer = BlueDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFE8EAED),
    background = BlueDarkBackground,
    onBackground = BlueDarkOnBackground,
    surface = BlueDarkSurface,
    onSurface = BlueDarkOnSurface,
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val RedLightScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = RedOnPrimary,
    primaryContainer = Color(0xFFFDF2F2),
    onPrimaryContainer = RedOnPrimaryContainer,
    secondary = RedSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF212121),
    background = Color(0xFFFFF5F5),
    onBackground = RedOnBackground,
    surface = Color(0xFFFFF5F5),
    onSurface = RedOnSurface,
    surfaceVariant = Color(0xFFFFE0E0),
    onSurfaceVariant = Color(0xFF534341),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color.Transparent,
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    surfaceDim = Color(0xFFE5E7EB),
    surfaceBright = Color(0xFFFFFFFF)
)

private val RedDarkScheme = darkColorScheme(
    primary = RedDarkPrimary,
    onPrimary = RedDarkOnPrimary,
    primaryContainer = RedDarkPrimaryContainer,
    onPrimaryContainer = RedDarkOnPrimaryContainer,
    secondary = RedDarkSecondary,
    onSecondary = Color(0xFF212121),
    secondaryContainer = RedDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFECEFF1),
    background = RedDarkBackground,
    onBackground = RedDarkOnBackground,
    surface = RedDarkSurface,
    onSurface = RedDarkOnSurface,
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val GreenLightScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = Color(0xFFF2F8F2),
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F6F4),
    onSecondaryContainer = Color(0xFF2E302E),
    background = Color(0xFFF2FCF3),
    onBackground = GreenOnBackground,
    surface = Color(0xFFF2FCF3),
    onSurface = GreenOnSurface,
    surfaceVariant = Color(0xFFDDF6E0),
    onSurfaceVariant = Color(0xFF43483E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color.Transparent,
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    surfaceDim = Color(0xFFE5E7EB),
    surfaceBright = Color(0xFFFFFFFF)
)

private val GreenDarkScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = GreenDarkOnPrimary,
    primaryContainer = GreenDarkPrimaryContainer,
    onPrimaryContainer = GreenDarkOnPrimaryContainer,
    secondary = GreenDarkSecondary,
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = GreenDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFE8EFE9),
    background = GreenDarkBackground,
    onBackground = GreenDarkOnBackground,
    surface = GreenDarkSurface,
    onSurface = GreenDarkOnSurface,
    surfaceVariant = Color(0xFF43483E),
    onSurfaceVariant = Color(0xFFC3C8BB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val OrangeLightScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = Color(0xFFFFF7F2),
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6F6F6),
    onSecondaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFF7F0),
    onBackground = OrangeOnBackground,
    surface = Color(0xFFFFF7F0),
    onSurface = OrangeOnSurface,
    surfaceVariant = Color(0xFFFFE5D0),
    onSurfaceVariant = Color(0xFF52443C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color.Transparent,
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    surfaceDim = Color(0xFFE5E7EB),
    surfaceBright = Color(0xFFFFFFFF)
)

private val OrangeDarkScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = OrangeDarkOnPrimary,
    primaryContainer = OrangeDarkPrimaryContainer,
    onPrimaryContainer = OrangeDarkOnPrimaryContainer,
    secondary = OrangeDarkSecondary,
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = OrangeDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFEFEBE9),
    background = OrangeDarkBackground,
    onBackground = OrangeDarkOnBackground,
    surface = OrangeDarkSurface,
    onSurface = OrangeDarkOnSurface,
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

fun getCustomLightScheme(customColor: Color): ColorScheme = lightColorScheme(
    primary = customColor,
    onPrimary = Color.White,
    primaryContainer = customColor.copy(alpha = 0.15f).compositeOver(Color.White),
    onPrimaryContainer = Color(0xFF202124),
    secondary = customColor.copy(alpha = 0.7f).compositeOver(Color.White),
    onSecondary = Color.White,
    secondaryContainer = customColor.copy(alpha = 0.1f).compositeOver(Color.White),
    onSecondaryContainer = Color(0xFF202124),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    surfaceVariant = customColor.copy(alpha = 0.05f).compositeOver(Color.White),
    onSurfaceVariant = Color(0xFF43474E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color.Transparent,
    surfaceContainer = customColor.copy(alpha = 0.03f).compositeOver(Color.White),
    surfaceContainerLow = customColor.copy(alpha = 0.02f).compositeOver(Color.White),
    surfaceContainerHigh = customColor.copy(alpha = 0.06f).compositeOver(Color.White),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = customColor.copy(alpha = 0.08f).compositeOver(Color.White),
    surfaceDim = customColor.copy(alpha = 0.06f).compositeOver(Color.White),
    surfaceBright = Color(0xFFFFFFFF)
)

fun getCustomDarkScheme(customColor: Color): ColorScheme {
    val darkBg = Color(0xFF121212)
    val surfaceBg = Color(0xFF1D1B20)
    return darkColorScheme(
        primary = customColor,
        onPrimary = Color(0xFF121212),
        primaryContainer = customColor.copy(alpha = 0.25f).compositeOver(surfaceBg),
        onPrimaryContainer = Color(0xFFE8EAED),
        secondary = customColor.copy(alpha = 0.6f).compositeOver(surfaceBg),
        onSecondary = Color(0xFF121212),
        secondaryContainer = customColor.copy(alpha = 0.15f).compositeOver(surfaceBg),
        onSecondaryContainer = Color(0xFFE8EAED),
        background = darkBg,
        onBackground = Color(0xFFE8EAED),
        surface = surfaceBg,
        onSurface = Color(0xFFE8EAED),
        surfaceVariant = customColor.copy(alpha = 0.1f).compositeOver(surfaceBg),
        onSurfaceVariant = Color(0xFFC3C7D0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HuolemeTheme(
    themeMode: AppThemeMode = AppThemeMode.CLASSIC_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    customColor: Color = Color(0xFF00897B),
    typography: Typography = AppTypography,
    content: @Composable () -> Unit
) {
    val effectiveMode = if (themeMode == AppThemeMode.DYNAMIC) AppThemeMode.CLASSIC_BLUE else themeMode

    val colorScheme = if (darkTheme) {
        when (effectiveMode) {
            AppThemeMode.CLASSIC_BLUE, AppThemeMode.DYNAMIC -> BlueDarkScheme
            AppThemeMode.SURVIVAL_RED -> RedDarkScheme
            AppThemeMode.FOREST_GREEN -> GreenDarkScheme
            AppThemeMode.SUNSET_ORANGE -> OrangeDarkScheme
            AppThemeMode.CUSTOM -> getCustomDarkScheme(customColor)
        }
    } else {
        when (effectiveMode) {
            AppThemeMode.CLASSIC_BLUE, AppThemeMode.DYNAMIC -> BlueLightScheme
            AppThemeMode.SURVIVAL_RED -> RedLightScheme
            AppThemeMode.FOREST_GREEN -> GreenLightScheme
            AppThemeMode.SUNSET_ORANGE -> OrangeLightScheme
            AppThemeMode.CUSTOM -> getCustomLightScheme(customColor)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}
