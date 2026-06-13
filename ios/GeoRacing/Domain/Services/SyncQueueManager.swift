import Foundation
import Network

/// Payload wrapper for storing failed DatabaseClient requests temporarily in User Defaults.
struct SyncPayload: Codable, Identifiable {
    var id: UUID = UUID()
    let table: String
    let dataData: Data // Since [String: Any] doesn't conform to Codable easily, we store the raw JSON payload
    let timestamp: Date
}

/// Resiliencia Offline Real (Sync Queue)
/// Monitors network state via NWPathMonitor.
/// Queues API payloads when offline and flushes them sequentially when connection is restored.
/// Operates on a singleton background actor to prevent data races.
actor SyncQueueManager {
    static let shared = SyncQueueManager()
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.georacing.syncqueue")
    private let defaultsKey = "offline_sync_queue"
    
    // Concurrency safe tracking
    var isOnline: Bool = true
    private var isFlushing: Bool = false
    
    // Expose a stream or published property if needed, but for now we interact directly
    private init() {
        startMonitoring()
    }
    
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            guard let self = self else { return }
            let online = path.status == .satisfied
            
            Task {
                await self.updateStatus(online)
            }
        }
        monitor.start(queue: queue)
    }
    
    private func updateStatus(_ online: Bool) async {
        let statusChanged = (self.isOnline != online)
        self.isOnline = online
        
        if online {
            Logger.info("[SyncQueueManager] Network Restored. Initiating flush...")
            await flushQueue()
        } else if statusChanged {
            Logger.info("[SyncQueueManager] Network Lost. Entering offline mode.")
        }
    }
    
    /// Enqueues a failed API upsert payload. Converts the raw AnyEncodable dictionary back to Data for stablility.
    func enqueue(table: String, rawBody: Data) {
        let payload = SyncPayload(table: table, dataData: rawBody, timestamp: Date())
        var currentQueue = getStoredQueue()
        currentQueue.append(payload)
        saveQueue(currentQueue)
        
        Logger.info("[SyncQueueManager] Enqueued \(table) payload for later. Queue size: \(currentQueue.count)")
    }
    
    /// Attempts to send all pending payloads to the server sequentially.
    private func flushQueue() async {
        guard !isFlushing else { return }
        isFlushing = true
        defer { isFlushing = false }
        
        var currentQueue = getStoredQueue()
        guard !currentQueue.isEmpty else { return }
        
        Logger.info("[SyncQueueManager] Flushing \(currentQueue.count) pending payloads...")
        
        var remainingQueue: [SyncPayload] = []
        
        for payload in currentQueue {
            do {
                // We recreate the HTTP Request directly to avoid circular dependency with DatabaseClient
                guard let url = URL(string: "\(AppConstants.apiBaseUrl)/_upsert") else { continue }
                var request = URLRequest(url: url)
                request.httpMethod = "POST"
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = payload.dataData
                
                let (_, response) = try await URLSession.shared.data(for: request)
                if let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) {
                    Logger.info("[SyncQueueManager] Successfully flushed payload \(payload.id) to table ^\(payload.table)")
                } else {
                    Logger.error("[SyncQueueManager] Flush failed (Server error) for payload \(payload.id) - keeping in queue.")
                    remainingQueue.append(payload)
                }
            } catch {
                Logger.error("[SyncQueueManager] Flush failed (Network error) for payload \(payload.id) - \(error.localizedDescription)")
                remainingQueue.append(payload)
                // If it's a network error, stop flushing to save battery/bandwidth until the connection stabilizes again
                break
            }
        }
        
        // Save whatever couldn't be uploaded back to UserDefaults
        saveQueue(remainingQueue)
    }
    
    // MARK: - Local Storage Helpers
    
    private func getStoredQueue() -> [SyncPayload] {
        guard let data = UserDefaults.standard.data(forKey: defaultsKey) else { return [] }
        do {
            return try JSONDecoder().decode([SyncPayload].self, from: data)
        } catch {
            Logger.error("[SyncQueueManager] Failed to decode sync queue: \(error)")
            return []
        }
    }
    
    private func saveQueue(_ queue: [SyncPayload]) {
        do {
            let data = try JSONEncoder().encode(queue)
            UserDefaults.standard.set(data, forKey: defaultsKey)
        } catch {
            Logger.error("[SyncQueueManager] Failed to encode sync queue: \(error)")
        }
    }
}
