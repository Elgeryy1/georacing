import SwiftUI

// MARK: - Fan Zone View (Main Hub)

struct FanZoneView: View {
    @StateObject private var viewModel = FanZoneViewModel()
    @StateObject private var energyService = EnergyManagementService.shared
    @Environment(\.dismiss) private var dismiss
    
    // Sheet state
    @State private var showTeamSelector = false
    @State private var showQuiz = false
    @State private var showNews = false
    @State private var showCollection = false
    
    // Quick trivia state
    @State private var quickTrivia: QuizQuestion?
    @State private var quickAnswer: Int?
    @State private var showQuickResult = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Team-themed background
                LinearGradient(
                    colors: [viewModel.teamColor.opacity(0.25), RacingColors.darkBackground],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 20) {
                        // Team Header
                        teamHeader
                        
                        if energyService.isSurvivalMode {
                            survivalBanner
                        }
                        
                        // Quick Actions
                        quickActions
                        
                        // News Preview
                        newsPreview
                        
                        // Quick Trivia Widget
                        triviaWidget
                        
                        // Collection Preview
                        collectionPreview
                        
                        // Unlock notification
                        if let unlocked = viewModel.rewardService.recentlyUnlocked {
                            unlockBanner(for: unlocked)
                        }
                        
                        Spacer(minLength: 40)
                    }
                    .padding(.top)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(RacingColors.silver)
                    }
                }
                ToolbarItem(placement: .principal) {
                    Text("Fan Zone")
                        .font(RacingFont.subheader(18))
                        .foregroundColor(.white)
                }
            }
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
        .task { await viewModel.loadAll() }
        .sheet(isPresented: $showTeamSelector) {
            TeamSelectorView(viewModel: viewModel)
        }
        .fullScreenCover(isPresented: $showQuiz) {
            QuizView(viewModel: viewModel)
        }
        .fullScreenCover(isPresented: $showNews) {
            FanNewsView(viewModel: viewModel)
        }
        .fullScreenCover(isPresented: $showCollection) {
            CardCollectionView(viewModel: viewModel)
        }
    }
    
    // MARK: - Survival Banner
    private var survivalBanner: some View {
        HStack(spacing: 12) {
            Image(systemName: "battery.25")
                .font(.title2)
                .foregroundColor(.white)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(LocalizationUtils.string("Survival Mode Active"))
                    .font(RacingFont.subheader(14))
                    .foregroundColor(.white)
                Text(LocalizationUtils.string("Playful features disabled to ensure your return home."))
                    .font(RacingFont.body(12))
                    .foregroundColor(.white.opacity(0.9))
            }
            Spacer()
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.orange.opacity(0.8))
        )
        .padding(.horizontal)
    }
    
    // MARK: - Team Header
    
    private var teamHeader: some View {
        HStack(spacing: 16) {
            // Team Logo
            if let team = viewModel.selectedTeam {
                TeamLogoView(team: team, size: 56)
                    .onTapGesture { showTeamSelector = true }
            }
            
            // Team Info
            VStack(alignment: .leading, spacing: 4) {
                Text(viewModel.selectedTeam?.name ?? "Select Team")
                    .font(RacingFont.header(22))
                    .foregroundColor(.white)
                
                Button(action: { showTeamSelector = true }) {
                    HStack(spacing: 4) {
                        Text(viewModel.selectedChampionship.displayName)
                            .font(RacingFont.body(14))
                            .foregroundColor(viewModel.teamColor)
                        
                        Image(systemName: "chevron.down.circle.fill")
                            .font(.caption)
                            .foregroundColor(viewModel.teamColor)
                    }
                }
            }
            
            Spacer()
            
            // Team color badge
            if let team = viewModel.selectedTeam {
                Text(team.shortName)
                    .font(.system(size: 14, weight: .black, design: .monospaced))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(viewModel.teamColor)
                    )
            }
        }
        .padding(.horizontal)
    }
    
    // MARK: - Quick Actions
    
    private var quickActions: some View {
        HStack(spacing: 12) {
            actionButton(
                icon: "brain.fill",
                title: LocalizationUtils.string("Trivia"),
                subtitle: "\(viewModel.questionService.totalAnswered) \(LocalizationUtils.string("played"))",
                color: .purple
            ) { showQuiz = true }
            
            actionButton(
                icon: "newspaper.fill",
                title: LocalizationUtils.string("News"),
                subtitle: "\(viewModel.newsCount) \(LocalizationUtils.string("articles"))",
                color: .blue
            ) { showNews = true }
            
            actionButton(
                icon: "rectangle.stack.fill",
                title: LocalizationUtils.string("Cards"),
                subtitle: viewModel.rewardService.collectionSummary,
                color: .orange
            ) { showCollection = true }
        }
        .padding(.horizontal)
        .disabled(energyService.isSurvivalMode)
        .opacity(energyService.isSurvivalMode ? 0.5 : 1.0)
    }
    
    private func actionButton(icon: String, title: String, subtitle: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                
                Text(title)
                    .font(RacingFont.body(13).bold())
                    .foregroundColor(.white)
                
                Text(subtitle)
                    .font(.system(size: 10))
                    .foregroundColor(RacingColors.silver)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(RacingColors.cardBackground)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(color.opacity(0.3), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
    
    // MARK: - News Preview
    
    private var newsPreview: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(LocalizationUtils.string("Latest News"), systemImage: "newspaper")
                    .font(RacingFont.subheader(16))
                    .foregroundColor(viewModel.teamColor)
                Spacer()
                Button(action: { showNews = true }) {
                    HStack(spacing: 4) {
                        Text(LocalizationUtils.string("See all"))
                            .font(.caption.bold())
                        Image(systemName: "chevron.right")
                            .font(.caption2)
                    }
                    .foregroundColor(viewModel.teamColor)
                }
            }
            
            let latestNews = Array(viewModel.newsService.articles(for: viewModel.selectedChampionship).prefix(3))
            
            if latestNews.isEmpty {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Image(systemName: "newspaper")
                            .foregroundColor(RacingColors.silver.opacity(0.4))
                        Text(LocalizationUtils.string("Loading news..."))
                            .font(.caption)
                            .foregroundColor(RacingColors.silver)
                    }
                    .padding(.vertical, 20)
                    Spacer()
                }
                .background(RoundedRectangle(cornerRadius: 12).fill(RacingColors.cardBackground))
            } else {
                ForEach(latestNews) { article in
                    newsRow(article)
                }
            }
        }
        .padding(.horizontal)
    }
    
    private func newsRow(_ article: FeedArticle) -> some View {
        HStack(spacing: 12) {
            // Thumbnail or placeholder
            if let imageUrl = article.imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(viewModel.teamColor.opacity(0.1))
                            .overlay(Image(systemName: "photo").foregroundColor(viewModel.teamColor.opacity(0.3)))
                    }
                }
                .frame(width: 50, height: 50)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(viewModel.teamColor.opacity(0.1))
                    .frame(width: 50, height: 50)
                    .overlay(Image(systemName: "newspaper.fill").foregroundColor(viewModel.teamColor.opacity(0.3)))
            }
            
            VStack(alignment: .leading, spacing: 3) {
                Text(article.title)
                    .font(RacingFont.body(13).bold())
                    .foregroundColor(.white)
                    .lineLimit(2)
                
                HStack(spacing: 6) {
                    Text(article.source)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(viewModel.teamColor)
                    Text(article.publishedAt.relativeFormatted)
                        .font(.system(size: 10))
                        .foregroundColor(RacingColors.silver.opacity(0.6))
                }
            }
            
            Spacer(minLength: 0)
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12).fill(RacingColors.cardBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(viewModel.teamColor.opacity(0.15), lineWidth: 1)
        )
    }
    
    // MARK: - Quick Trivia Widget
    
    private var triviaWidget: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(LocalizationUtils.string("Quick Trivia"), systemImage: "brain")
                    .font(RacingFont.subheader(16))
                    .foregroundColor(.purple)
                Spacer()
                if showQuickResult {
                    Button(action: { loadNewTrivia() }) {
                        Image(systemName: "arrow.clockwise.circle.fill")
                            .foregroundColor(.purple)
                    }
                }
            }
            
            if let trivia = quickTrivia {
                Text(trivia.prompt)
                    .font(RacingFont.body(14))
                    .foregroundColor(.white)
                
                // 2x2 Grid
                VStack(spacing: 8) {
                    ForEach(0..<2, id: \.self) { row in
                        HStack(spacing: 8) {
                            ForEach(0..<2, id: \.self) { col in
                                let index = row * 2 + col
                                if index < trivia.options.count {
                                    Button(action: {
                                        guard !showQuickResult else { return }
                                        withAnimation(.easeInOut(duration: 0.3)) {
                                            quickAnswer = index
                                            showQuickResult = true
                                            
                                            let correct = index == trivia.correctAnswer
                                            viewModel.questionService.recordAnswer(questionId: trivia.id, wasCorrect: correct)
                                            viewModel.questionService.updateStreak(correct: correct)
                                            
                                            if correct {
                                                Task { await viewModel.rewardService.recordEvent(.quizCorrect) }
                                            }
                                        }
                                    }) {
                                        Text(trivia.options[index])
                                            .font(RacingFont.body(12))
                                            .foregroundColor(.white)
                                            .padding(.vertical, 10)
                                            .frame(maxWidth: .infinity)
                                            .background(
                                                quickAnswerBg(index: index, correctIndex: trivia.correctAnswer)
                                            )
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(quickAnswerBorder(index: index, correctIndex: trivia.correctAnswer), lineWidth: 1.5)
                                            )
                                    }
                                    .disabled(showQuickResult)
                                }
                            }
                        }
                    }
                }
                
                // Feedback
                if showQuickResult {
                    HStack(spacing: 6) {
                        Image(systemName: quickAnswer == trivia.correctAnswer ? "checkmark.circle.fill" : "xmark.circle.fill")
                        Text(quickAnswer == trivia.correctAnswer
                             ? LocalizationUtils.string("Correct!")
                             : "\(LocalizationUtils.string("Incorrect")): \(trivia.options[trivia.correctAnswer])")
                            .font(RacingFont.body(13))
                    }
                    .foregroundColor(quickAnswer == trivia.correctAnswer ? .green : .orange)
                    
                    if !trivia.explanation.isEmpty {
                        Text(trivia.explanation)
                            .font(.caption)
                            .foregroundColor(RacingColors.silver.opacity(0.8))
                    }
                }
            } else {
                ProgressView()
                    .tint(.purple)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
            }
        }
        .padding()
        .background(RoundedRectangle(cornerRadius: 16).fill(RacingColors.cardBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.purple.opacity(0.3), lineWidth: 1)
        )
        .padding(.horizontal)
        .onAppear { loadNewTrivia() }
    }
    
    private func loadNewTrivia() {
        quickTrivia = viewModel.quickTrivia()
        quickAnswer = nil
        showQuickResult = false
    }
    
    private func quickAnswerBg(index: Int, correctIndex: Int) -> Color {
        guard showQuickResult else { return Color.purple.opacity(0.15) }
        if index == correctIndex { return Color.green.opacity(0.25) }
        if index == quickAnswer { return Color.red.opacity(0.25) }
        return Color.purple.opacity(0.08)
    }
    
    private func quickAnswerBorder(index: Int, correctIndex: Int) -> Color {
        guard showQuickResult else { return Color.purple.opacity(0.4) }
        if index == correctIndex { return .green }
        if index == quickAnswer { return .red }
        return Color.purple.opacity(0.2)
    }
    
    // MARK: - Collection Preview
    
    private var collectionPreview: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(LocalizationUtils.string("My Collection"), systemImage: "rectangle.stack")
                    .font(RacingFont.subheader(16))
                    .foregroundColor(.orange)
                Spacer()
                Button(action: { showCollection = true }) {
                    HStack(spacing: 4) {
                        Text(viewModel.rewardService.collectionSummary)
                            .font(.caption.bold())
                        Image(systemName: "chevron.right")
                            .font(.caption2)
                    }
                    .foregroundColor(.orange)
                }
            }
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    // Show first 5 cards (mix of unlocked + locked)
                    let previewCards = Array(viewModel.rewardService.cardDefinitions.prefix(5))
                    ForEach(previewCards) { card in
                        let isUnlocked = viewModel.rewardService.progress[card.id]?.isUnlocked ?? false
                        let cardProgress = viewModel.rewardService.progressRatio(for: card.id)
                        
                        CardView(
                            card: card,
                            team: viewModel.selectedTeam,
                            isUnlocked: isUnlocked,
                            progress: cardProgress
                        )
                        .scaleEffect(0.7)
                        .frame(width: 130, height: 185)
                    }
                }
            }
        }
        .padding(.horizontal)
    }
    
    // MARK: - Unlock Banner
    
    private func unlockBanner(for card: RewardCardDefinition) -> some View {
        HStack(spacing: 12) {
            Image(systemName: card.badgeIcon ?? "star.fill")
                .font(.title2)
                .foregroundColor(card.rarity.color)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(LocalizationUtils.string("Card Unlocked!"))
                    .font(RacingFont.subheader(14))
                    .foregroundColor(.white)
                Text(card.title)
                    .font(RacingFont.body(13))
                    .foregroundColor(card.rarity.color)
            }
            
            Spacer()
            
            Button(action: {
                viewModel.rewardService.dismissUnlockNotification()
            }) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(RacingColors.silver)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(card.rarity.color.opacity(0.15))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(card.rarity.color.opacity(0.5), lineWidth: 1.5)
                )
        )
        .padding(.horizontal)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}
