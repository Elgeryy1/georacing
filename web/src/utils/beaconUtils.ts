import { Beacon } from "../types";

/**
 * Verifica si una baliza está online basándose en su último heartbeat
 * @param beacon - La baliza a verificar
 * @returns true si la baliza está online (heartbeat < 15 segundos)
 */
export const isBeaconOnline = (beacon: Beacon): boolean => {
  if (!beacon.online) return false;
  
  const lastSeen = beacon.lastSeen ? new Date(beacon.lastSeen) : undefined;
  if (!lastSeen) return false;
  
  const now = new Date();
  const secondsAgo = (now.getTime() - lastSeen.getTime()) / 1000;
  
  // Considerar online si heartbeat < 15 segundos
  return secondsAgo < 15;
};

/**
 * Formatea el tiempo desde la última conexión
 * @param beacon - La baliza
 * @returns String con el tiempo formateado
 */
export const formatLastSeen = (beacon: Beacon): string => {
  const lastSeen = beacon.lastSeen ? new Date(beacon.lastSeen) : undefined;
  if (!lastSeen) return "Nunca";
  
  const now = new Date();
  const secondsAgo = Math.floor((now.getTime() - lastSeen.getTime()) / 1000);
  
  if (secondsAgo < 60) return `Hace ${secondsAgo}s`;
  if (secondsAgo < 3600) return `Hace ${Math.floor(secondsAgo / 60)}m`;
  if (secondsAgo < 86400) return `Hace ${Math.floor(secondsAgo / 3600)}h`;
  return `Hace ${Math.floor(secondsAgo / 86400)}d`;
};

/**
 * Obtiene el estado visual de una baliza
 * @param beacon - La baliza
 * @returns Emoji y texto del estado
 */
export const getBeaconStatus = (beacon: Beacon): { emoji: string; text: string; color: string } => {
  const online = isBeaconOnline(beacon);
  
  if (!online) {
    return { emoji: "🔴", text: "Offline", color: "text-red-500" };
  }
  
  if (!beacon.configured) {
    return { emoji: "⚠️", text: "Sin configurar", color: "text-yellow-500" };
  }
  
  switch (beacon.mode) {
    case "EMERGENCY":
    case "EVACUATION":
      return { emoji: "🚨", text: "Emergencia", color: "text-red-500" };
    case "MAINTENANCE":
      return { emoji: "🔧", text: "Mantenimiento", color: "text-blue-500" };
    case "CONGESTION":
      return { emoji: "⚠️", text: "Congestión", color: "text-orange-500" };
    case "NORMAL":
      return { emoji: "🟢", text: "Normal", color: "text-green-500" };
    default:
      return { emoji: "🟢", text: "Online", color: "text-green-500" };
  }
};

/**
 * Obtiene el color de fondo según el modo
 * @param mode - El modo de la baliza
 * @returns Color hexadecimal
 */
export const getModeColor = (mode: Beacon["mode"]): string => {
  switch (mode) {
    case "UNCONFIGURED":
      return "#333333";
    case "NORMAL":
      return "#00FF00";
    case "CONGESTION":
      return "#FFA500";
    case "EMERGENCY":
      return "#FF6600";
    case "EVACUATION":
      return "#FF0000";
    case "MAINTENANCE":
      return "#808080";
    default:
      return "#00FF00";
  }
};

/**
 * Obtiene el nombre traducido del modo
 * @param mode - El modo de la baliza
 * @returns Nombre del modo
 */
export const getModeName = (mode: Beacon["mode"]): string => {
  switch (mode) {
    case "UNCONFIGURED":
      return "Sin configurar";
    case "NORMAL":
      return "Normal";
    case "CONGESTION":
      return "Congestión";
    case "EMERGENCY":
      return "Emergencia";
    case "EVACUATION":
      return "Evacuación";
    case "MAINTENANCE":
      return "Mantenimiento";
    default:
      return mode || "Sin configurar";
  }
};

/**
 * Obtiene el nombre traducido de la dirección de flecha
 * @param arrow - La dirección de la flecha
 * @returns Nombre de la dirección
 */
export const getArrowName = (arrow: Beacon["arrow"]): string => {
  switch (arrow) {
    case "NONE":
      return "Sin flecha";
    case "UP":
      return "↑ Arriba";
    case "DOWN":
      return "↓ Abajo";
    case "LEFT":
      return "← Izquierda";
    case "RIGHT":
      return "→ Derecha";
    case "UP_LEFT":
      return "↖ Arriba-Izquierda";
    case "UP_RIGHT":
      return "↗ Arriba-Derecha";
    case "DOWN_LEFT":
      return "↙ Abajo-Izquierda";
    case "DOWN_RIGHT":
      return "↘ Abajo-Derecha";
    default:
      return arrow || "Sin flecha";
  }
};

/**
 * Filtra balizas por zona
 * @param beacons - Array de balizas
 * @param zone - Zona a filtrar
 * @returns Array de balizas filtradas
 */
export const filterBeaconsByZone = (beacons: Beacon[], zone: string): Beacon[] => {
  return beacons.filter(b => b.zone === zone);
};

/**
 * Filtra balizas por estado online/offline
 * @param beacons - Array de balizas
 * @param online - true para online, false para offline
 * @returns Array de balizas filtradas
 */
export const filterBeaconsByOnlineStatus = (beacons: Beacon[], online: boolean): Beacon[] => {
  return beacons.filter(b => isBeaconOnline(b) === online);
};

/**
 * Obtiene estadísticas de las balizas
 * @param beacons - Array de balizas
 * @returns Estadísticas
 */
export const getBeaconStats = (beacons: Beacon[]) => {
  const total = beacons.length;
  const online = beacons.filter(isBeaconOnline).length;
  const configured = beacons.filter(b => b.configured).length;
  const emergency = beacons.filter(b => b.mode === "EMERGENCY" || b.mode === "EVACUATION").length;
  
  return {
    total,
    online,
    offline: total - online,
    configured,
    unconfigured: total - configured,
    emergency,
    uptime: total > 0 ? Math.round((online / total) * 100) : 0
  };
};
