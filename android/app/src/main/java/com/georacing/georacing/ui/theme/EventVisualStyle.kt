package com.georacing.georacing.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.georacing.georacing.data.event.ActiveEventConfig
import com.georacing.georacing.data.event.EventThemeVariant
import com.georacing.georacing.data.event.themeVariantEnum

data class EventVisualStyle(
    val variant: EventThemeVariant,
    val appBackgroundStops: List<Color>,
    val ambientPrimary: Color,
    val ambientSecondary: Color,
    val heroStops: List<Color>,
    val heroChipBackground: Color,
    val heroChipText: Color,
    val heroHeadlineColor: Color,
    val heroBodyColor: Color,
    val panelSurface: Color,
    val panelSurfaceStrong: Color,
    val panelOutline: Color,
    val panelMuted: Color,
    val navSelected: Color,
    val navIdle: Color,
    val featureShell: Color,
    val featureLabel: Color,
    val featurePalette: List<Color>,
    val searchSurface: Color,
    val searchHint: Color,
    val highlightRing: Color,
    val loaderTrack: Color,
    val loaderStops: List<Color>,
    val loginButtonSurface: Color,
    val alertSurface: Color,
    val shapes: Shapes
)

val LocalEventVisualStyle = staticCompositionLocalOf {
    ActiveEventConfig.fallback().toEventVisualStyle()
}

