import SwiftUI

struct ParkingContainerView: View {
    @StateObject private var viewModel = ParkingViewModel()
    
    var body: some View {
        NavigationStack(path: $viewModel.navigationPath) {
            ParkingHomeView(viewModel: viewModel)
                .navigationDestination(for: ParkingRoute.self) { route in
                    switch route {
                    case .wizardStep1:
                        ParkingWizardStep1View(viewModel: viewModel)
                    case .wizardStep2:
                        ParkingWizardStep2View(viewModel: viewModel)
                    case .wizardStep3:
                        ParkingWizardStep3View(viewModel: viewModel)
                    case .wizardStep4:
                        ParkingWizardStep4View(viewModel: viewModel)
                    case .assignmentDetail:
                        ParkingAssignmentDetailView(viewModel: viewModel)
                    case .navigation:
                        ParkingNavigationView(viewModel: viewModel)
                    case .support:
                        ParkingSupportView()
                    }
                }
        }
    }
}
