package com.georacing.georacing.ui.orders

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.orders.OrdersRepositoryImpl
import com.georacing.georacing.data.products.ProductsRepositoryImpl
import com.georacing.georacing.domain.orders.OrderLine
import com.georacing.georacing.domain.products.Product
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

data class CartItem(
    val product: Product,
    var quantity: Int
)

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val productsRepository = ProductsRepositoryImpl()
    private val ordersRepository = OrdersRepositoryImpl()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Idle)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private lateinit var paymentsClient: PaymentsClient

    init {
        initializeGooglePay()
        loadProducts()
    }

    private fun initializeGooglePay() {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()
        paymentsClient = Wallet.getPaymentsClient(getApplication(), walletOptions)
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading
            try {
                val list = productsRepository.getAllProducts()
                _products.value = list
                _uiState.value = CartUiState.Idle
            } catch (e: Exception) {
                // Check for 500 error (Missing Table) either via HttpException or message text
                val is500 = (e is HttpException && e.code() == 500) || 
                            e.message?.contains("500") == true ||
                            e.message?.contains("Internal Server Error") == true

                if (is500) {
                    try {
                        seedDatabase()
                        val list = productsRepository.getAllProducts()
                        _products.value = list
                        _uiState.value = CartUiState.Idle
                    } catch (retryEx: Exception) {
                         _uiState.value = CartUiState.Error("Error seeding/loading: ${retryEx.message}")
                    }
                } else {
                    _uiState.value = CartUiState.Error("Error loading products: ${e.message}")
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        val demoProducts = listOf(
            Product("1", "Bocadillo Jamón", "Delicioso bocadillo de jamón serrano", 6.50, 100, "Comida", null, "🥪", true),
            Product("2", "Cerveza Estrella", "Cerveza fría 33cl", 5.00, 100, "Bebidas", null, "🍺", true),
            Product("3", "Hot Dog", "Perrito caliente con salsas", 5.50, 100, "Comida", null, "🌭", true),
            Product("4", "Agua 500ml", "Agua mineral natural", 2.50, 100, "Bebidas", null, "💧", true),
            Product("5", "Nachos con Queso", "Nachos crujientes con salsa de queso", 7.00, 100, "Comida", null, "🧀", true),
            Product("6", "Coca-Cola", "Refresco de cola 33cl", 3.50, 100, "Bebidas", null, "🥤", true),
            Product("7", "Gorra Oficial F1", "Gorra del equipo oficial", 35.00, 50, "Merchandising", null, "🧢", true),
            Product("8", "Camiseta Equipo", "Camiseta técnica oficial", 45.00, 50, "Merchandising", null, "👕", true)
        )
        demoProducts.forEach { productsRepository.upsertProduct(it) }
    }

    fun addToCart(product: Product) {
        val currentCart = _cartItems.value.toMutableList()
        val existingItem = currentCart.find { it.product.productId == product.productId }
        
        val currentQty = existingItem?.quantity ?: 0
        if (currentQty + 1 > product.stock) {
            Toast.makeText(getApplication(), "Sin stock suficiente", Toast.LENGTH_SHORT).show()
            return
        }

        if (existingItem != null) {
            existingItem.quantity++
        } else {
            currentCart.add(CartItem(product, 1))
        }
        _cartItems.value = currentCart
    }
    
    fun removeFromCart(product: Product) {
        val currentCart = _cartItems.value.toMutableList()
        val existingItem = currentCart.find { it.product.productId == product.productId }
        if (existingItem != null) {
            if (existingItem.quantity > 1) {
                existingItem.quantity--
            } else {
                currentCart.remove(existingItem)
            }
            _cartItems.value = currentCart
        }
    }
    
    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getGooglePayPaymentDataRequest(price: Double): JSONObject {
        // Simple fixed configuration for MVP
        val baseRequest = JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }
        
        val tokenizationSpecification = JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", "example")
                put("gatewayMerchantId", "exampleGatewayMerchantId")
            })
        }

        val allowedCardNetworks = JSONArray(listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA"))
        val allowedAuthMethods = JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))

        val cardPaymentMethod = JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", allowedAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
            })
            put("tokenizationSpecification", tokenizationSpecification)
        }

        val paymentDataRequest = JSONObject(baseRequest.toString()).apply {
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", JSONObject().apply {
                put("totalPriceStatus", "FINAL")
                put("totalPrice", String.format("%.2f", price).replace(",", "."))
                put("currencyCode", "EUR") // Assuming EUR
            })
            put("merchantInfo", JSONObject().apply {
                put("merchantName", "GeoRacing Test")
            })
        }
        
        return paymentDataRequest
    }

    fun createOrder(paymentToken: String) {
        viewModelScope.launch {
            _uiState.value = CartUiState.Processing
            try {
                val cart = _cartItems.value
                val total = cart.sumOf { it.product.price * it.quantity }
                
                // Map to OrderLine
                val orderLines = cart.map {
                    OrderLine(
                        productId = it.product.productId,
                        quantity = it.quantity,
                        unitPrice = it.product.price
                    )
                }
                
                // Temp user ID
                val userId = "test_user_uid" 
                
                val orderId = ordersRepository.createOrder(
                    userUid = userId,
                    items = orderLines,
                    totalAmount = total,
                    paymentToken = paymentToken
                )
                
                // Decrease stock logic (Fire and forget as requested)
                cart.forEach { item ->
                    val newStock = item.product.stock - item.quantity
                    productsRepository.updateProductStock(item.product.productId, newStock)
                }
                
                _cartItems.value = emptyList()
                _uiState.value = CartUiState.OrderPlaced(orderId)
                
            } catch (e: Exception) {
                _uiState.value = CartUiState.Error("Error creating order: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = CartUiState.Idle
    }
}

sealed class CartUiState {
    object Idle : CartUiState()
    object Loading : CartUiState()
    object Processing : CartUiState()
    data class OrderPlaced(val orderId: String) : CartUiState()
    data class Error(val message: String) : CartUiState()
}
