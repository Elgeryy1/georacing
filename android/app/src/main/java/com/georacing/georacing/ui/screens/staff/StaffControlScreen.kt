package com.georacing.georacing.ui.screens.staff

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.georacing.georacing.debug.ScenarioSimulator
import java.text.SimpleDateFormat
import java.util.*

// Racing Theme Colors
private val RacingRed = Color(0xFFE8253A)
private val WarningOrange = Color(0xFFF97316)
private val NeonCyan = Color(0xFF06B6D4)
private val NominalGreen = Color(0xFF22C55E)
private val RacingBorder = Color(0xFF1E293B)
private val ConsoleBg = Color(0xFF0E0E18)
private val ConsoleText = Color(0xFF06B6D4)
private val SlateLabel = Color(0xFF64748B)
private val OffWhite = Color(0xFFF8FAFC)
private val DeepBg = Color(0xFF080810)
private val SurfaceDark = Color(0xFF14141C)
private val RacingGradient = Brush.verticalGradient(listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810)))

@Composable
fun StaffControlScreen() {
    val context = LocalContext.current
    
    var showPinDialog by remember { mutableStateOf(true) }
    var isAuthenticated by remember { mutableStateOf(false) }
    
    // Observe ScenarioSimulator states for console
    val isNetworkDead by ScenarioSimulator.isNetworkDead.collectAsState()
    val crowdIntensity by ScenarioSimulator.crowdIntensity.collectAsState()
    val isAtGate by ScenarioSimulator.isAtGate.collectAsState()
    val batteryLevel by ScenarioSimulator.forcedBatteryLevel.collectAsState()
    
    // Console log state
    var consoleLog by remember { mutableStateOf(listOf("SYS > Centro de Mando Online")) }
    
    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        consoleLog = (consoleLog + "[$timestamp] $message").takeLast(8)
    }

    if (showPinDialog) {
        SecurityPinDialog(
            onDismiss = { },
            onSuccess = {
                isAuthenticated = true
                showPinDialog = false
                log("AUTH > ✅ Acceso autorizado. Nivel: ADMIN")
            }
        )
    }

    if (isAuthenticated) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RacingGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CENTRO DE MANDO",
                        color = OffWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "OPERACIONES • SIMULADOR",
                        color = SlateLabel,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp
                    )
                }
                
                // Live Status Indicator
                val pulse = rememberInfiniteTransition(label = "pulse")
                val alpha by pulse.animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when {
                                isNetworkDead -> RacingRed.copy(alpha)
                                crowdIntensity > 0.5f -> WarningOrange.copy(alpha)
                                else -> NominalGreen.copy(alpha)
                            },
                            RoundedCornerShape(6.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // =============================================
            // EMERGENCY BUTTON: KILL NETWORK
            // =============================================
            EmergencyButton(
                text = "🚨 SIMULAR CAÍDA DE RED",
                description = "Activa protocolo offline",
                color = RacingRed,
                isActive = isNetworkDead,
                onClick = {
                    ScenarioSimulator.killNetwork()
                    log("NET > ⚠️ RED CAÍDA - ERROR 503")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // =============================================
            // WARNING BUTTON: CROWD SURGE
            // =============================================
            EmergencyButton(
                text = "👥 SIMULAR AGLOMERACIÓN",
                description = "Densidad alta en acceso principal",
                color = WarningOrange,
                isActive = crowdIntensity > 0.5f,
                onClick = {
                    ScenarioSimulator.triggerCrowdSurge()
                    log("CROWD > ⚠️ DENSIDAD CRÍTICA 90%")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // =============================================
            // INFO BUTTON: GATE ARRIVAL
            // =============================================
            EmergencyButton(
                text = "📍 FORZAR LLEGADA A PUERTA",
                description = "Dispara entrada inteligente",
                color = NeonCyan,
                isActive = isAtGate,
                onClick = {
                    ScenarioSimulator.arriveAtGate()
                    log("GATE > 🚪 Llegada detectada Gate-3")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // =============================================
            // NOMINAL: RESET ALL
            // =============================================
            EmergencyButton(
                text = "✅ SISTEMAS NOMINALES",
                description = "Restablecer operaciones normales",
                color = NominalGreen,
                isActive = false,
                onClick = {
                    ScenarioSimulator.resetAll()
                    ScenarioSimulator.restoreNetwork()
                    ScenarioSimulator.resetCrowd()
                    ScenarioSimulator.resetGate()
                    log("SYS > ✅ Todos los sistemas NOMINAL")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // =============================================
            // CAR DISCONNECT SIMULATION
            // =============================================
            val carConnectionManager = remember { com.georacing.georacing.infrastructure.car.CarConnectionManager(context) }
            
            EmergencyButton(
                text = "🔌 SIMULAR DESCONEXIÓN COCHE",
                description = "Trigger handover sin cable USB",
                color = Color(0xFF8B5CF6), // Purple
                isActive = false,
                onClick = {
                    carConnectionManager.triggerHandover()
                    log("CAR > 🚗 Handover triggered! Parking saved")
                    Toast.makeText(context, "Handover activado", Toast.LENGTH_SHORT).show()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // =============================================
            // CONSOLE OUTPUT
            // =============================================
            Text(
                "TERMINAL DE ESTADO",
                color = SlateLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.dp, RacingBorder, RoundedCornerShape(8.dp))
                    .background(ConsoleBg, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    // Current Status Line
                    val statusText = when {
                        isNetworkDead -> "STATUS: NETWORK ERROR 503 ⚠️"
                        crowdIntensity > 0.5f -> "STATUS: CROWD DENSITY CRITICAL ⚠️"
                        isAtGate -> "STATUS: AT GATE - DEPLOYING SMART ENTRY"
                        batteryLevel != null && batteryLevel!! <= 20 -> "STATUS: BATTERY SURVIVAL MODE"
                        else -> "STATUS: ALL SYSTEMS OPERATIONAL ✓"
                    }
                    
                    Text(
                        statusText,
                        color = when {
                            isNetworkDead -> RacingRed
                            crowdIntensity > 0.5f -> WarningOrange
                            isAtGate -> NeonCyan
                            else -> NominalGreen
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = RacingBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Log entries
                    consoleLog.forEach { line ->
                        Text(
                            line,
                            color = ConsoleText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EmergencyButton(
    text: String,
    description: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderWidth = if (isActive) 3.dp else 1.dp
    val borderColor = if (isActive) color else RacingBorder
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) color.copy(alpha = 0.15f) else SurfaceDark
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text,
                color = if (isActive) color else OffWhite,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                color = SlateLabel,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            if (isActive) {
                Text(
                    "● ACTIVO",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SecurityPinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Lock, 
                    contentDescription = null, 
                    tint = RacingRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ACCESO RESTRINGIDO",
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "SOLO PERSONAL AUTORIZADO",
                    color = SlateLabel,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                TextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    placeholder = { Text("PIN (1234)", color = SlateLabel) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeepBg,
                        unfocusedContainerColor = DeepBg,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        cursorColor = RacingRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (pin == "1234") {
                            onSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ACCEDER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
