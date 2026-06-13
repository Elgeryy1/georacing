package com.georacing.georacing.debug

import android.util.Log
import com.georacing.georacing.data.ble.BleCircuitSignal
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.data.gamification.GamificationRepository
import com.georacing.georacing.domain.model.CircuitMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ScenarioSimulator {

    // Battery Simulation (Null = Real, Int = Forced %)
    private val _forcedBatteryLevel = MutableStateFlow<Int?>(null)
    val forcedBatteryLevel: StateFlow<Int?> = _forcedBatteryLevel.asStateFlow()

    // BLE Signal Simulation (Null = Real, Object = Forced)
    private val _forcedBleSignal = MutableStateFlow<BleCircuitSignal?>(null)
    val forcedBleSignal: StateFlow<BleCircuitSignal?> = _forcedBleSignal.asStateFlow()

    // Car Connection Simulation (Null = Real, Boolean = Forced (True=Connected, False=Disconnected))
    private val _forcedCarConnection = MutableStateFlow<Boolean?>(null)
    val forcedCarConnection: StateFlow<Boolean?> = _forcedCarConnection.asStateFlow()

    // Fake Health Data
    private val _extraFakeSteps = MutableStateFlow(0)
    val extraFakeSteps: StateFlow<Int> = _extraFakeSteps.asStateFlow()

    fun addFakeSteps(steps: Int) {
        _extraFakeSteps.value += steps
    }

    // Crowd Heatmap Simulation
    // 0.0 = Low (Green), 1.0 = Critical (Red)
    private val _crowdIntensity = MutableStateFlow(0.2f) // Default safe
    val crowdIntensity: StateFlow<Float> = _crowdIntensity.asStateFlow()

    fun triggerCrowdSurge() {
        _crowdIntensity.value = 0.9f
    }

    fun resetCrowd() {
        _crowdIntensity.value = 0.2f
    }

    // Network Resilience Simulation
    private val _isNetworkDead = MutableStateFlow(false)
    val isNetworkDead: StateFlow<Boolean> = _isNetworkDead.asStateFlow()

    fun killNetwork() {
        _isNetworkDead.value = true
    }

    fun restoreNetwork() {
        _isNetworkDead.value = false
    }

    // UI Visibility State
    private val _showDebugPanel = MutableStateFlow(false)
    val showDebugPanel: StateFlow<Boolean> = _showDebugPanel.asStateFlow()

    fun setDebugPanelVisible(visible: Boolean) {
        _showDebugPanel.value = visible
    }

    // Smart Ticket (Proximity to Gate)
    private val _isAtGate = MutableStateFlow(false)
    val isAtGate: StateFlow<Boolean> = _isAtGate.asStateFlow()

    fun arriveAtGate() {
        _isAtGate.value = true
    }

    fun resetGate() {
        _isAtGate.value = false
    }

    val isSimulationActive: Boolean
        get() = _forcedBatteryLevel.value != null || _forcedBleSignal.value != null || _forcedCarConnection.value != null

    fun simulateSurvivalMode() {
        _forcedBatteryLevel.value = 20
        _forcedCarConnection.value = false // Car disconnected usually implies relying on phone battery
    }
    
    fun simulateEvacuation() {
        val signal = BleCircuitSignal(
            version = 1,
            zoneId = 999,
            mode = CircuitMode.EVACUATION,
            flags = 0,
            sequence = 1000,
            ttlSeconds = 60,
            timestamp = System.currentTimeMillis()
        )
        _forcedBleSignal.value = signal
    }

    fun simulateCarConnect() {
        _forcedCarConnection.value = true
    }

    fun simulateCarDisconnect() {
        _forcedCarConnection.value = false
    }

    fun resetAll() {
        _forcedBatteryLevel.value = null
        _forcedBleSignal.value = null
        _forcedCarConnection.value = null
        _isAtGate.value = false
        _activeHazards.value = emptyList()
        _simulatedSpeed.value = 0f
    }
    
    // =============================================
    // WAZE-STYLE HAZARD SIMULATION
    // =============================================
    
    data class RoadHazard(
        val id: String,
        val type: HazardType,
        val lat: Double,
        val lon: Double,
        val description: String
    )
    
    enum class HazardType(val emoji: String, val label: String) {
        POLICE("👮", "Control de Acceso"),
        CONSTRUCTION("🚧", "Obras en Pista"),
        TRAFFIC("🚗", "Tráfico Pesado"),
        ACCIDENT("⚠️", "Accidente")
    }
    
    private val _activeHazards = MutableStateFlow<List<RoadHazard>>(emptyList())
    val activeHazards: StateFlow<List<RoadHazard>> = _activeHazards.asStateFlow()
    
    // Simulated Speed (km/h)
    private val _simulatedSpeed = MutableStateFlow(0f)
    val simulatedSpeed: StateFlow<Float> = _simulatedSpeed.asStateFlow()
    
    // Speed Limit for the current zone (km/h)
    private val _speedLimit = MutableStateFlow(50f)
    val speedLimit: StateFlow<Float> = _speedLimit.asStateFlow()
    
    fun setSimulatedSpeed(speed: Float) {
        _simulatedSpeed.value = speed
    }
    
    fun addHazard(type: HazardType) {
        // Default positions around Circuit de Barcelona-Catalunya
        val hazardPositions = mapOf(
            HazardType.POLICE to Pair(41.5705, 2.2585),
            HazardType.CONSTRUCTION to Pair(41.5690, 2.2560),
            HazardType.TRAFFIC to Pair(41.5680, 2.2550),
            HazardType.ACCIDENT to Pair(41.5710, 2.2575)
        )
        
        val pos = hazardPositions[type] ?: Pair(41.5700, 2.2600)
        val hazard = RoadHazard(
            id = "${type.name}_${System.currentTimeMillis()}",
            type = type,
            lat = pos.first,
            lon = pos.second,
            description = type.label
        )
        
        _activeHazards.value = _activeHazards.value + hazard
    }
    
    fun clearHazards() {
        _activeHazards.value = emptyList()
    }
    
    // =============================================
    // OTHER RACERS SIMULATION (Waze-like community)
    // =============================================
    
    data class SimulatedRacer(
        val id: String,
        var lat: Double,
        var lon: Double,
        val avatar: String = "🏎️"
    )
    
    private val _otherRacers = MutableStateFlow<List<SimulatedRacer>>(emptyList())
    val otherRacers: StateFlow<List<SimulatedRacer>> = _otherRacers.asStateFlow()
    
    fun spawnRacers(count: Int = 5) {
        val baseLat = 41.5700
        val baseLon = 2.2600
        val racers = (1..count).map { i ->
            SimulatedRacer(
                id = "racer_$i",
                lat = baseLat + (Math.random() - 0.5) * 0.01,
                lon = baseLon + (Math.random() - 0.5) * 0.01,
                avatar = listOf("🏎️", "🏁", "🪖", "⛑️").random()
            )
        }
        _otherRacers.value = racers
    }
    
    fun moveRacersRandomly() {
        _otherRacers.value = _otherRacers.value.map { racer ->
            racer.copy(
                lat = racer.lat + (Math.random() - 0.5) * 0.0005,
                lon = racer.lon + (Math.random() - 0.5) * 0.0005
            )
        }
    }
    
    fun clearRacers() {
        _otherRacers.value = emptyList()
    }

    // =============================================
    // GAMIFICATION / ACHIEVEMENTS SIMULATION
    // =============================================

    private val debugScope = CoroutineScope(Dispatchers.IO)
    private var gamificationRepo: GamificationRepository? = null

    fun setGamificationRepo(repo: GamificationRepository) {
        gamificationRepo = repo
    }

    /** Desbloquea un logro por su ID */
    fun unlockAchievement(id: String) {
        gamificationRepo?.unlockAchievement(id)
            ?: Log.w("ScenarioSimulator", "GamificationRepo no disponible")
    }

    /** Desbloquea TODOS los logros de golpe */
    fun unlockAllAchievements() {
        val repo = gamificationRepo ?: return
        GamificationRepository.allAchievements.forEach { a ->
            repo.unlockAchievement(a.id)
        }
    }

    /** Resetea todos los logros (perfil limpio) */
    fun resetAllAchievements() {
        debugScope.launch {
            try {
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "gamification_profile",
                    data = mapOf(
                        "id" to "current_user",
                        "totalXP" to 0,
                        "level" to 1,
                        "unlockedAchievements" to emptyList<String>(),
                        "circuitsVisited" to 0,
                        "kmWalked" to 0f,
                        "friendsInGroup" to 0
                    )
                ))
                Log.d("ScenarioSimulator", "Logros reseteados — reinicia la pantalla para ver cambios")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error reseteando logros: ${e.message}")
            }
        }
    }

    /** Añade XP bonus artificial */
    fun addBonusXP(amount: Int) {
        val repo = gamificationRepo ?: return
        val current = repo.profile.value
        // Hack: desbloquear un logro ficticio con XP custom sería complicado,
        // usamos el mecanismo existente: sumar XP directamente
        debugScope.launch {
            try {
                val newXP = current.totalXP + amount
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "gamification_profile",
                    data = mapOf(
                        "id" to "current_user",
                        "totalXP" to newXP,
                        "level" to (newXP / 250) + 1,
                        "unlockedAchievements" to current.achievements.filter { it.isUnlocked }.map { it.id },
                        "circuitsVisited" to current.circuitsVisited,
                        "kmWalked" to current.kmWalked,
                        "friendsInGroup" to current.friendsInGroup
                    )
                ))
                Log.d("ScenarioSimulator", "+${amount} XP → Total: $newXP")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error añadiendo XP: ${e.message}")
            }
        }
    }

    // =============================================
    // COLLECTIBLES SIMULATION
    // =============================================

    /** Desbloquea un coleccionable específico por ID */
    fun unlockCollectible(collectibleId: String) {
        debugScope.launch {
            try {
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "user_collectibles",
                    data = mapOf(
                        "id" to collectibleId,
                        "collectible_id" to collectibleId,
                        "unlocked" to true,
                        "unlocked_at" to System.currentTimeMillis()
                    )
                ))
                Log.d("ScenarioSimulator", "Coleccionable $collectibleId desbloqueado")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error desbloqueando coleccionable: ${e.message}")
            }
        }
    }

    /** Desbloquea TODOS los coleccionables (c01..c24) */
    fun unlockAllCollectibles() {
        debugScope.launch {
            try {
                for (i in 1..24) {
                    val id = "c${i.toString().padStart(2, '0')}"
                    FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                        table = "user_collectibles",
                        data = mapOf(
                            "id" to id,
                            "collectible_id" to id,
                            "unlocked" to true,
                            "unlocked_at" to System.currentTimeMillis()
                        )
                    ))
                }
                Log.d("ScenarioSimulator", "24 coleccionables desbloqueados")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error desbloqueando coleccionables: ${e.message}")
            }
        }
    }

    /** Desbloquea coleccionables aleatorios (n de 24) */
    fun unlockRandomCollectibles(count: Int = 5) {
        debugScope.launch {
            try {
                val ids = (1..24).map { "c${it.toString().padStart(2, '0')}" }.shuffled().take(count)
                ids.forEach { id ->
                    FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                        table = "user_collectibles",
                        data = mapOf(
                            "id" to id,
                            "collectible_id" to id,
                            "unlocked" to true,
                            "unlocked_at" to System.currentTimeMillis()
                        )
                    ))
                }
                Log.d("ScenarioSimulator", "$count coleccionables aleatorios desbloqueados: $ids")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error: ${e.message}")
            }
        }
    }

    /** Resetea todos los coleccionables */
    fun resetAllCollectibles() {
        debugScope.launch {
            try {
                for (i in 1..24) {
                    val id = "c${i.toString().padStart(2, '0')}"
                    FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                        table = "user_collectibles",
                        data = mapOf(
                            "id" to id,
                            "collectible_id" to id,
                            "unlocked" to false,
                            "unlocked_at" to 0
                        )
                    ))
                }
                Log.d("ScenarioSimulator", "Coleccionables reseteados")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error reseteando coleccionables: ${e.message}")
            }
        }
    }

    // =============================================
    // LOCATION / ZONE SIMULATION (desbloquea exploración)
    // =============================================

    /** Nombre de la zona simulada actual (null = ubicación real) */
    private val _simulatedZone = MutableStateFlow<String?>(null)
    val simulatedZone: StateFlow<String?> = _simulatedZone.asStateFlow()

    /** Simula estar en una zona específica del circuito */
    fun simulateZoneVisit(zoneName: String) {
        _simulatedZone.value = zoneName
        // También desbloquea logro de primera visita si corresponde
        unlockAchievement("exp_first_visit")
    }

    /** Simula haber visitado todas las zonas — desbloquea explorador total */
    fun simulateAllZonesVisited() {
        unlockAchievement("exp_first_visit")
        unlockAchievement("exp_all_zones")
        unlockAchievement("exp_paddock")
        // Desbloquear coleccionables de exploración
        unlockCollectible("c01") // Primer Paso
        unlockCollectible("c02") // Explorador
        unlockCollectible("c03") // Aventurero
        unlockCollectible("c04") // Descubridor Total
    }

    fun resetSimulatedZone() {
        _simulatedZone.value = null
    }

    // =============================================
    // FULL SCENARIO PRESETS
    // =============================================

    /** Simula un día completo de fan activo en el circuito */
    fun simulateActiveFanDay() {
        // Desbloquea logros típicos de un día activo
        unlockAchievement("exp_first_visit")
        unlockAchievement("spd_first_nav")
        unlockAchievement("fan_weather_check")
        unlockAchievement("saf_medical")
        addFakeSteps(12000)
        // Coleccionables comunes
        unlockCollectible("c01") // Primer Paso
        unlockCollectible("c05") // Marchador
        unlockCollectible("c08") // Primera Foto
        unlockCollectible("c11") // Primer Pedido
        unlockCollectible("c16") // Eco Warrior
    }

    /** Simula un fan VIP que ha hecho de todo */
    fun simulateVIPExperience() {
        // Todos los logros desbloqueados
        unlockAllAchievements()
        // Coleccionables épicos + legendarios
        unlockCollectible("c14") // VIP Access
        unlockCollectible("c15") // Pit Lane
        unlockCollectible("c13") // Master Chef
        unlockCollectible("c20") // Bajo la Lluvia
        unlockCollectible("c22") // Fiel al Circuito
        addBonusXP(2000)
    }

    /** Simula fan nuevo que acaba de llegar */
    fun simulateNewFan() {
        resetAllAchievements()
        resetAllCollectibles()
        _extraFakeSteps.value = 0
        unlockAchievement("exp_first_visit")
        unlockCollectible("c01") // Primer Paso
    }

    /** Simula situación de emergencia completa */
    fun simulateEmergencyScenario() {
        simulateEvacuation()
        triggerCrowdSurge()
        unlockAchievement("saf_report")
        unlockAchievement("saf_emergency")
        unlockAchievement("saf_medical")
    }

    // =============================================
    // ORDERS / CLICK & COLLECT SIMULATION
    // =============================================

    fun createFakeOrder(status: String = "pending") {
        debugScope.launch {
            try {
                val orderId = "dbg_${System.currentTimeMillis()}"
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "orders",
                    data = mapOf(
                        "id" to orderId,
                        "user_id" to "current_user",
                        "status" to status,
                        "items" to listOf(
                            mapOf("name" to "Hamburguesa F1", "price" to 12.50, "qty" to 1),
                            mapOf("name" to "Cerveza Circuito", "price" to 5.00, "qty" to 2)
                        ),
                        "total" to 22.50,
                        "stand_name" to "Paddock Grill",
                        "stand_id" to "stand_01",
                        "pickup_code" to "GR-${(1000..9999).random()}",
                        "created_at" to System.currentTimeMillis() / 1000
                    )
                ))
                Log.d("ScenarioSimulator", "Pedido $orderId creado ($status)")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error creando pedido: ${e.message}")
            }
        }
    }

    fun createOrdersAllStatuses() {
        listOf("pending", "preparing", "ready", "completed").forEach { status ->
            createFakeOrder(status)
        }
    }

    // =============================================
    // PARKING SIMULATION
    // =============================================

    private val _savedCarLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val savedCarLocation: StateFlow<Pair<Double, Double>?> = _savedCarLocation.asStateFlow()

    fun saveCarLocation() {
        _savedCarLocation.value = Pair(41.5700, 2.2590) // Parking Norte
        unlockAchievement("spd_find_car")
    }

    fun clearCarLocation() {
        _savedCarLocation.value = null
    }

    // =============================================
    // CIRCUIT FLAGS / MODE SIMULATION
    // =============================================

    fun simulateCircuitMode(mode: CircuitMode) {
        val signal = BleCircuitSignal(
            version = 1,
            zoneId = 1,
            mode = mode,
            flags = 0,
            sequence = (100..9999).random(),
            ttlSeconds = 120,
            timestamp = System.currentTimeMillis()
        )
        _forcedBleSignal.value = signal
    }

    fun clearCircuitMode() {
        _forcedBleSignal.value = null
    }

    // =============================================
    // WEATHER SIMULATION
    // =============================================

    private val _forcedTemperature = MutableStateFlow<Float?>(null)
    val forcedTemperature: StateFlow<Float?> = _forcedTemperature.asStateFlow()

    private val _forcedWeatherCondition = MutableStateFlow<String?>(null)
    val forcedWeatherCondition: StateFlow<String?> = _forcedWeatherCondition.asStateFlow()

    fun simulateWeather(tempC: Float, condition: String) {
        _forcedTemperature.value = tempC
        _forcedWeatherCondition.value = condition
        unlockAchievement("fan_weather_check")
    }

    fun clearWeather() {
        _forcedTemperature.value = null
        _forcedWeatherCondition.value = null
    }

    // =============================================
    // INCIDENTS / ALERTS SIMULATION
    // =============================================

    fun createFakeIncident(type: String = "safety") {
        debugScope.launch {
            try {
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "incidents",
                    data = mapOf(
                        "id" to "inc_${System.currentTimeMillis()}",
                        "type" to type,
                        "title" to when (type) {
                            "medical" -> "Asistencia médica requerida"
                            "fire" -> "Alerta de fuego en Zona D"
                            "crowd" -> "Aglomeración excesiva en T1"
                            else -> "Incidencia de seguridad general"
                        },
                        "description" to "Incidente simulado desde Debug Panel",
                        "zone" to "Tribuna Principal",
                        "status" to "active",
                        "priority" to "high",
                        "reported_at" to System.currentTimeMillis() / 1000
                    )
                ))
                unlockAchievement("saf_report")
                Log.d("ScenarioSimulator", "Incidente $type creado")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error: ${e.message}")
            }
        }
    }

    fun createFakeAlert(category: String = "GENERAL") {
        debugScope.launch {
            try {
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "news",
                    data = mapOf(
                        "id" to "alert_${System.currentTimeMillis()}",
                        "title" to when (category) {
                            "WEATHER" -> "⚠️ Previsión de lluvia a las 16:00"
                            "TRAFFIC" -> "🚗 Retención en acceso norte"
                            "SAFETY" -> "🛡️ Zona restringida Pit Lane"
                            "SCHEDULE_CHANGE" -> "📅 Carrera retrasada 30 min"
                            else -> "📢 Información general del circuito"
                        },
                        "content" to "Alerta de prueba generada por Debug Panel",
                        "category" to category,
                        "priority" to if (category == "SAFETY") "HIGH" else "MEDIUM",
                        "timestamp" to (System.currentTimeMillis() / 1000).toInt()
                    )
                ))
                Log.d("ScenarioSimulator", "Alerta $category creada")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error: ${e.message}")
            }
        }
    }

    // =============================================
    // SOCIAL / GROUPS SIMULATION
    // =============================================

    fun createFakeGroup() {
        debugScope.launch {
            try {
                val groupId = "grp_debug_${(100..999).random()}"
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "groups",
                    data = mapOf(
                        "id" to groupId,
                        "name" to "Debug Team ${(1..99).random()}",
                        "owner_id" to "current_user",
                        "members" to listOf("current_user", "user_fake_1", "user_fake_2", "user_fake_3"),
                        "created_at" to System.currentTimeMillis() / 1000
                    )
                ))
                // Simular posiciones GPS de miembros
                listOf(
                    Triple("user_fake_1", 41.5705, 2.2588),
                    Triple("user_fake_2", 41.5698, 2.2575),
                    Triple("user_fake_3", 41.5692, 2.2562)
                ).forEach { (userId, lat, lon) ->
                    FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                        table = "group_gps",
                        data = mapOf(
                            "id" to "${groupId}_$userId",
                            "group_id" to groupId,
                            "user_id" to userId,
                            "display_name" to userId.replace("_", " ").replaceFirstChar { it.uppercase() },
                            "latitude" to lat,
                            "longitude" to lon,
                            "updated_at" to System.currentTimeMillis() / 1000
                        )
                    ))
                }
                unlockAchievement("soc_first_group")
                unlockAchievement("soc_share_qr")
                unlockCollectible("c08") // Primera Foto
                Log.d("ScenarioSimulator", "Grupo $groupId con 3 miembros creado")
            } catch (e: Exception) {
                Log.e("ScenarioSimulator", "Error: ${e.message}")
            }
        }
    }

    // =============================================
    // ECO / SUSTAINABILITY SIMULATION
    // =============================================

    private val _fakeCO2Saved = MutableStateFlow(0f) // kg
    val fakeCO2Saved: StateFlow<Float> = _fakeCO2Saved.asStateFlow()

    fun addCO2Saved(kg: Float) {
        _fakeCO2Saved.value += kg
        if (_fakeCO2Saved.value >= 1f) unlockAchievement("eco_transport")
        if (_fakeCO2Saved.value >= 1f) unlockCollectible("c16") // Eco Warrior
        if (_fakeCO2Saved.value >= 5f) unlockCollectible("c17") // Planeta Verde
    }

    // =============================================
    // TRANSITION MODE SIMULATION
    // =============================================

    private val _forcedTransportMode = MutableStateFlow<String?>(null)
    val forcedTransportMode: StateFlow<String?> = _forcedTransportMode.asStateFlow()

    fun setTransportMode(mode: String?) {
        _forcedTransportMode.value = mode
    }

    // =============================================
    // EXTENDED RESET
    // =============================================

    fun resetEverything() {
        // Simulator state
        resetAll()
        resetSimulatedZone()
        clearWeather()
        clearCarLocation()
        clearRacers()
        _extraFakeSteps.value = 0
        _fakeCO2Saved.value = 0f
        _forcedTransportMode.value = null
        // Backend data
        resetAllAchievements()
        resetAllCollectibles()
    }
}
