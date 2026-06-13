# Security Policy

GeoRacing controls safety-critical, on-site signage (red flags, safety car,
evacuations). We take security and reliability seriously and appreciate
responsible disclosure of any vulnerability.

## Supported versions

This repository is a curated portfolio export of an academic project. It is
not deployed in production, so security fixes are applied on a best-effort
basis to the `main` branch only.

| Version | Supported |
|---------|-----------|
| `main`  | ✅        |
| Older tags / forks | ❌ |

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Instead, report privately through one of:

1. **GitHub Security Advisories** — use the repository's
   **Security → Report a vulnerability** tab to open a private advisory
   (preferred; this keeps the report confidential until a fix is ready).
2. **Email** the maintainers at the address listed on the repository profile.

Please include:

- A description of the issue and the component affected
  (`apps/android`, `apps/ios`, `web-panel`, `backend`, `beacons`,
  `discord-bot`).
- Steps to reproduce or a proof of concept.
- The potential impact (data exposure, command injection, denial of service,
  spoofed beacon commands, etc.).

We aim to acknowledge a report within **5 business days** and to provide a
remediation plan or fix timeline within **30 days**, depending on severity.

## Scope

In scope:

- The application source code in this repository.
- The REST command/heartbeat protocol between the backend and the beacons.
- The BLE advertising payload and how the apps trust it.
- Authentication/authorization logic in the web panel and backend.

Out of scope:

- Third-party services (Firebase, Supabase, Groq, Discord) — report those to
  the respective vendor.
- Issues that require a rooted/compromised device or physical access to a
  beacon PC.
- Missing security headers on a local dev server.

## Handling of secrets

This repository **must never contain real credentials**. API keys, tokens,
Firebase/Google service files and database passwords are provided through
environment variables or local untracked files; `.example` templates document
the expected keys. If you discover a committed secret, **report it privately**
so it can be rotated — see [CONTRIBUTING.md](CONTRIBUTING.md#security-and-secrets).
Removing a secret from the latest commit is **not** sufficient; it must be
rotated because it remains visible in history and in any clone.

## Disclosure

We follow coordinated disclosure: we will work with you on a fix and a
disclosure date, and credit you in the advisory unless you prefer to remain
anonymous.
