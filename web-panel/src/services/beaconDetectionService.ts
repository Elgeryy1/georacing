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
    let lastIds = new Set<string>();
    const interval = setInterval(async () => {
      try {
        const beacons = await api.get<Beacon>("beacons", { mode: "UNCONFIGURED" });
        const newBeacons: NewBeaconDetected[] = beacons.filter(b => !lastIds.has(b.beaconId)).map(b => ({
          beaconId: b.beaconId,
          firstSeen: b.lastSeen || ("createdAt" in b ? (b as any).createdAt : undefined),
          lastSeen: b.lastSeen || undefined,
          online: b.online ?? undefined
        }));
        if (newBeacons.length > 0) {
          newBeacons.forEach(b => lastIds.add(b.beaconId));
          callback(newBeacons);
        }
      } catch { }
    }, 4000);
    return () => clearInterval(interval);
  },
  subscribeAndDetectNew(existingBeaconIds: Set<string>, onNewBeacon: (beaconId: string, beacon: Partial<Beacon>) => void) {
    let notified = new Set(existingBeaconIds);
    const interval = setInterval(async () => {
      try {
        const beacons = await api.get<Beacon>("beacons", { mode: "UNCONFIGURED" });
        beacons.forEach(b => {
          if (!notified.has(b.beaconId)) {
            notified.add(b.beaconId);
            onNewBeacon(b.beaconId, b);
          }
        });
      } catch { }
    }, 4000);
    return () => clearInterval(interval);
  }
};
