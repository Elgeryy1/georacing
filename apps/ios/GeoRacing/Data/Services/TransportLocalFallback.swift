import Foundation
import CoreLocation

// MARK: - Routing Data Models

struct TransportStop {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
    let lines: [String]
    let type: StopType
    let timeOffsetFromSants: Int // Minutes from Sants departure for R2N
}

enum StopType {
    case trainStation
    case metroStation // Generic representation for heuristic
    case busStop
}

struct Departure {
    let time: Int // Timestamp
    let line: String
    let destination: String
}

// MARK: - Routing Engine

class TransportLocalFallback {
    static let shared = TransportLocalFallback()
    
    // MARK: - Network Data (R2 Nord Configuration)
    
    private let montmeloStation = TransportStop(
        id: "montmelo", name: "Estación de Montmeló",
        coordinate: CLLocationCoordinate2D(latitude: 41.551, longitude: 2.247),
        lines: ["R2", "R2N"], type: .trainStation, timeOffsetFromSants: 30
    )
    
    // Major R2N Stops (Order: Sants -> North)
    private let networkPoints: [TransportStop] = [
        TransportStop(id: "sants", name: "Barcelona Sants", coordinate: CLLocationCoordinate2D(latitude: 41.379, longitude: 2.140), lines: ["R2N", "L3", "L5"], type: .trainStation, timeOffsetFromSants: 0),
        TransportStop(id: "pdg", name: "Passeig de Gràcia", coordinate: CLLocationCoordinate2D(latitude: 41.392, longitude: 2.165), lines: ["R2N", "L2", "L3", "L4"], type: .trainStation, timeOffsetFromSants: 5),
        TransportStop(id: "clot", name: "El Clot-Aragó", coordinate: CLLocationCoordinate2D(latitude: 41.407, longitude: 2.187), lines: ["R2N", "L1", "L2"], type: .trainStation, timeOffsetFromSants: 9),
        TransportStop(id: "standreu", name: "Sant Andreu Comtal", coordinate: CLLocationCoordinate2D(latitude: 41.436, longitude: 2.190), lines: ["R2N", "L1"], type: .trainStation, timeOffsetFromSants: 14), // Approx
        TransportStop(id: "granollers", name: "Granollers Centre", coordinate: CLLocationCoordinate2D(latitude: 41.597, longitude: 2.290), lines: ["R2N", "R8"], type: .trainStation, timeOffsetFromSants: 38) // North of Circuit
    ]
    
    // MARK: - Public Interface
    
    func generateFallbackItinerary(from userLocation: CLLocationCoordinate2D, to destination: CLLocationCoordinate2D) -> Itinerary {
        let now = Int(Date().timeIntervalSince1970 * 1000)
        let origin = Place(name: "Tu Ubicación", lat: userLocation.latitude, lon: userLocation.longitude, departureTime: nil, arrivalTime: nil)
        let finalDest = Place(name: "Circuit (Acceso Recomendado)", lat: destination.latitude, lon: destination.longitude, departureTime: nil, arrivalTime: nil)
        
        // 1. Find Optimal Entry Station (Graph Search: Walk vs Metro+Walk)
        let bestPath = calculateBestEntryStation(userLocation: userLocation, startTime: now)
        
        guard let entryStation = bestPath.station else {
            // Fallback: If no station reachable reasonably, assumes close to circuit or error.
            return generateWalkingItinerary(from: origin, to: finalDest, startTime: now)
        }
        
        // 2. Find Next Train for Entry Station
        let arrivalAtStationTime = bestPath.arrivalTime
        let nextTrain = findNextTrain(for: entryStation, after: arrivalAtStationTime)
        
        // 3. Build Itinerary
        var legs: [Leg] = []
        var currentTime = now
        
        // Leg A: Access to Station (Walk or Metro)
        legs.append(contentsOf: bestPath.legs)
        currentTime = nextTrain.departureTime
        
        // Leg B: Train Ride (Entry -> Montmelo)
        let trainTime = abs(montmeloStation.timeOffsetFromSants - entryStation.timeOffsetFromSants) * 60
        // Adjust for Granollers (North -> South)? Assuming South -> North flow mostly for BCN users.
        // Logic handles generic time offsets.
        
        legs.append(Leg(
            mode: "RAIL",
            route: "R2N",
            routeColor: "009900",
            routeShortName: "R2N",
            routeLongName: nextTrain.destination,
            from: Place(name: entryStation.name, lat: entryStation.coordinate.latitude, lon: entryStation.coordinate.longitude, departureTime: nextTrain.departureTime, arrivalTime: nil),
            to: Place(name: montmeloStation.name, lat: montmeloStation.coordinate.latitude, lon: montmeloStation.coordinate.longitude, departureTime: nil, arrivalTime: nextTrain.departureTime + (trainTime * 1000)),
            realTime: true,
            distance: Double(trainTime) * 15.0, // Rough physics
            legGeometry: nil
        ))
        
        currentTime = nextTrain.departureTime + (trainTime * 1000)
        
        // Leg C: Shuttle Bus or Walk from Montmelo to Circuit
        // Shuttle usually runs on F1 weekends. Let's add Shuttle logic if time is right (Daytime).
        let shuttleLegs = generateLastMile(from: montmeloStation, to: finalDest, startTime: currentTime)
        legs.append(contentsOf: shuttleLegs.legs)
        
        let totalDuration = (shuttleLegs.endTime - now) / 1000
        
        // Calculate Totals properly
        let walkTime = legs.filter { $0.mode == "WALK" }.reduce(0) { $0 + ($1.duration) }
        let transitTime = legs.filter { $0.mode != "WALK" }.reduce(0) { $0 + ($1.duration) }
        
        return Itinerary(
            duration: totalDuration,
            startTime: now,
            endTime: shuttleLegs.endTime,
            walkTime: walkTime,
            transitTime: transitTime,
            legs: legs
        )
    }
    
