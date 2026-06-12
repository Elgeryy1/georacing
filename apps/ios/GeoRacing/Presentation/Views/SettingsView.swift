import SwiftUI
import Combine

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showLogoutConfirmation = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    headerView
                    
                    // Language Section
                    settingsSection(title: LocalizationUtils.string("Language")) {
                        languagePicker
                    }
                    
                    // Appearance Section
                    settingsSection(title: LocalizationUtils.string("Appearance")) {
                        themePicker
                    }
                    
                    // Accessibility Section
                    settingsSection(title: "Accessibility") {
                        accessibilityToggles
                    }
                    
                    // Notifications Section
                    settingsSection(title: LocalizationUtils.string("Notifications")) {
                        notificationsToggle
                    }
                    
                    // Account Section
                    settingsSection(title: "Account") {
                        accountButtons
                    }
                    
                    // App Info
                    appInfoSection
                }
                .padding()
            }
        }
        .confirmationDialog(LocalizationUtils.string("Sign Out?"), isPresented: $showLogoutConfirmation) {
            Button(LocalizationUtils.string("Sign Out"), role: .destructive) {
                viewModel.logout()
                dismiss()
            }
            Button(LocalizationUtils.string("Cancel"), role: .cancel) {}
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Text(LocalizationUtils.string("Settings"))
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
    }
    
    // MARK: - Section Builder
    
    private func settingsSection<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .foregroundColor(.orange)
            
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    
    // MARK: - Language Picker
    
    private var languagePicker: some View {
        VStack(spacing: 0) {
            ForEach(SettingsViewModel.Language.allCases, id: \.self) { language in
                Button {
                    viewModel.setLanguage(language)
                } label: {
                    HStack {
                        Image(systemName: language.flag)
                            .font(.title2)
                            .foregroundColor(.orange)
                        Text(language.displayName)
                            .foregroundColor(.white)
                        Spacer()
                        if viewModel.language == language {
                            Image(systemName: "checkmark")
                                .foregroundColor(.orange)
                        }
                    }
                    .padding()
                    .background(Color(white: 0.12))
                }
                
                if language != SettingsViewModel.Language.allCases.last {
                    Divider().background(Color.gray.opacity(0.3))
                }
            }
        }
        .cornerRadius(12)
    }
    
    // MARK: - Theme Picker
    
    private var themePicker: some View {
        VStack(spacing: 0) {
            ForEach(SettingsViewModel.Theme.allCases, id: \.self) { theme in
                Button {
                    viewModel.setTheme(theme)
                } label: {
                    HStack {
                        Image(systemName: theme.icon)
                            .foregroundColor(.gray)
                            .frame(width: 24)
                        Text(theme.displayName)
                            .foregroundColor(.white)
                        Spacer()
                        if viewModel.theme == theme {
                            Image(systemName: "checkmark")
                                .foregroundColor(.orange)
                        }
                    }
                    .padding()
                    .background(Color(white: 0.12))
                }
                
                if theme != SettingsViewModel.Theme.allCases.last {
                    Divider().background(Color.gray.opacity(0.3))
                }
            }
        }
        .cornerRadius(12)
    }
    
    // MARK: - Accessibility Toggles
    
    private var accessibilityToggles: some View {
        VStack(spacing: 0) {
            settingsToggle(
                icon: "textformat.size",
                title: "Texto grande",
                isOn: $viewModel.largeText
            )
            
            Divider().background(Color.gray.opacity(0.3))
            
            settingsToggle(
                icon: "circle.lefthalf.filled",
                title: "Alto contraste",
                isOn: $viewModel.highContrast
            )
        }
        .cornerRadius(12)
    }
    
    // MARK: - Notifications Toggle
    
    private var notificationsToggle: some View {
        settingsToggle(
            icon: "bell.fill",
            title: LocalizationUtils.string("Push Notifications"),
            isOn: $viewModel.notificationsEnabled
        )
        .cornerRadius(12)
    }
    
    private func settingsToggle(icon: String, title: String, isOn: Binding<Bool>) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(.gray)
                .frame(width: 24)
            Text(title)
                .foregroundColor(.white)
            Spacer()
            Toggle("", isOn: isOn)
                .tint(.orange)
        }
        .padding()
        .background(Color(white: 0.12))
    }
    
    // MARK: - Account Buttons
    
    private var accountButtons: some View {
        VStack(spacing: 0) {
            Button {
                // Navigate to seat setup
            } label: {
                HStack {
                    Image(systemName: "ticket")
                        .foregroundColor(.gray)
                        .frame(width: 24)
                    Text(LocalizationUtils.string("Configure my seat"))
                        .foregroundColor(.white)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundColor(.gray)
                }
                .padding()
                .background(Color(white: 0.12))
            }
            
            Divider().background(Color.gray.opacity(0.3))
            
            Button {
                showLogoutConfirmation = true
            } label: {
                HStack {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .foregroundColor(.red)
                        .frame(width: 24)
                    Text(LocalizationUtils.string("Sign Out"))
                        .foregroundColor(.red)
                    Spacer()
                }
                .padding()
                .background(Color(white: 0.12))
            }
        }
        .cornerRadius(12)
    }
    
    // MARK: - App Info
    
    private var appInfoSection: some View {
        VStack(spacing: 8) {
            Text("GeoRacing")
                .font(.headline)
                .foregroundColor(.white)
            Text("Versión \(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")")
                .font(.caption)
                .foregroundColor(.gray)
            Text("© 2026 GeoRacing")
                .font(.caption2)
                .foregroundColor(.gray.opacity(0.6))
        }
        .padding(.top, 24)
    }
}

