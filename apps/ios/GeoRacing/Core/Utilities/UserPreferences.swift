import Foundation

@MainActor
class UserPreferences {
    static let shared = UserPreferences()
    private let defaults = UserDefaults.standard
    
    private enum Keys {
        static let onboardingCompleted = "onboardingCompleted"
        static let language = "language"
        static let theme = "theme" // NEW
        static let highContrast = "highContrast"
        static let largeFont = "largeFont"
        
        static let grandstand = "seat_grandstand"
        static let zone = "seat_zone"
        static let row = "seat_row"
        static let seatNumber = "seat_number"
        static let dashboardWidgets = "dashboard_widgets"
        static let favoriteSeries = "favorite_series"
        static let favoriteTeam = "favorite_team"
        static let favoriteTeamId = "favorite_team_id"
    }

    enum AppTheme: String, CaseIterable, Identifiable {
        case system
        case light
        case dark
        var id: String { self.rawValue }
    }
    
    var theme: AppTheme {
        get {
            guard let raw = defaults.string(forKey: Keys.theme), let theme = AppTheme(rawValue: raw) else {
                return .system
            }
            return theme
        }
        set { defaults.set(newValue.rawValue, forKey: Keys.theme) }
    }
    
    var isOnboardingCompleted: Bool {
        get { defaults.bool(forKey: Keys.onboardingCompleted) }
        set { defaults.set(newValue, forKey: Keys.onboardingCompleted) }
    }
    
    var languageCode: String {
        get { defaults.string(forKey: Keys.language) ?? "es" }
        set { defaults.set(newValue, forKey: Keys.language) }
    }
    
    var isHighContrastEnabled: Bool {
        get { defaults.bool(forKey: Keys.highContrast) }
        set { defaults.set(newValue, forKey: Keys.highContrast) }
    }
    
    var isLargeFontEnabled: Bool {
        get { defaults.bool(forKey: Keys.largeFont) }
        set { defaults.set(newValue, forKey: Keys.largeFont) }
    }
    
    // MARK: - Seat Configuration
    var grandstand: String? {
        get { defaults.string(forKey: Keys.grandstand) }
        set { defaults.set(newValue, forKey: Keys.grandstand) }
    }
    
    var zone: String? {
        get { defaults.string(forKey: Keys.zone) }
        set { defaults.set(newValue, forKey: Keys.zone) }
    }
    
    var row: String? {
        get { defaults.string(forKey: Keys.row) }
        set { defaults.set(newValue, forKey: Keys.row) }
    }
    
    var seatNumber: String? {
        get { defaults.string(forKey: Keys.seatNumber) }
        set { defaults.set(newValue, forKey: Keys.seatNumber) }
    }
    
    // MARK: - Dashboard Config
    var dashboardWidgets: [String] {
        get { defaults.stringArray(forKey: Keys.dashboardWidgets) ?? ["map", "shop", "food", "wc", "parking", "schedule", "social", "incidents"] }
        set { defaults.set(newValue, forKey: Keys.dashboardWidgets) }
    }
    
    // MARK: - Fan Zone Config
    var favoriteSeries: String {
        get { defaults.string(forKey: Keys.favoriteSeries) ?? "F1" }
        set { defaults.set(newValue, forKey: Keys.favoriteSeries) }
    }
    
    var favoriteTeam: String {
        get { defaults.string(forKey: Keys.favoriteTeam) ?? "Ferrari" }
        set { defaults.set(newValue, forKey: Keys.favoriteTeam) }
    }
    
    /// New team ID system (e.g. "f1_ferrari"). Falls back to empty for migration.
    var favoriteTeamId: String {
        get { defaults.string(forKey: Keys.favoriteTeamId) ?? "" }
        set { defaults.set(newValue, forKey: Keys.favoriteTeamId) }
    }
}
