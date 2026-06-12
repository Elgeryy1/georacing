import SwiftUI

struct OrdersView: View {
    @StateObject private var viewModel = OrdersViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showCart = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                headerView
                categoryFilterView
                
                if viewModel.isLoading && viewModel.products.isEmpty {
                    loadingView
                } else if let error = viewModel.errorMessage {
                    errorView(error)
                } else {
                    productsGrid
                }
            }
            
            if !viewModel.isCartEmpty {
                VStack {
                    Spacer()
                    cartFloatingButton
                }
            }
        }
        .sheet(isPresented: $showCart) {
            CartSheetView(viewModel: viewModel)
        }
        .alert(LocalizationUtils.string("Success"), isPresented: $viewModel.showSuccessDialog) {
            Button("OK") { viewModel.dismissSuccessDialog() }
        } message: {
            Text(LocalizationUtils.string("Order processed successfully"))
        }
        .task {
            await viewModel.loadProducts()
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            Spacer()
            
            Text("Tienda")
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Button {
                showCart = true
            } label: {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "cart")
                        .font(.title2)
                        .foregroundColor(.white)
                    
                    if viewModel.cartItemCount > 0 {
                        Text("\(viewModel.cartItemCount)")
                            .font(.caption2.bold())
                            .foregroundColor(.white)
                            .padding(4)
                            .background(Color.orange)
                            .clipShape(Circle())
                            .offset(x: 8, y: -8)
                    }
                }
            }
        }
        .padding()
        .background(Color.black.opacity(0.8))
    }
    
    // MARK: - Category Filter
    
    private var categoryFilterView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                categoryChip(nil, title: LocalizationUtils.string("All"), icon: "square.grid.2x2")
                
                ForEach(viewModel.categories, id: \.self) { category in
                    categoryChip(category, title: displayName(for: category), icon: icon(for: category))
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
        }
        .background(Color(white: 0.1))
    }
    
    private func categoryChip(_ category: String?, title: String, icon: String) -> some View {
        let isSelected = viewModel.selectedCategory == category
        return Button {
            viewModel.setFilter(category)
        } label: {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.subheadline.weight(.medium))
            }
            .foregroundColor(isSelected ? .black : .white)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(isSelected ? Color.orange : Color(white: 0.2))
            .cornerRadius(20)
        }
    }
    
    private func displayName(for category: String) -> String {
        switch category.lowercased() {
        case "comida", "food": return "Comida"
        case "bebidas", "drinks": return "Bebidas"
        case "merchandise", "merch": return "Merch"
        case "tickets": return "Tickets"
        default: return category.capitalized
        }
    }
    
    private func icon(for category: String) -> String {
        switch category.lowercased() {
        case "comida", "food": return "fork.knife"
        case "bebidas", "drinks": return "cup.and.saucer.fill"
        case "merchandise", "merch": return "tshirt.fill"
        case "tickets": return "ticket.fill"
        default: return "tag.fill"
        }
    }
    
    // MARK: - Products Grid
    
    private var productsGrid: some View {
        ScrollView {
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 16),
                GridItem(.flexible(), spacing: 16)
            ], spacing: 16) {
                ForEach(viewModel.filteredProducts) { product in
                    ProductCardView(
                        product: product,
                        quantity: viewModel.quantity(for: product),
                        onAdd: { viewModel.addToCart(product) },
                        onRemove: { viewModel.removeFromCart(product) }
                    )
                }
            }
            .padding()
            .padding(.bottom, 80)
        }
    }
    
    // MARK: - Loading & Error
    
    private var loadingView: some View {
        VStack {
            Spacer()
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .orange))
                .scaleEffect(1.5)
            Text(LocalizationUtils.string("Loading products..."))
                .foregroundColor(.gray)
                .padding(.top)
            Spacer()
        }
    }
    
    private func errorView(_ message: String) -> some View {
        VStack {
            Spacer()
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundColor(.orange)
            Text(message)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding()
            Button("Reintentar") {
                Task { await viewModel.loadProducts() }
            }
            .foregroundColor(.orange)
            Spacer()
        }
    }
    
    // MARK: - Floating Cart Button
    
    private var cartFloatingButton: some View {
        Button {
            showCart = true
        } label: {
            HStack {
                Image(systemName: "cart.fill")
                Text("Ver Carrito")
                    .fontWeight(.semibold)
                Spacer()
                Text(String(format: "%.2f €", viewModel.cartTotal))
                    .fontWeight(.bold)
            }
            .foregroundColor(.black)
            .padding()
            .background(Color.orange)
            .cornerRadius(16)
            .padding(.horizontal)
            .padding(.bottom, 16)
        }
    }
}

