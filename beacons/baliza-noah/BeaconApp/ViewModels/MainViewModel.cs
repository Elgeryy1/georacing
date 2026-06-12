using System;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using BeaconApp.Config;
using BeaconApp.Models;
using BeaconApp.Services;

namespace BeaconApp.ViewModels
{
    /// <summary>
    /// ViewModel principal de la aplicación de baliza
    /// </summary>
    public class MainViewModel : INotifyPropertyChanged
    {
        private readonly ApiClient _apiClient;
        private readonly BeaconConfig _config;
        private readonly BleBeaconService _bleService;
        
        private Timer? _pollingTimer;
        private Timer? _heartbeatTimer;
        private CancellationTokenSource? _cts;
        private ApiLogger? _apiLogger;

        // Constantes de temporización
        private const int POLLING_INTERVAL_MS = 300;       // 300ms (lectura rápida DB)
        private const int HEARTBEAT_INTERVAL_MS = 10000;   // 10 segundos (registrarse)
        private DateTime _ignoreDbEvacuationUntil = DateTime.MinValue; // Grace period for DB lag

        // ... (Properties omitted for brevity) ...

        // ============================================
        // CONSTRUCTOR
        // ============================================

        public MainViewModel(BeaconConfig config, ApiClient apiClient)
        {
            _config = config;
            _apiClient = apiClient;
            _cts = new CancellationTokenSource();
            _apiLogger = new ApiLogger(_apiClient, _config.BeaconId);
            _bleService = new BleBeaconService(_config.ZoneId);

            Console.WriteLine($"[VM] ViewModel inicializado para baliza: {_config.BeaconId}");
            Log($"ViewModel inicializado para baliza: {_config.BeaconId}");
        }

        // ============================================
        // MÉTODOS PÚBLICOS
        // ============================================

        /// <summary>
        /// Inicia los servicios de heartbeat y polling
        /// </summary>
        public async Task Start()
        {
            StatusMessage = $"Conectando a API...";

            // 1. Healthcheck
            bool isOnline = await _apiClient.CheckHealthAsync();
            if (!isOnline)
            {
                StatusMessage = "⚠ SIN CONEXIÓN A API";
                Log("⚠ API no responde en el inicio");
            }
            else
            {
                StatusMessage = "✓ Conectado";
                Log("✓ API Online");

                // Sincronizar estado inicial
                await SyncConfigAsync();
            }

            // Inicializar Logger API
            if (_apiLogger != null)
            {
                await _apiLogger.InitializeAsync();
            }

            // 2. Iniciar Timers
            Log("🚀 Iniciando servicios de fondo (300ms)...");

            // Timer 1: Polling (leer comandos + sync config)
            _pollingTimer = new Timer(
                async _ => 
                {
                    await CheckGlobalStateAsync(); // Prioridad: Estado Global
                    await SyncConfigAsync();    // Leer estado DB individual
                    await PollCommandsAsync();  // Leer comandos
                },
                null,
                TimeSpan.FromMilliseconds(1000),
                TimeSpan.FromMilliseconds(POLLING_INTERVAL_MS)
            );

            // Timer 2: Heartbeat (registrarse)
            _heartbeatTimer = new Timer(
                async _ => await SendHeartbeatAsync(),
                null,
                TimeSpan.Zero,  // Enviar inmediatamente
                TimeSpan.FromMilliseconds(HEARTBEAT_INTERVAL_MS)
            );

            // Iniciar BLE Advertising
            _bleService.Start();
        }

        /// <summary>
        /// Detiene los servicios
        /// </summary>
        public void Stop()
        {
            _cts?.Cancel();
            _pollingTimer?.Dispose();
            _heartbeatTimer?.Dispose();
            _bleService.Stop();
            
            Log("✓ Servicios detenidos");
        }

        private string _backgroundColor = "#2E7D32";
        public string BackgroundColor
        {
            get => _backgroundColor;
            set
            {
                if (_backgroundColor != value)
                {
                    _backgroundColor = value;
                    OnPropertyChanged();
                }
            }
        }

        private string _currentLanguage = "ES";
        public string CurrentLanguage
        {
            get => _currentLanguage;
            set
            {
                if (_currentLanguage != value)
                {
                    _currentLanguage = value;
                    OnPropertyChanged();
                }
            }
        }

