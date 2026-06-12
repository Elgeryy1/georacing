import SwiftUI
@preconcurrency import AVFoundation
import Combine

struct QRScannerView: View {
    @StateObject private var viewModel = QRScannerViewModel()
    @Environment(\.dismiss) private var dismiss
    
    let onCodeScanned: (String) -> Void
    
    var body: some View {
        ZStack {
            // Camera Preview
            CameraPreviewView(session: viewModel.captureSession)
                .ignoresSafeArea()
            
            // Overlay
            scannerOverlay
            
            // Header
            VStack {
                headerView
                Spacer()
            }
            
            // Status Messages
            VStack {
                Spacer()
                statusView
            }
        }
        .onAppear {
            viewModel.startScanning()
        }
        .onDisappear {
            viewModel.stopScanning()
        }
        .onChange(of: viewModel.scannedCode) { _, newValue in
            if let code = newValue {
                onCodeScanned(code)
                dismiss()
            }
        }
        .alert("Error", isPresented: .constant(viewModel.error != nil)) {
            Button("OK") {
                viewModel.error = nil
            }
        } message: {
            Text(viewModel.error ?? "")
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.5))
                    .clipShape(Circle())
            }
            
            Spacer()
            
            Text(LocalizationUtils.string("Scan QR"))
                .font(.title2.bold())
                .foregroundColor(.white)
                .shadow(radius: 4)
            
            Spacer()
            
            // Toggle flash
            Button {
                viewModel.toggleFlash()
            } label: {
                Image(systemName: viewModel.isFlashOn ? "bolt.fill" : "bolt.slash")
                    .font(.title2)
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.5))
                    .clipShape(Circle())
            }
        }
        .padding()
        .padding(.top, 40)
    }
    
    // MARK: - Scanner Overlay
    
    private var scannerOverlay: some View {
        GeometryReader { geometry in
            let size = min(geometry.size.width, geometry.size.height) * 0.7
            let _ = CGRect(
                x: (geometry.size.width - size) / 2,
                y: (geometry.size.height - size) / 2,
                width: size,
                height: size
            )
            
            ZStack {
                // Dimmed background
                Color.black.opacity(0.6)
                    .mask(
                        Rectangle()
                            .overlay(
                                RoundedRectangle(cornerRadius: 20)
                                    .frame(width: size, height: size)
                                    .blendMode(.destinationOut)
                            )
                    )
                
                // Scanner frame
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.orange, lineWidth: 3)
                    .frame(width: size, height: size)
                
                // Corner accents
                scannerCorners(size: size)
                
                // Scanning line animation
                if viewModel.isScanning {
                    scanningLine(size: size)
                }
            }
        }
    }
    
    private func scannerCorners(size: CGFloat) -> some View {
        let cornerLength: CGFloat = 40
        let lineWidth: CGFloat = 4
        
        return ZStack {
            // Top-left
            Path { path in
                path.move(to: CGPoint(x: 0, y: cornerLength))
                path.addLine(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: cornerLength, y: 0))
            }
            .stroke(Color.orange, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .offset(x: -size/2 + 10, y: -size/2 + 10)
            
            // Top-right
            Path { path in
                path.move(to: CGPoint(x: -cornerLength, y: 0))
                path.addLine(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: 0, y: cornerLength))
            }
            .stroke(Color.orange, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .offset(x: size/2 - 10, y: -size/2 + 10)
            
            // Bottom-left
            Path { path in
                path.move(to: CGPoint(x: 0, y: -cornerLength))
                path.addLine(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: cornerLength, y: 0))
            }
            .stroke(Color.orange, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .offset(x: -size/2 + 10, y: size/2 - 10)
            
            // Bottom-right
            Path { path in
                path.move(to: CGPoint(x: -cornerLength, y: 0))
                path.addLine(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: 0, y: -cornerLength))
            }
            .stroke(Color.orange, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .offset(x: size/2 - 10, y: size/2 - 10)
        }
    }
    
    @State private var scanLineOffset: CGFloat = -1
    
    private func scanningLine(size: CGFloat) -> some View {
        Rectangle()
            .fill(
                LinearGradient(
                    colors: [.clear, .orange, .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(width: size - 40, height: 2)
            .offset(y: scanLineOffset * (size / 2 - 20))
            .onAppear {
                withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: true)) {
                    scanLineOffset = 1
                }
            }
    }
    
    // MARK: - Status View
    
    private var statusView: some View {
        VStack(spacing: 16) {
            if viewModel.isScanning {
                Text(LocalizationUtils.string("Point at QR code"))
                    .font(.headline)
                    .foregroundColor(.white)
            } else if !viewModel.hasCameraPermission {
                VStack(spacing: 12) {
                    Image(systemName: "camera.fill")
                        .font(.largeTitle)
                        .foregroundColor(.orange)
                    Text(LocalizationUtils.string("Camera access needed"))
                        .font(.headline)
                        .foregroundColor(.white)
                    Button(LocalizationUtils.string("Open Settings")) {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .foregroundColor(.orange)
                }
            }
        }
        .padding()
        .padding(.bottom, 60)
    }
}

// MARK: - Camera Preview

struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession?
    
    func makeUIView(context: Context) -> VideoPreviewUIView {
        let view = VideoPreviewUIView()
        view.backgroundColor = .black
        view.videoPreviewLayer.session = session
        view.videoPreviewLayer.videoGravity = .resizeAspectFill
        return view
    }
    
    func updateUIView(_ uiView: VideoPreviewUIView, context: Context) {
        uiView.videoPreviewLayer.session = session
    }
}

