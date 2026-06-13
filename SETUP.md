# Setup — credentials & configuration

This repository ships **no real credentials**. Every secret or project-specific
value is a placeholder you must replace with your own. This page lists exactly
what to fill in per component and where it goes.

> Nothing here is committed with real values. Files that would contain secrets
> (`.env`, `google-services.json`, `GoogleService-Info.plist`) are git-ignored;
> use the provided `*.example` templates.

## Quick reference

| Component | Create this file | From template | Fill in |
|---|---|---|---|
| Backend | `api/.env` | `api/.env.example` | `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` (+ optional `PORT`, TLS paths) |
| Web panel | `web/.env` | `web/.env.example` | `VITE_FIREBASE_*` (6 keys) and `VITE_API_BASE_URL` |
| Android | `android/app/google-services.json` | `…/google-services.json.example` | your Firebase Android app config (auto-provides `default_web_client_id`) |
| iOS | `ios/GeoRacing/GoogleService-Info.plist` | `…/GoogleService-Info.plist.example` | your Firebase iOS app config |
| iOS | edit `ios/GeoRacing/Info.plist` | — | replace `YOUR_IOS_CLIENT_ID` (2 places) with your reversed iOS OAuth client ID |
| Beacons | `C:\ProgramData\GeoRacing\beacon.json` | created on first run | `apiBaseUrl`, `beaconId`, `zoneId`, coordinates |

## Where to get the values

### Firebase (web panel, Android, iOS)
1. Create a Firebase project at <https://console.firebase.google.com>.
2. **Web panel:** Project settings → "Your apps" → Web app → copy the config into
   `web/.env` as the `VITE_FIREBASE_*` variables.
3. **Android:** add an Android app (package `com.georacing.georacing`), download
   `google-services.json`, drop it in `android/app/`. This file provides the
   `default_web_client_id` the app uses for Google Sign-In.
4. **iOS:** add an iOS app, download `GoogleService-Info.plist` into
   `ios/GeoRacing/`, and in `Info.plist` replace the two `YOUR_IOS_CLIENT_ID`
   placeholders with your iOS OAuth client (the `GIDClientID` and the reversed
   `com.googleusercontent.apps.…` URL scheme).
5. Enable **Google** as a sign-in provider in Firebase Authentication, and add
   your app's SHA-1/SHA-256 (Android) so Sign-In works.

### Backend database
`api/.env` points at a MySQL instance. For a quick local instance:
`cd api && docker compose up` (compose provisions MySQL and wires the env).

### Windows beacon
On first run the beacon writes `C:\ProgramData\GeoRacing\beacon.json`. Set
`apiBaseUrl` to your API (e.g. `https://your-api.example.com:4010/api/`).

## Local testing without real services
The API base URL placeholder in the apps is `georacing.example.com`. For a quick
end-to-end test you can run a local mock API and point the apps at it; see the
backend README and `docs/ARCHITECTURE.md` for the endpoint contract.

## Security note
The original project workspace had live credentials. Those have been removed from
this repository. If you are one of the original authors, **rotate** any key that
ever lived in the old workspace (Firebase
keys) — assume they are compromised.
