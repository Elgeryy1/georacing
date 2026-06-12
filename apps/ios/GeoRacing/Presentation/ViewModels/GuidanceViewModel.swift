import Foundation
import CoreLocation
import Combine

@MainActor
class GuidanceViewModel: ObservableObject {
    let itinerary: Itinerary
    
    @Published var currentStepIndex: Int = 0
    @Published var currentLeg: Leg?
    @Published var distanceToNextStop: Double = 0
    @Published var estimatedTimeRemaining: TimeInterval = 0
    @Published var feedbackMessage: String?
    
    private var cancellables = Set<AnyCancellable>()
    private let speechService = SpeechService.shared
    private let locationManager = LocationManager.shared
    
    init(itinerary: Itinerary) {
        self.itinerary = itinerary
        if let first = itinerary.legs.first {
            self.currentLeg = first
            self.distanceToNextStop = first.distance ?? 0
            speakCurrentLeg(first)
        }
        
        startMonitoring()
    }
    
    private func startMonitoring() {
        locationManager.$location
            .compactMap { $0 }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] location in
                self?.checkProgress(userLoc: location)
            }
            .store(in: &cancellables)
    }
    
    private func checkProgress(userLoc: CLLocationCoordinate2D) {
        guard let leg = currentLeg else { return }
        
        let destLoc = CLLocation(latitude: leg.to.lat, longitude: leg.to.lon)
        let userCLLoc = CLLocation(latitude: userLoc.latitude, longitude: userLoc.longitude)
        
        let dist = userCLLoc.distance(from: destLoc)
        self.distanceToNextStop = dist
        
        // Thresholds: Walk (20m), Bus/Train (300m - stations are big/GPS in tunnel)
        let threshold = (leg.mode == "WALK") ? 20.0 : 300.0
        
        if dist < threshold {
            advanceStep()
        }
    }
    
    func advanceStep() {
        if currentStepIndex < itinerary.legs.count - 1 {
            currentStepIndex += 1
            currentLeg = itinerary.legs[currentStepIndex]
            if let leg = currentLeg {
                speakCurrentLeg(leg)
            }
        } else {
            // Finished
            speechService.speak("Has llegado a tu destino. Disfruta de la carrera.")
            // Could trigger a finished state
            feedbackMessage = "¡Ruta Completada!"
        }
    }
    
    func prevStep() {
        if currentStepIndex > 0 {
            currentStepIndex -= 1
            currentLeg = itinerary.legs[currentStepIndex]
        }
    }
    
    private func speakCurrentLeg(_ leg: Leg) {
        // Construct natural language instruction
        var text = ""
        switch leg.mode {
        case "WALK":
            text = String(format: LocalizationUtils.string("Walk to %@. You are %d meters away."), leg.to.name, Int(leg.distance ?? 0))
        case "BUS":
            text = String(format: LocalizationUtils.string("Board bus %@ towards %@."), leg.routeShortName ?? "", leg.to.name)
        case "RAIL", "SUBWAY":
            text = String(format: LocalizationUtils.string("Take train %@ direction %@."), leg.routeShortName ?? "", leg.routeLongName ?? leg.to.name)
        default:
            text = String(format: LocalizationUtils.string("Head to %@"), leg.to.name)
        }
        
        speechService.speak(text)
    }
    
    func skipInstruction() {
        advanceStep()
    }
}
