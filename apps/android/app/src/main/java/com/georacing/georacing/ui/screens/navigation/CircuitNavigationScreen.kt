package com.georacing.georacing.ui.screens.navigation

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.car.MapStyleManager
import com.georacing.georacing.navigation.NavigationState
import com.georacing.georacing.ui.components.GamifiedSpeedometer
import com.georacing.georacing.ui.components.HazardAlertOverlay
import com.georacing.georacing.ui.components.RaceStatusPill
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

/**
 * Pantalla de navegación móvil para GeoRacing.
 * 
 * Muestra un mapa de MapLibre con:
 * - Ruta calculada desde OSRM
 * - Posición actual del usuario
 * - Información de navegación (distancia, ETA, próxima maniobra)
 * - Detección automática de off-route y recálculo
 * - Instrucciones de voz progresivas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitNavigationScreen(
    navController: NavController,
    poiId: String? = null,  // ID del POI al que navegar (opcional)
    viewModel: CircuitNavigationViewModel = viewModel()
) {
    val context = LocalContext.current
    val navigationState by viewModel.navigationState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()
    val isFollowMode by viewModel.isFollowMode.collectAsState()
    val showArrivalDialog by viewModel.showArrivalDialog.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Waze-style state
    val circuitMode by viewModel.circuitMode.collectAsState()
    val activeHazards by viewModel.activeHazards.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    
    // MapView y MapLibreMap
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    
    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }
    
    // Solicitar permisos al inicio
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Iniciar navegación si se proporciona un POI
    LaunchedEffect(poiId) {
        if (poiId != null && locationPermissionGranted) {
            viewModel.startNavigationToPoi(poiId)
        }
    }
    
    // Actualizar mapa cuando cambie el estado de navegación
    LaunchedEffect(navigationState) {
        if (navigationState is NavigationState.Active) {
            val state = navigationState as NavigationState.Active
            updateMapRoute(mapLibreMap, state.route.points)
        }
    }
    
    // Actualizar posición del usuario en el mapa
    LaunchedEffect(currentLocation, isFollowMode) {
        currentLocation?.let { location ->
            if (isFollowMode) {
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(17.0)
                            .bearing(location.bearing.toDouble())
                            .tilt(45.0)
                            .build()
                    ),
                    1000
                )
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa de MapLibre
        AndroidView(
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    mapView = this
                    
                    getMapAsync { map ->
                        mapLibreMap = map
                        setupMap(ctx, map)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Race Status Pill (reemplaza TopAppBar)
        RaceStatusPill(
            circuitMode = circuitMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )
        
        // Hazard Alert Overlay (pop-ups de incidentes)
        HazardAlertOverlay(
            hazards = activeHazards,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Gamified Speedometer (esquina inferior izquierda)
        GamifiedSpeedometer(
            currentSpeed = currentSpeed,
            speedLimit = speedLimit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp)
        )
        
        // Action Buttons (esquina superior derecha - reemplazo de TopAppBar actions)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón de seguimiento de cámara
            FloatingActionButton(
                onClick = { viewModel.toggleFollowMode() },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFollowMode) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    "Modo seguimiento",
                    tint = if (isFollowMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Botón de cerrar
            FloatingActionButton(
                onClick = {
                    viewModel.stopNavigation()
                    navController.popBackStack()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Close, "Cerrar")
            }
            
            // Botón de abrir en Google Maps
            if (navigationState is NavigationState.Active) {
                FloatingActionButton(
                    onClick = { viewModel.openInGoogleMaps(context) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Navigation, "Abrir en Google Maps")
                }
            }
        }
        
        // Panel de información de navegación (parte inferior)
        if (navigationState is NavigationState.Active) {
            val state = navigationState as NavigationState.Active
            NavigationInfoPanel(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        // Indicador de carga
        if (navigationState is NavigationState.Loading || navigationState is NavigationState.WaitingForLocation) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = when (navigationState) {
                            is NavigationState.Loading -> "Calculando ruta..."
                            is NavigationState.WaitingForLocation -> "Obteniendo ubicación..."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Diálogo de llegada
        if (showArrivalDialog) {
            ArrivalDialog(
                destinationName = (navigationState as? NavigationState.Arrived)?.destinationName ?: "Destino",
                onDismiss = {
                    viewModel.dismissArrivalDialog()
                    navController.popBackStack()
                }
            )
        }
        
        // Snackbar de errores
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
            
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(message)
            }
        }
    }
    
    // Lifecycle del MapView
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }
}

/**
 * Configura el mapa inicial.
 */
