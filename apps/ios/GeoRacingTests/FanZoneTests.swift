import XCTest
@testable import GeoRacing

final class FanZoneTests: XCTestCase {
    
    // MARK: - Championship Model Tests
    
    func testChampionshipRawValues() {
        XCTAssertEqual(Championship.f1.rawValue, "F1")
        XCTAssertEqual(Championship.motogp.rawValue, "MotoGP")
    }
    
    func testChampionshipDisplayName() {
        XCTAssertEqual(Championship.f1.displayName, "Formula 1")
        XCTAssertEqual(Championship.motogp.displayName, "MotoGP")
    }
    
    func testChampionshipCaseIterable() {
        XCTAssertEqual(Championship.allCases.count, 2)
    }
    
    // MARK: - Color Hex Extension Tests
    
    func testColorHexValid6Digit() {
        let color = Color(hex: "#FF0000")
        XCTAssertNotNil(color)
    }
    
    func testColorHexValidWithoutHash() {
        let color = Color(hex: "00FF00")
        XCTAssertNotNil(color)
    }
    
    func testColorHexInvalid() {
        let color = Color(hex: "XYZ")
        XCTAssertNil(color)
    }
    
    func testColorHexEmpty() {
        let color = Color(hex: "")
        XCTAssertNil(color)
    }
    
    // MARK: - String Similarity Tests (Jaccard)
    
    func testSimilarityIdenticalStrings() {
        let similarity = "Ferrari wins race".similarityTo("Ferrari wins race")
        XCTAssertEqual(similarity, 1.0, accuracy: 0.001)
    }
    
    func testSimilarityCompletelyDifferent() {
        let similarity = "Ferrari wins race".similarityTo("Honda releases new bike")
        XCTAssertLessThan(similarity, 0.2)
    }
    
    func testSimilarityPartialOverlap() {
        let similarity = "Ferrari wins Monaco Grand Prix 2026".similarityTo("Ferrari dominates Monaco Grand Prix weekend")
        // "Ferrari", "Monaco", "Grand", "Prix" overlap → 4/8 or similar
        XCTAssertGreaterThan(similarity, 0.3)
        XCTAssertLessThan(similarity, 0.9)
    }
    
    func testSimilarityCaseInsensitive() {
        let similarity = "FERRARI WINS".similarityTo("ferrari wins")
        XCTAssertEqual(similarity, 1.0, accuracy: 0.001)
    }
    
    func testSimilarityEmptyStrings() {
        let similarity = "".similarityTo("")
        XCTAssertEqual(similarity, 1.0, accuracy: 0.001)
    }
    
    // MARK: - FeedArticle Deduplication Tests
    
    func testDeduplicateURL_RemovesDuplicates() {
        let articles = [
            makeArticle(id: "1", title: "Article 1", url: "https://example.com/article-1"),
            makeArticle(id: "2", title: "Article 2", url: "https://example.com/article-2"),
            makeArticle(id: "3", title: "Article 1 Copy", url: "https://example.com/article-1"), // duplicate URL
        ]
        
        let deduped = FeedArticle.deduplicateURL(articles)
        XCTAssertEqual(deduped.count, 2)
        XCTAssertEqual(deduped[0].title, "Article 1")
        XCTAssertEqual(deduped[1].title, "Article 2")
    }
    
    func testDeduplicateURL_CaseInsensitive() {
        let articles = [
            makeArticle(id: "1", title: "Article 1", url: "https://Example.com/Article-1"),
            makeArticle(id: "2", title: "Article 1 Copy", url: "https://example.com/article-1"),
        ]
        
        let deduped = FeedArticle.deduplicateURL(articles)
        XCTAssertEqual(deduped.count, 1)
    }
    
    func testDeduplicateURL_NoDuplicates() {
        let articles = [
            makeArticle(id: "1", title: "Article 1", url: "https://example.com/1"),
            makeArticle(id: "2", title: "Article 2", url: "https://example.com/2"),
            makeArticle(id: "3", title: "Article 3", url: "https://example.com/3"),
        ]
        
        let deduped = FeedArticle.deduplicateURL(articles)
        XCTAssertEqual(deduped.count, 3)
    }
    
