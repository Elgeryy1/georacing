import Foundation
import CoreLocation
import Combine

@MainActor
class PublicTransportViewModel: ObservableObject {
    @Published var itineraries: [Itinerary] = []
    @Published var isLoading = false
    @Published var error: String? = nil
    @Published var showFallback = false
    
    private let apiClient = TransportAPIClient.shared
    
    @Published var selectedModeFilter: String = "ALL" // ALL, BUS, RAIL
    
    // Circuit Coordinates (Default fallback)
    private let defaultCircuitLocation = CLLocationCoordinate2D(latitude: 41.570, longitude: 2.260)
    
    private var allItineraries: [Itinerary] = []
    
    func loadRoutes(from userLocation: CLLocationCoordinate2D?, to destination: CLLocationCoordinate2D? = nil) {
        guard let from = userLocation else {
            self.error = "Ubicación desconocida"
            return
        }
        
        let toLocation = destination ?? defaultCircuitLocation
        
        Task { @MainActor in
            isLoading = true
            error = nil
            showFallback = false
        }
        
        Task {
            do {
                // First check health (optional, but good for UX to fail fast)
                let isHealthy = await apiClient.checkHealth()
                if !isHealthy {
                    Logger.warning("[PublicTransportVM] Backend not reachable, using mock.")
                    await MainActor.run {
                        self.allItineraries = self.mockItineraries(userLocation: from, destination: toLocation)
                        self.filterItineraries()
                        self.isLoading = false
                    }
                    return
                }
                
                let response = try await apiClient.planTrip(from: from, to: toLocation)
                await MainActor.run {
                    self.allItineraries = response.itineraries
                    self.filterItineraries()
                    self.isLoading = false
                    if self.itineraries.isEmpty {
                        self.error = LocalizationUtils.string("No routes found.")
                        self.showFallback = true
                    }
                }
            } catch {
                await MainActor.run {
                    self.error = String(format: LocalizationUtils.string("Connection error"), error.localizedDescription)
                    self.isLoading = false
                    self.showFallback = true
                    
                    // Fallback to Intelligent Local Route
                    self.allItineraries = self.mockItineraries(userLocation: from, destination: toLocation)
                    self.filterItineraries()
                }
            }
        }
    }
    
    func setFilter(_ mode: String) {
        selectedModeFilter = mode
        filterItineraries()
    }
    
    private func filterItineraries() {
        if selectedModeFilter == "ALL" {
            itineraries = allItineraries
        } else {
            itineraries = allItineraries.filter { itinerary in
                itinerary.legs.contains { $0.mode == selectedModeFilter }
            }
        }
    }
    
    // Mock/Fallback Data using Intelligent Routing
    private func mockItineraries(userLocation: CLLocationCoordinate2D? = nil, destination: CLLocationCoordinate2D? = nil) -> [Itinerary] {
        guard let userLoc = userLocation, let dest = destination else {
            // Default static if no coords (shouldn't happen in flow)
            return []
        }
        
        // 1. Calculate Intelligent Route via R2N (Standard F1 Route)
        let route = TransportLocalFallback.shared.generateFallbackItinerary(from: userLoc, to: dest)
        
        // 2. Add an alternative (e.g. Bus or just Walk if close? For now just one good one)
        // Check if user is very close (Walk only)
        let distance = CLLocation(latitude: userLoc.latitude, longitude: userLoc.longitude)
            .distance(from: CLLocation(latitude: dest.latitude, longitude: dest.longitude))
            
        if distance < 3000 {
            // If < 3km, suggest walking direct
            let walkTime = Int(distance / 1.2)
            let now = Int(Date().timeIntervalSince1970 * 1000)
            let walkLeg = Leg(mode: "WALK", route: nil, routeColor: nil, routeShortName: nil, routeLongName: nil,
                              from: Place(name: "Tu Ubicación", lat: userLoc.latitude, lon: userLoc.longitude, departureTime: nil, arrivalTime: nil),
                              to: Place(name: "Circuit", lat: dest.latitude, lon: dest.longitude, departureTime: nil, arrivalTime: nil),
                              realTime: false, distance: distance, legGeometry: nil)
                              
            let walkItinerary = Itinerary(duration: walkTime, startTime: now, endTime: now + walkTime*1000, walkTime: walkTime, transitTime: 0, legs: [walkLeg])
            return [walkItinerary]
        }
        
        return [route]
    }
}
