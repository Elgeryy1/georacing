import Foundation
import CoreBluetooth
import Combine

/// Staff como "Balizas Humanas" (Emisión BLE)
/// Acts as a dynamic BLE beacon emitted by Staff devices when physical beacons fail.
/// Needs `NSBluetoothPeripheralUsageDescription` in Info.plist.
@MainActor
class StaffBeaconService: NSObject, ObservableObject {
    static let shared = StaffBeaconService()
    
    @Published var isBroadcasting: Bool = false
    @Published var bluetoothState: CBManagerState = .unknown
    
    private var peripheralManager: CBPeripheralManager?
    
    // An agreed-upon UUID for GeoRacing Staff signals
    private let staffServiceUUID = CBUUID(string: "A1B2C3D4-E5F6-7890-1234-56789ABCDEF0")
    
    // We can use a characteristic to embed a short payload
    private var staffCharacteristic: CBMutableCharacteristic?
    
    override private init() {
        super.init()
        // Initialization of CBPeripheralManager is deferred to request permission only when needed
    }
    
    /// Request permissions and initialize the peripheral manager
    func prepare() {
        if peripheralManager == nil {
            peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        }
    }
    
    /// Start broadcasting as a peripheral (Human Beacon)
    func startBroadcasting(payload: String) {
        guard let manager = peripheralManager, manager.state == .poweredOn else {
            Logger.error("[StaffBeacon] Cannot broadcast. Bluetooth is not powered on or manager is nil.")
            return
        }
        
        // Stop if already advertising
        if isBroadcasting { stopBroadcasting() }
        
        // 1. Create the characteristic containing the payload (e.g. "STATUS: DANGER")
        let payloadData = payload.data(using: .utf8) ?? Data()
        staffCharacteristic = CBMutableCharacteristic(
            type: CBUUID(string: "A1B2C3D4-E5F6-7890-1234-56789ABCDEF1"),
            properties: [.read],
            value: payloadData,
            permissions: [.readable]
        )
        
        // 2. Create the service
        let service = CBMutableService(type: staffServiceUUID, primary: true)
        if let char = staffCharacteristic {
            service.characteristics = [char]
        }
        
        // 3. Add service to manager
        manager.add(service)
        
        // 4. Start advertising the service UUID
        // The local name provides an immediate glance, the service UUID allows background discovery 
        manager.startAdvertising([
            CBAdvertisementDataLocalNameKey: "GeoRacingStaff",
            CBAdvertisementDataServiceUUIDsKey: [staffServiceUUID]
        ])
        
        self.isBroadcasting = true
        Logger.info("[StaffBeacon] Started broadcasting: \(payload)")
    }
    
    /// Stop broadcasting
    func stopBroadcasting() {
        guard let manager = peripheralManager else { return }
        manager.stopAdvertising()
        manager.removeAllServices()
        self.isBroadcasting = false
        Logger.info("[StaffBeacon] Stopped broadcasting.")
    }
}

extension StaffBeaconService: CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        Task { @MainActor in
            self.bluetoothState = peripheral.state
            
            switch peripheral.state {
            case .poweredOn:
                Logger.info("[StaffBeacon] Bluetooth is Powered On.")
            case .poweredOff:
                Logger.warning("[StaffBeacon] Bluetooth powered off. Stopping broadcast.")
                if self.isBroadcasting { stopBroadcasting() }
            case .unauthorized:
                Logger.error("[StaffBeacon] Bluetooth unauthorized.")
                if self.isBroadcasting { stopBroadcasting() }
            default:
                break
            }
        }
    }
    
    func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        if let error = error {
            Logger.error("[StaffBeacon] Failed to start advertising: \(error.localizedDescription)")
            Task { @MainActor in self.isBroadcasting = false }
        } else {
            Logger.info("[StaffBeacon] Advertising started successfully.")
        }
    }
}
