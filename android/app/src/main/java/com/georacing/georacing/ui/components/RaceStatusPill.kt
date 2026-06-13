package com.georacing.georacing.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.ui.glass.LiquidPill
import com.georacing.georacing.ui.glass.LocalBackdrop

/**
 * Píldora flotante que muestra el estado actual de la pista.
 * 
 * Características:
 * - Diseño pill (RoundedCornerShape)
 * - Colores según flag: Verde (Green), Rojo (Red), Amarillo (Yellow)
 * - Emoji de flag + texto bold
 * - Animación de entrada desde arriba
 * - Elevation para efecto flotante
 */
@Composable
fun RaceStatusPill(
    circuitMode: CircuitMode,
    modifier: Modifier = Modifier
) {
    val pillColor = getPillColor(circuitMode)
    val flagEmoji = getFlagEmoji(circuitMode)
    val statusText = getStatusText(circuitMode)
    
    // Animación de entrada
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        modifier = modifier
    ) {
        val backdrop = LocalBackdrop.current
        
        LiquidPill(
            backdrop = backdrop,
            modifier = Modifier
                .height(42.dp)
                .widthIn(min = 200.dp, max = 300.dp),
            surfaceColor = pillColor.copy(alpha = 0.8f),
            tint = pillColor.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji de flag
                Text(
                    text = flagEmoji,
                    fontSize = 18.sp
                )
                
                // Texto del estado
                Text(
                    text = statusText,
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

/**
 * Variante compacta solo con emoji (para espacios reducidos)
 */
@Composable
fun CompactRaceStatusPill(
    circuitMode: CircuitMode,
    modifier: Modifier = Modifier
) {
    val pillColor = getPillColor(circuitMode)
    val flagEmoji = getFlagEmoji(circuitMode)
    
    val backdrop = LocalBackdrop.current
    
    LiquidPill(
        backdrop = backdrop,
        modifier = modifier.size(48.dp),
        surfaceColor = pillColor.copy(alpha = 0.8f),
        tint = pillColor.copy(alpha = 0.2f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = flagEmoji,
                fontSize = 24.sp
            )
        }
    }
}

/**
 * Retorna el color de fondo según el estado del circuito.
 */
private fun getPillColor(mode: CircuitMode): Color {
    return when (mode) {
        CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> Color(0xFF22C55E) // Racing Green
        CircuitMode.RED_FLAG -> Color(0xFFE8253A) // Racing Red
        CircuitMode.YELLOW_FLAG -> Color(0xFFFFA726) // Racing Yellow
        CircuitMode.SAFETY_CAR -> Color(0xFFF97316) // Neon Orange
        CircuitMode.VSC -> Color(0xFF06B6D4) // Neon Cyan
        CircuitMode.EVACUATION -> Color(0xFFEF4444) // Alert Red
        CircuitMode.UNKNOWN -> Color(0xFF64748B) // Slate
    }
}

/**
 * Retorna el emoji de flag correspondiente.
 */
private fun getFlagEmoji(mode: CircuitMode): String {
    return when (mode) {
        CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> "🟢"
        CircuitMode.RED_FLAG -> "🔴"
        CircuitMode.YELLOW_FLAG -> "🟡"
        CircuitMode.SAFETY_CAR -> "🚗"
        CircuitMode.VSC -> "🟣"
        CircuitMode.EVACUATION -> "🚨"
        CircuitMode.UNKNOWN -> "⚪"
    }
}

/**
 * Retorna el texto de estado.
 */
private fun getStatusText(mode: CircuitMode): String {
    return when (mode) {
        CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> "PISTA: GREEN FLAG"
        CircuitMode.RED_FLAG -> "PISTA: RED FLAG"
        CircuitMode.YELLOW_FLAG -> "PISTA: YELLOW FLAG"
        CircuitMode.SAFETY_CAR -> "SAFETY CAR"
        CircuitMode.VSC -> "VSC ACTIVO"
        CircuitMode.EVACUATION -> "EVACUACIÓN"
        CircuitMode.UNKNOWN -> "ESTADO DESCONOCIDO"
    }
}
