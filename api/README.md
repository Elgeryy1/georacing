# GeoRacing API

Express + MySQL/MariaDB REST API for the GeoRacing motorsport event platform. It serves the Android visitor app, the BeaconApp (track-side hardware) and the web panel.

Key features:

- **Smart Schema ("Firestore-like")** — generic `_upsert` / `_get` / `_read` endpoints automatically create tables and columns on demand, inferring SQL types from JSON values.
- **Beacon management** — heartbeat upserts, duplicate self-healing at startup, and a command queue (`PENDING` → `EXECUTED`) for remote beacon control.
- **Weather service** — fetches current conditions for the circuit from Open-Meteo every 15 minutes and stores them in `circuit_state`.
- **HTTPS only** — the server starts with the TLS key/certificate files configured via environment variables.

## API Endpoints

### Health

| Method | Route | Description |
|--------|-------|-------------|
| GET | `/health` | Health check (status, version, timestamp). |

### Domain endpoints

| Method | Route | Description |
|--------|-------|-------------|
| GET | `/api/beacons` | List all beacons. |
| POST | `/api/beacons/heartbeat` | Upsert a beacon by `beacon_uid` (body: `beacon_uid`, `mode`, `status`); updates `last_seen`. |
| GET | `/api/pois` | List points of interest. |
| GET | `/api/state` | Get the circuit state (single row, includes weather fields). |
| POST | `/api/incidents` | Create an incident report (arbitrary JSON body). |
| POST | `/api/group-gps` | Insert a GPS position sample for a group. |
| GET | `/api/group-gps/:groupName` | List members of a group (reads `group_members`). |
| POST | `/api/users` | Create/upsert a user. |
| POST | `/api/groups` | Create/upsert a group. |
| POST | `/api/commands` | Create a command for a beacon (sets `created_at` if missing). |
| GET | `/api/commands/pending/:beaconUid` | List `PENDING` commands for a beacon. |
| POST | `/api/commands/:id/execute` | Mark a command as `EXECUTED`. |

### Generic (Smart Schema) endpoints

| Method | Route | Description |
|--------|-------|-------------|
| POST | `/api/_get` | Generic SELECT. Body: `{ table, where? }` (equality filters, ANDed). |
| POST | `/api/_delete` | Generic DELETE. Body: `{ table, where }` (both required). |
| POST | `/api/_ensure_table` | Create table if it does not exist (`id VARCHAR PRIMARY KEY`). Body: `{ table }`. |
| POST | `/api/_ensure_column` | Add a column if missing. Body: `{ table, column, type?, value? }` (type inferred from `value` when `type` is omitted). |
| POST | `/api/_upsert` | Insert/update a row with auto table/column creation. Body: `{ table, data }`. Returns generated `id` if not provided. |
| GET | `/api/_read` | Generic SELECT via query string: `?table=<name>&limit=<n>&<col>=<value>...`. |

Notes:

- Reads against missing tables fail safe and return `[]` (or `{}` for `/api/state`).
- Errors return `500` with `{ "error": "<message>" }`; missing required parameters return `400`.
- **Identifier validation.** Because the generic endpoints take table/column
  names from the request and embed them as SQL identifiers (which cannot be bound
  as `?` placeholders), every table and column name is validated against a strict
  allow-list (`^[A-Za-z_][A-Za-z0-9_]*$`, see `lib/identifiers.js`) before it
  touches SQL. Anything else — e.g. a name containing a backtick — is rejected
  with `400`. Row **values** are always parameterized. The `type` accepted by
  `/api/_ensure_column` is likewise limited to a fixed allow-list of SQL types.

## Security model / trust boundary

The Smart-Schema (`/api/_*`) endpoints let a caller create tables and columns and
read/write arbitrary rows. They are designed for **trusted, internal clients**
(the Android app, the BeaconApp and the operator web panel on a private network),
not for direct exposure to the public internet. Defenses in place:

- **No SQL injection via identifiers or values** — identifiers go through the
  allow-list above; values are always parameterized (`?`). The pure builders in
  `lib/schema.js` re-validate identifiers as defense in depth, and the behavior is
  covered by unit tests (`test/identifiers.test.js`, `test/schema.test.js`).
- **`/api/_delete` requires a non-empty `where`** so it can never truncate a table.

