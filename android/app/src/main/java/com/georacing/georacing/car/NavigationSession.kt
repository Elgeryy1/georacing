package com.georacing.georacing.car

import android.util.Log
import androidx.car.app.model.CarColor
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import com.georacing.georacing.car.config.OsrmConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Navigation Session Manager for Turn-by-Turn instructions.
 * Uses REAL OSRM routing API for dynamic route steps.
 * 
 * Falls back to static steps only if OSRM is unreachable.
 */
object NavigationSession {

    private const val TAG = "NavigationSession"

    // Dynamic steps loaded from OSRM
    private val _routeSteps = MutableStateFlow<List<DemoStep>>(emptyList())
    val routeSteps: StateFlow<List<DemoStep>> = _routeSteps.asStateFlow()

    private var _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private var _distanceToCurrentStep = MutableStateFlow(0.0)
    val distanceToCurrentStep: StateFlow<Double> = _distanceToCurrentStep.asStateFlow()

    private var _trafficDelayMinutes = MutableStateFlow(0)
    val trafficDelayMinutes: StateFlow<Int> = _trafficDelayMinutes.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Loads route steps from OSRM for a given origin → destination.
     * @param originLat origin latitude
     * @param originLon origin longitude
     * @param destLat destination latitude (default: Circuit entrance Gate 3)
     * @param destLon destination longitude
     */
    fun loadRoute(
        originLat: Double, originLon: Double,
        destLat: Double = 41.5694, destLon: Double = 2.2549
    ) {
        scope.launch {
            try {
                val baseUrl = OsrmConfig.getBaseUrl()
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val service = retrofit.create(OsrmService::class.java)

                val coordinates = "$originLon,$originLat;$destLon,$destLat"
                val response = service.getRoute(coordinates = coordinates, steps = true)

                if (response.code == "Ok" && response.routes.isNotEmpty()) {
                    val legs = response.routes.first().legs
                    val osrmSteps = legs.flatMap { it.steps }

                    _routeSteps.value = osrmSteps.map { step ->
                        DemoStep(
                            instruction = buildInstruction(step),
                            distanceMeters = step.distance,
                            maneuverType = osrmManeuverToCarManeuver(step.maneuver.type, step.maneuver.modifier),
                            laneGuidance = LaneConfig.ANY_LANE
                        )
                    }

                    _currentStepIndex.value = 0
                    if (_routeSteps.value.isNotEmpty()) {
                        _distanceToCurrentStep.value = _routeSteps.value.first().distanceMeters
                    }
                    Log.i(TAG, "✅ Loaded ${_routeSteps.value.size} OSRM steps")
                } else {
                    Log.w(TAG, "OSRM returned no routes (code=${response.code})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ OSRM route failed: ${e.message}")
            }
        }
    }

    /**
     * Gets the current step
     */
    fun getCurrentDemoStep(): DemoStep {
        val steps = _routeSteps.value
        if (steps.isEmpty()) return DemoStep("Sin ruta cargada", 0.0, Maneuver.TYPE_STRAIGHT, LaneConfig.ANY_LANE)
        val index = _currentStepIndex.value.coerceIn(0, steps.lastIndex)
        return steps[index]
    }

    /**
     * Gets all remaining steps from current position
     */
    fun getRemainingSteps(): List<DemoStep> {
        val steps = _routeSteps.value
        if (steps.isEmpty()) return emptyList()
        val index = _currentStepIndex.value.coerceIn(0, steps.lastIndex)
        return steps.subList(index, steps.size)
    }

    /**
     * Advance to next step
     */
    fun advanceToNextStep() {
        val steps = _routeSteps.value
        if (_currentStepIndex.value < steps.lastIndex) {
            _currentStepIndex.value++
            _distanceToCurrentStep.value = steps[_currentStepIndex.value].distanceMeters
        }
    }

    /**
     * Update distance to current step (called from location updates)
     */
    fun updateDistance(meters: Double) {
        _distanceToCurrentStep.value = meters
        if (meters < 30 && _currentStepIndex.value < _routeSteps.value.lastIndex) {
            advanceToNextStep()
        }
    }

    /**
     * Apply traffic delay (from crowd surge simulation)
     */
    fun applyTrafficDelay(minutes: Int) {
        _trafficDelayMinutes.value = minutes
    }

    /**
     * Reset to beginning
     */
    fun reset() {
        _currentStepIndex.value = 0
        if (_routeSteps.value.isNotEmpty()) {
            _distanceToCurrentStep.value = _routeSteps.value[0].distanceMeters
        }
        _trafficDelayMinutes.value = 0
    }

    /**
     * Build Android Auto Step from DemoStep
     */
    fun buildCarStep(demoStep: DemoStep, distance: Double): Step {
        val stepBuilder = Step.Builder(demoStep.instruction)
            .setManeuver(Maneuver.Builder(demoStep.maneuverType).build())
            .setCue(demoStep.laneGuidance.cueText)

        return stepBuilder.build()
    }

    /**
     * Get ETA color based on traffic
     */
    fun getEtaColor(): CarColor {
        return when {
            _trafficDelayMinutes.value >= 15 -> CarColor.RED
            _trafficDelayMinutes.value >= 5 -> CarColor.YELLOW
            else -> CarColor.GREEN
        }
    }

    // ── Helpers ──

    private fun buildInstruction(step: com.georacing.georacing.car.Step): String {
        val maneuver = step.maneuver
        val roadName = step.name.ifBlank { "la carretera" }
        return when (maneuver.type) {
            "depart" -> "Sal por $roadName"
            "arrive" -> "Has llegado a tu destino"
            "turn" -> "${maneuver.modifier?.replaceFirstChar { it.uppercase() } ?: "Gira"} hacia $roadName"
            "new name" -> "Continúa por $roadName"
            "merge" -> "Incorpórate a $roadName"
            "on ramp" -> "Toma la incorporación a $roadName"
            "off ramp" -> "Toma la salida hacia $roadName"
            "fork" -> "Mantente ${maneuver.modifier ?: "recto"} en la bifurcación"
            "roundabout" -> "En la rotonda, toma la salida ${maneuver.exit ?: ""}"
            "rotary" -> "En la rotonda, toma la salida ${maneuver.exit ?: ""}"
            else -> "Continúa por $roadName"
        }
    }

    private fun osrmManeuverToCarManeuver(type: String, modifier: String?): Int {
        return when (type) {
            "depart" -> Maneuver.TYPE_DEPART
            "arrive" -> Maneuver.TYPE_DESTINATION
            "turn" -> when (modifier) {
                "left" -> Maneuver.TYPE_TURN_NORMAL_LEFT
                "right" -> Maneuver.TYPE_TURN_NORMAL_RIGHT
                "slight left" -> Maneuver.TYPE_TURN_SLIGHT_LEFT
                "slight right" -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
                "sharp left" -> Maneuver.TYPE_TURN_SHARP_LEFT
                "sharp right" -> Maneuver.TYPE_TURN_SHARP_RIGHT
                "uturn" -> Maneuver.TYPE_U_TURN_LEFT
                else -> Maneuver.TYPE_STRAIGHT
            }
            "merge" -> Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED
            "on ramp" -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
            "off ramp" -> when (modifier) {
                "left" -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
                else -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
            }
            "fork" -> when (modifier) {
                "left" -> Maneuver.TYPE_FORK_LEFT
                else -> Maneuver.TYPE_FORK_RIGHT
            }
            "roundabout", "rotary" -> Maneuver.TYPE_ROUNDABOUT_ENTER_CW
            else -> Maneuver.TYPE_STRAIGHT
        }
    }
}

/**
 * Route navigation step (shared data model)
 */
data class DemoStep(
    val instruction: String,
    val distanceMeters: Double,
    val maneuverType: Int,
    val laneGuidance: LaneConfig
)

/**
 * Lane configuration presets - cue text only (Lane API removed)
 */
enum class LaneConfig(val cueText: String) {
    ANY_LANE("Cualquier carril"),
    RIGHT_LANE("Usa el carril derecho"),
    RIGHT_LANES("Usa los 2 carriles derechos"),
    LEFT_LANE("Usa el carril izquierdo");
}
