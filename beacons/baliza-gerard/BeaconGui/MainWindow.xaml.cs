using System;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Threading;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Storage.Streams;
using Newtonsoft.Json;

namespace BeaconGui
{
    public partial class MainWindow : Window
    {
        // Configurable Constants
        private const string API_URL = "https://alpo.myqnapcloud.com:4010/api/state";
        private const ushort MANUFACTURER_ID = 0x1234; 
        private const int POLL_INTERVAL_MS = 2000;
        private const ushort ZONE_ID = 1001;

        // State Tracking
        private HttpClient client;
        private BluetoothLEAdvertisementPublisher publisher;
        private BluetoothLEAdvertisementWatcher watcher;
        private System.Collections.Generic.Dictionary<int, DateTime> detectedUsers = new System.Collections.Generic.Dictionary<int, DateTime>();
        private DispatcherTimer cleanupTimer;
        private ushort sequenceConfig = 0;
        private byte currentMode = 0; 
        private string lastKnownTemp = "";
        private bool isRunning = false;
        private bool simulateUser = false;

        public MainWindow()
        {
            InitializeComponent();
            SetupHttpClient();
            InitializeBeaconSystem();
        }

        private void SetupHttpClient()
        {
            var handler = new HttpClientHandler();
            handler.ClientCertificateOptions = ClientCertificateOption.Manual;
            handler.ServerCertificateCustomValidationCallback = 
                (httpRequestMessage, cert, cetChain, policyErrors) => true;
            client = new HttpClient(handler);
        }

        private async void InitializeBeaconSystem()
        {
            Log("Initializing System...");
            
            // Get MAC
            try {
                var adapter = await BluetoothAdapter.GetDefaultAsync();
                if (adapter != null) {
                    var addr = adapter.BluetoothAddress;
                    string mac = string.Join(":", BitConverter.GetBytes(addr).Reverse().Skip(2).Select(b => b.ToString("X2")));
                    Log($"My MAC Address: {mac}");
                } else {
                    Log("MAC: UNKNOWN (No Adapter)");
                }
            } catch (Exception ex) {
                Log($"Error getting MAC: {ex.Message}");
            }

            publisher = new BluetoothLEAdvertisementPublisher();
            publisher.StatusChanged += Publisher_StatusChanged;
            
            // Start Scanner for User Beacons
            watcher = new BluetoothLEAdvertisementWatcher();

            // Explicitly filter for our ID to improve reliability
            var manufacturerData = new BluetoothLEManufacturerData();
            manufacturerData.CompanyId = MANUFACTURER_ID;
            watcher.AdvertisementFilter.Advertisement.ManufacturerData.Add(manufacturerData);

            watcher.ScanningMode = BluetoothLEScanningMode.Active;
            watcher.Received += Watcher_Received;
            watcher.Start();
            Log("Scanner Started (Listening for Users)...");

            // Cleanup Timer
            cleanupTimer = new DispatcherTimer();
            cleanupTimer.Interval = TimeSpan.FromSeconds(5);
            cleanupTimer.Tick += CleanupUsers_Tick;
            cleanupTimer.Start();
            
            // Start Polling Loop in background
            _ = Task.Run(PollApiLoop);
        }

        private void Watcher_Received(BluetoothLEAdvertisementWatcher sender, BluetoothLEAdvertisementReceivedEventArgs args)
        {
            foreach (var section in args.Advertisement.ManufacturerData)
            {
                var reader = DataReader.FromBuffer(section.Data);
                byte[] bytes = new byte[section.Data.Length];
                reader.ReadBytes(bytes);
                string hex = BitConverter.ToString(bytes);

                if (section.CompanyId == MANUFACTURER_ID)
                {
                    if (bytes.Length >= 5 && bytes[0] == 0x01)
                    {
                        int hash = BitConverter.ToInt32(bytes, 1);
                        Dispatcher.Invoke(() => {
                            bool isNew = !detectedUsers.ContainsKey(hash);
                            detectedUsers[hash] = DateTime.Now;

                            if (isNew)
                            {
                                UsersText.Text = detectedUsers.Count.ToString();
                                Log($"[USER FOUND] {hash:X} ({args.RawSignalStrengthInDBm}dBm)"); 
                            }
                        });
                    }
                }
            }
        }

