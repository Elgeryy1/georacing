package com.georacing.georacing.ui.screens.staff

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.ble.BeaconAdvertiser

// Racing Theme Colors
private val RacingRed = Color(0xFFE8253A)
private val NeonCyan = Color(0xFF06B6D4)
private val NeonOrange = Color(0xFFF97316)
private val RacingGreen = Color(0xFF22C55E)
private val SlateLabel = Color(0xFF64748B)
private val OffWhite = Color(0xFFF8FAFC)
private val DeepBg = Color(0xFF080810)
private val SurfaceDark = Color(0xFF14141C)
private val SurfaceAlt = Color(0xFF0E0E18)
private val GoldAccent = Color(0xFFD4A84B)
private val RacingGradient = Brush.verticalGradient(listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810)))

/**
 * 🆘 Staff Mode Screen - Pantalla oculta para personal del circuito.
 * 
 * Permite activar el modo "Danger Broadcast" que emite señales BLE
 * de emergencia para alertar a otros dispositivos cercanos.
 * 
 * Acceso: Settings > 7 taps en versión (Easter egg style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffModeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    
    // BLE Advertiser
    val beaconAdvertiser = remember { BeaconAdvertiser(context) }
    val advertisingState by beaconAdvertiser.advertisingState.collectAsState()
    
    // State
    var isDangerModeActive by remember { mutableStateOf(false) }
    var selectedZone by remember { mutableStateOf(1) }
    var selectedAlertLevel by remember { mutableStateOf(BeaconAdvertiser.MODE_EVACUATION) }
    var selectedSection by remember { mutableIntStateOf(0) } // iOS parity: 0=Alerts, 1=Beacons, 2=Status, 3=BLE Broadcast
    
    // Beacon toggles state (iOS parity: beaconsSection)
    val beaconToggles = remember {
        mutableStateMapOf(
            "main_entrance" to true,
            "grandstand_a" to true,
            "grandstand_b" to true,
            "paddock" to true,
            "pit_lane" to false
        )
    }
    
    // Animación de pulso cuando está activo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDangerModeActive) Color(0xFF3B0A0A) else DeepBg,
        animationSpec = tween(500),
        label = "bgColor"
    )
    
    // Zonas del circuito
    val zones = listOf(
        1 to "Tribuna Principal",
        2 to "Zona Paddock",
        3 to "Curva 1-3",
        4 to "Curva 4-6",
        5 to "Curva 7-9",
        6 to "Recta Principal",
        7 to "Zona Hospitality",
        8 to "Parking General"
    )
    
    // Niveles de alerta
    val alertLevels = listOf(
        BeaconAdvertiser.MODE_WARNING to "⚠️ Precaución",
        BeaconAdvertiser.MODE_DANGER to "🔴 Peligro",
        BeaconAdvertiser.MODE_EVACUATION to "🆘 Evacuación"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Modo Staff",
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MODO STAFF",
                            color = OffWhite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        beaconAdvertiser.stopAdvertising()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            // ── iOS parity: Segmented Control (Alerts / Beacons / Status / BLE) ──
            val sectionLabels = listOf("Alertas", "Balizas", "Estado", "BLE")
            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = SurfaceDark,
                contentColor = OffWhite
            ) {
                sectionLabels.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedSection) {
                0 -> StaffAlertsSection()
                1 -> StaffBeaconsSection(beaconToggles)
                2 -> StaffStatusSection()
                3 -> StaffBLEBroadcastSection(
                    isDangerModeActive = isDangerModeActive,
                    onToggle = { isDangerModeActive = it },
                    selectedZone = selectedZone,
                    onZoneChange = { selectedZone = it },
                    selectedAlertLevel = selectedAlertLevel,
                    onAlertLevelChange = { selectedAlertLevel = it },
                    zones = zones,
                    alertLevels = alertLevels,
                    beaconAdvertiser = beaconAdvertiser,
                    advertisingState = advertisingState,
                    pulseScale = pulseScale,
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

// ── iOS parity: Alerts Section (send alerts to all users) ──
@Composable
private fun StaffAlertsSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ENVIAR ALERTA",
            style = MaterialTheme.typography.titleMedium,
            color = OffWhite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        StaffAlertButton(
            icon = Icons.Default.Warning,
            title = "Alerta General",
            description = "Enviar mensaje a todos los usuarios",
            color = Color(0xFFFBBF24)
        )
        StaffAlertButton(
            icon = Icons.Default.LocalFireDepartment,
            title = "Emergencia",
            description = "Activar protocolo de emergencia",
            color = Color(0xFFEF4444)
        )
        StaffAlertButton(
            icon = Icons.Default.Campaign,
            title = "Anuncio",
            description = "Enviar información general",
            color = Color(0xFF3B82F6)
        )
    }
}

@Composable
private fun StaffAlertButton(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp),
        onClick = { /* TODO: send alert via API */ }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OffWhite, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = SlateLabel)
            }
            Icon(Icons.Default.ChevronRight, null, tint = SlateLabel)
        }
    }
}

