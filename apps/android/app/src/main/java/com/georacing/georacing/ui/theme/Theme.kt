package com.georacing.georacing.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════
// 🏎️  GeoRacing — Racing Theme (Phase 6)
// ═══════════════════════════════════════════════════════
// Forzamos un tema oscuro (darkColorScheme) por diseño para 
// la visibilidad y ahorro extremo en pantallas OLED.

private val RacingColorScheme = darkColorScheme(
    primary = CatalunyaRed,
    onPrimary = KerbWhite,
    secondary = AsphaltLight,
    onSecondary = KerbWhite,
    tertiary = KerbYellow,
    onTertiary = TarmacBlack,
    background = TarmacBlack,     // Black background
    onBackground = KerbWhite,
    surface = AsphaltDark,        // Cards / dialogs surface
    onSurface = KerbWhite,
    surfaceVariant = AsphaltMedium,
    onSurfaceVariant = MutedText,
    error = CatalunyaRed,
    onError = KerbWhite,
    outline = AsphaltLight
)

// Sharp logic - Zero soft borders for technical aesthetic
val RacingShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun GeoRacingTheme(
    forceOledBlack: Boolean = false, // Modo SOS / Supervivencia extrema
    content: @Composable () -> Unit
) {
    // Si estamos en modo crítico, forzamos TODO a negro puro, ignorando las Cards de fondo Asphalt
    val finalColorScheme = if (forceOledBlack) {
        RacingColorScheme.copy(
            surface = TarmacBlack,
            surfaceVariant = TarmacBlack,
            background = TarmacBlack
        )
    } else {
        RacingColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Forzar status bar y navigation bar a negro OLED
            window.statusBarColor = Color.Black.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        shapes = RacingShapes,
        content = content
    )
}

/**
 * 🆘 Tema OLED forzado (Alias para compatibilidad con el ecosistema de supervivencia antiguo)
 */
@Composable
fun GeoRacingOLEDTheme(content: @Composable () -> Unit) {
    GeoRacingTheme(forceOledBlack = true, content = content)
}

// CompositionLocal fallbacks (mantenido por compatibilidad con Phase 1)
val LocalEnergyProfile = androidx.compose.runtime.staticCompositionLocalOf<com.georacing.georacing.domain.model.EnergyProfile> {
    com.georacing.georacing.domain.model.EnergyProfile.Performance
}
