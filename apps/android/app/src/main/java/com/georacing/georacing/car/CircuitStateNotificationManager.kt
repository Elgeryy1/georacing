package com.georacing.georacing.car

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import com.georacing.georacing.R
import com.georacing.georacing.domain.model.CircuitMode

/**
 * Tier 1: Circuit State Notification Manager
 * 
 * Gestiona notificaciones Heads-Up (HUN) para cambios críticos de estado del circuito.
 * Las notificaciones aparecen en el dashboard del coche como alertas de alta prioridad.
 */
class CircuitStateNotificationManager(private val carContext: CarContext) {
    
    private val notificationManager = carContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "circuit_state_alerts"
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Circuit State Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for critical circuit state changes (Red Flag, Yellow Flag, etc.)"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Envía una alerta HUN al dashboard del coche para un cambio de estado del circuito.
     */
    fun sendCircuitStateAlert(newMode: CircuitMode) {
        val (title, message, icon) = getAlertContent(newMode)
        
        // No enviar notificación para estado normal
        if (title.isEmpty()) return
        
        val notification = NotificationCompat.Builder(carContext, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .extend(
                CarAppExtender.Builder()
                    .setImportance(NotificationManager.IMPORTANCE_HIGH)
                    .build()
            )
            .build()
        
        notificationManager.notify(CIRCUIT_STATE_NOTIFICATION_ID, notification)
        
        android.util.Log.i("CircuitNotification", "📢 HUN enviada: $title - $message")
    }
    
    /**
     * Retorna (Título, Mensaje, Icono) según el modo del circuito.
     */
    private fun getAlertContent(mode: CircuitMode): Triple<String, String, Int> {
        return when (mode) {
            CircuitMode.RED_FLAG -> Triple(
                "⚠️ CARRERA DETENIDA",
                "Mantenga la calma al llegar. Siga las instrucciones del personal.",
                android.R.drawable.stat_sys_warning // System warning icon
            )
            CircuitMode.YELLOW_FLAG -> Triple(
                "🟡 PRECAUCIÓN",
                "Reduzca la velocidad. Incidente en la pista.",
                android.R.drawable.stat_sys_warning
            )
            CircuitMode.SAFETY_CAR -> Triple(
                "🚗 SAFETY CAR",
                "Vehículo de seguridad en pista. Reduzca velocidad.",
                android.R.drawable.stat_notify_sync
            )
            CircuitMode.EVACUATION -> Triple(
                "🚨 EVACUACIÓN",
                "EVACUACIÓN INMEDIATA. Siga las señales de salida.",
                android.R.drawable.stat_sys_warning
            )
            else -> Triple("", "", 0) // No notification for normal states
        }
    }
    
    companion object {
        private const val CIRCUIT_STATE_NOTIFICATION_ID = 1001
    }
}
