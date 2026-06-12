import CarPlay
import UIKit

/// CarPlay Scene Delegate - Entry point for CarPlay UI
/// Manages the lifecycle and interface controller for CarPlay
class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    
    // MARK: - Properties
    
    /// The interface controller provided by CarPlay
    private(set) var interfaceController: CPInterfaceController?
    
    /// Coordinator that manages navigation between templates
    private var coordinator: CarPlayCoordinator?
    
    /// Reference to the CarPlay window
    var carPlayWindow: CPWindow?
    
    // MARK: - CPTemplateApplicationSceneDelegate
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                   didConnect interfaceController: CPInterfaceController) {
        Logger.info("[CarPlay] Connected")
        
        self.interfaceController = interfaceController
        self.carPlayWindow = templateApplicationScene.carWindow
        
        // Initialize coordinator with interface controller
        coordinator = CarPlayCoordinator(interfaceController: interfaceController)
        
        // Set up root template
        coordinator?.start()
        
        // Notify app that CarPlay is connected
        NotificationCenter.default.post(name: .carPlayDidConnect, object: nil)
    }
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                   didDisconnect interfaceController: CPInterfaceController) {
        Logger.info("[CarPlay] Disconnected")
        
        self.interfaceController = nil
        self.coordinator = nil
        self.carPlayWindow = nil
        
        // Notify app that CarPlay disconnected
        NotificationCenter.default.post(name: .carPlayDidDisconnect, object: nil)
    }
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                   didSelect navigationAlert: CPNavigationAlert) {
        // Handle navigation alert selection
        Logger.debug("[CarPlay] Navigation alert selected")
    }
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                   didSelect maneuver: CPManeuver) {
        // Handle maneuver selection (for navigation apps)
        Logger.debug("[CarPlay] Maneuver selected")
    }
}

// MARK: - Notifications

extension Notification.Name {
    static let carPlayDidConnect = Notification.Name("carPlayDidConnect")
    static let carPlayDidDisconnect = Notification.Name("carPlayDidDisconnect")
}
