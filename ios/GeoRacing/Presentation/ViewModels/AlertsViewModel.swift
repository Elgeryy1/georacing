import Foundation
import Combine

enum AlertPriority: String, Codable {
    case high, medium, low
}

struct AppAlert: Identifiable, Codable {
    let id: String
    let title: String
    let message: String
    let priority: AlertPriority
    let timestamp: Date
}

@MainActor
class AlertsViewModel: ObservableObject {
    @Published var alerts: [AppAlert] = []
    
    func fetchAlerts() {
        Task {
            do {
                let records = try await DatabaseClient.shared.read(table: "alerts")
                let fetchedAlerts = records.compactMap { dict -> AppAlert? in
                    guard let id = dict["id"] as? String ?? dict["_id"] as? String,
                          let title = dict["title"] as? String,
                          let message = dict["message"] as? String else { return nil }
                    
                    // Priority mapping
                    let priorityString = dict["priority"] as? String ?? "medium"
                    let priority = AlertPriority(rawValue: priorityString.lowercased()) ?? .medium
                    
                    // Timestamp
                    let ts = dict["timestamp"] as? Double ?? Date().timeIntervalSince1970 * 1000
                    let date = Date(timeIntervalSince1970: ts / 1000)
                    
                    return AppAlert(id: id, title: title, message: message, priority: priority, timestamp: date)
                }
                
                await MainActor.run {
                    self.alerts = fetchedAlerts
                    // If empty, keep mock for demo if needed, or show empty state
                    if self.alerts.isEmpty {
                        self.alerts = [
                            AppAlert(id: "mock1", title: "System Check", message: "Alert system is active. No current warnings.", priority: .low, timestamp: Date())
                        ]
                    }
                }
            } catch {
                Logger.error("[Alerts][ERROR] Failed to fetch alerts: \(error)")
                await MainActor.run {
                    // Fallback Mock
                    self.alerts = [
                       AppAlert(id: "err1", title: "Connection Error", message: "Could not fetch live alerts.", priority: .medium, timestamp: Date())
                    ]
                }
            }
        }
    }
    
    func timeAgo(for date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = LocalizationUtils.locale
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}
