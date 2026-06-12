package com.georacing.georacing.ui.screens.achievements

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 🎴 Pantalla de Cromos / Coleccionables Digitales — Premium Racing Edition.
 */
@Composable
fun CollectiblesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val backdrop = LocalBackdrop.current
    val allCards = remember { generateAllCollectibles() }
    val unlockedIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingProgress by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while(true) {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "current_user"
                val req = com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(
                    table = "user_collectibles",
                    where = mapOf("user_id" to userId)
                )
                val response = com.georacing.georacing.data.firestorelike.FirestoreLikeClient.api.get(req)
                val ids = response.mapNotNull { map ->
                    val id = map["collectible_id"]?.toString()
                    val unlocked = map["unlocked"] as? Boolean ?: false
                    if (unlocked) id else null
                }.toSet()
                unlockedIds.value = ids
            } catch (e: Exception) {
                Log.w("CollectiblesScreen", "Error cargando progreso: ${e.message}")
                // No limpiamos los IDs en caso de error para no parpadear la UI si hay un corte de red
            }
            isLoadingProgress = false
            delay(2000) // Poll every 2 seconds for updates
        }
    }

    var selectedCard by remember { mutableStateOf<Collectible?>(null) }
    var filterRarity by remember { mutableStateOf<Rarity?>(null) }

    val displayedCards = remember(filterRarity) {
        if (filterRarity == null) allCards
        else allCards.filter { it.rarity == filterRarity }
    }

    val unlockedCount = allCards.count { it.id in unlockedIds.value }
    val totalCount = allCards.size
    val progressPercent = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Glass Top Bar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ChampagneGold)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "COLECCIÓN",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "$unlockedCount / $totalCount",
                                color = ChampagneGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // ── Premium Progress Bar ──
            AnimatedVisibility(
                visible = screenVisible,
                enter = fadeIn(tween(600)) + expandVertically(tween(600))
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "PROGRESO",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "${(progressPercent * 100).toInt()}%",
                            color = ChampagneGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MetalGrey)
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressPercent,
                            animationSpec = tween(1200, easing = EaseOutCubic),
                            label = "progress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ChampagneGold, NeonOrange)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Premium Filter Chips ──
            AnimatedVisibility(
                visible = screenVisible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { it / 3 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RarityFilterChip(
                        label = "TODOS",
                        color = TextSecondary,
                        selected = filterRarity == null,
                        onClick = { filterRarity = null }
                    )
                    Rarity.entries.forEach { rarity ->
                        RarityFilterChip(
                            label = rarity.label.uppercase(),
                            color = rarity.color,
                            selected = filterRarity == rarity,
                            onClick = { filterRarity = if (filterRarity == rarity) null else rarity }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Collectibles Grid ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayedCards, key = { _, card -> card.id }) { index, card ->
                    val isUnlocked = card.id in unlockedIds.value

                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 40L)
                        itemVisible = true
                    }

                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn(spring(dampingRatio = 0.7f)) +
                                scaleIn(spring(dampingRatio = 0.6f), initialScale = 0.8f)
                    ) {
                        PremiumCollectibleCard(
                            card = card,
                            isUnlocked = isUnlocked,
                            onClick = { selectedCard = card }
                        )
                    }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }

    selectedCard?.let { card ->
        PremiumCollectibleDialog(
            card = card,
            isUnlocked = card.id in unlockedIds.value,
            onDismiss = { selectedCard = null }
        )
    }
}

