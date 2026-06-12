using System;
using System.Net.Http;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Threading.Tasks;
using System.Linq; // Added for MAC formatting
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Storage.Streams;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace BeaconActivePc
{
    class Program
    {
        // Configurable Constants
        private const string API_URL = "https://alpo.myqnapcloud.com:4010/api/state";
        private const ushort MANUFACTURER_ID = 0x1234; // Test ID (Replace if you have a real one)
        private const int POLL_INTERVAL_MS = 2000;
        private const ushort ZONE_ID = 1001; // Example Zone ID for this PC Beacon

        // State Tracking
        private static HttpClient client;
        private static BluetoothLEAdvertisementPublisher publisher;
        private static ushort sequenceConfig = 0;
        private static byte currentMode = 0; // 0=NORMAL
        private static string lastKnownTemp = "";
        
        static async Task Main(string[] args)
        {
            // Initialize insecure HTTP client
            var handler = new HttpClientHandler();
            handler.ClientCertificateOptions = ClientCertificateOption.Manual;
            handler.ServerCertificateCustomValidationCallback = 
                (httpRequestMessage, cert, cetChain, policyErrors) =>
            {
                return true;
            };
            client = new HttpClient(handler);

            Console.WriteLine("=== GeoRacing Active Beacon (PC) ===");
            
            // PRINT ADAPTER ADDRESS
            try {
                var adapter = await BluetoothAdapter.GetDefaultAsync();
                if (adapter != null) {
                    var addr = adapter.BluetoothAddress;
                    // Format as XX:XX:XX:XX:XX:XX
                    string mac = string.Join(":", BitConverter.GetBytes(addr).Reverse().Skip(2).Select(b => b.ToString("X2")));
                    Console.WriteLine($"My MAC Address: {mac}");
                    Console.WriteLine($"Radio Info: {adapter.DeviceId}");
                } else {
                    Console.WriteLine("My MAC Address: UNKNOWN (Adapter not found)");
                }
            } catch (Exception ex) {
                 Console.WriteLine($"Could not get MAC: {ex.Message}");
            }

            Console.WriteLine($"API: {API_URL}");
            Console.WriteLine($"Zone: {ZONE_ID}");
            Console.WriteLine("Initializing BLE Publisher...");

            try
            {
                publisher = new BluetoothLEAdvertisementPublisher();
                publisher.StatusChanged += Publisher_StatusChanged;

                // Start Polling Loop
                _ = Task.Run(PollApiLoop);

                Console.WriteLine("Press any key to exit...");
                Console.ReadKey();
                publisher.Stop();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
            }
        }

        private static void Publisher_StatusChanged(BluetoothLEAdvertisementPublisher sender, BluetoothLEAdvertisementPublisherStatusChangedEventArgs args)
        {
            Console.WriteLine($"BLE Status: {args.Status} (Error: {args.Error})");
        }

        private static async Task PollApiLoop()
        {
            while (true)
            {
                try
                {
                    // 1. Fetch State
                    var json = await client.GetStringAsync(API_URL);
                    var state = JsonConvert.DeserializeObject<CircuitStateDto>(json);
                    
                    if (state != null)
                    {
                        var newMode = MapModeToByte(state.global_mode ?? state.mode);
                        lastKnownTemp = state.temperature;
                        
                        // 2. Update Payload if changed or refresh TTL
                        UpdateAdvertising(newMode);
                        
                        Console.WriteLine($"[{DateTime.Now:HH:mm:ss}] API OK. Mode: {state.global_mode} -> {newMode}. Seq: {sequenceConfig}");
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[{DateTime.Now:HH:mm:ss}] API Error: {ex.Message}");
                    // Keep broadcasting last known state (Active Beacon reliability)
                    // Or switch to DEGRADED/UNKNOWN if too long? For now, keep last state logic.
                }

                await Task.Delay(POLL_INTERVAL_MS);
            }
        }

        private static void UpdateAdvertising(byte mode)
        {
            // Payload Structure (9 Bytes)
            // 0: Version (0x01)
            // 1-2: Zone ID
            // 3: Mode
            // 4: Flags
            // 5-6: Seq
            // 7: TTL (Seconds)
            // 8: Temperature (Int8)

            sequenceConfig++; 
            byte ttl = 10; // 10 seconds validity
            byte flags = 0x00; // No flags for now
            
            // Parse temperature from global var or pass it in
            // For now let's modify method signature or use a static
            byte tempByte = 0;
            if (!string.IsNullOrEmpty(lastKnownTemp))
            {
                 // Remove non-numeric
                 var digits = new string(lastKnownTemp.Where(char.IsDigit).ToArray());
                 if (byte.TryParse(digits, out byte t)) tempByte = t;
            }

            var writer = new DataWriter();
            writer.WriteByte(0x01); // Version
            writer.WriteUInt16(ZONE_ID); // Zone
            writer.WriteByte(mode); // Mode
            writer.WriteByte(flags); // Flags
            writer.WriteUInt16(sequenceConfig); // Seq
            writer.WriteByte(ttl); // TTL
            writer.WriteByte(tempByte); // Temperature

            var buffer = writer.DetachBuffer();
            
            // Re-create manufacturer data to ensure update
            var manufacturerData = new BluetoothLEManufacturerData(MANUFACTURER_ID, buffer);

            // Update Publisher
            // Note: Optimally we only change if data changed, but Seq changes every time to prove liveness.
            publisher.Stop();
            publisher.Advertisement.ManufacturerData.Clear();
            publisher.Advertisement.ManufacturerData.Add(manufacturerData);
            publisher.Start();
        }

        private static byte MapModeToByte(string modeString)
        {
            if (string.IsNullOrEmpty(modeString)) return 0; // Normal default
            
            return modeString.ToUpper() switch
            {
                "NORMAL" => 0,
                "SAFETY_CAR" => 1, // Or CONGESTION mapping
                "RED_FLAG" => 2,
                "EVACUATION" => 3,
                _ => 0
            };
        }
    }

    // DTO matching API JSON
    class CircuitStateDto
    {
        public string id { get; set; }
        public string global_mode { get; set; }
        public string mode { get; set; } // Legacy or local
        public string message { get; set; }
        public string temperature { get; set; } // New field
    }
}
