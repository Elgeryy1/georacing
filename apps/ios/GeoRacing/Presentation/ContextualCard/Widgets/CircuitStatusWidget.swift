import SwiftUI

struct CircuitStatusWidget: View {
    let mode: CircuitMode
    
    var statusTitle: String {
        switch mode {
        case .normal: return "CIRCUIT STATUS: GREEN"
        case .congestion: return "HIGH TRAFFIC ALERT"
        case .maintenance: return "MAINTENANCE IN PROGRESS"
        default: return "CIRCUIT STATUS"
        }
    }
    
    var statusIcon: String {
        switch mode {
        case .normal: return "checkmark.circle.fill"
        case .congestion: return "exclamationmark.triangle.fill"
        case .maintenance: return "hammer.fill"
        default: return "info.circle.fill"
    }
    }
    
    var statusColor: Color {
        switch mode {
        case .normal: return .green
        case .congestion: return .orange
        case .maintenance: return .yellow
        default: return GeoToken.primary
        }
    }
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: statusIcon)
                .font(.system(size: 24))
                .foregroundColor(statusColor)
                .frame(width: 48, height: 48)
                .background(statusColor.opacity(0.1))
                .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 4) {
                Text(statusTitle)
                    .font(RacingFont.subheader(16))
                    .foregroundColor(GeoToken.textPrimary)
                
                Text("Tap for details")
                    .font(RacingFont.body(12))
                    .foregroundColor(GeoToken.textSecondary)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(GeoToken.textSecondary)
        }
        .padding()
        .background(GeoToken.surface)
        .cornerRadius(GeoLayout.radius)
    }
}

#Preview {
    ZStack {
        GeoToken.background.ignoresSafeArea()
        VStack {
            CircuitStatusWidget(mode: .normal)
            CircuitStatusWidget(mode: .congestion)
        }
        .padding()
    }
}
