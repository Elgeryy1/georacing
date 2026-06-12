import SwiftUI
import MapKit

struct PublicTransportSheetView: View {
    @StateObject private var viewModel = PublicTransportViewModel()
    @ObservedObject var mapViewModel: MapViewModel // To get user location
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationStack {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                VStack(spacing: 0) {
                    
                    // Filter Bar
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            FilterChip(title: LocalizationUtils.string("All"), isSelected: viewModel.selectedModeFilter == "ALL") {
                                viewModel.setFilter("ALL")
                            }
                            FilterChip(title: "Bus", isSelected: viewModel.selectedModeFilter == "BUS") {
                                viewModel.setFilter("BUS")
                            }
                            FilterChip(title: "Tren", isSelected: viewModel.selectedModeFilter == "RAIL") {
                                viewModel.setFilter("RAIL")
                            }
                        }
                        .padding()
                    }
                    .background(Color.black.opacity(0.2))
                    
                    if viewModel.isLoading {
                        Spacer()
                        ProgressView("Buscando mejores rutas...")
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .foregroundColor(.white)
                        Spacer()
                    } else if let error = viewModel.error, viewModel.itineraries.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "exclamationmark.triangle")
                                .font(.largeTitle)
                                .foregroundColor(.yellow)
                            Text(error)
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                            
                            Button(LocalizationUtils.string("Open in Apple Maps")) {
                                mapViewModel.openInAppleMaps() // Fallback
                            }
                            .padding()
                            .background(RacingColors.red)
                            .cornerRadius(10)
                            .foregroundColor(.white)
                        }
                        .padding()
                    } else {
                        List {
                            ForEach(viewModel.itineraries) { itinerary in
                                ZStack {
                                    ItineraryRow(itinerary: itinerary)
                                    // Hidden Navigation Link for cleaner UI interaction
                                    NavigationLink(destination: ItineraryDetailSheet(itinerary: itinerary)) {
                                        EmptyView()
                                    }
                                    .opacity(0)
                                    .buttonStyle(PlainButtonStyle())
                                }
                                .listRowBackground(RacingColors.cardBackground)
                                .listRowSeparator(.hidden)
                                .padding(.bottom, 8)
                            }
                        }
                        .listStyle(.plain)
                        .refreshable {
                            viewModel.loadRoutes(from: mapViewModel.userLocation, to: mapViewModel.transportDestination)
                        }
                    }
                }
            }
            .navigationTitle(LocalizationUtils.string("Public Transport"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizationUtils.string("Close")) { dismiss() }
                }
            }
            .onAppear {
                viewModel.loadRoutes(from: mapViewModel.userLocation, to: mapViewModel.transportDestination)
            }
        }
    }
}

struct ItineraryRow: View {
    let itinerary: Itinerary
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Times and Duration
            HStack {
                VStack(alignment: .leading) {
                    Text("\(formatTime(itinerary.startTime)) - \(formatTime(itinerary.endTime))")
                        .font(RacingFont.header(18))
                        .foregroundColor(.white)
                    Text(formatDuration(itinerary.duration))
                        .font(RacingFont.body(14))
                        .foregroundColor(RacingColors.silver)
                }
                Spacer()
                
                if hasRealTime(itinerary) {
                    HStack(spacing: 4) {
                        Image(systemName: "dot.radiowaves.left.and.right")
                            .symbolEffect(.variableColor.iterative.reversing)
                        Text("En vivo")
                    }
                    .font(.caption.bold())
                    .foregroundColor(.green)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green.opacity(0.2))
                    .cornerRadius(8)
                }
            }
            
            // Legs Visualization
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(Array(itinerary.legs.enumerated()), id: \.offset) { index, leg in
                        HStack(spacing: 0) {
                            // Icon based on mode
                            Image(systemName: iconForMode(leg.mode))
                                .font(.system(size: 14))
                                .foregroundColor(colorForMode(leg.mode))
                                .frame(width: 30, height: 30)
                                .background(Color.white.opacity(0.1))
                                .clipShape(Circle())
                            
                            if let shortName = leg.routeShortName {
                                Text(shortName)
                                    .font(.caption.bold())
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(colorForRoute(leg.routeColor))
                                    .cornerRadius(4)
                                    .padding(.leading, 4)
                            }
                            
                            if index < itinerary.legs.count - 1 {
                                Image(systemName: "chevron.right")
                                    .font(.caption2)
                                    .foregroundColor(RacingColors.silver)
                                    .padding(.horizontal, 8)
                            }
                        }
                    }
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(hex: "1C1C1E")) // Slightly lighter than dark background
        )
    }
    
    // Helpers
    func formatTime(_ millis: Int) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
    
    func formatDuration(_ seconds: Int) -> String {
        let min = seconds / 60
        if min > 60 {
            return "\(min / 60) h \(min % 60) min"
        }
        return "\(min) min"
    }
    
    func hasRealTime(_ it: Itinerary) -> Bool {
        return it.legs.contains { $0.realTime == true }
    }
    
    func iconForMode(_ mode: String) -> String {
        switch mode {
        case "WALK": return "figure.walk"
        case "BUS": return "bus.fill"
        case "RAIL": return "tram.fill"
        case "SUBWAY": return "train.side.front.car"
        default: return "arrow.triangle.swap"
        }
    }
    
    func colorForMode(_ mode: String) -> Color {
        switch mode {
        case "WALK": return .gray
        case "BUS": return .red
        case "RAIL": return .orange
        case "SUBWAY": return .blue
        default: return .white
        }
    }
    
    func colorForRoute(_ hex: String?) -> Color {
        guard let hex = hex else { return .gray }
        return Color(hex: hex)
    }
}

// Color Hex Extension (Simplified)
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}


