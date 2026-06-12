import SwiftUI
import Combine

struct RoadmapView: View {
    @StateObject private var viewModel = RoadmapViewModel()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                // Content
                ScrollView {
                    VStack(spacing: 16) {
                        ForEach(viewModel.categories) { category in
                            CategorySection(
                                category: category,
                                isExpanded: viewModel.expandedCategories.contains(category.id),
                                onToggle: { viewModel.toggleCategory(category.id) }
                            )
                        }
                    }
                    .padding()
                }
            }
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Text("Roadmap")
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
        .padding()
        .background(Color.black.opacity(0.8))
    }
}

// MARK: - Category Section

struct CategorySection: View {
    let category: RoadmapCat
    let isExpanded: Bool
    let onToggle: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            Button(action: onToggle) {
                HStack {
                    Image(systemName: category.icon)
                        .font(.title2)
                        .foregroundColor(.orange)
                        .frame(width: 32)
                    
                    Text(category.name)
                        .font(.headline)
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    // Progress indicator
                    Text("\(category.completedCount)/\(category.features.count)")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    Image(systemName: "chevron.down")
                        .foregroundColor(.gray)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
                .padding()
                .background(Color(white: 0.12))
            }
            
            // Features List
            if isExpanded {
                VStack(spacing: 0) {
                    ForEach(category.features) { feature in
                        RoadmapFeatureRow(feature: feature)
                        
                        if feature.id != category.features.last?.id {
                            Divider()
                                .background(Color.gray.opacity(0.2))
                        }
                    }
                }
                .background(Color(white: 0.08))
            }
        }
        .cornerRadius(12)
        .animation(.spring(response: 0.3), value: isExpanded)
    }
}

// MARK: - Feature Row

struct RoadmapFeatureRow: View {
    let feature: RoadmapFeature
    
    var body: some View {
        HStack(spacing: 12) {
            // Status Indicator
            FeatureStatusBadge(status: feature.status)
            
            // Feature Info
            VStack(alignment: .leading, spacing: 4) {
                Text(feature.name)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.white)
                
                if let description = feature.description {
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.gray)
                        .lineLimit(2)
                }
            }
            
            Spacer()
        }
        .padding()
    }
}

// MARK: - Status Badge

struct FeatureStatusBadge: View {
    let status: RoadmapFeature.Status
    
    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor)
                .frame(width: 8, height: 8)
            
            Text(status.localizedDisplayName)
                .font(.caption2.weight(.semibold))
                .foregroundColor(statusColor)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(statusColor.opacity(0.15))
        .cornerRadius(12)
    }
    
    private var statusColor: Color {
        switch status {
        case .completed: return .green
        case .inProgress: return .orange
        case .planned: return .blue
        case .stub: return .gray
        }
    }
}

// MARK: - Models

struct RoadmapCat: Identifiable {
    let id: String
    let name: String
    let icon: String
    let features: [RoadmapFeature]
    
    var completedCount: Int {
        features.filter { $0.status == .completed }.count
    }
}

struct RoadmapFeature: Identifiable {
    let id: String
    let name: String
    let description: String?
    let status: Status
    
    enum Status {
        case completed
        case inProgress
        case planned
        case stub
        
        var displayName: String {
            switch self {
            case .completed: return "Completed"
            case .inProgress: return "In Progress"
            case .planned: return "Planned"
            case .stub: return "Future"
            }
        }
        
        @MainActor
        var localizedDisplayName: String {
            LocalizationUtils.string(displayName)
        }
    }
}

// MARK: - ViewModel

@MainActor
final class RoadmapViewModel: ObservableObject {
    
    @Published var categories: [RoadmapCat] = []
    @Published var expandedCategories: Set<String> = []
    
    init() {
        loadFeatures()
    }
    
    func toggleCategory(_ id: String) {
        if expandedCategories.contains(id) {
            expandedCategories.remove(id)
        } else {
            expandedCategories.insert(id)
        }
    }
    
