package com.georacing.georacing.ui.screens.share

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.data.p2p.ProximityChatManager
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat de Proximidad — Premium Racing Edition.
 * P2P real via BLE + Nearby Connections.
 */
@Composable
fun ProximityChatScreen(
    proximityChatManager: ProximityChatManager,
    userName: String = "Usuario",
    userId: String = "user_${System.currentTimeMillis()}",
    onNavigateBack: () -> Unit = {}
) {
    val backdrop = LocalBackdrop.current
    val messages by proximityChatManager.messages.collectAsState()
    val nearbyUsers by proximityChatManager.nearbyUsers.collectAsState()
    val isActive by proximityChatManager.isActive.collectAsState()
    val connectionStatus by proximityChatManager.connectionStatus.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showQuickMessages by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (!isActive) proximityChatManager.start(userName, userId)
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            proximityChatManager.pruneOldMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Glass Top Bar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { proximityChatManager.stop(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // BLE Status dot
                        val pulseAnim = rememberInfiniteTransition(label = "ble_pulse")
                        val pulseAlpha by pulseAnim.animateFloat(
                            initialValue = 0.4f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                tween(1500), RepeatMode.Reverse
                            ), label = "pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) StatusGreen.copy(alpha = pulseAlpha)
                                    else TextTertiary.copy(alpha = 0.3f)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "CHAT PROXIMIDAD",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                connectionStatus.uppercase(),
                                color = if (nearbyUsers.isNotEmpty()) StatusGreen else TextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Nearby count badge
                    if (nearbyUsers.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(StatusGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, StatusGreen.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${nearbyUsers.size} cerca",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = StatusGreen
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )

            // ── Content ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    !isActive -> {
                        // Connecting state
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val rotateAnim = rememberInfiniteTransition(label = "scan")
                            val rotation by rotateAnim.animateFloat(
                                initialValue = 0f, targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                                label = "rotate"
                            )
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "BUSCANDO DISPOSITIVOS",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Conectando via Bluetooth...",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    messages.isEmpty() -> {
                        // Empty state
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NeonCyan.copy(alpha = 0.08f))
                                    .border(0.5.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "SIN MENSAJES",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (nearbyUsers.isNotEmpty())
                                    "Hay ${nearbyUsers.size} personas cerca — ¡sé el primero!"
                                else "Esperando a que alguien se acerque...",
                                color = TextTertiary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // BLE info banner
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeonCyan.copy(alpha = 0.06f))
                                        .border(0.5.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.BluetoothSearching,
                                            null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "P2P REAL VIA BLUETOOTH — ~50M ALCANCE",
                                            color = NeonCyan.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                            items(messages, key = { it.id }) { message ->
                                PremiumChatBubble(
                                    senderName = message.senderName,
                                    text = message.text,
                                    time = timeFormat.format(Date(message.timestamp)),
                                    isMe = message.isMe
                                )
                            }
                        }
                    }
                }
            }

            // ── Premium Quick Messages ──
            AnimatedVisibility(
                visible = showQuickMessages,
                enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.background(CarbonBlack.copy(alpha = 0.6f))
                ) {
                    items(proximityChatManager.quickMessages.size) { index ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AccentSocial.copy(alpha = 0.1f))
                                .border(0.5.dp, AccentSocial.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .clickable {
                                    proximityChatManager.sendQuickMessage(index)
                                    showQuickMessages = false
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                proximityChatManager.quickMessages[index],
                                fontSize = 12.sp,
                                color = AccentSocial,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Premium Input Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(0.dp), showBorder = false)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick messages toggle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (showQuickMessages) AccentSocial.copy(alpha = 0.2f)
                                else MetalGrey.copy(alpha = 0.3f)
                            )
                            .clickable { showQuickMessages = !showQuickMessages },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (showQuickMessages) Icons.Default.Close else Icons.Default.EmojiEmotions,
                            "Mensajes rápidos",
                            tint = if (showQuickMessages) AccentSocial else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Text input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.take(200) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Mensaje a los cercanos...",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        },
                        maxLines = 2,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = RacingRed.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = MetalGrey.copy(alpha = 0.2f),
                            unfocusedContainerColor = MetalGrey.copy(alpha = 0.15f),
                            cursorColor = RacingRed
                        ),
                        enabled = isActive
                    )

                    Spacer(Modifier.width(8.dp))

                    // Send button
                    val canSend = inputText.isNotBlank() && isActive
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) Brush.linearGradient(listOf(RacingRed, RacingRedBright))
                                else Brush.linearGradient(listOf(MetalGrey, PitLaneGrey))
                            )
                            .clickable(enabled = canSend) {
                                if (inputText.isNotBlank()) {
                                    proximityChatManager.sendMessage(inputText.trim())
                                    inputText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            "Enviar",
                            tint = if (canSend) Color.White else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Premium Chat Bubble
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumChatBubble(
    senderName: String,
    text: String,
    time: String,
    isMe: Boolean
) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                senderName.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = NeonCyan,
                modifier = Modifier.padding(start = 10.dp, bottom = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(
                    if (isMe) {
                        Brush.linearGradient(
                            listOf(
                                RacingRed.copy(alpha = 0.25f),
                                RacingRedDark.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MetalGrey.copy(alpha = 0.4f),
                                AsphaltGrey.copy(alpha = 0.3f)
                            )
                        )
                    },
                    bubbleShape
                )
                .border(
                    0.5.dp,
                    if (isMe) RacingRed.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.06f),
                    bubbleShape
                )
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.4f
                        )
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    time,
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
