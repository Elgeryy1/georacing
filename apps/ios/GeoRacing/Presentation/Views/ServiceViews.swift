import SwiftUI

// MARK: - Orders / Ecommerce

struct LegacyOrdersView: View {
    @StateObject private var cartManager = CartManager()
    
    // Mock Products
    // Products loaded via CartManager
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
            
            VStack {
                // Header
                Text(LocalizationUtils.string("Fan Shop"))
                    .font(RacingFont.header(30))
                    .foregroundColor(.white)
                    .padding(.top)
                
                ScrollView {
                    LazyVStack(spacing: 16) {
                        if cartManager.products.isEmpty {
                             ProgressView().foregroundColor(.white).padding()
                        } else {
                            ForEach(cartManager.products) { product in
                                HStack {
                                    VStack(alignment: .leading) {
                                        HStack {
                                            // Product Image (Web Parity)
                                            if let urlString = product.imageUrl, let url = URL(string: urlString) {
                                                AsyncImage(url: url) { phase in
                                                    switch phase {
                                                    case .empty:
                                                        ProgressView().frame(width: 40, height: 40)
                                                    case .success(let image):
                                                        image.resizable()
                                                             .aspectRatio(contentMode: .fit)
                                                             .frame(width: 40, height: 40)
                                                             .cornerRadius(4)
                                                    case .failure:
                                                        Image(systemName: product.emoji ?? "shippingbox.fill").font(.title).foregroundColor(.white)
                                                    @unknown default:
                                                        Image(systemName: product.emoji ?? "shippingbox.fill").font(.title).foregroundColor(.white)
                                                    }
                                                }
                                            } else {
                                                Image(systemName: product.emoji ?? "shippingbox.fill").font(.title).foregroundColor(.white)
                                            }
                                            
                                            Text(product.name)
                                                .font(RacingFont.subheader())
                                                .foregroundColor(.white)
                                        }
                                        Text("$\(product.price, specifier: "%.2f")")
                                            .font(RacingFont.body())
                                            .foregroundColor(RacingColors.silver)
                                    }
                                    Spacer()
                                    Button(LocalizationUtils.string("Add")) {
                                        cartManager.add(product: product)
                                    }
                                    .font(RacingFont.body().bold())
                                    .padding(8)
                                    .background(RacingColors.red)
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                                }
                                .padding()
                                .background(RacingColors.cardBackground)
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(RacingColors.silver.opacity(0.1), lineWidth: 1)
                                )
                            }
                        }
                    }
                    .padding()
                }
                .onAppear {
                    cartManager.loadProducts()
                }
                
                if !cartManager.items.isEmpty {
                    VStack {
                        HStack {
                            Text(LocalizationUtils.string("Cart Total: $"))
                                .font(RacingFont.body())
                                .foregroundColor(RacingColors.silver)
                            Text("\(cartManager.total, specifier: "%.2f")")
                                .font(RacingFont.subheader())
                                .foregroundColor(.white)
                        }
                        
                        Button(action: { Task { try? await cartManager.checkout() } }) {
                            Text(LocalizationUtils.string("Checkout"))
                        }
                        .racingButton(color: RacingColors.red)
                    }
                    .padding()
                    .background(RacingColors.cardBackground)
                    .cornerRadius(16)
                    .padding()
                }
            }
        }
        .alert("Checkout Success", isPresented: $cartManager.checkoutSuccess) {
            Button("OK", role: .cancel) { }
        } message: {
            Text("Your order has been placed!")
        }
        .alert("Error", isPresented: Binding<Bool>(
            get: { cartManager.errorMessage != nil },
            set: { _ in cartManager.errorMessage = nil }
        )) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(cartManager.errorMessage ?? "Unknown error")
        }
    }
}

// MARK: - Alerts

struct AlertsView: View {
    @StateObject private var viewModel = AlertsViewModel()
    @EnvironmentObject private var circuitState: HybridCircuitStateRepository
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
            
