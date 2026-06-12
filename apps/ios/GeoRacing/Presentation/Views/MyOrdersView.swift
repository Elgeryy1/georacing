import SwiftUI

struct MyOrdersView: View {
    @StateObject private var viewModel = OrdersViewModel()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                headerView
                
                if viewModel.isLoading {
                    loadingView
                } else if viewModel.orders.isEmpty {
                    emptyView
                } else {
                    ordersList
                }
            }
        }
        .task {
            await viewModel.loadOrders()
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Text("Mis Pedidos")
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
        .padding()
        .background(Color.black.opacity(0.8))
    }
    
    // MARK: - Orders List
    
    private var ordersList: some View {
        List {
            ForEach(viewModel.orders) { order in
                OrderRowView(order: order)
                    .listRowBackground(Color(white: 0.1))
                    .listRowSeparatorTint(.gray.opacity(0.3))
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
    
    // MARK: - Loading & Empty
    
    private var loadingView: some View {
        VStack {
            Spacer()
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .orange))
                .scaleEffect(1.5)
            Text(LocalizationUtils.string("Loading orders..."))
                .foregroundColor(.gray)
                .padding(.top)
            Spacer()
        }
    }
    
    private var emptyView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "bag")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            Text("No tienes pedidos")
                .font(.title3)
                .foregroundColor(.gray)
            Text("Tus pedidos aparecerán aquí")
                .font(.subheadline)
                .foregroundColor(.gray.opacity(0.7))
            Spacer()
        }
    }
}

// MARK: - Order Row

struct OrderRowView: View {
    let order: Order
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header Row
            Button {
                withAnimation(.spring(response: 0.3)) {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Pedido #\(order.orderId ?? order.id.prefix(8).description)")
                            .font(.headline)
                            .foregroundColor(.white)
                        
                        Text(formatDate(order.createdAt))
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(String(format: "%.2f €", order.totalAmount))
                            .font(.headline)
                            .foregroundColor(.orange)
                        
                        StatusBadge(status: order.status)
                    }
                    
                    Image(systemName: "chevron.down")
                        .foregroundColor(.gray)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
            }
            
            // Expanded Details
            if isExpanded {
                Divider()
                    .background(Color.gray.opacity(0.3))
                
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(order.items, id: \.productId) { item in
                        HStack {
                            Text("\(item.quantity)x")
                                .foregroundColor(.orange)
                                .frame(width: 30, alignment: .leading)
                            
                            Text("Producto #\(item.productId.prefix(6))")
                                .foregroundColor(.white)
                            
                            Spacer()
                            
                            Text(String(format: "%.2f €", item.unitPrice * Double(item.quantity)))
                                .foregroundColor(.gray)
                        }
                        .font(.subheadline)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding(.vertical, 8)
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            displayFormatter.locale = Locale(identifier: "es")
            return displayFormatter.string(from: date)
        }
        return dateString
    }
}

// MARK: - Status Badge

struct StatusBadge: View {
    let status: OrderStatus
    
    var body: some View {
        Text(displayName)
            .font(.caption2.weight(.semibold))
            .foregroundColor(foregroundColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(backgroundColor)
            .cornerRadius(8)
    }
    
    private var displayName: String {
        switch status {
        case .pending: return "Pendiente"
        case .delivered: return "Entregado"
        case .cancelled: return "Cancelado"
        }
    }
    
    private var backgroundColor: Color {
        switch status {
        case .pending: return Color.yellow.opacity(0.2)
        case .delivered: return Color.green.opacity(0.2)
        case .cancelled: return Color.red.opacity(0.2)
        }
    }
    
    private var foregroundColor: Color {
        switch status {
        case .pending: return .yellow
        case .delivered: return .green
        case .cancelled: return .red
        }
    }
}

#Preview {
    MyOrdersView()
}
