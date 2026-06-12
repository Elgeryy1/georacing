import Foundation
import Combine

public class HybridCircuitStateRepository: ObservableObject {
    public static let shared = HybridCircuitStateRepository()
    
    @Published public var mode: TrackStatus = .green
    @Published public var message: String = ""
    @Published public var updatedAt: String = ""
    
    private var cancellables = Set<AnyCancellable>()
    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.timeStyle = .medium
        formatter.dateStyle = .none
        return formatter
    }()
    
    public init() {
        // Observe the polling repository
        CircuitStatusRepository.shared.$currentStatus
            .combineLatest(CircuitStatusRepository.shared.$statusMessage)
            .receive(on: RunLoop.main)
            .sink { [weak self] (trackStatus, msg) in
                self?.updateState(trackStatus: trackStatus, message: msg)
            }
            .store(in: &cancellables)
    }
    
    public func start() {
        CircuitStatusRepository.shared.startPolling()
    }
    
    private func updateState(trackStatus: TrackStatus, message: String) {
        self.message = message
        self.mode = trackStatus
        updatedAt = Self.formatter.string(from: Date())
    }
    
    /// Resolves the current track status, using a fallback for `.unknown`.
    /// - Parameter fallback: The status to use when `mode` is `.unknown`. Defaults to `.green`.
    /// - Returns: The resolved `TrackStatus`.
    public func resolvedTrackStatus(fallback: TrackStatus = .green) -> TrackStatus {
        switch mode {
        case .evacuation: return .red
        case .unknown: return fallback
        default: return mode
        }
    }
}
