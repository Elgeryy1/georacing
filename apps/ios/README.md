# GeoRacing — iOS App

Native SwiftUI port of the GeoRacing Android app: an event companion for **spectators at a motorsport venue** — live circuit status (flags, safety car, evacuation), circuit map with POIs, BLE beacon zone detection, group location sharing via QR, incident reporting, a small shop, and CarPlay support.

**Honest status:** this is an **earlier-stage port** of the Android app, built explicitly for feature parity ("TOTAL FUNCTIONAL PARITY with Android" is the stated goal). The core loop is implemented and working against the real API, but of the ~35 features registered in `FeatureRegistry.swift` only ~8 are complete, ~4 are basic MVPs, and the rest are placeholders rendered through a generic `FeaturePlaceholderView`. The two tracking documents in this folder are the source of truth:

- [`FEATURES.md`](./FEATURES.md) — feature-by-feature parity map (Spanish)
- [`PARITY_CHECKLIST.md`](./PARITY_CHECKLIST.md) — Android-vs-iOS checklist with statuses

## What is implemented (per the code and parity docs)

**Complete / working**
- **Circuit status** — `CircuitStatusRepository` + `HybridCircuitStateRepository` poll the backend and handle NORMAL / SAFETY_CAR / RED_FLAG / EVACUATION, with banner UI (`HomeView`, `TrackStatus`), a full-screen `EvacuationView`, and local push notifications on state change (`LocalNotificationManager`).
- **BLE beacons** — `Data/BLE/BeaconScanner` scans circuit beacons in the background and feeds zone/state detection, mirroring the Android BLE logic.
- **Map & POIs** — `CircuitMapView` + `MapViewModel` (MapKit) with POIs fetched from the real API and type filters; routing exists as a stub architecture (`RouteManager`, `RouteSnapper`, `OffRouteDetector`) without full OSRM routing yet.
- **Auth** — Firebase Auth + Google Sign-In (`AuthService`, `LoginView`).
- **Shop** — product list with real images, cart (`CartManager`), checkout writing orders to the backend; order history is not implemented.
- **Social** — QR-based group session share/scan (`SocialView`, CoreImage QR), group members on the map; meet-up points are placeholders.
- **Incidents** — `IncidentReportView` posts reports to the API.
- **Onboarding & settings** — permission onboarding, language/theme/seat settings (`UserPreferences`).
- **CarPlay** — scene delegate, template factory and state sync under `GeoRacing/CarPlay/`.
- **Extras with real code** — contextual home card widgets, fan news, fan zone quiz/rewards, parking wizard with QR, team theming (`team_catalog.json`, per-team assets), TTS/speech services, telemetry logger, energy management.

**Basic / placeholder**
- Offline map tile persistence, alerts center, staff panel and group following are "basic". AR guidance, anti-queue routes, dynamic routes, QR positioning, Fan Immersive/360 and the visionary features (FlowSense etc.) are placeholders — see `FEATURES.md`.

Tests live in `GeoRacingTests/` (including `ParityTests.swift`) and `GeoRacingUITests/`.

## Architecture

MVVM with a feature-registry-driven navigation system:

```
GeoRacing/
├── GeoRacingApp.swift       # @main; Firebase + Google Sign-In + CarPlay scenes
├── ContentView.swift        # Root tab/modal routing
├── Core/                    # Constants, location, notifications, design system
├── Data/
│   ├── BLE/                 # BeaconScanner, BLE circuit signal parsing
│   ├── Repositories/        # Circuit status, groups, orders, products, routes…
│   └── Services/            # APIService (URLSession), AuthService, DatabaseClient
│                            #   (generic _read/_upsert client), TransportAPIClient…
├── Domain/
│   ├── Models/              # Codable domain models
│   ├── Services/            # CartManager, RouteManager, TTS, telemetry, energy…
│   └── Features/            # FeatureRegistry (source of truth, ~35 features)
│                            #   + FeatureViewFactory (maps feature id → view)
├── Presentation/
│   ├── Views/               # ~35 SwiftUI screens (Home, CircuitMap, Evacuation,
│   │                        #   Orders, Social, StaffMode, Settings…)
│   ├── ViewModels/          # Map, Navigation, Group, Orders, FanZone, Alerts…
│   ├── ContextualCard/      # Home contextual card + widgets
│   ├── Parking/             # Parking wizard (own MVVM substack)
│   └── Theme/               # GeoTheme + per-team theming
└── CarPlay/                 # CarPlay coordinator, templates, state sync
```

