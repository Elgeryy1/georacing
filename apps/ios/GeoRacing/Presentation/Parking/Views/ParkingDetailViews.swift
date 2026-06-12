import SwiftUI
import MapKit

struct ParkingAssignmentDetailView: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                if case .loaded(let assignment) = viewModel.viewState {
                    // Big summary
                    VStack {
                        Text(assignment.zone.rawValue)
                            .font(.system(size: 100, weight: .black))
                            .foregroundColor(Color(assignment.zone.colorName))
                        Text(assignment.virtualSpot)
                            .font(.largeTitle)
                            .bold()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(20)
                    
                    // Staff Validation QR
                    VStack(spacing: 12) {
                        Text(LocalizationUtils.string("Validation Code"))
                            .font(.headline)
                            .foregroundColor(.secondary)
                        
                        QRCodeView(content: "GEORACING:\(assignment.id.uuidString)")
                            .padding()
                            .background(Color.white)
                            .cornerRadius(12)
                        
                        Text(LocalizationUtils.string("This QR code validates your access to the assigned zone. Keep brightness high when scanning."))
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding()
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    
                    // Details
                    VStack(alignment: .leading, spacing: 16) {
                        DetailRow(label: LocalizationUtils.string("License Plate"), value: assignment.licensePlate)
                        DetailRow(label: "Ticket ID", value: assignment.ticketId)
                        DetailRow(label: LocalizationUtils.string("Status"), value: assignment.status.rawValue.capitalized)
                        DetailRow(label: LocalizationUtils.string("Date"), value: assignment.createdAt.formatted(date: .abbreviated, time: .shortened))
                    }
                    .padding()
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    
                    // Instructions
                    VStack(alignment: .leading, spacing: 10) {
                        Text(LocalizationUtils.string("Access Instructions"))
                            .font(.headline)
                        Text(String(format: LocalizationUtils.string("Follow signs to Zone %@. Upon arrival, show this code to staff or scan the QR at the barrier if available."), assignment.zone.rawValue))
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    
                    // Map Placeholder
                    VStack {
                        Image(systemName: "map.fill")
                            .font(.largeTitle)
                            .padding()
                        Text(LocalizationUtils.string("View location on map"))
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 150)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(16)
                    .onTapGesture {
                        // Open real map MVP placeholder
                    }
                    
                } else {
                    Text(LocalizationUtils.string("No active assignment"))
                }
            }
            .padding()
        }
        .navigationTitle(LocalizationUtils.string("Parking Detail"))
        .background(Color(UIColor.systemGroupedBackground))
    }
}

struct DetailRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .bold()
        }
    }
}

struct ParkingNavigationView: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        VStack {
            Text(LocalizationUtils.string("Guidance In Progress"))
                .font(.largeTitle)
            Spacer()
            // MVP Text Navigation
            if case .loaded(let assignment) = viewModel.viewState {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 100))
                    .foregroundColor(.blue)
                    .padding()
                
                Text(String(format: LocalizationUtils.string("Head to Zone %@"), assignment.zone.rawValue))
                    .font(.title)
                    .bold()
                
                Text(assignment.virtualSpot)
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Button(LocalizationUtils.string("End Navigation")) {
                viewModel.finishWizard() // Just pops back for MVP
            }
            .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
        }
        .padding()
    }
}

struct ParkingSupportView: View {
    var body: some View {
        List {
            Section(header: Text(LocalizationUtils.string("FAQ"))) {
                Text("¿Qué hago si mi plaza está ocupada?")
                Text("¿Cómo cambio mi matrícula?")
                Text("No tengo cobertura")
            }
            
            Section {
                Button("Contactar Soporte") {
                    // Action
                }
                .buttonStyle(GeoButtonStyle(variant: .secondary, size: .medium))
            }
        }
        .navigationTitle("Ayuda Parking")
    }
}