private fun setupMap(context: Context, map: MapLibreMap) {
    val styleManager = MapStyleManager(context)
    
    map.setStyle(Style.Builder().fromJson(styleManager.STYLE_DAY_JSON)) { style ->
        // Habilitar componente de ubicación con icono de F1
        try {
            val locationComponentOptions = org.maplibre.android.location.LocationComponentOptions.builder(context)
                .pulseEnabled(true)
                .pulseColor(android.graphics.Color.BLUE)
                // Usar icono de F1 car (igual que Android Auto)
                .foregroundDrawable(R.drawable.ic_f1_car_scaled)
                .gpsDrawable(R.drawable.ic_f1_car_scaled)
                .bearingDrawable(R.drawable.ic_f1_car_scaled)
                .accuracyAlpha(0.4f)
                .build()
            
            map.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .locationComponentOptions(locationComponentOptions)
                        .useDefaultLocationEngine(true)
                        .build()
                )
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING_GPS
                renderMode = RenderMode.GPS
            }
        } catch (e: SecurityException) {
            android.util.Log.e("CircuitNavScreen", "Error activando componente de ubicación", e)
        }
        
        // Agregar fuente y capa para la ruta
        style.addSource(GeoJsonSource(MapStyleManager.SOURCE_ROUTE))
        
        style.addLayer(
            LineLayer(MapStyleManager.LAYER_ROUTE, MapStyleManager.SOURCE_ROUTE)
                .withProperties(
                    PropertyFactory.lineColor(Color.parseColor("#2563EB")),
                    PropertyFactory.lineWidth(6f),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round")
                )
        )
    }
}

/**
 * Actualiza la ruta en el mapa.
 */
private fun updateMapRoute(map: MapLibreMap?, routePoints: List<LatLng>) {
    map?.getStyle { style ->
        val source = style.getSourceAs<GeoJsonSource>(MapStyleManager.SOURCE_ROUTE)
        
        if (routePoints.isNotEmpty()) {
            val points = routePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = LineString.fromLngLats(points)
            val feature = Feature.fromGeometry(lineString)
            
            source?.setGeoJson(feature)
        }
    }
}

/**
 * Panel de información de navegación (parte inferior).
 */
@Composable
fun NavigationInfoPanel(
    state: NavigationState.Active,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    
    LiquidCard(
        modifier = modifier,
        backdrop = backdrop,
        cornerRadius = 16.dp,
        surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Partially transparent surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Próxima maniobra
            state.currentStep?.let { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de maniobra
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getManeuverIcon(step.maneuver.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Instrucción
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatDistance(state.distanceToNextManeuver),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = getManeuverText(step.maneuver.type, step.name),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Divider()
            
            // Distancia y ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Distancia restante
                InfoItem(
                    icon = Icons.Default.Route,
                    label = "Distancia",
                    value = formatDistance(state.remainingDistance)
                )
                
                // Tiempo estimado
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Tiempo",
                    value = formatDuration(state.estimatedTimeRemaining)
                )
                
                // Paso actual
                InfoItem(
                    icon = Icons.Default.TurnRight,
                    label = "Paso",
                    value = "${state.currentStepIndex + 1}/${state.route.steps.size}"
                )
            }
        }
    }
}

/**
 * Componente de información individual.
 */
@Composable
fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Diálogo de llegada al destino.
 */
@Composable
fun ArrivalDialog(
    destinationName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "¡Has llegado!",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text("Has llegado a $destinationName")
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Finalizar")
            }
        }
    )
}

/**
 * Obtiene el icono correspondiente a un tipo de maniobra.
 */
fun getManeuverIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        type.contains("left", ignoreCase = true) -> Icons.Default.TurnLeft
        type.contains("right", ignoreCase = true) -> Icons.Default.TurnRight
        type.contains("straight", ignoreCase = true) -> Icons.Default.ArrowUpward
        type.contains("roundabout", ignoreCase = true) -> Icons.Default.Sync
        else -> Icons.Default.Navigation
    }
}

/**
 * Genera texto de instrucción legible.
 */
fun getManeuverText(type: String, roadName: String?): String {
    val action = when {
        type.contains("turn") && type.contains("left") -> "Gira a la izquierda"
        type.contains("turn") && type.contains("right") -> "Gira a la derecha"
        type.contains("straight") -> "Continúa recto"
        type.contains("roundabout") -> "Toma la rotonda"
        type.contains("end of road") -> "Al final de la calle"
        else -> "Continúa"
    }
    
    return if (!roadName.isNullOrBlank() && roadName != "unknown") {
        "$action hacia $roadName"
    } else {
        action
    }
}

/**
 * Formatea distancia en metros a texto legible.
 */
fun formatDistance(meters: Double): String {
    return when {
        meters < 100 -> "${meters.roundToInt()} m"
        meters < 1000 -> "${(meters / 100).roundToInt() * 100} m"
        else -> "${"%.1f".format(meters / 1000)} km"
    }
}

/**
 * Formatea duración en segundos a texto legible.
 */
fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60).roundToInt()
    return when {
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "${hours}h ${remainingMinutes}min"
        }
    }
}
