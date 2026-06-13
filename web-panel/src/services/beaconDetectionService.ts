import { Beacon } from "../types";
import { api } from "./apiClient";

export interface NewBeaconDetected {
  beaconId: string;
  firstSeen?: string;
  lastSeen?: string;
  online?: boolean;
}

export const beaconDetectionService = {
  subscribeToNewBeacons(callback: (newBeacons: NewBeaconDetected[]) => void) {
    const lastIds = new Set<string>();
    const interval = setInterval(async () => {
      try {
        const beacons = await api.get<Beacon>("beacons", { mode: "UNCONFIGURED" });
        // La API cruda devuelve beacon_uid (no beaconId): normalizamos el identificador
        const newBeacons: NewBeaconDetected[] = beacons
          .filter(b => {
            const uid = (b as any).beacon_uid ?? b.beaconId;
            return !!uid && !lastIds.has(uid);
          })
          .map(b => {
            const uid = (b as any).beacon_uid ?? b.beaconId;
            return {
              beaconId: uid,
              firstSeen: b.lastSeen || (b as any).last_heartbeat || b.createdAt || (b as any).created_at,
              lastSeen: b.lastSeen || (b as any).last_heartbeat || undefined,
              online: b.online ?? undefined
            };
          });
        if (newBeacons.length > 0) {
          newBeacons.forEach(b => lastIds.add(b.beaconId));
          callback(newBeacons);
        }
      } catch {
        // Polling is best-effort: a transient API/network error on one tick
        // should not tear down the interval. The next tick retries.
      }
    }, 4000);
    return () => clearInterval(interval);
  },
  subscribeAndDetectNew(existingBeaconIds: Set<string>, onNewBeacon: (beaconId: string, beacon: Partial<Beacon>) => void) {
    const notified = new Set(existingBeaconIds);
    const interval = setInterval(async () => {
      try {
        const beacons = await api.get<Beacon>("beacons", { mode: "UNCONFIGURED" });
        beacons.forEach(b => {
          const uid = (b as any).beacon_uid ?? b.beaconId;
          if (uid && !notified.has(uid)) {
            notified.add(uid);
            onNewBeacon(uid, b);
          }
        });
      } catch {
        // Best-effort polling: swallow transient errors so the interval keeps
        // running and detection resumes on the next tick.
      }
    }, 4000);
    return () => clearInterval(interval);
  }
};
