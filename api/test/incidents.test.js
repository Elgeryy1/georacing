'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {
    severityRank,
    haversineMeters,
    dedupeIncidents,
    prioritizeIncidents,
    triageIncidents,
} = require('../lib/incidents');

test('severityRank orders CRITICAL > WARNING > INFO and is case-insensitive', () => {
    assert.ok(severityRank('CRITICAL') > severityRank('WARNING'));
    assert.ok(severityRank('WARNING') > severityRank('INFO'));
    assert.equal(severityRank('critical'), severityRank('CRITICAL'));
    assert.equal(severityRank('unknown'), 0);
    assert.equal(severityRank(undefined), 0);
});

test('haversineMeters approximates known distances and rejects missing coords', () => {
    // ~111 m: 0.001 deg of latitude is ~111 m near the equator.
    const d = haversineMeters(41.570, 2.260, 41.571, 2.260);
    assert.ok(d > 105 && d < 117, `expected ~111 m, got ${d}`);
    assert.equal(haversineMeters(41.57, 2.26, 41.57, undefined), Infinity);
    assert.equal(haversineMeters(41.57, 2.26, 41.57, null), Infinity);
    assert.equal(haversineMeters(41.57, 2.26, 41.57, ''), Infinity);
    assert.equal(haversineMeters(NaN, 2.26, 41.57, 2.26), Infinity);
});

test('dedupeIncidents collapses same-category reports within radius and time', () => {
    const list = [
        { id: 1, category: 'medical', level: 'WARNING', lat: 41.5700, lon: 2.2600, created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'medical', level: 'CRITICAL', lat: 41.5700, lon: 2.2601, created_at: '2026-06-13T10:01:00Z' }, // dup, more severe
        { id: 3, category: 'medical', level: 'INFO', lat: 41.5700, lon: 2.2600, created_at: '2026-06-13T10:02:00Z' },     // dup
    ];
    const out = dedupeIncidents(list);
    assert.equal(out.length, 1);
    // Most severe report represents the cluster.
    assert.equal(out[0].level, 'CRITICAL');
    assert.equal(out[0].id, 2);
    assert.equal(out[0].duplicate_count, 3);
});

test('dedupeIncidents keeps incidents of different categories at the same spot', () => {
    const list = [
        { id: 1, category: 'medical', level: 'WARNING', lat: 41.57, lon: 2.26, created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'hazard', level: 'WARNING', lat: 41.57, lon: 2.26, created_at: '2026-06-13T10:00:30Z' },
    ];
    assert.equal(dedupeIncidents(list).length, 2);
});

test('dedupeIncidents does NOT merge same-category reports outside the radius', () => {
    const list = [
        { id: 1, category: 'fire', level: 'CRITICAL', lat: 41.5700, lon: 2.2600, created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'fire', level: 'CRITICAL', lat: 41.6000, lon: 2.3000, created_at: '2026-06-13T10:00:30Z' }, // far away
    ];
    assert.equal(dedupeIncidents(list).length, 2);
});

test('dedupeIncidents does NOT merge same-category reports outside the time window', () => {
    const list = [
        { id: 1, category: 'lost-child', level: 'CRITICAL', lat: 41.57, lon: 2.26, created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'lost-child', level: 'CRITICAL', lat: 41.57, lon: 2.26, created_at: '2026-06-13T11:00:00Z' }, // 60 min later
    ];
    assert.equal(dedupeIncidents(list).length, 2);
});

test('dedupeIncidents falls back to zone equality when coordinates are absent', () => {
    const list = [
        { id: 1, category: 'noise', level: 'INFO', zone: '5', created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'noise', level: 'INFO', zone: '5', created_at: '2026-06-13T10:02:00Z' }, // same zone -> dup
        { id: 3, category: 'noise', level: 'INFO', zone: '9', created_at: '2026-06-13T10:03:00Z' }, // different zone
    ];
    const out = dedupeIncidents(list);
    assert.equal(out.length, 2);
    assert.equal(out[0].duplicate_count, 2);
});

test('dedupeIncidents does not mutate the input list', () => {
    const list = [{ id: 1, category: 'x', level: 'INFO', zone: '1', created_at: '2026-06-13T10:00:00Z' }];
    const snapshot = JSON.stringify(list);
    dedupeIncidents(list);
    assert.equal(JSON.stringify(list), snapshot);
    assert.equal(list[0].duplicate_count, undefined);
});

test('prioritizeIncidents orders by severity then most-recent', () => {
    const list = [
        { id: 1, level: 'INFO', created_at: '2026-06-13T10:00:00Z' },
        { id: 2, level: 'CRITICAL', created_at: '2026-06-13T09:00:00Z' },
        { id: 3, level: 'WARNING', created_at: '2026-06-13T10:30:00Z' },
        { id: 4, level: 'CRITICAL', created_at: '2026-06-13T10:00:00Z' }, // newer CRITICAL
    ];
    const out = prioritizeIncidents(list).map(i => i.id);
    assert.deepEqual(out, [4, 2, 3, 1]);
});

test('prioritizeIncidents is stable for equal severity and missing timestamps', () => {
    const list = [
        { id: 1, level: 'INFO' },
        { id: 2, level: 'INFO' },
        { id: 3, level: 'INFO' },
    ];
    assert.deepEqual(prioritizeIncidents(list).map(i => i.id), [1, 2, 3]);
});

test('triageIncidents dedupes then prioritizes', () => {
    const list = [
        { id: 1, category: 'medical', level: 'INFO', zone: '1', created_at: '2026-06-13T10:00:00Z' },
        { id: 2, category: 'medical', level: 'CRITICAL', zone: '1', created_at: '2026-06-13T10:01:00Z' }, // dup, wins
        { id: 3, category: 'hazard', level: 'WARNING', zone: '2', created_at: '2026-06-13T10:05:00Z' },
    ];
    const out = triageIncidents(list);
    assert.equal(out.length, 2);
    assert.equal(out[0].level, 'CRITICAL'); // medical cluster, most severe
    assert.equal(out[0].duplicate_count, 2);
    assert.equal(out[1].level, 'WARNING');
});

test('dedupeIncidents and prioritizeIncidents tolerate non-array input', () => {
    assert.deepEqual(dedupeIncidents(undefined), []);
    assert.deepEqual(dedupeIncidents(null), []);
    assert.deepEqual(prioritizeIncidents(undefined), []);
});
