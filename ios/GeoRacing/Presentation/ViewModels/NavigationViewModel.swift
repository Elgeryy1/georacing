import Foundation
import SwiftUI
import Combine
import CoreLocation
import MapKit

// MARK: - Default Destination

/// Central constant for the circuit destination used throughout the app.
enum DefaultDestination {
    static let name = "Circuit de Barcelona-Catalunya"
    static let coordinate = CLLocationCoordinate2D(latitude: 41.5700, longitude: 2.2610)
}

// MARK: - Navigation State

enum NavigationState: Equatable {
    case idle              // Waiting to start
    case calculatingRoute  // Computing route via MKDirections
    case navigating        // Active turn-by-turn
    case arrived           // User reached destination
    case error(String)     // Something failed
}

// MARK: - NavigationViewModel

/// Real GPS navigation view model with turn-by-turn, rerouting, and live tracking.
@MainActor
class NavigationViewModel: ObservableObject {
    
    // MARK: - Published State
    
    /// Current navigation state
    @Published var state: NavigationState = .idle
    
    /// Destination
    @Published var destinationName: String = DefaultDestination.name
    @Published var destinationCoordinate: CLLocationCoordinate2D = DefaultDestination.coordinate
    
    /// Computed route
    @Published var route: MKRoute?
    @Published var polyline: MKPolyline?
    
    /// Turn-by-turn steps
    @Published var steps: [MKRoute.Step] = []
    @Published var currentStepIndex: Int = 0
    
    /// Live metrics
    @Published var remainingDistance: CLLocationDistance = 0  // meters
    @Published var remainingTime: TimeInterval = 0           // seconds
    @Published var eta: Date?
    @Published var nextInstruction: String = ""
    @Published var nextStepDistance: CLLocationDistance = 0
    
    /// Transport mode
    @Published var transportMode: TransportMode = .automobile {
        didSet {
            guard oldValue != transportMode else { return }
            if transportMode == .transit && state == .navigating {
                // Stop in-app nav, user will use Apple Maps for transit
                stopNavigation()
            } else if state == .navigating {
                recalculateRoute(reason: "mode_change")
            }
        }
    }
    
    /// Transit: flag to open Apple Maps with transit directions
    @Published var showTransitAction: Bool = false
    
    /// Map control
    @Published var mapRegion = MKCoordinateRegion(
        center: DefaultDestination.coordinate,
        span: MKCoordinateSpan(latitudeDelta: 0.5, longitudeDelta: 0.5)
    )
    @Published var isFollowingUser: Bool = true
    
    /// Reroute indicator
    @Published var isRerouting: Bool = false
    
    // MARK: - Private
    
    private let locationManager = LocationManager.shared
    private let speechService = SpeechService.shared
    private var cancellables = Set<AnyCancellable>()
    
    /// Reroute throttle: minimum seconds between reroutes
    private let rerouteThrottleInterval: TimeInterval = 15
    private var lastRerouteTime: Date = .distantPast
    
    /// Off-route detection: how many consecutive off-route samples before trigger
    private var offRouteCounter: Int = 0
    private let offRouteThreshold: Int = 3          // 3 consecutive samples
    private let offRouteDistanceMeters: Double = 50 // 50m from route
    
    /// Arrival radius
    private let arrivalRadius: Double = 30 // meters
    
    // MARK: - Init
    
    init() {
        setupLocationSubscription()
    }
    
    // MARK: - Location Pipeline
    
