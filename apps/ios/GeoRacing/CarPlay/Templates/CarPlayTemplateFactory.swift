import CarPlay
import CoreLocation

/// Factory for creating CarPlay templates
/// Centralizes template creation logic and keeps UI separate from business logic
@MainActor
class CarPlayTemplateFactory {
    
    // MARK: - Properties
    
    weak var coordinator: CarPlayCoordinator?
    
    // MARK: - Initialization
    
    init(coordinator: CarPlayCoordinator?) {
        self.coordinator = coordinator
    }
    
    // MARK: - Root Template
    
    /// Creates the main TabBar template (root of CarPlay UI)
    func createRootTemplate() -> CPTabBarTemplate {
        let tabs = [
            createNavigationTab(),
            createParkingTab(),
            createEventTab(),
            createSettingsTab()
        ]
        
        let tabBar = CPTabBarTemplate(templates: tabs)
        return tabBar
    }
    
    // MARK: - Tab Templates
    
    /// Navigation tab - destinations list
    private func createNavigationTab() -> CPListTemplate {
        let destinations = getDestinations()
        
        let items = destinations.map { destination -> CPListItem in
            let item = CPListItem(
                text: destination.name,
                detailText: destination.subtitle,
                image: UIImage(systemName: destination.type == .circuit ? "flag.checkered" : "mappin.circle")
            )
            
            item.handler = { [weak self] _, completion in
                self?.coordinator?.navigateTo(destination: destination)
                completion()
            }
            
            return item
        }
        
        let section = CPListSection(items: items, header: LocalizationUtils.string("Destinations"), sectionIndexTitle: nil)
        let template = CPListTemplate(title: LocalizationUtils.string("Navigate"), sections: [section])
        template.tabSystemItem = .search
        template.tabImage = UIImage(systemName: "location.fill")
        
        return template
    }
    
    /// Parking tab - parking locations and save feature
    private func createParkingTab() -> CPListTemplate {
        var items: [CPListItem] = []
        
        // Save current location button
        let saveItem = CPListItem(
            text: LocalizationUtils.string("Save My Location"),
            detailText: LocalizationUtils.string("Remember where you parked"),
            image: UIImage(systemName: "pin.fill")
        )
        saveItem.handler = { [weak self] _, completion in
            self?.coordinator?.saveCurrentParking()
            completion()
        }
        items.append(saveItem)
        
        // Navigate to saved parking
        if hasSavedParking() {
            let navigateItem = CPListItem(
                text: LocalizationUtils.string("Go to My Car"),
                detailText: getSavedParkingDescription(),
                image: UIImage(systemName: "car.fill")
            )
            navigateItem.handler = { [weak self] _, completion in
                self?.coordinator?.navigateToSavedParking()
                completion()
            }
            items.append(navigateItem)
        }
        
        // Separator
        let separatorSection = CPListSection(items: items, header: LocalizationUtils.string("Your Parking"), sectionIndexTitle: nil)
        
        // Available parkings
        let parkingItems = getParkingLocations().map { parking -> CPListItem in
            let item = CPListItem(
                text: parking.name,
                detailText: parking.subtitle,
                image: UIImage(systemName: "p.circle.fill")
            )
            item.handler = { [weak self] _, completion in
                self?.coordinator?.navigateTo(destination: parking)
                completion()
            }
            return item
        }
        
        let parkingSection = CPListSection(items: parkingItems, header: LocalizationUtils.string("Available Parkings"), sectionIndexTitle: nil)
        
        let template = CPListTemplate(title: "Parking", sections: [separatorSection, parkingSection])
        template.tabImage = UIImage(systemName: "p.circle.fill")
        
        return template
    }
    
    /// Event tab - race status (read-only)
    private func createEventTab() -> CPInformationTemplate {
        let eventInfo = getEventInfo()
        
        let items = [
            CPInformationItem(title: LocalizationUtils.string("Event"), detail: eventInfo.name),
            CPInformationItem(title: LocalizationUtils.string("Status"), detail: eventInfo.status),
            CPInformationItem(title: LocalizationUtils.string("Time"), detail: eventInfo.time),
            CPInformationItem(title: LocalizationUtils.string("Gates"), detail: eventInfo.gatesOpen ? LocalizationUtils.string("Open") : LocalizationUtils.string("Closed"))
        ]
        
        let template = CPInformationTemplate(
            title: LocalizationUtils.string("Event"),
            layout: .twoColumn,
            items: items,
            actions: []
        )
        template.tabImage = UIImage(systemName: "flag.checkered")
        
        return template
    }
    
