using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading.Tasks;
using BeaconApp.Models;

namespace BeaconApp.Services
{
    /// <summary>
    /// Cliente HTTP para comunicarse con la API de GeoRacing
    /// </summary>
    public class ApiClient
    {
        private readonly HttpClient _httpClient;
        private readonly string _baseUrl;

        public ApiClient(string baseUrl)
        {
            // Asegurar que termine en / para que HttpClient resuelva bien las rutas relativas
            _baseUrl = baseUrl.TrimEnd('/') + "/";
            
            // Permitir certificados autosignados (HTTPS)
            var handler = new HttpClientHandler
            {
                ServerCertificateCustomValidationCallback = (message, cert, chain, errors) => true
            };

            _httpClient = new HttpClient(handler)
            {
                BaseAddress = new Uri(_baseUrl),
                Timeout = TimeSpan.FromSeconds(10)
            };

            Log($"Cliente API inicializado: {_baseUrl} (SSL ignorado)");
        }

        /// <summary>
        /// Comprueba si la API responde
        /// GET health
        /// </summary>
        public async Task<bool> CheckHealthAsync()
        {
            try
            {
                var response = await _httpClient.GetAsync("health");
                return response.IsSuccessStatusCode;
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Error en Healthcheck", ex);
                return false;
            }
        }

        /// <summary>
        /// Envía heartbeat/registro de la baliza
        /// POST beacons/heartbeat
        /// </summary>
        /// <summary>
        /// Envía heartbeat/registro de la baliza
        /// POST beacons/heartbeat
        /// </summary>
        public async Task SendHeartbeatAsync(BeaconHeartbeatRequest request)
        {
            try
            {
                // 1. Intentar endpoint estándar de heartbeat (lógica de servidor, online status, etc.)
                var response = await _httpClient.PostAsJsonAsync("beacons/heartbeat", request);

                if (response.IsSuccessStatusCode)
                {
                    Log($"✓ Heartbeat enviado ({request.Mode})");
                }
                else
                {
                    Log($"⚠ Error en heartbeat: {response.StatusCode}");
                }

                // 2. FORZAR persistencia de batería y datos vía _upsert
                // Esto asegura que la columna 'battery_level' se rellene aunque el endpoint heartbeat la ignore
                var dbPayload = new 
                { 
                    beacon_uid = request.BeaconUid,
                    battery_level = request.BatteryLevel,
                    // También aseguramos otros datos críticos por si acaso
                    mode = request.Mode,
                    arrow_direction = request.ArrowDirection,
                    last_heartbeat = DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss")
                };
                
                await UpsertAsync("beacons", dbPayload);
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Excepción en heartbeat", ex);
                throw; // Re-lanzar para manejo superior si es necesario
            }
        }

        /// <summary>
        /// Obtiene los comandos pendientes para esta baliza
        /// GET commands/pending/{beaconUid}
        /// </summary>
        public async Task<List<BeaconCommandDto>> GetPendingCommandsAsync(string beaconUid)
        {
            try
            {
                var response = await _httpClient.GetAsync($"commands/pending/{Uri.EscapeDataString(beaconUid)}");

                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"[API] Error polling commands: {response.StatusCode}");
                    return new List<BeaconCommandDto>();
                }

                var content = await response.Content.ReadAsStringAsync();
                
                if (string.IsNullOrWhiteSpace(content) || content.Trim() == "null")
                {
                    return new List<BeaconCommandDto>();
                }

                Console.WriteLine($"[API] Raw commands: {content}"); // Uncomment for deep debug

