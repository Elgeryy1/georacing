package com.georacing.georacing.infrastructure.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleCommandService(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var advertiser: BluetoothLeAdvertiser? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopAdvertising() }

    companion object {
        private const val TAG = "BleCommandService"
        private const val MANUFACTURER_ID = 0x1234 // Example ID for GeoRacing Beacons
        
        // Commands
        const val CMD_NORMAL = 0x00
        const val CMD_EVACUATE = 0x01
        const val CMD_DANGER = 0x02
        
        private const val TIMEOUT_MS = 30_000L // 30 seconds safety timeout
    }

    fun startAdvertising(commandId: Int) {
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertising not supported on this device")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission")
            return
        }

        // Stop any previous session
        stopAdvertising()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // High priority
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Payload: [0xFF, commandId] 
        // 0xFF often used as a custom preamble or just data
        val payload = byteArrayOf(0xFF.toByte(), commandId.toByte())

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(MANUFACTURER_ID, payload)
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
        
        // Auto-stop safety timer
        handler.postDelayed(stopRunnable, TIMEOUT_MS)
    }

    fun stopAdvertising() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            advertiser?.stopAdvertising(advertiseCallback)
        }
        _isAdvertising.value = false
        handler.removeCallbacks(stopRunnable)
        Log.d(TAG, "Advertising stopped")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "BLE Broadcasting started successfully. Command active.")
            _isAdvertising.value = true
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Broadcasting failed: error $errorCode")
            _isAdvertising.value = false
        }
    }
}