    /// Settings tab - offline mode toggle
    private func createSettingsTab() -> CPListTemplate {
        let offlineItem = CPListItem(
            text: LocalizationUtils.string("Offline Mode"),
            detailText: UserDefaults.standard.bool(forKey: "offlineMode") ? LocalizationUtils.string("Enabled") : LocalizationUtils.string("Disabled"),
            image: UIImage(systemName: "wifi.slash")
        )
        offlineItem.handler = { _, completion in
            let current = UserDefaults.standard.bool(forKey: "offlineMode")
            UserDefaults.standard.set(!current, forKey: "offlineMode")
            // Would need to refresh template to update text
            completion()
        }
        
        let versionItem = CPListItem(
            text: LocalizationUtils.string("Version"),
            detailText: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0",
            image: UIImage(systemName: "info.circle")
        )
        versionItem.handler = { _, completion in completion() }
        
        let section = CPListSection(items: [offlineItem, versionItem])
        let template = CPListTemplate(title: LocalizationUtils.string("Settings"), sections: [section])
        template.tabImage = UIImage(systemName: "gearshape.fill")
        
        return template
    }
    
    // MARK: - Navigation Template
    
    /// Creates a map template for active navigation
    func createNavigationTemplate(for destination: CarPlayDestination) -> CPMapTemplate {
        let template = CPMapTemplate()
        template.hidesButtonsWithNavigationBar = false
        
        // Add trip info
        let tripInfo = createTripEstimates(for: destination)
        template.updateEstimates(tripInfo, for: CPTrip(origin: MKMapItem.forCurrentLocation(), destination: MKMapItem.fromCoordinate(CLLocationCoordinate2D(latitude: destination.latitude, longitude: destination.longitude)), routeChoices: []))

        
        // Add navigation bar buttons
        let endButton = CPBarButton(title: LocalizationUtils.string("Finish")) { [weak self] _ in
            self?.coordinator?.endNavigation()
        }
        template.leadingNavigationBarButtons = [endButton]
        
        return template
    }
    
    /// Creates parking list template
    func createParkingTemplate() -> CPListTemplate {
        let items = getParkingLocations().map { parking -> CPListItem in
            let item = CPListItem(
                text: parking.name,
                detailText: parking.subtitle,
                image: UIImage(systemName: "p.circle.fill")
            )
            item.handler = { [weak self] _, completion in
                self?.coordinator?.navigateTo(destination: parking)
                completion()
            }
            return item
        }
        
        let section = CPListSection(items: items)
        return CPListTemplate(title: "Parkings", sections: [section])
    }
    
    // MARK: - Data Providers (Mock - Replace with real services)
    
    private func getDestinations() -> [CarPlayDestination] {
        return [
            CarPlayDestination(
                id: "circuit_main",
                name: "Circuit de Barcelona-Catalunya",
                subtitle: LocalizationUtils.string("Main Entry"),
                latitude: 41.5700,
                longitude: 2.2611,
                type: .circuit
            ),
            CarPlayDestination(
                id: "circuit_paddock",
                name: LocalizationUtils.string("Paddock Access"),
                subtitle: LocalizationUtils.string("Pass only"),
                latitude: 41.5690,
                longitude: 2.2600,
                type: .circuit
            ),
            CarPlayDestination(
                id: "grandstand_main",
                name: "Tribuna Principal",
                subtitle: LocalizationUtils.string("Main Straight"),
                latitude: 41.5705,
                longitude: 2.2620,
                type: .poi
            )
        ]
    }
    
    private func getParkingLocations() -> [CarPlayDestination] {
        return [
            CarPlayDestination(
                id: "parking_a",
                name: "Parking A",
                subtitle: "Cercano a entrada - 10€",
                latitude: 41.5720,
                longitude: 2.2650,
                type: .parking
            ),
            CarPlayDestination(
                id: "parking_b",
                name: "Parking B",
                subtitle: "General - 5€",
                latitude: 41.5680,
                longitude: 2.2580,
                type: .parking
            ),
            CarPlayDestination(
                id: "parking_vip",
                name: "Parking VIP",
                subtitle: LocalizationUtils.string("Credentials only"),
                latitude: 41.5695,
                longitude: 2.2590,
                type: .parking
            )
        ]
    }
    
    private func getEventInfo() -> (name: String, status: String, time: String, gatesOpen: Bool) {
        // TODO: Connect to real EventService
        return (
            name: "GP España F1 2025",
            status: "En curso - Vuelta 23/66",
            time: "14:00 - 16:00",
            gatesOpen: true
        )
    }
    
    private func hasSavedParking() -> Bool {
        return UserDefaults.standard.object(forKey: "savedParkingLat") != nil
    }
    
    private func getSavedParkingDescription() -> String {
        guard let date = UserDefaults.standard.object(forKey: "savedParkingDate") as? Date else {
            return LocalizationUtils.string("Saved location")
        }
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        return "Guardado \(formatter.localizedString(for: date, relativeTo: Date()))"
    }
    
    private func createTripEstimates(for destination: CarPlayDestination) -> CPTravelEstimates {
        // Mock estimates - would come from RouteManager
        return CPTravelEstimates(
            distanceRemaining: Measurement(value: 5.2, unit: .kilometers),
            timeRemaining: 600 // 10 minutes in seconds
        )
    }
}