// ── iOS parity: Beacons Section (toggle beacons on/off) ──
@Composable
private fun StaffBeaconsSection(beaconToggles: MutableMap<String, Boolean>) {
    val beaconNames = mapOf(
        "main_entrance" to "Entrada Principal",
        "grandstand_a" to "Tribuna A",
        "grandstand_b" to "Tribuna B",
        "paddock" to "Paddock",
        "pit_lane" to "Pit Lane"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "CONTROL DE BALIZAS",
            style = MaterialTheme.typography.titleMedium,
            color = OffWhite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        beaconNames.forEach { (id, name) ->
            val isOn = beaconToggles[id] ?: false
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOn) RacingGreen else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(name, color = OffWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isOn,
                        onCheckedChange = { beaconToggles[id] = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = NeonOrange,
                            checkedThumbColor = OffWhite
                        )
                    )
                }
            }
        }
    }
}

// ── iOS parity: Status Section (circuit overview) ──
@Composable
private fun StaffStatusSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ESTADO DEL CIRCUITO",
            style = MaterialTheme.typography.titleMedium,
            color = OffWhite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        StaffStatusCard("Estado Actual", "BANDERA VERDE", Icons.Default.Flag, RacingGreen)
        StaffStatusCard("Usuarios Activos", "1,234", Icons.Default.People, Color(0xFF3B82F6))
        StaffStatusCard("Alertas Pendientes", "3", Icons.Default.NotificationsActive, NeonOrange)
        StaffStatusCard("Balizas Activas", "12/15", Icons.Default.Bluetooth, Color(0xFFA855F7))
    }
}