    private func loadFeatures() {
        categories = [
            RoadmapCat(
                id: "core",
                name: "Core",
                icon: "cpu",
                features: [
                    RoadmapFeature(id: "core.circuit_state", name: "Estado del Circuito", description: "Polling del estado en tiempo real", status: .completed),
                    RoadmapFeature(id: "core.context_card", name: "Card Contextual", description: "Tarjeta de información dinámica", status: .completed),
                    RoadmapFeature(id: "core.offline_map", name: "Mapa Vivo Offline", description: "Visualización sin conexión", status: .inProgress),
                    RoadmapFeature(id: "core.pois", name: "Puntos de Interés", description: "API real de POIs", status: .completed),
                    RoadmapFeature(id: "core.ble", name: "Balizas Inteligentes", description: "Escaneo BLE en background", status: .completed),
                    RoadmapFeature(id: "core.notifications", name: "Notificaciones", description: "Push notifications locales", status: .completed),
                ]
            ),
            RoadmapCat(
                id: "nav",
                name: "Navegación",
                icon: "location.fill",
                features: [
                    RoadmapFeature(id: "nav.ar_guide", name: "Guía AR", description: "Navegación en realidad aumentada", status: .stub),
                    RoadmapFeature(id: "nav.anticalas", name: "Rutas Anti-colas", description: "Rutas alternativas evitando aglomeraciones", status: .planned),
                    RoadmapFeature(id: "nav.services", name: "Rutas a Servicios", description: "Navegación a WC, comida, etc.", status: .planned),
                    RoadmapFeature(id: "nav.evacuation", name: "Evacuación Dinámica", description: "Rutas de evacuación de emergencia", status: .completed),
                ]
            ),
            RoadmapCat(
                id: "social",
                name: "Social",
                icon: "person.2.fill",
                features: [
                    RoadmapFeature(id: "social.follow_group", name: "Seguir al Grupo", description: "Ver ubicación de amigos", status: .completed),
                    RoadmapFeature(id: "social.qr_share", name: "Compartir QR", description: "Invitar amigos con código QR", status: .completed),
                    RoadmapFeature(id: "social.meetup", name: "Punto de Encuentro", description: "Definir punto de reunión", status: .planned),
                ]
            ),
            RoadmapCat(
                id: "commerce",
                name: "Tienda",
                icon: "cart.fill",
                features: [
                    RoadmapFeature(id: "commerce.products", name: "Catálogo de Productos", description: "Ver productos disponibles", status: .completed),
                    RoadmapFeature(id: "commerce.cart", name: "Carrito de Compras", description: "Añadir/eliminar productos", status: .completed),
                    RoadmapFeature(id: "commerce.checkout", name: "Checkout", description: "Procesar pedidos", status: .completed),
                    RoadmapFeature(id: "commerce.history", name: "Historial de Pedidos", description: "Ver pedidos anteriores", status: .completed),
                ]
            ),
            RoadmapCat(
                id: "fan",
                name: "Fan Experience",
                icon: "star.fill",
                features: [
                    RoadmapFeature(id: "fan.immersive", name: "Fan Immersive", description: "Experiencia inmersiva de carrera", status: .stub),
                    RoadmapFeature(id: "fan.360", name: "Momento 360", description: "Fotos y vídeos 360°", status: .stub),
                    RoadmapFeature(id: "fan.moments", name: "Momentos", description: "Capturar y compartir momentos", status: .stub),
                ]
            ),
            RoadmapCat(
                id: "staff",
                name: "Staff & Ops",
                icon: "person.badge.key.fill",
                features: [
                    RoadmapFeature(id: "staff.panel", name: "Panel Interno", description: "Control para operadores", status: .completed),
                    RoadmapFeature(id: "staff.beacon_remote", name: "Control Remoto", description: "Gestionar beacons remotamente", status: .planned),
                ]
            ),
        ]
        
        // Expand first category by default
        if let first = categories.first {
            expandedCategories.insert(first.id)
        }
    }
}

#Preview {
    RoadmapView()
}
