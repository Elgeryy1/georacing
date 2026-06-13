import Foundation

/// Defines a single telemetry event for the Black Box system.
struct TelemetryEvent: Codable {
    let id: UUID
    let timestamp: Date
    let name: String
    let metadata: [String: String]
    
    init(name: String, metadata: [String: String]) {
        self.id = UUID()
        self.timestamp = Date()
        self.name = name
        self.metadata = metadata
    }
}

/// Caja Negra Operativa (Telemetría Local)
/// Lightweight logger designed to save critical ops data locally (battery drops, signal loss).
/// Connects to `SyncQueueManager` to periodically upload chunks of data to a remote `QNAP` server transparently.
@MainActor
class TelemetryLogger {
    static let shared = TelemetryLogger()
    
    private let queue = DispatchQueue(label: "com.georacing.telemetry", qos: .background)
    private let fileName = "blackbox_telemetry.json"
    
    // Batch threshold to flush telemetry to SyncQueueManager
    private let batchThreshold = 20
    private var inMemoryLogs: [TelemetryEvent] = []
    
    private init() {
        loadLocalLogs()
    }
    
    /// Records an event locally. Once threshold is met, it queues an automated upload.
    func logEvent(_ name: String, metadata: [String: String] = [:]) {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            let event = TelemetryEvent(name: name, metadata: metadata)
            self.inMemoryLogs.append(event)
            self.saveLocalLogs()
            
            Logger.info("[TelemetryLogger] Logged Event: \(name)")
            
            // Check if threshold reached to trigger a sync
            if self.inMemoryLogs.count >= self.batchThreshold {
                Task {
                    await self.queueForUpload()
                }
            }
        }
    }
    
    /// Flushes all local events as a single json payload to the SyncQueueManager and clears the local file.
    private func queueForUpload() async {
        let eventsToUpload = inMemoryLogs
        guard !eventsToUpload.isEmpty else { return }
        
        do {
            let data = try JSONEncoder().encode(eventsToUpload)
            // By putting it in `SyncQueueManager`, it guarantees delivery when offline
            await SyncQueueManager.shared.enqueue(table: "telemetry_logs", rawBody: data)
            
            // Clear local logs
            queue.async {
                self.inMemoryLogs.removeAll()
                self.saveLocalLogs()
            }
            Logger.info("[TelemetryLogger] Queued \(eventsToUpload.count) events for server sync.")
        } catch {
            Logger.error("[TelemetryLogger] Failed to encode logs for upload: \(error)")
        }
    }
    
    // MARK: - Local Persistence
    
    private var logFileURL: URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        return paths[0].appendingPathComponent(fileName)
    }
    
    private func saveLocalLogs() {
        do {
            let data = try JSONEncoder().encode(inMemoryLogs)
            try data.write(to: logFileURL, options: .atomic)
        } catch {
            Logger.error("[TelemetryLogger] Failed to write local telemetry file: \(error)")
        }
    }
    
    private func loadLocalLogs() {
        queue.async {
            guard FileManager.default.fileExists(atPath: self.logFileURL.path) else { return }
            do {
                let data = try Data(contentsOf: self.logFileURL)
                let savedLogs = try JSONDecoder().decode([TelemetryEvent].self, from: data)
                self.inMemoryLogs = savedLogs
            } catch {
                Logger.error("[TelemetryLogger] Failed to read local telemetry file: \(error)")
            }
        }
    }
}
