import SwiftUI

// MARK: - Team Selector View

/// Full-screen team picker with logos, organized by championship.
struct TeamSelectorView: View {
    @ObservedObject var viewModel: FanZoneViewModel
    @Environment(\.dismiss) private var dismiss
    
    private let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16),
    ]
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Championship Picker
                        championshipPicker
                        
                        // Teams Grid
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(viewModel.availableTeams) { team in
                                TeamCard(
                                    team: team,
                                    isSelected: viewModel.selectedTeam?.id == team.id,
                                    onTap: {
                                        withAnimation(.spring(response: 0.3)) {
                                            viewModel.selectTeam(team)
                                        }
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle(LocalizationUtils.string("Choose Your Team"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(LocalizationUtils.string("Done")) {
                        dismiss()
                    }
                    .foregroundColor(viewModel.teamColor)
                }
            }
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
    
    // MARK: - Championship Picker
    
    private var championshipPicker: some View {
        HStack(spacing: 0) {
            ForEach(Championship.allCases) { champ in
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        viewModel.selectedChampionship = champ
                    }
                }) {
                    VStack(spacing: 6) {
                        Image(systemName: champ.icon)
                            .font(.title2)
                        Text(champ.displayName)
                            .font(RacingFont.subheader(14))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(viewModel.selectedChampionship == champ
                                  ? viewModel.teamColor.opacity(0.3)
                                  : Color.clear)
                    )
                    .foregroundColor(viewModel.selectedChampionship == champ
                                     ? viewModel.teamColor
                                     : RacingColors.silver)
                }
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(RacingColors.cardBackground)
        )
        .padding(.horizontal)
    }
}

// MARK: - Team Card

struct TeamCard: View {
    let team: RacingTeam
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 12) {
                // Team Logo
                TeamLogoView(team: team, size: 60)
                
                // Team Name
                Text(team.name)
                    .font(RacingFont.body(14).bold())
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.8)
                
                // Short Name Badge
                Text(team.shortName)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(team.primarySwiftColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(
                        Capsule()
                            .fill(team.primarySwiftColor.opacity(0.15))
                    )
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .padding(.horizontal, 8)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(RacingColors.cardBackground)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? team.primarySwiftColor : team.primarySwiftColor.opacity(0.2), lineWidth: isSelected ? 2.5 : 1)
            )
            .scaleEffect(isSelected ? 1.03 : 1.0)
            .shadow(color: isSelected ? team.primarySwiftColor.opacity(0.4) : .clear, radius: 8)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(team.name), \(isSelected ? "selected" : "not selected")")
    }
}
