import SwiftUI
import UniformTypeIdentifiers

struct HomeView: View {
    
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var teamTheme = TeamThemeService.shared
    @EnvironmentObject private var circuitState: HybridCircuitStateRepository
    @Binding var selectedTab: TabIdentifier
    @Binding var showMenu: Bool
    @Binding var showParkingSheet: Bool
    @State private var showReportSheet = false
    @State private var showSocialSheet = false
    @State private var showFanZoneSheet = false // NEW
    
    // Edit Mode State
    @State private var isEditing = false
    @State private var draggedItem: String?
    @State private var showAddWidgetSheet = false
    
    let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]
    
    var body: some View {
        NavigationView {
            ZStack {
                // Team-themed gradient background
                teamTheme.backgroundGradient
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        headerView
                        trackStatusCard
                        widgetsGrid
                        newsSection
                        Spacer()
                    }
                    .padding(.top)
                }
            }
            .navigationBarHidden(true)
            .onChange(of: viewModel.activeWidgetIds) { _, newValue in
                viewModel.updateWidgets(newValue)
            }
            .onAppear {
                teamTheme.refresh()
            }
        }
    }
    
    // MARK: - Subviews
    
    private var headerView: some View {
        HStack {
            Button(action: { withAnimation { showMenu = true } }) {
                Image(systemName: "line.3.horizontal")
                    .font(.title)
                    .foregroundColor(.white)
            }
            .accessibilityLabel("Menu")
            
            VStack(alignment: .leading) {
                Text(viewModel.greeting)
                    .font(RacingFont.body())
                    .foregroundColor(RacingColors.silver)
                Text(viewModel.currentDateString)
                    .font(RacingFont.header(20))
                    .foregroundColor(RacingColors.white)
            }
            
            // Team Badge
            Button(action: { showFanZoneSheet = true }) {
                HStack(spacing: 4) {
                    Image(systemName: teamTheme.teamIcon)
                        .font(.title3)
                        .foregroundColor(.white)
                    Text(teamTheme.teamName)
                        .font(RacingFont.body(12))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(teamTheme.primaryColor.opacity(0.3))
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(teamTheme.primaryColor.opacity(0.6), lineWidth: 1)
                )
            }
            .accessibilityLabel("Fan Zone: \(teamTheme.teamName)")
            .accessibilityHint("Opens Fan Zone settings")
            
            
            Spacer()
            
            // Edit Button (Toggle Mode)
            Button(action: {
                withAnimation { isEditing.toggle() }
            }) {
                HStack(spacing: 4) {
                    Image(systemName: isEditing ? "checkmark.circle.fill" : "pencil.circle.fill")
                    if isEditing { Text("Done").font(.caption).bold() }
                }
                .foregroundColor(isEditing ? .green : RacingColors.silver)
                .padding(6)
                .background(isEditing ? Color.black.opacity(0.3) : Color.clear)
                .cornerRadius(16)
            }
            .padding(.trailing, 8)
            
            // Weather Widget (Mini)
            if let weather = viewModel.weather {
                HStack {
                    Image(systemName: weather.iconName)
                        .renderingMode(.original)
                    Text("\(Int(weather.tempC))°")
                        .font(RacingFont.subheader())
                        .foregroundColor(RacingColors.white)
                }
                .padding(8)
                .background(RacingColors.cardBackground)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(RacingColors.silver.opacity(0.3), lineWidth: 1)
                )
            }
        }
        .padding(.horizontal)
    }
    
    private var trackStatusCard: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 16)
                .fill(liveTrackStatus.color.opacity(0.1))
                .frame(height: 180)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(liveTrackStatus.color.opacity(0.3), lineWidth: 1)
                )
            
            HStack {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: liveTrackStatus.iconName)
                            .font(.title)
                            .foregroundColor(liveTrackStatus == .yellow || liveTrackStatus == .sc ? .black : .white)
                            .padding(12)
                            .background(Circle().fill(liveTrackStatus.color))
                        
                        Text(LocalizationUtils.string(liveTrackStatus.titleKey))
                            .font(RacingFont.header(28))
                            .foregroundColor(.white)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(circuitState.message.isEmpty ? LocalizationUtils.string(liveTrackStatus.messageKey) : circuitState.message)
                            .font(RacingFont.subheader())
                            .foregroundColor(RacingColors.silver)
                            .multilineTextAlignment(.leading)
                        if !circuitState.updatedAt.isEmpty {
                            Text(circuitState.updatedAt)
                                .font(RacingFont.body(12))
                                .foregroundColor(RacingColors.silver.opacity(0.8))
                        }
                    }
                }
                .padding(24)
                Spacer()
            }
        }
        .padding(.horizontal)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Track Status: \(LocalizationUtils.string(liveTrackStatus.titleKey)). \(circuitState.message.isEmpty ? LocalizationUtils.string(liveTrackStatus.messageKey) : circuitState.message)")
    }
    
    private var widgetsGrid: some View {
        LazyVGrid(columns: columns, spacing: 20) {
            ForEach(Array(viewModel.activeWidgetIds.enumerated()), id: \.element) { index, id in
                if let widget = getWidget(id) {
                    widgetItem(widget: widget, id: id, index: index)
                }
            }
            
            // "Add Widget" Button (Only in Edit Mode)
            if isEditing {
                Button(action: { showAddWidgetSheet = true }) {
                    VStack {
                        Image(systemName: "plus")
                            .font(.title)
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.white.opacity(0.1))
                            .clipShape(Circle())
                        Text(LocalizationUtils.string("Add Widget"))
                            .font(.caption)
                            .foregroundColor(.white)
                    }
                }
            }
        }
        .padding(.horizontal)
        .sheet(isPresented: $showAddWidgetSheet) {
            HomeAddWidgetSheet(viewModel: viewModel, showSheet: $showAddWidgetSheet)
        }
        .sheet(isPresented: $showReportSheet) {
            IncidentReportView()
        }
        .sheet(isPresented: $showSocialSheet) {
            SocialView()
        }
        .sheet(isPresented: $showFanZoneSheet) {
            FanZoneView()
        }
    }
    
    // MARK: - Widget Item
    
    private func widgetItem(widget: DashboardWidget, id: String, index: Int) -> some View {
        ZStack(alignment: .topTrailing) {
            DashboardButton(icon: widget.icon, title: LocalizationUtils.string(widget.titleKey), color: widget.color) {
                if !isEditing {
                    handleWidgetAction(id)
                }
            }
            .if(isEditing) { view in
                view.onDrag {
                    self.draggedItem = id
                    return NSItemProvider(object: id as NSString)
                }
            }
            .onDrop(of: [UTType.text], delegate: WidgetDropDelegate(item: id, items: $viewModel.activeWidgetIds, draggedItem: $draggedItem))
            .opacity(draggedItem == id ? 0.5 : 1.0)
            .modifier(JiggleModifier(isJiggling: isEditing))
            
            if isEditing {
                Button {
                    withAnimation { viewModel.activeWidgetIds.removeAll { $0 == id } }
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title3)
                        .foregroundColor(.red)
                        .background(Circle().fill(.white))
                }
                .offset(x: 8, y: -8)
            }
        }
    }
    
    // MARK: - News Section
    
    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(LocalizationUtils.string("Latest News"))
                .font(RacingFont.header(24))
                .foregroundColor(.white)
                .padding(.horizontal)
            
            LazyVStack(spacing: 12) {
                ForEach(viewModel.newsItems) { item in
                    NewsItemView(item: item)
                }
            }
            .padding(.horizontal)
        }
    }
    
    // MARK: - Helpers
    
    private var liveTrackStatus: TrackStatus {
        circuitState.resolvedTrackStatus(fallback: viewModel.currentTrackStatus)
    }
    
    private func getWidget(_ id: String) -> DashboardWidget? {
        HomeViewModel.allAvailableWidgets.first { $0.id == id }
    }
    
    private func handleWidgetAction(_ id: String) {
        switch id {
        case "map": selectedTab = .map
        case "shop": selectedTab = .shop
        case "food": selectedTab = .shop
        case "wc": selectedTab = .map
        case "parking": showParkingSheet = true
        case "schedule": break
        case "social": showSocialSheet = true
        case "incidents": showReportSheet = true
        case "tickets": selectedTab = .shop
        case "video": break
        case "weather": break
        case "profile": break
        case "fanzone": showFanZoneSheet = true
        default: break
        }
    }
}