@Composable
private fun StaffStatusCard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = SlateLabel)
                Text(value, style = MaterialTheme.typography.titleMedium, color = OffWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Original BLE Broadcast Section (refactored from old single-view) ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffBLEBroadcastSection(
    isDangerModeActive: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedZone: Int,
    onZoneChange: (Int) -> Unit,
    selectedAlertLevel: Int,
    onAlertLevelChange: (Int) -> Unit,
    zones: List<Pair<Int, String>>,
    alertLevels: List<Pair<Int, String>>,
    beaconAdvertiser: BeaconAdvertiser,
    advertisingState: String,
    pulseScale: Float,
    backgroundColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Warning Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NeonOrange.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Advertencia",
                        tint = NeonOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "SOLO PARA PERSONAL AUTORIZADO\nUsar solo en emergencias reales.",
                        color = NeonOrange,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selector de Zona
            Text(
                "SELECCIONAR ZONA",
                style = MaterialTheme.typography.titleMedium,
                color = SlateLabel,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            
            var zoneExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = zoneExpanded,
                onExpandedChange = { if (!isDangerModeActive) zoneExpanded = it }
            ) {
                OutlinedTextField(
                    value = zones.find { it.first == selectedZone }?.second ?: "Zona $selectedZone",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isDangerModeActive,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RacingRed,
                        unfocusedBorderColor = SlateLabel,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )
                ExposedDropdownMenu(
                    expanded = zoneExpanded,
                    onDismissRequest = { zoneExpanded = false }
                ) {
                    zones.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text("$id - $name") },
                            onClick = {
                                onZoneChange(id)
                                zoneExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Selector de Nivel de Alerta
            Text(
                "NIVEL DE ALERTA",
                style = MaterialTheme.typography.titleMedium,
                color = SlateLabel,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                alertLevels.forEach { (level, label) ->
                    FilterChip(
                        selected = selectedAlertLevel == level,
                        onClick = { if (!isDangerModeActive) onAlertLevelChange(level) },
                        label = { Text(label) },
                        enabled = !isDangerModeActive,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (level) {
                                BeaconAdvertiser.MODE_WARNING -> NeonOrange
                                BeaconAdvertiser.MODE_DANGER -> Color(0xFFEF4444)
                                else -> RacingRed
                            }
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón Principal de Emergencia
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (isDangerModeActive) pulseScale else 1f)
            ) {
                Button(
                    onClick = {
                        if (isDangerModeActive) {
                            beaconAdvertiser.stopAdvertising()
                            onToggle(false)
                        } else {
                            beaconAdvertiser.startDangerAdvertising(
                                staffId = "STAFF_001",
                                zoneId = selectedZone,
                                alertMode = selectedAlertLevel
                            )
                            onToggle(true)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDangerModeActive) SlateLabel else RacingRed
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (isDangerModeActive) Icons.Default.Close else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OffWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (isDangerModeActive) "DETENER" else "ACTIVAR\nALERTA",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            color = OffWhite
                        )
                    }
                }
            }
            
            // Estado actual
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ESTADO BLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateLabel,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        advertisingState,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDangerModeActive) Color(0xFFEF4444) else RacingGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (isDangerModeActive) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RacingRed.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚠️ BROADCAST ACTIVO ⚠️",
                            style = MaterialTheme.typography.titleLarge,
                            color = OffWhite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Zona: ${zones.find { it.first == selectedZone }?.second}",
                            color = OffWhite
                        )
                        Text(
                            "Nivel: ${alertLevels.find { it.first == selectedAlertLevel }?.second}",
                            color = OffWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Los dispositivos cercanos recibirán esta alerta",
                            color = SlateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // ── Mapa visual de estados de zonas ──
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "MAPA DE ESTADOS",
                style = MaterialTheme.typography.titleMedium,
                color = SlateLabel,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Estado de cada zona (la zona activa aparece en su nivel de alerta)
            val zoneStates = remember(isDangerModeActive, selectedZone, selectedAlertLevel) {
                zones.map { (id, name) ->
                    val state = if (isDangerModeActive && id == selectedZone) {
                        when (selectedAlertLevel) {
                            BeaconAdvertiser.MODE_WARNING -> ZoneState.WARNING
                            BeaconAdvertiser.MODE_DANGER -> ZoneState.DANGER
                            BeaconAdvertiser.MODE_EVACUATION -> ZoneState.EVACUATION
                            else -> ZoneState.NORMAL
                        }
                    } else {
                        ZoneState.NORMAL
                    }
                    ZoneInfo(id, name, state)
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Mapa esquemático del circuito con zonas coloreadas
                    CircuitZoneMap(
                        zoneStates = zoneStates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Leyenda
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ZoneLegendItem("Normal", RacingGreen)
                        ZoneLegendItem("Precaución", NeonOrange)
                        ZoneLegendItem("Peligro", Color(0xFFEF4444))
                        ZoneLegendItem("Evacuación", RacingRed)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Lista de zonas con estado
                    zoneStates.forEach { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(zone.state.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${zone.id} - ${zone.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OffWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                zone.state.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = zone.state.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

// ── Modelos de estado de zona ──

private enum class ZoneState(val label: String, val color: Color) {
    NORMAL("Normal", Color(0xFF22C55E)),
    WARNING("Precaución", Color(0xFFF97316)),
    DANGER("Peligro", Color(0xFFEF4444)),
    EVACUATION("Evacuación", Color(0xFFE8253A))
}

private data class ZoneInfo(
    val id: Int,
    val name: String,
    val state: ZoneState
)

@Composable
private fun ZoneLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}

/**
 * Mapa esquemático del circuito con zonas coloreadas según su estado de alerta.
 * Diseño simplificado representando la forma del Circuit de Barcelona-Catalunya.
 */
@Composable
private fun CircuitZoneMap(
    zoneStates: List<ZoneInfo>,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "zonePulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16.dp.toPx()
        
        // Fondo del circuito (forma ovalada simplificada)
        drawRoundRect(
            color = Color(0xFF0E0E18),
            topLeft = Offset(padding, padding),
            size = Size(w - padding * 2, h - padding * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
        )
        
        // Borde del circuito
        drawRoundRect(
            color = Color(0xFF2A2A3A),
            topLeft = Offset(padding, padding),
            size = Size(w - padding * 2, h - padding * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Posiciones relativas de cada zona en el mapa esquemático
        val zonePositions = listOf(
            Offset(w * 0.5f, h * 0.2f),   // 1: Tribuna Principal (arriba centro)
            Offset(w * 0.75f, h * 0.25f),  // 2: Zona Paddock (arriba derecha)
            Offset(w * 0.85f, h * 0.5f),   // 3: Curva 1-3 (derecha)
            Offset(w * 0.7f, h * 0.75f),   // 4: Curva 4-6 (abajo derecha)
            Offset(w * 0.3f, h * 0.75f),   // 5: Curva 7-9 (abajo izquierda)
            Offset(w * 0.5f, h * 0.5f),    // 6: Recta Principal (centro)
            Offset(w * 0.25f, h * 0.25f),  // 7: Zona Hospitality (arriba izquierda)
            Offset(w * 0.15f, h * 0.5f)    // 8: Parking General (izquierda)
        )
        
        zonePositions.forEachIndexed { index, pos ->
            if (index < zoneStates.size) {
                val zone = zoneStates[index]
                val radius = 18.dp.toPx()
                val alpha = if (zone.state == ZoneState.EVACUATION) pulseAlpha else 0.8f
                
                // Halo de la zona
                drawCircle(
                    color = zone.state.color.copy(alpha = alpha * 0.3f),
                    radius = radius * 1.6f,
                    center = pos
                )
                
                // Punto de la zona
                drawCircle(
                    color = zone.state.color.copy(alpha = alpha),
                    radius = radius,
                    center = pos
                )
                
                // Borde
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius,
                    center = pos,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}
