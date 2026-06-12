import SwiftUI

struct EvacuationView: View {
    @State private var isFlashing = false
    
    var body: some View {
        ZStack {
            RacingColors.red
                .ignoresSafeArea()
            
            VStack(spacing: 30) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.white)
                
                Text(LocalizationUtils.string("EMERGENCY"))
                    .font(RacingFont.header(40))
                    .foregroundColor(.white)
                
                Text(LocalizationUtils.string("EVACUATION ORDER"))
                    .font(RacingFont.header(32))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .opacity(isFlashing ? 1.0 : 0.3)
                    .onAppear {
                        withAnimation(.linear(duration: 0.5).repeatForever()) {
                            isFlashing = true
                        }
                    }
                
                Text("Please follow staff instructions and proceed to the nearest exit immediately.")
                    .font(RacingFont.body())
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding()
            }
            .padding()
        }
    }
}

#Preview {
    EvacuationView()
}
