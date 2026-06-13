'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { getSqlType, columnsToAdd, buildUpsert } = require('../lib/schema');

test('getSqlType infers integer vs float numbers', () => {
    assert.equal(getSqlType(42), 'INT');
    assert.equal(getSqlType(0), 'INT');
    assert.equal(getSqlType(-7), 'INT');
    assert.equal(getSqlType(3.14), 'DOUBLE');
    assert.equal(getSqlType(41.57), 'DOUBLE');
});

test('getSqlType maps booleans and falls back to TEXT', () => {
    assert.equal(getSqlType(true), 'TINYINT(1)');
    assert.equal(getSqlType(false), 'TINYINT(1)');
    assert.equal(getSqlType('hello'), 'TEXT');
    assert.equal(getSqlType(null), 'TEXT');
    assert.equal(getSqlType(undefined), 'TEXT');
    assert.equal(getSqlType({ nested: 1 }), 'TEXT');
});

test('columnsToAdd returns only missing columns with inferred types', () => {
    const existing = ['id', 'beacon_uid'];
    const data = { id: 'b1', beacon_uid: 'uid-1', lat: 41.57, count: 3, active: true };

    const result = columnsToAdd(existing, data);

    assert.deepEqual(result, [
        { column: 'lat', type: 'DOUBLE' },
        { column: 'count', type: 'INT' },
        { column: 'active', type: 'TINYINT(1)' },
    ]);
});

test('columnsToAdd returns empty array when all columns already exist', () => {
    const existing = ['id', 'mode', 'status'];
    const data = { id: 'x', mode: 'A', status: 'ok' };
    assert.deepEqual(columnsToAdd(existing, data), []);
});

test('buildUpsert produces a parameterized idempotent statement', () => {
    const { sql, values } = buildUpsert('beacons', {
        id: 'uid-1',
        mode: 'RACE',
        status: 'ONLINE',
    });

    assert.equal(
        sql,
        'INSERT INTO `beacons` (`id`, `mode`, `status`) VALUES (?, ?, ?) ' +
        'ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `mode` = VALUES(`mode`), `status` = VALUES(`status`)'
    );
    assert.deepEqual(values, ['uid-1', 'RACE', 'ONLINE']);
    // No raw value should ever be interpolated into the SQL text.
    assert.ok(!sql.includes('uid-1'));
    assert.ok(!sql.includes('RACE'));
});

test('buildUpsert throws on empty data to avoid invalid SQL', () => {
    assert.throws(() => buildUpsert('beacons', {}), /at least one column/);
});

test('buildUpsert rejects an injecting table name instead of backtick-quoting it', () => {
    try {
        buildUpsert('x`; DROP TABLE beacons; -- ', { id: 'b1' });
        assert.fail('expected buildUpsert to reject the table name');
    } catch (err) {
        assert.equal(err.code, 'INVALID_IDENTIFIER');
        assert.match(err.message, /Invalid table name/);
    }
});

test('buildUpsert rejects an injecting column name', () => {
    assert.throws(
        () => buildUpsert('beacons', { 'evil` , (SELECT 1)) -- ': 'x' }),
        /Invalid column name/
    );
});

test('columnsToAdd rejects an unsafe column name before it reaches ALTER TABLE', () => {
    const existing = ['id'];
    const data = { id: 'b1', 'bad`name': 'oops' };
    assert.throws(() => columnsToAdd(existing, data), /Invalid column name/);
});
