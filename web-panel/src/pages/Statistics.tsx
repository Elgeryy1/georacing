import { useState, useEffect } from "react";
import { Layout } from "../components/Layout";
import { SystemStats, TrafficPeak } from "../types";
import { Users, TrendingUp, Percent } from "lucide-react";

export function Statistics() {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [trafficPeaks, setTrafficPeaks] = useState<TrafficPeak[]>([]);

  useEffect(() => {
    // Datos de ejemplo - en producción vendría de Firestore
    const mockStats: SystemStats = {
      activeVisitors: 30336,
      dailyTotal: 47523,
      satisfaction: 92.5,
      activeZones: 8,
      totalZones: 8,
      operationalRoutes: 6,
      totalRoutes: 8,
      activeBeacons: 10,
      totalBeacons: 12,
      activeAlerts: 6,
      avgVisitTime: 187,
      dailyRevenue: 12847
    };

    const mockTrafficPeaks: TrafficPeak[] = [
      { time: "10:00", count: 5200 },
      { time: "12:00", count: 8900 },
      { time: "14:00", count: 12300 },
      { time: "16:00", count: 9100 }
    ];

    setStats(mockStats);
    setTrafficPeaks(mockTrafficPeaks);
  }, []);

  if (!stats) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-96">
          <div className="text-gray-400">Cargando estadísticas...</div>
        </div>
      </Layout>
    );
  }

  const maxPeak = Math.max(...trafficPeaks.map(p => p.count));

  return (
    <Layout>
      <div className="space-y-6">
        {/* Métricas Principales */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Visitantes Activos */}
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-500/10 rounded-lg">
                <Users className="w-6 h-6 text-blue-400" />
              </div>
            </div>
            <div className="text-5xl font-bold text-white mb-2">
              {stats.activeVisitors.toLocaleString()}
            </div>
            <div className="text-sm text-gray-400 uppercase tracking-wide">
              VISITANTES ACTIVOS
            </div>
          </div>

          {/* Total del Día */}
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-500/10 rounded-lg">
                <TrendingUp className="w-6 h-6 text-green-400" />
              </div>
            </div>
            <div className="text-5xl font-bold text-white mb-2">
              {stats.dailyTotal.toLocaleString()}
            </div>
            <div className="text-sm text-gray-400 uppercase tracking-wide">
              TOTAL DEL DÍA
            </div>
          </div>

          {/* Satisfacción */}
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-yellow-500/10 rounded-lg">
                <Percent className="w-6 h-6 text-yellow-400" />
              </div>
            </div>
            <div className="text-5xl font-bold text-yellow-400 mb-2">
              {stats.satisfaction}%
            </div>
            <div className="text-sm text-gray-400 uppercase tracking-wide">
              SATISFACCIÓN
            </div>
          </div>
        </div>

        {/* Estadísticas del Sistema y Picos de Afluencia */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Estadísticas del Sistema */}
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <h2 className="text-xl font-semibold text-white mb-6">
              Estadísticas del Sistema
            </h2>
            <div className="space-y-4">
              {/* Zonas Activas */}
              <div className="flex items-center justify-between py-3 border-b border-dark-700">
                <span className="text-gray-400">Zonas Activas</span>
                <span className="text-lg font-semibold text-green-400">
                  {stats.activeZones} / {stats.totalZones}
                </span>
              </div>

              {/* Rutas Operativas */}
              <div className="flex items-center justify-between py-3 border-b border-dark-700">
                <span className="text-gray-400">Rutas Operativas</span>
                <span className="text-lg font-semibold text-green-400">
                  {stats.operationalRoutes} / {stats.totalRoutes}
                </span>
              </div>

              {/* Balizas Activas */}
              <div className="flex items-center justify-between py-3 border-b border-dark-700">
                <span className="text-gray-400">Balizas Activas</span>
                <span className="text-lg font-semibold text-green-400">
                  {stats.activeBeacons} / {stats.totalBeacons}
                </span>
              </div>

              {/* Alertas Activas */}
              <div className="flex items-center justify-between py-3 border-b border-dark-700">
                <span className="text-gray-400">Alertas Activas</span>
                <span className="text-lg font-semibold text-red-400">
                  {stats.activeAlerts}
                </span>
              </div>

              {/* Tiempo Promedio de Visita */}
              <div className="flex items-center justify-between py-3 border-b border-dark-700">
                <span className="text-gray-400">Tiempo Promedio de Visita</span>
                <span className="text-lg font-semibold text-white">
                  {stats.avgVisitTime} min
                </span>
              </div>

              {/* Ingresos del Día */}
              <div className="flex items-center justify-between py-3">
                <span className="text-gray-400">Ingresos del Día</span>
                <span className="text-lg font-semibold text-green-400">
                  {stats.dailyRevenue.toLocaleString()}
                </span>
              </div>
            </div>
          </div>

          {/* Picos de Afluencia */}
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <h2 className="text-xl font-semibold text-white mb-6">
              Picos de Afluencia
            </h2>
            <div className="space-y-6">
              {trafficPeaks.map((peak, index) => {
                const percentage = (peak.count / maxPeak) * 100;
                
                return (
                  <div key={index}>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-gray-400 font-medium">{peak.time}</span>
                      <span className="text-xl font-bold text-white">
                        {peak.count.toLocaleString()}
                      </span>
                    </div>
                    <div className="h-8 bg-dark-700 rounded-lg overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-blue-500 to-purple-500 transition-all flex items-center justify-end px-3"
                        style={{ width: `${percentage}%` }}
                      >
                        {percentage > 30 && (
                          <span className="text-xs font-semibold text-white">
                            {Math.round(percentage)}%
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
