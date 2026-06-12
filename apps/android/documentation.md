# Android_App — Documentación Técnica Completa

## Índice

1. [Descripción General](#descripción-general)
2. [Tecnologías y Dependencias](#tecnologías-y-dependencias)
3. [Arquitectura](#arquitectura)
4. [Estructura de Carpetas](#estructura-de-carpetas)
5. [Módulos y Ficheros Clave](#módulos-y-ficheros-clave)
   - [Punto de Entrada](#punto-de-entrada)
   - [Inyección de Dependencias (DI)](#inyección-de-dependencias-di)
   - [Domain Layer (Dominio)](#domain-layer-dominio)
   - [Data Layer (Datos)](#data-layer-datos)
   - [UI Layer (Interfaz de Usuario)](#ui-layer-interfaz-de-usuario)
   - [Services (Servicios en segundo plano)](#services-servicios-en-segundo-plano)
   - [Infrastructure](#infrastructure)
   - [Android Auto (Car)](#android-auto-car)
   - [Navigation (Motor de Navegación)](#navigation-motor-de-navegación)
   - [Utilities](#utilities)
6. [Funcionalidades Principales](#funcionalidades-principales)
7. [Flujo de Autenticación](#flujo-de-autenticación)
8. [Flujo de Datos Offline-First](#flujo-de-datos-offline-first)
9. [Sistema BLE](#sistema-ble)
10. [Modo Supervivencia de Batería](#modo-supervivencia-de-batería)

---

## Descripción General

La aplicación Android de GeoRacing está destinada a los **aficionados** que asisten físicamente al evento de automovilismo. Proporciona:

- Mapa interactivo del circuito con navegación peatonal.
- Estado del circuito en tiempo real (banderas, safety car, evacuaciones).
- Sistema de grupos para compartir ubicación con amigos.
- Tienda/pedidos de comida y merchandising.
- Integración con Android Auto para navegación desde el coche.
- Detección BLE de balizas para determinar la zona actual del usuario.
- Modo de bajo consumo de batería.
- Funcionalidades sociales y de gamificación.

---

## Tecnologías y Dependencias

### Lenguaje y Framework principal

| Tecnología | Versión | Justificación |
|---|---|---|
| **Kotlin** | 2.x | Lenguaje oficial de Android, conciso y seguro. Soporte completo de corrutinas. |
| **Jetpack Compose** | Latest | Framework de UI declarativo de Google para Android moderno. Elimina XML y facilita animaciones complejas. |
| **Android Gradle Plugin** | 8.x | Sistema de build de Android. |

### Networking

| Librería | Justificación |
|---|---|
| **Retrofit 2** | Cliente HTTP tipado para consumir la API REST. Serialización JSON automática con Gson/Moshi. |
| **OkHttp** | Cliente HTTP subyacente de Retrofit. Interceptores de logging y certificados SSL. |
| **Firebase Firestore** | Base de datos en la nube para datos en tiempo real (estado del circuito, noticias). |
| **Firebase Auth** | Autenticación de usuarios (email/contraseña, Google Sign-In). |

### Almacenamiento Local

| Librería | Justificación |
|---|---|
| **Room Database** | ORM oficial de Android sobre SQLite. Permite persistencia local para modo offline-first. |
| **DataStore (Preferences)** | Almacenamiento de preferencias de usuario (reemplaza SharedPreferences). |

### Localización y Mapas

| Librería | Justificación |
|---|---|
| **MapLibre Android** | SDK de mapas open-source personalizable. Permite cargar estilos de mapa propios y mostrar el circuito. |
| **Google Fused Location** | Proveedor de ubicación de Google que combina GPS, WiFi y red para mayor precisión y eficiencia energética. |

### UI y Animaciones

| Librería | Justificación |
|---|---|
| **Haze (chrisbanes)** | Efectos de desenfoque/blur avanzados (Liquid Glass UI). |
| **Kyant Backdrop** | Sistema de capas para efectos de material translúcido. |
| **Accompanist** | Utilidades para Compose (permisos, animaciones de navegación). |
| **Lottie** | Animaciones vectoriales (JSON) para la UI. |
| **Coil** | Carga de imágenes asíncrona, compatible con Compose. |

### Bluetooth

| Librería | Justificación |
|---|---|
| **Android Bluetooth LE API** | API nativa de Android para escanear y emitir señales BLE. Las balizas físicas del circuito emiten BLE y la app lo detecta para saber la zona del usuario. |

### Trabajo en Segundo Plano

| Librería | Justificación |
|---|---|
| **WorkManager** | Ejecución de tareas en segundo plano garantizadas (sincronización de telemetría, incluso si la app está cerrada). |
| **Android Foreground Services** | Servicios persistentes para polling del estado del circuito y notificaciones en tiempo real. |

### Integración con Hardware/Sistema

| Librería/API | Justificación |
|---|---|
| **Android Auto (Car App Library)** | Integración con la pantalla del coche. Muestra navegación y estado del circuito mientras el usuario conduce. |
| **Health Connect API** | Acceso a datos de salud del usuario (pasos, frecuencia cardíaca) para la sección EcoMeter. |
| **Text-to-Speech (TTS)** | Anuncios de voz para instrucciones de navegación y alertas de emergencia. |
| **Speech Recognizer** | Comandos de voz para control hands-free. |
| **Camera2 / CameraX** | Acceso a la cámara para el escáner QR y la funcionalidad de Realidad Aumentada. |
| **ML Kit (QR scanning)** | Lectura de códigos QR para posicionamiento preciso en el circuito. |

### In-App Purchases

| Librería | Justificación |
|---|---|
| **Google Play Billing** | Gestión de compras dentro de la aplicación. |

---

## Arquitectura

La app sigue la arquitectura **MVVM (Model-View-ViewModel)** con principios de **Clean Architecture** organizada en capas:

```
┌─────────────────────────────────────────┐
│            UI Layer (Compose)            │
│   Screens + ViewModels + Components     │
└──────────────────┬──────────────────────┘
                   │ StateFlow / UiState
┌──────────────────▼──────────────────────┐
│            Domain Layer                  │
│   Use Cases + Repository Interfaces     │
│   + Domain Models                       │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│            Data Layer                    │
│   Repositories (Offline-First)          │
│   Local (Room) + Remote (Retrofit/FB)   │
│   BLE Scanner + Sensors                 │
└─────────────────────────────────────────┘
```

**Patrón offline-first:** Cada repositorio tiene tres implementaciones:
- `Network*Repository` — Solo accede a la red.
- `OfflineFirst*Repository` — Lee de Room primero, sincroniza con la red en background.
- `Fake*Repository` — Datos simulados para testing y demo.

---

## Estructura de Carpetas

```
app/src/main/java/com/georacing/georacing/
│
├── MainActivity.kt                 # Actividad principal, punto de entrada
├── di/
│   └── AppContainer.kt            # Contenedor DI manual (singletons)
│
├── domain/                        # Capa de dominio (lógica de negocio pura)
│   ├── calculator/                # Cálculos (EcoCalculator)
│   ├── features/                  # Registro de features (FeatureRegistry)
│   ├── manager/                   # Gestores de dominio (Parking, Circuit, etc.)
│   ├── model/                     # Modelos de dominio
│   ├── products/                  # Dominio de productos
│   ├── repository/                # Interfaces de repositorios
│   ├── traffic/                   # Proveedores de tráfico
│   └── usecases/                  # Casos de uso (CheckArrival, UpdateLocation, etc.)
│
├── data/                          # Capa de datos
│   ├── ble/                       # BLE Scanner, Advertiser, PayloadParser
│   ├── billing/                   # BillingManager (Google Play)
│   ├── energy/                    # EnergyMonitor (batería)
│   ├── firebase/                  # Firebase Auth, Firestore services
│   ├── gamification/              # Repositorio de gamificación
│   ├── health/                    # HealthConnectManager
│   ├── local/                     # Room Database, DAOs, Entidades, DataStore
│   │   ├── dao/                   # IncidentDao, BeaconDao, PoiDao, etc.
│   │   ├── entities/              # Entidades Room (BeaconEntity, PoiEntity, etc.)
│   │   ├── mappers/               # Mappers entidad ↔ modelo de dominio
│   │   ├── Converters.kt          # Type converters para Room
│   │   └── GeoRacingDatabase.kt   # Base de datos Room (punto de entrada)
│   ├── map/                       # Configuración MapLibre
│   ├── model/                     # Modelos de datos (DTOs y modelos de datos)
│   ├── offline/                   # Caché offline y repositorio offline
│   ├── orders/                    # Repositorio de pedidos
│   ├── p2p/                       # Chat P2P por proximidad (Nearby API)
│   ├── parking/                   # Repositorio de aparcamiento
│   ├── products/                  # Repositorio de productos
│   ├── remote/                    # Retrofit API, cliente HTTP, DTOs
│   │   ├── dto/                   # Data Transfer Objects
│   │   ├── GeoRacingApi.kt        # Interfaz Retrofit con todos los endpoints
│   │   ├── RetrofitClient.kt      # Configuración del cliente Retrofit
│   │   └── ApiClient.kt           # Wrapper del cliente HTTP
│   ├── repository/                # Implementaciones de repositorios
│   │   ├── Network*Repository.kt  # Solo red
│   │   ├── OfflineFirst*.kt       # Red + caché Room
│   │   ├── Fake*Repository.kt     # Datos simulados para testing
│   │   └── Hybrid*Repository.kt   # Combina red + BLE
│   └── sensors/                   # OrientationManager, OrientationEngine
│
├── ui/                            # Capa UI
│   ├── navigation/                # NavHost, Screen (rutas)
│   ├── screens/                   # Pantallas organizadas por feature
│   │   ├── splash/                # SplashScreen
│   │   ├── login/                 # LoginScreen
│   │   ├── onboarding/            # OnboardingScreen, OnboardingQuizScreen
│   │   ├── home/                  # HomeScreen, HomeViewModel, EditDashboard
│   │   ├── map/                   # MapScreen, MapViewModel
│   │   ├── navigation/            # CircuitNavigationScreen, ViewModel
│   │   ├── incidents/             # IncidentReportScreen, ViewModel
│   │   ├── group/                 # GroupScreen, GroupMapScreen, ViewModel
│   │   ├── poi/                   # PoiListScreen, PoiViewModel
│   │   ├── parking/               # ParkingScreen
│   │   ├── orders/                # OrdersScreen, CartViewModel, etc.
│   │   ├── fan/                   # FanZoneScreen, FanImmersiveScreen
│   │   ├── eco/                   # EcoMeterScreen, EcoViewModel
│   │   ├── settings/              # SettingsScreen, SettingsViewModel
│   │   ├── staff/                 # StaffControlScreen, StaffModeScreen
│   │   ├── ar/                    # ARCameraView, ARCalculator
│   │   ├── medical/               # MedicalLockScreenScreen
│   │   ├── transport/             # TransportScreen
│   │   ├── security/              # SecurityViewModel
│   │   ├── share/                 # ShareQRScreen, ProximityChatScreen
│   │   ├── search/                # SearchScreen
│   │   ├── roadmap/               # RoadmapScreen, ViewModel
│   │   ├── alerts/                # AlertsScreen
│   │   ├── seat/                  # SeatSetupScreen, SeatViewModel
│   │   ├── traffic/               # RouteTrafficScreen
│   │   └── moments/               # MomentsScreen
│   ├── components/                # Componentes reutilizables
│   │   ├── DashboardBottomBar.kt  # Barra de navegación inferior
│   │   ├── GlassComponents.kt     # Componentes con efecto vidrio
│   │   ├── RacingComponents.kt    # Componentes con tema de carreras
│   │   ├── racecontrol/           # LiveFlagOverlay, RaceControlWidget
│   │   ├── circuit/               # CircuitTrack (visualización del circuito)
│   │   ├── map/                   # CrowdHeatmapOverlay
│   │   ├── ar/                    # AROverlayView
│   │   ├── debug/                 # DebugControlPanel
│   │   └── ...                    # Muchos más componentes
│   ├── glass/                     # UI "Liquid Glass" (translúcida)
│   │   ├── LiquidBottomTabs.kt    # Tabs con efecto líquido
│   │   ├── LiquidButton.kt        # Botón con efecto líquido
│   │   ├── LiquidCard.kt          # Tarjeta con efecto líquido
│   │   ├── LiquidDialog.kt        # Diálogo con efecto líquido
│   │   ├── LiquidSlider.kt        # Slider con efecto líquido
│   │   ├── LocalBackdrop.kt       # Proveedor del backdrop para efectos de vidrio
│   │   └── utils/                 # DampedDragAnimation, InteractiveHighlight
│   ├── theme/                     # Tema visual
│   │   ├── Theme.kt               # Definición del tema Material3
│   │   ├── Color.kt               # Paleta de colores
│   │   ├── Type.kt                # Tipografías
│   │   ├── Dimens.kt              # Dimensiones
│   │   ├── Motion.kt              # Animaciones de transición
│   │   └── LiquidModifiers.kt     # Modificadores Compose para UI líquida
│   ├── orders/                    # Pantallas y ViewModels de pedidos
│   └── AppMonitorManager.kt       # Monitor de la app
│
├── services/                      # Servicios Android en segundo plano
│   ├── StatePollingService.kt     # Foreground Service: polling del estado del circuito
│   ├── LiveSessionService.kt      # Servicio de sesión en directo
│   ├── LiveNotificationManager.kt # Gestión de notificaciones en tiempo real
│   ├── NetworkMonitor.kt          # Monitor de conectividad
│   ├── BatteryMonitor.kt          # Monitor de batería
│   ├── VoiceCommandManager.kt     # Gestión de comandos de voz
│   └── NotificationFactory.kt    # Fábrica de notificaciones
│
├── infrastructure/                # Infraestructura de sistema
│   ├── ble/                       # BLE Command Service, Staff Beacon Advertiser
│   ├── car/                       # CarTransitionManager, CarConnectionManager
│   ├── health/                    # HealthConnectManager
│   ├── security/                  # MedicalWallpaperGenerator
│   └── telemetry/                 # BlackBoxLogger, TelemetrySyncWorker
│
├── car/                           # Módulo Android Auto
│   ├── GeoRacingCarAppService.kt  # Servicio raíz de Android Auto
│   ├── GeoRacingCarSession.kt     # Sesión de Car App
│   ├── GeoRacingCarScreen.kt      # Pantalla principal en el coche
│   ├── GeoRacingNavigationScreen.kt # Pantalla de navegación en el coche
│   ├── NavigationHUD.kt           # HUD de navegación
│   ├── NavigationSession.kt       # Sesión de navegación activa
│   ├── RoutePlanner.kt            # Planificador de rutas OSRM
│   ├── OsrmService.kt             # Cliente OSRM (enrutamiento)
│   ├── RaceStatusScreen.kt        # Pantalla de estado de la carrera
│   ├── DestinationSearchScreen.kt # Búsqueda de destino en el coche
│   ├── PoiListScreen.kt           # Lista de POIs en el coche
│   ├── SpeedometerView.kt         # Velocímetro
│   ├── CircuitRenderer.kt         # Renderizador del circuito en el coche
│   └── ParkingProximityDetector.kt # Detector de proximidad de aparcamientos
│
├── navigation/
│   └── NavigationEngine.kt        # Motor principal de navegación peatonal
│
├── feature/navigation/routing/    # Feature de navegación
│   ├── domain/
│   │   └── FindPedestrianRouteUseCase.kt # Caso de uso para rutas peatonales
│   ├── algorithm/
│   │   └── PedestrianPathfinder.kt       # Algoritmo de pathfinding peatonal
│   └── models/
│       ├── CircuitGraph.kt        # Grafo del circuito para navegación
│       ├── RoutePreference.kt     # Preferencias de ruta (accesibilidad, distancia)
│       └── SurfaceType.kt         # Tipos de superficie (asfalto, hierba, etc.)
│
├── features/ar/                   # Realidad Aumentada
│   ├── AREnhancedOverlay.kt       # Overlay AR mejorado
│   └── QRCodePositioningManager.kt # Posicionamiento preciso por QR
│
├── debug/
│   └── ScenarioSimulator.kt       # Simulador de escenarios para testing
│
├── utils/                         # Utilidades
│   ├── TTSManager.kt              # Gestión de Text-to-Speech
│   ├── VoiceAnnouncer.kt          # Anunciador de voz
│   ├── DistanceCalculator.kt      # Cálculo de distancias geográficas
│   ├── ETACalculator.kt           # Cálculo de tiempo estimado de llegada
│   ├── RouteSnapper.kt            # Ajuste de posición a ruta
│   ├── OffRouteDetector.kt        # Detección de desvío de ruta
│   ├── StepDetector.kt            # Detector de pasos del podómetro
│   ├── MedicalLockScreenGenerator.kt # Generador de pantalla de bloqueo médica
│   └── ImageUtils.kt              # Utilidades de imagen
│
└── core/battery/                  # Modo batería
    ├── domain/
    │   ├── SurvivalMode.kt        # Definición del modo supervivencia
    │   ├── SurvivalModeManager.kt # Gestor del modo supervivencia
    │   ├── BatteryMonitor.kt      # Interfaz del monitor de batería
    │   └── BatteryState.kt        # Estado de la batería
    ├── data/
    │   └── BatteryMonitorImpl.kt  # Implementación del monitor de batería
    └── ui/
        └── MapSurvivalViewModel.kt # ViewModel del modo supervivencia en el mapa
```

---

## Módulos y Ficheros Clave

### Punto de Entrada

#### `MainActivity.kt`
La actividad principal de la aplicación. Es el único `Activity` de la app (arquitectura Single Activity).

**Responsabilidades:**
1. **Solicitar permisos** al iniciar: Ubicación, Bluetooth (Android 12+), Notificaciones (Android 13+), Reconocimiento de actividad.
2. **Inicializar el `AppContainer`** (contenedor de dependencias manual).
3. **Iniciar el `StatePollingService`** (foreground service) si los permisos de ubicación ya están concedidos.
4. **Configurar el árbol de Compose** completo con:
   - `GeoRacingTheme` — Tema material con soporte OLED black.
   - `LocalEnergyProfile` — Perfil energético para adaptar la UI en modo supervivencia.
   - `LocalGlassConfig` — Configuración del sistema de vidrio líquido.
   - `HazeState` — Estado compartido para efectos de blur.
   - `LayerBackdrop` — Backdrop compartido para translucidez.
5. **Renderizar el `GeoRacingNavHost`** con la navegación completa.
6. **Superponer overlays globales**: Barra inferior, SurvivalModeBanner, LiveFlagOverlay, ParkingConfirmationDialog, DebugControlPanel.
7. **Gestionar el ciclo de vida** del EnergyMonitor (start/stop en onStart/onStop).

---

### Inyección de Dependencias (DI)

#### `di/AppContainer.kt`
Contenedor DI **manual** (sin Hilt). Usa `lazy` para inicialización diferida de todos los singletons.

**¿Por qué DI manual en vez de Hilt?**  
El simulador de escenarios (`ScenarioSimulator`) y otras partes del código necesitan acceder a las mismas instancias. Con el contenedor manual se garantiza que toda la app use exactamente las mismas instancias.

**Singletons gestionados:**
- `energyMonitor` — Monitor de batería.
- `beaconScanner` — Escáner BLE.
- `carTransitionManager` — Gestor de transición auto↔caminando.
- `healthConnectManager` — Acceso a Health Connect.
- `beaconsRepository` — Repositorio de balizas (OfflineFirst).
- `circuitStateRepository` — Repositorio del estado del circuito (Hybrid: red + BLE).
- `database` — Base de datos Room.
- `api` — Cliente Retrofit.
- `poiRepository` — Repositorio de puntos de interés.
- `incidentsRepository` — Repositorio de incidentes.
- `autoParkingManager` — Gestor de aparcamiento automático.
- `parkingRepository` — Repositorio de aparcamiento.

---

### Domain Layer (Dominio)

Contiene la **lógica de negocio pura**, sin dependencias de Android ni de frameworks externos.

#### `domain/repository/` — Interfaces de repositorios
Contratos que definen qué operaciones existen. La capa UI y los casos de uso dependen de estas interfaces, no de implementaciones concretas.

- `BeaconsRepository` — Gestión de balizas BLE del circuito.
- `CircuitStateRepository` — Estado del circuito (modo, bandera, temperatura).
- `IncidentsRepository` — Incidentes reportados.
- `PoiRepository` — Puntos de interés.

#### `domain/usecases/` — Casos de uso
Encapsulan operaciones de negocio específicas:
- `UpdateLocationUseCase` — Actualiza la posición del usuario.
- `CheckArrivalUseCase` — Comprueba si el usuario ha llegado a su destino.
- `RecalculateRouteUseCase` — Recalcula la ruta cuando el usuario se desvía.

#### `domain/manager/`
Gestores de lógica compleja:
- `AutoParkingManager` — Detecta automáticamente cuando el usuario aparca y ofrece guardar la posición.
- `CircuitControlManager` — Control del circuito por el staff.
- `GateAssignmentManager` — Asignación de puertas de acceso.
- `TransitionModeDetector` — Detecta la transición de conducir a caminar.
- `QrPositioningManager` — Posicionamiento preciso mediante QR.

#### `domain/calculator/`
- `EcoCalculator` — Calcula métricas ecológicas (huella de carbono del viaje).

#### `domain/features/`
- `FeatureRegistry.kt` — Registro de features dinámicas habilitables/deshabilitables.

---

### Data Layer (Datos)

#### `data/ble/` — Bluetooth Low Energy
- **`BeaconScanner.kt`** — Escanea continuamente las señales BLE del circuito. Filtra por el Manufacturer ID `0x1234`. Parsea el payload de 9 bytes para extraer Zone ID, Mode, Sequence, TTL y Temperature.
- **`BeaconAdvertiser.kt`** — Emite señales BLE desde el teléfono (modo staff para que las balizas sepan que el personal está cerca).
- **`BlePayloadParser.kt`** — Parser del payload BLE propietario.
- **`BleCircuitSignal.kt`** — Modelo del dato BLE parseado.
- **`DetectedBeacon.kt`** — Modelo de una baliza detectada (incluye RSSI para calcular distancia).

#### `data/remote/` — API REST
- **`GeoRacingApi.kt`** — Interfaz Retrofit con todos los endpoints declarados (anotaciones `@GET`, `@POST`, etc.).
- **`RetrofitClient.kt`** — Configuración del cliente Retrofit (URL base, interceptores, timeouts).
- **`ApiClient.kt`** — Wrapper que añade reintentos y manejo de errores.
- **`dto/`** — Data Transfer Objects para la serialización JSON:
  - `CircuitStateDto` — Estado del circuito desde la API.
  - `BeaconConfigDto` — Configuración de baliza.
  - `IncidentReportDto` — Reporte de incidente.
  - `GroupLocationDto` — Ubicación de grupo.
  - `PoiDto` — Punto de interés.
  - `UserRequest` / `GroupCreateRequest` — Peticiones de usuario/grupo.

#### `data/local/` — Base de Datos Room (SQLite)
- **`GeoRacingDatabase.kt`** — Punto de entrada de Room. Define todas las entidades y DAOs.
- **`dao/`**:
  - `BeaconDao` — Operaciones CRUD sobre balizas.
  - `PoiDao` — Operaciones CRUD sobre POIs.
  - `IncidentDao` — Operaciones CRUD sobre incidentes.
  - `CircuitStateDao` — Lectura/escritura del estado del circuito.
  - `TelemetryDao` — Almacenamiento de datos de telemetría.
  - `MedicalInfoDao` — Datos médicos del usuario.
- **`entities/`** — Entidades de la base de datos (mapeo tabla↔clase).
- **`mappers/Mappers.kt`** — Conversión entre entidades Room y modelos de dominio.
- **`Converters.kt`** — Conversores de tipos para Room (ej: List↔String JSON).
- **`UserPreferencesDataStore.kt`** — Preferencias del usuario con DataStore.

#### `data/firebase/`
- **`FirebaseAuthService.kt`** — Autenticación con Firebase (email, Google).
- **`FirebaseFirestoreService.kt`** — Lectura/escritura en Firestore.
- **`FirebaseInitializer.kt`** — Inicialización de Firebase.

#### `data/repository/` — Implementaciones de Repositorios

Patrón **Offline-First**:
1. La app muestra datos cacheados de Room inmediatamente.
2. En background, sincroniza con la API REST.
3. Si la red falla, la app sigue funcionando con los datos cacheados.

- `OfflineFirstBeaconsRepository` — Balizas: Room caché + sincronización con API.
- `OfflineFirstCircuitStateRepository` — Estado del circuito: Room + Hybrid (red + BLE).
- `OfflineFirstPoiRepository` — POIs: Room + API.
- `HybridCircuitStateRepository` — Combina API REST y señales BLE para el estado del circuito. Si no hay internet, usa el estado inferido por BLE.
- `NetworkBeaconsRepository` / `NetworkCircuitStateRepository` — Solo red, sin caché.
- `FakeBeaconsRepository` / `FakePoiRepository` / etc. — Datos falsos para testing.
- `FirebaseIncidentsRepository` — Incidentes leídos/escritos en Firestore.

#### `data/energy/`
- **`EnergyMonitor.kt`** — Monitorea el nivel de batería y calcula el `EnergyProfile` (Normal, PowerSave, Survival). Activa el modo supervivencia automáticamente cuando la batería baja de cierto umbral.

#### `data/p2p/`
- **`NearbyP2PService.kt`** — Chat P2P entre usuarios cercanos usando Google Nearby API.
- **`ProximityChatManager.kt`** — Gestión de mensajes de chat por proximidad.

#### `data/sensors/`
- **`OrientationManager.kt`** — Lee el sensor de orientación del dispositivo para la brújula del mapa.
- **`OrientationEngine.kt`** — Procesa y filtra los datos del sensor de orientación.

---

### UI Layer (Interfaz de Usuario)

#### `ui/navigation/`
- **`Screen.kt`** — Enum/sealed class con todas las rutas de navegación. Ejemplo: `Screen.Home.route = "home"`.
- **`GeoRacingNavHost.kt`** — Composable que define el `NavHost` con todas las rutas y sus composables asociados.

#### Pantallas Principales

**`ui/screens/home/HomeScreen.kt`**  
Pantalla principal de la app. Dashboard personalizable donde el usuario puede añadir/quitar widgets (estado del circuito, mapa miniatura, pedidos, etc.).

**`ui/screens/map/MapScreen.kt`**  
Mapa del circuito con MapLibre. Muestra:
- Posición del usuario en tiempo real.
- Balizas del circuito.
- Densidad de crowd (heatmap).
- POIs (baños, comida, salidas, etc.).
- Estado del circuito en superposición.

**`ui/screens/navigation/CircuitNavigationScreen.kt`**  
Navegación peatonal paso a paso dentro del circuito. Usa el `NavigationEngine` y el `PedestrianPathfinder`.

**`ui/screens/incidents/IncidentReportScreen.kt`**  
Formulario para reportar incidentes (aglomeración, objeto perdido, problema médico, etc.).

**`ui/screens/group/GroupMapScreen.kt`**  
Mapa que muestra en tiempo real la posición de todos los miembros del grupo.

**`ui/screens/orders/OrdersScreen.kt`**  
Tienda de comida y merchandising. Los pedidos se recogen en puntos designados (Click & Collect).

**`ui/screens/fan/FanZoneScreen.kt`**  
Zona de fans: noticias, encuestas, quiz, gamificación.

**`ui/screens/eco/EcoMeterScreen.kt`**  
Métricas ecológicas: pasos dados, distancia recorrida, huella de carbono del viaje al evento.

**`ui/screens/staff/StaffModeScreen.kt`**  
Pantalla de control para el personal del evento. Solo visible en modo staff.

#### `ui/glass/` — Liquid Glass UI

Sistema de UI translúcida inspirado en el diseño de Apple. Los componentes "glass" muestran el contenido detrás de ellos con un efecto de blur y refracción de luz.

- **`LiquidBottomTabs.kt`** — Barra de tabs inferior con efecto de vidrio líquido.
- **`LiquidButton.kt`** — Botón translúcido con animación de presión.
- **`LiquidCard.kt`** — Tarjeta con fondo translúcido.
- **`LiquidDialog.kt`** — Diálogo con fondo translúcido.
- **`LiquidTopBar.kt`** — Barra superior translúcida.
- **`LiquidListItem.kt`** — Elemento de lista translúcido.
- **`LocalBackdrop.kt`** — CompositionLocal que provee el `LayerBackdrop` a todos los componentes glass.
- **`GlassSupport.kt`** — Detector de si el dispositivo soporta efectos de vidrio (deshabilita en emuladores).
- **`GlassConfig.kt`** — Configuración de los efectos de vidrio (intensidad del blur, opacidad, etc.).
- **`utils/DampedDragAnimation.kt`** — Animación amortiguada para gestos drag.
- **`utils/InteractiveHighlight.kt`** — Efecto de highlight cuando el usuario toca.
- **`utils/DragGestureInspector.kt`** — Inspector de gestos de arrastre.

#### `ui/theme/`
- **`Theme.kt`** — Tema Material3 de la app. Soporta modo oscuro y modo OLED black (forzado cuando la batería es baja).
- **`Color.kt`** — Paleta de colores corporativos de GeoRacing.
- **`Type.kt`** — Sistema tipográfico (fuentes, tamaños).
- **`Dimens.kt`** — Dimensiones estándar.
- **`Motion.kt`** — Especificaciones de animación y transición.
- **`LiquidModifiers.kt`** — Modificadores Compose que añaden el efecto glass a cualquier componente.

#### Componentes Reutilizables

- **`DashboardBottomBar.kt`** — Barra de navegación inferior. Cambia de apariencia según el scroll y el modo de energía.
- **`SurvivalModeBanner.kt`** — Banner amarillo que avisa del modo batería baja.
- **`ConnectivityAwareScaffold.kt`** — Scaffold que muestra un banner de "sin conexión" cuando la red no está disponible.
- **`racecontrol/LiveFlagOverlay.kt`** — Overlay global que muestra la bandera de carrera actual (amarilla, roja, safety car).
- **`circuit/CircuitTrack.kt`** — Componente Canvas que dibuja el trazado del circuito.
- **`map/CrowdHeatmapOverlay.kt`** — Mapa de calor de densidad de gente.
- **`debug/DebugControlPanel.kt`** — Panel de debug oculto (accesible con long press en esquina superior izquierda). Permite simular estados de batería, BLE y conexión del coche.

---

### Services (Servicios en segundo plano)

#### `StatePollingService.kt`
**Foreground Service** que corre continuamente mientras la app está activa. Hace polling del estado del circuito en la API y actualiza las notificaciones en tiempo real del sistema operativo (Live Notifications en Android 14+).

**¿Por qué Foreground Service?**  
Android mata los procesos en background agresivamente. Para recibir actualizaciones de emergencia (evacuación) incluso cuando la app no está en primer plano, se necesita un Foreground Service con notificación visible.

#### `LiveSessionService.kt`
Servicio de sesión en directo que mantiene la conexión con el backend durante un evento activo.

#### `LiveNotificationManager.kt`
Gestiona las notificaciones en tiempo real (Live Activities en Android 14):
- Muestra el estado de la carrera en la barra de notificaciones.
- Actualiza la temperatura del circuito.
- Alerta de evacuaciones.

#### `NetworkMonitor.kt`
Monitorea la conectividad de red y notifica a los repositorios para que cambien a modo offline/online.

#### `BatteryMonitor.kt`
Servicio de monitoreo de batería a nivel de servicio (complementa al `EnergyMonitor`).

#### `VoiceCommandManager.kt`
Gestiona el reconocimiento de voz para comandos hands-free:
- "Ir a [destino]"
- "Mostrar mapa"
- "Pedir comida"

---

### Infrastructure

#### `infrastructure/ble/`
- **`BleCommandService.kt`** — Servicio BLE para recibir comandos del staff mediante señales BLE.
- **`StaffBeaconAdvertiser.kt`** — Emite señal BLE identificando al usuario como staff.

#### `infrastructure/car/`
- **`CarTransitionManager.kt`** — Detecta cuando el usuario pasa de estar en el coche a caminar (usando sensor de pasos y velocidad GPS).
- **`CarConnectionManager.kt`** — Gestiona la conexión con Android Auto.

#### `infrastructure/health/`
- **`HealthConnectManager.kt`** — Accede a Health Connect para leer pasos, distancia y frecuencia cardíaca del usuario.

#### `infrastructure/security/`
- **`MedicalWallpaperGenerator.kt`** — Genera un fondo de pantalla con la información médica de emergencia del usuario (tipo de sangre, alergias, medicación). Visible incluso con pantalla bloqueada.

#### `infrastructure/telemetry/`
- **`BlackBoxLogger.kt`** — Logger de "caja negra" que registra todos los eventos importantes.
- **`TelemetrySyncWorker.kt`** — WorkManager Worker que sincroniza la telemetría local con el servidor periódicamente.

---

### Android Auto (Car)

Módulo completo de integración con **Android Auto**. Se activa cuando el teléfono se conecta a la pantalla del coche.

#### `GeoRacingCarAppService.kt`
Punto de entrada del módulo Car App. Registrado en el `AndroidManifest.xml` con los meta-datos necesarios para Android Auto.

#### `GeoRacingCarSession.kt`
Gestiona el ciclo de vida de la sesión de Car App. Crea las pantallas y gestiona el backstack de navegación del coche.

#### `GeoRacingCarScreen.kt`
Pantalla principal en el coche. Muestra:
- Estado del circuito actual.
- Lista de POIs cercanos.
- Acceso rápido a navegación.

#### `NavigationSession.kt`
Gestiona una sesión de navegación activa en el coche. Muestra instrucciones turn-by-turn, velocidad actual y ETA.

#### `RoutePlanner.kt`
Calcula rutas usando **OSRM** (Open Source Routing Machine). Soporte para rutas peatonales y de conducción.

#### `OsrmService.kt`
Cliente HTTP para la API de OSRM. Parsea respuestas GeoJSON de rutas.

#### `CircuitRenderer.kt`
Renderiza el trazado del circuito en la pantalla del coche usando la API de dibujo de Car App.

---

### Navigation (Motor de Navegación)

#### `NavigationEngine.kt`
Motor principal de navegación peatonal. Coordina:
1. Posición del usuario (GPS).
2. Ruta calculada por `PedestrianPathfinder`.
3. Instrucciones turn-by-turn.
4. Recálculo automático si el usuario se desvía (`OffRouteDetector`).
5. Anuncios de voz via `TTSManager`.

#### `feature/navigation/routing/algorithm/PedestrianPathfinder.kt`
Implementa el algoritmo de pathfinding (A* o Dijkstra) sobre el grafo peatonal del circuito (`CircuitGraph`). Soporta preferencias de ruta (accesibilidad, distancia mínima, etc.).

---

### Utilities

#### `TTSManager.kt`
Gestión del Text-to-Speech de Android. Soporta varios idiomas (ES, EN, FR, DE, IT). Se usa para:
- Instrucciones de navegación ("En 20 metros, gira a la izquierda").
- Alertas de emergencia ("Evacuación en progreso").
- Notificaciones de llegada.

#### `DistanceCalculator.kt`
Cálculo de distancias geográficas usando la fórmula de Haversine. Útil para calcular la distancia entre el usuario y su destino.

#### `ETACalculator.kt`
Calcula el tiempo estimado de llegada basándose en la distancia restante y la velocidad media de paso a pie.

#### `RouteSnapper.kt`
Ajusta la posición GPS del usuario al punto más cercano de la ruta planificada. Reduce el efecto del "jitter" del GPS.

#### `OffRouteDetector.kt`
Detecta si el usuario se ha desviado de la ruta más de un umbral de metros. Si es así, notifica al `NavigationEngine` para recalcular.

#### `StepDetector.kt`
Usa el sensor de pasos (pedómetro) del dispositivo para contar pasos. Se usa en el EcoMeter.

---

## Funcionalidades Principales

### Estado del Circuito en Tiempo Real

El circuito puede estar en varios modos que se muestran a los usuarios:
- **NORMAL** — Carrera en progreso normal.
- **SAFETY_CAR** — Safety car en pista.
- **RED_FLAG** — Bandera roja, carrera pausada.
- **EVACUATION** — Evacuación de emergencia.

Los datos llegan por dos canales simultáneos:
1. **API REST** — `HybridCircuitStateRepository` consulta la API cada pocos segundos.
2. **BLE** — `BeaconScanner` detecta las señales de las balizas físicas cercanas.

El modo BLE tiene prioridad sobre la API cuando hay señal, ya que es más cercano al estado físico real del circuito.

### Sistema de Grupos

Los usuarios pueden crear/unirse a un grupo para compartir ubicación en tiempo real:
- El `GroupLocationRepository` guarda la posición de cada miembro en la API/Firestore.
- El `GroupMapScreen` muestra todos los miembros en el mapa.
- El `GroupViewModel` gestiona la lógica de creación/unión/salida del grupo.

### Sistema de Pedidos

Los usuarios pueden pedir comida o merchandising:
1. Navegan por el catálogo (`OrdersScreen`).
2. Añaden productos al carrito (`CartViewModel`).
3. Confirman el pedido (`OrderConfirmationScreen`).
4. Reciben notificación cuando el pedido está listo.
5. Recogen el pedido en el punto designado (`ClickCollectScreen`).

---

## Flujo de Autenticación

```
Inicio de App
    │
    ▼
SplashScreen
    │ Comprueba si hay sesión activa en Firebase
    ├── Sesión activa → HomeScreen
    └── Sin sesión → LoginScreen
            │
            ├── Email/Contraseña → Firebase Auth
            ├── Google Sign-In → Firebase Auth
            └── Onboarding para nuevos usuarios → OnboardingScreen
                    │
                    └── Selección de equipo favorito, configuración inicial
                            │
                            └── HomeScreen
```

---

## Flujo de Datos Offline-First

```
ViewModel solicita datos
    │
    ▼
OfflineFirstRepository
    │
    ├── 1. Emite datos de Room inmediatamente (UI actualizada rápido)
    │
    └── 2. En background: fetch de la API REST
            │
            ├── Éxito → Guarda en Room → Room emite actualización → UI actualizada
            └── Error → Room sigue emitiendo datos cacheados → UI muestra datos cacheados
```

Este patrón garantiza que la app **siempre muestre algo útil**, incluso sin internet.

---

## Sistema BLE

### Escaneo (BeaconScanner)

La app escanea continuamente el BLE en background. Cuando detecta un paquete con Manufacturer ID `0x1234`:
1. Parsea el payload de 9 bytes.
2. Extrae el Zone ID, Mode, Sequence y Temperature.
3. Si el TTL ha expirado (> 10 segundos), descarta el dato.
4. Si el número de secuencia no ha cambiado respecto al anterior, descarta el dato (para evitar datos estancados).
5. Actualiza el repositorio de estado del circuito con el modo detectado.
6. Notifica al usuario si el modo ha cambiado (especialmente EVACUATION).

### Advertising (BeaconAdvertiser / StaffBeaconAdvertiser)

En modo staff, el teléfono puede emitir señales BLE para identificarse como personal del evento. Esto permite a las balizas físicas saber que hay staff cerca.

---

## Modo Supervivencia de Batería

Cuando la batería baja del umbral configurado (ej: 20%), el `EnergyMonitor` activa el `EnergyProfile.Survival`:

1. **UI** — `GeoRacingTheme` fuerza el modo OLED black (fondo completamente negro, ahorra energía en pantallas OLED).
2. **Mapa** — El `MapSurvivalViewModel` reduce la frecuencia de actualización del mapa.
3. **BLE** — El escaneo BLE se reduce a intervalos más largos.
4. **Servicios** — Se desactivan funcionalidades no esenciales (AR, vídeos, etc.).
5. **UI** — Se muestra el `SurvivalModeBanner` para avisar al usuario.

El `ScenarioSimulator` permite forzar este modo manualmente desde el panel de debug para testing.
