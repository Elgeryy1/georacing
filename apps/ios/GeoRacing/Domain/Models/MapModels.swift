import Foundation
import CoreLocation

public enum PoiType: String, Codable, CaseIterable, Sendable {
    case wc = "WC"
    case food = "FOOD"
    case parking = "PARKING"
    case grandstand = "GRANDSTAND"
    case medical = "MEDICAL"
    case merch = "MERCH"
    case access = "ACCESS"
    case exit = "EXIT"
    case gate = "GATE"
    case fanzone = "FANZONE"
    case service = "SERVICE"
    case other = "OTHER"
}

public struct Poi: Identifiable, Codable, Sendable {
    public let id: String
    public let name: String
    public let type: PoiType
    public let description: String?
    public let zone: String?
    public let mapX: Float
    public let mapY: Float
    
    // Optional: for cases where we have lat/lon logic, but mapX/Y is primary for image overlay
    public var coordinate: CLLocationCoordinate2D? = nil // Not Codable by default
    
    enum CodingKeys: String, CodingKey {
        case id, name, type, description, zone
        case mapX = "map_x"
        case mapY = "map_y"
    }
    
    // Helper init for mapping from DTO
    init(from dto: PoiDto) {
        self.id = dto.id
        self.name = dto.name
        self.type = PoiType(rawValue: dto.type) ?? .other
        self.description = dto.description
        self.zone = dto.zone
        self.mapX = Float(dto.map_x)
        self.mapY = Float(dto.map_y)
    }
}

public struct BeaconConfig: Identifiable, Codable, Sendable {
    public let id: String
    public let uuid: String
    public let major: Int
    public let minor: Int
    public let name: String
    public let mapX: Float
    public let mapY: Float
    
    enum CodingKeys: String, CodingKey {
        case id, uuid, major, minor, name
        case mapX = "map_x"
        case mapY = "map_y"
    }
    
    init(from dto: BeaconDto) {
        self.id = dto.id
        self.uuid = dto.uuid
        self.major = dto.major
        self.minor = dto.minor
        self.name = dto.name
        self.mapX = Float(dto.map_x)
        self.mapY = Float(dto.map_y)
    }
}
