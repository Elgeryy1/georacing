import Foundation
import MapKit
import CoreLocation

/// Transport modes for navigation
enum TransportMode: String, CaseIterable {
    case automobile = "car"
    case walking = "walk"
    case transit = "transit"
    
    var icon: String {
        switch self {
        case .automobile: return "car.fill"
        case .walking: return "figure.walk"
        case .transit: return "bus.fill"
        }
    }
    
    var title: String {
        switch self {
        case .automobile: return "Car"
        case .walking: return "On foot"
        case .transit: return "Transit"
        }
    }
    
    @MainActor
    var localizedTitle: String {
        LocalizationUtils.string(title)
    }
    
    var mkDirectionsTransportType: MKDirectionsTransportType {
        switch self {
        case .automobile: return .automobile
        case .walking: return .walking
        case .transit: return .transit
        }
    }
}

/// Route information wrapper
struct NavigationRoute {
    let route: MKRoute
    let destination: CLLocationCoordinate2D
    let destinationName: String
    let transportMode: TransportMode
    
    var distance: CLLocationDistance { route.distance }
    var expectedTravelTime: TimeInterval { route.expectedTravelTime }
    var steps: [MKRoute.Step] { route.steps }
    
    var formattedDistance: String {
        if distance >= 1000 {
            return String(format: "%.1f km", distance / 1000)
        } else {
            return String(format: "%.0f m", distance)
        }
    }
    
    var formattedETA: String {
        let hours = Int(expectedTravelTime) / 3600
        let minutes = (Int(expectedTravelTime) % 3600) / 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)min"
        } else {
            return "\(minutes) min"
        }
    }
}

/// Service for calculating navigation routes using MapKit
class NavigationService {
    
    static let shared = NavigationService()
    
    private init() {}
    
    // MARK: - Circuit Destination
    
    /// Circuit de Barcelona-Catalunya main entrance
    static let circuitCoordinate = CLLocationCoordinate2D(latitude: 41.5700, longitude: 2.2611)
    static let circuitName = "Circuit de Barcelona-Catalunya"
    
    // MARK: - Route Calculation
    
    /// Calculate route from user's current location to destination
    func calculateRoute(
        from origin: CLLocationCoordinate2D,
        to destination: CLLocationCoordinate2D,
        destinationName: String,
        transportMode: TransportMode
    ) async throws -> NavigationRoute {
        
        let request = MKDirections.Request()
        request.source = MKMapItem.fromCoordinate(origin)
        request.destination = MKMapItem.fromCoordinate(destination)
        request.transportType = transportMode.mkDirectionsTransportType
        request.requestsAlternateRoutes = false
        
        let directions = MKDirections(request: request)
        let response = try await directions.calculate()
        
        guard let route = response.routes.first else {
            throw NavigationError.noRouteFound
        }
        
        return NavigationRoute(
            route: route,
            destination: destination,
            destinationName: destinationName,
            transportMode: transportMode
        )
    }
    
    /// Calculate route to the circuit
    func calculateRouteToCircuit(
        from origin: CLLocationCoordinate2D,
        transportMode: TransportMode
    ) async throws -> NavigationRoute {
        try await calculateRoute(
            from: origin,
            to: Self.circuitCoordinate,
            destinationName: Self.circuitName,
            transportMode: transportMode
        )
    }
    
    // MARK: - Open in Apple Maps
    
    /// Open Apple Maps with directions to destination
    func openInAppleMaps(
        destination: CLLocationCoordinate2D,
        destinationName: String,
        transportMode: TransportMode
    ) {
        let destinationItem = MKMapItem.fromCoordinate(destination)
        destinationItem.name = destinationName
        
        let launchOptions: [String: Any] = [
            MKLaunchOptionsDirectionsModeKey: transportMode.appleMapsDirectionsMode
        ]
        
        destinationItem.openInMaps(launchOptions: launchOptions)
    }
    
    /// Open Apple Maps with directions to the circuit
    func openCircuitInAppleMaps(transportMode: TransportMode) {
        openInAppleMaps(
            destination: Self.circuitCoordinate,
            destinationName: Self.circuitName,
            transportMode: transportMode
        )
    }
}

// MARK: - Transport Mode Apple Maps Extension

extension TransportMode {
    var appleMapsDirectionsMode: String {
        switch self {
        case .automobile: return MKLaunchOptionsDirectionsModeDriving
        case .walking: return MKLaunchOptionsDirectionsModeWalking
        case .transit: return MKLaunchOptionsDirectionsModeTransit
        }
    }
}

// MARK: - Errors

enum NavigationError: LocalizedError {
    case noRouteFound
    case locationNotAvailable
    
    var errorDescription: String? {
        switch self {
        case .noRouteFound:
            return "No se encontró una ruta disponible"
        case .locationNotAvailable:
            return "No se pudo obtener tu ubicación"
        }
    }
}
