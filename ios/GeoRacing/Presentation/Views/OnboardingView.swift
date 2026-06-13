import SwiftUI

struct OnboardingView: View {
    @Binding var isPresented: Bool
    @State private var currentPage = 0
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.ignoresSafeArea()
            
            VStack {
                // TabView for slides
                TabView(selection: $currentPage) {
                    OnboardingSlide(
                        image: "flag.checkered.2.crossed",
                        title: "Bienvenido a GeoRacing",
                        description: LocalizationUtils.string("The ultimate circuit experience. Follow the race, track status and locate services."),
                        color: RacingColors.silver
                    ).tag(0)
                    
                    OnboardingSlide(
                        image: "map.fill",
                        title: "Mapa y Servicios",
                        description: LocalizationUtils.string("Find food, WC, parking and your friends on the interactive circuit map."),
                        color: .blue
                    ).tag(1)
                    
                    OnboardingPermissionsSlide(
                        action: {
                            // Request Logic happens here or user does it manually
                            LocalNotificationManager.shared.requestPermission()
                            // Location permission is handled by Map logic usually, but we can trigger it
                        },
                        finishAction: {
                            withAnimation {
                                isPresented = false
                            }
                        }
                    ).tag(2)
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .always))
                .indexViewStyle(PageIndexViewStyle(backgroundDisplayMode: .always))
                
                // Bottom Controls
                HStack {
                    if currentPage < 2 {
                        Button("Saltar") {
                            withAnimation { isPresented = false }
                        }
                        .foregroundColor(.gray)
                        
                        Spacer()
                        
                        Button("Siguiente") {
                            withAnimation { currentPage += 1 }
                        }
                        .font(.headline)
                        .foregroundColor(RacingColors.silver)
                    } else {
                        Spacer()
                    }
                }
                .padding()
            }
        }
    }
}

struct OnboardingSlide: View {
    let image: String
    let title: String
    let description: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: image)
                .resizable()
                .scaledToFit()
                .frame(width: 100, height: 100)
                .foregroundColor(color)
            
            Text(title)
                .font(RacingFont.header(28))
                .foregroundColor(.white)
            
            Text(description)
                .font(RacingFont.body())
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .padding()
    }
}

struct OnboardingPermissionsSlide: View {
    var action: () -> Void
    var finishAction: () -> Void
    
    var body: some View {
        VStack(spacing: 30) {
            Image(systemName: "bell.badge.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundColor(.yellow)
            
            Text("Permisos")
                .font(RacingFont.header(28))
                .foregroundColor(.white)
            
            Text(LocalizationUtils.string("To alert you about Safety Cars and emergencies, we need to send you notifications."))
                .font(RacingFont.body())
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(action: action) {
                Text(LocalizationUtils.string("Enable Notifications"))
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
            
            Button(action: finishAction) {
                Text("Comenzar")
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(RacingColors.red)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
        }
        .padding()
    }
}
