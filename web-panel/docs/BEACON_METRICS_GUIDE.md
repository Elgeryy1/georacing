# Actualización de Balizas con Métricas del Sistema

Las balizas ahora deben reportar métricas adicionales del sistema cuando envíen el heartbeat.

## Campos Adicionales en Firestore

```typescript
{
  // ... campos existentes
  battery: 100.0,              // Porcentaje de batería (0-100)
  voltage: 3.4,                // Voltaje en voltios
  signalStrength: 99.5779,     // Fuerza de señal WiFi/Red (0-100)
  connections: 56,             // Número de conexiones activas
  temperature: 23.5,           // Temperatura en °C
  power: +4                    // Potencia en dBm
}
```

## Actualización del Cliente Baliza

### JavaScript/TypeScript

```javascript
async function sendHeartbeat() {
  try {
    await updateDoc(beaconRef, {
      lastSeen: serverTimestamp(),
      online: true,
      // Métricas del sistema
      battery: await getBatteryLevel(),        // Implementar según hardware
      voltage: await getVoltage(),             // Implementar según hardware
      signalStrength: await getSignalStrength(), // WiFi signal
      connections: await getActiveConnections(),
      temperature: await getTemperature(),     // CPU/Sensor temperatura
      power: await getTransmitPower()          // WiFi/Red potencia
    });
  } catch (error) {
    console.error("Error en heartbeat:", error);
  }
}
```

### Python

```python
def send_heartbeat():
    try:
        beacon_ref.update({
            'lastSeen': firestore.SERVER_TIMESTAMP,
            'online': True,
            'battery': get_battery_level(),
            'voltage': get_voltage(),
            'signalStrength': get_signal_strength(),
            'connections': get_active_connections(),
            'temperature': get_temperature(),
            'power': get_transmit_power()
        })
    except Exception as e:
        print(f"Error en heartbeat: {e}")
```

## Implementaciones de Ejemplo

### Windows (C#)

```csharp
// Batería (para laptops/tablets con batería)
var batteryStatus = SystemInformation.PowerStatus;
double battery = batteryStatus.BatteryLifePercent * 100;

// Temperatura CPU (requiere librería como OpenHardwareMonitor)
// Aproximación simple:
var searcher = new ManagementObjectSearcher(@"root\WMI", 
    "SELECT * FROM MSAcpi_ThermalZoneTemperature");
foreach (var obj in searcher.Get()) {
    var temp = Convert.ToDouble(obj["CurrentTemperature"]);
    double celsius = (temp / 10.0) - 273.15;
}

// Señal WiFi
var wlan = new WlanClient();
var connection = wlan.Interfaces[0].CurrentConnection;
double signalQuality = connection.wlanAssociationAttributes.wlanSignalQuality;
```

### Linux/Raspberry Pi (Python)

```python
import psutil
import subprocess

def get_battery_level():
    """Obtener nivel de batería"""
    battery = psutil.sensors_battery()
    if battery:
        return battery.percent
    return 100.0  # Si no hay batería, reportar 100%

def get_temperature():
    """Temperatura del CPU"""
    temps = psutil.sensors_temperatures()
    if 'cpu_thermal' in temps:
        return temps['cpu_thermal'][0].current
    return 0.0

def get_signal_strength():
    """Señal WiFi en porcentaje"""
    try:
        result = subprocess.check_output(['iwconfig', 'wlan0'])
        # Parsear resultado para obtener signal level
        # Convertir de dBm a porcentaje
        return 75.0  # Placeholder
    except:
        return 0.0

def get_active_connections():
    """Número de conexiones de red"""
    return len(psutil.net_connections())

def get_voltage():
    """Voltaje (si hay sensor disponible)"""
    return 3.3  # Placeholder

def get_transmit_power():
    """Potencia de transmisión en dBm"""
    try:
        result = subprocess.check_output(['iw', 'dev', 'wlan0', 'info'])
        # Parsear resultado
        return 0  # Placeholder
    except:
        return 0
```

### Windows PowerShell (para scripts simples)

```powershell
# Batería
$battery = (Get-WmiObject Win32_Battery).EstimatedChargeRemaining

# WiFi Signal
$signal = (netsh wlan show interfaces | Select-String "Signal").ToString().Split(":")[1].Trim().TrimEnd("%")

# Conexiones activas
$connections = (Get-NetTCPConnection | Where-Object {$_.State -eq "Established"}).Count

# CPU Temperatura (requiere herramientas adicionales)
# Temperatura aproximada por uso de CPU
$cpuLoad = (Get-WmiObject Win32_Processor).LoadPercentage
$approxTemp = 25 + ($cpuLoad * 0.5)  # Aproximación simple
```

## Visualización en el Panel

El panel ahora muestra estas métricas en tarjetas individuales:

- ✅ Batería con icono de nivel y color según porcentaje
- ✅ Voltaje en voltios
- ✅ Señal WiFi con barras visuales
- ✅ Número de conexiones activas
- ✅ Temperatura con alerta si >50°C
- ✅ Tiempo desde última actualización
- ✅ Potencia de transmisión en dBm

## Alertas Automáticas (Futuro)

Se pueden configurar alertas cuando:
- Batería < 20%
- Temperatura > 60°C
- Señal < 25%
- Sin heartbeat por >2 minutos
