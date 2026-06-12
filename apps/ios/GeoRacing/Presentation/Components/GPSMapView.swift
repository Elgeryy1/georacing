import SwiftUI
import MapKit
import CoreLocation

/// UIViewRepresentable that wraps MKMapView for real GPS navigation.
/// Renders the route polyline, user location with heading, and destination pin.
struct GPSMapView: UIViewRepresentable {
    
    @Binding var region: MKCoordinateRegion
    let polyline: MKPolyline?
    let destinationCoordinate: CLLocationCoordinate2D
    let destinationName: String
    let isFollowingUser: Bool
    let onUserInteraction: () -> Void
    
    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.showsUserLocation = true
        mapView.showsCompass = true
        mapView.showsScale = true
        mapView.isPitchEnabled = true
        mapView.setRegion(region, animated: false)
        
        // Add destination annotation
        let destAnnotation = MKPointAnnotation()
        destAnnotation.coordinate = destinationCoordinate
        destAnnotation.title = destinationName
        mapView.addAnnotation(destAnnotation)
        
        return mapView
    }
    
    func updateUIView(_ mapView: MKMapView, context: Context) {
        // Update tracking mode
        if isFollowingUser {
            if mapView.userTrackingMode != .followWithHeading {
                mapView.setUserTrackingMode(.followWithHeading, animated: true)
            }
        } else {
            if !context.coordinator.isUserInteracting {
                mapView.setRegion(region, animated: true)
            }
        }
        
        // Update destination annotation position
        let existingDest = mapView.annotations.compactMap { $0 as? MKPointAnnotation }.first
        if let existing = existingDest {
            if existing.coordinate.latitude != destinationCoordinate.latitude ||
               existing.coordinate.longitude != destinationCoordinate.longitude {
                existing.coordinate = destinationCoordinate
                existing.title = destinationName
            }
        } else {
            let annotation = MKPointAnnotation()
            annotation.coordinate = destinationCoordinate
            annotation.title = destinationName
            mapView.addAnnotation(annotation)
        }
        
        // Update route overlay
        let existingPolylines = mapView.overlays.compactMap { $0 as? MKPolyline }
        
        if let newPolyline = polyline {
            // Only add if different reference
            if !existingPolylines.contains(where: { $0 === newPolyline }) {
                mapView.removeOverlays(existingPolylines)
                mapView.addOverlay(newPolyline, level: .aboveRoads)
            }
        } else {
            // Remove all polylines
            mapView.removeOverlays(existingPolylines)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    // MARK: - Coordinator
    
    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: GPSMapView
        var isUserInteracting = false
        
        init(_ parent: GPSMapView) {
            self.parent = parent
        }
        
        // Detect user dragging the map
        func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
            // Check if change was initiated by user gesture
            if let view = mapView.subviews.first,
               let gestureRecognizers = view.gestureRecognizers {
                for recognizer in gestureRecognizers {
                    if recognizer.state == .began || recognizer.state == .ended || recognizer.state == .changed {
                        isUserInteracting = true
                        parent.onUserInteraction()
                        return
                    }
                }
            }
        }
        
        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            isUserInteracting = false
        }
        
        // Route polyline renderer
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: polyline)
                renderer.strokeColor = UIColor.systemBlue
                renderer.lineWidth = 6
                renderer.lineCap = .round
                renderer.lineJoin = .round
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }
        
        // Destination pin
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            // Don't customize user location
            guard !(annotation is MKUserLocation) else { return nil }
            
            let identifier = "DestinationPin"
            var view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView
            if view == nil {
                view = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)
                view?.canShowCallout = true
            }
            view?.annotation = annotation
            view?.markerTintColor = .systemRed
            view?.glyphImage = UIImage(systemName: "flag.checkered")
            return view
        }
    }
}