// MARK: - Product Card

struct ProductCardView: View {
    let product: Product
    let quantity: Int
    let onAdd: () -> Void
    let onRemove: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image/Emoji
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(LinearGradient(
                        colors: [Color(white: 0.15), Color(white: 0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .aspectRatio(1, contentMode: .fit)
                
                if let emoji = product.emoji {
                    Image(systemName: emoji)
                        .font(.system(size: 40))
                        .foregroundColor(.white)
                } else {
                    Image(systemName: iconForCategory(product.category))
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                }
            }
            
            Text(product.name)
                .font(.subheadline.weight(.medium))
                .foregroundColor(.white)
                .lineLimit(2)
            
            Text(String(format: "%.2f €", product.price))
                .font(.headline.weight(.bold))
                .foregroundColor(.orange)
            
            // Quantity Controls
            HStack {
                if quantity > 0 {
                    Button(action: onRemove) {
                        Image(systemName: "minus")
                            .font(.caption.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 28, height: 28)
                            .background(Color(white: 0.2))
                            .cornerRadius(8)
                    }
                    
                    Text("\(quantity)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                        .frame(width: 28)
                }
                
                Button(action: onAdd) {
                    Image(systemName: "plus")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.black)
                        .frame(width: 28, height: 28)
                        .background(Color.orange)
                        .cornerRadius(8)
                }
                
                Spacer()
            }
        }
        .padding(12)
        .background(Color(white: 0.12))
        .cornerRadius(16)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(product.name), \(String(format: "%.2f", product.price)) euros, quantity: \(quantity)")
        .accessibilityHint("Double tap to add to cart")
    }
    
    private func iconForCategory(_ category: String) -> String {
        switch category.lowercased() {
        case "comida", "food": return "fork.knife"
        case "bebidas", "drinks": return "cup.and.saucer.fill"
        case "merchandise", "merch": return "tshirt.fill"
        case "tickets": return "ticket.fill"
        default: return "tag.fill"
        }
    }
}

// MARK: - Cart Sheet

struct CartSheetView: View {
    @ObservedObject var viewModel: OrdersViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                if viewModel.isCartEmpty {
                    emptyCartView
                } else {
                    cartContentView
                }
            }
            .navigationTitle("Carrito")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizationUtils.string("Close")) { dismiss() }
                        .foregroundColor(.orange)
                }
                if !viewModel.isCartEmpty {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Vaciar") { viewModel.clearCart() }
                            .foregroundColor(.red)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
    
    private var emptyCartView: some View {
        VStack(spacing: 16) {
            Image(systemName: "cart")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            Text("Tu carrito está vacío")
                .font(.title3)
                .foregroundColor(.gray)
        }
    }
    
    private var cartContentView: some View {
        VStack(spacing: 0) {
            List {
                ForEach(viewModel.cartItems) { item in
                    CartItemRow(
                        item: item,
                        onAdd: { viewModel.addToCart(item.product) },
                        onRemove: { viewModel.removeFromCart(item.product) }
                    )
                    .listRowBackground(Color(white: 0.1))
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            
            // Checkout Section
            VStack(spacing: 12) {
                HStack {
                    Text("Total")
                        .font(.title3.weight(.medium))
                        .foregroundColor(.white)
                    Spacer()
                    Text(String(format: "%.2f €", viewModel.cartTotal))
                        .font(.title2.bold())
                        .foregroundColor(.orange)
                }
                
                Button {
                    Task { await viewModel.checkout() }
                } label: {
                    HStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .black))
                        } else {
                            Text("Confirmar Pedido")
                                .fontWeight(.bold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.orange)
                    .foregroundColor(.black)
                    .cornerRadius(12)
                }
                .disabled(viewModel.isLoading)
            }
            .padding()
            .background(Color(white: 0.1))
        }
    }
}

struct CartItemRow: View {
    let item: CartItem
    let onAdd: () -> Void
    let onRemove: () -> Void
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(item.product.name)
                    .foregroundColor(.white)
                Text(String(format: "%.2f € c/u", item.product.price))
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            HStack(spacing: 12) {
                Button(action: onRemove) {
                    Image(systemName: "minus.circle.fill")
                        .foregroundColor(.orange)
                }
                
                Text("\(item.quantity)")
                    .foregroundColor(.white)
                    .frame(width: 24)
                
                Button(action: onAdd) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundColor(.orange)
                }
            }
            
            Text(String(format: "%.2f €", item.subtotal))
                .foregroundColor(.white)
                .fontWeight(.medium)
                .frame(width: 70, alignment: .trailing)
        }
    }
}

#Preview {
    OrdersView()
}
