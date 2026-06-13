import SwiftUI
import MapKit

struct GuidanceView: View {
    @StateObject private var viewModel: GuidanceViewModel
    @Environment(\.dismiss) var dismiss
    
    init(itinerary: Itinerary) {
        _viewModel = StateObject(wrappedValue: GuidanceViewModel(itinerary: itinerary))
    }
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
            
            VStack {
                // Header
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(.white)
                    }
                    Spacer()
                    Text(LocalizationUtils.string("Route Guidance"))
                        .font(RacingFont.header(20))
                        .foregroundColor(.white)
                    Spacer()
                    // Hidden balance for centering
                    Image(systemName: "xmark.circle.fill").font(.title).opacity(0)
                }
                .padding()
                
                // Progress Bar
                ProgressView(value: Double(viewModel.currentStepIndex + 1), total: Double(viewModel.itinerary.legs.count))
                    .tint(RacingColors.red)
                    .padding(.horizontal)
                
                // Content Carousel (Driven by VM)
                TabView(selection: $viewModel.currentStepIndex) {
                    ForEach(Array(viewModel.itinerary.legs.enumerated()), id: \.offset) { index, leg in
                        GuidanceStepCard(
                            leg: leg,
                            stepNumber: index + 1,
                            totalSteps: viewModel.itinerary.legs.count,
                            liveDistance: (index == viewModel.currentStepIndex) ? viewModel.distanceToNextStop : nil
                        )
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut, value: viewModel.currentStepIndex)
                
                // Controls
                HStack(spacing: 20) {
                    if viewModel.currentStepIndex > 0 {
                        Button(action: { 
                            withAnimation { viewModel.prevStep() }
                        }) {
                            Image(systemName: "arrow.left")
                                .font(.title2)
                                .foregroundColor(.white)
                                .frame(width: 50, height: 50)
                                .background(Color.white.opacity(0.1))
                                .clipShape(Circle())
                        }
                    } else {
                        Spacer().frame(width: 50)
                    }
                    
                    if viewModel.currentStepIndex < viewModel.itinerary.legs.count - 1 {
                        Button(action: { 
                            withAnimation { viewModel.advanceStep() }
                        }) {
                            VStack(spacing: 2) {
                                Text("Siguiente Paso")
                                    .font(RacingFont.header(18))
                                if viewModel.distanceToNextStop > 0 {
                                    Text("\(Int(viewModel.distanceToNextStop)) m restantes")
                                        .font(.caption)
                                        .foregroundColor(.white.opacity(0.8))
                                }
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(RacingColors.red)
                            .cornerRadius(25)
                        }
                    } else {
                        Button(action: { dismiss() }) {
                            Text("Finalizar Viaje")
                                .font(RacingFont.header(18))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(Color.green)
                                .cornerRadius(25)
                        }
                    }
                }
                .padding()
                .padding(.bottom, 20)
            }
            
            // Toast for Completion
            if let msg = viewModel.feedbackMessage {
                VStack {
                    Spacer()
                    Text(msg)
                        .font(RacingFont.header(24))
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(16)
                        .padding(.bottom, 100)
                }
                .transition(.scale)
            }
        }
    }
}

struct GuidanceStepCard: View {
    let leg: Leg
    let stepNumber: Int
    let totalSteps: Int
    let liveDistance: Double? // New: Show live GPS distance if active leg
    
    var body: some View {
        VStack(spacing: 24) {
            // Icon & Mode
            VStack(spacing: 8) {
                Image(systemName: iconForMode(leg.mode))
                    .font(.system(size: 60))
                    .foregroundColor(colorForMode(leg.mode))
                    .symbolEffect(.bounce, value: liveDistance) // Animate if tracking
                
                Text(modeTitle(leg))
                    .font(RacingFont.header(24))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
            }
            .padding(.top, 40)
            
            Divider().background(Color.white.opacity(0.2))
            
            // Instruction
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .top) {
                    Image(systemName: "mappin.and.ellipse")
                        .foregroundColor(RacingColors.red)
                    VStack(alignment: .leading) {
                        Text("SALIDA")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text(leg.from.name)
                            .font(.title3)
                            .foregroundColor(.white)
                        if let dep = leg.from.departureTime {
                            Text("Hora: " + formatTime(dep))
                                .font(.caption)
                                .foregroundColor(RacingColors.red)
                        }
                    }
                }
                
                // Connecting line (dashed idea)
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 2, height: 30)
                    .padding(.leading, 9)
                
                HStack(alignment: .top) {
                    Image(systemName: "mappin.circle.fill")
                        .foregroundColor(RacingColors.red)
                    VStack(alignment: .leading) {
                        Text("DESTINO")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text(leg.to.name)
                            .font(.title3)
                            .foregroundColor(.white)
                        if let arr = leg.to.arrivalTime {
                            Text("Llegada: " + formatTime(arr))
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                }
            }
            .padding()
            .background(Color.white.opacity(0.05))
            .cornerRadius(16)
            
            // Live Stats vs Static Stats
            HStack(spacing: 40) {
                VStack {
                    Image(systemName: "ruler")
                    if let live = liveDistance {
                        Text("\(Int(live)) m")
                            .foregroundColor(RacingColors.red)
                            .fontWeight(.bold)
                    } else {
                        Text("\(Int(leg.distance ?? 0)) m")
                    }
                }
                
                VStack {
                    Image(systemName: "clock")
                    if let live = liveDistance {
                        // Estimate walking time
                        Text("~\(Int(live / 1.2 / 60)) min")
                            .foregroundColor(RacingColors.red)
                            .fontWeight(.bold)
                    } else {
                         Text(formatDuration(leg.duration))
                    }
                }
            }
            .font(.headline)
            .foregroundColor(.gray)
            
            Spacer()
            
            Text("Paso \(stepNumber) de \(totalSteps)")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .padding()
        .background(RacingColors.cardBackground)
        .cornerRadius(24)
        .padding()
        .shadow(radius: 20)
    }
    
    // Helpers (Reused from other views, good candidate for shared helper file)
    func modeTitle(_ leg: Leg) -> String {
        switch leg.mode {
        case "WALK": return "Camina hacia \(leg.to.name)"
        case "BUS": return "Autobús \(leg.routeShortName ?? "") -> \(leg.to.name)"
        case "RAIL": return "Tren \(leg.routeShortName ?? "") -> \(leg.to.name)"
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
    
    func formatDuration(_ seconds: Int) -> String {
        let min = seconds / 60
        if min > 60 {
            return "\(min / 60) h \(min % 60) m"
        }
        return "\(min) min"
    }
    
    func formatTime(_ timestamp: Int) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}
