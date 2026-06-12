import Foundation
import Combine

// MARK: - Reward Event Types

/// Events that can trigger card progress
enum RewardEvent: Sendable {
    case quizCorrect
    case quizStreak(Int)
    case quizPerfect
    case firstQuiz
    case fanZoneVisit
    case newsRead
    case teamLoyaltyDay
    case collectionMilestone(Int)
}

// MARK: - Reward Service

/// Manages the collectible card system: catalog, progress, and unlocking.
@MainActor
final class RewardService: ObservableObject {
    
    static let shared = RewardService()
    
    // MARK: - Published
    
    @Published private(set) var cardDefinitions: [RewardCardDefinition] = []
    @Published private(set) var progress: [String: CardProgress] = [:]
    @Published private(set) var isLoading = false
    @Published private(set) var recentlyUnlocked: RewardCardDefinition?
    
    // MARK: - Config
    
    private let remoteURL: URL? = URL(string: "\(AppConstants.apiBaseUrl)/fanzone/rewards")
    private let progressKey = "reward_card_progress"
    private let catalogCacheKey = "reward_catalog_cache"
    private let visitDatesKey = "fanzone_visit_dates"
    private let teamLoyaltyKey = "fanzone_team_loyalty"
    
    // MARK: - Init
    
    private init() {
        cardDefinitions = Self.embeddedCards
        loadProgress()
    }
    
    // MARK: - Public API
    
    /// Load card catalog: remote → cache → embedded
    func loadCatalog() async {
        isLoading = true
        defer { isLoading = false }
        
        if let remote = await fetchRemoteCatalog() {
            cardDefinitions = remote
            saveCatalogCache(remote)
            Logger.info("[RewardService] Loaded \(remote.count) cards from remote")
        } else if let cached = loadCatalogCache() {
            cardDefinitions = cached
            Logger.info("[RewardService] Loaded \(cached.count) cards from cache")
        } else {
            Logger.info("[RewardService] Using \(cardDefinitions.count) embedded cards")
        }
        
        // Initialize progress entries for any new cards
        for card in cardDefinitions where progress[card.id] == nil {
            progress[card.id] = CardProgress(id: card.id, currentValue: 0, isUnlocked: false, unlockedAt: nil)
        }
        saveProgress()
    }
    
    /// Record an event and update all relevant card progress
    func recordEvent(_ event: RewardEvent) {
        var newlyUnlocked: RewardCardDefinition?
        
        for card in cardDefinitions {
            guard var prog = progress[card.id], !prog.isUnlocked else { continue }
            
            let shouldIncrement: Bool
            switch (event, card.unlockCondition.type) {
            case (.quizCorrect, .quizTotal):
                shouldIncrement = true
            case (.quizStreak(let streak), .quizStreak):
                shouldIncrement = streak >= card.unlockCondition.threshold
            case (.quizPerfect, .perfectQuiz):
                shouldIncrement = true
            case (.firstQuiz, .firstQuiz):
                shouldIncrement = true
            case (.fanZoneVisit, .fanZoneVisits):
                shouldIncrement = true
            case (.newsRead, .newsRead):
                shouldIncrement = true
            case (.teamLoyaltyDay, .teamLoyalty):
                shouldIncrement = true
            case (.collectionMilestone(let count), .collectionMilestone):
                shouldIncrement = count >= card.unlockCondition.threshold
            default:
                shouldIncrement = false
            }
            
            if shouldIncrement {
                prog.currentValue += 1
                
                if prog.currentValue >= card.unlockCondition.threshold {
                    prog.isUnlocked = true
                    prog.unlockedAt = Date()
                    newlyUnlocked = card
                    Logger.info("[RewardService] Card unlocked: \(card.title)")
                }
                
                progress[card.id] = prog
            }
        }
        
        saveProgress()
        
        if let unlocked = newlyUnlocked {
            recentlyUnlocked = unlocked
            // Check collection milestones
            let totalUnlocked = unlockedCards.count
            recordEvent(.collectionMilestone(totalUnlocked))
        }
    }
    
    /// Record a Fan Zone visit (once per day)
    func recordFanZoneVisit() {
        let today = Calendar.current.startOfDay(for: Date())
        var dates = visitDates
        if !dates.contains(today) {
            dates.append(today)
            UserDefaults.standard.set(dates.map { $0.timeIntervalSince1970 }, forKey: visitDatesKey)
            recordEvent(.fanZoneVisit)
        }
    }
    
    /// Dismiss the "recently unlocked" notification
    func dismissUnlockNotification() {
        recentlyUnlocked = nil
    }
    
    // MARK: - Computed
    
    /// All unlocked cards
    var unlockedCards: [RewardCardDefinition] {
        cardDefinitions.filter { progress[$0.id]?.isUnlocked == true }
    }
    
    /// All locked cards
    var lockedCards: [RewardCardDefinition] {
        cardDefinitions.filter { progress[$0.id]?.isUnlocked != true }
    }
    
