import Foundation

struct AppConstants {
    static let osrmBaseUrl = "https://router.project-osrm.org"
    static let apiBaseUrl = "https://alpo.myqnapcloud.com:4010/api"
    
    static let offRouteDistanceThreshold: Double = 50.0 // meters
    static let offRouteTimeThreshold: Double = 10.0 // seconds
    static let locationUpdateInterval: Double = 1.0 // seconds
}
