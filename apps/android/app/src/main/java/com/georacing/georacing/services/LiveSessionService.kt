package com.georacing.georacing.services

import android.app.NotificationManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.georacing.georacing.domain.model.LiveSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Live Session Service - The Engine for real-time notifications.
 * 
 * Design:
 * - Extends LifecycleService for coroutine-aware lifecycle.
 * - Uses StateFlow for reactive state management.
 * - Calls startForeground() immediately in onCreate() (<5s rule).
 * - Updates notification on every state change.
 */
class LiveSessionService : LifecycleService() {

    companion object {
        private const val TAG = "LiveSessionService"
        
        // External access to update state (Singleton pattern for simplicity)
        // In production, use a shared repository or event bus.
        private val _sessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.Idle)
        val sessionState: StateFlow<LiveSessionState> = _sessionState.asStateFlow()
        
        fun updateState(newState: LiveSessionState) {
            _sessionState.value = newState
            Log.d(TAG, "State updated: $newState")
        }
    }

    private lateinit var notificationFactory: NotificationFactory

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        // Initialize NotificationFactory (manual DI)
        notificationFactory = NotificationFactory(this)
        
        // CRITICAL: Start foreground immediately to avoid ANR (<5 seconds rule)
        val initialNotification = notificationFactory.build(LiveSessionState.Idle)
        startForeground(NotificationFactory.NOTIFICATION_ID, initialNotification)
        
        // Observe state changes and update notification
        observeStateChanges()
    }

    private fun observeStateChanges() {
        lifecycleScope.launch {
            sessionState.collectLatest { state ->
                Log.d(TAG, "State observed: $state")
                val notification = notificationFactory.build(state)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NotificationFactory.NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        // Reset state to Idle when service is killed
        _sessionState.value = LiveSessionState.Idle
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
