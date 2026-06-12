import SwiftUI
import Combine

/// Protocolo de Supervivencia (Energy Shedding)
/// Service responsible for monitoring battery health.
/// When battery drops below 30% and is not charging, we activate a "Survival Mode"
/// to aggressively shed non-critical features and ensure the user's phone survives the event.
@MainActor
class EnergyManagementService: ObservableObject {
    static let shared = EnergyManagementService()
    
    @Published var isSurvivalMode: Bool = false
    @Published var batteryLevel: Float = 1.0
    @Published var batteryState: UIDevice.BatteryState = .unknown
    
    private let survivalThreshold: Float = 0.30 // 30% battery threshold
    private var observers: Set<AnyCancellable> = []
    
    private init() {
        startMonitoring()
    }
    
    private func startMonitoring() {
        // Enable battery monitoring on UIDevice
        UIDevice.current.isBatteryMonitoringEnabled = true
        
        // Initial setup
        updateBatteryStatus()
        
        // Listen to level changes
        NotificationCenter.default.publisher(for: UIDevice.batteryLevelDidChangeNotification)
            .sink { [weak self] _ in self?.updateBatteryStatus() }
            .store(in: &observers)
        
        // Listen to state changes (charging vs unplugged)
        NotificationCenter.default.publisher(for: UIDevice.batteryStateDidChangeNotification)
            .sink { [weak self] _ in self?.updateBatteryStatus() }
            .store(in: &observers)
    }
    
    private func updateBatteryStatus() {
        self.batteryLevel = UIDevice.current.batteryLevel
        self.batteryState = UIDevice.current.batteryState
        
        // Gracefully handle simulator where batteryLevel might be -1.0
        if batteryLevel < 0 {
            // Simulator or unknown
            self.isSurvivalMode = false
            return
        }
        
        // Survival mode logic
        let isLowBattery = batteryLevel <= survivalThreshold
        let isNotCharging = batteryState == .unplugged || batteryState == .unknown
        
        let newSurvivalStatus = isLowBattery && isNotCharging
        
        if self.isSurvivalMode != newSurvivalStatus {
            self.isSurvivalMode = newSurvivalStatus
            if newSurvivalStatus {
                Logger.info("[EnergyManagement] Activated Survival Mode! Battery: \(Int(batteryLevel * 100))%")
            } else {
                Logger.info("[EnergyManagement] Deactivated Survival Mode. Back to normal operations.")
            }
        }
    }
    
    deinit {
        // Standard cleanup although this is a singleton
        UIDevice.current.isBatteryMonitoringEnabled = false
    }
}
