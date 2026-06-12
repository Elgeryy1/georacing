using System;
using System.IO;

namespace BeaconApp.Services
{
    public static class FileLogger
    {
        private static readonly string LogPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "beacon_log.txt");
        private static readonly object _lock = new object();

        public static void Log(string message)
        {
            try
            {
                lock (_lock)
                {
                    string timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
                    string logLine = $"{timestamp} {message}{Environment.NewLine}";
                    File.AppendAllText(LogPath, logLine);
                    
                    // También escribir en consola para debug en IDE
                    Console.Write(logLine);
                }
            }
            catch (Exception)
            {
                // Si falla el log, no podemos hacer mucho más, pero evitamos que tumbe la app
            }
        }

        public static void LogError(string context, Exception ex)
        {
            Log($"[ERROR] {context}: {ex.Message}\nStack Trace: {ex.StackTrace}");
        }
    }
}
