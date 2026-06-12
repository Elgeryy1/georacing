import { Beacon, BeaconUpdate, EmergencyLog } from "../types";
import { api } from "./apiClient";

/**
 * Converts frontend beacon fields to database column names.
 * Main mapping: 'arrow' -> 'arrow_direction'
 */
function toDbBeacon(data: Record<string, any>): Record<string, any> {
  const { arrow, ...rest } = data;
  return {
    ...rest,
    ...(arrow !== undefined && { arrow: arrow, arrow_direction: arrow })
  };
}

export const beaconsService = {
  subscribeToBeacons(callback: (beacons: Beacon[]) => void, intervalMs: number = 4000) {
    let lastBeacons: string = "";
    const interval = setInterval(async () => {
      try {
        const beacons = await api.getBeacons();
        const hash = JSON.stringify(beacons);
        if (hash !== lastBeacons) {
          lastBeacons = hash;
          callback(beacons);
        }
      } catch (e) {
        // Opcional: manejar error
      }
    }, intervalMs);
    return () => clearInterval(interval);
  },

  /**
   * Configura una baliza individual enviando comando UPDATE_CONFIG
   * Marca automáticamente configured = true al guardar
   */
  configureBeacon: async (beaconId: string, config: BeaconUpdate) => {
    // Preparar la configuración completa con configured = true
    const fullConfig = { ...config, configured: true };

    // Buscar ID numérico de la baliza para asegurar compatibilidad
    let numericId: number | undefined;
    try {
      const beacons = await api.get<any>("beacons", { beacon_uid: beaconId });
      if (beacons.length > 0) numericId = beacons[0].id;
    } catch (e) {
      console.warn("Could not resolve beacon numeric ID", e);
    }

    // Enviar comando UPDATE_CONFIG a la baliza (comunicación en tiempo real)
    await api.upsert("commands", {
      beacon_uid: beaconId,
      beacon_id: numericId, // Añadimos ID numérico
      command: "UPDATE_CONFIG",
      value: JSON.stringify(fullConfig),
      status: "PENDING",
      executed: 0,
      created_at: new Date().toISOString().slice(0, 19).replace("T", " ")
    });

    // Actualizar en la base de datos para persistencia
    await api.upsert("beacons", toDbBeacon({
      beacon_uid: beaconId,
      ...fullConfig
    }));
  },

  /**
   * Actualiza una baliza enviando comando UPDATE_CONFIG
   * Si incluye cambios significativos, marca configured = true
   */
  updateBeacon: async (beaconId: string, updates: BeaconUpdate) => {
    // Si se actualizan campos importantes, marcar como configurada
    const hasSignificantChanges = updates.mode || updates.arrow || updates.message || updates.zone;
    const fullUpdates = hasSignificantChanges ? { ...updates, configured: true } : updates;

    // Buscar ID numérico de la baliza
    let numericId: number | undefined;
    try {
      const beacons = await api.get<any>("beacons", { beacon_uid: beaconId });
      if (beacons.length > 0) numericId = beacons[0].id;
    } catch (e) { }

    // Enviar comando UPDATE_CONFIG a la baliza (comunicación en tiempo real)
    await api.upsert("commands", {
      beacon_uid: beaconId,
      beacon_id: numericId,
      command: "UPDATE_CONFIG",
      value: JSON.stringify(fullUpdates),
      status: "PENDING",
      executed: 0,
      created_at: new Date().toISOString().slice(0, 19).replace("T", " ")
    });

    // También actualizar en la base de datos para reflejar el cambio
    await api.upsert("beacons", toDbBeacon({
      beacon_uid: beaconId,
      ...fullUpdates
    }));
  },

  /**
   * Actualiza múltiples balizas
   */
  updateMultipleBeacons: async (beaconIds: string[], updates: BeaconUpdate) => {
    await Promise.all(beaconIds.map(id => beaconsService.updateBeacon(id, updates)));
  },
  /**
   * Cambia el modo de una baliza
   */
  setBeaconMode: async (beaconId: string, mode: BeaconUpdate["mode"]) => {
    await api.upsert("beacons", {
      beacon_uid: beaconId,
      mode
    });
  },

  /**
   * Activa modo emergencia en todas las balizas
   */
  activateEmergencyAll: async (message: string, arrow: BeaconUpdate["arrow"] = "NONE") => {
    const beacons = await api.getBeacons();
    await Promise.all(beacons.map(b => api.upsert("beacons", toDbBeacon({
      beacon_uid: b.beaconId,
      mode: "EMERGENCY",
      arrow,
      message,
      color: "#FF0000",
      brightness: 100,
      configured: true
    }))));
    return beacons.length;
  },

  /**
   * Crea una baliza de prueba manualmente
   */
  createTestBeacon: async (beaconId: string) => {
    await api.upsert("beacons", toDbBeacon({
      beacon_uid: beaconId,
      configured: true,
      mode: "NORMAL",
      arrow: "NONE",
      message: "Baliza de prueba",
      color: "#00FF00",
      brightness: 50,
      language: "ES",
      online: false,
      zone: "Zona Test",
      evacuationExit: "",
      tags: ["test"]
    }));
  },

  /**
   * Envía un comando a una baliza específica
   * El comando se auto-limpia después de 7 segundos
   */
  sendCommand: async (beaconId: string, command: string) => {
    // Buscar ID numérico de la baliza
    let numericId: number | undefined;
    try {
      const beacons = await api.get<any>("beacons", { beacon_uid: beaconId });
      if (beacons.length > 0) numericId = beacons[0].id;
    } catch (e) { }

    // Crear el comando
    await api.upsert("commands", {
      beacon_uid: beaconId,
      beacon_id: numericId,
      command: command,
      value: "{}",
      status: "PENDING",
      executed: 0,
      created_at: new Date().toISOString().slice(0, 19).replace("T", " ")
    });
  },

  /**
   * Reinicia una baliza específica (reinicia el sistema Windows completo)
   * El comando se auto-limpia después de 7 segundos
   */
  restartBeacon: async (beaconId: string) => {
    await beaconsService.sendCommand(beaconId, "RESTART");
  },

  /**
   * Reinicia todas las balizas (reinicia todos los sistemas Windows)
   * Los comandos se auto-limpian después de 7 segundos
   */
  restartAllBeacons: async () => {
    const beacons = await api.getBeacons();
    await Promise.all(beacons.map(b => beaconsService.sendCommand(b.beaconId, "RESTART")));
    return beacons.length;
  },

  /**
   * Apaga una baliza específica (apaga el sistema Windows completo)
   */
  shutdownBeacon: async (beaconId: string) => {
    await beaconsService.sendCommand(beaconId, "SHUTDOWN");
  },

  /**
   * Cierra la aplicación de la baliza (vuelve al escritorio)
   */
  closeAppBeacon: async (beaconId: string) => {
    await beaconsService.sendCommand(beaconId, "CLOSE_APP");
  }
};

