import Foundation
import SwiftUI

// MARK: - Championship

/// Championship type (F1 or MotoGP)
enum Championship: String, Codable, CaseIterable, Identifiable, Hashable, Sendable {
    case f1 = "F1"
    case motogp = "MotoGP"
    
    var id: String { rawValue }
    
    var displayName: String {
        switch self {
        case .f1: return "Formula 1"
        case .motogp: return "MotoGP"
        }
    }
    
    var icon: String {
        switch self {
        case .f1: return "car.fill"
        case .motogp: return "bicycle"
        }
    }
}

// MARK: - Racing Team

/// A team in the catalog (F1 or MotoGP)
struct RacingTeam: Identifiable, Codable, Hashable, Sendable {
    let id: String               // e.g. "f1_ferrari", "motogp_ducati_factory"
    let name: String             // "Ferrari"
    let championship: Championship
    let shortName: String        // "FER"
    let primaryColor: String     // hex "#DC0000"
    let secondaryColor: String   // hex "#FFF200"
    let logo: String             // local asset name
    let logoRemoteUrl: String?   // optional remote URL
    let fallbackIcon: String     // SF Symbol fallback
    let isActive: Bool
    let season: Int
    let lastUpdated: Date
    
    /// Convert hex string to SwiftUI Color
    var primarySwiftColor: Color {
        Color(hex: primaryColor)
    }
    
    var secondarySwiftColor: Color {
        Color(hex: secondaryColor)
    }
    
