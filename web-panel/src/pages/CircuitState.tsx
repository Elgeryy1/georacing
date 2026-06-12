import React, { useEffect, useState } from "react";
import { Layout } from "../components/Layout";
import { api } from "../services/apiClient";
import { Activity, ShieldAlert, Flag, Zap } from "lucide-react";
import { useToast } from "../context/ToastContext";

export const CircuitState: React.FC = () => {
    const [state, setState] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const { showToast } = useToast();

    const fetchState = async () => {
        try {
            const data = await api.getCircuitState();
            setState(data);
        } catch (error) {
            console.error("Error fetching circuit state:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchState();
        const interval = setInterval(fetchState, 5000);
        return () => clearInterval(interval);
    }, []);

    const handleModeChange = async (newMode: string) => {
        try {
            await api.setCircuitState(newMode);
            showToast(`Estado del circuito cambiado a ${newMode}`, "success");
            fetchState();
        } catch (error) {
            console.error("Error updating circuit state:", error);
            showToast("Error al actualizar estado", "error");
        }
    };

    if (loading) {
        return (
            <Layout>
                <div className="text-white text-center">Cargando estado del circuito...</div>
            </Layout>
        );
    }

    return (
        <Layout>
            <div className="space-y-8">
                <h1 className="text-2xl font-bold text-white flex items-center gap-2">
                    <Activity className="w-6 h-6 text-green-500" />
                    Estado del Circuito
                </h1>

                {/* Current State Card */}
                <div className="bg-dark-800 p-8 rounded-lg border border-dark-700 text-center">
                    <h2 className="text-gray-400 text-lg mb-4">ESTADO ACTUAL</h2>
                    <div className="text-6xl font-black text-white tracking-wider mb-4">
                        {state?.global_mode || state?.mode || "NORMAL"}
                    </div>
                    <p className="text-gray-500">
                        Última actualización: {state?.updated_at ? new Date(state.updated_at).toLocaleString() : "Desconocido"}
                    </p>
                </div>

                {/* Controls */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <button
                        onClick={() => handleModeChange("NORMAL")}
                        className={`p-6 rounded-lg border-2 transition-all flex flex-col items-center gap-4 ${state?.global_mode === "NORMAL"
                            ? "bg-green-500/20 border-green-500 text-green-400"
                            : "bg-dark-800 border-dark-700 text-gray-400 hover:border-green-500/50"
                            }`}
                    >
                        <Flag className="w-12 h-12" />
                        <span className="text-xl font-bold">NORMAL</span>
                        <span className="text-sm opacity-70">Carrera en curso</span>
                    </button>

                    <button
                        onClick={() => handleModeChange("SAFETY_CAR")}
                        className={`p-6 rounded-lg border-2 transition-all flex flex-col items-center gap-4 ${state?.global_mode === "SAFETY_CAR"
                            ? "bg-yellow-500/20 border-yellow-500 text-yellow-400"
                            : "bg-dark-800 border-dark-700 text-gray-400 hover:border-yellow-500/50"
                            }`}
                    >
                        <ShieldAlert className="w-12 h-12" />
                        <span className="text-xl font-bold">SAFETY CAR</span>
                        <span className="text-sm opacity-70">Pista neutralizada</span>
                    </button>

                    <button
                        onClick={() => handleModeChange("RED_FLAG")}
                        className={`p-6 rounded-lg border-2 transition-all flex flex-col items-center gap-4 ${state?.global_mode === "RED_FLAG"
                            ? "bg-red-500/20 border-red-500 text-red-400"
                            : "bg-dark-800 border-dark-700 text-gray-400 hover:border-red-500/50"
                            }`}
                    >
                        <Zap className="w-12 h-12" />
                        <span className="text-xl font-bold">RED FLAG</span>
                        <span className="text-sm opacity-70">Carrera detenida</span>
                    </button>
                </div>

                <div className="bg-dark-800 p-6 rounded-lg border border-dark-700">
                    <h3 className="text-lg font-bold text-white mb-4">Información Adicional (En Vivo)</h3>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                        <div className="flex justify-between p-3 bg-dark-700 rounded">
                            <span className="text-gray-400">Temperatura Pista</span>
                            <span className="text-white font-mono text-yellow-400">{state?.temperature || "--"}</span>
                        </div>
                        <div className="flex justify-between p-3 bg-dark-700 rounded">
                            <span className="text-gray-400">Humedad</span>
                            <span className="text-white font-mono text-blue-400">{state?.humidity || "--"}</span>
                        </div>
                        <div className="flex justify-between p-3 bg-dark-700 rounded">
                            <span className="text-gray-400">Viento</span>
                            <span className="text-white font-mono">{state?.wind || "--"}</span>
                        </div>
                        <div className="flex justify-between p-3 bg-dark-700 rounded">
                            <span className="text-gray-400">Previsión</span>
                            <span className="text-white font-mono">{state?.forecast || "--"}</span>
                        </div>
                    </div>
                </div>
            </div>
        </Layout>
    );
};
