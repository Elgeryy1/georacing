package com.georacing.georacing.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.orders.OrdersRepositoryImpl
import com.georacing.georacing.domain.orders.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val ordersRepository = OrdersRepositoryImpl()

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = ordersRepository.getOrder(orderId)
                if (result != null) {
                    _order.value = result
                } else {
                    _error.value = "Pedido no encontrado"
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar pedido: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
