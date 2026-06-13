package com.georacing.georacing.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.georacing.georacing.MainActivity
import com.georacing.georacing.R
import com.georacing.georacing.domain.model.EmergencyType
import com.georacing.georacing.domain.model.LiveSessionState

/**
 * Factory for creating "Live Activity" style notifications.
 * 
 * Samsung NowBar Hack:
 * - CATEGORY_NAVIGATION: Triggers the green status bar pill.
 * - CATEGORY_ALARM: For emergencies, bypasses DND.
 * - setColorized(true): Forces color background on Samsung.
 */
class NotificationFactory(private val context: Context) {

    companion object {
        const val CHANNEL_ID_NAVIGATION = "live_navigation_channel"
        const val CHANNEL_ID_EMERGENCY = "live_emergency_channel"
        const val NOTIFICATION_ID = 3001
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // Navigation Channel (Medium Priority, Silent Updates)
            val navChannel = NotificationChannel(
                CHANNEL_ID_NAVIGATION,
                "Live Navigation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time navigation updates"
                setShowBadge(false)
                setSound(null, null)
            }

            // Emergency Channel (Max Priority, Bypass DND)
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical safety alerts"
                enableVibration(true)
                setBypassDnd(true)
            }

            manager.createNotificationChannels(listOf(navChannel, emergencyChannel))
        }
    }

    /**
     * Builds a notification based on the current session state.
     */
    fun build(state: LiveSessionState): Notification {
        return when (state) {
            is LiveSessionState.Idle -> buildIdleNotification()
            is LiveSessionState.Navigation -> buildNavigationNotification(state)
            is LiveSessionState.Emergency -> buildEmergencyNotification(state)
        }
    }

    private fun buildIdleNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID_NAVIGATION)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("GeoRacing")
            .setContentText("Sesión activa")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildNavigationNotification(state: LiveSessionState.Navigation): Notification {
        val remoteViews = createNavigationRemoteViews(state)
        val contentIntent = createMainActivityIntent()

        return NotificationCompat.Builder(context, CHANNEL_ID_NAVIGATION)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(contentIntent)
            
            // ========== SAMSUNG NOWBAR HACK ==========
            .setCategory(Notification.CATEGORY_NAVIGATION) // 🟢 Green Pill
            .setColorized(true)
            .setColor(Color.BLACK) // OLED Friendly
            
            // ========== ENERGY EFFICIENCY ==========
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Silent updates
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildEmergencyNotification(state: LiveSessionState.Emergency): Notification {
        val remoteViews = createEmergencyRemoteViews(state)
        val contentIntent = createMainActivityIntent()

        val title = when (state.type) {
            EmergencyType.EVACUATION -> "🆘 EVACUACIÓN"
            EmergencyType.RED_FLAG -> "🔴 BANDERA ROJA"
            EmergencyType.MEDICAL -> "🏥 EMERGENCIA MÉDICA"
            EmergencyType.SECURITY -> "🔒 ALERTA SEGURIDAD"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(state.message.ifEmpty { "Siga las instrucciones: ${state.exitRoute}" })
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(contentIntent)
            
            // ========== SAMSUNG EMERGENCY MODE ==========
            .setCategory(Notification.CATEGORY_ALARM) // 🔴 Bypasses DND
            .setColorized(true)
            .setColor(Color.RED) // High Alert Color
            
            // ========== MAX PRIORITY ==========
            .setOngoing(true)
            .setOnlyAlertOnce(false) // Allow re-alerting
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNavigationRemoteViews(state: LiveSessionState.Navigation): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_live_status)
        remoteViews.setTextViewText(R.id.tvStatus, state.instruction)
        remoteViews.setTextViewText(R.id.tvDistance, "${state.distanceMeters} m")
        remoteViews.setTextViewText(R.id.tvTime, "${state.distanceMeters / 80} min") // ~80m/min walk
        remoteViews.setProgressBar(R.id.progressBar, 100, state.progress, false)
        return remoteViews
    }

    private fun createEmergencyRemoteViews(state: LiveSessionState.Emergency): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_live_status)
        remoteViews.setTextViewText(R.id.tvStatus, "⚠️ ${state.type.name}")
        remoteViews.setTextViewText(R.id.tvDistance, state.exitRoute)
        remoteViews.setTextViewText(R.id.tvTime, "AHORA")
        remoteViews.setProgressBar(R.id.progressBar, 100, 100, false)
        return remoteViews
    }

    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
