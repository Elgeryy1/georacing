import MapKit
import CoreLocation

extension MKMapItem {
    /// Creates an MKMapItem from a coordinate.
    ///
    /// Centralizes MKPlacemark usage for easy migration
    /// when targeting iOS 26+ (`MKMapItem(location:address:)`).
    static func fromCoordinate(_ coordinate: CLLocationCoordinate2D) -> MKMapItem {
        MKMapItem(
            location: CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude),
            address: nil
        )
    }
}
