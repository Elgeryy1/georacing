package com.georacing.georacing.domain.features

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.georacing.georacing.ui.navigation.Screen

// ── Feature Category (mirrors iOS FeatureCategory) ──
enum class FeatureCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    CORE("Core", Icons.Default.Memory, Color(0xFF3B82F6)),
    NAVIGATION("Navegación", Icons.Default.Navigation, Color(0xFFFF8C00)),
    SOCIAL("Social", Icons.Default.People, Color(0xFF22C55E)),
    FAN("Fan Experience", Icons.Default.Star, Color(0xFFA855F7)),
    STAFF("Staff & Ops", Icons.Default.Work, Color(0xFF64748B)),
    ADVANCED("Avanzado", Icons.Default.AutoAwesome, Color(0xFF6366F1)),
    VISIONARY("Visionario", Icons.Default.Visibility, Color(0xFFEC4899))
}

// ── Feature Status ──
enum class FeatureStatus(val displayName: String, val color: Color) {
    PLACEHOLDER("PLACEHOLDER", Color(0xFF64748B)),
    BASIC("BÁSICO", Color(0xFFFFA726)),
    COMPLETE("COMPLETO", Color(0xFF22C55E))
}

// ── Feature Audience ──
enum class FeatureAudience {
    PUBLIC, STAFF_ONLY
}

// ── Feature model ──
data class Feature(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: FeatureCategory,
    val priority: Int,
    val status: FeatureStatus,
    val icon: ImageVector,
    val audience: FeatureAudience = FeatureAudience.PUBLIC,
    val route: String? = null // Android Screen route, null = placeholder
)

// ── Registry singleton (mirrors iOS FeatureRegistry.shared) ──
object FeatureRegistry {