export const emergencyService = {
  logEmergencyAction: async (log: Omit<EmergencyLog, "id" | "triggeredAt">) => {
    await api.upsert("emergency_logs", {
      ...log,
      created_at: new Date().toISOString().slice(0, 19).replace("T", " ")
    });
  },
  activateGlobalEvacuation: async (beacons: Beacon[], userId: string, message: string, evacuationExit: string) => {
    await Promise.all(beacons.map(beacon => api.upsert("beacons", {
      beacon_uid: beacon.beaconId,
      mode: "EVACUATION",
      message,
      evacuationExit,
      color: "#FF0000",
      brightness: 100
    })));
    await emergencyService.logEmergencyAction({
      type: "GLOBAL_EVACUATION_ON",
      triggeredByUid: userId,
      payload: { message, evacuationExit, beaconCount: beacons.length }
    });
  },
  deactivateGlobalEvacuation: async (beacons: Beacon[], userId: string) => {
    await Promise.all(beacons.map(beacon => api.upsert("beacons", {
      beacon_uid: beacon.beaconId,
      mode: "NORMAL",
      message: "Acceso Principal",
      evacuationExit: "",
      color: "#00FFAA",
      brightness: 90
    })));
    await emergencyService.logEmergencyAction({
      type: "GLOBAL_EVACUATION_OFF",
      triggeredByUid: userId,
      payload: { beaconCount: beacons.length }
    });
  },
  activateZoneEvacuation: async (zone: string, beacons: Beacon[], userId: string, message: string, evacuationExit: string) => {
    const zoneBeacons = beacons.filter(b => b.zone === zone);
    await Promise.all(zoneBeacons.map(beacon => api.upsert("beacons", {
      beacon_uid: beacon.beaconId,
      mode: "EVACUATION",
      message,
      evacuationExit,
      color: "#FF0000",
      brightness: 100
    })));
    await emergencyService.logEmergencyAction({
      type: "ZONE_EVACUATION_ON",
      zone,
      triggeredByUid: userId,
      payload: { message, evacuationExit, beaconCount: zoneBeacons.length }
    });
  },
  deactivateZoneEvacuation: async (zone: string, beacons: Beacon[], userId: string) => {
    const zoneBeacons = beacons.filter(b => b.zone === zone);
    await Promise.all(zoneBeacons.map(beacon => api.upsert("beacons", {
      beacon_uid: beacon.beaconId,
      mode: "NORMAL",
      message: "Acceso Principal",
      evacuationExit: "",
      color: "#00FFAA",
      brightness: 90
    })));
    await emergencyService.logEmergencyAction({
      type: "ZONE_EVACUATION_OFF",
      zone,
      triggeredByUid: userId,
      payload: { beaconCount: zoneBeacons.length }
    });
  }
};
