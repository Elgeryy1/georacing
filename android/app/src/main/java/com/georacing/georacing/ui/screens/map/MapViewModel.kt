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
import com.georacing.georacing.domain.navigation.PedestrianPathfinder
import com.georacing.georacing.domain.repository.BeaconsRepository
import com.georacing.georacing.domain.traffic.CircuitTrafficProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val userPreferences: UserPreferencesDataStore
) : androidx.lifecycle.AndroidViewModel(application) {

    // ── Pedestrian routing (ideas P1 sombra, 3 anti-cola, 4 accesible, 9 ETA) ──
    // Feeds congestion into the A* graph from the backend (CircuitTrafficProvider).
    private val trafficProvider by lazy { CircuitTrafficProvider() }

    /** GeoJSON LineString of the active pedestrian route, or null if none. */
    private val _routeGeoJson = MutableStateFlow<String?>(null)
    val routeGeoJson: StateFlow<String?> = _routeGeoJson.asStateFlow()

    /** Human-readable ETA/distance summary for the active route, e.g. "12 min · 540 m". */
    private val _routeInfo = MutableStateFlow<String?>(null)
    val routeInfo: StateFlow<String?> = _routeInfo.asStateFlow()

    /** Pedestrian destinations the user can navigate to (POIs inside the circuit graph). */
    val pedestrianDestinations: List<PedestrianPathfinder.PathNode> =
        PedestrianPathfinder.getAllNodes()

    /**
     * Computes a pedestrian route from the user's GPS position to a graph node and
     * publishes it as GeoJSON for the map + an ETA string.
     *
     * Honors:
     *  - avoidStairs from UserPreferencesDataStore (idea 4, accessibility)
     *  - preferShadow automatically between 12:00–16:00 (idea P1, thermal/shade)
     *  - dynamic congestion weights fed by CircuitTrafficProvider (idea 3, anti-cola)
     *  - returns ETA (idea 9)
     */
    fun navigateToPedestrianNode(destinationNodeId: String, userPosition: LatLng?) {
        viewModelScope.launch {
            try {
                // Idea 3: make sure the graph has the latest backend congestion before routing.
                runCatching { trafficProvider.refreshNow() }

                val avoidStairs = userPreferences.avoidStairs.first()
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val preferShadow = hour in 12..15 // hot midday hours

                val route = if (userPosition != null) {
                    PedestrianPathfinder.findRouteFromGps(
                        userPosition = userPosition,
                        toId = destinationNodeId,
                        avoidStairs = avoidStairs,
                        preferShadow = preferShadow
                    )
                } else {
                    // No GPS yet: route from the main access as a sensible fallback.
                    PedestrianPathfinder.findRoute(
                        fromId = "gate_main",
                        toId = destinationNodeId,
                        avoidStairs = avoidStairs,
                        preferShadow = preferShadow
                    )
                }

                if (route == null || route.nodes.size < 2) {
                    _routeGeoJson.value = null
                    _routeInfo.value = "No se pudo calcular la ruta"
                    return@launch
                }

                _routeGeoJson.value = buildRouteGeoJson(route)

                val minutes = (route.estimatedTimeSeconds / 60.0).toInt().coerceAtLeast(1)
                val meters = route.totalDistance.toInt()
                val tags = buildList {
                    if (route.usedAccessibleRoute) add("sin escaleras")
                    if (route.usedShadowRoute) add("con sombra")
                }
                val suffix = if (tags.isEmpty()) "" else " · ${tags.joinToString(", ")}"
                _routeInfo.value = "$minutes min · $meters m$suffix"

                // Center the map on the destination.
                val dest = route.nodes.last()
                _cameraUpdate.emit(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(dest.position)
                            .zoom(17.0)
                            .build()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _routeInfo.value = "Error calculando la ruta"
            }
        }
    }

    /** Clears the active pedestrian route from the map. */
    fun clearRoute() {
        _routeGeoJson.value = null
        _routeInfo.value = null
    }

    /** Builds a GeoJSON FeatureCollection with a single LineString for the route. */
    private fun buildRouteGeoJson(route: PedestrianPathfinder.PathResult): String {
        val coordinates = route.nodes.map { node ->
            org.maplibre.geojson.Point.fromLngLat(node.position.longitude, node.position.latitude)
        }
        val line = org.maplibre.geojson.LineString.fromLngLats(coordinates)
        val feature = org.maplibre.geojson.Feature.fromGeometry(line)
        return org.maplibre.geojson.FeatureCollection.fromFeature(feature).toJson()
    }

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

