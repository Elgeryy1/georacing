package com.georacing.georacing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures // Added
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput // Added
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.georacing.georacing.data.energy.EnergyMonitor
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.infrastructure.car.CarTransitionManager
import com.georacing.georacing.domain.manager.AutoParkingManager
import com.georacing.georacing.ui.navigation.GeoRacingNavHost
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.GeoRacingTheme
import com.georacing.georacing.ui.theme.LocalEnergyProfile
import com.georacing.georacing.ui.components.SurvivalModeBanner
import com.georacing.georacing.ui.components.debug.DebugControlPanel // Added
import com.georacing.georacing.debug.ScenarioSimulator // Added
import com.georacing.georacing.utils.VoiceAnnouncer // Added for TTS events
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

import com.georacing.georacing.ui.glass.LocalGlassConfig
import com.georacing.georacing.ui.glass.GlassConfig

import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState

import com.georacing.georacing.ui.glass.LocalGlassConfigState
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.glass.GlassSupport
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

class MainActivity : ComponentActivity() {

    private lateinit var energyMonitor: EnergyMonitor
    private lateinit var carTransitionManager: com.georacing.georacing.infrastructure.car.CarTransitionManager
    
    // Public AppContainer for accessing from Composables if needed (e.g., EcoViewModel Factory)
    lateinit var appContainer: com.georacing.georacing.di.AppContainer

    private lateinit var parkingRepository: com.georacing.georacing.data.parking.ParkingRepository
    private lateinit var autoParkingManager: com.georacing.georacing.domain.manager.AutoParkingManager
    
