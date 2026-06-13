package com.georacing.georacing.ui.screens.group

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.model.GroupMemberLocation
import com.georacing.georacing.data.repository.NetworkGroupRepository
import com.georacing.georacing.data.repository.NetworkUserRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el estado de la pantalla "Mi grupo"
 * Maneja ubicación en tiempo real y lista de miembros usando la API del NAS
 */
class GroupMapViewModel(
    private val repository: NetworkGroupRepository
) : ViewModel() {
    private val userRepository = NetworkUserRepository()
    
    // Estado de ubicaciones de miembros del grupo
    private val _groupLocations = MutableStateFlow<List<GroupMemberLocation>>(emptyList())
    val groupLocations: StateFlow<List<GroupMemberLocation>> = _groupLocations.asStateFlow()
    
    // Estado del switch "Compartir mi ubicación"
    private val _isSharingLocation = MutableStateFlow(false)
    val isSharingLocation: StateFlow<Boolean> = _isSharingLocation.asStateFlow()
    
    // Estado de permisos de ubicación
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    // Estado de carga y errores
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // ID del grupo activo (en producción vendría del perfil del usuario)
    private var activeGroupId: String = "default_group"
    
    // Cliente de ubicación de Google
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationUpdateJob: Job? = null
    
    // Job para escuchar miembros
    private var membersListenJob: Job? = null
    
    // Scanner local para detectar compañeros por BLE
    @SuppressLint("StaticFieldLeak") // ViewModel lifecycle is shorter than App Context but we use it carefully
    private var beaconScanner: com.georacing.georacing.data.ble.BeaconScanner? = null
    
    companion object {
        private const val TAG = "GroupMapViewModel"
        private const val LOCATION_UPDATE_INTERVAL = 20_000L // 20 segundos para ahorrar batería
        private const val LOCATION_MIN_DISTANCE_METERS = 15f
        private const val LOCATION_BALANCED_UPDATE_INTERVAL = 30_000L
        
        // Coordenadas del Circuit de Barcelona-Catalunya
        const val CIRCUIT_LAT = 41.5700
        const val CIRCUIT_LNG = 2.2611
    }
    
    /**
     * Inicia la escucha de ubicaciones del grupo
     */
    fun startListeningGroupLocations(groupId: String, context: Context) {
        activeGroupId = groupId
        Log.d(TAG, "Iniciando polling para grupo: $groupId")
        _groupLocations.value = emptyList()
        ensureBackendEntities(groupId)
        
        // Init Scanner locally for Map View
        beaconScanner?.stopScanning()
        beaconScanner = com.georacing.georacing.data.ble.BeaconScanner(context)
        beaconScanner?.startScanning()

        // Restaurar estado de sharing desde SharedPreferences
        val prefs = context.getSharedPreferences("georacing_prefs", Context.MODE_PRIVATE)
        val wasSharing = prefs.getBoolean("is_sharing_$groupId", false)
        
        if (wasSharing) {
            Log.d(TAG, "Restaurando estado de sharing: ACTIVO")
            _isSharingLocation.value = true
            checkLocationPermission(context)
            if (_hasLocationPermission.value) {
                startSharingLocation(context)
            }
        }
        
        membersListenJob?.cancel()
        membersListenJob = viewModelScope.launch {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid ?: ""
            val currentName = currentUser?.displayName
            
            kotlinx.coroutines.flow.combine(
                repository.getGroupMembers(groupId, pollIntervalMs = 10_000L),
                beaconScanner!!.detectedUsers
            ) { netMembers, bleMembers ->
                 // PRIORITY 1: ONLINE - If we have network data, use ONLY network data.
                 // This effectively "disables" Bluetooth visualization when online.
                 if (netMembers.isNotEmpty()) {
                     // Log.d(TAG, "Online Mode: ${netMembers.size} members. BLE Visualization Disabled.") // Removed to reduce spam
                     return@combine netMembers
                 }
                 
                 // PRIORITY 2: OFFLINE - If Network is empty/down, show BLE signals.
                 // Log.d(TAG, "Offline Mode: Network empty/down. Showing ${bleMembers.size} BLE signals.") // Removed to reduce spam
                 
                 bleMembers.filter { it.latitude != null && it.longitude != null }.map { bleUser ->
                      GroupMemberLocation(
                          userId = "ble_${bleUser.idHash}",
                          displayName = "Guest ${Integer.toHexString(bleUser.idHash).uppercase().takeLast(4)}",
                          latitude = bleUser.latitude!!,
                          longitude = bleUser.longitude!!,
                          photoUrl = null,
                          lastUpdated = com.google.firebase.Timestamp(java.util.Date(bleUser.timestamp)),
                          sharing = true
                      )
                 }
            }.collectLatest { locations ->
                // Normalizamos nombres para evitar mostrar IDs crudos
                val updatedLocations = locations.map { member ->
                    val name = when {
                        member.userId == currentUserId && !currentName.isNullOrBlank() ->
                            currentName
                        !member.displayName.isNullOrBlank() &&
                                !member.displayName!!.startsWith("User ", ignoreCase = true) ->
                            member.displayName
                        else -> "Miembro"
                    }
                    member.copy(displayName = name)
                }.distinctBy { it.userId }
                _groupLocations.value = updatedLocations
                Log.d(TAG, "Ubicaciones actualizadas: ${updatedLocations.size} miembros")
            }
        }
    }

    /**
     * Asegura que el usuario y el grupo existan en el backend (idempotente)
     */
    private fun ensureBackendEntities(groupId: String) {
        viewModelScope.launch {
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@launch
                // Registrar/actualizar usuario
                userRepository.registerUser(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString()
                )
                // Registrar/actualizar grupo
                repository.createGroup(
                    groupId = groupId,
                    ownerUserId = user.uid,
                    groupName = groupId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error asegurando entidades backend", e)
                _errorMessage.value = "Error al sincronizar grupo. Reintenta más tarde."
            }
        }
    }
    
    /**
     * Verifica si la app tiene permisos de ubicación
     */
    fun checkLocationPermission(context: Context) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _hasLocationPermission.value = hasFineLocation || hasCoarseLocation
        Log.d(TAG, "Permiso de ubicación: ${_hasLocationPermission.value}")
    }
    
    /**
     * Inicia el servicio de compartir ubicación en tiempo real
     */
    @SuppressLint("MissingPermission")
    fun startSharingLocation(context: Context) {
        if (!_hasLocationPermission.value) {
            _errorMessage.value = "Se necesitan permisos de ubicación"
            return
        }
        
        _isSharingLocation.value = true
        
        // Guardar preferencia
        val prefs = context.getSharedPreferences("georacing_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_sharing_$activeGroupId", true).apply()
        
        // Inicializar cliente de ubicación
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
        
        // Cancelar job anterior si existe
        locationUpdateJob?.cancel()
        
        // Obtener UID y nombre del usuario
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "unknown_user"
        val displayName = currentUser?.displayName ?: "Usuario"
        
        // Job que actualiza la ubicación periódicamente
        locationUpdateJob = viewModelScope.launch {
            try {
                while (isActive && _isSharingLocation.value) {
                    try {
                        // Solicitar ubicación con prioridad balanceada y distancia mínima
                        val request = LocationRequest.Builder(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            LOCATION_BALANCED_UPDATE_INTERVAL
                        ).setMinUpdateDistanceMeters(LOCATION_MIN_DISTANCE_METERS)
                            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                            .build()

                        fusedLocationClient?.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            null
                        )?.addOnSuccessListener { location ->
                            if (location != null) {
                                viewModelScope.launch {
                                    val result = repository.sendLocation(
                                        userId = userId,
                                        groupName = activeGroupId,
                                        lat = location.latitude,
                                        lon = location.longitude,
                                        displayName = displayName
                                    )
                                    
                                    if (result.isSuccess) {
                                        Log.d(TAG, "✅ Ubicación enviada: (${location.latitude}, ${location.longitude})")
                                    } else {
                                        Log.e(TAG, "❌ Error enviando ubicación", result.exceptionOrNull())
                                    }
                                }
                            } else {
                                Log.w(TAG, "⚠️ Ubicación nula (GPS desactivado o sin señal)")
                            }
                        }?.addOnFailureListener { e ->
                            Log.e(TAG, "Error obteniendo ubicación fresca", e)
                        }

                        delay(LOCATION_UPDATE_INTERVAL)
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Job de ubicación cancelado")
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error obteniendo ubicación", e)
                        _errorMessage.value = "Error al obtener ubicación: ${e.message}"
                        delay(5000) // Esperar un poco si hay error antes de reintentar
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Limpieza de compartir ubicación")
            }
        }
        
        Log.d(TAG, "🚀 Compartir ubicación iniciado")
    }
    
    /**
     * Detiene el servicio de compartir ubicación
     */
    fun stopSharingLocation(context: Context? = null) {
        _isSharingLocation.value = false
        
        // Guardar preferencia (si tenemos contexto)
        context?.let {
            val prefs = it.getSharedPreferences("georacing_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_sharing_$activeGroupId", false).apply()
        }
        
        // Cancelar el job de forma segura
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        
        Log.d(TAG, "🛑 Compartir ubicación detenido localmente")
    }
    
    /**
     * Alterna el estado de compartir ubicación
     */
    fun toggleSharingLocation(context: Context) {
        if (_isSharingLocation.value) {
            stopSharingLocation(context)
        } else {
            startSharingLocation(context)
        }
    }
    
    /**
     * Abandona el grupo actual
     */
    fun leaveGroup(context: Context) {
        val groupToLeave = activeGroupId
        // 1. Detener compartir ubicación
        stopSharingLocation(context)
        
        // 2. Limpiar preferencia de grupo activo
        val prefs = context.getSharedPreferences("georacing_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("active_group_id").apply()
        
        // 3. Limpiar estado local
        clearGroupData()

        // 4. Borrar mi fila de group_gps en backend
        viewModelScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (!userId.isNullOrEmpty() && groupToLeave.isNotEmpty()) {
                    repository.removeUserFromGroup(userId, groupToLeave)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing user from group backend", e)
                _errorMessage.value = "Error al abandonar el grupo. Reintenta más tarde."
            }
        }
        
        Log.d(TAG, "👋 Grupo abandonado: $activeGroupId")
    }

    /**
     * Limpia el estado local del grupo y detiene escuchas
     */
    fun clearGroupData() {
        membersListenJob?.cancel()
        _groupLocations.value = emptyList()
        _isSharingLocation.value = false
        _errorMessage.value = null
        activeGroupId = ""
    }
    
    /**
     * Obtiene la ubicación actual del dispositivo (una sola vez)
     */
    @SuppressLint("MissingPermission")
    fun getMyCurrentLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
        if (!_hasLocationPermission.value) {
            _errorMessage.value = "Se necesitan permisos de ubicación"
            return
        }
        
        try {
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }
            
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Ubicación obtenida: (${location.latitude}, ${location.longitude})")
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    Log.w(TAG, "Ubicación nula")
                    _errorMessage.value = "No se pudo obtener la ubicación"
                }
            }?.addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo ubicación", e)
                _errorMessage.value = "Error obteniendo ubicación: ${e.message}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getMyCurrentLocation", e)
            _errorMessage.value = "Error: ${e.message}"
        }
    }
    
    /**
     * Limpieza al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        membersListenJob?.cancel()
        locationUpdateJob?.cancel()
        beaconScanner?.stopScanning()
        Log.d(TAG, "ViewModel limpiado")
    }
    
    /**
     * Limpia mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
