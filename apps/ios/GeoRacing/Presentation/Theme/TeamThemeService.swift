import SwiftUI
import Combine

/// Service that provides team-based theming across the app
/// Uses the favorite team from UserPreferences to determine colors
@MainActor
class TeamThemeService: ObservableObject {
    
    // MARK: - Singleton
    
    static let shared = TeamThemeService()
    
    // MARK: - Published Properties
    
    @Published private(set) var primaryColor: Color = .red
    @Published private(set) var secondaryColor: Color = .white
    @Published private(set) var accentColor: Color = .orange
    @Published private(set) var teamName: String = "Ferrari"
    @Published private(set) var teamIcon: String = "car.fill"
    
    // MARK: - Private
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    
    private init() {
        loadTeamTheme()
        
        // Listen for preference changes
        NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)
            .debounce(for: .milliseconds(100), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                self?.loadTeamTheme()
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Public Methods
    
    /// Reload theme from preferences
    func refresh() {
        loadTeamTheme()
    }
    
    /// Get gradient for backgrounds
    var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [primaryColor.opacity(0.3), Color.black],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    /// Get subtle gradient for cards
    var cardGradient: LinearGradient {
        LinearGradient(
            colors: [primaryColor.opacity(0.15), Color(white: 0.1)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    /// Get border color for widgets
    var borderColor: Color {
        primaryColor.opacity(0.4)
    }
    
    // MARK: - Private Methods
    
    private func loadTeamTheme() {
        // Try to load from TeamCatalogService first (new system)
        let teamId = UserPreferences.shared.favoriteTeamId
        if !teamId.isEmpty, let team = TeamCatalogService.shared.team(byId: teamId) {
            teamName = team.name
            primaryColor = team.primarySwiftColor
            secondaryColor = team.secondarySwiftColor
            accentColor = team.secondarySwiftColor
            teamIcon = team.fallbackIcon
            Logger.debug("[TeamTheme] Loaded from catalog: \(team.name)")
            return
        }
        
        // Legacy fallback: match by name
        let team = UserPreferences.shared.favoriteTeam
        if let catalogTeam = TeamCatalogService.shared.teams.first(where: { $0.name == team }) {
            teamName = catalogTeam.name
            primaryColor = catalogTeam.primarySwiftColor
            secondaryColor = catalogTeam.secondarySwiftColor
            accentColor = catalogTeam.secondarySwiftColor
            teamIcon = catalogTeam.fallbackIcon
            // Migrate to new ID system
            UserPreferences.shared.favoriteTeamId = catalogTeam.id
            Logger.debug("[TeamTheme] Migrated from legacy name: \(team) → \(catalogTeam.id)")
            return
        }
        
        // Hardcoded fallback for teams not in catalog
        teamName = team
        
        switch team {
        // F1 Teams
        case "Ferrari":
            primaryColor = Color(red: 0.86, green: 0.0, blue: 0.0)
            secondaryColor = .yellow
            accentColor = .yellow
            teamIcon = "car.fill"
            
        case "Red Bull":
            primaryColor = Color(red: 0.07, green: 0.16, blue: 0.38)
            secondaryColor = Color(red: 1.0, green: 0.84, blue: 0.0)
            accentColor = Color(red: 1.0, green: 0.84, blue: 0.0)
            teamIcon = "bolt.fill"
            
        case "Mercedes":
            primaryColor = Color(red: 0.0, green: 0.82, blue: 0.77)
            secondaryColor = .black
            accentColor = .white
            teamIcon = "star.fill"
            
        case "McLaren":
            primaryColor = Color(red: 1.0, green: 0.53, blue: 0.0)
            secondaryColor = .blue
            accentColor = .white
            teamIcon = "flame.fill"
            
        case "Aston Martin":
            primaryColor = Color(red: 0.0, green: 0.45, blue: 0.35)
            secondaryColor = .yellow
            accentColor = .yellow
            teamIcon = "leaf.fill"
            
        case "Alpine":
            primaryColor = Color(red: 0.0, green: 0.53, blue: 0.87)
            secondaryColor = .pink
            accentColor = .pink
            teamIcon = "mountain.2.fill"
            
        case "Williams":
            primaryColor = Color(red: 0.0, green: 0.26, blue: 0.58)
            secondaryColor = .cyan
            accentColor = .cyan
            teamIcon = "shield.fill"
            
        case "Haas":
            primaryColor = Color(red: 0.72, green: 0.72, blue: 0.72)
            secondaryColor = .red
            accentColor = .red
            teamIcon = "wrench.fill"
            
        case "Sauber", "Kick Sauber":
            primaryColor = Color(red: 0.32, green: 0.69, blue: 0.29)
            secondaryColor = .black
            accentColor = .white
            teamIcon = "cross.fill"
            
        case "RB", "Racing Bulls":
            primaryColor = Color(red: 0.14, green: 0.23, blue: 0.42)
            secondaryColor = .white
            accentColor = .red
            teamIcon = "hare.fill"
            
        // MotoGP Teams
        case "Ducati":
            primaryColor = Color(red: 0.8, green: 0.0, blue: 0.0)
            secondaryColor = .white
            accentColor = .white
            teamIcon = "bolt.circle.fill"
            
        case "Yamaha":
            primaryColor = Color(red: 0.0, green: 0.13, blue: 0.53)
            secondaryColor = .white
            accentColor = .black
            teamIcon = "tuningfork"
            
        case "Honda":
            primaryColor = Color(red: 0.8, green: 0.0, blue: 0.0)
            secondaryColor = .blue
            accentColor = .white
            teamIcon = "circle.circle.fill"
            
        case "KTM":
            primaryColor = Color(red: 1.0, green: 0.4, blue: 0.0)
            secondaryColor = .black
            accentColor = .white
            teamIcon = "flame.circle.fill"
            
        case "Aprilia":
            primaryColor = Color(red: 0.0, green: 0.0, blue: 0.0)
            secondaryColor = .red
            accentColor = .red
            teamIcon = "a.circle.fill"
            
        default:
            primaryColor = .orange
            secondaryColor = .white
            accentColor = .orange
            teamIcon = "flag.checkered"
        }
        
        Logger.debug("[TeamTheme] Loaded theme for \(team): \(teamIcon)")
    }
}

// MARK: - SwiftUI Environment Key

@MainActor
struct TeamThemeKey: EnvironmentKey {
    static let defaultValue = TeamThemeService.shared
}

extension EnvironmentValues {
    @MainActor
    var teamTheme: TeamThemeService {
        get { self[TeamThemeKey.self] }
        set { self[TeamThemeKey.self] = newValue }
    }
}

// MARK: - View Modifier

struct TeamThemedBackground: ViewModifier {
    @ObservedObject var theme = TeamThemeService.shared
    
    func body(content: Content) -> some View {
        content
            .background(theme.backgroundGradient)
    }
}

extension View {
    func teamThemedBackground() -> some View {
        modifier(TeamThemedBackground())
    }
}
