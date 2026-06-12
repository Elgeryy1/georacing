# 🏁 BALIZA GEORACING - Aplicación de Kiosko

## 📖 Descripción

Aplicación WPF .NET 8 para miniPCs que funcionan como balizas inteligentes en el sistema GeoRacing. Muestra información en tiempo real sobre el estado del circuito, direcciones de navegación, alertas de emergencia y evacuación.

## 🎯 Características Principales

- ✅ **Lectura en tiempo real** (polling cada 2 segundos)
- ✅ **9 direcciones de flechas** (cardinales + diagonales)
- ✅ **Mensajes personalizados** desde el panel web
- ✅ **Colores personalizados** (hexadecimal)
- ✅ **6 modos operativos** (Normal, Congestión, Emergencia, Evacuación, Mantenimiento, Sin configurar)
- ✅ **Salidas de evacuación** (visible solo en modo EVACUATION)
- ✅ **UI responsive** (horizontal y vertical)
- ✅ **Efectos visuales premium** (gradientes, resplandores)
- ✅ **Modo solo lectura** (no escribe en API)

## 🚀 Inicio Rápido

### **Ejecución Simple**
```powershell
.\dev.ps1 run
```

### **Compilar**
```powershell
.\dev.ps1 build
```

### **Ver Estado**
```powershell
.\dev.ps1 status
```

### **Publicar Portable**
```powershell
.\dev.ps1 publish
```

## 📋 Comandos Disponibles

```powershell
# Compilar en modo Debug
.\dev.ps1 build

# Recompilar (limpieza completa)
.\dev.ps1 rebuild

# Ejecutar aplicación
.\dev.ps1 run

# Publicar versión portable (single-file)
.\dev.ps1 publish

# Limpiar archivos compilados
.\dev.ps1 clean

# Ver estado del proyecto
.\dev.ps1 status
```

## ⚙️ Configuración

### **Archivo**: `C:\ProgramData\GeoRacing\beacon.json`

```json
{
  "BeaconId": "MINI-PC-01",
  "ApiBaseUrl": "http://192.168.1.99:4000"
}
```

### **Variables de Entorno** (opcional)
```powershell
$env:GEORACING_API_URL = "http://192.168.1.99:4000"
```

## 📡 API

### **Endpoint Principal**: `GET /api/beacons/{id}`

**Respuesta:**
```json
{
  "id": "MINI-PC-01",
  "mode": "NORMAL",
  "brightness": 80,
  "zone": "Sector A",
  "arrow": "UP_RIGHT",
  "message": "Bienvenido al circuito",
  "color": "#2E7D32",
  "language": "ES",
  "evacuationExit": "Salida 3 - Tribuna Principal",
  "configured": true,
  "online": true
}
```

**Frecuencia**: Polling cada 2 segundos

## 🎨 Modos Operativos

