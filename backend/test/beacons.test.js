'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { toEpoch, findDuplicateBeaconIds } = require('../lib/beacons');

test('toEpoch parses timestamps and defaults missing values to 0', () => {
    assert.equal(toEpoch(null), 0);
    assert.equal(toEpoch(undefined), 0);
    assert.equal(toEpoch('not-a-date'), 0);
    assert.equal(toEpoch('1970-01-01T00:00:00.000Z'), 0);
    assert.equal(toEpoch('2024-01-01T00:00:00.000Z'), Date.parse('2024-01-01T00:00:00.000Z'));
});

test('findDuplicateBeaconIds keeps the newest row per beacon_uid', () => {
    const rows = [
        { id: 1, beacon_uid: 'A', last_seen: '2024-01-01T10:00:00Z' },
        { id: 2, beacon_uid: 'A', last_seen: '2024-01-02T10:00:00Z' }, // newer A
        { id: 3, beacon_uid: 'B', last_seen: '2024-01-01T10:00:00Z' }, // unique B
        { id: 4, beacon_uid: 'A', last_seen: '2024-01-01T05:00:00Z' }, // older A
    ];

    // Rows 1 and 4 (older A duplicates) are removed; row 2 (newest A) and 3 (B) stay.
    assert.deepEqual(findDuplicateBeaconIds(rows), [1, 4]);
});

test('findDuplicateBeaconIds ignores rows without a beacon_uid', () => {
    const rows = [
        { id: 1, beacon_uid: null, last_seen: '2024-01-01T10:00:00Z' },
        { id: 2, beacon_uid: '', last_seen: '2024-01-02T10:00:00Z' },
        { id: 3, beacon_uid: 'C', last_seen: '2024-01-01T10:00:00Z' },
    ];
    assert.deepEqual(findDuplicateBeaconIds(rows), []);
});

test('findDuplicateBeaconIds keeps the first row on a last_seen tie', () => {
    const rows = [
        { id: 10, beacon_uid: 'D', last_seen: '2024-05-01T00:00:00Z' },
        { id: 11, beacon_uid: 'D', last_seen: '2024-05-01T00:00:00Z' }, // tie -> deleted
    ];
    assert.deepEqual(findDuplicateBeaconIds(rows), [11]);
});

test('findDuplicateBeaconIds treats a missing last_seen as oldest', () => {
    const rows = [
        { id: 20, beacon_uid: 'E' }, // no last_seen -> epoch 0
        { id: 21, beacon_uid: 'E', last_seen: '2024-01-01T00:00:00Z' }, // newer -> kept
    ];
    assert.deepEqual(findDuplicateBeaconIds(rows), [20]);
});
