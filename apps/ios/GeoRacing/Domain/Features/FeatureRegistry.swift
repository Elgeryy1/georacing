import Foundation
import SwiftUI

// MARK: - Models

public enum FeatureCategory: String, CaseIterable, Identifiable {
    case core = "Core"
    case navigation = "Navegación"
    case social = "Social"
    case fan = "Fan Experience"
    case staff = "Staff & Ops"
    case advanced = "Avanzado"
    case visionary = "Visionario"
    
    public var id: String { rawValue }
    
    var icon: String {
        switch self {
        case .core: return "cpu"
        case .navigation: return "location.fill"
        case .social: return "person.3.fill"
        case .fan: return "star.fill"
        case .staff: return "briefcase.fill"
        case .advanced: return "wand.and.stars"
        case .visionary: return "eye.fill"
        }
    }
    
    var color: Color {
        switch self {
        case .core: return .blue
        case .navigation: return .orange
        case .social: return .green
        case .fan: return .purple
        case .staff: return .gray
        case .advanced: return .indigo
        case .visionary: return .pink
        }
    }
}

public enum FeatureStatus: String, Codable {
    case placeholder = "PLACEHOLDER" // Not started or just a shell
    case basic = "BÁSICO"            // Partially implemented or MVP
    case complete = "COMPLETE"       // Parity achieved / Production ready
    
    var color: Color {
        switch self {
        case .placeholder: return .gray
        case .basic: return .orange
        case .complete: return .green
        }
    }
}

public enum FeatureAudience: String, Codable {
    case `public`
    case staffOnly
}

public struct Feature: Identifiable {
    public let id: String
    public let title: String
    public let subtitle: String
    public let category: FeatureCategory
    public let priority: Int // 1 (High) to 10 (Low)
    public let status: FeatureStatus
    public let icon: String
    public let audience: FeatureAudience
    
    // UI Helper for Next Steps (Placeholder View)
    public let nextSteps: [String]
    
    public init(id: String, title: String, subtitle: String, category: FeatureCategory, priority: Int, status: FeatureStatus, icon: String, audience: FeatureAudience = .public, nextSteps: [String]) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.category = category
        self.priority = priority
        self.status = status
        self.icon = icon
        self.audience = audience
        self.nextSteps = nextSteps
    }
}

// MARK: - Registry

/// Single source of truth for all app features and their implementation status.
///
/// **Localization Note:** Feature titles, subtitles, and nextSteps are currently hardcoded
/// in Spanish as domain content. In production, these would be served from a CMS/API
/// and localized server-side, not through `LocalizationUtils`.
public class FeatureRegistry {
    public static let shared = FeatureRegistry()
    
    // Single Source of Truth
    private let _allFeatures: [Feature]
    
    // Public Accessor (Reflects current mode visibility)
    // For now, hardcoded to PUBLIC ONLY.
    public var visibleFeatures: [Feature] {
        return _allFeatures.filter { $0.audience == .public }
    }
    
