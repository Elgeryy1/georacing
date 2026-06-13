import Foundation
import Combine

// MARK: - Question Service

/// Manages quiz questions: remote fetch, caching, rotation, difficulty adjustment.
/// Questions are never repeated until the pool is exhausted.
@MainActor
final class QuestionService: ObservableObject {
    
    static let shared = QuestionService()
    
    // MARK: - Published
    
    @Published private(set) var allQuestions: [QuizQuestion] = []
    @Published private(set) var isLoading = false
    
    // MARK: - State
    
    /// IDs of questions already answered in this rotation cycle
    private var answeredIds: Set<String> = []
    
    /// User's running score for difficulty adjustment
    private var recentCorrectCount: Int = 0
    private var recentTotalCount: Int = 0
    
    /// Persistent keys
    private let cacheKey = "quiz_questions_cache"
    private let answeredKey = "quiz_answered_ids"
    private let statsKey = "quiz_stats"
    
    // MARK: - Config
    
    private let remoteURL: URL? = URL(string: "\(AppConstants.apiBaseUrl)/fanzone/questions")
    private let cacheTTL: TimeInterval = 7 * 86_400 // 7 days
    
    // MARK: - Init
    
    private init() {
        loadAnsweredIds()
        loadStats()
        allQuestions = Self.embeddedQuestions
    }
    
    // MARK: - Public API
    
    /// Load questions: remote → cache → embedded
    func loadQuestions() async {
        isLoading = true
        defer { isLoading = false }
        
        if let remote = await fetchRemote() {
            allQuestions = remote
            saveToCache(remote)
            Logger.info("[QuestionService] Loaded \(remote.count) questions from remote")
            return
        }
        
        if let cached = loadFromCache() {
            allQuestions = cached
            Logger.info("[QuestionService] Loaded \(cached.count) questions from cache")
            return
        }
        
        allQuestions = Self.embeddedQuestions
        Logger.info("[QuestionService] Using \(allQuestions.count) embedded questions")
    }
    
    /// Get next question based on championship, team, and difficulty
    func nextQuestion(
        championship: Championship? = nil,
        teamId: String? = nil,
        targetDifficulty: Int? = nil
    ) -> QuizQuestion? {
        var pool = allQuestions
        
        // Filter by championship
        if let champ = championship {
            pool = pool.filter { $0.championship == champ }
        }
        
        // Filter by team (include general questions too)
        if let team = teamId {
            pool = pool.filter { $0.teamId == nil || $0.teamId == team }
        }
        
        // Filter by validity period
        let now = Date()
        pool = pool.filter { q in
            if let from = q.validFrom, now < from { return false }
            if let to = q.validTo, now > to { return false }
            return true
        }
        
        // Exclude already answered
        let unanswered = pool.filter { !answeredIds.contains($0.id) }
        
        // Reset rotation if all exhausted
        let candidates: [QuizQuestion]
        if unanswered.isEmpty {
            answeredIds.removeAll()
            saveAnsweredIds()
            candidates = pool
        } else {
            candidates = unanswered
        }
        
        guard !candidates.isEmpty else { return nil }
        
        // Difficulty adjustment
        let difficulty = targetDifficulty ?? adaptiveDifficulty()
        let sorted = candidates.sorted {
            abs($0.difficulty - difficulty) < abs($1.difficulty - difficulty)
        }
        
        // Pick from top 5 closest difficulty, randomly
        let topN = min(5, sorted.count)
        return sorted[Int.random(in: 0..<topN)]
    }
    
