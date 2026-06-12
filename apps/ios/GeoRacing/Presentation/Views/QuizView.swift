import SwiftUI

// MARK: - Quiz View

/// Full quiz session screen with progress, scoring, and card reward triggers.
struct QuizView: View {
    @ObservedObject var viewModel: FanZoneViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var questions: [QuizQuestion] = []
    @State private var currentIndex = 0
    @State private var selectedAnswer: Int?
    @State private var showResult = false
    @State private var correctCount = 0
    @State private var streak = 0
    @State private var isFinished = false
    @State private var animateCorrect = false
    @State private var animateWrong = false
    
    private let sessionSize = 10
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.ignoresSafeArea()
                
                if isFinished {
                    resultsSummary
                } else if questions.isEmpty {
                    loadingView
                } else {
                    questionView
                }
            }
            .navigationTitle(LocalizationUtils.string("Trivia"))
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
        }
        .task {
            questions = viewModel.questionService.quizSession(
                count: sessionSize,
                championship: viewModel.selectedChampionship,
                teamId: viewModel.selectedTeam?.id
            )
        }
    }
    
    // MARK: - Question View
    
    private var questionView: some View {
        let question = questions[currentIndex]
        
        return ScrollView {
            VStack(spacing: 24) {
                // Progress Bar
                progressBar
                
                // Stats Row
                HStack {
                    Label("\(correctCount)", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Spacer()
                    Label(
                        "\(LocalizationUtils.string("Streak")): \(streak)",
                        systemImage: "flame.fill"
                    )
                    .foregroundColor(streak >= 3 ? .orange : RacingColors.silver)
                    Spacer()
                    Text("\(currentIndex + 1)/\(questions.count)")
                        .foregroundColor(RacingColors.silver)
                }
                .font(RacingFont.body(14))
                .padding(.horizontal)
                
                // Difficulty
                HStack(spacing: 4) {
                    ForEach(1...5, id: \.self) { level in
                        Image(systemName: level <= question.difficulty ? "star.fill" : "star")
                            .font(.caption2)
                            .foregroundColor(level <= question.difficulty ? .yellow : .gray)
                    }
                    Spacer()
                    if let tag = question.tags.first {
                        Text(tag.capitalized)
                            .font(.caption2.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Capsule().fill(viewModel.teamColor.opacity(0.2)))
                            .foregroundColor(viewModel.teamColor)
                    }
                }
                .padding(.horizontal)
                
                // Question
                Text(question.prompt)
                    .font(RacingFont.subheader(20))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)
                    .padding(.horizontal)
                    .fixedSize(horizontal: false, vertical: true)
                
                // Options
                VStack(spacing: 12) {
                    ForEach(Array(question.options.enumerated()), id: \.offset) { index, option in
                        optionButton(index: index, text: option, question: question)
                    }
                }
                .padding(.horizontal)
                
                // Explanation
                if showResult {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: selectedAnswer == question.correctAnswer
                                  ? "checkmark.circle.fill" : "info.circle.fill")
                            Text(selectedAnswer == question.correctAnswer
                                 ? LocalizationUtils.string("Correct!")
                                 : LocalizationUtils.string("Incorrect"))
                                .font(RacingFont.subheader(16))
                        }
                        .foregroundColor(selectedAnswer == question.correctAnswer ? .green : .orange)
                        
                        Text(question.explanation)
                            .font(RacingFont.body(14))
                            .foregroundColor(RacingColors.silver)
                    }
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(RacingColors.cardBackground)
                    )
                    .padding(.horizontal)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    
                    // Next Button
                    Button(action: advanceQuestion) {
                        Text(currentIndex < questions.count - 1
                             ? LocalizationUtils.string("Next")
                             : LocalizationUtils.string("See Results"))
                            .font(RacingFont.subheader(16))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(viewModel.teamColor)
                            )
                    }
                    .padding(.horizontal)
                }
                
                Spacer(minLength: 40)
            }
            .padding(.top)
        }
    }
    
    // MARK: - Option Button
    
    private func optionButton(index: Int, text: String, question: QuizQuestion) -> some View {
        Button(action: {
            guard !showResult else { return }
            withAnimation(.easeInOut(duration: 0.3)) {
                selectedAnswer = index
                showResult = true
                
                let isCorrect = index == question.correctAnswer
                if isCorrect {
                    correctCount += 1
                    streak += 1
                    animateCorrect = true
                } else {
                    streak = 0
                    animateWrong = true
                }
                
                // Record in service
                viewModel.questionService.recordAnswer(questionId: question.id, wasCorrect: isCorrect)
                viewModel.questionService.updateStreak(correct: isCorrect)
                
                // Trigger reward events
                if isCorrect {
                    Task {
                        await viewModel.rewardService.recordEvent(.quizCorrect)
                        let currentStreak = viewModel.questionService.currentStreak
                        if currentStreak >= 5 {
                            await viewModel.rewardService.recordEvent(.quizStreak(currentStreak))
                        }
                    }
                }
            }
        }) {
            HStack {
                Text(optionLetter(index))
                    .font(.system(size: 14, weight: .bold, design: .monospaced))
                    .foregroundColor(optionLetterColor(index: index, question: question))
                    .frame(width: 28, height: 28)
                    .background(
                        Circle()
                            .fill(optionLetterBgColor(index: index, question: question))
                    )
                
                Text(text)
                    .font(RacingFont.body(15))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)
                
                Spacer()
                
                if showResult && index == question.correctAnswer {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                } else if showResult && index == selectedAnswer {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.red)
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(optionBackground(index: index, question: question))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(optionBorder(index: index, question: question), lineWidth: 1.5)
            )
        }
        .disabled(showResult)
    }
    
    // MARK: - Styling Helpers
    
    private func optionLetter(_ index: Int) -> String {
        ["A", "B", "C", "D", "E", "F"][safe: index] ?? "\(index)"
    }
    
    private func optionLetterColor(index: Int, question: QuizQuestion) -> Color {
        if showResult && index == question.correctAnswer { return .white }
        if showResult && index == selectedAnswer { return .white }
        return viewModel.teamColor
    }
    
    private func optionLetterBgColor(index: Int, question: QuizQuestion) -> Color {
        if showResult && index == question.correctAnswer { return .green }
        if showResult && index == selectedAnswer { return .red }
        return viewModel.teamColor.opacity(0.2)
    }
    
    private func optionBackground(index: Int, question: QuizQuestion) -> Color {
        if showResult && index == question.correctAnswer { return Color.green.opacity(0.15) }
        if showResult && index == selectedAnswer { return Color.red.opacity(0.15) }
        return RacingColors.cardBackground
    }
    
    private func optionBorder(index: Int, question: QuizQuestion) -> Color {
        if showResult && index == question.correctAnswer { return .green }
        if showResult && index == selectedAnswer { return .red }
        return viewModel.teamColor.opacity(0.3)
    }
    
    // MARK: - Progress Bar
    
    private var progressBar: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.gray.opacity(0.3))
                    .frame(height: 6)
                
                RoundedRectangle(cornerRadius: 4)
                    .fill(viewModel.teamColor)
                    .frame(width: geo.size.width * CGFloat(currentIndex + 1) / CGFloat(questions.count), height: 6)
                    .animation(.easeInOut, value: currentIndex)
            }
        }
        .frame(height: 6)
        .padding(.horizontal)
    }
    
    // MARK: - Advance
    
    private func advanceQuestion() {
        if currentIndex < questions.count - 1 {
            withAnimation {
                currentIndex += 1
                selectedAnswer = nil
                showResult = false
            }
        } else {
            withAnimation {
                isFinished = true
                
                // Check perfect quiz
                if correctCount == questions.count {
                    Task {
                        await viewModel.rewardService.recordEvent(.quizPerfect)
                    }
                }
                
                // First quiz
                if viewModel.questionService.totalAnswered <= questions.count {
                    Task {
                        await viewModel.rewardService.recordEvent(.firstQuiz)
                    }
                }
            }
        }
    }
    
    // MARK: - Results Summary
    
    private var resultsSummary: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Score Circle
                ZStack {
                    Circle()
                        .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                        .frame(width: 150, height: 150)
                    
                    Circle()
                        .trim(from: 0, to: CGFloat(correctCount) / CGFloat(questions.count))
                        .stroke(scoreColor, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                        .frame(width: 150, height: 150)
                        .rotationEffect(.degrees(-90))
                    
                    VStack {
                        Text("\(correctCount)/\(questions.count)")
                            .font(RacingFont.header(32))
                            .foregroundColor(.white)
                        Text(scoreLabel)
                            .font(RacingFont.body(14))
                            .foregroundColor(scoreColor)
                    }
                }
                .padding(.top, 32)
                
                // Stats
                HStack(spacing: 32) {
                    statItem(
                        icon: "percent",
                        value: "\(Int(Double(correctCount) / Double(max(1, questions.count)) * 100))%",
                        label: LocalizationUtils.string("Accuracy")
                    )
                    statItem(
                        icon: "flame.fill",
                        value: "\(viewModel.questionService.bestStreak)",
                        label: LocalizationUtils.string("Best Streak")
                    )
                    statItem(
                        icon: "number",
                        value: "\(viewModel.questionService.totalAnswered)",
                        label: LocalizationUtils.string("Total")
                    )
                }
                .padding()
                .background(RoundedRectangle(cornerRadius: 16).fill(RacingColors.cardBackground))
                .padding(.horizontal)
                
                // Actions
                VStack(spacing: 12) {
                    Button(action: {
                        // New session
                        questions = viewModel.questionService.quizSession(
                            count: sessionSize,
                            championship: viewModel.selectedChampionship,
                            teamId: viewModel.selectedTeam?.id
                        )
                        currentIndex = 0
                        selectedAnswer = nil
                        showResult = false
                        correctCount = 0
                        streak = 0
                        isFinished = false
                    }) {
                        Label(LocalizationUtils.string("Play Again"), systemImage: "arrow.counterclockwise")
                            .font(RacingFont.subheader(16))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(RoundedRectangle(cornerRadius: 12).fill(viewModel.teamColor))
                    }
                    
                    Button(action: { dismiss() }) {
                        Text(LocalizationUtils.string("Back to Fan Zone"))
                            .font(RacingFont.body(16))
                            .foregroundColor(viewModel.teamColor)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(viewModel.teamColor, lineWidth: 1.5)
                            )
                    }
                }
                .padding(.horizontal)
                
                Spacer(minLength: 40)
            }
        }
    }
    
    private var scoreColor: Color {
        let ratio = Double(correctCount) / Double(max(1, questions.count))
        if ratio >= 0.8 { return .green }
        if ratio >= 0.5 { return .yellow }
        return .red
    }
    
    private var scoreLabel: String {
        let ratio = Double(correctCount) / Double(max(1, questions.count))
        if ratio == 1.0 { return LocalizationUtils.string("Perfect!") }
        if ratio >= 0.8 { return LocalizationUtils.string("Excellent!") }
        if ratio >= 0.5 { return LocalizationUtils.string("Good job!") }
        return LocalizationUtils.string("Keep trying!")
    }
    
    private func statItem(icon: String, value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(viewModel.teamColor)
            Text(value)
                .font(RacingFont.subheader(18))
                .foregroundColor(.white)
            Text(label)
                .font(.caption)
                .foregroundColor(RacingColors.silver)
        }
    }
    
    // MARK: - Loading
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(viewModel.teamColor)
            Text(LocalizationUtils.string("Loading questions..."))
                .foregroundColor(RacingColors.silver)
        }
    }
}

// MARK: - Safe Array Access

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
