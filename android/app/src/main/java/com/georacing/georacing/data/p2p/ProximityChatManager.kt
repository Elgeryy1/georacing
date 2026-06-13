package com.georacing.georacing.data.p2p

import android.content.Context
import android.util.Log
import com.georacing.georacing.data.ble.BeaconScanner
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sistema de chat por proximidad para GeoRacing.
 *
 * Combina:
 * - BLE Scanner para descubrir usuarios cercanos (ya existente via BeaconScanner)
 * - Nearby Connections API para enviar/recibir mensajes de texto
 *
 * Funcionamiento:
 * 1. El usuario ve la lista de personas cercanas (vía BLE detectedUsers)
 * 2. Puede enviar mensajes broadcast a todos los cercanos
 * 3. Los mensajes se transmiten via Nearby Connections (P2P_CLUSTER)
 * 4. Alcance efectivo: ~50-100m (BLE + WiFi-Direct)
 *
 * Mensajes rápidos predefinidos + texto libre.
 */
class ProximityChatManager(
    private val context: Context,
    private val beaconScanner: BeaconScanner
) {
    companion object {
        private const val TAG = "ProximityChat"
        private const val SERVICE_ID = "com.georacing.proximity_chat"
        private const val MAX_MESSAGE_LENGTH = 200
        private const val MESSAGE_TTL_MS = 5 * 60 * 1000L // 5 min
    }

    // ── Modelos ──

    data class ChatMessage(
        val id: String = "${System.currentTimeMillis()}_${(1000..9999).random()}",
        val senderId: String,
        val senderName: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isMe: Boolean = false,
        val type: MessageType = MessageType.TEXT
    )

    enum class MessageType {
        TEXT,           // Texto libre
        QUICK,          // Mensaje rápido predefinido
        LOCATION_SHARE, // Compartir ubicación
        EMOJI_REACT     // Reacción emoji
    }

    data class NearbyUser(
        val endpointId: String,
        val name: String,
        val rssi: Int? = null,
        val connected: Boolean = false
    )

    // ── Estado ──

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _nearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())
    val nearbyUsers: StateFlow<List<NearbyUser>> = _nearbyUsers.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Desconectado")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()
    private var myName = "Anónimo"
    private var myId = ""
    private val connectedEndpoints = mutableSetOf<String>()

    // Mensajes rápidos predefinidos
    val quickMessages = listOf(
        "👋 ¡Hola vecino!",
        "🏎️ ¡Vamos!",
        "📸 ¿Alguien tiene buena vista desde aquí?",
        "🍺 ¿Alguien va al bar?",
        "🅿️ ¿Alguien sabe dónde hay parking libre?",
        "🔴 ¡Bandera roja!",
        "🏁 ¡Último stint!",
        "🎉 ¡Increíble adelantamiento!"
    )

    // ── API Pública ──

    /**
     * Inicia el chat de proximidad. Comienza a anunciar y descubrir.
     */
    fun start(userName: String, userId: String) {
        myName = userName
        myId = userId
        _isActive.value = true
        _connectionStatus.value = "Buscando gente cercana..."

        startAdvertisingChat()
        startDiscoveringChat()

        // Observar usuarios BLE cercanos
        scope.launch {
            beaconScanner.detectedUsers.collect { bleUsers ->
                // Mergeamos usuarios BLE con los de Nearby
                val bleNearby = bleUsers.map { user ->
                    NearbyUser(
                        endpointId = "ble_${user.idHash}",
                        name = "Usuario #${user.idHash.toString().takeLast(4)}",
                        rssi = user.rssi,
                        connected = false
                    )
                }
                val currentNearby = _nearbyUsers.value.filter { it.endpointId.startsWith("nearby_") }
                _nearbyUsers.value = (currentNearby + bleNearby).distinctBy { it.endpointId }
            }
        }

        Log.i(TAG, "💬 Proximity chat started as '$userName'")
    }

    /**
     * Envía un mensaje a todos los conectados.
     */
    fun sendMessage(text: String, type: MessageType = MessageType.TEXT) {
        val trimmed = text.take(MAX_MESSAGE_LENGTH)
        val message = ChatMessage(
            senderId = myId,
            senderName = myName,
            text = trimmed,
            isMe = true,
            type = type
        )

        // Añadir localmente
        _messages.value = (_messages.value + message).takeLast(100)

        // Broadcast a todos los endpoints conectados
        val json = gson.toJson(message.copy(isMe = false))
        val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))

        connectedEndpoints.forEach { endpointId ->
            try {
                Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send to $endpointId: ${e.message}")
            }
        }

        Log.d(TAG, "💬 Sent: '$trimmed' to ${connectedEndpoints.size} endpoints")
    }

    /**
     * Envía un mensaje rápido predefinido.
     */
    fun sendQuickMessage(index: Int) {
        if (index in quickMessages.indices) {
            sendMessage(quickMessages[index], MessageType.QUICK)
        }
    }

    /**
     * Limpia mensajes antiguos.
     */
    fun pruneOldMessages() {
        val cutoff = System.currentTimeMillis() - MESSAGE_TTL_MS
        _messages.value = _messages.value.filter { it.timestamp > cutoff }
    }

    /**
     * Detiene el chat.
     */
    fun stop() {
        try {
            Nearby.getConnectionsClient(context).stopAdvertising()
            Nearby.getConnectionsClient(context).stopDiscovery()
            Nearby.getConnectionsClient(context).stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping: ${e.message}")
        }
        connectedEndpoints.clear()
        _isActive.value = false
        _connectionStatus.value = "Desconectado"
        Log.i(TAG, "💬 Proximity chat stopped")
    }

    // ── Nearby Connections ──

    private fun startAdvertisingChat() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(myName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                Log.i(TAG, "✅ Advertising started")
                _connectionStatus.value = "Visible para otros"
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Advertising failed: ${e.message}")
            }
    }

    private fun startDiscoveringChat() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                Log.i(TAG, "✅ Discovery started")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Discovery failed: ${e.message}")
            }
    }

    // ── Callbacks ──

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.i(TAG, "📨 Connection initiated from ${info.endpointName}")
            // Auto-aceptar conexiones (es chat público de proximidad)
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpoints.add(endpointId)
                    _nearbyUsers.value = _nearbyUsers.value.map {
                        if (it.endpointId == "nearby_$endpointId") it.copy(connected = true) else it
                    }
                    _connectionStatus.value = "${connectedEndpoints.size} personas conectadas"
                    Log.i(TAG, "✅ Connected to $endpointId (total: ${connectedEndpoints.size})")
                }
                else -> {
                    Log.w(TAG, "❌ Connection failed to $endpointId: ${result.status}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            _nearbyUsers.value = _nearbyUsers.value.filter { it.endpointId != "nearby_$endpointId" }
            _connectionStatus.value = if (connectedEndpoints.isEmpty())
                "Buscando gente cercana..."
            else "${connectedEndpoints.size} personas conectadas"
            Log.i(TAG, "📴 Disconnected from $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "🔍 Found: ${info.endpointName} ($endpointId)")

            // Añadir a lista de cercanos
            val newUser = NearbyUser("nearby_$endpointId", info.endpointName)
            _nearbyUsers.value = (_nearbyUsers.value + newUser).distinctBy { it.endpointId }

            // Intentar conectar automáticamente
            Nearby.getConnectionsClient(context)
                .requestConnection(myName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Connection request failed: ${e.message}")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            _nearbyUsers.value = _nearbyUsers.value.filter { it.endpointId != "nearby_$endpointId" }
            Log.i(TAG, "📴 Lost endpoint: $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                try {
                    val json = String(bytes, Charsets.UTF_8)
                    val message = gson.fromJson(json, ChatMessage::class.java)
                    _messages.value = (_messages.value + message).takeLast(100)
                    Log.d(TAG, "💬 Received from ${message.senderName}: ${message.text}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse message: ${e.message}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No tracking necesario para mensajes cortos
        }
    }
}
