import Foundation
import AVFoundation

@MainActor
class TTSManager: NSObject, AVSpeechSynthesizerDelegate {
    
    private let synthesizer = AVSpeechSynthesizer()
    private var lastDistance: Double = Double.infinity
    
    // Voice cues thresholds in meters
    private let thresholds: [Double] = [800, 200, 100, 50]
    
    override init() {
        super.init()
        synthesizer.delegate = self
        configureAudioSession()
    }
    
    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .voicePrompt, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            Logger.error("Failed to configure AudioSession: \(error)")
        }
    }
    
    func update(distanceToNextManeuver: Double, instruction: String) {
        // Check if we crossed a threshold downwards
        for threshold in thresholds {
            if lastDistance > threshold && distanceToNextManeuver <= threshold {
                speak(distance: distanceToNextManeuver, instruction: instruction)
                break // Announce only the highest priority crossing
            }
        }
        
        lastDistance = distanceToNextManeuver
    }
    
    private func speak(distance: Double, instruction: String) {
        let text: String
        let cleanInstruction = instruction.replacingOccurrences(of: "slight ", with: "ligeramente a la ")
                                          .replacingOccurrences(of: "turn ", with: "gira ")
                                          // Add more translations or rely on localized instruction from OSRM
        
        if distance > 50 {
            // "En [distancia], [instrucción]"
            text = "En \(Int(distance)) metros, \(cleanInstruction)"
        } else {
            // "Ahora, [instrucción]"
            text = "Ahora, \(cleanInstruction)"
        }
        
        // Don't interrupt if already speaking? Or queue? AVSpeechSynthesizer queues by default.
        // But for nav, we might want to stop previous if it's outdated? 
        // For now, let it queue or stop immediate?
        // Usually immediate updates are better.
        synthesizer.stopSpeaking(at: .immediate)
        
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "es-ES") // Spanish as per source docs language hint
        utterance.rate = 0.5
        
        synthesizer.speak(utterance)
        Logger.info("TTS Speaking: \(text)")
    }
    
    func reset() {
        lastDistance = Double.infinity
        synthesizer.stopSpeaking(at: .immediate)
    }
}
