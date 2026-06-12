import Foundation
import CoreLocation

// MARK: - API Client

class TransportAPIClient {
    static let shared = TransportAPIClient()
    
    // In production, this would be configurable. For now, localhost for simulator needs careful handling.
    // If running on simulator, localhost refers to the Mac.
    private let baseURL = "http://localhost:3000/v1/transport"
    
    func planTrip(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) async throws -> TransportPlanResponse {
        var components = URLComponents(string: "\(baseURL)/plan")!
        components.queryItems = [
            URLQueryItem(name: "fromLat", value: "\(from.latitude)"),
            URLQueryItem(name: "fromLon", value: "\(from.longitude)"),
            URLQueryItem(name: "toLat", value: "\(to.latitude)"),
            URLQueryItem(name: "toLon", value: "\(to.longitude)"),
            URLQueryItem(name: "arriveBy", value: "false")
        ]
        
        guard let url = components.url else {
            throw URLError(.badURL)
        }
        
        let (data, response) = try await URLSession.shared.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        
        let decoder = JSONDecoder()
        return try decoder.decode(TransportPlanResponse.self, from: data)
    }
    
    func checkHealth() async -> Bool {
        guard let url = URL(string: "\(baseURL)/health") else { return false }
        do {
            let (_, response) = try await URLSession.shared.data(from: url)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
    }
}

// MARK: - Models

struct TransportPlanResponse: Codable {
    let itineraries: [Itinerary]
}

struct Itinerary: Codable, Identifiable {
    let id = UUID()
    let duration: Int // Seconds
    let startTime: Int // Epoch millis
    let endTime: Int // Epoch millis
    let walkTime: Int
    let transitTime: Int
    let legs: [Leg]
    
    private enum CodingKeys: String, CodingKey {
        case duration, startTime, endTime, walkTime, transitTime, legs
    }
}

struct Leg: Codable, Identifiable {
    let id = UUID()
    let mode: String
    let route: String?
    let routeColor: String?
    let routeShortName: String?
    let routeLongName: String?
    let from: Place
    let to: Place
    let realTime: Bool?
    let distance: Double?
    let legGeometry: String? // Encoded polyline usually, but our API returns points string? Needs verification.
    
    private enum CodingKeys: String, CodingKey {
        case mode, route, routeColor, routeShortName, routeLongName, from, to, realTime, distance, legGeometry
    }
    
    var duration: Int {
        guard let start = from.departureTime, let end = to.arrivalTime else { return 0 }
        return (end - start) / 1000
    }
}

struct Place: Codable {
    let name: String
    let lat: Double
    let lon: Double
    let departureTime: Int?
    let arrivalTime: Int?
}
