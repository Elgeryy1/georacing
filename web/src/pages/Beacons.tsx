import React, { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { Layout } from "../components/Layout";
import { NewBeaconModal } from "../components/NewBeaconModal";
import { BeaconEditModal } from "../components/BeaconEditModal";
import { BeaconMetricsCard } from "../components/BeaconMetricsCard";
import { useBeacons } from "../hooks/useBeacons";
import { useNewBeaconDetection } from "../hooks/useNewBeaconDetection";
import { beaconsService } from "../services/beaconService";
import { BeaconUpdate, BeaconMode, ArrowDirection, Language, Beacon } from "../types";
import { getModeColor, BeaconModeStrict } from "../utils/beaconHelpers";
import { Search, Circle, Edit, LayoutGrid, List, RotateCcw } from "lucide-react";
import { useToast } from "../context/ToastContext";

export const Beacons: React.FC = () => {
    const { beacons, loading, refresh } = useBeacons();
    const { newBeacons, clearNewBeacons } = useNewBeaconDetection();
    const { showToast } = useToast();
    const [showNewBeaconModal, setShowNewBeaconModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [selectedBeaconForEdit, setSelectedBeaconForEdit] = useState<Beacon | null>(null);
    const [selectedNewBeacon, setSelectedNewBeacon] = useState<number>(0);
    const [selectedBeacons, setSelectedBeacons] = useState<Set<string>>(new Set());
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [filterZone, setFilterZone] = useState<string>("");
    const [filterMode, setFilterMode] = useState<BeaconModeStrict | "all">("all");
    const [onlineOnly, setOnlineOnly] = useState(false);
    const [viewMode, setViewMode] = useState<'table' | 'cards'>('cards');

    const [bulkMode, setBulkMode] = useState<BeaconMode>("NORMAL");
    const [bulkArrow, setBulkArrow] = useState<ArrowDirection>("NONE");
    const [bulkMessage, setBulkMessage] = useState("");
    const [bulkColor, setBulkColor] = useState("#00FFAA");
    const [bulkBrightness, setBulkBrightness] = useState(90);
    const [bulkLanguage, setBulkLanguage] = useState<Language>("ES");
    const [bulkEvacuationExit, setBulkEvacuationExit] = useState("");

    const zones = useMemo(() => {
        return Array.from(new Set(beacons.map(b => b.zone))).sort();
    }, [beacons]);

    const filteredBeacons = useMemo(() => {
        return beacons.filter(beacon => {
            // Búsqueda por beaconId o name
            if (searchTerm) {
                const searchLower = searchTerm.toLowerCase();
                const matchesId = beacon.beaconId.toLowerCase().includes(searchLower);
                const matchesName = beacon.name?.toLowerCase().includes(searchLower);
                if (!matchesId && !matchesName) {
                    return false;
                }
            }
            // Filtro por zona
            if (filterZone && beacon.zone !== filterZone) {
                return false;
            }
            // Filtro por modo (solo si no es "all" y el modo no es null)
            if (filterMode && filterMode !== "all") {
                if (!beacon.mode || beacon.mode !== (filterMode as BeaconMode)) {
                    return false;
                }
            }
            // Filtro solo online
            if (onlineOnly && !beacon.online) {
                return false;
            }
            return true;
        });
    }, [beacons, searchTerm, filterZone, filterMode, onlineOnly]);

    const toggleBeaconSelection = (beaconId: string) => {
        const newSelected = new Set(selectedBeacons);
        if (newSelected.has(beaconId)) {
            newSelected.delete(beaconId);
        } else {
            newSelected.add(beaconId);
        }
        setSelectedBeacons(newSelected);
    };

    const toggleSelectAll = () => {
        if (selectedBeacons.size === filteredBeacons.length) {
            setSelectedBeacons(new Set());
        } else {
            setSelectedBeacons(new Set(filteredBeacons.map(b => b.beaconId)));
        }
    };

    const handleApplyBulkUpdate = async () => {
        if (selectedBeacons.size === 0) return;

        const updates: BeaconUpdate = {
            mode: bulkMode,
            arrow: bulkArrow,
            message: bulkMessage || undefined,
            color: bulkColor,
            brightness: bulkBrightness,
            language: bulkLanguage,
            evacuationExit: bulkEvacuationExit || undefined
        };

        try {
            showToast(`Enviando configuración a ${selectedBeacons.size} balizas...`, "info");
            await beaconsService.updateMultipleBeacons(Array.from(selectedBeacons), updates);
            showToast(`✅ Configuración aplicada a ${selectedBeacons.size} balizas`, "success");
            setSelectedBeacons(new Set());
            await refresh();
        } catch (error) {
            console.error("Error aplicando configuración masiva:", error);
            showToast("❌ Error al enviar la configuración", "error");
        }
    };

    const handleRestartAll = async () => {
        if (!confirm(`⚠️⚠️⚠️ ¿Seguro que quieres REINICIAR TODOS LOS SISTEMAS WINDOWS?\n\n🔴 ESTO REINICIARÁ ${beacons.length} ORDENADORES COMPLETOS.\n\nTodos los sistemas se apagarán y volverán a encender.\n\n¿CONTINUAR?`)) {
            return;
        }

        try {
            showToast(`Enviando comandos de reinicio a ${beacons.length} balizas...`, "warning");
            const count = await beaconsService.restartAllBeacons();
            showToast(`✅ Comando de reinicio enviado a ${count} balizas`, "success");
        } catch (error) {
            console.error("Error reiniciando balizas:", error);
            showToast("❌ Error al enviar comandos de reinicio", "error");
        }
    };

    const handleNewBeaconClick = () => {
        if (newBeacons.length > 0) {
            setSelectedNewBeacon(0);
            setShowNewBeaconModal(true);
        }
    };

    const handleConfigured = () => {
        if (selectedNewBeacon < newBeacons.length - 1) {
            setSelectedNewBeacon(selectedNewBeacon + 1);
        } else {
            setShowNewBeaconModal(false);
            clearNewBeacons();
        }
    };

    if (loading) {
        return (
            <Layout>
                <div className="text-white text-center">Cargando balizas...</div>
            </Layout>
        );
    }

    return (
        <Layout onNewBeaconClick={handleNewBeaconClick}>
            {showNewBeaconModal && newBeacons[selectedNewBeacon] && (
                <NewBeaconModal
                    beacon={newBeacons[selectedNewBeacon]}
                    onClose={() => {
                        setShowNewBeaconModal(false);
                        clearNewBeacons();
                    }}
                    onConfigured={handleConfigured}
                />
            )}
            <div className="space-y-6">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-bold text-white">Gestión de Balizas</h1>
                    <div className="flex items-center gap-4">
                        <button
                            onClick={handleRestartAll}
                            className="flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white font-semibold rounded-lg transition-colors"
                            title="Reiniciar todos los sistemas Windows de las balizas"
                        >
                            <RotateCcw className="w-5 h-5" />
                            Reiniciar Todos (Windows)
                        </button>
                        <div className="text-sm text-gray-400">
                            Total: {beacons.length} | Online: {beacons.filter(b => b.online).length}
                        </div>
                        <div className="flex bg-dark-700 rounded-lg p-1">
                            <button
                                onClick={() => setViewMode('cards')}
                                className={`p-2 rounded transition-colors ${viewMode === 'cards' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'
                                    }`}
                                title="Vista de tarjetas"
                            >
                                <LayoutGrid className="w-5 h-5" />
                            </button>
                            <button
                                onClick={() => setViewMode('table')}
                                className={`p-2 rounded transition-colors ${viewMode === 'table' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'
                                    }`}
                                title="Vista de tabla"
                            >
                                <List className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                </div>

                <div className="bg-dark-800 p-4 rounded-lg space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div className="relative">
                            <Search className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                            <input
                                type="text"
                                value={searchTerm}
                                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
                                placeholder="Buscar baliza por nombre o ID"
                                className="w-full pl-10 pr-4 py-2 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <select
                            value={filterZone}
                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setFilterZone(e.target.value)}
                            className="px-4 py-2 bg-dark-700 border border-dark-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="">Todas las zonas</option>
                            {zones.map(zone => (
                                <option key={zone ?? ""} value={zone ?? ""}>{zone ?? "Sin zona"}</option>
                            ))}
                        </select>

                        <select
                            value={filterMode}
                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setFilterMode(e.target.value as BeaconModeStrict | "all")}
                            className="px-4 py-2 bg-dark-700 border border-dark-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="all">Todos los modos</option>
                            <option value="NORMAL">Normal</option>
                            <option value="EMERGENCY">Emergencia</option>
                            <option value="EVACUATION">Evacuación</option>
                            <option value="MAINTENANCE">Mantenimiento</option>
                            <option value="CONGESTION">Congestión</option>
                            <option value="UNCONFIGURED">Sin configurar</option>
                        </select>

                        <label className="flex items-center gap-2 px-4 py-2 bg-dark-700 border border-dark-600 rounded-lg text-white cursor-pointer">
                            <input
                                type="checkbox"
                                checked={onlineOnly}
                                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setOnlineOnly(e.target.checked)}
                                className="w-4 h-4"
                            />
                            <span>Solo online</span>
                        </label>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div className="lg:col-span-2">
                        {viewMode === 'cards' ? (
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                                {Array.isArray(filteredBeacons) ? filteredBeacons.map(beacon => (
                                    <div key={beacon.beaconId} className="relative">
                                        <BeaconMetricsCard
                                            beacon={beacon}
                                            selected={selectedBeacons.has(beacon.beaconId)}
                                            onSelect={() => toggleBeaconSelection(beacon.beaconId)}
                                            onClick={() => {
                                                setSelectedBeaconForEdit(beacon);
                                                setShowEditModal(true);
                                            }}
                                        />
                                    </div>
                                )) : null}
                            </div>
                        ) : (
                            <div className="bg-dark-800 rounded-lg overflow-hidden">
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead className="bg-dark-700">
                                            <tr>
                                                <th className="px-4 py-3 text-left">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedBeacons.size === filteredBeacons.length && filteredBeacons.length > 0}
                                                        onChange={toggleSelectAll}
                                                        className="w-4 h-4 rounded border-gray-600 bg-dark-700 text-blue-600 focus:ring-blue-500 focus:ring-offset-dark-800"
                                                    />
                                                </th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Estado</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Baliza ID</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Zona</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Modo</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Flecha</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Mensaje</th>
                                                <th className="px-4 py-3 text-left text-sm font-semibold text-gray-300">Acciones</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-dark-700">
                                            {filteredBeacons.map(beacon => (
                                                <tr key={beacon.beaconId} className="hover:bg-dark-700/50">
                                                    <td className="px-4 py-3">
                                                        <input
                                                            type="checkbox"
                                                            checked={selectedBeacons.has(beacon.beaconId)}
                                                            onChange={() => toggleBeaconSelection(beacon.beaconId)}
                                                            className="w-4 h-4 rounded border-gray-600 bg-dark-700 text-blue-600 focus:ring-blue-500 focus:ring-offset-dark-800"
                                                        />
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        <Circle
                                                            className={`w-3 h-3 ${beacon.online ? 'text-green-500 fill-green-500' : 'text-red-500 fill-red-500'}`}
                                                        />
                                                    </td>
                                                    <td className="px-4 py-3 text-sm text-white font-mono">{beacon.beaconId}</td>
                                                    <td className="px-4 py-3 text-sm text-gray-300">{beacon.zone}</td>
                                                    <td className="px-4 py-3">
                                                        <span className={`px-2 py-1 text-xs rounded-full ${beacon.mode ? getModeColor(beacon.mode as BeaconModeStrict) : "bg-gray-500/20 text-gray-400"}`}>
                                                            {beacon.mode ?? "Modo desconocido"}
                                                        </span>
                                                    </td>
                                                    <td className="px-4 py-3 text-sm text-gray-300">
                                                        {beacon.arrow === "UP" && "↑ Arriba"}
                                                        {beacon.arrow === "DOWN" && "↓ Abajo"}
                                                        {beacon.arrow === "LEFT" && "← Izquierda"}
                                                        {beacon.arrow === "RIGHT" && "→ Derecha"}
                                                        {beacon.arrow === "UP_LEFT" && "↖ Arriba-Izq"}
                                                        {beacon.arrow === "UP_RIGHT" && "↗ Arriba-Der"}
                                                        {beacon.arrow === "DOWN_LEFT" && "↙ Abajo-Izq"}
                                                        {beacon.arrow === "DOWN_RIGHT" && "↘ Abajo-Der"}
                                                        {(!beacon.arrow || beacon.arrow === "NONE") && "-"}
                                                    </td>
                                                    <td className="px-4 py-3 text-sm text-gray-300 max-w-xs truncate">{beacon.message}</td>
                                                    <td className="px-4 py-3">
                                                        <Link
                                                            to={`/beacons/${beacon.beaconId}`}
                                                            className="inline-flex items-center gap-1 px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded transition-colors"
                                                        >
                                                            <Edit className="w-3 h-3" />
                                                            Editar
                                                        </Link>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="bg-dark-800 p-4 rounded-lg h-fit space-y-4 sticky top-4">
                        <div className="flex items-center justify-between">
                            <h3 className="text-lg font-semibold text-white">
                                Acciones Masivas
                            </h3>
                            {selectedBeacons.size > 0 && (
                                <span className="px-2 py-1 bg-blue-600 text-white text-xs rounded-full">
                                    {selectedBeacons.size} seleccionadas
                                </span>
                            )}
                        </div>

                        {selectedBeacons.size > 0 ? (
                            <div className="space-y-3">
                                {viewMode === 'cards' && (
                                    <button
                                        onClick={() => setSelectedBeacons(new Set())}
                                        className="w-full py-1 text-sm text-gray-400 hover:text-white border border-gray-600 rounded mb-2"
                                    >
                                        Deseleccionar todas
                                    </button>
                                )}

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Modo</label>
                                    <select
                                        value={bulkMode}
                                        onChange={(e) => setBulkMode(e.target.value as BeaconMode)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="NORMAL">Normal</option>
                                        <option value="CONGESTION">Congestión</option>
                                        <option value="EMERGENCY">Emergencia</option>
                                        <option value="EVACUATION">Evacuación</option>
                                        <option value="MAINTENANCE">Mantenimiento</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Flecha</label>
                                    <select
                                        value={bulkArrow}
                                        onChange={(e) => setBulkArrow(e.target.value as ArrowDirection)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="NONE">Ninguna</option>
                                        <option value="LEFT">Izquierda</option>
                                        <option value="RIGHT">Derecha</option>
                                        <option value="UP">Arriba</option>
                                        <option value="DOWN">Abajo</option>
                                        <option value="UP_LEFT">Arriba Izquierda</option>
                                        <option value="UP_RIGHT">Arriba Derecha</option>
                                        <option value="DOWN_LEFT">Abajo Izquierda</option>
                                        <option value="DOWN_RIGHT">Abajo Derecha</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Mensaje</label>
                                    <input
                                        type="text"
                                        value={bulkMessage}
                                        onChange={(e) => setBulkMessage(e.target.value)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        placeholder="Mensaje..."
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Color</label>
                                    <input
                                        type="color"
                                        value={bulkColor}
                                        onChange={(e) => setBulkColor(e.target.value)}
                                        className="w-full h-10 bg-dark-700 border border-dark-600 rounded cursor-pointer"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">
                                        Brillo: {bulkBrightness}%
                                    </label>
                                    <input
                                        type="range"
                                        min="0"
                                        max="100"
                                        value={bulkBrightness}
                                        onChange={(e) => setBulkBrightness(parseInt(e.target.value))}
                                        className="w-full"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Idioma</label>
                                    <select
                                        value={bulkLanguage}
                                        onChange={(e) => setBulkLanguage(e.target.value as Language)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="ES">Español</option>
                                        <option value="CAT">Catalán</option>
                                        <option value="EN">Inglés</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-1">Salida Evacuación</label>
                                    <input
                                        type="text"
                                        value={bulkEvacuationExit}
                                        onChange={(e) => setBulkEvacuationExit(e.target.value)}
                                        className="w-full px-3 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        placeholder="Ej: SALIDA 3"
                                    />
                                </div>

                                <button
                                    onClick={handleApplyBulkUpdate}
                                    className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded transition-colors"
                                >
                                    Aplicar a seleccionadas
                                </button>
                            </div>
                        ) : (
                            <div className="text-center py-8">
                                <p className="text-gray-400 text-sm mb-4">
                                    Selecciona balizas para aplicar cambios masivos
                                </p>
                                {viewMode === 'cards' && (
                                    <button
                                        onClick={toggleSelectAll}
                                        className="text-blue-400 hover:text-blue-300 text-sm underline"
                                    >
                                        {selectedBeacons.size === filteredBeacons.length ? "Deseleccionar todas" : "Seleccionar todas"}
                                    </button>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Modal de edición de baliza */}
            {showEditModal && selectedBeaconForEdit && (
                <BeaconEditModal
                    beacon={selectedBeaconForEdit}
                    onClose={() => {
                        setShowEditModal(false);
                        setSelectedBeaconForEdit(null);
                    }}
                    onSaved={async () => {
                        await refresh();
                    }}
                />
            )}
        </Layout>
    );
};
