package com.georacing.georacing.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.georacing.georacing.debug.ScenarioSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BeaconScanner(private val context: Context) {

    private val _detectedBeacons = MutableStateFlow<List<DetectedBeacon>>(emptyList())
    val detectedBeacons: StateFlow<List<DetectedBeacon>> = _detectedBeacons.asStateFlow()

    // Internal real signal
    private val _realActiveSignal = MutableStateFlow<BleCircuitSignal?>(null)
    
    // Public combined signal
    val activeSignal: StateFlow<BleCircuitSignal?> = combine(
        _realActiveSignal,
        ScenarioSimulator.forcedBleSignal
    ) { real, forced ->
        forced ?: real
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = null
    )

    private val _debugInfo = MutableStateFlow("Init")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    private val _detectedUsers = MutableStateFlow<List<DetectedUser>>(emptyList())
    val detectedUsers: StateFlow<List<DetectedUser>> = _detectedUsers.asStateFlow()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    val isScanningValue: Boolean get() = isScanning

    private val scope = CoroutineScope(Dispatchers.IO)
    private var restartJob: Job? = null
    private var shouldBeScanning = false
    private var lastRestartTime = 0L

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val device = scanResult.device
                val rssi = scanResult.rssi
                val scanRecord = scanResult.scanRecord
                
                // 1. Passive Beacons handling
                val deviceName = scanRecord?.deviceName ?: device.name
                val manufacturerData = scanRecord?.manufacturerSpecificData
                val hasGeoRacingId = manufacturerData?.get(0x1234) != null
                val isGeoRacing = hasGeoRacingId || deviceName?.contains("GEORACING", ignoreCase = true) == true
                
                var detectedMode: com.georacing.georacing.domain.model.CircuitMode? = null
                var detectedId: String? = null
                
                manufacturerData?.let {
                    val bytes = it.get(0x1234)
                    if (bytes != null) {
                         val signal = BlePayloadParser.parse(0x1234, bytes)
                         detectedMode = signal?.mode
                         detectedId = signal?.sourceId?.toString()
                         // If we parse a valid signal, update active signal
                         if (signal != null) {
                             updateActiveSignal(signal, rssi)
                         }
                    }
                }

                if (isGeoRacing) {
                    val finalId = if (detectedId != null) "STAFF_$detectedId" else device.address
                    updateBeacon(finalId, deviceName, rssi, isGeoRacing, detectedMode)
                }

                // 2. Active Beacon User Detection
                scanRecord?.manufacturerSpecificData?.let { mfData ->
                    for (i in 0 until mfData.size()) {
                        val id = mfData.keyAt(i)
                        val bytes = mfData.valueAt(i)
                        val isUserBeaconLength = bytes.size == 5 || bytes.size == 13
                        if (id == 0x1234 && bytes.isNotEmpty() && bytes[0] == 0x01.toByte() && isUserBeaconLength) {
                             if (bytes.size >= 5) {
                                  val buffer = java.nio.ByteBuffer.wrap(bytes)
                                  buffer.get() // Skip Type
                                  val hash = buffer.int
                                  var lat: Double? = null
                                  var lon: Double? = null
                                  if (bytes.size >= 13) {
                                      lat = buffer.float.toDouble()
                                      lon = buffer.float.toDouble()
                                  }
                                  updateDetectedUser(hash, rssi, lat, lon)
                             }
                        }
                    }
                }
            }
        }
    }

    private fun updateActiveSignal(signal: BleCircuitSignal, rssi: Int) {
        if (rssi > -90) {
             _realActiveSignal.value = signal
        }
    }

    private fun updateDetectedUser(hash: Int, rssi: Int, lat: Double?, lon: Double?) {
        val current = _detectedUsers.value.toMutableList()
        val index = current.indexOfFirst { it.idHash == hash }
        if (index != -1) {
            current[index] = DetectedUser(hash, rssi, System.currentTimeMillis(), lat, lon)
        } else {
            current.add(DetectedUser(hash, rssi, System.currentTimeMillis(), lat, lon))
        }
        val now = System.currentTimeMillis()
        current.removeAll { now - it.timestamp > 30000 }
        _detectedUsers.value = current
    }

    private fun updateBeacon(id: String, name: String?, rssi: Int, isGeoRacing: Boolean, mode: com.georacing.georacing.domain.model.CircuitMode?) {
        if (!isGeoRacing) return
        val currentList = _detectedBeacons.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == id }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            val newMode = mode ?: existing.circuitMode
            currentList[existingIndex] = existing.copy(
                rssi = rssi,
                timestamp = System.currentTimeMillis(),
                circuitMode = newMode
            )
        } else {
            currentList.add(DetectedBeacon(id, name, rssi, isGeoRacing, System.currentTimeMillis(), mode))
        }
        val now = System.currentTimeMillis()
        currentList.removeAll { now - it.timestamp > 60000 }
        _detectedBeacons.value = currentList
    }

    fun pruneInactiveDevices(ttlMs: Long) {
        val currentList = _detectedBeacons.value.toMutableList()
        val now = System.currentTimeMillis()
        val removed = currentList.removeAll { now - it.timestamp > ttlMs }
        if (removed) {
             _detectedBeacons.value = currentList
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (shouldBeScanning) return
        shouldBeScanning = true
        startWatchdog()
    }

    private fun startWatchdog() {
        restartJob?.cancel()
        restartJob = scope.launch {
            while (shouldBeScanning) {
                try {
                    val error = checkRequirements()
                    if (error != null) {
                        _debugInfo.value = "Wait: $error"
                        if (isScanning) stopScanInternal()
                    } else {
                        if (!isScanning) {
                            startScanInternal()
                            lastRestartTime = System.currentTimeMillis()
                        } else {
                            if (System.currentTimeMillis() - lastRestartTime > 15 * 60 * 1000L) {
                                stopScanInternal()
                                delay(2000) 
                            }
                        }
                    }
                } catch (e: Exception) { }
                delay(30000) 
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanInternal() {
        if (isScanning) return
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()
            _debugInfo.value = "Scanning (OPEN)..."
            scanner?.startScan(null, settings, scanCallback)
            isScanning = true
            
            scope.launch {
                while (isScanning) {
                    delay(1000)
                    if (!_debugInfo.value.contains("Found")) {
                         if (!_debugInfo.value.contains("ID:") && !_debugInfo.value.contains("ERR") && !_debugInfo.value.contains("Watchdog")) {
                             _debugInfo.value = "Scanning..."
                         }
                    }
                }
            }
        } catch (e: Exception) {
            _debugInfo.value = "Filter Error: ${e.message}"
        }
    }

    private fun checkRequirements(): String? {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return "BT OFF"
        return null 
    }

    fun stopScanning() {
        shouldBeScanning = false
        restartJob?.cancel()
        stopScanInternal()
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
            isScanning = false
        } catch (e: Exception) { }
    }
    
    fun forceConnect(deviceAddress: String) {
        val forcedSignal = BleCircuitSignal(
            version = 1,
            zoneId = 1001,
            mode = com.georacing.georacing.domain.model.CircuitMode.NORMAL,
            flags = 0,
            sequence = 999,
            ttlSeconds = 10,
            temperature = 25
        )
        _realActiveSignal.value = forcedSignal
        _debugInfo.value = "FORCED: $deviceAddress"
    }
}

data class DetectedUser(
    val idHash: Int,
    val rssi: Int,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)
