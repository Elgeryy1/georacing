import Foundation

enum MapTheme {
    case light
    case dark
}

protocol MapStyleManagerProtocol {
    var currentTheme: MapTheme { get }
    func shouldSwitchTheme(isNightModeSensor: Bool) -> MapTheme
    func forceTheme(_ theme: MapTheme)
}

class MapStyleManager: MapStyleManagerProtocol {
    
    private(set) var currentTheme: MapTheme = .light
    private var isManualOverride: Bool = false
    
    func shouldSwitchTheme(isNightModeSensor: Bool) -> MapTheme {
        if isManualOverride {
            return currentTheme
        }
        
        // Logic: specific sensor input or default to time if sensor not available (mocked here by input)
        // If sensor says night, use dark.
        
        if isNightModeSensor {
            currentTheme = .dark
        } else {
            currentTheme = .light
        }
        
        return currentTheme
    }
    
    func forceTheme(_ theme: MapTheme) {
        self.currentTheme = theme
        self.isManualOverride = true
    }
    
    func clearOverride() {
        self.isManualOverride = false
    }
}
