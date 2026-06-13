import Foundation
import SwiftUI
import Combine

// MARK: - Team Asset Manager

/// Downloads, caches, and serves team logos.
/// Pipeline: Memory → Disk → Remote → Local Asset → SF Symbol Fallback
@MainActor
final class TeamAssetManager: ObservableObject {
    
    static let shared = TeamAssetManager()
    
    // MARK: - Config
    
    /// Disk cache TTL (30 days)
    private let cacheTTL: TimeInterval = 30 * 24 * 3600
    
    /// In-memory cache (team ID → UIImage)
    private var memoryCache: [String: UIImage] = [:]
    
    /// Active download tasks (prevent duplicates)
    private var activeTasks: Set<String> = []
    
    /// Disk cache directory
    private lazy var cacheDirectory: URL = {
        let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("TeamLogos", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()
    
    private init() {}
    
    // MARK: - Public API
    
    /// Get logo image for a team. Returns cached or downloads.
    func logo(for team: RacingTeam) -> UIImage? {
        // 1) Memory cache
        if let cached = memoryCache[team.id] {
            return cached
        }
        
        // 2) Disk cache
        if let diskImage = loadFromDisk(teamId: team.id) {
            memoryCache[team.id] = diskImage
            return diskImage
        }
        
        // 3) Local asset bundle
        if let bundled = UIImage(named: team.logo) {
            memoryCache[team.id] = bundled
            return bundled
        }
        
        // 4) Trigger async download if remote URL exists
        if team.logoRemoteUrl != nil && !activeTasks.contains(team.id) {
            Task { await downloadLogo(for: team) }
        }
        
        return nil
    }
    
    /// Pre-load logos for a set of teams
    func preloadLogos(for teams: [RacingTeam]) async {
        await withTaskGroup(of: Void.self) { group in
            for team in teams {
                if memoryCache[team.id] == nil && team.logoRemoteUrl != nil {
                    group.addTask { [weak self] in
                        await self?.downloadLogo(for: team)
                    }
                }
            }
        }
    }
    
    /// Clear all caches
    func clearCache() {
        memoryCache.removeAll()
        try? FileManager.default.removeItem(at: cacheDirectory)
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        Logger.info("[TeamAssets] Cache cleared")
    }
    
    // MARK: - Download
    
    private func downloadLogo(for team: RacingTeam) async {
        guard let urlString = team.logoRemoteUrl,
              let url = URL(string: urlString),
              url.scheme == "https" else { return }
        
        guard !activeTasks.contains(team.id) else { return }
        activeTasks.insert(team.id)
        defer { activeTasks.remove(team.id) }
        
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 15
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode),
                  let image = UIImage(data: data) else {
                Logger.warning("[TeamAssets] Download failed for \(team.id)")
                return
            }
            
            // Save to memory + disk
            memoryCache[team.id] = image
            saveToDisk(data: data, teamId: team.id)
            
            Logger.debug("[TeamAssets] Downloaded logo for \(team.id)")
        } catch {
            Logger.warning("[TeamAssets] Download error for \(team.id): \(error.localizedDescription)")
        }
    }
    
    // MARK: - Disk Cache
    
    private func diskCachePath(teamId: String) -> URL {
        cacheDirectory.appendingPathComponent("\(teamId).png")
    }
    
    private func saveToDisk(data: Data, teamId: String) {
        let path = diskCachePath(teamId: teamId)
        do {
            try data.write(to: path)
        } catch {
            Logger.error("[TeamAssets] Disk save failed: \(error)")
        }
    }
    
    private func loadFromDisk(teamId: String) -> UIImage? {
        let path = diskCachePath(teamId: teamId)
        guard FileManager.default.fileExists(atPath: path.path) else { return nil }
        
        // Check TTL
        if let attrs = try? FileManager.default.attributesOfItem(atPath: path.path),
           let modified = attrs[.modificationDate] as? Date,
           Date().timeIntervalSince(modified) > cacheTTL {
            try? FileManager.default.removeItem(at: path)
            return nil
        }
        
        guard let data = try? Data(contentsOf: path) else { return nil }
        return UIImage(data: data)
    }
}

// MARK: - SwiftUI View: Team Logo

/// Displays a team logo with fallback chain:
/// 1. Local asset catalog (SVG vector) via SwiftUI Image
/// 2. Cached/downloaded image via TeamAssetManager
/// 3. SF Symbol fallback in team color
struct TeamLogoView: View {
    let team: RacingTeam
    let size: CGFloat
    
    @ObservedObject private var assetManager = TeamAssetManager.shared
    
    /// Check if a named image exists in the asset catalog
    private var hasLocalAsset: Bool {
        UIImage(named: team.logo) != nil
    }
    
    var body: some View {
        ZStack {
            if hasLocalAsset {
                // Prefer SwiftUI Image for crisp vector SVG rendering
                Image(team.logo)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(size * 0.1)
            } else if let image = assetManager.logo(for: team) {
                // Downloaded / disk-cached raster image
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(size * 0.1)
            } else {
                // Fallback: SF Symbol in team color circle
                Image(systemName: team.fallbackIcon)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(size * 0.2)
                    .foregroundColor(team.primarySwiftColor)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .background(
            Circle()
                .fill(team.primarySwiftColor.opacity(0.1))
        )
        .overlay(
            Circle()
                .stroke(team.primarySwiftColor.opacity(0.5), lineWidth: 1.5)
        )
    }
}
