# GeoRacing — Idea Coverage Matrix

Traceability of the full product backlog (5 resilience pillars + 66 backlog
ideas) against the actual code, plus a second-wave backlog of new ideas.

**Legend:** ✅ implemented · ◐ partial (code exists but incomplete/unwired/one
platform) · ❌ missing. Evidence points to real files/classes/screens.

**Status:** 71 ideas catalogued — **18 ✅ · 41 ◐ · 12 ❌**.

## 🛡️ Resilience pillars

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| P1 | Navegación Térmica (Grafo de Sombra) | ◐ | android, ios | A* shade-penalty graph exists (`PedestrianPathfinder`, `ThermalRoutingService`) but `findRoute` is never called by UI |
| P2 | Modo Supervivencia batería (30%) | ✅ | android, ios | `EnergyMonitor`/`EnergyProfile` disable AR/gamification/bg-sync + OLED black, wired in `MainActivity` |
| P3 | Staff como Balizas Humanas | ✅ | android, ios | `BeaconAdvertiser.startDangerAdvertising` ↔ `BeaconScanner`/`BlePayloadParser` (v2 staff payload) |
| P4 | Caja Negra Operativa (telemetría) | ◐ | android | `BlackBoxLogger`/`TelemetrySyncWorker` exist but `logEvent()` never called; upload is a stub |
| P5 | Lock Screen Médico de Emergencia | ✅ | android | `MedicalWallpaperGenerator` sets lock-screen bitmap (blood type/ICE/QR) |

## 🧭 A) Navegación y Core

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 1 | Mapa Vectorial Offline | ◐ | android, ios | Room offline-first POIs real; tiles online (no bundled `.mbtiles`), no delta sync |
| 2 | Guía AR al Asiento | ◐ | android | CameraX + magnetometer overlay (no ARCore/seat anchor); iOS placeholder |
| 3 | Rutas Anti-Colas | ◐ | android | A* congestion weights + `CircuitTrafficProvider` coded but never instantiated/called |
| 4 | Rutas Accesibles | ◐ | android | Stairs-avoid pathfinding + `avoidStairs` pref; no ramp/elevator metadata; engine unwired |
| 5 | Auto-Posicionamiento por QR | ◐ | android | Full recalibrate logic (`QrPositioningManager`) but unwired; QR route opens group-join |
| 6 | Micro-Posicionamiento BLE | ◐ | android, ios | Real RSSI scanning; no trilateration/smoothing |
| 7 | Modo Emergencia | ✅ | android, web | `StatePollingService` → full-screen `EvacuationActivity`; web sets state |
| 8 | Evacuación Dinámica | ◐ | android, web | Single hardcoded safe-point marker; not per-sector exit routes |
| 9 | Predicción de Tiempos a Pie | ◐ | android, ios | ETA = dist / fixed 4.5 km/h; no gait profile / density multiplier; iOS mock |
| 10 | Plan del Día | ❌ | — | No daily-schedule sync; only event-config sync exists |
| 11 | Wallet de Entradas Offline | ◐ | android | Demo ticket card with drawn QR; no encrypted storage |
| 12 | POIs Inteligentes | ✅ | android, ios | Room POI DB by category + offline-first repo + category filter UI |

## 🚗 B) Vehículo y Android Auto

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 13 | Ruta Inteligente al Circuito | ✅ | android | OSRM/GraphHopper driving nav in Android Auto + ticket→parking assignment |
| 14 | Asignación de Parking | ✅ | android, ios | Rules-based ticket-type → parking/gate |
| 15 | Guía al Parking | ◐ | android | Internal pedestrian last-mile graph; vehicle leg still OSRM |
| 16 | Find My Car | ◐ | android, ios | Offline coords persist; photo/sector-note fields unused |
| 17 | Modo Transición Coche-Peatón | ✅ | android | Auto-switch on Android Auto disconnect + proximity |
| 18 | Alertas de Tráfico Previas | ◐ | android | Bg service + traffic data, but no proactive pre-arrival access alerts |
| 19 | Integración Android Auto Completa | ◐ | android, ios | Android Auto template full; iOS CarPlay scaffolding (nav commented out) |

