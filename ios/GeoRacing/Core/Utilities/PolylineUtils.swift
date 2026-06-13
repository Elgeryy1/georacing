import Foundation
import CoreLocation

struct PolylineUtils {
    
    /// Decodes a polyline string into an array of coordinates.
    /// - Parameters:
    ///   - polyline: The encoded polyline string.
    ///   - precision: The precision of the encoding (e.g., 1e5 for 5 digits, 1e6 for 6 digits).
    /// - Returns: Array of CLLocationCoordinate2D.
    static func decode(_ polyline: String, precision: Double = 1e6) -> [CLLocationCoordinate2D] {
        var coordinates: [CLLocationCoordinate2D] = []
        var index = polyline.startIndex
        
        var lat = 0
        var lng = 0
        
        while index < polyline.endIndex {
            var b: Int
            var shift = 0
            var result = 0
            
            repeat {
                if index >= polyline.endIndex { break }
                let char = polyline[index]
                b = Int(char.asciiValue! - 63)
                index = polyline.index(after: index)
                result |= (b & 0x1f) << shift
                shift += 5
            } while b >= 0x20
            
            let dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1))
            lat += dLat
            
            shift = 0
            result = 0
            
            repeat {
                if index >= polyline.endIndex { break }
                let char = polyline[index]
                b = Int(char.asciiValue! - 63)
                index = polyline.index(after: index)
                result |= (b & 0x1f) << shift
                shift += 5
            } while b >= 0x20
            
            let dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1))
            lng += dLng
            
            let latitude = Double(lat) / precision
            let longitude = Double(lng) / precision
            
            coordinates.append(CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
        }
        
        return coordinates
    }
}
