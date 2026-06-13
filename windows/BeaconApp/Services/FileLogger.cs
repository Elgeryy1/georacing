using System;
using System.IO;

namespace BeaconApp.Services
{
    public static class FileLogger
    {
        private static readonly string LogPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "beacon_log.txt");
        private static readonly object _lock = new object();

        // La baliza corre 24/7: sin recorte, beacon_log.txt crecería sin límite.
        // Cuando supera MaxLogBytes lo rotamos a un único .old (1 generación) y empezamos limpio.
        private const long MaxLogBytes = 5 * 1024 * 1024; // 5 MB

        public static void Log(string message)
        {
            try
            {
                lock (_lock)
                {
                    TrimLogIfNeeded();

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

        /// <summary>
        /// Rota el log a un .old si supera el tamaño máximo. Debe llamarse bajo _lock.
        /// </summary>
        private static void TrimLogIfNeeded()
        {
            try
            {
                var info = new FileInfo(LogPath);
                if (info.Exists && info.Length > MaxLogBytes)
                {
                    string oldPath = LogPath + ".old";
                    if (File.Exists(oldPath)) File.Delete(oldPath);
                    File.Move(LogPath, oldPath);
                }
            }
            catch (Exception)
            {
                // Si la rotación falla, seguimos escribiendo en el log actual.
            }
        }

        public static void LogError(string context, Exception ex)
        {
            Log($"[ERROR] {context}: {ex.Message}\nStack Trace: {ex.StackTrace}");
        }
    }
}
