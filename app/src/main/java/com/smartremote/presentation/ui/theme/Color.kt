package com.smartremote.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Brand Colors ─────────────────────────────────────────────────────────────
// Deep electric indigo meets warm amber — premium tech feel with warmth

val Brand50   = Color(0xFFEEF2FF)
val Brand100  = Color(0xFFE0E7FF)
val Brand200  = Color(0xFFC7D2FE)
val Brand300  = Color(0xFFA5B4FC)
val Brand400  = Color(0xFF818CF8)
val Brand500  = Color(0xFF6366F1)   // Primary Indigo
val Brand600  = Color(0xFF4F46E5)
val Brand700  = Color(0xFF4338CA)
val Brand800  = Color(0xFF3730A3)
val Brand900  = Color(0xFF312E81)

val Amber400  = Color(0xFFFBBF24)   // Accent warm gold
val Amber500  = Color(0xFFF59E0B)
val Amber600  = Color(0xFFD97706)

val TealAccent = Color(0xFF14B8A6)
val RedAlert   = Color(0xFFEF4444)
val GreenOk    = Color(0xFF22C55E)

// Dark surface palette
val DarkBg      = Color(0xFF0F0F1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkCard    = Color(0xFF242442)
val DarkElevate = Color(0xFF2D2D50)

// Light surface palette
val LightBg      = Color(0xFFF8F7FF)
val LightSurface = Color(0xFFFFFFFF)
val LightCard    = Color(0xFFF1F0FF)

// ─── Color Schemes ────────────────────────────────────────────────────────────

val DarkColorScheme = darkColorScheme(
    primary          = Brand400,
    onPrimary        = Color(0xFF1A0080),
    primaryContainer = Brand800,
    onPrimaryContainer = Brand100,
    secondary        = Amber400,
    onSecondary      = Color(0xFF3D2700),
    secondaryContainer = Color(0xFF5C3C00),
    onSecondaryContainer = Color(0xFFFFDEA1),
    tertiary         = TealAccent,
    onTertiary       = Color(0xFF00201E),
    tertiaryContainer = Color(0xFF004D48),
    onTertiaryContainer = Color(0xFFB1EFEA),
    error            = RedAlert,
    errorContainer   = Color(0xFF93000A),
    background       = DarkBg,
    onBackground     = Color(0xFFE8E6FF),
    surface          = DarkSurface,
    onSurface        = Color(0xFFE8E6FF),
    surfaceVariant   = DarkCard,
    onSurfaceVariant = Color(0xFFCBCAE8),
    outline          = Color(0xFF958FBD),
    outlineVariant   = DarkElevate,
    scrim            = Color(0xFF000000),
    inverseSurface   = Color(0xFFE8E6FF),
    inverseOnSurface = DarkBg,
    inversePrimary   = Brand600,
)

val LightColorScheme = lightColorScheme(
    primary          = Brand600,
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Brand100,
    onPrimaryContainer = Brand900,
    secondary        = Amber600,
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF3D2700),
    tertiary         = Color(0xFF0D9488),
    onTertiary       = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF003D38),
    error            = Color(0xFFDC2626),
    errorContainer   = Color(0xFFFFEDED),
    background       = LightBg,
    onBackground     = Color(0xFF1A1A2E),
    surface          = LightSurface,
    onSurface        = Color(0xFF1A1A2E),
    surfaceVariant   = LightCard,
    onSurfaceVariant = Color(0xFF4A4678),
    outline          = Color(0xFF7878A0),
    outlineVariant   = Color(0xFFD0CFF0),
)

// ─── Device Category Colors ───────────────────────────────────────────────────

val DeviceColors = mapOf(
    "TV"              to Color(0xFF3B82F6),
    "AIR_CONDITIONER" to Color(0xFF06B6D4),
    "FAN"             to Color(0xFF8B5CF6),
    "SET_TOP_BOX"     to Color(0xFFF59E0B),
    "PROJECTOR"       to Color(0xFFEC4899),
    "SPEAKER"         to Color(0xFF10B981),
    "SMART_LIGHT"     to Color(0xFFEAB308),
    "STREAMING_DEVICE"to Color(0xFFEF4444),
    "SMART_PLUG"      to Color(0xFF6366F1),
    "THERMOSTAT"      to Color(0xFFF97316),
    "OTHER"           to Color(0xFF6B7280)
)
