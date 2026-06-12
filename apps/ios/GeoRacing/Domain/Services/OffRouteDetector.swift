import Foundation

class OffRouteDetector {
    
    private var offRouteStartTime: Date?
    
    /// Checks if the user is off-route based on the snap result.
    ///
    /// - Parameters:
    ///   - snapResult: The result from the RouteSnapper.
    /// - Returns: True if confirmed off-route, False otherwise.
    func isUserOffRoute(snapResult: SnapResult) -> Bool {
        // 1. Evaluate distance threshold
        if snapResult.distanceToRoute > AppConstants.offRouteDistanceThreshold {
            // Case YES
            if let startTime = offRouteStartTime {
                // If we already have a timestamp, check if time threshold exceeded
                let elapsed = Date().timeIntervalSince(startTime)
                if elapsed > AppConstants.offRouteTimeThreshold {
                    return true // Deviation confirmed
                } else {
                    return false // Waiting for confirmation
                }
            } else {
                // First time detected, save timestamp
                offRouteStartTime = Date()
                return false
            }
        } else {
            // Case NO: Reset timer
            offRouteStartTime = nil
            return false
        }
    }
    
    func reset() {
        offRouteStartTime = nil
    }
}
