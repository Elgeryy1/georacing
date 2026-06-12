import React, { useEffect, useState } from "react";
import { Layout } from "../components/Layout";
import { api } from "../services/apiClient";
import { Beacon, Command } from "../types";
import { Activity, Radio, AlertTriangle, Terminal, CheckCircle, XCircle, Clock } from "lucide-react";
import { Link } from "react-router-dom";

export const Dashboard: React.FC = () => {
  const [beacons, setBeacons] = useState<Beacon[]>([]);
  const [incidents, setIncidents] = useState<any[]>([]);
  const [commands, setCommands] = useState<Command[]>([]);
  const [circuitState, setCircuitState] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [beaconsData, incidentsData, commandsData, stateData] = await Promise.all([
        api.getBeacons(),
        api.getIncidents(),
        api.getCommands(),
        api.getCircuitState().catch(() => null) // Handle if state endpoint fails initially
      ]);

      setBeacons(beaconsData);
      setIncidents(incidentsData);
      setCommands(commandsData.slice(0, 5)); // Show only last 5
      setCircuitState(stateData);
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  const onlineCount = beacons.filter(b => b.online).length;
  const offlineCount = beacons.length - onlineCount;
  const activeIncidents = incidents.filter(i => i.status !== 'RESOLVED').length;

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-full text-white">
          <Activity className="w-8 h-8 animate-spin text-blue-500" />
          <span className="ml-2">Cargando Race Control...</span>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Activity className="w-6 h-6 text-blue-500" />
          RACE CONTROL DASHBOARD
        </h1>

        {/* Top Metrics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Circuit Status */}
          <div className="bg-dark-800 p-4 rounded-lg border border-dark-700">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-gray-400 text-sm font-medium">ESTADO CIRCUITO</h3>
              <Activity className="w-4 h-4 text-green-500" />
            </div>
            <div className="text-2xl font-bold text-white">
              {circuitState?.mode || "NORMAL"}
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Última act: {circuitState?.updated_at ? new Date(circuitState.updated_at).toLocaleTimeString() : "-"}
            </p>
          </div>

          {/* Beacons Status */}
          <div className="bg-dark-800 p-4 rounded-lg border border-dark-700">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-gray-400 text-sm font-medium">BALIZAS</h3>
              <Radio className="w-4 h-4 text-blue-500" />
            </div>
            <div className="flex items-end gap-2">
              <span className="text-2xl font-bold text-white">{beacons.length}</span>
              <span className="text-sm text-gray-400 mb-1">Total</span>
            </div>
            <div className="flex gap-2 mt-2 text-xs">
              <span className="text-green-400 flex items-center gap-1">
                <CheckCircle className="w-3 h-3" /> {onlineCount} Online
              </span>
              <span className="text-red-400 flex items-center gap-1">
                <XCircle className="w-3 h-3" /> {offlineCount} Offline
              </span>
            </div>
          </div>

          {/* Active Incidents */}
          <div className="bg-dark-800 p-4 rounded-lg border border-dark-700">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-gray-400 text-sm font-medium">INCIDENCIAS</h3>
              <AlertTriangle className="w-4 h-4 text-yellow-500" />
            </div>
            <div className="text-2xl font-bold text-white">
              {activeIncidents}
            </div>
            <p className="text-xs text-gray-500 mt-1">Activas ahora</p>
          </div>

          {/* System Health */}
          <div className="bg-dark-800 p-4 rounded-lg border border-dark-700">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-gray-400 text-sm font-medium">SISTEMA</h3>
              <Clock className="w-4 h-4 text-purple-500" />
            </div>
            <div className="text-2xl font-bold text-white">ONLINE</div>
            <p className="text-xs text-gray-500 mt-1">API Conectada</p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Commands */}
          <div className="bg-dark-800 rounded-lg border border-dark-700 overflow-hidden">
            <div className="p-4 border-b border-dark-700 flex justify-between items-center">
              <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                <Terminal className="w-5 h-5 text-gray-400" />
                Últimos Comandos
              </h3>
              <Link to="/logs" className="text-xs text-blue-400 hover:text-blue-300">Ver todo</Link>
            </div>
            <div className="divide-y divide-dark-700">
              {commands.length === 0 ? (
                <div className="p-4 text-gray-500 text-center text-sm">No hay comandos recientes</div>
              ) : (
                commands.map(cmd => (
                  <div key={cmd.id} className="p-3 hover:bg-dark-700/50 transition-colors">
                    <div className="flex justify-between items-start">
                      <div>
                        <span className="text-xs font-mono text-blue-400 bg-blue-400/10 px-1.5 py-0.5 rounded">
                          {cmd.command}
                        </span>
                        <p className="text-sm text-gray-300 mt-1">
                          Baliza: <span className="text-white">{cmd.beacon_uid}</span>
                        </p>
                      </div>
                      <div className="text-right">
                        <span className={`text-xs px-2 py-0.5 rounded-full ${cmd.status === 'EXECUTED' ? 'bg-green-500/20 text-green-400' :
                          cmd.status === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400' :
                            'bg-red-500/20 text-red-400'
                          }`}>
                          {cmd.status}
                        </span>
                        <p className="text-xs text-gray-500 mt-1">
                          {new Date(cmd.created_at).toLocaleTimeString()}
                        </p>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Recent Incidents / Activity */}
          <div className="bg-dark-800 rounded-lg border border-dark-700 overflow-hidden">
            <div className="p-4 border-b border-dark-700 flex justify-between items-center">
              <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-gray-400" />
                Últimas Incidencias
              </h3>
              <Link to="/incidents" className="text-xs text-blue-400 hover:text-blue-300">Ver todo</Link>
            </div>
            <div className="divide-y divide-dark-700">
              {incidents.length === 0 ? (
                <div className="p-4 text-gray-500 text-center text-sm">No hay incidencias recientes</div>
              ) : (
                incidents.slice(0, 5).map((inc, idx) => (
                  <div key={idx} className="p-3 hover:bg-dark-700/50 transition-colors">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="text-sm font-medium text-white">{inc.title}</h4>
                        <p className="text-xs text-gray-400 mt-0.5">{inc.description}</p>
                      </div>
                      <span className={`text-xs px-2 py-0.5 rounded-full ${inc.level === 'CRITICAL' ? 'bg-red-500/20 text-red-400' :
                        inc.level === 'WARNING' ? 'bg-yellow-500/20 text-yellow-400' :
                          'bg-blue-500/20 text-blue-400'
                        }`}>
                        {inc.level}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};
