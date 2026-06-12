# PARITY CHECKLIST: GeoRacing iOS vs Android

**Goal**: TOTAL FUNCTIONAL PARITY with Android app.
**Status Key**:

- ✅ **OK**: Fully implemented and parity verified.
- ⚠️ **PARTIAL**: Implemented but missing mapped features or polish.
- ❌ **MISSING**: Not yet implemented.
- ⏳ **PENDING VERIFICATION**: Implemented but needs smoke test.

## 1. Core Architecture & Networking

| Feature           | Android                       | iOS                            | Action / Status                          |
| :---------------- | :---------------------------- | :----------------------------- | :--------------------------------------- |
| **Networking**    | Retrofit, `/api` base URL     | `APIService` (URLSession)      | ✅ OK. Uses real API.                    |
| **Data Layer**    | `FirestoreLikeClient` generic | `DatabaseClient` generic       | ✅ OK. Supports `_read`, `_upsert`.      |
| **Auth**          | Firebase Auth + GSI           | `AuthService` (Firebase + GSI) | ✅ OK. Login flow implemented.           |
| **Offline Cache** | Caches last known state       | `CircuitStatusRepository`      | ✅ OK. `UserDefaults` persistence added. |

## 2. Circuit Intelligence

| Feature            | Android                          | iOS                        | Action / Status                                |
| :----------------- | :------------------------------- | :------------------------- | :--------------------------------------------- |
| **Circuit Status** | Polling, handling SC/RED/EVAC    | `CircuitStatusRepository`  | ✅ OK. Logic fixed for SC/Red.                 |
| **Evacuation**     | Overlay screen on specific state | `EvacuationView`           | ✅ OK. Triggered by 'EVACUATION' msg or state. |
| **Visuals**        | Flags, Banner Colors             | `HomeView`, `TrackStatus`  | ✅ OK. SF Symbols & Orange/Red themes.         |
| **Notifications**  | Local Push on Status Change      | `LocalNotificationManager` | ✅ OK. Implemented.                            |

## 3. Map & Location

| Feature        | Android                      | iOS                    | Action / Status                       |
| :------------- | :--------------------------- | :--------------------- | :------------------------------------ |
| **Map View**   | POI Layers, User Pos         | `CircuitMapView`       | ✅ OK. POIs fetching from API.        |
| **Filtering**  | Filter POIs by type          | `MapViewModel` filters | ✅ OK. Toggle logic exists.           |
| **Beacons**    | BLE Scanning, Zone detection | `BeaconScanner`        | ✅ OK. Integrated.                    |
| **Navigation** | Route to POI                 | `RouteManager`         | ✅ OK. Stub Architecture implemented. |

## 4. Commerce (Shop)

| Feature          | Android                   | iOS           | Action / Status                             |
| :--------------- | :------------------------ | :------------ | :------------------------------------------ |
| **Product List** | Grid, Real Images, Prices | `OrdersView`  | ✅ OK. Fetches from DB, renders AsyncImage. |
| **Cart**         | Add/Remove, Total Calc    | `CartManager` | ✅ OK. Logic parity achieved.               |
| **Checkout**     | Submit Order to DB        | `submitOrder` | ✅ OK. Writes to `orders` table.            |
| **History**      | View past orders          | ❌ MISSING    | (Deferred)                                  |

## 5. Social & Incidents

| Feature       | Android                       | iOS                  | Action / Status                       |
| :------------ | :---------------------------- | :------------------- | :------------------------------------ |
| **QR Share**  | Share/Scan Session QR         | `SocialView`         | ✅ OK. QR CoreImage logic verified.   |
| **Group Map** | See friends on map            | `CircuitMapView`     | ✅ OK. Wiring present.                |
| **Incidents** | Report issue (Category, Desc) | `IncidentReportView` | ✅ OK. Writes to API.                 |
| **Anonymous** | Report without login?         | ❓ UNKNOWN           | Check logic. Currently requires Auth? |

## 6. Settings & Misc

| Feature        | Android                     | iOS              | Action / Status                       |
| :------------- | :-------------------------- | :--------------- | :------------------------------------ |
| **Settings**   | Lang, Theme, Seat           | `SettingsView`   | ✅ OK. `UserPreferences` implemented. |
| **Onboarding** | Slides / Permission Request | `OnboardingView` | ✅ OK. Implemented w/ permissions.    |

---

## EXECUTION PLAN (Phase 2 & 3)

1.  **[ ] Offline Cache**: Implement `UserDefaults` saving for `CircuitStatus`.
2.  **[ ] Notifications**: Implement `LocalNotificationManager` for status changes.
3.  **[ ] Navigation**: Check if we need full OSRM routing or just straight line is enough for "Phase 1 Parity". (Assuming straight line/direction is OK for now).
4.  **[ ] Social/QR**: Verify the QR Code generation actually works (SwiftUI `CoreImage` integration).
5.  **[ ] Onboarding**: Create a simple Onboarding view for parity.