Before exposing this API beyond a trusted network, add authentication/authorization
and rate limiting in front of it (e.g. a reverse proxy or an auth middleware).

## Environment Variables

Copy `.env.example` to `.env` and fill in the values. The server exits at startup with a clear error if a required variable is missing.

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | Yes | — | MySQL/MariaDB host. |
| `DB_PORT` | No | `3306` | Database port. |
| `DB_USER` | Yes | — | Database user. |
| `DB_PASSWORD` | Yes | — | Database password. |
| `DB_NAME` | Yes | — | Database name (created automatically if missing). |
| `PORT` | No | `4010` | HTTPS listen port. |
| `SSL_KEY_PATH` | No | `SSLprivatekey.key` | Path to the TLS private key. |
| `SSL_CERT_PATH` | No | `SSLcertificate.crt` | Path to the TLS certificate. |
| `SSL_CA_PATH` | No | `SSLIntermediateCertificate.crt` | Path to the intermediate CA certificate. |

## Quick start with Docker

The fastest way to try the API is the bundled Compose stack, which provisions
MySQL 8 and the API together:

```bash
cd api
cp .env.example .env        # optional: tweak credentials/port
docker compose up --build
```

What happens:

- The `db` service (MySQL 8, utf8mb4) starts and exposes a healthcheck.
- The `api` service waits until the database is healthy (`depends_on: condition:
  service_healthy`), then boots on HTTPS port `4010`.
- Because the server is HTTPS-only, the container mints a throwaway self-signed
  certificate on first start (see `docker-entrypoint.sh`) so there is nothing to
  configure. The image also defines a `HEALTHCHECK` that probes `/health`.

Verify it is up (the dev certificate is self-signed, so skip verification):

```bash
curl -k https://localhost:4010/health
# {"status":"ok","version":"2.0-smart-schema","timestamp":"..."}
```

For production, mount real TLS material over the paths in `SSL_KEY_PATH` /
`SSL_CERT_PATH` / `SSL_CA_PATH` and the self-signed step is skipped.

## Local setup (without Docker)

```bash
cd api
npm install
cp .env.example .env   # then edit .env with your credentials
# Place the TLS key/cert files referenced by SSL_*_PATH
npm start
```

Requires Node.js >= 18 and a reachable MySQL/MariaDB server.

## Testing

Pure logic (Smart-Schema type inference and upsert generation, SQL identifier
validation, beacon deduplication, and weather formatting) is extracted into
`lib/` so it can be unit-tested without a database or network. Tests use Node's
built-in test runner — no extra dependencies:

```bash
npm test        # node --test
```

## Project structure

```
api/
├── server.js              # Express app, routes, DB bootstrap, weather loop
├── lib/
│   ├── schema.js          # SQL type inference + idempotent upsert builder
│   ├── identifiers.js     # SQL identifier allow-list validation
│   ├── beacons.js         # Beacon deduplication logic
│   └── weather.js         # WMO code + wind-bearing formatting
├── test/                  # node:test unit tests for lib/
├── Dockerfile             # Production image (alpine, npm ci, non-root)
├── docker-entrypoint.sh   # Mints a dev self-signed cert if none is mounted
└── docker-compose.yml     # API + MySQL 8 stack
```

## Database Schema

The schema is created and evolved automatically (utf8mb4). Tables the code touches:

| Table | Purpose |
|-------|---------|
| `beacons` | Track-side beacons: `id`, `beacon_uid` (unique), `mode`, `status`, `last_seen`. Deduplicated at startup. |
| `commands` | Command queue per beacon: `beacon_uid`, `status` (`PENDING`/`EXECUTED`), `created_at`. |
| `circuit_state` | Single-row circuit status; weather columns `temperature`, `humidity`, `wind`, `forecast` are ensured at startup. |
| `pois` | Points of interest shown in the visitor app. |
| `incidents` | Incident reports from clients. |
| `group_gps` | GPS position samples per group. |
| `group_members` | Group membership, queried by `group_name`. |
| `users` | App users. |
| `groups` | User groups. |
| `beacon_logs` | Beacon log entries (charset maintenance at startup). |

All dynamically created tables use `id VARCHAR(191) PRIMARY KEY`; additional columns are added on the fly with types inferred from the inserted JSON (`INT`, `DOUBLE`, `TINYINT(1)` or `TEXT`).

## License

MIT
