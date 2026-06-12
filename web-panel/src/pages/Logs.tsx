import React, { useEffect, useState } from "react";
import { Layout } from "../components/Layout";
import { api } from "../services/apiClient";
import { Command } from "../types";
import { Terminal, RefreshCw, Play } from "lucide-react";
import { useToast } from "../context/ToastContext";

export const Logs: React.FC = () => {
    const [commands, setCommands] = useState<Command[]>([]);
    const [loading, setLoading] = useState(true);
    const { showToast } = useToast();

    const fetchCommands = async () => {
        setLoading(true);
        try {
            const data = await api.getCommands();
            setCommands(data);
        } catch (error) {
            console.error("Error fetching commands:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCommands();
    }, []);

    const handleExecute = async (id: number) => {
        try {
            await api.upsert("commands", {
                id,
                status: "EXECUTED",
                executed_at: new Date().toISOString().slice(0, 19).replace("T", " ")
            });
            showToast("Comando ejecutado forzosamente", "success");
            fetchCommands();
        } catch (error) {
            console.error("Error executing command:", error);
            showToast("Error al ejecutar comando", "error");
        }
    };

    return (
        <Layout>
            <div className="space-y-6">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-bold text-white flex items-center gap-2">
                        <Terminal className="w-6 h-6 text-blue-500" />
                        Logs de Comandos
                    </h1>
                    <button
                        onClick={fetchCommands}
                        className="p-2 bg-dark-700 hover:bg-dark-600 text-white rounded-lg transition-colors"
                        title="Recargar"
                    >
                        <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
                    </button>
                </div>

                <div className="bg-dark-800 rounded-lg border border-dark-700 overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-dark-700">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">ID</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Baliza</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Tipo</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Payload</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Estado</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Creado</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Acciones</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-dark-700">
                                {commands.length === 0 ? (
                                    <tr>
                                        <td colSpan={7} className="px-6 py-4 text-center text-gray-400">No hay comandos registrados</td>
                                    </tr>
                                ) : (
                                    commands.map((cmd) => (
                                        <tr key={cmd.id} className="hover:bg-dark-700/50 transition-colors">
                                            <td className="px-6 py-4 text-sm text-gray-500 font-mono">#{cmd.id}</td>
                                            <td className="px-6 py-4 text-sm text-white font-mono">{cmd.beacon_uid}</td>
                                            <td className="px-6 py-4">
                                                <span className="text-xs font-mono text-blue-400 bg-blue-400/10 px-1.5 py-0.5 rounded">
                                                    {cmd.command}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-xs text-gray-400 font-mono max-w-xs truncate">
                                                {typeof cmd.value === 'string' ? cmd.value : JSON.stringify(cmd.value)}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className={`text-xs px-2 py-0.5 rounded-full ${cmd.status === 'EXECUTED' ? 'bg-green-500/20 text-green-400' :
                                                    cmd.status === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400' :
                                                        'bg-red-500/20 text-red-400'
                                                    }`}>
                                                    {cmd.status}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-400">
                                                {new Date(cmd.created_at).toLocaleString()}
                                            </td>
                                            <td className="px-6 py-4">
                                                {cmd.status === 'PENDING' && (
                                                    <button
                                                        onClick={() => handleExecute(cmd.id)}
                                                        className="text-blue-400 hover:text-blue-300 transition-colors"
                                                        title="Forzar ejecución"
                                                    >
                                                        <Play className="w-4 h-4" />
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </Layout>
    );
};
