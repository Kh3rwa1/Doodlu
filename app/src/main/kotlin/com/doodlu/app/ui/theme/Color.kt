package com.doodlu.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Kawaii / Candy UI Palette ──────────────────────────────────────────────

// Backgrounds
val KawaiiBlush      = Color(0xFFFFF0F5)   // lavender blush
val KawaiiLavender   = Color(0xFFF8E8FF)   // soft lavender
val KawaiiPeach      = Color(0xFFFFE8D6)   // soft peach

// Surface / Cards
val KawaiiCard       = Color(0xFFFFFFFF)
val KawaiiCardAlpha  = Color(0xE6FFFFFF)   // 90% opacity white

// Accent colors
val KawaiiPink       = Color(0xFFFF6B9D)   // primary hot pink
val KawaiiCoral      = Color(0xFFFF8A65)   // coral (gradient end)
val KawaiiPurple     = Color(0xFFC3A6FF)   // lavender purple
val KawaiiYellow     = Color(0xFFFFD93D)   // warm yellow / sparkles
val KawaiiGreen      = Color(0xFF6BCB77)   // success / connected
val KawaiiRed        = Color(0xFFFF6B6B)   // error / disconnected

// Text
val KawaiiTextPri    = Color(0xFF2D2D3A)   // dark charcoal
val KawaiiTextSec    = Color(0xFF8E8EA0)   // muted purple-grey

// Room code bubbles
val KawaiiCodeBg     = Color(0xFFE8D5FF)
val KawaiiCodeBorder = Color(0xFFC3A6FF)

// Input fields
val KawaiiInputBg    = Color(0xFFFFF5F0)
val KawaiiInputFocus = Color(0xFFFFE8EC)

// Drawing canvas (keep dark for contrast)
val CanvasBg         = Color(0xFF1A1A2E)
val CanvasSurface    = Color(0xFF16213E)
val CanvasSurfaceVar = Color(0xFF0F3460)

// Drawing colors (updated palette)
val DrawColorRed     = Color(0xFFE94560)
val DrawColorCoral   = Color(0xFFFF8A65)
val DrawColorYellow  = Color(0xFFFFD93D)
val DrawColorGreen   = Color(0xFF6BCB77)
val DrawColorBlue    = Color(0xFF118AB2)
val DrawColorPurple  = Color(0xFFC3A6FF)
val DrawColorPink    = Color(0xFFFF69B4)
val DrawColorWhite   = Color(0xFFFFFFFF)

val DrawingColors = listOf(
    "#E94560", "#FF8A65", "#FFD93D", "#6BCB77",
    "#118AB2", "#C3A6FF", "#FF69B4", "#FFFFFF"
)

val DrawingColorsCompose = listOf(
    DrawColorRed, DrawColorCoral, DrawColorYellow, DrawColorGreen,
    DrawColorBlue, DrawColorPurple, DrawColorPink, DrawColorWhite
)

// Legacy aliases (keep for wallpaper service compatibility)
val DoodluBackground    = CanvasBg
val DoodluSurface       = CanvasSurface
val DoodluSurfaceVariant = CanvasSurfaceVar
val DoodluPrimary       = KawaiiPink
val DoodluSecondary     = KawaiiPurple
val DoodluSuccess       = KawaiiGreen
val DoodluWarning       = KawaiiYellow
val DoodluTextPrimary   = KawaiiTextPri
val DoodluTextSecondary = KawaiiTextSec
