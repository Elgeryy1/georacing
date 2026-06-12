import SwiftUI

struct SideMenuView: View {
    @Binding var isShowing: Bool
    @Binding var selectedTab: TabIdentifier
    
    // Callbacks for non-tab actions
    var onOverview: () -> Void
    var onSelectFeature: (Feature) -> Void
    
    // Legacy callbacks kept for compatibility / if redirected
    var onSocial: () -> Void
    var onReport: () -> Void
    var onCircuitControl: () -> Void
    
    var body: some View {
        ZStack {
            if isShowing {
                // Dimmed background
                Color.black.opacity(0.5)
                    .ignoresSafeArea()
                    .onTapGesture {
                        withAnimation { isShowing = false }
                    }
                
                // Menu Content
                HStack {
                    VStack(alignment: .leading, spacing: 20) {
                        // Header
                        VStack(alignment: .leading) {
                            Image(systemName: "flag.checkered")
                                .resizable()
                                .frame(width: 50, height: 50)
                                .foregroundColor(.white)
                            Text("GeoRacing")
                                .font(RacingFont.header(24))
                                .foregroundColor(.white)
                            Text("Driver Menu")
                                .font(RacingFont.body())
                                .foregroundColor(.gray)
                        }
                        .padding(.top, 50)
                        .padding(.horizontal)
                        
                        Divider().background(Color.gray)
                        
                        // Menu Items
                        ScrollView {
                            VStack(alignment: .leading, spacing: 10) {
                                
                                // Navigation Items (Tabs)
                                MenuRow(icon: "house.fill", text: "Home", action: { selectTab(.home) })
                                MenuRow(icon: "map.fill", text: "Circuit Map", action: { selectTab(.map) })
                                MenuRow(icon: "bell.fill", text: "Alerts", action: { selectTab(.alerts) })
                                MenuRow(icon: "cart.fill", text: "Shop", action: { selectTab(.shop) })
                                MenuRow(icon: "gear", text: "Settings", action: { selectTab(.seat) })
                                
                                Divider().background(Color.gray).padding(.vertical, 10)
                                
                                // Quick Access Items
                                Text(LocalizationUtils.string("Quick Access"))
                                    .font(.caption.bold())
                                    .foregroundColor(.gray)
                                    .padding(.leading)
                                
                                MenuRow(icon: "bag.fill", text: LocalizationUtils.string("My Orders"), action: {
                                    closeAndRun { onSelectFeature(Feature(id: "commerce.my_orders", title: LocalizationUtils.string("My Orders"), subtitle: LocalizationUtils.string("Purchase History"), category: .fan, priority: 1, status: .complete, icon: "bag.fill", nextSteps: [])) }
                                })
                                MenuRow(icon: "mappin.and.ellipse", text: LocalizationUtils.string("POI List"), action: {
                                    closeAndRun { onSelectFeature(Feature(id: "core.poi_list", title: LocalizationUtils.string("POI List"), subtitle: LocalizationUtils.string("Points of Interest"), category: .core, priority: 1, status: .complete, icon: "mappin.and.ellipse", nextSteps: [])) }
                                })
                                MenuRow(icon: "ticket.fill", text: LocalizationUtils.string("My Seat"), action: {
                                    closeAndRun { onSelectFeature(Feature(id: "seat.setup", title: LocalizationUtils.string("My Seat"), subtitle: LocalizationUtils.string("Seat Setup"), category: .fan, priority: 1, status: .complete, icon: "ticket.fill", nextSteps: [])) }
                                })
                                MenuRow(icon: "chart.bar.fill", text: "Roadmap", action: {
                                    closeAndRun { onSelectFeature(Feature(id: "app.roadmap", title: "Roadmap", subtitle: LocalizationUtils.string("Project Progress"), category: .advanced, priority: 1, status: .complete, icon: "chart.bar.fill", nextSteps: [])) }
                                })
                                MenuRow(icon: "person.badge.key.fill", text: LocalizationUtils.string("Staff Mode"), action: {
                                    closeAndRun { onSelectFeature(Feature(id: "staff.mode", title: LocalizationUtils.string("Staff Mode"), subtitle: LocalizationUtils.string("Control Panel"), category: .staff, priority: 1, status: .complete, icon: "person.badge.key.fill", audience: .staffOnly, nextSteps: [])) }
                                })
                                
                                Divider().background(Color.gray).padding(.vertical, 10)
                                
                                 Divider().background(Color.gray).padding(.vertical, 10)
                                
                                 // -- FUNCIONES GEORACING --
                                 Text(LocalizationUtils.string("GeoRacing Features"))
                                    .font(.caption.bold())
                                    .foregroundColor(.gray)
                                    .padding(.leading)
                                 
                                 // Quick Access: Overview
                                 MenuRow(icon: "square.grid.2x2.fill", text: LocalizationUtils.string("Overview"), action: {
                                    closeAndRun(onOverview)
                                 })
                                 
                                 // Categories
                                 ForEach(FeatureCategory.allCases) { category in
                                     let features = FeatureRegistry.shared.features(for: category)
                                     if !features.isEmpty {
                                         DisclosureGroup(
                                             content: {
                                                 ForEach(features) { feature in
                                                     Button(action: { closeAndRun { onSelectFeature(feature) } }) {
                                                         HStack {
                                                             Image(systemName: feature.icon)
                                                                 .frame(width: 20)
                                                             Text(feature.title)
                                                                 .font(RacingFont.body(14))
                                                             Spacer()
                                                             if feature.status != .complete {
                                                                 Circle()
                                                                     .fill(feature.status.color)
                                                                     .frame(width: 6, height: 6)
                                                             }
                                                         }
                                                         .foregroundColor(.white)
                                                         .padding(.vertical, 8)
                                                         .padding(.leading, 20)
                                                     }
                                                 }
                                             },
                                             label: {
                                                 HStack {
                                                     Image(systemName: category.icon)
                                                         .foregroundColor(category.color)
                                                         .frame(width: 20)
                                                     Text(category.rawValue)
                                                         .font(RacingFont.subheader())
                                                         .foregroundColor(.white)
                                                     Spacer()
                                                     Text("\(FeatureRegistry.shared.completedCount(for: category))/\(features.count)")
                                                         .font(.caption)
                                                         .foregroundColor(.gray)
                                                 }
                                                 .padding(.vertical, 4)
                                             }
                                         )
                                         .accentColor(.gray)
                                         .padding(.horizontal)
                                     }
                                 }
                                 
                                 Divider().background(Color.gray).padding(.vertical, 10)
                                 
                                 // -- LEGACY ACTIONS (Kept for compatibility if needed, but redundant now usually) --
                                 /*
                                 MenuRow(icon: "person.3.fill", text: "Social / Group", action: {
                                     closeAndRun(onSocial)
                                 })
                                 */
                            }
                        }
                        
                        Spacer()
                        
                        // Footer
                        Text("v1.0.0 (Parity Build)")
                            .font(.caption)
                            .foregroundColor(.gray)
                            .padding()
                    }
                    .frame(width: 280)
                    .background(RacingColors.darkBackground)
                    .offset(x: isShowing ? 0 : -280) // Slide animation logic handled by parent usually, but here helps
                    
                    Spacer()
                }
                .transition(.move(edge: .leading))
            }
        }
        // No animation modifier here, handled by parent ZStack insertion or state change
    }
    
    private func selectTab(_ tab: TabIdentifier) {
        selectedTab = tab
        withAnimation { isShowing = false }
    }
    
    private func closeAndRun(_ action: @escaping () -> Void) {
        withAnimation { isShowing = false }
        // Delay slightly to allow menu to close?
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            action()
        }
    }
}

struct MenuRow: View {
    let icon: String
    let text: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .frame(width: 24, height: 24)
                    .foregroundColor(RacingColors.silver)
                Text(text)
                    .font(RacingFont.subheader())
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
        }
        .accessibilityLabel(text)
    }
}
