package com.georacing.georacing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.georacing.georacing.data.event.ActiveEventConfig

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onTertiary = Color(0xFFF8FAFC),
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error,
    onError = OnError,
    outline = GlassBorder,
    outlineVariant = OutlineLight
)

// Solo soportamos tema oscuro para estética racing
private val LightColorScheme = DarkColorScheme

/**
 * 🆘 OLED Emergency Color Scheme
 * Fondo negro puro (0x000000) para máximo ahorro de batería en pantallas OLED.
 * Solo colores esenciales: rojo emergencia, blanco para texto.
 */
private val OLEDEmergencyColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000),      // Rojo puro emergencia
    secondary = Color(0xFFFF4444),    // Rojo secundario
    tertiary = Color(0xFFFFFFFF),     // Blanco
    background = Color(0xFF000000),   // NEGRO PURO OLED
    surface = Color(0xFF000000),      // NEGRO PURO OLED
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFFF0000),
    onError = Color.White
)

@Composable
fun GeoRacingTheme(
    darkTheme: Boolean = true, // Forzar tema oscuro
    dynamicColor: Boolean = false, // Desactivar para mantener brand colors
    forceOledBlack: Boolean = false, // Survival Mode
    activeEventConfig: ActiveEventConfig? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        forceOledBlack -> OLEDEmergencyColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val resolvedEventConfig = activeEventConfig ?: LocalActiveEventConfig.current
    val colorScheme = if (forceOledBlack) {
        OLEDEmergencyColorScheme
    } else {
        applyEventBranding(baseColorScheme, resolvedEventConfig)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = if (forceOledBlack) Color.Black.toArgb() else Color.Transparent.toArgb()
            val navBarColor = if (forceOledBlack) Color.Black.toArgb() else Color.Transparent.toArgb()
            
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    val visualStyle = resolvedEventConfig.toEventVisualStyle()

    CompositionLocalProvider(
        LocalEventVisualStyle provides visualStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = visualStyle.shapes,
            content = content
        )
    }
}

/**
 * 🆘 Tema OLED para modo emergencia / batería crítica.
 * 
 * - Fondo negro puro (ahorra batería en OLED)
 * - Solo colores rojo y blanco
 * - Sin animaciones ni efectos
 * 
 * Usar cuando AppPowerState == CRITICAL o en EmergencyScreen.
 */
@Composable
fun GeoRacingOLEDTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Negro puro para status bar y navigation bar
            window.statusBarColor = Color.Black.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = OLEDEmergencyColorScheme,
        typography = Typography,
        content = content
    )
}

private fun applyEventBranding(
    baseColorScheme: ColorScheme,
    eventConfig: ActiveEventConfig
): ColorScheme {
    val branding = eventConfig.branding
    return baseColorScheme.copy(
        primary = branding.primaryHex.toComposeColor(baseColorScheme.primary),
        secondary = branding.secondaryHex.toComposeColor(baseColorScheme.secondary),
        tertiary = branding.tertiaryHex.toComposeColor(baseColorScheme.tertiary),
        background = branding.backgroundHex.toComposeColor(baseColorScheme.background),
        surface = branding.surfaceHex.toComposeColor(baseColorScheme.surface),
        surfaceVariant = branding.surfaceHex.toComposeColor(baseColorScheme.surfaceVariant),
        onPrimary = branding.onPrimaryHex.toComposeColor(baseColorScheme.onPrimary),
        onBackground = branding.onBackgroundHex.toComposeColor(baseColorScheme.onBackground),
        onSurface = branding.onSurfaceHex.toComposeColor(baseColorScheme.onSurface),
        outline = branding.outlineHex.toComposeColor(baseColorScheme.outline),
        outlineVariant = branding.outlineVariantHex.toComposeColor(baseColorScheme.outlineVariant)
    )
}

private fun String?.toComposeColor(fallback: Color): Color {
    return if (this.isNullOrBlank()) {
        fallback
    } else {
        try {
            Color(android.graphics.Color.parseColor(this))
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }
}

val LocalEnergyProfile = staticCompositionLocalOf<com.georacing.georacing.domain.model.EnergyProfile> {
    com.georacing.georacing.domain.model.EnergyProfile.Performance
}

val LocalActiveEventConfig = staticCompositionLocalOf<ActiveEventConfig> {
    ActiveEventConfig.fallback()
}
