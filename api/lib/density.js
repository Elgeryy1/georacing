'use strict';

/**
 * Crowd-density helpers for the `GET /api/zones/density` endpoint.
 *
 * The mobile apps (iOS `CrowdDensityService`, Android staff screen) poll the API
 * for a per-zone crowd estimate so they can warn about congested areas and
 * suggest quieter routes. The backend does not run a dedicated people-counting
 * pipeline, so density is *derived deterministically* from data already present
 * in the database — primarily live group-GPS pings (`group_gps`), with beacons
 * as a fallback signal. Identical inputs always produce identical output (no
 * randomness), which keeps the figure stable between polls and unit-testable.
 *
 * These functions are pure: the Express handler is responsible for reading the
 * rows from SQL and grouping them; everything here operates on plain objects.
 */

/**
 * Thresholds (in number of distinct people/devices observed in a zone) at which
 * the density level steps up. A zone with fewer than MEDIUM people is "low",
 * fewer than HIGH is "medium", and at/above HIGH is "high". Tuned for a circuit
 * grandstand/food-court scale where ~25 concurrent devices in one zone is busy.
 */
const LEVEL_THRESHOLDS = { medium: 8, high: 25 };

/**
 * Count at which a zone is considered effectively saturated for the purpose of
 * the normalized 0..1 `density` ratio. Counts at or above this map to 1.0.
 */
const SATURATION_COUNT = 40;

/**
 * Minutes of estimated wait added per person observed in the zone, capped by
 * {@link MAX_WAIT_MINUTES}. A linear, explainable model: empty zone -> 0 min.
 */
const WAIT_MINUTES_PER_PERSON = 0.8;
const MAX_WAIT_MINUTES = 45;

/**
 * Map a raw occupancy count to a coarse density level.
 *
 * @param {number} count - Number of people/devices observed in the zone.
 * @returns {'low'|'medium'|'high'} The density bucket.
 */
function densityLevel(count) {
    const n = Number(count);
    if (!Number.isFinite(n) || n < LEVEL_THRESHOLDS.medium) return 'low';
    if (n < LEVEL_THRESHOLDS.high) return 'medium';
    return 'high';
}

/**
 * Normalize a raw occupancy count to a 0..1 density ratio (count / saturation),
 * clamped into [0, 1] and rounded to two decimals for a stable payload.
 *
 * @param {number} count - Number of people/devices observed in the zone.
 * @returns {number} Density ratio in [0, 1].
 */
function densityRatio(count) {
    const n = Number(count);
    if (!Number.isFinite(n) || n <= 0) return 0;
    const ratio = Math.min(n / SATURATION_COUNT, 1);
    return Math.round(ratio * 100) / 100;
}

/**
 * Estimate the wait (in whole minutes) implied by a zone's occupancy. Linear in
 * the count and capped at {@link MAX_WAIT_MINUTES}.
 *
 * @param {number} count - Number of people/devices observed in the zone.
 * @returns {number} Estimated wait in minutes (integer, >= 0).
 */
function estimatedWaitMinutes(count) {
    const n = Number(count);
    if (!Number.isFinite(n) || n <= 0) return 0;
    return Math.min(Math.round(n * WAIT_MINUTES_PER_PERSON), MAX_WAIT_MINUTES);
}

/**
 * Uppercase density level for clients that expect the iOS enum spelling
 * (LOW/MEDIUM/HIGH). The mobile `ZoneDensityDto.density_level` also accepts
 * CRITICAL, which we map from a fully saturated zone.
 *
 * @param {number} count - Number of people/devices observed in the zone.
 * @returns {'LOW'|'MEDIUM'|'HIGH'|'CRITICAL'}
 */
function densityLevelUpper(count) {
    const n = Number(count);
    if (Number.isFinite(n) && n >= SATURATION_COUNT) return 'CRITICAL';
    return densityLevel(count).toUpperCase();
}

/**
 * Derive a deterministic crowd-trend label from the current count and an
 * optional previous count. With no history (previous === undefined) the trend is
 * 'STABLE'. This keeps the field meaningful for the iOS DTO without inventing
 * random movement.
 *
 * @param {number} count - Current occupancy.
 * @param {number} [previous] - Previous occupancy, if known.
 * @returns {'RISING'|'FALLING'|'STABLE'}
 */
