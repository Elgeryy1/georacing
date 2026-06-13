package com.georacing.georacing.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object SeatSetup : Screen("seat_setup")
    object Map : Screen("map")
    object PoiList : Screen("poi_list")
    object IncidentReport : Screen("incident_report")
    object Settings : Screen("settings")
    object Group : Screen("group")
    object GroupMap : Screen("group_map")
    object ShareQR : Screen("share_qr/{groupId}") {
        fun createRoute(groupId: String) = "share_qr/$groupId"
    }
    object QRScanner : Screen("qr_scanner")
    object Orders : Screen("orders")
    object OrderStatus : Screen("order_status/{orderId}") {
        fun createRoute(orderId: String) = "order_status/$orderId"
    }
    object Moments : Screen("moments")
    object Alerts : Screen("alerts")
    object Parking : Screen("parking")
    object Transport : Screen("transport")
    object ClimaSmart : Screen("clima_smart")
    object EcoMeter : Screen("eco_meter")
    object FanImmersive : Screen("fan_immersive")
    object Emergency : Screen("emergency")
    
    // Navegación al circuito
    object CircuitDestinations : Screen("circuit_destinations")
    object CircuitNavigation : Screen("circuit_navigation/{poiId}") {
        fun createRoute(poiId: String) = "circuit_navigation/$poiId"
    }
    object MyOrders : Screen("my_orders")
    object Roadmap : Screen("roadmap")
    
    // 🆘 Survival Features
    object MedicalLockScreen : Screen("medical_lockscreen")
    object StaffMode : Screen("staff_mode")
    
    // Config
    object EditDashboard : Screen("edit_dashboard")
    
    // New Features
    object AR : Screen("ar")
    object Search : Screen("search")
    object Achievements : Screen("achievements")
    
    // 🆕 New Features (Phase 3)
    object ClickCollect : Screen("click_collect")
    object Wrapped : Screen("wrapped")
    object Collectibles : Screen("collectibles")
    object ProximityChat : Screen("proximity_chat")
    object RouteTraffic : Screen("route_traffic")
    object FanZone : Screen("fan_zone") // iOS parity: FanZoneView
}
