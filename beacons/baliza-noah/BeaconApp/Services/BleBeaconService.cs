using System;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Storage.Streams;

namespace BeaconApp.Services
{
    /// <summary>
    /// Servicio BLE Advertising para emitir señales de baliza GeoRacing.
    /// Protocolo: Manufacturer ID 0x1234 con payload de 9 bytes.
    /// </summary>
    public class BleBeaconService
    {
        private readonly int _zoneId;
        private BluetoothLEAdvertisementPublisher? _publisher;
        private ushort _sequenceCounter = 0;
        private byte _currentMode = 0x00;
        private byte _currentTemp = 25;

        // Manufacturer ID GeoRacing
        private const ushort MANUFACTURER_ID = 0x1234;

        /// <summary>
        /// Indica si el advertising BLE está activo
        /// </summary>
        public bool IsRunning => _publisher?.Status == BluetoothLEAdvertisementPublisherStatus.Started;

        public BleBeaconService(int zoneId)
        {
            _zoneId = zoneId;
            Log($"BleBeaconService inicializado para ZoneId: {zoneId}");
        }

        /// <summary>
        /// Inicia el broadcasting BLE
        /// </summary>
        public void Start()
        {
            try
            {
                _publisher = new BluetoothLEAdvertisementPublisher();

                // Crear el advertisement con los datos iniciales
                var manufacturerData = CreateManufacturerData();
                _publisher.Advertisement.ManufacturerData.Add(manufacturerData);

                // Suscribirse a cambios de estado
                _publisher.StatusChanged += OnPublisherStatusChanged;

                // Iniciar broadcasting
                _publisher.Start();
                Log("✓ BLE Advertising iniciado");
            }
            catch (Exception ex)
            {
                Log($"✗ Error al iniciar BLE: {ex.Message}");
                // No crashear la app si BLE no está disponible
            }
        }

        /// <summary>
        /// Detiene el broadcasting BLE
        /// </summary>
        public void Stop()
        {
            try
            {
                if (_publisher != null)
                {
                    _publisher.Stop();
                    _publisher.StatusChanged -= OnPublisherStatusChanged;
                    _publisher = null;
                    Log("✓ BLE Advertising detenido");
                }
            }
            catch (Exception ex)
            {
                Log($"✗ Error al detener BLE: {ex.Message}");
            }
        }

        /// <summary>
        /// Actualiza el payload BLE en tiempo real - solo si hay cambios
        /// </summary>
        /// <param name="mode">Modo: "NORMAL", "CONGESTION", "EMERGENCY", "RED_FLAG", "EVACUATION"</param>
        /// <param name="temperature">Temperatura en grados (0-255)</param>
        public void UpdateStatus(string mode, int temperature = 25)
        {
            try
            {
                // Mapear modo string a byte
                var newMode = MapModeToByte(mode);
                var newTemp = (byte)Math.Clamp(temperature, 0, 255);

                // Solo actualizar si hay cambios reales (evitar Stop/Start innecesarios)
                if (newMode == _currentMode && newTemp == _currentTemp)
                {
                    return; // Sin cambios, no hacer nada
                }

                _currentMode = newMode;
                _currentTemp = newTemp;

                // Incrementar contador de secuencia (indica dato fresco)
                _sequenceCounter++;

                // Recrear el publisher completamente (WinRT no permite reusar después de Stop)
                if (_publisher != null)
                {
                    _publisher.Stop();
                    _publisher.StatusChanged -= OnPublisherStatusChanged;
                }

                _publisher = new BluetoothLEAdvertisementPublisher();
                _publisher.Advertisement.ManufacturerData.Add(CreateManufacturerData());
                _publisher.StatusChanged += OnPublisherStatusChanged;
                _publisher.Start();

                Log($"↻ BLE actualizado: Mode={mode} (0x{_currentMode:X2}), Seq={_sequenceCounter}, Temp={_currentTemp}°C");
            }
            catch (Exception ex)
            {
                Log($"✗ Error actualizando BLE: {ex.Message}");
            }
        }

        /// <summary>
        /// Crea el objeto ManufacturerData con el payload de 9 bytes
        /// </summary>
        private BluetoothLEManufacturerData CreateManufacturerData()
        {
            var manufacturerData = new BluetoothLEManufacturerData
            {
                CompanyId = MANUFACTURER_ID
            };

            // Payload de 9 bytes en Big Endian
            byte[] payload = new byte[9];

            // Byte 0: Versión (siempre 0x01)
            payload[0] = 0x01;

            // Byte 1-2: Zone ID (Big Endian)
            payload[1] = (byte)((_zoneId >> 8) & 0xFF);
            payload[2] = (byte)(_zoneId & 0xFF);

            // Byte 3: Modo (The Scream mapping)
            payload[3] = _currentMode;

            // Byte 4: Flags (siempre 0x00)
            payload[4] = 0x00;

            // Byte 5-6: Secuencia (Big Endian, contador incremental)
            payload[5] = (byte)((_sequenceCounter >> 8) & 0xFF);
            payload[6] = (byte)(_sequenceCounter & 0xFF);

            // Byte 7: TTL (siempre 0x0A = 10 segundos)
            payload[7] = 0x0A;

            // Byte 8: Temperatura
            payload[8] = _currentTemp;

            // Convertir a buffer de Windows Runtime
            var writer = new DataWriter();
            writer.WriteBytes(payload);
            manufacturerData.Data = writer.DetachBuffer();

            return manufacturerData;
        }

        /// <summary>
        /// Mapea strings de modo a bytes del protocolo
        /// </summary>
        private static byte MapModeToByte(string mode)
        {
            return mode?.ToUpperInvariant() switch
            {
                "NORMAL" => 0x00,
                "CONGESTION" => 0x01,
                "SAFETY_CAR" => 0x01,  // Same as congestion (caution flag)
                "EMERGENCY" => 0x02,
                "RED_FLAG" => 0x02,
                "EVACUATION" => 0x03,
                _ => 0x00  // Default: NORMAL
            };
        }

        private void OnPublisherStatusChanged(BluetoothLEAdvertisementPublisher sender, BluetoothLEAdvertisementPublisherStatusChangedEventArgs args)
        {
            var status = args.Status;
            var error = args.Error;

            if (error != BluetoothError.Success)
            {
                Log($"⚠ BLE Error: {error}");
            }

            Log($"BLE Status: {status}");
        }

        private static void Log(string message)
        {
            var timestamp = DateTime.Now.ToString("HH:mm:ss");
            Console.WriteLine($"[{timestamp}] [BLE] {message}");
            FileLogger.Log($"[BLE] {message}");
        }
    }
}
