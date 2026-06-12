package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop

@Composable
fun CircuitStatusCard(
    mode: CircuitMode,
    message: String?,
    temperature: String, // unused in card, moved to header
    modifier: Modifier = Modifier
) {
    // Colors & Text based on state
    val (statusColor, titleText, iconVector) = when (mode) {
        CircuitMode.NORMAL -> Triple(Color(0xFF22C55E), "PISTA LIBRE", Icons.Filled.Flag)
        CircuitMode.GREEN_FLAG -> Triple(Color(0xFF22C55E), "BANDERA VERDE", Icons.Filled.Flag)
        CircuitMode.YELLOW_FLAG -> Triple(Color(0xFFFFA726), "BANDERA AMARILLA", Icons.Filled.Flag)
        CircuitMode.VSC -> Triple(Color(0xFFFFA726), "VSC", Icons.Filled.Flag)
        CircuitMode.SAFETY_CAR -> Triple(Color(0xFFFFA726), "SAFETY CAR", Icons.Filled.Flag)
        CircuitMode.RED_FLAG -> Triple(Color(0xFFEF4444), "BANDERA ROJA", Icons.Filled.Flag)
        CircuitMode.EVACUATION -> Triple(Color(0xFFEF4444), "EVACUACIÓN", Icons.Filled.Flag)
        CircuitMode.UNKNOWN -> Triple(Color(0xFF64748B), "DESCONECTADO", Icons.Filled.Flag)
    }

    val backdrop = LocalBackdrop.current

    LiquidCard(
        backdrop = backdrop,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        cornerRadius = 28.dp,
        surfaceColor = Color(0xFF0E0E18).copy(alpha = 0.85f),
        tint = statusColor.copy(alpha = 0.12f)
    ) {
        // CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 0.dp)
        ) {
            
            // Top Row: Icon + Digital Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Circle Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.2f),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Digital Info Block
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "/ 2:9 / 006P", // Mock data mimicking image
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                            text = "PM",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(
                            text = "MAP - SENI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Status Text
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dashed Status Bar
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                val dashWidth = 6.dp.toPx()
                val dashHeight = size.height
                val spacing = 4.dp.toPx()
                val skewOffset = 4.dp.toPx() // How much to slant

                var currentX = 0f
                while (currentX < size.width) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                         // Skewed Rectangle (Parallelogram)
                         // Top-Left -> Top-Right -> Bottom-Right -> Bottom-Left
                        moveTo(currentX + skewOffset, 0f)
                        lineTo(currentX + dashWidth + skewOffset, 0f)
                        lineTo(currentX + dashWidth, dashHeight)
                        lineTo(currentX, dashHeight)
                        close()
                    }
                    drawPath(path, color = statusColor.copy(alpha = 0.8f))
                    currentX += dashWidth + spacing
                }
            }
        }
        
        // FOOTER STRIPES: Red/White Hazard Pattern at very bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(16.dp) // Stripe height
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB00020), Color(0xFFB00020), // Red
                            Color.White, Color.White // White
                        ),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(40f, 40f), // Angle of stripes
                        tileMode = androidx.compose.ui.graphics.TileMode.Repeated
                    )
                )
        )
    }
}