// MARK: - Settings ViewModel

@MainActor
final class SettingsViewModel: ObservableObject {
    
    // MARK: - Types
    
    enum Language: String, CaseIterable {
        case spanish = "es"
        case english = "en"
        case catalan = "ca"
        
        var displayName: String {
            switch self {
            case .spanish: return "Español"
            case .english: return "English"
            case .catalan: return "Català"
            }
        }
        
        var flag: String {
            switch self {
            case .spanish: return "s.circle.fill"
            case .english: return "e.circle.fill"
            case .catalan: return "c.circle.fill"
            }
        }
    }
    
    enum Theme: String, CaseIterable {
        case system = "system"
        case light = "light"
        case dark = "dark"
        
        var displayName: String {
            switch self {
            case .system: return "Automático"
            case .light: return "Claro"
            case .dark: return "Oscuro"
            }
        }
        
        var icon: String {
            switch self {
            case .system: return "circle.lefthalf.filled"
            case .light: return "sun.max.fill"
            case .dark: return "moon.fill"
            }
        }
    }
    
    // MARK: - Published Properties
    
    @Published var language: Language
    @Published var theme: Theme
    @Published var largeText: Bool
    @Published var highContrast: Bool
    @Published var notificationsEnabled: Bool
    
    // MARK: - Private
    
    private let defaults = UserDefaults.standard
    private let authService = AuthService.shared
    
    // MARK: - Keys
    
    private enum Keys {
        static let language = "settings.language"
        static let theme = "settings.theme"
        static let largeText = "settings.largeText"
        static let highContrast = "settings.highContrast"
        static let notifications = "settings.notifications"
    }
    
    // MARK: - Initialization
    
    init() {
        self.language = Language(rawValue: defaults.string(forKey: Keys.language) ?? "es") ?? .spanish
        self.theme = Theme(rawValue: defaults.string(forKey: Keys.theme) ?? "system") ?? .system
        self.largeText = defaults.bool(forKey: Keys.largeText)
        self.highContrast = defaults.bool(forKey: Keys.highContrast)
        self.notificationsEnabled = defaults.bool(forKey: Keys.notifications)
    }
    
    // MARK: - Actions
    
    func setLanguage(_ language: Language) {
        self.language = language
        defaults.set(language.rawValue, forKey: Keys.language)
    }
    
    func setTheme(_ theme: Theme) {
        self.theme = theme
        defaults.set(theme.rawValue, forKey: Keys.theme)
    }
    
    func logout() {
        authService.signOut()
    }
}

#Preview {
    SettingsView()
}
