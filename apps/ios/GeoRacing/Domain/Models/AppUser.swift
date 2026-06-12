import Foundation

struct AppUser: Codable, Identifiable, Sendable {
    let uid: String
    let email: String
    let displayName: String?
    let photoURL: String?
    
    var id: String { uid }
}
