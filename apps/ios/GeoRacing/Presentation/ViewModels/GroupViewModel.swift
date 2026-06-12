import Foundation
import Combine
import SwiftUI

class GroupViewModel: ObservableObject {
    @Published var isInGroup: Bool = false
    @Published var currentGroup: Group?
    @Published var members: [GroupMember] = []
    @Published var errorMsg: String?
    @Published var isLoading: Bool = false
    
    // Join Input
    @Published var joinCode: String = ""
    
    private var repository = GroupRepository.shared
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Subscribe to Repository changes
        repository.$currentGroup
            .receive(on: DispatchQueue.main)
            .sink { [weak self] group in
                self?.currentGroup = group
                self?.isInGroup = (group != nil)
            }
            .store(in: &cancellables)
            
        repository.$groupMembers
            .receive(on: DispatchQueue.main)
            .assign(to: \.members, on: self)
            .store(in: &cancellables)
    }
    
    @MainActor
    func createGroup() async {
        isLoading = true
        errorMsg = nil
        do {
            _ = try await repository.createGroup()
        } catch {
            errorMsg = "Error creating group: \(error.localizedDescription)"
        }
        isLoading = false
    }
    
    @MainActor
    func joinGroup() async {
        guard !joinCode.isEmpty else { return }
        isLoading = true
        errorMsg = nil
        do {
            try await repository.joinGroup(groupId: joinCode)
        } catch {
            errorMsg = "Error joining group: \(error.localizedDescription)"
        }
        isLoading = false
    }
    
    func leaveGroup() {
        repository.leaveGroup()
        joinCode = ""
    }
    
    func getInviteLink() -> String {
        return repository.generateInviteLink()
    }
}