// MARK: - Jiggle Modifier (iOS Style)

struct JiggleModifier: ViewModifier {
    let isJiggling: Bool
    
    @State private var animating = false
    
    func body(content: Content) -> some View {
        content
            .rotationEffect(.degrees(isJiggling && animating ? 2 : (isJiggling ? -2 : 0)))
            .animation(
                isJiggling 
                    ? .easeInOut(duration: 0.1).repeatForever(autoreverses: true)
                    : .easeOut(duration: 0.1),
                value: animating
            )
            .animation(.easeOut(duration: 0.1), value: isJiggling)
            .onChange(of: isJiggling) { _, jiggling in
                animating = jiggling
            }
            .onAppear {
                if isJiggling { animating = true }
            }
    }
}

// MARK: - Drop Delegate
struct WidgetDropDelegate: DropDelegate {
    let item: String
    @Binding var items: [String]
    @Binding var draggedItem: String?
    
    func performDrop(info: DropInfo) -> Bool {
        draggedItem = nil
        return true
    }
    
    func dropEntered(info: DropInfo) {
        guard let draggedItem = draggedItem else { return }
        
        if draggedItem != item {
            if let from = items.firstIndex(of: draggedItem),
               let to = items.firstIndex(of: item) {
                withAnimation {
                    items.move(fromOffsets: IndexSet(integer: from), toOffset: to > from ? to + 1 : to)
                }
            }
        }
    }
}