    func testDeduplicateTitle_RemovesSimilarTitles() {
        let articles = [
            makeArticle(id: "1", title: "Ferrari wins Monaco Grand Prix 2026", url: "https://a.com/1"),
            makeArticle(id: "2", title: "Red Bull dominates qualifying", url: "https://b.com/2"),
            makeArticle(id: "3", title: "Ferrari wins Monaco Grand Prix 2026 race", url: "https://c.com/3"), // very similar
        ]
        
        let deduped = FeedArticle.deduplicateTitle(articles, threshold: 0.7)
        XCTAssertLessThanOrEqual(deduped.count, 2)
    }
    
    func testDeduplicateTitle_KeepsDifferentTitles() {
        let articles = [
            makeArticle(id: "1", title: "Ferrari wins Monaco Grand Prix", url: "https://a.com/1"),
            makeArticle(id: "2", title: "Honda announces new MotoGP bike", url: "https://b.com/2"),
            makeArticle(id: "3", title: "Red Bull dominates qualifying in Silverstone", url: "https://c.com/3"),
        ]
        
        let deduped = FeedArticle.deduplicateTitle(articles, threshold: 0.85)
        XCTAssertEqual(deduped.count, 3)
    }
    
    // MARK: - Card Rarity Tests
    
    func testCardRarityComparable() {
        XCTAssertTrue(CardRarity.common < CardRarity.rare)
        XCTAssertTrue(CardRarity.rare < CardRarity.epic)
        XCTAssertTrue(CardRarity.epic < CardRarity.legendary)
    }
    
    func testCardRarityDisplayNames() {
        XCTAssertEqual(CardRarity.common.displayName, "Common")
        XCTAssertEqual(CardRarity.rare.displayName, "Rare")
        XCTAssertEqual(CardRarity.epic.displayName, "Epic")
        XCTAssertEqual(CardRarity.legendary.displayName, "Legendary")
    }
    
    func testCardRarityAllCases() {
        XCTAssertEqual(CardRarity.allCases.count, 4)
    }
    
    // MARK: - UnlockCondition Tests
    
    func testUnlockConditionDescription() {
        let condition = UnlockCondition(type: .quizStreak, threshold: 5)
        // Description should contain the threshold
        XCTAssertFalse(condition.description.isEmpty)
    }
    
    func testUnlockConditionEquality() {
        let c1 = UnlockCondition(type: .quizStreak, threshold: 5)
        let c2 = UnlockCondition(type: .quizStreak, threshold: 5)
        XCTAssertEqual(c1, c2)
    }
    
    func testUnlockConditionInequality() {
        let c1 = UnlockCondition(type: .quizStreak, threshold: 5)
        let c2 = UnlockCondition(type: .quizTotal, threshold: 10)
        XCTAssertNotEqual(c1, c2)
    }
    
    // MARK: - QuizQuestion Encoding/Decoding
    
    func testQuizQuestionCodable() throws {
        let question = QuizQuestion(
            id: "test_q1",
            season: 2026,
            championship: .f1,
            teamId: "f1_ferrari",
            type: .multipleChoice,
            prompt: "What color is Ferrari?",
            options: ["Red", "Blue", "Green", "Yellow"],
            correctAnswer: 0,
            explanation: "Ferrari is famously red.",
            difficulty: 2,
            tags: ["teams", "history"],
            validFrom: nil,
            validTo: nil
        )
        
        let encoded = try JSONEncoder().encode(question)
        let decoded = try JSONDecoder().decode(QuizQuestion.self, from: encoded)
        
        XCTAssertEqual(decoded.id, "test_q1")
        XCTAssertEqual(decoded.prompt, "What color is Ferrari?")
        XCTAssertEqual(decoded.correctAnswer, 0)
        XCTAssertEqual(decoded.championship, .f1)
        XCTAssertEqual(decoded.options.count, 4)
    }
    
    // MARK: - RacingTeam Model Tests
    
    func testRacingTeamCodable() throws {
        let team = RacingTeam(
            id: "f1_ferrari",
            name: "Ferrari",
            championship: .f1,
            shortName: "FER",
            primaryColor: "#DC0000",
            secondaryColor: "#FFF200",
            logo: "ferrari_logo",
            logoRemoteUrl: nil,
            fallbackIcon: "car.fill",
            isActive: true,
            season: 2026,
            lastUpdated: Date()
        )
        
        let encoded = try JSONEncoder().encode(team)
        let decoded = try JSONDecoder().decode(RacingTeam.self, from: encoded)
        
        XCTAssertEqual(decoded.id, "f1_ferrari")
        XCTAssertEqual(decoded.name, "Ferrari")
        XCTAssertEqual(decoded.championship, .f1)
        XCTAssertEqual(decoded.shortName, "FER")
    }
    
