import Foundation
import Combine

/// Service that synchronizes state between iPhone and CarPlay
/// Ensures both interfaces show consistent data
class CarPlayStateSync: ObservableObject {
    
    // MARK: - Singleton
    
    static let shared = CarPlayStateSync()
    
    // MARK: - Published State
    
    /// Is CarPlay currently connected?
    @Published private(set) var isCarPlayConnected = false
    
    /// Is navigation active on either device?
    @Published var isNavigating = false
    
    /// Current destination (if navigating)
    @Published var currentDestination: CarPlayDestination?
    
    /// Saved parking location
    @Published var savedParkingLocation: SavedParking?
    
    // MARK: - Private
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    
    private init() {
        setupNotifications()
        loadSavedParking()
    }
    
    // MARK: - Public Methods
    
    /// Save parking location (callable from iPhone or CarPlay)
    func saveParkingLocation(latitude: Double, longitude: Double) {
        let parking = SavedParking(
            latitude: latitude,
            longitude: longitude,
            savedAt: Date()
        )
        
        savedParkingLocation = parking
        
        // Persist
        UserDefaults.standard.set(latitude, forKey: "savedParkingLat")
        UserDefaults.standard.set(longitude, forKey: "savedParkingLon")
        UserDefaults.standard.set(Date(), forKey: "savedParkingDate")
        
        // Notify both interfaces
        NotificationCenter.default.post(name: .parkingLocationSaved, object: parking)
    }
    
    /// Clear saved parking
    func clearSavedParking() {
        savedParkingLocation = nil
        UserDefaults.standard.removeObject(forKey: "savedParkingLat")
        UserDefaults.standard.removeObject(forKey: "savedParkingLon")
        UserDefaults.standard.removeObject(forKey: "savedParkingDate")
    }
    
    /// Start navigation to destination (syncs to both interfaces)
    func startNavigation(to destination: CarPlayDestination) {
        currentDestination = destination
        isNavigating = true
        
        NotificationCenter.default.post(name: .navigationStarted, object: destination)
    }
    
    /// End navigation (syncs to both interfaces)
    func endNavigation() {
        currentDestination = nil
        isNavigating = false
        
        NotificationCenter.default.post(name: .navigationEnded, object: nil)
    }
    
    // MARK: - Private Methods
    
    private func setupNotifications() {
        // Listen for CarPlay connection state
        NotificationCenter.default.publisher(for: .carPlayDidConnect)
            .sink { [weak self] _ in
                self?.isCarPlayConnected = true
            }
            .store(in: &cancellables)
        
        NotificationCenter.default.publisher(for: .carPlayDidDisconnect)
            .sink { [weak self] _ in
                self?.isCarPlayConnected = false
            }
            .store(in: &cancellables)
    }
    
    private func loadSavedParking() {
        guard let lat = UserDefaults.standard.object(forKey: "savedParkingLat") as? Double,
              let lon = UserDefaults.standard.object(forKey: "savedParkingLon") as? Double,
              let date = UserDefaults.standard.object(forKey: "savedParkingDate") as? Date else {
            return
        }
        
        savedParkingLocation = SavedParking(
            latitude: lat,
            longitude: lon,
            savedAt: date
        )
    }
}

// MARK: - Models

struct SavedParking: Codable {
    let latitude: Double
    let longitude: Double
    let savedAt: Date
    
    var formattedTime: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        return formatter.localizedString(for: savedAt, relativeTo: Date())
    }
}

// MARK: - Notifications

extension Notification.Name {
    static let parkingLocationSaved = Notification.Name("parkingLocationSaved")
    static let navigationStarted = Notification.Name("navigationStarted")
    static let navigationEnded = Notification.Name("navigationEnded")
}
