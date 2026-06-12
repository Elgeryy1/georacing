import Foundation
import Combine
import CoreLocation

struct GroupLocation: Identifiable, Codable, Sendable {
    let id: String
    let userId: String
    let displayName: String
    let coordinate: CLLocationCoordinate2D
    let lastUpdatedMs: Int64
    let sharing: Bool

    enum CodingKeys: String, CodingKey {
        case id, userId, displayName, latitude, longitude, lastUpdatedMs, sharing
    }

    init(id: String, userId: String, displayName: String, coordinate: CLLocationCoordinate2D, lastUpdatedMs: Int64, sharing: Bool) {
        self.id = id
        self.userId = userId
        self.displayName = displayName
        self.coordinate = coordinate
        self.lastUpdatedMs = lastUpdatedMs
        self.sharing = sharing
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        userId = try container.decode(String.self, forKey: .userId)
        displayName = try container.decode(String.self, forKey: .displayName)
        let lat = try container.decode(Double.self, forKey: .latitude)
        let lon = try container.decode(Double.self, forKey: .longitude)
        coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
        lastUpdatedMs = try container.decode(Int64.self, forKey: .lastUpdatedMs)
        sharing = try container.decode(Bool.self, forKey: .sharing)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(userId, forKey: .userId)
        try container.encode(displayName, forKey: .displayName)
        try container.encode(coordinate.latitude, forKey: .latitude)
        try container.encode(coordinate.longitude, forKey: .longitude)
        try container.encode(lastUpdatedMs, forKey: .lastUpdatedMs)
        try container.encode(sharing, forKey: .sharing)
    }
}

protocol GroupLocationRepositoryProtocol {
    func startPolling(groupName: String, currentUserId: String) -> AnyPublisher<[GroupLocation], Error>
    func updateMyLocation(groupName: String, userId: String, location: CLLocationCoordinate2D, displayName: String) async throws
}

final class GroupLocationRepository: GroupLocationRepositoryProtocol {

    private var timer: AnyCancellable?

    func startPolling(groupName: String, currentUserId: String) -> AnyPublisher<[GroupLocation], Error> {
        let subject = PassthroughSubject<[GroupLocation], Error>()

        timer?.cancel()
        timer = Timer.publish(every: 3.0, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                Task {
                    do {
                        let members = try await APIService.shared.fetchGroupMembers(groupName: groupName)
                        let mapped: [GroupLocation] = members.compactMap { m in
                            guard m.user_uuid != currentUserId else { return nil }
                            return GroupLocation(
                                id: m.user_uuid,
                                userId: m.user_uuid,
                                displayName: m.displayName ?? ("User " + m.user_uuid.prefix(6)),
                                coordinate: CLLocationCoordinate2D(latitude: m.lat, longitude: m.lon),
                                lastUpdatedMs: m.timestamp ?? 0,
                                sharing: true
                            )
                        }
                        subject.send(mapped)
                    } catch {
                        // No cortamos el stream en errores transitorios
                        // Si quieres cortar, usa subject.send(completion: .failure(error))
                    }
                }
            }

        return subject.eraseToAnyPublisher()
    }

    func updateMyLocation(groupName: String, userId: String, location: CLLocationCoordinate2D, displayName: String) async throws {
        let req = GroupLocationRequest(
            user_uuid: userId,
            group_name: groupName,
            lat: location.latitude,
            lon: location.longitude,
            displayName: displayName
        )
        try await APIService.shared.upsertGroupLocation(req)
    }
}
