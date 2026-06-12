package com.georacing.georacing.di

import android.content.Context
import com.georacing.georacing.data.ble.BeaconScanner
import com.georacing.georacing.data.energy.EnergyMonitor
import com.georacing.georacing.data.repository.HybridCircuitStateRepository
import com.georacing.georacing.data.repository.NetworkCircuitStateRepository
import com.georacing.georacing.data.repository.NetworkBeaconsRepository
import com.georacing.georacing.data.repository.OfflineFirstBeaconsRepository
import com.georacing.georacing.data.repository.OfflineFirstCircuitStateRepository
import com.georacing.georacing.infrastructure.car.CarTransitionManager
import com.georacing.georacing.data.local.GeoRacingDatabase
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.repository.OfflineFirstPoiRepository

import com.georacing.georacing.domain.manager.AutoParkingManager

/**
 * Manual Dependency Injection Container.
 * Holds singletons to ensure the entire app (including Simulator) uses the SAME instances.
 */
class AppContainer(private val context: Context) {

    // 1. Core Sensors (Singletons)
    val energyMonitor by lazy { EnergyMonitor(context) }
    val beaconScanner by lazy { BeaconScanner(context) }
    val carTransitionManager by lazy { CarTransitionManager(context) }

    // Health (Real — requires Health Connect permissions)
    val healthConnectManager: com.georacing.georacing.data.health.HealthConnectManager by lazy {
        com.georacing.georacing.data.health.HealthConnectManager(context)
    }

    // 2. Repositories
    val beaconsRepository by lazy {
        OfflineFirstBeaconsRepository(
            networkRepository = NetworkBeaconsRepository(),
            context = context
        )
    }

    val circuitStateRepository by lazy {
        OfflineFirstCircuitStateRepository(
            networkRepository = HybridCircuitStateRepository(
                networkRepository = NetworkCircuitStateRepository(),
                beaconScanner = beaconScanner
            ),
            context = context
        )
    }

    // Database & Network
    val database by lazy { GeoRacingDatabase.getInstance(context) }
    val api by lazy { RetrofitClient.api }
    
    val poiRepository by lazy {
        OfflineFirstPoiRepository(api, database.poiDao())
    }

    val incidentsRepository by lazy {
        com.georacing.georacing.data.repository.OfflineFirstIncidentsRepository(
            incidentDao = database.incidentDao(),
            context = context
        )
    }

    // 3. Managers
    val parkingRepository by lazy { com.georacing.georacing.data.parking.ParkingRepository(context) }
    
    val autoParkingManager by lazy {
        AutoParkingManager(context, parkingRepository, carTransitionManager)
    }
    
    // Services / Monitors
    val batteryMonitor by lazy { com.georacing.georacing.services.BatteryMonitor(context) }
    val networkMonitor by lazy { com.georacing.georacing.services.NetworkMonitor(context) }

    // Centralized Monitor Manager (replaces LaunchedEffect logic in NavHost)
    val appMonitorManager by lazy {
        com.georacing.georacing.ui.AppMonitorManager(
            beaconScanner = beaconScanner,
            batteryMonitor = batteryMonitor,
            networkMonitor = networkMonitor
        )
    }

    // Gamification
    val gamificationRepository by lazy {
        com.georacing.georacing.data.gamification.GamificationRepository().also {
            com.georacing.georacing.debug.ScenarioSimulator.setGamificationRepo(it)
        }
    }

    // 🆕 New Feature Managers (Phase 3)
    val voiceCommandManager by lazy { com.georacing.georacing.services.VoiceCommandManager(context) }
    val osrmTrafficProvider by lazy { com.georacing.georacing.domain.traffic.OsrmTrafficProvider() }
    val osmSpeedLimitProvider by lazy { com.georacing.georacing.domain.speed.OsmSpeedLimitProvider() }
    val qrPositioningManager by lazy { com.georacing.georacing.domain.manager.QrPositioningManager(context) }
    val transitionModeDetector by lazy { com.georacing.georacing.domain.manager.TransitionModeDetector(context) }
    val proximityChatManager by lazy {
        com.georacing.georacing.data.p2p.ProximityChatManager(context, beaconScanner)
    }
    // LaneGuidanceManager → object singleton (com.georacing.georacing.car.LaneGuidanceManager)
    // AREnhancedOverlay → object singleton (com.georacing.georacing.features.ar.AREnhancedOverlay)
}
