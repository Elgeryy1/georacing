'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {
    densityLevel,
    densityRatio,
    estimatedWaitMinutes,
    densityLevelUpper,
    densityTrend,
    buildZoneDensity,
    buildDensityPayload,
    countOccupancyByZone,
    SATURATION_COUNT,
    MAX_WAIT_MINUTES,
} = require('../lib/density');

test('densityLevel buckets counts into low/medium/high', () => {
    assert.equal(densityLevel(0), 'low');
    assert.equal(densityLevel(7), 'low');
    assert.equal(densityLevel(8), 'medium');
    assert.equal(densityLevel(24), 'medium');
    assert.equal(densityLevel(25), 'high');
    assert.equal(densityLevel(1000), 'high');
});

test('densityLevel is defensive against non-numeric input', () => {
    assert.equal(densityLevel(undefined), 'low');
    assert.equal(densityLevel(null), 'low');
    assert.equal(densityLevel('not-a-number'), 'low');
    assert.equal(densityLevel(-5), 'low');
});

test('densityRatio normalizes to a clamped 0..1 value', () => {
    assert.equal(densityRatio(0), 0);
    assert.equal(densityRatio(-3), 0);
    assert.equal(densityRatio(SATURATION_COUNT / 2), 0.5);
    assert.equal(densityRatio(SATURATION_COUNT), 1);
    assert.equal(densityRatio(SATURATION_COUNT * 10), 1); // clamped
    assert.equal(densityRatio('garbage'), 0);
});

test('estimatedWaitMinutes is linear and capped', () => {
    assert.equal(estimatedWaitMinutes(0), 0);
    assert.equal(estimatedWaitMinutes(10), 8); // 10 * 0.8
    assert.equal(estimatedWaitMinutes(1000), MAX_WAIT_MINUTES); // capped
    assert.equal(estimatedWaitMinutes(-1), 0);
});

test('densityLevelUpper maps to iOS enum and CRITICAL at saturation', () => {
    assert.equal(densityLevelUpper(0), 'LOW');
    assert.equal(densityLevelUpper(8), 'MEDIUM');
    assert.equal(densityLevelUpper(25), 'HIGH');
    assert.equal(densityLevelUpper(SATURATION_COUNT), 'CRITICAL');
    assert.equal(densityLevelUpper(SATURATION_COUNT + 50), 'CRITICAL');
});

test('densityTrend compares against previous, defaulting to STABLE', () => {
    assert.equal(densityTrend(10), 'STABLE'); // no history
    assert.equal(densityTrend(10, undefined), 'STABLE');
    assert.equal(densityTrend(10, 5), 'RISING');
    assert.equal(densityTrend(5, 10), 'FALLING');
    assert.equal(densityTrend(7, 7), 'STABLE');
});

test('buildZoneDensity returns a superset satisfying both web and iOS contracts', () => {
    const rec = buildZoneDensity('A', 10);
    // Task / web contract fields
    assert.equal(rec.zone_id, 'A');
    assert.equal(rec.zone, 'A');
    assert.equal(rec.density, 0.25); // 10 / 40
    assert.equal(rec.estimated_wait_minutes, 8);
    assert.equal(rec.level, 'medium');
    // iOS ZoneDensityDto fields
    assert.equal(rec.density_level, 'MEDIUM');
    assert.equal(rec.trend, 'STABLE');
    assert.equal(rec.count, 10);
});

test('buildZoneDensity is deterministic (same input -> same output)', () => {
    assert.deepEqual(buildZoneDensity('Z1', 12), buildZoneDensity('Z1', 12));
});

test('buildZoneDensity coerces a missing/zero count to an empty-but-valid zone', () => {
    const rec = buildZoneDensity(3, 0);
    assert.equal(rec.zone_id, '3');
    assert.equal(rec.density, 0);
    assert.equal(rec.level, 'low');
    assert.equal(rec.density_level, 'LOW');
    assert.equal(rec.estimated_wait_minutes, 0);
});

test('countOccupancyByZone tallies rows by the first present zone field', () => {
    const rows = [
        { zone_id: 'gateA' },
        { zone_id: 'gateA' },
        { zone: 'gateB' },           // falls back to `zone`
        { group_name: 'gateC' },     // falls back to `group_name`
        { unrelated: 'x' },          // skipped (no zone)
        { zone_id: '  ' },           // skipped (blank)
    ];
    const counts = countOccupancyByZone(rows);
    assert.equal(counts.get('gateA'), 2);
    assert.equal(counts.get('gateB'), 1);
    assert.equal(counts.get('gateC'), 1);
    assert.equal(counts.size, 3);
});

test('buildDensityPayload sorts by zone_id and accepts Map or object', () => {
    const fromMap = buildDensityPayload(new Map([['B', 30], ['A', 5]]));
    assert.deepEqual(fromMap.map(r => r.zone_id), ['A', 'B']);
    assert.equal(fromMap[0].level, 'low');   // A: 5
    assert.equal(fromMap[1].level, 'high');  // B: 30

    const fromObj = buildDensityPayload({ A: 5, B: 30 });
    assert.deepEqual(fromObj, fromMap);
});

test('buildDensityPayload computes trend from a previous snapshot', () => {
    const payload = buildDensityPayload(
        new Map([['A', 20]]),
        new Map([['A', 10]])
    );
    assert.equal(payload[0].trend, 'RISING');
});

test('buildDensityPayload tolerates empty/undefined input', () => {
    assert.deepEqual(buildDensityPayload(new Map()), []);
    assert.deepEqual(buildDensityPayload(undefined), []);
    assert.deepEqual(buildDensityPayload({}), []);
});
