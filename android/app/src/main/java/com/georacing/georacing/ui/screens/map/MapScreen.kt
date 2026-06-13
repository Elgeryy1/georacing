package com.georacing.georacing.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.car.MapStyleManager
import com.georacing.georacing.data.event.isMapLayerEnabled
import com.georacing.georacing.data.event.isRouteEnabled
import com.georacing.georacing.data.event.labelForMapLayer
import com.georacing.georacing.data.event.labelForRoute
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.domain.model.Confidence
import com.georacing.georacing.domain.model.NodeType
import com.georacing.georacing.domain.repository.BeaconsRepository
import com.georacing.georacing.ui.components.Sidebar
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import com.google.gson.Gson
import com.google.gson.JsonObject

// Racing Dark Theme Colors
private val RacingAccent = Color(0xFF06B6D4)    // NeonCyan
private val SearchBarBg = Color(0xFF14141C)       // Dark surface
private val SearchBarText = Color(0xFFF8FAFC)     // Light text
private val ChipBg = Color(0xFF14141C)            // Dark surface
private val ChipBgSelected = Color(0xFF14141C).copy(alpha = 0.8f)
private val ChipText = Color(0xFFF8FAFC)          // Light text
private val SheetBg = Color(0xFF0E0E18)           // Darker surface
private val SubtleGray = Color(0xFF64748B)        // Slate

// Category chips data
data class CategoryChip(
    val emoji: String,
    val label: String,
    val type: NodeType?
)