            VStack {
                Text(LocalizationUtils.string("Alerts Title"))
                    .font(RacingFont.header(30))
                    .foregroundColor(.white)
                    .padding(.top)

                // Live circuit status banner
                HStack {
                    Circle()
                        .fill(liveTrackStatus.color)
                        .frame(width: 12, height: 12)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(LocalizationUtils.string(liveTrackStatus.titleKey))
                            .font(RacingFont.subheader())
                            .foregroundColor(.white)
                        Text(circuitState.message.isEmpty ? LocalizationUtils.string(liveTrackStatus.messageKey) : circuitState.message)
                            .font(RacingFont.body(12))
                            .foregroundColor(RacingColors.silver)
                        if !circuitState.updatedAt.isEmpty {
                            Text(circuitState.updatedAt)
                                .font(RacingFont.body(11))
                                .foregroundColor(RacingColors.silver.opacity(0.8))
                        }
                    }
                    Spacer()
                }
                .padding()
                .background(
                    (liveTrackStatus == .sc || liveTrackStatus == .vsc) ? Color.orange.opacity(0.3) : RacingColors.cardBackground
                )
                .cornerRadius(12)
                .padding(.horizontal)
                
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.alerts) { alert in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(color(for: alert.priority))
                                    Text(alert.title)
                                        .font(RacingFont.subheader())
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text(viewModel.timeAgo(for: alert.timestamp))
                                        .font(RacingFont.body(12))
                                        .foregroundColor(RacingColors.silver)
                                }
                                Text(alert.message)
                                    .font(RacingFont.body())
                                    .foregroundColor(RacingColors.silver)
                            }
                            .padding()
                            .background(RacingColors.cardBackground)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(color(for: alert.priority), lineWidth: 1)
                            )
                        }
                    }
                    .padding()
                }
            }
            .onAppear {
                viewModel.fetchAlerts()
            }
        }
    }
    
    func color(for priority: AlertPriority) -> Color {
        switch priority {
        case .high: return RacingColors.red
        case .medium: return .orange
        case .low: return .green
        }
    }

    private var liveTrackStatus: TrackStatus {
        circuitState.resolvedTrackStatus()
    }
}

// MARK: - Seat Setup

// MARK: - Settings

struct LegacySettingsView: View {
    @State private var grandstand: String = UserPreferences.shared.grandstand ?? ""
    @State private var zone: String = UserPreferences.shared.zone ?? ""
    @State private var row: String = UserPreferences.shared.row ?? ""
    @State private var seat: String = UserPreferences.shared.seatNumber ?? ""
    
    @State private var selectedLanguage: String = UserPreferences.shared.languageCode
    @State private var selectedTheme: UserPreferences.AppTheme = UserPreferences.shared.theme
    
    let languages = [("English", "en"), ("Español", "es"), ("Català", "ca")]
    
    // For Settings, Form is very convenient. We can try to keep it but apply dark theme?
    // SwiftUI Forms can be stubborn. Let's wrap in ZStack and use .scrollContentBackground(.hidden) if iOS 16, else just standard form.
    // For now, let's stick to standard navigation view for Settings as it's a utility screen, but ensure colors are okay.
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(LocalizationUtils.string("General"))) {
                    Picker(LocalizationUtils.string("Language"), selection: $selectedLanguage) {
                        ForEach(languages, id: \.1) { language in
                            Text(language.0).tag(language.1)
                        }
                    }
                    .onChange(of: selectedLanguage) {
                        UserPreferences.shared.languageCode = selectedLanguage
                    }
                    
                    Picker(LocalizationUtils.string("Appearance"), selection: $selectedTheme) {
                        Text("System").tag(UserPreferences.AppTheme.system)
                        Text("Light").tag(UserPreferences.AppTheme.light)
                        Text("Dark").tag(UserPreferences.AppTheme.dark)
                    }
                    .onChange(of: selectedTheme) {
                        UserPreferences.shared.theme = selectedTheme
                    }
                }
                
                Section(header: Text(LocalizationUtils.string("My Seat Location"))) {
                    TextField(LocalizationUtils.string("Grandstand"), text: $grandstand)
                    TextField(LocalizationUtils.string("Zone"), text: $zone)
                }
                
                Section(header: Text(LocalizationUtils.string("Seat Details"))) {
                    TextField(LocalizationUtils.string("Row"), text: $row)
                    TextField(LocalizationUtils.string("Seat Number"), text: $seat)
                }
                
                Button(action: {
                    UserPreferences.shared.grandstand = grandstand
                    UserPreferences.shared.zone = zone
                    UserPreferences.shared.row = row
                    UserPreferences.shared.seatNumber = seat
                }) {
                    Text(LocalizationUtils.string("Save Seat Config"))
                        .foregroundColor(RacingColors.red)
                }
                
                Section(header: Text("Race Control (Admin)")) {
                    NavigationLink(destination: CircuitControlView()) {
                         Text("Circuit Status & Messaging")
                    }
                }
            }
            .navigationTitle(LocalizationUtils.string("Settings"))
        }
        // Force simple dark style if preferred scheme is dark, handled by ContentView
    }
}
