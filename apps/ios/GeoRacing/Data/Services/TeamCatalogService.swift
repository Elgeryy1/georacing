import Foundation
import Combine

// MARK: - Team Catalog Service

/// Loads and caches the team catalog.
/// Priority: Remote → Disk Cache → Embedded Fallback
@MainActor
final class TeamCatalogService: ObservableObject {
    
    static let shared = TeamCatalogService()
    
    // MARK: - Published
    
    @Published private(set) var teams: [RacingTeam] = []
    @Published private(set) var isLoading = false
    @Published private(set) var lastUpdated: Date?
    
    // MARK: - Config
    
    /// Remote catalog URL (update this to your actual endpoint)
    private let remoteURL: URL? = URL(string: "\(AppConstants.apiBaseUrl)/fanzone/teams")
    
    /// Disk cache key
    private let cacheKey = "team_catalog_cache"
    private let cacheTimestampKey = "team_catalog_timestamp"
    
    /// Cache TTL: 24 hours
    private let cacheTTL: TimeInterval = 86_400
    
    // MARK: - Init
    
    private init() {
        // Start with embedded fallback immediately so UI is never empty
        teams = Self.embeddedCatalog
    }
    
    // MARK: - Public API
    
    /// Load catalog: try remote → cached → bundled JSON → embedded
    func loadCatalog() async {
        isLoading = true
        defer { isLoading = false }
        
        // 1) Try remote
        if let remote = await fetchRemote() {
            teams = remote
            saveToDiskCache(remote)
            lastUpdated = Date()
            Logger.info("[TeamCatalog] Loaded \(remote.count) teams from remote")
            return
        }
        
        // 2) Try disk cache
        if let cached = loadFromDiskCache() {
            teams = cached.teams
            lastUpdated = cached.timestamp
            Logger.info("[TeamCatalog] Loaded \(cached.teams.count) teams from cache")
            return
        }
        
        // 3) Try bundled JSON catalog (team_catalog.json)
        if let bundled = TeamCatalogLoader.shared.loadFromBundle() {
            teams = bundled
            lastUpdated = bundled.first?.lastUpdated
            Logger.info("[TeamCatalog] Loaded \(bundled.count) teams from bundled JSON")
            return
        }
        
        // 4) Embedded fallback (already set in init)
        lastUpdated = nil
        Logger.info("[TeamCatalog] Using embedded catalog (\(teams.count) teams)")
    }
    
    /// Get teams by championship
    func teams(for championship: Championship) -> [RacingTeam] {
        teams.filter { $0.championship == championship && $0.isActive }
    }
    
    /// Find team by ID
    func team(byId id: String) -> RacingTeam? {
        teams.first { $0.id == id }
    }
    
    // MARK: - Remote Fetch
    
