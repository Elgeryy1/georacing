import HealthKit
import Combine
import Foundation

/// Integración de Salud Nativa (EcoMeter vía HealthKit)
/// This service provides an offline-first way to measure the user's carbon footprint/activity
/// by reading native step counts without draining the battery using constant background GPS tracking.
/// Handles graceful degradation for devices that do not support HealthKit (like some iPads).
@MainActor
class HealthService: ObservableObject {
    static let shared = HealthService()
    
    @Published var isSupported: Bool = false
    @Published var authorizationStatus: HKAuthorizationStatus = .notDetermined
    @Published var currentEcoPoints: Double = 0.0
    @Published var todaySteps: Double = 0.0
    
    private var healthStore: HKHealthStore?
    
    // Constant: 1 step = 0.5 EcoPoints
    private let stepsToEcoPointsMultiplier: Double = 0.5
    
    private init() {
        if HKHealthStore.isHealthDataAvailable() {
            self.healthStore = HKHealthStore()
            self.isSupported = true
        } else {
            self.isSupported = false
            Logger.info("[HealthService] HealthKit is not supported on this device. Graceful degradation active.")
        }
    }
    
    /// Requests user authorization to read step count data from HealthKit.
    func requestAuthorization() async throws {
        guard let healthStore = healthStore, isSupported else {
            throw HealthError.notSupported
        }
        
        guard let stepType = HKObjectType.quantityType(forIdentifier: .stepCount) else {
            throw HealthError.typeNotAvailable
        }
        
        let typesToRead: Set<HKObjectType> = [stepType]
        
        do {
            try await healthStore.requestAuthorization(toShare: [], read: typesToRead)
            self.authorizationStatus = healthStore.authorizationStatus(for: stepType)
            
            if self.authorizationStatus == .sharingAuthorized {
                await fetchTodaySteps()
            }
        } catch {
            Logger.error("[HealthService] Failed to request authorization: \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Asynchronously fetches the total steps taken today (since 00:00).
    func fetchTodaySteps() async {
        guard let healthStore = healthStore, isSupported else { return }
        
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return }
        
        let calendar = Calendar.current
        let now = Date()
        let startOfDay = calendar.startOfDay(for: now)
        
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)
        
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: stepType,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { [weak self] _, result, error in
                guard let self = self else {
                    continuation.resume()
                    return
                }
                
                if let error = error {
                    Logger.error("[HealthService] Failed to fetch steps: \(error.localizedDescription)")
                    continuation.resume()
                    return
                }
                
                var steps = 0.0
                if let sum = result?.sumQuantity() {
                    steps = sum.doubleValue(for: HKUnit.count())
                }
                
                Task { @MainActor in
                    self.todaySteps = steps
                    self.currentEcoPoints = steps * self.stepsToEcoPointsMultiplier
                    Logger.info("[HealthService] Fetched \(Int(steps)) steps. EcoPoints: \(self.currentEcoPoints)")
                    continuation.resume()
                }
            }
            
            healthStore.execute(query)
        }
    }
    
    enum HealthError: Error, LocalizedError {
        case notSupported
        case typeNotAvailable
        
        var errorDescription: String? {
            switch self {
            case .notSupported:
                return "HealthKit is not supported on this device."
            case .typeNotAvailable:
                return "The required health data type is not available."
            }
        }
    }
}