                var commands = JsonSerializer.Deserialize<List<BeaconCommandDto>>(content);
                return commands ?? new List<BeaconCommandDto>();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[API] Exception polling commands: {ex.Message}");
                FileLogger.LogError("Error al obtener comandos", ex);
                return new List<BeaconCommandDto>();
            }
        }
        
        /// <summary>
        /// Obtiene la configuración actual de la baliza
        /// GET beacons?beacon_uid={uid}
        /// </summary>
        public async Task<BeaconConfigUpdate?> GetBeaconConfigAsync(string beaconUid)
        {
            try
            {
                // Usamos _get para buscar por beacon_uid
                var payload = new { table = "beacons", where = new { beacon_uid = beaconUid } };
                var response = await _httpClient.PostAsJsonAsync("_get", payload);

                if (!response.IsSuccessStatusCode) return null;

                var content = await response.Content.ReadAsStringAsync();
                var beacons = JsonSerializer.Deserialize<List<JsonElement>>(content);
                
                if (beacons != null && beacons.Count > 0)
                {
                    var b = beacons[0];
                    // Mapear respuesta dinámica a BeaconConfigUpdate
                    return new BeaconConfigUpdate
                    {
                        Mode = GetString(b, "mode"),
                        Arrow = GetString(b, "arrow") ?? GetString(b, "arrow_direction"),
                        Message = GetString(b, "message"),
                        Color = GetString(b, "color"),
                        Brightness = GetInt(b, "brightness"),
                        Zone = GetString(b, "zone"),
                        EvacuationExit = GetString(b, "evacuation_exit")
                    };
                }
                return null;
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Error obteniendo config de baliza", ex);
                return null;
            }
        }
        
        /// <summary>
        /// Obtiene el estado global del circuito
        /// GET circuit_state?id=1 aka _get
        /// </summary>
        public async Task<string?> GetGlobalModeAsync()
        {
            var state = await GetCircuitStateAsync();
            return state?.GlobalMode;
        }

        /// <summary>
        /// Obtiene el estado completo del circuito incluyendo modo y temperatura
        /// GET circuit_state?id=1 aka _get
        /// </summary>
        public async Task<CircuitState?> GetCircuitStateAsync()
        {
            try
            {
                var payload = new { table = "circuit_state", where = new { id = "1" } };
                var response = await _httpClient.PostAsJsonAsync("_get", payload);

                if (!response.IsSuccessStatusCode) return null;

                var content = await response.Content.ReadAsStringAsync();
                var states = JsonSerializer.Deserialize<List<JsonElement>>(content);
                
                if (states != null && states.Count > 0)
                {
                    var row = states[0];
                    return new CircuitState
                    {
                        GlobalMode = GetString(row, "global_mode") ?? "NORMAL",
                        Temperature = GetDouble(row, "temperature"),
                        Message = GetString(row, "message"),
                        EvacuationRoute = GetString(row, "evacuation_route")
                    };
                }
                return null;
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Error obteniendo estado del circuito", ex);
                return null;
            }
        }
    
        private string? GetString(JsonElement element, string property)
        {
            if (element.TryGetProperty(property, out var prop) && prop.ValueKind == JsonValueKind.String)
                return prop.GetString();
            return null;
        }

        private int? GetInt(JsonElement element, string property)
        {
            if (element.TryGetProperty(property, out var prop) && prop.ValueKind == JsonValueKind.Number)
                return prop.GetInt32();
            return null;
        }

        private double? GetDouble(JsonElement element, string property)
        {
            if (element.TryGetProperty(property, out var prop))
            {
                if (prop.ValueKind == JsonValueKind.Number)
                    return prop.GetDouble();
                // Handle string temperature like "11.3°C"
                if (prop.ValueKind == JsonValueKind.String)
                {
                    var str = prop.GetString();
                    if (str != null && double.TryParse(str.Replace("°C", "").Trim(), 
                        System.Globalization.NumberStyles.Any, 
                        System.Globalization.CultureInfo.InvariantCulture, out var result))
                        return result;
                }
            }
            return null;
        }

        /// <summary>
        /// Marca un comando como ejecutado
        /// POST commands/{id}/execute
        /// </summary>
        /// <summary>
        /// Inserta o actualiza un registro en una tabla dinámica
        /// POST /_upsert
        /// </summary>
        public async Task UpsertAsync(string table, object data)
        {
            try
            {
                var payload = new { table, data };
                var response = await _httpClient.PostAsJsonAsync("_upsert", payload);
                
                if (!response.IsSuccessStatusCode)
                {
                    Log($"⚠ Error en _upsert ({table}): {response.StatusCode}");
                }
            }
            catch (Exception ex)
            {
                FileLogger.LogError($"Error en _upsert ({table})", ex);
            }
        }

        /// <summary>
        /// Elimina un registro de una tabla dinámica
        /// POST /_delete
        /// </summary>
        public async Task DeleteAsync(string table, object where)
        {
            try
            {
                var payload = new { table, where };
                var response = await _httpClient.PostAsJsonAsync("_delete", payload);

                if (!response.IsSuccessStatusCode)
                {
                    Log($"⚠ Error en _delete ({table}): {response.StatusCode}");
                }
            }
            catch (Exception ex)
            {
                FileLogger.LogError($"Error en _delete ({table})", ex);
            }
        }

        /// <summary>
        /// Asegura que exista una tabla dinámica
        /// POST /_ensure_table
        /// </summary>
        public async Task EnsureTableAsync(string table)
        {
            try
            {
                var payload = new { table };
                await _httpClient.PostAsJsonAsync("_ensure_table", payload);
            }
            catch (Exception ex)
            {
                FileLogger.LogError($"Error en _ensure_table ({table})", ex);
            }
        }

        /// <summary>
        /// Asegura que exista una columna en una tabla dinámica
        /// POST /_ensure_column
        /// </summary>
        public async Task EnsureColumnAsync(string table, string column, string type)
        {
            try
            {
                var payload = new { table, column, type };
                await _httpClient.PostAsJsonAsync("_ensure_column", payload);
            }
            catch (Exception ex)
            {
                FileLogger.LogError($"Error en _ensure_column ({table}.{column})", ex);
            }
        }

        public async Task ExecuteCommandAsync(string commandId)
        {
            try
            {
                var response = await _httpClient.PostAsync($"commands/{commandId}/execute", null);

                if (response.IsSuccessStatusCode)
                {
                    Log($"✓ Comando {commandId} marcado como EJECUTADO");
                }
                else
                {
                    Log($"⚠ Error al marcar comando {commandId}: {response.StatusCode}");
                }
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Error al marcar comando", ex);
            }
        }

        public async Task<bool> CreateCommandAsync(string beaconUid, string command, string? value = null)
        {
            try
            {
                var payload = new
                {
                    beacon_uid = beaconUid,
                    command = command,
                    value = value ?? "",
                    status = "PENDING"
                };
                
                // Intentamos usar el endpoint estándar REST
                var response = await _httpClient.PostAsJsonAsync("commands", payload);
                
                if (!response.IsSuccessStatusCode)
                {
                    Log($"⚠ Error enviando comando: {response.StatusCode}");
                    
                    // Fallback: Si falla, intentamos por _upsert (por si acaso)
                    if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
                    {
                        Log("⚠ Reintentando con _upsert...");
                        var upsertPayload = new
                        {
                            id = Guid.NewGuid().ToString(), // Generamos ID si usamos upsert
                            beacon_uid = beaconUid,
                            command = command,
                            value = value ?? "",
                            status = "PENDING",
                            created_at = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")
                        };
                        await UpsertAsync("commands", upsertPayload);
                        return true; // Asumimos éxito si no lanza excepción
                    }
                }
                
                return response.IsSuccessStatusCode;
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Error sending command", ex);
                return false;
            }
        }

        // Removed Dispose to prevent HttpClient from being disposed prematurely by DI container
        // public void Dispose()
        // {
        //     _httpClient?.Dispose();
        // }

        private void Log(string message)
        {
            FileLogger.Log($"[API] {message}");
        }
    }
}
