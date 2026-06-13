package com.georacing.georacing.ui.screens.fan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay

private data class DriverTelemetry(
    val position: Int,
    val number: Int,
    val name: String,
    val team: String,
    val teamColor: Color,
    val gap: String,
    val speed: Int,
    val tire: String,
    val tireAge: Int,
    val drs: Boolean = false,
    val sector1: String,
    val sector2: String,
    val sector3: String,
    val trackProgress: Float
)

/**
 * FanImmersiveScreen — Premium F1 Live Telemetry HUD
 * Clasificación en vivo con datos de pilotos simulados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanImmersiveScreen(navController: NavController) {
    val backdrop = LocalBackdrop.current
    var isLive by remember { mutableStateOf(true) }
    var currentLap by remember { mutableIntStateOf(42) }
    val totalLaps = 66
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    // Simulated driver data for Circuit de Catalunya
    var drivers by remember {
        mutableStateOf(
            listOf(
                DriverTelemetry(1, 1, "M. Verstappen", "Red Bull Racing", Color(0xFF1E41FF), "LEADER", 312, "M", 18, true, "25.432", "28.871", "26.114", 0.78f),
                DriverTelemetry(2, 16, "C. Leclerc", "Ferrari", Color(0xFFE8002D), "+2.341", 308, "M", 18, false, "25.567", "28.912", "26.298", 0.73f),
                DriverTelemetry(3, 44, "L. Hamilton", "Ferrari", Color(0xFFE8002D), "+5.892", 305, "H", 24, false, "25.612", "29.001", "26.445", 0.68f),
                DriverTelemetry(4, 4, "L. Norris", "McLaren", Color(0xFFFF8000), "+8.124", 310, "M", 18, true, "25.501", "28.943", "26.332", 0.62f),
                DriverTelemetry(5, 81, "O. Piastri", "McLaren", Color(0xFFFF8000), "+9.567", 307, "M", 18, false, "25.589", "29.012", "26.401", 0.55f),
                DriverTelemetry(6, 63, "G. Russell", "Mercedes", Color(0xFF27F4D2), "+12.891", 306, "H", 30, false, "25.678", "29.098", "26.503", 0.48f),
                DriverTelemetry(7, 14, "F. Alonso", "Aston Martin", Color(0xFF229971), "+15.234", 303, "H", 30, false, "25.734", "29.145", "26.578", 0.42f),
                DriverTelemetry(8, 55, "C. Sainz", "Williams", Color(0xFF1868DB), "+17.891", 304, "M", 22, false, "25.789", "29.201", "26.623", 0.35f)
            )
        )
    }

    // Simulate live updates
    LaunchedEffect(isLive) {
        while (isLive) {
            delay(3000)
            drivers = drivers.map { d ->
                d.copy(
                    speed = d.speed + (-5..5).random(),
                    trackProgress = ((d.trackProgress + 0.02f) % 1.0f)
                )
            }
            if ((0..10).random() > 8) currentLap = (currentLap + 1).coerceAtMost(totalLaps)
        }
    }

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (isLive) StatusRed else TextTertiary))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("F1 LIVE", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Telemetría en directo", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                },
                actions = {
                    // Live toggle
                    IconButton(onClick = { isLive = !isLive }) {
                        Icon(
                            if (isLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isLive) "Pausar" else "Reanudar",
                            tint = if (isLive) StatusGreen else TextTertiary
                        )
                    }
                    HomeIconButton { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Circuit Location ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -15 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RacingRed.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = RacingRed, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Circuit de Barcelona-Catalunya", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 0.5.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Race Status Card ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 30 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .liquidGlass(RoundedCornerShape(20.dp), GlassLevel.L3, accentGlow = RacingRed)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("VUELTA", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "$currentLap / $totalLaps",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = TextPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("BANDERA", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(StatusGreen)
                                                    .drawBehind {
                                                        drawCircle(StatusGreen.copy(alpha = 0.3f), radius = size.minDimension)
                                                    }
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("VERDE", style = MaterialTheme.typography.titleMedium, color = StatusGreen, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))

                                // Premium lap progress
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(AsphaltGrey)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(currentLap.toFloat() / totalLaps)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(listOf(RacingRed, Color(0xFFFF6B35)))
                                            )
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(StatusGreen.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("DRS ACTIVO", fontSize = 9.sp, color = StatusGreen, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text("Zona 1", fontSize = 10.sp, color = TextTertiary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Vuelta rápida: ", fontSize = 10.sp, color = TextTertiary)
                                        Text("1:19.432", fontSize = 10.sp, color = NeonPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Column Headers ──
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("POS", Modifier.width(32.dp), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("PILOTO", Modifier.weight(1f), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("GAP", Modifier.width(64.dp), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Black, textAlign = TextAlign.End, letterSpacing = 1.sp)
                        Text("VEL", Modifier.width(48.dp), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Black, textAlign = TextAlign.End, letterSpacing = 1.sp)
                        Text("NEUM", Modifier.width(40.dp), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Black, textAlign = TextAlign.End, letterSpacing = 1.sp)
                    }
                }

                // ── Driver Rows ──
                itemsIndexed(drivers) { index, driver ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(300, 250 + index * 50)) + slideInVertically(tween(300, 250 + index * 50)) { 20 }
                    ) {
                        PremiumDriverRow(driver)
                    }
                }

                // ── Sectors Comparison ──
                item {
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 700)) + slideInVertically(tween(500, 700)) { 20 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = NeonPurple)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text("SECTORES — LÍDER", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(14.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    PremiumSectorBlock("S1", drivers[0].sector1, StatusGreen)
                                    PremiumSectorBlock("S2", drivers[0].sector2, NeonPurple)
                                    PremiumSectorBlock("S3", drivers[0].sector3, StatusAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// ── Premium Composables ──
// ══════════════════════════════════════════════

@Composable
private fun PremiumDriverRow(driver: DriverTelemetry) {
    val isTopThree = driver.position <= 3
    val positionColor = when (driver.position) {
        1 -> ChampagneGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextPrimary
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .liquidGlass(
                RoundedCornerShape(12.dp),
                if (isTopThree) GlassLevel.L2 else GlassLevel.L1,
                accentGlow = if (isTopThree) driver.teamColor else Color.Transparent
            )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position
            Text(
                "${driver.position}",
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = positionColor
            )

            // Team color bar
            Box(
                Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(driver.teamColor)
            )
            Spacer(Modifier.width(8.dp))

            // Name
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${driver.number}", fontSize = 10.sp, color = driver.teamColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(driver.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Text(driver.team, fontSize = 9.sp, color = TextTertiary, maxLines = 1)
            }

            // Gap
            Text(
                driver.gap,
                modifier = Modifier.width(64.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (driver.position == 1) RacingRed else TextSecondary,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Bold
            )

            // Speed
            Text(
                "${driver.speed}",
                modifier = Modifier.width(44.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = TextPrimary,
                textAlign = TextAlign.End
            )

            // Tire
            Spacer(Modifier.width(4.dp))
            val tireColor = when (driver.tire) {
                "S" -> StatusRed
                "M" -> StatusAmber
                "H" -> TextPrimary
                else -> TextTertiary
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tireColor.copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(driver.tire, fontSize = 10.sp, color = tireColor, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
            Text("${driver.tireAge}", fontSize = 8.sp, color = TextTertiary, modifier = Modifier.width(16.dp), textAlign = TextAlign.End)

            // DRS indicator
            if (driver.drs) {
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StatusGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("DRS", fontSize = 8.sp, color = StatusGreen, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremiumSectorBlock(label: String, time: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            time,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
