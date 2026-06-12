package com.georacing.georacing.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.data.repository.CircuitLocationsRepository
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.model.CircuitNode
import com.georacing.georacing.domain.model.Confidence
import com.georacing.georacing.domain.model.NodeType
import com.georacing.georacing.domain.model.SeatInfo
import com.georacing.georacing.domain.repository.BeaconsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition

class MapViewModel(
    application: android.app.Application,
    beaconsRepository: BeaconsRepository,
    // PoiRepository deprecated/removed in favor of CircuitLocationsRepository
    userPreferences: UserPreferencesDataStore
) : androidx.lifecycle.AndroidViewModel(application) {

    // Camera Event to drive MapLibre View
    private val _cameraUpdate = kotlinx.coroutines.flow.MutableSharedFlow<CameraUpdate>()
    val cameraUpdate = _cameraUpdate.asSharedFlow()

    val beacons: StateFlow<List<BeaconConfig>> = beaconsRepository.getBeacons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Source of Truth: CircuitLocationsRepository (Official Dataset)
    private val _allNodes = MutableStateFlow(CircuitLocationsRepository.getAllNodes())
    val allNodes: StateFlow<List<CircuitNode>> = _allNodes.asStateFlow()

    val seatInfo: StateFlow<SeatInfo?> = userPreferences.seatInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedType = MutableStateFlow<NodeType?>(null)
    val selectedType = _selectedType.asStateFlow()

    val visibleNodes: StateFlow<List<CircuitNode>> = combine(allNodes, _selectedType) { nodes, type ->
        val filtered = if (type == null) nodes else nodes.filter { it.type == type }
        // Rule: Do not show PENDING nodes in standard map view to avoid confusion
        // (Only show them if explicitly debugging or if we decide to show them as "Works in Progress")
        // User said: "Si intenta ir a un PENDING: Mostrar aviso". "En el mapa: Ocultar o desactivar navegación a PENDING".
        // Let's show them but visual treatment will be different in MapScreen (Greyed out).
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mock user position for canvas fallback (deprecated by real GPS)
    val userPositionX = MutableStateFlow(0.5f)
    val userPositionY = MutableStateFlow(0.6f)

    fun filterNodes(type: NodeType?) {
        _selectedType.value = type
    }

    fun centerOnSeat() {
        val seat = seatInfo.value ?: return
        viewModelScope.launch {
            try {
                // Read grandstands.json from assets
                val json = getApplication<android.app.Application>().assets.open("grandstands.json").bufferedReader().use { it.readText() }
                val jsonObject = org.json.JSONObject(json)
                val grandstands = jsonObject.getJSONArray("grandstands")
                
                var foundLat = 0.0
                var foundLon = 0.0
                var found = false

                // Simplified match
                for (i in 0 until grandstands.length()) {
                    val g = grandstands.getJSONObject(i)
                    if (seat.grandstand.contains(g.getString("id"), ignoreCase = true) || 
                        seat.grandstand.contains(g.getString("name"), ignoreCase = true)) {
                        foundLat = g.getDouble("lat")
                        foundLon = g.getDouble("lon")
                        found = true
                        break
                    }
                }

                if (found) {
                    _cameraUpdate.emit(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(foundLat, foundLon))
                                .zoom(18.0)
                                .build()
                        )
                    )
                } else {
                    // Fallback to general circuit center
                     _cameraUpdate.emit(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(41.5700, 2.2600))
                                .zoom(15.0)
                                .build()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

