import React, { useState } from "react";
import { NewBeaconDetected } from "../services/beaconDetectionService";
import { BeaconMode, ArrowDirection, Language } from "../types";
import { X, Save, AlertCircle } from "lucide-react";
import { beaconsService } from "../services/beaconService";
import { useToast } from "../context/ToastContext";

interface NewBeaconModalProps {
  beacon: NewBeaconDetected;
  onClose: () => void;
  onConfigured: () => void;
}

export const NewBeaconModal: React.FC<NewBeaconModalProps> = ({ beacon, onClose, onConfigured }) => {
  const { showToast } = useToast();
  const [zone, setZone] = useState("");
  const [mode, setMode] = useState<BeaconMode>("NORMAL");
  const [arrow, setArrow] = useState<ArrowDirection>("NONE");
  const [message, setMessage] = useState("Acceso Principal");
  const [color, setColor] = useState("#00FFAA");
  const [brightness, setBrightness] = useState(90);
  const [language, setLanguage] = useState<Language>("ES");
  const [tags, setTags] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    if (!zone.trim()) {
      showToast("La zona es obligatoria", "warning");
      return;
    }

    setSaving(true);
    try {
      showToast("Configurando baliza...", "info");
      
      await beaconsService.configureBeacon(beacon.beaconId, {
        mode,
        arrow,
        message,
        color,
        brightness,
        language,
        evacuationExit: ""
      });
      
      showToast("✅ Baliza configurada correctamente", "success");
      onConfigured();
      onClose();
    } catch (error) {
      console.error("Error configurando baliza:", error);
      showToast("❌ Error al configurar la baliza", "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-dark-800 rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-dark-800 border-b border-dark-700 px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-yellow-500" />
            <div>
              <h2 className="text-xl font-bold text-white">Nueva Baliza Detectada</h2>
              <p className="text-sm text-gray-400">Configura los parámetros iniciales</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-dark-700 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="bg-blue-500/10 border border-blue-500 rounded-lg p-4">
            <div className="flex items-center gap-2 text-blue-400 mb-2">
              <span className="font-semibold">Baliza ID:</span>
              <span className="font-mono">{beacon.beaconId}</span>
            </div>
            <div className="text-sm text-gray-400">
              Primera conexión: {beacon.firstSeen ? new Date(beacon.firstSeen as any).toLocaleString('es-ES') : 'Desconocida'}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Zona <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={zone}
                onChange={(e) => setZone(e.target.value)}
                className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Ej: GRADA-G, PADDOCK, VIP"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Modo</label>
              <select
                value={mode}
                onChange={(e) => setMode(e.target.value as BeaconMode)}
                className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="UNCONFIGURED">Sin configurar</option>
                <option value="NORMAL">Normal</option>
                <option value="CONGESTION">Congestión</option>
                <option value="EMERGENCY">Emergencia</option>
                <option value="EVACUATION">Evacuación</option>
                <option value="MAINTENANCE">Mantenimiento</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Flecha</label>
              <select
                value={arrow}
                onChange={(e) => setArrow(e.target.value as ArrowDirection)}
                className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="NONE">Ninguna</option>
                <option value="UP">↑ Arriba</option>
                <option value="DOWN">↓ Abajo</option>
                <option value="LEFT">← Izquierda</option>
                <option value="RIGHT">→ Derecha</option>
                <option value="UP_LEFT">↖ Arriba-Izquierda</option>
                <option value="UP_RIGHT">↗ Arriba-Derecha</option>
                <option value="DOWN_LEFT">↙ Abajo-Izquierda</option>
                <option value="DOWN_RIGHT">↘ Abajo-Derecha</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Idioma</label>
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value as Language)}
                className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="ES">Español</option>
                <option value="CA">Catalán</option>
                <option value="EN">Inglés</option>
                <option value="FR">Francés</option>
                <option value="DE">Alemán</option>
                <option value="IT">Italiano</option>
                <option value="PT">Portugués</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Mensaje</label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={2}
              className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Mensaje a mostrar en la baliza"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Color</label>
              <div className="flex gap-2">
                <input
                  type="color"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="h-10 w-20 bg-dark-700 border border-dark-600 rounded cursor-pointer"
                />
                <input
                  type="text"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="flex-1 px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Brillo: {brightness}%
              </label>
              <input
                type="range"
                min="0"
                max="100"
                value={brightness}
                onChange={(e) => setBrightness(parseInt(e.target.value))}
                className="w-full"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Etiquetas (separadas por comas)
            </label>
            <input
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="entrada, grada, principal"
            />
          </div>
        </div>

        <div className="sticky bottom-0 bg-dark-800 border-t border-dark-700 px-6 py-4 flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-3 bg-dark-700 hover:bg-dark-600 text-white font-semibold rounded-lg transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !zone.trim()}
            className="flex-1 flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition-colors"
          >
            <Save className="w-5 h-5" />
            {saving ? "Guardando..." : "Guardar Configuración"}
          </button>
        </div>
      </div>
    </div>
  );
};
