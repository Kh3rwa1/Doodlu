package com.doodlu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KawaiiColorScheme = lightColorScheme(
    primary            = KawaiiPink,
    onPrimary          = Color.White,
    primaryContainer   = KawaiiCodeBg,
    onPrimaryContainer = KawaiiTextPri,
    secondary          = KawaiiPurple,
    onSecondary        = Color.White,
    secondaryContainer = KawaiiCodeBg,
    onSecondaryContainer = KawaiiTextPri,
    tertiary           = KawaiiYellow,
    onTertiary         = KawaiiTextPri,
    background         = KawaiiBlush,
    onBackground       = KawaiiTextPri,
    surface            = KawaiiCard,
    onSurface          = KawaiiTextPri,
    surfaceVariant     = KawaiiCodeBg,
    onSurfaceVariant   = KawaiiTextSec,
    error              = KawaiiRed,
    onError            = Color.White,
    outline            = KawaiiCodeBorder,
    outlineVariant     = Color(0xFFE8D5FF),
)

@Composable
fun DoodluTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KawaiiColorScheme,
        typography  = DoodluTypography,
        content     = content
    )
}
