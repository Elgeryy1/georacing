package com.georacing.georacing.domain.orders

interface OrdersRepository {
    suspend fun createOrder(
        userUid: String,
        items: List<OrderLine>,
        totalAmount: Double,
        paymentToken: String
    ): String


    suspend fun getOrder(orderId: String): Order?
}

// I need to make sure I don't break the contract the user expects.
// If I use OrderLine, I need to make sure I map it in the VM.
// OrderLine has (productId, quantity, unitPrice). That sounds exactly like what is needed.
