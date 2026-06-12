import Foundation

struct Product: Identifiable, Codable, Sendable {
    let id: String
    let productId: String? // Nullable in DB
    let name: String
    let description: String
    let price: Double
    let stock: Int
    let category: String
    let imageUrl: String? // "image_url"
    let emoji: String?
    let isActive: Bool // "is_active" 1/0
    
    // API Mapping
    enum CodingKeys: String, CodingKey {
        case id
        case productId = "product_id"
        case name, description, price, stock, category, emoji
        case imageUrl = "image_url"
        case isActive = "is_active"
    }
    
    // Memberwise Init
    init(id: String, productId: String?, name: String, description: String, price: Double, stock: Int, category: String, imageUrl: String?, emoji: String?, isActive: Bool) {
        self.id = id
        self.productId = productId
        self.name = name
        self.description = description
        self.price = price
        self.stock = stock
        self.category = category
        self.imageUrl = imageUrl
        self.emoji = emoji
        self.isActive = isActive
    }

    // Custom decoding to handle Int/Bool 1/0 and optional fields
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        // Relaxed decoding for product_id
        productId = try container.decodeIfPresent(String.self, forKey: .productId)
        name = try container.decode(String.self, forKey: .name)
        
        // Relaxed decoding for optional/nullable fields
        description = try container.decodeIfPresent(String.self, forKey: .description) ?? ""
        price = try container.decodeIfPresent(Double.self, forKey: .price) ?? 0.0
        stock = try container.decodeIfPresent(Int.self, forKey: .stock) ?? 0
        category = try container.decodeIfPresent(String.self, forKey: .category) ?? "General"
        
        imageUrl = try? container.decode(String.self, forKey: .imageUrl)
        emoji = try? container.decode(String.self, forKey: .emoji)
        
        if let activeInt = try? container.decode(Int.self, forKey: .isActive) {
            isActive = activeInt == 1
        } else if let activeBool = try? container.decode(Bool.self, forKey: .isActive) {
            isActive = activeBool
        } else {
            // Default to true if missing, or false? Let's say true to be visible
            isActive = true
        }
    }
    
    func encode(to encoder: Encoder) throws {
       var container = encoder.container(keyedBy: CodingKeys.self)
       try container.encode(id, forKey: .id)
       try container.encode(productId, forKey: .productId)
       // ... simplified for saving if needed
    }
}

struct OrderItem: Codable, Sendable {
    let productId: String // UUID or product_id? usage implies UUID in Android typically
    let quantity: Int
    let unitPrice: Double
    
    enum CodingKeys: String, CodingKey {
        case productId = "product_id"
        case quantity
        case unitPrice = "unit_price"
    }
}

enum OrderStatus: String, Codable, Sendable {
    case pending = "PENDING"
    case delivered = "DELIVERED"
    case cancelled = "CANCELLED"
}

struct Order: Identifiable, Codable, Sendable {
    let id: String // UUID
    let orderId: String?
    let userUid: String
    let status: OrderStatus
    let items: [OrderItem] 
    let totalAmount: Double
    let platform: String
    let createdAt: String
    
    // For manual decoding of "items_json" string if it comes as string
    // But Codable won't handle JSON string automatically. 
    // We'll likely handle this in Repository mapping.
}
