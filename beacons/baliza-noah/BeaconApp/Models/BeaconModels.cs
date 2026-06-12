using System;
using System.Text.Json.Serialization;
using System.Text.Json;

namespace BeaconApp.Models
{
    /// <summary>
    /// Modelo para el payload de heartbeat (POST /api/beacons/heartbeat)
    /// </summary>
    public class BeaconHeartbeatRequest
    {
        [JsonPropertyName("beacon_uid")]
        public string BeaconUid { get; set; } = string.Empty;

        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("description")]
        public string Description { get; set; } = string.Empty;

        [JsonPropertyName("zone_id")]
        public int ZoneId { get; set; }

        [JsonPropertyName("latitude")]
        public double Latitude { get; set; }

        [JsonPropertyName("longitude")]
        public double Longitude { get; set; }

        [JsonPropertyName("has_screen")]
        public int HasScreen { get; set; } = 1; // 1 = true, 0 = false

        [JsonPropertyName("mode")]
        public string Mode { get; set; } = "NORMAL";

        [JsonPropertyName("arrow_direction")]
        public string ArrowDirection { get; set; } = "NONE";

        [JsonPropertyName("message")]
        public string Message { get; set; } = string.Empty;

        [JsonPropertyName("color")]
        public string Color { get; set; } = "#00FF00";

        [JsonPropertyName("brightness")]
        public int Brightness { get; set; } = 100;

        [JsonPropertyName("battery_level")]
        public int BatteryLevel { get; set; } = 100;
    }

    /// <summary>
    /// Modelo para comandos recibidos desde la API (GET /api/commands/pending/{beaconUid})
    /// </summary>
    public class BeaconCommandDto
    {
        [JsonPropertyName("id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("beacon_uid")]
        public string BeaconUid { get; set; } = string.Empty;

        [JsonPropertyName("command")]
        public string Command { get; set; } = string.Empty;

        [JsonPropertyName("value")]
        public string Value { get; set; } = string.Empty;

        [JsonPropertyName("status")]
        public string Status { get; set; } = "PENDING";

        [JsonPropertyName("created_at")]
        [JsonConverter(typeof(CustomDateTimeConverter))]
        public DateTime CreatedAt { get; set; }

        [JsonPropertyName("executed_at")]
        [JsonConverter(typeof(CustomDateTimeConverter))]
        public DateTime? ExecutedAt { get; set; }
    }

    /// <summary>
    /// Modelo para deserializar el JSON de Value cuando el comando es UPDATE_CONFIG
    /// </summary>
    public class BeaconConfigUpdate
    {
        [JsonPropertyName("mode")]
        public string? Mode { get; set; }

        [JsonPropertyName("arrow")]
        public string? Arrow { get; set; }

        [JsonPropertyName("message")]
        public string? Message { get; set; }

        [JsonPropertyName("color")]
        public string? Color { get; set; }

        [JsonPropertyName("brightness")]
        public int? Brightness { get; set; }

        [JsonPropertyName("evacuation_exit")]
        public string? EvacuationExit { get; set; }

        [JsonPropertyName("zone")]
        public string? Zone { get; set; }
    }

    /// <summary>
    /// Modelo para el estado global del circuito (tabla circuit_state)
    /// </summary>
    public class CircuitState
    {
        [JsonPropertyName("global_mode")]
        public string GlobalMode { get; set; } = "NORMAL";

        [JsonPropertyName("temperature")]
        public double? Temperature { get; set; }

        [JsonPropertyName("message")]
        public string? Message { get; set; }

        [JsonPropertyName("evacuation_route")]
        public string? EvacuationRoute { get; set; }
    }

    public class CustomDateTimeConverter : JsonConverter<DateTime>
    {
        public override DateTime Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            var str = reader.GetString();
            if (string.IsNullOrEmpty(str)) return DateTime.MinValue;

            // Intentar formato con espacio (DB) usando InvariantCulture para evitar errores de locales (ej: ES vs EN)
            if (DateTime.TryParse(str, System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.None, out var dt))
            {
                if (dt.Kind == DateTimeKind.Unspecified)
                {
                    return DateTime.SpecifyKind(dt, DateTimeKind.Utc);
                }
                return dt;
            }
            
            return DateTime.MinValue;
        }

        public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions options)
        {
            writer.WriteStringValue(value.ToString("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
