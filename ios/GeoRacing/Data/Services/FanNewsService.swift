import Foundation
import Combine
import UIKit

// MARK: - Fan News Service

/// Aggregates motorsport news from RSS feeds.
/// Supports multiple sources, deduplication, caching, and offline mode.
@MainActor
final class FanNewsService: ObservableObject {
    
    static let shared = FanNewsService()
    
    // MARK: - Published
    
    @Published private(set) var articles: [FeedArticle] = []
    @Published private(set) var isLoading = false
    @Published private(set) var lastRefreshed: Date?
    @Published private(set) var errorMessage: String?
    
    // MARK: - Config
    
    /// RSS feed sources
    private let feeds: [(url: String, source: String, championship: Championship)] = [
        ("https://www.motorsport.com/rss/f1/news/", "Motorsport.com", .f1),
        ("https://www.motorsport.com/rss/motogp/news/", "Motorsport.com", .motogp),
        ("https://www.autosport.com/rss/feed/f1", "Autosport", .f1),
        ("https://www.autosport.com/rss/feed/motogp", "Autosport", .motogp),
    ]
    
    /// Cache keys
    private let cacheKey = "fan_news_cache"
    private let cacheTimestampKey = "fan_news_timestamp"
    
    /// Refresh minimum interval (5 minutes)
    private let refreshInterval: TimeInterval = 5 * 60
    
    /// Max articles to keep
    private let maxArticles = 100
    
    // MARK: - Init
    
    private init() {
        loadFromCache()
    }
    
    // MARK: - Public API
    
    /// Fetch latest news from all feeds
    func refreshNews() async {
        // Throttle: don't refresh too frequently
        if let last = lastRefreshed, Date().timeIntervalSince(last) < refreshInterval {
            Logger.debug("[FanNews] Throttled, last refresh \(Int(Date().timeIntervalSince(last)))s ago")
            return
        }
        
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        var allArticles: [FeedArticle] = []
        
        await withTaskGroup(of: [FeedArticle].self) { group in
            for feed in feeds {
                group.addTask { [weak self] in
                    await self?.fetchFeed(urlString: feed.url, source: feed.source, championship: feed.championship) ?? []
                }
            }
            
            for await feedArticles in group {
                allArticles.append(contentsOf: feedArticles)
            }
        }
        
        if allArticles.isEmpty && articles.isEmpty {
            errorMessage = "No news available"
            Logger.warning("[FanNews] No articles fetched")
            return
        }
        
        // Deduplicate
        var deduped = FeedArticle.deduplicateURL(allArticles)
        deduped = FeedArticle.deduplicateTitle(deduped)
        
        // Sort by date (newest first)
        deduped.sort { $0.publishedAt > $1.publishedAt }
        
        // Limit
        if deduped.count > maxArticles {
            deduped = Array(deduped.prefix(maxArticles))
        }
        
        articles = deduped
        lastRefreshed = Date()
        saveToCache()
        
        Logger.info("[FanNews] Refreshed: \(deduped.count) articles from \(feeds.count) feeds")
    }
    
    /// Force refresh (ignore throttle)
    func forceRefresh() async {
        lastRefreshed = nil
        await refreshNews()
    }
    
    /// Get articles for a specific championship
    func articles(for championship: Championship) -> [FeedArticle] {
        articles.filter { $0.championship == championship }
    }
    
    /// Human-readable "last updated" label
    var lastRefreshedText: String {
        guard let date = lastRefreshed else {
            return LocalizationUtils.string("Never")
        }
        let interval = Date().timeIntervalSince(date)
        if interval < 60 {
            return LocalizationUtils.string("Just now")
        } else if interval < 3600 {
            let mins = Int(interval / 60)
            return "\(mins) min"
        } else {
            let hours = Int(interval / 3600)
            return "\(hours)h"
        }
    }
    
    /// Track that a user read an article (for rewards)
    func markAsRead(_ articleId: String) {
        var readIds = UserDefaults.standard.stringArray(forKey: "fan_news_read_ids") ?? []
        if !readIds.contains(articleId) {
            readIds.append(articleId)
            UserDefaults.standard.set(readIds, forKey: "fan_news_read_ids")
            // Notify reward service
            RewardService.shared.recordEvent(.newsRead)
        }
    }
    
    var totalRead: Int {
        (UserDefaults.standard.stringArray(forKey: "fan_news_read_ids") ?? []).count
    }
    
    // MARK: - RSS Fetch
    
    private func fetchFeed(urlString: String, source: String, championship: Championship) async -> [FeedArticle] {
        guard let url = URL(string: urlString), url.scheme == "https" else { return [] }
        
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 15
            request.setValue("GeoRacing/1.0", forHTTPHeaderField: "User-Agent")
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let http = response as? HTTPURLResponse,
                  (200...299).contains(http.statusCode) else { return [] }
            
            let parser = RSSParser(source: source, championship: championship)
            return parser.parse(data: data)
        } catch {
            Logger.warning("[FanNews] Feed fetch failed (\(source)): \(error.localizedDescription)")
            return []
        }
    }
    
    // MARK: - Cache
    
    private func saveToCache() {
        do {
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .iso8601
            let data = try encoder.encode(articles)
            UserDefaults.standard.set(data, forKey: cacheKey)
            UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: cacheTimestampKey)
        } catch {
            Logger.error("[FanNews] Cache save failed: \(error)")
        }
    }
    
    private func loadFromCache() {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else { return }
        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            articles = try decoder.decode([FeedArticle].self, from: data)
            let ts = UserDefaults.standard.double(forKey: cacheTimestampKey)
            if ts > 0 { lastRefreshed = Date(timeIntervalSince1970: ts) }
            Logger.debug("[FanNews] Loaded \(articles.count) articles from cache")
        } catch {
            Logger.error("[FanNews] Cache decode failed: \(error)")
        }
    }
}

