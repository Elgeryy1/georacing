import SwiftUI

struct SocialView: View {
    @StateObject private var viewModel = SocialViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        ZStack {
            RacingColors.darkBackground.edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 24) {
                // Header
                HStack {
                    Text(LocalizationUtils.string("Social Group"))
                        .font(RacingFont.header(28))
                        .foregroundColor(.white)
                    Spacer()
                    Button(action: { presentationMode.wrappedValue.dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(RacingColors.silver)
                    }
                }
                .padding()
                
                if viewModel.isJoined {
                    // Group Dashboard
                    VStack(spacing: 20) {
                        Text("You are in a group!")
                            .font(RacingFont.subheader())
                            .foregroundColor(.green)
                        
                        if let qr = viewModel.qrCodeImage {
                            Image(uiImage: qr)
                                .resizable()
                                .interpolation(.none)
                                .scaledToFit()
                                .frame(width: 200, height: 200)
                                .padding()
                                .background(Color.white)
                                .cornerRadius(12)
                            
                            Text("Share this QR code with friends")
                                .font(RacingFont.body())
                                .foregroundColor(RacingColors.silver)
                        } else {
                            // If joined but no QR (joined via scan), show info
                            Text("Group Active")
                                .font(RacingFont.header(20))
                                .foregroundColor(.white)
                        }
                        
                        Button(action: viewModel.leaveGroup) {
                            Text("Leave Group")
                        }
                        .racingButton(color: RacingColors.red)
                    }
                } else {
                    // Join / Create Options
                    VStack(spacing: 30) {
                        Button(action: viewModel.createGroup) {
                            HStack {
                                Image(systemName: "person.3.fill")
                                Text("Create Group")
                            }
                        }
                        .racingButton(color: .blue)
                        
                        Divider().background(Color.white)
                        
                        Text("OR")
                            .font(RacingFont.subheader())
                            .foregroundColor(RacingColors.silver)
                        
                        Button(action: {
                            // Simulate Scan for Dev
                            // In real app: Open Camera View
                            viewModel.joinGroup(url: "georacing://join?groupId=simulated_scan")
                        }) {
                            HStack {
                                Image(systemName: "qrcode.viewfinder")
                                Text("Scan QR Code (Sim)")
                            }
                        }
                        .racingButton(color: .green)
                    }
                    .padding()
                }
                
                Spacer()
            }
        }
        .alert("Alert",
               isPresented: Binding<Bool>(
                get: { viewModel.alertMessage != nil },
                set: { if !$0 { viewModel.alertMessage = nil } }
               ),
               actions: {
                Button("OK", role: .cancel) { viewModel.alertMessage = nil }
               },
               message: {
                Text(viewModel.alertMessage ?? "")
               })
    }
}
