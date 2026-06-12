import SwiftUI

struct NewsItemView: View {
    let item: NewsItem
    
    var body: some View {
        HStack(spacing: 16) {
            // Image
            // In a real app we would use AsyncImage, here we use Image with fallback or AsyncImage if URL
            if item.imageUrl.starts(with: "http") {
                AsyncImage(url: URL(string: item.imageUrl)) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "photo").foregroundColor(.gray)
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(width: 80, height: 80)
                .cornerRadius(8)
                .clipped()
            } else {
                Image(item.imageUrl) // Asset
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 80, height: 80)
                    .cornerRadius(8)
                    .clipped()
            }
            
            VStack(alignment: .leading, spacing: 4) {
                if item.isEvent {
                    Text("EVENT")
                        .font(RacingFont.body(10).bold())
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(RacingColors.red)
                        .foregroundColor(.white)
                        .cornerRadius(4)
                }
                
                Text(item.title)
                    .font(RacingFont.subheader(16))
                    .foregroundColor(.white)
                    .lineLimit(2)
                
                if let subtitle = item.subtitle {
                    Text(subtitle)
                        .font(RacingFont.body(14))
                        .foregroundColor(RacingColors.silver)
                        .lineLimit(2)
                }
            }
            Spacer()
        }
        .padding()
        .background(RacingColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(RacingColors.silver.opacity(0.1), lineWidth: 1)
        )
    }
}
