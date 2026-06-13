'use strict';

const { quoteIdentifier } = require('./identifiers');

/**
 * Smart-Schema helpers.
 *
 * The GeoRacing API maintains a dynamic ("Firestore-like") schema: tables and
 * columns are created on demand and SQL types are inferred from the JSON values
 * being inserted. These pure functions encapsulate that inference and the
 * generation of the idempotent upsert statement so they can be unit-tested
 * without a database connection.
 *
 * Table and column names are validated against a strict identifier allow-list
 * before being embedded in SQL (see ./identifiers), so a malicious key such as
 * "x`; DROP TABLE beacons; --" is rejected rather than quoted.
 */

/**
 * Map a JavaScript value to the MySQL/MariaDB column type used when a column is
 * created on the fly.
 *
 * @param {*} value - The sample value driving the inference.
 * @returns {string} A MySQL column type: INT, DOUBLE, TINYINT(1) or TEXT.
 */
function getSqlType(value) {
    if (typeof value === 'number') {
        return Number.isInteger(value) ? 'INT' : 'DOUBLE';
    }
    if (typeof value === 'boolean') return 'TINYINT(1)';
    return 'TEXT';
}

/**
 * Compute which columns from a data object are missing relative to the columns
 * that already exist in a table. The comparison is case-sensitive, matching
 * MySQL's default column-name handling for the dynamically managed tables.
 *
 * @param {string[]} existingColumns - Column names already present in the table.
 * @param {object} data - The row being inserted/upserted.
 * @returns {Array<{column: string, type: string}>} Columns to add, with types.
 */
function columnsToAdd(existingColumns, data) {
    const existing = new Set(existingColumns);
    return Object.keys(data)
        .filter((key) => !existing.has(key))
        .map((column) => {
            // Reject unsafe column names before they reach an ALTER TABLE.
            quoteIdentifier(column, 'column name');
            return { column, type: getSqlType(data[column]) };
        });
}

/**
 * Build the parameterized, idempotent INSERT ... ON DUPLICATE KEY UPDATE
 * statement used by genericInsert. Returns both the SQL text (with `?`
 * placeholders) and the ordered values array so callers never interpolate
 * user-supplied values into SQL.
 *
 * @param {string} table - Target table name (validated as an identifier).
 * @param {object} data - Row to insert; key order defines column/value order.
 * @returns {{ sql: string, values: any[] }}
 * @throws {Error} If `data` has no keys (nothing to insert), or if the table or
 *   any column name is not a valid SQL identifier (`code = 'INVALID_IDENTIFIER'`).
 */
function buildUpsert(table, data) {
    const quotedTable = quoteIdentifier(table, 'table name');
    const keys = Object.keys(data);
    if (keys.length === 0) {
        throw new Error('buildUpsert: data object must have at least one column');
    }
    // Validate every column name; quotedKeys can never contain a stray backtick.
    const quotedKeys = keys.map((k) => quoteIdentifier(k, 'column name'));
    const values = keys.map((k) => data[k]);
    const placeholders = keys.map(() => '?').join(', ');
    const updates = quotedKeys.map((qk) => `${qk} = VALUES(${qk})`).join(', ');
    const columnList = quotedKeys.join(', ');

    const sql =
        `INSERT INTO ${quotedTable} (${columnList}) VALUES (${placeholders}) ` +
        `ON DUPLICATE KEY UPDATE ${updates}`;

    return { sql, values };
}

module.exports = { getSqlType, columnsToAdd, buildUpsert };
