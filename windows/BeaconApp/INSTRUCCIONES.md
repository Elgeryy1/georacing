# 🏁 GeoRacing - Aplicación de Beacon para Windows

## ✅ ¿Qué se ha creado?

Se ha creado **EXCLUSIVAMENTE** la aplicación de beacon para Windows (WPF .NET 8), sin tocar API, base de datos ni panel web.

## 📁 Estructura Creada

```
BeaconApp/
├── Config/
│   └── BeaconConfigService.cs          # Gestión de beacon.json
├── Models/
│   └── BeaconModels.cs                 # Modelos de datos (ScreenConfig, BeaconCommand, BeaconHeartbeat)
├── Services/
│   └── ApiClient.cs                    # Cliente HTTP para comunicación con API
├── ViewModels/
│   └── MainViewModel.cs                # Lógica de presentación con INotifyPropertyChanged
├── MainWindow.xaml                     # Interfaz XAML (pantalla completa)
├── MainWindow.xaml.cs                  # Code-behind
├── App.xaml                            # Configuración de aplicación
├── App.xaml.cs                         # Manejo de excepciones
├── BeaconApp.csproj                    # Proyecto .NET 8
├── README.md                           # Documentación completa
├── build-and-run.ps1                   # Script de compilación y ejecución
└── .gitignore                          # Archivos a ignorar
```

## 🚀 Cómo Ejecutar

### Opción 1: Modo desarrollo

```powershell
cd "windows\BeaconApp"
dotnet run
```

### Opción 2: Usar el script

```powershell
cd "windows\BeaconApp"
.\build-and-run.ps1
```

### Opción 3: Compilar y ejecutar binario

```powershell
cd "windows\BeaconApp"
dotnet build -c Release /p:BaseIntermediateOutputPath=obj_new\ /p:BaseOutputPath=bin_new\
.\bin_new\Release\net8.0-windows\GeoRacingBeacon.exe
```

## ⚙️ Configuración Automática

Al iniciar por primera vez, la aplicación:

1. Crea `C:\ProgramData\GeoRacing\beacon.json` con:
   ```json
   {
     "beaconId": "NOMBRE-DE-TU-PC",
     "apiBaseUrl": "http://192.168.1.99:4000"
   }
   ```

2. Si quieres cambiar la URL de la API, edita ese archivo o usa:
   ```powershell
   $env:GEORACING_API_URL = "http://tu-servidor:4000"
   ```

## 🎯 Características Implementadas

### ✅ Pantalla Completa (Kiosco)
- `WindowStyle="None"` - Sin bordes
- `WindowState="Maximized"` - Pantalla completa
- `Topmost="True"` - Siempre al frente
- Solo se puede cerrar con **ESC**

### ✅ Comunicación con API

**Heartbeat** (cada 10 segundos):
```
POST /api/beacons
{
  "id": "BEACON-1",
  "battery": null,
  "brightness": 80,
  "mode": "NORMAL",
  "online": true
}
```

**Polling de comandos** (cada 2 segundos):
```
GET /api/commands/pending/{beaconId}
```

**Marcar comando ejecutado**:
```
POST /api/commands/{id}/execute
```

### ✅ Modos Implementados

| Modo | Color | Descripción |
|------|-------|-------------|
| `UNCONFIGURED` | Azul `#1565C0` | Sin configurar |
| `NORMAL` | Verde `#2E7D32` | Operación normal |
| `CONGESTION` | Naranja `#F57C00` | Congestión |
| `EMERGENCY` | Rojo `#C62828` | Emergencia |

### ✅ Flechas Direccionales

- `NONE` - Sin flecha
- `FORWARD` - ⬆
- `LEFT` - ⬅
- `RIGHT` - ➡
- `BACKWARD` - ⬇

### ✅ Visualización

- **Texto principal** con el mensaje del modo o zona
- **Flecha grande** según configuración
- **Barra de estado** inferior con:
  - Mensaje de estado
  - Indicador de brillo
  - Ayuda para cerrar (ESC)

## 🔍 Logs

Los logs se guardan automáticamente en:
```
C:\ProgramData\GeoRacing\beacon-debug.log
```

También se muestran en la consola cuando ejecutas con `dotnet run`.

## 📝 Ejemplo de Uso

1. **Iniciar la API** (debe estar corriendo en `http://192.168.1.99:4000`)

2. **Ejecutar la beacon**:
   ```powershell
   cd "windows\BeaconApp"
   dotnet run
   ```

3. La aplicación se abre en **pantalla completa** mostrando "SIN CONFIGURACIÓN" (azul)

4. **Desde el panel web**, configura la beacon:
   - Selecciona modo: NORMAL, CONGESTION o EMERGENCY
   - Establece brillo: 0-100%
   - Selecciona flecha: FORWARD, LEFT, RIGHT, etc.
   - Añade zona: "Paddock A", "Curva 1", etc.

5. La beacon **automáticamente** recibe la configuración y actualiza su pantalla

6. Presiona **ESC** para cerrar

## 🐛 Solución de Problemas

### "No se puede conectar a la API"

1. Verifica que la API está corriendo:
   ```powershell
   Invoke-WebRequest -Uri http://192.168.1.99:4000/health
   ```

2. Revisa el archivo de configuración:
   ```powershell
   Get-Content C:\ProgramData\GeoRacing\beacon.json
   ```

3. Revisa los logs:
   ```powershell
   Get-Content C:\ProgramData\GeoRacing\beacon-debug.log -Tail 50
   ```

### "La configuración no cambia"

1. Verifica que se están creando comandos en la API
2. Revisa los logs de la beacon para ver si se reciben
3. Comprueba que el `beaconId` coincide

## 🎓 Arquitectura Técnica

### Patrón MVVM
- **Model**: `BeaconModels.cs` - DTOs para comunicación con API
- **ViewModel**: `MainViewModel.cs` - Lógica de negocio y estado
- **View**: `MainWindow.xaml` - Presentación WPF

### Servicios
- **BeaconConfigService**: Lee/crea `beacon.json` automáticamente
- **ApiClient**: Gestiona todas las llamadas HTTP a la API

### Threading
- **Heartbeat Timer**: `System.Threading.Timer` cada 10s
- **Polling Timer**: `System.Threading.Timer` cada 2s
- **UI Updates**: `Dispatcher.InvokeAsync()` para cambios desde background threads

### Binding
- Propiedades observables con `INotifyPropertyChanged`
- Binding bidireccional XAML ↔ ViewModel
- DataTriggers para cambiar UI según modo

## 📦 Compilar para Producción

```powershell
dotnet publish -c Release -r win-x64 --self-contained false -o publish
```

Archivos en `BeaconApp\publish\` listos para distribuir.

## ✨ Características Destacadas

- ✅ **Auto-configuración**: No necesita setup manual
- ✅ **Resiliente**: Reintenta conexiones automáticamente
- ✅ **Logs completos**: Todas las operaciones se registran
- ✅ **Modo kiosco**: No se puede cerrar accidentalmente
- ✅ **Actualización en tiempo real**: 2s de latencia máxima
- ✅ **Binding reactivo**: Cambios instantáneos en la UI

---

## ⚠️ IMPORTANTE - RESTRICCIONES CUMPLIDAS

✅ **NO se creó ninguna web**
✅ **NO se creó ningún backend**
✅ **NO se creó ninguna base de datos**
✅ **SOLO se creó la aplicación de escritorio para Windows**
✅ **Toda la comunicación es vía API REST existente**

---

**¡Aplicación de beacon lista para usar! 🏁**
