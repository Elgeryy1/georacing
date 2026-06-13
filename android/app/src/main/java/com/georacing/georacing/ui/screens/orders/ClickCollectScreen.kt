package com.georacing.georacing.ui.screens.orders

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Pantalla Click & Collect — Pedir comida y recoger en un punto del circuito.
 *
 * Flujo:
 * 1. Usuario ve los puntos de recogida (stands) con tiempo estimado
 * 2. Selecciona un stand → se abre el menú (OrdersScreen)
 * 3. Tras pedir, vuelve aquí con tracking en vivo del pedido
 * 4. Cuando está listo → se muestra la dirección para ir al stand
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickCollectScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToOrders: (String) -> Unit = {}, // standId
    onNavigateToMyOrders: () -> Unit = {},
    onNavigateHome: () -> Unit = {}
) {
    // ── Stands del circuito — datos reales del backend ──
    var stands by remember { mutableStateOf<List<FoodStand>>(emptyList()) }
    var isLoadingStands by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = FirestoreLikeClient.api.read("food_stands")
            stands = response.mapNotNull { map ->
                try {
                    FoodStand(
                        id = map["id"]?.toString() ?: return@mapNotNull null,
                        name = map["name"]?.toString() ?: "",
                        description = map["description"]?.toString() ?: "",
                        latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                        zone = map["zone"]?.toString() ?: "",
                        waitMinutes = (map["waitMinutes"] as? Number)?.toInt() ?: 10,
                        rating = (map["rating"] as? Number)?.toFloat() ?: 4.0f,
                        isOpen = map["isOpen"] as? Boolean ?: true
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            Log.w("ClickCollectScreen", "Error cargando stands: ${e.message}. Sin datos de stands.")
            stands = emptyList()
        }
        isLoadingStands = false
    }

    // ── Estado ──
    var selectedStand by remember { mutableStateOf<FoodStand?>(null) }
    var activeOrder by remember { mutableStateOf<CollectOrder?>(null) }
    var showOrderTracker by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("🍔 Todo") }

    // Simular progreso del pedido activo
    LaunchedEffect(activeOrder) {
        if (activeOrder != null && activeOrder?.status != CollectOrderStatus.DELIVERED) {
            delay(15_000)
            activeOrder = activeOrder?.copy(status = CollectOrderStatus.PREPARING)
            try {
                activeOrder?.let {
                    FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                        table = "orders",
                        data = mapOf("id" to it.orderId, "standId" to it.standId, "status" to it.status.name, "pickupCode" to it.pickupCode)
                    ))
                }
            } catch (_: Exception) {}
            delay(20_000)
            activeOrder = activeOrder?.copy(status = CollectOrderStatus.READY)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Racing-style glass top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .glassSmall(shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color(0xFFF8FAFC))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8253A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLICK & COLLECT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color(0xFFF8FAFC)
                            )
                        }

                        IconButton(onClick = onNavigateToMyOrders) {
                            Icon(Icons.Default.Receipt, "Mis pedidos", tint = Color(0xFFF8FAFC))
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Pedido activo ──
                AnimatedVisibility(visible = activeOrder != null) {
                    activeOrder?.let { order ->
                        ActiveOrderCard(
                            order = order,
                            stand = stands.find { it.id == order.standId },
                            onTrack = { showOrderTracker = true }
                        )
                    }
                }

                // ── Header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint = Color(0xFFE8253A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PUNTOS DE RECOGIDA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFFF8FAFC)
                    )
                }

                // ── Filtros racing ──
                val filters = listOf("🍔 Todo", "🍕 Comida", "🍺 Bebidas", "🍦 Dulces", "⚡ Rápido")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = filter == selectedFilter
                        val bgAlpha = if (isSelected) 0.2f else 0.05f
                        val accentColor = if (isSelected) Color(0xFFE8253A) else TextSecondary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = bgAlpha))
                                .border(
                                    0.5.dp,
                                    accentColor.copy(alpha = if (isSelected) 0.5f else 0.1f),
                                    RoundedCornerShape(50)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                filter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFF8FAFC) else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Loading state ──
                if (isLoadingStands) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFE8253A),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (stands.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .liquidGlass(shape = RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍽️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No hay stands disponibles",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // ── Lista de stands ──
                    stands.forEach { stand ->
                        StandCard(
                            stand = stand,
                            isSelected = selectedStand?.id == stand.id,
                            onSelect = { selectedStand = stand },
                            onOrder = {
                                activeOrder = CollectOrder(
                                    orderId = "ORD-${System.currentTimeMillis() % 10000}",
                                    standId = stand.id,
                                    standName = stand.name,
                                    status = CollectOrderStatus.CONFIRMED,
                                    estimatedMinutes = stand.waitMinutes + 5,
                                    pickupCode = "GR-${(100..999).random()}"
                                )
                                onNavigateToOrders(stand.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ── Bottom Sheet: Tracker ──
        if (showOrderTracker && activeOrder != null) {
            OrderTrackerSheet(
                order = activeOrder!!,
                stand = stands.find { it.id == activeOrder!!.standId },
                onDismiss = { showOrderTracker = false },
                onCollected = {
                    activeOrder = activeOrder?.copy(status = CollectOrderStatus.DELIVERED)
                    showOrderTracker = false
                }
            )
        }
    }
}

// ── Componentes ──

@Composable
private fun StandCard(
    stand: FoodStand,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOrder: () -> Unit
) {
    val accentColor = if (isSelected) Color(0xFFE8253A) else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                accentGlow = accentColor
            )
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    Color(0xFFE8253A).copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .clickable { onSelect() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (stand.isOpen) Color(0xFFF8FAFC).copy(alpha = 0.08f)
                        else Color(0xFFEF4444).copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(stand.name.take(2), fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stand.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF8FAFC)
                    )
                    if (!stand.isOpen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CERRADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Text(
                    text = stand.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tiempo espera
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (stand.waitMinutes > 10)
                                Color(0xFFEF4444) else Color(0xFF22C55E)
                        )
                        Text(
                            text = " ${stand.waitMinutes}'",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFD4A855)
                        )
                        Text(
                            text = " ${stand.rating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Zona
                    Text(
                        text = "📍 ${stand.zone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            // Botón pedir
            if (stand.isOpen) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOrder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8253A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "PEDIR",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp,
                        color = Color(0xFFF8FAFC)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(
    order: CollectOrder,
    stand: FoodStand?,
    onTrack: () -> Unit
) {
    val statusColor = when (order.status) {
        CollectOrderStatus.CONFIRMED -> ElectricBlue
        CollectOrderStatus.PREPARING -> Color(0xFFFFA726)
        CollectOrderStatus.READY -> Color(0xFF22C55E)
        CollectOrderStatus.DELIVERED -> TextTertiary
    }

    // Pulse animation for ready state
    val infiniteTransition = rememberInfiniteTransition(label = "order_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                accentGlow = statusColor
            )
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onTrack() }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(
                            alpha = if (order.status == CollectOrderStatus.READY) pulseAlpha else 1f
                        ))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = order.statusText(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                // Pickup code badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.pickupCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC),
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stand?.name ?: order.standName} · ~${order.estimatedMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (order.status == CollectOrderStatus.READY) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTrack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF080810))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "IR A RECOGER",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF080810)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderTrackerSheet(
    order: CollectOrder,
    stand: FoodStand?,
    onDismiss: () -> Unit,
    onCollected: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141C),
        scrimColor = Color(0xFF080810).copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status steps
            val steps = listOf("Confirmado", "Preparando", "¡Listo!")
            val currentStep = when (order.status) {
                CollectOrderStatus.CONFIRMED -> 0
                CollectOrderStatus.PREPARING -> 1
                CollectOrderStatus.READY, CollectOrderStatus.DELIVERED -> 2
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep) Color(0xFF22C55E)
                                    else Color(0xFF1E1E2A)
                                )
                                .then(
                                    if (index > currentStep) Modifier.border(
                                        1.dp, Color(0xFF64748B).copy(alpha = 0.3f), CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < currentStep) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    color = if (index <= currentStep) Color.White else TextTertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index <= currentStep) Color(0xFFF8FAFC) else TextTertiary,
                            fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Código de recogida
            Text(
                "CÓDIGO DE RECOGIDA",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = order.pickupCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE8253A),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stand info
            if (stand != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint = Color(0xFFE8253A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stand.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    stand.zone,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (order.status == CollectOrderStatus.READY) {
                Button(
                    onClick = onCollected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "✅ YA LO TENGO",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF080810)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Modelos locales ──

private data class FoodStand(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val zone: String,
    val waitMinutes: Int,
    val rating: Float,
    val isOpen: Boolean
)

private data class CollectOrder(
    val orderId: String,
    val standId: String,
    val standName: String,
    val status: CollectOrderStatus,
    val estimatedMinutes: Int,
    val pickupCode: String
) {
    fun statusText(): String = when (status) {
        CollectOrderStatus.CONFIRMED -> "Pedido confirmado"
        CollectOrderStatus.PREPARING -> "En preparación..."
        CollectOrderStatus.READY -> "¡Listo para recoger!"
        CollectOrderStatus.DELIVERED -> "Recogido ✓"
    }
}

private enum class CollectOrderStatus {
    CONFIRMED, PREPARING, READY, DELIVERED
}
