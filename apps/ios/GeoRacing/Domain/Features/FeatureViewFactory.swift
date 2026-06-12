import SwiftUI

struct FeatureViewFactory {
    
    @ViewBuilder
    static func view(for feature: Feature) -> some View {
        // Here we map Feature IDs to real implementations if they exist.
        // Otherwise, we return the FeaturePlaceholderView.
        
        switch feature.id {
            
        // --- CORE ---
        case "core.notifications", "core.alerts":
            AlertsView()
        
        case "core.pois", "core.offline_map":
            CircuitMapView()
            
        case "core.poi_list":
            PoiListView()
            
        case "core.feedback":
            IncidentReportView()
            
        case "core.qr_position", "social.qr_share":
            SocialView()
            
        case "core.qr_scanner":
            QRScannerView { _ in }
            
        // --- NAVIGATION ---
        case "nav.evacuation":
            EvacuationView()
            
        // --- SOCIAL ---
        case "social.follow_group":
            GroupView()
            
        // --- COMMERCE ---
        case "commerce.orders", "commerce.shop":
            OrdersView()
            
        case "commerce.history", "commerce.my_orders":
            MyOrdersView()
            
        // --- SETTINGS ---
        case "settings.seat", "seat.setup":
            SeatSetupView()
            
        case "settings.main":
            SettingsView()
            
        // --- STAFF ---
        case "staff.panel", "staff.mode":
            StaffModeView()
            
        // --- ROADMAP ---
        case "roadmap", "app.roadmap":
            RoadmapView()
            
        // --- DEFAULT / PLACEHOLDER ---
        default:
            FeaturePlaceholderView(feature: feature)
        }
    }
}
