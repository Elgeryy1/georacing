import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Layout } from "../components/Layout";
import { BeaconPreview } from "../components/BeaconPreview";
import { BeaconMetricsCard } from "../components/BeaconMetricsCard";
import { CommandPanel } from "../components/CommandPanel";
import { useBeacons } from "../hooks/useBeacons";
import { beaconsService } from "../services/beaconService";
import { BeaconMode, ArrowDirection, Language } from "../types";
import { ArrowLeft, Save, Wrench, RotateCcw } from "lucide-react";

export const BeaconDetail: React.FC = () => {
  const { beaconId } = useParams<{ beaconId: string }>();
  const navigate = useNavigate();
  const { beacons } = useBeacons();
  
  const beacon = beacons.find(b => b.beaconId === beaconId);

  const [mode, setMode] = useState<BeaconMode>("NORMAL");
  const [arrow, setArrow] = useState<ArrowDirection>("NONE");
  const [message, setMessage] = useState("");
  const [color, setColor] = useState("#00FFAA");
  const [brightness, setBrightness] = useState(90);
  const [language, setLanguage] = useState<Language>("ES");
  const [evacuationExit, setEvacuationExit] = useState("");
  const [zone, setZone] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (beacon) {
      setMode(beacon.mode || "NORMAL");
      setArrow(beacon.arrow || "NONE");
      setMessage(beacon.message || "");
      setColor(beacon.color || "#00FFAA");
      setBrightness(beacon.brightness ?? 90);
      setLanguage(beacon.language || "ES");
      setEvacuationExit(beacon.evacuationExit || "");
      setZone(beacon.zone || "");
      setTags(beacon.tags || []);
    }
  }, [beacon]);

  if (!beacon) {
    return (
      <Layout>
        <div className="text-white">Baliza no encontrada</div>
      </Layout>
    );
  }

  const handleSave = async () => {
    setSaving(true);
    try {
      await beaconsService.updateBeacon(beacon.beaconId, {
        mode,
        arrow,
        message: message.trim() || undefined,
        color,
        brightness,
        language,
        evacuationExit: evacuationExit.trim() || undefined,
        zone: zone.trim() || undefined,
        tags: tags.length > 0 ? tags : undefined
      });
      alert("✅ Baliza actualizada correctamente");
      // No navegar automáticamente, permitir seguir editando
    } catch (error) {
      console.error("Error al guardar:", error);
      alert("❌ Error al guardar la baliza");
    } finally {
      setSaving(false);
    }
  };

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(t => t !== tagToRemove));
  };

  const handleSetMaintenance = async () => {
    setSaving(true);
    try {
      await beaconsService.updateBeacon(beacon.beaconId, {
        mode: "MAINTENANCE"
      });
      setMode("MAINTENANCE");
    } catch (error) {
      console.error("Error:", error);
    } finally {
      setSaving(false);
    }
  };

  const handleResetToNormal = async () => {
    setSaving(true);
    try {
      await beaconsService.updateBeacon(beacon.beaconId, {
        mode: "NORMAL",
        message: "Acceso Principal",
        color: "#00FFAA",
        brightness: 90,
        arrow: "RIGHT"
      });
      setMode("NORMAL");
      setMessage("Acceso Principal");
      setColor("#00FFAA");
      setBrightness(90);
      setArrow("RIGHT");
    } catch (error) {
      console.error("Error:", error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate("/dashboard")}
            className="p-2 hover:bg-dark-700 rounded-lg transition-colors"
            title="Volver al dashboard"
          >
            <ArrowLeft className="w-6 h-6 text-white" />
          </button>
          <h1 className="text-2xl font-bold text-white">Editar Baliza: {beacon.beaconId}</h1>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="space-y-6">
            <BeaconMetricsCard beacon={beacon} />

            <div className="bg-dark-800 p-6 rounded-lg space-y-4">
              <h2 className="text-lg font-semibold text-white mb-4">Configuración Editable</h2>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Modo</label>
                <select
                  value={mode}
                  onChange={(e) => setMode(e.target.value as BeaconMode)}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  title="Modo de operación"
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
                  title="Dirección de la flecha"
                >
                  <option value="NONE">Sin flecha</option>
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
                <label className="block text-sm font-medium text-gray-300 mb-2">Mensaje</label>
                <textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  rows={3}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Mensaje a mostrar..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Color</label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={color}
                    onChange={(e) => setColor(e.target.value)}
                    className="h-10 w-20 bg-dark-700 border border-dark-600 rounded cursor-pointer"
                    title="Selector de color"
                  />
                  <input
                    type="text"
                    value={color}
                    onChange={(e) => setColor(e.target.value)}
                    className="flex-1 px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="#RRGGBB"
                    title="Color en hexadecimal"
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
                  title="Control de brillo"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Idioma</label>
                <select
                  value={language}
                  onChange={(e) => setLanguage(e.target.value as Language)}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  title="Idioma de los mensajes"
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

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Salida de Evacuación</label>
                <input
                  type="text"
                  value={evacuationExit}
                  onChange={(e) => setEvacuationExit(e.target.value)}
                  maxLength={100}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ej: SALIDA 3"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Zona</label>
                <input
                  type="text"
                  value={zone}
                  onChange={(e) => setZone(e.target.value)}
                  maxLength={50}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ej: GRADA-G, PADDOCK"
                />
              </div>

              {/* Sistema de Tags */}
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Etiquetas (Tags)
                </label>
                <div className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddTag())}
                    className="flex-1 px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Agregar etiqueta (presione Enter)"
                  />
                  <button
                    type="button"
                    onClick={handleAddTag}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
                  >
                    Agregar
                  </button>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {tags.map((tag, index) => (
                      <span
                        key={index}
                        className="inline-flex items-center gap-2 px-3 py-1 bg-blue-600/20 text-blue-400 rounded-full text-sm"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="hover:text-blue-300"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => navigate("/dashboard")}
                className="flex-1 flex items-center justify-center gap-2 py-3 bg-dark-700 hover:bg-dark-600 text-white font-semibold rounded-lg transition-colors"
              >
                <ArrowLeft className="w-5 h-5" />
                Volver al Dashboard
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-800 text-white font-semibold rounded-lg transition-colors"
              >
                <Save className="w-5 h-5" />
                {saving ? "Guardando..." : "Guardar Cambios"}
              </button>
            </div>

            <div className="flex gap-3">
              <button
                onClick={handleSetMaintenance}
                disabled={saving}
                className="flex-1 flex items-center justify-center gap-2 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-lg transition-colors"
              >
                <Wrench className="w-4 h-4" />
                Poner en Mantenimiento
              </button>
              <button
                onClick={handleResetToNormal}
                disabled={saving}
                className="flex-1 flex items-center justify-center gap-2 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition-colors"
              >
                <RotateCcw className="w-4 h-4" />
                Reset a Normal
              </button>
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-dark-800 p-6 rounded-lg">
              <h2 className="text-lg font-semibold text-white mb-4">Vista Previa</h2>
              <BeaconPreview
                mode={mode}
                arrow={arrow}
                message={message}
                color={color}
                language={language}
                evacuationExit={evacuationExit}
              />
            </div>

            <CommandPanel beaconId={beacon.beaconId} />
          </div>
        </div>
      </div>
    </Layout>
  );
};