    // MARK: - Logic Core
    
    struct StationPath {
        let station: TransportStop?
        let arrivalTime: Int
        let legs: [Leg]
        let score: Double // Time driven score
    }
    
    // Calculates best way to get to *any* R2N station
    private func calculateBestEntryStation(userLocation: CLLocationCoordinate2D, startTime: Int) -> StationPath {
        let userLoc = CLLocation(latitude: userLocation.latitude, longitude: userLocation.longitude)
        
        var bestPath: StationPath? = nil
        
        for station in networkPoints {
            let stationLoc = CLLocation(latitude: station.coordinate.latitude, longitude: station.coordinate.longitude)
            let directDistance = userLoc.distance(from: stationLoc)
            
            // Path 1: Direct Walk
            let walkTimeSec = Int(directDistance / 1.2)
            let walkArrival = startTime + (walkTimeSec * 1000)
            
            // Heuristic for Metro: If Walk > 20 mins, assume Metro exists in BCN center
            // Simple generic Metro model: 5 min walk + 5 min wait + (Distance / 30kmh)
            var metroTimeSec = 999999
            if directDistance > 1200 { // Only consider metro if far
                let speedMps = 30.0 * 1000 / 3600 // ~8.3 m/s
                let rideTime = Int(directDistance / speedMps)
                metroTimeSec = 300 + 300 + rideTime // Walk 5 + Wait 5 + Ride
            }
            
            // Check Train Schedule for this station (Wait Time penalty)
            let nextTrain = findNextTrain(for: station, after: min(walkArrival, startTime + (metroTimeSec * 1000)))
            let waitTime = (nextTrain.departureTime - min(walkArrival, startTime + (metroTimeSec * 1000))) / 1000
            
            // Total Time to DEPARTURE (Cost Function)
            // We want to minimize time until we are ON the train
            let costWalk = walkTimeSec + waitTime
            let costMetro = metroTimeSec + waitTime
            
            // Decide Walk vs Metro for THIS station
            let useMetro = costMetro < costWalk
            let finalArrivalAtStation = useMetro ? (startTime + metroTimeSec * 1000) : walkArrival
            let totalCost = useMetro ? costMetro : costWalk
            
            // Compare with other stations
            if bestPath == nil || totalCost < Int(bestPath?.score ?? .infinity) {
                var legs: [Leg] = []
                let origin = Place(name: "Tu Ubicación", lat: userLocation.latitude, lon: userLocation.longitude, departureTime: nil, arrivalTime: nil)
                
                if useMetro {
                    // Walk to Metro
                    legs.append(Leg(mode: "WALK", route: nil, routeColor: nil, routeShortName: nil, routeLongName: nil, from: origin, to: Place(name: "Metro/Bus", lat: 0, lon: 0, departureTime: nil, arrivalTime: nil), realTime: false, distance: 400, legGeometry: nil))
                    // Metro Ride
                    legs.append(Leg(
                        mode: "SUBWAY", route: "L-Metro", routeColor: "FF0000", routeShortName: "Metro", routeLongName: "Dirección \(station.name)",
                        from: Place(name: "Red TMB", lat: 0, lon: 0, departureTime: nil, arrivalTime: nil),
                        to: Place(name: station.name, lat: station.coordinate.latitude, lon: station.coordinate.longitude, departureTime: nil, arrivalTime: nil),
                        realTime: false, distance: directDistance, legGeometry: nil))
                } else {
                    // Walk Direct
                    legs.append(Leg(
                        mode: "WALK", route: nil, routeColor: nil, routeShortName: nil, routeLongName: nil,
                        from: origin,
                        to: Place(name: station.name, lat: station.coordinate.latitude, lon: station.coordinate.longitude, departureTime: nil, arrivalTime: nil),
                        realTime: false, distance: directDistance, legGeometry: nil))
                }
                
                bestPath = StationPath(station: station, arrivalTime: finalArrivalAtStation, legs: legs, score: Double(totalCost))
            }
        }
        
        return bestPath ?? StationPath(station: nil, arrivalTime: 0, legs: [], score: 9999999)
    }
    