## 🎉 C) Fan Experience

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 20 | GeoRacing Wrapped | ✅ | android | Local usage aggregation + shareable recap |
| 21 | Pedidos Click & Collect | ◐ | android, ios, web | Cart/checkout + live stock, but `FakePaymentProcessor` and online menu |
| 22 | Fan Zone Mejorada | ◐ | android, ios | iOS downloads CMS content; Android hardcoded; no activity agenda |
| 23 | Fan Zone Personalizada | ◐ | android | Onboarding quiz → personalized widgets; FanZone content hardcoded |
| 24 | Gamificación (Misiones) | ◐ | android, ios | Achievements/XP real, but only debug-triggered; no geo/BLE/QR triggers |
| 25 | Recompensas por Contenido | ◐ | android | Photo+GPS report; wired repo has no photo field; sync simulated |
| 26 | Coleccionables (Cromos) | ◐ | android, ios | Card inventory + sync, but plain storage (no encryption/signed sync) |
| 27 | Momento360 | ❌ | — | Only still-photo gallery; no video/360/POV |
| 28 | Trivia F1 | ◐ | android, ios | Playable offline (rich iOS); not SQLite, no synced rankings |
| 29 | Pop-Ups Curiosidades | ❌ | — | Roadmap label only, no code |
| 30 | Microlearning (Duolingo) | ❌ | — | No lesson engine |
| 31 | EcoMeter | ✅ | android, ios | Health Connect/HealthKit steps → CO₂ saved |
| 32 | Fan Immersive Mode | ◐ | android, ios | Telemetry HUD simulated; no 3D packages; iOS placeholder |
| 33 | SoundTags 3D (Audio Espacial) | ❌ | — | Placeholder label only |
| 34 | Modo Sigue al Piloto | ◐ | android | Feed pipeline exists; API has no driver timing; HUD simulated |
| 35 | Interfaz por Temporada | ✅ | android | Remote skin/theme by event config |
| 36 | IA para Interfaz (Contextual UI) | ◐ | android, ios | Deterministic priority rules (not probabilistic) |

## 👥 D) Social y Comunidad

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 37 | Seguir al Grupo | ✅ | android, ios | Online share/poll + offline last-known BLE fallback (iOS create/join mock) |
| 38 | Punto de Reunión Compartido | ❌ | — | Registry/roadmap label only |
| 39 | Compartir Perfil por QR | ◐ | android, ios | QR encodes group invite (TTL), not anon profile token |
| 40 | Compartir Perfil por Redes | ❌ | — | No registered deep links / web landing |
| 41 | Clubes y Peñas | ❌ | — | Backlog label only |
| 42 | Chat de Proximidad | ◐ | android | Offline P2P mesh chat works (Android); online geo-chat missing |
| 43 | Encuestas al Usuario | ❌ | — | No survey code |

## 🧑‍💼 E) Staff, Seguridad y Panel Web

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 44 | Panel de Control en Tiempo Real | ◐ | web, api | React polling panel; mock auth (no roles), no 3D twin |
| 45 | Mapa de Calor de Afluencia | ◐ | android, ios | Heatmap pipeline exists but overlay disabled / demo projection |
| 46 | Control Remoto de Balizas | ✅ | web, api, windows | web→api→beacon command flow; audit log (plaintext) |
| 47 | Modos de Baliza | ✅ | windows, api, web | Standardized states as BLE manufacturer-data + color coding |
| 48 | Gestión de Incidencias (Triage) | ◐ | web, api | Create/list + filter; no priority queue / proximity dedup |
| 49 | Botón Rojo de Evacuación Global | ◐ | web, api, windows | Global evac sets max-priority + logs; gated by typed text, not 2FA |
| 50 | Monitorización de Aforos | ◐ | android, ios | Demo-grade; iOS polls `/zones/density` which doesn't exist in API |
| 51 | Feedback de Restauración | ❌ | — | No feedback form / analytics |
| 52 | Panel de Staff Móvil | ◐ | android, ios | Staff BLE broadcast real; no assigned-incidents map; iOS stubs |

## ⚙️ F) Sistema, Calidad y Escalado

