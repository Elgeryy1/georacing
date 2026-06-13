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
import com.georacing.georacing.ui.theme.LocalEventVisualStyle

@Composable
fun ConnectivityAwareScaffold(
    content: @Composable () -> Unit
) {
    val isNetworkDead by ScenarioSimulator.isNetworkDead.collectAsState()
    val visuals = LocalEventVisualStyle.current

    Box(modifier = Modifier.fillMaxSize()) {
        
        // Main Content (Always rendered, but covered when dead)
        content()

        // Dead State Overlay (Terminal Style)
        if (isNetworkDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(visuals.panelSurfaceStrong)
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
                        tint = visuals.navSelected,
                        modifier = Modifier.size(80.dp)
                    )

                    Text(
                        text = "CRITICAL NETWORK FAILURE\nERROR 503",
                        color = visuals.heroChipText,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = "ACTIVATING OFFLINE PROTOCOL...\nESTABLISHING MESH NODE...",
                        color = visuals.heroChipText.copy(alpha = 0.8f),
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
                            .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(4.dp))
                            .clickable { /* Action for demo */ }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VER TICKET OFFLINE",
                            color = MaterialTheme.colorScheme.onBackground,
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
    val visuals = LocalEventVisualStyle.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            color = visuals.heroChipText.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp
        )
        Text(
            text = value,
            color = visuals.heroChipText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
