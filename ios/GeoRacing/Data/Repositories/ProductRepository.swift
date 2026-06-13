import Foundation
import Combine

class ProductRepository {
    static let shared = ProductRepository()
    private init() {}
    
    func fetchProducts() async throws -> [Product] {
        // Read directly from 'products' table
        Logger.debug("[ProductRepo] Fetching from table 'products'...")
        let records = try await DatabaseClient.shared.read(table: "products")
        Logger.debug("[ProductRepo] Raw records found: \(records.count)")
        
        let products = records.compactMap { dict -> Product? in
            // Convert dictionary to JSON data for decoding
            guard let jsonData = try? JSONSerialization.data(withJSONObject: dict) else { 
                Logger.warning("[ProductRepo] Failed to serialize dict: \(dict)")
                return nil 
            }
            
            do {
                let product = try JSONDecoder().decode(Product.self, from: jsonData)
                
                // Debugging "Invisible" Products
                if !product.isActive {
                    Logger.debug("[ProductRepo] Hidden (Inactive): \(product.name)")
                }
                
                return product.isActive ? product : nil 
            } catch {
                Logger.error("[ProductRepo] Decoding Error for item: \(dict). Reason: \(error)")
                return nil
            }
        }
        
        Logger.info("[ProductRepo] Final visible products: \(products.count)")
        return products
    }
    
    // Create 'orders' entry
    func submitOrder(items: [OrderItem], total: Double, user: AppUser) async throws -> String {
        let orderId = UUID().uuidString
        let itemsData = try JSONEncoder().encode(items)
        let itemsJson = String(data: itemsData, encoding: .utf8) ?? "[]"
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .medium
        let createdAt = dateFormatter.string(from: Date())
        
        let orderData: [String: Any] = [
            "order_id": orderId,
            "user_uid": user.uid,
            "status": "PENDING",
            "items_json": itemsJson,
            "total_amount": total,
            "platform": "IOS",
            "created_at": createdAt
        ]
        
        try await DatabaseClient.shared.upsert(table: "orders", data: orderData)
        return orderId
    }
}

