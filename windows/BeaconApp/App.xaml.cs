using System;
using System.Windows;

namespace BeaconApp
{
    /// <summary>
    /// Clase principal de la aplicación
    /// </summary>
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Manejar excepciones no controladas
            AppDomain.CurrentDomain.UnhandledException += OnUnhandledException;
            DispatcherUnhandledException += OnDispatcherUnhandledException;

            Console.WriteLine("========================================");
            Console.WriteLine("GeoRacing - Sistema de Baliza");
            Console.WriteLine($"Iniciado: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            Console.WriteLine("========================================");
        }

        private void OnUnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            var ex = e.ExceptionObject as Exception;
            Console.Error.WriteLine($"[FATAL] Excepción no controlada: {ex?.Message}");
            Console.Error.WriteLine(ex?.StackTrace);

            MessageBox.Show(
                $"Error crítico:\n\n{ex?.Message}\n\nLa aplicación se cerrará.",
                "Error Fatal",
                MessageBoxButton.OK,
                MessageBoxImage.Error
            );
        }

        private void OnDispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
        {
            Console.Error.WriteLine($"[ERROR] Excepción en UI: {e.Exception.Message}");
            Console.Error.WriteLine(e.Exception.StackTrace);

            MessageBox.Show(
                $"Error en la interfaz:\n\n{e.Exception.Message}",
                "Error",
                MessageBoxButton.OK,
                MessageBoxImage.Error
            );

            e.Handled = true;
        }

        protected override void OnExit(ExitEventArgs e)
        {
            Console.WriteLine("[App] Aplicación cerrada");
            base.OnExit(e);
        }
    }
}
