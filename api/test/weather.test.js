'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { getCardinalDirection, describeWeatherCode } = require('../lib/weather');

test('getCardinalDirection maps the principal bearings', () => {
    assert.equal(getCardinalDirection(0), 'N');
    assert.equal(getCardinalDirection(45), 'NE');
    assert.equal(getCardinalDirection(90), 'E');
    assert.equal(getCardinalDirection(135), 'SE');
    assert.equal(getCardinalDirection(180), 'S');
    assert.equal(getCardinalDirection(225), 'SW');
    assert.equal(getCardinalDirection(270), 'W');
    assert.equal(getCardinalDirection(315), 'NW');
});

test('getCardinalDirection rounds to the nearest sector and wraps around', () => {
    assert.equal(getCardinalDirection(22), 'N');   // rounds down toward N
    assert.equal(getCardinalDirection(23), 'NE');  // rounds up toward NE
    assert.equal(getCardinalDirection(360), 'N');  // full rotation wraps to N
    assert.equal(getCardinalDirection(-45), 'NW'); // negative bearing normalizes
});

test('describeWeatherCode returns the mapped description', () => {
    assert.equal(describeWeatherCode(0), 'Cielo despejado');
    assert.equal(describeWeatherCode(61), 'Lluvia leve');
    assert.equal(describeWeatherCode(95), 'Tormenta');
});

test('describeWeatherCode falls back to "Desconocido" for unknown codes', () => {
    assert.equal(describeWeatherCode(1234), 'Desconocido');
    assert.equal(describeWeatherCode(undefined), 'Desconocido');
});
