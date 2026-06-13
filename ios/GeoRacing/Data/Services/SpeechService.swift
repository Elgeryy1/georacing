import AVFoundation

class SpeechService {
    static let shared = SpeechService()
    private let synthesizer = AVSpeechSynthesizer()
    
    // Voices for supported languages
    private var voice: AVSpeechSynthesisVoice? {
        // Prefer Spanish
        return AVSpeechSynthesisVoice(language: "es-ES") ?? AVSpeechSynthesisVoice(language: "en-US")
    }
    
    func speak(_ text: String) {
        // Stop current speech to say new instruction immediately
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = voice
        utterance.rate = 0.5 // Normal speaking rate
        utterance.pitchMultiplier = 1.0
        
        synthesizer.speak(utterance)
    }
    
    func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }
}
