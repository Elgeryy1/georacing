import SwiftUI

struct OfflineWidget: View {
    let lastUpdated: Date
    
    var timeString: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: lastUpdated)
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 20))
                .foregroundColor(GeoToken.textSecondary)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("YOU ARE OFFLINE")
                    .font(RacingFont.subheader(14))
                    .foregroundColor(GeoToken.textSecondary)
                
                Text("Data last updated at \(timeString)")
                    .font(RacingFont.body(12))
                    .foregroundColor(GeoToken.textSecondary.opacity(0.7))
            }
            
            Spacer()
            
            Text("RETRYING...")
                .font(RacingFont.body(10).bold())
                .foregroundColor(GeoToken.textSecondary)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(GeoToken.surfaceHighlight)
                .cornerRadius(4)
        }
        .padding()
        .background(GeoToken.surface.opacity(0.8))
        .cornerRadius(GeoLayout.radius)
        .overlay(
            RoundedRectangle(cornerRadius: GeoLayout.radius)
                .stroke(GeoToken.borderSubtle, lineWidth: 1)
        )
    }
}

#Preview {
    ZStack {
        GeoToken.background.ignoresSafeArea()
        OfflineWidget(lastUpdated: Date())
            .padding()
    }
}
