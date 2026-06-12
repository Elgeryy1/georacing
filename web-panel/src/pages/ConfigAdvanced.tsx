import { Layout } from "../components/Layout";
import { Settings, Radio, Activity, AlertTriangle, CheckCircle } from "lucide-react";
import { useBeacons } from "../hooks/useBeacons";
import { getBeaconStats, getBeaconStatus } from "../utils/beaconUtils";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { emergencyService } from "../services/beaconService";

export function ConfigAdvanced() {
  const { beacons, loading } = useBeacons();
  const { user } = useAuth();
  const [activatingEmergency, setActivatingEmergency] = useState(false);
  
  const stats = getBeaconStats(beacons);
  
  const handleGlobalEmergency = async () => {
    if (!user) {
      alert("Debes estar autenticado para activar emergencias");
      return;
    }
    
    const confirmed = window.confirm(
      `¿Activar EMERGENCIA GLOBAL en ${stats.total} balizas?\n\nEsta acción afectará a todo el sistema.`
    );
    
    if (!confirmed) return;
    
    setActivatingEmergency(true);
    try {
      await emergencyService.activateGlobalEvacuation(
        beacons,
        user.uid,
        "¡EMERGENCIA! Siga las instrucciones del personal",
        "SALIDA PRINCIPAL"
      );
      alert(`Emergencia activada en ${stats.total} balizas`);
    } catch (error) {
      console.error("Error activando emergencia:", error);
      alert("Error al activar emergencia global");
    } finally {
      setActivatingEmergency(false);
    }
  };
  
  const handleDeactivateEmergency = async () => {
    if (!user) return;
    
    const confirmed = window.confirm(
      `¿Desactivar emergencias en ${stats.total} balizas?`
    );
    
    if (!confirmed) return;
    
    setActivatingEmergency(true);
    try {
      await emergencyService.deactivateGlobalEvacuation(beacons, user.uid);
      alert("Emergencias desactivadas");
    } catch (error) {
      console.error("Error:", error);
      alert("Error al desactivar emergencias");
    } finally {
      setActivatingEmergency(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64">
          <div className="text-gray-400">Cargando configuración...</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
          <div className="flex items-center gap-3">
            <Settings className="w-6 h-6 text-blue-400" />
            <h2 className="text-2xl font-semibold text-white">Configuración del Sistema</h2>
          </div>
        </div>

        {/* Estadísticas Globales */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-2">
              <Radio className="w-8 h-8 text-blue-400" />
              <span className="text-3xl font-bold text-white">{stats.total}</span>
            </div>
            <div className="text-sm text-gray-400">Balizas Totales</div>
          </div>

          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-2">
              <Activity className="w-8 h-8 text-green-400" />
              <span className="text-3xl font-bold text-green-400">{stats.online}</span>
            </div>
            <div className="text-sm text-gray-400">
              Online ({stats.uptime}% uptime)
            </div>
          </div>

          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-2">
              <AlertTriangle className="w-8 h-8 text-yellow-400" />
              <span className="text-3xl font-bold text-yellow-400">{stats.unconfigured}</span>
            </div>
            <div className="text-sm text-gray-400">Sin Configurar</div>
          </div>

          <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
            <div className="flex items-center justify-between mb-2">
              <AlertTriangle className="w-8 h-8 text-red-400" />
              <span className="text-3xl font-bold text-red-400">{stats.emergency}</span>
            </div>
            <div className="text-sm text-gray-400">En Emergencia</div>
          </div>
        </div>

        {/* Control de Emergencias */}
        <div className="bg-dark-800 border border-red-700 rounded-lg p-6">
          <div className="flex items-center gap-3 mb-4">
            <AlertTriangle className="w-6 h-6 text-red-400" />
            <h3 className="text-xl font-semibold text-white">Control de Emergencias</h3>
          </div>
          
          <div className="space-y-4">
            <p className="text-gray-400 text-sm">
              Activa o desactiva el modo emergencia en todas las balizas del sistema simultáneamente.
            </p>
            
            <div className="flex gap-4">
              <button
                onClick={handleGlobalEmergency}
                disabled={activatingEmergency || stats.total === 0}
                className="flex-1 py-3 px-6 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition-colors"
              >
                {activatingEmergency ? "Activando..." : "🚨 ACTIVAR EMERGENCIA GLOBAL"}
              </button>
              
              <button
                onClick={handleDeactivateEmergency}
                disabled={activatingEmergency || stats.emergency === 0}
                className="flex-1 py-3 px-6 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition-colors"
              >
                {activatingEmergency ? "Desactivando..." : "✅ DESACTIVAR EMERGENCIAS"}
              </button>
            </div>
            
            {stats.emergency > 0 && (
              <div className="bg-red-500/10 border border-red-500 rounded-lg p-4">
                <p className="text-red-400 font-semibold">
                  ⚠️ Hay {stats.emergency} baliza(s) en modo emergencia actualmente
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Lista de Balizas */}
        <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
          <h3 className="text-xl font-semibold text-white mb-4">Estado de Balizas</h3>
          
          {beacons.length === 0 ? (
            <div className="text-center py-8 text-gray-400">
              No hay balizas registradas en el sistema
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-dark-700">
                    <th className="text-left py-3 px-4 text-gray-400 font-medium">Estado</th>
                    <th className="text-left py-3 px-4 text-gray-400 font-medium">ID</th>
                    <th className="text-left py-3 px-4 text-gray-400 font-medium">Zona</th>
                    <th className="text-left py-3 px-4 text-gray-400 font-medium">Modo</th>
                    <th className="text-left py-3 px-4 text-gray-400 font-medium">Mensaje</th>
                  </tr>
                </thead>
                <tbody>
                  {beacons.map((beacon) => {
                    const status = getBeaconStatus(beacon);
                    
                    return (
                      <tr key={beacon.beaconId} className="border-b border-dark-700 hover:bg-dark-700/50">
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2">
                            <span className="text-xl">{status.emoji}</span>
                            <span className={`text-sm font-medium ${status.color}`}>
                              {status.text}
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <span className="font-mono text-sm text-white">{beacon.beaconId}</span>
                        </td>
                        <td className="py-3 px-4">
                          <span className="text-sm text-gray-300">{beacon.zone || "-"}</span>
                        </td>
                        <td className="py-3 px-4">
                          <span className={`px-2 py-1 rounded text-xs font-semibold ${
                            beacon.mode === "EMERGENCY" || beacon.mode === "EVACUATION" ? "bg-red-500/20 text-red-400" :
                            beacon.mode === "MAINTENANCE" ? "bg-blue-500/20 text-blue-400" :
                            beacon.mode === "CONGESTION" ? "bg-orange-500/20 text-orange-400" :
                            beacon.mode === "UNCONFIGURED" ? "bg-gray-500/20 text-gray-400" :
                            "bg-green-500/20 text-green-400"
                          }`}>
                            {beacon.mode}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <span className="text-sm text-gray-300 truncate max-w-xs block">
                            {beacon.message}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Balizas Sin Configurar */}
        {stats.unconfigured > 0 && (
          <div className="bg-yellow-500/10 border border-yellow-500 rounded-lg p-6">
            <div className="flex items-center gap-3 mb-2">
              <AlertTriangle className="w-5 h-5 text-yellow-500" />
              <h3 className="text-lg font-semibold text-yellow-500">Atención Requerida</h3>
            </div>
            <p className="text-yellow-400">
              Hay {stats.unconfigured} baliza(s) sin configurar. 
              Configúralas desde el panel de balizas para que estén operativas.
            </p>
          </div>
        )}

        {/* Balizas Offline */}
        {stats.offline > 0 && (
          <div className="bg-red-500/10 border border-red-500 rounded-lg p-6">
            <div className="flex items-center gap-3 mb-2">
              <AlertTriangle className="w-5 h-5 text-red-500" />
              <h3 className="text-lg font-semibold text-red-500">Balizas Desconectadas</h3>
            </div>
            <p className="text-red-400">
              Hay {stats.offline} baliza(s) offline. Verifica la conexión de red.
            </p>
          </div>
        )}

        {/* Sistema Operativo */}
        {stats.online === stats.total && stats.total > 0 && stats.unconfigured === 0 && (
          <div className="bg-green-500/10 border border-green-500 rounded-lg p-6">
            <div className="flex items-center gap-3">
              <CheckCircle className="w-5 h-5 text-green-500" />
              <p className="text-green-400 font-semibold">
                ✅ Sistema completamente operativo - Todas las balizas online y configuradas
              </p>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
