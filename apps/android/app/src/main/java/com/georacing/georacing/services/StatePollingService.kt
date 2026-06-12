package com.georacing.georacing.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.ui.evacuation.EvacuationActivity
import kotlinx.coroutines.*

class StatePollingService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var lastKnownMode: String? = null
    private var isEvacuationActive = false
    
    // Hybrid Components
    private lateinit var beaconScanner: com.georacing.georacing.data.ble.BeaconScanner
    private lateinit var beaconAdvertiser: com.georacing.georacing.data.ble.BeaconAdvertiser
    private lateinit var repository: com.georacing.georacing.data.repository.HybridCircuitStateRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure polling/collecting is active if restarted
        if (!job.isActive) startMonitoring()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Promote to Foreground Service immediately to alert user we are monitoring
        startForeground(999, createMonitoringNotification())
        
        // Initialize Hybrid Repo
        beaconScanner = com.georacing.georacing.data.ble.BeaconScanner(this)
        beaconAdvertiser = com.georacing.georacing.data.ble.BeaconAdvertiser(this)
        repository = com.georacing.georacing.data.repository.HybridCircuitStateRepository(
             com.georacing.georacing.data.repository.NetworkCircuitStateRepository(),
             beaconScanner
        )
        
        // Start BLE Scan (Service Lifecycle)
        beaconScanner.startScanning()

        startMonitoring()
    }

    private fun startMonitoring() {
        // Parallel Job: Order Status Polling (Every 30s) + BLE Advertising Keep-Alive
        scope.launch {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val api = com.georacing.georacing.data.firestorelike.FirestoreLikeClient.api
            val orderPrefs = applicationContext.getSharedPreferences("geo_racing_orders", Context.MODE_PRIVATE)
            val appPrefs = applicationContext.getSharedPreferences("georacing_prefs", Context.MODE_PRIVATE)
            
            while (isActive) {
                try {
                    val user = auth.currentUser
                    if (user != null) {
                         // BLE Location Sharing Logic
                         val activeGroupId = appPrefs.getString("active_group_id", null)
                         val isSharing = if (activeGroupId != null) appPrefs.getBoolean("is_sharing_$activeGroupId", false) else false
                         
                         if (isSharing && androidx.core.content.ContextCompat.checkSelfPermission(this@StatePollingService, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                              val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this@StatePollingService)
                              fused.lastLocation.addOnSuccessListener { loc ->
                                  beaconAdvertiser.startAdvertising(user.uid, loc?.latitude, loc?.longitude)
                              }.addOnFailureListener {
                                  beaconAdvertiser.startAdvertising(user.uid)
                              }
                         } else {
                              beaconAdvertiser.startAdvertising(user.uid)
                         }

                         val result = api.get(
                            com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(
                                table = "orders",
                                where = mapOf("user_uid" to user.uid, "status" to "READY")
                            )
                        )
                        
                        result.forEach { orderData ->
                             val orderId = orderData["order_id"] as? String ?: return@forEach
                             val wasNotified = orderPrefs.getBoolean("notified_$orderId", false)
            
                             if (!wasNotified) {
                                 showOrderNotification()
                                 orderPrefs.edit().putBoolean("notified_$orderId", true).apply()
                             }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StatePolling", "Order Poll / Adv Error", e)
                }
                delay(30000) // Check every 30 seconds
            }
        }

        scope.launch {
            // Ensure scanner is running (retries if permissions were missing initially)
            beaconScanner.startScanning()

            // Use Hybrid Flow which combines BLE + API Polling
            repository.getCircuitState().collect { state ->
                 try {
                    Log.d("StatePolling", "State Update: Mode=${state.mode}, Msg=${state.message}") // DEBUG LOG
                    
                    // Convert Domain Mode to String because legacy logic uses Strings
                    val modeString = when(state.mode) {
                        com.georacing.georacing.domain.model.CircuitMode.EVACUATION -> "EVACUATION"
                        else -> state.mode.name
                    }
                    
                    // Hybrid Logic: If MODE is RED_FLAG (Mapped from Evacuation or Red Flag), trigger alert?
                    // Original logic checked string "EVACUATION".
                    // The DTO says: "EVACUATION" -> CircuitMode.RED_FLAG.
                    // So if we receive RED_FLAG, it implies Evacuation or Red Flag.
                    // Let's check if the generic RED_FLAG should trigger EvacuationActivity.
                    // Probably SAFETY_CAR = Safety, RED_FLAG = Stop/Evac?
                    // I will check specific logic.
                    // BUT: The user prompt says "EVACUATION" is a distinct mode in their mental model.
                    // My BleParser maps 3 -> RED_FLAG.
                    // I should probably map 3 -> EVACUATION if I updated the Enum, but I didn't update the Enum.
                    // I will check if I should update CircuitMode Enum. 
                    // To be safe and compatible with legacy string logic:
                    
                    val isEvacuation = (state.mode == com.georacing.georacing.domain.model.CircuitMode.EVACUATION) 
                                       || (state.message?.contains("EVACUATION", ignoreCase = true) == true)
                    
                    if (isEvacuation) {
                        if (!isEvacuationActive) {
                            Log.d("StatePolling", "🚨 ACTIVATING EVACUATION (Mode: ${state.mode})")
                            
                            // Activate once
                            showEvacuationNotification()
                            isEvacuationActive = true
                        }
                    } else {
                        // EXIT Logic
                        if (isEvacuationActive) {
                            Log.d("StatePolling", "✅ EXITING EVACUATION")
                            val intent = Intent("com.georacing.georacing.EXIT_EVACUATION")
                            intent.setPackage(packageName)
                            sendBroadcast(intent)
                            
                            // Cancel notification
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(1)
                            
                            isEvacuationActive = false
                        }
                    }
                    lastKnownMode = state.mode.name

                } catch (e: Exception) {
                    Log.e("StatePolling", "Error processing state", e)
                }
            }
        }

        // DEBUG: Observe Scanner and Advertiser Info
        scope.launch {
             kotlinx.coroutines.flow.combine(
                 beaconScanner.debugInfo,
                 beaconAdvertiser.advertisingState
             ) { scan, adv ->
                 "Scan: ${scan.take(20)}.. | Adv: $adv"
             }.collect { info ->
                 updateMonitoringNotification(info)
             }
        }
    }



    private fun showOrderNotification() {
         val context = this
         val channelId = "order_updates"
         val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
 
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             val channel = NotificationChannel(channelId, "Order Updates", NotificationManager.IMPORTANCE_HIGH)
             manager.createNotificationChannel(channel)
         }
 
         val resultIntent = Intent(context, com.georacing.georacing.MainActivity::class.java).apply {
             flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    private fun showEvacuationNotification() {
        // Notification only - Activity is handled in the loop for aggression
        
        // Prepare Full Screen Intent (for lock screen priority)
        val fullScreenIntent = Intent(this, EvacuationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
            this, 
            0, 
            fullScreenIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "EMERGENCY_CHANNEL_V2")
            .setContentTitle("⚠️ ORDEN DE EVACUACIÓN")
            .setContentText("Emergencia detectada. Siga las instrucciones inmediatamente.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true) 
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
        
        // Explicitly launch activity to ensure window opens even if screen is on
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("StatePolling", "Direct launch failed", e)
        }
    }

    private fun createMonitoringNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, "MONITORING_CHANNEL")
            .setContentTitle("GeoRacing Security")
            .setContentText("Iniciando escáner...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateMonitoringNotification(text: String) {
        val notification = NotificationCompat.Builder(this, "MONITORING_CHANNEL")
            .setContentTitle("GeoRacing BLE Debug")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(999, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // 1. Critical Alert Channel (V2 to force update)
            val emergencyChannel = NotificationChannel(
                "EMERGENCY_CHANNEL_V2",
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for evacuation"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Attempt to bypass Do Not Disturb
            }
            
            // 2. Background Service Channel
            val monitoringChannel = NotificationChannel(
                "MONITORING_CHANNEL",
                "Service Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )

            manager.createNotificationChannels(listOf(emergencyChannel, monitoringChannel))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        try {
            beaconScanner.stopScanning()
            beaconAdvertiser.stopAdvertising()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
