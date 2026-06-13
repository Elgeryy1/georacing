import SwiftUI
import MapKit

struct ItineraryDetailSheet: View {
    let itinerary: Itinerary
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                VStack(spacing: 0) {
                    // Header Summary
                    headerSummary
                    
                    Divider().background(Color.white.opacity(0.2))
                    
                    // Steps List
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(Array(itinerary.legs.enumerated()), id: \.offset) { index, leg in
                                LegDetailRow(leg: leg, isLast: index == itinerary.legs.count - 1)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle(LocalizationUtils.string("Trip Detail"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizationUtils.string("Close")) { dismiss() }
                }
            }
        }
    }
    
    var headerSummary: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading) {
                    Text("Duración Total")
                        .font(RacingFont.body(14))
                        .foregroundColor(RacingColors.silver)
                    Text(formatDuration(itinerary.duration))
                        .font(RacingFont.header(24))
                        .foregroundColor(.white)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Llegada Estimada")
                        .font(RacingFont.body(14))
                        .foregroundColor(RacingColors.silver)
                    Text(formatTime(itinerary.endTime))
                        .font(RacingFont.header(24))
                        .foregroundColor(RacingColors.red)
                }
            }
            .padding()
            
            // "Go" Button
            Button(action: {
                showGuidance = true
            }) {
                HStack {
                    Image(systemName: "location.fill")
                    Text("Iniciar Guiado")
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(RacingColors.red)
                .cornerRadius(12)
            }
            .padding(.horizontal)
            .padding(.bottom)
            .fullScreenCover(isPresented: $showGuidance) {
                GuidanceView(itinerary: itinerary)
            }
        }
        .background(RacingColors.cardBackground)
    }
    
    @State private var showGuidance = false
    
    // Helpers (Duplicate of Row logic, could be shared)
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
}

struct LegDetailRow: View {
    let leg: Leg
    let isLast: Bool
    
    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            // Timeline Line
            VStack(spacing: 0) {
                Circle()
                    .fill(colorForMode(leg.mode))
                    .frame(width: 16, height: 16)
                
                // Line connecting to next
                if !isLast {
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .frame(width: 2)
                        .frame(maxHeight: .infinity)
                }
            }
            .frame(width: 20)
            
            // Content
            VStack(alignment: .leading, spacing: 8) {
                // Mode Header
                HStack {
                    Image(systemName: iconForMode(leg.mode))
                        .foregroundColor(colorForMode(leg.mode))
                    Text(modeTitle(leg))
                        .font(RacingFont.header(16))
                        .foregroundColor(.white)
                    Spacer()
                    if let dist = leg.distance {
                        Text("\(Int(dist)) m")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                
                // From / To Info
                VStack(alignment: .leading, spacing: 4) {
                    if leg.mode == "WALK" {
                        Text("Camina hacia \(leg.to.name)")
                            .font(RacingFont.body())
                            .foregroundColor(.white)
                    } else {
                        Text("Sube en: \(leg.from.name)")
                            .font(RacingFont.body())
                            .foregroundColor(.white)
                        Text("Baja en: \(leg.to.name)")
                            .font(RacingFont.body())
                            .foregroundColor(.white)
                    }
                }
                .padding(12)
                .background(Color.white.opacity(0.05))
                .cornerRadius(8)
                
                Spacer().frame(height: 16)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }
    
    func modeTitle(_ leg: Leg) -> String {
        switch leg.mode {
        case "WALK": return "Caminar"
        case "BUS": return "Bus \(leg.routeShortName ?? "")"
        case "RAIL": return "Tren \(leg.routeShortName ?? "")"
        case "SUBWAY": return "Metro \(leg.routeShortName ?? "")"
        default: return leg.mode.capitalized
        }
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
}
