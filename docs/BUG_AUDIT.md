# Bug audit & fixes

This document tracks the code audit performed while preparing GeoRacing for
open source. Each app was reviewed for crashes, functional defects, lifecycle
leaks and UI bugs. Items marked **Fixed** are applied in this repository;
items marked **Open** are documented here for contributors.

The web panel and the Android app were **compiled and verified** during this
pass (`tsc` + `vite build` for the panel, `./gradlew assembleDebug` for
Android). The iOS app and the C# beacon apps could not be compiled in the
build environment, so their fixes are conservative, mechanical changes.

## Build status

| Component | Verified build | Notes |
|-----------|----------------|-------|
| web | ✅ `tsc --noEmit` + `vite build` | strict TypeScript, clean |
| android | ✅ `./gradlew assembleDebug` | build files restored from the matching team snapshot; `google-services.json.example` includes a placeholder OAuth client so the google-services plugin generates `default_web_client_id` |
| ios | ⬜ not compiled here | fixes are mechanical; verify in Xcode |
| beacons (C#) | ⬜ not compiled here | fixes are mechanical; verify in Visual Studio |

## Android — Fixed

- **BLE evacuation broadcast dropped.** `BlePayloadParser` capped payloads at 9
  bytes, so the staff's 13-byte v2 danger/evacuation broadcast was rejected and
  never reached phones. Size is now validated per protocol version.
- **Crash on scan without `BLUETOOTH_CONNECT`.** `device.name` in the scan
  callback threw `SecurityException` (SCAN granted, CONNECT denied) and killed
  the process. Now guarded.
- **Beacon broadcast with sharing disabled.** `StatePollingService` advertised
  the user beacon even when location sharing was off; it now stops advertising.
- **TextToSpeech leak in Android Auto.** `GeoRacingNavigationScreen` never called
  `tts.shutdown()` on destroy. Fixed.
- **Incident form lost input on rotation.** Switched `remember` to
  `rememberSaveable` for the category and description fields.

## Android — Open (documented for contributors)

- Circuit-state flow in `MainActivity` uses `collectAsState` instead of
  `collectAsStateWithLifecycle`, so REST polling continues in the background.
- `BeaconScanner.activeSignal` uses a `CoroutineScope` that is never cancelled;
  `GroupMapViewModel` creates a new scanner per screen entry (cumulative leak).
- Cart/checkout screens lack double-submit guards in a few places.
- Staff emergency broadcast via `StaffBeaconAdvertiser` uses service-data /
  text payloads that the scanner does not read (use the v2 manufacturer-data
  format from `BeaconAdvertiser`).

## Web panel — Fixed

- **False "evacuation active" while loading.** `beacons.every(...)` returns
  `true` for an empty array; guarded with `beacons.length > 0`.
- **Silent evacuation failures.** Activate/deactivate now surface errors and
  success via the toast system.
- **Logout did not clear the session** (mock user never existed in Firebase);
  `setUser(null)` added.
- **Dashboard showed stale circuit state** — it read `mode`/`updated_at` while
  the writer uses `global_mode`/`last_updated`; now reads both.
- **`€NaN` in orders** when a product was missing from the catalog (`?? 0` does
  not catch `NaN`).
- Service layer: beacon detection normalises `beacon_uid`; new-beacon modal
  sends `zone`/`tags`; API client no longer retries non-idempotent mutations
  or received HTTP errors, and only forces HTTPS in production; circuit-state id
  type unified; evacuation goes through the same command path as updates.

## Web panel — Open

- Several pages (Incidents filters, Orders status race, Products/News/FoodStands
  double-submit, ZonesMap/Routes re-seed on poll, BeaconDetail/BeaconEditModal
  form reset) have UI-polish bugs catalogued during the audit.

## iOS — Fixed (mechanical, verify in Xcode)

- Duplicate-key crash in `CrowdDensityService` (`Dictionary(uniqueKeysWithValues:)`).
- `AnyEncodable` could not encode `Int64`, so writing circuit state always failed.
- Group location polling never stopped on leaving a group; retain cycles in
  view models (`assign(to:on:self)` → `assign(to:&$x)`).
- TTS "arrived" message looped; QR scanner stayed black on first permission grant;
  route spinner never showed for "to circuit".

## iOS — Open

- Trust-all TLS (self-signed dev server) — documented in code; do not ship to
  production without certificate validation.
- `TelemetryLogger` / `SyncQueueManager` concurrency issues (flagged in code).
- Yellow flag mapped to Safety Car; reward thresholds off by a factor.

## Beacons (C#) — Fixed (mechanical, verify in Visual Studio)

- 300 ms polling timer reentrancy (could double-execute RESTART/SHUTDOWN).
- Temperature mis-encoded (`"11.3°C"` → 113 °C) in the BLE payload.
- Battery reported as 25500 on desktop PCs without a battery.
- Evacuation arrow `NONE` always pointed up; advertising restarted every tick;
  command timestamp timezone handling; config file destroyed on transient I/O error.
- User-beacon scan filter matched circuit packets; HttpClient without timeout.
