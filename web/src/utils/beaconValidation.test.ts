import { describe, it, expect } from "vitest";
import {
  isValidHexColor,
  isValidBrightness,
  isValidMessage,
  isValidZone,
  validateBeaconConfig,
  normalizeColor,
  normalizeMessage,
  parseTags,
  stringifyTags,
} from "./beaconValidation";

describe("beaconValidation primitives", () => {
  it("accepts only 6-digit #RRGGBB hex colours", () => {
    expect(isValidHexColor("#00FFAA")).toBe(true);
    expect(isValidHexColor("#abc123")).toBe(true);
    expect(isValidHexColor("#FFF")).toBe(false); // 3-digit shorthand rejected
    expect(isValidHexColor("00FFAA")).toBe(false); // missing hash
    expect(isValidHexColor("#GG0000")).toBe(false); // non-hex chars
  });

  it("bounds brightness to 0..100 inclusive", () => {
    expect(isValidBrightness(0)).toBe(true);
    expect(isValidBrightness(100)).toBe(true);
    expect(isValidBrightness(-1)).toBe(false);
    expect(isValidBrightness(101)).toBe(false);
  });

  it("rejects messages longer than 255 chars", () => {
    expect(isValidMessage("x".repeat(255))).toBe(true);
    expect(isValidMessage("x".repeat(256))).toBe(false);
  });

  it("requires a non-empty zone of at most 50 chars", () => {
    expect(isValidZone("PADDOCK")).toBe(true);
    expect(isValidZone("   ")).toBe(false);
    expect(isValidZone("z".repeat(51))).toBe(false);
  });
});

describe("validateBeaconConfig", () => {
  it("returns no errors for a fully valid config", () => {
    const errors = validateBeaconConfig({
      mode: "NORMAL",
      arrow: "RIGHT",
      message: "Acceso Principal",
      color: "#00FFAA",
      brightness: 90,
      language: "ES",
      zone: "GRADA-G",
    });
    expect(errors).toEqual([]);
  });

  it("flags an invalid colour and out-of-range brightness", () => {
    const errors = validateBeaconConfig({ color: "red", brightness: 250 });
    const fields = errors.map((e) => e.field);
    expect(fields).toContain("color");
    expect(fields).toContain("brightness");
  });

  it("requires an evacuation exit when mode is EVACUATION", () => {
    const errors = validateBeaconConfig({ mode: "EVACUATION" });
    expect(errors.some((e) => e.field === "evacuationExit")).toBe(true);
  });

  it("accepts EVACUATION mode once an exit is provided", () => {
    const errors = validateBeaconConfig({ mode: "EVACUATION", evacuationExit: "SALIDA 3" });
    expect(errors.some((e) => e.field === "evacuationExit")).toBe(false);
  });
});

describe("normalizers and tag (de)serialization", () => {
  it("uppercases colours and collapses whitespace in messages", () => {
    expect(normalizeColor("#00ffaa")).toBe("#00FFAA");
    expect(normalizeMessage("  hola   mundo \n ")).toBe("hola mundo");
  });

  it("parseTags is the inverse of stringifyTags for arrays", () => {
    const tags = ["test", "vip", "grada"];
    expect(parseTags(stringifyTags(tags))).toEqual(tags);
  });

  it("parseTags degrades gracefully on null and malformed JSON", () => {
    expect(parseTags(null)).toEqual([]);
    expect(parseTags("{not json")).toEqual([]);
    expect(parseTags('{"a":1}')).toEqual([]); // object, not array
  });
});
