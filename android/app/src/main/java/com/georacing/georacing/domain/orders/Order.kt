package com.georacing.georacing.domain.orders

data class Order(
    val orderId: String,
    val status: OrderStatus,
    val totalAmount: Double,
    val items: List<OrderLine>,
    val createdAt: Long
)

data class OrderLine(
    @com.google.gson.annotations.SerializedName("product_id")
    val productId: String,
    @com.google.gson.annotations.SerializedName("quantity")
    val quantity: Int,
    @com.google.gson.annotations.SerializedName("unit_price")
    val unitPrice: Double
)

enum class OrderStatus {
    PENDING,
    PAID,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED
}
