import Foundation

struct ShareSession: Codable, Identifiable, Sendable {
    let id: String // UUID
    let ownerId: String
    let groupId: String
    let expiresAt: Date
    
    var isValid: Bool {
        return Date() < expiresAt
    }
}
