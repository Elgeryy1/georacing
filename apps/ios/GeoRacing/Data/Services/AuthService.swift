import Foundation
import SwiftUI
import Combine

#if canImport(FirebaseAuth)
import FirebaseAuth
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

class AuthService: ObservableObject {
    static let shared = AuthService()
    
    @Published var currentUser: AppUser?
    @Published var isAuthenticated = false
    
    private init() {
        #if canImport(FirebaseAuth)
        // Check existing session
        if let firebaseUser = Auth.auth().currentUser {
            self.mapFirebaseUser(firebaseUser)
        }
        #endif
    }
    
    @MainActor
    func signInWithGoogle() async throws {
        #if canImport(FirebaseAuth) && canImport(GoogleSignIn)
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            return
        }
        
        // 1. Google Sign In
        let gidResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        
        guard let idToken = gidResult.user.idToken?.tokenString else {
            throw customError("Could not get ID Token from Google")
        }
        let accessToken = gidResult.user.accessToken.tokenString
        
        // 2. Firebase Credential
        let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                                                       accessToken: accessToken)
        
        // 3. Firebase Auth
        let authResult = try await Auth.auth().signIn(with: credential)
        
        // 4. Update State & Sync
        let user = self.mapFirebaseUser(authResult.user)
        
        // 5. Sync to Backend (Async, don't block UI)
        Task {
            do {
                try await UserProfileRepository.shared.syncUser(user)
                Logger.info("[AuthService] User Synced to Backend")
            } catch {
                Logger.error("[AuthService] User Sync Failed: \(error)")
            }
        }
        #else
        // Stub Implementation for Dev/Demo when pods are missing
        Logger.warning("[AuthService] Dependencies missing. Using Stub Auth.")
        try await Task.sleep(nanoseconds: 1 * 1_000_000_000) // Simulate delay
        let stubUser = AppUser(uid: "stub_123", email: "demo@georacing.com", displayName: "Demo Driver", photoURL: nil)
        self.currentUser = stubUser
        self.isAuthenticated = true
        #endif
    }
    
    func signOut() {
        #if canImport(FirebaseAuth)
        do {
            try Auth.auth().signOut()
            self.currentUser = nil
            self.isAuthenticated = false
        } catch {
            Logger.error("Error signing out: \(error)")
        }
        #else
        self.currentUser = nil
        self.isAuthenticated = false
        #endif
    }
    
    #if canImport(FirebaseAuth)
    @discardableResult
    private func mapFirebaseUser(_ firebaseUser: FirebaseAuth.User) -> AppUser {
        let user = AppUser(
            uid: firebaseUser.uid,
            email: firebaseUser.email ?? "",
            displayName: firebaseUser.displayName,
            photoURL: firebaseUser.photoURL?.absoluteString
        )
        
        Task { @MainActor in
            self.currentUser = user
            self.isAuthenticated = true
        }
        return user
    }
    #endif
    
    private func customError(_ msg: String) -> NSError {
        NSError(domain: "AuthService", code: -1, userInfo: [NSLocalizedDescriptionKey: msg])
    }
}
