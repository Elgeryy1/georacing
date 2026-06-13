import SwiftUI

// MARK: - Fan News View

/// Aggregated news screen with F1/MotoGP tabs, pull-to-refresh, and offline cache.
struct FanNewsView: View {
    @ObservedObject var viewModel: FanZoneViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedTab: Championship = .f1
    @State private var selectedArticle: FeedArticle?
    @State private var showWebView = false
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Championship Tabs
                    championshipTabs
                    
                    // Last updated indicator
                    lastUpdatedBar
                    
                    // Articles List
                    if viewModel.newsService.isLoading && filteredArticles.isEmpty {
                        loadingView
                    } else if filteredArticles.isEmpty {
                        emptyView
                    } else {
                        articlesList
                    }
                }
            }
            .navigationTitle(LocalizationUtils.string("News"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(RacingColors.silver)
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(action: {
                        Task { await viewModel.newsService.forceRefresh() }
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .foregroundColor(viewModel.teamColor)
                    }
                }
            }
            .toolbarColorScheme(.dark, for: .navigationBar)
            .sheet(isPresented: $showWebView) {
                if let article = selectedArticle, let url = URL(string: article.url) {
                    SafariWebView(url: url)
                }
            }
        }
        .task {
            selectedTab = viewModel.selectedChampionship
            await viewModel.newsService.refreshNews()
        }
    }
    
    // MARK: - Tabs
    
    private var championshipTabs: some View {
        HStack(spacing: 0) {
            ForEach(Championship.allCases) { champ in
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) { selectedTab = champ }
                }) {
                    VStack(spacing: 4) {
                        HStack(spacing: 6) {
                            Image(systemName: champ.icon)
                            Text(champ.rawValue)
                                .font(RacingFont.subheader(15))
                        }
                        
                        // Count badge
                        Text("\(viewModel.newsService.articles(for: champ).count)")
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(selectedTab == champ ? viewModel.teamColor : Color.gray.opacity(0.3)))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        selectedTab == champ
                        ? viewModel.teamColor.opacity(0.15)
                        : Color.clear
                    )
                    .foregroundColor(selectedTab == champ ? viewModel.teamColor : RacingColors.silver)
                }
            }
        }
        .background(RacingColors.cardBackground)
    }
    
    // MARK: - Last Updated
    
    private var lastUpdatedBar: some View {
        HStack {
            Image(systemName: "clock")
                .font(.caption2)
            Text("\(LocalizationUtils.string("Updated")) \(viewModel.newsService.lastRefreshedText)")
                .font(.caption)
            
            Spacer()
            
            if viewModel.newsService.isLoading {
                ProgressView()
                    .scaleEffect(0.7)
                    .tint(viewModel.teamColor)
            }
        }
        .foregroundColor(RacingColors.silver.opacity(0.7))
        .padding(.horizontal)
        .padding(.vertical, 6)
    }
    
    // MARK: - Articles List
    
    private var filteredArticles: [FeedArticle] {
        viewModel.newsService.articles(for: selectedTab)
    }
    
    private var articlesList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(filteredArticles) { article in
                    NewsArticleRow(
                        article: article,
                        teamColor: viewModel.teamColor,
                        onTap: {
                            viewModel.newsService.markAsRead(article.id)
                            selectedArticle = article
                            showWebView = true
                        }
                    )
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .refreshable {
            await viewModel.newsService.forceRefresh()
        }
    }
    
    // MARK: - Empty / Loading
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            Spacer()
            ProgressView()
                .tint(viewModel.teamColor)
            Text(LocalizationUtils.string("Loading news..."))
                .foregroundColor(RacingColors.silver)
            Spacer()
        }
    }
    
    private var emptyView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "newspaper")
                .font(.system(size: 48))
                .foregroundColor(RacingColors.silver.opacity(0.5))
            Text(LocalizationUtils.string("No news available"))
                .font(RacingFont.subheader())
                .foregroundColor(RacingColors.silver)
            Button(action: {
                Task { await viewModel.newsService.forceRefresh() }
            }) {
                Label(LocalizationUtils.string("Refresh"), systemImage: "arrow.clockwise")
                    .racingButton(color: viewModel.teamColor)
            }
            Spacer()
        }
    }
}

// MARK: - News Article Row

struct NewsArticleRow: View {
    let article: FeedArticle
    let teamColor: Color
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                // Thumbnail
                if let imageUrl = article.imageUrl, let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        case .failure:
                            imagePlaceholder
                        default:
                            ProgressView()
                                .frame(width: 80, height: 80)
                        }
                    }
                    .frame(width: 80, height: 80)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                } else {
                    imagePlaceholder
                }
                
                // Content
                VStack(alignment: .leading, spacing: 6) {
                    Text(article.title)
                        .font(RacingFont.body(15).bold())
                        .foregroundColor(.white)
                        .lineLimit(2)
                    
                    if !article.summary.isEmpty {
                        Text(article.summary)
                            .font(RacingFont.body(13))
                            .foregroundColor(RacingColors.silver)
                            .lineLimit(2)
                    }
                    
                    HStack(spacing: 8) {
                        Text(article.source)
                            .font(.caption.bold())
                            .foregroundColor(teamColor)
                        
                        Text(article.publishedAt.relativeFormatted)
                            .font(.caption)
                            .foregroundColor(RacingColors.silver.opacity(0.7))
                    }
                }
                
                Spacer(minLength: 0)
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(RacingColors.silver.opacity(0.5))
                    .padding(.top, 8)
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(RacingColors.cardBackground)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(teamColor.opacity(0.15), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
    
    private var imagePlaceholder: some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(teamColor.opacity(0.1))
            .frame(width: 80, height: 80)
            .overlay(
                Image(systemName: "newspaper.fill")
                    .foregroundColor(teamColor.opacity(0.3))
            )
    }
}

// MARK: - Safari Web View

import SafariServices

struct SafariWebView: UIViewControllerRepresentable {
    let url: URL
    
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let config = SFSafariViewController.Configuration()
        config.entersReaderIfAvailable = true
        let vc = SFSafariViewController(url: url, configuration: config)
        return vc
    }
    
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

// MARK: - Date Extension

extension Date {
    var relativeFormatted: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: self, relativeTo: Date())
    }
}
