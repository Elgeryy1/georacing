import SwiftUI

struct FeaturesOverviewView: View {
    @State private var searchText = ""
    @State private var selectedCategory: FeatureCategory? = nil
    
    var filteredFeatures: [FeatureCategory: [Feature]] {
        var result = [FeatureCategory: [Feature]]()
        
        let all = FeatureRegistry.shared.visibleFeatures.filter { feature in
            let matchesSearch = searchText.isEmpty || 
                feature.title.localizedCaseInsensitiveContains(searchText) ||
                feature.subtitle.localizedCaseInsensitiveContains(searchText)
            
            let matchesCategory = selectedCategory == nil || feature.category == selectedCategory
            
            return matchesSearch && matchesCategory
        }
        
        // Group by category but maintain order
        for category in FeatureCategory.allCases {
            let inCat = all.filter { $0.category == category }
            if !inCat.isEmpty {
                result[category] = inCat
            }
        }
        
        return result
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                VStack(spacing: 0) {
                    // Search Bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField(LocalizationUtils.string("Search function..."), text: $searchText)
                            .foregroundColor(.white)
                    }
                    .padding()
                    .background(RacingColors.cardBackground)
                    .cornerRadius(8)
                    .padding()
                    
                    // Filter Chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            CategoryFilterChip(title: "Todas", isSelected: selectedCategory == nil, color: Color.gray) {
                                selectedCategory = nil
                            }
                            
                            ForEach(FeatureCategory.allCases) { cat in
                                CategoryFilterChip(title: cat.rawValue, isSelected: selectedCategory == cat, color: cat.color) {
                                    selectedCategory = cat
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    .padding(.bottom)
                    
                    // List
                    ScrollView {
                        VStack(spacing: 24) {
                            ForEach(FeatureCategory.allCases) { category in
                                if let features = filteredFeatures[category], !features.isEmpty {
                                    Section(header: CategoryHeader(category: category)) {
                                        ForEach(features) { feature in
                                            NavigationLink(destination: FeatureViewFactory.view(for: feature)) {
                                                FeatureListRow(feature: feature)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Funciones GeoRacing")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Subviews

struct CategoryHeader: View {
    let category: FeatureCategory
    
    var body: some View {
        HStack {
            Image(systemName: category.icon)
                .foregroundColor(category.color)
            Text(category.rawValue.uppercased())
                .font(.caption.bold())
                .foregroundColor(category.color)
            Spacer()
        }
        .padding(.top, 8)
    }
}

struct FeatureListRow: View {
    let feature: Feature
    
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(feature.category.color.opacity(0.1))
                    .frame(width: 44, height: 44)
                
                Image(systemName: feature.icon)
                    .foregroundColor(feature.category.color)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(feature.title)
                    .font(RacingFont.body().bold())
                    .foregroundColor(.white)
                Text(feature.subtitle)
                    .font(.caption)
                    .foregroundColor(.gray)
                    .lineLimit(1)
            }
            
            Spacer()
            
            if feature.status != .complete {
                Badge(text: feature.status == .placeholder ? "WIP" : "MVP", 
                      color: feature.status == .placeholder ? .gray : .orange)
            } else {
                 Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.caption)
            }
            
            Image(systemName: "chevron.right")
                .foregroundColor(.gray)
                .font(.caption)
        }
        .padding()
        .background(RacingColors.cardBackground)
        .cornerRadius(12)
    }
}

struct CategoryFilterChip: View {
    let title: String
    let isSelected: Bool
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption.bold())
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(isSelected ? color : Color.gray.opacity(0.2))
                .foregroundColor(isSelected ? .white : .gray)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? color : Color.clear, lineWidth: 1)
                )
        }
    }
}
