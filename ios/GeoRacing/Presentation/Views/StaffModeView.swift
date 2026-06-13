import SwiftUI

struct StaffModeView: View {
    @State private var pinCode = ""
    @State private var showError = false
    @State private var isAuthenticated = false
    @Environment(\.dismiss) private var dismiss
    
    private let correctPin = "1234" // In production, this should be from secure storage
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if isAuthenticated {
                StaffControlView()
            } else {
                pinEntryView
            }
        }
    }
    
    // MARK: - PIN Entry View
    
    private var pinEntryView: some View {
        VStack(spacing: 32) {
            // Header
            HStack {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.title2.weight(.semibold))
                        .foregroundColor(.white)
                }
                Spacer()
            }
            .padding()
            
            Spacer()
            
            // Lock Icon
            Image(systemName: "lock.shield")
                .font(.system(size: 60))
                .foregroundColor(.orange)
            
            Text(LocalizationUtils.string("Staff Mode"))
                .font(.title.bold())
                .foregroundColor(.white)
            
            Text(LocalizationUtils.string("Enter access PIN"))
                .font(.subheadline)
                .foregroundColor(.gray)
            
            // PIN Display
            HStack(spacing: 16) {
                ForEach(0..<4, id: \.self) { index in
                    Circle()
                        .fill(index < pinCode.count ? Color.orange : Color(white: 0.2))
                        .frame(width: 16, height: 16)
                }
            }
            .padding(.vertical)
            .shake(showError)
            
            // Keypad
            VStack(spacing: 12) {
                ForEach(0..<3) { row in
                    HStack(spacing: 12) {
                        ForEach(1..<4) { col in
                            let number = row * 3 + col
                            keypadButton(String(number))
                        }
                    }
                }
                
                HStack(spacing: 12) {
                    // Empty space
                    Color.clear
                        .frame(width: 80, height: 80)
                    
                    keypadButton("0")
                    
                    // Delete
                    Button {
                        if !pinCode.isEmpty {
                            pinCode.removeLast()
                        }
                    } label: {
                        Image(systemName: "delete.left")
                            .font(.title2)
                            .foregroundColor(.white)
                            .frame(width: 80, height: 80)
                            .background(Color(white: 0.15))
                            .cornerRadius(40)
                    }
                }
            }
            
            Spacer()
        }
    }
    
    private func keypadButton(_ number: String) -> some View {
        Button {
            if pinCode.count < 4 {
                pinCode += number
                
                if pinCode.count == 4 {
                    verifyPin()
                }
            }
        } label: {
            Text(number)
                .font(.title.weight(.semibold))
                .foregroundColor(.white)
                .frame(width: 80, height: 80)
                .background(Color(white: 0.15))
                .cornerRadius(40)
        }
    }
    
    private func verifyPin() {
        if pinCode == correctPin {
            withAnimation {
                isAuthenticated = true
            }
        } else {
            showError = true
            pinCode = ""
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                showError = false
            }
        }
    }
}

// MARK: - Staff Control View