    /// Get a batch of questions for a quiz session
    func quizSession(
        count: Int = 10,
        championship: Championship? = nil,
        teamId: String? = nil
    ) -> [QuizQuestion] {
        var result: [QuizQuestion] = []
        var usedIds = Set<String>()
        
        for _ in 0..<count {
            var pool = allQuestions
            
            if let champ = championship {
                pool = pool.filter { $0.championship == champ }
            }
            if let team = teamId {
                pool = pool.filter { $0.teamId == nil || $0.teamId == team }
            }
            
            pool = pool.filter { !usedIds.contains($0.id) && !answeredIds.contains($0.id) }
            
            if pool.isEmpty {
                // Reset and retry
                pool = allQuestions
                if let champ = championship { pool = pool.filter { $0.championship == champ } }
                if let team = teamId { pool = pool.filter { $0.teamId == nil || $0.teamId == team } }
                pool = pool.filter { !usedIds.contains($0.id) }
            }
            
            guard let question = pool.randomElement() else { break }
            result.append(question)
            usedIds.insert(question.id)
        }
        
        return result
    }
    
    /// Record an answer
    func recordAnswer(questionId: String, wasCorrect: Bool) {
        answeredIds.insert(questionId)
        recentTotalCount += 1
        if wasCorrect { recentCorrectCount += 1 }
        saveAnsweredIds()
        saveStats()
    }
    
    /// Reset all progress
    func resetProgress() {
        answeredIds.removeAll()
        recentCorrectCount = 0
        recentTotalCount = 0
        saveAnsweredIds()
        saveStats()
    }
    
    /// Current accuracy percentage
    var accuracy: Double {
        guard recentTotalCount > 0 else { return 0 }
        return Double(recentCorrectCount) / Double(recentTotalCount) * 100
    }
    
    /// Total questions answered
    var totalAnswered: Int { recentTotalCount }
    
    /// Current streak (loaded separately if needed)
    var currentStreak: Int {
        UserDefaults.standard.integer(forKey: "quiz_current_streak")
    }
    
    func updateStreak(correct: Bool) {
        if correct {
            UserDefaults.standard.set(currentStreak + 1, forKey: "quiz_current_streak")
        } else {
            let best = max(bestStreak, currentStreak)
            UserDefaults.standard.set(best, forKey: "quiz_best_streak")
            UserDefaults.standard.set(0, forKey: "quiz_current_streak")
        }
    }
    
    var bestStreak: Int {
        UserDefaults.standard.integer(forKey: "quiz_best_streak")
    }
    
    // MARK: - Adaptive Difficulty
    
    private func adaptiveDifficulty() -> Int {
        guard recentTotalCount >= 5 else { return 3 } // Default medium
        let ratio = Double(recentCorrectCount) / Double(recentTotalCount)
        switch ratio {
        case 0.9...: return 5
        case 0.75..<0.9: return 4
        case 0.5..<0.75: return 3
        case 0.3..<0.5: return 2
        default: return 1
        }
    }
    
    // MARK: - Persistence
    
    private func loadAnsweredIds() {
        answeredIds = Set(UserDefaults.standard.stringArray(forKey: answeredKey) ?? [])
    }
    
    private func saveAnsweredIds() {
        UserDefaults.standard.set(Array(answeredIds), forKey: answeredKey)
    }
    
    private func loadStats() {
        recentCorrectCount = UserDefaults.standard.integer(forKey: "\(statsKey)_correct")
        recentTotalCount = UserDefaults.standard.integer(forKey: "\(statsKey)_total")
    }
    
    private func saveStats() {
        UserDefaults.standard.set(recentCorrectCount, forKey: "\(statsKey)_correct")
        UserDefaults.standard.set(recentTotalCount, forKey: "\(statsKey)_total")
    }
    
    // MARK: - Remote
    
