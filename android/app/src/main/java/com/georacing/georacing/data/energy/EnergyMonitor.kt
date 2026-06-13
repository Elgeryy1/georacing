package com.georacing.georacing.data.energy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.domain.model.EnergyProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EnergyMonitor(private val context: Context) {

    private val _energyProfile = MutableStateFlow<EnergyProfile>(EnergyProfile.Performance)
    val energyProfile: StateFlow<EnergyProfile> = _energyProfile.asStateFlow()

    // Internal state for real battery
    private val _realBatteryLevel = MutableStateFlow(100f)

    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                
                if (level != -1 && scale != -1) {
                    val batteryPct = (level / scale.toFloat()) * 100
                    _realBatteryLevel.value = batteryPct
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        // Start observing both Real + Simulation
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            combine(
                _realBatteryLevel,
                ScenarioSimulator.forcedBatteryLevel
            ) { real, forced ->
                val effective = forced?.toFloat() ?: real
                effective
            }.collect { percentage ->
                updateEnergyProfile(percentage)
            }
        }
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        monitoringJob?.cancel()
    }

    private fun updateEnergyProfile(percentage: Float) {
        val newProfile = when {
            percentage > 50 -> EnergyProfile.Performance
            percentage >= 30 -> EnergyProfile.Balanced
            else -> EnergyProfile.Survival
        }

        if (_energyProfile.value != newProfile) {
            _energyProfile.value = newProfile
        }
    }
}
