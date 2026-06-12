# GeoRacing Backend API

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

## Setup

```bash
cd backend
npm install
cp .env.example .env   # then edit .env with your credentials
# Place the TLS key/cert files referenced by SSL_*_PATH
npm start
```

Requires Node.js >= 18 and a reachable MySQL/MariaDB server.

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