    /// Gradient for UI backgrounds
    var gradient: LinearGradient {
        LinearGradient(
            colors: [primarySwiftColor.opacity(0.7), primarySwiftColor.opacity(0.15)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - Quiz Question

/// Question types for trivia
enum QuestionType: String, Codable, CaseIterable, Sendable {
    case multipleChoice = "multiple_choice"
    case trueFalse = "true_false"
}

/// A quiz question with metadata
struct QuizQuestion: Identifiable, Codable, Hashable, Sendable {
    let id: String
    let season: Int
    let championship: Championship
    let teamId: String?          // nil = general question
    let type: QuestionType
    let prompt: String
    let options: [String]
    let correctAnswer: Int       // index into options[]
    let explanation: String
    let difficulty: Int          // 1-5
    let tags: [String]           // ["history", "drivers", "circuits", "rules", "tech", "trivia"]
    let validFrom: Date?
    let validTo: Date?
}

// MARK: - Feed Article (News)

/// A normalized news article from RSS or API
struct FeedArticle: Identifiable, Codable, Hashable, Sendable {
    let id: String              // SHA256 hash of url
    let title: String
    let summary: String
    let source: String          // "Motorsport.com", "Autosport"
    let publishedAt: Date
    let url: String
    let imageUrl: String?
    let championship: Championship
    let tags: [String]          // team, driver, circuit tags
    
    /// Deduplicate by URL
    static func deduplicateURL(_ articles: [FeedArticle]) -> [FeedArticle] {
        var seen = Set<String>()
        return articles.filter { article in
            let key = article.url.lowercased()
            guard !seen.contains(key) else { return false }
            seen.insert(key)
            return true
        }
    }
    
    /// Deduplicate by similar title (Levenshtein threshold)
    static func deduplicateTitle(_ articles: [FeedArticle], threshold: Double = 0.85) -> [FeedArticle] {
        var result: [FeedArticle] = []
        for article in articles {
            let isDuplicate = result.contains { existing in
                existing.title.lowercased().similarityTo(article.title.lowercased()) > threshold
            }
            if !isDuplicate {
                result.append(article)
            }
        }
        return result
    }
}

// MARK: - Reward Card

/// Card rarity levels
enum CardRarity: String, Codable, CaseIterable, Comparable, Sendable {
    case common
    case rare
    case epic
    case legendary
    
    var displayName: String {
        switch self {
        case .common: return "Common"
        case .rare: return "Rare"
        case .epic: return "Epic"
        case .legendary: return "Legendary"
        }
    }
    
    var color: Color {
        switch self {
        case .common: return .gray
        case .rare: return .blue
        case .epic: return .purple
        case .legendary: return .orange
        }
    }
    
    var frameGradient: LinearGradient {
        switch self {
        case .common:
            return LinearGradient(colors: [.gray, .gray.opacity(0.5)], startPoint: .top, endPoint: .bottom)
        case .rare:
            return LinearGradient(colors: [.blue, .cyan], startPoint: .topLeading, endPoint: .bottomTrailing)
        case .epic:
            return LinearGradient(colors: [.purple, .pink], startPoint: .topLeading, endPoint: .bottomTrailing)
        case .legendary:
            return LinearGradient(colors: [.orange, .yellow, .orange], startPoint: .topLeading, endPoint: .bottomTrailing)
        }
    }
    
    private var sortOrder: Int {
        switch self {
        case .common: return 0
        case .rare: return 1
        case .epic: return 2
        case .legendary: return 3
        }
    }
    
    static func < (lhs: CardRarity, rhs: CardRarity) -> Bool {
        lhs.sortOrder < rhs.sortOrder
    }
}

/// Unlock condition types
enum UnlockConditionType: String, Codable, Sendable {
    case quizStreak          // Answer N correct in a row
    case quizTotal           // Answer N total correct
    case fanZoneVisits       // Visit Fan Zone on N different days
    case newsRead            // Read N news articles
    case firstQuiz           // Complete first quiz
    case perfectQuiz         // Get 100% on a quiz session
    case teamLoyalty         // Keep same team for N days
    case eventAttendance     // Be at a circuit event
    case collectionMilestone // Unlock N other cards
}

/// Describes what the user must do to unlock a card
struct UnlockCondition: Codable, Hashable, Sendable {
    let type: UnlockConditionType
    let threshold: Int
    
    var description: String {
        switch type {
        case .quizStreak: return "Answer \(threshold) questions correctly in a row"
        case .quizTotal: return "Answer \(threshold) questions correctly"
        case .fanZoneVisits: return "Visit Fan Zone \(threshold) days"
        case .newsRead: return "Read \(threshold) news articles"
        case .firstQuiz: return "Complete your first quiz"
        case .perfectQuiz: return "Get a perfect score on a quiz"
        case .teamLoyalty: return "Keep your team for \(threshold) days"
        case .eventAttendance: return "Attend a circuit event"
        case .collectionMilestone: return "Unlock \(threshold) cards"
        }
    }
    
    /// Localized description (call from @MainActor context only)
    @MainActor
    var localizedDescription: String {
        LocalizationUtils.string(description)
    }
}

/// A collectible reward card definition (from catalog)
struct RewardCardDefinition: Identifiable, Codable, Hashable, Sendable {
    let id: String
    let teamId: String?        // nil = global card
    let season: Int
    let rarity: CardRarity
    let title: String
    let description: String
    let unlockCondition: UnlockCondition
    let artTemplate: String    // template name for rendering
    let badgeIcon: String?     // SF Symbol
    let number: Int            // card number in collection
    let totalInSet: Int        // total cards in this set
}

/// User's progress toward unlocking a card
struct CardProgress: Codable, Identifiable, Sendable {
    let id: String             // matches RewardCardDefinition.id
    var currentValue: Int      // current progress count
    var isUnlocked: Bool
    var unlockedAt: Date?
    
    var progress: Double {
        return 1.0 // placeholder; actual progress computed via condition threshold
    }
}

// MARK: - Color Hex Extension

extension Color {
    /// Convert Color to hex string
    func toHex() -> String {
        let uiColor = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "#%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
    }
}

// MARK: - String Similarity (for news dedup)

extension String {
    /// Jaccard similarity coefficient for quick title comparison
    func similarityTo(_ other: String) -> Double {
        let set1 = Set(self.split(separator: " ").map { $0.lowercased() })
        let set2 = Set(other.split(separator: " ").map { $0.lowercased() })
        guard !set1.isEmpty || !set2.isEmpty else { return 1.0 }
        let intersection = set1.intersection(set2).count
        let union = set1.union(set2).count
        return Double(intersection) / Double(union)
    }
}
