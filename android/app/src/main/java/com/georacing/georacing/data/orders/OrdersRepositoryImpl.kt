package com.georacing.georacing.data.orders

import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.orders.Order
import com.georacing.georacing.domain.orders.OrderLine
import com.georacing.georacing.domain.orders.OrdersRepository
import com.georacing.georacing.domain.orders.OrderStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class OrdersRepositoryImpl : OrdersRepository {

    private val api = FirestoreLikeClient.api
    private val gson = Gson()

    override suspend fun createOrder(
        userUid: String,
        items: List<OrderLine>,
        totalAmount: Double,
        paymentToken: String
    ): String {
        val orderId = UUID.randomUUID().toString()
        val itemsJson = gson.toJson(items)
        val currentTime = System.currentTimeMillis()

        // Use ISO 8601 string for timestamps to avoid Integer Overflow in backend (MariaDB INT vs BIGINT)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timeString = sdf.format(java.util.Date(currentTime))

        val data = mapOf(
            "order_id" to orderId,
            "user_uid" to userUid,
            "status" to "PAID", // Starting as PAID since we just got the token
            "items_json" to itemsJson,
            "total_amount" to totalAmount,
            "platform" to "ANDROID",
            "payment_token" to paymentToken,
            "created_at" to timeString, 
            "updated_at" to timeString
        )

        api.upsert(
            com.georacing.georacing.data.firestorelike.FirestoreLikeApi.UpsertRequest(
                table = "orders",
                data = data
            )
        )

        return orderId
    }

    override suspend fun getOrder(orderId: String): Order? {
        val result = api.get(
            com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(
                table = "orders",
                where = mapOf("order_id" to orderId)
            )
        ).firstOrNull() ?: return null

        return try {
            val statusStr = result["status"] as? String ?: "PENDING"
            val status = try {
                OrderStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                OrderStatus.PENDING
            }

            val itemsJson = result["items_json"] as? String ?: "[]"
            val itemsType = object : TypeToken<List<OrderLine>>() {}.type
            val items: List<OrderLine> = gson.fromJson(itemsJson, itemsType)

            Order(
                orderId = result["order_id"] as? String ?: orderId,
                status = status,
                totalAmount = (result["total_amount"] as? Number)?.toDouble() ?: 0.0,
                items = items,
                createdAt = (result["created_at"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
