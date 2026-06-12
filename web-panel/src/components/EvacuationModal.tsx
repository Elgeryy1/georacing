import React, { useState } from 'react';
import { AlertTriangle, ShieldAlert, X } from 'lucide-react';
import { api } from '../services/apiClient';

interface EvacuationModalProps {
    isOpen: boolean;
    onClose: () => void;
    isEvacuationActive: boolean;
}

export const EvacuationModal: React.FC<EvacuationModalProps> = ({ isOpen, onClose, isEvacuationActive }) => {
    const [loading, setLoading] = useState(false);
    const [confirmText, setConfirmText] = useState('');

    if (!isOpen) return null;

    const handleToggle = async () => {
        setLoading(true);
        try {
            const newMode = isEvacuationActive ? "NORMAL" : "EVACUATION";
            const message = isEvacuationActive ? "System Normal" : "EVACUATION ORDER - PLEASE EXIT";

            await api.setCircuitState(newMode, message);

            // Also send command to beacons? Ideally API handles this or beacons poll. 
            // For immediate effect we could iterate beacons, but let's rely on polling/db first as per plan.

            onClose();
        } catch (err) {
            console.error(err);
            alert("Error changing state");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
            <div className={`w-full max-w-lg rounded-2xl border-2 p-6 shadow-2xl ${isEvacuationActive ? 'bg-green-900/20 border-green-500' : 'bg-red-900/20 border-red-500'}`}>

                <div className="flex justify-between items-start mb-6">
                    <div className="flex items-center gap-3">
                        <div className={`p-3 rounded-full ${isEvacuationActive ? 'bg-green-500/20 text-green-500' : 'bg-red-500/20 text-red-500'}`}>
                            {isEvacuationActive ? <ShieldAlert className="w-8 h-8" /> : <AlertTriangle className="w-8 h-8" />}
                        </div>
                        <div>
                            <h2 className="text-2xl font-bold text-white">
                                {isEvacuationActive ? "DESACTIVAR EVACUACIÓN" : "ACTIVAR MODO EVACUACIÓN"}
                            </h2>
                            <p className="text-gray-400">Control de Emergencia Global</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="p-2 text-gray-400 hover:text-white rounded-lg hover:bg-white/10">
                        <X className="w-6 h-6" />
                    </button>
                </div>

                <div className="space-y-4 mb-8">
                    {isEvacuationActive ? (
                        <p className="text-gray-300">
                            ¿Confirmas que la situación de emergencia ha pasado? Esto devolverá todas las balizas y apps al modo <strong>NORMAL</strong>.
                        </p>
                    ) : (
                        <div className="bg-red-500/10 p-4 rounded-lg border border-red-500/30">
                            <p className="text-red-200 font-medium mb-2">⚠️ ESTA ES UNA ACCIÓN CRÍTICA</p>
                            <ul className="list-disc list-inside text-sm text-red-300 space-y-1">
                                <li>Todas las balizas mostrarán flechas de salida.</li>
                                <li>Los usuarios recibirán alertas en sus móviles.</li>
                                <li>Se activarán los protocolos de seguridad.</li>
                            </ul>
                        </div>
                    )}

                    {!isEvacuationActive && (
                        <div>
                            <label className="text-xs uppercase tracking-wider text-gray-500 font-semibold">Confirmación</label>
                            <input
                                type="text"
                                placeholder="Escribe 'EVACUAR' para confirmar"
                                value={confirmText}
                                onChange={(e) => setConfirmText(e.target.value)}
                                className="w-full mt-2 bg-dark-800 border-dark-600 text-white p-3 rounded-lg focus:ring-red-500 focus:border-red-500"
                            />
                        </div>
                    )}
                </div>

                <div className="flex gap-3">
                    <button onClick={onClose} className="flex-1 py-3 bg-dark-700 hover:bg-dark-600 text-white rounded-xl font-medium transition-colors">
                        Cancelar
                    </button>
                    <button
                        onClick={handleToggle}
                        disabled={loading || (!isEvacuationActive && confirmText !== 'EVACUAR')}
                        className={`flex-1 py-3 rounded-xl font-bold text-white transition-all transform active:scale-95 flex items-center justify-center gap-2
              ${loading ? 'opacity-50 cursor-not-allowed' : ''}
              ${isEvacuationActive
                                ? 'bg-green-600 hover:bg-green-500 shadow-lg shadow-green-900/20'
                                : 'bg-red-600 hover:bg-red-500 shadow-lg shadow-red-900/20 disabled:bg-gray-700 disabled:text-gray-500'
                            }
            `}
                    >
                        {loading ? "Procesando..." : (isEvacuationActive ? "RESTAURAR NORMALIDAD" : "INICIAR EVACUACIÓN")}
                    </button>
                </div>
            </div>
        </div>
    );
};
