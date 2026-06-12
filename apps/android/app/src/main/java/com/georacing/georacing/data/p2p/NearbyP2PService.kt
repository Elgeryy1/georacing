package com.georacing.georacing.data.p2p

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Servicio P2P usando Nearby Connections API
 * Permite compartir ubicaciones entre dispositivos cercanos sin servidor
 */
class NearbyP2PService(private val context: Context) {
    
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()
    
    private val connectedEndpoints = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "NearbyP2PService"
        private const val SERVICE_ID = "com.georacing.location_share"
        private val STRATEGY = Strategy.P2P_CLUSTER  // Permite múltiples conexiones
    }
    
    /**
     * Inicia como HOST (anunciante) - el que genera el QR
     */
    fun startAdvertising(
        username: String,
        onEndpointIdReceived: (String) -> Unit,
        onLocationReceived: (LocationPayload) -> Unit
    ): Flow<ConnectionStatus> = callbackFlow {
        
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                Log.d(TAG, "📱 Conexión iniciada con: ${info.endpointName}")
                
                // Aceptar automáticamente conexiones
                connectionsClient.acceptConnection(endpointId, payloadCallback(onLocationReceived))
                
                trySend(ConnectionStatus.Connecting(info.endpointName))
            }
            
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d(TAG, "✅ Conectado con: $endpointId")
                        connectedEndpoints.add(endpointId)
                        trySend(ConnectionStatus.Connected(endpointId))
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Log.w(TAG, "❌ Conexión rechazada: $endpointId")
                        trySend(ConnectionStatus.Disconnected(endpointId))
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Error de conexión: ${result.status.statusMessage}")
                        trySend(ConnectionStatus.Error(result.status.statusMessage ?: "Error desconocido"))
                    }
                }
            }
            
            override fun onDisconnected(endpointId: String) {
                Log.d(TAG, "🔌 Desconectado: $endpointId")
                connectedEndpoints.remove(endpointId)
                trySend(ConnectionStatus.Disconnected(endpointId))
            }
        }
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
        
        connectionsClient.startAdvertising(
            username,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "🚀 Anunciando como: $username")
            
            // El endpoint ID se obtiene cuando alguien se conecta
            // Por ahora usamos el nombre de usuario como identificador
            onEndpointIdReceived(username.hashCode().toString())
            
            trySend(ConnectionStatus.Advertising)
            
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Error al anunciar", e)
            trySend(ConnectionStatus.Error(e.message ?: "Error al iniciar anuncio"))
            close(e)
        }
        
        awaitClose {
            Log.d(TAG, "🛑 Deteniendo anuncio")
            connectionsClient.stopAdvertising()
            connectionsClient.stopAllEndpoints()
        }
    }
    
    /**
     * Inicia como CLIENT (descubridor) - el que escanea el QR
     */
    fun startDiscovering(
        myName: String,
        targetEndpointId: String,
        onLocationReceived: (LocationPayload) -> Unit
    ): Flow<ConnectionStatus> = callbackFlow {
        
        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                Log.d(TAG, "🔍 Endpoint encontrado: ${info.endpointName} ($endpointId)")
                
                // Conectar automáticamente al endpoint del host
                val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                        Log.d(TAG, "📱 Conexión iniciada con host: ${connectionInfo.endpointName}")
                        connectionsClient.acceptConnection(endpointId, payloadCallback(onLocationReceived))
                        trySend(ConnectionStatus.Connecting(connectionInfo.endpointName))
                    }
                    
                    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                        when (result.status.statusCode) {
                            ConnectionsStatusCodes.STATUS_OK -> {
                                Log.d(TAG, "✅ Conectado con host: $endpointId")
                                connectedEndpoints.add(endpointId)
                                trySend(ConnectionStatus.Connected(endpointId))
                            }
                            else -> {
                                Log.w(TAG, "⚠️ Error conectando: ${result.status.statusMessage}")
                                trySend(ConnectionStatus.Error(result.status.statusMessage ?: "Error"))
                            }
                        }
                    }
                    
                    override fun onDisconnected(endpointId: String) {
                        Log.d(TAG, "🔌 Desconectado del host")
                        connectedEndpoints.remove(endpointId)
                        trySend(ConnectionStatus.Disconnected(endpointId))
                    }
                }
                
                connectionsClient.requestConnection(
                    myName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnSuccessListener {
                    Log.d(TAG, "📞 Solicitando conexión a: $endpointId")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error solicitando conexión", e)
                    trySend(ConnectionStatus.Error(e.message ?: "Error"))
                }
            }
            
            override fun onEndpointLost(endpointId: String) {
                Log.d(TAG, "📡 Endpoint perdido: $endpointId")
            }
        }
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
        
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "🔍 Buscando dispositivos...")
            trySend(ConnectionStatus.Discovering)
            
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Error al buscar", e)
            trySend(ConnectionStatus.Error(e.message ?: "Error al buscar"))
            close(e)
        }
        
        awaitClose {
            Log.d(TAG, "🛑 Deteniendo búsqueda")
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        }
    }
    
    /**
     * Callback para recibir ubicaciones
     */
    private fun payloadCallback(onLocationReceived: (LocationPayload) -> Unit) = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val json = String(bytes)
                
                try {
                    val locationPayload = gson.fromJson(json, LocationPayload::class.java)
                    Log.d(TAG, "📍 Ubicación recibida de ${locationPayload.userName}: (${locationPayload.lat}, ${locationPayload.lng})")
                    onLocationReceived(locationPayload)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando ubicación", e)
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No necesario para BYTES
        }
    }
    
    /**
     * Envía ubicación a todos los dispositivos conectados
     */
    fun broadcastLocation(locationPayload: LocationPayload) {
        if (connectedEndpoints.isEmpty()) {
            Log.w(TAG, "⚠️ No hay dispositivos conectados")
            return
        }
        
        val json = gson.toJson(locationPayload)
        val payload = Payload.fromBytes(json.toByteArray())
        
        connectionsClient.sendPayload(connectedEndpoints.toList(), payload)
            .addOnSuccessListener {
                Log.d(TAG, "📤 Ubicación enviada a ${connectedEndpoints.size} dispositivos")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error enviando ubicación", e)
            }
    }
    
    /**
     * Detiene todas las conexiones
     */
    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        Log.d(TAG, "🛑 Todas las conexiones detenidas")
    }
}

/**
 * Estados de conexión P2P
 */
sealed class ConnectionStatus {
    object Advertising : ConnectionStatus()
    object Discovering : ConnectionStatus()
    data class Connecting(val name: String) : ConnectionStatus()
    data class Connected(val endpointId: String) : ConnectionStatus()
    data class Disconnected(val endpointId: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

/**
 * Payload de ubicación compartida por P2P
 */
data class LocationPayload(
    val userId: String,
    val userName: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)