struct StaffControlView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedSection = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            headerView
            
            // Segmented Control
            Picker(LocalizationUtils.string("Section"), selection: $selectedSection) {
                Text(LocalizationUtils.string("Alerts")).tag(0)
                Text("Beacons").tag(1)
                Text(LocalizationUtils.string("Status")).tag(2)
            }
            .pickerStyle(.segmented)
            .padding()
            
            // Content
            TabView(selection: $selectedSection) {
                alertsSection.tag(0)
                beaconsSection.tag(1)
                statusSection.tag(2)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
        }
    }
    
    private var headerView: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            VStack {
                Text(LocalizationUtils.string("Control Panel"))
                    .font(.title2.bold())
                    .foregroundColor(.white)
                Text("STAFF MODE")
                    .font(.caption.weight(.bold))
                    .foregroundColor(.orange)
            }
            
            Spacer()
            
            Color.clear.frame(width: 24)
        }
        .padding()
        .background(Color(white: 0.08))
    }
    
    // MARK: - Alerts Section
    
    private var alertsSection: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(LocalizationUtils.string("Send Alert"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                alertButton(
                    icon: "exclamationmark.triangle.fill",
                    title: LocalizationUtils.string("General Alert"),
                    description: LocalizationUtils.string("Send message to all users"),
                    color: .yellow
                )
                
                alertButton(
                    icon: "flame.fill",
                    title: LocalizationUtils.string("Emergency"),
                    description: LocalizationUtils.string("Activate emergency protocol"),
                    color: .red
                )
                
                alertButton(
                    icon: "megaphone.fill",
                    title: LocalizationUtils.string("Announcement"),
                    description: LocalizationUtils.string("Send general information"),
                    color: .blue
                )
            }
            .padding()
        }
    }
    
    private func alertButton(icon: String, title: String, description: String, color: Color) -> some View {
        Button {
            // Send alert action
        } label: {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title)
                    .foregroundColor(color)
                    .frame(width: 50)
                
                VStack(alignment: .leading) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.white)
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray)
            }
            .padding()
            .background(Color(white: 0.12))
            .cornerRadius(12)
        }
        .accessibilityLabel("\(title): \(description)")
    }
    
    private var beaconsSection: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(LocalizationUtils.string("Beacon Control"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                ForEach([LocalizationUtils.string("Main Entrance"), LocalizationUtils.string("Grandstand A"), LocalizationUtils.string("Grandstand B"), "Paddock", "Pit Lane"], id: \.self) { beacon in
                    beaconRow(name: beacon)
                }
            }
            .padding()
        }
    }
    
    private func beaconRow(name: String) -> some View {
        HStack {
            Circle()
                .fill(Color.green)
                .frame(width: 10, height: 10)
            
            Text(name)
                .foregroundColor(.white)
            
            Spacer()
            
            Toggle("", isOn: .constant(true))
                .tint(.orange)
        }
        .padding()
        .background(Color(white: 0.12))
        .cornerRadius(12)
    }
    
    // MARK: - Status Section
    
    private var statusSection: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(LocalizationUtils.string("Circuit Status"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                statusCard(
                    title: LocalizationUtils.string("Current Status"),
                    value: LocalizationUtils.string("GREEN FLAG"),
                    icon: "flag.fill",
                    color: .green
                )
                
                statusCard(
                    title: LocalizationUtils.string("Active Users"),
                    value: "1,234",
                    icon: "person.2.fill",
                    color: .blue
                )
                
                statusCard(
                    title: LocalizationUtils.string("Pending Alerts"),
                    value: "3",
                    icon: "bell.badge.fill",
                    color: .orange
                )
                
                statusCard(
                    title: LocalizationUtils.string("Active Beacons"),
                    value: "12/15",
                    icon: "antenna.radiowaves.left.and.right",
                    color: .purple
                )
            }
            .padding()
        }
    }
    
    private func statusCard(title: String, value: String, icon: String, color: Color) -> some View {
        HStack {
            Image(systemName: icon)
                .font(.title)
                .foregroundColor(color)
                .frame(width: 50)
            
            VStack(alignment: .leading) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.gray)
                Text(value)
                    .font(.headline)
                    .foregroundColor(.white)
            }
            
            Spacer()
        }
        .padding()
        .background(Color(white: 0.12))
        .cornerRadius(12)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title): \(value)")
    }
}

// MARK: - Shake Effect

extension View {
    func shake(_ trigger: Bool) -> some View {
        modifier(ShakeEffect(trigger: trigger))
    }
}

struct ShakeEffect: ViewModifier {
    let trigger: Bool
    @State private var offset: CGFloat = 0
    
    func body(content: Content) -> some View {
        content
            .offset(x: offset)
            .onChange(of: trigger) { _, newValue in
                if newValue {
                    withAnimation(.default.repeatCount(4, autoreverses: true).speed(4)) {
                        offset = 10
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        offset = 0
                    }
                }
            }
    }
}

#Preview {
    StaffModeView()
}
