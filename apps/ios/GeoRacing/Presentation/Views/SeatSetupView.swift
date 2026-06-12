import SwiftUI
import Combine

struct SeatSetupView: View {
    @StateObject private var viewModel = SeatSetupViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showSaveConfirmation = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    headerView
                    
                    // Info Card
                    infoCard
                    
                    // Form Fields
                    formFields
                    
                    // Save Button
                    saveButton
                    
                    Spacer(minLength: 40)
                }
                .padding()
            }
        }
        .alert(LocalizationUtils.string("Seat saved"), isPresented: $showSaveConfirmation) {
            Button("OK") {
                dismiss()
            }
        } message: {
            Text(LocalizationUtils.string("Your seat has been saved. You can use it to navigate directly to your seat."))
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
            
            Text(LocalizationUtils.string("My Seat"))
                .font(.title2.bold())
                .foregroundColor(.white)
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
    }
    
    // MARK: - Info Card
    
    private var infoCard: some View {
        HStack(spacing: 16) {
            Image(systemName: "ticket.fill")
                .font(.largeTitle)
                .foregroundColor(.orange)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(LocalizationUtils.string("Configure your seat"))
                    .font(.headline)
                    .foregroundColor(.white)
                Text(LocalizationUtils.string("Save your seat to navigate directly to it from anywhere in the circuit."))
                    .font(.caption)
                    .foregroundColor(.gray)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(white: 0.12))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.orange.opacity(0.3), lineWidth: 1)
                )
        )
    }
    
    // MARK: - Form Fields
    
    private var formFields: some View {
        VStack(spacing: 16) {
            // Tribuna
            formField(
                icon: "building.2",
                title: LocalizationUtils.string("Grandstand"),
                text: $viewModel.tribuna,
                placeholder: LocalizationUtils.string("e.g. Main Grandstand")
            )
            
            // Zona
            formField(
                icon: "square.grid.2x2",
                title: LocalizationUtils.string("Zone"),
                text: $viewModel.zona,
                placeholder: LocalizationUtils.string("e.g. Zone A")
            )
            
            // Fila
            formField(
                icon: "line.3.horizontal",
                title: LocalizationUtils.string("Row"),
                text: $viewModel.fila,
                placeholder: LocalizationUtils.string("e.g. Row 12")
            )
            
            // Asiento
            formField(
                icon: "chair",
                title: LocalizationUtils.string("Seat"),
                text: $viewModel.asiento,
                placeholder: LocalizationUtils.string("e.g. 24")
            )
        }
    }
    
    private func formField(icon: String, title: String, text: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: icon)
                .font(.subheadline.weight(.medium))
                .foregroundColor(.gray)
            
            TextField(placeholder, text: text)
                .textFieldStyle(RacingTextFieldStyle())
        }
    }
    
    // MARK: - Save Button
    
    private var saveButton: some View {
        Button {
            viewModel.save()
            showSaveConfirmation = true
        } label: {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                Text(LocalizationUtils.string("Save Seat"))
                    .fontWeight(.bold)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.orange)
            .foregroundColor(.black)
            .cornerRadius(12)
        }
        .disabled(!viewModel.isValid)
        .opacity(viewModel.isValid ? 1 : 0.5)
    }
}

// MARK: - Custom Text Field Style

struct RacingTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding()
            .background(Color(white: 0.12))
            .cornerRadius(12)
            .foregroundColor(.white)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
            )
    }
}

// MARK: - Seat Setup ViewModel

@MainActor
final class SeatSetupViewModel: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var tribuna: String
    @Published var zona: String
    @Published var fila: String
    @Published var asiento: String
    
    // MARK: - Computed
    
    var isValid: Bool {
        !tribuna.trimmingCharacters(in: .whitespaces).isEmpty ||
        !zona.trimmingCharacters(in: .whitespaces).isEmpty ||
        !fila.trimmingCharacters(in: .whitespaces).isEmpty ||
        !asiento.trimmingCharacters(in: .whitespaces).isEmpty
    }
    
    var seatDescription: String {
        [tribuna, zona, fila, asiento]
            .filter { !$0.isEmpty }
            .joined(separator: " - ")
    }
    
    // MARK: - Private
    
    private let defaults = UserDefaults.standard
    
    private enum Keys {
        static let tribuna = "seat.tribuna"
        static let zona = "seat.zona"
        static let fila = "seat.fila"
        static let asiento = "seat.asiento"
    }
    
    // MARK: - Initialization
    
    init() {
        self.tribuna = defaults.string(forKey: Keys.tribuna) ?? ""
        self.zona = defaults.string(forKey: Keys.zona) ?? ""
        self.fila = defaults.string(forKey: Keys.fila) ?? ""
        self.asiento = defaults.string(forKey: Keys.asiento) ?? ""
    }
    
    // MARK: - Actions
    
    func save() {
        defaults.set(tribuna, forKey: Keys.tribuna)
        defaults.set(zona, forKey: Keys.zona)
        defaults.set(fila, forKey: Keys.fila)
        defaults.set(asiento, forKey: Keys.asiento)
    }
    
    func clear() {
        tribuna = ""
        zona = ""
        fila = ""
        asiento = ""
        defaults.removeObject(forKey: Keys.tribuna)
        defaults.removeObject(forKey: Keys.zona)
        defaults.removeObject(forKey: Keys.fila)
        defaults.removeObject(forKey: Keys.asiento)
    }
}

#Preview {
    SeatSetupView()
}
