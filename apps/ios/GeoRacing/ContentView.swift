//
//  ContentView.swift
//  GeoRacing
//
//  Created by Daniel Colet on 15/12/25.
//

import SwiftUI

struct ContentView: View {
    
    @State private var selectedTab: TabIdentifier = .home
    @StateObject private var circuitState = HybridCircuitStateRepository()
    
    // Side Menu States
    @State private var isShowingSideMenu = false
    @State private var showSocial = false
    @State private var showReport = false
    @State private var showControl = false
    @State private var showOverview = false
    @State private var selectedFeature: Feature? = nil
    @State private var showParking = false // Parking Module State
    
    // Watch for theme changes via Preferences or Environment? 
    // Since UserPreferences is a singleton, we might need a small observable wrapper or manually check on appear.
    // For simplicity, let's use AppStorage for themeID if possible, or just read it.
    @AppStorage("theme") private var themeRaw: String = "system"
    @AppStorage("language") private var languageCode: String = "es" // Watch language to trigger redraw
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding: Bool = false
    
    @ObservedObject private var authService = AuthService.shared
    
    var body: some View {
        if authService.isAuthenticated {
            ZStack {
                TabView(selection: $selectedTab) {
                    
                    // Tab 0: Home
                    HomeView(selectedTab: $selectedTab, showMenu: $isShowingSideMenu, showParkingSheet: $showParking)
                        .tabItem {
                            Label(LocalizationUtils.string("Home"), systemImage: "house.fill")
                        }
                        .tag(TabIdentifier.home)
                    
                    // Tab 1: Map
                    CircuitMapView()
                        .tabItem {
                            Label(LocalizationUtils.string("Map"), systemImage: "map")
                        }
                        .tag(TabIdentifier.map)
                    
                    // Tab 2: Alerts
                    AlertsView()
                        .tabItem {
                            Label(LocalizationUtils.string("Alerts"), systemImage: "bell")
                        }
                        .tag(TabIdentifier.alerts)
                    
                    // Tab 3: Shop
                    OrdersView()
                        .tabItem {
                            Label(LocalizationUtils.string("Shop"), systemImage: "cart")
                        }
                        .tag(TabIdentifier.shop)
                    
                    // Tab 4: Settings / Seat
                    SettingsView()
                        .tabItem {
                            Label(LocalizationUtils.string("Settings"), systemImage: "gear")
                        }
                        .tag(TabIdentifier.seat)
                }
                
                // Side Menu Overlay
                SideMenuView(
                    isShowing: $isShowingSideMenu,
                    selectedTab: $selectedTab,
                    onOverview: { showOverview = true },
                    onSelectFeature: { feature in
                        self.selectedFeature = feature
                    },
                    onSocial: { showSocial = true },
                    onReport: { showReport = true },
                    onCircuitControl: { showControl = true }
                )
            }
            // Force redraw when language changes (id hack)
            .id(languageCode)
            .preferredColorScheme(scheme(for: themeRaw))
            .environmentObject(circuitState)
            .onAppear { circuitState.start() }
            // Global Evacuation Overlay
            .fullScreenCover(
                isPresented: Binding<Bool>(
                    get: { circuitState.mode == .evacuation },
                    set: { _ in }
                )
            ) {
                EvacuationView()
            }
            // Onboarding Flow
            .fullScreenCover(isPresented: Binding(
                get: { !hasSeenOnboarding },
                set: { _ in }
            )) {
                OnboardingView(isPresented: Binding(
                    get: { true },
                    set: { if !$0 { hasSeenOnboarding = true } }
                ))
            }
            // Side Menu Sheets
            .sheet(isPresented: $showSocial) { SocialView() }
            .sheet(isPresented: $showReport) { IncidentReportView() }
            .sheet(isPresented: $showControl) { NavigationView { CircuitControlView() } }
            .sheet(isPresented: $showParking) { ParkingContainerView() } // Parking Module
            // Feature Navigation
            .sheet(isPresented: $showOverview) { FeaturesOverviewView() }
            .sheet(item: $selectedFeature) { feature in
                // We wrap real views in NavigationView if they don't have one, 
                // but FeatureViewFactory might return view with or without it. 
                // Placeholder has titleDisplayMode inline so it expects nav.
                NavigationView {
                    FeatureViewFactory.view(for: feature)
                        .toolbar {
                             ToolbarItem(placement: .navigationBarTrailing) {
                                 Button(LocalizationUtils.string("Close")) { selectedFeature = nil }
                             }
                        }
                }
            }
        } else {
            LoginView()
                .fullScreenCover(isPresented: Binding(
                    get: { !hasSeenOnboarding },
                    set: { _ in }
                )) {
                    OnboardingView(isPresented: Binding(
                        get: { true },
                        set: { if !$0 { hasSeenOnboarding = true } }
                    ))
                }
        }
    }
    
    func scheme(for raw: String) -> ColorScheme? {
        switch raw {
        case "light": return .light
        case "dark": return .dark
        default: return nil
        }
    }
}

#Preview {
    ContentView()
}