// MARK: - Add Widget Sheet
struct HomeAddWidgetSheet: View {
    @ObservedObject var viewModel: HomeViewModel
    @Binding var showSheet: Bool
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                List {
                    ForEach(availableIds, id: \.self) { id in
                        if let widget = getWidget(id) {
                            HStack {
                                Image(systemName: widget.icon)
                                    .foregroundColor(widget.color)
                                Text(LocalizationUtils.string(widget.titleKey))
                                    .foregroundColor(.white)
                                Spacer()
                                Button(LocalizationUtils.string("Add")) {
                                    withAnimation {
                                        viewModel.activeWidgetIds.append(id)
                                    }
                                    showSheet = false
                                }
                                .foregroundColor(.green)
                            }
                            .listRowBackground(RacingColors.cardBackground)
                        }
                    }
                }
                .listStyle(InsetGroupedListStyle())
                .onAppear { UITableView.appearance().backgroundColor = .clear }
            }
            .navigationTitle(LocalizationUtils.string("Add Widget"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizationUtils.string("Close")) { showSheet = false }
                }
            }
        }
    }
    
    private var availableIds: [String] {
        HomeViewModel.allAvailableWidgets.map { $0.id }.filter { !viewModel.activeWidgetIds.contains($0) }
    }
    
    private func getWidget(_ id: String) -> DashboardWidget? {
        HomeViewModel.allAvailableWidgets.first { $0.id == id }
    }
}

// MARK: - Edit Mode Wiggle Modifier

struct EditModeWiggle: ViewModifier {
    let isEditing: Bool
    let angle: Double
    let bounceOffset: CGFloat
    let phaseOffset: Double
    
    @State private var phase: Double = 0
    
    func body(content: Content) -> some View {
        content
            .rotationEffect(.degrees(isEditing ? angle * sin(phase * Double.pi * 2) : 0))
            .offset(y: isEditing ? CGFloat(bounceOffset * sin((phase + 0.25) * Double.pi * 2)) : 0)
            .scaleEffect(isEditing ? 0.95 + 0.03 * sin((phase + 0.5) * Double.pi * 2) : 1.0)
            .onChange(of: isEditing) { _, newValue in
                if newValue {
                    startWiggling()
                } else {
                    phase = 0
                }
            }
            .onAppear {
                if isEditing {
                    startWiggling()
                }
            }
    }
    
    private func startWiggling() {
        phase = phaseOffset
        withAnimation(
            .linear(duration: 0.4)
            .repeatForever(autoreverses: false)
        ) {
            phase = 1 + phaseOffset
        }
    }
}

// MARK: - Pulse Effect for Delete Button

struct PulseEffect: ViewModifier {
    @State private var isPulsing = false
    
    func body(content: Content) -> some View {
        content
            .scaleEffect(isPulsing ? 1.15 : 1.0)
            .opacity(isPulsing ? 1.0 : 0.8)
            .onAppear {
                withAnimation(
                    .easeInOut(duration: 0.5)
                    .repeatForever(autoreverses: true)
                ) {
                    isPulsing = true
                }
            }
    }
}
