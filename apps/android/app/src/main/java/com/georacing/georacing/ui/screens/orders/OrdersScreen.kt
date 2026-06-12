package com.georacing.georacing.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.billing.BillingManager
import com.georacing.georacing.data.billing.FakePaymentProcessor
import com.georacing.georacing.data.billing.PaymentResult
import com.georacing.georacing.ui.components.background.CarbonBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.data.orders.OrdersRepositoryImpl
import com.georacing.georacing.domain.orders.OrderLine
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val iconEmoji: String,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Dynamic Products State
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    // ... [existing fetching logic] ...
    
    // Fetch Products from Backend
    LaunchedEffect(Unit) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val api = com.georacing.georacing.data.firestorelike.FirestoreLikeClient.api
                val rawProducts = api.get(com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(table = "products", where = null))
                
                val fetched = rawProducts.mapNotNull { it ->
                    try {
                        Product(
                            id = it["id"] as? String ?: return@mapNotNull null,
                            name = it["name"] as? String ?: "Producto",
                            price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                            category = it["category"] as? String ?: "General",
                            iconEmoji = it["emoji"] as? String ?: "📦",
                            isAvailable = when(val stock = it["in_stock"]) {
                                is Boolean -> stock
                                is Number -> stock.toInt() == 1
                                else -> true 
                            }
                        )
                    } catch(e: Exception) { null }
                }
                
                // Update UI on Main Thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    products = fetched
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    var cart by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    val total = cart.entries.sumOf { (id, qty) ->
        products.find { it.id == id }?.price?.times(qty) ?: 0.0
    }

    // Billing Integration (Kept for reference but bypassed for Demo)
    val billingResult by BillingManager.billingFlowResult.collectAsState()
    val purchases by BillingManager.purchases.collectAsState()
    
    // Payment State
    var isProcessingPayment by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Conectando con banco...") }

    // Repository
    val repository = remember { OrdersRepositoryImpl() }
    val userUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous" }

    // Function to process payment
    fun processCheckout() {
        scope.launch {
            isProcessingPayment = true
            FakePaymentProcessor.processPayment(total).collect { result ->
                when (result) {
                     is PaymentResult.Processing -> {
                         processingMessage = "Procesando pago..."
                     }
                     is PaymentResult.Success -> {
                         processingMessage = "¡Pago Completado!"
                         // Create Order locally
                         val currentCart = cart.toMap()
                         val orderLines = currentCart.mapNotNull { (id, qty) ->
                            val product = products.find { it.id == id }
                            if (product != null) {
                                OrderLine(product.id, qty, product.price)
                            } else null
                         }
                         
                         val orderId = java.util.UUID.randomUUID().toString()
                         // Fire and forget save (demo)
                         launch {
                             repository.createOrder(
                                 userUid = userUid,
                                 items = orderLines,
                                 totalAmount = total,
                                 paymentToken = "DEMO_TOKEN_$orderId"
                             )
                         }
                         
                         cart.clear()
                         isProcessingPayment = false
                         // Navigate to Confirmation
                         navController.navigate("order_confirmation/$orderId")
                     }
                     is PaymentResult.Error -> {
                         isProcessingPayment = false
                         // Show error snackbar logic here if needed
                     }
                     else -> {}
                }
            }
        }
    }

    // Old Billing LaunchedEffect removed as we use FakeProcessor for Demo
    
    Box(modifier = Modifier.fillMaxSize()) {
        OrdersScreenContent(
            products = products,
            cart = cart,
            total = total,
            onAddProduct = { product -> cart = cart.toMutableMap().apply { this[product.id] = (this[product.id] ?: 0) + 1 } },
            onRemoveProduct = { product -> 
                cart = cart.toMutableMap().apply { 
                    val currentQty = this[product.id] ?: 0
                    if (currentQty > 1) this[product.id] = currentQty - 1 else remove(product.id) 
                } 
            },
            onCheckout = { processCheckout() },
            onNavigateBack = { navController.navigateUp() },
            onNavigateMyOrders = { navController.navigate(com.georacing.georacing.ui.navigation.Screen.MyOrders.route) },
            showSuccessDialog = false, // We use navigation now
            onDismissSuccessDialog = { } 
        )
        
        // Processing Overlay
        if (isProcessingPayment) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080810).copy(alpha = 0.9f))
                    .clickable(enabled = false) {}, // content blocker
                contentAlignment = Alignment.Center
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     CircularProgressIndicator(color = Color(0xFFE8253A), modifier = Modifier.size(48.dp))
                     Spacer(modifier = Modifier.height(24.dp))
                     Text(
                         text = processingMessage,
                         style = MaterialTheme.typography.titleMedium,
                         color = Color(0xFFF8FAFC),
                         fontWeight = FontWeight.ExtraBold,
                         letterSpacing = 1.sp
                     )
                 }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreenContent(
    products: List<Product>,
    cart: Map<String, Int>,
    total: Double,
    onAddProduct: (Product) -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onCheckout: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateMyOrders: () -> Unit,
    showSuccessDialog: Boolean,
    onDismissSuccessDialog: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()
        
        Scaffold(
            topBar = {
                // Custom Glass Top Bar
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
                                text = "PEDIDOS EXPRESS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color(0xFFF8FAFC)
                            )
                        }
                        
                        IconButton(onClick = onNavigateMyOrders) {
                             Icon(androidx.compose.material.icons.Icons.Default.Receipt, "Mis Pedidos", tint = Color(0xFFF8FAFC))
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            bottomBar = {
                if (cart.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding() // Proper inset handling
                            .liquidGlass(shape = RoundedCornerShape(24.dp), level = GlassLevel.L2)
                            .padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL A PAGAR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "€%.2f".format(total),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF22C55E)
                                )
                            }
                            
                            Button(
                                onClick = onCheckout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8253A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito de compras", tint = Color(0xFFF8FAFC))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PAGAR", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFFF8FAFC))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            var isRefreshing by remember { mutableStateOf(false) }
            val refreshScope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    refreshScope.launch { delay(1500); isRefreshing = false }
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp) // Content padding
            ) {
                 item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8253A))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TU MENÚ",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }
                
                items(products) { product ->
                    val qty = cart[product.id] ?: 0
                    ProductGlassCard(
                        product = product,
                        quantity = qty,
                        onAdd = { onAddProduct(product) },
                        onRemove = { onRemoveProduct(product) }

                    )
                }
            }
        }
        
        // Success Dialog
        if (showSuccessDialog) {
             AlertDialog(
                onDismissRequest = onDismissSuccessDialog,
                containerColor = Color(0xFF14141C),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Pedido confirmado", tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "¡Pedido Realizado!",
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                text = {
                    Text(
                        "Tu pedido se ha enviado a cocina. Recibirás una notificación cuando esté listo.",
                        color = Color(0xFF64748B)
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissSuccessDialog) {
                        Text("ENTENDIDO", color = Color(0xFFE8253A), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                },
                modifier = Modifier.border(1.dp, Color(0xFF22C55E), RoundedCornerShape(24.dp))
            )
        }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    val dummyProducts = listOf(
        Product("1", "Hamburguesa Geo", 12.50, "Food", "🍔", true),
        Product("2", "Cola Zero", 3.00, "Drink", "🥤", true),
        Product("3", "Papas Fritas", 4.50, "Side", "🍟", false)
    )
    val dummyCart = mapOf("1" to 2, "2" to 1)
    
    GeoRacingTheme {
        OrdersScreenContent(
            products = dummyProducts,
            cart = dummyCart,
            total = 28.0,
            onAddProduct = {},
            onRemoveProduct = {},
            onCheckout = {},
            onNavigateBack = {},
            onNavigateMyOrders = {},
            showSuccessDialog = false,
            onDismissSuccessDialog = {}
        )
    }
}

@Composable
fun ProductGlassCard(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val isAvailable = product.isAvailable
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
            .alpha(if (isAvailable) 1f else 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon & Info
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAvailable) Color(0xFFF8FAFC).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.iconEmoji, fontSize = 24.sp, modifier = Modifier.then(if(!isAvailable) Modifier.alpha(0.5f) else Modifier))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAvailable) Color(0xFFF8FAFC) else Color(0xFF64748B)
                    )
                    if (isAvailable) {
                        Text(
                            text = "€%.2f".format(product.price),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE8253A),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "AGOTADO",
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 1.5.sp),
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                 if (quantity > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color(0xFF64748B), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Quitar uno", tint = Color(0xFFF8FAFC), modifier = Modifier.size(16.dp))
                    }
                    
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF8FAFC)
                    )
                 }
                
                IconButton(
                    onClick = { if (isAvailable) onAdd() },
                    enabled = isAvailable,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (isAvailable) Color(0xFFE8253A) else Color(0xFF64748B), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir uno", tint = Color(0xFFF8FAFC), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}