    /// Progress for a specific card (0.0 - 1.0)
    func progressRatio(for cardId: String) -> Double {
        guard let prog = progress[cardId],
              let card = cardDefinitions.first(where: { $0.id == cardId }) else { return 0 }
        guard card.unlockCondition.threshold > 0 else { return 0 }
        return min(1.0, Double(prog.currentValue) / Double(card.unlockCondition.threshold))
    }
    
    /// Cards for a specific team (includes global cards)
    func cards(for teamId: String?) -> [RewardCardDefinition] {
        cardDefinitions.filter { $0.teamId == nil || $0.teamId == teamId }
    }
    
    /// Cards filtered by rarity
    func cards(rarity: CardRarity) -> [RewardCardDefinition] {
        cardDefinitions.filter { $0.rarity == rarity }
    }
    
    /// Total collection count string (e.g. "5/20")
    var collectionSummary: String {
        "\(unlockedCards.count)/\(cardDefinitions.count)"
    }
    
    // MARK: - Persistence
    
    private func saveProgress() {
        do {
            let data = try JSONEncoder().encode(progress)
            UserDefaults.standard.set(data, forKey: progressKey)
        } catch {
            Logger.error("[RewardService] Progress save failed: \(error)")
        }
    }
    
    private func loadProgress() {
        guard let data = UserDefaults.standard.data(forKey: progressKey) else { return }
        do {
            progress = try JSONDecoder().decode([String: CardProgress].self, from: data)
        } catch {
            Logger.error("[RewardService] Progress load failed: \(error)")
        }
    }
    
    private var visitDates: [Date] {
        let timestamps = UserDefaults.standard.array(forKey: visitDatesKey) as? [Double] ?? []
        return timestamps.map { Date(timeIntervalSince1970: $0) }
    }
    
    // MARK: - Remote
    
