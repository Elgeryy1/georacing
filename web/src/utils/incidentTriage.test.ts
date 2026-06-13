import { describe, it, expect } from "vitest";
import {
    severityRank,
    haversineMeters,
    dedupeIncidents,
    prioritizeIncidents,
    triageIncidents,
    type Incident,
} from "./incidentTriage";

describe("severityRank", () => {
    it("orders CRITICAL > WARNING > INFO and is case-insensitive", () => {
        expect(severityRank("CRITICAL")).toBeGreaterThan(severityRank("WARNING"));
        expect(severityRank("WARNING")).toBeGreaterThan(severityRank("INFO"));
        expect(severityRank("critical")).toBe(severityRank("CRITICAL"));
        expect(severityRank("nope")).toBe(0);
        expect(severityRank(null)).toBe(0);
    });
});

describe("haversineMeters", () => {
    it("approximates a ~111 m north step and rejects missing coords", () => {
        const d = haversineMeters(41.57, 2.26, 41.571, 2.26);
        expect(d).toBeGreaterThan(105);
        expect(d).toBeLessThan(117);
        expect(haversineMeters(41.57, 2.26, 41.57, null)).toBe(Infinity);
    });
});

describe("dedupeIncidents", () => {
    it("collapses same-category reports within radius and time, keeping the worst", () => {
        const list: Incident[] = [
            { id: 1, category: "medical", level: "WARNING", lat: 41.57, lon: 2.26, created_at: "2026-06-13T10:00:00Z" },
            { id: 2, category: "medical", level: "CRITICAL", lat: 41.57, lon: 2.2601, created_at: "2026-06-13T10:01:00Z" },
            { id: 3, category: "medical", level: "INFO", lat: 41.57, lon: 2.26, created_at: "2026-06-13T10:02:00Z" },
        ];
        const out = dedupeIncidents(list);
        expect(out).toHaveLength(1);
        expect(out[0].level).toBe("CRITICAL");
        expect(out[0].id).toBe(2);
        expect(out[0].duplicate_count).toBe(3);
    });

    it("keeps different categories and far-apart reports", () => {
        const list: Incident[] = [
            { id: 1, category: "medical", level: "WARNING", lat: 41.57, lon: 2.26, created_at: "2026-06-13T10:00:00Z" },
            { id: 2, category: "hazard", level: "WARNING", lat: 41.57, lon: 2.26, created_at: "2026-06-13T10:00:30Z" },
            { id: 3, category: "medical", level: "WARNING", lat: 41.60, lon: 2.30, created_at: "2026-06-13T10:00:30Z" },
        ];
        expect(dedupeIncidents(list)).toHaveLength(3);
    });

    it("falls back to zone equality when coordinates are absent", () => {
        const list: Incident[] = [
            { id: 1, category: "noise", level: "INFO", zone: "5", created_at: "2026-06-13T10:00:00Z" },
            { id: 2, category: "noise", level: "INFO", zone: "5", created_at: "2026-06-13T10:02:00Z" },
            { id: 3, category: "noise", level: "INFO", zone: "9", created_at: "2026-06-13T10:03:00Z" },
        ];
        const out = dedupeIncidents(list);
        expect(out).toHaveLength(2);
        expect(out[0].duplicate_count).toBe(2);
    });

    it("does not mutate the input list", () => {
        const list: Incident[] = [
            { id: 1, category: "x", level: "INFO", zone: "1", created_at: "2026-06-13T10:00:00Z" },
        ];
        const snapshot = JSON.stringify(list);
        dedupeIncidents(list);
        expect(JSON.stringify(list)).toBe(snapshot);
        expect(list[0].duplicate_count).toBeUndefined();
    });

    it("tolerates non-array input", () => {
        // @ts-expect-error testing runtime guard against bad input
        expect(dedupeIncidents(undefined)).toEqual([]);
    });
});

describe("prioritizeIncidents", () => {
    it("orders by severity then most-recent", () => {
        const list: Incident[] = [
            { id: 1, level: "INFO", created_at: "2026-06-13T10:00:00Z" },
            { id: 2, level: "CRITICAL", created_at: "2026-06-13T09:00:00Z" },
            { id: 3, level: "WARNING", created_at: "2026-06-13T10:30:00Z" },
            { id: 4, level: "CRITICAL", created_at: "2026-06-13T10:00:00Z" },
        ];
        expect(prioritizeIncidents(list).map((i) => i.id)).toEqual([4, 2, 3, 1]);
    });

    it("is stable when severity and timestamps tie", () => {
        const list: Incident[] = [{ id: 1, level: "INFO" }, { id: 2, level: "INFO" }, { id: 3, level: "INFO" }];
        expect(prioritizeIncidents(list).map((i) => i.id)).toEqual([1, 2, 3]);
    });
});

describe("triageIncidents", () => {
    it("dedupes then prioritizes", () => {
        const list: Incident[] = [
            { id: 1, category: "medical", level: "INFO", zone: "1", created_at: "2026-06-13T10:00:00Z" },
            { id: 2, category: "medical", level: "CRITICAL", zone: "1", created_at: "2026-06-13T10:01:00Z" },
            { id: 3, category: "hazard", level: "WARNING", zone: "2", created_at: "2026-06-13T10:05:00Z" },
        ];
        const out = triageIncidents(list);
        expect(out).toHaveLength(2);
        expect(out[0].level).toBe("CRITICAL");
        expect(out[0].duplicate_count).toBe(2);
        expect(out[1].level).toBe("WARNING");
    });
});
