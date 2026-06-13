const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });
const express = require('express');
const https = require('https');
const fs = require('fs');
const mysql = require('mysql2/promise');
const cors = require('cors');
const crypto = require('crypto');

// Pure, unit-tested helpers (see lib/ and test/).
const { getSqlType, columnsToAdd, buildUpsert } = require('./lib/schema');
const { findDuplicateBeaconIds } = require('./lib/beacons');
const { getCardinalDirection, describeWeatherCode } = require('./lib/weather');
const { isValidIdentifier, quoteIdentifier } = require('./lib/identifiers');

// --- Environment Validation ---
const REQUIRED_ENV = ['DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
const missingEnv = REQUIRED_ENV.filter((key) => !process.env[key]);

if (missingEnv.length > 0) {
    console.error(`❌ FATAL: Missing required environment variables: ${missingEnv.join(', ')}`);
    console.error('   Copy .env.example to .env and fill in the values.');
    process.exit(1);
}

const app = express();
app.use(cors());
app.use(express.json());

// Health Check Endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', version: '2.0-smart-schema', timestamp: new Date().toISOString() });
});


// Database Config
const dbConfig = {
    host: process.env.DB_HOST,
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    charset: 'utf8mb4'
};

// TLS certificate paths (overridable via env; defaults preserve previous behavior)
const SSL_KEY_PATH = process.env.SSL_KEY_PATH || 'SSLprivatekey.key';
const SSL_CERT_PATH = process.env.SSL_CERT_PATH || 'SSLcertificate.crt';
const SSL_CA_PATH = process.env.SSL_CA_PATH || 'SSLIntermediateCertificate.crt';

// Database Connection Pool (initialized in startServer)
let pool;


// --- Identifier validation (dynamic schema) ---
//
// The generic endpoints accept table/column names from the request and embed
// them as SQL identifiers (which cannot be passed as `?` placeholders). Validate
// them against the strict allow-list in lib/identifiers before they touch SQL.
// Row VALUES remain parameterized everywhere.

/**
 * Validate a candidate table/column name, responding with HTTP 400 when it is
 * not a safe identifier.
 *
 * @returns {boolean} True if valid (caller continues); false if a 400 was sent.
 */
function rejectInvalidIdentifier(res, name, kind) {
    if (!isValidIdentifier(name)) {
        res.status(400).json({ error: `Invalid ${kind}: ${JSON.stringify(name)}` });
        return false;
    }
    return true;
}

/**
 * Validate every key of a `where`/filter object as a column identifier. Sends a
 * 400 and returns false on the first invalid key; returns true otherwise.
 */
function rejectInvalidFilterKeys(res, filters) {
    for (const key of Object.keys(filters || {})) {
        if (!rejectInvalidIdentifier(res, key, 'column name')) return false;
    }
    return true;
}


// --- Endpoints ---

// --- Specific REST Endpoints (mapped to dynamic tables) ---

// Beacons (Android: GET /beacons)
app.get('/api/beacons', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM `beacons`');
        res.json(rows);
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json([]);
        console.error('Error getting beacons:', err);
        res.status(500).json({ error: err.message });
    }
});

// Beacons Heartbeat (BeaconApp: POST /beacons/heartbeat)
app.post('/api/beacons/heartbeat', async (req, res) => {
    const { beacon_uid, mode, status } = req.body;
    try {
        if (!beacon_uid) return res.status(400).json({ error: 'beacon_uid required' });

        // FIX: Manual check to prevent duplicates if DB schema lacks unique constraint
        const [existing] = await pool.query('SELECT id FROM `beacons` WHERE beacon_uid = ?', [beacon_uid]);
        const last_seen = new Date().toISOString().slice(0, 19).replace('T', ' ');

        if (existing.length > 0) {
            // Update existing
            await pool.query('UPDATE `beacons` SET mode = ?, status = ?, last_seen = ? WHERE beacon_uid = ?',
                [mode, status, last_seen, beacon_uid]);
        } else {
            // Insert new
            await genericInsert('beacons', {
                id: beacon_uid,
                beacon_uid,
                mode,
                status,
                last_seen
            });
        }
        res.json({ success: true });
    } catch (err) {
        console.error('Error heartbeat:', err);
        res.status(500).json({ error: err.message });
    }
});

// POIs (Android: GET /pois)
app.get('/api/pois', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM `pois`');
        res.json(rows);
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json([]);
        res.status(500).json({ error: err.message });
    }
});

