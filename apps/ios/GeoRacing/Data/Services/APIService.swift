import Foundation

class APIService: NSObject, URLSessionDelegate {
        
    static let shared = APIService()
    
    private let baseURL = AppConstants.apiBaseUrl
    
    // We use an implicitly unwrapped optional or just a regular optional 
    // to allow 'self' to be used in init.
    // However, it's cleaner to assign it lazily but ensuring thread safety is harder.
    // The previous implementation used lazy var which was MainActor isolated.
    // Here we will use a private var and a public accessor or just initialize it in a way that respects Swift's init rules.
    var session: URLSession!
    
    override init() {
        super.init()
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        // Now 'self' is fully initialized, we can pass it as delegate
        self.session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }
    
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if let trust = challenge.protectionSpace.serverTrust {
            completionHandler(.useCredential, URLCredential(trust: trust))
        } else {
            completionHandler(.performDefaultHandling, nil)
        }
    }
    
    // MARK: - Endpoints
    
    func fetchCircuitState() async throws -> (status: TrackStatus, message: String?) {
        guard let url = URL(string: "\(baseURL)/state") else { throw URLError(.badURL) }
        
        // Debugging: Print Request URL
        Logger.debug("[APIService] Fetching Circuit State from: \(url)")
        
        let (data, _) = try await session.data(from: url)
        
        // Debugging: Convert data to string to see RAW RESPONSE
        if let jsonString = String(data: data, encoding: .utf8) {
            Logger.debug("[APIService] Raw Response: \(jsonString)")
        }
        
        // Explicitly catching decoding errors to print them clearly
        do {
            let dto = try JSONDecoder().decode(CircuitStateDto.self, from: data)
            Logger.debug("[APIService] Decoded DTO: flag='\(dto.flag)', message='\(dto.message ?? "nil")'")
            return (mapStatus(dto.flag), dto.message)
        } catch {
            Logger.error("[APIService] JSON Decode Error: \(error)")
            throw error
        }
    }
    
    func fetchProducts() async throws -> [Product] {
        guard let url = URL(string: "\(baseURL)/products") else { throw URLError(.badURL) }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode([Product].self, from: data)
    }

    func fetchPois() async throws -> [PoiDto] {
        guard let url = URL(string: "\(baseURL)/pois") else { throw URLError(.badURL) }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode([PoiDto].self, from: data)
    }
    
    func fetchBeacons() async throws -> [BeaconDto] {
        guard let url = URL(string: "\(baseURL)/beacons") else { throw URLError(.badURL) }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode([BeaconDto].self, from: data)
    }
    
    func sendIncident(_ report: IncidentReportDto) async throws {
        guard let url = URL(string: "\(baseURL)/incidents") else { throw URLError(.badURL) }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(report)
        
        let (_, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 || httpResponse.statusCode == 201 else {
            throw URLError(.badServerResponse)
        }
    }
    
    func fetchGroupMembers(groupName: String) async throws -> [GroupMemberDto] {
        // GET /groups/{groupName}/members
        guard let url = URL(string: "\(baseURL)/groups/\(groupName)/members") else { throw URLError(.badURL) }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode([GroupMemberDto].self, from: data)
    }

    func upsertGroupLocation(_ req: GroupLocationRequest) async throws {
        // POST /groups/location
        guard let url = URL(string: "\(baseURL)/groups/location") else { throw URLError(.badURL) }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(req)
        
        let (_, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 || httpResponse.statusCode == 201 else {
            throw URLError(.badServerResponse)
        }
    }
    
    func fetchZoneDensities() async throws -> [ZoneDensityDto] {
        guard let url = URL(string: "\(baseURL)/zones/density") else { throw URLError(.badURL) }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode([ZoneDensityDto].self, from: data)
    }
    
    private func mapStatus(_ flag: String) -> TrackStatus {
        let normalized = flag.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
        
        // 0. EVACUATION (Highest Priority)
        if normalized.contains("EVACUATION") {
            return .evacuation
        }
        
        // 1. RED / CLOSED / STOP
        if normalized.contains("RED") || normalized.contains("STOP") || normalized.contains("BLOCK") || normalized.contains("CLOSE") {
            return .red
        }
        
        // 2. ORANGE / CAUTION / SAFETY CAR / YELLOW
        if normalized.contains("SC") || normalized.contains("SAFETY") || normalized.contains("VSC") || normalized.contains("VIRTUAL") || normalized.contains("CAUTION") || normalized.contains("WARN") || normalized.contains("YELLOW") {
            return .sc 
        }

        // 3. GREEN / CLEAN
        if normalized.contains("GREEN") || normalized.contains("CLEAN") || normalized.contains("CLEAR") || normalized.contains("PISTA") {
            return .green
        }
        
        // 4. UNKNOWN - Strict parity rule: don't hide unknown states
        Logger.warning("[APIService] Unknown Status String: '\(normalized)' -> Mapped to .unknown")
        return .unknown
    }
}
