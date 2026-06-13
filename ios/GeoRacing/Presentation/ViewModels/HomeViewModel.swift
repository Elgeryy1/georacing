import Foundation
import SwiftUI
import Combine

struct WeatherInfo {
    let tempC: Double
    let condition: String // "Sun", "Cloud", "Rain"
    let iconName: String
}

struct NewsItem: Identifiable, Sendable {
    let id: String
    let title: String
    let subtitle: String?
    let imageUrl: String // Asset name or URL
    let isEvent: Bool
}

// ... TrackStatus is now in Domain/Models/TrackStatus.swift ...

@MainActor
class HomeViewModel: ObservableObject {

    @Published var weather: WeatherInfo?
    @Published var currentTrackStatus: TrackStatus = .green // Default
    @Published var greeting: String = ""
    
    // We keep NewsItem for other cards if needed, but Hero is now Status
    @Published var newsItems: [NewsItem] = []
    
    init() {
        updateGreeting()
        loadWidgets()
        fetchData()
    }
    
    private func updateGreeting() {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { greeting = LocalizationUtils.string("Good Morning") }
        else if hour < 18 { greeting = LocalizationUtils.string("Good Afternoon") }
        else { greeting = LocalizationUtils.string("Good Evening") }
    }
    
    func fetchData() {
        // Mock Weather (No API found yet)
        self.weather = WeatherInfo(tempC: 22.5, condition: "Sunny", iconName: "sun.max.fill")
            
        // Fetch News
        Task {
            do {
                let articles = try await NewsRepository.shared.fetchNews()
                let items = articles.map { article in
                    NewsItem(
                        id: article.id,
                        title: article.title,
                        subtitle: article.subtitle,
                        imageUrl: article.imageUrl ?? "circuit_hero",
                        isEvent: false // Default to false unless flagged
                    )
                }
                await MainActor.run {
                    self.newsItems = items
                }
            } catch {
                Logger.error("[Home][ERROR] Failed to fetch news: \(error)")
                // Keep Mock if fail? Or empty?
                // Let's keep one mock item if empty so UI isn't broken
                if self.newsItems.isEmpty {
                     await MainActor.run {
                         self.newsItems = [
                            NewsItem(id: "mock1", title: "Welcome to GeoRacing", subtitle: "Live updates from the circuit", imageUrl: "circuit_hero", isEvent: true)
                         ]
                     }
                }
            }
        }
    }
    
    var currentDateString: String {
        let formatter = DateFormatter()
        formatter.locale = LocalizationUtils.locale
        formatter.dateStyle = .full
        return formatter.string(from: Date())
    }
    
    // MARK: - Dashboard Customization
    
    @Published var activeWidgetIds: [String] = []
    
    static let allAvailableWidgets: [DashboardWidget] = [
        DashboardWidget(id: "map", icon: "map.fill", titleKey: "Map", color: .blue),
        DashboardWidget(id: "shop", icon: "cart.fill", titleKey: "Shop", color: .green),
        DashboardWidget(id: "food", icon: "fork.knife", titleKey: "Food", color: .orange),
        DashboardWidget(id: "wc", icon: "toilet.fill", titleKey: "WC", color: .purple),
        DashboardWidget(id: "parking", icon: "parkingsign.circle.fill", titleKey: "Parking", color: .gray),
        DashboardWidget(id: "schedule", icon: "calendar", titleKey: "Schedule", color: .pink),
        DashboardWidget(id: "social", icon: "person.2.fill", titleKey: "Social", color: .indigo),
        DashboardWidget(id: "incidents", icon: "exclamationmark.shield.fill", titleKey: "Incidents", color: RacingColors.red),
        // NEW WIDGETS
        DashboardWidget(id: "tickets", icon: "ticket.fill", titleKey: "Billetes", color: .teal),
        DashboardWidget(id: "video", icon: "play.tv.fill", titleKey: "Live TV", color: .pink),
        DashboardWidget(id: "weather", icon: "cloud.sun.fill", titleKey: "El Tiempo", color: .cyan),
        DashboardWidget(id: "profile", icon: "person.crop.circle", titleKey: "Perfil", color: .gray),
        DashboardWidget(id: "fanzone", icon: "flag.2.crossed.fill", titleKey: "Fan Zone", color: .orange) // NEW
    ]
    
    func loadWidgets() {
        self.activeWidgetIds = UserPreferences.shared.dashboardWidgets
    }
    
    func updateWidgets(_ newOrder: [String]) {
        self.activeWidgetIds = newOrder
        UserPreferences.shared.dashboardWidgets = newOrder
    }
}

struct DashboardWidget: Identifiable, Equatable {
    let id: String
    let icon: String
    let titleKey: String
    let color: Color
}
