package com.georacing.georacing.ui

import android.util.Log
import com.georacing.georacing.data.ble.BeaconScanner
import com.georacing.georacing.domain.model.AppPowerState
import com.georacing.georacing.services.BatteryMonitor
import com.georacing.georacing.services.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * AppMonitorManager centralizes background monitoring logic that was
 * previously embedded in GeoRacingNavHost LaunchedEffect blocks.
 *
 * This class should be initialized once at the Application or Activity level
 * and held as a singleton in the AppContainer.
 *
 * Responsibilities:
 * - BLE beacon scanning start
 * - Battery monitoring + critical state callback
 * - Network monitoring + offline/online logging
 */
class AppMonitorManager(
    private val beaconScanner: BeaconScanner,
    private val batteryMonitor: BatteryMonitor,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "AppMonitorManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isStarted = false

    // ── Exposed state flows (UI can collect these) ──────────────────────────

    /** Current power state (NORMAL, POWER_SAVE, CRITICAL). */
    val powerState: StateFlow<AppPowerState> get() = batteryMonitor.powerState

    /** Current battery level (0-100). */
    val batteryLevel: StateFlow<Int> get() = batteryMonitor.batteryLevel

    /** Whether the device currently has network connectivity. */
    val isOnline: StateFlow<Boolean> get() = networkMonitor.isOnline

    /** Current connection type (WIFI, CELLULAR, etc.). */
    val connectionType: StateFlow<com.georacing.georacing.services.ConnectionType>
        get() = networkMonitor.connectionType

    // ── Callback for critical battery (navigation is UI-only) ───────────────

    /**
     * Called when battery state transitions to CRITICAL.
     * The NavHost should set this to trigger emergency navigation.
     */
    var onCriticalBattery: ((batteryLevel: Int) -> Unit)? = null

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Starts all monitors. Safe to call multiple times (idempotent).
     * Should be called from Activity.onCreate or Application.onCreate.
     */
    fun startAll() {
        if (isStarted) return
        isStarted = true

        // 1. BLE Beacon Scanning
        if (!beaconScanner.isScanningValue) {
            beaconScanner.startScanning()
            Log.d(TAG, "BLE beacon scanning started")
        }

        // 2. Battery Monitoring + Kill Switch
        batteryMonitor.startMonitoring()
        Log.d(TAG, "Battery monitoring started")

        scope.launch {
            batteryMonitor.powerState.collect { state ->
                if (state == AppPowerState.CRITICAL) {
                    val level = batteryMonitor.batteryLevel.value
                    Log.w(TAG, "🆘 CRITICAL BATTERY ($level%) - invoking onCriticalBattery callback")
                    onCriticalBattery?.invoke(level)
                }
            }
        }

        // 3. Network Monitoring + Logging
        networkMonitor.startMonitoring()
        Log.d(TAG, "Network monitoring started")

        networkMonitor.isOnline
            .onEach { online ->
                if (!online) {
                    Log.w(TAG, "📡 OFFLINE - Relying on BLE beacons (physical + staff phones)")
                } else {
                    Log.d(TAG, "🌐 ONLINE - Network: ${networkMonitor.connectionType.value}")
                }
            }
            .launchIn(scope)
    }
}
