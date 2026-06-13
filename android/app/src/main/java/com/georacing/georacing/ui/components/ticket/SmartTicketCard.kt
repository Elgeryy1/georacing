package com.georacing.georacing.ui.components.ticket

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.debug.ScenarioSimulator

@Composable
fun SmartTicketCard(
    modifier: Modifier = Modifier
) {
    val isAtGate by ScenarioSimulator.isAtGate.collectAsState()
    
    // Animation for expansion
    val transition = updateTransition(targetState = isAtGate, label = "TicketExpansion")
    
    val cardHeight by transition.animateDp(label = "Height") { state ->
        if (state) 500.dp else 100.dp 
    }
    
    // Ripple Effect Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
    )

    // Brightness/Glow effect state
    val brightnessAlpha = if (isAtGate) 0.3f else 0f

    // Haptic Feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(isAtGate) {
        if (isAtGate) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val backdrop = LocalBackdrop.current

    LiquidCard(
        backdrop = backdrop,
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .animateContentSize()
            .graphicsLayer {
                // Subtle scale up when active
                scaleX = if (isAtGate) 1.02f else 1f
                scaleY = if (isAtGate) 1.02f else 1f
            },
        cornerRadius = 20.dp,
        blurRadius = if (isAtGate) 16.dp else 12.dp,
        surfaceColor = Color(0xFF14141C).copy(alpha = if (isAtGate) 0.85f else 0.6f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Ripple Background (Only when At Gate)
            if (isAtGate) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp)
                        .scale(rippleScale)
                        .alpha(rippleAlpha)
                        .background(Color(0xFFE8253A), shape = RoundedCornerShape(100)) // Racing Red Ripple
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = if (isAtGate) Arrangement.SpaceEvenly else Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACCESS PASS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Gate 3 - Tribuna Principal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF8FAFC)
                        )
                    }
                    if (!isAtGate) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR",
                            tint = Color(0xFFF8FAFC),
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                         // Close Button
                         IconButton(onClick = { ScenarioSimulator.resetGate() }) {
                             Icon(
                                 imageVector = Icons.Default.Close,
                                 contentDescription = "Close Ticket",
                                 tint = Color(0xFFF8FAFC)
                             )
                         }
                    }
                }

                // Expanded Content (QR Code)
                if (isAtGate) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // "Official" QR Placeholder
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(4.dp, Brush.linearGradient(listOf(Color(0xFFE8253A), Color(0xFF06B6D4))), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Simulated QR Pattern
                        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val cellSize = size.width / 10
                            for (i in 0 until 10) {
                                for (j in 0 until 10) {
                                    if ((i + j) % 2 == 0 || (i * j) % 3 == 0) {
                                        drawRect(
                                            color = Color(0xFF080810),
             
                                            topLeft = Offset(i * cellSize, j * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                        )
                                    }
                                }
                            }
                            // Corner markers
                            drawRect(Color(0xFF080810), Offset(0f, 0f), androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
                            drawRect(Color(0xFFF8FAFC), Offset(cellSize * 0.5f, cellSize * 0.5f), androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                            drawRect(Color(0xFF080810), Offset(cellSize * 1f, cellSize * 1f), androidx.compose.ui.geometry.Size(cellSize * 1, cellSize * 1))
                            
                            drawRect(Color(0xFF080810), Offset(size.width - cellSize * 3, 0f), androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
                             drawRect(Color(0xFFF8FAFC), Offset(size.width - cellSize * 2.5f, cellSize * 0.5f), androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                             drawRect(Color(0xFF080810), Offset(size.width - cellSize * 2f, cellSize * 1f), androidx.compose.ui.geometry.Size(cellSize * 1, cellSize * 1))
                             
                             drawRect(Color(0xFF080810), Offset(0f, size.height - cellSize * 3), androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
                             drawRect(Color(0xFFF8FAFC), Offset(cellSize * 0.5f, size.height - cellSize * 2.5f), androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                             drawRect(Color(0xFF080810), Offset(cellSize * 1f, size.height - cellSize * 2f), androidx.compose.ui.geometry.Size(cellSize * 1, cellSize * 1))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SCANNING...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFF06B6D4),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(if (rippleAlpha > 0.5f) 1f else 0.5f)
                    )
                }
            }
            
            // Brightness Overlay
            if (isAtGate) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.1f + (rippleAlpha * 0.1f))) // Pulsing brightness
                )
            }
        }
    }
}
