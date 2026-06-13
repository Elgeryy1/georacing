import SwiftUI

// MARK: - Design Tokens
// Centralized source of truth for the premium minimalist theme.
// Maps semantic names to existing RacingColors or system colors.

struct GeoLayout {
    static let radius: CGFloat = 14
    static let buttonHeightLarge: CGFloat = 56
    static let buttonHeightMedium: CGFloat = 48
    static let buttonHeightSmall: CGFloat = 36
    static let borderWidth: CGFloat = 1
}

struct GeoToken {
    // Colors mapped from RacingColors
    static let primary = RacingColors.red
    static let background = RacingColors.darkBackground
    static let surface = RacingColors.cardBackground
    static let textOnPrimary = RacingColors.white
    static let textPrimary = RacingColors.white
    static let textSecondary = RacingColors.silver
    
    // Derived tokens for specific UI needs (using opacity on existing colors as requested)
    static let borderSubtle = RacingColors.silver.opacity(0.2)
    static let surfaceHighlight = RacingColors.silver.opacity(0.1) // For pressed states on secondary/tertiary
}
