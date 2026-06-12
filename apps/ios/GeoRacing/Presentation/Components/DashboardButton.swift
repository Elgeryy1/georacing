import SwiftUI

struct DashboardButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    // Minimalist Premium Style
                    RoundedRectangle(cornerRadius: 12)
                        .fill(RacingColors.cardBackground)
                        .frame(width: 65, height: 65)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(color.opacity(0.3), lineWidth: 1)
                        )
                        .shadow(color: Color.black.opacity(0.2), radius: 2, x: 0, y: 2)
                    
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundColor(color)
                }
                
                Text(title)
                    .font(RacingFont.body(12).bold())
                    .foregroundColor(RacingColors.silver)
                    .lineLimit(1)
            }
        }
        .buttonStyle(DashboardButtonStyle())
        .accessibilityLabel(title)
    }
}

// MARK: - Button Style

struct DashboardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}