To add a feature: register it in `FeatureRegistry.swift`, build the SwiftUI view, map the id in `FeatureViewFactory.swift` — it then appears automatically in the menu and search.

## Tech stack

- Swift / SwiftUI, iOS 16.0+ (see `Podfile`), Xcode project `GeoRacing.xcodeproj`
- CocoaPods: `Firebase/Auth`, `Firebase/Firestore`, `GoogleSignIn`
- Apple frameworks: MapKit, CoreLocation, CoreBluetooth, CoreImage (QR), AVFoundation (TTS/speech), UserNotifications, CarPlay, HealthKit-adjacent services
- Backend access: plain `URLSession` (`APIService`, `DatabaseClient`) against the GeoRacing REST API; OSRM public router for road routing

## Setup

1. **Firebase project (required).** Create your own Firebase project, register an iOS app, download its `GoogleService-Info.plist`, and place it at:
   ```
   cp GeoRacing/GoogleService-Info.plist.example GeoRacing/GoogleService-Info.plist
   ```
   Replace the `YOUR_API_KEY` / `YOUR_GOOGLE_APP_ID` placeholders with your real values (or drop in the downloaded file). Enable Authentication (Email/Password + Google) and Firestore. For Google Sign-In, add your `REVERSED_CLIENT_ID` as a URL scheme in the target settings.
2. **Install pods:**
   ```
   pod install
   ```
   then open the generated `GeoRacing.xcworkspace` (not the `.xcodeproj`) in Xcode.
3. **Backend.** The API base URL and OSRM URL are hardcoded in `GeoRacing/Core/Constants/AppConstants.swift`; point `apiBaseUrl` at your own GeoRacing backend deployment.
4. Build and run on an iOS 16+ device (BLE scanning and CarPlay require real hardware; the simulator covers the rest).

## The `backend/` subfolder (transport-api)

A small server-side companion used only by the **public transport** feature (`PublicTransportViewModel`, `TransportAPIClient.swift`, which calls `http://localhost:3000/v1/transport`):

- **`backend/transport-api/`** — a Node.js/Express **BFF ("Backend For Frontend") for GeoRacing public transport** (`georacing-transport-api`, dependencies: express, axios, cors, redis). It is meant to sit between the app and an OpenTripPlanner instance, exposing `/v1/transport/plan` and `/v1/transport/health`. **Note:** only `package.json` is included in this snapshot — the `server.js` entry point referenced by `npm start` was not published, so the service cannot be run as-is; treat it as a specification of the expected BFF.
- **`backend/otp/`** — configuration for an **OpenTripPlanner** router (`graphs/default/router-config.json`) with GTFS-Realtime updaters for Renfe Cercanías (trip updates, vehicle positions, alerts) and AMB bus feeds. The actual GTFS data and built graph are not included.

If you don't run this stack, the app degrades gracefully: `TransportLocalFallback.swift` provides local fallback data when the transport API health check fails.

## Known limitations

- Earlier-stage port: most non-core features are placeholders (see `FEATURES.md` / `PARITY_CHECKLIST.md`); offline tile caching, full OSRM pedestrian routing, order history and staff tooling are pending.
- `backend/transport-api` ships without its `server.js`; the OTP folder ships config only (no graph/GTFS data).
- API base URL is a hardcoded development server in `AppConstants.swift`; replace it with your own.
- The parity/feature docs are written in Spanish.
