import Foundation
import Combine

@MainActor
final class OrdersViewModel: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var products: [Product] = []
    @Published var cart: [String: CartItem] = [:]
    @Published var orders: [Order] = []
    @Published var isLoading = false
    @Published var showSuccessDialog = false
    @Published var errorMessage: String?
    @Published var selectedCategory: String?
    
    // MARK: - Computed Properties
    
    var cartItems: [CartItem] {
        Array(cart.values).sorted { $0.product.name < $1.product.name }
    }
    
    var cartTotal: Double {
        cart.values.reduce(0) { $0 + $1.subtotal }
    }
    
    var cartItemCount: Int {
        cart.values.reduce(0) { $0 + $1.quantity }
    }
    
    var filteredProducts: [Product] {
        guard let category = selectedCategory else {
            return products.filter { $0.isActive }
        }
        return products.filter { $0.isActive && $0.category.lowercased() == category.lowercased() }
    }
    
    var isCartEmpty: Bool {
        cart.isEmpty
    }
    
    var categories: [String] {
        let cats = Set(products.map { $0.category.lowercased() })
        return Array(cats).sorted()
    }
    
    // MARK: - Dependencies
    
    private let repository: OrdersRepositoryProtocol
    private let authService: AuthService
    
    // MARK: - Initialization
    
    init(repository: OrdersRepositoryProtocol = OrdersRepository(),
         authService: AuthService = .shared) {
        self.repository = repository
        self.authService = authService
    }
    
    // MARK: - Actions
    
    func loadProducts() async {
        isLoading = true
        errorMessage = nil
        
        do {
            products = try await repository.fetchProducts()
        } catch {
            errorMessage = "Error cargando productos: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func loadOrders() async {
        guard let userId = authService.currentUser?.uid else {
            errorMessage = "Debes iniciar sesión para ver tus pedidos"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            orders = try await repository.fetchOrders(userId: userId)
        } catch {
            errorMessage = "Error cargando pedidos: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func addToCart(_ product: Product) {
        if var existing = cart[product.id] {
            existing.quantity += 1
            cart[product.id] = existing
        } else {
            cart[product.id] = CartItem(productId: product.id, product: product, quantity: 1)
        }
    }
    
    func removeFromCart(_ product: Product) {
        guard var existing = cart[product.id] else { return }
        
        if existing.quantity > 1 {
            existing.quantity -= 1
            cart[product.id] = existing
        } else {
            cart.removeValue(forKey: product.id)
        }
    }
    
    func clearCart() {
        cart.removeAll()
    }
    
    func checkout() async {
        guard let userId = authService.currentUser?.uid else {
            errorMessage = LocalizationUtils.string("You must be logged in to place an order")
            return
        }
        
        guard !cart.isEmpty else {
            errorMessage = LocalizationUtils.string("Cart is empty")
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let order = try await repository.createOrder(userId: userId, items: cartItems)
            orders.insert(order, at: 0)
            clearCart()
            showSuccessDialog = true
        } catch {
            errorMessage = String(format: LocalizationUtils.string("Order processing error"), error.localizedDescription)
        }
        
        isLoading = false
    }
    
    func dismissSuccessDialog() {
        showSuccessDialog = false
    }
    
    func setFilter(_ category: String?) {
        selectedCategory = category
    }
    
    func quantity(for product: Product) -> Int {
        cart[product.id]?.quantity ?? 0
    }
}
