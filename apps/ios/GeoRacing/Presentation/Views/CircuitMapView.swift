import SwiftUI
import Combine
import CoreLocation
import MapKit

struct CircuitMapView: View {
    
    @StateObject private var viewModel = MapViewModel()
    @StateObject private var energyService = EnergyManagementService.shared
    
    // UI State
    @State private var selectedPOI: Poi? = nil
    @State private var showPoiDetail: Bool = false
    @State private var showTransportSheet: Bool = false
    @State private var showGPSNavigation: Bool = false
    
    var body: some View {
        ZStack(alignment: .top) {
            
            MapViewRepresentable(
                region: $viewModel.region,
                annotations: viewModel.allAnnotations,
                routePolyline: viewModel.routePolyline,
                shadowPolygons: viewModel.shadowPolygons,
                userTrackingMode: viewModel.userTrackingMode,
                selectedPOI: $selectedPOI,
                showPoiDetail: $showPoiDetail
            )
            .edgesIgnoringSafeArea(.all)
            
            // 2. Navigation Top Banner (Turn-by-Turn)
            if viewModel.showNavigationOverlay, let route = viewModel.activeRoute, viewModel.activeWebUrl == nil {
                TopNavigationBanner(
                    route: route,
                    currentStepIndex: viewModel.currentStepIndex,
                    onNext: { viewModel.nextStep() },
                    onPrev: { viewModel.prevStep() },
                    onClose: { viewModel.endNavigation() }
                )
                .transition(.move(edge: .top))
            }
            
            // 3. UI Overlay: Filter Bar (Hidden when navigating)
            if !viewModel.showNavigationOverlay {
                VStack {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            FilterChip(title: "All", isSelected: viewModel.selectedCategory == nil) {
                                viewModel.filterPOIs(by: nil)
                            }
                            ForEach(PoiType.allCases, id: \.self) { type in
                                FilterChip(title: type.rawValue.capitalized, isSelected: viewModel.selectedCategory == type) {
                                    viewModel.filterPOIs(by: type)
                                }
                            }
                        }
                        .padding()
                    }
                    .background(
                        LinearGradient(gradient: Gradient(colors: [Color.black.opacity(0.7), Color.black.opacity(0.0)]), startPoint: .top, endPoint: .bottom)
                    )
                    
                    Spacer()
                }
            }
            
            // 4. Bottom Controls (Navigation or Standard)
            VStack {
                Spacer()
                
                if viewModel.showNavigationOverlay {
                    // Navigation Bottom Info
                    if let route = viewModel.activeRoute {
                        NavigationBottomPanel(
                            route: route,
                            transportMode: viewModel.transportMode,
                            onModeChange: { viewModel.setTransportMode($0) },
                            onEndNavigation: { viewModel.endNavigation() }
                        )
                        .transition(.move(edge: .bottom))
                    }
                } else {
                    // Standard Bottom Buttons
                    HStack(spacing: 12) {
                        // Navigate to Circuit Button — opens full GPS navigation
                        Button(action: {
                            showGPSNavigation = true
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "arrow.triangle.turn.up.right.circle.fill")
                                Text(LocalizationUtils.string("Go to Circuit"))
                                    .font(RacingFont.body(14).bold())
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(RacingColors.red)
                            .cornerRadius(25)
                            .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
                        }
                        
                        Spacer()
                        
                        // Recenter Button
                        Button(action: {
                            if let userLoc = viewModel.userLocation {
                                withAnimation {
                                    viewModel.region.center = userLoc
                                    viewModel.region.span = MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                                }
                            } else if let circuit = viewModel.circuit {
                                withAnimation {
                                    viewModel.region.center = circuit.bounds.center
                                    viewModel.region.span = MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
                                }
                            }
                        }) {
                            Image(systemName: "location.circle.fill")
                                .resizable()
                                .frame(width: 44, height: 44)
                                .foregroundColor(RacingColors.red)
                                .background(Color.white)
                                .clipShape(Circle())
                                .shadow(radius: 4)
                        }
                    }
                    .padding()
                }
            }
            
            // Error Toast
            if let error = viewModel.navigationError {
                VStack {
                    Spacer()
                    Text(error)
                        .font(RacingFont.body(14))
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.red.opacity(0.9))
                        .cornerRadius(8)
                        .padding(.bottom, 100)
                }
                .transition(.opacity)
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        viewModel.navigationError = nil
                    }
                }
            }
        }
        .sheet(isPresented: $showPoiDetail) {
            if let poi = selectedPOI {
                PoiDetailSheet(poi: poi, viewModel: viewModel)
            }
        }
        .sheet(isPresented: $viewModel.showTransportSheet) {
            PublicTransportSheetView(mapViewModel: viewModel)
        }
        .fullScreenCover(isPresented: $showGPSNavigation) {
            NavigationScreen()
        }
        .animation(.spring(response: 0.3), value: viewModel.showNavigationOverlay)
        .preferredColorScheme(energyService.isSurvivalMode ? .dark : nil)
    }
}

