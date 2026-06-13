# 🏁 GeoRacing - Beacon de Windows

Aplicación de escritorio para Windows (WPF .NET 8) que funciona como terminal de señalización inteligente para circuitos de carreras.

## 📋 Características

- **Pantalla completa** (modo kiosco, borderless y `Topmost`)
- **Auto-configuración** desde `C:\ProgramData\GeoRacing\beacon.json`
- **Comunicación con API REST** para recibir comandos y enviar telemetría (heartbeat con batería)
- **Difusión BLE** del estado del circuito vía `BluetoothLEAdvertisementPublisher` (Manufacturer ID `0x1234`)
- **Modos de operación** (color de fondo asociado en `MainViewModel`):
  - `UNCONFIGURED` - Sin configurar (azul `#1565C0`)
  - `NORMAL` - Operación normal (verde `#2E7D32`)
  - `CONGESTION` - Advertencia de congestión (naranja `#F57C00`)
  - `EMERGENCY` - Emergencia (rojo `#C62828`)
  - `EVACUATION` - Evacuación (rojo `#D32F2F`, fuerza flecha direccional)
  - `MAINTENANCE` - Mantenimiento (morado `#7B1FA2`)
- **Flecha direccional** con 8 orientaciones (incluye diagonales) para guiar evacuaciones

## 🔧 Requisitos

- Windows 10/11 (build 19041 o superior; usa APIs WinRT de Bluetooth)
- .NET 8 SDK
- Adaptador Bluetooth LE para la difusión del beacon
- Acceso a la API REST de GeoRacing (por defecto: `https://georacing.example.com:4010/api/`)

## 🚀 Instalación y Ejecución

### 1. Compilar y ejecutar en desarrollo

```powershell
cd BeaconApp
dotnet restore
dotnet build
dotnet run
```

### 2. Compilar para producción

```powershell
dotnet publish -c Release -r win-x64 --self-contained false -o publish
```

Los archivos estarán en `BeaconApp\publish\`

### 3. Ejecutar el binario

```powershell
.\publish\GeoRacingBeacon.exe
```

## ⚙️ Configuración

### Archivo `beacon.json`

Ubicación: `C:\ProgramData\GeoRacing\beacon.json`

```json
{
  "beaconId": "BEACON-1",
  "apiBaseUrl": "https://georacing.example.com:4010/api/",
  "name": "Beacon Paddock A",
  "description": "Mini-PC trackside",
  "zoneId": 1,
  "latitude": 41.57,
  "longitude": 2.26
}
```

**Comportamiento al iniciar** (`BeaconConfigService.ReadOrCreateConfig`):

- Si el archivo **NO existe**: Se crea automáticamente con:
  - `beaconId` = Nombre del PC (`Environment.MachineName`)
  - `apiBaseUrl` = URL HTTPS por defecto del backend

- Si el archivo **existe**: Se lee y usa su configuración. Si contiene una URL
  de desarrollo antigua, se migra automáticamente a la URL HTTPS actual y se
  reescribe el archivo (la migración tolera fallos de escritura: si no puede
  persistir, sigue usando la URL en memoria sin perder la identidad del beacon).

- Si el archivo está **corrupto**: Se hace un backup con marca de tiempo y se regenera.

## 🌐 Comunicación con API

Todas las rutas son relativas a `apiBaseUrl`. El cliente acepta certificados
autofirmados (servidor de desarrollo) — comportamiento pensado solo para
entornos de desarrollo.

### Endpoints utilizados

#### 1. Heartbeat / Registro
```
POST beacons/heartbeat
```
Cuerpo `BeaconHeartbeatRequest` (incluye `mode`, `arrow_direction`, `brightness`,
`battery_level`, posición GPS y `zone_id`). Se envía cada **10 segundos**.
Adicionalmente persiste batería y estado vía `POST _upsert` sobre la tabla `beacons`.

#### 2. Estado global del circuito
```
POST _get   { "table": "circuit_state", "where": { "id": "1" } }
```
Se consulta en cada tick de polling (**cada 300 ms**) y alimenta tanto la UI
como el payload BLE (modo + temperatura).

#### 3. Comandos pendientes
```
GET commands/pending/{beaconId}
```
Se consulta en cada tick de polling (**cada 300 ms**). Los comandos con más de
60 minutos de antigüedad (comparando contra `DateTime.UtcNow`) se descartan.
Tras procesarse, el comando se elimina con `POST _delete`.

Comandos soportados: `UPDATE_CONFIG`, `RESTART`, `SHUTDOWN`, `CLOSE` / `CLOSE_APP`.

## 🎨 Modos de Visualización

| Modo | Color | Descripción |
|------|-------|-------------|
| **UNCONFIGURED** | Azul `#1565C0` | Beacon sin configurar |
| **NORMAL** | Verde `#2E7D32` | Operación normal del circuito |
| **CONGESTION** | Naranja `#F57C00` | Advertencia de congestión |
| **EMERGENCY** | Rojo `#C62828` | Emergencia |
| **EVACUATION** | Rojo `#D32F2F` | Evacuación (fuerza flecha de salida) |
| **MAINTENANCE** | Morado `#7B1FA2` | Mantenimiento / pit |

### Flechas direccionales

