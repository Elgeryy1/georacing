package com.georacing.georacing.infrastructure.car

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.car.app.connection.CarConnection
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.georacing.georacing.MainActivity
import com.georacing.georacing.R
import com.georacing.georacing.data.parking.ParkingLocation
import com.georacing.georacing.data.parking.ParkingRepository
import com.georacing.georacing.data.repository.CircuitLocationsRepository
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.services.NotificationFactory
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages seamless handover between Android Auto and pedestrian mode.
 * 
 * When car disconnects:
 * 1. Saves current location as "Car Position"
 * 2. Shows high-priority notification
 * 3. Triggers navigation to assigned gate
 */
class CarConnectionManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID_HANDOVER = "car_handover_channel"
        const val NOTIFICATION_ID_HANDOVER = 5001
        
        // Assigned gate for navigation (can be set from user preferences)
        var assignedGate: String = "Gate 3"
    }
    
    private val parkingRepository = ParkingRepository(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private var previousConnectionType = CarConnection.CONNECTION_TYPE_NOT_CONNECTED
    
    // Observable state for handover events
    private val _handoverTriggered = MutableStateFlow(false)
    val handoverTriggered: StateFlow<Boolean> = _handoverTriggered.asStateFlow()
    
    private val _savedParkingLocation = MutableStateFlow<ParkingLocation?>(null)
    val savedParkingLocation: StateFlow<ParkingLocation?> = _savedParkingLocation.asStateFlow()
    
    init {
        createHandoverChannel()
        startObserving()
    }
    
    private fun createHandoverChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID_HANDOVER,
                "Car Handover",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when parking is saved"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startObserving() {
        CarConnection(context).type.observeForever { connectionType ->
            android.util.Log.d("CarConnectionManager", "Connection type changed: $previousConnectionType -> $connectionType")
            
            // Detect disconnect: was connected, now not connected
            if (previousConnectionType == CarConnection.CONNECTION_TYPE_NATIVE && 
                connectionType == CarConnection.CONNECTION_TYPE_NOT_CONNECTED) {
                
                android.util.Log.i("CarConnectionManager", "🚗 Car disconnected! Triggering handover...")
                triggerHandover()
            }
            
            previousConnectionType = connectionType
        }
    }
    
    /**
     * Triggers the seamless handover sequence.
     * Can be called manually for simulation/demo purposes.
     */
    fun triggerHandover() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Save current location as car position
                val location = getCurrentLocation()
                if (location != null) {
                    val parkingLocation = ParkingLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        photoUri = null
                    )
                    
                    parkingRepository.saveParkingLocation(parkingLocation)
                    _savedParkingLocation.value = parkingLocation
                    
                    android.util.Log.i("CarConnectionManager", "✅ Parking saved: (${location.latitude}, ${location.longitude})")
                }
                
                // 2. Show high-priority notification
                showHandoverNotification()
                
                // 3. Signal that handover was triggered (for UI to react)
                _handoverTriggered.value = true
                
                android.util.Log.i("CarConnectionManager", "✅ Handover triggered successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("CarConnectionManager", "❌ Error during handover: ${e.message}", e)
            }
        }
    }
    
    private suspend fun getCurrentLocation(): android.location.Location? {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.await()
            } else {
                android.util.Log.w("CarConnectionManager", "Location permission not granted")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CarConnectionManager", "Error getting location: ${e.message}")
            null
        }
    }
    
    private fun showHandoverNotification() {
        val gates = CircuitLocationsRepository.getGates()
        val targetGate = gates.find { it.name.contains("3") || it.name.contains("Principal") } ?: gates.firstOrNull()
        
        // Intent to open app and navigate to gate
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_gate", true)
            putExtra("gate_lat", targetGate?.lat ?: 0.0)
            putExtra("gate_lon", targetGate?.lon ?: 0.0)
            putExtra("gate_name", targetGate?.name ?: "Puerta")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HANDOVER)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("🅿️ Coche guardado")
            .setContentText("Toca para ir a ${targetGate?.name ?: "tu Puerta"}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tu posición de aparcamiento ha sido guardada automáticamente. Toca para iniciar navegación peatonal hacia ${targetGate?.name ?: "tu puerta asignada"}."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setColorized(true)
            .setColor(Color.parseColor("#1976D2")) // Material Blue
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_HANDOVER, notification)
    }
    
    /**
     * Clears the handover state after navigation has started
     */
    fun clearHandoverState() {
        _handoverTriggered.value = false
    }
    
    /**
     * Gets the target gate coordinates for walking navigation
     */
    fun getTargetGateCoordinates(): Pair<Double, Double>? {
        val gates = CircuitLocationsRepository.getGates()
        val targetGate = gates.find { it.name.contains("3") || it.name.contains("Principal") } ?: gates.firstOrNull()
        return targetGate?.let { Pair(it.lat, it.lon) }
    }
}
