import Foundation

// Product and CartItem are defined in ShopModels.swift and CartManager logic is in Domain/Services/CartManager.swift

// MARK: - Incidents

enum IncidentCategory: String, Codable, CaseIterable {
    case medical = "MEDICAL"
    case security = "SECURITY"
    case cleaning = "CLEANING"
    case maintenance = "MAINTENANCE"
}

struct Incident: Codable, Identifiable {
    let id: String
    let userId: String
    let description: String
    let category: IncidentCategory
    let timestamp: Date
    let photoCount: Int
    
    // Status?
}
