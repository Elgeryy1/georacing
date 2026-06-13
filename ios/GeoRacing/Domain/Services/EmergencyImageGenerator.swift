import SwiftUI
import UIKit

/// Lock Screen Médico de Emergencia
/// Captures a SwiftUI View into a UIImage that the user can set as their Lock Screen.
/// This acts as a physical resilience feature in case of fainting without connectivity.
@MainActor
class EmergencyImageGenerator {
    
    enum GeneratorError: Error {
        case failedToRender
    }
    
    /// Generates a UIImage from any given SwiftUI View.
    /// - Parameters:
    ///   - view: The SwiftUI view providing the layout for the image.
    ///   - size: The target size for the output image. A match with UIScreen.main.bounds works best for wallpapers.
    /// - Returns: The rendered UIImage.
    static func render<Content: View>(view: Content, size: CGSize) throws -> UIImage {
        // Embed the view in an environment with the proper sizing semantics
        let hostingController = UIHostingController(rootView: view)
        hostingController.view.bounds = CGRect(origin: .zero, size: size)
        hostingController.view.backgroundColor = .black // Default lockscreen background
        
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { context in
            hostingController.view.layer.render(in: context.cgContext)
        }
        
        guard let _ = image.cgImage else {
            throw GeneratorError.failedToRender
        }
        
        return image
    }
    
    /// Helper to save the image directly to the user's photo album.
    /// Requires NSPhotoLibraryAddUsageDescription in Info.plist
    static func saveToPhotos(_ image: UIImage) {
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
    }
}
