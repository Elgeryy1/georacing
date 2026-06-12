import React, { useState } from "react";
import { Layout } from "../components/Layout";
import { useBeacons } from "../hooks/useBeacons";
import { useZones } from "../hooks/useZones";
import { useAuth } from "../context/AuthContext";
import { emergencyService } from "../services/beaconService";
import { AlertTriangle, Power, Shield } from "lucide-react";

export const Emergencies: React.FC = () => {
  const { beacons } = useBeacons();
  const zones = useZones(beacons);
  const { user } = useAuth();

  const [globalMessage, setGlobalMessage] = useState("");
  const [globalExit, setGlobalExit] = useState("");
  const [selectedZone, setSelectedZone] = useState("");
  const [processing, setProcessing] = useState(false);

  const isGlobalEvacuationActive = beacons.every(b => b.mode === "EVACUATION");

  const handleGlobalEvacuation = async () => {
    if (!user) return;
    setProcessing(true);

    try {
      if (isGlobalEvacuationActive) {
        await emergencyService.deactivateGlobalEvacuation(beacons, user.uid);
      } else {
        const message = globalMessage || "EVACUACIÓN EN CURSO. SIGA LAS FLECHAS.";
        const exit = globalExit || "EVACUATION_EXIT_DEFAULT";
        await emergencyService.activateGlobalEvacuation(beacons, user.uid, message, exit);
      }
    } catch (error) {
      console.error("Error:", error);
    } finally {
      setProcessing(false);
    }
  };

  const handleZoneEvacuation = async (zone: string, activate: boolean) => {
    if (!user) return;
    setProcessing(true);

    try {
      if (activate) {
        const message = globalMessage || "EVACUACIÓN EN CURSO. SIGA LAS FLECHAS.";
        const exit = globalExit || "EVACUATION_EXIT_DEFAULT";
        await emergencyService.activateZoneEvacuation(zone, beacons, user.uid, message, exit);
      } else {
        await emergencyService.deactivateZoneEvacuation(zone, beacons, user.uid);
      }
    } catch (error) {
      console.error("Error:", error);
    } finally {
      setProcessing(false);
    }
  };

  const handleFilteredEvacuation = async () => {
    if (!user || !selectedZone) return;
    setProcessing(true);

    try {
      const message = globalMessage || "EVACUACIÓN EN CURSO. SIGA LAS FLECHAS.";
      const exit = globalExit || "EVACUATION_EXIT_DEFAULT";
      await emergencyService.activateZoneEvacuation(selectedZone, beacons, user.uid, message, exit);
    } catch (error) {
      console.error("Error:", error);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-8 h-8 text-red-500" />
          <h1 className="text-2xl font-bold text-white">Emergencias & Evacuación</h1>
        </div>

        <div className="bg-red-900/20 border border-red-500 rounded-lg p-6">
          <div className="flex items-start gap-4">
            <Shield className="w-12 h-12 text-red-500 flex-shrink-0" />
            <div className="flex-1">
              <h2 className="text-xl font-bold text-white mb-2">Control Global de Evacuación</h2>
              <p className="text-gray-300 mb-4">
                Activar modo evacuación afectará a todas las balizas del sistema o solo a las de una zona específica.
              </p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Mensaje de Evacuación (opcional)
                  </label>
                  <input
                    type="text"
                    value={globalMessage}
                    onChange={(e) => setGlobalMessage(e.target.value)}
                    className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                    placeholder="Por defecto: EVACUACIÓN EN CURSO. SIGA LAS FLECHAS."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Salida de Evacuación (opcional)
                  </label>
                  <input
                    type="text"
                    value={globalExit}
                    onChange={(e) => setGlobalExit(e.target.value)}
                    className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                    placeholder="Ej: SALIDA 3"
                  />
                </div>
              </div>

              <div className="flex gap-4">
                <button
                  onClick={handleGlobalEvacuation}
                  disabled={processing}
                  className={`flex-1 flex items-center justify-center gap-2 py-4 font-bold rounded-lg transition-colors ${
                    isGlobalEvacuationActive
                      ? 'bg-green-600 hover:bg-green-700 text-white'
                      : 'bg-red-600 hover:bg-red-700 text-white'
                  } disabled:opacity-50`}
                >
                  <Power className="w-6 h-6" />
                  {isGlobalEvacuationActive ? 'Desactivar Evacuación Global' : 'Activar Evacuación Global'}
                </button>
              </div>

              {isGlobalEvacuationActive && (
                <div className="mt-4 p-4 bg-red-500/20 border border-red-500 rounded text-red-400 font-semibold text-center">
                  ⚠️ MODO EVACUACIÓN GLOBAL ACTIVO
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="bg-dark-800 rounded-lg p-6">
          <h2 className="text-xl font-bold text-white mb-4">Evacuación por Zona Seleccionada</h2>
          
          <div className="flex gap-4 mb-6">
            <select
              value={selectedZone}
              onChange={(e) => setSelectedZone(e.target.value)}
              className="flex-1 px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Seleccionar zona...</option>
              {zones.map(z => (
                <option key={z.zone} value={z.zone}>{z.zone}</option>
              ))}
            </select>

            <button
              onClick={handleFilteredEvacuation}
              disabled={!selectedZone || processing}
              className="px-6 py-2 bg-orange-600 hover:bg-orange-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded transition-colors"
            >
              Activar en Zona
            </button>
          </div>
        </div>

        <div className="bg-dark-800 rounded-lg p-6">
          <h2 className="text-xl font-bold text-white mb-4">Control por Zonas</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {zones.map(zone => {
              const isZoneEvacuating = zone.evacuationCount > 0;
              
              return (
                <div
                  key={zone.zone}
                  className={`p-4 rounded-lg border-2 ${
                    isZoneEvacuating
                      ? 'bg-red-900/20 border-red-500'
                      : 'bg-dark-700 border-dark-600'
                  }`}
                >
                  <h3 className="text-lg font-semibold text-white mb-2">{zone.zone}</h3>
                  
                  <div className="space-y-2 mb-4">
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-400">Total balizas:</span>
                      <span className="text-white font-semibold">{zone.totalBeacons}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-400">En evacuación:</span>
                      <span className={`font-semibold ${isZoneEvacuating ? 'text-red-400' : 'text-green-400'}`}>
                        {zone.evacuationCount}
                      </span>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    {isZoneEvacuating ? (
                      <button
                        onClick={() => handleZoneEvacuation(zone.zone, false)}
                        disabled={processing}
                        className="flex-1 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white text-sm font-semibold rounded transition-colors"
                      >
                        Volver a Normal
                      </button>
                    ) : (
                      <button
                        onClick={() => handleZoneEvacuation(zone.zone, true)}
                        disabled={processing}
                        className="flex-1 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 text-white text-sm font-semibold rounded transition-colors"
                      >
                        Poner en Evacuación
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="bg-dark-800 rounded-lg p-6">
          <h2 className="text-xl font-bold text-white mb-4">Mensajes por Idioma (Referencia)</h2>
          
          <div className="space-y-3">
            <div className="p-3 bg-dark-700 rounded">
              <span className="font-semibold text-blue-400">ES:</span>
              <span className="text-white ml-3">EVACUACIÓN EN CURSO. SIGA LAS FLECHAS.</span>
            </div>
            <div className="p-3 bg-dark-700 rounded">
              <span className="font-semibold text-blue-400">CAT:</span>
              <span className="text-white ml-3">EVACUACIÓ EN CURS. SEGUEIX LES FLETXES.</span>
            </div>
            <div className="p-3 bg-dark-700 rounded">
              <span className="font-semibold text-blue-400">EN:</span>
              <span className="text-white ml-3">EVACUATION IN PROGRESS. FOLLOW THE ARROWS.</span>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};
