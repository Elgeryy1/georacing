package com.georacing.georacing.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.georacing.georacing.R
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi

class OrderStatusWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) return Result.success()

        val api = FirestoreLikeClient.api
        val prefs = applicationContext.getSharedPreferences("geo_racing_orders", Context.MODE_PRIVATE)

        try {
            // Fetch orders for this user that are READY
            val result = api.get(
                FirestoreLikeApi.GetRequest(
                    table = "orders",
                    where = mapOf("user_uid" to user.uid, "status" to "READY")
                )
            )

            result.forEach { orderData ->
                 val orderId = orderData["order_id"] as? String ?: return@forEach
                 val wasNotified = prefs.getBoolean("notified_$orderId", false)

                 if (!wasNotified) {
                     showNotification()
                     // Mark as notified
                     prefs.edit().putBoolean("notified_$orderId", true).apply()
                 }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showNotification() {
        val context = applicationContext
        val channelId = "order_updates"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Order Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val resultIntent = android.content.Intent(context, com.georacing.georacing.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "my_orders")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, resultIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("¡Tu pedido está listo!")
            .setContentText("Pasa por la barra para recoger tu comida.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1001, notification)
    }
}
