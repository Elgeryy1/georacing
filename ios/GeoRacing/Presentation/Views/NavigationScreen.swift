import SwiftUI
import MapKit
import CoreLocation

/// Full-screen GPS navigation view with turn-by-turn guidance.
/// Default destination: Circuit de Barcelona-Catalunya.
struct NavigationScreen: View {
    
    @StateObject private var viewModel = NavigationViewModel()
    @ObservedObject private var locationManager = LocationManager.shared
    @Environment(\.dismiss) private var dismiss
    
    /// Optional custom destination (if nil, uses default circuit)
    var customDestination: CLLocationCoordinate2D?
    var customDestinationName: String?
    
    var body: some View {
        ZStack {
            // 1. Map (always visible)
            GPSMapView(
                region: $viewModel.mapRegion,
                polyline: viewModel.polyline,
                destinationCoordinate: viewModel.destinationCoordinate,
                destinationName: viewModel.destinationName,
                isFollowingUser: viewModel.isFollowingUser,
                onUserInteraction: { viewModel.isFollowingUser = false }
            )
            .ignoresSafeArea()
            
            // 2. Overlays based on state
            VStack(spacing: 0) {
                switch viewModel.state {
                case .idle:
                    idleTopBar
                    Spacer()
                    idleBottomPanel
                    
                case .calculatingRoute:
                    Spacer()
                    calculatingOverlay
                    
                case .navigating:
                    turnByTurnBanner
                    Spacer()
                    navigationBottomPanel
                    
                case .arrived:
                    Spacer()
                    arrivedPanel
                    
                case .error(let message):
                    Spacer()
                    errorPanel(message)
                }
            }
            
            // 3. Recenter button (visible during navigation when map was dragged)
            if viewModel.state == .navigating && !viewModel.isFollowingUser {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: { viewModel.recenterOnUser() }) {
                            Image(systemName: "location.fill")
                                .font(.title2)
                                .foregroundColor(.white)
                                .padding(14)
                                .background(Circle().fill(Color.blue))
                                .shadow(radius: 4)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 180)
                    }
                }
            }
            
            // 4. GPS status badge
            if locationManager.gpsState != .active && viewModel.state != .idle {
                VStack {
                    gpsStatusBadge
                        .padding(.top, 60)
                    Spacer()
                }
            }
            
            // 5. Rerouting indicator
            if viewModel.isRerouting {
                VStack {
                    Spacer()
                    HStack {
                        ProgressView()
                            .tint(.white)
                        Text(LocalizationUtils.string("Recalculating route"))
                            .font(.subheadline.bold())
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Color.orange))
                    .padding(.bottom, 200)
                }
                .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.state)
        .onAppear {
            if let dest = customDestination, let name = customDestinationName {
                viewModel.destinationCoordinate = dest
                viewModel.destinationName = name
            }
        }
    }
    
    // MARK: - Idle State (Before navigation starts)
    
    private var idleTopBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .foregroundColor(.primary)
                    .padding(12)
                    .background(Circle().fill(.ultraThinMaterial))
            }
            Spacer()
        }
        .padding(.horizontal)
        .padding(.top, 55)
    }
    
    private var idleBottomPanel: some View {
        VStack(spacing: 16) {
            // Destination info
            HStack(spacing: 12) {
                Image(systemName: "flag.checkered")
                    .font(.title2)
                    .foregroundColor(.red)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(LocalizationUtils.string("Destination"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(viewModel.destinationName)
                        .font(.headline)
                        .lineLimit(1)
                }
                Spacer()
            }
            
            // Transport mode picker (Car / Walk / Transit)
            Picker("", selection: $viewModel.transportMode) {
                ForEach([TransportMode.automobile, .walking, .transit], id: \.self) { mode in
                    Label(mode.localizedTitle, systemImage: mode.icon)
                        .tag(mode)
                }
            }
            .pickerStyle(.segmented)
            
            // Transit info banner
            if viewModel.transportMode == .transit {
                HStack(spacing: 8) {
                    Image(systemName: "info.circle.fill")
                        .foregroundColor(.blue)
                    Text(LocalizationUtils.string("Transit opens in Apple Maps"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 8).fill(Color.blue.opacity(0.1)))
            }
            
            // GPS status
            if locationManager.gpsState == .unauthorized {
                HStack {
                    Image(systemName: "location.slash.fill")
                        .foregroundColor(.red)
                    Text(LocalizationUtils.string("Location permission required"))
                        .font(.subheadline)
                        .foregroundColor(.red)
                }
            }
            
            // Start button
            Button(action: { viewModel.startNavigation() }) {
                HStack {
                    Image(systemName: viewModel.transportMode == .transit
                          ? "arrow.up.right.square.fill"
                          : "arrow.triangle.turn.up.right.circle.fill")
                        .font(.title2)
                    Text(viewModel.transportMode == .transit
                         ? LocalizationUtils.string("Open in Apple Maps")
                         : LocalizationUtils.string("Start Navigation"))
                        .font(.headline)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .fill(locationManager.location != nil ? Color.blue : Color.gray)
                )
            }
            .disabled(locationManager.location == nil)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(.ultraThickMaterial)
                .shadow(radius: 10)
        )
        .padding()
    }
    
    // MARK: - Calculating Route
    
    private var calculatingOverlay: some View {
        VStack(spacing: 12) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.blue)
            Text(LocalizationUtils.string("Calculating route..."))
                .font(.headline)
        }
        .padding(30)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThickMaterial)
        )
    }
    
    // MARK: - Turn-by-Turn Banner
    
    private var turnByTurnBanner: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 16) {
                // Maneuver icon
                Image(systemName: viewModel.maneuverIcon)
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 50)
                
                VStack(alignment: .leading, spacing: 4) {
                    // Distance to next step
                    Text(viewModel.formattedNextStepDistance)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)
                    
                    // Instruction
                    Text(viewModel.nextInstruction)
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.9))
                        .lineLimit(2)
                }
                
                Spacer()
                
                // Close navigation
                Button(action: { viewModel.stopNavigation() }) {
                    Image(systemName: "xmark")
                        .font(.headline)
                        .foregroundColor(.white.opacity(0.8))
                        .padding(10)
                        .background(Circle().fill(Color.white.opacity(0.2)))
                }
            }
            .padding()
            .background(Color.blue.gradient)
            .padding(.top, 44) // Safe area offset
            
            // Step progress indicator
            if !viewModel.steps.isEmpty {
                GeometryReader { geo in
                    Rectangle()
                        .fill(Color.blue.opacity(0.3))
                        .frame(height: 3)
                        .overlay(alignment: .leading) {
                            let progress = viewModel.steps.isEmpty
                                ? 0
                                : CGFloat(viewModel.currentStepIndex) / CGFloat(viewModel.steps.count)
                            Rectangle()
                                .fill(Color.white)
                                .frame(width: geo.size.width * progress, height: 3)
                        }
                }
                .frame(height: 3)
            }
        }
    }
    
    // MARK: - Navigation Bottom Panel
    
    private var navigationBottomPanel: some View {
        VStack(spacing: 12) {
            HStack {
                // ETA
                VStack(alignment: .leading, spacing: 2) {
                    Text(LocalizationUtils.string("Arrival"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(viewModel.formattedETATime)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                // Remaining time
                VStack(spacing: 2) {
                    Text(viewModel.formattedETA)
                        .font(.title3.bold())
                    Text(LocalizationUtils.string("remaining"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Remaining distance
                VStack(spacing: 2) {
                    Text(viewModel.formattedDistance)
                        .font(.title3.bold())
                    Text(LocalizationUtils.string("distance"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Divider()
            
            HStack(spacing: 20) {
                // Mode toggle
                ForEach([TransportMode.automobile, .walking, .transit], id: \.self) { mode in
                    Button(action: { viewModel.transportMode = mode }) {
                        Image(systemName: mode.icon)
                            .font(.title3)
                            .foregroundColor(viewModel.transportMode == mode ? .white : .secondary)
                            .padding(10)
                            .background(
                                Circle().fill(viewModel.transportMode == mode ? Color.blue : Color.gray.opacity(0.2))
                            )
                    }
                }
                
                Spacer()
                
                // End navigation
                Button(action: { viewModel.stopNavigation() }) {
                    Text(LocalizationUtils.string("End Navigation"))
                        .font(.subheadline.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Color.red))
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(.ultraThickMaterial)
                .shadow(radius: 10)
        )
        .padding()
    }
    
    // MARK: - Arrived
    
    private var arrivedPanel: some View {
        VStack(spacing: 16) {
            Image(systemName: "flag.checkered")
                .font(.system(size: 48))
                .foregroundColor(.green)
            
            Text(LocalizationUtils.string("You have arrived!"))
                .font(.title2.bold())
            
            Text(viewModel.destinationName)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Button(action: {
                viewModel.stopNavigation()
                dismiss()
            }) {
                Text(LocalizationUtils.string("Close"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color.green))
            }
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(.ultraThickMaterial)
                .shadow(radius: 10)
        )
        .padding()
    }
    
    // MARK: - Error
    
    private func errorPanel(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 36))
                .foregroundColor(.red)
            
            Text(message)
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            
            HStack(spacing: 12) {
                Button(action: { viewModel.startNavigation() }) {
                    Text(LocalizationUtils.string("Retry"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color.blue))
                }
                
                Button(action: { dismiss() }) {
                    Text(LocalizationUtils.string("Close"))
                        .font(.headline)
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color.gray.opacity(0.2)))
                }
            }
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(.ultraThickMaterial)
                .shadow(radius: 10)
        )
        .padding()
    }
    
    // MARK: - GPS Status Badge
    
    private var gpsStatusBadge: some View {
        HStack(spacing: 6) {
            switch locationManager.gpsState {
            case .unauthorized:
                Image(systemName: "location.slash.fill")
                Text(LocalizationUtils.string("No GPS permission"))
            case .searching:
                ProgressView().tint(.white)
                Text(LocalizationUtils.string("Searching GPS..."))
            case .lowAccuracy:
                Image(systemName: "location.circle")
                Text(LocalizationUtils.string("Low GPS accuracy"))
            case .error(let msg):
                Image(systemName: "exclamationmark.triangle")
                Text(msg)
            case .active:
                EmptyView()
            }
        }
        .font(.caption.bold())
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Capsule().fill(Color.black.opacity(0.7)))
    }
}

#Preview {
    NavigationScreen()
}
