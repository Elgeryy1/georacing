import Foundation
import SwiftUI
import Combine

enum ParkingViewState {
    case idle
    case loading
    case loaded(ParkingAssignment)
    case error(ParkingError)
}

@MainActor
class ParkingViewModel: ObservableObject {
    
    // Dependencies
    private let assignmentService: ParkingAssignmentServiceProtocol
    private let repository: ParkingRepositoryProtocol
    
    // Global Navigation
    @Published var navigationPath = NavigationPath()
    
    // Main State
    @Published var viewState: ParkingViewState = .idle
    
    // Wizard Form Data
    @Published var licensePlateInput: String = ""
    @Published var scannedTicketId: String = ""
    // Estimated arrival removed as per requirements (valid for full event day)
    @Published var wizardStep: Int = 1 // 1..4 internal step tracking for progress bar
    
    // Derived
    var hasActiveAssignment: Bool {
        if case .loaded = viewState { return true }
        return false
    }
    
    init(assignmentService: ParkingAssignmentServiceProtocol = ParkingAssignmentService(),
         repository: ParkingRepositoryProtocol = ParkingRepository()) {
        self.assignmentService = assignmentService
        self.repository = repository
        
        // Initial load
        self.loadAssignment()
    }
    
    // MARK: - Core Logic
    
    func loadAssignment() {
        if let assignment = repository.getAssignment() {
            self.viewState = .loaded(assignment)
        } else {
            self.viewState = .idle
        }
    }
    
    func resetFlow() {
        navigationPath = NavigationPath()
        wizardStep = 1
        licensePlateInput = ""
        scannedTicketId = ""
        viewState = .idle
    }
    
    // MARK: - Wizard Actions
    
    func startWizard() {
        // Reset inputs
        licensePlateInput = ""
        scannedTicketId = ""
        wizardStep = 1
        
        // Navigate to Step 1 (Now Ticket Scan)
        navigationPath.append(ParkingRoute.wizardStep1)
    }
    
    func submitTicketScan() {
        guard !scannedTicketId.isEmpty else { return }
        wizardStep = 2
        // Navigate to Step 2 (Now License Plate)
        navigationPath.append(ParkingRoute.wizardStep2)
    }
    
    func submitLicensePlate() {
        guard validateLicensePlate(licensePlateInput) else {
            return
        }
        wizardStep = 3
        navigationPath.append(ParkingRoute.wizardStep3)
    }
    
    func confirmAssignment() async {
        viewState = .loading
        // Delay simulated
        try? await Task.sleep(nanoseconds: 1 * 1_000_000_000)
        
        do {
            let assignment = try await assignmentService.assignParking(
                licensePlate: licensePlateInput,
                ticketId: scannedTicketId
            )
            repository.saveAssignment(assignment)
            viewState = .loaded(assignment)
            
            // Navigate to Result
            wizardStep = 4
            navigationPath.append(ParkingRoute.wizardStep4)
        } catch let err as ParkingError {
            viewState = .error(err)
        } catch {
            viewState = .error(.unknown)
        }
    }
    
    func finishWizard() {
        // Clear stack to return to Home, which will now show the Assignment card
        navigationPath = NavigationPath()
    }
    
    func clearAssignment() {
        repository.clearAssignment()
        viewState = .idle
        navigationPath = NavigationPath()
    }
    
    // MARK: - Navigation Helpers
    
    func navigateToDetail() {
        navigationPath.append(ParkingRoute.assignmentDetail)
    }
    
    func navigateToNavigation() {
        navigationPath.append(ParkingRoute.navigation)
    }
    
    func navigateToSupport() {
        navigationPath.append(ParkingRoute.support)
    }
    
    // MARK: - Validation
    
    func validateLicensePlate(_ text: String) -> Bool {
        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return cleaned.count >= 4
    }
    
    func simulateScan() {
        self.scannedTicketId = "TICKET-\(Int.random(in: 10000...99999))"
    }
}
