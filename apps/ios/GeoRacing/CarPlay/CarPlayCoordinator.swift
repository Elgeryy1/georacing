import CarPlay
import Combine

/// Coordinator that manages CarPlay template navigation and state
@MainActor
class CarPlayCoordinator {
    
    // MARK: - Properties
    
    private let interfaceController: CPInterfaceController
    private let templateFactory: CarPlayTemplateFactory
    private var cancellables = Set<AnyCancellable>()
    
    /// Current navigation state
    private var isNavigating = false
    
    // MARK: - Initialization
    
    init(interfaceController: CPInterfaceController) {
        self.interfaceController = interfaceController
        self.templateFactory = CarPlayTemplateFactory(coordinator: nil)
        self.templateFactory.coordinator = self
        
        setupObservers()
    }
    
    // MARK: - Public Methods
    
    /// Start CarPlay UI with root template
    func start() {
        let rootTemplate = templateFactory.createRootTemplate()
        
        interfaceController.setRootTemplate(rootTemplate, animated: true) { success, error in
            if let error = error {
                Logger.error("[CarPlay] Error setting root template: \(error)")
            } else {
                Logger.info("[CarPlay] Root template set successfully")
            }
        }
    }
    
    // MARK: - Navigation Actions
    
    /// Navigate to a destination
    func navigateTo(destination: CarPlayDestination) {
        Logger.debug("[CarPlay] Navigating to: \(destination.name)")
        
        // Create navigation template
        let navTemplate = templateFactory.createNavigationTemplate(for: destination)
        
        interfaceController.pushTemplate(navTemplate, animated: true) { success, error in
            if success {
                self.isNavigating = true
                // Start actual navigation via RouteManager
                self.startNavigation(to: destination)
            }
        }
    }
    
    /// Show parking list
    func showParkingList() {
        let parkingTemplate = templateFactory.createParkingTemplate()
        interfaceController.pushTemplate(parkingTemplate, animated: true, completion: nil)
    }
    
    /// Save current parking location
    func saveCurrentParking() {
        // Get current location from LocationManager
        guard let location = LocationManager.shared.location else {
            showAlert(title: "Error", message: LocalizationUtils.string("Could not get current location"))
            return
        }
        
        // Save to UserDefaults
        UserDefaults.standard.set(location.latitude, forKey: "savedParkingLat")
        UserDefaults.standard.set(location.longitude, forKey: "savedParkingLon")
        UserDefaults.standard.set(Date(), forKey: "savedParkingDate")
        
        showAlert(title: LocalizationUtils.string("Saved"), message: LocalizationUtils.string("Parking location saved"))
        Logger.info("[CarPlay] Parking saved at: \(location)")
    }
    
    /// Navigate to saved parking
    func navigateToSavedParking() {
        guard let lat = UserDefaults.standard.object(forKey: "savedParkingLat") as? Double,
              let lon = UserDefaults.standard.object(forKey: "savedParkingLon") as? Double else {
            showAlert(title: "No data", message: "No saved parking")
            return
        }
        
        let destination = CarPlayDestination(
            id: "saved_parking",
            name: LocalizationUtils.string("Go to My Car"),
            subtitle: LocalizationUtils.string("Saved location"),
            latitude: lat,
            longitude: lon,
            type: .parking
        )
        
        navigateTo(destination: destination)
    }
    
    /// End current navigation
    func endNavigation() {
        isNavigating = false
        interfaceController.popToRootTemplate(animated: true, completion: nil)
        
        // Stop RouteManager navigation
        // RouteManager.shared.stopNavigation() // Uncomment when method exists
    }
    
    // MARK: - Private Methods
    
    private func setupObservers() {
        // Observe location updates for navigation
        // This syncs with the existing LocationManager
    }
    
    private func startNavigation(to destination: CarPlayDestination) {
        // Integration point with existing RouteManager
        // RouteManager.shared.startNavigation(to: CLLocationCoordinate2D(...))
        Logger.debug("[CarPlay] Starting navigation to \(destination.name)")
    }
    
    private func showAlert(title: String, message: String) {
        let alert = CPAlertTemplate(titleVariants: [title], actions: [
            CPAlertAction(title: "OK", style: .default, handler: { [weak self] _ in
                self?.interfaceController.dismissTemplate(animated: true, completion: nil)
            })
        ])
        
        interfaceController.presentTemplate(alert, animated: true, completion: nil)
    }
}

// MARK: - CarPlay Destination Model

struct CarPlayDestination: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let latitude: Double
    let longitude: Double
    let type: DestinationType
    
    enum DestinationType {
        case circuit
        case parking
        case poi
        case savedLocation
    }
}
