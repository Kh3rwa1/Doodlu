package com.celestial.spire.ui.theme

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

// Canvas background presets — kawaii palette
val CanvasBgPresets = listOf(
    "#1A1A2E" to "Midnight",    // default dark navy
    "#2D2D3A" to "Charcoal",    // dark charcoal (KawaiiTextPri base)
    "#FFF0F5" to "Blush",       // KawaiiBlush
    "#F8E8FF" to "Lavender",    // KawaiiLavender
    "#FFE8D6" to "Peach",       // KawaiiPeach
    "#FFE8EC" to "Rose",        // KawaiiInputFocus
    "#E8D5FF" to "Purple",      // KawaiiCodeBg
    "#FFFFFF" to "White"
)

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

// ── Premium / Aurora Palette ──────────────────────────────────────────────
// Deep aurora tints for animated background mesh
val AuroraDeepPink     = Color(0xFFFF3D8B)
val AuroraMagenta      = Color(0xFFE040FB)
val AuroraViolet       = Color(0xFF7C4DFF)
val AuroraIndigo       = Color(0xFF536DFE)
val AuroraTeal         = Color(0xFF18FFFF)
val AuroraRose         = Color(0xFFFF6090)

// Glassmorphism
val GlassFill          = Color(0x33FFFFFF)   // 20% white
val GlassBorder        = Color(0x40FFFFFF)   // 25% white
val GlassHighlight     = Color(0x66FFFFFF)   // 40% white — top-edge shimmer

// Glow / Shimmer accents
val ShimmerPink        = Color(0xFFFF80AB)
val ShimmerGold        = Color(0xFFFFD740)
val ShimmerLavender    = Color(0xFFB388FF)
val GlowPink           = Color(0x40FF6B9D)   // soft glow ring
val GlowPurple         = Color(0x30C3A6FF)

// Premium text gradient endpoints
val GradTextStart      = Color(0xFFFF6B9D)
val GradTextEnd        = Color(0xFFC3A6FF)

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
