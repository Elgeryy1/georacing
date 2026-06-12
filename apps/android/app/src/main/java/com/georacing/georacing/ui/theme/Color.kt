package com.georacing.georacing.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════
// 🏎️  GeoRacing — Industrial Grade Color System (Phase 6)
// ═══════════════════════════════════════════════════════
// Optimizado para visibilidad bajo luz solar directa (100k lux)
// y cero consumo de batería en OLED (Overdraw = 0).

// MÓDULO 1: Brand Identity & Color System

// Colores Crudos (Raw Colors)
val TarmacBlack = Color(0xFF000000)      // Pure Black (OLED Pixels OFF)
val AsphaltDark = Color(0xFF121212)      // Base surfaces
val AsphaltMedium = Color(0xFF1E1E1E)    // Elevated surfaces or separators
val AsphaltLight = Color(0xFF2C2C2C)     // High emphasis borders/lines

val CatalunyaRed = Color(0xFFE5001C)     // Circuit official red (Primary CTAs & Danger)
val KerbYellow = Color(0xFFFFD700)       // Maximum contrast accents (Warnings)
val KerbWhite = Color(0xFFFFFFFF)        // Pure White (High Emphasis Text/Icons)

// Secondary semantic colors mapped to Industrial requirements
val TelemetryGreen = Color(0xFF00E676)   // Systems nominal / Success
val MutedText = Color(0xFFAAAAAA)        // Low emphasis text for telemetry metadata

// Transparent utilities (Use sparingly to avoid overdraw, only for ripples/states)
val TransparentOutline = KerbWhite.copy(alpha = 0.12f)
val TransparentRed = CatalunyaRed.copy(alpha = 0.15f)

// =========================================================================
// ⚠️ BACKWARD COMPATIBILITY ALIASES (Deprecated - To be refactored)
// =========================================================================
val CarbonBlack = TarmacBlack
val AsphaltGrey = AsphaltDark
val MetalGrey = AsphaltMedium
val PitLaneGrey = AsphaltLight
val RacingRed = CatalunyaRed
val RacingRedBright = CatalunyaRed
val RacingRedDark = Color(0xFFB31222)

val NeonOrange = KerbYellow
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFFB5179E)
val ElectricBlue = Color(0xFF4361EE)
val ChampagneGold = KerbYellow

val StatusGreen = TelemetryGreen
val StatusAmber = KerbYellow
val StatusRed = CatalunyaRed
val StatusBlue = ElectricBlue

val TextPrimary = KerbWhite
val TextSecondary = MutedText
val TextTertiary = Color(0xFF6B7280)
val TextAccent = CatalunyaRed

val InfoBlue = StatusBlue
val NeutralGrey = MutedText
val DisabledGrey = AsphaltLight
val OutlineLight = TransparentOutline
val OutlineAccent = NeonCyan.copy(alpha = 0.4f)

val AccentFood = NeonOrange
val AccentSocial = NeonPurple
val AccentSafety = StatusRed
val AccentInfo = ElectricBlue
val AccentEvent = RacingRedBright
val AccentNavigation = NeonCyan
val AccentParking = TextSecondary
val AccentMoments = Color(0xFFF72585)

val GlassSurface = AsphaltDark.copy(alpha = 0.9f) // Removed blur, just solid with slight alpha if absolutely forced
val GlassHighlight = TransparentOutline
val GlassBorder = TransparentOutline

val CircuitStop = StatusRed
val CircuitGreen = StatusGreen
val CircuitCongestion = StatusAmber

val Primary = CatalunyaRed
val Secondary = AsphaltLight
val Tertiary = KerbYellow
val Background = TarmacBlack
val Surface = AsphaltDark
val SurfaceVariant = AsphaltMedium
val OnPrimary = KerbWhite
val OnSecondary = KerbWhite
val OnBackground = KerbWhite
val OnSurface = KerbWhite
val Error = CatalunyaRed
val OnError = KerbWhite