    private func fetchRemote() async -> [RacingTeam]? {
        guard let url = remoteURL else { return nil }
        
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            request.cachePolicy = .reloadIgnoringLocalCacheData
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                Logger.warning("[TeamCatalog] Remote returned non-200 status")
                return nil
            }
            
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode([RacingTeam].self, from: data)
        } catch {
            Logger.warning("[TeamCatalog] Remote fetch failed: \(error.localizedDescription)")
            return nil
        }
    }
    
    // MARK: - Disk Cache
    
    private func saveToDiskCache(_ teams: [RacingTeam]) {
        do {
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .iso8601
            let data = try encoder.encode(teams)
            UserDefaults.standard.set(data, forKey: cacheKey)
            UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: cacheTimestampKey)
        } catch {
            Logger.error("[TeamCatalog] Cache save failed: \(error)")
        }
    }
    
    private func loadFromDiskCache() -> (teams: [RacingTeam], timestamp: Date)? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else { return nil }
        
        let timestamp = UserDefaults.standard.double(forKey: cacheTimestampKey)
        let cacheDate = Date(timeIntervalSince1970: timestamp)
        
        // Check TTL
        guard Date().timeIntervalSince(cacheDate) < cacheTTL else {
            Logger.debug("[TeamCatalog] Cache expired")
            return nil
        }
        
        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let teams = try decoder.decode([RacingTeam].self, from: data)
            return (teams, cacheDate)
        } catch {
            Logger.error("[TeamCatalog] Cache decode failed: \(error)")
            return nil
        }
    }
    
    // MARK: - Embedded Fallback (2026 Season)
    
    // swiftlint:disable function_body_length
    static let embeddedCatalog: [RacingTeam] = {
        let now = Date()
        return [
            // ─────────────────── F1 2026 ───────────────────
            RacingTeam(
                id: "f1_alpine", name: "Alpine", championship: .f1,
                shortName: "ALP", primaryColor: "#0093CC", secondaryColor: "#FF87BC",
                logo: "logo_alpine", logoRemoteUrl: nil, fallbackIcon: "mountain.2.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_aston_martin", name: "Aston Martin", championship: .f1,
                shortName: "AMR", primaryColor: "#006F62", secondaryColor: "#CEDC00",
                logo: "logo_aston_martin", logoRemoteUrl: nil, fallbackIcon: "leaf.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_audi", name: "Audi", championship: .f1,
                shortName: "AUD", primaryColor: "#0F0F0F", secondaryColor: "#E10600",
                logo: "logo_audi", logoRemoteUrl: nil, fallbackIcon: "circle.grid.2x2.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_cadillac", name: "Cadillac", championship: .f1,
                shortName: "CAD", primaryColor: "#1A1A2E", secondaryColor: "#D4AF37",
                logo: "logo_cadillac", logoRemoteUrl: nil, fallbackIcon: "shield.lefthalf.filled",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_ferrari", name: "Ferrari", championship: .f1,
                shortName: "FER", primaryColor: "#DC0000", secondaryColor: "#FFF200",
                logo: "logo_ferrari", logoRemoteUrl: nil, fallbackIcon: "car.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_haas", name: "Haas F1 Team", championship: .f1,
                shortName: "HAA", primaryColor: "#B6BABD", secondaryColor: "#E6002B",
                logo: "logo_haas", logoRemoteUrl: nil, fallbackIcon: "wrench.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_mclaren", name: "McLaren", championship: .f1,
                shortName: "MCL", primaryColor: "#FF8700", secondaryColor: "#47C7FC",
                logo: "logo_mclaren", logoRemoteUrl: nil, fallbackIcon: "flame.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_mercedes", name: "Mercedes", championship: .f1,
                shortName: "MER", primaryColor: "#27F4D2", secondaryColor: "#000000",
                logo: "logo_mercedes", logoRemoteUrl: nil, fallbackIcon: "star.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_racing_bulls", name: "Racing Bulls", championship: .f1,
                shortName: "RCB", primaryColor: "#2B4562", secondaryColor: "#FFFFFF",
                logo: "logo_racing_bulls", logoRemoteUrl: nil, fallbackIcon: "hare.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_red_bull", name: "Red Bull Racing", championship: .f1,
                shortName: "RBR", primaryColor: "#3671C6", secondaryColor: "#FCD700",
                logo: "logo_red_bull", logoRemoteUrl: nil, fallbackIcon: "bolt.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "f1_williams", name: "Williams", championship: .f1,
                shortName: "WIL", primaryColor: "#64C4FF", secondaryColor: "#005AFF",
                logo: "logo_williams", logoRemoteUrl: nil, fallbackIcon: "shield.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            
            // ─────────────────── MotoGP 2026 ───────────────────
            RacingTeam(
                id: "motogp_aprilia", name: "Aprilia Racing", championship: .motogp,
                shortName: "APR", primaryColor: "#000000", secondaryColor: "#E10600",
                logo: "logo_aprilia", logoRemoteUrl: nil, fallbackIcon: "a.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_ducati_factory", name: "Ducati Team", championship: .motogp,
                shortName: "DUC", primaryColor: "#CC0000", secondaryColor: "#FFFFFF",
                logo: "logo_ducati", logoRemoteUrl: nil, fallbackIcon: "bolt.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_gresini", name: "Gresini Racing", championship: .motogp,
                shortName: "GRE", primaryColor: "#0046AD", secondaryColor: "#E10600",
                logo: "logo_gresini", logoRemoteUrl: nil, fallbackIcon: "flag.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_honda_hrc", name: "Honda HRC", championship: .motogp,
                shortName: "HON", primaryColor: "#CC0000", secondaryColor: "#003DA5",
                logo: "logo_honda", logoRemoteUrl: nil, fallbackIcon: "circle.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_pramac", name: "Pramac Racing", championship: .motogp,
                shortName: "PRA", primaryColor: "#7B2D8E", secondaryColor: "#1E90FF",
                logo: "logo_pramac", logoRemoteUrl: nil, fallbackIcon: "p.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_ktm_factory", name: "Red Bull KTM Factory Racing", championship: .motogp,
                shortName: "KTM", primaryColor: "#FF6600", secondaryColor: "#000000",
                logo: "logo_ktm", logoRemoteUrl: nil, fallbackIcon: "flame.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_lcr_honda", name: "LCR Honda", championship: .motogp,
                shortName: "LCR", primaryColor: "#006400", secondaryColor: "#CC0000",
                logo: "logo_lcr", logoRemoteUrl: nil, fallbackIcon: "l.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_vr46", name: "VR46 Racing Team", championship: .motogp,
                shortName: "VR46", primaryColor: "#FFDD00", secondaryColor: "#000000",
                logo: "logo_vr46", logoRemoteUrl: nil, fallbackIcon: "46.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_ktm_tech3", name: "Red Bull KTM Tech3", championship: .motogp,
                shortName: "TE3", primaryColor: "#FF6600", secondaryColor: "#1E3A5F",
                logo: "logo_tech3", logoRemoteUrl: nil, fallbackIcon: "3.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_trackhouse", name: "Trackhouse Racing", championship: .motogp,
                shortName: "TRK", primaryColor: "#5B2C6F", secondaryColor: "#FF4500",
                logo: "logo_trackhouse", logoRemoteUrl: nil, fallbackIcon: "t.circle.fill",
                isActive: true, season: 2026, lastUpdated: now
            ),
            RacingTeam(
                id: "motogp_yamaha_factory", name: "Yamaha Factory Racing", championship: .motogp,
                shortName: "YAM", primaryColor: "#0041C4", secondaryColor: "#FFFFFF",
                logo: "logo_yamaha", logoRemoteUrl: nil, fallbackIcon: "tuningfork",
                isActive: true, season: 2026, lastUpdated: now
            ),
        ]
    }()
    // swiftlint:enable function_body_length
}
