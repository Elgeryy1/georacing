# 📚 Documentación Técnica - GeoRacing Beacon App

**Versión:** 1.0  
**Plataforma:** Windows (.NET 8.0 + WPF)  
**Última actualización:** Febrero 2026

---

## 📁 Estructura del Proyecto

```
BeaconApp/
├── App.xaml                    # Definición de la aplicación WPF
├── App.xaml.cs                 # Punto de entrada + manejo de errores globales
├── MainWindow.xaml             # Interfaz de usuario (5 vistas de estado)
├── MainWindow.xaml.cs          # Code-behind de la ventana principal
├── BeaconApp.csproj            # Configuración del proyecto .NET
│
├── Config/
│   └── BeaconConfigService.cs  # Gestión de configuración local (beacon.json)
│
├── Models/
│   └── BeaconModels.cs         # DTOs para comunicación con API
│
├── ViewModels/
│   └── MainViewModel.cs        # Lógica principal (MVVM), polling, comandos
│
└── Services/
    ├── ApiClient.cs            # Cliente HTTP para comunicación con backend
    ├── ApiLogger.cs            # Logger que envía logs al servidor
    └── FileLogger.cs           # Logger local a disco
```

---

## 🏗️ Arquitectura General

```
┌─────────────────────────────────────────────────────────────────┐
│                      PANEL DE CONTROL (Web)                      │
│                  Envía comandos a la base de datos               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API REST (Backend)                       │
│              https://alpo.myqnapcloud.com:4010/api/             │
│  Endpoints: /beacons, /commands, /health, /_upsert, /_get       │
└─────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
            ┌──────────────┐        ┌──────────────┐
            │   BALIZA 1   │        │   BALIZA N   │
            │  (WPF App)   │        │  (WPF App)   │
            └──────────────┘        └──────────────┘
```

### Flujo de Datos

1. **Heartbeat (cada 10s):** La baliza envía su estado actual al servidor
2. **Polling (cada 300ms):** La baliza consulta:
   - Estado global del circuito (`circuit_state`)
   - Configuración individual (`beacons`)
   - Comandos pendientes (`commands`)
3. **Ejecución de comandos:** Al recibir un comando, lo ejecuta y lo elimina de la BD

---

## 📄 Archivos Detallados

### 1. `App.xaml` y `App.xaml.cs`

**Propósito:** Punto de entrada de la aplicación WPF.

```xml
<!-- App.xaml -->
<Application x:Class="BeaconApp.App"
             StartupUri="MainWindow.xaml">
```

**Funcionalidades en `App.xaml.cs`:**

| Método | Descripción |
|--------|-------------|
| `OnStartup()` | Inicializa la app y registra manejadores de excepciones |
| `OnUnhandledException()` | Captura errores fatales no controlados del dominio |
| `OnDispatcherUnhandledException()` | Captura errores de la UI (thread principal) |
| `OnExit()` | Limpieza al cerrar la aplicación |

**Manejo de Errores:**
- Muestra `MessageBox` con detalles del error
- Escribe en consola con stack trace completo
- Marca `e.Handled = true` para errores de UI (evita crash)

---

### 2. `MainWindow.xaml`

**Propósito:** Define la interfaz visual completa de la baliza.

#### Recursos y Estilos Definidos

