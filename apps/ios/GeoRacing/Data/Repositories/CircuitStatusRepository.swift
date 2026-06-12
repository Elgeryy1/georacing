import Foundation
import Combine

class CircuitStatusRepository: ObservableObject {
    static let shared = CircuitStatusRepository()
    
    @Published var currentStatus: TrackStatus = .green
    @Published var statusMessage: String = ""
    // Removed isEvacuation boolean, now handled by .evacuation case in TrackStatus
    
    private var timer: Timer?
    private let pollInterval: TimeInterval = 5.0
    private let cacheKey = "cached_circuit_state"
    
    struct CachedCircuitState: Codable {
        let status: String
        let message: String
        let timestamp: Date
    }
    
    private init() {
        loadFromCache()
        startPolling()
    }
    
    func startPolling() {
        stopPolling() // Ensure no duplicates
        
        // Initial fetch
        fetchStatus()
        
        // Schedule timer
        timer = Timer.scheduledTimer(withTimeInterval: pollInterval, repeats: true) { [weak self] _ in
            self?.fetchStatus()
        }
    }
    
    func stopPolling() {
        timer?.invalidate()
        timer = nil
    }
    
    private func fetchStatus() {
        Task {
            do {
                // Use APIService to fetch state (Single Source of Truth)
                let (status, message) = try await APIService.shared.fetchCircuitState()
                
                await MainActor.run {
                    // Only print if status actually changes
                    if self.currentStatus != status {
                         Logger.info("[CircuitStatusRepo] Status Changed: \(self.currentStatus) -> \(status)")
                         
                         // NOTIFICATIONS: Alert user of status change
                         // Only notify for "Active" states (Yellow, Red, SC, Evacuation), or all? 
                         // Generally Green->Red needs alert. Red->Green is good news.
                         // Let's notify for ALL changes for parity, unless Android does otherwise.
                         if status != .green && status != .unknown {
                             LocalNotificationManager.shared.sendNotification(
                                 title: LocalizationUtils.string(status.titleKey),
                                 body: message ?? LocalizationUtils.string(status.messageKey)
                             )
                         }
                    }
                    
                    self.currentStatus = status
                    self.statusMessage = message ?? ""
                    
                    // Persist to cache
                    self.saveToCache(status: status, message: message ?? "")
                }
            } catch {
                Logger.error("[CircuitStatusRepo] Polling Error: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Persistence
    
    private func saveToCache(status: TrackStatus, message: String) {
        let cached = CachedCircuitState(status: status.rawValue, message: message, timestamp: Date())
        if let data = try? JSONEncoder().encode(cached) {
            UserDefaults.standard.set(data, forKey: cacheKey)
        }
    }
    
    private func loadFromCache() {
        guard let data = UserDefaults.standard.data(forKey: cacheKey),
              let cached = try? JSONDecoder().decode(CachedCircuitState.self, from: data),
              let status = TrackStatus(rawValue: cached.status) else { return }
        
        self.currentStatus = status
        self.statusMessage = cached.message
        Logger.debug("[CircuitStatusRepo] Loaded cached state: \(status) (\(cached.timestamp))")
    }
    
    // MARK: - Write State
    
    func updateStatus(mode: TrackStatus, message: String) async throws {
        // Fetch existing records
        let records = try? await DatabaseClient.shared.read(table: "circuit_state")
        
        let flagString: String
        switch mode {
        case .green: flagString = "GREEN_FLAG"
        case .yellow: flagString = "YELLOW_FLAG"
        case .red: flagString = "RED_FLAG"
        case .sc: flagString = "SC_DEPLOYED"
        case .vsc: flagString = "VSC_DEPLOYED"
        case .evacuation: flagString = "EVACUATION_MODE"
        case .unknown: flagString = "UNKNOWN"
        }
        
        // Prepare data payload minus ID
        let baseData: [String: Any] = [
            "global_mode": flagString,
            "message": message,
            "updated_at": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        
        if let existingRecords = records, !existingRecords.isEmpty {
            // Update ALL existing records
            for record in existingRecords {
                if let existingId = record["id"] as? String {
                    var data = baseData
                    data["id"] = existingId
                    try await DatabaseClient.shared.upsert(table: "circuit_state", data: data)
                }
            }
        } else {
            // Create new default record
            var data = baseData
            data["id"] = "global_circuit_state"
            try await DatabaseClient.shared.upsert(table: "circuit_state", data: data)
        }
        
        // Optimistic update locally
        await MainActor.run {
            self.currentStatus = mode
            self.statusMessage = message
            
            // Optimistic update locally
            // Note: isEvacuation was removed. If message contains EVACUATION, 
            // the mode passed in should ideally be .evacuation, or we trust the next poll.
            // For now, we rely on the `mode` argument being correct.

        }
    }
    
    private func mapStatus(_ flag: String) -> TrackStatus {
        // Map from DB values (RED_FLAG, GREEN_FLAG, etc)
        let normalized = flag.uppercased()
        
        if normalized.contains("RED") { return .red }
        if normalized.contains("YELLOW") { return .yellow }
        if normalized.contains("GREEN") { return .green }
        // VSC check MUST be before SC check because "VSC" contains "SC"
        if normalized.contains("VSC") { return .vsc }
        if normalized.contains("SC") { return .sc }
        
        return .green
    }
}
