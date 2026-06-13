package com.georacing.georacing.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop

/**
 * Overlay estilo Waze para mostrar alertas de incidentes en la ruta.
 * 
 * Características:
 * - Burbujas grandes con colores saturados (naranja/rojo)
 * - Iconos cartoonish grandes
 * - Animación de entrada desde arriba
 * - Auto-dismiss después de 5 segundos
 * - Pulso sutil para llamar la atención
 */
@Composable
fun HazardAlertOverlay(
    hazards: List<ScenarioSimulator.RoadHazard>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Mostrar solo el hazard más cercano/importante
        hazards.firstOrNull()?.let { hazard ->
            HazardBubble(
                hazard = hazard,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            )
        }
    }
}

@Composable
private fun HazardBubble(
    hazard: ScenarioSimulator.RoadHazard,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    // Auto-show on composition
    LaunchedEffect(hazard.id) {
        visible = true
        kotlinx.coroutines.delay(5000) // Show for 5 seconds
        visible = false
    }
    
    // Pulso sutil para llamar atención
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val backdrop = LocalBackdrop.current
        
        LiquidCard(
            backdrop = backdrop,
            modifier = Modifier
                .scale(pulseScale)
                .width(280.dp)
                .height(100.dp),
            cornerRadius = 20.dp,
            surfaceColor = getHazardColor(hazard.type).copy(alpha = 0.8f),
            tint = getHazardColor(hazard.type).copy(alpha = 0.3f),
            blurRadius = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono cartoonish grande
                Icon(
                    imageVector = getHazardIcon(hazard.type),
                    contentDescription = null,
                    tint = Color(0xFFF8FAFC),
                    modifier = Modifier.size(48.dp)
                )
                
                // Texto del incidente
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = hazard.type.emoji + " " + hazard.type.label.uppercase(),
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "EN LA RUTA",
                        color = Color(0xFFF8FAFC).copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Retorna el color de fondo según el tipo de incidente.
 * Colores saturados estilo Waze.
 */
private fun getHazardColor(type: ScenarioSimulator.HazardType): Color {
    return when (type) {
        ScenarioSimulator.HazardType.ACCIDENT -> Color(0xFFEF4444) // Racing Red
        ScenarioSimulator.HazardType.TRAFFIC -> Color(0xFFF97316)   // Neon Orange
        ScenarioSimulator.HazardType.CONSTRUCTION -> Color(0xFFFFA726) // Amber
        ScenarioSimulator.HazardType.POLICE -> Color(0xFF06B6D4) // Neon Cyan
    }
}

/**
 * Retorna el icono apropiado para cada tipo de incidente.
 */
private fun getHazardIcon(type: ScenarioSimulator.HazardType): ImageVector {
    return when (type) {
        ScenarioSimulator.HazardType.ACCIDENT -> Icons.Default.Warning
        ScenarioSimulator.HazardType.TRAFFIC -> Icons.Default.Traffic
        ScenarioSimulator.HazardType.CONSTRUCTION -> Icons.Default.Construction
        ScenarioSimulator.HazardType.POLICE -> Icons.Default.Security
    }
}

/**
 * Formatea la distancia en texto legible.
 */
private fun formatDistance(meters: Double): String {
    return when {
        meters < 100 -> "${meters.toInt()}m adelante"
        meters < 1000 -> "${(meters / 100).toInt() * 100}m adelante"
        else -> "${String.format("%.1f", meters / 1000)}km adelante"
    }
}
