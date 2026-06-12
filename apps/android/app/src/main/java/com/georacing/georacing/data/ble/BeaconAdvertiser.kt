package com.georacing.georacing.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.UUID

class BeaconAdvertiser(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val advertiser = adapter?.bluetoothLeAdvertiser

    private var isAdvertising = false
    private val _advertisingState = MutableStateFlow("Idle")
    val advertisingState = _advertisingState.asStateFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            _advertisingState.value = "Active"
            Log.d(TAG, "Advertising started successfully")
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "GeoRacing: BLE Broadcasting Active", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            isAdvertising = false
            _advertisingState.value = "Failed: $errorCode"
            Log.e(TAG, "Advertising failed: $errorCode")
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "GeoRacing BLE Error: $errorCode", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private var lastAdvertisedData: ByteArray? = null

    @SuppressLint("MissingPermission")
    fun startAdvertising(userUid: String, lat: Double? = null, lon: Double? = null) {
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertising not supported on this device")
            _advertisingState.value = "Not Supported"
            return
        }

        // 1. Build Payload
        // [Type(1) | ID(4) | Lat(4) | Lon(4)]
        val shortId = userUid.hashCode()
        val shortIdBytes = ByteBuffer.allocate(4).putInt(shortId).array()
        
        val hasLocation = lat != null && lon != null
        val bufferSize = if (hasLocation) 13 else 5
        val buffer = ByteBuffer.allocate(bufferSize)
        
        buffer.put(0x01.toByte()) // Type: User Beacon
        buffer.put(shortIdBytes)  // ID Hash
        
        if (hasLocation) {
            buffer.putFloat(lat!!.toFloat())
            buffer.putFloat(lon!!.toFloat())
        }
        
        val manufacturerData = buffer.array()

        // 2. Diff Check (Avoid restarting if data unchanged)
        if (isAdvertising && java.util.Arrays.equals(lastAdvertisedData, manufacturerData)) {
            return 
        }

        // 3. Restart Sequence
        if (isAdvertising) {
            stopAdvertising()
        }
        
        lastAdvertisedData = manufacturerData

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(0x1234, manufacturerData) 
            .build()

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
            Log.d(TAG, "Requesting advertising start... ID=$shortId Loc=$hasLocation")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
            _advertisingState.value = "Exception: ${e.message}"
        }
    }

    /**
     * 🆘 STAFF DANGER MODE: Emite señal de peligro para alertar a otros dispositivos.
     * 
     * Payload format:
     * [Type=0x02 (Staff) | ZoneId(2) | Mode(1) | Flags(1) | Sequence(2) | TTL(1)]
     * 
     * @param staffId ID del miembro del staff
     * @param zoneId ID de la zona donde está el peligro
     * @param alertMode 0=Normal, 1=Warning, 2=Danger, 3=Evacuation
     */
    @SuppressLint("MissingPermission")
    fun startDangerAdvertising(staffId: String, zoneId: Int, alertMode: Int = MODE_EVACUATION) {
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertising not supported")
            _advertisingState.value = "Not Supported"
            return
        }

        // Construir payload de emergencia
        // New Format (Version 2 extended or reusing Type=2)
        // [Type=0x02(1) | ID(4) | ZoneId(2) | Mode(1) | Flags(1) | Sequence(2) | TTL(1)] = 12 bytes + 1(Type) = 13 bytes
        val buffer = java.nio.ByteBuffer.allocate(13)
        buffer.put(TYPE_STAFF_BEACON)           // Type: Staff Danger Beacon
        
        // INSERT STAFF ID HASH (4 bytes)
        val shortId = staffId.hashCode()
        buffer.putInt(shortId)
        
        buffer.putShort(zoneId.toShort())       // Zone ID
        buffer.put(alertMode.toByte())          // Mode: EVACUATION/DANGER
        buffer.put(0x01.toByte())               // Flags: Active
        buffer.putShort(System.currentTimeMillis().toInt().toShort()) // Sequence
        buffer.put(60.toByte())                 // TTL: 60 seconds

        val manufacturerData = buffer.array()

        // Restart si ya está emitiendo
        if (isAdvertising) {
            stopAdvertising()
        }
        
        lastAdvertisedData = manufacturerData

        // Settings: Alta potencia para máximo alcance en emergencia
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(MANUFACTURER_ID, manufacturerData)
            .build()

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
            _advertisingState.value = "⚠️ DANGER BROADCAST ACTIVE"
            Log.w(TAG, "🆘 DANGER ADVERTISING STARTED - Zone: $zoneId, Mode: $alertMode")
            
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(
                    context, 
                    "🆘 ALERTA DE PELIGRO ACTIVA - Zona $zoneId", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting danger advertising", e)
            _advertisingState.value = "Exception: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            lastAdvertisedData = null
            _advertisingState.value = "Stopped"
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
        }
    }

    companion object {
        private const val TAG = "BeaconAdvertiser"
        
        // Manufacturer ID (para desarrollo, usar ID registrado en producción)
        const val MANUFACTURER_ID = 0x1234
        
        // Beacon Types
        const val TYPE_USER_BEACON: Byte = 0x01
        const val TYPE_STAFF_BEACON: Byte = 0x02
        
        // Alert Modes
        const val MODE_NORMAL = 0
        const val MODE_WARNING = 1
        const val MODE_DANGER = 2
        const val MODE_EVACUATION = 3
    }
}
