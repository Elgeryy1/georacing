package com.georacing.georacing.ui.screens.transport

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class TransportType(val label: String, val icon: ImageVector, val color: Color) {
    TRAIN("Tren", Icons.Default.Train, ElectricBlue),
    SHUTTLE("Shuttle", Icons.Default.DirectionsBus, StatusGreen),
    WALKING("A pie", Icons.Default.DirectionsWalk, NeonOrange)
}

private data class TransportSchedule(
    val type: TransportType,
    val line: String,
    val origin: String,
    val destination: String,
    val departures: List<String>,
    val duration: String,
    val status: TransportStatus,
    val notes: String = ""
)

private enum class TransportStatus(val label: String, val color: Color) {
    ON_TIME("Puntual", StatusGreen),
    DELAYED("Retraso 5'", StatusAmber),
    CROWDED("Lleno", StatusRed),
    EXTRA("Refuerzo", ElectricBlue)
}

@Composable
fun TransportScreen(navController: NavController) {
    val backdrop = LocalBackdrop.current
    var selectedType by remember { mutableStateOf<TransportType?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(300); isLoading = false; screenVisible = true }

    val schedules = remember {
        listOf(
            TransportSchedule(TransportType.TRAIN, "R2 Nord", "Barcelona Passeig de Gràcia", "Montmeló", listOf("08:12", "08:42", "09:12", "09:42", "10:12", "10:42", "11:12"), "45 min", TransportStatus.ON_TIME, "Cada 30 min. Bajar en estación Montmeló."),
            TransportSchedule(TransportType.TRAIN, "R2 Nord", "Granollers Centre", "Montmeló", listOf("08:25", "08:55", "09:25", "09:55", "10:25", "10:55"), "8 min", TransportStatus.ON_TIME, "Frecuencia cada 30 min."),
            TransportSchedule(TransportType.SHUTTLE, "Shuttle A", "Estación Montmeló", "Puerta Principal Circuito", listOf("08:30", "08:45", "09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30"), "10 min", TransportStatus.ON_TIME, "Servicio gratuito con entrada. Cada 15 min."),
            TransportSchedule(TransportType.SHUTTLE, "Shuttle B", "Parking P4 (La Roca Village)", "Puerta Norte Circuito", listOf("08:00", "08:30", "09:00", "09:30", "10:00", "10:30"), "15 min", TransportStatus.EXTRA, "Servicio adicional para eventos especiales."),
            TransportSchedule(TransportType.SHUTTLE, "Shuttle C", "Pl. Catalunya (Barcelona)", "Circuito Directo", listOf("07:00", "08:00", "09:00"), "55 min", TransportStatus.CROWDED, "Bus directo sin paradas. Reserva online recomendada."),
            TransportSchedule(TransportType.WALKING, "Ruta a pie", "Estación Montmeló", "Puerta Principal", listOf("Libre"), "25 min", TransportStatus.ON_TIME, "1.8 km por camino señalizado. Seguir indicaciones naranja.")
        )
    }

    val filteredSchedules = if (selectedType != null) schedules.filter { it.type == selectedType } else schedules

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Glass Top Bar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "TRANSPORTE",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                },
                actions = {
                    HomeIconButton {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    // Location header
                    item {
                        AnimatedVisibility(
                            visible = screenVisible,
                            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 3 }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = RacingRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "CIRCUIT DE BARCELONA-CATALUNYA · MONTMELÓ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }

                    // Info banner
                    item {
                        AnimatedVisibility(
                            visible = screenVisible,
                            enter = fadeIn(tween(600, 100)) + slideInVertically(tween(600, 100)) { it / 3 }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElectricBlue.copy(alpha = 0.06f))
                                    .border(0.5.dp, ElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Próximo shuttle desde Montmeló en 12 min",
                                        fontSize = 13.sp,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Filter chips
                    item {
                        AnimatedVisibility(
                            visible = screenVisible,
                            enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { it / 3 }
                        ) {
                            Column {
                                Text(
                                    "FILTRAR POR TIPO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = TextTertiary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PremiumFilterChip("TODOS", TextSecondary, selectedType == null) { selectedType = null }
                                    TransportType.entries.forEach { type ->
                                        PremiumFilterChip(type.label.uppercase(), type.color, selectedType == type) {
                                            selectedType = if (selectedType == type) null else type
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Schedule cards with staggered anim
                    itemsIndexed(filteredSchedules) { index, schedule ->
                        var cardVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(index * 60L + 300L); cardVisible = true }
                        AnimatedVisibility(
                            visible = cardVisible,
                            enter = fadeIn(spring(dampingRatio = 0.8f)) +
                                    slideInVertically(spring(dampingRatio = 0.7f)) { it / 2 }
                        ) {
                            PremiumTransportCard(schedule)
                        }
                    }

                    // Tips card
                    item {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    "CONSEJOS DE TRANSPORTE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = TextTertiary
                                )
                                Spacer(Modifier.height(10.dp))
                                val tips = listOf(
                                    "Llega al menos 1h antes del evento",
                                    "La T-casual funciona para R2 Nord",
                                    "Evita AP-7 salida 14 en hora punta",
                                    "Shuttle gratuito con entrada al circuito",
                                    "Parking P1 y P2 se llenan antes de las 10:00"
                                )
                                tips.forEach { tip ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 5.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(RacingRed)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(tip, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Premium Transport Card
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumTransportCard(schedule: TransportSchedule) {
    val nextDeparture = remember(schedule) {
        val now = LocalTime.now()
        schedule.departures.firstOrNull { dep ->
            try { LocalTime.parse(dep, DateTimeFormatter.ofPattern("HH:mm")).isAfter(now) } catch (_: Exception) { false }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                accentGlow = schedule.type.color
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(schedule.type.color.copy(alpha = 0.12f))
                        .border(0.5.dp, schedule.type.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(schedule.type.icon, schedule.type.label, tint = schedule.type.color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(schedule.line, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    Text(
                        "${schedule.origin} → ${schedule.destination}",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
                // Status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(schedule.status.color.copy(alpha = 0.12f))
                        .border(0.5.dp, schedule.status.color.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        schedule.status.label.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = schedule.status.color
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DURACIÓN", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextTertiary)
                    Text(schedule.duration, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                }
                if (nextDeparture != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PRÓXIMA SALIDA", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextTertiary)
                        Text(nextDeparture, fontSize = 16.sp, color = StatusGreen, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            if (schedule.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(schedule.notes, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Departure times
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(schedule.departures) { dep ->
                    val isPast = try { LocalTime.parse(dep, DateTimeFormatter.ofPattern("HH:mm")).isBefore(LocalTime.now()) } catch (_: Exception) { false }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPast) Color.Transparent else MetalGrey.copy(alpha = 0.4f))
                            .then(
                                if (!isPast) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            dep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPast) TextTertiary.copy(alpha = 0.3f) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Premium Filter Chip
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumFilterChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(if (selected) 0.2f else 0.05f, label = "bg")
    val borderAlpha by animateFloatAsState(if (selected) 0.5f else 0.08f, label = "brd")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = bgAlpha))
            .border(0.5.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = color.copy(alpha = if (selected) 1f else 0.5f)
        )
    }
}
