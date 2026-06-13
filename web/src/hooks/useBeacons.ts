import { useState, useEffect } from "react";
import { Beacon } from "../types";
import { beaconsService } from "../services/beaconService";
import { api } from "../services/apiClient";

export const useBeacons = () => {
  const [beacons, setBeacons] = useState<Beacon[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = async () => {
    try {
      const data = await api.getBeacons();
      setBeacons(data);
    } catch (e) {
      // opcional: manejar error
    }
  };

  useEffect(() => {
    setLoading(true);
    const unsubscribe = beaconsService.subscribeToBeacons((beaconsData) => {
      setBeacons(beaconsData);
      setLoading(false);
    }, 3000); // Adjusted interval slightly
    return () => unsubscribe();
  }, []);

  return { beacons, loading, refresh };
};
