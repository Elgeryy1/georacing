import Foundation
import CoreLocation

public struct MapBounds: Sendable {
    public let minLat: Double
    public let maxLat: Double
    public let minLon: Double
    public let maxLon: Double
    
    public init(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        self.minLat = minLat
        self.maxLat = maxLat
        self.minLon = minLon
        self.maxLon = maxLon
    }
    
    public var center: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: (minLat + maxLat) / 2, longitude: (minLon + maxLon) / 2)
    }
}

public struct Circuit: Sendable {
    public let name: String
    public let bounds: MapBounds
    public let imageAssetName: String // e.g. "circuit_map"
    
    public init(name: String, bounds: MapBounds, imageAssetName: String) {
        self.name = name
        self.bounds = bounds
        self.imageAssetName = imageAssetName
    }
    
    // Default circuit configuration (Barcelona-Catalunya as placeholder/defaults)
    public static let montmelo = Circuit(
        name: "Circuit de Barcelona-Catalunya",
        bounds: MapBounds(
            minLat: 41.565,
            maxLat: 41.575,
            minLon: 2.250,
            maxLon: 2.265
        ),
        imageAssetName: "circuit_overlay"
    )
}
