import Foundation
import SwiftUI
import Combine
import CoreLocation
import MapKit

@MainActor
class MapViewModel: ObservableObject {

    @Published var circuit: Circuit?
    @Published var pois: [Poi] = []
    @Published var beacons: [BeaconConfig] = []
    @Published var filteredPOIs: [Poi] = []
    @Published var selectedCategory: PoiType? = nil
    @Published var isLoading = false
    
    // Map State
    @Published var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 41.570, longitude: 2.260),
        span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
    )
    
    // user & friends location
    @Published var userLocation: CLLocationCoordinate2D?
    @Published var friends: [GroupMember] = []
    
    private var cancellables = Set<AnyCancellable>()
    private let groupRepository = GroupRepository.shared
    private let navigationService = NavigationService.shared
    private let speechService = SpeechService.shared
    
    // ... (Navigation State fields remain same)

    // ... (init remains same)

    // ...


    
    // Navigation State
    @Published var activeRoute: NavigationRoute?
    @Published var routePolyline: MKPolyline?
    @Published var transportMode: TransportMode = .automobile
    @Published var isCalculatingRoute = false
    @Published var showNavigationOverlay = false
    @Published var navigationError: String?
    
    // Web Navigation (for Transit) -- Deprecated/Removed in favor of Native Sheet
    @Published var activeWebUrl: URL?
    @Published var showWebNavigation = false
    
    // Native OpenTripPlanner Sheet
    @Published var showTransportSheet = false
    
    // Turn-by-Turn State
    @Published var currentStepIndex: Int = 0
    @Published var userTrackingMode: MapUserTrackingMode = .follow
    

    

    // Annotations for Map
    @Published var allAnnotations: [MapAnnotationItem] = []
    
    // Thermal Navigation (Shadow Zones)
    @Published var shadowPolygons: [MKPolygon] = []

    init() {
        // Load dummy data or fetch
        // For MVP, we setup a dummy circuit (Barcelona style for coords match Repository mocks)
        let montmelo = Circuit(
            name: "Circuit de Barcelona-Catalunya",
            bounds: MapBounds(minLat: 41.560, maxLat: 41.580, minLon: 2.250, maxLon: 2.270),
            imageAssetName: "circuit_map_base"
        )
        self.circuit = montmelo
        
        // Init region based on circuit
        self.region = MKCoordinateRegion(
            center: montmelo.bounds.center,
            span: MKCoordinateSpan(latitudeDelta: (montmelo.bounds.maxLat - montmelo.bounds.minLat) * 1.2,
                                   longitudeDelta: (montmelo.bounds.maxLon - montmelo.bounds.minLon) * 1.2)
        )
        
        // Load Thermal Routing Polygons
        self.shadowPolygons = ThermalRoutingService.shared.getShadowPolygons()
        
        setupPipelines()
        
        // Call loadPOIs when the view model is initialized
        loadPOIs()
        loadBeacons()
        setupGroupSubscription()
        setupLocationSubscription()
    }
    
    private func setupPipelines() {
        // Combine filteredPOIs, friends, and beacons into one annotation list
        Publishers.CombineLatest3($filteredPOIs, $friends, $beacons)
            .debounce(for: .milliseconds(100), scheduler: RunLoop.main) // Debounce to avoid rapid updates
            .map { [weak self] (pois, friends, beacons) -> [MapAnnotationItem] in
                guard let self = self else { return [] }
                var items: [MapAnnotationItem] = []
                
                // POIs
                for poi in pois {
                    let coord = self.coordinate(forX: poi.mapX, y: poi.mapY)
                    items.append(MapAnnotationItem(id: "poi_\(poi.id)", coordinate: coord, type: .poi(poi)))
                }
                
                // Friends
                for friend in friends {
                    if let coord = friend.coordinate {
                        items.append(MapAnnotationItem(id: "friend_\(friend.id)", coordinate: coord, type: .friend(friend)))
                    }
                }
                
                // Beacons
                for beacon in beacons {
                    let coord = self.coordinate(forX: beacon.mapX, y: beacon.mapY)
                    items.append(MapAnnotationItem(id: "beacon_\(beacon.id)", coordinate: coord, type: .beacon(beacon)))
                }
                return items
            }
            .assign(to: \.allAnnotations, on: self)
            .store(in: &cancellables)
    }

    private func setupLocationSubscription() {
        LocationManager.shared.$location
            .receive(on: DispatchQueue.main)
            .assign(to: \.userLocation, on: self)
            .store(in: &cancellables)
            
        // Monitor Navigation Progress
        LocationManager.shared.$location
            .receive(on: DispatchQueue.main)
            .compactMap { $0 }
            .sink { [weak self] location in
                self?.monitorRouteProgress(location: location)
            }
            .store(in: &cancellables)
    }
    
    /// Checks distance to next step and advances if close
    private func monitorRouteProgress(location: CLLocationCoordinate2D) {
        guard let route = activeRoute, showNavigationOverlay else { return }
        guard currentStepIndex < route.steps.count else { return }
        
        let step = route.steps[currentStepIndex]
        let stepLoc = CLLocation(latitude: step.polyline.coordinate.latitude, longitude: step.polyline.coordinate.longitude)
        let userLoc = CLLocation(latitude: location.latitude, longitude: location.longitude)
        
        // Thresholds: 30m for driving, 10m for walking
        let threshold = transportMode == .walking ? 15.0 : 30.0
        
        if userLoc.distance(from: stepLoc) < threshold {
            // We reached the step point (approx), advance to next instruction
            if currentStepIndex < route.steps.count - 1 {
                currentStepIndex += 1
                speakCurrentStep()
            } else {
                // Arrived at destination
                speechService.speak("\(LocalizationUtils.string("You have arrived at your destination.")) \(route.destinationName)")
                endNavigation()
            }
        }
    }

    private func setupGroupSubscription() {
        groupRepository.$groupMembers
            .receive(on: DispatchQueue.main)
            .assign(to: \.friends, on: self)
            .store(in: &cancellables)
    }

    func loadPOIs() {
        isLoading = true
        Task {
            do {
                let fetchedDtos = try await APIService.shared.fetchPois()
                let mapped = fetchedDtos.map { Poi(from: $0) }
                self.pois = mapped
                self.filterPOIs(by: self.selectedCategory)
                self.isLoading = false
            } catch {
                Logger.error("[MapViewModel] Failed to fetch POIs: \(error)")
                // Fallback to mock or empty
                let fallbackDtos = [
                    PoiDto(id: "1", name: "Food Court", type: "FOOD", description: "Comida rapida", zone: "A", map_x: 0.55, map_y: 0.52),
                    PoiDto(id: "2", name: "WC Zone A", type: "WC", description: "Servicios", zone: "A", map_x: 0.42, map_y: 0.60)
                ]
                self.pois = fallbackDtos.map { Poi(from: $0) }
                self.filterPOIs(by: self.selectedCategory)
                self.isLoading = false
            }
        }
    }

    func loadBeacons() {
        Task {
            do {
                let fetched = try await APIService.shared.fetchBeacons()
                let mapped = fetched.map { BeaconConfig(from: $0) }
                self.beacons = mapped
            } catch {
                Logger.error("[MapViewModel] Failed to fetch beacons: \(error)")
            }
        }
    }
    
    func filterPOIs(by category: PoiType?) {
        self.selectedCategory = category
        if let category = category {
            filteredPOIs = pois.filter { $0.type == category }
        } else {
            filteredPOIs = pois
        }
    }
    
    // MARK: - Coordinate Mapping
    
    func coordinate(forX mapX: Float, y mapY: Float) -> CLLocationCoordinate2D {
        guard let bounds = circuit?.bounds else { return CLLocationCoordinate2D(latitude: 0, longitude: 0) }
        
        // Lat: Lower Lat is "Bottom". Upper Lat is "Top".
        // In screen coords/Map coords (0-1), Top is 0, Bottom is 1.
        // normalizedY = 1 - (lat - min) / range  ->  lat - min = (1 - normY) * range -> lat = min + (1-normY)*range
        // Wait, previously: normalizedY = 1.0 - ((lat - min) / range)
        // So: (lat - min)/range = 1.0 - normalizedY
        // lat - min = range * (1.0 - normalizedY)
        // lat = min + range * (1.0 - mapY)
        
        let latRange = bounds.maxLat - bounds.minLat
        let lonRange = bounds.maxLon - bounds.minLon
        
        let latitude = bounds.minLat + (latRange * Double(1.0 - mapY))
        let longitude = bounds.minLon + (lonRange * Double(mapX))
        
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
    
    /// Converts a GPS coordinate to a normalized (0-1) point relative to the circuit bounds.
    func normalizeCoordinate(_ coordinate: CLLocationCoordinate2D) -> CGPoint {
        guard let bounds = circuit?.bounds else { return .zero }
        
        let latRange = bounds.maxLat - bounds.minLat
        let lonRange = bounds.maxLon - bounds.minLon
        
        // Safety check dev/zero
        if latRange == 0 || lonRange == 0 { return .zero }
        
        let normalizedX = (coordinate.longitude - bounds.minLon) / lonRange
        let normalizedY = 1.0 - ((coordinate.latitude - bounds.minLat) / latRange)
        
        return CGPoint(x: normalizedX, y: normalizedY)
    }
    
    // MARK: - Navigation
    
    @Published var transportDestination: CLLocationCoordinate2D?
    @Published var transportDestinationName: String?

    /// Calculate route to the circuit
    /// Calculate route to the circuit (Smart Routing to nearest Gate/Parking)
    func calculateRouteToCircuit() {
        guard let origin = userLocation else {
            navigationError = LocalizationUtils.string("Could not get your location")
            return
        }
        var bestDestination: CLLocationCoordinate2D?
        var bestName: String = "Circuit de Barcelona-Catalunya"
        
        let gates = pois.filter { $0.type == .gate }
        if !gates.isEmpty {
            // Find nearest to user
            let userLoc = CLLocation(latitude: origin.latitude, longitude: origin.longitude)
            
            let sorted = gates.sorted { p1, p2 in
                let c1 = coordinate(forX: p1.mapX, y: p1.mapY)
                let c2 = coordinate(forX: p2.mapX, y: p2.mapY)
                let d1 = userLoc.distance(from: CLLocation(latitude: c1.latitude, longitude: c1.longitude))
                let d2 = userLoc.distance(from: CLLocation(latitude: c2.latitude, longitude: c2.longitude))
                return d1 < d2
            }
            
            if let best = sorted.first {
                bestDestination = coordinate(forX: best.mapX, y: best.mapY)
                bestName = best.name
            }
        }
        
        self.transportDestination = bestDestination
        self.transportDestinationName = bestName
        
        // Transit Check - Use Native Public Transport Sheet
        if transportMode == .transit {
            Task { @MainActor in
                self.showTransportSheet = true
                self.isCalculatingRoute = false
            }
            return
        }
        
        let destination = bestDestination
        let name = bestName
        
        Task {
            do {
                let route: NavigationRoute
                if let dest = destination {
                    route = try await navigationService.calculateRoute(
                        from: origin,
                        to: dest,
                        destinationName: name,
                        transportMode: transportMode
                    )
                } else {
                     route = try await navigationService.calculateRouteToCircuit(
                        from: origin,
                        transportMode: transportMode
                    )
                }
                
                await MainActor.run {
                    self.activeRoute = route
                    self.routePolyline = route.route.polyline
                    self.showNavigationOverlay = true
                    self.isCalculatingRoute = false
                    self.currentStepIndex = 0 // Start at first step
                    self.userTrackingMode = .followWithHeading // Waze-style tracking
                    self.zoomToRoute(route.route)
                    self.speakCurrentStep()
                }
            } catch {
                await MainActor.run {
                    // Start navigation error handling
                    self.navigationError = String(format: LocalizationUtils.string("Route calculation error"), error.localizedDescription)
                    self.isCalculatingRoute = false
                }
            }
        }
    }
    
    /// Advance to next step
    func nextStep() {
        guard let route = activeRoute, currentStepIndex < route.steps.count - 1 else { return }
        currentStepIndex += 1
        speakCurrentStep()
    }
    
    /// Go back to previous step
    func prevStep() {
        guard currentStepIndex > 0 else { return }
        currentStepIndex -= 1
        speakCurrentStep()
    }
    
    private func speakCurrentStep() {
        guard let route = activeRoute, currentStepIndex < route.steps.count else { return }
        let step = route.steps[currentStepIndex]
        if !step.instructions.isEmpty {
            speechService.speak(step.instructions)
        }
    }
    
    /// Calculate route to a specific POI
    func calculateRouteToPOI(_ poi: Poi) {
        guard let origin = userLocation else {
            navigationError = LocalizationUtils.string("Could not get your location")
            return
        }
        
        let destination = coordinate(forX: poi.mapX, y: poi.mapY)
        
        // Transit Check - Use Native Public Transport Sheet
        if transportMode == .transit {
            Task {
                await MainActor.run {
                    self.showTransportSheet = true
                    self.isCalculatingRoute = false
                }
            }
            return
        }
        
        isCalculatingRoute = true
        navigationError = nil
        
        Task {
            do {
                let route = try await navigationService.calculateRoute(
                    from: origin,
                    to: destination,
                    destinationName: poi.name,
                    transportMode: transportMode
                )
                
                await MainActor.run {
                    self.activeRoute = route
                    self.routePolyline = route.route.polyline
                    self.showNavigationOverlay = true
                    self.isCalculatingRoute = false
                    self.currentStepIndex = 0
                    self.userTrackingMode = .followWithHeading
                    self.zoomToRoute(route.route)
                    self.speakCurrentStep()
                }
            } catch {
                await MainActor.run {
                    self.navigationError = error.localizedDescription
                    self.isCalculatingRoute = false
                }
            }
        }
    }
    
    /// Change transport mode and recalculate route
    func setTransportMode(_ mode: TransportMode) {
        transportMode = mode
        if activeRoute != nil {
            calculateRouteToCircuit()
        }
    }
    
    /// Clear active navigation
    func endNavigation() {
        activeRoute = nil
        routePolyline = nil
        showNavigationOverlay = false
        activeWebUrl = nil
        showWebNavigation = false
    }
    
    /// Open current route in Apple Maps
    func openInAppleMaps() {
        guard let route = activeRoute else { return }
        navigationService.openInAppleMaps(
            destination: route.destination,
            destinationName: route.destinationName,
            transportMode: route.transportMode
        )
    }
    
    /// Zoom map to show the entire route
    private func zoomToRoute(_ route: MKRoute) {
        let rect = route.polyline.boundingMapRect
        
        withAnimation {
            region = MKCoordinateRegion(rect.insetBy(dx: -rect.width * 0.1, dy: -rect.height * 0.1))
        }
    }
}

// MARK: - Map Items Unified
enum MapItemType {
    case poi(Poi)
    case friend(GroupMember)
    case beacon(BeaconConfig)
}

struct MapAnnotationItem: Identifiable {
    let id: String
    let coordinate: CLLocationCoordinate2D
    let type: MapItemType
}

enum MapUserTrackingMode {
    case none
    case follow
    case followWithHeading
}


