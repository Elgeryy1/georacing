import Foundation

struct NewsArticle: Identifiable, Codable, Sendable {
    let id: String
    let title: String
    let subtitle: String?
    let content: String?
    let imageUrl: String?
    let timestamp: Double
    
    var date: Date {
        Date(timeIntervalSince1970: timestamp / 1000)
    }
}

class NewsRepository {
    static let shared = NewsRepository()
    private init() {}
    
    func fetchNews() async throws -> [NewsArticle] {
        // Try 'news' first, if fails maybe 'articles'?
        // Since we don't know the exact table, we default to 'news'
        let records = try await DatabaseClient.shared.read(table: "news")
        
        return records.compactMap { dict in
            // Map dictionary to NewsArticle
            guard let id = dict["id"] as? String ?? dict["_id"] as? String,
                  let title = dict["title"] as? String else {
                return nil
            }
            
            return NewsArticle(
                id: id,
                title: title,
                subtitle: dict["subtitle"] as? String,
                content: dict["content"] as? String,
                imageUrl: dict["image_url"] as? String,
                timestamp: dict["timestamp"] as? Double ?? Date().timeIntervalSince1970 * 1000
            )
        }
    }
}
