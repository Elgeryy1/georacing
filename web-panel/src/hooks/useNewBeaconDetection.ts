import { useState, useEffect } from "react";
import { beaconDetectionService, NewBeaconDetected } from "../services/beaconDetectionService";

export const useNewBeaconDetection = () => {
  const [newBeacons, setNewBeacons] = useState<NewBeaconDetected[]>([]);
  const [hasNewBeacons, setHasNewBeacons] = useState(false);

  useEffect(() => {
    const unsubscribe = beaconDetectionService.subscribeToNewBeacons((detected) => {
      setNewBeacons(detected);
      setHasNewBeacons(detected.length > 0);
    });

    return () => unsubscribe();
  }, []);

  const clearNewBeacons = () => {
    setNewBeacons([]);
    setHasNewBeacons(false);
  };

  return { newBeacons, hasNewBeacons, clearNewBeacons };
};
