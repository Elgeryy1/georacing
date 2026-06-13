import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { isBeaconOnline, getBeaconStats } from "./beaconUtils";
import type { Beacon } from "../types";

function beacon(overrides: Partial<Beacon>): Beacon {
  return {
    beaconId: "B1",
    name: null,
    mode: "NORMAL",
    arrow: "NONE",
    message: null,
    color: null,
    brightness: null,
    battery: null,
    online: true,
    configured: true,
    ...overrides,
  };
}

describe("isBeaconOnline", () => {
  const NOW = new Date("2026-06-13T12:00:00Z");

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it("is online when the flag is set and the heartbeat is fresh (<15s)", () => {
    const fresh = new Date(NOW.getTime() - 5000).toISOString();
    expect(isBeaconOnline(beacon({ online: true, lastSeen: fresh }))).toBe(true);
  });

  it("is offline when the heartbeat is stale (>=15s) even if flagged online", () => {
    const stale = new Date(NOW.getTime() - 20000).toISOString();
    expect(isBeaconOnline(beacon({ online: true, lastSeen: stale }))).toBe(false);
  });

  it("is offline when the online flag is false", () => {
    const fresh = new Date(NOW.getTime() - 1000).toISOString();
    expect(isBeaconOnline(beacon({ online: false, lastSeen: fresh }))).toBe(false);
  });

  it("is offline when there is no lastSeen timestamp", () => {
    expect(isBeaconOnline(beacon({ online: true, lastSeen: null }))).toBe(false);
  });
});

describe("getBeaconStats", () => {
  const NOW = new Date("2026-06-13T12:00:00Z");
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it("aggregates totals, online/offline split, and uptime percentage", () => {
    const fresh = new Date(NOW.getTime() - 2000).toISOString();
    const stale = new Date(NOW.getTime() - 60000).toISOString();
    const stats = getBeaconStats([
      beacon({ beaconId: "A", online: true, lastSeen: fresh, configured: true, mode: "NORMAL" }),
      beacon({ beaconId: "B", online: true, lastSeen: fresh, configured: true, mode: "EVACUATION" }),
      beacon({ beaconId: "C", online: true, lastSeen: stale, configured: false, mode: "UNCONFIGURED" }),
      beacon({ beaconId: "D", online: false, lastSeen: fresh, configured: true, mode: "NORMAL" }),
    ]);

    expect(stats.total).toBe(4);
    expect(stats.online).toBe(2); // A and B are fresh+flagged
    expect(stats.offline).toBe(2);
    expect(stats.configured).toBe(3);
    expect(stats.unconfigured).toBe(1);
    expect(stats.emergency).toBe(1); // B is in EVACUATION
    expect(stats.uptime).toBe(50);
  });

  it("reports 0% uptime for an empty fleet without dividing by zero", () => {
    const stats = getBeaconStats([]);
    expect(stats.total).toBe(0);
    expect(stats.uptime).toBe(0);
  });
});
