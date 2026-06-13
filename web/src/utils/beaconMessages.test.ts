import { describe, it, expect } from "vitest";
import { getDefaultBeaconMessage } from "./beaconMessages";
import { VALID_LANGUAGES } from "./beaconValidation";
import type { ArrowDirection, BeaconMode, Language } from "../types";

const ALL_ARROWS: ArrowDirection[] = [
  "NONE", "LEFT", "RIGHT", "UP", "DOWN",
  "UP_LEFT", "UP_RIGHT", "DOWN_LEFT", "DOWN_RIGHT",
  "FORWARD", "BACKWARD", "FORWARD_LEFT", "FORWARD_RIGHT",
  "BACKWARD_LEFT", "BACKWARD_RIGHT"
];

const ALL_MODES: BeaconMode[] = [
  "UNCONFIGURED", "NORMAL", "CONGESTION", "EMERGENCY", "EVACUATION", "MAINTENANCE"
];

describe("getDefaultBeaconMessage", () => {
  it("resolves a non-empty message for every NORMAL-mode arrow in every language", () => {
    for (const arrow of ALL_ARROWS) {
      for (const lang of VALID_LANGUAGES) {
        const msg = getDefaultBeaconMessage("NORMAL", lang, arrow);
        expect(msg, `arrow=${arrow} lang=${lang}`).toBeTypeOf("string");
        expect(msg.length, `arrow=${arrow} lang=${lang}`).toBeGreaterThan(0);
      }
    }
  });

  it("resolves a non-empty message for every non-NORMAL mode in every language", () => {
    for (const mode of ALL_MODES) {
      for (const lang of VALID_LANGUAGES) {
        const msg = getDefaultBeaconMessage(mode, lang);
        expect(msg, `mode=${mode} lang=${lang}`).toBeTypeOf("string");
        expect(msg.length, `mode=${mode} lang=${lang}`).toBeGreaterThan(0);
      }
    }
  });

  it("returns direction-specific copy for NORMAL mode (LEFT vs RIGHT differ)", () => {
    expect(getDefaultBeaconMessage("NORMAL", "EN", "LEFT")).toBe("Turn Left");
    expect(getDefaultBeaconMessage("NORMAL", "EN", "RIGHT")).toBe("Turn Right");
    expect(getDefaultBeaconMessage("NORMAL", "ES", "LEFT")).toBe("Gire a la Izquierda");
  });

  it("treats FORWARD as an alias of UP and BACKWARD as an alias of DOWN", () => {
    expect(getDefaultBeaconMessage("NORMAL", "EN", "FORWARD"))
      .toBe(getDefaultBeaconMessage("NORMAL", "EN", "UP"));
    expect(getDefaultBeaconMessage("NORMAL", "ES", "BACKWARD"))
      .toBe(getDefaultBeaconMessage("NORMAL", "ES", "DOWN"));
  });

  it("defaults the arrow to NONE when omitted", () => {
    expect(getDefaultBeaconMessage("NORMAL", "EN")).toBe("Normal Traffic");
  });

  it("falls back to a defined language when an unknown language is requested for a fixed-mode message", () => {
    // 'JP' is not in our catalogue: should fall back to the Spanish entry, never undefined/empty.
    const msg = getDefaultBeaconMessage("EMERGENCY", "JP" as unknown as Language);
    expect(msg).toBe("⚠️ EMERGENCIA\nPRECAUCIÓN");
  });

  it("uses the EVACUATION copy that instructs following the arrows", () => {
    expect(getDefaultBeaconMessage("EVACUATION", "EN")).toContain("Follow the Arrows");
    expect(getDefaultBeaconMessage("EVACUATION", "ES")).toContain("Siga las Flechas");
  });
});
