import Foundation
import CoreLocation
import Combine

/// Manages real-time GPS location, heading, and speed for navigation.
/// Uses high-accuracy updates suitable for turn-by-turn guidance.
class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = LocationManager()
    
    private let manager = CLLocationManager()
    
    // MARK: - Published State
    
    /// Current user coordinate (updated every ~5 m)
    @Published var location: CLLocationCoordinate2D?
    
    /// Full CLLocation with altitude, speed, course, accuracy
    @Published var clLocation: CLLocation?
    
    /// Device heading in degrees (0-360, relative to true north)
    @Published var heading: CLLocationDirection = 0
    
    /// Current speed in m/s (-1 if unavailable)
    @Published var speed: CLLocationSpeed = -1
    
    /// Course (direction of travel) in degrees
    @Published var course: CLLocationDirection = -1
    
    /// Authorization status
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    
    /// Human-readable GPS state
    @Published var gpsState: GPSState = .searching
    
    enum GPSState: Equatable {
        case unauthorized
        case searching
        case lowAccuracy
        case active
        case error(String)
    }
    
    // MARK: - Init
    
    override private init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        manager.distanceFilter = 5 // Update every 5 meters
        manager.activityType = .automotiveNavigation
        manager.allowsBackgroundLocationUpdates = false // Enable if Info.plist has bg modes
        manager.pausesLocationUpdatesAutomatically = false
        manager.requestWhenInUseAuthorization()
    }
    
    // MARK: - Control
    
    /// Start GPS updates (called automatically on authorization)
    func startUpdating() {
        manager.startUpdatingLocation()
        manager.startUpdatingHeading()
    }
    
    /// Stop GPS updates (battery savings)
    func stopUpdating() {
        manager.stopUpdatingLocation()
        manager.stopUpdatingHeading()
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        switch authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            gpsState = .searching
            startUpdating()
        case .denied, .restricted:
            gpsState = .unauthorized
        default:
            gpsState = .searching
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        
        // Filter out stale / inaccurate readings
        let age = -loc.timestamp.timeIntervalSinceNow
        guard age < 10, loc.horizontalAccuracy >= 0 else { return }
        
        self.clLocation = loc
        self.location = loc.coordinate
        self.speed = loc.speed
        self.course = loc.course
        
        // Update GPS state based on accuracy
        if loc.horizontalAccuracy > 100 {
            gpsState = .lowAccuracy
        } else {
            gpsState = .active
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        if newHeading.trueHeading >= 0 {
            heading = newHeading.trueHeading
        } else {
            heading = newHeading.magneticHeading
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Logger.error("[LocationManager] Error: \(error)")
        if let clError = error as? CLError, clError.code == .denied {
            gpsState = .unauthorized
        } else {
            gpsState = .error(error.localizedDescription)
        }
    }
}
