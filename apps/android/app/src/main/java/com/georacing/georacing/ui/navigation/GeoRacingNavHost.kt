package com.georacing.georacing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.ui.screens.home.HomeScreen
import com.georacing.georacing.ui.screens.incidents.IncidentReportScreen
import com.georacing.georacing.ui.screens.map.MapScreen
import com.georacing.georacing.ui.screens.onboarding.OnboardingScreen
import com.georacing.georacing.ui.screens.poi.PoiListScreen
import com.georacing.georacing.ui.screens.seat.SeatSetupScreen
import com.georacing.georacing.ui.screens.settings.SettingsScreen
import com.georacing.georacing.ui.screens.group.GroupScreen
import com.georacing.georacing.ui.screens.orders.OrdersScreen
import com.georacing.georacing.ui.screens.moments.MomentsScreen
import com.georacing.georacing.ui.screens.alerts.AlertsScreen
import com.georacing.georacing.ui.screens.parking.ParkingScreen
import com.georacing.georacing.ui.screens.transport.TransportScreen
import com.georacing.georacing.ui.screens.clima.ClimaSmartScreen
import com.georacing.georacing.ui.screens.eco.EcoMeterScreen
import com.georacing.georacing.ui.screens.fan.FanImmersiveScreen
import com.georacing.georacing.ui.screens.navigation.CircuitDestinationSelector
import com.georacing.georacing.ui.screens.navigation.CircuitNavigationScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.georacing.georacing.ui.screens.group.GroupMapViewModel
import com.georacing.georacing.data.repository.NetworkGroupRepository


