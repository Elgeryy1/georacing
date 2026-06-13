'use strict';

/**
 * SQL identifier validation for the dynamic ("Firestore-like") schema.
 *
 * The generic endpoints accept table and column names from the request body or
 * query string and embed them as identifiers in SQL. Row VALUES are always
 * parameterized, but identifiers cannot be passed as `?` placeholders, so they
 * must be validated against a strict allow-list before being interpolated.
 *
 * Wrapping an unvalidated name in backticks is NOT sufficient: a backtick inside
 * the name (e.g. `x`; DROP TABLE beacons; -- `) breaks out of the quoting and
 * enables SQL injection. These helpers reject anything that is not a plain
 * `[A-Za-z_][A-Za-z0-9_]*` identifier, which by construction can never contain a
 * backtick, whitespace, or SQL metacharacter.
 */

/**
 * Allow-list for SQL identifiers (table and column names): an ASCII letter or
 * underscore followed by letters, digits or underscores. Deliberately strict —
 * the dynamically managed tables/columns only ever use these characters.
 */
const SAFE_IDENTIFIER = /^[A-Za-z_][A-Za-z0-9_]*$/;

/**
 * Test whether a value is a safe SQL identifier.
 *
 * @param {*} name - Candidate table or column name.
 * @returns {boolean} True only for non-empty strings matching the allow-list.
 */
function isValidIdentifier(name) {
    return typeof name === 'string' && SAFE_IDENTIFIER.test(name);
}

/**
 * Assert that a value is a safe SQL identifier, returning it unchanged on
 * success. Throws an {@link Error} tagged with `code = 'INVALID_IDENTIFIER'` so
 * callers (e.g. the Express handlers) can translate it into an HTTP 400.
 *
 * @param {*} name - Candidate table or column name.
 * @param {string} [kind='identifier'] - Label used in the error message.
 * @returns {string} The validated identifier.
 * @throws {Error} If `name` is not a valid identifier.
 */
function assertValidIdentifier(name, kind = 'identifier') {
    if (!isValidIdentifier(name)) {
        const err = new Error(`Invalid ${kind}: ${JSON.stringify(name)}`);
        err.code = 'INVALID_IDENTIFIER';
        throw err;
    }
    return name;
}

/**
 * Quote a validated identifier with backticks for embedding in SQL. The name is
 * validated first, so the result can never contain an unescaped backtick.
 *
 * @param {*} name - Candidate table or column name.
 * @param {string} [kind='identifier'] - Label used in the error message.
 * @returns {string} The backtick-quoted identifier, e.g. "`beacons`".
 * @throws {Error} If `name` is not a valid identifier.
 */
function quoteIdentifier(name, kind = 'identifier') {
    return '`' + assertValidIdentifier(name, kind) + '`';
}

module.exports = {
    SAFE_IDENTIFIER,
    isValidIdentifier,
    assertValidIdentifier,
    quoteIdentifier,
};
