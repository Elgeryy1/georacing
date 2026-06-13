import Foundation

// MARK: - Team Catalog Loader

/// Loads team catalog from:
/// 1. Remote API (optional)
/// 2. Bundled team_catalog.json (offline-first fallback)
///
/// Works in conjunction with TeamCatalogService.
/// The embedded Swift array in TeamCatalogService is the ultimate fallback;
/// this loader provides a JSON-driven alternative that's easier to update.
@MainActor
final class TeamCatalogLoader {
    
    static let shared = TeamCatalogLoader()
    
    private init() {}
    
    // MARK: - JSON Model (matches team_catalog.json)
    
    private struct CatalogFile: Codable {
        let version: String
        let season: Int
        let lastUpdated: String
        let teams: [TeamEntry]
    }
    
    private struct TeamEntry: Codable {
        let id: String
        let name: String
        let championship: String
        let shortName: String
        let primaryColor: String
        let secondaryColor: String
        let logo: String
        let logoRemoteUrl: String?
        let fallbackIcon: String
        let isActive: Bool
        let season: Int
        let concept: String?
    }
    
    // MARK: - Public API
    
    /// Load teams from the bundled team_catalog.json file.
    /// Returns nil if file not found or parsing fails.
    func loadFromBundle() -> [RacingTeam]? {
        guard let url = Bundle.main.url(forResource: "team_catalog", withExtension: "json") else {
            Logger.warning("[TeamCatalogLoader] team_catalog.json not found in bundle")
            return nil
        }
        
        do {
            let data = try Data(contentsOf: url)
            let catalog = try JSONDecoder().decode(CatalogFile.self, from: data)
            
            let teams = catalog.teams.compactMap { entry -> RacingTeam? in
                guard let championship = Championship(rawValue: entry.championship) else {
                    Logger.warning("[TeamCatalogLoader] Unknown championship: \(entry.championship)")
                    return nil
                }
                
                return RacingTeam(
                    id: entry.id,
                    name: entry.name,
                    championship: championship,
                    shortName: entry.shortName,
                    primaryColor: entry.primaryColor,
                    secondaryColor: entry.secondaryColor,
                    logo: entry.logo,
                    logoRemoteUrl: entry.logoRemoteUrl,
                    fallbackIcon: entry.fallbackIcon,
                    isActive: entry.isActive,
                    season: entry.season,
                    lastUpdated: ISO8601DateFormatter().date(from: catalog.lastUpdated) ?? Date()
                )
            }
            
            Logger.info("[TeamCatalogLoader] Loaded \(teams.count) teams from bundle JSON")
            return teams
        } catch {
            Logger.error("[TeamCatalogLoader] Failed to parse team_catalog.json: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Load from a remote URL (for future OTA catalog updates)
    func loadFromRemote(url: URL) async -> [RacingTeam]? {
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let http = response as? HTTPURLResponse,
                  (200...299).contains(http.statusCode) else {
                return nil
            }
            
            let catalog = try JSONDecoder().decode(CatalogFile.self, from: data)
            
            return catalog.teams.compactMap { entry -> RacingTeam? in
                guard let championship = Championship(rawValue: entry.championship) else { return nil }
                return RacingTeam(
                    id: entry.id,
                    name: entry.name,
                    championship: championship,
                    shortName: entry.shortName,
                    primaryColor: entry.primaryColor,
                    secondaryColor: entry.secondaryColor,
                    logo: entry.logo,
                    logoRemoteUrl: entry.logoRemoteUrl,
                    fallbackIcon: entry.fallbackIcon,
                    isActive: entry.isActive,
                    season: entry.season,
                    lastUpdated: ISO8601DateFormatter().date(from: catalog.lastUpdated) ?? Date()
                )
            }
        } catch {
            Logger.warning("[TeamCatalogLoader] Remote catalog fetch failed: \(error.localizedDescription)")
            return nil
        }
    }
}
