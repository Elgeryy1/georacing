import SwiftUI

struct LoginView: View {
    @StateObject private var authService = AuthService.shared
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack {
            // Background
            RacingColors.darkBackground
                .ignoresSafeArea()
            
            VStack {
                Spacer()
                
                // Logo / Title
                VStack(spacing: 10) {
                    Image(systemName: "flag.checkered.2.crossed")
                        .font(.system(size: 80))
                        .foregroundColor(RacingColors.red)
                        .padding(.bottom, 20)
                    
                    Text("GeoRacing")
                        .font(RacingFont.header(40))
                        .foregroundColor(.white)
                    
                    Text(LocalizationUtils.string("OFFICIAL APP"))
                        .font(RacingFont.subheader())
                        .foregroundColor(RacingColors.silver)
                        .tracking(2)
                }
                
                Spacer()
                
                // Login Button
                VStack(spacing: 20) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Button(action: handleLogin) {
                            HStack {
                                Image(systemName: "g.circle.fill") // Placeholder for Google Logo
                                    .font(.title2)
                                Text("Sign in with Google")
                                    .font(RacingFont.subheader())
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.white)
                            .foregroundColor(.black)
                            .cornerRadius(12)
                            .shadow(radius: 5)
                        }
                        .padding(.horizontal, 40)
                    }
                    
                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(RacingColors.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                }
                .padding(.bottom, 50)
            }
        }
    }
    
    private func handleLogin() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                try await authService.signInWithGoogle()
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}

#Preview {
    LoginView()
}
