



export type BeaconMode = "UNCONFIGURED" | "NORMAL" | "CONGESTION" | "EMERGENCY" | "EVACUATION" | "MAINTENANCE";
export type ArrowDirection = "NONE" | "LEFT" | "RIGHT" | "UP" | "DOWN" | "UP_LEFT" | "UP_RIGHT" | "DOWN_LEFT" | "DOWN_RIGHT" | "FORWARD" | "BACKWARD" | "FORWARD_LEFT" | "FORWARD_RIGHT" | "BACKWARD_LEFT" | "BACKWARD_RIGHT";
export type Language = "ES" | "EN" | "FR" | "DE" | "IT" | "PT" | "CA";
export type CommandType = "SET_MODE" | "SET_MESSAGE" | "SET_BRIGHTNESS" | "SET_ARROW" | "SET_CONFIG" | "REBOOT" | "PING" | "CUSTOM";
export type CommandStatus = "PENDING" | "SENT" | "EXECUTED" | "FAILED" | "CANCELLED";
export type EmergencyLevel = "INFO" | "ADVISORY" | "WARNING" | "CRITICAL";
export type EmergencyStatus = "ACTIVE" | "RESOLVED";
export type EmergencyAction = "EVACUATION_PATH" | "BLOCKED_ZONE" | "ALERT_ONLY";

export interface Beacon {
  // Campos principales - mapeo con DB
  id?: number;                         // id: int auto
  beaconId: string;                    // beacon_uid: varchar
  beacon_uid?: string;                 // Alias for beaconId to match DB
  name: string | null;                 // name: varchar
  description?: string | null;         // description: text
  zoneId?: number | null;              // zone_id: int
  latitude?: number | null;            // latitude: double
  longitude?: number | null;           // longitude: double
  hasScreen?: boolean;                 // has_screen: boolean
  mode: BeaconMode | null;             // mode: varchar
  arrow: ArrowDirection | null;        // arrow_direction: varchar
  message: string | null;              // message: text
  color: string | null;                // color: varchar
  brightness: number | null;           // brightness: int
  battery: number | null;              // battery_level: int
  online: boolean | null;              // is_online: boolean
  configured: boolean;                 // configured: boolean
  lastHeartbeat?: string | null;       // last_heartbeat: datetime
  lastStatusChange?: string | null;    // last_status_change: datetime
  createdAt?: string;                  // created_at: datetime
  updatedAt?: string;                  // updated_at: datetime

  // Legacy/Frontend specific fields (keeping for compatibility if needed)
  lastUpdate?: string | null;
  lastSeen?: string | null;
  zone?: string | null; // Nombre de la zona si viene populado
  evacuationExit?: string | null;
  tags?: string[] | null;
  language?: Language | null;
}

export interface ZoneDB {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
}

export interface Command {
  id: number;
  beacon_uid?: string;
  beacon_id?: number;
  command: string;
  value: any;
  status: string;
  created_at: string;
  executed_at?: string;

  // Legacy mappings for UI compatibility (optional)
  beaconId?: number;
  commandType?: CommandType;
  createdAt?: string;
}

export interface BeaconLog {
  id: number;
  beacon_id?: number;
  beaconId?: number;
  message: string;
  created_at: string;

  // Legacy
  eventType?: "HEARTBEAT" | "STATUS_CHANGE" | "COMMAND_SENT" | "COMMAND_EXECUTED" | "COMMAND_FAILED" | "ERROR" | "INFO";
  data?: any;
  createdAt?: string;
}

export interface Emergency {
  id: number;
  title: string;
  description?: string;
  level: EmergencyLevel;
  status: EmergencyStatus;
  triggeredBy?: string;
  zoneId?: number;
  startedAt?: string;
  resolvedAt?: string;
}

export interface EmergencyBeacon {
  id: number;
  emergencyId: number;
  beaconId: number;
  action: EmergencyAction;
  createdAt: string;
}

export interface StaffGroup {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
}

export interface StaffPosition {
  id: number;
  groupId: number;
  displayName: string;
  phone?: string;
  notes?: string;
  createdAt: string;
}

export interface GroupLocation {
  id: number;
  groupId: number;
  userIdentifier: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  source?: string;
  recordedAt: string;
}

// --- Legacy / UI Types ---

export interface EmergencyLog {
  id?: string;
  type: "GLOBAL_EVACUATION_ON" | "GLOBAL_EVACUATION_OFF" | "ZONE_EVACUATION_ON" | "ZONE_EVACUATION_OFF";
  zone?: string | null;
  triggeredByUid: string;
  triggeredAt?: string | null;
  created_at?: string;
  payload: Record<string, unknown>;
}

export interface BeaconUpdate {
  mode?: BeaconMode;
  arrow?: ArrowDirection;
  message?: string;
  color?: string;
  brightness?: number;
  language?: Language;
  evacuationExit?: string;
  zone?: string;
  tags?: string[];
  configured?: boolean;
  name?: string;
}

export interface ZoneInfo {
  zone: string;
  totalBeacons: number;
  evacuationCount: number;
}

export type ZoneStatus = "ABIERTA" | "SATURADA" | "CERRADA" | "MANTENIMIENTO" | "OPERATIVA";

export interface Zone {
  id: string;
  name: string;
  type: string; // "GRADA", "PADDOCK", "FANZONE", "VIAL", "PARKING"
  status: ZoneStatus;
  capacity: number;
  currentOccupancy: number;
  temperature: number;
  waitTime: number; // minutos
  entryRate: number; // personas/min
  exitRate: number; // personas/min
  alerts?: number;
  color?: string; // Color del estado
}

export type RouteStatus = "OPERATIVA" | "SATURADA" | "CERRADA" | "MANTENIMIENTO";

export interface Route {
  id: string;
  name: string;
  origin: string;
  destination: string;
  status: RouteStatus;
  activeUsers: number;
  capacity: number;
  capacityPercentage: number;
  averageSpeed: number; // m/s
  distance: number; // metros
  signalQuality: number; // porcentaje
  estimatedTime: number; // minutos
  velocity: number; // m/s
  color?: string;
}

export interface SystemStats {
  activeVisitors: number;
  dailyTotal: number;
  satisfaction: number; // porcentaje
  activeZones: number;
  totalZones: number;
  operationalRoutes: number;
  totalRoutes: number;
  activeBeacons: number;
  totalBeacons: number;
  activeAlerts: number;
  avgVisitTime: number; // minutos
  dailyRevenue: number;
}

export interface TrafficPeak {
  time: string;
  count: number;
}

