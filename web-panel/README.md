# GeoRacing Control Panel

Web-based race-control panel for the GeoRacing motorsport event platform. It is the operator-facing dashboard used during an event to manage:

- **Circuit state (flags)** — switch the global circuit mode between NORMAL, SAFETY CAR and RED FLAG, with live weather/track info (`pages/CircuitState.tsx`).
- **Smart beacons** — real-time fleet view, per-beacon configuration (mode, arrow direction, message, color, brightness, language, zone), remote commands (restart, shutdown, close app) and automatic detection of newly connected beacons (`pages/Beacons.tsx`, `pages/BeaconDetail.tsx`).
- **Emergencies and evacuation** — global or per-zone evacuation protocol with confirmation flow and audit logging (`pages/Emergencies.tsx`, `components/EvacuationModal.tsx`).
- **Incidents** — live incident list with severity levels (`pages/Incidents.tsx`).
- **Circuit map and zones** — zone occupancy/traffic view and circuit routes status (`pages/ZonesMap.tsx`, `pages/Routes.tsx`).
- **Event services** — users, orders, products, food stands and news management (`pages/UsersPage.tsx`, `pages/OrdersPage.tsx`, `pages/ProductsPage.tsx`, `pages/FoodStandsPage.tsx`, `pages/NewsPage.tsx`).
- **Logs and statistics** — command history and system metrics (`pages/Logs.tsx`, `pages/Statistics.tsx`).

## Tech stack

- **React 18** + **TypeScript** (strict mode)
- **Vite 5** build tooling
- **Tailwind CSS 3** for styling
- **Firebase** (Authentication; Firestore rules/indexes included for the beacon legacy flow)
- **react-router-dom 6** for routing
- **lucide-react** icons
- Data access through a small generic REST client (`src/services/apiClient.ts`) that polls the GeoRacing backend (`_get` / `_upsert` / `_delete` endpoints)

## Setup

```bash
cd web-panel

# 1. Configure environment variables
cp .env.example .env
#    Fill in your Firebase project values (and optionally VITE_API_BASE_URL)

# 2. Install dependencies
npm install

# 3. Start the dev server (http://localhost:3000)
npm run dev
```

## Build

```bash
npm run build      # type-checks (tsc) and produces dist/
npm run preview    # serve the production build locally
npm run lint       # ESLint
```

Deployment to Firebase Hosting is preconfigured (`firebase.json`):

```bash
npm run deploy             # build + firebase deploy
npm run deploy:firestore   # deploy Firestore rules/indexes only
```

## Environment variables

All variables are read via `import.meta.env` (see `.env.example`):

| Variable | Purpose |
| --- | --- |
| `VITE_FIREBASE_API_KEY` | Firebase web API key |
| `VITE_FIREBASE_AUTH_DOMAIN` | Firebase auth domain |
| `VITE_FIREBASE_PROJECT_ID` | Firebase project id |
| `VITE_FIREBASE_STORAGE_BUCKET` | Firebase storage bucket |
| `VITE_FIREBASE_MESSAGING_SENDER_ID` | Firebase sender id |
| `VITE_FIREBASE_APP_ID` | Firebase app id |
| `VITE_API_BASE_URL` | Backend REST API base URL (optional; falls back to the demo server) |

No real credentials are committed; `src/firebase/config.ts` only reads from the environment.

## Project structure (`src/`)

| Folder | Contents |
| --- | --- |
| `components/` | Reusable UI: layout, beacon config form/preview/edit modal, command panel, evacuation modal, toasts, protected route |
| `context/` | React contexts: `AuthContext` (Firebase Auth session) and `ToastContext` (notifications) |
| `examples/` | `beaconConfigExamples.ts` — annotated, runnable code samples for the beacon configuration API (documentation, not imported by the app) |
| `firebase/` | Firebase initialization; config is loaded from `VITE_FIREBASE_*` env vars |
| `hooks/` | Polling hooks: `useBeacons`, `useCircuitState`, `useNewBeaconDetection`, `useZones` |
| `pages/` | One component per route (see feature list above) plus `Login` |
| `services/` | `apiClient` (generic REST client), `beaconService` (beacon CRUD + commands + emergency service), `beaconDetectionService` |
| `types/` | Shared TypeScript domain types (Beacon, Command, Zone, Route, CircuitStateData, ...) |
| `utils/` | Beacon helpers: status/formatting, multilanguage default messages, validation |
| `beacon_renderer/` | Standalone rendering engine (layout + arrow component) mirroring how physical beacon screens draw their state; reference implementation, not imported by the panel |

Other useful files in this folder:

- `BEACON_CLIENT_EXAMPLE.js` — example client code (JavaScript and Python variants) to run on a physical beacon: auto-registration, heartbeat and config subscription.
- `GeoRacingDB.json` — sample database export (tables + seed rows) matching the backend schema used by the panel.
- `firestore.rules`, `firestore.indexes.json` — Firestore security rules and indexes.

## Additional docs

Detailed guides live in [`docs/`](./docs):

| Document | Topic |
| --- | --- |
| [QUICK_START.md](./docs/QUICK_START.md) | Quick start with examples |
| [AUTH_GUIDE.md](./docs/AUTH_GUIDE.md) | Authentication setup |
| [FIRESTORE_SETUP.md](./docs/FIRESTORE_SETUP.md) | Firestore configuration |
| [API_MIGRATION_GUIDE.md](./docs/API_MIGRATION_GUIDE.md) | Migration from Firestore to the REST API |
| [BEACON_INTEGRATION_GUIDE.md](./docs/BEACON_INTEGRATION_GUIDE.md) | Beacon integration overview |
| [BEACON_CONFIG_COMPLETE.md](./docs/BEACON_CONFIG_COMPLETE.md) | Full beacon configuration reference |
| [BEACON_METRICS_GUIDE.md](./docs/BEACON_METRICS_GUIDE.md) | Beacon metrics |
| [COMMAND_SYSTEM_GUIDE.md](./docs/COMMAND_SYSTEM_GUIDE.md) | Remote command system |
| [COMMAND_IMPLEMENTATION_SUMMARY.md](./docs/COMMAND_IMPLEMENTATION_SUMMARY.md) | Command system implementation notes |
| [SMART_MESSAGES_GUIDE.md](./docs/SMART_MESSAGES_GUIDE.md) | Multilanguage smart default messages |
| [CUSTOM_TEXT_INTEGRATION_GUIDE.md](./docs/CUSTOM_TEXT_INTEGRATION_GUIDE.md) | Custom text integration (WPF clients) |
| [WPF_INTEGRATION_CHECKLIST.md](./docs/WPF_INTEGRATION_CHECKLIST.md) | WPF beacon client integration checklist |
| [INTEGRATION_SUMMARY.md](./docs/INTEGRATION_SUMMARY.md) | Integration change summary |
| [IMPLEMENTATION_SUMMARY.md](./docs/IMPLEMENTATION_SUMMARY.md) | Implementation summary |
| [COMPLETION_REPORT.md](./docs/COMPLETION_REPORT.md) | Project completion report |
| [VERIFICATION_CHECKLIST.md](./docs/VERIFICATION_CHECKLIST.md) | Manual verification checklist |
| [FUTURE_IMPROVEMENTS_ROADMAP.md](./docs/FUTURE_IMPROVEMENTS_ROADMAP.md) | Roadmap of future improvements |

> Note: some guides predate the REST API migration and describe the original Firestore-based data flow; see `API_MIGRATION_GUIDE.md` for the current architecture.

## License

MIT — see the repository [LICENSE](../LICENSE).
