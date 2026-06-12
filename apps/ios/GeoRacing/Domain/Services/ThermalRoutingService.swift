import Foundation
import MapKit

/// Navegación Térmica (Grafo de Sombra / Capa Visual)
/// Provides a layer of "Cool Routes" or "Shadow Zones" across the circuit.
/// This allows users to navigate the circuit while avoiding the heat, complementing MapKit
/// which doesn't know about local shaded areas like tree canopies or covered grandstands.
/// Operates Offline-First by keeping polygonal definitions of shaded zones.
@MainActor
class ThermalRoutingService {
    static let shared = ThermalRoutingService()
    
    private init() {}
    
    /// Returns a list of MKPolygons representing areas with assured shadow on the circuit.
    /// In a real scenario, this could be loaded from a GeoJSON or local database.
    func getShadowPolygons() -> [MKPolygon] {
        return [
            createPolygon(coordinates: [
                CLLocationCoordinate2D(latitude: 41.5702, longitude: 2.2590),
                CLLocationCoordinate2D(latitude: 41.5708, longitude: 2.2595),
                CLLocationCoordinate2D(latitude: 41.5705, longitude: 2.2605),
                CLLocationCoordinate2D(latitude: 41.5699, longitude: 2.2600)
            ], title: "Zona Arboleda Norte"),
            
            createPolygon(coordinates: [
                CLLocationCoordinate2D(latitude: 41.5670, longitude: 2.2580),
                CLLocationCoordinate2D(latitude: 41.5675, longitude: 2.2582),
                CLLocationCoordinate2D(latitude: 41.5673, longitude: 2.2590),
                CLLocationCoordinate2D(latitude: 41.5668, longitude: 2.2588)
            ], title: "Tribuna Cubierta Principal"),
            
            createPolygon(coordinates: [
                CLLocationCoordinate2D(latitude: 41.5720, longitude: 2.2620),
                CLLocationCoordinate2D(latitude: 41.5725, longitude: 2.2625),
                CLLocationCoordinate2D(latitude: 41.5722, longitude: 2.2635),
                CLLocationCoordinate2D(latitude: 41.5717, longitude: 2.2630)
            ], title: "Paseo Sombrío Este")
        ]
    }
    
    private func createPolygon(coordinates: [CLLocationCoordinate2D], title: String) -> MKPolygon {
        let polygon = MKPolygon(coordinates: coordinates, count: coordinates.count)
        polygon.title = title
        return polygon
    }
}
