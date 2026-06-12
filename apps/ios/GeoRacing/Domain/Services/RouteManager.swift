import Foundation
import CoreLocation
import Combine

class RouteManager: ObservableObject {
    static let shared = RouteManager()
    
    // Published State
    @Published var currentRoute: [CLLocationCoordinate2D] = []
    @Published var routeSteps: [RouteStep] = []
    @Published var isNavigating = false
    @Published var currentInstruction: String?
    @Published var distanceToNextManeuver: Double = 0
    @Published var isOffRoute = false
    
    // Dependencies
    private let repository = RouteRepository.shared
    private var cancellables = Set<AnyCancellable>()
    
    // Snapping State (Android Spec 1.2)
    private var lastSnappedIndex: Int = 0
    
    // Off-Route State (Android Spec 1.3)
    private var offRouteTimestamp: Date?
    private let offRouteDistanceThreshold: Double = AppConstants.offRouteDistanceThreshold // 50m
    private let offRouteTimeThreshold: TimeInterval = 3.0 // 3 seconds (Android Spec)
    
    private var currentDestination: CLLocationCoordinate2D?
    
    private init() {
        setupLocationSubscription()
    }
    
    private func setupLocationSubscription() {
        LocationManager.shared.$location
            .compactMap { $0 } // Filter nil
            .map { CLLocation(latitude: $0.latitude, longitude: $0.longitude) } // Convert to CLLocation
            .receive(on: DispatchQueue.main)
            .sink { [weak self] location in
                Task {
                    // print("[RouteManager] Location update received")
                    await self?.updateLocation(location)
                }
            }
            .store(in: &cancellables)
    }
    
    // MARK: - API
    
    @MainActor
    func calculateRoute(from start: CLLocationCoordinate2D, to end: CLLocationCoordinate2D) async {
        Logger.debug("[RouteManager] Calculating route...")
        self.currentDestination = end
        
        do {
            let result = try await repository.fetchRoute(from: start, to: end)
            
            self.currentRoute = result.geometry
            self.routeSteps = result.steps
            self.isNavigating = true
            self.lastSnappedIndex = 0
            self.offRouteTimestamp = nil
            self.isOffRoute = false
            
            // Initial Instruction
            if let first = result.steps.first {
                self.currentInstruction = first.instruction
                self.distanceToNextManeuver = first.distance
            }
            
            Logger.info("[RouteManager] Route calculated: \(result.distance)m, \(result.steps.count) steps")
            
        } catch {
            Logger.error("[RouteManager] Failed to calculate route: \(error)")
            self.isNavigating = false
        }
    }
    
    @MainActor
    func clearRoute() {
        self.currentRoute = []
        self.routeSteps = []
        self.isNavigating = false
        self.currentInstruction = nil
        self.offRouteTimestamp = nil
    }
    
    // MARK: - Location Updates & Core Logic (Spec 1.2 & 1.3)
    
    @MainActor
    func updateLocation(_ location: CLLocation) {
        guard isNavigating, !currentRoute.isEmpty else { return }
        
        // 1.2 Snap to Route
        let snapResult = snapToRoute(location.coordinate)
        
        // 1.3 Check Off Route
        if checkOffRoute(snapResult) {
            Logger.warning("[RouteManager] USER IS OFF ROUTE! Recalculating...")
            self.isOffRoute = true
            
            // Trigger Recalculation
            // Debounce or immediate? Spec says "Dispara el recálculo inmediato"
            if let dest = currentDestination {
                Task {
                    await calculateRoute(from: location.coordinate, to: dest)
                }
            }
            return
        } else {
            self.isOffRoute = false
        }
        
        // Update Progress (Instruction / Distance)
        // Find next step based on snapped index or distance
        updateNavigationProgress(snapResult)
    }
    
    // MARK: - Private Algorithms
    
    /// Android Spec 1.2: Snap to Route (Optimized)
    private func snapToRoute(_ location: CLLocationCoordinate2D) -> SnapResult {
        let windowSize = 50
        let startIndex = max(0, lastSnappedIndex - windowSize)
        let endIndex = min(currentRoute.count - 1, lastSnappedIndex + windowSize)
        
        var minDistance: Double = .greatestFiniteMagnitude
        var bestIndex = lastSnappedIndex
        var bestPoint = location // Fallback
        
        let loc = CLLocation(latitude: location.latitude, longitude: location.longitude)
        
        // Iterate window
        for i in startIndex...endIndex {
            let point = currentRoute[i]
            let pLoc = CLLocation(latitude: point.latitude, longitude: point.longitude)
            let dist = loc.distance(from: pLoc)
            
            if dist < minDistance {
                minDistance = dist
                bestIndex = i
                bestPoint = point
            }
        }
        
        // Update state
        self.lastSnappedIndex = bestIndex
        
        return SnapResult(
            snappedLocation: bestPoint,
            routeIndex: bestIndex,
            distanceToRoute: minDistance,
            isSuccessful: minDistance <= offRouteDistanceThreshold
        )
    }
    
    /// Android Spec 1.3: Off Route Detector
    private func checkOffRoute(_ snap: SnapResult) -> Bool {
        if snap.distanceToRoute > offRouteDistanceThreshold {
            // Case YES
            if let timestamp = offRouteTimestamp {
                // Already tracking
                if Date().timeIntervalSince(timestamp) > offRouteTimeThreshold {
                    return true // Confirmed Off Route
                }
            } else {
                // Sample 0
                offRouteTimestamp = Date()
                return false // Waiting for confirmation
            }
        } else {
            // Case NO (Back on track)
            offRouteTimestamp = nil
        }
        return false
    }
    
    private func updateNavigationProgress(_ snap: SnapResult) {
        // Logic to update distanceToNextManeuver
        // This is simplified. Real logic projects point to polyline segment.
        // For parity, we assume distance to next step's start node.
        
        // Find the step that corresponds to current index?
        // OSRM steps don't map 1:1 to indices easily without geometry matching.
        // Simplified: Just show first step for now or keep previous instruction.
        // To do this well, we'd need to map route indices to steps (via Leg -> Annotation).
        
        // Stub update for now to just show we are alive
        // self.distanceToNextManeuver = ...
    }
}