// ═══════════════════════════════════════════════════════
// 🎴 Premium Collectible Card
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumCollectibleCard(
    card: Collectible,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = card.rarity.color

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_${card.id}")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val isLegendary = card.rarity == Rarity.LEGENDARY && isUnlocked
    val isEpic = card.rarity == Rarity.EPIC && isUnlocked

    Box(
        modifier = Modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isLegendary) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            0f to Color(0xFFFFD700).copy(alpha = 0.3f + shimmerPhase * 0.5f),
                            0.25f to Color(0xFFFF6B35),
                            0.5f to Color(0xFFFFD700).copy(alpha = 0.3f + (1f - shimmerPhase) * 0.5f),
                            0.75f to Color(0xFFFFA726),
                            1f to Color(0xFFFFD700).copy(alpha = 0.3f + shimmerPhase * 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else if (isEpic) {
                    Modifier.border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(NeonPurple.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                } else if (isUnlocked) {
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(rarityColor.copy(alpha = 0.4f), rarityColor.copy(alpha = 0.08f))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier.border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                }
            )
            .background(
                if (isUnlocked) {
                    Brush.verticalGradient(
                        listOf(
                            rarityColor.copy(alpha = 0.08f),
                            AsphaltGrey.copy(alpha = 0.85f),
                            CarbonBlack.copy(alpha = 0.95f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF14141C).copy(alpha = 0.6f),
                            Color(0xFF0E0E14).copy(alpha = 0.8f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .drawWithContent {
                drawContent()
                if (isUnlocked) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.3f
                        )
                    )
                }
                if (isLegendary) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.06f * glowPulse),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height * 0.3f),
                            radius = size.width * 0.8f
                        )
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUnlocked && card.imageRes != null) {
                Image(
                    painter = painterResource(id = card.imageRes),
                    contentDescription = card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes up most of the card space
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(6.dp))
            } else {
                val emojiScale by animateFloatAsState(
                    targetValue = if (isUnlocked) 1f else 0.85f,
                    label = "emoji_scale"
                )
                Text(
                    text = if (isUnlocked) card.emoji else "❓",
                    fontSize = 38.sp,
                    modifier = Modifier
                        .scale(emojiScale)
                        .alpha(if (isUnlocked) 1f else 0.35f)
                )
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = if (isUnlocked) card.name else "???",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp,
                color = if (isUnlocked) TextPrimary else TextTertiary.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isUnlocked) rarityColor.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.04f)
                    )
                    .border(
                        0.5.dp,
                        if (isUnlocked) rarityColor.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = card.rarity.label.uppercase(),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = if (isUnlocked) rarityColor else TextTertiary.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Filter Chip
// ═══════════════════════════════════════════════════════

@Composable
private fun RarityFilterChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(if (selected) 0.2f else 0.05f, label = "chip_bg")
    val borderAlpha by animateFloatAsState(if (selected) 0.5f else 0.1f, label = "chip_border")
    val textAlpha by animateFloatAsState(if (selected) 1f else 0.5f, label = "chip_text")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = bgAlpha))
            .border(0.5.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = color.copy(alpha = textAlpha)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Premium Detail Dialog
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumCollectibleDialog(
    card: Collectible,
    isUnlocked: Boolean,
    onDismiss: () -> Unit
) {
    val rarityColor = card.rarity.color

    var dialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dialogVisible = true }

    val dialogScale by animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "dialog_scale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "dialog_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        alpha = dialogAlpha
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (card.rarity == Rarity.LEGENDARY && isUnlocked) {
                            Modifier.border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFFD700), Color(0xFFFF6B35),
                                        Color(0xFFFFD700), Color(0xFFFFA726), Color(0xFFFFD700)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                        } else {
                            Modifier.border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        rarityColor.copy(alpha = if (isUnlocked) 0.4f else 0.1f),
                                        rarityColor.copy(alpha = 0.05f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                        }
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A24), Color(0xFF12121A), Color(0xFF0A0A12))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.25f
                            )
                        )
                        if (isUnlocked) {
                            drawRect(
                                Brush.radialGradient(
                                    listOf(rarityColor.copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width / 2, size.height * 0.2f),
                                    radius = size.width * 0.6f
                                )
                            )
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Image with glow or Big emoji
                    Box(contentAlignment = Alignment.Center) {
                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .size(if (card.imageRes != null) 160.dp else 80.dp)
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(rarityColor.copy(alpha = 0.2f), Color.Transparent)
                                            ),
                                            radius = size.width
                                        )
                                    }
                            )
                        }
                        
                        if (isUnlocked && card.imageRes != null) {
                            Image(
                                painter = painterResource(id = card.imageRes),
                                contentDescription = card.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, rarityColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            )
                        } else {
                            Text(
                                text = if (isUnlocked) card.emoji else "🔒",
                                fontSize = 64.sp,
                                modifier = Modifier.alpha(if (isUnlocked) 1f else 0.4f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isUnlocked) card.name else "Cromo Bloqueado",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    // Rarity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(rarityColor.copy(alpha = 0.15f))
                            .border(0.5.dp, rarityColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            card.rarity.label.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = rarityColor
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    if (isUnlocked) {
                        Text(
                            card.description,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(card.unlockHint, textAlign = TextAlign.Center, fontSize = 13.sp, color = TextTertiary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        card.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextTertiary
                    )

                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MetalGrey.copy(alpha = 0.5f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 32.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "CERRAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Modelos
// ═══════════════════════════════════════════════════════

private enum class Rarity(val label: String, val color: Color) {
    COMMON("Común", Color(0xFF9E9E9E)),
    RARE("Raro", ElectricBlue),
    EPIC("Épico", NeonPurple),
    LEGENDARY("Legendario", ChampagneGold)
}

private data class Collectible(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val rarity: Rarity,
    val category: String,
    val unlockHint: String,
    val imageRes: Int? = null
)

private fun generateAllCollectibles(): List<Collectible> = listOf(
    Collectible("c01", "Fernando Alonso", "🏎️", "El Plan, Defensa Titánica en Pista", Rarity.LEGENDARY, "F1", "Escanea Aston Martin", com.georacing.georacing.R.drawable.card_alonso),
    Collectible("c02", "Lewis Hamilton", "🏎️", "Pilotaje maestro en Silverstone", Rarity.LEGENDARY, "F1", "Encuentra el casco", com.georacing.georacing.R.drawable.card_hamilton),
    Collectible("c03", "Max Verstappen", "🏎️", "Dominio absoluto bajo la lluvia", Rarity.LEGENDARY, "F1", "Escanea Red Bull", com.georacing.georacing.R.drawable.card_verstappen),
    Collectible("c04", "Marc Márquez", "🏍️", "Salvada imposible a 68º", Rarity.LEGENDARY, "MotoGP", "Visita curva 9", com.georacing.georacing.R.drawable.card_marquez),
    Collectible("c05", "Pecco Bagnaia", "🏍️", "Ritmo de campeón mundial", Rarity.LEGENDARY, "MotoGP", "Forza Ducati", com.georacing.georacing.R.drawable.card_bagnaia),
    Collectible("c06", "Marchador", "🚶", "Andaste 5.000 pasos en un evento", Rarity.COMMON, "Fitness", "Camina 5.000 pasos"),
    Collectible("c07", "Corredor", "🏃", "Andaste 10.000 pasos en un evento", Rarity.RARE, "Fitness", "Camina 10.000 pasos"),
    Collectible("c25", "Maratonista", "🥇", "Andaste 20.000 pasos en un evento", Rarity.EPIC, "Fitness", "Camina 20.000 pasos"),
    Collectible("c08", "Primera Foto", "📸", "Capturaste tu primer momento", Rarity.COMMON, "Social", "Toma una foto"),
    Collectible("c09", "Fotógrafo", "📷", "10 momentos capturados", Rarity.RARE, "Social", "Captura 10 momentos"),
    Collectible("c10", "Paparazzi", "🎬", "50 momentos capturados", Rarity.EPIC, "Social", "Captura 50 momentos"),
    Collectible("c11", "Primer Pedido", "🍔", "Hiciste tu primer pedido Click & Collect", Rarity.COMMON, "Foodie", "Haz un pedido"),
    Collectible("c12", "Gourmet", "🍽️", "Probaste 3 stands distintos", Rarity.RARE, "Foodie", "Pide en 3 stands distintos"),
    Collectible("c13", "Master Chef", "👨‍🍳", "Probaste todos los stands", Rarity.LEGENDARY, "Foodie", "Pide en todos los stands"),
    Collectible("c14", "VIP Access", "🎫", "Entraste al Paddock", Rarity.RARE, "VIP", "Accede al Paddock"),
    Collectible("c15", "Pit Lane", "🔧", "Visitaste la zona de Pit Lane", Rarity.EPIC, "VIP", "Visita Pit Lane"),
    Collectible("c16", "Eco Warrior", "🌱", "Ahorraste 1 kg CO₂ andando", Rarity.COMMON, "Eco", "Ahorra 1 kg CO₂"),
    Collectible("c17", "Planeta Verde", "🌎", "Ahorraste 5 kg CO₂", Rarity.RARE, "Eco", "Ahorra 5 kg CO₂"),
    Collectible("c18", "Nocturno", "🌙", "Estuviste en el circuito después de las 21h", Rarity.RARE, "Especial", "Visita al anochecer"),
    Collectible("c19", "Madrugador", "🌅", "Llegaste antes de las 8h", Rarity.RARE, "Especial", "Llega antes de las 8h"),
    Collectible("c20", "Bajo la Lluvia", "🌧️", "Asististe con lluvia", Rarity.EPIC, "Especial", "Asiste cuando llueva"),
    Collectible("c21", "Leyenda GeoRacing", "🏆", "Completaste toda la colección", Rarity.LEGENDARY, "Legendario", "Desbloquea todos los cromos"),
    Collectible("c22", "Fiel al Circuito", "❤️", "Asististe a 5 eventos", Rarity.LEGENDARY, "Legendario", "Asiste a 5 eventos"),
    Collectible("c23", "El Primero", "1️⃣", "Fuiste el primero en escanear un QR especial", Rarity.LEGENDARY, "Legendario", "Escanea QR especial el primero"),
    Collectible("c24", "Grupo Legendario", "👥", "Tu grupo completó un reto conjunto", Rarity.LEGENDARY, "Legendario", "Completa un reto grupal")
)