    private func fetchRemote() async -> [QuizQuestion]? {
        guard let url = remoteURL else { return nil }
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else { return nil }
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode([QuizQuestion].self, from: data)
        } catch {
            Logger.warning("[QuestionService] Remote fetch failed: \(error.localizedDescription)")
            return nil
        }
    }
    
    // MARK: - Cache
    
    private func saveToCache(_ questions: [QuizQuestion]) {
        do {
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .iso8601
            let data = try encoder.encode(questions)
            UserDefaults.standard.set(data, forKey: cacheKey)
        } catch {
            Logger.error("[QuestionService] Cache save failed: \(error)")
        }
    }
    
    private func loadFromCache() -> [QuizQuestion]? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else { return nil }
        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode([QuizQuestion].self, from: data)
        } catch {
            Logger.error("[QuestionService] Cache decode failed: \(error)")
            return nil
        }
    }
    
    // MARK: - Embedded Questions (2026 Season)
    
    // swiftlint:disable function_body_length
    static let embeddedQuestions: [QuizQuestion] = [
        // ───── F1 General ─────
        QuizQuestion(id: "f1_gen_01", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many teams are on the F1 2026 grid?",
                     options: ["10", "11", "12", "9"],
                     correctAnswer: 1, explanation: "Cadillac joins as the 11th team for 2026.",
                     difficulty: 1, tags: ["rules", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_02", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which new team joins F1 in 2026?",
                     options: ["Cadillac", "Porsche", "Andretti", "Toyota"],
                     correctAnswer: 0, explanation: "Cadillac (GM) is the 11th team on the 2026 grid.",
                     difficulty: 1, tags: ["2026", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_03", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What major regulation change defines F1 2026?",
                     options: ["Active aerodynamics", "V8 engines", "Wider cars", "No DRS"],
                     correctAnswer: 0, explanation: "2026 introduces active aero with movable front and rear wing elements.",
                     difficulty: 2, tags: ["rules", "tech", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_04", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the new F1 engine formula for 2026?",
                     options: ["1.6L V6 turbo with 50% electric power", "2.0L V6 turbo", "1.6L V6 hybrid (same)", "Full electric"],
                     correctAnswer: 0, explanation: "The 2026 PU splits power roughly 50/50 between ICE and electric motor.",
                     difficulty: 3, tags: ["tech", "rules", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_05", season: 2026, championship: .f1, teamId: nil,
                     type: .trueFalse,
                     prompt: "True or False: The MGU-H is removed from the 2026 power unit.",
                     options: ["True", "False"],
                     correctAnswer: 0, explanation: "The MGU-H has been dropped from the 2026 regulations to reduce costs and attract new manufacturers.",
                     difficulty: 3, tags: ["tech", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_06", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which brand replaces Sauber on the F1 grid in 2026?",
                     options: ["Audi", "Porsche", "BMW", "Lamborghini"],
                     correctAnswer: 0, explanation: "Audi takes over the Sauber entry for 2026.",
                     difficulty: 1, tags: ["teams", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_07", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which fuel will F1 cars use from 2026?",
                     options: ["100% sustainable fuel", "E10 fuel", "Standard gasoline", "Hydrogen"],
                     correctAnswer: 0, explanation: "F1 mandates fully sustainable fuel from 2026 onwards.",
                     difficulty: 2, tags: ["tech", "rules", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_08", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many World Constructors' Championships has Ferrari won?",
                     options: ["16", "12", "21", "8"],
                     correctAnswer: 0, explanation: "Ferrari holds the record with 16 Constructors' Championships.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_09", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Who holds the record for most F1 race wins?",
                     options: ["Lewis Hamilton", "Michael Schumacher", "Max Verstappen", "Ayrton Senna"],
                     correctAnswer: 0, explanation: "Lewis Hamilton holds the all-time record with 100+ Grand Prix victories.",
                     difficulty: 1, tags: ["drivers", "history"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_10", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the minimum weight of an F1 car in 2026 (without driver)?",
                     options: ["768 kg", "798 kg", "740 kg", "820 kg"],
                     correctAnswer: 0, explanation: "The 2026 regulations target a lighter car at approximately 768 kg.",
                     difficulty: 4, tags: ["tech", "rules", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_11", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which circuit hosts the first race of a typical F1 season?",
                     options: ["Bahrain", "Australia", "Saudi Arabia", "Monaco"],
                     correctAnswer: 0, explanation: "Bahrain has become the traditional season opener in recent years.",
                     difficulty: 2, tags: ["circuits"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_12", season: 2026, championship: .f1, teamId: nil,
                     type: .trueFalse,
                     prompt: "True or False: DRS is removed in 2026 F1 regulations.",
                     options: ["True", "False"],
                     correctAnswer: 0, explanation: "DRS is replaced by active aerodynamics in 2026.",
                     difficulty: 3, tags: ["rules", "tech", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_13", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many points does a race winner receive?",
                     options: ["25", "30", "20", "10"],
                     correctAnswer: 0, explanation: "The race winner receives 25 points.",
                     difficulty: 1, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_14", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which driver has the most consecutive World Championships?",
                     options: ["Max Verstappen (4)", "Michael Schumacher (5)", "Sebastian Vettel (4)", "Lewis Hamilton (4)"],
                     correctAnswer: 1, explanation: "Schumacher holds the record with 5 consecutive titles (2000-2004).",
                     difficulty: 3, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_15", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What does a red flag during a race mean?",
                     options: ["Session stopped", "Last lap", "Pit lane open", "DRS enabled"],
                     correctAnswer: 0, explanation: "A red flag means the session is immediately stopped, usually due to unsafe conditions.",
                     difficulty: 1, tags: ["rules"], validFrom: nil, validTo: nil),
        
        // ───── F1 Team-Specific ─────
        QuizQuestion(id: "f1_fer_01", season: 2026, championship: .f1, teamId: "f1_ferrari",
                     type: .multipleChoice,
                     prompt: "In what year was Scuderia Ferrari founded?",
                     options: ["1929", "1947", "1950", "1935"],
                     correctAnswer: 0, explanation: "Enzo Ferrari founded Scuderia Ferrari in 1929 in Modena, Italy.",
                     difficulty: 3, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_fer_02", season: 2026, championship: .f1, teamId: "f1_ferrari",
                     type: .multipleChoice,
                     prompt: "Who is Ferrari's team principal (2026)?",
                     options: ["Frédéric Vasseur", "Mattia Binotto", "Maurizio Arrivabene", "Stefano Domenicali"],
                     correctAnswer: 0, explanation: "Frédéric Vasseur leads Ferrari since 2023.",
                     difficulty: 2, tags: ["teams", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_rbr_01", season: 2026, championship: .f1, teamId: "f1_red_bull",
                     type: .multipleChoice,
                     prompt: "In what year did Red Bull Racing win their first Constructors' Championship?",
                     options: ["2010", "2008", "2012", "2011"],
                     correctAnswer: 0, explanation: "Red Bull won their first Constructors' title in 2010 with Sebastian Vettel.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_rbr_02", season: 2026, championship: .f1, teamId: "f1_red_bull",
                     type: .multipleChoice,
                     prompt: "What is the name of Red Bull Racing's F1 car factory location?",
                     options: ["Milton Keynes", "Maranello", "Enstone", "Brackley"],
                     correctAnswer: 0, explanation: "Red Bull Racing is based in Milton Keynes, England.",
                     difficulty: 3, tags: ["teams", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_mer_01", season: 2026, championship: .f1, teamId: "f1_mercedes",
                     type: .multipleChoice,
                     prompt: "How many consecutive Constructors' Championships did Mercedes win (2014-2021)?",
                     options: ["8", "6", "7", "5"],
                     correctAnswer: 0, explanation: "Mercedes won 8 straight Constructors' titles from 2014 to 2021.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_mcl_01", season: 2026, championship: .f1, teamId: "f1_mclaren",
                     type: .multipleChoice,
                     prompt: "Who was the last World Champion driving for McLaren?",
                     options: ["Lewis Hamilton (2008)", "Mika Häkkinen (1999)", "Ayrton Senna (1991)", "Alain Prost (1989)"],
                     correctAnswer: 0, explanation: "Lewis Hamilton won his first title with McLaren in 2008.",
                     difficulty: 2, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_amr_01", season: 2026, championship: .f1, teamId: "f1_aston_martin",
                     type: .multipleChoice,
                     prompt: "What was Aston Martin's team name before 2021?",
                     options: ["Racing Point", "Force India", "Jordan", "Spyker"],
                     correctAnswer: 0, explanation: "The team was Racing Point (2019-2020) before becoming Aston Martin in 2021.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_alp_01", season: 2026, championship: .f1, teamId: "f1_alpine",
                     type: .multipleChoice,
                     prompt: "Which manufacturer owns Alpine F1 Team?",
                     options: ["Renault", "Peugeot", "Citroën", "Bugatti"],
                     correctAnswer: 0, explanation: "Alpine is the motorsport brand of the Renault Group.",
                     difficulty: 2, tags: ["teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_wil_01", season: 2026, championship: .f1, teamId: "f1_williams",
                     type: .multipleChoice,
                     prompt: "How many Constructors' Championships has Williams won?",
                     options: ["9", "7", "5", "11"],
                     correctAnswer: 0, explanation: "Williams has won 9 Constructors' Championships in its history.",
                     difficulty: 3, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_haa_01", season: 2026, championship: .f1, teamId: "f1_haas",
                     type: .multipleChoice,
                     prompt: "In what year did Haas F1 Team debut in Formula 1?",
                     options: ["2016", "2018", "2014", "2020"],
                     correctAnswer: 0, explanation: "Haas made their F1 debut in 2016, the first American team in decades.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_cad_01", season: 2026, championship: .f1, teamId: "f1_cadillac",
                     type: .trueFalse,
                     prompt: "True or False: Cadillac is General Motors' first factory F1 entry.",
                     options: ["True", "False"],
                     correctAnswer: 0, explanation: "Cadillac, backed by GM, enters F1 in 2026 as the 11th team.",
                     difficulty: 2, tags: ["teams", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_aud_01", season: 2026, championship: .f1, teamId: "f1_audi",
                     type: .multipleChoice,
                     prompt: "Which team did Audi take over to enter F1 in 2026?",
                     options: ["Sauber", "Williams", "Haas", "Alpine"],
                     correctAnswer: 0, explanation: "Audi acquired the Sauber F1 team to enter the championship in 2026.",
                     difficulty: 1, tags: ["teams", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_circuits_01", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which is the longest circuit on the F1 calendar?",
                     options: ["Spa-Francorchamps", "Silverstone", "Monza", "Jeddah"],
                     correctAnswer: 0, explanation: "Spa-Francorchamps in Belgium is the longest at 7.004 km.",
                     difficulty: 2, tags: ["circuits"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_circuits_02", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "In which country is the Circuit de Barcelona-Catalunya?",
                     options: ["Spain", "Italy", "France", "Portugal"],
                     correctAnswer: 0, explanation: "The Circuit de Barcelona-Catalunya is located in Montmeló, Spain.",
                     difficulty: 1, tags: ["circuits"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_circuits_03", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which F1 circuit is known as 'The Temple of Speed'?",
                     options: ["Monza", "Silverstone", "Spa", "Suzuka"],
                     correctAnswer: 0, explanation: "Monza in Italy is nicknamed 'The Temple of Speed' for its ultra-high velocities.",
                     difficulty: 1, tags: ["circuits", "trivia"], validFrom: nil, validTo: nil),
        
        // ───── MotoGP General ─────
        QuizQuestion(id: "mgp_gen_01", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Who holds the record for most MotoGP premier class titles?",
                     options: ["Giacomo Agostini (8)", "Valentino Rossi (7)", "Marc Márquez (6)", "Mick Doohan (5)"],
                     correctAnswer: 0, explanation: "Giacomo Agostini holds 8 premier class titles (500cc era).",
                     difficulty: 3, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_02", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What engine configuration do MotoGP bikes use?",
                     options: ["1000cc 4-cylinder", "750cc 3-cylinder", "800cc 4-cylinder", "1200cc V-twin"],
                     correctAnswer: 0, explanation: "MotoGP bikes use 1000cc 4-cylinder engines (inline-4 or V4).",
                     difficulty: 2, tags: ["tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_03", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the maximum number of engines a MotoGP rider can use per season?",
                     options: ["7", "5", "9", "Unlimited"],
                     correctAnswer: 0, explanation: "Each rider is allocated 7 engines per season.",
                     difficulty: 4, tags: ["rules", "tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_04", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which circuit is known as the 'Cathedral of Speed' in MotoGP?",
                     options: ["Mugello", "Assen", "Phillip Island", "Sachsenring"],
                     correctAnswer: 0, explanation: "Mugello in Italy is often called the Cathedral of MotoGP for its iconic status.",
                     difficulty: 2, tags: ["circuits", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_05", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many points does a MotoGP race winner receive?",
                     options: ["25", "20", "30", "15"],
                     correctAnswer: 0, explanation: "The winner gets 25 points in a conventional race. Sprint races award fewer.",
                     difficulty: 1, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_06", season: 2026, championship: .motogp, teamId: nil,
                     type: .trueFalse,
                     prompt: "True or False: MotoGP sprint races award half the points of a full race.",
                     options: ["True", "False"],
                     correctAnswer: 1, explanation: "Sprint race points use a different scale — not exactly half. The sprint winner gets 12 points.",
                     difficulty: 3, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_07", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the typical top speed of a MotoGP bike?",
                     options: ["Over 350 km/h", "280 km/h", "300 km/h", "400 km/h"],
                     correctAnswer: 0, explanation: "MotoGP bikes regularly exceed 350 km/h on straights like Mugello.",
                     difficulty: 2, tags: ["tech", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_08", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which circuit is known as 'The Cathedral' (oldest circuit) in MotoGP?",
                     options: ["Assen", "Mugello", "Silverstone", "Sachsenring"],
                     correctAnswer: 0, explanation: "Assen is the oldest circuit on the MotoGP calendar, called 'The Cathedral of Motorcycling'.",
                     difficulty: 2, tags: ["circuits", "history"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_09", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many races did Marc Márquez win in his debut MotoGP season (2013)?",
                     options: ["6", "3", "10", "1"],
                     correctAnswer: 0, explanation: "Márquez won 6 races in his rookie MotoGP season to clinch the title at age 20.",
                     difficulty: 4, tags: ["drivers", "history"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_10", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What tire manufacturer is the sole MotoGP supplier?",
                     options: ["Michelin", "Bridgestone", "Pirelli", "Dunlop"],
                     correctAnswer: 0, explanation: "Michelin has been the sole tire supplier for MotoGP since 2016.",
                     difficulty: 1, tags: ["tech"], validFrom: nil, validTo: nil),
        
        // ───── MotoGP Team-Specific ─────
        QuizQuestion(id: "mgp_duc_01", season: 2026, championship: .motogp, teamId: "motogp_ducati_factory",
                     type: .multipleChoice,
                     prompt: "Who scored Ducati's first MotoGP race win?",
                     options: ["Loris Capirossi", "Casey Stoner", "Andrea Dovizioso", "Valentino Rossi"],
                     correctAnswer: 0, explanation: "Loris Capirossi won Ducati's first MotoGP race at Catalunya in 2003.",
                     difficulty: 4, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_duc_02", season: 2026, championship: .motogp, teamId: "motogp_ducati_factory",
                     type: .multipleChoice,
                     prompt: "What type of engine does the Ducati Desmosedici use?",
                     options: ["V4 90°", "Inline-4", "V4 75°", "V-twin"],
                     correctAnswer: 0, explanation: "The Ducati Desmosedici features a 90-degree V4 engine with desmodromic valves.",
                     difficulty: 3, tags: ["tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_yam_01", season: 2026, championship: .motogp, teamId: "motogp_yamaha_factory",
                     type: .multipleChoice,
                     prompt: "Who won Yamaha's last MotoGP World Championship?",
                     options: ["Fabio Quartararo (2021)", "Valentino Rossi (2009)", "Jorge Lorenzo (2015)", "Ben Spies (2011)"],
                     correctAnswer: 0, explanation: "Fabio Quartararo won the 2021 MotoGP title with Yamaha.",
                     difficulty: 2, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_hon_01", season: 2026, championship: .motogp, teamId: "motogp_honda_hrc",
                     type: .multipleChoice,
                     prompt: "How many premier class Constructors' titles does Honda have?",
                     options: ["25+", "15", "10", "5"],
                     correctAnswer: 0, explanation: "Honda holds over 25 premier class Constructors' championships (500cc + MotoGP).",
                     difficulty: 3, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_ktm_01", season: 2026, championship: .motogp, teamId: "motogp_ktm_factory",
                     type: .multipleChoice,
                     prompt: "In what year did KTM make their MotoGP debut?",
                     options: ["2017", "2015", "2019", "2020"],
                     correctAnswer: 0, explanation: "KTM entered MotoGP in 2017 with the RC16.",
                     difficulty: 2, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_apr_01", season: 2026, championship: .motogp, teamId: "motogp_aprilia",
                     type: .multipleChoice,
                     prompt: "What engine layout does the Aprilia RS-GP use?",
                     options: ["V4 90°", "Inline-4", "V4 65°", "V-twin"],
                     correctAnswer: 2, explanation: "The Aprilia RS-GP uses a narrow-angle V4 (approximately 65°).",
                     difficulty: 4, tags: ["tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_vr46_01", season: 2026, championship: .motogp, teamId: "motogp_vr46",
                     type: .multipleChoice,
                     prompt: "Who founded the VR46 Racing Team?",
                     options: ["Valentino Rossi", "Luca Marini", "Marco Bezzecchi", "Uccio Salucci"],
                     correctAnswer: 0, explanation: "Valentino Rossi founded VR46 Racing, named after his iconic #46.",
                     difficulty: 1, tags: ["teams", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_trk_01", season: 2026, championship: .motogp, teamId: "motogp_trackhouse",
                     type: .trueFalse,
                     prompt: "True or False: Trackhouse Racing is the first American-owned team in MotoGP.",
                     options: ["True", "False"],
                     correctAnswer: 0, explanation: "Trackhouse Racing, owned by Justin Marks, became the first American-owned MotoGP team.",
                     difficulty: 2, tags: ["teams", "trivia"], validFrom: nil, validTo: nil),
        
        // ───── Mixed / Trivia ─────
        QuizQuestion(id: "mix_01", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which driver won the most Monaco Grand Prix races?",
                     options: ["Ayrton Senna (6)", "Graham Hill (5)", "Michael Schumacher (5)", "Lewis Hamilton (3)"],
                     correctAnswer: 0, explanation: "Ayrton Senna holds the record with 6 Monaco GP victories.",
                     difficulty: 3, tags: ["history", "circuits", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_02", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What color flag indicates dangerous conditions and no overtaking?",
                     options: ["Yellow", "Red", "Blue", "White"],
                     correctAnswer: 0, explanation: "A yellow flag warns of danger ahead and prohibits overtaking.",
                     difficulty: 1, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_03", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What does the black flag with an orange circle mean?",
                     options: ["Mechanical problem, return to pits", "Disqualified", "Penalty", "Last lap"],
                     correctAnswer: 0, explanation: "The meatball flag signals a mechanical problem and the rider must return to the pits.",
                     difficulty: 3, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_04", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the fastest pit stop ever recorded in F1?",
                     options: ["Under 2 seconds", "2.5 seconds", "3 seconds", "4 seconds"],
                     correctAnswer: 0, explanation: "Red Bull holds the record with pit stops under 2 seconds.",
                     difficulty: 2, tags: ["trivia", "tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_05", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the penalty for exceeding track limits 3 times?",
                     options: ["Black and white flag warning", "5-second penalty", "Drive through", "No penalty"],
                     correctAnswer: 0, explanation: "After 3 track limit violations, the driver receives a black and white flag warning.",
                     difficulty: 3, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_06", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is 'arm pump' in MotoGP?",
                     options: ["Compartment syndrome in forearms", "Engine vibration issue", "Tire degradation", "Aerodynamic effect"],
                     correctAnswer: 0, explanation: "Arm pump is compartment syndrome caused by intense braking forces on the forearms.",
                     difficulty: 3, tags: ["tech", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_07", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How much downforce does a modern F1 car generate at 250 km/h?",
                     options: ["Over 1000 kg", "500 kg", "300 kg", "200 kg"],
                     correctAnswer: 0, explanation: "Modern F1 cars generate over 1 tonne of downforce at high speed.",
                     difficulty: 4, tags: ["tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_08", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is 'ride height device' in MotoGP?",
                     options: ["Lowers bike for better starts", "Adjusts suspension mid-corner", "Controls wheelie", "Reduces drag"],
                     correctAnswer: 0, explanation: "The ride height device lowers the rear of the bike for better traction off the line.",
                     difficulty: 4, tags: ["tech"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_09", season: 2026, championship: .f1, teamId: nil,
                     type: .trueFalse,
                     prompt: "True or False: F1 drivers can lose up to 3 kg of body weight during a race.",
                     options: ["True", "False"],
                     correctAnswer: 0, explanation: "F1 drivers can lose 2-3 kg through sweating during a race, especially in hot conditions.",
                     difficulty: 2, tags: ["trivia", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mix_10", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What does parc fermé mean in F1?",
                     options: ["No modifications allowed to the car", "Pit lane closure", "Car impounded after race", "Start procedure"],
                     correctAnswer: 0, explanation: "Parc fermé rules prohibit changes to the car between qualifying and the race.",
                     difficulty: 2, tags: ["rules"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_16", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "Which F1 team has won the most Constructors' Championships?",
                     options: ["Ferrari (16)", "McLaren (8)", "Williams (9)", "Mercedes (8)"],
                     correctAnswer: 0, explanation: "Ferrari holds the record with 16 Constructors' Championships.",
                     difficulty: 1, tags: ["history", "teams"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_11", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many MotoGP titles did Valentino Rossi win?",
                     options: ["7", "9", "5", "6"],
                     correctAnswer: 0, explanation: "Valentino Rossi won 7 MotoGP/500cc premier class World Championships.",
                     difficulty: 2, tags: ["history", "drivers"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_12", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the lean angle MotoGP riders can achieve?",
                     options: ["Over 60°", "45°", "50°", "30°"],
                     correctAnswer: 0, explanation: "MotoGP riders regularly lean their bikes beyond 60 degrees in corners.",
                     difficulty: 3, tags: ["tech", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_17", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is the cost cap for F1 teams in 2026?",
                     options: ["~$135 million", "$200 million", "$100 million", "No cap"],
                     correctAnswer: 0, explanation: "The F1 cost cap is approximately $135 million, with some exclusions.",
                     difficulty: 4, tags: ["rules", "2026"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "f1_gen_18", season: 2026, championship: .f1, teamId: nil,
                     type: .multipleChoice,
                     prompt: "How many G-forces does an F1 driver experience during heavy braking?",
                     options: ["Up to 6G", "3G", "8G", "2G"],
                     correctAnswer: 0, explanation: "F1 drivers experience up to 6G under heavy braking.",
                     difficulty: 3, tags: ["tech", "trivia"], validFrom: nil, validTo: nil),
        
        QuizQuestion(id: "mgp_gen_13", season: 2026, championship: .motogp, teamId: nil,
                     type: .multipleChoice,
                     prompt: "What is a 'long lap penalty' in MotoGP?",
                     options: ["Extended loop through a wider section of track", "10-second time penalty", "Ride through pit lane", "Position drop"],
                     correctAnswer: 0, explanation: "A long lap penalty requires the rider to take a designated extended loop, costing several seconds.",
                     difficulty: 2, tags: ["rules"], validFrom: nil, validTo: nil),
    ]
    // swiftlint:enable function_body_length
}
