import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  buildSearchTerms,
  extractKeywords,
  summarizeStatuses,
  isAllowedDocExtension,
  isValidPurgeAmount,
  classifyContext,
  escapeLikePattern,
} from '../lib/helpers.js';

// ---------------------------------------------------------------------------
// buildSearchTerms
// ---------------------------------------------------------------------------
test('buildSearchTerms joins significant tokens with OR and drops short words', () => {
  // "como", "se" are <4 chars and dropped; "configura", "batería" survive.
  assert.equal(
    buildSearchTerms('¿Como se configura la batería?'),
    'Como | configura | batería'
  );
});

test('buildSearchTerms strips punctuation but preserves Spanish accents', () => {
  assert.equal(buildSearchTerms('telemetría, GPS!!!'), 'telemetría');
});

test('buildSearchTerms returns empty string when nothing is significant', () => {
  assert.equal(buildSearchTerms('a la de ok'), '');
  assert.equal(buildSearchTerms(''), '');
  assert.equal(buildSearchTerms(null), '');
});

// ---------------------------------------------------------------------------
// extractKeywords
// ---------------------------------------------------------------------------
test('extractKeywords returns 4+ letter words and de-duplicates case-insensitively', () => {
  assert.deepEqual(
    extractKeywords('GeoRacing usa GPS y la batería georacing'),
    ['GeoRacing', 'batería']
  );
});

test('extractKeywords ignores numbers and short tokens, returns [] for non-strings', () => {
  assert.deepEqual(extractKeywords('v2 id 42 ok go'), []);
  assert.deepEqual(extractKeywords(undefined), []);
});

// ---------------------------------------------------------------------------
// summarizeStatuses
// ---------------------------------------------------------------------------
test('summarizeStatuses buckets known statuses and counts the total', () => {
  const rows = [
    { status: 'Acabado' },
    { status: 'Acabado' },
    { status: 'en proceso' },
    { status: 'por empezar' },
  ];
  assert.deepEqual(summarizeStatuses(rows), {
    done: 2,
    inProgress: 1,
    pending: 1,
    other: 0,
    total: 4,
  });
});

test('summarizeStatuses tallies unknown/missing statuses under other', () => {
  const rows = [{ status: 'cancelado' }, {}, { status: 'Acabado' }];
  const summary = summarizeStatuses(rows);
  assert.equal(summary.other, 2);
  assert.equal(summary.done, 1);
  assert.equal(summary.total, 3);
});

test('summarizeStatuses tolerates non-array input', () => {
  assert.deepEqual(summarizeStatuses(null), {
    done: 0,
    inProgress: 0,
    pending: 0,
    other: 0,
    total: 0,
  });
});

// ---------------------------------------------------------------------------
// isAllowedDocExtension
// ---------------------------------------------------------------------------
test('isAllowedDocExtension accepts txt/md/markdown case-insensitively', () => {
  assert.equal(isAllowedDocExtension('runbook.md'), true);
  assert.equal(isAllowedDocExtension('NOTES.TXT'), true);
  assert.equal(isAllowedDocExtension('readme.MARKDOWN'), true);
});

test('isAllowedDocExtension rejects other extensions and malformed names', () => {
  assert.equal(isAllowedDocExtension('payload.pdf'), false);
  assert.equal(isAllowedDocExtension('archive.tar.gz'), false);
  assert.equal(isAllowedDocExtension('noextension'), false);
  assert.equal(isAllowedDocExtension(''), false);
  assert.equal(isAllowedDocExtension(null), false);
});

// ---------------------------------------------------------------------------
// isValidPurgeAmount
// ---------------------------------------------------------------------------
test('isValidPurgeAmount enforces the Discord 1-100 bulkDelete range', () => {
  assert.equal(isValidPurgeAmount(1), true);
  assert.equal(isValidPurgeAmount(100), true);
  assert.equal(isValidPurgeAmount(50), true);
  assert.equal(isValidPurgeAmount(0), false);
  assert.equal(isValidPurgeAmount(101), false);
  assert.equal(isValidPurgeAmount(5.5), false);
  assert.equal(isValidPurgeAmount(NaN), false);
});

// ---------------------------------------------------------------------------
// classifyContext
// ---------------------------------------------------------------------------
test('classifyContext distinguishes found / none / error', () => {
  assert.equal(classifyContext('some retrieved docs').key, 'found');
  assert.equal(classifyContext('').key, 'none');
  assert.equal(classifyContext('ERROR: Database connection failed.').key, 'error');
});

test('classifyContext returns a human label alongside the key', () => {
  assert.match(classifyContext('docs').label, /Found/);
  assert.match(classifyContext('').label, /No Matches/);
  assert.match(classifyContext('ERROR: Database connection failed.').label, /Error/);
});

test('classifyContext treats a KB doc that merely mentions ERROR as found', () => {
  // Regression: a real document whose body discusses the word ERROR (e.g. a
  // troubleshooting runbook) must NOT be mislabelled as a failed lookup. Only
  // the anchored `ERROR:` sentinel prefix means "retrieval failed".
  assert.equal(
    classifyContext('Si aparece un ERROR de batería, revisa el conector.').key,
    'found'
  );
  assert.equal(
    classifyContext('Troubleshooting: common ERROR codes and their meaning.').key,
    'found'
  );
  // The anchored sentinel still classifies as error.
  assert.equal(classifyContext('ERROR: Database connection failed.').key, 'error');
  // A bare "ERROR" with no colon prefix is treated as legitimate content.
  assert.equal(classifyContext('ERROR').key, 'found');
});

test('classifyContext tolerates non-string input', () => {
  assert.equal(classifyContext(null).key, 'none');
  assert.equal(classifyContext(undefined).key, 'none');
});

// ---------------------------------------------------------------------------
// escapeLikePattern
// ---------------------------------------------------------------------------
test('escapeLikePattern escapes LIKE wildcards and the escape char', () => {
  assert.equal(escapeLikePattern('100%'), '100\\%');
  assert.equal(escapeLikePattern('a_b'), 'a\\_b');
  assert.equal(escapeLikePattern('a\\b'), 'a\\\\b');
  // Combined: backslash, percent and underscore in one term.
  assert.equal(escapeLikePattern('%_\\'), '\\%\\_\\\\');
});

test('escapeLikePattern leaves ordinary keywords untouched', () => {
  // The keywords extractKeywords produces today carry none of these chars.
  assert.equal(escapeLikePattern('GeoRacing'), 'GeoRacing');
  assert.equal(escapeLikePattern('batería'), 'batería');
  assert.equal(escapeLikePattern(''), '');
  assert.equal(escapeLikePattern(null), '');
});
