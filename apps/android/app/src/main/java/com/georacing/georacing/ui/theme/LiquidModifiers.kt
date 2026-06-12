package com.georacing.georacing.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassLevel {
    L1, // Chips, Filters, Small items (Higher Opacity, subtle edge)
    L2, // Cards, Large Containers (Translucent, ambient glow)
    L3  // Hero / Premium elements (Deep translucency, accent edge)
}

/**
 * 🏎️ Aplica el efecto "Liquid Glass" Premium al componente.
 * 
 * Capas del efecto:
 * 1. Background translúcido con tinte azulado (profundidad)
 * 2. Inner highlight gradient (luminosidad superior)
 * 3. Borde con gradiente (specular edge)
 *
 * @param level Nivel de intensidad del glass.
 * @param showBorder Si debe mostrar el borde luminoso specular.
 * @param borderColor Color personalizado para el borde.
 * @param accentGlow Color de glow sutil en la parte superior.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    level: GlassLevel = GlassLevel.L2,
    showBorder: Boolean = true,
    borderColor: Color? = null,
    accentGlow: Color? = null
) = composed {
    val baseAlpha = when(level) {
        GlassLevel.L1 -> 0.82f
        GlassLevel.L2 -> 0.68f
        GlassLevel.L3 -> 0.55f
    }
    
    val borderWidth = when(level) {
        GlassLevel.L1 -> 0.5.dp
        GlassLevel.L2 -> 0.75.dp
        GlassLevel.L3 -> 1.dp
    }
    
    val glowAlpha = when(level) {
        GlassLevel.L1 -> 0.03f
        GlassLevel.L2 -> 0.05f
        GlassLevel.L3 -> 0.08f
    }
    
    // Surface color con tinte azulado para profundidad
    val surfaceColor = AsphaltGrey.copy(alpha = baseAlpha)
    val highlightColor = accentGlow ?: NeonCyan
    
    this
        .background(
            color = surfaceColor,
            shape = shape
        )
        // Inner highlight — luz sutil en la parte superior
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.4f
                )
            )
        }
        .then(
            if (showBorder) {
                Modifier.border(
                    width = borderWidth,
                    brush = if (borderColor != null) {
                        Brush.verticalGradient(
                            colors = listOf(
                                borderColor.copy(alpha = 0.5f),
                                borderColor.copy(alpha = 0.08f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    },
                    shape = shape
                )
            } else Modifier
        )
        .clip(shape)
}

/**
 * Variante más ligera para elementos pequeños (Pills, botones, chips)
 * Con borde specular sutil.
 */
fun Modifier.glassSmall(
    shape: Shape = RoundedCornerShape(50),
    color: Color = AsphaltGrey
) = composed {
    this
        .background(
            color = color.copy(alpha = 0.55f),
            shape = shape
        )
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            ),
            shape = shape
        )
        .clip(shape)
}

/**
 * 🏎️ Efecto de acento deportivo: borde lateral con color racing.
 * Ideal para cards activas o seleccionadas.
 */
fun Modifier.racingAccent(
    color: Color = RacingRed,
    shape: Shape = RoundedCornerShape(16.dp)
) = composed {
    this
        .drawWithContent {
            drawContent()
            // Línea de acento lateral izquierda
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.2f)
                    )
                ),
                start = Offset(0f, size.height * 0.15f),
                end = Offset(0f, size.height * 0.85f),
                strokeWidth = 3.dp.toPx()
            )
        }
}
