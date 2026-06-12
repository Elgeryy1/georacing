import SwiftUI
import Photos

struct EmergencySetupView: View {
    @State private var bloodType: String = ""
    @State private var emergencyContact: String = ""
    @State private var gateInfo: String = "Tribuna N, Puerta 3" // Example context
    @State private var healthConditions: String = ""
    
    @State private var isSaved: Bool = false
    @State private var errorMessage: String?
    
    var body: some View {
        NavigationView {
            ZStack {
                RacingColors.darkBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        
                        Text("Configura tu Fondo de Bloqueo")
                            .font(RacingFont.header(24))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                            .padding(.top)
                        
                        Text("En caso de desmayo o emergencia y sin conexión a internet, el personal médico podrá ver esta información en tu pantalla.")
                            .font(RacingFont.body(14))
                            .foregroundColor(RacingColors.silver)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        // Form Input
                        VStack(spacing: 16) {
                            inputField(title: "Grupo Sanguíneo", text: $bloodType, placeholder: "Ej. O+, A-")
                            inputField(title: "Contacto Emergencia", text: $emergencyContact, placeholder: "Nombre y Teléfono")
                            inputField(title: "Condiciones Médicas", text: $healthConditions, placeholder: "Ej. Diabético, Alergia Penicilina")
                            inputField(title: "Alojamiento / Puerta", text: $gateInfo, placeholder: "Dónde estás ubicado")
                        }
                        .padding()
                        .background(RacingColors.cardBackground)
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        // Preview
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Previsualización de Emergencia:")
                                .font(RacingFont.subheader(16))
                                .foregroundColor(RacingColors.red)
                                .padding(.horizontal)
                            
                            lockScreenPreview
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 20)
                                        .stroke(RacingColors.silver.opacity(0.3), lineWidth: 2)
                                )
                                .padding(.horizontal, 40)
                        }
                        
                        // Action Button
                        Button(action: saveWallpaper) {
                            HStack {
                                Image(systemName: isSaved ? "checkmark.circle.fill" : "square.and.arrow.down")
                                Text(isSaved ? "Guardado en Fotos" : "Guardar Fondo")
                            }
                            .font(RacingFont.header(18))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(isSaved ? Color.green : RacingColors.red)
                            .cornerRadius(16)
                            .shadow(radius: 5)
                        }
                        .disabled(isSaved)
                        .padding(.horizontal)
                        
                        if let error = errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                                .padding(.horizontal)
                        }
                        
                        Text("Una vez guardada, ve a Ajustes > Fondo de Pantalla y configúrala como Pantalla de Bloqueo.")
                            .font(.caption)
                            .foregroundColor(RacingColors.silver.opacity(0.7))
                            .multilineTextAlignment(.center)
                            .padding()
                    }
                    .padding(.bottom, 40)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text("Medical Lock Screen")
                        .font(RacingFont.subheader(18))
                        .foregroundColor(.white)
                }
            }
        }
    }
    
    // MARK: - Subviews
    
    private func inputField(title: String, text: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(RacingFont.body(12).bold())
                .foregroundColor(RacingColors.silver)
            
            TextField(placeholder, text: text)
                .font(RacingFont.body(16))
                .foregroundColor(.white)
                .padding(12)
                .background(Color.white.opacity(0.05))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(RacingColors.silver.opacity(0.2), lineWidth: 1)
                )
        }
    }
    
    /// The exact view that will be rendered to the image
    private var lockScreenPreview: some View {
        ZStack {
            // Simulated Lock Screen background (Black/Dark)
            Color.black.ignoresSafeArea()
            
            VStack {
                Spacer() // Push to the bottom half as the clock takes the top
                
                VStack(spacing: 16) {
                    HStack {
                        Image(systemName: "cross.case.fill")
                            .font(.title)
                            .foregroundColor(.red)
                        Text("INFO MÉDICA DE EMERGENCIA")
                            .font(.system(size: 16, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                    }
                    
                    VStack(alignment: .leading, spacing: 10) {
                        previewRow(title: "Sangre:", value: bloodType.isEmpty ? "N/A" : bloodType)
                        previewRow(title: "Alerta:", value: healthConditions.isEmpty ? "Ninguna declarada" : healthConditions)
                        previewRow(title: "Llamar:", value: emergencyContact.isEmpty ? "No especificado" : emergencyContact)
                        previewRow(title: "Ticket:", value: gateInfo)
                    }
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                }
                .padding(20)
                .background(Color.red.opacity(0.2)) // Red hue for emergency feel
                .cornerRadius(16)
                .padding([.horizontal, .bottom], 20)
            }
        }
        // Assuming a standard screen aspect ratio for preview
        .aspectRatio(9/19.5, contentMode: .fit) 
    }
    
    private func previewRow(title: String, value: String) -> some View {
        HStack(alignment: .top) {
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.gray)
                .frame(width: 60, alignment: .leading)
            
            Text(value)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white)
            Spacer()
        }
    }
    
    // MARK: - Actions
    
    private func saveWallpaper() {
        errorMessage = nil
        
        // 1. Check Permissions
        let status = PHPhotoLibrary.authorizationStatus(for: .addOnly)
        if status == .notDetermined {
            PHPhotoLibrary.requestAuthorization(for: .addOnly) { newStatus in
                if newStatus == .authorized {
                    Task { @MainActor in self.renderAndSave() }
                } else {
                    Task { @MainActor in self.errorMessage = "Se requiere permiso para guardar fotos." }
                }
            }
        } else if status == .authorized || status == .limited {
            renderAndSave()
        } else {
            errorMessage = "Permiso de fotos denegado. Ve a Ajustes."
        }
    }
    
    private func renderAndSave() {
        do {
            // Render the lockScreenPreview View
            let size = UIScreen.main.bounds.size
            let image = try EmergencyImageGenerator.render(view: lockScreenPreview, size: size)
            
            // Save to photos
            EmergencyImageGenerator.saveToPhotos(image)
            
            withAnimation {
                isSaved = true
            }
            
            // Log telemetry event!
            TelemetryLogger.shared.logEvent("saved_emergency_lockscreen")
            
        } catch {
            errorMessage = "Hubo un error al generar la imagen."
            Logger.error("[EmergencySetup] Render Error: \(error)")
        }
    }
}
