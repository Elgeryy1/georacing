import Foundation

protocol ShareSessionRepositoryProtocol {
    func createSession(ownerId: String, groupId: String) async throws -> ShareSession
    func joinSession(sessionId: String, userId: String) async throws -> String // Returns GroupID
}

class ShareSessionRepository: ShareSessionRepositoryProtocol {
    
    // Mock Database
    private var sessions: [String: ShareSession] = [:]
    
    func createSession(ownerId: String, groupId: String) async throws -> ShareSession {
        let uuid = UUID().uuidString
        // Expires end of day
        let calendar = Calendar.current
        var components = calendar.dateComponents([.year, .month, .day], from: Date())
        components.hour = 23
        components.minute = 59
        components.second = 59
        let expiresAt = calendar.date(from: components) ?? Date().addingTimeInterval(86400)
        
        let session = ShareSession(id: uuid, ownerId: ownerId, groupId: groupId, expiresAt: expiresAt)
        
        // Save to DB (Mock)
        sessions[uuid] = session
        Logger.info("Created session \(uuid) for group \(groupId)")
        
        return session
    }
    
    func joinSession(sessionId: String, userId: String) async throws -> String {
        // 1. Consult UUID
        guard let session = sessions[sessionId] else {
            throw NSError(domain: "ShareSession", code: 404, userInfo: [NSLocalizedDescriptionKey: "Session not found"])
        }
        
        // 2. Validate Expiry
        guard session.isValid else {
            throw NSError(domain: "ShareSession", code: 400, userInfo: [NSLocalizedDescriptionKey: "Session expired"])
        }
        
        // 3. Join (Add user to group members in DB)
        Logger.info("User \(userId) joining group \(session.groupId)")
        
        return session.groupId
    }
}
