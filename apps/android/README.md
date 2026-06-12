# GeoRacing — Android App

Native Android application for **spectators attending a motorsport event**, with a dedicated staff mode. It turns the phone into an event companion: an interactive circuit map, real-time circuit status (flags, safety car, evacuation), BLE beacon detection to know which zone of the venue you are in, group location sharing, food/merchandising ordering, and Android Auto integration for the drive to the circuit.

> Detailed docs (Spanish): see [`documentation.md`](./documentation.md) for a full module-by-module technical reference (architecture, data flows, BLE protocol, battery survival mode).

## What the app does (as implemented in the code)

- **Live circuit state** — `HybridCircuitStateRepository` merges two channels: REST polling of the backend (`StatePollingService`, a foreground service) and direct **BLE beacon scanning** (`data/ble/BeaconScanner`, `BlePayloadParser`). Beacons broadcast a 9-byte payload (zone id, mode, sequence, temperature); BLE takes priority over the API when in range. State changes (SAFETY_CAR, RED_FLAG, EVACUATION) trigger local notifications and, for evacuation, a dedicated full-screen `EvacuationActivity`.
- **Circuit map & pedestrian navigation** — MapLibre-based map with POI layers, crowd heatmap overlay, and a custom pedestrian routing engine (`NavigationEngine`, A*/Dijkstra `PedestrianPathfinder` over a `CircuitGraph`), with off-route detection, route snapping, ETA calculation and TTS voice guidance.
- **Android Auto** — full Car App Library integration (`car/` package: `GeoRacingCarAppService`, navigation session, OSRM-based route planner, speedometer, circuit renderer on the car screen).
- **Groups & social** — share live location with friends (`GroupLocationRepository`, group map), QR-based session sharing, Nearby Connections P2P proximity chat.
- **Shop / orders** — product catalog, cart, checkout and click-and-collect flow, with an `OrderStatusWorker` (WorkManager) for status updates; Google Play Billing wired in (`data/billing`).
- **Staff mode** — staff screens plus `StaffBeaconAdvertiser` / `BleCommandService` so a staff phone can advertise itself over BLE and interact with trackside beacons.
- **Safety & health** — incident reporting, medical info stored locally with a lock-screen "medical wallpaper" generator, Health Connect integration (steps/distance) feeding an EcoMeter, emergency broadcast use case.
- **Battery survival mode** — `EnergyMonitor` + `SurvivalModeManager` switch the app to an OLED-black theme, slow down map/BLE refresh and disable non-essential features when battery is low.
- **Offline-first** — Room database + DataStore; repositories come in three flavors (`Network*`, `OfflineFirst*`, `Fake*`) so the UI always renders cached data when the network drops. A debug `ScenarioSimulator` and in-app `DebugControlPanel` let you force states without real infrastructure.

## Architecture

MVVM + Clean Architecture layers, manual dependency injection (no Hilt) via `di/AppContainer.kt`. Single activity (`MainActivity`) hosting a Jetpack Compose `NavHost`.

```
app/src/main/java/com/georacing/georacing/
├── MainActivity.kt        # Single-activity Compose entry point
├── di/                    # Manual DI container (AppContainer)
├── domain/                # Models, use cases, repository interfaces, managers
├── data/                  # Repositories (Network / OfflineFirst / Fake), Room,
│   │                      #   Retrofit API client, Firebase (Auth/Firestore),
│   ├── ble/               #   BLE beacon scanner/advertiser + payload parser
│   ├── local/             #   Room DB (beacons, circuit state, incidents, POIs,
│   │                      #   telemetry, medical info) + DataStore prefs
│   └── remote/            #   Retrofit API + DTOs + OSRM/Open-Meteo clients
├── ui/                    # Compose screens (~30 feature screens), components,
│                          #   glass/blur design system, theme, navigation graph
├── services/              # Foreground services: StatePollingService,
│                          #   LiveSessionService, notifications, voice commands
├── car/                   # Android Auto (Car App Library) screens & nav engine
├── navigation/ feature/   # Pedestrian routing engine (A* over circuit graph)
├── infrastructure/        # BLE command service, telemetry black-box, car
│                          #   transition manager, Health Connect, security
├── core/                  # Battery monitoring / survival mode
└── debug/                 # ScenarioSimulator for demoing states
```

## Tech stack

| Area | Technology |
|---|---|
| Language / UI | Kotlin 2.x, Jetpack Compose (Material 3) |
| Build | Gradle 8.13 (wrapper), Android Gradle Plugin 8.x |
| Networking | Retrofit 2 + OkHttp; Firebase Firestore & Firebase Auth (incl. Google Sign-In) |
| Local storage | Room, DataStore Preferences |
| Maps / location | MapLibre Android, Google Fused Location, OSRM (routing), Open-Meteo (weather) |
| Background work | WorkManager, foreground services |
| Hardware | Android BLE (scan + advertise), Nearby Connections, CameraX + ML Kit (QR), Health Connect, TTS / Speech Recognizer |
| UI extras | Haze (blur/glass), Kyant Backdrop, Lottie, Coil, Accompanist |
| Car | Android Auto Car App Library (navigation category) |
| Payments | Google Play Billing |

## Setup

1. **Firebase project (required to build).** Create your own Firebase project, register an Android app whose package name matches the project's `applicationId`, and download its config:
   ```
   cp app/google-services.json.example app/google-services.json
   ```
   Then replace the placeholder values (`YOUR_PROJECT_NUMBER`, `YOUR_API_KEY`, etc.) with the ones from your Firebase console — or simply drop in the real `google-services.json` you downloaded. Enable **Authentication** (Email/Password + Google) and **Firestore**.
2. **Open in Android Studio** (latest stable). Let it sync the Gradle project.
3. **Build:**
   ```
   ./gradlew assembleDebug
   ```
   (on Windows: `gradlew.bat assembleDebug`).
4. **Backend.** The REST base URL is hardcoded in `data/remote/ApiClient.kt`; point it at your own deployment of the GeoRacing backend. Without a backend the app still runs using the offline cache and the `Fake*` repositories / debug scenario simulator.

## Known limitations

- **Build scripts were restored from a slightly earlier team snapshot** (root and `:app` `build.gradle.kts`, version catalog, Gradle 8.13 wrapper). The source set published here is a few iterations newer, so a handful of newer classes may need an extra dependency added to `app/build.gradle.kts` — fix as reported by the compiler.
- The backend API URL in `data/remote/ApiClient.kt` is a placeholder (`georacing.example.com`); point it at your own deployment of the [backend](../../backend).
- Several data sources have `Fake*` implementations used for demos/testing; some features (AR overlay, gamification, payments) are demo-grade rather than production-complete.
- BLE features require real beacons broadcasting the expected manufacturer payload (manufacturer ID `0x1234`, 9-byte payload — see `documentation.md`, "Sistema BLE"); without them the app falls back to API polling.
- The detailed technical documentation (`documentation.md`) is in Spanish.
