import type { Beacon, ZoneDB, Command, BeaconLog } from "../types";

const API_BASE_URL = "http://alpo.myqnapcloud.com:4010/api";

interface RequestOptions extends RequestInit {
  timeout?: number;
  retries?: number;
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { timeout = 5000, retries = 3, ...fetchOptions } = options;
  const fullUrl = url.startsWith("http") ? url : `${API_BASE_URL}${url}`;

  // Ensure HTTPS
  const secureUrl = fullUrl.replace(/^http:\/\//, "https://");

  let lastError: any;

  for (let i = 0; i <= retries; i++) {
    try {
      const controller = new AbortController();
      const id = setTimeout(() => controller.abort(), timeout);

      const res = await fetch(secureUrl, {
        ...fetchOptions,
        signal: controller.signal,
        headers: {
          "Content-Type": "application/json",
          ...(fetchOptions.headers || {})
        }
      });
      clearTimeout(id);

      if (!res.ok) {
        const errorText = await res.text();
        console.error(`API Error [${secureUrl}]: ${res.status} ${res.statusText} - ${errorText}`);
        throw new Error(errorText || `HTTP ${res.status}`);
      }
      return await res.json();
    } catch (err: any) {
      lastError = err;
      console.warn(`Attempt ${i + 1}/${retries + 1} failed for ${secureUrl}:`, err);
      if (i < retries) {
        await new Promise(resolve => setTimeout(resolve, 500 * Math.pow(2, i)));
      }
    }
  }
  throw lastError;
}

// --- Generic Firestore-like API ---

export const api = {
  /**
   * Generic GET (reads a table, optionally filtered)
   * POST /api/_get { table: "...", where: { ... } }
   */
  get: async <T = any>(table: string, where?: Record<string, any>): Promise<T[]> => {
    return request<T[]>("/_get", {
      method: "POST",
      body: JSON.stringify({ table, where })
    });
  },

  /**
   * Generic UPSERT (creates or updates a record)
   * POST /api/_upsert { table: "...", data: { ... } }
   */
  upsert: async (table: string, data: any): Promise<void> => {
    await request<void>("/_upsert", {
      method: "POST",
      body: JSON.stringify({ table, data })
    });
  },

  /**
   * Generic DELETE
   * POST /api/_delete { table: "...", where: { ... } }
   */
  delete: async (table: string, where: Record<string, any>): Promise<void> => {
    await request<void>("/_delete", {
      method: "POST",
      body: JSON.stringify({ table, where })
    });
  },

  // --- Helpers (wrapping generics) ---

  // Beacons
  getBeacons: async (): Promise<Beacon[]> => {
    const beacons = await api.get<any>("beacons");
    const now = Date.now();
    return beacons.map(b => {
      const lastSeenTime = b.last_heartbeat ? new Date(b.last_heartbeat).getTime() : 0;
      // Consider online if seen in the last 2 minutes (120000ms)
      const isOnline = (now - lastSeenTime) < 120000;

      return {
        ...b,
        beaconId: b.beacon_uid || b.beaconId, // Ensure beaconId is populated
        lastSeen: b.last_heartbeat,
        lastUpdate: b.last_status_change || b.updated_at,
        battery: b.battery_level,
        online: isOnline
      };
    });
  },

  // Zones
  getZones: async (): Promise<ZoneDB[]> => {
    return api.get<ZoneDB>("zones");
  },

  // Commands
  getCommands: async (): Promise<Command[]> => {
    return api.get<Command>("commands");
  },

  getPendingCommands: async (beaconUid: string): Promise<Command[]> => {
    return api.get<Command>("commands", { beacon_uid: beaconUid, executed: false });
  },

  // Circuit State
  getCircuitState: async (): Promise<any> => {
    const states = await api.get<any>("circuit_state", { id: 1 });
    return states[0] || null;
  },

  setCircuitState: async (mode: string, message: string = ""): Promise<void> => {
    await api.upsert("circuit_state", {
      id: "1",
      global_mode: mode,
      message: message,
      last_updated: new Date().toISOString().slice(0, 19).replace('T', ' ')
    });
  },

  // Incidents
  getIncidents: async (): Promise<any[]> => {
    return api.get<any>("incidents");
  },

  // Logs
  getBeaconLogs: async (): Promise<BeaconLog[]> => {
    return api.get<BeaconLog>("beacon_logs");
  }
};
