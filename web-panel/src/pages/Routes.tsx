import { useState, useEffect, useCallback } from "react";
import { Layout } from "../components/Layout";
import { Route, RouteStatus } from "../types";
import { api } from "../services/apiClient";
import { Navigation, Gauge, Radio, Clock, ArrowRight, Target, Lock, RefreshCw } from "lucide-react";

const SEED_ROUTES: Route[] = [
  { id: "parking-norte-grada-t1", name: "Parking Norte → Grada T1", origin: "Parking Norte", destination: "Grada T1", status: "OPERATIVA", activeUsers: 0, capacity: 600, capacityPercentage: 0, averageSpeed: 1.1, distance: 800, signalQuality: 95, estimatedTime: 12, velocity: 1.1 },
  { id: "parking-sur-grada-t2", name: "Parking Sur → Grada T2", origin: "Parking Sur", destination: "Grada T2", status: "OPERATIVA", activeUsers: 0, capacity: 800, capacityPercentage: 0, averageSpeed: 0.6, distance: 650, signalQuality: 88, estimatedTime: 18, velocity: 0.6 },
  { id: "fan-zone-paddock", name: "Fan Zone → Paddock VIP", origin: "Fan Zone Principal", destination: "Paddock VIP", status: "OPERATIVA", activeUsers: 0, capacity: 450, capacityPercentage: 0, averageSpeed: 1.5, distance: 420, signalQuality: 97, estimatedTime: 5, velocity: 1.5 },
  { id: "grada-t1-fan-zone", name: "Grada T1 → Fan Zone", origin: "Grada T1", destination: "Fan Zone", status: "OPERATIVA", activeUsers: 0, capacity: 350, capacityPercentage: 0, averageSpeed: 1.3, distance: 580, signalQuality: 91, estimatedTime: 7, velocity: 1.3 },
  { id: "parking-este-grada-t3", name: "Parking Este → Grada T3", origin: "Parking Este", destination: "Grada T3", status: "OPERATIVA", activeUsers: 0, capacity: 500, capacityPercentage: 0, averageSpeed: 0, distance: 720, signalQuality: 0, estimatedTime: 0, velocity: 0 },
  { id: "metro-fan-zone-tech", name: "Metro → Fan Zone Tech", origin: "Estación Metro", destination: "Fan Zone Tecnológica", status: "OPERATIVA", activeUsers: 0, capacity: 550, capacityPercentage: 0, averageSpeed: 1.0, distance: 900, signalQuality: 82, estimatedTime: 15, velocity: 1.0 },
  { id: "paddock-grada-t2", name: "Paddock → Grada T2", origin: "Paddock VIP", destination: "Grada T2", status: "OPERATIVA", activeUsers: 0, capacity: 250, capacityPercentage: 0, averageSpeed: 1.4, distance: 550, signalQuality: 92, estimatedTime: 9, velocity: 1.4 },
];