private val defaultCategoryChips = listOf(
    CategoryChip("🍔", "Comida", NodeType.FOOD),
    CategoryChip("🚻", "Baños", NodeType.RESTROOM),
    CategoryChip("👕", "Merch", NodeType.MERCHANDISE),
    CategoryChip("🎟️", "Mi Puerta", NodeType.GATE)
)

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    beaconsRepository: BeaconsRepository,
    userPreferences: UserPreferencesDataStore
) {
    val viewModel: MapViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application)
                MapViewModel(app, beaconsRepository, userPreferences)
            }
        }
    )

    val selectedType by viewModel.selectedType.collectAsState()
    val visibleNodes by viewModel.visibleNodes.collectAsState()
    var isSidebarOpen by remember { mutableStateOf(false) }

    // Map Lifecycle & Context
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val mapStyleManager = remember { MapStyleManager(context) }
    var mapInstance by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var isMapStyleLoaded by remember { mutableStateOf(false) }

    // Permissions
    val permissions = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
    }
    val permissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(permissions = permissions)
    
    LaunchedEffect(Unit) { permissionsState.launchMultiplePermissionRequest() }
    
    // Camera Updates
    LaunchedEffect(mapInstance) {
        viewModel.cameraUpdate.collect { update -> mapInstance?.animateCamera(update) }
    }

    // Dynamic GeoJSON Update
    LaunchedEffect(visibleNodes, mapInstance, isMapStyleLoaded) {
        val map = mapInstance
        if (map != null && isMapStyleLoaded && map.style != null) {
            try {
                val features = visibleNodes.map { node ->
                    var iconImage = if (node.type == NodeType.GATE) MapStyleManager.IMAGE_GATE else MapStyleManager.IMAGE_PARKING
                    val properties = JsonObject()
                    properties.addProperty("name", node.name)
                    properties.addProperty("id", node.id)
                    properties.addProperty("icon_image", iconImage)
                    properties.addProperty("confidence", node.confidence.name)
                    
                    org.maplibre.geojson.Feature.fromGeometry(
                        org.maplibre.geojson.Point.fromLngLat(node.lon, node.lat),
                        properties
                    )
                }
                
                val featureCollection = org.maplibre.geojson.FeatureCollection.fromFeatures(features)
                val source = map.style!!.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>(MapStyleManager.SOURCE_POIS)
                source?.setGeoJson(featureCollection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Initialize MapLibre
    org.maplibre.android.MapLibre.getInstance(context)

    val mapView = remember {
        org.maplibre.android.maps.MapView(context).apply { onCreate(null) }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    GoogleMapsStyleContent(
        selectedType = selectedType,
        onFilterSelect = { viewModel.filterNodes(it) },
        onMenuClick = { isSidebarOpen = true },
        onLocationClick = {
            mapView.getMapAsync { map ->
                if (permissionsState.allPermissionsGranted && map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING
                }
            }
        },
        onCompassClick = {
            mapView.getMapAsync { map ->
                map.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                        org.maplibre.android.camera.CameraPosition.Builder()
                            .target(map.cameraPosition.target)
                            .zoom(map.cameraPosition.zoom)
                            .bearing(0.0)
                            .tilt(0.0)
                            .build()
                    ),
                    1000
                )
            }
        },
        onSearchClick = { navController.navigate(Screen.Search.route) },
        isSidebarOpen = isSidebarOpen,
        onSidebarClose = { isSidebarOpen = false },
        navController = navController,
        mapContent = {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.getMapAsync { map ->
                        mapInstance = map
                        
                        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(org.maplibre.android.geometry.LatLng(41.5700, 2.2600))
                            .zoom(15.0)
                            .build()

                        map.setStyle(com.georacing.georacing.data.map.MapLibreConfig.MAP_STYLE_URL) { style ->
                            mapStyleManager.applyLayers(style)
                            isMapStyleLoaded = true
                            
                            try {
                                val geoJson = context.assets.open("circuit_routes.geojson").bufferedReader().use { it.readText() }
                                val source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>(MapStyleManager.SOURCE_ROUTE)
                                source?.setGeoJson(geoJson)
                            } catch (e: Exception) { e.printStackTrace() }
                            
                            if (permissionsState.allPermissionsGranted) {
                                val locationComponent = map.locationComponent
                                val options = org.maplibre.android.location.LocationComponentActivationOptions.builder(context, style)
                                    .useDefaultLocationEngine(true)
                                    .build()
                                
                                locationComponent.activateLocationComponent(options)
                                locationComponent.isLocationComponentEnabled = true
                                
                                val lastLocation = locationComponent.lastKnownLocation
                                if (lastLocation != null) {
                                    map.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                            org.maplibre.android.camera.CameraPosition.Builder()
                                                .target(org.maplibre.android.geometry.LatLng(lastLocation.latitude, lastLocation.longitude))
                                                .zoom(16.0)
                                                .build()
                                        ),
                                        1000
                                    )
                                }
                                
                                locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING
                                locationComponent.renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                            }
                        }

                        if (!permissionsState.allPermissionsGranted) {
                             map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(com.georacing.georacing.data.map.MapLibreConfig.CircuitBarcelona.toLatLng())
                                .zoom(14.5)
                                .tilt(0.0)
                                .bearing(0.0)
                                .build()
                        }
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isCompassEnabled = false
                    }
                }
            )
            
            // Crowd Heatmap Overlay
            val crowdIntensity by com.georacing.georacing.debug.ScenarioSimulator.crowdIntensity.collectAsState()
            
            LaunchedEffect(crowdIntensity, mapInstance) {
                val map = mapInstance
                if (map != null) {
                    if (crowdIntensity > 0.5f) {
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                org.maplibre.android.camera.CameraPosition.Builder()
                                    .target(org.maplibre.android.geometry.LatLng(41.569502, 2.2541165))
                                    .zoom(16.0)
                                    .build()
                            ),
                            1500
                        )
                        map.uiSettings.isScrollGesturesEnabled = false
                        map.uiSettings.isZoomGesturesEnabled = false
                        map.uiSettings.isTiltGesturesEnabled = false
                        map.uiSettings.isRotateGesturesEnabled = false
                    } else {
                        map.uiSettings.isScrollGesturesEnabled = true
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isTiltGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = true
                    }
                }
            }

            // CrowdHeatmapOverlay disabled — fake data removed
        },
        permissionsGranted = permissionsState.allPermissionsGranted
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapsStyleContent(
    selectedType: NodeType?,
    onFilterSelect: (NodeType?) -> Unit,
    onMenuClick: () -> Unit,
    onLocationClick: () -> Unit,
    onCompassClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSidebarOpen: Boolean,
    onSidebarClose: () -> Unit,
    navController: NavController,
    mapContent: @Composable () -> Unit,
    permissionsGranted: Boolean
) {
    val activeEvent = LocalActiveEventConfig.current
    // Bottom sheet state
    var selectedPlace by remember { mutableStateOf<String?>(null) }
    val categoryChips = remember(activeEvent) {
        defaultCategoryChips.mapNotNull { chip ->
            val key = chip.type?.name ?: return@mapNotNull chip
            if (!activeEvent.isMapLayerEnabled(key)) {
                null
            } else {
                chip.copy(label = activeEvent.labelForMapLayer(key, chip.label).replace("ñ", "n"))
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Map Layer (Bottom)
        mapContent()
        
        // 2. UI Overlay Layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =============================================
            // TOP: Floating Search Pill
            // =============================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SearchBarBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearchClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu Icon
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = ChipText
                        )
                    }
                    
                    // Search Text
                    Text(
                        text = "Buscar puerta, grada o fan zone...",
                        color = SubtleGray,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RacingAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "G",
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            
            // =============================================
            // CHIPS: Category Filters
            // =============================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryChips) { chip ->
                    val isSelected = selectedType == chip.type
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelect(if (isSelected) null else chip.type) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(chip.emoji, fontSize = 14.sp)
                                Text(
                                    chip.label,
                                    color = if (isSelected) RacingAccent else ChipText
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ChipBg,
                            selectedContainerColor = ChipBgSelected
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF2A2A3C),
                            selectedBorderColor = RacingAccent,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // =============================================
        // FABs: Location and Compass Buttons (Bottom Right)
        // =============================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 200.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Compass FAB
            FloatingActionButton(
                onClick = onCompassClick,
                containerColor = Color(0xFF14141C),
                contentColor = ChipText,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = "Orientar mapa al norte",
                    tint = ChipText
                )
            }
            
            // Location FAB
            FloatingActionButton(
                onClick = onLocationClick,
                containerColor = Color(0xFF14141C),
                contentColor = RacingAccent,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    if (permissionsGranted) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Mi ubicación",
                    tint = if (permissionsGranted) RacingAccent else SubtleGray
                )
            }
        }
        
        // =============================================
        // BOTTOM SHEET: Place Info / Explore
        // =============================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = SheetBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2A2A3C))
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (selectedPlace != null) {
                        // Selected Place Info
                        Text(
                            selectedPlace!!,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SearchBarText
                        )
                    } else {
                        // Explore Mode
                        Text(
                            "Explorar el evento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = SearchBarText
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Toca el mapa para ver información de los puntos de interés",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SubtleGray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Quick Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Navigation,
                                label = activeEvent.ctaLabel.replace("ñ", "n"),
                                onClick = { navController.navigate(Screen.CircuitDestinations.route) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.DirectionsWalk,
                                label = activeEvent.labelForRoute(Screen.CircuitDestinations.route, "A mi puerta").replace("ñ", "n"),
                                onClick = { navController.navigate(Screen.CircuitDestinations.route) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.LocalParking,
                                label = activeEvent.labelForRoute(Screen.Parking.route, "Mi Coche").replace("ñ", "n"),
                                onClick = { navController.navigate(Screen.Parking.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            // Fill the gap between the card and the bottom navigation bar + system nav
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp + navBarInset)
                    .background(SheetBg)
            )
        }

        // Sidebar
        Sidebar(
            isOpen = isSidebarOpen,
            onClose = onSidebarClose
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = activeEvent.shortName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = RacingAccent,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                listOf(
                    Triple(activeEvent.labelForRoute(Screen.Home.route, "Inicio"), Icons.Default.Home, Screen.Home.route),
                    Triple(activeEvent.labelForRoute(Screen.SeatSetup.route, "Mi Localidad"), Icons.Default.EventSeat, Screen.SeatSetup.route),
                    Triple(activeEvent.labelForRoute(Screen.IncidentReport.route, "Incidencias"), Icons.Default.ReportProblem, Screen.IncidentReport.route),
                    Triple("Configuración", Icons.Default.Settings, Screen.Settings.route)
                ).filter { (_, _, route) -> activeEvent.isRouteEnabled(route) }.forEach { (text, icon, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSidebarClose()
                                navController.navigate(route)
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = ChipText, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text, 
                            color = SearchBarText, 
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2A2A3C))),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF14141C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = RacingAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = SearchBarText,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    GeoRacingTheme {
        GoogleMapsStyleContent(
            selectedType = null,
            onFilterSelect = {},
            onMenuClick = {},
            onLocationClick = {},
            onCompassClick = {},
            onSearchClick = {},
            isSidebarOpen = false,
            onSidebarClose = {},
            navController = androidx.navigation.compose.rememberNavController(),
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E0E18)), 
                    contentAlignment = Alignment.Center
                ) {
                    Text("Map", color = SubtleGray)
                }
            },
            permissionsGranted = true
        )
    }
}