        private void CleanupUsers_Tick(object sender, EventArgs e)
        {
             var now = DateTime.Now;
             var stale = detectedUsers.Where(kvp => (now - kvp.Value).TotalSeconds > 30).ToList();
             
             if (stale.Any())
             {
                 foreach (var user in stale)
                 {
                     detectedUsers.Remove(user.Key);
                     Log($"[USER LOST] {user.Key:X}");
                 }
                 UsersText.Text = detectedUsers.Count.ToString();
             }
        }

        // --- BUTTON HANDLERS ---

        private async void BtnStart_Click(object sender, RoutedEventArgs e)
        {
            try 
            {
                if (isRunning) return;
                Log("Starting Signal...");
                
                // key fix: Initialize payload BEFORE starting
                await UpdateAdvertisingAsync(currentMode);
                
                // publisher.Start() called inside UpdateAdvertising usually, 
                // but UpdateAdvertising logic stops/clears/adds/STARTS.
                // So checking UpdateAdvertising implementation...
                // It does call Start(). So we don't need to call it here again explicitly 
                // OR we can just set isRunning = true first.
                
                // Let's rely on UpdateAdvertising to do the mechanics, but we need to set isRunning first
                // so the loop keeps it alive? No, the loop uses isRunning to decide whether to update.
                // But UpdateAdvertising is what puts the data in.
                
                // Let's modify UpdateAdvertising to be safer or call it here.
                
                isRunning = true; 
                UpdateStatusUI(true);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Crash Error: {ex.Message}\nStack: {ex.StackTrace}", "Error de Inicio");
                Log($"CRITICAL START ERROR: {ex.Message}");
                isRunning = false;
                UpdateStatusUI(false);
            }
        }

        private void BtnStop_Click(object sender, RoutedEventArgs e)
        {
            if (!isRunning) return;
            Log("Stopping Signal...");
            publisher.Stop();
            isRunning = false;
            UpdateStatusUI(false);
        }

        private async void ChkSimUser_Changed(object sender, RoutedEventArgs e)
        {
             if (sender is System.Windows.Controls.CheckBox chk) 
             {
                 simulateUser = chk.IsChecked ?? false;
                 Log($"Simulation Mode: {simulateUser}");
                 if (isRunning) await UpdateAdvertisingAsync(currentMode);
             }
        }

        // --- CORE LOGIC ---

