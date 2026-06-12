import SwiftUI

struct ParkingHomeView: View {
    @ObservedObject var viewModel: ParkingViewModel
    
    var body: some View {
        ZStack {
            Color(UIColor.systemGroupedBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                // Header
                HStack {
                    Image(systemName: "car.circle.fill")
                        .font(.largeTitle)
                        .foregroundColor(.accentColor)
                    Text(LocalizationUtils.string("Parking Management"))
                        .font(.title2)
                        .bold()
                    Spacer()
                    Button(action: { viewModel.navigateToSupport() }) {
                        Image(systemName: "questionmark.circle")
                            .font(.title3)
                    }
                }
                .padding(.horizontal)
                .padding(.top)
                
                // Content
                if case .loaded(let assignment) = viewModel.viewState {
                    // Active Assignment Card
                    AssignmentCard(assignment: assignment)
                        .onTapGesture {
                            viewModel.navigateToDetail()
                        }
                    
                    // Action Buttons
                    HStack(spacing: 16) {
                        ActionButton(title: LocalizationUtils.string("View Route"), icon: "map.fill", color: .blue) {
                            viewModel.navigateToNavigation()
                        }
                        
                        ActionButton(title: LocalizationUtils.string("Share"), icon: "square.and.arrow.up", color: .green) {
                            // Mock Share
                        }
                    }
                    .padding(.horizontal)
                    
                    Spacer()
                    
                    Button(LocalizationUtils.string("Release / Change Assignment")) {
                        viewModel.clearAssignment()
                    }
                    .buttonStyle(GeoButtonStyle(variant: .tertiary, size: .small))
                    .foregroundColor(.red) // Override for destructive action
                    .padding(.bottom)
                    
                } else if case .loading = viewModel.viewState {
                    ProgressView(LocalizationUtils.string("Loading assignment..."))
                    Spacer()
                } else {
                    // Empty State / Call to Action
                    VStack(spacing: 30) {
                        Spacer()
                        Image(systemName: "parkingsign.circle")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 100, height: 100)
                            .foregroundColor(.gray)
                        
                        Text(LocalizationUtils.string("No parking assigned"))
                            .font(.title3)
                            .foregroundColor(.secondary)
                        
                        Button(action: {
                            viewModel.startWizard()
                        }) {
                            Text(LocalizationUtils.string("Assign my spot"))
                        }
                        .buttonStyle(GeoButtonStyle(variant: .primary, size: .large))
                        .padding(.horizontal, 40)
                        
                        Spacer()
                    }
                }
            }
        }
        .navigationTitle("")
        .navigationBarHidden(true)
    }
}

// MARK: - Subviews

struct AssignmentCard: View {
    let assignment: ParkingAssignment
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("TU PLAZA")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.secondary)
                Spacer()
                Text(assignment.status.rawValue.uppercased())
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green.opacity(0.2))
                    .foregroundColor(.green)
                    .cornerRadius(4)
            }
            
            Divider()
            
            HStack(alignment: .top) {
                VStack(alignment: .leading) {
                    Text("Zona")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(assignment.zone.rawValue)
                        .font(.system(size: 40, weight: .bold))
                        .foregroundColor(Color(assignment.zone.colorName))
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text("Plaza Virtual")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(assignment.virtualSpot)
                        .font(.title)
                        .bold()
                }
            }
            
            Divider()
            
            HStack {
                Image(systemName: "car.fill")
                    .foregroundColor(.secondary)
                Text(assignment.licensePlate)
                    .font(.headline)
                Spacer()
                Text(assignment.ticketId)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: 2)
        .padding(.horizontal)
    }
}

struct ActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Text(title)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(GeoButtonStyle(variant: .secondary, size: .medium))
    }
}

struct ParkingViews_Previews: PreviewProvider {
    static var previews: some View {
        let vm = ParkingViewModel()
        ParkingHomeView(viewModel: vm)
    }
}