// Circuit State (Android: GET /state)
app.get('/api/state', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM `circuit_state` LIMIT 1');
        res.json(rows[0] || {});
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json({});
        res.status(500).json({ error: err.message });
    }
});

// Incidents (Android: POST /incidents)
app.post('/api/incidents', async (req, res) => {
    try {
        await genericInsert('incidents', req.body);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Groups & GPS (Android)
app.post('/api/group-gps', async (req, res) => {
    try {
        await genericInsert('group_gps', req.body);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.get('/api/group-gps/:groupName', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM `group_members` WHERE group_name = ?', [req.params.groupName]);
        res.json(rows);
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/users', async (req, res) => {
    try {
        await genericInsert('users', req.body);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/groups', async (req, res) => {
    try {
        await genericInsert('groups', req.body);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

// Commands (BeaconApp)
// 1. Create Command
app.post('/api/commands', async (req, res) => {
    try {
        const data = { ...req.body };
        // Created_at is text in generic logic or date object? best to format string for SQL text.
        if (!data.created_at) data.created_at = new Date().toISOString().slice(0, 19).replace('T', ' ');

        const id = await genericInsert('commands', data);
        res.json({ success: true, id });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

// 2. Get Pending Commands
app.get('/api/commands/pending/:beaconUid', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM `commands` WHERE beacon_uid = ? AND status = ?', [req.params.beaconUid, 'PENDING']);
        res.json(rows);
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json([]);
        res.status(500).json({ error: err.message });
    }
});

// 3. Execute Command
app.post('/api/commands/:id/execute', async (req, res) => {
    try {
        await pool.query('UPDATE `commands` SET status = ? WHERE id = ?', ['EXECUTED', req.params.id]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ error: err.message }); }
});


// --- Generic Firestore-like Endpoints ---

// 1. POST /api/_get (BeaconApp variant of read)
app.post('/api/_get', async (req, res) => {
    const { table, where } = req.body;
    if (!table) return res.status(400).json({ error: 'Table required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;
    if (!rejectInvalidFilterKeys(res, where)) return;

    try {
        let query = `SELECT * FROM ${quoteIdentifier(table)}`;
        const queryParams = [];
        const whereClauses = [];

        if (where) {
            for (const [key, value] of Object.entries(where)) {
                whereClauses.push(`${quoteIdentifier(key)} = ?`);
                queryParams.push(value);
            }
        }

        if (whereClauses.length > 0) {
            query += ` WHERE ${whereClauses.join(' AND ')}`;
        }

        const [rows] = await pool.query(query, queryParams);
        res.json(rows); // Return array
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json([]); // Fail safe
        console.error(`Error _get ${table}:`, err);
        res.status(500).json({ error: err.message });
    }
});

// 2. POST /api/_delete
app.post('/api/_delete', async (req, res) => {
    const { table, where } = req.body;
    if (!table || !where) return res.status(400).json({ error: 'Table and where required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;
    if (!rejectInvalidFilterKeys(res, where)) return;
    // Guard against an empty `where` (would DELETE every row).
    if (Object.keys(where).length === 0) {
        return res.status(400).json({ error: 'where must contain at least one filter' });
    }

    try {
        let query = `DELETE FROM ${quoteIdentifier(table)}`;
        const queryParams = [];
        const whereClauses = [];

        for (const [key, value] of Object.entries(where)) {
            whereClauses.push(`${quoteIdentifier(key)} = ?`);
            queryParams.push(value);
        }

        query += ` WHERE ${whereClauses.join(' AND ')}`;
        await pool.query(query, queryParams);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 3. POST /api/_ensure_table
app.post('/api/_ensure_table', async (req, res) => {
    const { table } = req.body;
    if (!table) return res.status(400).json({ error: 'Table name required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;

    try {
        // Assumption: 'id' is standard primary key for this dynamic schema
        const query = `CREATE TABLE IF NOT EXISTS ${quoteIdentifier(table)} (id VARCHAR(255) PRIMARY KEY)`;
        await pool.query(query);
        res.json({ success: true, message: `Table ${table} ensured` });
    } catch (err) {
        console.error(`Error ensuring table ${table}:`, err);
        res.status(500).json({ error: err.message });
    }
});

// 4. POST /api/_ensure_column
// Allow-list of SQL column types accepted from the request body. A caller may
// only request one of these exact strings; anything else (including attempts to
// smuggle SQL via the `type` field) is rejected. Type inference from `value`
// always yields one of these by construction.
const ALLOWED_COLUMN_TYPES = new Set(['INT', 'DOUBLE', 'TINYINT(1)', 'TEXT', 'VARCHAR(50)', 'VARCHAR(100)', 'VARCHAR(191)', 'VARCHAR(255)']);

app.post('/api/_ensure_column', async (req, res) => {
    const { table, column, type, value } = req.body; // 'value' used to infer type if 'type' not provided
    if (!table || !column) return res.status(400).json({ error: 'Table and Column required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;
    if (!rejectInvalidIdentifier(res, column, 'column name')) return;
    if (type !== undefined && !ALLOWED_COLUMN_TYPES.has(type)) {
        return res.status(400).json({ error: `Invalid column type: ${JSON.stringify(type)}` });
    }

    try {
        // Check if column exists
        const [columns] = await pool.query(`SHOW COLUMNS FROM ${quoteIdentifier(table)} LIKE ?`, [column]);

        if (columns.length === 0) {
            let sqlType = 'TEXT'; // Default
            if (type) {
                sqlType = type; // Direct mapping if provided (validated against the allow-list above)
            } else if (value !== undefined) {
                sqlType = getSqlType(value);
            }

            const query = `ALTER TABLE ${quoteIdentifier(table)} ADD COLUMN ${quoteIdentifier(column)} ${sqlType}`;
            await pool.query(query);
            res.json({ success: true, message: `Column ${column} added to ${table}` });
        } else {
            res.json({ success: true, message: `Column ${column} already exists` });
        }
    } catch (err) {
        console.error(`Error ensuring column ${column} in ${table}:`, err);
        res.status(500).json({ error: err.message });
    }
});

// 5. POST /api/_upsert
app.post('/api/_upsert', async (req, res) => {
    const { table, data } = req.body;
    if (!table || !data) return res.status(400).json({ error: 'Table and data required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;
    if (!rejectInvalidFilterKeys(res, data)) return;

    try {
        // Use genericInsert to benefit from Smart Schema (auto-create table/columns)
        const id = await genericInsert(table, data);
        res.json({ success: true, id });
    } catch (err) {
        // Defense in depth: lib/schema also validates identifiers and maps to a 400.
        if (err.code === 'INVALID_IDENTIFIER') return res.status(400).json({ error: err.message });
        console.error(`Error awaiting upsert in ${table}:`, err);
        res.status(500).json({ error: err.message });
    }
});

// 6. GET /api/_read
app.get('/api/_read', async (req, res) => {
    const { table, limit, ...filters } = req.query;
    if (!table) return res.status(400).json({ error: 'Table name required' });
    if (!rejectInvalidIdentifier(res, table, 'table name')) return;
    if (!rejectInvalidFilterKeys(res, filters)) return;

    try {
        let query = `SELECT * FROM ${quoteIdentifier(table)}`;
        const queryParams = [];
        const whereClauses = [];

        // Simple equality filters
        for (const [key, value] of Object.entries(filters)) {
            whereClauses.push(`${quoteIdentifier(key)} = ?`);
            queryParams.push(value);
        }

        if (whereClauses.length > 0) {
            query += ` WHERE ${whereClauses.join(' AND ')}`;
        }

        if (limit !== undefined) {
            const parsedLimit = parseInt(limit, 10);
            if (!Number.isInteger(parsedLimit) || parsedLimit < 0) {
                return res.status(400).json({ error: `Invalid limit: ${JSON.stringify(limit)}` });
            }
            query += ` LIMIT ?`;
            queryParams.push(parsedLimit);
        }

        const [rows] = await pool.query(query, queryParams);
        res.json(rows);
    } catch (err) {
        if (err.code === 'ER_NO_SUCH_TABLE') return res.json([]); // Fail safe (consistent with /api/_get)
        console.error(`Error reading from ${table}:`, err);
        res.status(500).json({ error: err.message });
    }
});

// Internal Helper: Maintain schema dynamically
async function ensureSchema(table, data) {
    // Validate the table identifier once; reused (already quoted) below.
    const quotedTable = quoteIdentifier(table, 'table name');

    // 1. Ensure Table
    try {
        await pool.query(`CREATE TABLE IF NOT EXISTS ${quotedTable} (id VARCHAR(191) PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
    } catch (err) {
        console.error(`Auto-creation of table ${table} failed:`, err);
        throw err;
    }

    // 2. Ensure Columns
    const keys = Object.keys(data);
    if (keys.length === 0) return;

    // Get existing columns
    let existingColumns = [];
    try {
        const [cols] = await pool.query(`SHOW COLUMNS FROM ${quotedTable}`);
        existingColumns = cols.map(c => c.Field);
    } catch (err) {
        // Ignored
    }

    // columnsToAdd validates each column name; getSqlType yields a fixed set of
    // safe type strings, so the ALTER TABLE below contains no untrusted SQL.
    for (const { column, type } of columnsToAdd(existingColumns, data)) {
        try {
            await pool.query(`ALTER TABLE ${quotedTable} ADD COLUMN ${quoteIdentifier(column)} ${type}`);
            console.log(`✨ Dynamically added column '${column}' to '${table}'`);
        } catch (err) {
            // Ignore duplicate column errors (race conditions)
            if (err.code !== 'ER_DUP_FIELDNAME') throw err;
        }
    }
}

// Helper for generic insert/upsert with Auto-Schema
async function genericInsert(table, data) {
    if (!data.id) data.id = crypto.randomUUID();

    await ensureSchema(table, data); // <--- The magic "Firestore-like" part

    // Build the idempotent INSERT ... ON DUPLICATE KEY UPDATE (parameterized).
    const { sql, values } = buildUpsert(table, data);
    await pool.query(sql, values);
    return data.id;
}

// --- Server Startup (HTTPS Only) ---
const PORT = process.env.PORT || 4010;

console.log('--- Config Check ---');
console.log('PORT:', PORT);
console.log('DB_HOST:', process.env.DB_HOST);
console.log('DB_NAME:', process.env.DB_NAME);
console.log('--------------------');

async function startServer() {
    try {
        // 1. Initialize DB
        const connection = await mysql.createConnection({
            host: dbConfig.host,
            port: dbConfig.port,
            user: dbConfig.user,
            password: dbConfig.password
        });

        await connection.query(`CREATE DATABASE IF NOT EXISTS \`${dbConfig.database}\``);
        console.log(`✅ Database '${dbConfig.database}' ensured.`);
        await connection.end();

        pool = mysql.createPool({
            ...dbConfig,
            waitForConnections: true,
            connectionLimit: 10,
            queueLimit: 0
        });

        const poolConn = await pool.getConnection();
        console.log('✅ Pool connected to MariaDB successfully.');
        poolConn.release();

        // 3. Deduplicate Beacons (Self-Healing)
        console.log('🧹 Checking for duplicate beacons...');
        try {
            const [rows] = await pool.query('SELECT id, beacon_uid, last_seen FROM beacons');
            const deleteIds = findDuplicateBeaconIds(rows);

            if (deleteIds.length > 0) {
                console.log(`🧹 Removing ${deleteIds.length} duplicate beacons...`);
                await pool.query('DELETE FROM beacons WHERE id IN (?)', [deleteIds]);
                console.log('✅ Duplicates removed.');
            } else {
                console.log('✅ No duplicates found.');
            }

            // Now safe to add UNIQUE constraint
            try {
                await pool.query('ALTER TABLE beacons ADD UNIQUE INDEX idx_beacon_uid (beacon_uid)');
                console.log('🔒 Unique constraint added to beacons.beacon_uid');
            } catch (e) {
                if (e.code !== 'ER_DUP_KEYNAME') console.log('ℹ️  Index creation skipped: ' + e.message);
            }

        } catch (e) {
            console.error('⚠️ Cleanup failed:', e);
        }


        // 4. Auto-Fix Charset for existing tables (Robust)
        const tablesToFix = ['beacon_logs', 'beacons', 'commands'];

        for (const table of tablesToFix) {
            try {
                // Resize ID to 191 to prevent "Data too long" for indexes in utf8mb4
                // We attempt to resize 'id' and 'beacon_uid' just in case.
                try { await pool.query(`ALTER TABLE \`${table}\` MODIFY \`id\` VARCHAR(191)`); } catch (e) { }
                if (table === 'beacons') {
                    try { await pool.query(`ALTER TABLE \`${table}\` MODIFY \`beacon_uid\` VARCHAR(191)`); } catch (e) { }
                }

                // Convert Charset
                await pool.query(`ALTER TABLE \`${table}\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
                console.log(`✨ Table '${table}' check/upgraded to UTF8MB4`);
            } catch (e) {
                console.log(`⚠️ Charset upgrade skipped for '${table}': ` + e.message);
            }
        }

        // 5. Ensure Circuit State Schema (Weather Fields)
        try {
            console.log('🔧 Ensuring circuit_state schema...');
            await pool.query(`CREATE TABLE IF NOT EXISTS circuit_state (id VARCHAR(191) PRIMARY KEY)`);
            try { await pool.query(`ALTER TABLE circuit_state ADD COLUMN temperature VARCHAR(50)`); } catch (e) { }
            try { await pool.query(`ALTER TABLE circuit_state ADD COLUMN humidity VARCHAR(50)`); } catch (e) { }
            try { await pool.query(`ALTER TABLE circuit_state ADD COLUMN wind VARCHAR(50)`); } catch (e) { }
            try { await pool.query(`ALTER TABLE circuit_state ADD COLUMN forecast VARCHAR(100)`); } catch (e) { }
            console.log('✅ circuit_state schema ensured.');
        } catch (e) {
            console.error('⚠️ Failed to ensure circuit_state schema:', e);
        }

        try {
            await pool.query('ALTER DATABASE `' + dbConfig.database + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
        } catch (e) { }

        // 6. Start HTTPS Server
        const privateKey = fs.readFileSync(SSL_KEY_PATH, 'utf8');
        const certificate = fs.readFileSync(SSL_CERT_PATH, 'utf8');
        const ca = fs.readFileSync(SSL_CA_PATH, 'utf8');
        const credentials = { key: privateKey, cert: certificate, ca: ca };

        const httpsServer = https.createServer(credentials, app);

        httpsServer.listen(PORT, () => {
            console.log(`🚀 GeoRacing API running on HTTPS port ${PORT}`);
        });

        httpsServer.on('error', (e) => {
            if (e.code === 'EACCES') {
                console.error(`\n❌ ERROR: Permiso denegado en el puerto ${PORT}.`);
                process.exit(1);
            } else {
                console.error('❌ Server Error:', e);
            }
        });

    } catch (err) {
        console.error('❌ Critical Error: Failed to initialize/start server.');
        console.error(err);
        process.exit(1);
    }
}

// --- Weather Service (Open-Meteo) ---
// Weather code descriptions and wind-bearing formatting live in ./lib/weather.

async function fetchCircuitWeather() {
    try {
        // Montmeló Circuit Coordinates + Extra Params
        const url = 'https://api.open-meteo.com/v1/forecast?latitude=41.57&longitude=2.26&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m';

        https.get(url, (resp) => {
            let data = '';
            resp.on('data', (chunk) => data += chunk);
            resp.on('end', async () => {
                try {
                    const weather = JSON.parse(data);
                    if (weather.current) {
                        const w = weather.current;
                        const u = weather.current_units;

                        const temp = (w.temperature_2m || '--') + (u.temperature_2m || '°C');
                        const humidity = (w.relative_humidity_2m || '--') + (u.relative_humidity_2m || '%');
                        const windDir = getCardinalDirection(w.wind_direction_10m || 0);
                        const wind = `${w.wind_speed_10m} ${u.wind_speed_10m} ${windDir}`;
                        const forecast = describeWeatherCode(w.weather_code);

                        console.log(`🌤 Weather: ${temp}, ${humidity}, ${wind}, ${forecast}`);

                        // Update DB with auto-schema evolution
                        const updateQuery = `
                            UPDATE circuit_state SET 
                            temperature = ?, 
                            humidity = ?, 
                            wind = ?, 
                            forecast = ?
                        `;

                        try {
                            await pool.query(updateQuery, [temp, humidity, wind, forecast]);
                        } catch (err) {
                            if (err.code === 'ER_BAD_FIELD_ERROR') {
                                console.log('🔧 Evolving Schema: Adding weather columns...');
                                try { await pool.query('ALTER TABLE circuit_state ADD COLUMN temperature VARCHAR(50)'); } catch (e) { }
                                try { await pool.query('ALTER TABLE circuit_state ADD COLUMN humidity VARCHAR(50)'); } catch (e) { }
                                try { await pool.query('ALTER TABLE circuit_state ADD COLUMN wind VARCHAR(50)'); } catch (e) { }
                                try { await pool.query('ALTER TABLE circuit_state ADD COLUMN forecast VARCHAR(100)'); } catch (e) { }

                                // Retry update
                                await pool.query(updateQuery, [temp, humidity, wind, forecast]);
                            } else {
                                throw err; // Rethrow other errors
                            }
                        }
                    }
                } catch (e) {
                    console.error('Error parsing weather:', e.message);
                }
            });
        }).on('error', (err) => {
            console.error('Error fetching weather:', err.message);
        });
    } catch (err) {
        console.error('Weather service error:', err);
    }
}

// Start Weather Loop (every 15 minutes)
setInterval(fetchCircuitWeather, 15 * 60 * 1000);
// Initial fetch after 5 seconds
setTimeout(fetchCircuitWeather, 5000);

startServer();
