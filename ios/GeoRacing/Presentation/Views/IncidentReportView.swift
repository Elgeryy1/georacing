import SwiftUI

struct IncidentReportView: View {
    @StateObject private var viewModel = IncidentViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
                
                VStack(spacing: 24) {
                    
                    // Header
                    Text(LocalizationUtils.string("Report Incident"))
                        .font(RacingFont.header(24))
                        .foregroundColor(.white)
                        .padding(.top)
                    
                    // Category Selection
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(IncidentCategory.allCases, id: \.self) { category in
                                CategoryChip(
                                    title: category.rawValue.capitalized,
                                    isSelected: viewModel.selectedCategory == category
                                ) {
                                    viewModel.selectedCategory = category
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    // Description Input
                    VStack(alignment: .leading) {
                        Text(LocalizationUtils.string("Description"))
                            .font(RacingFont.subheader())
                            .foregroundColor(RacingColors.silver)
                        
                        TextEditor(text: $viewModel.description)
                            .frame(height: 120)
                            .padding(8)
                            .background(RacingColors.cardBackground)
                            .cornerRadius(8)
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal)
                    
                    if let error = viewModel.submissionError {
                        Text(error)
                            .foregroundColor(RacingColors.red)
                            .font(RacingFont.body())
                    }
                    
                    if viewModel.submissionSuccess {
                        Text(LocalizationUtils.string("Report submitted successfully!"))
                            .foregroundColor(.green)
                            .font(RacingFont.header(18))
                            .transition(.opacity)
                            .onAppear {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                                    presentationMode.wrappedValue.dismiss()
                                }
                            }
                    }
                    
                    Spacer()
                    
                    // Submit Button
                    Button(action: viewModel.submit) {
                        HStack {
                            if viewModel.isSubmitting {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            } else {
                                Text(LocalizationUtils.string("Submit Report"))
                                    .fontWeight(.bold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [RacingColors.red, Color.red.opacity(0.8)]),
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(viewModel.isSubmitting)
                    .padding()
                }
            }
            .navigationBarHidden(true)
            .navigationBarItems(trailing: Button(LocalizationUtils.string("Close")) {
                presentationMode.wrappedValue.dismiss()
            })
        }
    }
}

struct CategoryChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(RacingFont.body())
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .background(isSelected ? RacingColors.red : RacingColors.cardBackground)
                .foregroundColor(.white)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? Color.clear : RacingColors.silver.opacity(0.3), lineWidth: 1)
                )
        }
    }
}
