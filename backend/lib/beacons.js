'use strict';

/**
 * Beacon deduplication helper.
 *
 * Beacon hardware re-registers periodically and, before a UNIQUE constraint was
 * added on `beacon_uid`, the table could accumulate duplicate rows for the same
 * physical beacon. At startup the server self-heals by keeping the most recently
 * seen row per `beacon_uid` and deleting the rest. This pure function contains
 * that decision logic so it can be tested without a database.
 */

/**
 * Parse a `last_seen` value into a comparable epoch (milliseconds). Missing or
 * unparseable timestamps are treated as the epoch (0) so any real timestamp
 * wins over them.
 *
 * @param {*} lastSeen - A Date, ISO string, or null/undefined.
 * @returns {number} Milliseconds since the epoch, or 0 when not parseable.
 */
function toEpoch(lastSeen) {
    if (lastSeen === null || lastSeen === undefined) return 0;
    const ms = new Date(lastSeen).getTime();
    return Number.isNaN(ms) ? 0 : ms;
}

/**
 * Given the current `beacons` rows, decide which row ids should be deleted so
 * that exactly one row remains per `beacon_uid` — the one with the newest
 * `last_seen`. Rows without a `beacon_uid` are ignored (left untouched).
 *
 * On ties (equal `last_seen`), the first row encountered is kept, matching the
 * server's "existing wins" behavior.
 *
 * @param {Array<{id: *, beacon_uid: *, last_seen?: *}>} rows
 * @returns {Array<*>} The ids of duplicate rows to delete (order preserved).
 */
function findDuplicateBeaconIds(rows) {
    const newestByUid = new Map();
    const deleteIds = [];

    for (const row of rows) {
        if (!row.beacon_uid) continue;

        const existing = newestByUid.get(row.beacon_uid);
        if (!existing) {
            newestByUid.set(row.beacon_uid, row);
            continue;
        }

        // Duplicate: keep the row with the strictly newer last_seen.
        if (toEpoch(row.last_seen) > toEpoch(existing.last_seen)) {
            deleteIds.push(existing.id);
            newestByUid.set(row.beacon_uid, row);
        } else {
            deleteIds.push(row.id);
        }
    }

    return deleteIds;
}

module.exports = { toEpoch, findDuplicateBeaconIds };
