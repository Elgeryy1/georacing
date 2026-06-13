# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Test suites across the stack: Vitest for the web panel, JUnit for Android,
  `node --test` for the backend and the Discord bot (**194 tests** in total).
- Continuous integration: GitHub Actions for the web panel (lint + test +
  build), Node services (syntax + tests) and Android (unit tests +
  `assembleDebug`, with the debug APK uploaded as an artifact).
- `docker-compose.yml` and `Dockerfile` for the backend, plus a `Dockerfile`
  for the Discord bot.
- Project hygiene: `CODE_OF_CONDUCT.md`, `SECURITY.md`, `.editorconfig`,
  `.gitattributes`, issue and pull-request templates, and `docs/BUG_AUDIT.md`.
- Per-component READMEs and an English `docs/ARCHITECTURE.md` with a Mermaid
  system diagram.

### Fixed
- **Backend:** closed a SQL-injection vector in the dynamic-schema endpoints —
  all table and column identifiers are now validated against a strict allow-list
  and back-tick quoted; values stay parameterized. `_delete` rejects an empty
  `where`; `_ensure_column` restricts the column type to an allow-list.
- **Beacons:** the trust-all TLS callback is now secure by default and only
  bypasses certificate validation in debug builds with an explicit opt-in env
  var. Temperature is parsed and clamped correctly; the polling timer no longer
  re-enters; the evacuation arrow hides when there is no direction.
- **Android:** the BLE parser now accepts the 13-byte staff evacuation
  broadcast; the scan callback is guarded against `SecurityException`; the user
  beacon stops advertising when location sharing is off; Android Auto releases
  TextToSpeech; the incident form survives rotation.
- **Web panel:** fixed a permanently stuck refresh spinner, a false
  "evacuation active" state while loading, a logout that did not clear the
  session, stale circuit-state fields on the dashboard and a `€NaN` order price;
  the API client no longer retries non-idempotent mutations.
- **iOS:** fixed a duplicate-key crash, `Int64` encoding that broke circuit
  state writes, a group-location polling leak and retain cycles in view models.

### Changed
- The Android app now builds from a clean checkout: build files, the Gradle
  wrapper and missing drawable resources were restored, and
  `google-services.json.example` carries a placeholder OAuth client.
- Replaced the personal development server hostname with `georacing.example.com`
  throughout the repository.

> This repository is a curated export of the original team workspace; build
> artifacts, duplicates and credentials were removed before publication.
