import SwiftUI

struct EmergencyWidget: View {
    let mode: CircuitMode
    
    var title: String {
        switch mode {
        case .evacuation: return "EVACUATION ORDER"
        case .emergency: return "EMERGENCY ALERT"
        default: return "ALERT"
        }
    }
    
    var message: String {
        switch mode {
        case .evacuation: return "Please proceed calmly to the nearest exit. Follow staff instructions."
        case .emergency: return "Incident reported nearby. Stay clear of the area."
        default: return "Important safety announcement."
        }
    }
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Color.white.opacity(0.2))
                .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(RacingFont.header(20))
                    .foregroundColor(.white)
                
                Text(message)
                    .font(RacingFont.body(14))
                    .foregroundColor(.white.opacity(0.9))
                    .fixedSize(horizontal: false, vertical: true)
            }
            
            Spacer()
        }
        .padding()
        .background(GeoToken.primary)
        .cornerRadius(GeoLayout.radius)
        .overlay(
            RoundedRectangle(cornerRadius: GeoLayout.radius)
                .stroke(Color.white.opacity(0.3), lineWidth: 2)
        )
        .shadow(color: GeoToken.primary.opacity(0.5), radius: 8, x: 0, y: 4)
    }
}

#Preview {
    ZStack {
        GeoToken.background.ignoresSafeArea()
        EmergencyWidget(mode: .evacuation)
            .padding()
    }
}
