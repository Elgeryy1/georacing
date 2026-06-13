import Foundation
import SwiftUI

enum RacePhase: String, Codable {
    case pre = "PRE"
    case formation = "FORMATION"
    case live = "LIVE"
    case safetyCar = "SAFETY_CAR"
    case redFlag = "RED_FLAG"
    case post = "POST"
}

enum CircuitMode: String, Codable {
    case normal = "NORMAL"
    case congestion = "CONGESTION"
    case emergency = "EMERGENCY"
    case evacuation = "EVACUATION"
    case maintenance = "MAINTENANCE"
}

enum DataHealth: String, Codable {
    case ok = "OK"
    case degraded = "DEGRADED"
    case offline = "OFFLINE"
}

enum UserRole: String, Codable {
    case fan = "FAN"
    case staff = "STAFF"
}

enum UserFocus: String, Codable {
    case seat = "SEAT"
    case route = "ROUTE"
    case parking = "PARKING"
    case incidentNearby = "INCIDENT_NEARBY"
    case none = "NONE"
}

struct ContextState: Equatable {
    let racePhase: RacePhase
    let circuitMode: CircuitMode
    let dataHealth: DataHealth
    let userRole: UserRole
    let focus: UserFocus
    let accessibilityEnabled: Bool
    let lastUpdated: Date
    let routeSuggestion: RouteSuggestion?
    
    // Default initial state
    static var initial: ContextState {
        ContextState(
            racePhase: .pre,
            circuitMode: .normal,
            dataHealth: .ok,
            userRole: .fan,
            focus: .none,
            accessibilityEnabled: UIAccessibility.isVoiceOverRunning,
            lastUpdated: Date(),
            routeSuggestion: nil
        )
    }
}
