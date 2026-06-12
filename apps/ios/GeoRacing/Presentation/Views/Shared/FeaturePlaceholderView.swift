import SwiftUI

struct FeaturePlaceholderView: View {
    let feature: Feature
    
    @State private var isSimulating = false
    @State private var demoLevel = 0
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: feature.icon)
                            .font(.system(size: 40))
                            .foregroundColor(feature.category.color)
                        Spacer()
                        Badge(text: feature.status.rawValue, color: feature.status.color)
                    }
                    
                    Text(feature.title)
                        .font(RacingFont.header(32))
                        .foregroundColor(.white)
                    
                    Text(feature.subtitle)
                        .font(RacingFont.subheader())
                        .foregroundColor(.gray)
                }
                .padding()
                .background(RacingColors.cardBackground)
                .cornerRadius(16)
                
                // What it does
                VStack(alignment: .leading, spacing: 12) {
                    Text("Funcionalidad")
                        .font(RacingFont.header(20))
                        .foregroundColor(.white)
                    
                    Text("Esta función permite \(feature.subtitle.lowercased()) interactuando con los servicios de GeoRacing.")
                        .font(RacingFont.body())
                        .foregroundColor(RacingColors.silver)
                }
                .padding(.horizontal)
                
                // Next Steps
                VStack(alignment: .leading, spacing: 12) {
                    Text("Próximos Pasos (WIP)")
                        .font(RacingFont.header(20))
                        .foregroundColor(.white)
                    
                    ForEach(feature.nextSteps, id: \.self) { step in
                        HStack(alignment: .top) {
                            Image(systemName: "circle")
                                .foregroundColor(.gray)
                            Text(step)
                                .font(RacingFont.body())
                                .foregroundColor(RacingColors.silver)
                        }
                    }
                    
                    if feature.nextSteps.isEmpty {
                        Text("• Implementación pendiente de especificación.")
                            .font(RacingFont.body())
                            .foregroundColor(.gray)
                    }
                }
                .padding(.horizontal)
                
                // Simulation Control
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text(LocalizationUtils.string("Simulation Environment"))
                            .font(RacingFont.header(20))
                            .foregroundColor(.white)
                        Spacer()
                        Image(systemName: "testtube.2")
                            .foregroundColor(.orange)
                    }
                    
                    Toggle("Simular Activo", isOn: $isSimulating)
                        .toggleStyle(SwitchToggleStyle(tint: feature.category.color))
                        .foregroundColor(.white)
                    
                    if isSimulating {
                        Picker("Nivel Demo", selection: $demoLevel) {
                            Text("Mock Básico").tag(0)
                            Text("Interacción Real").tag(1)
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        
                        Button(action: runDemo) {
                            HStack {
                                Image(systemName: "play.fill")
                                Text("Ejecutar Demo Local")
                            }
                            .bold()
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(feature.category.color.opacity(0.8))
                            .cornerRadius(8)
                            .foregroundColor(.white)
                        }
                    }
                }
                .padding()
                .background(Color.black.opacity(0.3))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                )
                .padding(.horizontal)
                
                // Tech Data
                VStack(alignment: .leading, spacing: 4) {
                    Text("ID: \(feature.id)")
                    Text("Categoria: \(feature.category.rawValue)")
                    Text("Prioridad: \(feature.priority)")
                    Text("Última actualización: Ahora")
                }
                .font(.caption)
                .foregroundColor(.gray)
                .padding()
            }
            .padding(.bottom, 40)
        }
        .background(RacingColors.darkBackground.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
    }
    
    // Actions
    func runDemo() {
        Logger.debug("[Feature] Running demo for \(feature.id) at level \(demoLevel)")
        // Haptic feedback could go here
    }
}

// Helper Badge
struct Badge: View {
    let text: String
    let color: Color
    
    var body: some View {
        Text(text)
            .font(.caption.bold())
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.2))
            .foregroundColor(color)
            .cornerRadius(4)
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .stroke(color, lineWidth: 1)
            )
    }
}
