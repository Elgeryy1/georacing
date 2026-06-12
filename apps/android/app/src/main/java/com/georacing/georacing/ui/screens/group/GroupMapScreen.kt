package com.georacing.georacing.ui.screens.group

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.data.map.MapLibreConfig
import com.georacing.georacing.data.model.GroupMemberLocation
import com.georacing.georacing.data.repository.NetworkGroupRepository
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.components.HomeIconButton
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMapScreen(
    navController: NavController,
    viewModel: GroupMapViewModel
) {
    val context = LocalContext.current
    
    val groupLocations by viewModel.groupLocations.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    var showMembersSheet by remember { mutableStateOf(false) }
    var focusMember by remember { mutableStateOf<GroupMemberLocation?>(null) }
    
    // Estado para centrar el mapa en mi ubicación
    var shouldCenterOnMyLocation by remember { mutableStateOf(false) }

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            Log.d("GroupMapScreen", "Permisos de ubicación concedidos")
            viewModel.checkLocationPermission(context)
        } else {
            Log.d("GroupMapScreen", "Permisos de ubicación denegados")
        }
    }
    
    // Inicializar MapLibre una sola vez al entrar a la pantalla
    DisposableEffect(Unit) {
        try {
            MapLibre.getInstance(context)
            Log.d("GroupMapScreen", "MapLibre inicializado")
        } catch (e: Exception) {
            Log.e("GroupMapScreen", "Error inicializando MapLibre", e)
        }
        
        viewModel.checkLocationPermission(context)
        
        // Obtener el ID del grupo activo de las preferencias
        val prefs = context.getSharedPreferences("georacing_prefs", android.content.Context.MODE_PRIVATE)
        val activeGroupId = prefs.getString("active_group_id", null)
        
        if (activeGroupId != null) {
            viewModel.startListeningGroupLocations(activeGroupId, context)
        } else {
            Log.d("GroupMapScreen", "Sin grupo activo; no se inicia listener")
        }
        
        onDispose {
            Log.d("GroupMapScreen", "GroupMapScreen disposed")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa del Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    HomeIconButton {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (hasLocationPermission) {
                        shouldCenterOnMyLocation = true
                        viewModel.getMyCurrentLocation(context) { lat, lng ->
                            Log.d("GroupMapScreen", "Ubicación obtenida: ($lat, $lng)")
                        }
                    } else {
                        // Solicitar permisos
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mapa con MapLibre
            MapLibreMapView(
                groupLocations = groupLocations,
                shouldCenterOnMyLocation = shouldCenterOnMyLocation,
                onLocationCentered = { shouldCenterOnMyLocation = false },
                onMapReady = { map ->
                    Log.d("GroupMapScreen", "Mapa listo con ${groupLocations.size} miembros")
                },
                focusMember = focusMember,
                onFocusHandled = { focusMember = null },
                modifier = Modifier.fillMaxSize()
            )
            
            // Mostrar errores
            errorMessage?.let { error ->
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
                    Text(error)
                }
            }
            
            // Contador de miembros compartiendo
            if (groupLocations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "${groupLocations.filter { it.sharing }.size} compartiendo",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Botón flotante para lista de miembros
            FloatingActionButton(
                onClick = { showMembersSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.List, contentDescription = "Ver miembros")
            }

            if (showMembersSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMembersSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Miembros del grupo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (groupLocations.isEmpty()) {
                            Text(
                                text = "No hay miembros",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            groupLocations.forEach { member ->
                                ListItem(
                                    headlineContent = { Text(member.displayName ?: member.userId) },
                                    supportingContent = { Text(member.getStatusText()) },
                                    leadingContent = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    modifier = Modifier.clickable {
                                        focusMember = member
                                        showMembersSheet = false
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MapLibreMapView(
    groupLocations: List<GroupMemberLocation>,
    shouldCenterOnMyLocation: Boolean,
    onLocationCentered: () -> Unit,
    onMapReady: (MapLibreMap) -> Unit,
    focusMember: GroupMemberLocation?,
    onFocusHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Estados que sobreviven recomposiciones
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    var currentMarkers by remember { mutableStateOf<Map<String, org.maplibre.android.annotations.Marker>>(emptyMap()) }
    
    // Estado para el miembro seleccionado
    var selectedMember by remember { mutableStateOf<GroupMemberLocation?>(null) }

    // Actualizar selectedMember si la lista cambia (para mantener datos frescos)
    LaunchedEffect(groupLocations) {
        if (selectedMember != null) {
            selectedMember = groupLocations.find { it.userId == selectedMember?.userId }
        }
    }

    fun assetExists(ctx: Context, name: String): Boolean {
        return try {
            ctx.assets.open(name).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Centrar en miembro seleccionado desde la sheet
    LaunchedEffect(focusMember, isStyleLoaded) {
        if (focusMember != null && isStyleLoaded) {
            val map = mapLibreMap
            if (map != null) {
                try {
                    val member = focusMember!!
                    val camera = CameraPosition.Builder()
                        .target(LatLng(member.latitude, member.longitude))
                        .zoom(17.5)
                        .build()
                    map.cameraPosition = camera
                    selectedMember = member
                } catch (e: Exception) {
                    Log.e("MapLibreMapView", "Error centrando en miembro", e)
                } finally {
                    onFocusHandled()
                }
            }
        }
    }

    fun addRoutesOverlay(style: Style) {
        try {
            val sourceId = "circuit_routes_source"
            val layerId = "circuit_routes_layer"
            val assetName = MapLibreConfig.ROUTES_GEOJSON_ASSET
            // Carga GeoJSON desde assets si existe
            if (assetExists(context, assetName)) {
                if (style.getSource(sourceId) == null) {
                    style.addSource(GeoJsonSource(sourceId, URI("asset://$assetName")))
                }
                if (style.getLayer(layerId) == null) {
                    val layer = LineLayer(layerId, sourceId).withProperties(
                        PropertyFactory.lineColor(MapLibreConfig.ROUTES_VEHICLE_COLOR),
                        PropertyFactory.lineWidth(MapLibreConfig.ROUTES_WIDTH)
                    )
                    style.addLayer(layer)
                }
                Log.d("MapLibreMapView", "Overlay de rutas cargado desde assets")
            } else {
                Log.d("MapLibreMapView", "No se encontró GeoJSON de rutas en assets")
            }
        } catch (e: Exception) {
            Log.e("MapLibreMapView", "Error añadiendo rutas al mapa", e)
        }
    }

    fun addPoisOverlay(style: Style) {
        try {
            val sourceId = MapLibreConfig.POI_SOURCE_ID
            val layerId = MapLibreConfig.POI_LAYER_ID
            val assetName = MapLibreConfig.POIS_GEOJSON_ASSET

            if (!assetExists(context, assetName)) {
                Log.d("MapLibreMapView", "No se encontró GeoJSON de POIs en assets")
                return
            }

            if (style.getSource(sourceId) == null) {
                style.addSource(GeoJsonSource(sourceId, URI("asset://$assetName")))
            }
            if (style.getLayer(layerId) == null) {
                val layer = CircleLayer(layerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(MapLibreConfig.POI_RADIUS),
                    PropertyFactory.circleColor(MapLibreConfig.POI_COLOR_DEFAULT),
                    PropertyFactory.circleOpacity(0.9f)
                )
                style.addLayer(layer)
            }
            Log.d("MapLibreMapView", "Overlay de POIs cargado desde assets")
        } catch (e: Exception) {
            Log.e("MapLibreMapView", "Error añadiendo POIs al mapa", e)
        }
    }
    
    // Centrar mapa en mi ubicación cuando se solicita
    LaunchedEffect(shouldCenterOnMyLocation) {
        if (shouldCenterOnMyLocation && isStyleLoaded) {
            val map = mapLibreMap
            if (map != null) {
                try {
                    // Obtener ubicación actual
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    
                    // Verificar permisos
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        ).addOnSuccessListener { location ->
                            if (location != null) {
                                val myPosition = CameraPosition.Builder()
                                    .target(LatLng(location.latitude, location.longitude))
                                    .zoom(16.0) // Zoom más cercano para ubicación personal
                                    .build()
                                
                                map.cameraPosition = myPosition
                                Log.d("MapLibreMapView", "Mapa centrado en mi ubicación: (${location.latitude}, ${location.longitude})")
                            } else {
                                Log.w("MapLibreMapView", "Ubicación nula, no se puede centrar")
                            }
                            onLocationCentered()
                        }.addOnFailureListener { e ->
                            Log.e("MapLibreMapView", "Error obteniendo ubicación", e)
                            onLocationCentered()
                        }
                    } else {
                        Log.w("MapLibreMapView", "Sin permisos de ubicación")
                        onLocationCentered()
                    }
                } catch (e: Exception) {
                    Log.e("MapLibreMapView", "Error centrando en ubicación", e)
                    onLocationCentered()
                }
            }
        }
    }
    
    // Gestionar ciclo de vida del MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        Log.d("MapLibreMapView", "Lifecycle: ON_START")
                        mapView?.onStart()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("MapLibreMapView", "Lifecycle: ON_RESUME")
                        mapView?.onResume()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        Log.d("MapLibreMapView", "Lifecycle: ON_PAUSE")
                        mapView?.onPause()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        Log.d("MapLibreMapView", "Lifecycle: ON_STOP")
                        mapView?.onStop()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Log.d("MapLibreMapView", "Lifecycle: ON_DESTROY")
                        // Limpiar marcadores
                        currentMarkers.values.forEach { marker ->
                            try {
                                mapLibreMap?.removeMarker(marker)
                            } catch (e: Exception) {
                                Log.e("MapLibreMapView", "Error eliminando marcador", e)
                            }
                        }
                        currentMarkers = emptyMap()
                        
                        // Destruir mapa
                        mapView?.onDestroy()
                        mapView = null
                        mapLibreMap = null
                        isStyleLoaded = false
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("MapLibreMapView", "Error en lifecycle event: $event", e)
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            Log.d("MapLibreMapView", "DisposableEffect: onDispose")
            lifecycleOwner.lifecycle.removeObserver(observer)
            
            // Limpieza final solo si no se hizo en ON_DESTROY
            try {
                currentMarkers.values.forEach { marker ->
                    mapLibreMap?.removeMarker(marker)
                }
                currentMarkers = emptyMap()
                
                mapView?.onDestroy()
            } catch (e: Exception) {
                Log.e("MapLibreMapView", "Error en cleanup final", e)
            }
        }
    }
    
    // Actualizar marcadores cuando cambien las ubicaciones (solo si el estilo está cargado)
    LaunchedEffect(groupLocations, isStyleLoaded) {
        if (!isStyleLoaded) {
            Log.d("MapLibreMapView", "Estilo no cargado aún, esperando...")
            return@LaunchedEffect
        }
        
        val map = mapLibreMap ?: run {
            Log.w("MapLibreMapView", "MapLibreMap es null, no se pueden actualizar marcadores")
            return@LaunchedEffect
        }
        
        try {
            Log.d("MapLibreMapView", "Actualizando marcadores: ${groupLocations.size} ubicaciones")
            
            // Filtrar solo miembros que están compartiendo con coordenadas válidas
            val validLocations = groupLocations.filter { member ->
                member.sharing && 
                member.latitude != 0.0 && 
                member.longitude != 0.0 &&
                member.latitude >= -90.0 && member.latitude <= 90.0 &&
                member.longitude >= -180.0 && member.longitude <= 180.0
            }
            
            Log.d("MapLibreMapView", "Ubicaciones válidas: ${validLocations.size}")
            
            // IDs actuales
            val currentIds = validLocations.map { it.userId }.toSet()
            
            // Eliminar marcadores de usuarios que ya no están
            val markersToRemove = currentMarkers.keys.filter { it !in currentIds }
            markersToRemove.forEach { userId ->
                currentMarkers[userId]?.let { marker ->
                    try {
                        map.removeMarker(marker)
                        Log.d("MapLibreMapView", "Marcador eliminado: $userId")
                    } catch (e: Exception) {
                        Log.e("MapLibreMapView", "Error eliminando marcador $userId", e)
                    }
                }
            }
            
            // Actualizar mapa de marcadores
            val updatedMarkers = currentMarkers.toMutableMap()
            markersToRemove.forEach { updatedMarkers.remove(it) }
            
            // Añadir o actualizar marcadores
            validLocations.forEach { member ->
                try {
                    val existingMarker = updatedMarkers[member.userId]
                    
                    if (existingMarker != null) {
                        // Actualizar posición del marcador existente
                        existingMarker.position = LatLng(member.latitude, member.longitude)
                        existingMarker.title = member.displayName ?: "Usuario" // Actualizar título también
                        existingMarker.snippet = member.getStatusText()
                        Log.d("MapLibreMapView", "Marcador actualizado: ${member.displayName}")
                    } else {
                        // Crear nuevo marcador
                        val markerOptions = MarkerOptions()
                            .position(LatLng(member.latitude, member.longitude))
                            .title(member.displayName ?: "Usuario")
                            .snippet(member.getStatusText())
                        
                        val newMarker = map.addMarker(markerOptions)
                        if (newMarker != null) {
                            updatedMarkers[member.userId] = newMarker
                            Log.d("MapLibreMapView", "📍 Marcador creado para: ${member.displayName} en (${member.latitude}, ${member.longitude})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MapLibreMapView", "Error gestionando marcador para ${member.userId}", e)
                }
            }
            
            currentMarkers = updatedMarkers
            Log.d("MapLibreMapView", "Total marcadores en mapa: ${currentMarkers.size}")
            
        } catch (e: Exception) {
            Log.e("MapLibreMapView", "Error actualizando marcadores", e)
        }
    }
    
    Box(modifier = modifier) {
        // Crear el MapView
        AndroidView(
            factory = { ctx ->
                Log.d("MapLibreMapView", "AndroidView factory: Creando MapView")
                
                MapView(ctx).apply {
                    mapView = this
                    
                    // Inicializar con Bundle vacío
                    try {
                        onCreate(Bundle())
                        Log.d("MapLibreMapView", "MapView.onCreate() completado")
                    } catch (e: Exception) {
                        Log.e("MapLibreMapView", "Error en MapView.onCreate()", e)
                    }
                    
                    // Obtener referencia al mapa de forma asíncrona
                    try {
                        getMapAsync { map ->
                            Log.d("MapLibreMapView", "getMapAsync: Mapa recibido")
                            mapLibreMap = map
                            


                            // Función para habilitar el componente de ubicación
                            fun enableLocationComponent(style: Style) {
                                try {
                                    // Verificar permisos de nuevo por seguridad
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val locationComponent = map.locationComponent
                                        val locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, style)
                                            .build()

                                        locationComponent.activateLocationComponent(locationComponentActivationOptions)
                                        locationComponent.isLocationComponentEnabled = true
                                        locationComponent.cameraMode = CameraMode.TRACKING
                                        locationComponent.renderMode = RenderMode.COMPASS
                                        
                                        Log.d("MapLibreMapView", "✅ Componente de ubicación nativo activado")
                                    } else {
                                        Log.w("MapLibreMapView", "⚠️ No se pudo activar LocationComponent: faltan permisos")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MapLibreMapView", "❌ Error activando LocationComponent", e)
                                }
                            }

                            // Función local para configurar el mapa tras cargar el estilo
                            fun configureMapAfterStyleLoad(style: Style) {
                                try {
                                    Log.d("MapLibreMapView", "✅ Estilo cargado correctamente")
                                    isStyleLoaded = true
                                    
                                    // Habilitar ubicación nativa
                                    enableLocationComponent(style)
                                    
                                    // Configurar zoom
                                    try {
                                        val maxZoom = 18.5
                                        map.setMinZoomPreference(12.0)
                                        map.setMaxZoomPreference(maxZoom) // limitar para evitar tiles negros
                                        map.addOnCameraIdleListener {
                                            val cam = map.cameraPosition
                                            if (cam.zoom > maxZoom) {
                                                map.cameraPosition = CameraPosition.Builder(cam)
                                                    .zoom(maxZoom)
                                                    .build()
                                            }
                                        }
                                        Log.d("MapLibreMapView", "✅ Zoom configurado (max $maxZoom)")
                                    } catch (e: Exception) {
                                        Log.e("MapLibreMapView", "Error configurando zoom", e)
                                    }
                                    
                                    // Centrar cámara en el Circuit de Barcelona-Catalunya
                                    val circuitPosition = CameraPosition.Builder()
                                        .target(MapLibreConfig.CircuitBarcelona.getLatLng())
                                        .zoom(MapLibreConfig.CircuitBarcelona.DEFAULT_ZOOM)
                                        .build()
                                    
                                    map.cameraPosition = circuitPosition
                                    
                                    // Añadir overlay de rutas si existe GeoJSON
                                    addRoutesOverlay(style)
                                    // Añadir overlay de POIs si existe GeoJSON
                                    addPoisOverlay(style)

                                    Log.d("MapLibreMapView", "✅ Mapa racing inicializado en Circuit Barcelona")
                                    onMapReady(map)
                                    
                                } catch (e: Exception) {
                                    Log.e("MapLibreMapView", "❌ Error configurando estilo del mapa", e)
                                }
                            }

                            // Configurar listener de clicks en marcadores
                            map.setOnMarkerClickListener { marker ->
                                // Buscar el miembro correspondiente al marcador
                                val userId = currentMarkers.entries.find { it.value == marker }?.key
                                if (userId != null) {
                                    selectedMember = groupLocations.find { it.userId == userId }
                                }
                                // Retornar true para indicar que hemos consumido el evento (no mostrar info window default)
                                true
                            }
                            
                            // Limpiar selección al hacer click en el mapa
                            map.addOnMapClickListener { 
                                selectedMember = null
                                true
                            }

                            // Cargar estilo: primero offline asset (si existe), luego remoto, luego fallback
                            val offlineStyle = MapLibreConfig.OFFLINE_STYLE_ASSET
                            val triedOffline = if (assetExists(context, offlineStyle)) {
                                try {
                                    map.setStyle("asset://$offlineStyle") { loadedStyle ->
                                        Log.d("MapLibreMapView", "✅ Estilo offline cargado")
                                        configureMapAfterStyleLoad(loadedStyle)
                                    }
                                    true
                                } catch (e: Exception) {
                                    Log.e("MapLibreMapView", "Error cargando estilo offline", e)
                                    false
                                }
                            } else false

                            if (!triedOffline) {
                                try {
                                    map.setStyle(MapLibreConfig.MAP_STYLE_URL) { loadedStyle ->
                                        configureMapAfterStyleLoad(loadedStyle)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MapLibreMapView", "Error cargando estilo principal, intentando fallback", e)
                                    try {
                                        map.setStyle(MapLibreConfig.FALLBACK_STYLE_URL) { loadedStyle ->
                                            Log.w("MapLibreMapView", "⚠️ Usando estilo de fallback")
                                            configureMapAfterStyleLoad(loadedStyle)
                                        }
                                    } catch (e2: Exception) {
                                        Log.e("MapLibreMapView", "Error cargando estilo fallback", e2)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MapLibreMapView", "Error en getMapAsync", e)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Tarjeta de información del miembro seleccionado
        if (selectedMember != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 96.dp), // levantarla sobre los FABs para que se vea bien
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Usamos el mismo componente que en la lista
                    GroupMemberItem(member = selectedMember!!)
                }
            }
        }
    }
}
