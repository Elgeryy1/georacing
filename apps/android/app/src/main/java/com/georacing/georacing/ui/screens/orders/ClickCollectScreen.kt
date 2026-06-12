package com.georacing.georacing.ui.screens.orders

import android.location.Location
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
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
    onNavigateToMyOrders: () -> Unit = {}
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

    // Simular progreso del pedido activo — En producción, esto se consultaría al backend
    LaunchedEffect(activeOrder) {
        if (activeOrder != null && activeOrder?.status != CollectOrderStatus.DELIVERED) {
            delay(15_000)
            activeOrder = activeOrder?.copy(status = CollectOrderStatus.PREPARING)
            // Guardar estado en backend
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Click & Collect") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Botón para ver mis pedidos
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        IconButton(onClick = onNavigateToMyOrders) {
                            Icon(Icons.Default.Receipt, "Mis pedidos")
                        }
                    }
                }
            )
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
            Text(
                text = "📍 Puntos de recogida",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // ── Filtros rápidos ──
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("🍔 Todo", "🍕 Comida", "🍺 Bebidas", "🍦 Dulces", "⚡ Rápido")
                items(filters) { filter ->
                    FilterChip(
                        onClick = { /* Filter logic */ },
                        label = { Text(filter) },
                        selected = filter == "🍔 Todo"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Lista de stands ──
            stands.forEach { stand ->
                StandCard(
                    stand = stand,
                    isSelected = selectedStand?.id == stand.id,
                    onSelect = { selectedStand = stand },
                    onOrder = {
                        // Simular crear pedido (en producción → navegar a OrdersScreen)
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

            Spacer(modifier = Modifier.height(80.dp))
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

// ── Componentes ──

@Composable
private fun StandCard(
    stand: FoodStand,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOrder: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (stand.isOpen)
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji grande
            Text(
                text = stand.name.take(2),
                fontSize = 32.sp,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stand.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!stand.isOpen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CERRADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = stand.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tiempo espera
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (stand.waitMinutes > 10)
                                MaterialTheme.colorScheme.error
                            else Color(0xFF4CAF50)
                        )
                        Text(
                            text = " ${stand.waitMinutes} min",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFFC107)
                        )
                        Text(
                            text = " ${stand.rating}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    // Ubicación
                    Text(
                        text = "📍 ${stand.zone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botón pedir
            if (stand.isOpen) {
                FilledTonalButton(
                    onClick = onOrder,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Pedir")
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
        CollectOrderStatus.CONFIRMED -> Color(0xFF2196F3)
        CollectOrderStatus.PREPARING -> Color(0xFFFF9800)
        CollectOrderStatus.READY -> Color(0xFF4CAF50)
        CollectOrderStatus.DELIVERED -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onTrack() },
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.statusText(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.pickupCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stand?.name ?: order.standName} • ~${order.estimatedMinutes} min",
                style = MaterialTheme.typography.bodySmall
            )

            if (order.status == CollectOrderStatus.READY) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTrack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Place, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir a recoger")
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.outlineVariant
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
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Código de recogida grande
            Text("Código de recogida", style = MaterialTheme.typography.labelMedium)
            Text(
                text = order.pickupCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info del stand
            if (stand != null) {
                Text("📍 ${stand.name}", style = MaterialTheme.typography.titleMedium)
                Text(stand.zone, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (order.status == CollectOrderStatus.READY) {
                Button(
                    onClick = onCollected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✅ Ya lo tengo")
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
