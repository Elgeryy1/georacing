import Foundation
import Combine

class CartManager: ObservableObject {
    @Published var items: [Product] = []
    @Published var products: [Product] = [] // Loaded products
    @Published var isCheckingOut = false
    @Published var checkoutSuccess = false
    @Published var errorMessage: String?
    
    var total: Double {
        items.reduce(0) { $0 + $1.price }
    }
    
    func loadProducts() {
        Task {
            do {
                let fetched = try await ProductRepository.shared.fetchProducts()
                await MainActor.run {
                    self.products = fetched
                }
            } catch {
                Logger.error("Failed to load products: \(error)")
                // Fallback mock
                await MainActor.run {
                    self.products = [
                        Product(id: "m1", productId: "1", name: "Cap (Mock)", description: "", price: 25.0, stock: 10, category: "Merch", imageUrl: "cap", emoji: "crown.fill", isActive: true),
                        Product(id: "m2", productId: "2", name: "Water (Mock)", description: "", price: 2.0, stock: 10, category: "Drink", imageUrl: "water", emoji: "drop.fill", isActive: true)
                    ]
                }
            }
        }
    }
    
    func add(product: Product) {
        items.append(product)
    }
    
    func remove(product: Product) {
        if let index = items.firstIndex(where: { $0.id == product.id }) {
            items.remove(at: index)
        }
    }
    
    func checkout() async throws {
        guard let user = AuthService.shared.currentUser else {
            await MainActor.run { errorMessage = "Please sign in to checkout." }
            return
        }
        
        await MainActor.run { isCheckingOut = true }
        
        // Group items to OrderItems
        // Use productId if available (Web Panel Link), otherwise fallback to internal id
        let grouped = Dictionary(grouping: items, by: { $0.productId ?? $0.id })
        let orderItems: [OrderItem] = grouped.map { (pid, list) in
            OrderItem(productId: pid, quantity: list.count, unitPrice: list.first?.price ?? 0.0)
        }
        
        do {
            _ = try await ProductRepository.shared.submitOrder(items: orderItems, total: total, user: user)
            await MainActor.run {
                self.items.removeAll()
                self.isCheckingOut = false
                self.checkoutSuccess = true
            }
        } catch {
            await MainActor.run {
                self.errorMessage = "Checkout failed: \(error.localizedDescription)"
                self.isCheckingOut = false
            }
        }
    }
}
