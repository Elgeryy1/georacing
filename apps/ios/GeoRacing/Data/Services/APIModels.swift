import Foundation

struct CircuitStateDto: Codable, Sendable {
    let flag: String
    let message: String?
    
    enum CodingKeys: String, CodingKey {
        case flag = "global_mode" // Critical: Map backend 'global_mode' to our 'flag' property
        case message
    }
}

struct PoiDto: Codable, Identifiable, Sendable {
    let id: String
    let name: String
    let type: String
    let description: String?
    let zone: String?
    let map_x: Double
    let map_y: Double
}

struct BeaconDto: Codable, Identifiable, Sendable {
    let id: String
    let uuid: String
    let major: Int
    let minor: Int
    let name: String
    let map_x: Double
    let map_y: Double
}

struct IncidentReportDto: Codable, Sendable {
    let category: String
    let description: String
    let beacon_id: String?
    let zone: String?
    let timestamp: Int64
}

struct GroupLocationRequest: Codable, Sendable {
    let user_uuid: String
    let group_name: String
    let lat: Double
    let lon: Double
    let displayName: String
}

struct GroupMemberDto: Codable, Sendable {
    let user_uuid: String
    let displayName: String?
    let lat: Double
    let lon: Double
    let timestamp: Int64?
}

struct ZoneDensityDto: Codable, Identifiable, Sendable {
    var id: String { zone_id }
    let zone_id: String
    let density_level: String // LOW, MEDIUM, HIGH, CRITICAL
    let estimated_wait_minutes: Int
    let trend: String // RISING, FALLING, STABLE
}

