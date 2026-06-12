package com.georacing.georacing.ui.screens.orders

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class OrderItem(
    val productId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double
)

data class OrderTicket(
    val id: String,
    val orderId: String,
    val status: String,
    val itemsSummary: String,
    val items: List<OrderItem>,
    val total: Double,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    var orders by remember { mutableStateOf<List<OrderTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (user == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val api = FirestoreLikeClient.api
                val result = api.get(
                    FirestoreLikeApi.GetRequest(
                        table = "orders",
                        where = mapOf("user_uid" to user.uid)
                    )
                )
                
                // Sort by date desc (assuming generic string sort works for ISO dates, or we parse)
                // We'll trust backend order or sort client side
                
                orders = result.mapNotNull {
                    try {
                        val itemsJson = it["items_json"] as? String ?: "[]"
                        // Parse JSON into individual items
                        var summaryText = ""
                        val parsedItems = mutableListOf<OrderItem>()
                        try {
                             val jsonArray = JSONArray(itemsJson)
                             for (i in 0 until jsonArray.length()) {
                                 val obj = jsonArray.getJSONObject(i)
                                 val qty = obj.optInt("quantity", 1)
                                 val productId = obj.optString("product_id", "")
                                 val name = obj.optString("name", "Producto #${productId.take(6)}")
                                 val unitPrice = obj.optDouble("unit_price", 0.0)
                                 parsedItems.add(OrderItem(productId, name, qty, unitPrice))
                             }
                             summaryText = "${parsedItems.size} artículos"
                        } catch (e: Exception) { summaryText = "Varios productos" }

                        OrderTicket(
                            id = it["id"] as? String ?: "",
                            orderId = it["order_id"] as? String ?: "???",
                            status = it["status"] as? String ?: "PENDING",
                            itemsSummary = summaryText,
                            items = parsedItems,
                            total = (it["total_amount"] as? Number)?.toDouble() ?: 0.0,
                            createdAt = it["created_at"] as? String ?: ""
                        )
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()
        
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .glassSmall(shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color(0xFFF8FAFC))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE8253A), shape = RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MIS PEDIDOS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE8253A))
                }
            } else if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes pedidos recientes", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Separate Ready from Others
                    val readyOrders = orders.filter { it.status == "READY" }
                    val activeOrders = orders.filter { it.status == "PAID" || it.status == "PREPARING" }
                    val history = orders.filter { it.status == "DELIVERED" }

                    if (readyOrders.isNotEmpty()) {
                        item { Header("LISTO PARA RECOGER") }
                        items(readyOrders) { OrderCard(it, true) }
                    }
                    
                    if (activeOrders.isNotEmpty()) {
                         item { Header("EN PREPARACIÓN") }
                         items(activeOrders) { OrderCard(it, false) }
                    }
                    
                    if (history.isNotEmpty()) {
                        item { Header("HISTORIAL") }
                        items(history) { OrderCard(it, false) }
                    }
                }
            }
        }
    }
}

@Composable
fun Header(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFFE8253A), shape = RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.5.sp
            ),
            color = Color(0xFF64748B),
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun OrderCard(order: OrderTicket, isReady: Boolean) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chevron"
    )

    val borderColor = when {
        isReady -> Color(0xFF22C55E)
        order.status == "PREPARING" -> Color(0xFFFFA726) // Amber
        order.status == "PAID" -> Color(0xFFD4A84B) // Gold
        else -> Color(0xFF64748B).copy(alpha=0.3f)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReady) "TICKET DE RECOGIDA" else "PEDIDO #${order.orderId.takeLast(4)}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = if(isReady) Color(0xFF22C55E) else Color(0xFF64748B),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "#${order.orderId.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isReady) {
                        Box(modifier = Modifier.background(Color(0xFF22C55E), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("LISTO", color = Color(0xFF080810), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    } else {
                        val statusText = when(order.status) {
                            "PAID" -> "EN COLA"
                            "PREPARING" -> "COCINANDO..."
                            "DELIVERED" -> "ENTREGADO"
                            else -> order.status
                        }
                        val statusColor = if(order.status == "PREPARING") Color(0xFFFFA726) else Color(0xFF64748B)
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 1.sp),
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Contraer" else "Expandir",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text(order.itemsSummary, color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold)
                 Text("€${"%.2f".format(order.total)}", color = Color(0xFFE8253A), fontWeight = FontWeight.ExtraBold)
            }
            
            // ── Expandable Items (iOS parity: OrderRowView) ──
            AnimatedVisibility(
                visible = isExpanded && order.items.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFF64748B).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${item.quantity}x",
                                color = Color(0xFFFFA726),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                item.name,
                                color = Color(0xFFF8FAFC),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "€${"%.2f".format(item.unitPrice * item.quantity)}",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            if (isReady) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Muestra este código en la barra",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
