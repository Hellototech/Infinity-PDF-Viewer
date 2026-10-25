package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val InfinityOledDarkColorScheme = darkColorScheme(
    primary = InfinityCrimsonRed,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF282828),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = InfinityCrimsonRed,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1E1E1E),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = InfinityCrimsonRed,
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF2E2E2E),
    error = InfinityCrimsonRed,
    onError = Color(0xFFFFFFFF)
)

private val InfinityLightColorScheme = lightColorScheme(
    primary = InfinityCrimsonRed,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAECEF),
    onPrimaryContainer = Color(0xFF141518),
    secondary = InfinityCrimsonRed,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF141518),
    tertiary = InfinityCrimsonRed,
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F6F9),
    onBackground = Color(0xFF141518),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141518),
    surfaceVariant = Color(0xFFEAECEF),
    onSurfaceVariant = Color(0xFF5F6368),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAECEF),
    surfaceContainerHighest = Color(0xFFDADCE0),
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFDADCE0),
    error = InfinityCrimsonRed,
    onError = Color(0xFFFFFFFF)
)

val InfinityShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun InfinityPdfTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    // Update immediately during composition and keep synced via SideEffect
    InfinityThemePalette.isDark = useDark
    SideEffect {
        InfinityThemePalette.isDark = useDark
    }

    MaterialTheme(
        colorScheme = if (useDark) InfinityOledDarkColorScheme else InfinityLightColorScheme,
        typography = Typography,
        shapes = InfinityShapes,
        content = content
    )
}
