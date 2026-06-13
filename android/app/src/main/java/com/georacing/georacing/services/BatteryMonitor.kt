package com.georacing.georacing.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.georacing.georacing.domain.model.AppPowerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor de batería para implementar Load Shedding.
 * 
 * Observa el nivel de batería del dispositivo y emite estados
 * que determinan qué funcionalidades deben estar activas.
 * 
 * Uso:
 * ```
 * val monitor = BatteryMonitor(context)
 * monitor.startMonitoring()
 * 
 * // En Compose:
 * val powerState by monitor.powerState.collectAsState()
 * when (powerState) {
 *     AppPowerState.CRITICAL -> // Solo emergencia
 *     AppPowerState.POWER_SAVE -> // Funciones reducidas
 *     AppPowerState.NORMAL -> // Todo activo
 * }
 * ```
 */
class BatteryMonitor(private val context: Context) {

    companion object {
        private const val TAG = "BatteryMonitor"
        
        // Umbrales de batería
        const val THRESHOLD_POWER_SAVE = 30  // Activa modo ahorro
        const val THRESHOLD_CRITICAL = 15    // Solo emergencia
    }

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _powerState = MutableStateFlow(AppPowerState.NORMAL)
    val powerState: StateFlow<AppPowerState> = _powerState.asStateFlow()

    private var isMonitoring = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { processIntent(it) }
        }
    }

    /**
     * Inicia la monitorización de la batería.
     * Llama a esto en onCreate de MainActivity o en el NavHost.
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(batteryReceiver, filter)
        
        // Procesar estado inicial
        intent?.let { processIntent(it) }
        
        isMonitoring = true
        Log.d(TAG, "Battery monitoring started")
    }

    /**
     * Detiene la monitorización.
     * Llama a esto en onDestroy o cuando ya no sea necesario.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered")
        }
        
        isMonitoring = false
        Log.d(TAG, "Battery monitoring stopped")
    }

    private fun processIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val percentage = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            100
        }

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL

        _batteryLevel.value = percentage
        _isCharging.value = charging

        // Calcular nuevo estado de energía
        val newState = calculatePowerState(percentage, charging)
        
        if (_powerState.value != newState) {
            Log.i(TAG, "Power state changed: ${_powerState.value} -> $newState (battery: $percentage%, charging: $charging)")
            _powerState.value = newState
        }
    }

    private fun calculatePowerState(percentage: Int, charging: Boolean): AppPowerState {
        // Si está cargando, siempre modo normal
        if (charging) return AppPowerState.NORMAL

        return when {
            percentage < THRESHOLD_CRITICAL -> AppPowerState.CRITICAL
            percentage < THRESHOLD_POWER_SAVE -> AppPowerState.POWER_SAVE
            else -> AppPowerState.NORMAL
        }
    }

    /**
     * Fuerza una lectura inmediata del estado de batería.
     * Útil para verificación en tests.
     */
    fun forceUpdate() {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        intent?.let { processIntent(it) }
    }
}
