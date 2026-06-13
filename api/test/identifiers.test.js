'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {
    isValidIdentifier,
    assertValidIdentifier,
    quoteIdentifier,
} = require('../lib/identifiers');

test('isValidIdentifier accepts plain SQL identifiers', () => {
    assert.equal(isValidIdentifier('beacons'), true);
    assert.equal(isValidIdentifier('beacon_uid'), true);
    assert.equal(isValidIdentifier('_private'), true);
    assert.equal(isValidIdentifier('Col123'), true);
    assert.equal(isValidIdentifier('A'), true);
});

test('isValidIdentifier rejects injection attempts and malformed names', () => {
    // The exact payload from the security review.
    assert.equal(isValidIdentifier('x`; DROP TABLE beacons; -- '), false);
    assert.equal(isValidIdentifier('beacons`'), false);      // stray backtick
    assert.equal(isValidIdentifier('has space'), false);
    assert.equal(isValidIdentifier('1leading_digit'), false); // cannot start with a digit
    assert.equal(isValidIdentifier('semi;colon'), false);
    assert.equal(isValidIdentifier('quote\'d'), false);
    assert.equal(isValidIdentifier('a-b'), false);            // hyphen not allowed
    assert.equal(isValidIdentifier(''), false);               // empty string
});

test('isValidIdentifier rejects non-string inputs', () => {
    assert.equal(isValidIdentifier(null), false);
    assert.equal(isValidIdentifier(undefined), false);
    assert.equal(isValidIdentifier(123), false);
    assert.equal(isValidIdentifier({ toString: () => 'beacons' }), false);
    assert.equal(isValidIdentifier(['beacons']), false);
});

test('assertValidIdentifier returns valid names and labels the error code', () => {
    assert.equal(assertValidIdentifier('beacons', 'table name'), 'beacons');

    try {
        assertValidIdentifier('x`; DROP TABLE beacons; -- ', 'table name');
        assert.fail('expected assertValidIdentifier to throw');
    } catch (err) {
        assert.equal(err.code, 'INVALID_IDENTIFIER');
        assert.match(err.message, /Invalid table name/);
    }
});

test('quoteIdentifier wraps valid names in backticks and never embeds an unescaped one', () => {
    assert.equal(quoteIdentifier('beacons'), '`beacons`');
    assert.equal(quoteIdentifier('beacon_uid', 'column name'), '`beacon_uid`');

    // A safe identifier can contain only one backtick: the surrounding pair has
    // exactly two, so no payload can ever break out of the quoting.
    const quoted = quoteIdentifier('circuit_state');
    assert.equal((quoted.match(/`/g) || []).length, 2);
});

test('quoteIdentifier throws on a backtick-bearing payload instead of quoting it', () => {
    assert.throws(
        () => quoteIdentifier('x`; DROP TABLE beacons; -- ', 'table name'),
        /Invalid table name/
    );
});