export function Routes() {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(true);

  const loadRoutes = useCallback(async () => {
    try {
      setLoading(true);
      let data = await api.get<any>("routes");
      if (!data || data.length === 0) {
        for (const r of SEED_ROUTES) {
          await api.upsert("routes", {
            ...r,
            active_users: r.activeUsers,
            capacity_percentage: r.capacityPercentage,
            average_speed: r.averageSpeed,
            signal_quality: r.signalQuality,
            estimated_time: r.estimatedTime,
            updated_at: new Date().toISOString(),
          });
        }
        data = await api.get<any>("routes");
      }
      const mapped: Route[] = data.map((r: any) => ({
        id: r.id || r.route_id,
        name: r.name || `${r.origin} → ${r.destination}`,
        origin: r.origin || "",
        destination: r.destination || "",
        status: r.status || "OPERATIVA",
        activeUsers: r.active_users ?? r.activeUsers ?? 0,
        capacity: r.capacity ?? 0,
        capacityPercentage: r.capacity_percentage ?? r.capacityPercentage ?? (r.capacity > 0 ? Math.round(((r.active_users ?? r.activeUsers ?? 0) / r.capacity) * 100) : 0),
        averageSpeed: r.average_speed ?? r.averageSpeed ?? 0,
        distance: r.distance ?? 0,
        signalQuality: r.signal_quality ?? r.signalQuality ?? 0,
        estimatedTime: r.estimated_time ?? r.estimatedTime ?? 0,
        velocity: r.velocity ?? 0,
      }));
      setRoutes(mapped);
    } catch (err) {
      console.error("Error loading routes:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRoutes();
    const interval = setInterval(loadRoutes, 15000);
    return () => clearInterval(interval);
  }, [loadRoutes]);

  const updateRouteStatus = async (routeId: string, newStatus: RouteStatus) => {
    try {
      const route = routes.find((r) => r.id === routeId);
      if (!route) return;
      const updatedUsers = newStatus === "CERRADA" ? 0 : route.activeUsers;
      const updatedPct = newStatus === "CERRADA" ? 0 : route.capacityPercentage;
      await api.upsert("routes", {
        id: routeId,
        status: newStatus,
        active_users: updatedUsers,
        capacity_percentage: updatedPct,
        updated_at: new Date().toISOString(),
      });
      await loadRoutes();
    } catch (err) {
      console.error("Error updating route status:", err);
    }
  };

  const getStatusColor = (status: RouteStatus) => {
    switch (status) {
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

  const getCapacityColor = (percentage: number) => {
    if (percentage >= 90) return "bg-red-500";
    if (percentage >= 70) return "bg-yellow-500";
    return "bg-green-500";
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-white">Rutas del Circuito</h2>
          <button
            onClick={loadRoutes}
            disabled={loading}
            className="flex items-center space-x-2 px-3 py-2 bg-dark-700 text-white rounded hover:bg-dark-600 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            <span className="text-sm">Actualizar</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {routes.map((route) => (
            <div
              key={route.id}
              className="bg-dark-800 border border-dark-700 rounded-lg p-4 hover:border-blue-500/50 transition-all"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h3 className="text-white font-medium mb-2">{route.name}</h3>
                  <div className="flex items-center text-xs text-gray-400 space-x-1">
                    <span>{route.origin}</span>
                    <ArrowRight className="w-3 h-3" />
                    <span>{route.destination}</span>
                  </div>
                </div>
                <span
                  className={`px-2 py-1 text-xs font-medium rounded border ${getStatusColor(route.status)}`}
                >
                  {route.status}
                </span>
              </div>

              <div className="mb-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-400">Usuarios Activos</span>
                  <span className="text-2xl font-bold text-red-400">{route.activeUsers}</span>
                </div>
                <div className="flex items-center justify-between text-xs text-gray-400 mb-1">
                  <span>Tiempo Estimado</span>
                  <span>{route.estimatedTime > 0 ? `${route.estimatedTime} min` : "–"}</span>
                </div>
              </div>

              <div className="mb-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-400">Capacidad</span>
                  <span className="text-sm text-white">{route.capacityPercentage}%</span>
                </div>
                <div className="h-2 bg-dark-700 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${getCapacityColor(route.capacityPercentage)}`}
                    style={{ width: `${Math.min(route.capacityPercentage, 100)}%` }}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 mb-4">
                <div className="flex items-center space-x-2">
                  <Gauge className="w-4 h-4 text-gray-400" />
                  <div>
                    <div className="text-xs text-gray-400">Velocidad</div>
                    <div className="text-sm text-white font-medium">
                      {route.velocity > 0 ? `${route.velocity} m/s` : "0.0 m/s"}
                    </div>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <Navigation className="w-4 h-4 text-gray-400" />
                  <div>
                    <div className="text-xs text-gray-400">Distancia</div>
                    <div className="text-sm text-white font-medium">{route.distance}m</div>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <Radio className="w-4 h-4 text-gray-400" />
                  <div>
                    <div className="text-xs text-gray-400">Señal</div>
                    <div
                      className={`text-sm font-medium ${
                        route.signalQuality >= 90
                          ? "text-green-400"
                          : route.signalQuality >= 70
                          ? "text-yellow-400"
                          : "text-red-400"
                      }`}
                    >
                      {route.signalQuality > 0 ? `${route.signalQuality}%` : "0%"}
                    </div>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <Clock className="w-4 h-4 text-gray-400" />
                  <div>
                    <div className="text-xs text-gray-400">Estimado</div>
                    <div className="text-sm text-white font-medium">
                      {route.estimatedTime > 0 ? `${route.estimatedTime} min` : "–"}
                    </div>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 pt-3 border-t border-dark-700">
                {route.status !== "SATURADA" && route.status !== "CERRADA" && (
                  <button
                    onClick={() => updateRouteStatus(route.id, "SATURADA")}
                    className="flex items-center justify-center space-x-2 px-3 py-2 bg-gradient-to-r from-pink-500 to-blue-500 text-white rounded hover:opacity-90 transition-opacity"
                  >
                    <Target className="w-4 h-4" />
                    <span className="text-sm">Saturar</span>
                  </button>
                )}
                {route.status !== "CERRADA" && (
                  <button
                    onClick={() => updateRouteStatus(route.id, "CERRADA")}
                    className="flex items-center justify-center space-x-2 px-3 py-2 bg-dark-700 text-white rounded hover:bg-dark-600 transition-colors"
                  >
                    <Lock className="w-4 h-4" />
                    <span className="text-sm">Cerrar</span>
                  </button>
                )}
              </div>

              {(route.status === "CERRADA" || route.status === "SATURADA") && (
                <div className="pt-3">
                  <button
                    onClick={() => updateRouteStatus(route.id, "OPERATIVA")}
                    className="w-full flex items-center justify-center space-x-2 px-3 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                  >
                    <Navigation className="w-4 h-4" />
                    <span className="text-sm">Abrir Ruta</span>
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
}
