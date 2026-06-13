package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Velocímetro circular gamificado estilo Waze.
 * 
 * Características:
 * - Círculo de 80.dp en esquina inferior izquierda
 * - Se pone rojo y pulsa al exceder el límite de velocidad
 * - Muestra velocidad actual en grande y bold
 * - Muestra límite en gris cuando no se excede
 * - Fondo semi-transparente para no obstruir el mapa
 */
@Composable
fun GamifiedSpeedometer(
    currentSpeed: Float,
    speedLimit: Int?,
    modifier: Modifier = Modifier
) {
    val isExceedingLimit = speedLimit != null && currentSpeed > speedLimit
    
    // Animación de color
    val backgroundColor by animateColorAsState(
        targetValue = if (isExceedingLimit) {
            Color(0xFFE8253A) // Racing red vibrante
        } else {
            Color(0xFF14141C).copy(alpha = 0.85f) // Glass-like dark
        },
        animationSpec = tween(300),
        label = "background_color"
    )
    
    // Animación del borde
    val borderColor by animateColorAsState(
        targetValue = if (isExceedingLimit) {
            Color(0xFFFF3352).copy(alpha = 0.6f)
        } else {
            Color.White.copy(alpha = 0.1f)
        },
        animationSpec = tween(300),
        label = "border_color"
    )
    
    // Pulso cuando excede el límite
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isExceedingLimit) 1f else 1f,
        targetValue = if (isExceedingLimit) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier
            .size(80.dp)
            .scale(if (isExceedingLimit) pulseScale else 1f)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Velocidad actual (número grande)
            Text(
                text = currentSpeed.toInt().toString(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 28.sp,
                letterSpacing = (-1).sp
            )
            
            // "km/h" pequeño
            Text(
                text = "km/h",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            
            // Mostrar límite si existe
            speedLimit?.let { limit ->
                Text(
                    text = if (isExceedingLimit) "⚠ $limit" else "$limit",
                    color = if (isExceedingLimit) {
                        Color.White
                    } else {
                        Color.LightGray.copy(alpha = 0.7f)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Versión alternativa con borde circular para mayor visibilidad.
 */
@Composable
fun GamifiedSpeedometerWithBorder(
    currentSpeed: Float,
    speedLimit: Int?,
    modifier: Modifier = Modifier
) {
    val isExceedingLimit = speedLimit != null && currentSpeed > speedLimit
    
    val borderColor by animateColorAsState(
        targetValue = if (isExceedingLimit) {
            Color(0xFFFFFFFF) // Borde blanco cuando excede
        } else {
            Color.Transparent
        },
        animationSpec = tween(300),
        label = "border_color"
    )
    
    Box(
        modifier = modifier
            .size(84.dp)
            .background(borderColor, CircleShape)
            .padding(2.dp)
    ) {
        GamifiedSpeedometer(
            currentSpeed = currentSpeed,
            speedLimit = speedLimit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