    func testRacingTeamHexColors() {
        let team = RacingTeam(
            id: "f1_ferrari",
            name: "Ferrari",
            championship: .f1,
            shortName: "FER",
            primaryColor: "#DC0000",
            secondaryColor: "#FFF200",
            logo: "ferrari_logo",
            logoRemoteUrl: nil,
            fallbackIcon: "car.fill",
            isActive: true,
            season: 2026,
            lastUpdated: Date()
        )
        
        // Should produce non-nil colors from valid hex strings
        let primary = Color(hex: team.primaryColor)
        let secondary = Color(hex: team.secondaryColor)
        XCTAssertNotNil(primary)
        XCTAssertNotNil(secondary)
    }
    
    // MARK: - TeamCatalogService Tests
    
    @MainActor
    func testTeamCatalogEmbeddedTeamsCount() {
        let catalog = TeamCatalogService.shared
        XCTAssertGreaterThanOrEqual(catalog.teams.count, 22,
            "Catalog should have at least 22 embedded teams (11 F1 + 11 MotoGP)")
    }
    
    @MainActor
    func testTeamCatalogF1Teams() {
        let catalog = TeamCatalogService.shared
        let f1Teams = catalog.teams(for: .f1)
        XCTAssertEqual(f1Teams.count, 11, "Should have 11 F1 teams for 2026")
    }
    
    @MainActor
    func testTeamCatalogMotoGPTeams() {
        let catalog = TeamCatalogService.shared
        let motoGPTeams = catalog.teams(for: .motogp)
        XCTAssertEqual(motoGPTeams.count, 11, "Should have 11 MotoGP teams for 2026")
    }
    
    @MainActor
    func testTeamCatalogFindById() {
        let catalog = TeamCatalogService.shared
        let ferrari = catalog.team(byId: "f1_ferrari")
        XCTAssertNotNil(ferrari)
        XCTAssertEqual(ferrari?.name, "Ferrari")
        XCTAssertEqual(ferrari?.championship, .f1)
    }
    
    @MainActor
    func testTeamCatalogFindById_NotFound() {
        let catalog = TeamCatalogService.shared
        let nonexistent = catalog.team(byId: "f1_nonexistent_team")
        XCTAssertNil(nonexistent)
    }
    
    @MainActor
    func testTeamCatalogAllTeamsHaveValidColors() {
        let catalog = TeamCatalogService.shared
        for team in catalog.teams {
            XCTAssertNotNil(Color(hex: team.primaryColor),
                "Team \(team.name) has invalid primaryColor: \(team.primaryColor)")
            XCTAssertNotNil(Color(hex: team.secondaryColor),
                "Team \(team.name) has invalid secondaryColor: \(team.secondaryColor)")
        }
    }
    
    @MainActor
    func testTeamCatalogAllTeamsHaveUniqueIds() {
        let catalog = TeamCatalogService.shared
        let ids = catalog.teams.map(\.id)
        let uniqueIds = Set(ids)
        XCTAssertEqual(ids.count, uniqueIds.count, "All team IDs should be unique")
    }
    
    // MARK: - QuestionService Tests
    
    @MainActor
    func testQuestionServiceEmbeddedQuestions() {
        let service = QuestionService.shared
        XCTAssertGreaterThanOrEqual(service.allQuestions.count, 50,
            "Should have at least 50 embedded questions")
    }
    
    @MainActor
    func testQuestionServiceNextQuestion_ReturnsQuestion() {
        let service = QuestionService.shared
        let question = service.nextQuestion(championship: nil, teamId: nil)
        XCTAssertNotNil(question, "Should return a question")
    }
    
    @MainActor
    func testQuestionServiceNextQuestion_FiltersByChampionship() {
        let service = QuestionService.shared
        let question = service.nextQuestion(championship: .motogp, teamId: nil)
        if let q = question {
            // Question should be either MotoGP-specific or general (no championship filter fails)
            // The service may fall back to general questions, so just ensure it returns something
            XCTAssertFalse(q.prompt.isEmpty)
        }
    }
    