    private func fetchRemoteCatalog() async -> [RewardCardDefinition]? {
        guard let url = remoteURL else { return nil }
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else { return nil }
            return try JSONDecoder().decode([RewardCardDefinition].self, from: data)
        } catch {
            Logger.warning("[RewardService] Remote fetch failed: \(error.localizedDescription)")
            return nil
        }
    }
    
    private func saveCatalogCache(_ cards: [RewardCardDefinition]) {
        do {
            let data = try JSONEncoder().encode(cards)
            UserDefaults.standard.set(data, forKey: catalogCacheKey)
        } catch {
            Logger.error("[RewardService] Catalog cache save failed: \(error)")
        }
    }
    
    private func loadCatalogCache() -> [RewardCardDefinition]? {
        guard let data = UserDefaults.standard.data(forKey: catalogCacheKey) else { return nil }
        return try? JSONDecoder().decode([RewardCardDefinition].self, from: data)
    }
    
    // MARK: - Embedded Card Catalog (2026)
    
    static let embeddedCards: [RewardCardDefinition] = [
        // ───── Common Cards ─────
        RewardCardDefinition(
            id: "card_first_quiz", teamId: nil, season: 2026, rarity: .common,
            title: "Rookie Driver", description: "Complete your first quiz in Fan Zone.",
            unlockCondition: UnlockCondition(type: .firstQuiz, threshold: 1),
            artTemplate: "template_rookie", badgeIcon: "play.circle.fill",
            number: 1, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_quiz_5", teamId: nil, season: 2026, rarity: .common,
            title: "Knowledge Pit Stop", description: "Answer 5 quiz questions correctly.",
            unlockCondition: UnlockCondition(type: .quizTotal, threshold: 5),
            artTemplate: "template_quiz", badgeIcon: "brain.fill",
            number: 2, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_news_3", teamId: nil, season: 2026, rarity: .common,
            title: "Press Pass", description: "Read 3 news articles.",
            unlockCondition: UnlockCondition(type: .newsRead, threshold: 3),
            artTemplate: "template_news", badgeIcon: "newspaper.fill",
            number: 3, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_visit_1", teamId: nil, season: 2026, rarity: .common,
            title: "Paddock Access", description: "Visit Fan Zone for the first time.",
            unlockCondition: UnlockCondition(type: .fanZoneVisits, threshold: 1),
            artTemplate: "template_visit", badgeIcon: "door.left.hand.open",
            number: 4, totalInSet: 20
        ),
        
        // ───── Rare Cards ─────
        RewardCardDefinition(
            id: "card_quiz_25", teamId: nil, season: 2026, rarity: .rare,
            title: "Race Engineer", description: "Answer 25 questions correctly.",
            unlockCondition: UnlockCondition(type: .quizTotal, threshold: 25),
            artTemplate: "template_engineer", badgeIcon: "wrench.and.screwdriver.fill",
            number: 5, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_streak_5", teamId: nil, season: 2026, rarity: .rare,
            title: "Hot Streak", description: "Answer 5 questions correctly in a row.",
            unlockCondition: UnlockCondition(type: .quizStreak, threshold: 5),
            artTemplate: "template_streak", badgeIcon: "flame.fill",
            number: 6, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_news_10", teamId: nil, season: 2026, rarity: .rare,
            title: "Journalist", description: "Read 10 news articles.",
            unlockCondition: UnlockCondition(type: .newsRead, threshold: 10),
            artTemplate: "template_journalist", badgeIcon: "text.book.closed.fill",
            number: 7, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_visit_3", teamId: nil, season: 2026, rarity: .rare,
            title: "Regular Fan", description: "Visit Fan Zone on 3 different days.",
            unlockCondition: UnlockCondition(type: .fanZoneVisits, threshold: 3),
            artTemplate: "template_fan", badgeIcon: "person.fill.checkmark",
            number: 8, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_loyalty_7", teamId: nil, season: 2026, rarity: .rare,
            title: "Team Loyal", description: "Keep your team for 7 days.",
            unlockCondition: UnlockCondition(type: .teamLoyalty, threshold: 7),
            artTemplate: "template_loyalty", badgeIcon: "heart.fill",
            number: 9, totalInSet: 20
        ),
        
        // ───── Epic Cards ─────
        RewardCardDefinition(
            id: "card_quiz_50", teamId: nil, season: 2026, rarity: .epic,
            title: "Team Principal", description: "Answer 50 questions correctly.",
            unlockCondition: UnlockCondition(type: .quizTotal, threshold: 50),
            artTemplate: "template_principal", badgeIcon: "star.circle.fill",
            number: 10, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_streak_10", teamId: nil, season: 2026, rarity: .epic,
            title: "Pole Position", description: "Answer 10 questions correctly in a row.",
            unlockCondition: UnlockCondition(type: .quizStreak, threshold: 10),
            artTemplate: "template_pole", badgeIcon: "flag.checkered.2.crossed",
            number: 11, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_perfect", teamId: nil, season: 2026, rarity: .epic,
            title: "Grand Slam", description: "Get a perfect score on a quiz session.",
            unlockCondition: UnlockCondition(type: .perfectQuiz, threshold: 1),
            artTemplate: "template_slam", badgeIcon: "trophy.fill",
            number: 12, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_news_25", teamId: nil, season: 2026, rarity: .epic,
            title: "Editor-in-Chief", description: "Read 25 news articles.",
            unlockCondition: UnlockCondition(type: .newsRead, threshold: 25),
            artTemplate: "template_editor", badgeIcon: "doc.text.magnifyingglass",
            number: 13, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_visit_7", teamId: nil, season: 2026, rarity: .epic,
            title: "VIP Pass", description: "Visit Fan Zone on 7 different days.",
            unlockCondition: UnlockCondition(type: .fanZoneVisits, threshold: 7),
            artTemplate: "template_vip", badgeIcon: "crown.fill",
            number: 14, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_collection_5", teamId: nil, season: 2026, rarity: .epic,
            title: "Collector", description: "Unlock 5 cards in your collection.",
            unlockCondition: UnlockCondition(type: .collectionMilestone, threshold: 5),
            artTemplate: "template_collector", badgeIcon: "square.stack.3d.up.fill",
            number: 15, totalInSet: 20
        ),
        
        // ───── Legendary Cards ─────
        RewardCardDefinition(
            id: "card_quiz_100", teamId: nil, season: 2026, rarity: .legendary,
            title: "World Champion", description: "Answer 100 questions correctly.",
            unlockCondition: UnlockCondition(type: .quizTotal, threshold: 100),
            artTemplate: "template_champion", badgeIcon: "medal.fill",
            number: 16, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_streak_20", teamId: nil, season: 2026, rarity: .legendary,
            title: "Dominant Era", description: "Answer 20 questions correctly in a row.",
            unlockCondition: UnlockCondition(type: .quizStreak, threshold: 20),
            artTemplate: "template_dominant", badgeIcon: "bolt.shield.fill",
            number: 17, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_loyalty_30", teamId: nil, season: 2026, rarity: .legendary,
            title: "Lifetime Contract", description: "Keep your team for 30 days.",
            unlockCondition: UnlockCondition(type: .teamLoyalty, threshold: 30),
            artTemplate: "template_lifetime", badgeIcon: "signature",
            number: 18, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_collection_15", teamId: nil, season: 2026, rarity: .legendary,
            title: "Hall of Fame", description: "Unlock 15 cards in your collection.",
            unlockCondition: UnlockCondition(type: .collectionMilestone, threshold: 15),
            artTemplate: "template_hall", badgeIcon: "building.columns.fill",
            number: 19, totalInSet: 20
        ),
        RewardCardDefinition(
            id: "card_news_50", teamId: nil, season: 2026, rarity: .legendary,
            title: "Motorsport Guru", description: "Read 50 news articles.",
            unlockCondition: UnlockCondition(type: .newsRead, threshold: 50),
            artTemplate: "template_guru", badgeIcon: "graduationcap.fill",
            number: 20, totalInSet: 20
        ),
    ]
}
