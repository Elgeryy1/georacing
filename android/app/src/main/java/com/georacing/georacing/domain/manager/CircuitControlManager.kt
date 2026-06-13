package com.georacing.georacing.domain.manager

import android.content.Context
import com.georacing.georacing.infrastructure.ble.BleCommandService
import kotlinx.coroutines.flow.StateFlow

class CircuitControlManager(context: Context) {

    private val bleService = BleCommandService(context)
    val isBroadcasting: StateFlow<Boolean> = bleService.isAdvertising

    fun activateEvacuation() {
        // High priority broadcast
        bleService.startAdvertising(BleCommandService.CMD_EVACUATE)
    }

    fun activateDangerZone() {
        bleService.startAdvertising(BleCommandService.CMD_DANGER)
    }

    fun normalizeCircuit() {
        // Broadcast "Normal" state for 30s to ensure all beacons reset
        bleService.startAdvertising(BleCommandService.CMD_NORMAL)
    }

    fun stopBroadcast() {
        bleService.stopAdvertising()
    }
}
