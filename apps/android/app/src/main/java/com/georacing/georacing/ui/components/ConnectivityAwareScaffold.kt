package com.georacing.georacing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.debug.ScenarioSimulator

@Composable
fun ConnectivityAwareScaffold(
    content: @Composable () -> Unit
) {
    val isNetworkDead by ScenarioSimulator.isNetworkDead.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        
        // Main Content (Always rendered, but covered when dead)
        content()

        // Dead State Overlay (Terminal Style)
        if (isNetworkDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E0E18)) // Racing Dark Surface
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Critical Failure",
                        tint = Color(0xFFE8253A), // Racing Red
                        modifier = Modifier.size(80.dp)
                    )

                    Text(
                        text = "CRITICAL NETWORK FAILURE\nERROR 503",
                        color = Color(0xFF06B6D4),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = "ACTIVATING OFFLINE PROTOCOL...\nESTABLISHING MESH NODE...",
                        color = Color(0xFF06B6D4).copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Static Vital Data
                    TerminalDataRow("Ubicación segura:", "PUERTA 3")
                    TerminalDataRow("Ticket Status:", "CACHED (VALID)")
                    TerminalDataRow("Emergency Route:", "DOWNLOADED")

                    Spacer(modifier = Modifier.height(48.dp))

                    // Offline Action Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
                            .clickable { /* Action for demo */ }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VER TICKET OFFLINE",
                            color = Color(0xFFF8FAFC),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF06B6D4).copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp
        )
        Text(
            text = value,
            color = Color(0xFF06B6D4),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