    private func setupLocationSubscription() {
        locationManager.$clLocation
            .compactMap { $0 }
            .removeDuplicates { old, new in
                old.coordinate.latitude == new.coordinate.latitude &&
                old.coordinate.longitude == new.coordinate.longitude
            }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] location in
                self?.handleLocationUpdate(location)
            }
            .store(in: &cancellables)
    }
    
    private func handleLocationUpdate(_ location: CLLocation) {
        guard state == .navigating, route != nil else { return }
        
        updateLiveMetrics(from: location)
        checkStepAdvance(from: location)
        checkArrival(from: location)
        checkOffRoute(from: location)
    }
    
    // MARK: - Public Actions
    
    /// Start navigation to the default circuit destination.
    func startNavigation() {
        destinationName = DefaultDestination.name
        destinationCoordinate = DefaultDestination.coordinate
        
        if transportMode == .transit {
            openTransitInAppleMaps()
            return
        }
        calculateAndBeginRoute()
    }
    
    /// Start navigation to a custom destination.
    func startNavigation(to coordinate: CLLocationCoordinate2D, name: String) {
        destinationName = name
        destinationCoordinate = coordinate
        
        if transportMode == .transit {
            openTransitInAppleMaps()
            return
        }
        calculateAndBeginRoute()
    }
    
    /// Open Apple Maps with transit directions to the destination.
    func openTransitInAppleMaps() {
        let destinationItem = MKMapItem.fromCoordinate(destinationCoordinate)
        destinationItem.name = destinationName
        destinationItem.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeTransit
        ])
    }
    
    /// Stop navigation and reset.
    func stopNavigation() {
        speechService.stop()
        state = .idle
        route = nil
        polyline = nil
        steps = []
        currentStepIndex = 0
        remainingDistance = 0
        remainingTime = 0
        eta = nil
        nextInstruction = ""
        nextStepDistance = 0
        isRerouting = false
        offRouteCounter = 0
    }
    
    /// Recenter map on user.
    func recenterOnUser() {
        guard let loc = locationManager.location else { return }
        isFollowingUser = true
        withAnimation {
            mapRegion = MKCoordinateRegion(
                center: loc,
                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            )
        }
    }
    
    /// Zoom to show the entire route.
    func zoomToRoute() {
        guard let route else { return }
        let rect = route.polyline.boundingMapRect
        let padded = rect.insetBy(dx: -rect.width * 0.15, dy: -rect.height * 0.15)
        withAnimation {
            mapRegion = MKCoordinateRegion(padded)
        }
    }
    
    // MARK: - Route Calculation
    
    private func calculateAndBeginRoute() {
        guard let origin = locationManager.location else {
            state = .error(LocalizationUtils.string("Could not get your location"))
            return
        }
        
        state = .calculatingRoute
        
        Task {
            do {
                let mkRoute = try await calculateRoute(
                    from: origin,
                    to: destinationCoordinate,
                    mode: transportMode
                )
                
                self.route = mkRoute
                self.polyline = mkRoute.polyline
                self.steps = mkRoute.steps.filter { !$0.instructions.isEmpty }
                self.currentStepIndex = 0
                self.remainingDistance = mkRoute.distance
                self.remainingTime = mkRoute.expectedTravelTime
                self.eta = Date().addingTimeInterval(mkRoute.expectedTravelTime)
                self.offRouteCounter = 0
                self.state = .navigating
                
                updateNextInstruction()
                speakCurrentStep()
                zoomToRoute()
                
                // Auto-follow after brief overview
                try? await Task.sleep(for: .seconds(2))
                recenterOnUser()
                
            } catch {
                Logger.error("[NavigationVM] Route error: \(error)")
                self.state = .error(error.localizedDescription)
            }
        }
    }
    
    private func calculateRoute(
        from origin: CLLocationCoordinate2D,
        to destination: CLLocationCoordinate2D,
        mode: TransportMode
    ) async throws -> MKRoute {
        let request = MKDirections.Request()
        request.source = MKMapItem.fromCoordinate(origin)
        request.destination = MKMapItem.fromCoordinate(destination)
        request.transportType = mode.mkDirectionsTransportType
        request.requestsAlternateRoutes = false
        
        let directions = MKDirections(request: request)
        let response = try await directions.calculate()
        
        guard let route = response.routes.first else {
            throw NavigationError.noRouteFound
        }
        return route
    }
    
    // MARK: - Live Metrics
    
    private func updateLiveMetrics(from location: CLLocation) {
        // Remaining distance = sum of remaining step distances
        var remaining: CLLocationDistance = 0
        for i in currentStepIndex..<steps.count {
            remaining += steps[i].distance
        }
        // Add distance from current position to current step endpoint
        if currentStepIndex < steps.count {
            let stepCoord = steps[currentStepIndex].polyline.coordinate
            let stepLoc = CLLocation(latitude: stepCoord.latitude, longitude: stepCoord.longitude)
            let toStep = location.distance(from: stepLoc)
            // Replace step's full distance with actual distance-to-step
            remaining = remaining - steps[currentStepIndex].distance + toStep
        }
        
        remainingDistance = max(remaining, 0)
        
        // ETA based on current speed or route expected time
        if location.speed > 1 {
            remainingTime = remainingDistance / location.speed
        } else if let route {
            // Estimate proportionally
            let fraction = remainingDistance / max(route.distance, 1)
            remainingTime = route.expectedTravelTime * fraction
        }
        eta = Date().addingTimeInterval(remainingTime)
    }
    
    // MARK: - Step Advance
    
    private func checkStepAdvance(from location: CLLocation) {
        guard currentStepIndex < steps.count else { return }
        
        let step = steps[currentStepIndex]
        let stepCoord = step.polyline.coordinate
        let stepLoc = CLLocation(latitude: stepCoord.latitude, longitude: stepCoord.longitude)
        let dist = location.distance(from: stepLoc)
        
        // Threshold depends on transport mode and speed
        let threshold: Double = transportMode == .walking ? 15 : 35
        
        if dist < threshold && currentStepIndex < steps.count - 1 {
            currentStepIndex += 1
            updateNextInstruction()
            speakCurrentStep()
            offRouteCounter = 0 // Reset off-route when advancing
        }
        
        // Update distance to next step
        nextStepDistance = dist
    }
    
    // MARK: - Arrival Detection
    
    private func checkArrival(from location: CLLocation) {
        let destLoc = CLLocation(
            latitude: destinationCoordinate.latitude,
            longitude: destinationCoordinate.longitude
        )
        let dist = location.distance(from: destLoc)
        
        if dist < arrivalRadius {
            speechService.speak(LocalizationUtils.string("You have arrived at your destination."))
            state = .arrived
        }
    }
    
    // MARK: - Off-Route / Reroute
    
    private func checkOffRoute(from location: CLLocation) {
        guard let route else { return }
        
        // Find minimum distance from user to any point on the route polyline
        let distToRoute = minimumDistance(from: location, to: route.polyline)
        
        if distToRoute > offRouteDistanceMeters {
            offRouteCounter += 1
            if offRouteCounter >= offRouteThreshold {
                recalculateRoute(reason: "off_route")
            }
        } else {
            offRouteCounter = 0
        }
    }
    
    /// Recalculate route from current position (throttled).
    private func recalculateRoute(reason: String) {
        let now = Date()
        guard now.timeIntervalSince(lastRerouteTime) > rerouteThrottleInterval else { return }
        guard !isRerouting else { return }
        
        Logger.info("[NavigationVM] Rerouting: \(reason)")
        lastRerouteTime = now
        isRerouting = true
        offRouteCounter = 0
        
        speechService.speak(LocalizationUtils.string("Recalculating route"))
        
        Task {
            guard let origin = locationManager.location else {
                isRerouting = false
                return
            }
            
            do {
                let mkRoute = try await calculateRoute(
                    from: origin,
                    to: destinationCoordinate,
                    mode: transportMode
                )
                
                self.route = mkRoute
                self.polyline = mkRoute.polyline
                self.steps = mkRoute.steps.filter { !$0.instructions.isEmpty }
                self.currentStepIndex = 0
                self.remainingDistance = mkRoute.distance
                self.remainingTime = mkRoute.expectedTravelTime
                self.eta = Date().addingTimeInterval(mkRoute.expectedTravelTime)
                self.isRerouting = false
                
                updateNextInstruction()
                speakCurrentStep()
                
            } catch {
                Logger.error("[NavigationVM] Reroute failed: \(error)")
                isRerouting = false
            }
        }
    }
    
    // MARK: - Helpers
    
    private func updateNextInstruction() {
        if currentStepIndex < steps.count {
            nextInstruction = steps[currentStepIndex].instructions
        } else {
            nextInstruction = LocalizationUtils.string("You have arrived at your destination.")
        }
    }
    
    private func speakCurrentStep() {
        guard currentStepIndex < steps.count else { return }
        let step = steps[currentStepIndex]
        if !step.instructions.isEmpty {
            speechService.speak(step.instructions)
        }
    }
    
    /// Find minimum distance from a point to any coordinate along an MKPolyline.
    private func minimumDistance(from location: CLLocation, to polyline: MKPolyline) -> CLLocationDistance {
        let points = polyline.points()
        let count = polyline.pointCount
        
        var minDist: CLLocationDistance = .greatestFiniteMagnitude
        
        // Sample every few points for performance (full polyline can be thousands of points)
        let stride = max(1, count / 200)
        for i in Swift.stride(from: 0, to: count, by: stride) {
            let mapPoint = points[i]
            let coord = mapPoint.coordinate
            let pointLoc = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
            let dist = location.distance(from: pointLoc)
            if dist < minDist {
                minDist = dist
            }
        }
        
        return minDist
    }
    
    // MARK: - Formatted Strings
    
    var formattedDistance: String {
        if remainingDistance >= 1000 {
            return String(format: "%.1f km", remainingDistance / 1000)
        } else {
            return String(format: "%.0f m", remainingDistance)
        }
    }
    
    var formattedETA: String {
        let hours = Int(remainingTime) / 3600
        let minutes = (Int(remainingTime) % 3600) / 60
        if hours > 0 {
            return "\(hours)h \(minutes)min"
        } else {
            return "\(minutes) min"
        }
    }
    
    var formattedETATime: String {
        guard let eta else { return "--:--" }
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        return fmt.string(from: eta)
    }
    
    var formattedNextStepDistance: String {
        if nextStepDistance >= 1000 {
            return String(format: "%.1f km", nextStepDistance / 1000)
        } else {
            return String(format: "%.0f m", nextStepDistance)
        }
    }
    
    /// Icon for the current maneuver based on step instructions.
    var maneuverIcon: String {
        let inst = nextInstruction.lowercased()
        if inst.contains("left") || inst.contains("izquierda") || inst.contains("esquerra") {
            return "arrow.turn.up.left"
        } else if inst.contains("right") || inst.contains("derecha") || inst.contains("dreta") {
            return "arrow.turn.up.right"
        } else if inst.contains("u-turn") || inst.contains("media vuelta") || inst.contains("mitja volta") {
            return "arrow.uturn.down"
        } else if inst.contains("roundabout") || inst.contains("rotonda") || inst.contains("rotonda") {
            return "arrow.triangle.turn.up.right.circle"
        } else if inst.contains("merge") || inst.contains("incorpor") {
            return "arrow.merge"
        } else if inst.contains("exit") || inst.contains("salida") || inst.contains("sortida") {
            return "arrow.up.right"
        } else if inst.contains("destination") || inst.contains("destino") || inst.contains("destinació") {
            return "flag.checkered"
        } else {
            return "arrow.up"
        }
    }
}
