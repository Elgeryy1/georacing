package com.georacing.georacing.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.georacing.georacing.MainActivity
import com.georacing.georacing.R
import com.georacing.georacing.domain.model.CircuitMode

/**
 * Manages "Live Activities" style notifications for Android 15 & Samsung One UI.
 */
class LiveNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Activity",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows live race status and navigation"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun buildNotification(mode: CircuitMode, message: String): Notification {
        // Intent to open App
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Categorize for System Priority (Samsung Capsule / Dynamic Island)
        val category = when (mode) {
            CircuitMode.EVACUATION, CircuitMode.RED_FLAG -> Notification.CATEGORY_ALARM // High priority, breaks DND
            else -> Notification.CATEGORY_NAVIGATION // Standard persistent status
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Replace with a solid white icon ideally
            .setContentTitle(getTitleForMode(mode))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(category)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(getColorForMode(mode))
            .setColorized(true) // 🟢 Critical for Samsung "NowBar" look
            .setUsesChronometer(true) // ⏱️ Triggers "time elapsed" style


        // Add Action Button
        val mapIntent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_MAP" // Handle this in MainActivity
        }
        val mapPendingIntent = PendingIntent.getActivity(
            context, 1, mapIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_mapmode, "Ver Mapa", mapPendingIntent)

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun updateNotification(notificationId: Int, mode: CircuitMode, message: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
             val notification = buildNotification(mode, message)
             notificationManager.notify(notificationId, notification)
        }
    }

    private fun getTitleForMode(mode: CircuitMode): String {
        return when (mode) {
            CircuitMode.NORMAL -> "🟢 Carrera en Curso"
            CircuitMode.GREEN_FLAG -> "🟢 BANDERA VERDE"
            CircuitMode.YELLOW_FLAG -> "🟡 BANDERA AMARILLA"
            CircuitMode.VSC -> "🟡 SAFETY CAR VIRTUAL"
            CircuitMode.SAFETY_CAR -> "🟡 Safety Car (SC)"
            CircuitMode.RED_FLAG -> "🔴 BANDERA ROJA"
            CircuitMode.EVACUATION -> "🆘 EVACUACIÓN"
            CircuitMode.UNKNOWN -> "GeoRacing"
        }
    }

    private fun getColorForMode(mode: CircuitMode): Int {
        return when (mode) {
            CircuitMode.NORMAL, CircuitMode.GREEN_FLAG -> android.graphics.Color.GREEN
            CircuitMode.YELLOW_FLAG, CircuitMode.VSC, CircuitMode.SAFETY_CAR -> android.graphics.Color.YELLOW
            CircuitMode.RED_FLAG, CircuitMode.EVACUATION -> android.graphics.Color.RED
            else -> android.graphics.Color.GRAY
        }
    }

    companion object {
        const val CHANNEL_ID = "live_activity_channel"
        const val NOTIFICATION_ID = 1001
    }
}
