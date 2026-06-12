import SwiftUI

struct RouteGuidanceWidget: View {
    let target: String
    let eta: String
    let instruction: String
    var badge: String? = nil
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "arrow.turn.up.right")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(badge != nil ? .teal : GeoToken.primary)
                .frame(width: 48, height: 48)
                .background(badge != nil ? Color.teal.opacity(0.1) : GeoToken.surfaceHighlight)
                .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(instruction)
                        .font(RacingFont.subheader(16))
                        .foregroundColor(GeoToken.textPrimary)
                    
                    if let badge = badge {
                        Text(badge)
                            .font(RacingFont.body(10).bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.teal)
                            .cornerRadius(4)
                    }
                }
                
                Text("To \(target) • \(eta)")
                    .font(RacingFont.body(12))
                    .foregroundColor(GeoToken.textSecondary)
            }
            
            Spacer()
            
            Button("Navigate") {
                // Action
            }
            .font(RacingFont.body(12).bold())
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(badge != nil ? Color.teal : GeoToken.primary)
            .foregroundColor(.white)
            .cornerRadius(20)
        }
        .padding()
        .background(GeoToken.surface)
        .cornerRadius(GeoLayout.radius)
        .overlay(
             RoundedRectangle(cornerRadius: GeoLayout.radius)
                 .stroke(badge != nil ? Color.teal.opacity(0.3) : Color.clear, lineWidth: 1)
        )
    }
}

#Preview {
    ZStack {
        GeoToken.background.ignoresSafeArea()
        RouteGuidanceWidget(target: "Seat A-12", eta: "5 min", instruction: "Turn Right")
            .padding()
    }
}
