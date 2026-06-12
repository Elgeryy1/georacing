package com.georacing.georacing.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor de conectividad de red.
 * 
 * Detecta automáticamente cuando el dispositivo pierde conexión a internet
 * y emite estados que permiten activar el fallback a BLE.
 * 
 * Uso:
 * ```
 * val monitor = NetworkMonitor(context)
 * monitor.startMonitoring()
 * 
 * // En Compose:
 * val isOnline by monitor.isOnline.collectAsState()
 * if (!isOnline) {
 *     // Mostrar indicador offline + confiar en BLE
 * }
 * ```
 */
class NetworkMonitor(private val context: Context) {

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.UNKNOWN)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val _lastOnlineTime = MutableStateFlow(System.currentTimeMillis())
    val lastOnlineTime: StateFlow<Long> = _lastOnlineTime.asStateFlow()

    private var isMonitoring = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            _isOnline.value = true
            _lastOnlineTime.value = System.currentTimeMillis()
            updateConnectionType()
        }

        override fun onLost(network: Network) {
            Log.w(TAG, "Network lost - Switching to offline/BLE mode")
            _isOnline.value = false
            _connectionType.value = ConnectionType.NONE
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateConnectionType(capabilities)
        }
    }

    /**
     * Inicia la monitorización de red.
     */
    fun startMonitoring() {
        if (isMonitoring) return

        // Verificar estado inicial
        updateCurrentState()

        // Registrar callback para cambios futuros
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isMonitoring = true
            Log.d(TAG, "Network monitoring started. Online: ${_isOnline.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitor", e)
        }
    }

    /**
     * Detiene la monitorización.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
            Log.d(TAG, "Network monitoring stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping network monitor", e)
        }
    }

    private fun updateCurrentState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        if (_isOnline.value) {
            _lastOnlineTime.value = System.currentTimeMillis()
        }
        
        updateConnectionType(capabilities)
    }

    private fun updateConnectionType(capabilities: NetworkCapabilities? = null) {
        val caps = capabilities ?: connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )

        _connectionType.value = when {
            caps == null -> ConnectionType.NONE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
    }

    /**
     * Verifica si hay conexión en este momento (síncrono).
     */
    fun isCurrentlyOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Tiempo en segundos desde que se perdió la conexión.
     * Retorna 0 si está online.
     */
    fun secondsOffline(): Long {
        return if (_isOnline.value) {
            0
        } else {
            (System.currentTimeMillis() - _lastOnlineTime.value) / 1000
        }
    }
}

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE,
    UNKNOWN
}
