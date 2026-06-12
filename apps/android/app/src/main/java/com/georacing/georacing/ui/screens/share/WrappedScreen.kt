package com.georacing.georacing.ui.screens.share

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.data.gamification.GamificationRepository
import com.georacing.georacing.data.health.HealthConnectManager
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.*

/**
 * GeoRacing Wrapped — Resumen post-evento con DATOS REALES.
 * Premium version con CarbonBackground + LiquidGlass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    onNavigateBack: () -> Unit = {},
    healthConnectManager: HealthConnectManager? = null,
    gamificationRepository: GamificationRepository? = null
) {
    val backdrop = LocalBackdrop.current
    val context = LocalContext.current

    // ── Datos reales ──
    var stepsCount by remember { mutableIntStateOf(0) }
    var distanceWalkedKm by remember { mutableStateOf(0.0) }
    var achievementsUnlocked by remember { mutableIntStateOf(0) }
    var totalAchievements by remember { mutableIntStateOf(0) }
    var totalXP by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var ordersPlaced by remember { mutableIntStateOf(0) }
    var totalSpent by remember { mutableStateOf(0.0) }
    var photosCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }

    val eventName = "GP de España 2025"
    val circuitName = "Circuit de Barcelona-Catalunya"

    // Cargar datos reales
    LaunchedEffect(Unit) {
        // 1. HealthConnect — pasos y distancia
        try {
            healthConnectManager?.let { hc ->
                val metrics = hc.readDailyMetrics()
                stepsCount = metrics.steps.toInt()
                distanceWalkedKm = metrics.distanceMeters / 1000.0
            }
        } catch (e: Exception) {
            Log.w("WrappedScreen", "HealthConnect no disponible: ${e.message}")
        }

        // 2. Gamification — logros
        try {
            gamificationRepository?.let { repo ->
                val profile = repo.profile.value
                achievementsUnlocked = profile.achievements.count { it.isUnlocked }
                totalAchievements = profile.achievements.size
                totalXP = profile.totalXP
                level = profile.level
            }
        } catch (e: Exception) {
            Log.w("WrappedScreen", "Gamification error: ${e.message}")
        }

        // 3. Backend — pedidos y fotos
        try {
            val orders = FirestoreLikeClient.api.read("orders")
            ordersPlaced = orders.size
            totalSpent = orders.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }
        } catch (e: Exception) {
            Log.w("WrappedScreen", "No se pudieron cargar pedidos: ${e.message}")
        }

        try {
            val photos = FirestoreLikeClient.api.read("moments")
            photosCount = photos.size
        } catch (e: Exception) {
            Log.w("WrappedScreen", "No se pudieron cargar fotos: ${e.message}")
        }

        isLoading = false
        showContent = true
    }

    val co2Saved = "%.1f".format(distanceWalkedKm * 0.21)

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ChampagneGold))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("TU WRAPPED", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(eventName, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildShareText(
                            eventName, circuitName, stepsCount, distanceWalkedKm,
                            achievementsUnlocked, totalAchievements, photosCount,
                            ordersPlaced, co2Saved
                        )
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir mi Wrapped"))
                    }) {
                        Icon(Icons.Default.Share, "Compartir", tint = ChampagneGold)
                    }
                }
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val pulseAnim = rememberInfiniteTransition(label = "p")
                        val pulseScale by pulseAnim.animateFloat(
                            initialValue = 0.8f, targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s"
                        )
                        Box(
                            Modifier
                                .size(64.dp)
                                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                                .drawBehind {
                                    drawCircle(ChampagneGold.copy(alpha = 0.15f), radius = size.minDimension / 2)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ChampagneGold, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Recopilando tus datos reales...", color = TextTertiary, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // ── Hero Header Card ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 50 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .liquidGlass(RoundedCornerShape(24.dp), GlassLevel.L3, accentGlow = RacingRed)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(RacingRed.copy(alpha = 0.12f), NeonPurple.copy(alpha = 0.05f), Color.Transparent)
                                        )
                                    )
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏁", fontSize = 56.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        eventName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(circuitName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(RacingRed.copy(alpha = 0.15f))
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Nivel $level", color = RacingRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("·", color = TextTertiary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${totalXP} XP", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Movement Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 200)) + slideInVertically(tween(500, 200)) { 30 }
                    ) {
                        PremiumWrappedSection(
                            title = "TU MOVIMIENTO",
                            icon = Icons.Default.DirectionsWalk,
                            accentColor = StatusGreen
                        ) {
                            if (stepsCount == 0 && distanceWalkedKm == 0.0) {
                                Text("Sin datos de HealthConnect. Activa los permisos.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    PremiumStatBubble("Pasos", if (stepsCount > 1000) "${stepsCount / 1000}K" else "$stepsCount", "🦶", StatusGreen)
                                    PremiumStatBubble("Andado", "%.1f km".format(distanceWalkedKm), "🚶", ElectricBlue)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Achievements Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 350)) + slideInVertically(tween(500, 350)) { 30 }
                    ) {
                        PremiumWrappedSection(
                            title = "LOGROS",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = ChampagneGold
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                PremiumStatBubble("Logros", "$achievementsUnlocked/$totalAchievements", "🏅", ChampagneGold)
                                PremiumStatBubble("Fotos", "$photosCount", "📸", NeonPurple)
                                PremiumStatBubble("Nivel", "$level", "⭐", RacingRed)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Gastronómico Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 500)) + slideInVertically(tween(500, 500)) { 30 }
                    ) {
                        PremiumWrappedSection(
                            title = "GASTRONÓMICO",
                            icon = Icons.Default.Restaurant,
                            accentColor = NeonCyan
                        ) {
                            if (ordersPlaced == 0) {
                                Text("No has realizado pedidos aún.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    PremiumStatBubble("Pedidos", "$ordersPlaced", "🛒", NeonCyan)
                                    PremiumStatBubble("Gastado", "%.1f€".format(totalSpent), "💰", StatusGreen)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Eco Impact Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 650)) + slideInVertically(tween(500, 650)) { 30 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = StatusGreen)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(StatusGreen.copy(alpha = 0.06f), Color.Transparent)
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(StatusGreen.copy(alpha = 0.12f))
                                            .drawBehind {
                                                drawCircle(StatusGreen.copy(alpha = 0.06f), radius = size.minDimension * 0.9f)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🌱", fontSize = 28.sp)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "$co2Saved kg CO₂ ahorrados",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = StatusGreen
                                        )
                                        Text(
                                            "Caminando en vez de usar transporte motorizado",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Datos reales · HealthConnect + Backend",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Share Button ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 800)) + slideInVertically(tween(500, 800)) { 20 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(RacingRed, Color(0xFFFF4D6D)))
                                )
                                .clickable {
                                    val shareText = buildShareText(eventName, circuitName, stepsCount, distanceWalkedKm, achievementsUnlocked, totalAchievements, photosCount, ordersPlaced, co2Saved)
                                    val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("COMPARTIR MI WRAPPED", color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// ── Premium Composables ──
// ══════════════════════════════════════════════

@Composable
private fun PremiumWrappedSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = accentColor)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun PremiumStatBubble(label: String, value: String, emoji: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .drawBehind {
                    drawCircle(color.copy(alpha = 0.06f), radius = size.minDimension / 1.5f)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 18.sp)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
    }
}

private fun buildShareText(
    eventName: String, circuitName: String, steps: Int, distKm: Double,
    achievementsUnl: Int, achievementsTotal: Int, photos: Int, orders: Int, co2: String
): String = """
🏁 Mi GeoRacing Wrapped — $eventName

🚶 ${"%.1f".format(distKm)} km andados ($steps pasos)
🏆 $achievementsUnl/$achievementsTotal logros desbloqueados
📸 $photos momentos capturados
🛒 $orders pedidos realizados
🌱 $co2 kg CO₂ ahorrados

¡Descarga GeoRacing y vive el circuito! 🏎️
#GeoRacing #Wrapped #${eventName.replace(" ", "")}
""".trimIndent()
