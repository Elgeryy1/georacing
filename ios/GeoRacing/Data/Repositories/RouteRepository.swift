import Foundation
import CoreLocation
import Combine

class RouteRepository {
    
    static let shared = RouteRepository()
    
    private let baseURL = AppConstants.osrmBaseUrl
    private let session = URLSession.shared
    
    private init() {}
    
    /// Fetches a route from Origin to Destination using OSRM
    /// Android Spec 1.1: /route/v1/driving/{lon},{lat};{lon},{lat}?overview=full&steps=true
    func fetchRoute(from origin: CLLocationCoordinate2D, to destination: CLLocationCoordinate2D) async throws -> RouteResult {
        
        // Format URL: OSRM uses {lon},{lat}
        let coordinates = "\(origin.longitude),\(origin.latitude);\(destination.longitude),\(destination.latitude)"
        
        // Ensure we request polyline6 if needed by spec (Android doc says Polyline6), otherwise standard is 5.
        // We will request geometries=polyline6 explicitly to be safe and match PolylineUtils default
        let queryItems = [
            URLQueryItem(name: "overview", value: "full"),
            URLQueryItem(name: "steps", value: "true"),
            URLQueryItem(name: "geometries", value: "polyline6")
        ]
        
        var urlComps = URLComponents(string: "\(baseURL)/route/v1/driving/\(coordinates)")
        urlComps?.queryItems = queryItems
        
        guard let url = urlComps?.url else {
            throw URLError(.badURL)
        }
        
        Logger.debug("[RouteRepo] Requesting Route: \(url.absoluteString)")
        
        let (data, response) = try await session.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            Logger.error("[RouteRepo][ERROR] HTTP Error: \((response as? HTTPURLResponse)?.statusCode ?? -1)")
            throw URLError(.badServerResponse)
        }
        
        // Decode OSRM Response
        let osrmResponse = try JSONDecoder().decode(OSRMResponse.self, from: data)
        
        guard let primaryRoute = osrmResponse.routes.first else {
            throw URLError(.cannotParseResponse)
        }
        
        return try mapToDomain(osrmRoute: primaryRoute)
    }
    
    private func mapToDomain(osrmRoute: OSRMRoute) throws -> RouteResult {
        // 1. Decode Geometry (Polyline6)
        // Android Spec 1.1.3: Decodificación de Polilínea
        let geometry = PolylineUtils.decode(osrmRoute.geometry, precision: 1e6)
        
        // 2. Extract Steps
        // Android Spec 1.1.4: Extracción de Pasos (Steps)
        var steps: [RouteStep] = []
        
        // OSRM usually has one 'leg' for point A to B
        if let leg = osrmRoute.legs.first {
            steps = leg.steps.map { osrmStep in
                // Each step also has geometry, we can decode if needed for turn-by-turn highlighting,
                // but essential for instruction list.
                // Required fields: Type, Modifier, Name, Distance.
                
                return RouteStep(
                    id: UUID(),
                    instruction: buildInstruction(step: osrmStep),
                    distance: osrmStep.distance,
                    duration: osrmStep.duration,
                    maneuverType: osrmStep.maneuver.type,
                    maneuverModifier: osrmStep.maneuver.modifier
                )
            }
        }
        
        return RouteResult(
            geometry: geometry,
            duration: osrmRoute.duration,
            distance: osrmRoute.distance,
            steps: steps
        )
    }
    
    private func buildInstruction(step: OSRMStep) -> String {
        // Simple formatter. Real implementations often use OSRM Text Instructions library.
        // Android Spec 1.4: mentions "Gira a la derecha", etc.
        // We do a basic mapping here for parity.
        
        let type = step.maneuver.type
        let modifier = step.maneuver.modifier ?? ""
        let name = step.name.isEmpty ? "" : "en \(step.name)"
        
        switch type {
        case "depart": return "Salida"
        case "arrive": return "Has llegado a tu destino"
        case "turn":
            if modifier.contains("left") { return "Gira a la izquierda \(name)" }
            if modifier.contains("right") { return "Gira a la derecha \(name)" }
            return "Gira \(name)"
        case "roundabout":
            return "En la rotonda, toma la salida \(step.maneuver.modifier ?? "")"
        default:
            return "\(type.capitalized) \(modifier) \(name)"
        }
    }
}
