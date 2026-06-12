import Foundation
import CoreLocation

// Domain Models

public struct RouteResult: Codable, Identifiable {
    public let id: UUID
    public let geometry: [CLLocationCoordinate2D]
    public let duration: TimeInterval
    public let distance: Double
    public let steps: [RouteStep]
    
    public init(id: UUID = UUID(), geometry: [CLLocationCoordinate2D], duration: TimeInterval, distance: Double, steps: [RouteStep]) {
        self.id = id
        self.geometry = geometry
        self.duration = duration
        self.distance = distance
        self.steps = steps
    }

    enum CodingKeys: String, CodingKey {
        case id, geometry, duration, distance, steps
    }
    
    // Explicitly decode geometry as [[Double]] and convert to [CLLocationCoordinate2D]
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decodeIfPresent(UUID.self, forKey: .id) ?? UUID()
        duration = try container.decode(TimeInterval.self, forKey: .duration)
        distance = try container.decode(Double.self, forKey: .distance)
        steps = try container.decode([RouteStep].self, forKey: .steps)
        
        // Decode geometry
        let coords = try container.decode([[Double]].self, forKey: .geometry)
        geometry = coords.compactMap { pair in
            guard pair.count == 2 else { return nil }
            return CLLocationCoordinate2D(latitude: pair[0], longitude: pair[1])
        }
    }
    
    // Explicitly encode geometry as [[Double]]
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(duration, forKey: .duration)
        try container.encode(distance, forKey: .distance)
        try container.encode(steps, forKey: .steps)
        
        let coords = geometry.map { [$0.latitude, $0.longitude] }
        try container.encode(coords, forKey: .geometry)
    }
}

public struct RouteStep: Codable, Identifiable {
    public var id = UUID()
    public let instruction: String
    public let distance: Double
    public let duration: TimeInterval
    public let maneuverType: String
    public let maneuverModifier: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case instruction
        case distance
        case duration
        case maneuverType = "maneuver_type"
        case maneuverModifier = "maneuver_modifier"
    }
    
    // Simpler fix:
    // var id = UUID()

}

public struct SnapResult {
    public let snappedLocation: CLLocationCoordinate2D
    public let routeIndex: Int
    public let distanceToRoute: Double // In meters
    public let isSuccessful: Bool
    
    public init(snappedLocation: CLLocationCoordinate2D, routeIndex: Int, distanceToRoute: Double, isSuccessful: Bool) {
        self.snappedLocation = snappedLocation
        self.routeIndex = routeIndex
        self.distanceToRoute = distanceToRoute
        self.isSuccessful = isSuccessful
    }
}

// OSRM DTOs (Data Transfer Objects) - Internal to Data Layer usually, 
// but putting here for visibility in this "Models" file for now or separate file.

struct OSRMResponse: Codable {
    let routes: [OSRMRoute]
    let code: String
}

struct OSRMRoute: Codable {
    let geometry: String // Polyline6 string
    let legs: [OSRMLeg]
    let distance: Double
    let duration: Double
}

struct OSRMLeg: Codable {
    let steps: [OSRMStep]
    let distance: Double
    let duration: Double
}

struct OSRMStep: Codable {
    let geometry: String // Polyline6 for this step
    let maneuver: OSRMManeuver
    let distance: Double
    let duration: Double
    let name: String
}

struct OSRMManeuver: Codable {
    let type: String
    let modifier: String?
    let location: [Double] // [lon, lat]
}
