import SwiftUI
import Combine

struct PoiListView: View {
    @StateObject private var viewModel = PoiListViewModel()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                // Search Bar
                searchBar
                
                // Filter Chips
                filterChips
                
                // Content
                if viewModel.isLoading {
                    loadingView
                } else if viewModel.filteredPois.isEmpty {
                    emptyView
                } else {
                    poiList
                }
            }
        }
        .task {
            await viewModel.loadPois()
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
            
            Text(LocalizationUtils.string("Points of Interest"))
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
        .padding()
        .background(Color.black.opacity(0.8))
    }
    
    // MARK: - Search Bar
    
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField(LocalizationUtils.string("Search..."), text: $viewModel.searchText)
                .foregroundColor(.white)
            
            if !viewModel.searchText.isEmpty {
                Button {
                    viewModel.searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding()
        .background(Color(white: 0.12))
        .cornerRadius(12)
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Filter Chips
    
    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                filterChip(nil, title: LocalizationUtils.string("All"), icon: "square.grid.2x2")
                filterChip("WC", title: "WC", icon: "toilet")
                filterChip("FOOD", title: LocalizationUtils.string("Food"), icon: "fork.knife")
                filterChip("PARKING", title: LocalizationUtils.string("Parking"), icon: "car.fill")
                filterChip("ENTRANCE", title: LocalizationUtils.string("Entries"), icon: "door.left.hand.open")
                filterChip("MEDICAL", title: "Medical", icon: "cross.fill")
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }
    
    private func filterChip(_ type: String?, title: String, icon: String) -> some View {
        let isSelected = viewModel.selectedType == type
        return Button {
            viewModel.selectedType = type
        } label: {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.caption.weight(.medium))
            }
            .foregroundColor(isSelected ? .black : .white)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? Color.orange : Color(white: 0.15))
            .cornerRadius(16)
        }
    }
    
    // MARK: - POI List
    
    private var poiList: some View {
        List {
            ForEach(viewModel.filteredPois) { poi in
                PoiRowView(poi: poi)
                    .listRowBackground(Color(white: 0.1))
                    .listRowSeparatorTint(.gray.opacity(0.3))
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
    
    // MARK: - Loading & Empty
    
    private var loadingView: some View {
        VStack {
            Spacer()
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .orange))
                .scaleEffect(1.5)
            Text(LocalizationUtils.string("Loading..."))
                .foregroundColor(.gray)
                .padding(.top)
            Spacer()
        }
    }
    
    private var emptyView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "mappin.slash")
                .font(.system(size: 50))
                .foregroundColor(.gray)
            Text(LocalizationUtils.string("No results found"))
                .foregroundColor(.gray)
            Spacer()
        }
    }
}

// MARK: - POI Row

struct PoiRowView: View {
    let poi: PoiItem
    
    var body: some View {
        HStack(spacing: 16) {
            // Icon
            ZStack {
                Circle()
                    .fill(poi.type.color.opacity(0.2))
                    .frame(width: 44, height: 44)
                
                Image(systemName: poi.type.icon)
                    .foregroundColor(poi.type.color)
            }
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(poi.name)
                    .font(.headline)
                    .foregroundColor(.white)
                
                if let zone = poi.zone {
                    Text(zone)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            }
            
            Spacer()
            
            // Navigate button
            Button {
                // Navigate to POI
            } label: {
                Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                    .foregroundColor(.orange)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Models

struct PoiItem: Identifiable {
    let id: String
    let name: String
    let type: PoiItemType
    let zone: String?
    let latitude: Double
    let longitude: Double
}

enum PoiItemType: String {
    case wc = "WC"
    case food = "FOOD"
    case parking = "PARKING"
    case entrance = "ENTRANCE"
    case medical = "MEDICAL"
    case shop = "SHOP"
    case info = "INFO"
    case other = "OTHER"
    
    var icon: String {
        switch self {
        case .wc: return "toilet"
        case .food: return "fork.knife"
        case .parking: return "car.fill"
        case .entrance: return "door.left.hand.open"
        case .medical: return "cross.fill"
        case .shop: return "bag.fill"
        case .info: return "info.circle.fill"
        case .other: return "mappin"
        }
    }
    
    var color: Color {
        switch self {
        case .wc: return .blue
        case .food: return .orange
        case .parking: return .purple
        case .entrance: return .green
        case .medical: return .red
        case .shop: return .pink
        case .info: return .cyan
        case .other: return .gray
        }
    }
}

// MARK: - ViewModel

@MainActor
final class PoiListViewModel: ObservableObject {
    
    @Published var pois: [PoiItem] = []
    @Published var isLoading = false
    @Published var searchText = ""
    @Published var selectedType: String?
    
    var filteredPois: [PoiItem] {
        var result = pois
        
        // Filter by type
        if let type = selectedType {
            result = result.filter { $0.type.rawValue == type }
        }
        
        // Filter by search
        if !searchText.isEmpty {
            let query = searchText.lowercased()
            result = result.filter {
                $0.name.lowercased().contains(query) ||
                ($0.zone?.lowercased().contains(query) ?? false)
            }
        }
        
        return result
    }
    
    func loadPois() async {
        isLoading = true
        
        // Simulate loading - in real app, fetch from API/repository
        try? await Task.sleep(for: .milliseconds(500))
        
        pois = [
            PoiItem(id: "1", name: "WC Principal", type: .wc, zone: "Tribuna Principal", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "2", name: "WC Paddock", type: .wc, zone: "Paddock", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "3", name: "Restaurante Circuit", type: .food, zone: "Tribuna Principal", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "4", name: "Food Truck", type: .food, zone: "Zona Fan", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "5", name: "Parking A", type: .parking, zone: "Entrada Norte", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "6", name: "Parking B", type: .parking, zone: "Entrada Sur", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "7", name: "Entrada Principal", type: .entrance, zone: nil, latitude: 41.57, longitude: 2.26),
            PoiItem(id: "8", name: "Entrada VIP", type: .entrance, zone: nil, latitude: 41.57, longitude: 2.26),
            PoiItem(id: "9", name: "Centro Médico", type: .medical, zone: "Tribuna Central", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "10", name: "Tienda Oficial", type: .shop, zone: "Entrada Principal", latitude: 41.57, longitude: 2.26),
            PoiItem(id: "11", name: "Info Point", type: .info, zone: "Tribuna Principal", latitude: 41.57, longitude: 2.26),
        ]
        
        isLoading = false
    }
}

#Preview {
    PoiListView()
}