@Composable
fun GeoRacingNavHost(
    navController: NavHostController,
    startDestination: String,
    userPreferences: UserPreferencesDataStore,
    appContainer: com.georacing.georacing.di.AppContainer, // 🆕 Inject Container
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Dependencies from Container
    val beaconScanner = appContainer.beaconScanner
    val circuitStateRepository = appContainer.circuitStateRepository
    val poiRepository = appContainer.poiRepository
    // ... others as needed

    // =========================================================================
    // 🎛️ AppMonitorManager: Centralized background monitoring
    // BLE scanning, battery monitoring, network monitoring
    // =========================================================================
    val appMonitorManager = appContainer.appMonitorManager
    
    // Start all monitors (idempotent — safe to call on recomposition)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appMonitorManager.startAll()
    }
    
    // 🆘 KILL SWITCH: Navigate to Emergency when battery critical
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appMonitorManager.onCriticalBattery = { level ->
            navController.navigate(Screen.Emergency.route) {
                popUpTo(0) { inclusive = true }
            }
            android.util.Log.w("KillSwitch", "🆘 CRITICAL BATTERY ($level%) - Forced to EmergencyScreen")
        }
    }
    
    // Observe state from the centralized manager (for passing to composables)
    val powerState by appMonitorManager.powerState.collectAsState()
    val batteryLevel by appMonitorManager.batteryLevel.collectAsState()
    val isOnline by appMonitorManager.isOnline.collectAsState()
    val connectionType by appMonitorManager.connectionType.collectAsState()
    
    // Hoisted ViewModel for Group Sharing Persistence
    val groupMapViewModel: GroupMapViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GroupMapViewModel(NetworkGroupRepository())
            }
        }
    )
    
    // Beacons repository from DI container (offline-first with caching)
    val beaconsRepository = appContainer.beaconsRepository
    val incidentsRepository = appContainer.incidentsRepository


    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            com.georacing.georacing.ui.screens.splash.SplashScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            com.georacing.georacing.ui.screens.login.LoginScreen(navController = navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                circuitStateRepository = circuitStateRepository,
                beaconScanner = beaconScanner,
                isOnline = isOnline,
                bleBeaconsCount = beaconScanner.detectedBeacons.collectAsState().value.size,
                userPreferences = userPreferences,
                appContainer = appContainer // 🆕 Pass Container
            )
        }
        composable(Screen.EditDashboard.route) {
            com.georacing.georacing.ui.screens.home.EditDashboardScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        composable(Screen.SeatSetup.route) {
            SeatSetupScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                navController = navController,
                beaconsRepository = beaconsRepository,
                userPreferences = userPreferences
            )
        }
        composable(Screen.PoiList.route) {
            PoiListScreen(
                navController = navController,
                poiRepository = poiRepository
            )
        }
        composable(Screen.IncidentReport.route) {
            IncidentReportScreen(
                navController = navController,
                incidentsRepository = incidentsRepository
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        composable(Screen.Group.route) {
            GroupScreen(
                navController = navController,
                userPreferences = userPreferences,
                viewModel = groupMapViewModel
            )
        }
        composable(Screen.GroupMap.route) {
            com.georacing.georacing.ui.screens.group.GroupMapScreen(
                navController = navController,
                viewModel = groupMapViewModel
            )
        }
        composable(
            route = Screen.ShareQR.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "default_group"
            com.georacing.georacing.ui.screens.share.ShareQRScreen(
                navController = navController,
                groupId = groupId
            )
        }
        composable(Screen.QRScanner.route) {
            com.georacing.georacing.ui.screens.share.QRScannerScreen(navController = navController)
        }
        composable(Screen.Orders.route) {
            com.georacing.georacing.ui.screens.orders.OrdersScreen(navController = navController)
        }
        composable(Screen.MyOrders.route) {
            com.georacing.georacing.ui.screens.orders.MyOrdersScreen(navController = navController)
        }
        composable(
            route = "order_confirmation/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            com.georacing.georacing.ui.screens.orders.OrderConfirmationScreen(
                navController = navController,
                orderId = orderId
            )
        }
        composable(
            route = Screen.OrderStatus.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            com.georacing.georacing.ui.orders.OrderStatusScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() } 
            )
        }
        composable(Screen.Moments.route) {
            MomentsScreen(navController = navController)
        }
        composable(Screen.Alerts.route) {
            AlertsScreen(navController = navController)
        }
        composable(Screen.Parking.route) {
            ParkingScreen(
                navController = navController,
                parkingRepository = appContainer.parkingRepository
            )
        }
        composable(Screen.Transport.route) {
            TransportScreen(navController = navController)
        }
        composable(Screen.ClimaSmart.route) {
            ClimaSmartScreen(navController = navController)
        }
        composable(Screen.EcoMeter.route) {
            EcoMeterScreen(navController = navController)
        }
        composable(Screen.FanImmersive.route) {
            FanImmersiveScreen(navController = navController)
        }
        composable(Screen.Emergency.route) {
            com.georacing.georacing.ui.screens.emergency.EmergencyScreen(navController = navController)
        }
        
        // Navegación al circuito
        composable(Screen.CircuitDestinations.route) {
            CircuitDestinationSelector(navController = navController)
        }
        composable(
            route = Screen.CircuitNavigation.route,
            arguments = listOf(navArgument("poiId") { type = NavType.StringType })
        ) { backStackEntry ->
            val poiId = backStackEntry.arguments?.getString("poiId")
            CircuitNavigationScreen(
                navController = navController,
                poiId = poiId
            )
        }
        composable(Screen.Roadmap.route) {
            com.georacing.georacing.ui.screens.roadmap.RoadmapScreen()
        }
        
        // 🆘 Survival Features
        composable(Screen.MedicalLockScreen.route) {
            com.georacing.georacing.ui.screens.medical.MedicalLockScreenScreen(
                navController = navController
            )
        }
        composable(Screen.StaffMode.route) {
            com.georacing.georacing.ui.screens.staff.StaffModeScreen(
                navController = navController
            )
        }
        
        // New Features
        composable(Screen.AR.route) {
            com.georacing.georacing.ui.screens.ar.ARNavigationScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            com.georacing.georacing.ui.screens.search.SearchScreen(navController = navController)
        }
        composable(Screen.Achievements.route) {
            com.georacing.georacing.ui.screens.achievements.AchievementsScreen(
                navController = navController,
                gamificationRepository = appContainer.gamificationRepository
            )
        }
        
        // 🆕 New Features (Phase 3)
        composable(Screen.ClickCollect.route) {
            com.georacing.georacing.ui.screens.orders.ClickCollectScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrders = { standId -> 
                    navController.navigate(Screen.Orders.route)
                },
                onNavigateToMyOrders = { navController.navigate(Screen.MyOrders.route) }
            )
        }
        composable(Screen.Wrapped.route) {
            com.georacing.georacing.ui.screens.share.WrappedScreen(
                onNavigateBack = { navController.popBackStack() },
                healthConnectManager = appContainer.healthConnectManager,
                gamificationRepository = appContainer.gamificationRepository
            )
        }
        composable(Screen.Collectibles.route) {
            com.georacing.georacing.ui.screens.achievements.CollectiblesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ProximityChat.route) {
            com.georacing.georacing.ui.screens.share.ProximityChatScreen(
                proximityChatManager = appContainer.proximityChatManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.RouteTraffic.route) {
            com.georacing.georacing.ui.screens.traffic.RouteTrafficScreen(
                navController = navController
            )
        }
        composable(Screen.FanZone.route) {
            com.georacing.georacing.ui.screens.fan.FanZoneScreen(
                navController = navController
            )
        }
    }
}
