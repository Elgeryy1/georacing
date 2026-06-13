# 📊 DIAGRAMA DE FUNCIONAMIENTO - BEACON GEORACING

```
┌─────────────────────────────────────────────────────────────────────┐
│                          BASE DE DATOS                              │
│                         (Backend MySQL)                             │
├─────────────────────────────────────────────────────────────────────┤
│  Tabla: beacons                                                     │
│  ┌────────────┬──────┬─────────┬───────┬──────────────┬──────────┐ │
│  │ id         │ mode │ arrow   │ color │ message      │ online   │ │
│  ├────────────┼──────┼─────────┼───────┼──────────────┼──────────┤ │
│  │ MINI-PC-01 │ NORM │ UP      │ #2E.. │ Bienvenido   │ true     │ │
│  │            │ AL   │         │       │ al circuito  │          │ │
│  └────────────┴──────┴─────────┴───────┴──────────────┴──────────┘ │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     │  ⬆ POST /api/beacons (cada 30s)
                     │     { id, online: true, brightness, mode }
                     │
                     │  ⬇ GET /api/beacons/{id} (cada 2s)
                     │     { message, color, arrow, mode, ... }
                     │
┌────────────────────┴────────────────────────────────────────────────┐
│                        BACKEND API                                  │
│                     (Node.js Express)                               │
├─────────────────────────────────────────────────────────────────────┤
│  • GET  /api/beacons/{id}      → Lee estado de la BD               │
│  • POST /api/beacons           → Recibe heartbeat (actualiza online)│
│  • PUT  /api/beacons/{id}      → Panel actualiza configuración     │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     │  HTTP/REST
                     │
┌────────────────────┴────────────────────────────────────────────────┐
│                      BEACON (miniPC)                                │
│                    WPF .NET 8 Application                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    ApiClient (Services)                     │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │  • GetBeaconStatusAsync()  ← Polling cada 2s               │   │
│  │  • SendHeartbeatAsync()    ← Heartbeat cada 30s            │   │
│  └────────────────┬───────────────────────┬────────────────────┘   │
│                   │                       │                        │
│                   ▼                       ▼                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │             MainViewModel (MVVM Pattern)                    │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │  📥 LECTURA (cada 2s):                                      │   │
│  │     UpdateFromStatusAsync(BeaconStatus status)              │   │
│  │     {                                                       │   │
│  │       DisplayText = status.message;  // ⭐ Texto de BD      │   │
│  │       BackgroundColor = status.color;                       │   │
│  │       CurrentArrow = status.arrow;                          │   │
│  │       CurrentMode = status.mode;                            │   │
│  │     }                                                       │   │
│  │                                                             │   │
│  │  📤 ESCRITURA (cada 30s):                                   │   │
│  │     SendHeartbeatAsync()                                    │   │
│  │     {                                                       │   │
│  │       POST { id, online: true, brightness, mode }           │   │
│  │     }                                                       │   │
│  │                                                             │   │
│  │  🚫 NO HACE:                                                │   │
│  │     ✗ NO procesa comandos UPDATE_CONFIG                     │   │
│  │     ✗ NO modifica su configuración                          │   │
│  │     ✗ NO recalcula mensajes                                 │   │
│  └────────────────┬────────────────────────────────────────────┘   │
│                   │ Data Binding (INotifyPropertyChanged)          │
│                   ▼                                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   MainWindow (XAML UI)                      │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │  ┌─────────────────────────────────────────────────────┐   │   │
│  │  │  Header: Zone Badge + Clock                        │   │   │
│  │  ├─────────────────────────────────────────────────────┤   │   │
│  │  │  Content:                                           │   │   │
│  │  │    • Icon (ℹ️ / 🚨 / ⚠️ / 🔧)                        │   │   │
│  │  │    • MainText ← DisplayText (⭐ message de BD)      │   │   │
│  │  │    • Arrow ← CurrentArrow (⬆⬇⬅➡↖↗↙↘)               │   │   │
│  │  │    • EvacuationExit (si mode=EVACUATION)           │   │   │
│  │  ├─────────────────────────────────────────────────────┤   │   │
│  │  │  Footer: Status Pill (online indicator)            │   │   │
│  │  └─────────────────────────────────────────────────────┘   │   │
│  │                                                             │   │
│  │  Background Color ← BackgroundColor (⭐ color de BD)        │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘


════════════════════════════════════════════════════════════════════════
                            EJEMPLO DE FLUJO
════════════════════════════════════════════════════════════════════════

1️⃣  PANEL WEB actualiza configuración:
    ┌─────────────────────────────────────────┐
    │ PUT /api/beacons/MINI-PC-01             │
    │ {                                       │
    │   "message": "Bienvenido al circuito",  │
    │   "color": "#4CAF50",                   │
    │   "arrow": "UP",                        │
    │   "mode": "NORMAL"                      │
    │ }                                       │
    └────────────────┬────────────────────────┘
                     │
                     ▼
    ┌─────────────────────────────────────────┐
    │ Backend actualiza tabla beacons en BD   │
    └─────────────────────────────────────────┘

2️⃣  BEACON lee estado (máximo 2 segundos después):
    ┌─────────────────────────────────────────┐
    │ GET /api/beacons/MINI-PC-01             │
    │ ← {                                     │
    │     "message": "Bienvenido al circuito",│
    │     "color": "#4CAF50",                 │
    │     "arrow": "UP",                      │
    │     "mode": "NORMAL"                    │
    │   }                                     │
    └────────────────┬────────────────────────┘
                     │
                     ▼
    ┌─────────────────────────────────────────┐
    │ MainViewModel.UpdateFromStatusAsync()   │
    ��� DisplayText = "Bienvenido al circuito"  │
    │ BackgroundColor = "#4CAF50"             │
    │ CurrentArrow = "UP"                     │
    └────────────────┬────────────────────────┘
                     │
                     ▼
    ┌─────────────────────────────────────────┐
    │ PANTALLA SE ACTUALIZA:                  │
    │ ┌─────────────────────────────────────┐ │
    │ │ 🟢 Sector A           10:30:45      │ │
    │ ├─────────────────────────────────────┤ │
    │ │                                     │ │
    │ │            ℹ️                        │ │
    │ │                                     │ │
    │ │  BIENVENIDO AL CIRCUITO            │ │
    │ │  Welcome to the Circuit             │ │
    │ │                                     │ │
    │ │            ⬆                        │ │
    │ │                                     │ │
    │ ├─────────────────────────────────────┤ │
    │ │  🟢 GEORACING                       │ │
    │ └─────────────────────────────────────┘ │
    │ Fondo: Verde claro (#4CAF50)            │
    └─────────────────────────────────────────┘

3️⃣  BEACON registra presencia (cada 30 segundos):
    ┌─────────────────────────────────────────┐
    │ POST /api/beacons                       │
    │ {                                       │
    │   "id": "MINI-PC-01",                   │
    │   "online": true,                       │
    │   "brightness": 80,                     │
    │   "mode": "NORMAL"                      │
    │ }                                       │
    └────────────────┬────────────────────────┘
                     │
                     ▼
    ┌─────────────────────────────────────────┐
    │ Backend actualiza: lastSeen = NOW()     │
    │ Panel ve: beacon online hace 5 segundos │
    └─────────────────────────────────────────┘


════════════════════════════════════════════════════════════════════════
                          TEMPORIZACIÓN
════════════════════════════════════════════════════════════════════════

⏱️  POLLING (Lectura):
    ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
    │ 0s │ 2s │ 4s │ 6s │ 8s │10s │12s │14s │16s │18s │
    └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
      ↓    ↓    ↓    ↓    ↓    ↓    ↓    ↓    ↓    ↓
     GET  GET  GET  GET  GET  GET  GET  GET  GET  GET

💓 HEARTBEAT (Escritura):
    ┌────┬────────────────────────────────┬────────────────────────────────┐
    │ 0s │             30s                │             60s                │
    └────┴────────────────────────────────┴────────────────────────────────┘
      ↓                                    ↓
     POST                                 POST


════════════════════════════════════════════════════════════════════════
                        RESUMEN DE OPERACIONES
════════════════════════════════════════════════════════════════════════

LA BEACON HACE:
  ✅ Lee estado cada 2 segundos (GET /api/beacons/{id})
  ✅ Muestra campo "message" de BD directamente en pantalla
  ✅ Aplica "color" de BD al fondo
  ✅ Muestra "arrow" de BD
  ✅ Envía heartbeat cada 30 segundos (POST /api/beacons)

LA BEACON NO HACE:
  ❌ NO procesa comandos UPDATE_CONFIG
  ❌ NO modifica su configuración
  ❌ NO recalcula mensajes
  ❌ NO traduce textos
  ❌ NO confirma comandos ejecutados

RESULTADO:
  🎯 Panel web → Base de datos → Beacon lee → Pantalla muestra
  💓 Beacon → Backend → Actualiza lastSeen (presencia online)
```