fun ActiveEventConfig.toEventVisualStyle(): EventVisualStyle {
    val variant = themeVariantEnum()
    val primary = branding.primaryHex.toComposeColor(RacingRed)
    val secondary = branding.secondaryHex.toComposeColor(AsphaltGrey)
    val accent = branding.tertiaryHex.toComposeColor(StatusGreen)
    val background = branding.backgroundHex.toComposeColor(CarbonBlack)
    val surface = branding.surfaceHex.toComposeColor(AsphaltGrey)
    val onBackground = branding.onBackgroundHex.toComposeColor(TextPrimary)
    val onSurface = branding.onSurfaceHex.toComposeColor(TextPrimary)
    val outline = branding.outlineHex.toComposeColor(GlassBorder)

    val shells = when (variant) {
        EventThemeVariant.SUNSET -> listOf(primary, accent, primary.mix(Color(0xFFFFB347), 0.45f), accent.mix(Color.White, 0.2f))
        EventThemeVariant.NIGHT -> listOf(primary, accent, Color(0xFF38BDF8), Color(0xFF94A3B8))
        EventThemeVariant.ELECTRIC -> listOf(primary, accent, Color(0xFFEC4899), Color(0xFF22D3EE))
        EventThemeVariant.CLASSIC -> listOf(primary, accent, Color(0xFFF97316), Color(0xFF38BDF8))
        EventThemeVariant.RACING_RED -> listOf(primary, accent, Color(0xFFFF6B2C), Color(0xFF3B82F6))
    }

    val appStops = when (variant) {
        EventThemeVariant.SUNSET -> listOf(
            background.mix(primary, 0.26f),
            background.mix(Color(0xFF451A03), 0.22f),
            background.mix(surface, 0.12f)
        )
        EventThemeVariant.NIGHT -> listOf(
            background.mix(secondary, 0.25f),
            background.mix(Color(0xFF020617), 0.3f),
            background.mix(surface, 0.08f)
        )
        EventThemeVariant.ELECTRIC -> listOf(
            background.mix(primary, 0.22f),
            background.mix(accent, 0.18f),
            background.mix(Color(0xFF0F0B1F), 0.32f)
        )
        EventThemeVariant.CLASSIC -> listOf(
            background.mix(primary, 0.16f),
            background.mix(Color(0xFF0F172A), 0.2f),
            background.mix(surface, 0.1f)
        )
        EventThemeVariant.RACING_RED -> listOf(
            background.mix(primary, 0.18f),
            background.mix(Color(0xFF111827), 0.14f),
            background.mix(surface, 0.08f)
        )
    }

    val heroStops = when (variant) {
        EventThemeVariant.SUNSET -> listOf(surface.mix(primary, 0.5f), background.mix(primary, 0.22f))
        EventThemeVariant.NIGHT -> listOf(surface.mix(Color(0xFF0F172A), 0.35f), background.mix(accent, 0.14f))
        EventThemeVariant.ELECTRIC -> listOf(surface.mix(primary, 0.4f), background.mix(accent, 0.26f))
        EventThemeVariant.CLASSIC -> listOf(surface.mix(primary, 0.24f), background.mix(accent, 0.2f))
        EventThemeVariant.RACING_RED -> listOf(surface.mix(primary, 0.32f), background.mix(primary, 0.16f))
    }

    val shapes = when (variant) {
        EventThemeVariant.SUNSET -> Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp)
        )
        EventThemeVariant.NIGHT -> Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp)
        )
        EventThemeVariant.ELECTRIC -> Shapes(
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(36.dp)
        )
        EventThemeVariant.CLASSIC -> Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp)
        )
        EventThemeVariant.RACING_RED -> Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(28.dp)
        )
    }

    return EventVisualStyle(
        variant = variant,
        appBackgroundStops = appStops,
        ambientPrimary = primary.copy(alpha = when (variant) {
            EventThemeVariant.NIGHT -> 0.10f
            EventThemeVariant.CLASSIC -> 0.12f
            else -> 0.16f
        }),
        ambientSecondary = accent.copy(alpha = when (variant) {
            EventThemeVariant.RACING_RED -> 0.08f
            EventThemeVariant.NIGHT -> 0.07f
            else -> 0.12f
        }),
        heroStops = heroStops,
        heroChipBackground = primary.copy(alpha = when (variant) {
            EventThemeVariant.NIGHT -> 0.12f
            else -> 0.18f
        }),
        heroChipText = when (variant) {
            EventThemeVariant.NIGHT -> accent
            else -> primary.mix(Color.White, 0.08f)
        },
        heroHeadlineColor = onBackground,
        heroBodyColor = onSurface.copy(alpha = 0.82f),
        panelSurface = surface.copy(alpha = 0.72f),
        panelSurfaceStrong = surface.mix(primary, when (variant) {
            EventThemeVariant.ELECTRIC -> 0.22f
            EventThemeVariant.SUNSET -> 0.16f
            EventThemeVariant.NIGHT -> 0.06f
            else -> 0.1f
        }).copy(alpha = 0.92f),
        panelOutline = outline.copy(alpha = 0.8f),
        panelMuted = onBackground.copy(alpha = 0.48f),
        navSelected = when (variant) {
            EventThemeVariant.NIGHT -> accent
            EventThemeVariant.CLASSIC -> primary
            else -> primary
        },
        navIdle = onBackground.copy(alpha = when (variant) {
            EventThemeVariant.NIGHT -> 0.58f
            else -> 0.42f
        }),
        featureShell = surface.mix(primary, when (variant) {
            EventThemeVariant.ELECTRIC -> 0.18f
            EventThemeVariant.SUNSET -> 0.12f
            else -> 0.08f
        }).copy(alpha = 0.78f),
        featureLabel = onSurface.copy(alpha = 0.86f),
        featurePalette = shells,
        searchSurface = surface.copy(alpha = 0.62f),
        searchHint = onBackground.copy(alpha = 0.42f),
        highlightRing = primary.copy(alpha = 0.32f),
        loaderTrack = onBackground.copy(alpha = 0.12f),
        loaderStops = listOf(primary, accent.mix(primary, 0.4f), primary.mix(Color.White, 0.12f)),
        loginButtonSurface = when (variant) {
            EventThemeVariant.NIGHT -> surface.mix(Color.White, 0.14f)
            else -> Color.White.copy(alpha = 0.92f)
        },
        alertSurface = primary.copy(alpha = 0.12f),
        shapes = shapes
    )
}

private fun Color.mix(other: Color, ratio: Float): Color = lerp(this, other, ratio.coerceIn(0f, 1f))

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