    // Finds next R2N departure based on fixed pattern :08 / :38
    private func findNextTrain(for station: TransportStop, after timestamp: Int) -> (departureTime: Int, destination: String) {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: date)
        let minute = calendar.component(.minute, from: date)
        
        // Schedule Pattern: Sants departures at :08 and :38
        // Station departure = Sants departure + offset
        let offset = station.timeOffsetFromSants
        
        // Candidates for current hour
        let dep1Candidate = (hour * 60) + 8 + offset
        let dep2Candidate = (hour * 60) + 38 + offset
        
        // Candidates for next hour
        let dep3Candidate = ((hour + 1) * 60) + 8 + offset
        let dep4Candidate = ((hour + 1) * 60) + 38 + offset
        
        let currentMinutesOfDay = (hour * 60) + minute
        
        let candidates = [dep1Candidate, dep2Candidate, dep3Candidate, dep4Candidate]
        
        // Find first candidate > current time
        let nextDepMinutes = candidates.first { $0 > currentMinutesOfDay + 1 } ?? dep3Candidate // +1 min buffer
        
        // Convert back to timestamp
        let nextHour = nextDepMinutes / 60
        let nextMinute = nextDepMinutes % 60
        
        // Construct New Date
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = nextHour
        components.minute = nextMinute
        components.second = 0
        
        let departureDate = calendar.date(from: components) ?? date.addingTimeInterval(1800)
        
        return (Int(departureDate.timeIntervalSince1970 * 1000), "Maçanet-Massanes / St. Celoni")
    }
    
    // Shuttle Bus (Montmeló -> Circuit) - Only on Race Days logic
    private func generateLastMile(from: TransportStop, to: Place, startTime: Int) -> (legs: [Leg], endTime: Int) {
        // Shuttle runs frequently on race days. Approx 10 mins ride.
        // Or walk 25 mins.
        // We propose Shuttle for comfort.
        
        var legs: [Leg] = []
        var currentTime = startTime
        
        // Walk station to shuttle stop (2 mins)
        legs.append(Leg(
            mode: "WALK", route: nil, routeColor: nil, routeShortName: nil, routeLongName: nil,
            from: Place(name: from.name, lat: from.coordinate.latitude, lon: from.coordinate.longitude, departureTime: nil, arrivalTime: nil),
            to: Place(name: "Parada Shuttle F1", lat: from.coordinate.latitude + 0.001, lon: from.coordinate.longitude + 0.001, departureTime: nil, arrivalTime: nil),
            realTime: false, distance: 100, legGeometry: nil
        ))
        currentTime += 120000
        
        // Shuttle Ride
        let rideTimeSec = 600 // 10 mins
        legs.append(Leg(
            mode: "BUS", route: "Shuttle F1", routeColor: "FF0000", routeShortName: "Shuttle F1", routeLongName: "Directo al Circuito",
            from: Place(name: "Parada Shuttle F1", lat: 0, lon: 0, departureTime: currentTime, arrivalTime: nil),
            to: Place(name: "Acceso Circuito", lat: to.lat, lon: to.lon, departureTime: nil, arrivalTime: currentTime + (rideTimeSec * 1000)),
            realTime: true, distance: 2000, legGeometry: nil
        ))
        
        currentTime += (rideTimeSec * 1000)
        
        return (legs, currentTime)
    }
    
    // Fallback for weird cases
    private func generateWalkingItinerary(from: Place, to: Place, startTime: Int) -> Itinerary {
        let dist = CLLocation(latitude: from.lat, longitude: from.lon).distance(from: CLLocation(latitude: to.lat, longitude: to.lon))
        let time = Int(dist / 1.2)
        let leg = Leg(mode: "WALK", route: nil, routeColor: nil, routeShortName: nil, routeLongName: nil, from: from, to: to, realTime: false, distance: dist, legGeometry: nil)
        return Itinerary(duration: time, startTime: startTime, endTime: startTime + time*1000, walkTime: time, transitTime: 0, legs: [leg])
    }
}