| # | Idea | Status | Platforms | Notes |
|---|---|:--:|---|---|
| 53 | Modo Offline Total | ◐ | android | Real offline-first (Room + offline MapLibre style); no formal capability matrix |
| 54 | Descarga Previa del Evento | ❌ | — | No home-WiFi detect / signed .zip package |
| 55 | Soporte Multi-Idioma | ◐ | android | 17 localized `strings.xml`; in-app picker doesn't switch locale; iOS/web none |
| 56 | Alto Contraste (Sun Friendly) | ◐ | android | Settings toggle exists but never consumed; no light sensor |
| 57 | Modo Oscuro (OLED Black) | ✅ | android | Pure-black theme + OLED map style (battery-triggered) |
| 58 | Filtros de Alérgenos | ❌ | — | No allergen fields in menus |
| 59 | Sistema de Sugerencias | ◐ | ios, android | Contextual rules engine; suggestion generation is mock |
| 60 | Notificaciones sin Red (BLE) | ✅ | android, windows | Totem state byte → native offline notification |
| 61 | Sincronización Segura Diferida | ◐ | ios, android | Retry queue exists; no exponential backoff / idempotency tokens |
| 62 | Single Source of Truth Local | ✅ | android | UI observes Room; network writes to Room |
| 63 | Escalabilidad Multi-Evento | ◐ | android | Per-event remote config; no EventID data partitioning |
| 64 | Soporte Multi-Circuito | ◐ | android | Config-driven venue branding; map/POI/BLE not a swappable package |
| 65 | API para Integraciones Futuras | ◐ | api | Documented REST + idempotent upsert; no auth scopes / rate limiting |
| 66 | Ecosistema Híbrido App + Hardware | ✅ | android, windows, api | Internet + BLE + Room channels arbitrated (`HybridCircuitStateRepository`) |

---

# 🚀 Backlog v2 — New ideas

Second-wave ideas that extend GeoRacing's strengths (offline resilience, BLE,
safety). 🏛️ = candidate for a 6th resilience pillar. Most build on existing code.

## Safety / emergency
- **N1 🏛️ Muster Point Check-in (post-evacuation reunification)** — check in at a safe point via QR/BLE; group shows who's safe; panel shows live headcount per point. Builds on groups (37) + evacuation (8/49).
- **N2 🏛️ Differentiated shelter-in-place** — per-zone "evacuate / hold / staggered" instead of one global evacuate, to prevent crush. Extends `circuit_state` + beacons (47/49).
- **N3 Lost-person / child find** — nearby phones passively scan a reported person's BLE token; anonymized sightings → staff map. Reuses BLE (P3/6) + deferred queue (61).
- **N4 🏛️ Multi-hop BLE mesh for critical alerts** — alerts hop phone→phone (store-and-forward, TTL+sequence) to reach dead zones. Extends P3.
- **N5 Staff panic button** — geolocated SOS dispatched to nearest staff. Extends staff panel (52) + triage (48).

## Crowd management
- **N6 Staggered egress** — personalized "leave now / wait 15 min" by parking/tribune to flatten the exit surge. Uses parking (14) + heatmap (45) + Find My Car (16).
- **N7 Virtual queue / time-slot** — reserve a slot for activations/food, get notified at your turn. Complements Click & Collect (21).
- **N8 Live crowdsourced accessibility** — wheelchair/stroller users confirm/deny "elevator working", "ramp clear" → live accessibility map. Extends accessible routes (4) + reports (25).

## Experience
- **N9 "Calm" / sensory route & mode** — routes that avoid noise/crowds/flashing for neurodivergent or anxious visitors. Edge `noise/intensity` attribute on the graph.
- **N10 Haptic turn-by-turn (wearable)** — watch/phone vibration cues so you don't look at the screen in the sun. Sibling of SoundTags (33).
- **N11 Kid mode / digital wristband** — pair a child's phone or BLE tag to an adult; alert both if separation exceeds a threshold. Proximity BLE (6) + groups (37).

## Resilience (thesis)
- **N12 🏛️ Decentralized degraded failover** — if the central server/NAS dies, beacons hold last state, phones coordinate via mesh, staff operate degraded; reconcile on recovery. Last-write-wins + deferred queue (61) + beacon state (47/60).
- **N13 Beacon fleet health (predictive)** — panel monitors per-totem battery/signal/uptime and warns before failure ("Z3 no heartbeat for 40s"). Uses existing heartbeats + thresholds.