        private async Task PollApiLoop()
        {
            while (true)
            {
                if (!isRunning)
                {
                    await Task.Delay(1000);
                    continue;
                }

                try
                {
                    var json = await client.GetStringAsync(API_URL);
                    var state = JsonConvert.DeserializeObject<CircuitStateDto>(json);
                    
                    if (state != null)
                    {
                        var newMode = MapModeToByte(state.global_mode ?? state.mode);
                        var newTemp = state.temperature;

                        // UI Update
                        Dispatcher.Invoke(() => {
                            ModeText.Text = (state.global_mode ?? "NORMAL").ToUpper();
                            ModeText.Foreground = GetColorForMode(newMode);
                        });

                        // Only Update if Changed
                        if (newMode != currentMode || newTemp != lastKnownTemp || !isRunning)
                        {
                             lastKnownTemp = newTemp;
                             currentMode = newMode;
                             await UpdateAdvertisingAsync(newMode);
                             Log($"Context Changed -> Mode: {newMode}, Temp: {lastKnownTemp}");
                        }
                        else 
                        {
                             // Heartbeat log only
                             Dispatcher.Invoke(() => SeqText.Text = $"{sequenceConfig} (Hold)");
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log($"API Error: {ex.Message}");
                }

                await Task.Delay(POLL_INTERVAL_MS);
            }
        }

        private async Task UpdateAdvertisingAsync(byte mode)
        {
            sequenceConfig++; 
            var writer = new DataWriter();

            if (simulateUser)
            {
                // Fake User Packet [Type(1) | Hash(4) | Lat(4) | Lon(4)]
                writer.WriteByte(0x01);
                writer.WriteInt32(unchecked((int)0xDEADBEEF));
                // Coordinates near Barcelona Circuit
                writer.WriteSingle(41.5700f); 
                writer.WriteSingle(2.2611f);
                if (sequenceConfig % 10 == 0) Log("Sim User at 41.57, 2.26");
            }
            else
            {
                // Normal Circuit Packet
                byte ttl = 10;
                byte flags = 0x00;
                byte tempByte = 0;
                if (!string.IsNullOrEmpty(lastKnownTemp))
                {
                     var digits = new string(lastKnownTemp.Where(char.IsDigit).ToArray());
                     if (byte.TryParse(digits, out byte t)) tempByte = t;
                }

                writer.WriteByte(0x01); // Version
                writer.WriteUInt16(ZONE_ID);
                writer.WriteByte(mode);
                writer.WriteByte(flags);
                writer.WriteUInt16(sequenceConfig);
                writer.WriteByte(ttl);
                writer.WriteByte(tempByte);
            }

            var buffer = writer.DetachBuffer();
            var manufacturerData = new BluetoothLEManufacturerData(MANUFACTURER_ID, buffer);

            // 1. Stop (UI Thread)
            await Dispatcher.InvokeAsync(() => {
                try { publisher.Stop(); } catch {}
            });

            // 2. Wait for stack to clear
            await Task.Delay(500);

            // 3. Start (UI Thread)
            await Dispatcher.InvokeAsync(() => {
                try {
                    publisher.Advertisement.ManufacturerData.Clear();
                    publisher.Advertisement.ManufacturerData.Add(manufacturerData);
                    
                    // Removed Name to prevent "Value does not fall within range" error
                    // publisher.Advertisement.LocalName = "GEORACING";
                    
                    publisher.Start();
                    SeqText.Text = sequenceConfig.ToString();
                } catch (Exception ex) {
                    Log($"Broadcast Error: {ex.Message}");
                }
            });
        }

        private void Publisher_StatusChanged(BluetoothLEAdvertisementPublisher sender, BluetoothLEAdvertisementPublisherStatusChangedEventArgs args)
        {
            Dispatcher.Invoke(() => {
                Log($"BLE Status: {args.Status}");
                if (args.Error != BluetoothError.Success)
                {
                    Log($"BLE CLICK ERROR: {args.Error}");
                }
            });
        }

        // --- HELPERS ---

        private void UpdateStatusUI(bool active)
        {
            if (active) {
                StatusText.Text = "BROADCASTING";
                StatusText.Foreground = new SolidColorBrush(Color.FromRgb(46, 204, 113)); // Green
            } else {
                StatusText.Text = "STOPPED";
                StatusText.Foreground = new SolidColorBrush(Color.FromRgb(231, 76, 60)); // Red
                ModeText.Text = "-";
                SeqText.Text = "-";
            }
        }

        private void Log(string msg)
        {
            Dispatcher.Invoke(() => {
                LogText.Text += $"[{DateTime.Now:HH:mm:ss}] {msg}\n";
                LogScroll.ScrollToEnd();
            });
        }

        private byte MapModeToByte(string modeString)
        {
            if (string.IsNullOrEmpty(modeString)) return 0;
            return modeString.ToUpper() switch
            {
                "NORMAL" => 0,
                "SAFETY_CAR" => 1,
                "RED_FLAG" => 2,
                "EVACUATION" => 3,
                _ => 0
            };
        }

         private Brush GetColorForMode(byte mode)
        {
            return mode switch {
                1 => Brushes.Orange,   // SC
                2 => Brushes.Red,      // Red Flag
                3 => Brushes.Magenta,  // Evacuation
                _ => Brushes.LightBlue // Normal
            };
        }
    }

    class CircuitStateDto
    {
        public string id { get; set; }
        public string global_mode { get; set; }
        public string mode { get; set; } 
        public string message { get; set; }
        public string temperature { get; set; } 
    }
}