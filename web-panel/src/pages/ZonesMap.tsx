import { useState, useEffect, useCallback } from "react";
import { Layout } from "../components/Layout";
import { Zone, ZoneStatus } from "../types";
import { api } from "../services/apiClient";
import { Thermometer, Timer, TrendingUp, TrendingDown, Lock, AlertTriangle, RefreshCw, Unlock } from "lucide-react";

const SEED_ZONES: Zone[] = [
  { id: "grada-t1-recta-principal", name: "Grada T1 - Recta Principal", type: "GRADA", status: "ABIERTA", capacity: 15000, currentOccupancy: 0, temperature: 24.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "grada-t2-curva-ascari", name: "Grada T2 - Curva Ascari", type: "GRADA", status: "ABIERTA", capacity: 12000, currentOccupancy: 0, temperature: 26.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "fan-zone-principal", name: "Fan Zone Principal", type: "FANZONE", status: "ABIERTA", capacity: 7600, currentOccupancy: 0, temperature: 25.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "paddock-vip-boxes", name: "Paddock VIP - Boxes", type: "PADDOCK", status: "ABIERTA", capacity: 3500, currentOccupancy: 0, temperature: 22.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "vial-acceso-a-norte", name: "Vial Acceso A - Norte", type: "VIAL", status: "ABIERTA", capacity: 5000, currentOccupancy: 0, temperature: 23.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "grada-t3-chicane", name: "Grada T3 - Chicane", type: "GRADA", status: "ABIERTA", capacity: 18000, currentOccupancy: 0, temperature: 24.0, waitTime: 0, entryRate: 0, exitRate: 0 },
  { id: "fan-zone-tecnologica", name: "Fan Zone Tecnológica", type: "FANZONE", status: "ABIERTA", capacity: 4500, currentOccupancy: 0, temperature: 25.0, waitTime: 0, entryRate: 0, exitRate: 0 },
];

