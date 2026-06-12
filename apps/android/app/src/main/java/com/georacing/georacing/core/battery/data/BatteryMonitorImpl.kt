package com.georacing.georacing.core.battery.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.georacing.georacing.core.battery.domain.BatteryMonitor
import com.georacing.georacing.core.battery.domain.BatteryState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Implementación de infraestructura (Data Layer).
 * Escucha los intents de Android sin polling constante.
 */
class BatteryMonitorImpl(
    private val context: Context
) : BatteryMonitor {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    private val _batteryState = MutableStateFlow(getInitialBatteryState())
    override val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    init {
        applicationScope.launch {
            observeBatteryIntents()
                .distinctUntilChanged()
                .collect { state ->
                    _batteryState.value = state
                }
        }
    }

    private fun getInitialBatteryState(): BatteryState {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
        return batteryStatus?.toBatteryState() ?: BatteryState.UNKNOWN
    }

    private fun observeBatteryIntents(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    trySend(it.toBatteryState())
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level != -1 && scale != -1) {
            (level * 100) / scale
        } else {
            -1
        }

        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(percentage = percentage, isCharging = isCharging)
    }
}
