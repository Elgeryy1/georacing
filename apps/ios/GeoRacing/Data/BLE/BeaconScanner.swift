import Foundation
import CoreLocation
import Combine

@MainActor
class BeaconScanner: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = BeaconScanner()
    
    private var locationManager: CLLocationManager?

    @Published var foundBeacons: [CLBeacon] = []
    
    // API Integrated Beacons
    @Published var configBeacons: [BeaconDto] = []
    @Published var currentBeacon: BeaconDto?
    
    private var monitoredRegions: [CLBeaconRegion] = []
    
    override init() {
        super.init()
        self.locationManager = CLLocationManager()
        self.locationManager?.delegate = self
        self.locationManager?.requestAlwaysAuthorization()
    }
    
    func loadBeacons() {
        Task {
            do {
                let beacons = try await APIService.shared.fetchBeacons()
                self.configBeacons = beacons
                self.startScanning()
            } catch {
                Logger.error("Failed to fetch beacons: \(error)")
            }
        }
    }
    
    func startScanning() {
        guard !configBeacons.isEmpty else { return }
        
        // Group by UUID to monitor unique UUIDs
        let uniqueUUIDs = Set(configBeacons.compactMap { UUID(uuidString: $0.uuid) })
        
        for uuid in uniqueUUIDs {
            let constraint = CLBeaconIdentityConstraint(uuid: uuid)
            let region = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: "GeoRacing_\(uuid.uuidString)")
            
            locationManager?.startMonitoring(for: region)
            locationManager?.startRangingBeacons(satisfying: constraint)
            monitoredRegions.append(region)
        }
    }
    
    func stopScanning() {
        for region in monitoredRegions {
            locationManager?.stopMonitoring(for: region)
            locationManager?.stopRangingBeacons(satisfying: region.beaconIdentityConstraint)
        }
        monitoredRegions.removeAll()
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        self.foundBeacons = beacons
        updateCurrentBeacon(from: beacons)
    }
    
    private func updateCurrentBeacon(from clBeacons: [CLBeacon]) {
        // Find the closest beacon from the ranged list
        // CLBeacon already sort by proximity usually, but let's be safe.
        // We need to match major/minor/uuid to our configBeacons
        
        // Closest beacon by accuracy (immediate/near)
        if let closest = clBeacons.first(where: { $0.accuracy != -1 && $0.accuracy < 5.0 }) { // 5 meters threshold
             // Matche with config
            if let matched = configBeacons.first(where: {
                $0.uuid.caseInsensitiveCompare(closest.uuid.uuidString) == .orderedSame &&
                $0.major == closest.major.intValue &&
                $0.minor == closest.minor.intValue
            }) {
                // Update current beacon only if it changed
                if self.currentBeacon?.id != matched.id {
                    self.currentBeacon = matched
                    Logger.info("Locked on beacon: \(matched.name)")
                }
                return
            }
        }
        
        // If we lost proximity or empty, maybe reset?
        // Or keep last known? For incident reporting, immediate presence is key.
        // Let's reset if list is empty or far.
        if clBeacons.isEmpty {
           // We might want to keep it for a few seconds (hysteresis), but for simple impl:
           // self.currentBeacon = nil
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        Logger.debug("Entered beacon region: \(region.identifier)")
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        Logger.debug("Exited beacon region: \(region.identifier)")
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            if configBeacons.isEmpty {
                loadBeacons()
            } else {
                startScanning()
            }
        default:
            break
        }
    }
}
