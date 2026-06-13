package com.georacing.georacing.data.billing

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class PaymentResult {
    object Idle : PaymentResult()
    object Processing : PaymentResult()
    object Success : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}

/**
 * Simula una pasarela de pagos para propsitos de Demo.
 */
object FakePaymentProcessor {

    fun processPayment(amount: Double): Flow<PaymentResult> = flow {
        emit(PaymentResult.Processing)
        
        // Simular tiempo de conexión con banco (simulado)
        delay(2000)
        
        // Simular verificación de tarjeta (simulado)
        delay(1500)
        
        // Siempre éxito para la demo
        emit(PaymentResult.Success)
    }
}
