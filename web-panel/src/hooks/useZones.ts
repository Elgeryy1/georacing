import { useMemo } from "react";
import { Beacon, ZoneInfo } from "../types";

export const useZones = (beacons: Beacon[]): ZoneInfo[] => {
  return useMemo(() => {
    const zoneMap = new Map<string, ZoneInfo>();

    beacons.forEach((beacon) => {
      if (!beacon.zone) return;
      const existing = zoneMap.get(beacon.zone);
      
      if (existing) {
        existing.totalBeacons++;
        if (beacon.mode === "EVACUATION") {
          existing.evacuationCount++;
        }
      } else {
        zoneMap.set(beacon.zone, {
          zone: beacon.zone,
          totalBeacons: 1,
          evacuationCount: beacon.mode === "EVACUATION" ? 1 : 0
        });
      }
    });

    return Array.from(zoneMap.values()).sort((a, b) => a.zone.localeCompare(b.zone));
  }, [beacons]);
};