// MARK: - RSS XML Parser

/// Lightweight RSS/Atom parser using Foundation's XMLParser.
final class RSSParser: NSObject, XMLParserDelegate {
    
    private let source: String
    private let championship: Championship
    
    private var articles: [FeedArticle] = []
    
    // Parsing state
    private var currentElement = ""
    private var currentTitle = ""
    private var currentLink = ""
    private var currentDescription = ""
    private var currentPubDate = ""
    private var currentImageUrl: String?
    private var isInsideItem = false
    
    // Date formatters for RSS
    private lazy var rssDateFormatter: DateFormatter = {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        return fmt
    }()
    
    private lazy var iso8601Formatter: ISO8601DateFormatter = {
        let fmt = ISO8601DateFormatter()
        fmt.formatOptions = [.withInternetDateTime]
        return fmt
    }()
    
    init(source: String, championship: Championship) {
        self.source = source
        self.championship = championship
    }
    
    func parse(data: Data) -> [FeedArticle] {
        articles = []
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.parse()
        return articles
    }
    
    // MARK: - XMLParserDelegate
    
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?,
                qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
        currentElement = elementName
        
        if elementName == "item" || elementName == "entry" {
            isInsideItem = true
            currentTitle = ""
            currentLink = ""
            currentDescription = ""
            currentPubDate = ""
            currentImageUrl = nil
        }
        
        // Atom link
        if elementName == "link" && isInsideItem, let href = attributeDict["href"] {
            currentLink = href
        }
        
        // Media thumbnail
        if (elementName == "media:thumbnail" || elementName == "media:content"),
           let url = attributeDict["url"] {
            currentImageUrl = url
        }
        
        // Enclosure (some feeds use this for images)
        if elementName == "enclosure",
           let type = attributeDict["type"], type.hasPrefix("image"),
           let url = attributeDict["url"] {
            currentImageUrl = url
        }
    }
    
    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard isInsideItem else { return }
        switch currentElement {
        case "title": currentTitle += string
        case "link": currentLink += string
        case "description", "summary", "content": currentDescription += string
        case "pubDate", "published", "updated": currentPubDate += string
        default: break
        }
    }
    
    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?,
                qualifiedName qName: String?) {
        if elementName == "item" || elementName == "entry" {
            isInsideItem = false
            
            let title = currentTitle.trimmingCharacters(in: .whitespacesAndNewlines)
            let link = currentLink.trimmingCharacters(in: .whitespacesAndNewlines)
            
            guard !title.isEmpty, !link.isEmpty else { return }
            
            // Parse date
            let dateStr = currentPubDate.trimmingCharacters(in: .whitespacesAndNewlines)
            let date = rssDateFormatter.date(from: dateStr)
                ?? iso8601Formatter.date(from: dateStr)
                ?? Date()
            
            // Clean description (strip HTML)
            let summary = currentDescription
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .strippingHTML()
                .prefix(300)
            
            // Extract image from description if no media tag
            let imageUrl = currentImageUrl ?? extractImageUrl(from: currentDescription)
            
            // Generate stable ID from URL
            let id = link.lowercased().data(using: .utf8).map {
                $0.map { String(format: "%02x", $0) }.joined()
            } ?? UUID().uuidString
            
            let article = FeedArticle(
                id: String(id.prefix(32)),
                title: title,
                summary: String(summary),
                source: source,
                publishedAt: date,
                url: link,
                imageUrl: imageUrl,
                championship: championship,
                tags: []
            )
            
            articles.append(article)
        }
    }
    
    // MARK: - Helpers
    
    /// Extract first image URL from HTML content
    private func extractImageUrl(from html: String) -> String? {
        guard let range = html.range(of: "src=\"", options: .caseInsensitive) else { return nil }
        let after = html[range.upperBound...]
        guard let endRange = after.range(of: "\"") else { return nil }
        let url = String(after[..<endRange.lowerBound])
        return url.hasPrefix("http") ? url : nil
    }
}

// MARK: - HTML Stripping

extension String {
    /// Remove HTML tags from string
    func strippingHTML() -> String {
        guard let data = self.data(using: .utf8) else { return self }
        
        if let attributed = try? NSAttributedString(
            data: data,
            options: [.documentType: NSAttributedString.DocumentType.html,
                      .characterEncoding: String.Encoding.utf8.rawValue],
            documentAttributes: nil
        ) {
            return attributed.string
        }
        
        // Fallback: regex strip
        return self.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
    }
}
