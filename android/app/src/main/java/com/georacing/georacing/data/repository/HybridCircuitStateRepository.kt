package com.georacing.georacing.data.repository

import com.georacing.georacing.data.ble.BeaconScanner
import com.georacing.georacing.domain.model.AppMode
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Repository that arbitrates between Network API and Active BLE Beacon.
 * Priority: BLE (if active & recent) -> Network -> Cache (Fallback) -> Default
 */
class HybridCircuitStateRepository(
    private val networkRepository: NetworkCircuitStateRepository,
    private val beaconScanner: BeaconScanner
) : com.georacing.georacing.domain.repository.CircuitStateRepository {

    // Helper to determie current AppMode based on signal
    // Arbitrate AppMode based on BOTH Network and BLE
    override val appMode: Flow<AppMode> = combine(
        networkRepository.getCircuitState(),
        beaconScanner.activeSignal
    ) { netState, bleSignal ->
        // PRIORITY: NETWORK > BLE (as requested)
        if (netState.mode != CircuitMode.UNKNOWN) {
            AppMode.ONLINE
        } else if (bleSignal != null && !bleSignal.isExpired()) {
            AppMode.OFFLINE_BLE
        } else {
            AppMode.ONLINE
        }
    }

    override val debugInfo: Flow<String> = beaconScanner.debugInfo

    override fun getCircuitState(): Flow<CircuitState> {
        // Polling Network (Continuous)
        val networkFlow = networkRepository.getCircuitState()
            .onEach { state ->
                // ALWAYS SCAN (Hot Standby / Override Priority)
                // We do NOT stop scanning even if network is fine, 
                // because a Physical Beacon (Red Flag) should override the Cloud.
                if (!beaconScanner.isScanningValue) {
                    beaconScanner.startScanning()
                }
            }

        // Active Signal
        val bleFlow = beaconScanner.activeSignal

        return combine(networkFlow, bleFlow) { netState, bleSignal ->
            // PRIORITY: BLE EVACUATION > Network > Other BLE > Default
            // Safety-critical BLE signals (EVACUATION, RED_FLAG) MUST override network.
            // This is essential for demos and real-world safety.
            
            val isBleEvacuation = bleSignal != null && !bleSignal.isExpired() &&
                (bleSignal.mode == CircuitMode.EVACUATION || bleSignal.mode == CircuitMode.RED_FLAG)

            if (isBleEvacuation) {
                // CRITICAL: Physical Beacon / Simulation takes absolute priority for safety
                CircuitState(
                    mode = bleSignal!!.mode,
                    message = "⚠️ SEÑAL BALIZA ACTIVA (Zona ${bleSignal.zoneId})",
                    temperature = bleSignal.temperature?.let { "${it / 10.0}°C" },
                    updatedAt = "BLE Seq ${bleSignal.sequence}"
                )
            } else if (netState.mode != CircuitMode.UNKNOWN) {
                // Network Healthy and no critical BLE signal
                netState
            } else if (bleSignal != null && !bleSignal.isExpired()) {
                 CircuitState(
                    mode = bleSignal.mode,
                    message = "SEÑAL BALIZA ACTIVA (Zona ${bleSignal.zoneId})",
                    temperature = bleSignal.temperature?.let { "${it / 10.0}°C" },
                    updatedAt = "BLE Seq ${bleSignal.sequence}"
                )
            } else {
                // Both sources failed
                CircuitState(CircuitMode.UNKNOWN, "Buscando señal...", null, "")
            }
        }
    }

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        // No-op or delegate to network if writable (currently read-only)
    }
}