// MARK: - Banner & Overlay

struct TopNavigationBanner: View {
    let route: NavigationRoute
    let currentStepIndex: Int
    let onNext: () -> Void
    let onPrev: () -> Void
    let onClose: () -> Void
    
    var currentStep: MKRoute.Step? {
        if currentStepIndex < route.steps.count {
            return route.steps[currentStepIndex]
        }
        return nil
    }
    
    var nextStep: MKRoute.Step? {
        if currentStepIndex + 1 < route.steps.count {
            return route.steps[currentStepIndex + 1]
        }
        return nil
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Main Instruction Bar
            HStack(alignment: .top, spacing: 16) {
                // Direction Icon (Big)
                Image(systemName: "arrow.turn.up.right") // Placeholder, ideally dynamic
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 40)
                    .padding(.top, 4)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(currentStep?.instructions ?? LocalizationUtils.string("Follow the route"))
                        .font(RacingFont.header(20))
                        .foregroundColor(.white)
                        .fixedSize(horizontal: false, vertical: true)
                    
                    if let dist = currentStep?.distance {
                        Text("\(Int(dist)) m")
                            .font(RacingFont.header(24))
                            .foregroundColor(RacingColors.silver)
                    }
                }
                
                Spacer()
                
                // Controls
                VStack(spacing: 12) {
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.headline)
                            .foregroundColor(RacingColors.silver)
                            .padding(8)
                            .background(Color.white.opacity(0.1))
                            .clipShape(Circle())
                    }
                    
                    HStack(spacing: 2) {
                        Button(action: onPrev) {
                            Image(systemName: "chevron.left")
                                .padding(8)
                        }
                        .disabled(currentStepIndex == 0)
                        
                        Divider().frame(height: 20).background(Color.white.opacity(0.2))
                        
                        Button(action: onNext) {
                            Image(systemName: "chevron.right")
                                .padding(8)
                        }
                        .disabled(currentStepIndex >= route.steps.count - 1)
                    }
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(8)
                }
                .foregroundColor(.white)
            }
            .padding()
            .background(RacingColors.cardBackground)
            .padding(.top, 44) // Safe area
            
            // Next Step Preview
            if let next = nextStep {
                HStack {
                    Text("Después: \(next.instructions)")
                        .font(RacingFont.body(14))
                        .foregroundColor(RacingColors.silver)
                        .lineLimit(1)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(RacingColors.darkBackground.opacity(0.9))
            }
        }
        .cornerRadius(0)
        .shadow(radius: 10)
    }
}

struct NavigationBottomPanel: View {
    let route: NavigationRoute
    let transportMode: TransportMode
    let onModeChange: (TransportMode) -> Void
    let onEndNavigation: () -> Void
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading) {
                    Text(route.destinationName)
                        .font(RacingFont.header(16))
                        .foregroundColor(.white)
                    HStack {
                        Label(route.formattedETA, systemImage: "clock.fill")
                            .foregroundColor(RacingColors.red)
                        Text("•")
                        Text(route.formattedDistance)
                    }
                    .font(RacingFont.body(14))
                    .foregroundColor(RacingColors.silver)
                }
                Spacer()
                
                Button(action: onEndNavigation) {
                    Text(LocalizationUtils.string("Exit"))
                        .font(RacingFont.body(14).bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Color.gray.opacity(0.5))
                        .cornerRadius(20)
                }
            }
            .padding(.bottom, 4)
            
            // Mode Selector
            HStack(spacing: 8) {
                ForEach(TransportMode.allCases, id: \.self) { mode in
                    Button(action: { onModeChange(mode) }) {
                        Image(systemName: mode.icon)
                            .font(.body)
                            .foregroundColor(transportMode == mode ? .white : RacingColors.silver)
                            .padding(10)
                            .background(transportMode == mode ? RacingColors.red : Color.white.opacity(0.1))
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(RacingColors.cardBackground)
                .shadow(radius: 5)
        )
        .padding()
    }
}

// MARK: - Map View Representable (UIKit wrapper)

