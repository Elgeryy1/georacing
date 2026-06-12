import Combine
import SwiftUI

// MARK: - Fan Zone ViewModel

/// Global state coordinator for Fan Zone.
/// Owns references to all Fan Zone services and propagates the selected team
/// across the entire app via TeamThemeService + UserPreferences.
@MainActor
final class FanZoneViewModel: ObservableObject {
    
    // MARK: - Published State
    
    /// Currently selected championship
    @Published var selectedChampionship: Championship {
        didSet {
            guard oldValue != selectedChampionship else { return }
            UserPreferences.shared.favoriteSeries = selectedChampionship.rawValue
            // Auto-select first team in new championship if current team doesn't match
            if selectedTeam?.championship != selectedChampionship {
                let teams = catalog.teams(for: selectedChampionship)
                selectTeam(teams.first)
            }
        }
    }
    
    /// Currently selected team (full model)
    @Published private(set) var selectedTeam: RacingTeam?
    
    /// Team lists for current championship
    var availableTeams: [RacingTeam] {
        catalog.teams(for: selectedChampionship)
    }
    
    /// Convenience: team color
    var teamColor: Color {
        selectedTeam?.primarySwiftColor ?? RacingColors.red
    }
    
    /// Convenience: team secondary color
    var teamSecondaryColor: Color {
        selectedTeam?.secondarySwiftColor ?? .white
    }
    
    /// Active widgets for layout customization
    @Published var activeWidgets: Set<String> = ["news", "trivia", "collectibles"]
    
    /// Whether the full module has loaded
    @Published private(set) var isLoaded = false
    
    // MARK: - Services
    
    let catalog = TeamCatalogService.shared
    let questionService = QuestionService.shared
    let newsService = FanNewsService.shared
    let rewardService = RewardService.shared
    let assetManager = TeamAssetManager.shared
    
    // MARK: - Init
    
    init() {
        let series = UserPreferences.shared.favoriteSeries
        self.selectedChampionship = Championship(rawValue: series) ?? .f1
        
        // Restore selected team from preferences
        let teamId = UserPreferences.shared.favoriteTeamId
        if !teamId.isEmpty, let team = catalog.team(byId: teamId) {
            self.selectedTeam = team
        } else {
            // Fallback: match by name from old preference
            let teamName = UserPreferences.shared.favoriteTeam
            self.selectedTeam = catalog.teams.first { $0.name == teamName }
                ?? catalog.teams(for: selectedChampionship).first
        }
    }
    
    // MARK: - Load All Data
    
    /// Bootstrap all Fan Zone data (call from .task on FanZoneView)
    func loadAll() async {
        guard !isLoaded else { return }
        
        async let teamsTask: () = catalog.loadCatalog()
        async let questionsTask: () = questionService.loadQuestions()
        async let newsTask: () = newsService.refreshNews()
        async let rewardsTask: () = rewardService.loadCatalog()
        
        _ = await (teamsTask, questionsTask, newsTask, rewardsTask)
        
        // Re-resolve selected team after catalog reload
        if let id = selectedTeam?.id, let refreshed = catalog.team(byId: id) {
            selectedTeam = refreshed
        }
        
        // Record visit for rewards
        rewardService.recordFanZoneVisit()
        
        // Preload logos for current championship
        await assetManager.preloadLogos(for: availableTeams)
        
        isLoaded = true
        Logger.info("[FanZoneVM] All data loaded")
    }
    
    // MARK: - Team Selection
    
    /// Select a new team and propagate globally
    func selectTeam(_ team: RacingTeam?) {
        guard let team else { return }
        
        selectedTeam = team
        selectedChampionship = team.championship
        
        // Persist
        UserPreferences.shared.favoriteTeamId = team.id
        UserPreferences.shared.favoriteTeam = team.name
        UserPreferences.shared.favoriteSeries = team.championship.rawValue
        
        // Update global theme
        TeamThemeService.shared.refresh()
        
        Logger.info("[FanZoneVM] Team selected: \(team.name) (\(team.id))")
    }
    
    // MARK: - Widget Management
    
    func toggleWidget(_ id: String) {
        if activeWidgets.contains(id) {
            activeWidgets.remove(id)
        } else {
            activeWidgets.insert(id)
        }
    }
    
    func isWidgetActive(_ id: String) -> Bool {
        activeWidgets.contains(id)
    }
    
    // MARK: - Quick Access Helpers
    
    /// Get a quick trivia question for the current team
    func quickTrivia() -> QuizQuestion? {
        questionService.nextQuestion(
            championship: selectedChampionship,
            teamId: selectedTeam?.id
        )
    }
    
    /// Latest news count for badge
    var newsCount: Int {
        newsService.articles(for: selectedChampionship).count
    }
    
    /// Unlocked cards count
    var unlockedCardsCount: Int {
        rewardService.unlockedCards.count
    }
    
    /// Total cards
    var totalCardsCount: Int {
        rewardService.cardDefinitions.count
    }
}
