// ============================================================================
// Pure helpers for GeoOps.
//
// Everything in this module is deterministic and free of side effects: no
// Discord client, no Supabase, no network. That makes the bot's trickiest
// logic (search-term extraction, status aggregation, input validation) unit
// testable in isolation — see ../test/helpers.test.js.
// ============================================================================

/**
 * Build a PostgreSQL full-text `to_tsquery`-style OR expression from a free-form
 * user question.
 *
 * Punctuation is stripped, the query is tokenised on whitespace, short stop-word-
 * like tokens (<= 3 chars) are dropped, and the survivors are joined with ` | `
 * (logical OR) so Supabase's `.textSearch()` matches documents containing ANY of
 * the significant terms.
 *
 * @param {string} query Raw user input.
 * @param {number} [minLength=4] Minimum token length to keep (exclusive boundary
 *   is `> minLength - 1`, i.e. tokens must be at least `minLength` chars).
 * @returns {string} An ` | `-joined search expression, or `''` if nothing
 *   significant remains.
 */
export function buildSearchTerms(query, minLength = 4) {
  if (typeof query !== 'string') return '';
  return query
    .replace(/[^\w\sñÑáéíóúÁÉÍÓÚ]/gi, ' ')
    .split(/\s+/)
    .map((w) => w.trim())
    .filter((w) => w.length >= minLength)
    .join(' | ');
}

/**
 * Extract candidate keywords (words of >= `minLength` letters, accents included)
 * from a query. Used as the ILIKE fallback when full-text search returns nothing.
 *
 * @param {string} query Raw user input.
 * @param {number} [minLength=4] Minimum keyword length.
 * @returns {string[]} De-duplicated keywords preserving first-seen order.
 */
export function extractKeywords(query, minLength = 4) {
  if (typeof query !== 'string') return [];
  const pattern = new RegExp(`\\b[A-Za-zñÑáéíóúÁÉÍÓÚ]{${minLength},}\\b`, 'g');
  const matches = query.match(pattern) || [];
  // De-duplicate (case-insensitively) while preserving order.
  const seen = new Set();
  const out = [];
  for (const word of matches) {
    const key = word.toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      out.push(word);
    }
  }
  return out;
}

// Recognised `ideas.status` values mapped to their report buckets.
const STATUS_BUCKETS = {
  Acabado: 'done',
  'en proceso': 'inProgress',
  'por empezar': 'pending',
};

/**
 * Aggregate a list of `ideas` rows into status counts for the `/estado` report.
 * Unknown statuses are tallied under `other` rather than silently dropped, and
 * `total` always reflects the number of rows received.
 *
 * @param {Array<{status?: string}>} rows Rows from `ideas`.
 * @returns {{done:number,inProgress:number,pending:number,other:number,total:number}}
 */
export function summarizeStatuses(rows) {
  const summary = { done: 0, inProgress: 0, pending: 0, other: 0, total: 0 };
  if (!Array.isArray(rows)) return summary;

  for (const row of rows) {
    summary.total += 1;
    const bucket = STATUS_BUCKETS[row?.status];
    if (bucket) summary[bucket] += 1;
    else summary.other += 1;
  }
  return summary;
}

// Extensions accepted by `/aprender`.
const ALLOWED_DOC_EXTENSIONS = ['txt', 'md', 'markdown'];

/**
 * Validate a documentation filename for `/aprender`.
 *
 * @param {string} filename Attachment filename (e.g. `"runbook.md"`).
 * @returns {boolean} `true` if the extension is one we ingest.
 */
export function isAllowedDocExtension(filename) {
  if (typeof filename !== 'string' || !filename.includes('.')) return false;
  const ext = filename.split('.').pop().toLowerCase();
  return ALLOWED_DOC_EXTENSIONS.includes(ext);
}

/**
 * Validate the `/purgar` message count. Discord's `bulkDelete` only accepts
 * 1-100 messages in a single call, so we enforce that range up front.
 *
 * @param {number} amount Requested message count.
 * @param {number} [min=1] Lower bound (inclusive).
 * @param {number} [max=100] Upper bound (inclusive).
 * @returns {boolean} `true` if `amount` is an integer within `[min, max]`.
 */
export function isValidPurgeAmount(amount, min = 1, max = 100) {
  return Number.isInteger(amount) && amount >= min && amount <= max;
}

// Anchored sentinel prefix that retrieval prepends when the lookup itself
// failed (see `fetchContext`'s catch block in ../index.js). Anchoring on a
// `ERROR:` *prefix* — rather than the bare substring `ERROR` — means a genuine
// knowledge-base document that merely mentions the word "ERROR" still
// classifies as a successful hit instead of a failed lookup.
export const CONTEXT_ERROR_SENTINEL = 'ERROR:';

/**
 * Classify the knowledge-base retrieval result for the `/duda` footer.
 *
 * @param {string} context The context string returned by retrieval. An empty
 *   string means "no matches"; a string *starting with* `ERROR:` means the
 *   lookup failed; anything else is a successful hit.
 * @returns {{ key: 'found'|'none'|'error', label: string }}
 */
export function classifyContext(context) {
  if (typeof context === 'string' && context.startsWith(CONTEXT_ERROR_SENTINEL)) {
    return { key: 'error', label: '❌ Error' };
  }
  if (context) {
    return { key: 'found', label: '✅ Found' };
  }
  return { key: 'none', label: '⚠️ No Matches' };
}

/**
 * Escape PostgreSQL `LIKE`/`ILIKE` wildcards (`%`, `_`) and the escape
 * character itself (`\`) in a search term so they are matched literally.
 *
 * Supabase already binds the value as a parameter (so this is NOT about SQL
 * injection); this is defense-in-depth so that user-derived keywords carrying
 * `%` or `_` can never widen an `ILIKE 'content', '%word%'` fallback into an
 * unintended pattern match.
 *
 * @param {string} term Raw term to embed inside a `%...%` pattern.
 * @returns {string} The term with `\`, `%` and `_` backslash-escaped.
 */
export function escapeLikePattern(term) {
  if (typeof term !== 'string') return '';
  return term.replace(/[\\%_]/g, (ch) => `\\${ch}`);
}

export const __testables = {
  STATUS_BUCKETS,
  ALLOWED_DOC_EXTENSIONS,
};