struct MapViewRepresentable: UIViewRepresentable {
    @Binding var region: MKCoordinateRegion
    let annotations: [MapAnnotationItem]
    let routePolyline: MKPolyline?
    let shadowPolygons: [MKPolygon]
    let userTrackingMode: MapUserTrackingMode
    @Binding var selectedPOI: Poi?
    @Binding var showPoiDetail: Bool
    
    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.showsUserLocation = true
        mapView.setRegion(region, animated: false)
        return mapView
    }
    
    func updateUIView(_ mapView: MKMapView, context: Context) {
        // Update User Tracking
        switch userTrackingMode {
        case .none:
            mapView.userTrackingMode = .none
        case .follow:
            if mapView.userTrackingMode != .follow {
                mapView.setUserTrackingMode(.follow, animated: true)
            }
        case .followWithHeading:
            if mapView.userTrackingMode != .followWithHeading {
                mapView.setUserTrackingMode(.followWithHeading, animated: true)
            }
        }
        
        // Update region if NOT tracking (otherwise tracking handles it)
        if userTrackingMode == .none && !context.coordinator.isUserInteracting {
            mapView.setRegion(region, animated: true)
        }
        
        // Update annotations
        let existingAnnotations = mapView.annotations.compactMap { $0 as? MapPinAnnotation }
        let newIds = Set(annotations.map { $0.id })
        let existingIds = Set(existingAnnotations.map { $0.id })
        
        // Remove old
        let toRemove = existingAnnotations.filter { !newIds.contains($0.id) }
        mapView.removeAnnotations(toRemove)
        
        // Add new
        let toAdd = annotations.filter { !existingIds.contains($0.id) }
        for item in toAdd {
            let annotation = MapPinAnnotation(item: item)
            mapView.addAnnotation(annotation)
        }
        
        // Update route overlay
        if let existingOverlay = mapView.overlays.first(where: { $0 is MKPolyline }) as? MKPolyline {
            if routePolyline == nil || existingOverlay !== routePolyline {
                mapView.removeOverlay(existingOverlay)
            }
        }
        
        if let polyline = routePolyline, !mapView.overlays.contains(where: { $0 === polyline }) {
            mapView.addOverlay(polyline, level: .aboveRoads)
        }
        
        // Update Thermal Overlays
        let existingPolygons = mapView.overlays.compactMap { $0 as? MKPolygon }
        let newPolygons = shadowPolygons.filter { newPoly in
            !existingPolygons.contains(where: { $0.title == newPoly.title })
        }
        if !newPolygons.isEmpty {
            mapView.addOverlays(newPolygons, level: .aboveLabels)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: MapViewRepresentable
        var isUserInteracting = false
        
        init(_ parent: MapViewRepresentable) {
            self.parent = parent
        }
        
        func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
            isUserInteracting = true
        }
        
        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            isUserInteracting = false
            // Don't update parent region if tracking, creates loop
            if parent.userTrackingMode == .none {
                parent.region = mapView.region
            }
        }
        
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let pinAnnotation = annotation as? MapPinAnnotation else { return nil }
            
            let identifier = "MapPin"
            var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
            
            if annotationView == nil {
                annotationView = MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
                annotationView?.canShowCallout = false
            }
            
            switch pinAnnotation.item.type {
            case .poi(let poi):
                let hostingView = UIHostingController(rootView: POIMarker(poi: poi))
                hostingView.view.backgroundColor = .clear
                hostingView.view.frame = CGRect(x: 0, y: 0, width: 50, height: 60)
                annotationView?.addSubview(hostingView.view)
                annotationView?.frame = hostingView.view.frame
                
            case .friend(let friend):
                let hostingView = UIHostingController(rootView: FriendMarker(friend: friend))
                hostingView.view.backgroundColor = .clear
                hostingView.view.frame = CGRect(x: 0, y: 0, width: 50, height: 60)
                annotationView?.addSubview(hostingView.view)
                annotationView?.frame = hostingView.view.frame
                
            case .beacon(let beacon):
                let hostingView = UIHostingController(rootView: BeaconMarker(beacon: beacon))
                hostingView.view.backgroundColor = .clear
                hostingView.view.frame = CGRect(x: 0, y: 0, width: 40, height: 40)
                annotationView?.addSubview(hostingView.view)
                annotationView?.frame = hostingView.view.frame
            }
            
            return annotationView
        }
        
        func mapView(_ mapView: MKMapView, didSelect annotation: MKAnnotation) {
            guard let pinAnnotation = annotation as? MapPinAnnotation else { return }
            
            if case .poi(let poi) = pinAnnotation.item.type {
                parent.selectedPOI = poi
                parent.showPoiDetail = true
            }
            
            mapView.deselectAnnotation(annotation, animated: false)
        }
        
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: polyline)
                renderer.strokeColor = UIColor(RacingColors.red)
                renderer.lineWidth = 5
                renderer.lineCap = .round
                return renderer
            } else if let polygon = overlay as? MKPolygon {
                // Thermal Navigation: renders shadows as semi-transparent blue.
                let renderer = MKPolygonRenderer(polygon: polygon)
                renderer.fillColor = UIColor.systemBlue.withAlphaComponent(0.3)
                renderer.strokeColor = UIColor.systemBlue.withAlphaComponent(0.5)
                renderer.lineWidth = 1
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }
    }
}

