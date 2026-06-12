package com.georacing.georacing.ui.screens.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.gamification.GamificationRepository
import com.georacing.georacing.domain.model.Achievement
import com.georacing.georacing.domain.model.AchievementCategory
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*

/**
 * AchievementsScreen — Premium Gamification HUD
 * Logros, XP, niveles y progreso del fan en el circuito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    gamificationRepository: GamificationRepository
) {
    val backdrop = LocalBackdrop.current
    val energyProfile = LocalEnergyProfile.current

    // Si la batería está en modo supervivencia, bloquear gamificación
    if (!energyProfile.canUseGamification) {
        Box(Modifier.fillMaxSize()) {
            CarbonBackground()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(StatusAmber.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BatteryAlert, null, tint = StatusAmber, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("MODO AHORRO ACTIVO", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "La gamificación se ha desactivado para ahorrar batería.\nRecarga tu dispositivo para continuar.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .liquidGlass(RoundedCornerShape(12.dp), GlassLevel.L2, accentGlow = RacingRed)
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("VOLVER", color = RacingRed, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                        modifier = Modifier.noRippleClickable { navController.popBackStack() })
                }
            }
        }
        return
    }

    val profile by gamificationRepository.profile.collectAsState()
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    val displayedAchievements = if (selectedCategory != null) {
        profile.achievements.filter { it.category == selectedCategory }
    } else {
        profile.achievements
    }

    val unlockedCount = profile.achievements.count { it.isUnlocked }
    val totalCount = profile.achievements.size

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
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ChampagneGold))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("LOGROS", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Nivel ${profile.level} · ${profile.levelName}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                },
                actions = {
                    HomeIconButton { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Profile Hero Card ──
                item {
                    Spacer(Modifier.height(4.dp))
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .liquidGlass(RoundedCornerShape(24.dp), GlassLevel.L3, accentGlow = RacingRed)
                        ) {
                            // Red glow overlay
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(RacingRed.copy(alpha = 0.1f), Color.Transparent)
                                        )
                                    )
                                    .padding(28.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Level badge with glow
                                    val pulseAnim = rememberInfiniteTransition(label = "lvl")
                                    val glowAlpha by pulseAnim.animateFloat(
                                        initialValue = 0.3f, targetValue = 0.6f,
                                        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g"
                                    )
                                    Box(
                                        Modifier
                                            .size(88.dp)
                                            .drawBehind {
                                                drawCircle(RacingRed.copy(alpha = glowAlpha * 0.3f), radius = size.minDimension / 1.5f)
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(RacingRed.copy(alpha = 0.25f), Color.Transparent)
                                                )
                                            )
                                            .border(2.dp, RacingRed.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${profile.level}",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = TextPrimary
                                        )
                                    }

                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        profile.levelName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${profile.totalXP} XP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RacingRed,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(20.dp))

                                    // XP Progress Bar - premium gradient
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Nivel ${profile.level}", fontSize = 11.sp, color = TextTertiary)
                                            Text("Nivel ${profile.level + 1}", fontSize = 11.sp, color = TextTertiary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(AsphaltGrey)
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth(profile.xpProgress.coerceIn(0f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(RacingRed, Color(0xFFFF6B35))
                                                        )
                                                    )
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${(profile.totalXP % profile.xpForNextLevel)} / ${profile.xpForNextLevel} XP",
                                            fontSize = 10.sp,
                                            color = TextTertiary,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(Modifier.height(20.dp))

                                    // Stats row - glass sub-pills
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        GlassStatPill("Logros", "$unlockedCount/$totalCount", ChampagneGold)
                                        GlassStatPill("Circuitos", "${profile.circuitsVisited}", NeonCyan)
                                        GlassStatPill("km", String.format("%.1f", profile.kmWalked), StatusGreen)
                                        GlassStatPill("Amigos", "${profile.friendsInGroup}", ElectricBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Category Filter Chips ──
                item {
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 20 }
                    ) {
                        Column {
                            Text("CATEGORÍAS", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    PremiumFilterChip(
                                        selected = selectedCategory == null,
                                        label = "Todos ($unlockedCount/$totalCount)",
                                        accentColor = RacingRed,
                                        onClick = { selectedCategory = null }
                                    )
                                }
                                items(AchievementCategory.entries.toList()) { cat ->
                                    val catCount = profile.achievements.count { it.category == cat && it.isUnlocked }
                                    val catTotal = profile.achievements.count { it.category == cat }
                                    PremiumFilterChip(
                                        selected = selectedCategory == cat,
                                        label = "${cat.emoji} $catCount/$catTotal",
                                        accentColor = ElectricBlue,
                                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── Achievement Cards ──
                itemsIndexed(displayedAchievements) { index, achievement ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(300, 350 + index * 40)) + slideInVertically(tween(300, 350 + index * 40)) { 25 }
                    ) {
                        PremiumAchievementCard(achievement)
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
private fun PremiumAchievementCard(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    val accentColor = if (isUnlocked) StatusGreen else TextTertiary

    Box(
        Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.6f)
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(
                RoundedCornerShape(18.dp),
                GlassLevel.L2,
                accentGlow = if (isUnlocked) StatusGreen else Color.Transparent
            )
    ) {
        // Unlocked shimmer overlay
        if (isUnlocked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, StatusGreen.copy(alpha = 0.03f), Color.Transparent)
                        )
                    )
            )
        }

        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Emoji badge with glass background
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked)
                            Brush.radialGradient(listOf(accentColor.copy(alpha = 0.12f), Color.Transparent))
                        else
                            Brush.radialGradient(listOf(MetalGrey.copy(alpha = 0.3f), Color.Transparent))
                    )
                    .then(
                        if (isUnlocked) Modifier.border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isUnlocked) achievement.emoji else "🔒",
                    fontSize = 26.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        achievement.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (isUnlocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.CheckCircle, "Desbloqueado", tint = StatusGreen, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                if (!isUnlocked && achievement.progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AsphaltGrey)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(achievement.progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(ElectricBlue, NeonCyan))
                                    )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(achievement.progress * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // XP badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "+${achievement.xpReward}",
                    fontSize = 13.sp,
                    color = ChampagneGold,
                    fontWeight = FontWeight.Black
                )
                Text("XP", fontSize = 9.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun GlassStatPill(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun PremiumFilterChip(
    selected: Boolean,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.2f) else MetalGrey.copy(alpha = 0.3f)
    val textColor = if (selected) accentColor else TextSecondary

    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (selected) Modifier.border(0.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .noRippleClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * No-ripple clickable modifier for premium feel.
 */
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this
        .graphicsLayer { clip = false }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
