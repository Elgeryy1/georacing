import Foundation
import Combine

// MARK: - Cart Item (Local model for cart state)

struct CartItem: Identifiable, Equatable {
    var id: String { productId }
    let productId: String
    let product: Product
    var quantity: Int
    
    var subtotal: Double {
        product.price * Double(quantity)
    }
    
    static func == (lhs: CartItem, rhs: CartItem) -> Bool {
        lhs.productId == rhs.productId && lhs.quantity == rhs.quantity
    }
}

// MARK: - Orders Repository Protocol

protocol OrdersRepositoryProtocol {
    func fetchProducts() async throws -> [Product]
    func fetchOrders(userId: String) async throws -> [Order]
    func createOrder(userId: String, items: [CartItem]) async throws -> Order
}

// MARK: - Orders Repository Implementation

final class OrdersRepository: OrdersRepositoryProtocol {
    
    private let apiService: APIService
    
    init(apiService: APIService = .shared) {
        self.apiService = apiService
    }
    
    // MARK: - Fetch Products
    
    func fetchProducts() async throws -> [Product] {
        guard let url = URL(string: "\(AppConstants.apiBaseUrl)/products") else {
            Logger.warning("[OrdersRepository] Invalid URL")
            return Self.mockProducts
        }
        
        Logger.debug("[OrdersRepository] Fetching products from: \(url)")
        
        do {
            let products = try await apiService.fetchProducts()
            Logger.info("[OrdersRepository] Loaded \(products.count) products from API")
            
            // Debug: Log each product
            for product in products {
                Logger.debug("  [Product] \(product.name) - \(product.price)€ - \(product.category) \(product.emoji ?? "")")
            }
            
            return products
        } catch {
            Logger.error("[OrdersRepository] API Error: \(error.localizedDescription)")
            Logger.error("[OrdersRepository] Full error: \(error)")
            Logger.warning("[OrdersRepository] Using mock products as fallback")
            return Self.mockProducts
        }
    }
    
    // MARK: - Fetch Orders
    
    func fetchOrders(userId: String) async throws -> [Order] {
        // For now, return mock orders - can be extended to fetch from API
        return Self.mockOrders(for: userId)
    }
    
    // MARK: - Create Order
    
    func createOrder(userId: String, items: [CartItem]) async throws -> Order {
        let total = items.reduce(0) { $0 + $1.subtotal }
        
        // Simulate order creation - in production this would POST to API
        let orderItems = items.map { item in
            OrderItem(
                productId: item.productId,
                quantity: item.quantity,
                unitPrice: item.product.price
            )
        }
        
        return Order(
            id: UUID().uuidString,
            orderId: "ORD-\(Int.random(in: 1000...9999))",
            userUid: userId,
            status: .pending,
            items: orderItems,
            totalAmount: total,
            platform: "iOS",
            createdAt: ISO8601DateFormatter().string(from: Date())
        )
    }
    
    // MARK: - Mock Data
    
    static let mockProducts: [Product] = [
        Product(id: "1", productId: "prod-1", name: "Camiseta Circuit", description: "Camiseta oficial del circuito", price: 35.99, stock: 100, category: "merchandise", imageUrl: nil, emoji: "tshirt.fill", isActive: true),
        Product(id: "2", productId: "prod-2", name: "Gorra Racing", description: "Gorra oficial de carreras", price: 24.99, stock: 50, category: "merchandise", imageUrl: nil, emoji: "crown.fill", isActive: true),
        Product(id: "3", productId: "prod-3", name: "Bocadillo", description: "Bocadillo de jamón serrano", price: 8.50, stock: 200, category: "food", imageUrl: nil, emoji: "fork.knife", isActive: true),
        Product(id: "4", productId: "prod-4", name: "Hamburguesa", description: "Hamburguesa premium con patatas", price: 12.99, stock: 150, category: "food", imageUrl: nil, emoji: "fork.knife", isActive: true),
        Product(id: "5", productId: "prod-5", name: "Coca-Cola", description: "Refresco 500ml", price: 3.50, stock: 500, category: "drinks", imageUrl: nil, emoji: "cup.and.saucer.fill", isActive: true),
        Product(id: "6", productId: "prod-6", name: "Agua", description: "Agua mineral 500ml", price: 2.50, stock: 600, category: "drinks", imageUrl: nil, emoji: "drop.fill", isActive: true),
        Product(id: "7", productId: "prod-7", name: "Cerveza", description: "Cerveza nacional 330ml", price: 5.00, stock: 300, category: "drinks", imageUrl: nil, emoji: "mug.fill", isActive: true),
        Product(id: "8", productId: "prod-8", name: "Llavero", description: "Llavero conmemorativo", price: 9.99, stock: 80, category: "merchandise", imageUrl: nil, emoji: "key.fill", isActive: true),
    ]
    
    static func mockOrders(for userId: String) -> [Order] {
        [
            Order(
                id: "order-1",
                orderId: "ORD-1234",
                userUid: userId,
                status: .delivered,
                items: [
                    OrderItem(productId: "1", quantity: 1, unitPrice: 35.99),
                    OrderItem(productId: "5", quantity: 2, unitPrice: 3.50)
                ],
                totalAmount: 42.99,
                platform: "iOS",
                createdAt: "2026-01-28T10:30:00Z"
            )
        ]
    }
}
