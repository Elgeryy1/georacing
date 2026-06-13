import SwiftUI

struct ContextualCardView: View {
    @StateObject private var viewModel = ContextualCardViewModel()
    
    var body: some View {
        VStack {
            activeWidgetView
                .animation(.spring(response: 0.5, dampingFraction: 0.8), value: viewModel.activeWidget)
                .id(caseName(for: viewModel.activeWidget)) // Force transition on type change
                .accessibilityElement(children: .contain)
                .accessibilityLabel("Circuit status card")
        }
        .padding()
    }
    
    @ViewBuilder
    private var activeWidgetView: some View {
        switch viewModel.activeWidget {
        case .emergency(let mode):
            EmergencyWidget(mode: mode)
                .transition(.asymmetric(insertion: .scale.combined(with: .opacity), removal: .opacity))
        case .offline(let date):
            OfflineWidget(lastUpdated: date)
                .transition(.opacity)
        case .racePositions(let lap, let total):
            RacePositionsWidget(currentLap: lap, totalLaps: total)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        case .circuitStatus(let mode):
            CircuitStatusWidget(mode: mode)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        case .routeGuidance(let target, let eta, let instruction, let badge):
            RouteGuidanceWidget(target: target, eta: eta, instruction: instruction, badge: badge)
                .transition(.slide)
        }
    }
    
    // Helper to identify unique widget types for transitions
    private func caseName(for widget: WidgetType) -> String {
        switch widget {
        case .emergency: return "emergency"
        case .offline: return "offline"
        case .racePositions: return "racePositions"
        case .circuitStatus: return "circuitStatus"
        case .routeGuidance: return "routeGuidance"
        }
    }
}

// MARK: - Previews

struct ContextualCardView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            // Standard Preview
            ZStack {
                GeoToken.background.ignoresSafeArea()
                ContextualCardView()
            }
            .frame(height: 200)
            
            Divider()
            
            // Simulation Preview
            SimulationView()
                .frame(height: 200)
        }
        .background(Color.gray)
    }
    
    struct SimulationView: View {
        @StateObject var vm = ContextualCardViewModel()
        
        var body: some View {
            ZStack {
                GeoToken.background.ignoresSafeArea()
                
                VStack {
                    Spacer()
                    
                    // Render the widget manually using the VM to test transitions
                    widgetView(for: vm.activeWidget)
                        .animation(.spring(), value: vm.activeWidget)
                        .padding()
                    
                    Spacer()
                    
                    Button("Start Simulation Loop") {
                        vm.simulateChanges()
                    }
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                    
                    Button("Stop") {
                        vm.stopSimulation()
                    }
                    .padding(.bottom)
                }
            }
        }
        
        @ViewBuilder
        func widgetView(for widget: WidgetType) -> some View {
            switch widget {
            case .emergency(let mode):
                EmergencyWidget(mode: mode)
            case .offline(let date):
                OfflineWidget(lastUpdated: date)
            case .racePositions(let lap, let total):
                RacePositionsWidget(currentLap: lap, totalLaps: total)
            case .circuitStatus(let mode):
                CircuitStatusWidget(mode: mode)
            case .routeGuidance(let target, let eta, let instruction, let badge):
                RouteGuidanceWidget(target: target, eta: eta, instruction: instruction, badge: badge)
            }
        }
    }
}
