import Foundation

/// A generic client for the "FirestoreLike" API used in the project.
/// Replicates Android's FirestoreLikeClient / FirestoreLikeApi.
@MainActor
class DatabaseClient {
    static let shared = DatabaseClient()
    
    private let baseURL = AppConstants.apiBaseUrl
    private let session: URLSession
    
    private init() {
        // Reuse the same unsafe configuration as APIService given it's the same dev server
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config, delegate: APIService.shared, delegateQueue: nil)
    }
    
    // MARK: - API Response Types
    
    struct UpsertRequest: Encodable {
        let table: String
        let data: [String: AnyEncodable]
    }
    
    struct GetRequest: Encodable {
        let table: String
        let whereClause: [String: AnyEncodable]
        
        enum CodingKeys: String, CodingKey {
            case table
            case whereClause = "where"
        }
    }
    
    // MARK: - Methods
    
    /// Replicates `GET /_read?table={table}`
    func read(table: String) async throws -> [[String: Any]] {
        guard let url = URL(string: "\(baseURL)/_read?table=\(table)") else {
            throw URLError(.badURL)
        }
        
        let (data, response) = try await session.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        
        // The API returns a list of maps (JSON Objects)
        let jsonResult = try JSONSerialization.jsonObject(with: data, options: [])
        guard let list = jsonResult as? [[String: Any]] else {
            throw URLError(.cannotParseResponse)
        }
        return list
    }
    
    /// Replicates `POST /_get` with body { table, where }
    func get(table: String, where criteria: [String: Any]) async throws -> [[String: Any]] {
        guard let url = URL(string: "\(baseURL)/_get") else {
            throw URLError(.badURL)
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let wrappedCriteria = criteria.mapValues { AnyEncodable($0) }
        let bodyObj = GetRequest(table: table, whereClause: wrappedCriteria)
        request.httpBody = try JSONEncoder().encode(bodyObj)
        
        let (data, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        
        let jsonResult = try JSONSerialization.jsonObject(with: data, options: [])
        guard let list = jsonResult as? [[String: Any]] else {
            throw URLError(.cannotParseResponse)
        }
        return list
    }
    
    /// Replicates `POST /_upsert` with body { table, data }
    func upsert(table: String, data: [String: Any]) async throws {
        guard let url = URL(string: "\(baseURL)/_upsert") else {
            throw URLError(.badURL)
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let wrappedData = data.mapValues { AnyEncodable($0) }
        let bodyObj = UpsertRequest(table: table, data: wrappedData)
        let bodyData = try JSONEncoder().encode(bodyObj)
        request.httpBody = bodyData
        
        // Check local network monitor state first if possible, or attempt directly
        let isOnline = await SyncQueueManager.shared.isOnline
        
        if !isOnline {
            Logger.warning("[DatabaseClient] Device is offline. Queuing upsert to table: \(table)")
            await SyncQueueManager.shared.enqueue(table: table, rawBody: bodyData)
            return // Return success locally (Deferred execution)
        }
        
        do {
            let (_, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                Logger.warning("[DatabaseClient] Server error. Queuing upsert to table: \(table)")
                await SyncQueueManager.shared.enqueue(table: table, rawBody: bodyData)
                return
            }
            
            Logger.info("[DatabaseClient] Upsert successful to table: \(table)")
        } catch {
            Logger.warning("[DatabaseClient] Network/Timeout error. Queuing upsert to table: \(table)")
            await SyncQueueManager.shared.enqueue(table: table, rawBody: bodyData)
        }
    }
}

extension DatabaseClient {
    // Compatibilidad con codigo previo (no cambia la API real)
    func upsert(table: String, key: String, value: Any, data: [String: Any]) async throws {
        var merged = data
        merged[key] = value
        try await upsert(table: table, data: merged)
    }
}

// MARK: - Helper for Any encoding
struct AnyEncodable: Encodable {
    let value: Any
    
    init(_ value: Any) {
        self.value = value
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        
        switch value {
        case let int as Int: try container.encode(int)
        case let double as Double: try container.encode(double)
        case let string as String: try container.encode(string)
        case let bool as Bool: try container.encode(bool)
        // Add other types as needed
        default:
            let context = EncodingError.Context(codingPath: [], debugDescription: "Invalid JSON value")
            throw EncodingError.invalidValue(value, context)
        }
    }
}
