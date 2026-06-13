import Foundation
import CoreLocation

class RouteSnapper {
    
    /// Finds the closest point on the route to the current user location.
    /// Utilizes a sliding window optimization based on the last known index.
    ///
    /// - Parameters:
    ///   - currentLocation: The current GPS reading.
    ///   - routePoints: The full list of route coordinates.
    ///   - lastKnownIndex: The index of the last snapped point (for optimization).
    /// - Returns: A SnapResult containing the snapped location and index.
    func snapToRoute(currentLocation: CLLocationCoordinate2D, routePoints: [CLLocationCoordinate2D], lastKnownIndex: Int?) -> SnapResult {
        guard !routePoints.isEmpty else {
            return SnapResult(snappedLocation: currentLocation, routeIndex: -1, distanceToRoute: Double.infinity, isSuccessful: false)
        }
        
        let searchWindowSize = 50
        var startIndex = 0
        var endIndex = routePoints.count - 1
        
        // Window Optimization
        if let lastIndex = lastKnownIndex {
            startIndex = max(0, lastIndex - searchWindowSize)
            endIndex = min(routePoints.count - 1, lastIndex + searchWindowSize)
        }
        
        var closestPoint = routePoints[startIndex]
        var closestIndex = startIndex
        var minDistance = Double.infinity
        
        for i in startIndex...endIndex {
            let point = routePoints[i]
            let distance = currentLocation.distance(to: point)
            
            if distance < minDistance {
                minDistance = distance
                closestPoint = point
                closestIndex = i
            }
        }
        
        let threshold = AppConstants.offRouteDistanceThreshold
        let isSuccessful = minDistance <= threshold
        
        return SnapResult(
            snappedLocation: closestPoint,
            routeIndex: closestIndex,
            distanceToRoute: minDistance,
            isSuccessful: isSuccessful
        )
    }
}

fileprivate extension CLLocationCoordinate2D {
    /// Calculates distance in meters between two coordinates.
    func distance(to other: CLLocationCoordinate2D) -> Double {
        let loc1 = CLLocation(latitude: self.latitude, longitude: self.longitude)
        let loc2 = CLLocation(latitude: other.latitude, longitude: other.longitude)
        return loc1.distance(from: loc2)
    }
}
