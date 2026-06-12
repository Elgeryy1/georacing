import Foundation
import Combine
// Apps usually share module, so APIService should be available.
// But we used IncidentReportDto which is defined in APIService file. 
// Ideally DTOs should be in Domain or Data/Models. 
// For now, if APIService.swift is target member, it's fine.


@MainActor
class IncidentViewModel: ObservableObject {
    
    @Published var description: String = ""
    @Published var selectedCategory: IncidentCategory = .medical
    @Published var isSubmitting = false
    @Published var submissionError: String?
    @Published var submissionSuccess = false
    
    private let beaconScanner: BeaconScanner
    
    init(beaconScanner: BeaconScanner? = nil) {
        self.beaconScanner = beaconScanner ?? BeaconScanner.shared
    }
    
    func submit() {
        guard !description.isEmpty else {
            submissionError = LocalizationUtils.string("Description cannot be empty.")
            return
        }
        
        guard let _ = AuthService.shared.currentUser else {
             submissionError = LocalizationUtils.string("You must be logged in to report.")
             return
        }
        
        isSubmitting = true
        submissionError = nil
        
        // Use current beacon if available
        let beaconId = beaconScanner.currentBeacon?.id
        
        let report = IncidentReportDto(
            category: selectedCategory.rawValue,
            description: description,
            beacon_id: beaconId,
            zone: nil,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        Task {
            do {
                try await APIService.shared.sendIncident(report)
                self.isSubmitting = false
                self.submissionSuccess = true
                self.description = ""
            } catch {
                self.isSubmitting = false
                self.submissionError = LocalizationUtils.string("Failed to submit: \(error.localizedDescription)")
            }
        }
    }
}
