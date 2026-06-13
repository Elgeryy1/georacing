import React from "react";
import { Link, useLocation } from "react-router-dom";
import { LogOut, Radio, Bell, MapPin, AlertTriangle, Settings, Activity, Flag, Terminal, ShoppingBag, Package, UtensilsCrossed, Newspaper, Users } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useNewBeaconDetection } from "../hooks/useNewBeaconDetection";

import { useCircuitState } from "../hooks/useCircuitState"; // Assuming hook created
import { EvacuationModal } from "./EvacuationModal"; // Assuming component created
import { ShieldAlert } from "lucide-react";

interface LayoutProps {
  children: React.ReactNode;
  onNewBeaconClick?: () => void;
}

export const Layout: React.FC<LayoutProps> = ({ children, onNewBeaconClick }) => {
  const { user, logout } = useAuth();
  const { hasNewBeacons, newBeacons } = useNewBeaconDetection();
  const { state: circuitState } = useCircuitState(); // Poll global state
  const location = useLocation();
  const [showEvacModal, setShowEvacModal] = React.useState(false);

  const isEvacuation = circuitState?.global_mode === 'EVACUATION';

  const navItems = [
    { path: "/dashboard", label: "Dashboard", icon: Activity },
    { path: "/users", label: "Usuarios", icon: Users },
    { path: "/orders", label: "Pedidos", icon: ShoppingBag },
    { path: "/products", label: "Productos", icon: Package },
    { path: "/food-stands", label: "Food Stands", icon: UtensilsCrossed },
    { path: "/news", label: "Noticias", icon: Newspaper },
    { path: "/beacons", label: "Balizas", icon: Radio },
    { path: "/zones", label: "Mapa", icon: MapPin },
    { path: "/circuit-state", label: "Estado Circuito", icon: Flag },
    { path: "/incidents", label: "Incidencias", icon: AlertTriangle, badge: true },
    { path: "/logs", label: "Logs", icon: Terminal },
    { path: "/config", label: "Configuración", icon: Settings }
  ];

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + "/");
  };

  return (
    <div className={`min-h-screen flex flex-col transition-colors duration-500 ${isEvacuation ? 'bg-red-950' : 'bg-dark-900'}`}>

      {/* Evacuation Modal */}
      <EvacuationModal
        isOpen={showEvacModal}
        onClose={() => setShowEvacModal(false)}
        isEvacuationActive={isEvacuation}
      />

      {/* GLOBAL EMERGENCY ALERT BANNER */}
      {isEvacuation && (
        <div className="bg-red-600 animate-pulse px-6 py-2 flex items-center justify-center gap-3 shadow-lg z-50">
          <ShieldAlert className="text-white w-6 h-6" />
          <span className="text-white font-black tracking-widest uppercase">⚠️ MODO EVACUACIÓN ACTIVO - PROTOCOLO DE EMERGENCIA EN CURSO ⚠️</span>
        </div>
      )}

      {/* Header */}
      <header className={`border-b transition-colors ${isEvacuation ? 'bg-red-900/50 border-red-700' : 'bg-dark-800 border-dark-700'}`}>
        <div className="px-6 py-4">
          <div className="flex items-center justify-between">
            {/* Logo y Título */}
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <Radio className={`w-8 h-8 ${isEvacuation ? 'text-white' : 'text-blue-500'}`} />
                <div>
                  <h1 className="text-xl font-bold text-white">PANEL DE CONTROL</h1>
                  <p className={`text-xs ${isEvacuation ? 'text-red-200' : 'text-gray-400'}`}>Sistema de Gestión Inteligente METROPOLIS</p>
                </div>
              </div>
            </div>

            {/* Indicadores de Estado y Usuario */}
            <div className="flex items-center gap-4">

              {/* EMERGENCY BUTTON */}
              <button
                onClick={() => setShowEvacModal(true)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg font-bold transition-all shadow-lg hover:scale-105 ${isEvacuation
                  ? 'bg-white text-red-600 hover:bg-gray-100 ring-4 ring-red-500/30'
                  : 'bg-red-500/10 text-red-500 border border-red-500/50 hover:bg-red-500 hover:text-white'
                  }`}
              >
                <AlertTriangle className="w-5 h-5" />
                {isEvacuation ? "GESTIONAR EMERGENCIA" : "EMERGENCIA"}
              </button>

              <div className="h-6 w-px bg-gray-700 mx-2"></div>

              {/* Indicadores de Estado */}
              <div className="flex items-center gap-3 text-xs hidden md:flex">
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-green-500/10 text-green-400 border border-green-500/30 rounded">
                  <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                  <span className="font-medium">Soleado</span>
                </div>
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-green-500/10 text-green-400 border border-green-500/30 rounded">
                  <span className="font-medium">⚡ ELECTRICIDAD ON</span>
                </div>
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-green-500/10 text-green-400 border border-green-500/30 rounded">
                  <span className="font-medium">📡 IA ACTIVA</span>
                </div>
              </div>

              {hasNewBeacons && (
                <button
                  onClick={onNewBeaconClick}
                  className="relative flex items-center gap-2 px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-white rounded-lg transition-colors animate-pulse"
                >
                  <Bell className="w-4 h-4" />
                  <span className="font-semibold">
                    {newBeacons.length} {newBeacons.length === 1 ? 'Baliza Nueva' : 'Balizas Nuevas'}
                  </span>
                  <span className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full"></span>
                </button>
              )}

              <span className="text-sm text-gray-400">{user?.email}</span>
              <button
                onClick={logout}
                className="flex items-center gap-2 px-4 py-2 bg-dark-700 hover:bg-dark-600 text-white rounded-lg transition-colors"
              >
                <LogOut className="w-4 h-4" />
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>

        {/* Navegación con tabs */}
        <div className="px-6">
          <nav className="flex items-center gap-1 -mb-px">
            {navItems.map(({ path, label, icon: Icon, badge }) => (
              <Link
                key={path}
                to={path}
                className={`
                  flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all relative
                  ${isActive(path)
                    ? "text-white border-b-2 border-white"
                    : "text-gray-400 hover:text-gray-300"
                  }
                `}
              >
                <Icon className="w-4 h-4" />
                <span>{label}</span>
                {badge && path === "/emergencies" && (
                  <span className="ml-1 px-2 py-0.5 bg-red-500 text-white text-xs rounded-full">
                    6
                  </span>
                )}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 overflow-auto p-6">
        {children}
      </main>
    </div>
  );
};