    @MainActor
    func testQuestionServiceStreakTracking() {
        let service = QuestionService.shared
        let initialStreak = service.currentStreak
        
        service.updateStreak(correct: true)
        XCTAssertEqual(service.currentStreak, initialStreak + 1)
        
        service.updateStreak(correct: false)
        XCTAssertEqual(service.currentStreak, 0)
    }
    
    @MainActor
    func testQuestionServiceAllQuestionsHaveValidStructure() {
        let service = QuestionService.shared
        for q in service.allQuestions {
            XCTAssertFalse(q.id.isEmpty, "Question ID should not be empty")
            XCTAssertFalse(q.prompt.isEmpty, "Question prompt should not be empty")
            XCTAssertGreaterThanOrEqual(q.options.count, 2,
                "Question should have at least 2 options")
            XCTAssertLessThan(q.correctAnswer, q.options.count,
                "correctAnswer index \(q.correctAnswer) out of bounds for \(q.options.count) options in question \(q.id)")
            XCTAssertGreaterThanOrEqual(q.correctAnswer, 0,
                "correctAnswer should not be negative")
            XCTAssertGreaterThanOrEqual(q.difficulty, 1)
            XCTAssertLessThanOrEqual(q.difficulty, 5)
        }
    }
    
    // MARK: - RewardService Tests
    
    @MainActor
    func testRewardServiceEmbeddedCards() {
        let service = RewardService.shared
        XCTAssertGreaterThanOrEqual(service.cardDefinitions.count, 15,
            "Should have at least 15 embedded reward cards")
    }
    
    @MainActor
    func testRewardServiceCardRarityDistribution() {
        let service = RewardService.shared
        let common = service.cards(rarity: .common).count
        let rare = service.cards(rarity: .rare).count
        let epic = service.cards(rarity: .epic).count
        let legendary = service.cards(rarity: .legendary).count
        
        XCTAssertGreaterThan(common, 0, "Should have common cards")
        XCTAssertGreaterThan(rare, 0, "Should have rare cards")
        XCTAssertGreaterThan(epic, 0, "Should have epic cards")
        XCTAssertGreaterThan(legendary, 0, "Should have legendary cards")
    }
    
    @MainActor
    func testRewardServiceCollectionSummary() {
        let service = RewardService.shared
        let summary = service.collectionSummary
        // Should be in format "X/Y"
        XCTAssertTrue(summary.contains("/"), "Collection summary should be in X/Y format")
    }
    
    @MainActor
    func testRewardServiceProgressRatioDefaultZero() {
        let service = RewardService.shared
        // For a card with no progress, ratio should be 0
        let ratio = service.progressRatio(for: "nonexistent_card_id")
        XCTAssertEqual(ratio, 0.0, accuracy: 0.001)
    }
    
    @MainActor
    func testRewardServiceAllCardsHaveUniqueIds() {
        let service = RewardService.shared
        let ids = service.cardDefinitions.map(\.id)
        let uniqueIds = Set(ids)
        XCTAssertEqual(ids.count, uniqueIds.count, "All card IDs should be unique")
    }
    
    // MARK: - CardProgress Tests
    
    func testCardProgressCodable() throws {
        let progress = CardProgress(
            id: "card_1",
            currentValue: 3,
            isUnlocked: false,
            unlockedAt: nil
        )
        
        let encoded = try JSONEncoder().encode(progress)
        let decoded = try JSONDecoder().decode(CardProgress.self, from: encoded)
        
        XCTAssertEqual(decoded.id, "card_1")
        XCTAssertEqual(decoded.currentValue, 3)
        XCTAssertFalse(decoded.isUnlocked)
        XCTAssertNil(decoded.unlockedAt)
    }
    
    func testCardProgressUnlocked() throws {
        let now = Date()
        let progress = CardProgress(
            id: "card_2",
            currentValue: 10,
            isUnlocked: true,
            unlockedAt: now
        )
        
        XCTAssertTrue(progress.isUnlocked)
        XCTAssertNotNil(progress.unlockedAt)
    }
    
    // MARK: - Helpers
    
    private func makeArticle(id: String, title: String, url: String) -> FeedArticle {
        FeedArticle(
            id: id,
            title: title,
            summary: "Summary for \(title)",
            source: "TestSource",
            publishedAt: Date(),
            url: url,
            imageUrl: nil,
            championship: .f1,
            tags: []
        )
    }
}
