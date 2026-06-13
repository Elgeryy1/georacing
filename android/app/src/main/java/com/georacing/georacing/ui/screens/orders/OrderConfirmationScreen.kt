package com.georacing.georacing.ui.screens.orders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun OrderConfirmationScreen(
    navController: NavController,
    orderId: String
) {
    // Generate a fake pickup number for demo
    val pickupNumber = remember { (100..999).random() }
    val stallNumber = remember { (1..12).random() }
    
    // Confetti/Animation effect state if we wanted to be fancy
    
    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Ticket Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Accent dot + title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ORDER CONFIRMED",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF22C55E)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "READY FOR PICKUP IN 10 MIN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // QR Code Simulation (Procedural Drawing)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(4.dp, Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        FakeQrCanvas()
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "ORDER #$orderId",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFF64748B).copy(alpha = 0.3f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text(
                                 "PICKUP AT",
                                 style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                                 color = Color(0xFF64748B),
                                 fontWeight = FontWeight.Bold
                             )
                             Text(
                                 "STALL $stallNumber",
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.ExtraBold,
                                 color = Color(0xFFF8FAFC)
                             )
                         }
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text(
                                 "YOUR NUMBER",
                                 style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                                 color = Color(0xFF64748B),
                                 fontWeight = FontWeight.Bold
                             )
                             Text(
                                 "#$pickupNumber",
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Black,
                                 color = Color(0xFFE8253A)
                             )
                         }
                    }
                }
            }
            
            Button(
                onClick = { 
                    // Clear stack back to Home or Orders
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                        popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8253A)),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "VOLVER AL PADDOCK",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFF8FAFC)
                )
            }
        }
    }
}

@Composable
fun FakeQrCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = size.width / 25
        val matrixSize = 25
        
        val qrDark = Color(0xFF080810)
        val qrLight = Color(0xFFF8FAFC)
        
        // Draw Finder Patterns (Corners)
        drawRect(qrDark, topLeft = Offset(0f, 0f), size = Size(cellSize * 7, cellSize * 7))
        drawRect(qrLight, topLeft = Offset(cellSize, cellSize), size = Size(cellSize * 5, cellSize * 5))
        drawRect(qrDark, topLeft = Offset(cellSize * 2, cellSize * 2), size = Size(cellSize * 3, cellSize * 3))

        drawRect(qrDark, topLeft = Offset(size.width - cellSize * 7, 0f), size = Size(cellSize * 7, cellSize * 7))
        drawRect(qrLight, topLeft = Offset(size.width - cellSize * 6, cellSize), size = Size(cellSize * 5, cellSize * 5))
        drawRect(qrDark, topLeft = Offset(size.width - cellSize * 5, cellSize * 2), size = Size(cellSize * 3, cellSize * 3))

        drawRect(qrDark, topLeft = Offset(0f, size.height - cellSize * 7), size = Size(cellSize * 7, cellSize * 7))
        drawRect(qrLight, topLeft = Offset(cellSize, size.height - cellSize * 6), size = Size(cellSize * 5, cellSize * 5))
        drawRect(qrDark, topLeft = Offset(cellSize * 2, size.height - cellSize * 5), size = Size(cellSize * 3, cellSize * 3))

        // Random Data Noise
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                // Avoid Finder Patterns areas
                val inTopLeft = i < 8 && j < 8
                val inTopRight = i > 16 && j < 8
                val inBottomLeft = i < 8 && j > 16
                
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    if ((i + j * 3).hashCode() % 2 == 0) {
                        drawRect(
                            color = qrDark,
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}
