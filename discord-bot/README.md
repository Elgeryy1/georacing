# GeoOps — GeoRacing Discord Bot

Operations bot ("GeoOps") for the GeoRacing Discord server. It answers technical questions with a RAG-style pipeline (Supabase knowledge base + Groq LLM inference), manages incident tickets, assigns team roles via buttons, and watches the Supabase `ideas` table in realtime to announce status changes.

Architecture:

- **discord.js v14** — slash commands (auto-deployed globally on startup) and button interactions.
- **Supabase** — knowledge base (`documents`), project status (`ideas`, with realtime subscription) and incident tickets (`incidents`).
- **Groq** — chat completion for answers (default model `llama-3.3-70b-versatile`, low temperature for technical precision; answers in Spanish).
- **OpenAI (optional)** — reserved for embedding-based retrieval; without a key the bot runs in keyword-search mode.

## Slash Commands

| Command | Options | Description |
|---------|---------|-------------|
| `/duda` | `question` (required) | Technical Q&A: searches the Supabase knowledge base (full-text search with keyword fallback) and answers via Groq with the retrieved context. |
| `/estado` | — | System status report: counts `ideas` by status (`Acabado` / `en proceso` / `por empezar`). |
| `/ping` | — | Latency diagnostics (round-trip and WebSocket). |
| `/aprender` | `texto` (optional), `archivo` (optional `.txt`/`.md`) | Ingests documentation into the `documents` knowledge base. At least one option is required. |
| `/incidente` | `titulo` (required), `severidad` (required: BAJA/MEDIA/ALTA/CRITICA) | Creates an incident ticket in Supabase and opens a public thread for it. |
| `/emitir` | `titulo`, `mensaje` (required), `imagen`, `mencion` (optional) | Broadcasts an official announcement embed. Administrators only. |
| `/equipo` | — | Posts a role-selection panel with buttons (Telemetría / Pista / Logística). Roles are auto-created if missing and toggled per click. |
| `/purgar` | `cantidad` (required, 1-100) | Bulk-deletes recent messages. Requires the Manage Messages permission. |
| `/perfil` | — | Operator profile card: user ID, join date and number of incidents reported. |

### Realtime Watchdog

The bot subscribes to `UPDATE` events on `public.ideas`. When a row's `status` changes, it posts a color-coded embed (old status → new status) to the channel configured in `CHANNEL_ID`.

## Environment Variables

Copy `.env.example` to `.env` and fill in the values. The bot exits at startup with a clear error if a required variable is missing.

| Variable | Required | Description |
|----------|----------|-------------|
| `DISCORD_TOKEN` | Yes | Discord bot token. |
| `CLIENT_ID` | Yes | Discord application ID (used to deploy slash commands). |
| `CHANNEL_ID` | Yes | Channel ID for watchdog status announcements. |
| `SUPABASE_URL` | Yes | Supabase project URL. |
| `SUPABASE_KEY` | Yes | Supabase service-role key (bot needs read/write + realtime). |
| `GROQ_API_KEY` | Yes | Groq API key for LLM inference. |
| `GROQ_MODEL` | No | Groq model ID (default: `llama-3.3-70b-versatile`). |
| `OPENAI_API_KEY` | No | Optional; reserved for embedding-based RAG. Without it the bot uses keyword search. |

## Setup

```bash
cd discord-bot
npm install
cp .env.example .env   # then edit .env with your credentials
npm start
```

Requires Node.js >= 18. Slash commands are registered automatically on startup (global commands can take up to an hour to propagate the first time).

### Database

Run `schema_v2.sql` in the Supabase SQL editor to create the `incidents` table. The bot also expects:

- `documents` — knowledge base; a `content` text column (full-text search is used with the `spanish` config, with `ILIKE` keyword fallback).
- `ideas` — project items with `title` and `status` columns; realtime must be enabled on this table for the watchdog.

## License

MIT
