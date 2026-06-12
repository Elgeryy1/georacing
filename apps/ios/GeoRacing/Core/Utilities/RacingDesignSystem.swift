import SwiftUI

struct RacingColors {
    static let red = Color(red: 0.85, green: 0.1, blue: 0.1) // Ferrari/Racing Red
    static let darkBackground = Color(red: 0.1, green: 0.12, blue: 0.15) // Asphalt Dark
    static let cardBackground = Color(red: 0.15, green: 0.18, blue: 0.22)
    static let white = Color.white
    static let silver = Color(red: 0.8, green: 0.8, blue: 0.82)
}

struct RacingFont {
    static func header(_ size: CGFloat = 24) -> Font {
        .system(size: size, weight: .black, design: .rounded).italic()
    }
    
    static func subheader(_ size: CGFloat = 18) -> Font {
        .system(size: size, weight: .bold, design: .default)
    }
    
    static func body(_ size: CGFloat = 16) -> Font {
        .system(size: size, weight: .medium, design: .default)
    }
}

// MARK: - Modifiers

struct RacingCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .background(RacingColors.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: 12)) // Alternatively use skewed text for headers, but rounded cards are cleaner for UI
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(LinearGradient(colors: [RacingColors.red.opacity(0.8), .clear], startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.3), radius: 5, x: 0, y: 2)
    }
}

struct RacingButtonModifier: ViewModifier {
    var color: Color = RacingColors.red
    
    func body(content: Content) -> some View {
        content
            .font(RacingFont.subheader())
            .foregroundColor(.white)
            .padding(.vertical, 12)
            .padding(.horizontal, 24)
            .background(
                Capsule()
                    .fill(color)
                    .shadow(color: color.opacity(0.4), radius: 8, x: 0, y: 4)
            )
    }
}

extension View {
    func racingCard() -> some View {
        self.modifier(RacingCardModifier())
    }
    
    func racingButton(color: Color = RacingColors.red) -> some View {
        self.modifier(RacingButtonModifier(color: color))
    }
    
    /// Conditionally apply a modifier
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}
