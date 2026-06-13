import Foundation

// MARK: - Assignment Service Logic

protocol ParkingAssignmentServiceProtocol {
    func assignParking(licensePlate: String, ticketId: String) async throws -> ParkingAssignment
}

class ParkingAssignmentService: ParkingAssignmentServiceProtocol {
    
    // Simulate capacity for zones (MVP logic)
    private let zoneCapacities: [ParkingZone: Int] = [
        .zoneA: 5500,
        .zoneB: 4500,
        .zoneC: 6500,
        .zoneD: 2500
    ]
    
    // Weights for distribution
    private let zoneWeights: [ParkingZone: Double] = [
        .zoneC: 0.35,
        .zoneA: 0.28,
        .zoneB: 0.25,
        .zoneD: 0.12
    ]
    
    func assignParking(licensePlate: String, ticketId: String) async throws -> ParkingAssignment {
        // 1. Deterministic hashing to select a zone and spot
        let seed = "\(ticketId)-\(licensePlate)".uppercased()
        let hash = abs(seed.hashValue)
        
        // 2. Select Zone based on weights (simplistic weighted random simulation using hash)
        let zone = selectZone(hash: hash)
        
        // 3. Generate Spot ID
        // Format: [Zone]-Fila-[Row]-[Number] or similar. Let's use simple C-4321 style as requested.
        // We use the hash again to determine the number part.
        let spotNumber = (hash % 1000) + 1 // 1...1000
        let spotString = "\(zone.rawValue)-\(String(format: "%04d", spotNumber))"
        
        // Calculate Expiration: End of current day
        let calendar = Calendar.current
        let endOfDay = calendar.startOfDay(for: Date()).addingTimeInterval(24 * 60 * 60 - 1)
        
        // 4. Create Assignment
        return ParkingAssignment(
            zone: zone,
            virtualSpot: spotString,
            licensePlate: licensePlate,
            ticketId: ticketId,
            createdAt: Date(),
            expirationDate: endOfDay,
            status: .confirmed,
            notes: "Asignación automática basada en ticket."
        )
    }
    
    private func selectZone(hash: Int) -> ParkingZone {
        // Normalized value 0.0 - 1.0 from hash
        let normalized = Double(hash % 100) / 100.0
        
        var cumulative: Double = 0.0
        
        // Order by weight descending to fill largest first effectively in this crude simulation,
        // or just iterate.
        let sortedZones = zoneWeights.sorted { $0.value > $1.value }
        
        for (zone, weight) in sortedZones {
            cumulative += weight
            if normalized <= cumulative {
                return zone
            }
        }
        return .zoneC // Default fallback
    }
}

// MARK: - Repository (Persistence)

protocol ParkingRepositoryProtocol {
    func saveAssignment(_ assignment: ParkingAssignment)
    func getAssignment() -> ParkingAssignment?
    func clearAssignment()
}

struct ParkingStorageContainer: Codable {
    let schemaVersion: Int
    let assignment: ParkingAssignment
}

class ParkingRepository: ParkingRepositoryProtocol {
    
    private let key = "saved_parking_assignment"
    private let currentSchemaVersion = 1
    
    func saveAssignment(_ assignment: ParkingAssignment) {
        let container = ParkingStorageContainer(schemaVersion: currentSchemaVersion, assignment: assignment)
        if let data = try? JSONEncoder().encode(container) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
    
    // Cleaned up orphaned code block
    
    func getAssignment() -> ParkingAssignment? {
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        
        var retrievedAssignment: ParkingAssignment?
        
        // Try decoding versioned container
        if let container = try? JSONDecoder().decode(ParkingStorageContainer.self, from: data) {
            retrievedAssignment = container.assignment
        }
        // Fallback for legacy
        else if let legacyAssignment = try? JSONDecoder().decode(ParkingAssignment.self, from: data) {
            retrievedAssignment = legacyAssignment
        }
        
        // Valid Expiration Check
        if let assignment = retrievedAssignment {
            if Date() > assignment.expirationDate {
                // Expired: Clear and return nil
                clearAssignment()
                return nil
            }
            return assignment
        }
        
        return nil
    }
    
    func clearAssignment() {
        UserDefaults.standard.removeObject(forKey: key)
    }
}
