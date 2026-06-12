import Foundation
import Combine

class CrowdDensityService: ObservableObject {
    static let shared = CrowdDensityService()
    
    @Published var densities: [String: ZoneDensityDto] = [:]
    
    private var timer: Timer?
    private let apiService = APIService.shared
    
    init() {
        // Start polling
        startPolling()
    }
    
    func startPolling() {
        stopPolling()
        fetchData() // initial fetch
        timer = Timer.scheduledTimer(withTimeInterval: 60.0, repeats: true) { [weak self] _ in
            self?.fetchData()
        }
    }
    
    func stopPolling() {
        timer?.invalidate()
        timer = nil
    }
    
    private func fetchData() {
        Task { [weak self] in
            guard let self else { return }
            do {
                let list = try await self.apiService.fetchZoneDensities()
                await MainActor.run {
                    self.densities = Dictionary(uniqueKeysWithValues: list.map { ($0.zone_id, $0) })
                }
            } catch {
                Logger.error("[CrowdDensityService] Error fetching crowd density: \(error)")
            }
        }
    }
    
    // Logic to find if a better route exists
    func getEfficientRoute(from: String, to: String) -> RouteSuggestion? {
        // MOCK LOGIC: In a real app, this would use a graph.
        // Simplified: Check if "Gate A" is congested, suggest "Gate B"
        
        let destinationDensity = densities[to]
        
        if let density = destinationDensity, density.density_level == "CRITICAL" || density.density_level == "HIGH" {
            // Suggest alternative
            return RouteSuggestion(
                target: to,
                originalEta: "\(density.estimated_wait_minutes + 10) min",
                newEta: "\(density.estimated_wait_minutes / 2) min",
                instruction: "Use alternative Route B to avoid queues",
                timeSaved: "\(density.estimated_wait_minutes / 2)m saved"
            )
        }
        
        return nil
    }
}

struct RouteSuggestion: Equatable {
    let target: String
    let originalEta: String
    let newEta: String
    let instruction: String
    let timeSaved: String
}
