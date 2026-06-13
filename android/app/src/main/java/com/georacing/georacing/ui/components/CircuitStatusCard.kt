package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.LocalEventVisualStyle
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CircuitStatusCard(
    mode: CircuitMode,
    message: String?,
    temperature: String,
    modifier: Modifier = Modifier
) {
    val visuals = LocalEventVisualStyle.current
    val (statusColor, titleText, iconVector) = when (mode) {
        CircuitMode.NORMAL -> Triple(Color(0xFF22C55E), "PISTA LIBRE", Icons.Filled.Flag)
        CircuitMode.GREEN_FLAG -> Triple(Color(0xFF22C55E), "BANDERA VERDE", Icons.Filled.Flag)
        CircuitMode.YELLOW_FLAG -> Triple(Color(0xFFFFA726), "PRECAUCIÓN", Icons.Filled.Warning)
        CircuitMode.VSC -> Triple(Color(0xFFFFA726), "VIRTUAL SAFETY CAR", Icons.Filled.Warning)
        CircuitMode.SAFETY_CAR -> Triple(Color(0xFFFFA726), "SAFETY CAR", Icons.Filled.DirectionsCar)
        CircuitMode.RED_FLAG -> Triple(Color(0xFFEF4444), "BANDERA ROJA", Icons.Filled.Flag)
        CircuitMode.EVACUATION -> Triple(Color(0xFFEF4444), "EVACUACIÓN", Icons.Filled.Warning)
        CircuitMode.UNKNOWN -> Triple(Color(0xFF64748B), "SIN CONEXIÓN", Icons.Filled.WifiOff)
    }

    val subtitle = when (mode) {
        CircuitMode.NORMAL -> "Circulación normal en todos los sectores"
        CircuitMode.GREEN_FLAG -> "Circulación reanudada"
        CircuitMode.YELLOW_FLAG -> "Reduce la velocidad — incidente en pista"
        CircuitMode.VSC -> "Mantén la velocidad delta"
        CircuitMode.SAFETY_CAR -> "Sigue al Safety Car — no adelantar"
        CircuitMode.RED_FLAG -> "Detente en un lugar seguro"
        CircuitMode.EVACUATION -> "Dirígete a la salida más cercana"
        CircuitMode.UNKNOWN -> "Esperando datos del circuito…"
    }

    // Pulse animation for danger states
    val isDanger = mode == CircuitMode.RED_FLAG || mode == CircuitMode.EVACUATION
    val isWarning = mode == CircuitMode.YELLOW_FLAG || mode == CircuitMode.VSC || mode == CircuitMode.SAFETY_CAR

    val pulseAlpha by if (isDanger) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.08f) }
    }

    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    val backdrop = LocalBackdrop.current

    LiquidCard(
        backdrop = backdrop,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        cornerRadius = 24.dp,
        surfaceColor = visuals.panelSurfaceStrong,
        tint = statusColor.copy(alpha = pulseAlpha)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Kerb stripes at bottom (proper diagonal pattern)
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val stripeWidth = 16.dp.toPx()
                val skew = size.height
                var x = -stripeWidth
                var colorIndex = 0
                val stripeColor = if (isDanger) Color(0xFFEF4444)
                    else if (isWarning) statusColor
                    else statusColor.copy(alpha = 0.6f)

                while (x < size.width + stripeWidth) {
                    if (colorIndex % 2 == 0) {
                        val path = Path().apply {
                            moveTo(x, size.height)
                            lineTo(x + skew, 0f)
                            lineTo(x + stripeWidth + skew, 0f)
                            lineTo(x + stripeWidth, size.height)
                            close()
                        }
                        drawPath(path, color = stripeColor)
                    }
                    x += stripeWidth
                    colorIndex++
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
            ) {
                // Top Row: Status label + live indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EN VIVO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            ),
                            color = statusColor
                        )
                    }

                    // Time
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = visuals.panelMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Row: Icon + Status Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Status Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.5.dp, statusColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = visuals.heroHeadlineColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message ?: subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = visuals.heroBodyColor,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom: Sector dots (visual indicator)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECTORES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = visuals.panelMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    repeat(3) { sector ->
                        val sectorColor = when {
                            isDanger -> Color(0xFFEF4444)
                            isWarning && sector == 1 -> statusColor
                            else -> visuals.navSelected
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(sectorColor.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
    }
}
