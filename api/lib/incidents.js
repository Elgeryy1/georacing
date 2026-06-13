'use strict';

/**
 * Incident triage helpers: deduplication + priority ordering.
 *
 * Field staff and the public app both file incidents (medical, hazard, lost
 * child, etc.). The same real-world event is frequently reported several times
 * within seconds from people standing near each other, so the raw `incidents`
 * table fills with near-duplicates. The control panel wants a clean, severity-
 * ordered list. These pure functions implement that triage so it can be unit-
 * tested without a database and reused by the API and the web panel.
 *
 * "Near-identical" = same category, reported within N metres and T minutes of an
 * already-kept incident. When coordinates are missing, proximity falls back to
 * "same zone". Severity order is CRITICAL > WARNING > INFO.
 */

/** Severity rank: higher number = more urgent. Unknown levels rank lowest. */
const SEVERITY_RANK = { CRITICAL: 3, WARNING: 2, INFO: 1 };

/** Default dedup window: 75 metres and 10 minutes. */
const DEFAULT_RADIUS_METERS = 75;
const DEFAULT_WINDOW_MINUTES = 10;

const EARTH_RADIUS_M = 6371000;

/**
 * Numeric severity rank for an incident's level (case-insensitive). Unknown or
 * missing levels rank 0 (below INFO).
 *
 * @param {string} [level] - e.g. 'CRITICAL', 'WARNING', 'INFO'.
 * @returns {number}
 */
function severityRank(level) {
    if (typeof level !== 'string') return 0;
    return SEVERITY_RANK[level.toUpperCase()] || 0;
}

/**
 * Great-circle distance between two lat/lon points in metres (haversine).
 * Returns Infinity if either coordinate pair is not fully numeric, so callers
 * treat "unknown distance" as "not within radius".
 *
 * @returns {number} Distance in metres, or Infinity when coordinates are absent.
 */
function haversineMeters(lat1, lon1, lat2, lon2) {
    // Treat null/undefined/blank as missing (Number(null) === 0 would otherwise
    // be read as a valid coordinate at the equator/prime meridian).
    const toNum = (v) => (v === null || v === undefined || v === '' ? NaN : Number(v));
    const a1 = toNum(lat1), o1 = toNum(lon1), a2 = toNum(lat2), o2 = toNum(lon2);
    if (![a1, o1, a2, o2].every(Number.isFinite)) return Infinity;

    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(a2 - a1);
    const dLon = toRad(o2 - o1);
    const s =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(a1)) * Math.cos(toRad(a2)) * Math.sin(dLon / 2) ** 2;
    return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(s)));
}

/** Parse an incident timestamp into epoch ms; NaN when absent/unparseable. */
function incidentEpoch(inc) {
    const raw = inc && (inc.created_at ?? inc.timestamp ?? inc.time);
    if (raw === null || raw === undefined) return NaN;
    const ms = new Date(raw).getTime();
    return Number.isNaN(ms) ? NaN : ms;
}

/** Normalize a category for comparison (trimmed, lower-cased; '' when absent). */
function normCategory(inc) {
    const c = inc && (inc.category ?? inc.type ?? inc.title);
    return typeof c === 'string' ? c.trim().toLowerCase() : '';
}

/**
 * Decide whether `candidate` is a near-duplicate of an already-kept `existing`
 * incident under the given radius/window. Same category is required. Proximity
 * is by haversine when both have coordinates; otherwise it falls back to a
 * same-zone check. The time check is skipped when either timestamp is unknown
 * (so coordinate/zone proximity alone can still collapse undated dupes).
 *
 * @returns {boolean}
 */
function isNearDuplicate(candidate, existing, radiusMeters, windowMinutes) {
    if (normCategory(candidate) !== normCategory(existing)) return false;

    // Time window (only enforced when both timestamps are known).
    const t1 = incidentEpoch(candidate);
    const t2 = incidentEpoch(existing);
    if (Number.isFinite(t1) && Number.isFinite(t2)) {
        if (Math.abs(t1 - t2) > windowMinutes * 60 * 1000) return false;
    }

    // Spatial proximity: prefer coordinates, fall back to zone equality.
    const dist = haversineMeters(candidate.lat, candidate.lon, existing.lat, existing.lon);
    if (Number.isFinite(dist)) {
        return dist <= radiusMeters;
    }

    const z1 = candidate.zone ?? candidate.zone_id;
    const z2 = existing.zone ?? existing.zone_id;
    if (z1 !== null && z1 !== undefined && String(z1) !== '') {
        return String(z1) === String(z2);
    }

    // No coordinates and no zone on the candidate: same category within the time
    // window is enough to treat as a duplicate.
    return true;
}

