# GeoRacing — GPS & BLE-powered motorsport event platform

GeoRacing is a full-stack platform for managing live motorsport events (F1-style circuit racing) and enhancing the on-site experience for attendees. It combines real-time GPS positioning, Bluetooth Low Energy (BLE) beacon detection and cloud services to deliver live circuit status, pedestrian navigation, group location sharing, food ordering, and smart physical signage distributed around the circuit.

Built by a three-person student team as a multi-platform capstone project.

## Architecture

```
                         ┌──────────────────────────────┐
                         │        Cloud / Backend        │
                         │  Node.js REST API + MySQL     │
                         │  Firebase Auth + Firestore    │
                         └──────┬───────────────┬───────┘
                                │               │
        ┌───────────┬───────────┼───────────────┼──────────────┐
        │           │           │               │              │
   Android app   iOS app    Web panel      BLE beacons    Discord bot
   (attendees)  (attendees) (organizers)  (smart signage)  (community)
```

| Component | Path | Stack | Description |
|---|---|---|---|
| Android app | `apps/android` | Kotlin, Jetpack Compose, Retrofit, Firebase, Android Auto | Attendee app: interactive circuit map, real-time flags/safety-car status, BLE zone detection, group location sharing, food orders, battery-survival mode |
| iOS app | `apps/ios` | Swift, SwiftUI, CarPlay, Firebase | Feature-parity attendee app for iOS |
| Web panel | `web-panel` | React, TypeScript, Vite, Tailwind, Firebase | Organizer control panel: beacon fleet management, circuit state, evacuations, incidents, statistics, orders and users |
| Beacon apps | `beacons` | C# / .NET (WPF, WinRT BLE) | Desktop software for physical signage beacons: BLE advertising, remote-configured displays (circuit status, evacuation routes, directions) |
| Backend API | `backend` | Node.js, Express, MySQL | REST API serving circuit state, beacons, zones and evacuation logic |
| Discord bot | `discord-bot` | Node.js, discord.js, Supabase, Groq | Community bot with AI-powered Q&A over project knowledge |
| Documentation | `docs` | Markdown | Global technical documentation (architecture, data flow, BLE protocol, REST API) |

## Team

- **Gerard** — Android app, backend API, BLE beacon prototype
- **Dani** — Discord bot, iOS app
- **Noah** — Beacon desktop apps

## Setup

### Backend API
```bash
cd backend
npm install
# set DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME in your environment
node server.js
```

### Web panel
```bash
cd web-panel
npm install
cp .env.example .env   # fill in your Firebase project values
npm run dev
```

### Android app
Open `apps/android` in Android Studio. Add your own `app/google-services.json` (see `app/google-services.json.example`) and sync Gradle.

### iOS app
Open `apps/ios/GeoRacing.xcodeproj` in Xcode. Add your own `GeoRacing/GoogleService-Info.plist` (see the `.example` file) and run `pod install` if needed.

### Beacon apps
Open `beacons/baliza-noah/METROPOLIS BALIZA 2.sln` or `beacons/baliza-gerard` in Visual Studio (.NET, Windows — BLE advertising requires WinRT APIs).

### Discord bot
```bash
cd discord-bot
npm install
cp .env.example .env   # fill in Discord, Supabase and Groq credentials
node index.js
```

## Note

This repository is a curated export of the original team workspace; build artifacts, duplicates and credentials were removed.
