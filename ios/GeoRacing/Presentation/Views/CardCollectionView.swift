import SwiftUI

// MARK: - Card Collection View

/// "My Collection" screen with filters by rarity, team, and unlock status.
struct CardCollectionView: View {
    @ObservedObject var viewModel: FanZoneViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedFilter: CollectionFilter = .all
    @State private var selectedCard: RewardCardDefinition?
    @State private var showCardDetail = false
    
    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]
    
    enum CollectionFilter: String, CaseIterable {
        case all = "All"
        case unlocked = "Unlocked"
        case locked = "Locked"
        case common = "Common"
        case rare = "Rare"
        case epic = "Epic"
        case legendary = "Legendary"
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Collection Summary
                    collectionHeader
                    
                    // Filters
                    filterBar
                    
                    // Cards Grid
                    if filteredCards.isEmpty {
                        emptyState
                    } else {
                        cardsGrid
                    }
                }
            }
            .navigationTitle(LocalizationUtils.string("My Collection"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(RacingColors.silver)
                    }
                }
            }
            .toolbarColorScheme(.dark, for: .navigationBar)
            .sheet(isPresented: $showCardDetail) {
                if let card = selectedCard {
                    CardDetailSheet(
                        card: card,
                        team: viewModel.selectedTeam,
                        isUnlocked: viewModel.rewardService.progress[card.id]?.isUnlocked ?? false,
                        progress: viewModel.rewardService.progressRatio(for: card.id)
                    )
                }
            }
        }
    }
    
    // MARK: - Collection Header
    
    private var collectionHeader: some View {
        HStack(spacing: 20) {
            // Progress Circle
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 6)
                
                Circle()
                    .trim(from: 0, to: collectionProgress)
                    .stroke(viewModel.teamColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                
                Text(viewModel.rewardService.collectionSummary)
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
            }
            .frame(width: 60, height: 60)
            
            // Stats
            VStack(alignment: .leading, spacing: 4) {
                Text(LocalizationUtils.string("Collection Progress"))
                    .font(RacingFont.subheader(15))
                    .foregroundColor(.white)
                
                HStack(spacing: 12) {
                    rarityCount(.common)
                    rarityCount(.rare)
                    rarityCount(.epic)
                    rarityCount(.legendary)
                }
            }
            
            Spacer()
        }
        .padding()
        .background(RacingColors.cardBackground)
    }
    
    private func rarityCount(_ rarity: CardRarity) -> some View {
        let total = viewModel.rewardService.cards(rarity: rarity).count
        let unlocked = viewModel.rewardService.cards(rarity: rarity)
            .filter { viewModel.rewardService.progress[$0.id]?.isUnlocked ?? false }.count
        
        return HStack(spacing: 2) {
            Circle()
                .fill(rarity.color)
                .frame(width: 8, height: 8)
            Text("\(unlocked)/\(total)")
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(RacingColors.silver)
        }
    }
    
    private var collectionProgress: CGFloat {
        let total = viewModel.rewardService.cardDefinitions.count
        guard total > 0 else { return 0 }
        return CGFloat(viewModel.rewardService.unlockedCards.count) / CGFloat(total)
    }
    
    // MARK: - Filter Bar
    
    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(CollectionFilter.allCases, id: \.self) { filter in
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedFilter = filter
                        }
                    }) {
                        Text(filterLabel(filter))
                            .font(.system(size: 13, weight: selectedFilter == filter ? .bold : .medium))
                            .foregroundColor(selectedFilter == filter ? .white : RacingColors.silver)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(selectedFilter == filter
                                          ? filterColor(filter)
                                          : Color.gray.opacity(0.15))
                            )
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 10)
        }
    }
    
    private func filterLabel(_ filter: CollectionFilter) -> String {
        switch filter {
        case .all: return LocalizationUtils.string("All")
        case .unlocked: return LocalizationUtils.string("Unlocked")
        case .locked: return LocalizationUtils.string("Locked")
        default: return LocalizationUtils.string(filter.rawValue)
        }
    }
    
    private func filterColor(_ filter: CollectionFilter) -> Color {
        switch filter {
        case .all: return viewModel.teamColor
        case .unlocked: return .green
        case .locked: return .gray
        case .common: return CardRarity.common.color
        case .rare: return CardRarity.rare.color
        case .epic: return CardRarity.epic.color
        case .legendary: return CardRarity.legendary.color
        }
    }
    
    // MARK: - Filtered Cards
    
    private var filteredCards: [RewardCardDefinition] {
        let cards = viewModel.rewardService.cardDefinitions
        switch selectedFilter {
        case .all:
            return cards
        case .unlocked:
            return cards.filter { viewModel.rewardService.progress[$0.id]?.isUnlocked ?? false }
        case .locked:
            return cards.filter { !(viewModel.rewardService.progress[$0.id]?.isUnlocked ?? false) }
        case .common:
            return cards.filter { $0.rarity == .common }
        case .rare:
            return cards.filter { $0.rarity == .rare }
        case .epic:
            return cards.filter { $0.rarity == .epic }
        case .legendary:
            return cards.filter { $0.rarity == .legendary }
        }
    }
    
    // MARK: - Cards Grid
    
    private var cardsGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 16) {
                ForEach(filteredCards) { card in
                    let isUnlocked = viewModel.rewardService.progress[card.id]?.isUnlocked ?? false
                    let cardProgress = viewModel.rewardService.progressRatio(for: card.id)
                    
                    CardView(
                        card: card,
                        team: viewModel.selectedTeam,
                        isUnlocked: isUnlocked,
                        progress: cardProgress
                    )
                    .onTapGesture {
                        selectedCard = card
                        showCardDetail = true
                    }
                }
            }
            .padding()
        }
    }
    
    // MARK: - Empty State
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "rectangle.stack")
                .font(.system(size: 48))
                .foregroundColor(RacingColors.silver.opacity(0.4))
            Text(LocalizationUtils.string("No cards in this filter"))
                .font(RacingFont.body())
                .foregroundColor(RacingColors.silver)
            Spacer()
        }
    }
}
