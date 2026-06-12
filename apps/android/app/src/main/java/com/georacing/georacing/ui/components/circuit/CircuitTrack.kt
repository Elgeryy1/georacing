package com.georacing.georacing.ui.components.circuit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Simplified Circuit de Barcelona-Catalunya track layout
 */
@Composable
fun CircuitTrack(
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE8253A),
    congestionZones: List<Int> = emptyList() // Zone indices with congestion
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        // Simplified track path (Barcelona Catalunya layout inspired)
        val trackPath = Path().apply {
            // Start/Finish straight
            moveTo(centerX - width * 0.3f, centerY + height * 0.25f)
            lineTo(centerX + width * 0.1f, centerY + height * 0.25f)
            
            // Turn 1 (Elf)
            cubicTo(
                centerX + width * 0.15f, centerY + height * 0.25f,
                centerX + width * 0.2f, centerY + height * 0.15f,
                centerX + width * 0.2f, centerY + height * 0.05f
            )
            
            // Turn 2 (Renault)
            cubicTo(
                centerX + width * 0.2f, centerY - height * 0.05f,
                centerX + width * 0.15f, centerY - height * 0.15f,
                centerX + width * 0.05f, centerY - height * 0.2f
            )
            
            // Turn 3 (Repsol)
            cubicTo(
                centerX - width * 0.05f, centerY - height * 0.25f,
                centerX - width * 0.15f, centerY - height * 0.25f,
                centerX - width * 0.25f, centerY - height * 0.2f
            )
            
            // Turns 4-5 (Seat-Wurth)
            cubicTo(
                centerX - width * 0.3f, centerY - height * 0.15f,
                centerX - width * 0.32f, centerY - height * 0.05f,
                centerX - width * 0.3f, centerY
            )
            
            // Turn 7 (Campsa)
            cubicTo(
                centerX - width * 0.28f, centerY + height * 0.05f,
                centerX - width * 0.25f, centerY + height * 0.1f,
                centerX - width * 0.2f, centerY + height * 0.12f
            )
            
            // Turns 9-10 (La Caixa-Banc Sabadell)
            lineTo(centerX - width * 0.1f, centerY + height * 0.12f)
            cubicTo(
                centerX - width * 0.05f, centerY + height * 0.12f,
                centerX, centerY + height * 0.15f,
                centerX, centerY + height * 0.2f
            )
            
            // Turn 12-13 (New chicane)
            cubicTo(
                centerX, centerY + height * 0.23f,
                centerX - width * 0.05f, centerY + height * 0.25f,
                centerX - width * 0.1f, centerY + height * 0.25f
            )
            
            // Back to start
            lineTo(centerX - width * 0.3f, centerY + height * 0.25f)
            close()
        }
        
        // Draw track outline
        drawPath(
            path = trackPath,
            color = Color(0xFF64748B),
            style = Stroke(
                width = 28f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw track surface
        drawPath(
            path = trackPath,
            color = Color(0xFF14141C),
            style = Stroke(
                width = 24f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw center line
        drawPath(
            path = trackPath,
            color = Color(0xFFF8FAFC).copy(alpha = 0.25f),
            style = Stroke(
                width = 2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw start/finish line
        val startX = centerX - width * 0.3f
        val startY = centerY + height * 0.25f
        for (i in 0 until 8) {
            val segmentHeight = 20f
            val y = startY - segmentHeight * i
            val isWhite = i % 2 == 0
            drawLine(
                color = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF080810),
                start = Offset(startX - 14f, y),
                end = Offset(startX - 14f, y - segmentHeight),
                strokeWidth = 4f,
                cap = StrokeCap.Butt
            )
        }
        
        // Draw track name markers
        drawCircle(
            color = trackColor,
            radius = 8f,
            center = Offset(centerX - width * 0.3f, centerY + height * 0.25f)
        )
        
        drawCircle(
            color = Color(0xFFF97316),
            radius = 6f,
            center = Offset(centerX + width * 0.15f, centerY + height * 0.25f)
        )
        
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 6f,
            center = Offset(centerX + width * 0.2f, centerY)
        )
    }
}
