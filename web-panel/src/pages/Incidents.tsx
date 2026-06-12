import React, { useEffect, useState } from "react";
import { Layout } from "../components/Layout";
import { api } from "../services/apiClient";
import { AlertTriangle, Plus, Filter } from "lucide-react";
import { useToast } from "../context/ToastContext";

export const Incidents: React.FC = () => {
    const [incidents, setIncidents] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const { showToast } = useToast();

    // Form state
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [level, setLevel] = useState("INFO");
    const [zone, setZone] = useState("");

    const fetchIncidents = async () => {
        try {
            const data = await api.getIncidents();
            setIncidents(data);
        } catch (error) {
            console.error("Error fetching incidents:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchIncidents();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await api.upsert("incidents", {
                category: title,
                description,
                level,
                zone: zone ? zone.toString() : null,
                status: "ACTIVE",
                created_at: new Date().toISOString().slice(0, 19).replace("T", " ")
            });
            showToast("Incidencia creada correctamente", "success");
            setShowModal(false);
            setTitle("");
            setDescription("");
            setLevel("INFO");
            setZone("");
            fetchIncidents();
        } catch (error) {
            console.error("Error creating incident:", error);
            showToast("Error al crear incidencia", "error");
        }
    };

    return (
        <Layout>
            <div className="space-y-6">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-bold text-white flex items-center gap-2">
                        <AlertTriangle className="w-6 h-6 text-yellow-500" />
                        Gestión de Incidencias
                    </h1>
                    <button
                        onClick={() => setShowModal(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                    >
                        <Plus className="w-4 h-4" />
                        Nueva Incidencia
                    </button>
                </div>

                {/* Filters (Visual only for now) */}
                <div className="bg-dark-800 p-4 rounded-lg border border-dark-700 flex gap-4">
                    <div className="relative flex-1">
                        <Filter className="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
                        <input
                            type="text"
                            placeholder="Filtrar por título..."
                            className="w-full pl-10 pr-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <select className="px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
                        <option value="ALL">Todos los niveles</option>
                        <option value="CRITICAL">Crítica</option>
                        <option value="WARNING">Advertencia</option>
                        <option value="INFO">Info</option>
                    </select>
                </div>

                {/* List */}
                <div className="bg-dark-800 rounded-lg border border-dark-700 overflow-hidden">
                    <table className="w-full">
                        <thead className="bg-dark-700">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Nivel</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Título</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Zona</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Estado</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">Fecha</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-dark-700">
                            {loading ? (
                                <tr>
                                    <td colSpan={5} className="px-6 py-4 text-center text-gray-400">Cargando incidencias...</td>
                                </tr>
                            ) : incidents.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="px-6 py-4 text-center text-gray-400">No hay incidencias registradas</td>
                                </tr>
                            ) : (
                                incidents.map((inc, idx) => (
                                    <tr key={idx} className="hover:bg-dark-700/50 transition-colors">
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`px-2 py-1 text-xs rounded-full ${inc.level === 'CRITICAL' ? 'bg-red-500/20 text-red-400' :
                                                inc.level === 'WARNING' ? 'bg-yellow-500/20 text-yellow-400' :
                                                    'bg-blue-500/20 text-blue-400'
                                                }`}>
                                                {inc.level || 'INFO'}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="text-sm font-medium text-white">{inc.category}</div>
                                            <div className="text-sm text-gray-400">{inc.description}</div>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-300">
                                            {inc.zone ? `Zona ${inc.zone}` : '-'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`text-xs font-medium ${inc.status === 'ACTIVE' ? 'text-green-400' : 'text-gray-500'
                                                }`}>
                                                {inc.status || 'ACTIVE'}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                                            {inc.created_at ? new Date(inc.created_at).toLocaleString() : '-'}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-dark-800 rounded-lg border border-dark-700 w-full max-w-md p-6">
                        <h2 className="text-xl font-bold text-white mb-4">Nueva Incidencia</h2>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Título</label>
                                <input
                                    type="text"
                                    required
                                    value={title}
                                    onChange={e => setTitle(e.target.value)}
                                    className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
                                <textarea
                                    required
                                    value={description}
                                    onChange={e => setDescription(e.target.value)}
                                    className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    rows={3}
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Nivel</label>
                                    <select
                                        value={level}
                                        onChange={e => setLevel(e.target.value)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="INFO">Info</option>
                                        <option value="WARNING">Advertencia</option>
                                        <option value="CRITICAL">Crítica</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Zona ID (Opcional)</label>
                                    <input
                                        type="number"
                                        value={zone}
                                        onChange={e => setZone(e.target.value)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>
                            <div className="flex justify-end gap-3 mt-6">
                                <button
                                    type="button"
                                    onClick={() => setShowModal(false)}
                                    className="px-4 py-2 text-gray-300 hover:text-white transition-colors"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                                >
                                    Crear Incidencia
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    );
};