function densityTrend(count, previous) {
    const now = Number(count);
    const before = Number(previous);
    if (!Number.isFinite(before)) return 'STABLE';
    if (now > before) return 'RISING';
    if (now < before) return 'FALLING';
    return 'STABLE';
}

/**
 * Build the full density record for a single zone from its occupancy count.
 *
 * The returned shape is a superset that satisfies both the web/task contract
 * ({ zone_id, zone, density, estimated_wait_minutes, level }) and the iOS
 * `ZoneDensityDto` ({ zone_id, density_level, estimated_wait_minutes, trend }):
 *
 *   {
 *     zone_id: string,
 *     zone: string,                     // human label (same as zone_id here)
 *     density: number,                  // 0..1 ratio
 *     count: number,                    // raw observed occupancy
 *     estimated_wait_minutes: number,
 *     level: 'low'|'medium'|'high',     // task contract
 *     density_level: 'LOW'|'MEDIUM'|'HIGH'|'CRITICAL', // iOS DTO
 *     trend: 'RISING'|'FALLING'|'STABLE'
 *   }
 *
 * @param {string|number} zoneId - Zone identifier.
 * @param {number} count - Observed occupancy for the zone.
 * @param {number} [previousCount] - Prior occupancy, for trend (optional).
 * @returns {object} The density record.
 */
function buildZoneDensity(zoneId, count, previousCount) {
    const n = Number.isFinite(Number(count)) && Number(count) > 0 ? Math.trunc(Number(count)) : 0;
    const id = String(zoneId);
    return {
        zone_id: id,
        zone: id,
        density: densityRatio(n),
        count: n,
        estimated_wait_minutes: estimatedWaitMinutes(n),
        level: densityLevel(n),
        density_level: densityLevelUpper(n),
        trend: densityTrend(n, previousCount),
    };
}

/**
 * Build the full density payload (an array of zone records) from a map of
 * zone -> occupancy count. Output is sorted by zone id for a stable response.
 *
 * @param {Map<string, number>|Object<string, number>} countsByZone
 * @param {Map<string, number>|Object<string, number>} [previousByZone]
 * @returns {Array<object>} One record per zone, sorted by zone_id.
 */
function buildDensityPayload(countsByZone, previousByZone) {
    const entries = countsByZone instanceof Map
        ? Array.from(countsByZone.entries())
        : Object.entries(countsByZone || {});

    const prevGet = (zone) => {
        if (!previousByZone) return undefined;
        return previousByZone instanceof Map
            ? previousByZone.get(zone)
            : previousByZone[zone];
    };

    return entries
        .map(([zone, count]) => buildZoneDensity(zone, count, prevGet(zone)))
        .sort((a, b) => a.zone_id.localeCompare(b.zone_id));
}

/**
 * Count occupancy per zone from a list of rows. Each row contributes 1 to the
 * count of its zone, where the zone is read from the first present field in
 * `zoneFields` (default: zone_id, zone, group_name). Rows with no recognizable
 * zone are skipped.
 *
 * @param {Array<object>} rows - Database rows (e.g. group_gps or beacons).
 * @param {string[]} [zoneFields] - Candidate zone-identifying column names.
 * @returns {Map<string, number>} Map of zone id -> count.
 */
function countOccupancyByZone(rows, zoneFields = ['zone_id', 'zone', 'group_name']) {
    const counts = new Map();
    for (const row of rows || []) {
        if (!row || typeof row !== 'object') continue;
        let zone;
        for (const field of zoneFields) {
            const v = row[field];
            if (v !== null && v !== undefined && String(v).trim() !== '') {
                zone = String(v).trim();
                break;
            }
        }
        if (zone === undefined) continue;
        counts.set(zone, (counts.get(zone) || 0) + 1);
    }
    return counts;
}

module.exports = {
    LEVEL_THRESHOLDS,
    SATURATION_COUNT,
    WAIT_MINUTES_PER_PERSON,
    MAX_WAIT_MINUTES,
    densityLevel,
    densityRatio,
    estimatedWaitMinutes,
    densityLevelUpper,
    densityTrend,
    buildZoneDensity,
    buildDensityPayload,
    countOccupancyByZone,
};
