# Beacons (Balizas)

Windows desktop applications that turn a trackside PC (or mini-PC) into a **GeoRacing physical beacon**: a remotely controlled light/signal panel that also broadcasts the current circuit state over **Bluetooth LE** so nearby spectator phones can pick it up even without network coverage.

## What a beacon actually does (verified against the code)

Both implementations share the same core behavior:

- **Poll the GeoRacing backend REST API** (`GET /api/state`) on a short interval to fetch the current circuit state: `NORMAL`, `SAFETY_CAR`, `RED_FLAG` or `EVACUATION` (plus temperature and free-text messages).
- **Advertise that state over BLE** using `BluetoothLEAdvertisementPublisher` (WinRT). The payload is a custom manufacturer-data packet (manufacturer ID `0x1234`, test value) of ~9 bytes: protocol version, zone ID, mode byte, flags, a sequence counter (so receivers can detect fresh data), TTL and temperature. There is **no GATT server** — it is connectionless advertising only.
- **Show the state on screen** so the PC itself acts as a visual signal.

They are *not* passive iBeacons: they are active, networked signaling nodes.

## Folder layout

### `baliza-gerard/` — Gerard's implementation

Two projects in one folder:

| Project | Type | Target framework |
|---|---|---|
| `BeaconActivePc.csproj` (root, `Program.cs`) | Console app | `net8.0-windows10.0.19041.0` |
| `BeaconGui/BeaconGui.csproj` | WPF app | `net9.0-windows10.0.19041.0` |

- **BeaconActivePc** is the minimal headless version: polls the API and re-publishes the BLE advertisement whenever the mode or temperature changes.
- **BeaconGui** adds a WPF control window (Start/Stop broadcasting, live log, current mode with color coding, sequence counter) and additionally runs a `BluetoothLEAdvertisementWatcher` that **scans for user beacons** (phones advertising with the same manufacturer ID), counts nearby users and expires stale ones after 30 s. It also has a "simulate user" mode that fakes a user packet with GPS coordinates near the Barcelona circuit, useful for testing the detection pipeline end to end.

### `baliza-noah/` — Noah's implementation

Visual Studio solution **`METROPOLIS BALIZA 2.sln`** containing a single WPF project, `BeaconApp/` (assembly name `GeoRacingBeacon`, target `net8.0-windows10.0.19041.0`).

This is the more "product-like" beacon: a **full-screen, borderless, always-on-top panel** (racing-style UI with color-coded modes and a large directional arrow that rotates — FORWARD/LEFT/RIGHT/diagonals — for evacuation guidance). Structure:

- `Config/BeaconConfigService.cs` — per-device config (`beacon.json` under `C:\ProgramData\GeoRacing`): beacon ID, API base URL, zone, GPS position.
- `Services/ApiClient.cs` — health check, **heartbeat registration** (`POST /beacons/heartbeat`, including battery level and current arrow direction) and generic upserts against the backend.
- `Services/BleBeaconService.cs` — the BLE advertiser described above.
- `ViewModels/MainViewModel.cs` — MVVM glue: 300 ms state polling + 10 s heartbeat timers.
- Several Spanish-language design/working notes (`DOCUMENTACION-CODIGO.md`, `DIAGRAMA-FUNCIONAMIENTO.md`, etc.) kept as-is from development.

## Building

**Windows is required** — both apps use WinRT Bluetooth APIs (via the `windows10.0.19041.0` TFM) and WPF. They will not build or run on Linux/macOS.

```powershell
# Gerard's console beacon
dotnet build beacons/baliza-gerard/BeaconActivePc.csproj

# Gerard's GUI beacon (needs the .NET 9 SDK)
dotnet build beacons/baliza-gerard/BeaconGui/BeaconGui.csproj

# Noah's beacon (or open the .sln in Visual Studio 2022)
dotnet build "beacons/baliza-noah/METROPOLIS BALIZA 2.sln"
```

SDK requirements: .NET 8 SDK for `BeaconActivePc` and `BeaconApp`, .NET 9 SDK for `BeaconGui`. Running them needs a Bluetooth LE-capable adapter.

## Status

These are **prototypes from an educational project**, not production firmware:

- API endpoints and IDs (manufacturer ID, zone ID) are hard-coded constants in places.
- TLS certificate validation is deliberately disabled to talk to a self-signed dev server — do not reuse this pattern in production.
- Error handling is best-effort and logging is console/file based.

They exist to prove the concept: a cheap Windows box can act as a networked track signal and a BLE broadcast point for the GeoRacing mobile apps.
