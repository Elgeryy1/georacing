import Foundation
import SwiftUI

public enum TrackStatus: String, Sendable, CaseIterable {
    case green
    case yellow
    case red
    case sc // Safety Car
    case vsc // Virtual Safety Car
    case evacuation // Emergency Evacuation
    case unknown // Fallback for unmapped states
    
    var color: Color {
        switch self {
        case .green: return .green
        case .yellow: return .yellow
        case .red: return RacingColors.red
        case .sc, .vsc: return .orange
        case .evacuation: return RacingColors.red // Flash or distinct red?
        case .unknown: return .gray
        }
    }
    
    var iconName: String {
        // User requested standard iOS icons (SF Symbols)
        switch self {
        case .green, .yellow, .red: return "flag.fill"
        case .sc, .vsc: return "exclamationmark.triangle.fill"
        case .evacuation: return "exclamationmark.shield.fill"
        case .unknown: return "questionmark.circle.fill"
        }
    }
    
    var titleKey: String {
        switch self {
        case .green: return "TRACK CLEAR"
        case .yellow: return "YELLOW FLAG"
        case .red: return "RED FLAG"
        case .sc: return "SAFETY CAR"
        case .vsc: return "VIRTUAL SC"
        case .evacuation: return "EVACUATION"
        case .unknown: return "UNKNOWN STATE"
        }
    }
    
    var messageKey: String {
        switch self {
        case .green: return "Track is Green. Racing resumes."
        case .yellow: return "Hazard reported. Slow down."
        case .red: return "Session Suspended. Return to pits."
        case .sc, .vsc: return "Safety Car deployed."
        case .evacuation: return "EMERGENCY: EVACUATE CIRCUIT IMMEDIATELY."
        case .unknown: return "Waiting for race control..."
        }
    }
}
