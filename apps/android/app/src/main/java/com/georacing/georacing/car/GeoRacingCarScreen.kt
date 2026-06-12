package com.georacing.georacing.car

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import android.graphics.Color
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.TravelEstimate

class GeoRacingCarScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarMapPresentation? = null
    private var mapLibreMap: MapLibreMap? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var isLocationGranted = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val permissionCheckRunnable = Runnable { checkPermissions() }
    
    // Waze-style map manager
    private lateinit var mapStyleManager: MapStyleManager
    
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback

    init {
        lifecycle.addObserver(this)
        MapLibre.getInstance(carContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(carContext)
        mapStyleManager = MapStyleManager(carContext)
        
        // Spawn Waze-style elements
        com.georacing.georacing.debug.ScenarioSimulator.spawnRacers(5)
        
        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapLocation(location)
                    // Update speedometer with real speed
                    val speedKmh = location.speed * 3.6f // m/s to km/h
                    com.georacing.georacing.debug.ScenarioSimulator.setSimulatedSpeed(speedKmh)
                    updateSpeedometer(speedKmh)
                }
            }
        }
    }

    // Removed internal Poi class and hardcoded list
    
    // Fetch official nodes from Repository
    private val gates = com.georacing.georacing.data.repository.CircuitLocationsRepository.getGates()
    private val parkings = com.georacing.georacing.data.repository.CircuitLocationsRepository.getNavigableParkings()
    
    // Combine for display (Gates first, then Parkings)
    private val displayNodes = gates + parkings

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        // Create a distance span which is required for PlaceListMapTemplate rows
        val distance = Distance.create(0.0, Distance.UNIT_KILOMETERS)
        val distanceSpan = DistanceSpan.create(distance)
        val distanceText = SpannableString("   ") // Empty space
        distanceText.setSpan(distanceSpan, 0, distanceText.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        for (node in displayNodes) {
            // Icon selection logic could be improved here (Gate vs Parking icon) if ItemList supported it easily
            val title = if (node.type == com.georacing.georacing.domain.model.NodeType.GATE) "🚪 ${node.name}" else "🅿️ ${node.name}"
            
            val row = Row.Builder()
                .setTitle(title)
                .addText(distanceText) // Required to avoid crash
                .setOnClickListener {
                    screenManager.push(
                        GeoRacingNavigationScreen(
                            carContext = carContext,
                            destTitle = node.name,
                            destLat = node.lat,
                            destLon = node.lon
                        )
                    )
                }
                .build()
            itemListBuilder.addItem(row)
        }

        // Add Free Drive Option at the top
        val freeDriveRow = Row.Builder()
            .setTitle("Free Drive")
            .addText(distanceText) // Required to avoid crash in PlaceListMapTemplate
            .setOnClickListener {
                screenManager.push(GeoRacingNavigationScreen(carContext))
            }
            .build()
        itemListBuilder.addItem(freeDriveRow)

        return PlaceListMapTemplate.Builder()
            .setTitle("GeoRacing Destinations")
            .setHeaderAction(Action.APP_ICON)
            .setItemList(itemListBuilder.build())
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Search")
                            .setOnClickListener {
                                screenManager.push(DestinationSearchScreen(carContext))
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    // --- MAP RENDERING LOGIC ---
    
    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            val surface = surfaceContainer.surface
            if (surface == null) return

            val width = surfaceContainer.width
            val height = surfaceContainer.height
            val dpi = surfaceContainer.dpi
            
            surfaceWidth = width
            surfaceHeight = height

            val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            
            try {
                virtualDisplay = displayManager.createVirtualDisplay(
                    "GeoRacingMapMenu",
                    width,
                    height,
                    dpi,
                    surface,
                    0
                )

                presentation = CarMapPresentation(carContext, virtualDisplay!!.display)
                presentation?.show()

                presentation?.mapView?.let { mapView ->
                    mapView.onCreate(null)
                    mapView.onStart()
                    mapView.onResume()
                    
                    mapView.getMapAsync { map ->
                        mapLibreMap = map
                        setupMap(map)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {}

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            presentation?.mapView?.onPause()
            presentation?.mapView?.onStop()
            presentation?.mapView?.onDestroy()
            presentation?.dismiss()
            presentation = null
            
            virtualDisplay?.release()
            virtualDisplay = null
            mapLibreMap = null
        }
    }

    private fun setupMap(map: MapLibreMap) {
        // Use Waze cartoon style
        val wazeStyleJson = mapStyleManager.buildWazeStyleJson()
        
        map.setStyle(Style.Builder().fromJson(wazeStyleJson)) { style ->
            mapStyleManager.applyWazeLayers(style)
            enableLocationComponent(style)
            startLocationUpdates()
            startRacerMovement()
        }
        
        map.uiSettings.isAttributionEnabled = false
        map.uiSettings.isLogoEnabled = false
    }
    
    private fun updateSpeedometer(speedKmh: Float) {
        handler.post {
            presentation?.speedometer?.updateSpeed(
                speedKmh,
                com.georacing.georacing.debug.ScenarioSimulator.speedLimit.value
            )
        }
    }
    
    // Move other racers periodically for community feel
    private val racerMovementRunnable = object : Runnable {
        override fun run() {
            com.georacing.georacing.debug.ScenarioSimulator.moveRacersRandomly()
            handler.postDelayed(this, 2000) // Move every 2 seconds
        }
    }
    
    private fun startRacerMovement() {
        handler.postDelayed(racerMovementRunnable, 2000)
    }
    
    private fun stopRacerMovement() {
        handler.removeCallbacks(racerMovementRunnable)
    }

    private fun enableLocationComponent(style: Style) {
        if (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            carContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            val locationComponent = mapLibreMap?.locationComponent
            val options = org.maplibre.android.location.LocationComponentOptions.builder(carContext)
                .pulseEnabled(true)
                .pulseColor(Color.BLUE)
                .foregroundDrawable(com.georacing.georacing.R.drawable.ic_f1_car_scaled)
                .gpsDrawable(com.georacing.georacing.R.drawable.ic_f1_car_scaled)
                .bearingDrawable(com.georacing.georacing.R.drawable.ic_f1_car_scaled)
                .accuracyAlpha(0.4f)
                .build()

            val locationComponentActivationOptions = org.maplibre.android.location.LocationComponentActivationOptions.builder(carContext, style)
                .locationComponentOptions(options)
                .useDefaultLocationEngine(false)
                .build()
                
            locationComponent?.activateLocationComponent(locationComponentActivationOptions)
            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.renderMode = org.maplibre.android.location.modes.RenderMode.GPS 
            locationComponent?.cameraMode = org.maplibre.android.location.modes.CameraMode.NONE
        }
    }

    private fun startLocationUpdates() {
        if (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            carContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.e("GeoRacing", "Location permission missing", e)
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMapLocation(location: android.location.Location) {
        val map = mapLibreMap ?: return
        map.locationComponent.forceLocationUpdate(location)
        
        val cameraPosition = CameraPosition.Builder()
             .target(LatLng(location.latitude, location.longitude))
             .zoom(14.5) 
             .tilt(30.0) 
             .bearing(location.bearing.toDouble()) 
             .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
    }

    private fun checkPermissions() {
        val nowGranted = (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            carContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            
        if (nowGranted) {
            isLocationGranted = true
            startLocationUpdates()
            mapLibreMap?.style?.let { enableLocationComponent(it) }
            handler.removeCallbacks(permissionCheckRunnable)
        } else {
            isLocationGranted = false
            stopLocationUpdates()
            handler.removeCallbacks(permissionCheckRunnable)
            handler.postDelayed(permissionCheckRunnable, 2000)
        }
    }
    
    override fun onStart(owner: LifecycleOwner) {
        checkPermissions()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(permissionCheckRunnable)
        presentation?.mapView?.onStop()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
        stopLocationUpdates()
        stopRacerMovement()
        com.georacing.georacing.debug.ScenarioSimulator.clearRacers()
        presentation?.mapView?.onDestroy()
        virtualDisplay?.release()
    }

    override fun onPause(owner: LifecycleOwner) {
        presentation?.mapView?.onPause()
    }

    override fun onResume(owner: LifecycleOwner) {
        presentation?.mapView?.onResume()
        checkPermissions()
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }
}
