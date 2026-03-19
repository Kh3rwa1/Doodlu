package com.doodlu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DoodluColorScheme = darkColorScheme(
    primary = DoodluPrimary,
    secondary = DoodluSecondary,
    tertiary = DoodluSuccess,
    background = DoodluBackground,
    surface = DoodluSurface,
    surfaceVariant = DoodluSurfaceVariant,
    onPrimary = DoodluTextPrimary,
    onSecondary = DoodluTextPrimary,
    onBackground = DoodluTextPrimary,
    onSurface = DoodluTextPrimary,
    onSurfaceVariant = DoodluTextSecondary,
    error = DoodluPrimary,
)

@Composable
fun DoodluTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DoodluColorScheme,
        typography = DoodluTypography,
        content = content
    )
}
