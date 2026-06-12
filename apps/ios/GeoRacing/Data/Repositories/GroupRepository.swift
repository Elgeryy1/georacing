import Foundation
import Combine
import CoreLocation

class GroupRepository: ObservableObject {
    static let shared = GroupRepository()
    
    @Published var currentGroup: Group?
    @Published var groupMembers: [GroupMember] = []
    
    private let locationRepository = GroupLocationRepository()
    private var cancellables = Set<AnyCancellable>()
    
    private init() {
        // Mock default state: No group
    }
    
    func createGroup() async throws -> Group {
        let newGroup = Group(id: "group_\(Int.random(in: 1000...9999))", name: "My Racing Group", ownerId: AuthService.shared.currentUser?.uid ?? "me", members: [])
        
        await MainActor.run {
            self.currentGroup = newGroup
            self.startPollingMembers()
        }
        return newGroup
    }
    
    func joinGroup(groupId: String) async throws {
         await MainActor.run {
             self.currentGroup = Group(id: groupId, name: "Joined Group", ownerId: "friend", members: [])
             self.startPollingMembers()
         }
    }
    
    func leaveGroup() {
        self.currentGroup = nil
        self.groupMembers = []
        cancellables.removeAll()
    }
    
    private func startPollingMembers() {
        guard let group = currentGroup else { return }
        // Don't cancel everything, we might want to keep location sharing? 
        // Actually, simpler to restart everything on group change.
        cancellables.removeAll()
        
        let userId = AuthService.shared.currentUser?.uid ?? "guest_user"
        
        // 1. Poll Members
        locationRepository
            .startPolling(groupName: group.id, currentUserId: userId)
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { _ in }, receiveValue: { [weak self] locations in
                self?.groupMembers = locations.map { loc in
                    GroupMember(id: loc.userId, displayName: loc.displayName, coordinate: loc.coordinate, isSharing: loc.sharing)
                }
            })
            .store(in: &cancellables)
            
        // 2. Start Sharing My Location
        startSharingLocation()
    }
    
    func generateInviteLink() -> String {
        guard let group = currentGroup else { return "" }
        return "georacing://join?groupId=\(group.id)"
    }
    
    private func startSharingLocation() {
        // Stop previous sharing if any
        // We will piggyback on the same timer logic or a new subscription?
        // Using Combine to observe LocationManager + Throttle
        
        // Subscription to LocationManager
        LocationManager.shared.$location
            .compactMap { $0 }
            .throttle(for: .seconds(3), scheduler: DispatchQueue.main, latest: true)
            .sink { [weak self] loc in
                guard let self = self, let group = self.currentGroup else { return }
                guard let user = AuthService.shared.currentUser else { return } 
                 // Assuming AuthService has user. If not, fallback.
                let userId = user.uid
                let displayName = user.displayName ?? "User"
                
                Task {
                    try? await self.locationRepository.updateMyLocation(
                        groupName: group.id,
                        userId: userId,
                        location: loc,
                        displayName: displayName
                    )
                }
            }
            .store(in: &cancellables)
    }
}
