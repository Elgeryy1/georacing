import Foundation

// MARK: - Models

public enum ParkingZone: String, Codable, CaseIterable, Identifiable {
    case zoneA = "A"
    case zoneB = "B"
    case zoneC = "C"
    case zoneD = "D"
    
    public var id: String { rawValue }
    
    var displayName: String {
        return "Zona \(rawValue)"
    }
    
    var colorName: String {
        // In a real app, these would map to Asset colors
        switch self {
        case .zoneA: return "Red"
        case .zoneB: return "Blue"
        case .zoneC: return "Green"
        case .zoneD: return "Yellow"
        }
    }
}

public enum AssignmentStatus: String, Codable {
    case pending
    case confirmed
    case expired
}

public struct ParkingAssignment: Codable, Identifiable {
    public let id: UUID
    public let zone: ParkingZone
    public let virtualSpot: String // e.g., "C-4321"
    public let licensePlate: String
    public let ticketId: String
    public let createdAt: Date
    public let expirationDate: Date
    public var status: AssignmentStatus
    public let notes: String?
    
    public init(id: UUID = UUID(), zone: ParkingZone, virtualSpot: String, licensePlate: String, ticketId: String, createdAt: Date = Date(), expirationDate: Date, status: AssignmentStatus = .confirmed, notes: String? = nil) {
        self.id = id
        self.zone = zone
        self.virtualSpot = virtualSpot
        self.licensePlate = licensePlate
        self.ticketId = ticketId
        self.createdAt = createdAt
        self.expirationDate = expirationDate
        self.status = status
        self.notes = notes
    }
}

public struct TicketInfo: Codable {
    public let ticketId: String
    public let eventName: String?
    public let eventDate: Date?
    
    public init(ticketId: String, eventName: String? = nil, eventDate: Date? = nil) {
        self.ticketId = ticketId
        self.eventName = eventName
        self.eventDate = eventDate
    }
}

public enum ParkingError: LocalizedError {
    case invalidLicensePlate
    case invalidTicket
    case persistenceFailed
    case unknown
    
    public var errorDescription: String? {
        switch self {
        case .invalidLicensePlate:
            return "Invalid license plate."
        case .invalidTicket:
            return "Invalid ticket."
        case .persistenceFailed:
            return "Could not save the assignment."
        case .unknown:
            return "An unknown error occurred."
        }
    }
    
    @MainActor
    public var localizedErrorDescription: String {
        LocalizationUtils.string(errorDescription ?? "")
    }
}

// Placeholder for future multi-event support
public struct ParkingEventContext: Codable {
    public let eventId: String
    public let eventDate: Date?
    
    public init(eventId: String, eventDate: Date? = nil) {
        self.eventId = eventId
        self.eventDate = eventDate
    }
}
