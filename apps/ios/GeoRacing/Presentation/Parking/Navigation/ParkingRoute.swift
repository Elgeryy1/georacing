import Foundation

/// Defines navigation routes for the Parking module.
/// Supports future deep linking integration.
enum ParkingRoute: Hashable {
    case wizardStep1 // License Plate
    case wizardStep2 // Scan Ticket
    case wizardStep3 // Confirm
    case wizardStep4 // Result
    case assignmentDetail
    case navigation
    case support
}