    private init() {
        self._allFeatures = [
            // --- CORE ---
            Feature(id: "core.circuit_state", title: "Estado del Circuito", subtitle: "Monitorización en tiempo real de banderas y seguridad", category: .core, priority: 1, status: .complete, icon: "flag.fill", nextSteps: ["Optimizar polling", "Añadir histórico"]),
            Feature(id: "core.context_card", title: "Card Contextual", subtitle: "Widgets dinámicos según el estado de carrera", category: .core, priority: 2, status: .complete, icon: "rectangle.grid.1x2.fill", nextSteps: ["Animaciones de transición"]),
            Feature(id: "core.offline_map", title: "Mapa Vivo Offline", subtitle: "Acceso a mapa y POIs sin conexión", category: .core, priority: 3, status: .basic, icon: "map.fill", nextSteps: ["Persistencia completa de tiles", "Sincronización delta"]),
            Feature(id: "core.pois", title: "Puntos de Interés", subtitle: "Filtros y localización de servicios", category: .core, priority: 4, status: .complete, icon: "mappin.and.ellipse", nextSteps: []),
            Feature(id: "core.qr_position", title: "Posicionamiento QR", subtitle: "Escanear para ubicarte en el mapa", category: .core, priority: 5, status: .placeholder, icon: "qrcode.viewfinder", nextSteps: ["Implementar escáner en mapa", "Lógica de triangulación"]),
            Feature(id: "core.ble", title: "Balizas Inteligentes", subtitle: "Detección de zonas por Bluetooth", category: .core, priority: 6, status: .complete, icon: "antenna.radiowaves.left.and.right", nextSteps: ["Calibración fina de RSSI"]),
            Feature(id: "core.offline_mode", title: "Modo Sin Conexión", subtitle: "Funcionalidad completa sin internet", category: .core, priority: 7, status: .basic, icon: "wifi.slash", nextSteps: ["Cola de peticiones POST", "Cacheo de tiendas"]),
            Feature(id: "core.alerts", title: "Centro de Alertas", subtitle: "Historial de notificaciones push", category: .core, priority: 8, status: .basic, icon: "bell.badge.fill", nextSteps: ["Persistencia local de alertas leídas"]),
            Feature(id: "core.notifications", title: "Notificaciones Críticas", subtitle: "Alertas push locales por seguridad", category: .core, priority: 9, status: .complete, icon: "exclamationmark.bubble.fill", nextSteps: []),
            Feature(id: "core.feedback", title: "Incidencias", subtitle: "Reporte de problemas en pista", category: .core, priority: 10, status: .complete, icon: "exclamationmark.triangle.fill", nextSteps: ["Añadir fotos al reporte"]),

            // --- NAVEGACIÓN ---
            Feature(id: "nav.ar_guide", title: "Guía AR al Asiento", subtitle: "Navegación aumentada con cámara", category: .navigation, priority: 1, status: .placeholder, icon: "camera.viewfinder", nextSteps: ["Integrar ARKit", "Mapeo de gradas"]),
            Feature(id: "nav.anticalas", title: "Rutas Anti-colas", subtitle: "Algoritmo de desvío por tráfico", category: .navigation, priority: 2, status: .placeholder, icon: "arrow.triangle.swap", nextSteps: ["Conectar API de afluencia", "Lógica de grafo"]),
            Feature(id: "nav.services", title: "Rutas a Servicios", subtitle: "Camino más rápido a WC/Comida", category: .navigation, priority: 3, status: .placeholder, icon: "figure.walk", nextSteps: ["Implementar OSRM routing", "UX de guiado"]),
            Feature(id: "nav.state_routes", title: "Rutas Dinámicas", subtitle: "Cambio de ruta según estado circuito", category: .navigation, priority: 4, status: .placeholder, icon: "shuffle", nextSteps: ["Lógica de zonas cerradas"]),
            Feature(id: "nav.evacuation", title: "Evacuación Dinámica", subtitle: "Guiado de emergencia a salidas seguras", category: .navigation, priority: 5, status: .complete, icon: "exclamationmark.shield.fill", nextSteps: ["Simulacros"]),

            // --- SOCIAL ---
            Feature(id: "social.follow_group", title: "Seguir al Grupo", subtitle: "Ver ubicación de amigos en tiempo real", category: .social, priority: 1, status: .basic, icon: "person.2.circle.fill", nextSteps: ["Mejorar refresco de posición"]),
            Feature(id: "social.meetup", title: "Punto de Encuentro", subtitle: "Establecer meeting point compartido", category: .social, priority: 2, status: .placeholder, icon: "flag.2.crossed.fill", nextSteps: ["UI de selección en mapa", "Notificación de llegada"]),

            // --- FAN ---
            Feature(id: "fan.immersive", title: "Fan Immersive Mode", subtitle: "Experiencia augmentada durante carrera", category: .fan, priority: 1, status: .placeholder, icon: "headset", nextSteps: ["Audio 3D", "Stats en vivo"]),
            Feature(id: "fan.360", title: "Momento 360", subtitle: "Replay de momentos clave en 360", category: .fan, priority: 2, status: .placeholder, icon: "arrow.triangle.2.circlepath.camera.fill", nextSteps: ["Player de video 360", "Integración content delivery"]),

            // --- STAFF (HIDDEN FOR PUBLIC) ---
            Feature(id: "staff.panel", title: "Panel Interno Staff", subtitle: "Gestión y métricas para operarios", category: .staff, priority: 1, status: .basic, icon: "idcard.fill", audience: .staffOnly, nextSteps: ["Login de staff específico", "Métricas de afluencia"]),
            Feature(id: "staff.beacon_remote", title: "Control Remoto Baliza", subtitle: "Forzar estados de baliza manualmente", category: .staff, priority: 2, status: .placeholder, icon: "remote", audience: .staffOnly, nextSteps: ["API de control", "Permisos de admin"]),
            Feature(id: "staff.safezone", title: "SafeZone Live", subtitle: "Monitorización de áreas seguras", category: .staff, priority: 3, status: .placeholder, icon: "shield.check.fill", audience: .staffOnly, nextSteps: ["Heatmap de densidad"]),

            // --- AVANZADO ---
            Feature(id: "adv.flowsense", title: "FlowSense", subtitle: "Análisis de flujos de movimiento", category: .advanced, priority: 1, status: .placeholder, icon: "wave.3.right", nextSteps: ["Integración CoreMotion"]),
            Feature(id: "adv.ghostpath", title: "GhostPath AR", subtitle: "Sigue la traza fantasma en AR", category: .advanced, priority: 2, status: .placeholder, icon: "ghost.fill", nextSteps: []),
            Feature(id: "adv.clima", title: "ClimaSmart IA", subtitle: "Predicción micro-climática", category: .advanced, priority: 3, status: .placeholder, icon: "cloud.sun.rain.fill", nextSteps: []),
            Feature(id: "adv.soundtags", title: "SoundTags 3D", subtitle: "Audio espacial geolocalizado", category: .advanced, priority: 4, status: .placeholder, icon: "speaker.wave.3.fill", nextSteps: []),
            Feature(id: "adv.ecometer", title: "EcoMeter", subtitle: "Huella de carbono en tiempo real", category: .advanced, priority: 5, status: .placeholder, icon: "leaf.fill", nextSteps: []),
            Feature(id: "adv.ai_qr", title: "QR Inteligentes IA", subtitle: "Códigos contextuales generativos", category: .advanced, priority: 6, status: .placeholder, icon: "qrcode", nextSteps: []),
            Feature(id: "adv.parking", title: "Parking SmartView", subtitle: "Visualización de plazas libres", category: .advanced, priority: 7, status: .placeholder, icon: "parkingsign.circle", nextSteps: []),
            Feature(id: "adv.transport", title: "Transporte Sincro", subtitle: "Shuttles coordinados con carrera", category: .advanced, priority: 8, status: .placeholder, icon: "bus.fill", nextSteps: []),
            Feature(id: "adv.adv_alerts", title: "Alertas Avanzadas", subtitle: "Priorización por IA", category: .advanced, priority: 9, status: .placeholder, icon: "bell.and.waves.left.and.right", nextSteps: []),
            Feature(id: "adv.follow_pilot", title: "Sigue al Piloto", subtitle: "Tracking específico de corredor", category: .advanced, priority: 10, status: .placeholder, icon: "helmet.fill", nextSteps: []),

            // --- VISIONARIO ---
            Feature(id: "vis.detect", title: "GeoRacing Neural Network", subtitle: "Cerebro central de operaciones", category: .visionary, priority: 1, status: .placeholder, icon: "network", nextSteps: []),
            Feature(id: "vis.heatmap", title: "EmoHeatmap", subtitle: "Mapa de calor emocional del público", category: .visionary, priority: 2, status: .placeholder, icon: "heart.fill", nextSteps: []),
            Feature(id: "vis.ticket_ar", title: "Ticket AR", subtitle: "Entrada holográfica", category: .visionary, priority: 3, status: .placeholder, icon: "ticket.fill", nextSteps: []),
            Feature(id: "vis.pulse", title: "Fan Pulse", subtitle: "Ritmo cardíaco colectivo", category: .visionary, priority: 4, status: .placeholder, icon: "waveform.path.ecg", nextSteps: []),
            Feature(id: "vis.chat_ai", title: "Chat IA Contextual", subtitle: "Asistente inteligente de carrera", category: .visionary, priority: 5, status: .placeholder, icon: "message.and.waveform.fill", nextSteps: []),
            Feature(id: "vis.glasses", title: "Modo Gafas AR", subtitle: "Segunda pantalla en gafas", category: .visionary, priority: 6, status: .placeholder, icon: "eyeglasses", nextSteps: []),
            Feature(id: "vis.cooling", title: "Smart Cooling", subtitle: "Gestión de zonas de frescor", category: .visionary, priority: 7, status: .placeholder, icon: "thermometer.snowflake", nextSteps: []),
            Feature(id: "vis.solar", title: "Puntos Solar", subtitle: "Carga verde optimizada", category: .visionary, priority: 8, status: .placeholder, icon: "sun.max.fill", nextSteps: [])
        ]
    }
    
    // Helpers
    public func features(for category: FeatureCategory) -> [Feature] {
        visibleFeatures.filter { $0.category == category }.sorted { $0.priority < $1.priority }
    }
    
    public func totalCount(for category: FeatureCategory) -> Int {
        features(for: category).count
    }
    
    public func completedCount(for category: FeatureCategory) -> Int {
        features(for: category).filter { $0.status == .complete }.count
    }
    
    public func feature(id: String) -> Feature? {
        visibleFeatures.first { $0.id == id }
    }
    
    // Admin/Internal Accessor if needed
    public func allFeaturesIncludingHidden() -> [Feature] {
        _allFeatures
    }
}
