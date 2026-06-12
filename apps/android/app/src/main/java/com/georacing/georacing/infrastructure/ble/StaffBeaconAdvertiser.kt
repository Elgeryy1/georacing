package com.georacing.georacing.infrastructure.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Emisor BLE nativo para la "Infraestructura Dinámica" (Staff Beacon).
 * Permite convertir el móvil del guardia de seguridad en una baliza emisora
 * en caso de emergencia, a máxima potencia y baja latencia.
 */
class StaffBeaconAdvertiser(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    companion object {
        // UUID de servicio específico para identificar balizas humanas de GeoRacing (formato Hex estándar)
        val GEORACING_EMERGENCY_UUID: UUID = UUID.fromString("c3084c18-0000-1000-8000-00805f9b34fb")
    }

    /**
     * Inicia la emisión BLE con el payload indicado y devuelve un Flow
     * para monitorizar el estado (true = emitiendo, false = error o detenido).
     */
    @SuppressLint("MissingPermission") // Asumimos permisos validados por la UI/UseCase
    fun startAdvertising(payload: String): Flow<Boolean> = callbackFlow {
        if (advertiser == null || bluetoothAdapter?.isEnabled == false) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val pUuid = ParcelUuid(GEORACING_EMERGENCY_UUID)
        val data = AdvertiseData.Builder()
            .addServiceUuid(pUuid)
            .addServiceData(pUuid, payload.toByteArray(Charsets.UTF_8))
            .setIncludeDeviceName(false)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d("StaffBeacon", "BLE Advertising Started Successfully - Payload: $payload")
                trySend(true)
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e("StaffBeacon", "BLE Advertising Failed with code: $errorCode")
                trySend(false)
            }
        }

        advertiser?.startAdvertising(settings, data, callback)

        awaitClose {
            Log.d("StaffBeacon", "BLE Advertising Stopped")
            advertiser?.stopAdvertising(callback)
        }
    }
}
