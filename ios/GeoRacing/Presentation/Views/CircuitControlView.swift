import SwiftUI

struct CircuitControlView: View {
    @State private var selectedStatus: TrackStatus = .green
    @State private var customMessage: String = ""
    @State private var isUpdating = false
    @State private var errorMessage: String?
    
    // Environment object to observe current state
    @ObservedObject var repository = CircuitStatusRepository.shared
    
    var body: some View {
        Form {
            Section(header: Text("Current Status")) {
                HStack {
                    Label(repository.currentStatus.titleKey, systemImage: repository.currentStatus.iconName)
                        .foregroundColor(repository.currentStatus.color)
                    Spacer()
                    if repository.currentStatus == .evacuation {
                        Text("EVACUATION")
                            .font(.caption)
                            .padding(4)
                            .background(Color.red)
                            .foregroundColor(.white)
                            .cornerRadius(4)
                    }
                }
                Text("Message: \(repository.statusMessage)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Section(header: Text("Update Status")) {
                Picker("Flag Color", selection: $selectedStatus) {
                    ForEach(TrackStatus.allCases, id: \.self) { status in
                        Label(status.rawValue.uppercased(), systemImage: status.iconName)
                            .foregroundColor(status.color)
                            .tag(status)
                    }
                }
                .pickerStyle(.segmented)
                
                VStack(alignment: .leading) {
                    Text("Custom Message")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField("Enter message for drivers...", text: $customMessage, axis: .vertical)
                        .lineLimit(3...5)
                        .textFieldStyle(.roundedBorder)
                }
            }
            
            Section {
                Button(action: updateStatus) {
                    if isUpdating {
                        ProgressView()
                    } else {
                        Text("Update Circuit State")
                            .frame(maxWidth: .infinity)
                            .foregroundColor(.white)
                    }
                }
                .listRowBackground(Color.blue)
                .disabled(isUpdating)
            }
            
            if let error = errorMessage {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                }
            }
        }
        .navigationTitle("Circuit Control")
        .onAppear {
            // Pre-fill with current values
            self.selectedStatus = repository.currentStatus
            self.customMessage = repository.statusMessage
        }
    }
    
    private func updateStatus() {
        guard !isUpdating else { return }
        isUpdating = true
        errorMessage = nil
        
        Task {
            do {
                try await repository.updateStatus(mode: selectedStatus, message: customMessage)
                // Success feedback handled by repo updating observed properties
            } catch {
                errorMessage = "Failed to update: \(error.localizedDescription)"
            }
            isUpdating = false
        }
    }
}

#Preview {
    NavigationView {
        CircuitControlView()
    }
}
