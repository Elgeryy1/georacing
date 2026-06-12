package com.georacing.georacing.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.georacing.georacing.domain.orders.Order
import com.georacing.georacing.domain.orders.OrderLine
import com.georacing.georacing.domain.orders.OrderStatus
import com.georacing.georacing.ui.components.background.CarbonBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderStatusViewModel = viewModel()
) {
    val order by viewModel.order.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                         Text(
                            "ESTADO DEL PEDIDO",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadOrder(orderId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0A0A0A).copy(alpha = 0.9f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFEF4444))
                } else if (error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadOrder(orderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Reintentar")
                        }
                    }
                } else {
                    order?.let { currentOrder ->
                        OrderDetails(currentOrder)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetails(order: Order) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "PEDIDO #${order.orderId.take(8).uppercase()}", 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        StatusBadge(status = order.status)
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "EL PEDIDO CONTIENE:", 
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFA3A3A3),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(order.items) { item ->
                OrderLineItem(item)
            }
        }
        
        HorizontalDivider(color = Color(0xFF404040))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Text(
                "Total Pagado", 
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFA3A3A3)
             )
             Text(
                "€${String.format("%.2f", order.totalAmount)}", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun StatusBadge(status: OrderStatus) {
    val (bgColor, textColor, text) = when (status) {
        OrderStatus.PENDING -> Triple(Color(0xFF3F3F46), Color.White, "PROCESANDO...")
        OrderStatus.PAID -> Triple(Color(0xFF1D4ED8), Color.White, "PAGADO")
        OrderStatus.PREPARING -> Triple(Color(0xFFEAB308), Color.Black, "PREPARANDO")
        OrderStatus.READY -> Triple(Color(0xFF22C55E), Color.Black, "¡LISTO PARA RECOGER!")
        OrderStatus.DELIVERED -> Triple(Color(0xFF262626), Color(0xFFA3A3A3), "ENTREGADO")
        OrderStatus.CANCELLED -> Triple(Color(0xFFDC2626), Color.White, "CANCELADO")
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun OrderLineItem(item: OrderLine) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
         Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.productId, // En un caso real buscaríamos el nombre del producto
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Cantidad: ${item.quantity}", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA3A3A3)
                )
            }
            Text(
                text = "€${String.format("%.2f", item.unitPrice * item.quantity)}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
