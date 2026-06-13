package com.georacing.georacing.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════
// 🏎️  GeoRacing — Premium Racing Color System
// ═══════════════════════════════════════════════════════

// Base Racing Colors
val CarbonBlack = Color(0xFF080810)          // Ultra-deep black with blue undertone
val AsphaltGrey = Color(0xFF16161E)          // Card Background — dark with personality
val MetalGrey = Color(0xFF24242E)            // Secondary elements — metallic feel
val PitLaneGrey = Color(0xFF32323E)          // Elevated surfaces
val RacingRed = Color(0xFFE8253A)            // Vibrant Racing Red (F1-inspired)
val RacingRedBright = Color(0xFFFF3352)      // Glow / Hover state
val RacingRedDark = Color(0xFFA01025)        // Pressed / Deep accent

// Neon Racing Accents
val NeonOrange = Color(0xFFFF6B2C)           // Hot lap / Performance
val NeonCyan = Color(0xFF00E5FF)             // Tech / Data / Telemetry
val NeonPurple = Color(0xFFA855F7)           // Special events
val ElectricBlue = Color(0xFF3B82F6)         // Interactive / Links
val ChampagneGold = Color(0xFFD4A855)        // Premium / VIP

// Status Colors (LED Panel style — brighter, more saturated)
val StatusGreen = Color(0xFF22C55E)          // Green flag
val StatusAmber = Color(0xFFFFA726)          // Yellow flag / Caution
val StatusRed = Color(0xFFEF4444)            // Red flag / Danger
val StatusBlue = Color(0xFF3B82F6)           // Info / Blue flag

// Text Colors (improved contrast)
val TextPrimary = Color(0xFFF8FAFC)          // Crisp white with warmth
val TextSecondary = Color(0xFFCBD5E1)        // Readable secondary
val TextTertiary = Color(0xFF64748B)         // Subtle tertiary
val TextAccent = RacingRed                    // Highlighted text

// Semantic Colors
val InfoBlue = StatusBlue
val NeutralGrey = Color(0xFF8E8E93)
val DisabledGrey = Color(0xFF3F3F46)
val OutlineLight = Color.White.copy(alpha = 0.08f)
val OutlineAccent = RacingRed.copy(alpha = 0.3f)

// Accents (Enriched for feature grid)
val AccentFood = Color(0xFFFF9F0A)
val AccentSocial = Color(0xFFA855F7)
val AccentSafety = StatusRed
val AccentInfo = ElectricBlue
val AccentEvent = Color(0xFFFF375F)
val AccentNavigation = NeonCyan
val AccentParking = Color(0xFF8B8B97)
val AccentMoments = Color(0xFFEC4899)

// Liquid Glass Specifics
val GlassSurface = AsphaltGrey.copy(alpha = 0.75f)
val GlassHighlight = Color.White.copy(alpha = 0.06f)
val GlassBorder = Color.White.copy(alpha = 0.12f)
val CircuitStop = Color(0xFFCF1828)
val CircuitGreen = Color(0xFF28CD41)
val CircuitCongestion = Color(0xFFFF9F0A)

// Gradient Brushes — Racing Edition
val RacingGradient = Brush.linearGradient(
    colors = listOf(RacingRed, RacingRedBright)
)
val CarbonGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0C0C14),
        Color(0xFF080810),
        Color(0xFF0A0A14)
    )
)
val NeonGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, ElectricBlue, NeonPurple)
)
val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1A1A24),
        Color(0xFF12121A)
    )
)

// Racing Component Aliases (used by RacingComponents.kt)
val TarmacBlack = CarbonBlack                     // OLED-black backgrounds
val AsphaltDark = AsphaltGrey                     // Card backgrounds
val AsphaltLight = PitLaneGrey                    // Borders / disabled bg
val KerbWhite = TextPrimary                       // Primary text / content
val KerbYellow = StatusAmber                      // Active / warning accent
val CatalunyaRed = RacingRed                      // Danger / primary accent
val MutedText = TextTertiary                      // Disabled text

// Semantic Aliases (Material 3 Mapping)
val Primary = RacingRed
val Secondary = MetalGrey
val Tertiary = StatusGreen
val Background = CarbonBlack
val Surface = AsphaltGrey
val SurfaceVariant = MetalGrey
val OnPrimary = Color.White
val OnSecondary = TextPrimary
val OnBackground = TextPrimary
val OnSurface = TextPrimary
val Error = StatusRed
val OnError = Color.White
