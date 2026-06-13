import SwiftUI

// MARK: - Card View

/// Visual component for a collectible reward card.
/// Renders with team gradients, rarity frame, and numbered badge.
/// Supports export to PNG for sharing.
struct CardView: View {
    let card: RewardCardDefinition
    let team: RacingTeam?
    let isUnlocked: Bool
    let progress: Double // 0.0 - 1.0
    
    var body: some View {
        ZStack {
            // Card Base
            RoundedRectangle(cornerRadius: 16)
                .fill(cardBackground)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(card.rarity.frameGradient, lineWidth: rarityBorderWidth)
                )
            
            if isUnlocked {
                unlockedContent
            } else {
                lockedContent
            }
        }
        .frame(width: 180, height: 260)
        .shadow(color: isUnlocked ? card.rarity.color.opacity(0.4) : .black.opacity(0.3), radius: 8, y: 4)
    }
    
    // MARK: - Unlocked Card
    
    private var unlockedContent: some View {
        VStack(spacing: 8) {
            // Rarity Badge
            HStack {
                Text(card.rarity.displayName.uppercased())
                    .font(.system(size: 9, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(card.rarity.color))
                
                Spacer()
                
                // Number
                Text(String(format: "%02d/%02d", card.number, card.totalInSet))
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(.white.opacity(0.7))
            }
            .padding(.horizontal, 12)
            .padding(.top, 12)
            
            Spacer()
            
            // Team Logo or Badge Icon
            if let team {
                TeamLogoView(team: team, size: 56)
            } else if let icon = card.badgeIcon {
                Image(systemName: icon)
                    .font(.system(size: 36))
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
            }
            
            // Title
            Text(card.title)
                .font(RacingFont.subheader(15))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .padding(.horizontal, 8)
            
            // Description
            Text(card.description)
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .padding(.horizontal, 8)
            
            // Season badge
            Text("2026")
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundColor(.white.opacity(0.5))
                .padding(.bottom, 10)
        }
    }
    
    // MARK: - Locked Card
    
    private var lockedContent: some View {
        VStack(spacing: 12) {
            Spacer()
            
            // Lock icon
            Image(systemName: "lock.fill")
                .font(.system(size: 32))
                .foregroundColor(.white.opacity(0.3))
            
            // Progress bar
            VStack(spacing: 4) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.white.opacity(0.1))
                        
                        RoundedRectangle(cornerRadius: 3)
                            .fill(card.rarity.color.opacity(0.6))
                            .frame(width: geo.size.width * progress)
                    }
                }
                .frame(height: 6)
                .padding(.horizontal, 24)
                
                Text("\(Int(progress * 100))%")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white.opacity(0.5))
            }
            
            // Condition hint
            Text(card.unlockCondition.description)
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.4))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .padding(.horizontal, 12)
            
            // Rarity indicator
            Text(card.rarity.displayName)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(card.rarity.color.opacity(0.5))
            
            Spacer()
        }
    }
    
    // MARK: - Styling
    
    private var cardBackground: LinearGradient {
        if isUnlocked, let team {
            return LinearGradient(
                colors: [team.primarySwiftColor.opacity(0.6), team.primarySwiftColor.opacity(0.15), Color.black.opacity(0.9)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
        return LinearGradient(
            colors: [Color(white: 0.15), Color(white: 0.08)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var rarityBorderWidth: CGFloat {
        switch card.rarity {
        case .common: return 1.5
        case .rare: return 2
        case .epic: return 2.5
        case .legendary: return 3
        }
    }
}

// MARK: - Card Share Extension

extension CardView {
    /// Render the card to a UIImage for sharing
    @MainActor
    func renderToImage() -> UIImage {
        let renderer = ImageRenderer(content: self.frame(width: 360, height: 520))
        renderer.scale = UITraitCollection.current.displayScale
        return renderer.uiImage ?? UIImage()
    }
}

// MARK: - Card Detail Sheet

struct CardDetailSheet: View {
    let card: RewardCardDefinition
    let team: RacingTeam?
    let isUnlocked: Bool
    let progress: Double
    
    @Environment(\.dismiss) private var dismiss
    @State private var showShareSheet = false
    @State private var shareImage: UIImage?
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.ignoresSafeArea()
            
            VStack(spacing: 24) {
                // Large Card
                CardView(card: card, team: team, isUnlocked: isUnlocked, progress: progress)
                    .scaleEffect(1.5)
                    .padding(.top, 60)
                    .padding(.bottom, 40)
                
                Spacer()
                
                // Card Info
                VStack(spacing: 8) {
                    Text(card.title)
                        .font(RacingFont.header(24))
                        .foregroundColor(.white)
                    
                    Text(card.description)
                        .font(RacingFont.body(15))
                        .foregroundColor(RacingColors.silver)
                        .multilineTextAlignment(.center)
                    
                    HStack(spacing: 16) {
                        Label(card.rarity.displayName, systemImage: "sparkles")
                            .foregroundColor(card.rarity.color)
                        
                        Label("\(card.number)/\(card.totalInSet)", systemImage: "number")
                            .foregroundColor(RacingColors.silver)
                    }
                    .font(RacingFont.body(14))
                    .padding(.top, 4)
                    
                    if !isUnlocked {
                        // Progress detail
                        VStack(spacing: 4) {
                            Text(card.unlockCondition.description)
                                .font(RacingFont.body(13))
                                .foregroundColor(RacingColors.silver.opacity(0.8))
                            
                            ProgressView(value: progress)
                                .tint(card.rarity.color)
                                .padding(.horizontal, 40)
                        }
                        .padding(.top, 8)
                    }
                }
                .padding(.horizontal)
                
                Spacer()
                
                // Actions
                HStack(spacing: 16) {
                    if isUnlocked {
                        Button(action: {
                            let cardView = CardView(card: card, team: team, isUnlocked: true, progress: 1.0)
                            shareImage = cardView.renderToImage()
                            showShareSheet = true
                        }) {
                            Label(LocalizationUtils.string("Share"), systemImage: "square.and.arrow.up")
                                .font(RacingFont.subheader(15))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(card.rarity.color)
                                )
                        }
                    }
                    
                    Button(action: { dismiss() }) {
                        Text(LocalizationUtils.string("Close"))
                            .font(RacingFont.body(15))
                            .foregroundColor(RacingColors.silver)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(RacingColors.silver.opacity(0.3), lineWidth: 1)
                            )
                    }
                }
                .padding(.horizontal)
                .padding(.bottom)
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let image = shareImage {
                ShareSheet(items: [image])
            }
        }
    }
}

// MARK: - Share Sheet

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