export function ZonesMap() {
  const [zones, setZones] = useState<Zone[]>([]);
  const [loading, setLoading] = useState(true);

  const loadZones = useCallback(async () => {
    try {
      setLoading(true);
      let data = await api.get<any>("zone_traffic");
      if (!data || data.length === 0) {
        for (const z of SEED_ZONES) {
          await api.upsert("zone_traffic", {
            ...z,
            current_occupancy: z.currentOccupancy,
            wait_time: z.waitTime,
            entry_rate: z.entryRate,
            exit_rate: z.exitRate,
            updated_at: new Date().toISOString(),
          });
        }
        data = await api.get<any>("zone_traffic");
      }
      const mapped: Zone[] = data.map((z: any) => ({
        id: z.id || z.zone_id,
        name: z.name || "",
        type: z.type || "GRADA",
        status: z.status || "ABIERTA",
        capacity: z.capacity ?? 0,
        currentOccupancy: z.current_occupancy ?? z.currentOccupancy ?? 0,
        temperature: z.temperature ?? 0,
        waitTime: z.wait_time ?? z.waitTime ?? 0,
        entryRate: z.entry_rate ?? z.entryRate ?? 0,
        exitRate: z.exit_rate ?? z.exitRate ?? 0,
        alerts: z.alerts ?? 0,
        color: z.color,
      }));
      setZones(mapped);
    } catch (err) {
      console.error("Error loading zones:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadZones();
    const interval = setInterval(loadZones, 15000);
    return () => clearInterval(interval);
  }, [loadZones]);

  const updateZoneStatus = async (zoneId: string, newStatus: ZoneStatus) => {
    try {
      await api.upsert("zone_traffic", {
        id: zoneId,
        status: newStatus,
        updated_at: new Date().toISOString(),
      });
      await loadZones();
    } catch (err) {
      console.error("Error updating zone:", err);
    }
  };

  const getStatusColor = (status: ZoneStatus) => {
    switch (status) {
      case "ABIERTA":
      case "OPERATIVA":
        return "bg-green-500/20 text-green-400 border-green-500/30";
      case "SATURADA":
        return "bg-yellow-500/20 text-yellow-400 border-yellow-500/30";
      case "CERRADA":
        return "bg-red-500/20 text-red-400 border-red-500/30";
      case "MANTENIMIENTO":
        return "bg-purple-500/20 text-purple-400 border-purple-500/30";
      default:
        return "bg-gray-500/20 text-gray-400 border-gray-500/30";
    }
  };

  const getOccupancyColor = (percentage: number) => {
    if (percentage >= 85) return "bg-red-500";
    if (percentage >= 60) return "bg-yellow-500";
    return "bg-green-500";
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-white">Zonas del Circuito</h2>
          <button
            onClick={loadZones}
            disabled={loading}
            className="flex items-center space-x-2 px-3 py-2 bg-dark-700 text-white rounded hover:bg-dark-600 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            <span className="text-sm">Actualizar</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {zones.map((zone) => {
            const occupancyPercentage = zone.capacity > 0 ? Math.round((zone.currentOccupancy / zone.capacity) * 100) : 0;

            return (
              <div
                key={zone.id}
                className="bg-dark-800 border border-dark-700 rounded-lg p-4 hover:border-blue-500/50 transition-all"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-white font-medium mb-1">{zone.name}</h3>
                    <p className="text-xs text-gray-400">Tipo: {zone.type}</p>
                  </div>
                  <span
                    className={`px-2 py-1 text-xs font-medium rounded border ${getStatusColor(zone.status)}`}
                  >
                    {zone.status}
                  </span>
                </div>

                <div className="mb-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-gray-400">Ocupación</span>
                    <span className="text-sm text-white">
                      {zone.currentOccupancy.toLocaleString()} / {zone.capacity.toLocaleString()}
                    </span>
                  </div>
                  <div className="h-2 bg-dark-700 rounded-full overflow-hidden">
                    <div
                      className={`h-full ${getOccupancyColor(occupancyPercentage)}`}
                      style={{ width: `${Math.min(occupancyPercentage, 100)}%` }}
                    />
                  </div>
                  <div className="text-right text-xs text-gray-400 mt-1">{occupancyPercentage}%</div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="flex items-center space-x-2">
                    <Thermometer className="w-4 h-4 text-gray-400" />
                    <div>
                      <div className="text-xs text-gray-400">Temperatura</div>
                      <div className="text-sm text-white font-medium">{zone.temperature}°C</div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Timer className="w-4 h-4 text-gray-400" />
                    <div>
                      <div className="text-xs text-gray-400">Espera</div>
                      <div className="text-sm text-white font-medium">{zone.waitTime} min</div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <TrendingUp className="w-4 h-4 text-green-400" />
                    <div>
                      <div className="text-xs text-gray-400">Entrada</div>
                      <div className="text-sm text-green-400 font-medium">{zone.entryRate}/min</div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <TrendingDown className="w-4 h-4 text-red-400" />
                    <div>
                      <div className="text-xs text-gray-400">Salida</div>
                      <div className="text-sm text-red-400 font-medium">{zone.exitRate}/min</div>
                    </div>
                  </div>
                </div>

                {zone.alerts && zone.alerts > 0 && (
                  <div className="mt-3 pt-3 border-t border-dark-700">
                    <button className="w-full flex items-center justify-center space-x-2 px-3 py-2 bg-yellow-500/10 text-yellow-400 rounded border border-yellow-500/30 hover:bg-yellow-500/20 transition-colors">
                      <AlertTriangle className="w-4 h-4" />
                      <span className="text-sm font-medium">{zone.alerts} ALERTAS ACTIVAS</span>
                    </button>
                  </div>
                )}

                <div className="mt-3 pt-3 border-t border-dark-700 grid grid-cols-2 gap-2">
                  {zone.status !== "CERRADA" ? (
                    <button
                      onClick={() => updateZoneStatus(zone.id, "CERRADA")}
                      className="flex items-center justify-center space-x-2 px-3 py-2 bg-dark-700 text-white rounded hover:bg-dark-600 transition-colors"
                    >
                      <Lock className="w-4 h-4" />
                      <span className="text-sm">Cerrar</span>
                    </button>
                  ) : (
                    <button
                      onClick={() => updateZoneStatus(zone.id, "ABIERTA")}
                      className="flex items-center justify-center space-x-2 px-3 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                    >
                      <Unlock className="w-4 h-4" />
                      <span className="text-sm">Abrir</span>
                    </button>
                  )}
                  {zone.status !== "SATURADA" && zone.status !== "CERRADA" && (
                    <button
                      onClick={() => updateZoneStatus(zone.id, "SATURADA")}
                      className="flex items-center justify-center space-x-2 px-3 py-2 bg-yellow-500/20 text-yellow-400 rounded border border-yellow-500/30 hover:bg-yellow-500/30 transition-colors"
                    >
                      <AlertTriangle className="w-4 h-4" />
                      <span className="text-sm">Saturar</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </Layout>
  );
}
