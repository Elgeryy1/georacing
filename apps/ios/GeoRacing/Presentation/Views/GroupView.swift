import SwiftUI

struct GroupView: View {
    @StateObject private var viewModel = GroupViewModel()
    @Environment(\.dismiss) var dismiss
    
    // Optional: Pass navigation capability to Map if integrated differently
    // For now we assume "View on Map" just dismisses this sheet if opened from Map,
    // OR we might need to navigate to Map.
    // The user requirement says "Boton Ver en Mapa (abre CircuitMapView con layer de grupo activa)".
    // If we are in features overview, we present this as a sheet. "Ver en Mapa" should probably open Map View.
    @State private var showMap = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                if viewModel.isLoading {
                    ProgressView()
                } else if viewModel.isInGroup {
                    activeGroupView
                } else {
                    noGroupView
                }
            }
            .navigationTitle("Grupo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizationUtils.string("Close")) {
                        dismiss()
                    }
                }
            }
            .navigationDestination(isPresented: $showMap) {
                CircuitMapView()
            }
        }
        .onAppear {
            // Check if already in group via repository
        }
    }
    
    var noGroupView: some View {
        VStack(spacing: 24) {
            Image(systemName: "person.3.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundColor(RacingColors.red)
            
            Text(LocalizationUtils.string("Connect with your group"))
                .font(RacingFont.header(24))
                .foregroundColor(.white)
            
            Text(LocalizationUtils.string("Create a group to share location in real time at the circuit."))
                .font(RacingFont.body())
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Divider().background(Color.gray)
            
            // Create
            Button(action: {
                Task {
                    await viewModel.createGroup()
                }
            }) {
                Text(LocalizationUtils.string("Create New Group"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(RacingColors.red)
                    .cornerRadius(12)
            }
            
            Text(LocalizationUtils.string("Or"))
                .foregroundColor(.gray)
            
            // Join
            VStack {
                TextField(LocalizationUtils.string("Group Code"), text: $viewModel.joinCode)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(8)
                    .foregroundColor(.white)
                
                Button(action: {
                    Task {
                        await viewModel.joinGroup()
                    }
                }) {
                    Text(LocalizationUtils.string("Join"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(viewModel.joinCode.isEmpty ? Color.gray : RacingColors.silver)
                        .cornerRadius(12)
                }
                .disabled(viewModel.joinCode.isEmpty)
            }
            
            if let error = viewModel.errorMsg {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }
            
            Spacer()
        }
        .padding()
    }
    
    var activeGroupView: some View {
        VStack(spacing: 20) {
            // Header Info
            VStack(spacing: 8) {
                Text(viewModel.currentGroup?.name ?? "Grupo")
                    .font(RacingFont.header(22))
                    .foregroundColor(.white)
                
                HStack {
                    Text("Código:")
                        .foregroundColor(.gray)
                    Text(viewModel.currentGroup?.id ?? "---")
                        .font(.monospacedDigit(.system(size: 18, weight: .bold))())
                        .foregroundColor(RacingColors.silver)
                    
                    Button(action: {
                        UIPasteboard.general.string = viewModel.currentGroup?.id
                    }) {
                        Image(systemName: "doc.on.doc")
                            .foregroundColor(RacingColors.red)
                    }
                }
                .padding(8)
                .background(Color.white.opacity(0.05))
                .cornerRadius(8)
            }
            .padding(.top)
            
            // Map Button
            Button(action: {
                showMap = true
            }) {
                HStack {
                    Image(systemName: "map.fill")
                    Text(LocalizationUtils.string("View on Map"))
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(LinearGradient(gradient: Gradient(colors: [RacingColors.red, Color.orange]), startPoint: .leading, endPoint: .trailing))
                .cornerRadius(12)
                .shadow(radius: 4)
            }
            .padding(.horizontal)
            
            // Members List
            List {
                Section(header: Text("Miembros (\(viewModel.members.count))").foregroundColor(.gray)) {
                    ForEach(viewModel.members) { member in
                        HStack {
                            Image(systemName: "person.crop.circle.fill")
                                .foregroundColor(member.isSharing ? .green : .gray)
                            
                            VStack(alignment: .leading) {
                                Text(member.displayName)
                                    .font(RacingFont.body())
                                    .foregroundColor(.white)
                            }
                            
                            Spacer()
                            
                            if member.isSharing {
                                Image(systemName: "location.fill")
                                    .font(.caption)
                                    .foregroundColor(.green)
                            }
                        }
                        .listRowBackground(Color.clear)
                    }
                }
            }
            .listStyle(.plain)
            
            // Footer: Leave
            Button(action: {
                viewModel.leaveGroup()
            }) {
                Text(LocalizationUtils.string("Leave Group"))
                    .foregroundColor(.red)
            }
            .padding(.bottom)
        }
    }
}
