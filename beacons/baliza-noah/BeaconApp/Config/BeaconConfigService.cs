using System;
using System.IO;
using System.Text.Json;

namespace BeaconApp.Config
{
    /// <summary>
    /// Configuración de la baliza cargada desde beacon.json
    /// </summary>
    public record BeaconConfig(
        string BeaconId, 
        string ApiBaseUrl,
        string Name,
        string Description,
        int ZoneId,
        double Latitude,
        double Longitude
    );

    /// <summary>
    /// Servicio para leer/crear la configuración local de la baliza
    /// </summary>
    public static class BeaconConfigService
    {
        private const string CONFIG_DIR = @"C:\ProgramData\GeoRacing";
        private const string CONFIG_FILE = "beacon.json";
        private static readonly string CONFIG_PATH = Path.Combine(CONFIG_DIR, CONFIG_FILE);

        /// <summary>
        /// Lee o crea la configuración de la baliza
        /// </summary>
        public static BeaconConfig ReadOrCreateConfig()
        {
            try
            {
                // Intentar leer el archivo existente
                if (File.Exists(CONFIG_PATH))
                {
                    var json = File.ReadAllText(CONFIG_PATH);
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var configJson = JsonSerializer.Deserialize<ConfigJson>(json, options);

                    if (configJson != null && 
                        !string.IsNullOrWhiteSpace(configJson.beaconId) &&
                        !string.IsNullOrWhiteSpace(configJson.apiBaseUrl))
                    {
                        Log($"✓ Configuración cargada: {configJson.beaconId}");

                        // AUTO-UPDATE: Si es la URL vieja, actualizar a la nueva HTTPS
                        if (configJson.apiBaseUrl == "http://192.168.1.99:4000")
                        {
                            Log("⚠ Detectada configuración antigua. Actualizando a HTTPS...");
                            configJson.apiBaseUrl = "https://alpo.myqnapcloud.com:4010/api/";
                            
                            // Guardar cambios
                            SaveConfig(configJson);
                            Log($"✓ Configuración actualizada a: {configJson.apiBaseUrl}");
                        }

                        return new BeaconConfig(
                            configJson.beaconId, 
                            configJson.apiBaseUrl,
                            configJson.name ?? "Baliza Sin Nombre",
                            configJson.description ?? "Sin descripción",
                            configJson.zoneId,
                            configJson.latitude,
                            configJson.longitude
                        );
                    }

                    Log("⚠ Archivo de configuración corrupto, regenerando...");
                    BackupCorruptedFile();
                }

                // Crear configuración por defecto
                return CreateDefaultConfig();
            }
            catch (Exception ex)
            {
                Log($"✗ Error al leer config: {ex.Message}");
                
                try
                {
                    BackupCorruptedFile();
                    return CreateDefaultConfig();
                }
                catch (Exception ex2)
                {
                    Log($"✗ Error crítico: {ex2.Message}");
                    throw;
                }
            }
        }

        private static BeaconConfig CreateDefaultConfig()
        {
            // Crear directorio si no existe
            if (!Directory.Exists(CONFIG_DIR))
            {
                Directory.CreateDirectory(CONFIG_DIR);
                Log($"✓ Directorio creado: {CONFIG_DIR}");
            }

            // Generar valores por defecto
            var beaconId = Environment.MachineName.Trim();
            var apiBaseUrl = "https://alpo.myqnapcloud.com:4010/api/";

            var configJson = new ConfigJson
            {
                beaconId = beaconId,
                apiBaseUrl = apiBaseUrl,
                name = beaconId,
                description = "Baliza autogenerada",
                zoneId = 1,
                latitude = 41.57,
                longitude = 2.26
            };

            // Guardar a disco
            SaveConfig(configJson);

            Log($"✓ Configuración creada: {beaconId} → {apiBaseUrl}");

            return new BeaconConfig(
                configJson.beaconId, 
                configJson.apiBaseUrl,
                configJson.name,
                configJson.description,
                configJson.zoneId,
                configJson.latitude,
                configJson.longitude
            );
        }

        private static void SaveConfig(ConfigJson config)
        {
            var options = new JsonSerializerOptions { WriteIndented = true };
            var json = JsonSerializer.Serialize(config, options);
            File.WriteAllText(CONFIG_PATH, json);
        }

        private static void BackupCorruptedFile()
        {
            if (File.Exists(CONFIG_PATH))
            {
                var timestamp = DateTime.Now.ToString("yyyyMMdd-HHmmss");
                var backupPath = Path.Combine(CONFIG_DIR, $"beacon.json.bak-{timestamp}");
                
                try
                {
                    File.Copy(CONFIG_PATH, backupPath, true);
                    File.Delete(CONFIG_PATH);
                    Log($"✓ Backup creado: {backupPath}");
                }
                catch (Exception ex)
                {
                    Log($"⚠ No se pudo crear backup: {ex.Message}");
                }
            }
        }

        private static void Log(string message)
        {
            var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
            var logMessage = $"{timestamp} [CONFIG] {message}";
            Console.WriteLine(logMessage);

            try
            {
                var logPath = Path.Combine(CONFIG_DIR, "beacon-debug.log");
                File.AppendAllText(logPath, logMessage + Environment.NewLine);
            }
            catch
            {
                // Ignorar errores de logging
            }
        }

        // Clase interna para serialización JSON (camelCase)
        private class ConfigJson
        {
            public string beaconId { get; set; } = string.Empty;
            public string apiBaseUrl { get; set; } = string.Empty;
            public string? name { get; set; }
            public string? description { get; set; }
            public int zoneId { get; set; }
            public double latitude { get; set; }
            public double longitude { get; set; }
        }
    }
}