// MARK: - Map Annotation Wrapper

class MapPinAnnotation: NSObject, MKAnnotation {
    let id: String
    let item: MapAnnotationItem
    
    var coordinate: CLLocationCoordinate2D {
        item.coordinate
    }
    
    init(item: MapAnnotationItem) {
        self.id = item.id
        self.item = item
    }
}

// MARK: - Subviews (Keep Existing)
// (FilterChip, POIDetailSheet, Markers - assuming they are reused/available or need to be redefined if overwrite is full)

// Since overwrite is full, I need to include FilterChip etc.
// I will include them concisely.

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(RacingFont.body(12))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? RacingColors.red : RacingColors.cardBackground)
                .foregroundColor(.white)
                .cornerRadius(16)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(RacingColors.silver.opacity(0.3), lineWidth: isSelected ? 0 : 1))
        }
        .accessibilityLabel(title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

struct PoiDetailSheet: View {
    let poi: Poi
    @ObservedObject var viewModel: MapViewModel
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 20) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(poi.name)
                            .font(RacingFont.header(24))
                            .foregroundColor(.white)
                        Text(poi.type.rawValue.capitalized)
                            .font(RacingFont.body())
                            .foregroundColor(RacingColors.silver)
                    }
                    Spacer()
                    Image(systemName: POIIconHelper.iconName(for: poi.type))
                        .font(.largeTitle)
                        .foregroundColor(RacingColors.red)
                }
                
                if let description = poi.description {
                    Text(description)
                        .font(RacingFont.body())
                        .foregroundColor(.white.opacity(0.8))
                }
                
                Spacer()
                
                // Navigate to POI Button
                Button(action: {
                    dismiss()
                    viewModel.calculateRouteToPOI(poi)
                }) {
                    HStack {
                        Image(systemName: "arrow.triangle.turn.up.right.circle.fill")
                        Text("Cómo llegar")
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(RacingColors.red)
                    .cornerRadius(12)
                }
            }
            .padding(24)
        }
        .presentationDetents([.medium])
    }
}

// Helper for Icons
struct POIIconHelper {
    static func iconName(for category: PoiType) -> String {
        switch category {
            case .wc: return "toilet.fill"
            case .food: return "fork.knife"
            case .medical: return "cross.case.fill"
            case .parking: return "p.circle.fill"
            case .grandstand: return "person.3.fill"
            case .merch: return "bag.fill"
            case .access: return "arrow.right.circle.fill"
            case .exit: return "arrow.left.circle.fill"
            case .gate: return "rectangle.compress.vertical"
            case .fanzone: return "sportscourt"
            case .service: return "wrench.adjustable"
            case .other: return "mappin"
        }
    }
}

struct POIMarker: View {
    let poi: Poi
    
    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: POIIconHelper.iconName(for: poi.type))
                .resizable()
                .scaledToFit()
                .frame(width: 22, height: 22)
                .foregroundColor(.white)
                .padding(8)
                .background(RacingColors.red)
                .clipShape(Circle())
                .shadow(radius: 2)
            
            Text(poi.name)
                .font(.caption2)
                .padding(2)
                .background(Color.black.opacity(0.6))
                .foregroundColor(.white)
                .cornerRadius(4)
                .offset(y: 2)
        }
    }
}

struct FriendMarker: View {
    let friend: GroupMember
    
    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: "person.crop.circle.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 30, height: 30)
                .foregroundColor(.blue)
                .background(Color.white)
                .clipShape(Circle())
                .shadow(radius: 2)
            
            Text(friend.displayName)
                .font(.caption2)
                .padding(2)
                .background(Color.black.opacity(0.6))
                .foregroundColor(.white)
                .cornerRadius(4)
        }
    }
}

struct BeaconMarker: View {
    let beacon: BeaconConfig
    
    var body: some View {
        Circle()
            .fill(Color.purple.opacity(0.3))
            .frame(width: 30, height: 30)
            .overlay(
                Circle()
                    .stroke(Color.purple, lineWidth: 2)
            )
            .overlay(
                Image(systemName: "antenna.radiowaves.left.and.right")
                    .font(.caption)
                    .foregroundColor(.purple)
            )
    }
}
