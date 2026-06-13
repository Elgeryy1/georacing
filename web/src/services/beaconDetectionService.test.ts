import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { beaconDetectionService } from "./beaconDetectionService";
import { api } from "./apiClient";

describe("beaconDetectionService.subscribeToNewBeacons", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("normalizes the raw beacon_uid field into beaconId", async () => {
    vi.spyOn(api, "get").mockResolvedValue([
      { beacon_uid: "ESP32-AA", last_heartbeat: "2026-06-13T10:00:00Z" } as any,
    ]);
    const seen: string[] = [];
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons((news) =>
      news.forEach((b) => seen.push(b.beaconId))
    );

    await vi.advanceTimersByTimeAsync(4000);
    unsubscribe();

    expect(seen).toEqual(["ESP32-AA"]);
  });

  it("falls back to beaconId when beacon_uid is absent", async () => {
    vi.spyOn(api, "get").mockResolvedValue([{ beaconId: "LEGACY-1" } as any]);
    const seen: string[] = [];
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons((news) =>
      news.forEach((b) => seen.push(b.beaconId))
    );

    await vi.advanceTimersByTimeAsync(4000);
    unsubscribe();

    expect(seen).toEqual(["LEGACY-1"]);
  });

  it("does not re-notify a beacon that was already reported", async () => {
    vi.spyOn(api, "get").mockResolvedValue([{ beacon_uid: "ESP32-BB" } as any]);
    const batches: number[] = [];
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons((news) =>
      batches.push(news.length)
    );

    await vi.advanceTimersByTimeAsync(4000); // first poll: new beacon
    await vi.advanceTimersByTimeAsync(4000); // second poll: same beacon, deduped
    unsubscribe();

    // Only the first poll fires the callback; the second is suppressed.
    expect(batches).toEqual([1]);
  });

  it("ignores entries without any usable identifier", async () => {
    vi.spyOn(api, "get").mockResolvedValue([
      { name: "no id here" } as any,
      { beacon_uid: "ESP32-CC" } as any,
    ]);
    const seen: string[] = [];
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons((news) =>
      news.forEach((b) => seen.push(b.beaconId))
    );

    await vi.advanceTimersByTimeAsync(4000);
    unsubscribe();

    expect(seen).toEqual(["ESP32-CC"]);
  });

  it("swallows polling errors without throwing", async () => {
    vi.spyOn(api, "get").mockRejectedValue(new Error("boom"));
    const cb = vi.fn();
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons(cb);

    // A rejected poll must not surface as an unhandled rejection.
    await expect(vi.advanceTimersByTimeAsync(4000)).resolves.not.toThrow();
    unsubscribe();

    expect(cb).not.toHaveBeenCalled();
  });
});

describe("beaconDetectionService.subscribeAndDetectNew", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("does not emit beacons already present in the seed set", async () => {
    vi.spyOn(api, "get").mockResolvedValue([
      { beacon_uid: "KNOWN-1" } as any,
      { beacon_uid: "NEW-2" } as any,
    ]);
    const emitted: string[] = [];
    const unsubscribe = beaconDetectionService.subscribeAndDetectNew(
      new Set(["KNOWN-1"]),
      (id) => emitted.push(id)
    );

    await vi.advanceTimersByTimeAsync(4000);
    unsubscribe();

    expect(emitted).toEqual(["NEW-2"]);
  });
});
