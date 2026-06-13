import Foundation

class UserProfileRepository {
    static let shared = UserProfileRepository()
    
    private init() {}
    
    /// Syncs user data to the backend 'users' table.
    /// Replicates Android's NetworkUserRepository.
    func syncUser(_ user: AppUser) async throws {
        let data: [String: Any] = [
            "uid": user.uid,
            "email": user.email,
            "display_name": user.displayName ?? "",
            "photo_url": user.photoURL ?? "",
            "last_login": Date().timeIntervalSince1970 * 1000 // Android usually sends MS
        ]
        
        try await DatabaseClient.shared.upsert(table: "users", data: data)
    }
}
