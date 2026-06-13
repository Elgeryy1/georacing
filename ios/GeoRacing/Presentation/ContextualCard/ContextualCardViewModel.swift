import SwiftUI
import Combine

enum WidgetType: Equatable {
    case emergency(CircuitMode)
    case offline(Date)
    case racePositions(lap: Int, total: Int)
    case circuitStatus(CircuitMode)
    case routeGuidance(target: String, eta: String, instruction: String, badge: String? = nil)
}

class ContextualCardViewModel: ObservableObject {
    @Published var activeWidget: WidgetType = .circuitStatus(.normal)
    @Published var currentState: ContextState
    
    // Mock simulation timer
    private var timer: AnyCancellable?
    private var cancellables = Set<AnyCancellable>()
    private let densityService = CrowdDensityService.shared
    
    init(initialState: ContextState = .initial) {
        self.currentState = initialState
        self.activeWidget = selectWidget(for: initialState)
        
        // Subscribe to density updates
        densityService.$densities
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.checkForBetterRoutes()
            }
            .store(in: &cancellables)
    }
    
    func updateState(_ newState: ContextState) {
        withAnimation(.easeInOut) {
            self.currentState = newState
            self.activeWidget = selectWidget(for: newState)
        }
    }
    
    private func checkForBetterRoutes() {
        // Example: If user focus is parking or route, check for better path
        if currentState.focus == .route || currentState.focus == .parking {
            // Mock destination "Gate A"
            if let suggestion = densityService.getEfficientRoute(from: "Current Location", to: "Gate A") {
                let newState = ContextState(
                    racePhase: currentState.racePhase,
                    circuitMode: currentState.circuitMode,
                    dataHealth: currentState.dataHealth,
                    userRole: currentState.userRole,
                    focus: currentState.focus,
                    accessibilityEnabled: currentState.accessibilityEnabled,
                    lastUpdated: Date(),
                    routeSuggestion: suggestion
                )
                updateState(newState)
            }
        }
    }
    
    private func selectWidget(for state: ContextState) -> WidgetType {
        // Priority 1: Safety & Emergencies
        if state.circuitMode == .emergency || state.circuitMode == .evacuation {
            return .emergency(state.circuitMode)
        }
        
        // Priority 2: Data Connectivity
        if state.dataHealth == .offline {
            return .offline(state.lastUpdated)
        }
        
        // Priority 3: Anti-Queue / Route Guidance
        if let suggestion = state.routeSuggestion {
            // New "Anti-Queue" widget variant
            return .routeGuidance(target: suggestion.target, eta: suggestion.newEta, instruction: suggestion.instruction, badge: suggestion.timeSaved)
        }
        
        // Priority 3b: Standard Active User Focus
        if state.focus == .route || state.focus == .parking {
            return .routeGuidance(target: "Parking Zone A", eta: "4 min", instruction: "Turn Left at Gate 3", badge: nil)
        }
        
        // Priority 4: Race Context
        switch state.racePhase {
        case .live, .safetyCar, .redFlag:
            // In a real app, we'd check if we have race data
            return .racePositions(lap: 14, total: 56)
        case .formation:
            return .circuitStatus(.normal) // Or specific formation status
        case .pre, .post:
             // Priority 5: Default Environment State
            if state.circuitMode == .congestion {
                return .circuitStatus(.congestion)
            } else if state.circuitMode == .maintenance {
                return .circuitStatus(.maintenance)
            } else {
                return .circuitStatus(.normal)
            }
        }
    }
    
    // DEMO: Function to simulate state changes for preview purposes
    func simulateChanges() {
        timer = Timer.publish(every: 3.0, on: .main, in: .common).autoconnect().sink { [weak self] _ in
            guard let self = self else { return }
            let nextState = self.generateNextDemoState()
            self.updateState(nextState)
        }
    }
    
    func stopSimulation() {
        timer?.cancel()
    }
    
    private func generateNextDemoState() -> ContextState {
        // Simple rotation of states for demo
        switch activeWidget {
        case .circuitStatus:
            return ContextState(racePhase: .live, circuitMode: .normal, dataHealth: .ok, userRole: .fan, focus: .none, accessibilityEnabled: false, lastUpdated: Date(), routeSuggestion: nil)
        case .racePositions:
            return ContextState(racePhase: .live, circuitMode: .emergency, dataHealth: .ok, userRole: .fan, focus: .none, accessibilityEnabled: false, lastUpdated: Date(), routeSuggestion: nil)
        case .emergency:
             return ContextState(racePhase: .live, circuitMode: .normal, dataHealth: .offline, userRole: .fan, focus: .none, accessibilityEnabled: false, lastUpdated: Date(), routeSuggestion: nil)
        case .offline:
            // Simulate Anti-Queue Route finding
             return ContextState(
                racePhase: .post,
                circuitMode: .normal,
                dataHealth: .ok,
                userRole: .fan,
                focus: .route,
                accessibilityEnabled: false,
                lastUpdated: Date(),
                routeSuggestion: RouteSuggestion(target: "Gate A", originalEta: "20m", newEta: "10m", instruction: "Take Fast Track >", timeSaved: "10m saved")
             )
        case .routeGuidance:
             return ContextState.initial
        }
    }
}