| Recurso | Tipo | Descripción |
|---------|------|-------------|
| `CircuitRed` | Color | Rojo de emergencia (#E30613) |
| `RacingGreen` | Color | Verde normal (#00D26A) |
| `SafetyYellow` | Color | Amarillo de precaución (#FFED00) |
| `TechCyan` | Color | Cian de mantenimiento (#00AEEF) |
| `CarbonBlack` | Color | Negro base (#111111) |
| `CarbonFiberPattern` | DrawingBrush | Patrón de fibra de carbono para fondo |
| `ScanlinesPattern` | DrawingBrush | Efecto de líneas de escaneo retro |
| `RacingHeader` | Style | Texto grande estilo racing (Impact, 40px) |
| `HudText` | Style | Texto estilo HUD (Consolas, Bold, Cyan) |
| `ArrowPathStyle` | Style | Flecha direccional con rotaciones dinámicas |

#### Vistas por Modo

La ventana contiene **5 vistas exclusivas** que se muestran según `CurrentMode`:

| Vista | Modo | Color | Descripción |
|-------|------|-------|-------------|
| `ViewNormal` | NORMAL | Verde | Operación normal, flecha direccional |
| `ViewCongestion` | CONGESTION | Amarillo | Advertencia de tráfico, patrón de rayas |
| `ViewEmergency` | EMERGENCY | Rojo | Bandera roja, detener inmediatamente |
| `ViewEvacuation` | EVACUATION | Rojo intenso | Evacuación del circuito, grid de triángulos |
| `ViewMaintenance` | MAINTENANCE | Cyan | Telemetría del sistema, datos técnicos |

#### Direcciones de Flecha Soportadas

El estilo `ArrowPathStyle` usa `DataTrigger` para rotar la flecha:

| Valor de `CurrentArrow` | Rotación |
|------------------------|----------|
| FORWARD, UP | 0° |
| BACKWARD, DOWN | 180° |
| LEFT | -90° |
| RIGHT | 90° |
| FORWARD_LEFT, UP_LEFT | -45° |
| FORWARD_RIGHT, UP_RIGHT | 45° |
| BACKWARD_LEFT, DOWN_LEFT | -135° |
| BACKWARD_RIGHT, DOWN_RIGHT | 135° |

#### Propiedades de Ventana

```xml
WindowStyle="None"          <!-- Sin bordes de Windows -->
WindowState="Maximized"     <!-- Pantalla completa -->
ResizeMode="NoResize"       <!-- No redimensionable -->
Topmost="True"              <!-- Siempre encima -->
Background="Black"          <!-- Fondo negro base -->
```

---

### 3. `MainWindow.xaml.cs`

**Propósito:** Code-behind de la ventana principal.

#### Campos Privados

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `_viewModel` | MainViewModel? | ViewModel principal (MVVM) |
| `_apiClient` | ApiClient? | Cliente HTTP |
| `_clockTimer` | DispatcherTimer? | Timer para actualizar reloj cada segundo |
| `_commandTimer` | DispatcherTimer? | Timer legacy (no usado activamente) |
| `_beaconId` | string | ID único de la baliza |

#### Métodos Principales

| Método | Descripción |
|--------|-------------|
| `MainWindow()` | Constructor: inicializa UI y timer de reloj |
| `MainWindow_Loaded()` | Al cargar: lee config, crea ApiClient y ViewModel, inicia servicios |
| `MainWindow_Closing()` | Al cerrar: detiene ViewModel y limpia recursos |
| `UpdateClock()` | Actualiza el texto del reloj (HH:mm:ss) |
| `Window_KeyDown()` | Maneja ESC para cerrar la aplicación |
| `ExecuteSystemCommand()` | Ejecuta comandos de sistema (RESTART, SHUTDOWN, CLOSE, CLOSE_APP) |
| `RunShutdown()` | Ejecuta shutdown.exe con argumentos |

#### Comandos de Sistema Soportados

| Comando | Acción |
|---------|--------|
| RESTART | `shutdown.exe /r /t 3` (reinicia Windows en 3s) |
| SHUTDOWN | `shutdown.exe /s /t 3` (apaga Windows en 3s) |
| CLOSE | Igual que SHUTDOWN |
| CLOSE_APP | `Application.Current.Shutdown()` (cierra solo la app) |

---

### 4. `Config/BeaconConfigService.cs`

**Propósito:** Gestiona la configuración local de la baliza.

#### Ubicación del Archivo de Configuración

```
C:\ProgramData\GeoRacing\beacon.json
```

#### Estructura de `BeaconConfig` (Record)

| Propiedad | Tipo | Descripción |
|-----------|------|-------------|
| BeaconId | string | Identificador único (por defecto: nombre de máquina) |
| ApiBaseUrl | string | URL base de la API |
| Name | string | Nombre descriptivo |
| Description | string | Descripción de la baliza |
| ZoneId | int | ID de zona asignada |
| Latitude | double | Coordenada de latitud |
| Longitude | double | Coordenada de longitud |

#### Métodos

| Método | Descripción |
|--------|-------------|
| `ReadOrCreateConfig()` | Lee `beacon.json` o crea uno por defecto |
| `CreateDefaultConfig()` | Genera configuración con valores por defecto |
| `SaveConfig()` | Guarda configuración a disco en JSON |
| `BackupCorruptedFile()` | Hace backup de config corrupta antes de regenerar |

#### Auto-Actualización de URL

Si detecta la URL antigua `http://192.168.1.99:4000`, la actualiza automáticamente a `https://alpo.myqnapcloud.com:4010/api/`.

---

### 5. `Models/BeaconModels.cs`

**Propósito:** Define los DTOs (Data Transfer Objects) para la API.

#### `BeaconHeartbeatRequest`

Payload enviado en `POST /beacons/heartbeat`:

| Propiedad | JSON Key | Tipo | Valor por defecto |
|-----------|----------|------|-------------------|
| BeaconUid | beacon_uid | string | "" |
| Name | name | string | "" |
| Description | description | string | "" |
| ZoneId | zone_id | int | 0 |
| Latitude | latitude | double | 0 |
| Longitude | longitude | double | 0 |
| HasScreen | has_screen | int | 1 |
| Mode | mode | string | "NORMAL" |
| ArrowDirection | arrow_direction | string | "NONE" |
| Message | message | string | "" |
| Color | color | string | "#00FF00" |
| Brightness | brightness | int | 100 |
| BatteryLevel | battery_level | int | 100 |

#### `BeaconCommandDto`

Comando recibido de `GET /commands/pending/{beaconUid}`:

| Propiedad | JSON Key | Tipo |
|-----------|----------|------|
| Id | id | string |
| BeaconUid | beacon_uid | string |
| Command | command | string |
| Value | value | string |
| Status | status | string |
| CreatedAt | created_at | DateTime |
| ExecutedAt | executed_at | DateTime? |

#### `BeaconConfigUpdate`

Payload deserializado del campo `Value` cuando `Command = "UPDATE_CONFIG"`:

| Propiedad | JSON Key | Tipo |
|-----------|----------|------|
| Mode | mode | string? |
| Arrow | arrow | string? |
| Message | message | string? |
| Color | color | string? |
| Brightness | brightness | int? |
| EvacuationExit | evacuation_exit | string? |
| Zone | zone | string? |

#### `CustomDateTimeConverter`

Conversor JSON personalizado para fechas que:
- Parsea cualquier formato de fecha usando `InvariantCulture`
- Especifica `DateTimeKind.Utc` si no está definido
- Escribe en formato `yyyy-MM-dd HH:mm:ss`

---

### 6. `Services/ApiClient.cs`

**Propósito:** Cliente HTTP para todas las comunicaciones con el backend.

#### Constructor

```csharp
public ApiClient(string baseUrl)
```

- Normaliza URL para que termine en `/`
- Configura `HttpClientHandler` para ignorar certificados SSL autofirmados
- Timeout de 10 segundos

#### Métodos de API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `CheckHealthAsync()` | GET /health | Verifica si la API está online |
| `SendHeartbeatAsync()` | POST /beacons/heartbeat | Envía estado de la baliza |
| `GetPendingCommandsAsync()` | GET /commands/pending/{uid} | Obtiene comandos pendientes |
| `GetBeaconConfigAsync()` | POST /_get | Obtiene config de la baliza |
| `GetGlobalModeAsync()` | POST /_get | Obtiene estado global del circuito |
| `ExecuteCommandAsync()` | POST /commands/{id}/execute | Marca comando como ejecutado |
| `CreateCommandAsync()` | POST /commands | Crea un nuevo comando |
| `UpsertAsync()` | POST /_upsert | Inserta/actualiza registro |
| `DeleteAsync()` | POST /_delete | Elimina registro |
| `EnsureTableAsync()` | POST /_ensure_table | Asegura existencia de tabla |
| `EnsureColumnAsync()` | POST /_ensure_column | Asegura existencia de columna |

#### Helpers Privados

| Método | Descripción |
|--------|-------------|
| `GetString()` | Extrae string de JsonElement |
| `GetInt()` | Extrae int de JsonElement |
| `Log()` | Escribe en FileLogger con prefijo [API] |

---

### 7. `Services/ApiLogger.cs`

**Propósito:** Sistema de logging que envía logs al servidor.

#### Funcionamiento

1. Los logs se encolan en `ConcurrentQueue<LogEntry>`
2. Un timer cada 5 segundos hace flush a la API
3. Cada log se escribe primero en disco (fallback)

#### Estructura de `LogEntry`

| Campo | Tipo |
|-------|------|
| BeaconUid | string |
| Level | string |
| Message | string |
| Timestamp | DateTime |

#### Inicialización

Al llamar `InitializeAsync()`:
- Crea tabla `beacon_logs` si no existe
- Crea columnas: `beacon_uid`, `level`, `message`, `timestamp`

---

### 8. `Services/FileLogger.cs`

**Propósito:** Logger simple a archivo local.

#### Ubicación del Log

```
{AppDirectory}/beacon_log.txt
```

#### Métodos

| Método | Descripción |
|--------|-------------|
| `Log(message)` | Escribe línea con timestamp `yyyy-MM-dd HH:mm:ss` |
| `LogError(context, ex)` | Escribe error con contexto y stack trace |

#### Thread Safety

Usa `lock (_lock)` para escritura segura desde múltiples threads.

---

### 9. `ViewModels/MainViewModel.cs`

**Propósito:** ViewModel principal que implementa MVVM y contiene toda la lógica de negocio.

#### Constantes de Temporización

| Constante | Valor | Descripción |
|-----------|-------|-------------|
| POLLING_INTERVAL_MS | 300 | Intervalo de polling (lectura BD) |
| HEARTBEAT_INTERVAL_MS | 10000 | Intervalo de heartbeat (registro) |

#### Propiedades Bindables (INotifyPropertyChanged)

| Propiedad | Tipo | Descripción |
|-----------|------|-------------|
| CurrentMode | string | Modo actual (NORMAL, CONGESTION, etc.) |
| CurrentZone | string | Zona actual mostrada |
| CurrentArrow | string | Dirección de flecha |
| CurrentBrightness | int | Brillo de pantalla (0-100) |
| CurrentEvacuationExit | string | Salida de evacuación asignada |
| CurrentLanguage | string | Idioma (ES/EN) |
| BackgroundColor | string | Color de fondo hexadecimal |
| DisplayText | string | Texto principal mostrado |
| StatusMessage | string | Mensaje de estado (debug) |
| IsConfigured | bool | Si la baliza está configurada |
| BrightnessOpacity | double | Opacidad calculada para overlay |

#### Métodos Públicos

| Método | Descripción |
|--------|-------------|
| `Start()` | Inicia healthcheck, sincronización y timers |
| `Stop()` | Detiene todos los servicios y timers |

#### Métodos de Sincronización

| Método | Descripción |
|--------|-------------|
| `SyncConfigAsync()` | Lee configuración de la baliza desde BD |
| `SendHeartbeatAsync()` | Envía estado actual al servidor |
| `CheckGlobalStateAsync()` | Lee estado global y prioriza EVACUACIÓN |
| `PollCommandsAsync()` | Lee y procesa comandos pendientes |

#### Procesamiento de Comandos

| Comando | Método | Acción |
|---------|--------|--------|
| UPDATE_CONFIG | `ProcessUpdateConfig()` | Actualiza propiedades desde JSON |
| RESTART | `ProcessRestart()` | Reinicia Windows |
| SHUTDOWN | `ProcessShutdown()` | Apaga Windows |
| CLOSE / CLOSE_APP | `ProcessCloseApp()` | Cierra la aplicación |

#### Lógica de Resolución de Conflictos

1. **EVACUACIÓN Global:** Si `circuit_state.global_mode = "EVACUATION"`, ignora modos individuales
2. **Grace Period:** Al salir de evacuación, ignora DB por 5 segundos para evitar flickering
3. **Expiración de Comandos:** Ignora comandos con más de 60 minutos de antigüedad

#### Métodos de UI

| Método | Descripción |
|--------|-------------|
| `UpdateDisplayForMode()` | Actualiza texto y color según modo |
| `UpdateDefaultTextForMode()` | Asigna texto por defecto según modo |
| `UpdateDefaultColorForMode()` | Asigna color por defecto según modo |
| `SetWindowsBrightness()` | Ajusta brillo via PowerShell/WMI |

#### Colores por Modo

| Modo | Color Hex | Nombre |
|------|-----------|--------|
| UNCONFIGURED | #1565C0 | Azul |
| NORMAL | #2E7D32 | Verde |
| CONGESTION | #F57C00 | Naranja |
| EMERGENCY | #C62828 | Rojo |
| EVACUATION | #D32F2F | Rojo intenso |
| MAINTENANCE | #7B1FA2 | Púrpura |

---

## 🔌 Endpoints de API Utilizados

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/health` | GET | Healthcheck |
| `/beacons/heartbeat` | POST | Registrar estado de baliza |
| `/commands/pending/{uid}` | GET | Obtener comandos pendientes |
| `/commands/{id}/execute` | POST | Marcar comando ejecutado |
| `/commands` | POST | Crear nuevo comando |
| `/_get` | POST | Consulta genérica a tabla |
| `/_upsert` | POST | Insertar/actualizar registro |
| `/_delete` | POST | Eliminar registro |
| `/_ensure_table` | POST | Crear tabla si no existe |
| `/_ensure_column` | POST | Crear columna si no existe |

---

## 🔄 Ciclo de Vida de la Aplicación

```
1. App.OnStartup()
   └── Registra manejadores de excepciones

2. MainWindow.Loaded
   ├── BeaconConfigService.ReadOrCreateConfig()
   ├── new ApiClient(baseUrl)
   ├── new MainViewModel(config, apiClient)
   └── viewModel.Start()

3. MainViewModel.Start()
   ├── apiClient.CheckHealthAsync()
   ├── SyncConfigAsync() (inicial)
   ├── apiLogger.InitializeAsync()
   ├── Timer: Polling cada 300ms
   │   ├── CheckGlobalStateAsync()
   │   ├── SyncConfigAsync()
   │   └── PollCommandsAsync()
   └── Timer: Heartbeat cada 10s
       └── SendHeartbeatAsync()

4. MainWindow.Closing
   └── viewModel.Stop()

5. App.OnExit()
   └── Limpieza final
```

---

## 🛠️ Comandos de Compilación

```powershell
# Restaurar dependencias
dotnet restore

# Compilar
dotnet build

# Ejecutar
dotnet run

# Publicar release
dotnet publish -c Release -r win-x64 --self-contained
```

---

## 📋 Dependencias

- **.NET 8.0 SDK**
- **Windows Desktop Runtime 8.0** (para WPF)
- **System.Text.Json** (serialización JSON)
- **System.Net.Http** (cliente HTTP)

---

## 🔐 Seguridad

- **SSL Bypass:** El cliente HTTP ignora errores de certificado para permitir HTTPS con certificados autofirmados
- **Configuración Local:** Se almacena en `C:\ProgramData\GeoRacing\` con permisos de administrador recomendados
- **Sin Autenticación:** La API no requiere autenticación (asumir red privada)

---

## 📝 Notas para Desarrolladores

1. **Modo Kiosko:** La ventana está configurada como `Topmost`, `WindowStyle=None`, `Maximized` para funcionar como kiosko
2. **ESC para Salir:** Única forma de cerrar la app localmente (útil para desarrollo)
3. **Logs Duales:** Todo se loguea tanto a disco como a la API para debugging remoto
4. **UTC vs Local:** Los timestamps de comandos se manejan en UTC para evitar problemas de timezone

---

*Documentación generada para el ecosistema GeoRacing - Febrero 2026*
