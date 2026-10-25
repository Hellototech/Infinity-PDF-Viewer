package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

object InfinityThemePalette {
    var isDark by mutableStateOf(true)
}

// Primary Background: Pure OLED Black (#000000) in Dark mode, Crisp Alabaster (#F5F6F9) in Light mode
val InfinityOledBlack: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFF000000) else Color(0xFFF5F6F9)

// Elevated Cards, Bottom Sheets, Surfaces: Dark Slate Gray (#1E1E1E) in Dark mode, Pure White (#FFFFFF) in Light mode
val InfinitySlateGray: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

// Secondary Elevated Surface: (#282828) in Dark mode, Soft Cool Gray (#EAECEF) in Light mode
val InfinitySlateElevated: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFF282828) else Color(0xFFEAECEF)

// Subtle Divider / Hairline Border: (#2E2E2E) in Dark mode, Subtle Border (#DADCE0) in Light mode
val InfinityDividerGray: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFF2E2E2E) else Color(0xFFDADCE0)

// Primary Text & Primary Icons: Crisp White (#FFFFFF) in Dark mode, Deep Charcoal (#141518) in Light mode
val InfinityCrispWhite: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFFFFFFFF) else Color(0xFF141518)

// Secondary Subtitles, Metadata & Inactive Icons: Muted Gray (#B3B3B3) in Dark mode, Slate Muted (#5F6368) in Light mode
val InfinityMutedGray: Color
    get() = if (InfinityThemePalette.isDark) Color(0xFFB3B3B3) else Color(0xFF5F6368)

// Accent: Strictly Crimson Red (#ED1C24) for active bottom navigation pills,
// selected tool states, and primary action icons across all themes.
val InfinityCrimsonRed = Color(0xFFED1C24)

// Annotation Canvas Ink Swatches
val InkCrimson = Color(0xFFED1C24)
val InkSolarYellow = Color(0xFFFFEA00)
val InkElectricCyan = Color(0xFF00E5FF)
val InkNeonEmerald = Color(0xFF00E676)
val InkPureWhite = Color(0xFFFFFFFF)
