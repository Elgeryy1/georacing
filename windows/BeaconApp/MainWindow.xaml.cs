using System;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;
using System.Windows.Threading;
using System.Diagnostics;
using System.Threading.Tasks;
using BeaconApp.Config;
using BeaconApp.Services;
using BeaconApp.ViewModels;

namespace BeaconApp
{
    /// <summary>
    /// Ventana principal de la aplicación de baliza
    /// </summary>
    public partial class MainWindow : Window
    {
        private MainViewModel? _viewModel;
        private ApiClient? _apiClient;
        private DispatcherTimer? _clockTimer;
        private DispatcherTimer? _commandTimer;
        private string _beaconId = string.Empty;
        
        public MainWindow()
        {
            InitializeComponent();
            
            Loaded += MainWindow_Loaded;
            Closing += MainWindow_Closing;
            
            // Iniciar reloj en tiempo real
            _clockTimer = new DispatcherTimer
            {
                Interval = TimeSpan.FromSeconds(1)
            };
            _clockTimer.Tick += UpdateClock;
            _clockTimer.Start();
        }

        private void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                Console.WriteLine("========================================");
                Console.WriteLine("🏁 GeoRacing - Baliza");
                Console.WriteLine("========================================");

                // 1. Leer/crear configuración
                var config = BeaconConfigService.ReadOrCreateConfig();
                _beaconId = config.BeaconId;
                Console.WriteLine($"Baliza ID: {config.BeaconId}");
                Console.WriteLine($"API URL: {config.ApiBaseUrl}");

                // 2. Crear cliente API
                _apiClient = new ApiClient(config.ApiBaseUrl);

                // 3. Crear ViewModel
                _viewModel = new MainViewModel(config, _apiClient);
                DataContext = _viewModel;

                // 4. Suscribirse a cambios de color de fondo
                _viewModel.PropertyChanged += ViewModel_PropertyChanged;

                // 5. Iniciar servicios
                _ = _viewModel.Start();

                Console.WriteLine("✓ Aplicación iniciada correctamente");
                Console.WriteLine("========================================");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"✗ Error al iniciar: {ex.Message}");
                Console.Error.WriteLine(ex.StackTrace);
                
                MessageBox.Show(
                    $"Error al iniciar la aplicación:\n\n{ex.Message}",
                    "Error de Inicio",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error
                );
                
                Application.Current.Shutdown();
            }
        }

        private void MainWindow_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
        {
            Console.WriteLine("Cerrando aplicación...");
            
            _viewModel?.Stop();
            // _apiClient?.Dispose();
            if (_commandTimer != null)
            {
                _commandTimer.Stop();
                _commandTimer = null;
            }
            
            Console.WriteLine("✓ Aplicación cerrada");
        }

        private void UpdateClock(object? sender, EventArgs e)
        {
            // Update clock in all views
            if (TimeTextNormal != null) TimeTextNormal.Text = DateTime.Now.ToString("HH:mm:ss");
            // Add other TimeText controls if needed or bind them
        }

        private void ViewModel_PropertyChanged(object? sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            // La UI se actualiza automáticamente via DataBinding en XAML
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Escape)
            {
                Console.WriteLine("ESC presionado, cerrando aplicación...");
                Close();
            }
        }

        // ============================================================
        // COMANDOS DEL BACKEND → ACCIONES LOCALES
        // ============================================================
        // ============================================================
        // COMANDOS DEL BACKEND → ACCIONES LOCALES
        // ============================================================
        // La lógica de polling se ha movido al ViewModel (MainViewModel.cs)
        // para centralizar la comunicación con la API.

        private bool ExecuteSystemCommand(string command, string? value)
        {
            try
            {
                switch (command)
                {
                    case "RESTART":
                        return RunShutdown("/r /t 3"); // Reiniciar Windows (3s delay)
                    case "SHUTDOWN":
                        return RunShutdown("/s /t 3"); // Apagar Windows (3s delay)
                    case "CLOSE":
                        return RunShutdown("/s /t 3"); // Según tu nota: close apaga el PC
                    case "CLOSE_APP":
                        Application.Current.Shutdown();
                        return true;
                    default:
                        Console.WriteLine($"[CMD] No reconocido: {command}");
                        return false;
                }
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"✗ Error ejecutando comando {command}: {ex.Message}");
                return false;
            }
        }

        private bool RunShutdown(string args)
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "shutdown.exe",
                    Arguments = args,
                    CreateNoWindow = true,
                    UseShellExecute = false
                };
                Process.Start(psi);
                return true;
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"✗ shutdown.exe fallo: {ex.Message}");
                return false;
            }
        }

        private async void SendCommandButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (_apiClient == null || string.IsNullOrWhiteSpace(_beaconId)) return;

                var command = CommandInput.Text?.Trim();
                string? value = null; // Simplificado para diseño limpio

                if (string.IsNullOrWhiteSpace(command))
                {
                    Console.WriteLine("[UI] Comando vacío – no se envía");
                    return;
                }

                var ok = await _apiClient.CreateCommandAsync(_beaconId, command.ToUpperInvariant(), string.IsNullOrWhiteSpace(value) ? null : value);
                // BottomButtonText.Text = ok ? $"CMD: {command.ToUpperInvariant()}" : "ERROR CMD";
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"✗ Error enviando comando: {ex.Message}");
                // BottomButtonText.Text = "ERROR CMD";
            }
        }

        private async void RestartButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (_apiClient == null || string.IsNullOrWhiteSpace(_beaconId)) return;
                var ok = await _apiClient.CreateCommandAsync(_beaconId, "RESTART");
                // BottomButtonText.Text = ok ? "CMD: RESTART" : "ERROR CMD";
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"✗ Error enviando RESTART: {ex.Message}");
                // BottomButtonText.Text = "ERROR CMD";
            }
        }
    }
}