    // Voice Announcer for ScenarioSimulator events
    private var voiceAnnouncer: VoiceAnnouncer? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLoc = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                     permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLoc) {
            val pollingIntent = android.content.Intent(this, com.georacing.georacing.services.StatePollingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    startForegroundService(pollingIntent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error starting polling service", e)
                }
            } else {
                startService(pollingIntent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        // Initialize DI Container
        appContainer = com.georacing.georacing.di.AppContainer(applicationContext)
        
        if (intent?.action == "com.georacing.georacing.REQUEST_LOCATION") {
            val permissionsToRequest = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Also request on normal launch to ensure we have it for Evacuation & BLE
            val permissions = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            // Add BLE Permissions for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
            
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
        
        

        
        // Use Container instances where possible or keep legacy lateinits for now to minimize changes
        energyMonitor = appContainer.energyMonitor
        carTransitionManager = appContainer.carTransitionManager
        // parkingRepository = appContainer.parkingRepository // Not accessible publicly in AppContainer? It is.
        // autoParkingManager = appContainer.autoParkingManager
        
        // Ensure they are initialized for legacy code in this activity
        // (If checking appContainer properties visibility, they are public val)

        carTransitionManager.startMonitoring(this)
        
        // Initialize VoiceAnnouncer for TTS event announcements
        voiceAnnouncer = VoiceAnnouncer(this)

        // Start Global State Polling for Evacuation Protocol ONLY if permissions are already granted
        val hasLocationPerm = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasLocationPerm) {
            val pollingIntent = android.content.Intent(this, com.georacing.georacing.services.StatePollingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    startForegroundService(pollingIntent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error starting polling service", e)
                }
            } else {
                startService(pollingIntent)
            }
        }

        // 2. Initialize WorkManager (Existing)
        val workManager = WorkManager.getInstance(applicationContext)
        
        val userPreferences = UserPreferencesDataStore(this)

        setContent {
            // Collect State from Container's EnergyMonitor
            val energyProfile by appContainer.energyMonitor.energyProfile.collectAsState()
            val parkingConfirmationLocation by appContainer.autoParkingManager.showParkingConfirmation.collectAsState()
            
            // Debug State
            val showDebugPanel by ScenarioSimulator.showDebugPanel.collectAsState()




            // Initialize Glass Configuration
            // We can load this from preferences later, for now defaults
            val glassConfigState = remember { mutableStateOf(GlassConfig()) }
            
            val hazeState = remember { dev.chrisbanes.haze.HazeState() }
            
            // Backdrop for Liquid Glass components
            val backdrop = rememberLayerBackdrop()

            CompositionLocalProvider(
                LocalEnergyProfile provides energyProfile,
                LocalGlassConfig provides glassConfigState.value,
                LocalGlassConfigState provides glassConfigState,
                com.georacing.georacing.ui.glass.LocalHazeState provides hazeState,
                LocalBackdrop provides backdrop
            ) {
                GeoRacingTheme(
                    forceOledBlack = energyProfile.forceOledBlack
                ) {
                    // val appContainer = (application as GeoRacingApplication).container // Removed invalid cast
                    // Use activity's appContainer directly
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) { 
                    val navController = rememberNavController()
                
                // Show BottomBar on all screens EXCEPT Splash, Login, Onboarding
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute !in listOf(
                    Screen.Splash.route,
                    Screen.Login.route,
                    Screen.Onboarding.route
                )
                
                val navigateTo = intent?.getStringExtra("navigate_to")
                val isRationale = intent?.action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
                
                val startDestination = when {
                    isRationale -> Screen.EcoMeter.route 
                    navigateTo == "my_orders" -> Screen.MyOrders.route 
                    else -> Screen.Splash.route
                }

                // 🆕 ROOT CONTAINER for Layers
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // LAYER 1: App Content (Protected by Safe Mode)
                    com.georacing.georacing.ui.components.ConnectivityAwareScaffold {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = androidx.compose.ui.graphics.Color(0xFF080810) 
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding()) 
                            ) {
                                // 0. Background layer captured by backdrop (MUST be a sibling, NOT parent of glass components)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (!GlassSupport.isEmulator) Modifier.layerBackdrop(backdrop)
                                            else Modifier
                                        )
                                        .background(androidx.compose.ui.graphics.Color(0xFF080810))
                                )

                                // 1. Content (NavHost) — glass components inside use drawBackdrop safely
                                GeoRacingNavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    userPreferences = userPreferences,
                                    appContainer = appContainer,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .haze(hazeState)
                                )

                                // 2. Navigation Bar (Overlay)
                                if (showBottomBar) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                    ) {
                                        com.georacing.georacing.ui.components.DashboardBottomBar(navController = navController)
                                    }
                                }

                                // 3. Survival Mode Banner (Overlay)
                                if (energyProfile is com.georacing.georacing.domain.model.EnergyProfile.Survival) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .padding(top = innerPadding.calculateTopPadding()) 
                                    ) {
                                        SurvivalModeBanner()
                                    }
                                }

                                // 4. Parking Confirmation Dialog (Use container's manager)
                                parkingConfirmationLocation?.let { loc ->
                                    com.georacing.georacing.ui.components.ParkingConfirmationDialog(
                                        location = loc,
                                        onConfirm = { appContainer.autoParkingManager.confirmParking(loc) },
                                        onDismiss = { appContainer.autoParkingManager.dismissParkingDialog() },
                                        onAddPhoto = { /* TODO */ }
                                    )
                                }
                                
                                // 5. Simulation Indicator
                                val isSimBat by ScenarioSimulator.forcedBatteryLevel.collectAsState()
                                val isSimBle by ScenarioSimulator.forcedBleSignal.collectAsState()
                                val isSimCar by ScenarioSimulator.forcedCarConnection.collectAsState()
                                
                                val circuitStateFlow = remember { appContainer.circuitStateRepository.getCircuitState() }
                                val circuitState by circuitStateFlow.collectAsState(
                                    initial = com.georacing.georacing.domain.model.CircuitState(
                                        com.georacing.georacing.domain.model.CircuitMode.UNKNOWN, 
                                        null, null, ""
                                    )
                                )

                                if (isSimBat != null || isSimBle != null || isSimCar != null) {
                                     Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = innerPadding.calculateTopPadding() + 8.dp, end = 8.dp)
                                            .size(10.dp)
                                            .background(androidx.compose.ui.graphics.Color.Red, androidx.compose.foundation.shape.CircleShape)
                                    )
                                }
                                
                                // 🆕 6. Live Flag Overlay (Global) - High Z-Index
                                if (circuitState.mode != com.georacing.georacing.domain.model.CircuitMode.UNKNOWN) {
                                     com.georacing.georacing.ui.components.racecontrol.LiveFlagOverlay(
                                         state = circuitState,
                                         modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                                     )
                                }
                            }
                        }
                    } 

                    // LAYER 2: Global Debug Trigger (Hidden, Always Accessible)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // Handle status bars since we are edge-to-edge
                            .padding(top = 32.dp) // Manual safe padding instead of statusBarsPadding 
                            .size(60.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { ScenarioSimulator.setDebugPanelVisible(true) }
                                )
                            }
                    )

                    // LAYER 3: Debug Panel Wrapper
                    if (showDebugPanel) {
                        DebugControlPanel(
                            onDismiss = { ScenarioSimulator.setDebugPanelVisible(false) }
                        )
                    }
                } // End Root Box
                    }


    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        energyMonitor.startMonitoring()
    }

    override fun onStop() {
        super.onStop()
        energyMonitor.stopMonitoring()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceAnnouncer?.shutdown()
        voiceAnnouncer = null
    }
}