| Modo | Color | Icono | Descripción |
|------|-------|-------|-------------|
| **UNCONFIGURED** | Gris (#90A4AE) | ⚠ | Baliza sin configurar |
| **NORMAL** | Verde (#2E7D32) | ℹ️ | Operación normal |
| **CONGESTION** | Naranja (#F57C00) | ⚠️ | Congestión de tráfico |
| **EMERGENCY** | Rojo oscuro (#C62828) | 🚨 | Emergencia activa |
| **EVACUATION** | Rojo brillante (#D32F2F) | 🚨 | Evacuación en curso |
| **MAINTENANCE** | Morado (#7B1FA2) | 🔧 | Mantenimiento |

## 🧭 Direcciones de Flechas

```
    ↖  ⬆  ↗
     \ | /
    ⬅  •  ➡
     / | \
    ↙  ⬇  ↘
```

**Valores válidos:**
- `UP`, `DOWN`, `LEFT`, `RIGHT` (cardinales)
- `UP_LEFT`, `UP_RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT` (diagonales)
- `NONE` (sin flecha)

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────┐
│         API Backend (Node.js)           │
│    http://192.168.1.99:4000             │
└───────────────────┬─────────────────────┘
                    │ GET /api/beacons/{id}
                    │ (cada 2 segundos)
                    ▼
┌─────────────────────────────────────────┐
│      ApiClient (Services)               │
│  - GetBeaconStatusAsync()               │
└───────────────────┬─────────────────────┘
                    │ BeaconStatus
                    ▼
┌─────────────────────────────────────────┐
│    MainViewModel (ViewModels)           │
│  - UpdateFromStatusAsync()              │
│  - Propiedades observables              │
└───────────────────┬─────────────────────┘
                    │ Data Binding
                    ▼
┌─────────────────────────────────────────┐
│      MainWindow (XAML)                  │
│  - UI Responsive con Viewbox            │
│  - Bindings: DisplayText, CurrentArrow  │
└─────────────────────────────────────────┘
```

## 📂 Estructura del Proyecto

```
BeaconApp/
├── Models/
│   └── BeaconModels.cs        # Beacon, BeaconStatus, ScreenConfig
├── ViewModels/
│   └── MainViewModel.cs       # Lógica de negocio, polling
├── Services/
│   └── ApiClient.cs           # HTTP client (GET /api/beacons/{id})
├── Config/
│   └── BeaconConfigService.cs # Lee beacon.json
├── MainWindow.xaml            # UI completa
├── MainWindow.xaml.cs         # Code-behind (estilos de modos)
├── App.xaml                   # Recursos globales
├── dev.ps1                    # Script de desarrollo
└── IMPLEMENTACION-COMPLETA.md # Documentación completa
```

## 🧪 Testing

### **Test Básico**
```powershell
# 1. Iniciar backend (en otra terminal)
cd backend
npm run dev

# 2. Ejecutar baliza
cd BeaconApp
.\dev.ps1 run
```

### **Simulación de Modos**
```bash
# Cambiar a modo CONGESTION
curl -X PUT http://192.168.1.99:4000/api/beacons/MINI-PC-01 \
  -H "Content-Type: application/json" \
  -d '{"mode": "CONGESTION", "arrow": "LEFT"}'

# Mensaje personalizado
curl -X PUT http://192.168.1.99:4000/api/beacons/MINI-PC-01 \
  -H "Content-Type: application/json" \
  -d '{"message": "¡Bienvenido!", "color": "#FF5722"}'

# Evacuación con salida
curl -X PUT http://192.168.1.99:4000/api/beacons/MINI-PC-01 \
  -H "Content-Type: application/json" \
  -d '{"mode": "EVACUATION", "evacuationExit": "Salida 3", "arrow": "DOWN_RIGHT"}'
```

## 🐞 Troubleshooting

### **Error: "Access denied to GeoRacingBeacon.exe"**
```powershell
# Cerrar app antes de compilar
.\dev.ps1 status    # Ver si está corriendo
# Presionar ESC en la ventana de la baliza
```

### **Backend no responde**
```powershell
# Verificar conectividad
Test-NetConnection 192.168.1.99 -Port 4000

# Probar endpoint
curl http://192.168.1.99:4000/api/beacons/MINI-PC-01
```

### **Flecha no aparece**
✅ Usar valores en MAYÚSCULAS: `UP`, `DOWN`, `UP_RIGHT`, etc.  
❌ No usar minúsculas: `up`, `down`

### **Mensaje personalizado no se muestra**
✅ Verificar que backend envía `"message": "Tu texto aquí"`  
✅ Verificar que no es null ni cadena vacía

## 📝 Notas Importantes

### **Modo Solo Lectura (Configuración)**
- ✅ **SÍ** envía heartbeat cada 30 segundos (se registra como "estoy aquí")
- ❌ **NO** procesa comandos de configuración (UPDATE_CONFIG)
- ❌ **NO** modifica su propia configuración
- ✅ **SÍ** lee estado cada 2 segundos (GET /api/beacons/{id})
- ⭐ **Campo `message` de BD** se muestra directamente en pantalla

### **Prioridad de Datos**
```
message (panel) > Texto por defecto del modo
color (panel)   > Color por defecto del modo
```

### **Sin Lógica de Negocio**
- ❌ No recalcula mensajes
- ❌ No traduce textos
- ❌ No modifica colores
- ✅ Muestra exactamente lo que llega del panel

## 🔐 Requisitos

- **.NET 8.0 SDK** o superior
- **Windows 10/11** (64-bit)
- **PowerShell 5.1** o superior
- **Resolución mínima**: 800x600 (optimizado para 1920x1080)

## 📦 Despliegue en Producción

### **1. Publicar Single-File**
```powershell
.\dev.ps1 publish
```

### **2. Copiar ejecutable a miniPC**
```
bin\Release\net8.0-windows\win-x64\publish\GeoRacingBeacon.exe
```

### **3. Configurar autoarranque** (Task Scheduler)
```powershell
# Crear tarea que ejecute al inicio de Windows
$Action = New-ScheduledTaskAction -Execute "C:\Balizas\GeoRacingBeacon.exe"
$Trigger = New-ScheduledTaskTrigger -AtStartup
$Principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest
Register-ScheduledTask -TaskName "GeoRacing Baliza" -Action $Action -Trigger $Trigger -Principal $Principal
```

### **4. Configurar beacon.json**
```powershell
# Crear directorio
New-Item -ItemType Directory -Path "C:\ProgramData\GeoRacing" -Force

# Crear config
@{
    BeaconId = "MINI-PC-01"
    ApiBaseUrl = "http://192.168.1.99:4000"
} | ConvertTo-Json | Set-Content "C:\ProgramData\GeoRacing\beacon.json"
```

## 📞 Soporte

**Logs en consola:**
```
yyyy-MM-dd HH:mm:ss [VM] Mensaje
```

**Archivo de configuración:**
```
C:\ProgramData\GeoRacing\beacon.json
```

**Verificar estado de la API:**
```powershell
curl http://192.168.1.99:4000/api/beacons/{id}
```

## 📄 Licencia

Proyecto interno GeoRacing - Todos los derechos reservados

---

**Desarrollado con ❤️ para GeoRacing**