/**
 * Collapse near-identical incidents. Two incidents are merged when they share a
 * category and fall within `radiusMeters` and `windowMinutes` of one another
 * (see {@link isNearDuplicate}). Among a duplicate cluster the most severe (then
 * earliest) incident is kept and annotated with `duplicate_count` (total reports
 * collapsed into it, including itself).
 *
 * Input is not mutated; kept incidents are returned as shallow copies with the
 * added `duplicate_count`. Order of the kept incidents follows first appearance.
 *
 * @param {Array<object>} list - Raw incidents.
 * @param {object} [opts]
 * @param {number} [opts.radiusMeters=75]
 * @param {number} [opts.windowMinutes=10]
 * @returns {Array<object>} Deduplicated incidents (copies).
 */
function dedupeIncidents(list, opts = {}) {
    if (!Array.isArray(list)) return [];
    const radiusMeters = Number.isFinite(opts.radiusMeters) ? opts.radiusMeters : DEFAULT_RADIUS_METERS;
    const windowMinutes = Number.isFinite(opts.windowMinutes) ? opts.windowMinutes : DEFAULT_WINDOW_MINUTES;

    const kept = []; // array of { rep: incident, count: number }

    for (const inc of list) {
        if (!inc || typeof inc !== 'object') continue;

        const match = kept.find((k) => isNearDuplicate(inc, k.rep, radiusMeters, windowMinutes));
        if (!match) {
            kept.push({ rep: inc, count: 1 });
            continue;
        }

        match.count += 1;
        // Promote the representative if the new report is more severe, or equally
        // severe but earlier, so the cluster is represented by its worst/earliest.
        const better =
            severityRank(inc.level) > severityRank(match.rep.level) ||
            (severityRank(inc.level) === severityRank(match.rep.level) &&
                earlier(inc, match.rep));
        if (better) match.rep = inc;
    }

    return kept.map(({ rep, count }) => ({ ...rep, duplicate_count: count }));
}

/** True when `a` is strictly earlier than `b` (unknown timestamps sort last). */
function earlier(a, b) {
    const ta = incidentEpoch(a);
    const tb = incidentEpoch(b);
    if (Number.isFinite(ta) && Number.isFinite(tb)) return ta < tb;
    if (Number.isFinite(ta)) return true;   // a dated, b not -> a earlier
    return false;
}

/**
 * Return incidents ordered by priority: most severe first (CRITICAL > WARNING >
 * INFO), breaking ties by most recent `created_at` first. Stable for equal keys.
 * Input is not mutated.
 *
 * @param {Array<object>} list
 * @returns {Array<object>} New, ordered array.
 */
function prioritizeIncidents(list) {
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
            if (fa && fb && tb !== ta) return tb - ta; // most recent first
            if (fa !== fb) return fa ? -1 : 1;          // dated before undated
            return a.idx - b.idx;                       // stable fallback
        })
        .map(({ inc }) => inc);
}

/**
 * Convenience pipeline: dedupe then prioritize. This is what UIs should call to
 * render a clean, ordered incident list.
 *
 * @param {Array<object>} list
 * @param {object} [opts] - Passed through to {@link dedupeIncidents}.
 * @returns {Array<object>}
 */
function triageIncidents(list, opts) {
    return prioritizeIncidents(dedupeIncidents(list, opts));
}

module.exports = {
    SEVERITY_RANK,
    DEFAULT_RADIUS_METERS,
    DEFAULT_WINDOW_MINUTES,
    severityRank,
    haversineMeters,
    isNearDuplicate,
    dedupeIncidents,
    prioritizeIncidents,
    triageIncidents,
};
