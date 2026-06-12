package com.georacing.georacing.ui.screens.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.components.*

@Composable
fun EmergencyScreen(navController: NavController) {
    val context = LocalContext.current
    var flashlightOn by remember { mutableStateOf(false) }

    // Pulse Animation for the SOS Button
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))
                )
            )
    ) {
        // Red Glow Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B0A0A),
                            Color(0xFF1A0505),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFE2E8F0))
                }
                Text(
                    text = "SOS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444),
                    letterSpacing = 4.sp
                )
                // Spacer to balance the Close button
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Main 112 Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Outer Ripple
                Box(
                    modifier = Modifier
                        .size(200.dp * pulseScale)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                )
                // Middle Ripple
                Box(
                    modifier = Modifier
                        .size(170.dp * pulseScale)
                        .background(Color(0xFFDC2626).copy(alpha = 0.35f), CircleShape)
                )
                
                // Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:112")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call 112",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "112",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF8FAFC),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "LLAMADA DE EMERGENCIA",
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 3.sp
            )
            Text(
                text = "Presiona para conectar con el 112",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Grid of Tools
            Text(
                text = "HERRAMIENTAS RÁPIDAS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Security
                SosToolCard(
                    title = "Seguridad",
                    icon = Icons.Default.Security,
                    color = Color(0xFF06B6D4), // NeonCyan
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:+34934444444") // Dummy Internal Security Number
                    }
                    context.startActivity(intent)
                }
                
                // Share Location
                SosToolCard(
                    title = "Ubicación",
                    icon = Icons.Default.Share,
                    color = Color(0xFF22C55E), // Green
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "¡Ayuda! Emergencia en el Circuito. Mis coords: 41.5693, 2.2576")
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir Ubicación"))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Medical ID
                SosToolCard(
                    title = "Ficha Médica",
                    icon = Icons.Default.MedicalServices, // Or LocalHospital
                    color = Color(0xFFEF4444), // Red
                    modifier = Modifier.weight(1f)
                ) {
                   // 🆘 Navegar a Lock Screen Médico
                   navController.navigate(com.georacing.georacing.ui.navigation.Screen.MedicalLockScreen.route)
                }
                
                // Flashlight
                SosToolCard(
                    title = "Linterna",
                    icon = if (flashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    color = if (flashlightOn) Color(0xFFF97316) else Color(0xFF64748B),
                    textColor = if (flashlightOn) Color(0xFF080810) else Color(0xFFE2E8F0),
                    modifier = Modifier.weight(1f)
                ) {
                    flashlightOn = !flashlightOn
                    // Toggle Flashlight Logic (Requires CameraManager, omitting for now to avoid permission crash complexity)
                }
            }
        }
    }
}

@Composable
fun SosToolCard(
    title: String,
    icon: ImageVector,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun EmergencyScreenPreview() {
    GeoRacingTheme {
        EmergencyScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}
