/**
 * Incident triage helpers (web mirror of api/lib/incidents.js).
 *
 * The control panel fetches the raw `incidents` table, which accumulates
 * near-duplicate reports of the same event filed within seconds of each other.
 * These pure functions deduplicate those reports and order the result by
 * severity (CRITICAL > WARNING > INFO, then most recent) so operators see a
 * clean, prioritized list. Kept entirely framework-free so it can be unit-tested
 * with vitest and reused outside React.
 */

export interface Incident {
    id?: string | number;
    category?: string | null;
    type?: string | null;
    title?: string | null;
    description?: string | null;
    level?: string | null;
    zone?: string | number | null;
    zone_id?: string | number | null;
    lat?: number | string | null;
    lon?: number | string | null;
    created_at?: string | null;
    timestamp?: string | number | null;
    status?: string | null;
    duplicate_count?: number;
    // The dynamic schema may carry arbitrary extra columns.
    [key: string]: unknown;
}

/** Severity rank: higher = more urgent. Unknown levels rank 0 (below INFO). */
const SEVERITY_RANK: Record<string, number> = { CRITICAL: 3, WARNING: 2, INFO: 1 };

export const DEFAULT_RADIUS_METERS = 75;
export const DEFAULT_WINDOW_MINUTES = 10;
const EARTH_RADIUS_M = 6371000;

export interface TriageOptions {
    radiusMeters?: number;
    windowMinutes?: number;
}

export function severityRank(level?: string | null): number {
    if (typeof level !== "string") return 0;
    return SEVERITY_RANK[level.toUpperCase()] ?? 0;
}

/** Great-circle distance in metres; Infinity when any coordinate is missing. */
export function haversineMeters(
    lat1: unknown, lon1: unknown, lat2: unknown, lon2: unknown
): number {
    // Treat null/undefined/blank as missing (Number(null) === 0 would otherwise
    // be read as a valid coordinate on the equator/prime meridian).
    const toNum = (v: unknown): number =>
        v === null || v === undefined || v === "" ? NaN : Number(v);
    const a1 = toNum(lat1), o1 = toNum(lon1), a2 = toNum(lat2), o2 = toNum(lon2);
    if (![a1, o1, a2, o2].every((n) => Number.isFinite(n))) return Infinity;

    const toRad = (d: number) => (d * Math.PI) / 180;
    const dLat = toRad(a2 - a1);
    const dLon = toRad(o2 - o1);
    const s =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(a1)) * Math.cos(toRad(a2)) * Math.sin(dLon / 2) ** 2;
    return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(s)));
}

function incidentEpoch(inc: Incident): number {
    const raw = inc.created_at ?? inc.timestamp;
    if (raw === null || raw === undefined) return NaN;
    const ms = new Date(raw).getTime();
    return Number.isNaN(ms) ? NaN : ms;
}

function normCategory(inc: Incident): string {
    const c = inc.category ?? inc.type ?? inc.title;
    return typeof c === "string" ? c.trim().toLowerCase() : "";
}

function earlier(a: Incident, b: Incident): boolean {
    const ta = incidentEpoch(a);
    const tb = incidentEpoch(b);
    if (Number.isFinite(ta) && Number.isFinite(tb)) return ta < tb;
    if (Number.isFinite(ta)) return true;
    return false;
}

function isNearDuplicate(
    candidate: Incident, existing: Incident, radiusMeters: number, windowMinutes: number
): boolean {
    if (normCategory(candidate) !== normCategory(existing)) return false;

    const t1 = incidentEpoch(candidate);
    const t2 = incidentEpoch(existing);
    if (Number.isFinite(t1) && Number.isFinite(t2)) {
        if (Math.abs(t1 - t2) > windowMinutes * 60 * 1000) return false;
    }

    const dist = haversineMeters(candidate.lat, candidate.lon, existing.lat, existing.lon);
    if (Number.isFinite(dist)) {
        return dist <= radiusMeters;
    }

    const z1 = candidate.zone ?? candidate.zone_id;
    const z2 = existing.zone ?? existing.zone_id;
    if (z1 !== null && z1 !== undefined && String(z1) !== "") {
        return String(z1) === String(z2);
    }
    return true;
}

/**
 * Collapse near-identical incidents (same category within radius + time window).
 * The most severe (then earliest) report represents each cluster and carries a
 * `duplicate_count`. Input is not mutated.
 */
export function dedupeIncidents(list: Incident[], opts: TriageOptions = {}): Incident[] {
    if (!Array.isArray(list)) return [];
    const radiusMeters = Number.isFinite(opts.radiusMeters)
        ? (opts.radiusMeters as number)
        : DEFAULT_RADIUS_METERS;
    const windowMinutes = Number.isFinite(opts.windowMinutes)
        ? (opts.windowMinutes as number)
        : DEFAULT_WINDOW_MINUTES;

    const kept: Array<{ rep: Incident; count: number }> = [];

    for (const inc of list) {
        if (!inc || typeof inc !== "object") continue;

        const match = kept.find((k) => isNearDuplicate(inc, k.rep, radiusMeters, windowMinutes));
        if (!match) {
            kept.push({ rep: inc, count: 1 });
            continue;
        }

        match.count += 1;
        const better =
            severityRank(inc.level) > severityRank(match.rep.level) ||
            (severityRank(inc.level) === severityRank(match.rep.level) && earlier(inc, match.rep));
        if (better) match.rep = inc;
    }

    return kept.map(({ rep, count }) => ({ ...rep, duplicate_count: count }));
}

/**
 * Order incidents by severity (CRITICAL > WARNING > INFO), then most recent.
 * Stable for equal keys. Input is not mutated.
 */
export function prioritizeIncidents(list: Incident[]): Incident[] {
    if (!Array.isArray(list)) return [];
    return list
        .map((inc, idx) => ({ inc, idx }))
        .sort((a, b) => {
            const sev = severityRank(b.inc.level) - severityRank(a.inc.level);
            if (sev !== 0) return sev;

            const ta = incidentEpoch(a.inc);
            const tb = incidentEpoch(b.inc);
            const fa = Number.isFinite(ta);
            const fb = Number.isFinite(tb);
            if (fa && fb && tb !== ta) return tb - ta;
            if (fa !== fb) return fa ? -1 : 1;
            return a.idx - b.idx;
        })
        .map(({ inc }) => inc);
}

/** Dedupe then prioritize — the canonical view for the operator panel. */
export function triageIncidents(list: Incident[], opts?: TriageOptions): Incident[] {
    return prioritizeIncidents(dedupeIncidents(list, opts));
}
