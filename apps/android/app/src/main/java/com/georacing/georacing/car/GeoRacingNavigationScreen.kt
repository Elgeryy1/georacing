package com.georacing.georacing.car

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.PointF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.ItemList
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.georacing.georacing.car.Step as OsrmStep
import com.georacing.georacing.domain.usecases.CheckArrivalUseCase
import com.georacing.georacing.utils.SnapResult

/**
 * Main Navigation Screen implementing Google Maps-style behavior.
 * Integrates OSRM Routing, MapLibre rendering, and Android Auto Templates.
 */
class GeoRacingNavigationScreen(
    carContext: CarContext,
    private val destTitle: String? = null,
    private val destLat: Double? = null,
    private val destLon: Double? = null
) : Screen(carContext), LifecycleEventObserver, TextToSpeech.OnInitListener {

    private val TAG = "GeoRacingNav"

    private val poiRepository = PoiRepository
    private val routeRepository = RouteRepository()
    private val mapStyleManager = MapStyleManager(carContext)
    private val cameraController = CameraController()
    
    // FASE 3.2: Velocímetro y límites de velocidad
    private val speedLimitProvider: com.georacing.georacing.domain.speed.SpeedLimitProvider =
        com.georacing.georacing.domain.speed.FakeSpeedLimitProvider()
    
    // FASE 3.3: Factor de tráfico para ETA ajustado
    // TODO FASE 3: Reemplazar con TrafficProvider real
    private var trafficFactor: Double = 1.0
    
    // FASE 3.4: HUD visual para mostrar velocímetro, límites, instrucciones sobre el mapa
    private val navigationHUD = NavigationHUD()
    
    private var isNavigationActive = (destLat != null && destLon != null)
    private var isFollowMode = true
    private var manualDarkMode: Boolean? = null // null = automático, true/false = manual
    private var hasArrived = false // Para evitar llamar handleArrival múltiples veces
    
    private var currentRouteResult: RouteResult? = null
    private var currentManeuverStep: OsrmStep? = null
    private var currentStepIndexValue = 0
    private var distToManeuver = 0.0
    private var currentSpeedKmh = 0.0
    private var currentSpeedLimitKmh: Int? = null
    private var currentDistance: Double? = null
    private var currentDuration: Double? = null
    
    // Tier 1: Parking
    private var assignedParkingLocation: com.georacing.georacing.data.parking.ParkingLocation? = null
    
    // Tier 1: TTS Mute State
    private var isTtsMuted = false
    private var lastRouteFetchTime = 0L
    private var currentStepIndex = 0 // Índice del paso actual en la ruta
    private var distanceToNextStep = 0.0 // Distancia al siguiente paso/maniobra
    
    // Tier 1: Race Day Context
    private var raceStartTime: Long? = null // Epoch millis for race start
    private var assignedParking: String = "C" // Default parking assignment
    
    // Tier 1: Hazard Notifications
    private var lastHazardAlertTime = 0L
    
    // Race Control: Track state for alerts
    private var currentCircuitMode = com.georacing.georacing.domain.model.CircuitMode.GREEN_FLAG
    private var lastCircuitMode: com.georacing.georacing.domain.model.CircuitMode? = null
    private var lastCircuitModeAlertTime = 0L
    
    // Tier 1: Circuit State Notification Manager
    private lateinit var circuitNotificationManager: CircuitStateNotificationManager
    
    private var tts: TextToSpeech? = null
    private var lastInstruction: String? = null
    private var lastSpokenThreshold = -1
    
    // FASE 1.1: Cache de snap to route con threshold de movimiento
    private var lastSnapResult: com.georacing.georacing.utils.SnapResult? = null
    
    // Flag para calcular ruta inicial apenas se obtenga primera posición GPS
    private var isInitialRouteCalculated = false
    
    // FASE 1.4: Tracking de GPS accuracy para filtrar señales malas
    private var lastGoodGPSTime = System.currentTimeMillis()

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarMapPresentation? = null
    private var mapLibreMap: MapLibreMap? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback
    private var isLocationGranted = false
    private val handler = Handler(Looper.getMainLooper())
    
    // Rastrear el modo oscuro actual para detectar cambios
    private var currentDarkMode = false

    init {
        lifecycle.addObserver(this)
        MapLibre.getInstance(carContext)
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(carContext)
        
        // Inicializar el estado del modo oscuro
        currentDarkMode = (carContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapLocation(location)
                }
            }
        }
        tts = TextToSpeech(carContext, this)
        
        // Tier 1: Initialize Circuit State Notification Manager
        circuitNotificationManager = CircuitStateNotificationManager(carContext)
        
        android.util.Log.d(TAG, "✅ GeoRacingNavigationScreen inicializada (Tier 1 Mode)")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.e(TAG, "TTS: Idioma español no soportado, usando default")
                tts?.language = Locale.getDefault()
            } else {
                android.util.Log.d(TAG, "TTS inicializado correctamente en español")
            }
            // Hablar un mensaje de prueba
            speak("Navegación iniciada")
        } else {
            android.util.Log.e(TAG, "TTS: Error al inicializar, status=$status")
        }
    }

    override fun onGetTemplate(): Template {
        // Detectar cambios en el modo día/noche del sensor del coche (solo si no hay modo manual)
        if (manualDarkMode == null) {
            val isDarkMode = (carContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isDarkMode != currentDarkMode) {
                currentDarkMode = isDarkMode
                // El modo cambió (el coche detectó día/noche), actualizar el estilo del mapa
                setupMapStyle()
            }
        }
        
        // ==============================================
        // RACE CONTROL: Check for critical flag changes
        // ==============================================
        checkRaceControlAlerts()
        
        // ==============================================
        // ACTION STRIP (Must use Icons only for NavigationTemplate)
        // ==============================================
        
        val actionStripBuilder = ActionStrip.Builder()
        
        // 1. Theme Toggle (Icon only)
        val modeIcon = when (manualDarkMode) {
            null -> CarIcon.APP_ICON // Auto (Use generic icon or create specific one)
            true -> CarIcon.APP_ICON // Night
            false -> CarIcon.APP_ICON // Day
        }
        
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(CarIcon.APP_ICON) // Placeholder for theme icon
                .setOnClickListener {
                    manualDarkMode = when (manualDarkMode) {
                        null -> false  // AUTO -> DÍA
                        false -> true  // DÍA -> NOCHE
                        true -> null   // NOCHE -> AUTO
                    }
                    currentDarkMode = manualDarkMode ?: ((carContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
                    setupMapStyle()
                    invalidate()
                    CarToast.makeText(carContext, "Tema cambiado", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )
        
        // 2. Race Status (Icon only)
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(CarIcon.ALERT) // Usar icono de alerta para status
                .setOnClickListener {
                    screenManager.push(RaceStatusScreen(carContext, currentCircuitMode, "C"))
                }
                .build()
        )
        
        // 3. Recenter/Follow (Icon only)
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(CarIcon.APP_ICON)
                .setOnClickListener {
                    isFollowMode = true
                    CarToast.makeText(carContext, "Recalculando...", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // 4. Exit (Standard Back Action usually handles this, putting explicit exit)
        actionStripBuilder.addAction(
             Action.Builder()
                .setIcon(CarIcon.BACK)
                .setOnClickListener { screenManager.pop() }
                .build()
        )

        if (!isLocationGranted) {
            android.util.Log.d(TAG, "🚫 Sin permisos de ubicación, mostrando template de carga")
            return NavigationTemplate.Builder()
                .setNavigationInfo(RoutingInfo.Builder().setLoading(true).build())
                .setActionStrip(actionStripBuilder.build())
                .setBackgroundColor(CarColor.RED)
                .build()
        }

        if (!isNavigationActive) {
            android.util.Log.d(TAG, "🗺️ Modo Free Drive (sin navegación activa)")
            return NavigationTemplate.Builder()
                .setNavigationInfo(
                    RoutingInfo.Builder()
                        .setCurrentStep(
                            Step.Builder("Modo exploración libre")
                                .setManeuver(Maneuver.Builder(Maneuver.TYPE_DESTINATION).build())
                                .build(),
                            Distance.create(0.0, Distance.UNIT_KILOMETERS)
                        )
                        .build()
                )
                .setActionStrip(actionStripBuilder.build())
                .build()
        } else {
            android.util.Log.d(TAG, "🧭 Navegación activa, mostrando ruta")
            val navInfo = buildRoutingInfo()
            val travelEstimate = buildTravelEstimate()
            
            // Tier 1: Check for hazard alerts
            checkAndShowHazardAlerts()

            // NOTE: MapActionStrip also cannot have titles in NavigationTemplate
            return NavigationTemplate.Builder()
                .setNavigationInfo(navInfo)
                .setDestinationTravelEstimate(travelEstimate)
                .setActionStrip(actionStripBuilder.build())
                .setMapActionStrip(buildMapActionStrip())
                .build()
        }
    }
    
    /**
     * Tier 1: Build MapActionStrip with professional navigation controls
     * - Stop Navigation (Red stop icon)
     * - Mute/Unmute TTS (Volume toggle)
     * - Zoom/Re-center (Follow mode)
     */
    private fun buildMapActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            // 1. STOP Navigation Button
            .addAction(
                Action.Builder()
                    .setTitle("Detener")
                    .setIcon(
                        CarIcon.Builder(
                            androidx.core.graphics.drawable.IconCompat.createWithResource(
                                carContext,
                                android.R.drawable.ic_delete // Using system stop icon
                            )
                        ).setTint(CarColor.RED).build()
                    )
                    .setOnClickListener {
                        stopNavigation()
                        CarToast.makeText(carContext, "Navegación detenida", CarToast.LENGTH_SHORT).show()
                    }
                    .build()
            )
            // 2. MUTE/UNMUTE TTS Button
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            androidx.core.graphics.drawable.IconCompat.createWithResource(
                                carContext,
                                if (isTtsMuted) {
                                    android.R.drawable.ic_lock_silent_mode_off // Muted
                                } else {
                                    android.R.drawable.ic_lock_silent_mode // Unmuted
                                }
                            )
                        ).build()
                    )
                    .setOnClickListener {
                        toggleTtsMute()
                    }
                    .build()
            )
            // 3. ZOOM/Re-center Button
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            androidx.core.graphics.drawable.IconCompat.createWithResource(
                                carContext,
                                com.georacing.georacing.R.drawable.ic_f1_car_scaled
                            )
                        ).build()
                    )
                    .setOnClickListener {
                        isFollowMode = true
                        adjustZoomLevel()
                        CarToast.makeText(carContext, "Centrando mapa", CarToast.LENGTH_SHORT).show()
                    }
                    .build()
            )
            .build()
    }
    
    /**
     * Tier 1: Stop navigation and return to main screen
     */
    private fun stopNavigation() {
        android.util.Log.i(TAG, "🛑 Deteniendo navegación por usuario")
        
        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // Clear route
        currentRouteResult = null
        currentManeuverStep = null
        
        // Stop TTS
        tts?.stop()
        
        // Invalidate to refresh screen
        invalidate()
        
        // Navigate back
        screenManager.pop()
    }
    
    /**
     * Tier 1: Toggle TTS mute state
     */
    private fun toggleTtsMute() {
        isTtsMuted = !isTtsMuted
        
        val message = if (isTtsMuted) {
            "Instrucciones de voz silenciadas"
        } else {
            "Instrucciones de voz activadas"
        }
        
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
        android.util.Log.d(TAG, "🔇 TTS Mute: $isTtsMuted")
        
        // Invalidate to refresh MapActionStrip with new icon
        invalidate()
    }
    
    /**
     * Tier 1: Adjust zoom level for better visibility
     */
    private fun adjustZoomLevel() {
        val map = mapLibreMap ?: return
        
        // Increase zoom slightly for better route visibility
        val currentZoom = map.cameraPosition.zoom
        val targetZoom = if (currentZoom < 16.0) 16.5 else currentZoom + 0.5
        
        map.animateCamera(
            CameraUpdateFactory.zoomTo(targetZoom.coerceAtMost(18.0)),
            500
        )
        
        android.util.Log.d(TAG, "➕ Zoom ajustado: $currentZoom -> $targetZoom")
    }
    
    /**
     * Tier 1: Navigate to assigned parking using official coordinates
     */
    /**
     * Tier 1: Navigate to assigned parking using ticket-based assignment.
     * Usa ParkingAssignmentManager para asignar dinámicamente según tipo de entrada.
     */
    private fun navigateToAssignedParking() {
        // Obtener asignación dinámica según tipo de entrada (default: GENERAL)
        val assignment = com.georacing.georacing.data.parking.ParkingAssignmentManager
            .getAssignmentByCode(assignedParking)
        
        val targetParking = com.georacing.georacing.data.parking.ParkingLocation(
            latitude = assignment.latitude,
            longitude = assignment.longitude,
            timestamp = System.currentTimeMillis(),
            photoUri = null
        )
        
        assignedParkingLocation = targetParking
        
        CarToast.makeText(carContext, "Redirigiendo a ${assignment.parkingName}", CarToast.LENGTH_SHORT).show()
            
        // Push new navigation screen to assigned parking
        screenManager.push(
            GeoRacingNavigationScreen(
                carContext = carContext,
                destTitle = assignment.parkingName,
                destLat = assignment.latitude,
                destLon = assignment.longitude
            )
        )
    }
    
    /**
     * Tier 1: Check ScenarioSimulator for hazards and show heads-up notifications
     */
    private fun checkAndShowHazardAlerts() {
        val hazards = com.georacing.georacing.debug.ScenarioSimulator.activeHazards.value
        val now = System.currentTimeMillis()
        
        // Only show alert every 30 seconds to avoid spam
        if (hazards.isNotEmpty() && (now - lastHazardAlertTime) > 30000) {
            val hazard = hazards.first()
            val message = "⚠️ ${hazard.type.label} - Desviando..."
            
            CarToast.makeText(carContext, message, CarToast.LENGTH_LONG).show()
            speak(hazard.type.label + " reportado en la ruta")
            
            lastHazardAlertTime = now
        }
    }
    
    /**
     * Tier 1: Check for critical circuit state changes and send HUN notifications
     */
    private fun checkRaceControlAlerts() {
        val now = System.currentTimeMillis()
        
        // Get current circuit mode from ScenarioSimulator
        val crowdIntensity = com.georacing.georacing.debug.ScenarioSimulator.crowdIntensity.value
        
        val currentMode = when {
            crowdIntensity > 0.9f -> com.georacing.georacing.domain.model.CircuitMode.RED_FLAG
            crowdIntensity > 0.7f -> com.georacing.georacing.domain.model.CircuitMode.SAFETY_CAR
            crowdIntensity > 0.5f -> com.georacing.georacing.domain.model.CircuitMode.YELLOW_FLAG
            else -> com.georacing.georacing.domain.model.CircuitMode.GREEN_FLAG
        }
        
        // Tier 1: Detect state change and send HUN notification
        if (lastCircuitMode != null && lastCircuitMode != currentMode) {
            android.util.Log.w(TAG, "🚩 Circuit Mode Changed: $lastCircuitMode -> $currentMode")
            
            when (currentMode) {
                com.georacing.georacing.domain.model.CircuitMode.RED_FLAG,
                com.georacing.georacing.domain.model.CircuitMode.YELLOW_FLAG,
                com.georacing.georacing.domain.model.CircuitMode.SAFETY_CAR,
                com.georacing.georacing.domain.model.CircuitMode.EVACUATION -> {
                    // Tier 1: Send HUN notification to dashboard
                    circuitNotificationManager.sendCircuitStateAlert(currentMode)
                    
                    // TTS announcement (if not muted)
                    if (!isTtsMuted) {
                        val ttsMessage = getTtsMessageForMode(currentMode)
                        speak(ttsMessage)
                    }
                    
                    // CarToast for immediate feedback
                    val toastMessage = when (currentMode) {
                        com.georacing.georacing.domain.model.CircuitMode.RED_FLAG -> "⚠️ CARRERA DETENIDA"
                        com.georacing.georacing.domain.model.CircuitMode.YELLOW_FLAG -> "🟡 PRECAUCIÓN"
                        com.georacing.georacing.domain.model.CircuitMode.SAFETY_CAR -> "🚗 SAFETY CAR"
                        com.georacing.georacing.domain.model.CircuitMode.EVACUATION -> "🚨 EVACUACIÓN"
                        else -> ""
                    }
                    CarToast.makeText(carContext, toastMessage, CarToast.LENGTH_LONG).show()
                    
                    lastCircuitModeAlertTime = now
                }
                else -> {
                    // Green flag or normal - no notification
                }
            }
        }
        
        lastCircuitMode = currentMode
    }
    
    /**
     * Tier 1: Get TTS message for circuit mode
     */
    private fun getTtsMessageForMode(mode: com.georacing.georacing.domain.model.CircuitMode): String {
        return when (mode) {
            com.georacing.georacing.domain.model.CircuitMode.RED_FLAG -> 
                "Atención. Bandera roja. Carrera detenida. Mantenga la calma al llegar."
            com.georacing.georacing.domain.model.CircuitMode.YELLOW_FLAG -> 
                "Precaución. Bandera amarilla. Reduzca la velocidad."
            com.georacing.georacing.domain.model.CircuitMode.SAFETY_CAR -> 
                "Safety car en pista. Reduzca velocidad."
            com.georacing.georacing.domain.model.CircuitMode.EVACUATION -> 
                "Evacuación inmediata. Siga las señales de salida."
            else -> ""
        }
    }

    private fun buildRoutingInfo(): RoutingInfo {
        if (currentRouteResult == null) {
             return RoutingInfo.Builder().setLoading(true).build()
        }
        
        // Determinar texto de la instrucción
        val stepTitle = when {
            currentDistance != null && currentDistance!! < 50 -> "Ha llegado a su destino"
            currentManeuverStep != null -> getInstructionText(currentManeuverStep!!)
            else -> "Continúe por la ruta"
        }
        
        android.util.Log.d(TAG, "🧭 Instrucción actual: $stepTitle, distancia: ${distToManeuver.toInt()}m")

        val stepObj = currentManeuverStep
        
        val maneuverBuilder = if (stepObj != null) {
             Maneuver.Builder(getManeuverType(stepObj.maneuver.type, stepObj.maneuver.modifier))
        } else {
             Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
        }

        // Construir el Step con la distancia a la maniobra
        val distanceToManeuver = if (distToManeuver > 0) {
            Distance.create(distToManeuver / 1000.0, Distance.UNIT_KILOMETERS)
        } else {
            Distance.create(0.0, Distance.UNIT_KILOMETERS)
        }

        val step = Step.Builder(stepTitle)
            .setManeuver(maneuverBuilder.build())
            .setCue(buildLaneCue(stepObj)) // Lane suggestion text
            .build()
        
        // FASE 3.2: Construir info de velocímetro
        val routingInfoBuilder = RoutingInfo.Builder()
            .setCurrentStep(step, distanceToManeuver)
        
        // Añadir texto de velocidad si está disponible
        if (currentSpeedKmh > 0 || currentSpeedLimitKmh != null) {
            val speedText = buildSpeedDisplayText()
            // Nota: Android Auto no tiene campo específico de velocímetro en NavigationTemplate
            // Usaremos el debug logging por ahora
            android.util.Log.d(TAG, "FASE 3.2 Velocímetro: $speedText")
        }
            
        return routingInfoBuilder.build()
    }
    // Lane guidance removed - API not compatible with library version
    
    /**
     * Tier 1: Build lane cue text for display
     */
    private fun buildLaneCue(step: OsrmStep?): String {
        return when (step?.maneuver?.modifier) {
            "right", "sharp right" -> "Usa el carril derecho"
            "slight right" -> "Usa los 2 carriles derechos"
            "left", "sharp left" -> "Usa el carril izquierdo"
            "slight left" -> "Usa los 2 carriles izquierdos"
            else -> "Cualquier carril"
        }
    }
    
    /**
     * FASE 3.2: Construye texto de velocímetro para mostrar.
     * Formato: "85 / 100 km/h" o "85 km/h" si no hay límite.
     * Si excede límite: "⚠ 130 / 120 km/h"
     */
    private fun buildSpeedDisplayText(): String {
        return if (currentSpeedLimitKmh != null) {
            val exceedsLimit = currentSpeedKmh.toInt() > currentSpeedLimitKmh!!
            val prefix = if (exceedsLimit) "⚠ " else ""
            "$prefix${currentSpeedKmh.toInt()} / $currentSpeedLimitKmh km/h"
        } else {
            "${currentSpeedKmh.toInt()} km/h"
        }
    }

    private fun buildTravelEstimate(): TravelEstimate {
        val dist = currentDistance ?: 0.0
        var dur = currentDuration ?: 0.0
        
        // ==============================================
        // TRAFFIC INTEGRATION: Crowd surge adds delay
        // ==============================================
        val crowdIntensity = com.georacing.georacing.debug.ScenarioSimulator.crowdIntensity.value
        val trafficDelaySeconds = if (crowdIntensity > 0.5f) {
            // High crowd = 15 min delay
            val delayMinutes = ((crowdIntensity - 0.5f) * 30).toInt() // 0-15 min scale
            NavigationSession.applyTrafficDelay(delayMinutes)
            delayMinutes * 60.0
        } else {
            NavigationSession.applyTrafficDelay(0)
            0.0
        }
        
        dur += trafficDelaySeconds
        
        val arrivalCalendar = Calendar.getInstance()
        arrivalCalendar.add(Calendar.SECOND, dur.toInt())
        val arrivalTime = DateTimeWithZone.create(
            arrivalCalendar.timeInMillis,
            (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000),
            TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
        )
        
        // ==============================================
        // ETA COLOR: Red for traffic, Yellow for late, Green for on-time
        // ==============================================
        val etaColor = when {
            crowdIntensity > 0.7f -> CarColor.RED // Heavy traffic!
            raceStartTime != null && arrivalCalendar.timeInMillis > raceStartTime!! -> CarColor.RED // Late!
            crowdIntensity > 0.5f || dur > 1800 -> CarColor.YELLOW // Moderate traffic or >30 min
            else -> CarColor.GREEN // On time
        }
        
        return TravelEstimate.Builder(
            Distance.create(dist / 1000.0, Distance.UNIT_KILOMETERS),
            arrivalTime
        ).setRemainingTimeSeconds(dur.toLong())
         .setRemainingTimeColor(etaColor)
         .build()
    }

    private fun setupMapStyle() {
        val map = mapLibreMap ?: run {
            android.util.Log.e(TAG, "❌ setupMapStyle: mapLibreMap es null")
            return
        }
        
        android.util.Log.d(TAG, "🗺️ setupMapStyle: Iniciando carga de estilo...")
        
        // Detectar automáticamente el modo oscuro/claro del sistema
        val isDarkMode = (carContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val json = if (isDarkMode) mapStyleManager.STYLE_NIGHT_JSON else mapStyleManager.STYLE_DAY_JSON
        
        android.util.Log.d(TAG, "🎨 Modo: ${if (isDarkMode) "OSCURO" else "CLARO"}")
        
        map.setStyle(Style.Builder().fromJson(json)) { style ->
            android.util.Log.d(TAG, "🗺️ Callback de estilo ejecutado, isFullyLoaded=${style.isFullyLoaded}")
            
            // CRITICAL FIX: Esperar a que el estilo esté completamente cargado antes de activar location component
            // Esto previene el crash: "Calling getSourceAs when a newer style is loading/has loaded"
            if (!style.isFullyLoaded) {
                android.util.Log.w(TAG, "⚠️ Estilo no completamente cargado, esperando con retry...")
                waitForStyleFullyLoaded(style, retryCount = 0)
            } else {
                android.util.Log.d(TAG, "✅ Estilo ya completamente cargado")
                initializeMapComponents(style)
            }
        }
    }
    
    private fun waitForStyleFullyLoaded(style: Style, retryCount: Int) {
        val maxRetries = 10  // Máximo 10 intentos (2 segundos total)
        val retryDelayMs = 200L
        
        if (retryCount >= maxRetries) {
            android.util.Log.e(TAG, "❌ TIMEOUT: Estilo no se cargó después de ${maxRetries * retryDelayMs}ms")
            // Intentar inicializar de todas formas (puede fallar pero es mejor que pantalla negra)
            initializeMapComponents(style)
            return
        }
        
        handler.postDelayed({
            if (style.isFullyLoaded) {
                android.util.Log.d(TAG, "✅ Estilo cargado después de ${(retryCount + 1) * retryDelayMs}ms")
                initializeMapComponents(style)
            } else {
                android.util.Log.d(TAG, "⏳ Intento ${retryCount + 1}/$maxRetries - Estilo aún cargando...")
                waitForStyleFullyLoaded(style, retryCount + 1)
            }
        }, retryDelayMs)
    }
    
    private fun initializeMapComponents(style: Style) {
        android.util.Log.d(TAG, "🔧 initializeMapComponents: Iniciando...")
        
        try {
            mapStyleManager.applyLayers(style)
            android.util.Log.d(TAG, "✅ Capas aplicadas")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error aplicando capas: ${e.message}", e)
        }
        

        
        try {
            updatePois(style)
            android.util.Log.d(TAG, "✅ POIs actualizados")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error actualizando POIs: ${e.message}", e)
        }
        
        if (currentRouteResult != null) {
            try {
                drawRoute(currentRouteResult!!)
                android.util.Log.d(TAG, "✅ Ruta dibujada")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error dibujando ruta: ${e.message}", e)
            }
        }
        
        android.util.Log.d(TAG, "✅ initializeMapComponents COMPLETADO")
    }
    


    private fun updatePois(style: Style) {
        // CRITICAL: Verificar que el estilo esté completamente cargado antes de acceder a sources
        if (!style.isFullyLoaded) {
            android.util.Log.w(TAG, "⚠️ Estilo no listo, saltando POIs update")
            return
        }
        
        try {
            val pois = poiRepository.getAllPois()
            val features = pois.map { poi ->
                Feature.fromGeometry(Point.fromLngLat(poi.longitude, poi.latitude)).apply {
                    addStringProperty("id", poi.id)
                    addStringProperty("name", poi.name)
                    addStringProperty("icon_image", if (poi.type == PoiType.PARKING) MapStyleManager.IMAGE_PARKING else MapStyleManager.IMAGE_GATE)
                }
            }
            
            val source = style.getSourceAs<GeoJsonSource>(MapStyleManager.SOURCE_POIS)
            source?.setGeoJson(FeatureCollection.fromFeatures(features))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error actualizando POIs: ${e.message}", e)
        }
    }

    private fun updateMapLocation(location: Location) {
        val map = mapLibreMap ?: return
        
        // CALCULAR RUTA INICIAL: Si hay destino pero no se ha calculado ruta, hacerlo ahora
        if (destLat != null && destLon != null && !isInitialRouteCalculated && currentRouteResult == null) {
            isInitialRouteCalculated = true
            android.util.Log.i(TAG, "🚀 Calculando ruta inicial desde (${location.latitude}, ${location.longitude}) hasta ($destLat, $destLon)")
            
            CoroutineScope(Dispatchers.IO).launch {
                val origin = LatLng(location.latitude, location.longitude)
                val dest = LatLng(destLat!!, destLon!!)
                val result = routeRepository.getRoute(origin, dest, avoidTraffic = true)
                
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        currentRouteResult = result
                        processRouteResult(result)
                        android.util.Log.i(TAG, "✅ Ruta inicial calculada: ${result.distance}m, ${result.duration}s, ${result.steps.size} pasos")
                    } else {
                        android.util.Log.e(TAG, "❌ Error al calcular ruta inicial")
                    }
                }
            }
        }
        
        // FASE 3.2: Actualizar velocidad actual y límite
        currentSpeedKmh = if (location.hasSpeed()) (location.speed * 3.6).toDouble() else 0.0
        currentSpeedLimitKmh = speedLimitProvider.getSpeedLimitForLocation(location)
        
        if (isFollowMode) {
             val speed = currentSpeedKmh.toFloat()
             val cameraState = cameraController.update(
                 location = location,
                 speedKmh = speed,
                 distanceToTurnMeters = if (isNavigationActive) distToManeuver else 0.0,
                 isNavigationActive = isNavigationActive
             )
             
             val camPos = org.maplibre.android.camera.CameraPosition.Builder()
                 .target(cameraState.target)
                 .zoom(cameraState.zoom)
                 .tilt(cameraState.tilt)
                 .bearing(cameraState.bearing)
                 .build()
                 
             map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos), 1000)
        }

        // 3. Update Custom Car Layer (The Racing Car Icon)
        val style = map.style
        if (style != null) {
            val carSource = style.getSourceAs<GeoJsonSource>(MapStyleManager.SOURCE_CAR)
            if (carSource != null) {
                val carFeature = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
                carFeature.addNumberProperty("bearing", location.bearing.toDouble())
                carSource.setGeoJson(carFeature)
            }
        }

        if (isNavigationActive) {
            updateNavigationState(location)
            // Actualizar distancia y tiempo restante en tiempo real
            updateDistanceAndTime(location)
        }
        
        // FASE 3.4: Actualizar HUD SIEMPRE (incluso sin navegación activa para mostrar velocímetro)
        updateNavigationHUD(location)
    }
    
    private fun updateDistanceAndTime(location: Location) {
        if (destLat == null || destLon == null) return
        
        // FASE 1.4: FILTRO GPS POR ACCURACY - Ignorar señales GPS poco confiables
        if (location.accuracy > 50f) {
            android.util.Log.w(TAG, "⚠️ GPS inestable (accuracy=${location.accuracy}m), manteniendo última posición")
            
            // Si llevamos >10s sin GPS bueno, congelar ETA
            val timeSinceGoodGPS = System.currentTimeMillis() - lastGoodGPSTime
            if (timeSinceGoodGPS > 10000) {
                android.util.Log.w(TAG, "⚠️ Sin GPS válido desde hace ${timeSinceGoodGPS}ms - ETA congelado")
                // No actualizar currentDuration ni currentDistance
            }
            return  // Mantener snap/ETA anterior
        }
        
        // GPS válido, actualizar timestamp
        lastGoodGPSTime = System.currentTimeMillis()
        
        android.util.Log.d(TAG, "📍 GPS válido (accuracy=${location.accuracy}m) - Posición: (${location.latitude}, ${location.longitude})")
        
        // Usar los nuevos utilities para cálculo optimizado
        currentRouteResult?.let { route ->
            if (route.points.isEmpty()) return@let
            
            // FASE 1.1: SNAP TO ROUTE CON CACHE - Solo recalcular si te moviste >10m
            val needsRecalculation = lastSnapResult == null || run {
                val lastPoint = lastSnapResult!!.closestPoint
                val lastLoc = Location("").apply {
                    latitude = lastPoint.latitude
                    longitude = lastPoint.longitude
                }
                location.distanceTo(lastLoc) > 10.0  // Threshold 10m
            }
            
            val snapResult = if (needsRecalculation) {
                // FASE 2.1: Usar snap adaptativo en dos pasadas
                // Primera pasada rápida (radius=30), segunda ampliada (radius=100) si está lejos
                com.georacing.georacing.utils.RouteSnapper.snapToRouteAdaptive(
                    currentLocation = location,
                    routePoints = route.points,
                    lastIndex = lastSnapResult?.closestIndex ?: 0,
                    firstRadius = 30,
                    secondRadius = 100,
                    distanceThresholdMeters = 80.0
                ).also { 
                    lastSnapResult = it  // Actualizar cache
                    android.util.Log.d(TAG, "FASE 2.1 - Snap adaptativo: index=${it.closestIndex}, distToRoute=${it.distanceToRoute}m")
                }
            } else {
                // Usar snap cacheado
                lastSnapResult!!
            }
            
            // 2. CALCULAR DISTANCIA RESTANTE
            val remainingDistance = com.georacing.georacing.utils.DistanceCalculator.calculateRemainingDistance(
                snapResult = snapResult,
                routePoints = route.points
            )
            currentDistance = remainingDistance
            
            // 3. CALCULAR ETA PROPORCIONAL CON FACTOR DE TRÁFICO (FASE 3.3)
            currentDuration = com.georacing.georacing.utils.ETACalculator.calculateRemainingTimeWithTraffic(
                remainingDistance = remainingDistance,
                totalDistance = route.distance,
                totalDuration = route.duration,
                trafficFactor = trafficFactor  // FASE 3: Actualmente 1.0 (placeholder)
            )
            
            android.util.Log.d(TAG, "📊 Distancia restante: ${(remainingDistance/1000).format(1)} km, ETA: ${(currentDuration?.div(60))?.toInt() ?: 0} min")
            
            // 4. ACTUALIZAR PASO ACTUAL (calcula distanceToManeuver correctamente)
            updateCurrentStep(route, snapResult.closestIndex, location)
            
            android.util.Log.d(TAG, "🎯 Paso actual: ${currentStepIndex + 1}/${route.steps.size}, distancia a maniobra: ${distToManeuver.toInt()}m")
            
            // 5. CHECK OFF-ROUTE
            val isOffRoute = com.georacing.georacing.utils.OffRouteDetector.checkOffRoute(
                location = location,
                snapResult = snapResult
            )
            
            if (isOffRoute) {
                handleOffRoute(location)
            }
            
            // 6. CHECK ARRIVAL
            val destination = LatLng(destLat!!, destLon!!)
            val arrived = CheckArrivalUseCase.executeSimple(
                currentLocation = location,
                destination = destination
            )
            
            if (arrived) {
                handleArrival()
                return
            }
            
            // 7. HANDLE TTS PROGRESIVO (ya manejado en updateCurrentStep)
            // (TTS se maneja automáticamente en updateCurrentStep vía handleTtsProgressive)
            
            // Forzar actualización de la UI
            invalidate()
        }
    }
    
    private fun updateCurrentStep(route: RouteResult, closestIndex: Int, location: Location) {
        if (route.steps.isEmpty()) return
        
        // Calcular qué paso/instrucción está próximo
        var accumulatedDistance = 0.0
        var foundStep = false
        
        for (stepIndex in 0 until route.steps.size) {
            accumulatedDistance += route.steps[stepIndex].distance
            val progressThroughRoute = closestIndex.toDouble() / route.points.size.toDouble()
            val stepProgressPoint = accumulatedDistance / route.distance
            
            // Si este paso está adelante en la ruta
            if (stepProgressPoint > progressThroughRoute) {
                currentStepIndex = stepIndex
                
                // Calcular distancia al siguiente paso
                var distToStep = 0.0
                val stepStartIndex = (stepProgressPoint * route.points.size).toInt()
                for (i in closestIndex until stepStartIndex.coerceAtMost(route.points.size - 1)) {
                    val p1 = Location("").apply {
                        latitude = route.points[i].latitude
                        longitude = route.points[i].longitude
                    }
                    val p2 = Location("").apply {
                        latitude = route.points[i + 1].latitude
                        longitude = route.points[i + 1].longitude
                    }
                    distToStep += p1.distanceTo(p2)
                }
                
                distanceToNextStep = distToStep
                distToManeuver = distToStep
                currentManeuverStep = route.steps[stepIndex]
                
                // Manejar instrucciones de voz progresivas (estilo Google Maps)
                route.steps[stepIndex].let { step ->
                    val text = getInstructionText(step)
                    handleTtsProgressive(text, distToStep)
                }
                
                foundStep = true
                break
            }
        }
        
        if (!foundStep && route.steps.isNotEmpty()) {
            // Si no encontramos paso siguiente, usar el último
            currentStepIndex = route.steps.size - 1
            currentManeuverStep = route.steps[currentStepIndex]
        }
    }
    
    private fun handleTtsProgressive(instruction: String, distance: Double) {
        // Tier 1: Respect mute state
        if (isTtsMuted) {
            return
        }
        
        // Determinar threshold basado en distancia
        val threshold = when {
            distance > 1000 -> 4 // "En 1 kilómetro"
            distance > 500 -> 3  // "En 500 metros"
            distance > 250 -> 2  // "En 250 metros"
            distance > 100 -> 1  // "En 100 metros"
            else -> 0           // "Ahora"
        }
        
        // Solo hablar si hemos cruzado un nuevo threshold HACIA ABAJO
        if (threshold < lastSpokenThreshold || lastInstruction != instruction) {
            val prefix = when (threshold) {
                4 -> "En un kilómetro, "
                3 -> "En 500 metros, "
                2 -> "En 250 metros, "
                1 -> "En 100 metros, "
                else -> ""
            }
            val fullMessage = prefix + instruction
            speak(fullMessage)
            lastSpokenThreshold = threshold
            lastInstruction = instruction
            
            android.util.Log.d(TAG, "🔊 TTS Progressive: '$fullMessage' (dist=${distance.toInt()}m, threshold=$threshold)")
        }
        
        // Resetear si volvemos a pasar un threshold HACIA ARRIBA (ej: recalculado)
        if (threshold > lastSpokenThreshold) {
            lastSpokenThreshold = threshold
        }
    }
    
    private fun handleArrival() {
        if (!isNavigationActive || hasArrived) return
        
        android.util.Log.d(TAG, "¡LLEGADA AL DESTINO!")
        hasArrived = true
        isNavigationActive = false
        
        // Usar TTSManager para anuncio de llegada
        com.georacing.georacing.utils.TTSManager.announceArrival(
            destinationName = destTitle ?: "su destino",
            tts = tts
        )
        
        // Animación de celebración
        handler.postDelayed({
            CarToast.makeText(
                carContext,
                "🏁 ¡Destino Alcanzado! 🏁",
                CarToast.LENGTH_LONG
            ).show()
        }, 500)
        
        // Zoom out para mostrar la ubicación completa
        mapLibreMap?.let { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(destLat!!, destLon!!),
                    16.0
                ),
                2000
            )
        }
    }
    
    /**
     * Maneja la situación de estar fuera de ruta.
     * Recalcula automáticamente la ruta desde la posición actual.
     */
    private fun handleOffRoute(location: Location) {
        if (destLat == null || destLon == null) return
        
        android.util.Log.w(TAG, "🔄 Usuario fuera de ruta, recalculando...")
        
        // Anunciar recalculo
        com.georacing.georacing.utils.TTSManager.announceRouteRecalculation(tts)
        
        CarToast.makeText(
            carContext,
            "Recalculando ruta...",
            CarToast.LENGTH_SHORT
        ).show()
        
        // Recalcular ruta en background
        CoroutineScope(Dispatchers.IO).launch {
            val origin = LatLng(location.latitude, location.longitude)
            val dest = LatLng(destLat!!, destLon!!)
            
            val result = routeRepository.getRoute(origin, dest, avoidTraffic = true)
            
            withContext(Dispatchers.Main) {
                if (result != null) {
                    currentRouteResult = result
                    processRouteResult(result)
                    
                    CarToast.makeText(
                        carContext,
                        "Ruta actualizada",
                        CarToast.LENGTH_SHORT
                    ).show()
                    
                    android.util.Log.i(TAG, "✅ Ruta recalculada exitosamente")
                } else {
                    android.util.Log.e(TAG, "❌ Error al recalcular ruta")
                }
            }
        }
    }

    private fun updateNavigationState(location: Location) {
        // FASE 1.5: RECALCULO PERIÓDICO ELIMINADO
        // OSRM público no tiene tráfico real, recalcular cada X segundos no aporta nada.
        // La ruta solo se recalcula cuando:
        // 1. OffRouteDetector detecta que el usuario salió de ruta (confirmado tras 3s)
        // 2. Usuario pulsa acción manual de recalcular (si existe)
        
        /* ELIMINADO - Recalculo cada 30s innecesario:
        val now = System.currentTimeMillis()
        if (now - lastRouteFetchTime > 30000) {
            lastRouteFetchTime = now
            CoroutineScope(Dispatchers.IO).launch {
                val origin = LatLng(location.latitude, location.longitude)
                val dest = LatLng(destLat!!, destLon!!)
                val result = routeRepository.getRoute(origin, dest, avoidTraffic = true)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        currentRouteResult = result
                        processRouteResult(result)
                    }
                }
            }
        }
        */
    }

    private fun processRouteResult(result: RouteResult) {
        currentDistance = result.distance
        currentDuration = result.duration
        currentStepIndex = 0
        lastInstruction = null
        lastSpokenThreshold = -1
        drawRoute(result)
        
        if (result.steps.isNotEmpty()) {
            currentManeuverStep = result.steps[0]
            distToManeuver = result.steps[0].distance
            
            // Anunciar primera instrucción
            val text = getInstructionText(result.steps[0])
            speak("Ruta calculada. $text en ${formatdist(result.steps[0].distance)}")
        }
        invalidate()
    }
    
    private fun drawRoute(result: RouteResult) {
        val map = mapLibreMap ?: return
        val style = map.style ?: return
        
        // Dibujar ruta principal (cyan)
        val points = result.points.map { Point.fromLngLat(it.longitude, it.latitude) }
        val lineString = LineString.fromLngLats(points)
        val source = style.getSourceAs<GeoJsonSource>(MapStyleManager.SOURCE_ROUTE)
        source?.setGeoJson(Feature.fromGeometry(lineString))
        
        // Dibujar segmentos de tráfico en rojo/naranja/amarillo
        drawTrafficSegments(result, style)
    }
    
    private fun drawTrafficSegments(result: RouteResult, style: Style) {
        // Remover capa de tráfico anterior si existe
        style.getLayer("traffic-layer")?.let { style.removeLayer(it) }
        style.getSource("traffic-source")?.let { style.removeSource(it) }
        
        if (result.trafficSegments.isEmpty()) return
        
        val features = result.trafficSegments.map { segment ->
            val segmentPoints = result.points.subList(
                segment.startIndex,
                (segment.endIndex + 1).coerceAtMost(result.points.size)
            ).map { Point.fromLngLat(it.longitude, it.latitude) }
            
            val color = when (segment.congestionLevel) {
                CongestionLevel.SEVERE -> "#FF0000"    // Rojo - tráfico muy denso
                CongestionLevel.HEAVY -> "#FF6600"     // Naranja - tráfico denso
                CongestionLevel.MODERATE -> "#FFAA00"  // Amarillo - tráfico moderado
                else -> "#00FF00"                      // Verde - fluido
            }
            
            val feature = Feature.fromGeometry(LineString.fromLngLats(segmentPoints))
            feature.addStringProperty("traffic_color", color)
            feature
        }
        
        val featureCollection = FeatureCollection.fromFeatures(features)
        val trafficSource = GeoJsonSource("traffic-source", featureCollection)
        style.addSource(trafficSource)
        
        // Añadir capa de tráfico encima de la ruta
        val trafficLayer = LineLayer("traffic-layer", "traffic-source").withProperties(
            PropertyFactory.lineColor(Expression.get("traffic_color")),
            PropertyFactory.lineWidth(10f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineOpacity(0.8f)
        )
        style.addLayerAbove(trafficLayer, MapStyleManager.LAYER_ROUTE)
    }

private fun speak(text: String) {
        android.util.Log.d(TAG, "TTS Speak: '$text'")
        if (tts != null) {
            // QUEUE_FLUSH para que hable inmediatamente, no QUEUE_ADD
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        } else {
            android.util.Log.e(TAG, "TTS no inicializado!")
        }
    }
    
    private fun formatdist(meters: Double): String {
        return if (meters >= 1000) "%.1f kilómetros".format(meters/1000) else "${meters.toInt()} metros"
    }
    
    // Helper para formatear decimales
    private fun Double.format(decimals: Int): String {
        return String.format("%.${decimals}f", this)
    }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(container: SurfaceContainer) {
            android.util.Log.d(TAG, "🖥️ onSurfaceAvailable: ${container.width}x${container.height}, dpi=${container.dpi}")
            
            surfaceWidth = container.width
            surfaceHeight = container.height
            val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            
            virtualDisplay = displayManager.createVirtualDisplay(
                "GeoRacingMap",
                container.width, container.height, container.dpi,
                container.surface, 0
            )
            
            android.util.Log.d(TAG, "🖥️ VirtualDisplay creado")
            
            presentation = CarMapPresentation(carContext, virtualDisplay!!.display)
            presentation?.show()
            
            android.util.Log.d(TAG, "🗺️ Presentation mostrado, inicializando MapView...")
            
            presentation?.mapView?.let { mapView ->
                mapView.onCreate(null)
                mapView.onStart()
                mapView.onResume()
                
                android.util.Log.d(TAG, "🗺️ MapView inicializado, esperando getMapAsync...")
                
                mapView.getMapAsync { map ->
                    android.util.Log.d(TAG, "✅ getMapAsync callback ejecutado")
                    
                    mapLibreMap = map
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    
                    android.util.Log.d(TAG, "🗺️ Configuración del mapa completa, llamando a setupMapStyle...")
                    setupMapStyle()
                }
            } ?: run {
                android.util.Log.e(TAG, "❌ ERROR: presentation.mapView es null!")
            }
        }
        override fun onSurfaceDestroyed(container: SurfaceContainer) {
            presentation?.mapView?.onDestroy()
            virtualDisplay?.release()
            mapLibreMap = null
        }
        override fun onScroll(distanceX: Float, distanceY: Float) {
            isFollowMode = false
            // Manual Scroll Implementation using Projection
            val map = mapLibreMap ?: return
            val projection = map.projection
            val center = map.cameraPosition.target ?: return
            val centerPoint = projection.toScreenLocation(center)
            
            // Shift screen point by distance
            val newCenterPoint = PointF(centerPoint.x + distanceX, centerPoint.y + distanceY)
            val newCenterLatLng = projection.fromScreenLocation(newCenterPoint)
            
            if (newCenterLatLng != null) {
                map.moveCamera(CameraUpdateFactory.newLatLng(newCenterLatLng))
            }
        }
        
        override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
             isFollowMode = false
             val map = mapLibreMap ?: return
             val zoomDiff = kotlin.math.ln(scaleFactor.toDouble()) / kotlin.math.ln(2.0)
             map.moveCamera(CameraUpdateFactory.zoomBy(zoomDiff))
        }
        override fun onVisibleAreaChanged(area: Rect) {}
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        android.util.Log.d(TAG, "🔄 Lifecycle Event: $event")
        
        if (event == Lifecycle.Event.ON_CREATE) {
            android.util.Log.d(TAG, "🎬 ON_CREATE: Registrando SurfaceCallback...")
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
            android.util.Log.d(TAG, "✅ SurfaceCallback registrado")
        } else if (event == Lifecycle.Event.ON_START) {
            android.util.Log.d(TAG, "▶️ ON_START: Verificando permisos...")
            checkPermissions()
        } else if (event == Lifecycle.Event.ON_STOP) {
            android.util.Log.d(TAG, "⏸️ ON_STOP: Deteniendo actualizaciones de ubicación")
            stopLocationUpdates()
        } else if (event == Lifecycle.Event.ON_DESTROY) {
            android.util.Log.d(TAG, "💀 ON_DESTROY: Liberando recursos...")
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
            presentation?.dismiss()
            virtualDisplay?.release()
            android.util.Log.d(TAG, "✅ Recursos liberados")
        }
    }

    private fun checkPermissions() {
        if (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
             startLocationUpdates()
             isLocationGranted = true
             invalidate()
        }
    }
    
    private fun startLocationUpdates() {
          try {
              val req = com.google.android.gms.location.LocationRequest.Builder(
                  com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
              ).build()
              fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
          } catch (e: SecurityException) { e.printStackTrace() }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getInstructionText(step: OsrmStep): String {
        val modifier = step.maneuver.modifier
        val type = step.maneuver.type
        val exit = step.maneuver.exit
        val streetName = if (step.name.isNotEmpty() && step.name != "unknown") " hacia ${step.name}" else ""
        
        return when (type) {
            "turn" -> {
                when (modifier) {
                    "left" -> "Gire a la izquierda$streetName"
                    "right" -> "Gire a la derecha$streetName"
                    "slight left" -> "Continúe ligeramente a la izquierda$streetName"
                    "slight right" -> "Continúe ligeramente a la derecha$streetName"
                    "sharp left" -> "Gire completamente a la izquierda$streetName"
                    "sharp right" -> "Gire completamente a la derecha$streetName"
                    else -> "Continúe$streetName"
                }
            }
            "depart" -> "Inicie el recorrido$streetName"
            "arrive" -> "Ha llegado a su destino"
            "roundabout", "rotary" -> {
                // FASE 2.2: Rotondas con ordinales en español natural
                if (exit != null && exit > 0) {
                    val ordinal = exitNumberToSpanishOrdinal(exit)
                    if (streetName.isNotEmpty()) {
                        "En la rotonda, toma la $ordinal salida$streetName"
                    } else {
                        "En la rotonda, toma la $ordinal salida"
                    }
                } else {
                    // Sin exit específico, mensaje genérico
                    if (streetName.isNotEmpty()) {
                        "En la rotonda, toma la salida$streetName"
                    } else {
                        "En la rotonda, continúa recto"
                    }
                }
            }
            "continue" -> "Continúe recto$streetName"
            else -> {
                if (step.name.isNotEmpty() && step.name != "unknown") {
                    "Continúe por ${step.name}"
                } else {
                    "Continúe por la ruta"
                }
            }
        }
    }
    
    /**
     * FASE 2.2: Convierte número de salida de rotonda a ordinal en español.
     * Usado para instrucciones naturales tipo Google Maps.
     * 
     * @param exit Número de salida (1-based)
     * @return Ordinal en español ("primera", "segunda", "tercera", etc.)
     */
    private fun exitNumberToSpanishOrdinal(exit: Int): String {
        return when (exit) {
            1 -> "primera"
            2 -> "segunda"
            3 -> "tercera"
            4 -> "cuarta"
            5 -> "quinta"
            else -> "${exit}ª"  // A partir de 6: "6ª", "7ª", etc.
        }
    }
    
    private fun translate(s: String?) = when(s) { "left"->"izquierda"; "right"->"derecha"; else->s?:"" }
    
    /**
     * FASE 3.4: Actualiza el HUD visual que se dibuja sobre el mapa.
     * Muestra velocímetro, límite de velocidad, instrucción siguiente y ETA.
     * 
     * @param location Ubicación GPS actual del usuario
     */
    private fun updateNavigationHUD(location: Location) {
        val pres = presentation ?: run {
            android.util.Log.w(TAG, "⚠️ HUD: Presentation no inicializada")
            return
        }
        
        try {
            // Generar bitmap del HUD más compacto para no ocupar tanto espacio
            val hudBitmap = navigationHUD.createHUDBitmap(
                width = 280,   // Reducido de 350
                height = 140,  // Reducido de 200
                currentSpeedKmh = currentSpeedKmh.toInt(),
                speedLimitKmh = currentSpeedLimitKmh,
                nextInstruction = currentManeuverStep?.let { step ->
                    getInstructionText(step)
                } ?: "Continúe recto",
                distanceToManeuver = distToManeuver,
                etaMinutes = (currentDuration?.div(60))?.toInt() ?: 0,
                arrowSymbol = currentManeuverStep?.let { step ->
                    getManeuverArrow(step.maneuver.type, step.maneuver.modifier)
                } ?: "↑"
            )
            
            android.util.Log.d(TAG, "🎨 HUD generado: ${currentSpeedKmh}/${currentSpeedLimitKmh ?: "?"} km/h, ${distToManeuver.toInt()}m")
            
            // Actualizar la vista del HUD en la presentation
            handler.post {
                try {
                    pres.hudOverlay.setImageBitmap(hudBitmap)
                    android.util.Log.d(TAG, "✅ HUD actualizado en ImageView")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ Error actualizando HUD en ImageView: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error generando HUD bitmap: ${e.message}", e)
        }
    }
    
    // Explicit constants from androidx.car.app.navigation.model.Maneuver
    private fun getManeuverType(type: String, mod: String?): Int {
        // Safe mapping
        if (type == "turn" && mod == "left") return Maneuver.TYPE_TURN_NORMAL_LEFT
        if (type == "turn" && mod == "right") return Maneuver.TYPE_TURN_NORMAL_RIGHT
        if (type == "fork" && mod == "left") return Maneuver.TYPE_FORK_LEFT
        if (type == "fork" && mod == "right") return Maneuver.TYPE_FORK_RIGHT
        // Fallback for roundabouts or other types
        return Maneuver.TYPE_STRAIGHT
    }

    private fun getManeuverArrow(type: String, modifier: String?): String {
        return when (type) {
            "turn" -> when (modifier) {
                "left" -> "←"
                "right" -> "→"
                "slight left" -> "↖"
                "slight right" -> "↗"
                "sharp left" -> "↙"
                "sharp right" -> "↘"
                else -> "↑"
            }
            "depart" -> "↑"
            "arrive" -> "🏁"
            "roundabout", "rotary" -> "↺"
            else -> "↑"
        }
    }

    // ==============================================
    // SURFACE CALLBACK for Map Rendering (DUPLICATE REMOVED)
    // ==============================================
}