La flecha rota según `arrow_direction`. Si vale `NONE`, se oculta (`Collapsed`).

- `NONE` - Sin flecha (oculta)
- `FORWARD` / `UP` - ⬆ (0°)
- `RIGHT` - ➡ (90°)
- `BACKWARD` / `DOWN` - ⬇ (180°)
- `LEFT` - ⬅ (-90°)
- Diagonales: `FORWARD_LEFT`/`UP_LEFT` (-45°), `FORWARD_RIGHT`/`UP_RIGHT` (45°),
  `BACKWARD_LEFT`/`DOWN_LEFT` (-135°), `BACKWARD_RIGHT`/`DOWN_RIGHT` (135°)

## ⌨️ Controles

- **ESC** - Cerrar la aplicación

## 📂 Estructura del Proyecto

```
BeaconApp/
├── Config/
│   └── BeaconConfigService.cs     # Gestión de beacon.json (lectura/creación/migración)
├── Models/
│   └── BeaconModels.cs            # DTOs + CustomDateTimeConverter (UTC)
├── Services/
│   ├── ApiClient.cs               # Cliente HTTP para la API REST
│   ├── ApiLogger.cs               # Cola de logs hacia la API (flush cada 5 s)
│   ├── BleBeaconService.cs        # Difusión BLE del estado del circuito
│   └── FileLogger.cs              # Log a disco con rotación (5 MB)
├── ViewModels/
│   └── MainViewModel.cs           # MVVM: polling 300 ms + heartbeat 10 s
├── MainWindow.xaml                # Interfaz XAML (vistas por modo)
├── MainWindow.xaml.cs             # Code-behind
├── App.xaml / App.xaml.cs         # Bootstrap + handlers de excepciones
└── BeaconApp.csproj               # Proyecto .NET 8 (assembly: GeoRacingBeacon)
```

## 🔍 Logs

Los logs se guardan en:
```
C:\ProgramData\GeoRacing\beacon-debug.log
```

Formato:
```
2025-11-18 18:30:45 [CONFIG] ✓ Configuración cargada: BEACON-1
2025-11-18 18:30:45 [API] Cliente API inicializado: https://georacing.example.com:4010/api/
2025-11-18 18:30:45 [VM] ViewModel inicializado para beacon: BEACON-1
2025-11-18 18:30:55 [API] ✓ Heartbeat enviado (NORMAL)
2025-11-18 18:31:00 [CMD] Recibido: UPDATE_CONFIG (ID: 42)
```

> El log de aplicación (`beacon_log.txt`, junto al ejecutable) rota
> automáticamente a `beacon_log.txt.old` al superar 5 MB para no crecer sin límite.

## 🐛 Solución de Problemas

### El beacon no se conecta a la API

1. Verificar que la API responde: `GET {apiBaseUrl}health`
2. Revisar `beacon.json` y confirmar la URL correcta
3. Verificar conectividad de red con el servidor de la API
4. Revisar logs en `beacon-debug.log` y `beacon_log.txt`

### La configuración no cambia

1. Verificar que se están creando comandos en la API
2. Revisar logs para ver si se reciben comandos
3. Comprobar que los comandos se marcan como ejecutados

### Pantalla bloqueada en "SIN CONFIGURACIÓN"

1. Enviar un comando `UPDATE_CONFIG` desde el panel
2. Verificar que el `mode` en la base de datos no sea `NULL`
3. Reiniciar la aplicación

## 🔄 Actualización

Para actualizar la aplicación en producción:

1. Compilar nueva versión
2. Detener la aplicación en cada PC
3. Reemplazar ejecutables en `C:\Program Files\GeoRacing\`
4. Reiniciar aplicación

**Nota**: El archivo `beacon.json` se mantiene entre actualizaciones.

## 🚀 Inicio Automático (Windows)

Para que el beacon inicie automáticamente con Windows:

```powershell
$action = New-ScheduledTaskAction -Execute "C:\Program Files\GeoRacing\GeoRacingBeacon.exe"
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest

Register-ScheduledTask -TaskName "GeoRacing Beacon" -Action $action -Trigger $trigger -Principal $principal
```

## 📝 Notas Técnicas

- La aplicación usa un `HttpClient` reutilizable (con timeout de 10 s) para todas las peticiones.
- El timer de polling es **no reentrante**: usa periodo infinito y se re-arma en el
  bloque `finally` del callback, de modo que un tick lento nunca solapa con el siguiente.
- Las marcas de tiempo de los comandos se comparan en **UTC** (`DateTime.UtcNow`);
  el `CustomDateTimeConverter` normaliza las fechas entrantes a UTC para evitar
  errores por zona horaria o cultura local.
- El nivel de batería se interpreta correctamente en equipos **sin batería**
  (`BatteryLifePercent` devuelve `255` → se reporta `100`).
- Los cambios de configuración y de UI se aplican siempre en el `Dispatcher` de WPF.
- El payload BLE se reconstruye por completo en cada cambio de estado (WinRT no
  permite reutilizar de forma fiable un publisher tras `Stop()`).

## 📄 Licencia

Parte del sistema GeoRacing - Ver LICENSE en el directorio raíz del proyecto.

---

**¡Listo para carreras! 🏁**
