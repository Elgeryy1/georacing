import SwiftUI

// MARK: - Button System
// Premium minimalist button styles. No glow, no neon.

enum GeoButtonVariant {
    case primary    // Solid Brand Color
    case secondary  // Surface Color + Subtle Border
    case tertiary   // Ghost / Text only
}

enum GeoButtonSize {
    case small
    case medium
    case large
    
    var height: CGFloat {
        switch self {
        case .small: return GeoLayout.buttonHeightSmall
        case .medium: return GeoLayout.buttonHeightMedium
        case .large: return GeoLayout.buttonHeightLarge
        }
    }
    
    var horizontalPadding: CGFloat {
        switch self {
        case .small: return 12
        case .medium: return 20
        case .large: return 24
        }
    }
    
    var fontSize: CGFloat {
        switch self {
        case .small: return 14
        case .medium: return 16 // Body
        case .large: return 17  // Headline equivalent
        }
    }
}

struct GeoButtonStyle: ButtonStyle {
    let variant: GeoButtonVariant
    let size: GeoButtonSize
    
    init(variant: GeoButtonVariant = .primary, size: GeoButtonSize = .large) {
        self.variant = variant
        self.size = size
    }
    
    func makeBody(configuration: Configuration) -> some View {
        let isPressed = configuration.isPressed
        
        return configuration.label
            .font(.system(size: size.fontSize, weight: .semibold, design: .default))
            .foregroundColor(textColor(isPressed: isPressed))
            .frame(height: size.height)
            .frame(maxWidth: variant == .tertiary ? nil : .infinity) // Tertiary fits content, others expand
            .padding(.horizontal, size.horizontalPadding)
            .background(backgroundView(isPressed: isPressed))
            .scaleEffect(isPressed ? 0.97 : 1.0)
            .opacity(isPressed ? 0.92 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: isPressed)
    }
    
    // MARK: - Subviews
    
    @ViewBuilder
    private func backgroundView(isPressed: Bool) -> some View {
        switch variant {
        case .primary:
            RoundedRectangle(cornerRadius: GeoLayout.radius)
                .fill(GeoToken.primary)
                // No shadow/glow as strictly requested.
            
        case .secondary:
            RoundedRectangle(cornerRadius: GeoLayout.radius)
                .fill(GeoToken.surface)
                .overlay(
                    RoundedRectangle(cornerRadius: GeoLayout.radius)
                        .stroke(GeoToken.borderSubtle, lineWidth: GeoLayout.borderWidth)
                )
            
        case .tertiary:
            Color.clear // Transparent
        }
    }
    
    // MARK: - Color Logic
    
    private func textColor(isPressed: Bool) -> Color {
        switch variant {
        case .primary:
            return GeoToken.textOnPrimary.opacity(isPressed ? 0.9 : 1.0)
        case .secondary:
            return GeoToken.textPrimary.opacity(isPressed ? 0.8 : 1.0)
        case .tertiary:
            return GeoToken.primary.opacity(isPressed ? 0.7 : 1.0) // Link color behavior
        }
    }
}

// MARK: - Check Requirements (Previews)
// Shows Primary, Secondary, Tertiary in Normal, Pressed (Simulated roughly), Disabled.
struct GeoButtonSystem_Previews: PreviewProvider {
    static var previews: some View {
        ZStack {
            GeoToken.background.ignoresSafeArea()
            
            VStack(spacing: 30) {
                // Large Buttons
                VStack(spacing: 16) {
                    Text("Large Variants").foregroundColor(.gray)
                    
                    Button("Primary Action") {}
                        .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
                    
                    Button("Secondary Action") {}
                        .buttonStyle(GeoButtonStyle(variant: .secondary, size: .large))
                    
                    Button("Tertiary Action") {}
                        .buttonStyle(GeoButtonStyle(variant: .tertiary, size: .large))
                }
                
                // Small Buttons
                HStack(spacing: 16) {
                    Button("Add") {}
                        .buttonStyle(GeoButtonStyle(variant: .primary, size: .small))
                    
                    Button("Cancel") {}
                        .buttonStyle(GeoButtonStyle(variant: .secondary, size: .small))
                }
                
                // Disabled State Example (Using opacity modifier)
                Button("Disabled Primary") {}
                    .buttonStyle(GeoButtonStyle(variant: .primary, size: .medium))
                    .disabled(true)
                    .opacity(0.5) // Standard SwiftUI disable handling
            }
            .padding()
        }
        .preferredColorScheme(.dark)
    }
}
