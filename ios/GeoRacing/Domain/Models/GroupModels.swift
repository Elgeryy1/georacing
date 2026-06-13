import Foundation
import CoreLocation

struct Group: Identifiable, Codable, Sendable {
    let id: String
    let name: String
    let ownerId: String
    let members: [String] // User IDs
}

struct GroupMember: Identifiable, Codable, Sendable {
    let id: String // User ID
    let displayName: String
    let coordinate: CLLocationCoordinate2D?
    let isSharing: Bool
    
    // Custom coding for Coordinate
    enum CodingKeys: String, CodingKey {
        case id, displayName, latitude, longitude, isSharing
    }
    
    public init(id: String, displayName: String, coordinate: CLLocationCoordinate2D?, isSharing: Bool) {
        self.id = id
        self.displayName = displayName
        self.coordinate = coordinate
        self.isSharing = isSharing
    }
    
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        displayName = try container.decode(String.self, forKey: .displayName)
        isSharing = try container.decode(Bool.self, forKey: .isSharing)
        
        if let lat = try? container.decode(Double.self, forKey: .latitude),
           let lon = try? container.decode(Double.self, forKey: .longitude) {
            coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
        } else {
            coordinate = nil
        }
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(displayName, forKey: .displayName)
        try container.encode(isSharing, forKey: .isSharing)
        if let coord = coordinate {
            try container.encode(coord.latitude, forKey: .latitude)
            try container.encode(coord.longitude, forKey: .longitude)
        }
    }
}

// ShareSession is defined in SocialModels.swift
