# Contributing to GeoRacing

Thanks for your interest in GeoRacing! This document explains how the repository is organized, how to get each component running, and the conventions we follow.

## Before you start

- Read the root [README.md](README.md) for an overview of the system.
- Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) to understand how the components talk to each other (REST API, command queue, BLE protocol).
- Each component has its own README with detailed setup steps — always prefer those over guesswork.

## Development setup per component

### API (`api/`)

Prerequisites: Node.js >= 18, a MySQL 8 instance.

```bash
cd api
npm install
# Configure via environment variables (or a local .env file):
#   DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME
node server.js
npm test            # run the test suite
docker compose up   # or bring up the API + database with Docker
```

The server creates/validates its schema on startup. Verify with `GET /health`.

### Web panel (`web/`)

Prerequisites: Node.js >= 18, a Firebase project (Auth + Firestore).

```bash
cd web
npm install
# Provide your Firebase configuration (see web/README.md and FIRESTORE_SETUP.md)
npm run dev     # start the dev server
npm run build   # production build
npm test        # run the test suite
```

### Android app (`android/`)

Prerequisites: Android Studio (latest stable), JDK 17.

1. Open `android` in Android Studio.
2. Copy `app/google-services.json.example` to `app/google-services.json` and fill in your own Firebase project values.
3. Sync Gradle and run on a device (BLE scanning requires real hardware).

### iOS app (`ios/`)

Prerequisites: Xcode (latest stable), CocoaPods.

1. Open the Xcode project in `ios`.
2. Add your own `GoogleService-Info.plist` (an `.example` template is provided).
3. Run `pod install` if needed and build on a device for BLE/CarPlay features.

### Windows beacon (`windows/`)

Prerequisites: Windows 10/11, Visual Studio with .NET desktop workload. BLE advertising uses WinRT APIs, so it must run on Windows with a BLE-capable adapter.

- Open `windows/GeoRacingBeacon.sln` in Visual Studio to build and run the full-screen WPF signage app.

## Conventions

### Commits

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short imperative summary>
```

- **type**: `feat`, `fix`, `docs`, `refactor`, `style`, `test`, `chore`
- **scope** (optional): the component touched — `android`, `ios`, `web`, `api`, `windows`, `docs`
- Examples:
  - `feat(android): add survival mode battery thresholds`
  - `fix(web): retry beacon command on network timeout`
  - `docs: update BLE payload table`

Keep commits focused on a single change; avoid mixing components in one commit when possible.

### Code style

- **Kotlin**: official Kotlin style, Compose best practices, keep ViewModels free of Android framework references where feasible.
- **Swift**: SwiftUI-first, follow the existing Domain/Data/Presentation layering.
- **TypeScript/React**: strict TypeScript, functional components and hooks, Tailwind for styling.
- **C#**: standard .NET conventions, MVVM in the WPF app.
- **Node.js**: keep endpoints thin; database access via the shared pool.

### Branches and pull requests

- Branch from `main` using `feature/<short-name>` or `fix/<short-name>`.
- Open a pull request with a clear description of what changed and why, plus screenshots for UI changes.
- A PR should build and run for every component it touches.

## Security and secrets

**Never commit credentials.** API keys, tokens, Firebase config files and database passwords belong in environment variables or local untracked files. `.example` templates document the expected keys. If you accidentally commit a secret, rotate it immediately — removing it from history is not enough.

## Where the documentation lives

| Topic | Location |
|---|---|
| Global architecture, data flows, BLE protocol, REST API | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Original global documentation (Spanish) | [docs/documentation.md](docs/documentation.md) |
| Per-component setup and details | The README inside each component folder |
| Web panel feature guides (beacons, commands, smart messages…) | Markdown guides inside `web/` |
| Android in-depth documentation | `android/documentation.md` |
| iOS feature list and parity checklist | `ios/FEATURES.md`, `ios/PARITY_CHECKLIST.md` |

## Questions

Open an issue to ask a question. For substantial changes, please discuss in an issue before investing significant time.
