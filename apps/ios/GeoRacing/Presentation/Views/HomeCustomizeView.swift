import SwiftUI

struct HomeCustomizeView: View {
    @ObservedObject var viewModel: HomeViewModel
    @Environment(\.presentationMode) var presentationMode
    
    // Local state for editing
    @State private var activeIds: [String] = []
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                List {
                    Section(header: Text("Widgets Activos").foregroundColor(.gray)) {
                        ForEach(activeIds, id: \.self) { id in
                            if let widget = getWidget(id) {
                                HStack {
                                    Image(systemName: "line.3.horizontal")
                                        .foregroundColor(.gray)
                                        .padding(.trailing, 8)
                                    
                                    Image(systemName: widget.icon)
                                        .foregroundColor(widget.color)
                                        .frame(width: 24)
                                    
                                    Text(LocalizationUtils.string(widget.titleKey))
                                        .foregroundColor(.white)
                                    
                                    Spacer()
                                    
                                    Button(action: {
                                        withAnimation {
                                            activeIds.removeAll { $0 == id }
                                        }
                                    }) {
                                        Image(systemName: "minus.circle.fill")
                                            .foregroundColor(.red)
                                    }
                                }
                                .listRowBackground(RacingColors.cardBackground)
                            }
                        }
                        .onMove(perform: move)
                    }
                    
                    Section(header: Text("Disponibles").foregroundColor(.gray)) {
                        ForEach(availableIds, id: \.self) { id in
                            if let widget = getWidget(id) {
                                HStack {
                                    Image(systemName: widget.icon)
                                        .foregroundColor(widget.color)
                                        .frame(width: 24)
                                    
                                    Text(LocalizationUtils.string(widget.titleKey))
                                        .foregroundColor(.white)
                                    
                                    Spacer()
                                    
                                    Button(action: {
                                        withAnimation {
                                            activeIds.append(id)
                                        }
                                    }) {
                                        Image(systemName: "plus.circle.fill")
                                            .foregroundColor(.green)
                                    }
                                }
                                .listRowBackground(RacingColors.cardBackground)
                            }
                        }
                    }
                }
                .listStyle(InsetGroupedListStyle())
                // Ensure list background is clear to show dark theme
                .onAppear {
                    UITableView.appearance().backgroundColor = .clear
                }
            }
            .navigationTitle(LocalizationUtils.string("Customize Home"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizationUtils.string("Cancel")) {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizationUtils.string("Save")) {
                        viewModel.updateWidgets(activeIds)
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
        .onAppear {
            self.activeIds = viewModel.activeWidgetIds
        }
    }
    
    // Helpers
    
    private var availableIds: [String] {
        let all = HomeViewModel.allAvailableWidgets.map { $0.id }
        return all.filter { !activeIds.contains($0) }
    }
    
    private func getWidget(_ id: String) -> DashboardWidget? {
        HomeViewModel.allAvailableWidgets.first { $0.id == id }
    }
    
    private func move(from source: IndexSet, to destination: Int) {
        activeIds.move(fromOffsets: source, toOffset: destination)
    }
}
