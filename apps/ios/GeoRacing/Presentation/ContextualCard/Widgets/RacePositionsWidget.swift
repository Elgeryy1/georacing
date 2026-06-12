import SwiftUI

struct RacePositionsWidget: View {
    // Mock data for now, ideally this comes from a ViewModel
    let positions = [
        (driver: "VER", team: "Red Bull", gap: "Leader"),
        (driver: "NOR", team: "McLaren", gap: "+2.4s"),
        (driver: "HAM", team: "Mercedes", gap: "+5.1s"),
        (driver: "LEC", team: "Ferrari", gap: "+8.9s"),
        (driver: "PIA", team: "McLaren", gap: "+12.3s")
    ]
    
    let currentLap: Int
    let totalLaps: Int
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("LIVE STANDINGS")
                    .font(RacingFont.header(14))
                    .foregroundColor(GeoToken.textSecondary)
                Spacer()
                Text("LAP \(currentLap)/\(totalLaps)")
                    .font(RacingFont.body(12).bold())
                    .foregroundColor(GeoToken.primary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(GeoToken.surfaceHighlight)
                    .cornerRadius(4)
            }
            .padding(.bottom, 4)
            
            VStack(spacing: 0) {
                ForEach(Array(positions.enumerated()), id: \.offset) { index, position in
                    HStack {
                        Text("\(index + 1)")
                            .font(RacingFont.body().bold())
                            .foregroundColor(GeoToken.primary)
                            .frame(width: 24, alignment: .leading)
                        
                        Text(position.driver)
                            .font(RacingFont.body().weight(.heavy))
                            .foregroundColor(GeoToken.textPrimary)
                        
                        Text(position.team)
                            .font(RacingFont.body(12))
                            .foregroundColor(GeoToken.textSecondary)
                        
                        Spacer()
                        
                        Text(position.gap)
                            .font(RacingFont.body(12).monospacedDigit())
                            .foregroundColor(GeoToken.textOnPrimary)
                    }
                    .padding(.vertical, 6)
                    
                    if index < positions.count - 1 {
                        Divider().background(GeoToken.borderSubtle)
                    }
                }
            }
        }
        .padding()
        .background(GeoToken.surface)
        .cornerRadius(GeoLayout.radius)
    }
}

#Preview {
    ZStack {
        GeoToken.background.ignoresSafeArea()
        RacePositionsWidget(currentLap: 12, totalLaps: 56)
            .padding()
    }
}