    private val allFeatures: List<Feature> = listOf(
        // ─── CORE ───
        Feature("core.circuit_state", "Estado del Circuito", "Monitorización en tiempo real de banderas y seguridad", FeatureCategory.CORE, 1, FeatureStatus.COMPLETE, Icons.Default.Flag, route = Screen.Home.route),
        Feature("core.context_card", "Card Contextual", "Widgets dinámicos según estado de carrera", FeatureCategory.CORE, 2, FeatureStatus.COMPLETE, Icons.Default.ViewModule, route = Screen.Home.route),
        Feature("core.offline_map", "Mapa Vivo Offline", "Acceso a mapa y POIs sin conexión", FeatureCategory.CORE, 3, FeatureStatus.BASIC, Icons.Default.Map, route = Screen.Map.route),
        Feature("core.pois", "Puntos de Interés", "Filtros y localización de servicios", FeatureCategory.CORE, 4, FeatureStatus.COMPLETE, Icons.Default.PinDrop, route = Screen.PoiList.route),
        Feature("core.qr_position", "Posicionamiento QR", "Escanear para ubicarte en el mapa", FeatureCategory.CORE, 5, FeatureStatus.BASIC, Icons.Default.QrCodeScanner, route = Screen.QRScanner.route),
        Feature("core.ble", "Balizas Inteligentes", "Detección de zonas por Bluetooth", FeatureCategory.CORE, 6, FeatureStatus.COMPLETE, Icons.Default.Bluetooth),
        Feature("core.offline_mode", "Modo Sin Conexión", "Funcionalidad completa sin internet", FeatureCategory.CORE, 7, FeatureStatus.BASIC, Icons.Default.WifiOff),
        Feature("core.alerts", "Centro de Alertas", "Historial de notificaciones push", FeatureCategory.CORE, 8, FeatureStatus.COMPLETE, Icons.Default.Notifications, route = Screen.Alerts.route),
        Feature("core.notifications", "Notificaciones Críticas", "Alertas push locales por seguridad", FeatureCategory.CORE, 9, FeatureStatus.COMPLETE, Icons.Default.NotificationsActive),
        Feature("core.feedback", "Incidencias", "Reporte de problemas en pista", FeatureCategory.CORE, 10, FeatureStatus.COMPLETE, Icons.Default.Warning, route = Screen.IncidentReport.route),

        // ─── NAVEGACIÓN ───
        Feature("nav.ar_guide", "Guía AR al Asiento", "Navegación aumentada con cámara", FeatureCategory.NAVIGATION, 1, FeatureStatus.BASIC, Icons.Default.CameraAlt, route = Screen.AR.route),
        Feature("nav.anticalas", "Rutas Anti-colas", "Algoritmo de desvío por tráfico", FeatureCategory.NAVIGATION, 2, FeatureStatus.COMPLETE, Icons.Default.AltRoute, route = Screen.RouteTraffic.route),
        Feature("nav.services", "Rutas a Servicios", "Camino más rápido a WC/Comida", FeatureCategory.NAVIGATION, 3, FeatureStatus.COMPLETE, Icons.Default.DirectionsWalk, route = Screen.CircuitDestinations.route),
        Feature("nav.state_routes", "Rutas Dinámicas", "Cambio de ruta según estado circuito", FeatureCategory.NAVIGATION, 4, FeatureStatus.BASIC, Icons.Default.Shuffle),
        Feature("nav.evacuation", "Evacuación Dinámica", "Guiado de emergencia a salidas seguras", FeatureCategory.NAVIGATION, 5, FeatureStatus.COMPLETE, Icons.Default.Shield, route = Screen.Emergency.route),

        // ─── SOCIAL ───
        Feature("social.follow_group", "Seguir al Grupo", "Ver ubicación de amigos en tiempo real", FeatureCategory.SOCIAL, 1, FeatureStatus.COMPLETE, Icons.Default.GroupWork, route = Screen.Group.route),
        Feature("social.meetup", "Punto de Encuentro", "Establecer meeting point compartido", FeatureCategory.SOCIAL, 2, FeatureStatus.BASIC, Icons.Default.Flag),
        Feature("social.proximity_chat", "Chat de Cercanía", "Comunicación con fans cercanos", FeatureCategory.SOCIAL, 3, FeatureStatus.COMPLETE, Icons.Default.Forum, route = Screen.ProximityChat.route),

        // ─── FAN ───
        Feature("fan.immersive", "Fan Immersive Mode", "Experiencia augmentada durante carrera", FeatureCategory.FAN, 1, FeatureStatus.COMPLETE, Icons.Default.Headphones, route = Screen.FanImmersive.route),
        Feature("fan.fan_zone", "Fan Zone", "Trivia, equipo y coleccionables", FeatureCategory.FAN, 2, FeatureStatus.COMPLETE, Icons.Default.SportsMotorsports, route = Screen.FanZone.route),
        Feature("fan.collectibles", "Coleccionables", "Cromos digitales del circuito", FeatureCategory.FAN, 3, FeatureStatus.COMPLETE, Icons.Default.AccountBox, route = Screen.Collectibles.route),
        Feature("fan.wrapped", "GeoRacing Wrapped", "Resumen personalizado de tu experiencia", FeatureCategory.FAN, 4, FeatureStatus.COMPLETE, Icons.Default.Star, route = Screen.Wrapped.route),

        // ─── STAFF ───
        Feature("staff.panel", "Panel Interno Staff", "Gestión y métricas para operarios", FeatureCategory.STAFF, 1, FeatureStatus.COMPLETE, Icons.Default.Badge, FeatureAudience.STAFF_ONLY, route = Screen.StaffMode.route),
        Feature("staff.beacon_remote", "Control Remoto Baliza", "Forzar estados de baliza manualmente", FeatureCategory.STAFF, 2, FeatureStatus.PLACEHOLDER, Icons.Default.SettingsRemote, FeatureAudience.STAFF_ONLY),
        Feature("staff.safezone", "SafeZone Live", "Monitorización de áreas seguras", FeatureCategory.STAFF, 3, FeatureStatus.PLACEHOLDER, Icons.Default.Security, FeatureAudience.STAFF_ONLY),

        // ─── AVANZADO ───
        Feature("adv.clima", "ClimaSmart IA", "Predicción micro-climática", FeatureCategory.ADVANCED, 1, FeatureStatus.COMPLETE, Icons.Default.Cloud, route = Screen.ClimaSmart.route),
        Feature("adv.ecometer", "EcoMeter", "Huella de carbono en tiempo real", FeatureCategory.ADVANCED, 2, FeatureStatus.COMPLETE, Icons.Default.Eco, route = Screen.EcoMeter.route),
        Feature("adv.parking", "Parking SmartView", "Visualización de plazas libres", FeatureCategory.ADVANCED, 3, FeatureStatus.COMPLETE, Icons.Default.LocalParking, route = Screen.Parking.route),
        Feature("adv.transport", "Transporte Sincro", "Shuttles coordinados con carrera", FeatureCategory.ADVANCED, 4, FeatureStatus.COMPLETE, Icons.Default.DirectionsBus, route = Screen.Transport.route),
        Feature("adv.achievements", "Logros", "Sistema de gamificación", FeatureCategory.ADVANCED, 5, FeatureStatus.COMPLETE, Icons.Default.EmojiEvents, route = Screen.Achievements.route),
        Feature("adv.click_collect", "Click & Collect", "Pedidos para recoger en tienda", FeatureCategory.ADVANCED, 6, FeatureStatus.COMPLETE, Icons.Default.ShoppingCart, route = Screen.ClickCollect.route),

        // ─── VISIONARIO ───
        Feature("vis.detect", "GeoRacing Neural Network", "Cerebro central de operaciones", FeatureCategory.VISIONARY, 1, FeatureStatus.PLACEHOLDER, Icons.Default.Hub),
        Feature("vis.heatmap", "EmoHeatmap", "Mapa de calor emocional del público", FeatureCategory.VISIONARY, 2, FeatureStatus.PLACEHOLDER, Icons.Default.Favorite),
        Feature("vis.ticket_ar", "Ticket AR", "Entrada holográfica", FeatureCategory.VISIONARY, 3, FeatureStatus.PLACEHOLDER, Icons.Default.ConfirmationNumber),
        Feature("vis.pulse", "Fan Pulse", "Ritmo cardíaco colectivo", FeatureCategory.VISIONARY, 4, FeatureStatus.PLACEHOLDER, Icons.Default.MonitorHeart),
        Feature("vis.chat_ai", "Chat IA Contextual", "Asistente inteligente de carrera", FeatureCategory.VISIONARY, 5, FeatureStatus.PLACEHOLDER, Icons.Default.SmartToy),
        Feature("vis.glasses", "Modo Gafas AR", "Segunda pantalla en gafas", FeatureCategory.VISIONARY, 6, FeatureStatus.PLACEHOLDER, Icons.Default.Visibility)
    )

    val visibleFeatures: List<Feature>
        get() = allFeatures.filter { it.audience == FeatureAudience.PUBLIC }

    fun features(category: FeatureCategory): List<Feature> =
        visibleFeatures.filter { it.category == category }.sortedBy { it.priority }

    fun totalCount(category: FeatureCategory): Int = features(category).size

    fun completedCount(category: FeatureCategory): Int =
        features(category).count { it.status == FeatureStatus.COMPLETE }

    fun feature(id: String): Feature? = visibleFeatures.firstOrNull { it.id == id }
}