class VideoPreviewUIView: UIView {
    override class var layerClass: AnyClass {
        AVCaptureVideoPreviewLayer.self
    }
    
    var videoPreviewLayer: AVCaptureVideoPreviewLayer {
        // Safe: layerClass is set to AVCaptureVideoPreviewLayer
        // swiftlint:disable:next force_cast
        layer as! AVCaptureVideoPreviewLayer
    }
}

// MARK: - QR Scanner ViewModel

@MainActor
final class QRScannerViewModel: NSObject, ObservableObject {
    
    @Published var scannedCode: String?
    @Published var isScanning = false
    @Published var isFlashOn = false
    @Published var hasCameraPermission = false
    @Published var error: String?
    
    var captureSession: AVCaptureSession?
    private let metadataOutput = AVCaptureMetadataOutput()
    
    override init() {
        super.init()
        checkPermissions()
    }
    
    private func checkPermissions() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            hasCameraPermission = true
            setupCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                Task { @MainActor in
                    self?.hasCameraPermission = granted
                    if granted {
                        self?.setupCamera()
                    }
                }
            }
        default:
            hasCameraPermission = false
        }
    }
    
    private func setupCamera() {
        let session = AVCaptureSession()
        
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else {
            error = LocalizationUtils.string("Could not access camera")
            return
        }
        
        if session.canAddInput(input) {
            session.addInput(input)
        }
        
        if session.canAddOutput(metadataOutput) {
            session.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: .main)
            metadataOutput.metadataObjectTypes = [.qr]
        }
        
        self.captureSession = session
    }
    
    func startScanning() {
        guard let session = captureSession, !session.isRunning else { return }
        let capturedSession = session
        Task.detached {
            capturedSession.startRunning()
            await MainActor.run { [weak self] in
                self?.isScanning = true
            }
        }
    }
    
    func stopScanning() {
        guard let session = captureSession, session.isRunning else { return }
        let capturedSession = session
        Task.detached {
            capturedSession.stopRunning()
        }
        isScanning = false
    }
    
    func toggleFlash() {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else { return }
        
        do {
            try device.lockForConfiguration()
            isFlashOn.toggle()
            device.torchMode = isFlashOn ? .on : .off
            device.unlockForConfiguration()
        } catch {
            self.error = LocalizationUtils.string("Could not activate flash")
        }
    }
}

// MARK: - AVCaptureMetadataOutputObjectsDelegate

extension QRScannerViewModel: AVCaptureMetadataOutputObjectsDelegate {
    nonisolated func metadataOutput(_ output: AVCaptureMetadataOutput, 
                         didOutput metadataObjects: [AVMetadataObject], 
                         from connection: AVCaptureConnection) {
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              object.type == .qr,
              let code = object.stringValue else { return }
        
        // Vibrate on success
        AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
        
        Task { @MainActor in
            self.stopScanning()
            self.scannedCode = code
        }
    }
}

#Preview {
    QRScannerView { code in
        Logger.debug("[QRScanner] Scanned: \(code)")
    }
}
