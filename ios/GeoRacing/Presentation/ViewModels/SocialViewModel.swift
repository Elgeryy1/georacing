import Foundation
import SwiftUI
import Combine
import CoreImage.CIFilterBuiltins

class SocialViewModel: ObservableObject {
    
    @Published var inviteLink: String = ""
    @Published var qrCodeImage: UIImage?
    @Published var alertMessage: String?
    @Published var isShowingQR = false
    @Published var isJoined = false
    
    private let groupRepo = GroupRepository.shared
    private let context = CIContext()
    private let filter = CIFilter.qrCodeGenerator()
    
    func createGroup() {
        Task {
            do {
                _ = try await groupRepo.createGroup()
                await MainActor.run {
                    self.inviteLink = groupRepo.generateInviteLink()
                    self.generateQRCode(from: self.inviteLink)
                    self.isShowingQR = true
                    self.isJoined = true
                }
            } catch {
                await MainActor.run {
                    self.alertMessage = "Failed to create group"
                }
            }
        }
    }
    
    func joinGroup(url: String) {
        // url format georacing://join?groupId=XYZ
        guard let components = URLComponents(string: url),
              let queryItems = components.queryItems,
              let groupId = queryItems.first(where: { $0.name == "groupId" })?.value else {
            self.alertMessage = "Invalid QR Code"
            return
        }
        
        Task {
            do {
                try await groupRepo.joinGroup(groupId: groupId)
                await MainActor.run {
                    self.alertMessage = "Joined Group successfully!"
                    self.isJoined = true
                }
            } catch {
                await MainActor.run {
                    self.alertMessage = "Failed to join group"
                }
            }
        }
    }
    
    func leaveGroup() {
        groupRepo.leaveGroup()
        self.isJoined = false
        self.inviteLink = ""
        self.qrCodeImage = nil
    }
    
    private func generateQRCode(from string: String) {
        let data = Data(string.utf8)
        filter.setValue(data, forKey: "inputMessage")
        
        if let outputImage = filter.outputImage {
            // Scale up
            let transform = CGAffineTransform(scaleX: 10, y: 10)
            let scaledImage = outputImage.transformed(by: transform)
            
            if let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) {
                self.qrCodeImage = UIImage(cgImage: cgImage)
            }
        }
    }
}
