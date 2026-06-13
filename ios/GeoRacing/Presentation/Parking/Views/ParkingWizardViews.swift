import SwiftUI

struct ParkingWizardStep1View: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        VStack(spacing: 20) {
            WizardHeader(title: LocalizationUtils.string("Step 1 of 3"), subtitle: LocalizationUtils.string("Scan your ticket"))
            
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.black.opacity(0.1))
                    .frame(height: 300)
                
                if viewModel.scannedTicketId.isEmpty {
                    VStack {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        Text(LocalizationUtils.string("Simulating camera..."))
                            .font(.caption)
                            .padding(.top)
                        
                        Button(LocalizationUtils.string("Simulate Scan")) {
                            viewModel.simulateScan()
                        }
                        .buttonStyle(GeoButtonStyle(variant: .secondary, size: .small))
                        .padding()
                    }
                } else {
                    VStack {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.green)
                        Text(LocalizationUtils.string("Ticket Detected"))
                            .font(.headline)
                            .padding(.top)
                        Text(viewModel.scannedTicketId)
                            .font(.monospaced(.body)())
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()
            
            HStack {
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(Color(UIColor.separator))
                Text(LocalizationUtils.string("Or"))
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(Color(UIColor.separator))
            }
            .padding(.horizontal)
            
            VStack(alignment: .leading, spacing: 8) {
                Text(LocalizationUtils.string("Enter code manually"))
                    .font(.caption)
                    .foregroundColor(.secondary)
                TextField("Ej: TICKET-9999", text: $viewModel.scannedTicketId)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(10)
                    .textInputAutocapitalization(.characters)
            }
            .padding(.horizontal)
            
            Spacer()
            
            Button(action: viewModel.submitTicketScan) {
                Text(LocalizationUtils.string("Continue"))
            }
            .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
            .disabled(viewModel.scannedTicketId.isEmpty)
            .padding()
        }
        .padding()
    }
}

struct ParkingWizardStep2View: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        VStack(spacing: 20) {
            WizardHeader(title: LocalizationUtils.string("Step 2 of 3"), subtitle: LocalizationUtils.string("Enter your license plate"))
            
            TextField("Ej: 1234ABC", text: $viewModel.licensePlateInput)
                .font(.system(size: 32, weight: .bold, design: .monospaced))
                .multilineTextAlignment(.center)
                .padding()
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
                .textInputAutocapitalization(.characters)
                .padding(.horizontal)
            
            Text(LocalizationUtils.string("Required to validate your parking access."))
                .font(.caption)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Button(action: viewModel.submitLicensePlate) {
                Text(LocalizationUtils.string("Continue"))
            }
            .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
            .disabled(!viewModel.validateLicensePlate(viewModel.licensePlateInput))
            .padding()
        }
        .padding()
    }
}

struct ParkingWizardStep3View: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        VStack(spacing: 20) {
            WizardHeader(title: LocalizationUtils.string("Confirm"), subtitle: LocalizationUtils.string("Review details"))
            
            List {
                Section(header: Text(LocalizationUtils.string("Ticket"))) {
                    HStack {
                        Text("Ticket ID")
                        Spacer()
                        Text(viewModel.scannedTicketId)
                            .font(.monospaced(.body)())
                    }

                    Text(LocalizationUtils.string("Valid until end of day"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Section(header: Text(LocalizationUtils.string("Vehicle"))) {
                    HStack {
                        Text(LocalizationUtils.string("License Plate"))
                        Spacer()
                        Text(viewModel.licensePlateInput)
                            .bold()
                    }
                }
            }
            .listStyle(.insetGrouped)
            
            if case .error(let error) = viewModel.viewState {
                Text(error.localizedDescription)
                    .foregroundColor(.red)
                    .font(.caption)
            }
            
            Button(action: {
                Task {
                    await viewModel.confirmAssignment()
                }
            }) {
                if case .loading = viewModel.viewState {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(LocalizationUtils.string("Confirm and Assign"))
                }
            }
            .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
            .padding()
        }
    }
}


struct ParkingWizardStep4View: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        VStack(spacing: 30) {
            Spacer()
            
            Image(systemName: "checkmark.seal.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 100, height: 100)
                .foregroundColor(.green)
            
            Text(LocalizationUtils.string("Spot Assigned!"))
                .font(.largeTitle)
                .bold()
            
            if case .loaded(let assignment) = viewModel.viewState {
                VStack {
                    Text(LocalizationUtils.string("Go to Zone"))
                        .foregroundColor(.secondary)
                    Text(assignment.zone.rawValue)
                        .font(.system(size: 80, weight: .heavy))
                        .foregroundColor(Color(assignment.zone.colorName))
                    
                    Text("\(LocalizationUtils.string("Virtual Spot")): \(assignment.virtualSpot)")
                        .font(.title2)
                        .padding(.top, 10)
                }
                .padding()
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(20)
                .shadow(radius: 10)
                
                VStack(spacing: 8) {
                    Text(LocalizationUtils.string("Staff Validation"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    QRCodeView(content: "GEORACING:\(assignment.id.uuidString)")
                        .frame(width: 150, height: 150)
                    
                    Text(LocalizationUtils.string("This QR code is your confirmed access pass. Show it to security staff to enter your zone."))
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                        .frame(maxWidth: 250)
                }
                .padding(.top)
            }
            
            Spacer()
            
            Button(action: viewModel.finishWizard) {
                Text(LocalizationUtils.string("Go to Home"))
            }
            .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
            .padding()
        }
        .navigationBarBackButtonHidden(true)
        .padding()
    }
}

// Helper for consistency
struct WizardHeader: View {
    let title: String
    let subtitle: String
    
    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
            Text(subtitle)
                .font(.title2)
                .bold()
        }
        .padding(.top)
    }
}