        private string _currentEvacuationExit = string.Empty;
        public string CurrentEvacuationExit
        {
            get => _currentEvacuationExit;
            set
            {
                if (_currentEvacuationExit != value)
                {
                    _currentEvacuationExit = value;
                    OnPropertyChanged();
                }
            }
        }

        private bool _isConfigured = false;
        public bool IsConfigured
        {
            get => _isConfigured;
            set
            {
                if (_isConfigured != value)
                {
                    _isConfigured = value;
                    OnPropertyChanged();
                }
            }
        }

        // ============================================
        // SYNC & HEARTBEAT
        // ============================================

        private async Task SyncConfigAsync()
        {
            try
            {
                var remoteConfig = await _apiClient.GetBeaconConfigAsync(_config.BeaconId);
                
                if (remoteConfig != null)
                {
                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        bool changed = false;
                        if (!string.IsNullOrEmpty(remoteConfig.Mode) && remoteConfig.Mode != CurrentMode)
                        {
                            // LOGIC: Conflict Resolution

                            // 1. Block flickering: If Global is EVACUATION, ignore individual NORMAL
                            if (_lastGlobalMode == "EVACUATION" && remoteConfig.Mode != "EVACUATION")
                            {
                                // Ignore downgrade
                            }
                            // 2. Block Stale Revert: If we just exited Global Evacuation, ignore individual EVACUATION for a few seconds
                            else if (remoteConfig.Mode == "EVACUATION" && DateTime.Now < _ignoreDbEvacuationUntil)
                            {
                                // Ignore stale DB config
                                // Log("Ignorando configuración EVACUATION antigua (DB lag)");
                            }
                            else
                            {
                                CurrentMode = remoteConfig.Mode;
                                changed = true;
                            }
                        }
                        if (!string.IsNullOrEmpty(remoteConfig.Arrow) && remoteConfig.Arrow != CurrentArrow)
                        {
                            CurrentArrow = remoteConfig.Arrow;
                            changed = true;
                        }
                        if (!string.IsNullOrEmpty(remoteConfig.Message) && remoteConfig.Message != DisplayText)
                        {
                            DisplayText = remoteConfig.Message;
                            changed = true;
                        }
                        if (!string.IsNullOrEmpty(remoteConfig.Color) && remoteConfig.Color != BackgroundColor)
                        {
                            BackgroundColor = remoteConfig.Color;
                            changed = true;
                        }
                        if (remoteConfig.Brightness.HasValue && remoteConfig.Brightness.Value != CurrentBrightness)
                        {
                            CurrentBrightness = remoteConfig.Brightness.Value;
                            changed = true;
                        }
                        if (!string.IsNullOrEmpty(remoteConfig.Zone) && remoteConfig.Zone != CurrentZone)
                        {
                            CurrentZone = remoteConfig.Zone;
                            changed = true;
                        }
                        if (!string.IsNullOrEmpty(remoteConfig.EvacuationExit) && remoteConfig.EvacuationExit != CurrentEvacuationExit)
                        {
                            CurrentEvacuationExit = remoteConfig.EvacuationExit;
                            changed = true;
                        }
                        
                        // Solo loguear si hubo cambio para no saturar
                        if (changed) Log($"↻ Sincronizado: {CurrentMode}");
                    });
                }
            }
            catch (Exception ex)
            {
                // No loguear error en cada tick de 300ms para no saturar, solo consola debug si es necesario
                System.Diagnostics.Debug.WriteLine($"Error sync: {ex.Message}");
            }
        }

        private async Task SendHeartbeatAsync()
        {
            if (_cts?.Token.IsCancellationRequested ?? true) return;

            try
            {
                // Ya no necesitamos sincronizar aquí porque lo hace el polling cada 300ms
                // Simplemente enviamos el estado actual

                var heartbeat = new BeaconHeartbeatRequest
                {
                    BeaconUid = _config.BeaconId,
                    Name = _config.Name,
                    Description = _config.Description,
                    ZoneId = _config.ZoneId,
                    Latitude = _config.Latitude,
                    Longitude = _config.Longitude,
                    HasScreen = 1,
                    Mode = CurrentMode,
                    ArrowDirection = CurrentArrow,
                    Message = DisplayText,
                    Color = BackgroundColor,
                    Brightness = CurrentBrightness,
                    BatteryLevel = (int)(System.Windows.Forms.SystemInformation.PowerStatus.BatteryLifePercent * 100)
                };

                await _apiClient.SendHeartbeatAsync(heartbeat);
            }
            catch (Exception ex)
            {
                Log($"✗ Error al enviar heartbeat: {ex.Message}");
            }
        }

        // ============================================
        // GLOBAL STATE CHECK
        // ============================================

        private string _lastGlobalMode = "";

        private async Task CheckGlobalStateAsync()
        {
            try
            {
                // Get full circuit state including mode and temperature
                var circuitState = await _apiClient.GetCircuitStateAsync();
                
                if (circuitState != null && !string.IsNullOrEmpty(circuitState.GlobalMode))
                {
                    var globalMode = circuitState.GlobalMode;
                    var circuitTemp = (int)(circuitState.Temperature ?? 25);

                    // Update BLE with current circuit state (mode + temperature)
                    _bleService.UpdateStatus(globalMode, circuitTemp);

                    // Si el modo global es EVACUACIÓN, forzamos localmente
                    if (globalMode == "EVACUATION")
                    {
                        if (CurrentMode != "EVACUATION")
                        {
                            Application.Current.Dispatcher.Invoke(() => 
                            {
                                CurrentMode = "EVACUATION";
                                // Forzar flecha arriba/salida por defecto si no tenemos una específica
                                if (string.IsNullOrEmpty(CurrentArrow) || CurrentArrow == "NONE")
                                    CurrentArrow = "UP"; 
                                
                                DisplayText = "EVACUACIÓN";
                            });
                            Log("🚨 MODO GLOBAL EVACUACIÓN ACTIVADO");
                        }
                    }
                    else if (globalMode != "EVACUATION" && _lastGlobalMode == "EVACUATION")
                    {
                        // SALIDA DE EVACUACIÓN DETECTADA
                        // 1. Forzamos modo local al nuevo modo global inmediatamente
                        Application.Current.Dispatcher.Invoke(() => 
                        {
                            CurrentMode = globalMode; // "NORMAL", "SAFETY_CAR", etc.
                            DisplayText = "SISTEMA RESTAURADO";
                            
                            // Resetear flechas si estaban forzadas
                            if (CurrentArrow == "UP") CurrentArrow = "NONE";
                        });

                        // 2. Establecer periodo de gracia (5s) para ignorar config antigua de la BD
                        // Esto da tiempo al backend para actualizar la tabla 'beacons' sin que la App vuelva a ponerse roja
                        _ignoreDbEvacuationUntil = DateTime.Now.AddSeconds(5);

                        Log($"✓ FIN DE EVACUACIÓN GLOBAL - Cambiando a {globalMode}");
                    }
                    _lastGlobalMode = globalMode;
                }
            }
            catch (Exception)
            {
                // Silent
            }
        }

        // ============================================
        // POLLING DE COMANDOS
        // ============================================

        private async Task PollCommandsAsync()
        {
            if (_cts?.Token.IsCancellationRequested ?? true) return;

            try
            {
                var commands = await _apiClient.GetPendingCommandsAsync(_config.BeaconId);

                foreach (var cmd in commands)
                {
                    // Check expiration (60 minutes to handle timezone skews)
                    // El servidor envía UTC (toISOString), así que comparamos con UtcNow
                    var age = DateTime.UtcNow - cmd.CreatedAt;
                    
                    // Debug de tiempos para verificar
                    // Log($"[DEBUG] Cmd: {cmd.CreatedAt}, NowUTC: {DateTime.UtcNow}, Age: {age.TotalMinutes:F1} min");

                    if (age > TimeSpan.FromMinutes(60))
                    {
                        Log($"⚠ Comando expirado (ignorando): {cmd.Command} (ID: {cmd.Id}). Age: {age.TotalMinutes:F1} min");
                        // Delete from DB
                        await _apiClient.DeleteAsync("commands", new { id = cmd.Id });
                        continue;
                    }

                    Log($"[CMD] Recibido: {cmd.Command} (ID: {cmd.Id})");
                    
                    bool success = await ProcessCommandAsync(cmd);
                    
                    if (success)
                    {
                        // Delete from DB instead of marking executed
                        await _apiClient.DeleteAsync("commands", new { id = cmd.Id });
                    }
                }
            }
            catch (Exception)
            {
                // Silencioso en bucle rápido
            }
        }

        private string _currentMode = "NORMAL";
        public string CurrentMode
        {
            get => _currentMode;
            set
            {
                // Normalize to uppercase for XAML triggers
                var normalized = value?.ToUpper() ?? "NORMAL";
                if (_currentMode != normalized)
                {
                    _currentMode = normalized;
                    OnPropertyChanged();
                    UpdateDisplayForMode();
                    
                    // Actualizar BLE Advertising con el nuevo modo
                    _bleService.UpdateStatus(normalized);
                }
            }
        }

        private string _currentZone = "Sistema iniciado";
        public string CurrentZone
        {
            get => _currentZone;
            set
            {
                if (_currentZone != value)
                {
                    _currentZone = value;
                    OnPropertyChanged();
                }
            }
        }

        private string _currentArrow = "NONE";
        public string CurrentArrow
        {
            get => _currentArrow;
            set
            {
                // Normalize to uppercase for XAML triggers
                var normalized = value?.ToUpper() ?? "NONE";
                if (_currentArrow != normalized)
                {
                    _currentArrow = normalized;
                    OnPropertyChanged();
                }
            }
        }

        private int _currentBrightness = 100;
        public int CurrentBrightness
        {
            get => _currentBrightness;
            set
            {
                if (_currentBrightness != value)
                {
                    _currentBrightness = value;
                    OnPropertyChanged();
                    SetWindowsBrightness(value);
                }
            }
        }

        private void SetWindowsBrightness(int brightness)
        {
            try
            {
                // Asegurar rango 0-100
                brightness = Math.Max(0, Math.Min(100, brightness));

                var psi = new System.Diagnostics.ProcessStartInfo
                {
                    FileName = "powershell",
                    Arguments = $"-Command \"(Get-WmiObject -Namespace root/wmi -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, {brightness})\"",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                System.Diagnostics.Process.Start(psi);
            }
            catch (Exception ex)
            {
                Log($"⚠ Error ajustando brillo Windows: {ex.Message}");
            }
        }

        public double BrightnessOpacity => 1.0 - (_currentBrightness / 100.0);

        private string _statusMessage = "Iniciando...";
        public string StatusMessage
        {
            get => _statusMessage;
            set
            {
                if (_statusMessage != value)
                {
                    _statusMessage = value;
                    OnPropertyChanged();
                }
            }
        }

        private string _displayText = "SISTEMA LISTO";
        public string DisplayText
        {
            get => _displayText;
            set
            {
                if (_displayText != value)
                {
                    _displayText = value;
                    OnPropertyChanged();
                }
            }
        }

        private Task<bool> ProcessCommandAsync(BeaconCommandDto cmd)
        {
            try
            {
                switch (cmd.Command.ToUpper())
                {
                    case "UPDATE_CONFIG":
                        return Task.FromResult(ProcessUpdateConfig(cmd.Value));

                    case "RESTART":
                        return Task.FromResult(ProcessRestart());

                    case "SHUTDOWN":
                        return Task.FromResult(ProcessShutdown());

                    case "CLOSE":
                    case "CLOSE_APP":
                        return Task.FromResult(ProcessCloseApp());

                    default:
                        Log($"⚠ Comando desconocido: {cmd.Command}");
                        return Task.FromResult(true); // Marcar como ejecutado para no bloquear
                }
            }
            catch (Exception ex)
            {
                Log($"✗ Error procesando comando {cmd.Command}: {ex.Message}");
                return Task.FromResult(false);
            }
        }

        private bool ProcessUpdateConfig(string jsonValue)
        {
            try
            {
                var config = JsonSerializer.Deserialize<BeaconConfigUpdate>(jsonValue);
                if (config == null) return false;

                Application.Current.Dispatcher.Invoke(() =>
                {
                    if (!string.IsNullOrEmpty(config.Mode))
                        CurrentMode = config.Mode;

                    if (!string.IsNullOrEmpty(config.Arrow))
                        CurrentArrow = config.Arrow;

                    if (!string.IsNullOrEmpty(config.Message))
                        DisplayText = config.Message;

                    if (!string.IsNullOrEmpty(config.Color))
                        BackgroundColor = config.Color;

                    if (config.Brightness.HasValue)
                        CurrentBrightness = config.Brightness.Value;

                    if (config.EvacuationExit != null)
                        CurrentEvacuationExit = config.EvacuationExit;

                    if (!string.IsNullOrEmpty(config.Zone))
                        CurrentZone = config.Zone;
                });

                Log("✓ Configuración actualizada");
                return true;
            }
            catch (Exception ex)
            {
                Log($"✗ Error deserializando config: {ex.Message}");
                return false;
            }
        }

        private bool ProcessRestart()
        {
            Log("🔄 Ejecutando REINICIO DE WINDOWS...");
            
            try 
            {
                // Delay 3s to allow command deletion from DB
                var psi = new System.Diagnostics.ProcessStartInfo("shutdown.exe", "/r /f /t 3")
                {
                    CreateNoWindow = true,
                    UseShellExecute = false
                };
                System.Diagnostics.Process.Start(psi);
                return true;
            }
            catch (Exception ex)
            {
                Log($"✗ Error al reiniciar Windows: {ex.Message}");
                return false;
            }
        }

        private bool ProcessShutdown()
        {
            Log("🔌 Ejecutando APAGADO DE WINDOWS...");
            
            try 
            {
                // Delay 3s to allow command deletion
                var psi = new System.Diagnostics.ProcessStartInfo("shutdown.exe", "/s /f /t 3")
                {
                    CreateNoWindow = true,
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };
                
                Log($"[DEBUG] Ejecutando: {psi.FileName} {psi.Arguments}");
                
                var process = System.Diagnostics.Process.Start(psi);
                if (process != null)
                {
                    process.WaitForExit(1000); // Esperar un poco para ver si falla inmediato
                    if (process.HasExited && process.ExitCode != 0)
                    {
                         var error = process.StandardError.ReadToEnd();
                         Log($"✗ shutdown.exe falló con código {process.ExitCode}: {error}");
                         return false;
                    }
                }
                
                return true;
            }
            catch (Exception ex)
            {
                Log($"✗ Error CRÍTICO al apagar Windows: {ex.Message}");
                return false;
            }
        }

        private bool ProcessCloseApp()
        {
            Log("❌ Cerrando aplicación en 2 segundos...");
            
            // Run in background to allow returning 'true' immediately so checking loop can delete the command
            Task.Run(async () => 
            {
                await Task.Delay(2000);
                Application.Current.Dispatcher.Invoke(() =>
                {
                    Application.Current.Shutdown();
                });
            });
            
            return true;
        }

        // ============================================
        // ACTUALIZACIÓN DE DISPLAY
        // ============================================

        private void UpdateDisplayForMode()
        {
            // Solo actualiza si NO se ha establecido un mensaje/color manual recientemente
            // Pero en este diseño, UPDATE_CONFIG sobrescribe todo, así que podemos dejar defaults
            // como fallback si el mensaje viene vacío.
            
            // Lógica simplificada: Si el modo cambia, ponemos defaults, 
            // pero si viene un mensaje específico en el mismo comando, ese prevalecerá 
            // porque se asigna después en ProcessUpdateConfig.
            
            UpdateDefaultTextForMode();
            UpdateDefaultColorForMode();
        }

        private void UpdateDefaultTextForMode()
        {
            // Solo cambiar si no hay un mensaje personalizado activo? 
            // Para simplificar, al cambiar de modo reseteamos a default.
            DisplayText = CurrentMode?.ToUpper() switch
            {
                "UNCONFIGURED" => "SIN CONFIGURAR",
                "NORMAL" => "MODO NORMAL",
                "CONGESTION" => "⚠️ CONGESTIÓN",
                "EMERGENCY" => "🚨 EMERGENCIA",
                "EVACUATION" => "🚨 EVACUACIÓN",
                "MAINTENANCE" => "🔧 MANTENIMIENTO",
                _ => "SISTEMA LISTO"
            };
        }

        private void UpdateDefaultColorForMode()
        {
            BackgroundColor = CurrentMode?.ToUpper() switch
            {
                "UNCONFIGURED" => "#1565C0",
                "NORMAL" => "#2E7D32",
                "CONGESTION" => "#F57C00",
                "EMERGENCY" => "#C62828",
                "EVACUATION" => "#D32F2F",
                "MAINTENANCE" => "#7B1FA2",
                _ => "#424242"
            };
        }

        // ============================================
        // INotifyPropertyChanged
        // ============================================

        public event PropertyChangedEventHandler? PropertyChanged;

        protected virtual void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        private void Log(string message)
        {
            // Escribir siempre en consola para debug inmediato
            Console.WriteLine($"[LOG] {message}");

            // Log local y API
            _apiLogger?.Log("INFO", message);
            
            // Fallback si el logger no está listo (aunque ApiLogger ya llama a FileLogger)
            if (_apiLogger == null)
            {
                FileLogger.Log($"[VM] {message}");
            }
        }
    }
}
