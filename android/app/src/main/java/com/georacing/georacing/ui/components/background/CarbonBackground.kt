package com.georacing.georacing.ui.components.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * 🏎️ Fondo racing premium con degradado multicapa,
 * líneas de velocidad diagonales y glow sutil animado.
 */
@Composable
fun CarbonBackground(
    modifier: Modifier = Modifier
) {
    // Ambient glow pulse muy sutil
    val infiniteTransition = rememberInfiniteTransition(label = "bg_glow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // ── Capa 1: Degradado base profundo con tinte azulado ──
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF06060C),   // Ultra negro azulado
                Color(0xFF0A0A12),   // Transición
                Color(0xFF0E0E18),   // Sutil azulado en bottom
                Color(0xFF080810)    // Cierre oscuro
            ),
            startY = 0f,
            endY = height
        )
        drawRect(brush = gradientBrush)
        
        // ── Capa 2: Glow radial sutil en la zona superior (efecto pit light) ──
        val glowAlpha = 0.04f + (glowPhase * 0.02f)
        val glowBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8253A).copy(alpha = glowAlpha), // Racing Red sutil
                Color.Transparent
            ),
            center = Offset(width * 0.7f, height * 0.08f),
            radius = width * 0.6f
        )
        drawRect(brush = glowBrush)
        
        // ── Capa 3: Glow cyan sutil zona inferior izquierda ──
        val cyanGlowBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = 0.02f + glowPhase * 0.01f),
                Color.Transparent
            ),
            center = Offset(width * 0.15f, height * 0.85f),
            radius = width * 0.5f
        )
        drawRect(brush = cyanGlowBrush)
        
        // ── Capa 4: Líneas de velocidad diagonales premium ──
        val lineSpacing = 50f
        val lineCount = (width / lineSpacing).toInt() + 15
        
        for (i in 0..lineCount) {
            val x = i * lineSpacing - height * 0.35f
            // Variación de opacidad por línea para efecto orgánico
            val lineAlpha = if (i % 3 == 0) 0.025f else 0.015f
            drawLine(
                color = Color.White.copy(alpha = lineAlpha),
                start = Offset(x, 0f),
                end = Offset(x + height * 0.35f, height),
                strokeWidth = if (i % 5 == 0) 1.5f else 0.8f
            )
        }
        
        // ── Capa 5: Línea de acento horizontal sutil ──
        val accentY = height * 0.35f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE8253A).copy(alpha = 0.06f),
                    Color(0xFFE8253A).copy(alpha = 0.04f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, accentY),
            end = Offset(width, accentY),
            strokeWidth = 1f
        )
    }
}
