package com.georacing.georacing.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GlassConfig(
    val enabled: Boolean = true,
    val quality: GlassQuality = GlassQuality.Medium,
    val surfaceTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    
    // Aesthetic Tuning
    val blurRadius: Dp = 32.dp, // Premium heavy blur
    val saturation: Float = 1.3f, // Extra vibrancy for UI elements behind glass
    val noiseFactor: Float = 0.05f, // Subtle grain for realism
    
    // Readability
    val surfaceTintAlpha: Float = 0.5f, // Tint on top of glass
    val borderAlpha: Float = 0.25f
)

@Immutable
enum class GlassQuality {
    High,   // Lens + Blur + Vibrancy (Android 13+)
    Medium, // Blur + Vibrancy (Android 12+)
    Low     // Fallback (Transparency/Scrim only)
}


val LocalGlassConfig = androidx.compose.runtime.staticCompositionLocalOf { GlassConfig() }
val LocalGlassConfigState = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.runtime.MutableState<GlassConfig>> { error("No Glass Config State provided") }
val LocalHazeState = androidx.compose.runtime.staticCompositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }
