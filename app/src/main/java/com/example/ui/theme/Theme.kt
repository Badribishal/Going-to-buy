package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = ProfessionalBlueDark,
    onPrimaryContainer = ProfessionalBlueContainer,
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF333D4B),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = EmeraldLight,
    onTertiary = Color(0xFF003822),
    background = Color(0xFF111315),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF181A1D),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF24272C),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF4A4E54),
    outlineVariant = Color(0xFF2E3238),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = ProfessionalBlue,
    onPrimary = Color.White,
    primaryContainer = ProfessionalBlueContainer,
    onPrimaryContainer = ProfessionalNavyDeep,
    secondary = ProfessionalBlueDark,
    onSecondary = Color.White,
    secondaryContainer = ProfessionalSurfaceVariant,
    onSecondaryContainer = ProfessionalTextPrimary,
    tertiary = EmeraldAccent,
    onTertiary = Color.White,
    background = ProfessionalBg,
    onBackground = ProfessionalTextPrimary,
    surface = ProfessionalSurface,
    onSurface = ProfessionalTextPrimary,
    surfaceVariant = ProfessionalSurfaceVariant,
    onSurfaceVariant = ProfessionalTextSecondary,
    outline = ProfessionalBorder,
    outlineVariant = ProfessionalBorderSubtle,
    error = RoseError
)

@Composable
fun BuySpaceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

