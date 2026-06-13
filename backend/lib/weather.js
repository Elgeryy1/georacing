'use strict';

/**
 * Weather formatting helpers for the Open-Meteo integration.
 *
 * The server polls Open-Meteo for the circuit's current conditions and stores a
 * human-readable summary in `circuit_state`. These pure functions translate the
 * raw API fields (WMO weather codes, wind bearing) into the display strings,
 * isolated here so the mapping can be unit-tested without a network call.
 */

/**
 * WMO weather interpretation codes mapped to Spanish descriptions, matching the
 * strings shown in the operator panel.
 * @see https://open-meteo.com/en/docs (WMO Weather interpretation codes)
 */
const WEATHER_CODES = {
    0: 'Cielo despejado', 1: 'Mayormente despejado', 2: 'Parcialmente nublado', 3: 'Nublado',
    45: 'Niebla', 48: 'Niebla escarchada',
    51: 'Llovizna ligera', 53: 'Llovizna moderada', 55: 'Llovizna densa',
    61: 'Lluvia leve', 63: 'Lluvia moderada', 65: 'Lluvia fuerte',
    71: 'Nieve leve', 73: 'Nieve moderada', 75: 'Nieve fuerte',
    95: 'Tormenta', 96: 'Tormenta con granizo', 99: 'Tormenta fuerte'
};

const CARDINAL_DIRECTIONS = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];

/**
 * Convert a wind bearing in degrees to one of eight cardinal directions.
 * Angles are normalized so values outside [0, 360) and full rotations map
 * correctly (e.g. 360 -> 'N', -45 -> 'NW').
 *
 * @param {number} angle - Wind direction in degrees (meteorological bearing).
 * @returns {string} One of N, NE, E, SE, S, SW, W, NW.
 */
function getCardinalDirection(angle) {
    const idx = ((Math.round(Number(angle) / 45) % 8) + 8) % 8;
    return CARDINAL_DIRECTIONS[idx];
}

/**
 * Translate a WMO weather code into its description, falling back to
 * 'Desconocido' for unknown codes.
 *
 * @param {number} code - WMO weather interpretation code.
 * @returns {string}
 */
function describeWeatherCode(code) {
    return WEATHER_CODES[code] || 'Desconocido';
}

module.exports = { WEATHER_CODES, CARDINAL_DIRECTIONS, getCardinalDirection, describeWeatherCode };
