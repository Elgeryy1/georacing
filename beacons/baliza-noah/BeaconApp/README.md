# 🏁 GeoRacing - Aplicación de Baliza

Aplicación de escritorio para Windows (WPF .NET 8) que funciona como terminal de señalización inteligente para circuitos de carreras.

## 📋 Características

- **Pantalla completa** (modo kiosco)
- **Auto-configuración** desde `C:\ProgramData\GeoRacing\beacon.json`
- **Comunicación con API REST** para recibir comandos y enviar telemetría
- **4 modos de operación**:
  - `UNCONFIGURED` - Sin configurar (azul)
  - `NORMAL` - Operación normal (verde)
  - `CONGESTION` - Advertencia de congestión (naranja)
  - `EMERGENCY` - Emergencia/evacuación (rojo)

## 🔧 Requisitos

- Windows 10/11
- .NET 8 SDK
- Acceso a la API REST de GeoRacing (por defecto: `http://192.168.1.99:4000`)

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
  "beaconId": "BALIZA-1",
  "apiBaseUrl": "http://192.168.1.99:4000"
}
```

**Comportamiento al iniciar**:

- Si el archivo **NO existe**: Se crea automáticamente con:
  - `beaconId` = Nombre del PC (`Environment.MachineName`)
  - `apiBaseUrl` = Variable de entorno `GEORACING_API_URL` o valor por defecto

- Si el archivo **existe**: Se lee y usa su configuración

- Si el archivo está **corrupto**: Se hace backup y se regenera

### Variable de Entorno (opcional)

```powershell
# Configurar API URL mediante variable de entorno
[System.Environment]::SetEnvironmentVariable("GEORACING_API_URL", "http://tu-servidor:4000", "Machine")
```

## 🌐 Comunicación con API

### Endpoints utilizados

#### 1. Heartbeat / Registro
```
POST /api/beacons
{
  "id": "BALIZA-1",
  "battery": null,
  "brightness": 80,
  "mode": "NORMAL",
  "online": true
}
```
Se envía cada **10 segundos**.

#### 2. Obtener comando pendiente
```
GET /api/commands/pending/{beaconId}
```
Se consulta cada **2 segundos**.

Respuesta:
```json
{
  "id": 123,
  "beaconId": "BALIZA-1",
  "command": "UPDATE_CONFIG",
  "value": "{\"mode\":\"NORMAL\",\"brightness\":80,\"arrow\":\"FORWARD\",\"zone\":\"Paddock A\"}",
  "executed": false,
  "createdAt": "2025-11-18T18:00:00.000Z"
}
```

#### 3. Marcar comando como ejecutado
```
POST /api/commands/{id}/execute
```

## 🎨 Modos de Visualización

| Modo | Color | Descripción |
|------|-------|-------------|
| **UNCONFIGURED** | Azul `#1565C0` | Baliza sin configurar |
| **NORMAL** | Verde `#2E7D32` | Operación normal del circuito |
| **CONGESTION** | Naranja `#F57C00` | Advertencia de congestión |
| **EMERGENCY** | Rojo `#C62828` | Emergencia/evacuación |

### Flechas direccionales

- `NONE` - Sin flecha
- `FORWARD` - ⬆
- `LEFT` - ⬅
- `RIGHT` - ➡
- `BACKWARD` - ⬇

## ⌨️ Controles

- **ESC** - Cerrar la aplicación

## 📂 Estructura del Proyecto

```
BeaconApp/
├── Config/
│   └── BeaconConfigService.cs     # Gestión de beacon.json
├── Models/
│   └── BeaconModels.cs            # Modelos de datos
├── Services/
│   └── ApiClient.cs               # Cliente HTTP para API
├── ViewModels/
│   └── MainViewModel.cs           # Lógica de presentación
├── MainWindow.xaml                # Interfaz XAML
├── MainWindow.xaml.cs             # Code-behind
├── App.xaml                       # Configuración de aplicación
└── BeaconApp.csproj               # Proyecto .NET
```

## 🔍 Logs

Los logs se guardan en:
```
C:\ProgramData\GeoRacing\beacon-debug.log
```

Formato:
```
2025-11-18 18:30:45 [CONFIG] ✓ Configuración cargada: BALIZA-1
2025-11-18 18:30:45 [API] Cliente API inicializado: http://192.168.1.99:4000
2025-11-18 18:30:45 [VM] ViewModel inicializado para baliza: BALIZA-1
2025-11-18 18:30:55 [API] ✓ Heartbeat enviado: NORMAL
2025-11-18 18:31:00 [API] ✓ Comando recibido: UPDATE_CONFIG (ID: 42)
```

## 🐛 Solución de Problemas

### La baliza no se conecta a la API

1. Verificar que la API está corriendo: `http://192.168.1.99:4000/health`
2. Revisar `beacon.json` y confirmar la URL correcta
3. Verificar conectividad de red: `ping 192.168.1.99`
4. Revisar logs en `beacon-debug.log`

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

Para que la baliza inicie automáticamente con Windows:

```powershell
$action = New-ScheduledTaskAction -Execute "C:\Program Files\GeoRacing\GeoRacingBeacon.exe"
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest

Register-ScheduledTask -TaskName "GeoRacing Beacon" -Action $action -Trigger $trigger -Principal $principal
```

## 📝 Notas Técnicas

- La aplicación usa `HttpClient` reutilizable para todas las peticiones
- Los timers usan `System.Threading.Timer` para no bloquear el UI thread
- Los cambios de configuración se aplican en el `Dispatcher` de WPF
- El fondo cambia dinámicamente usando binding a `BackgroundColor`

## 📄 Licencia

Parte del sistema GeoRacing - Ver LICENSE en el directorio raíz del proyecto.

---

**¡Listo para carreras! 🏁**
