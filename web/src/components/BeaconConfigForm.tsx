import React, { useState, useEffect } from "react";
import { Save, X } from "lucide-react";
import { Beacon, BeaconMode, ArrowDirection, Language } from "../types";
import { beaconsService } from "../services/beaconService";
import { BeaconPreview } from "./BeaconPreview";

interface BeaconConfigFormProps {
  beacon: Beacon;
  onSave?: () => void;
  onCancel?: () => void;
}

export const BeaconConfigForm: React.FC<BeaconConfigFormProps> = ({ beacon, onSave, onCancel }) => {
  const [mode, setMode] = useState<BeaconMode>(beacon.mode || "UNCONFIGURED");
  const [arrow, setArrow] = useState<ArrowDirection>(beacon.arrow || "NONE");
  const [message, setMessage] = useState(beacon.message || "");
  const [color, setColor] = useState(beacon.color || "#00FFAA");
  const [brightness, setBrightness] = useState(beacon.brightness ?? 90);
  const [language, setLanguage] = useState<Language>(beacon.language || "ES");
  const [evacuationExit, setEvacuationExit] = useState(beacon.evacuationExit || "");
  const [zone, setZone] = useState(beacon.zone || "");
  const [tags, setTags] = useState<string[]>(beacon.tags || []);
  const [tagInput, setTagInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    // Actualizar valores si cambia el beacon
    setMode(beacon.mode || "UNCONFIGURED");
    setArrow(beacon.arrow || "NONE");
    setMessage(beacon.message || "");
    setColor(beacon.color || "#00FFAA");
    setBrightness(beacon.brightness ?? 90);
    setLanguage(beacon.language || "ES");
    setEvacuationExit(beacon.evacuationExit || "");
    setZone(beacon.zone || "");
    setTags(beacon.tags || []);
  }, [beacon]);

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Validar zona
    if (!zone || !zone.trim()) {
      newErrors.zone = "La zona es obligatoria";
    }

    // Validar mensaje (máximo 255 caracteres)
    if (message && message.length > 255) {
      newErrors.message = "El mensaje no puede exceder 255 caracteres";
    }

    // Validar color hexadecimal
    const hexColorRegex = /^#[0-9A-Fa-f]{6}$/;
    if (color && !hexColorRegex.test(color)) {
      newErrors.color = "Color debe ser un valor hexadecimal válido (#RRGGBB)";
    }

    // Validar brillo (0-100)
    if (brightness < 0 || brightness > 100) {
      newErrors.brightness = "El brillo debe estar entre 0 y 100";
    }

    // Validar salida de evacuación si está en modo evacuación
    if (mode === "EVACUATION" && (!evacuationExit || !evacuationExit.trim())) {
      newErrors.evacuationExit = "Debe especificar una salida de evacuación en modo EVACUATION";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSaving(true);
    try {
      await beaconsService.configureBeacon(beacon.beaconId, {
        mode,
        arrow,
        message: message.trim() || undefined,
        color,
        brightness,
        language,
        evacuationExit: evacuationExit.trim() || undefined,
        zone: zone.trim(),
        tags: tags.length > 0 ? tags : undefined
      });

      if (onSave) onSave();
    } catch (error) {
      console.error("Error guardando configuración:", error);
      alert("Error al guardar la configuración");
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

  return (
    <div className="space-y-6">
      {/* Vista previa */}
      <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Vista Previa</h3>
        <BeaconPreview
          mode={mode}
          arrow={arrow}
          message={message}
          color={color}
          language={language}
          evacuationExit={evacuationExit}
        />
      </div>

      {/* Formulario */}
      <form onSubmit={handleSubmit} className="bg-dark-800 border border-dark-700 rounded-lg p-6 space-y-4">
        <h3 className="text-lg font-semibold text-white mb-4">Configuración</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Zona <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={zone}
              onChange={(e) => setZone(e.target.value)}
              className={`w-full px-4 py-2 bg-dark-700 border ${errors.zone ? 'border-red-500' : 'border-dark-600'} rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500`}
              placeholder="Ej: GRADA-G, PADDOCK, VIP"
            />
            {errors.zone && <p className="text-red-500 text-xs mt-1">{errors.zone}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Modo</label>
            <select
              value={mode}
              onChange={(e) => setMode(e.target.value as BeaconMode)}
              className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              title="Modo de la baliza"
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
              <option value="FORWARD">↑ Adelante (Arriba)</option>
              <option value="BACKWARD">↓ Atrás (Abajo)</option>
              <option value="LEFT">← Izquierda</option>
              <option value="RIGHT">→ Derecha</option>
              <option value="FORWARD_LEFT">↖ Diagonal Izq-Adelante</option>
              <option value="FORWARD_RIGHT">↗ Diagonal Der-Adelante</option>
              <option value="BACKWARD_LEFT">↙ Diagonal Izq-Atrás</option>
              <option value="BACKWARD_RIGHT">↘ Diagonal Der-Atrás</option>
            </select>
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
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">
            Mensaje {message.length > 0 && <span className="text-xs text-gray-400">({message.length}/255)</span>}
          </label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={2}
            maxLength={255}
            className={`w-full px-4 py-2 bg-dark-700 border ${errors.message ? 'border-red-500' : 'border-dark-600'} rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500`}
            placeholder="Mensaje a mostrar en la baliza (opcional, máx. 255 caracteres)"
          />
          {errors.message && <p className="text-red-500 text-xs mt-1">{errors.message}</p>}
          <p className="text-xs text-gray-400 mt-1">Si no especifica mensaje, se usará uno predeterminado según el modo</p>
        </div>

        {mode === "EVACUATION" && (
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Salida de Evacuación <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={evacuationExit}
              onChange={(e) => setEvacuationExit(e.target.value)}
              maxLength={100}
              className={`w-full px-4 py-2 bg-dark-700 border ${errors.evacuationExit ? 'border-red-500' : 'border-dark-600'} rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500`}
              placeholder="Ej: SALIDA NORTE, EXIT A"
            />
            {errors.evacuationExit && <p className="text-red-500 text-xs mt-1">{errors.evacuationExit}</p>}
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Color</label>
            <div className="flex gap-2">
              <input
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value.toUpperCase())}
                className="h-10 w-20 bg-dark-700 border border-dark-600 rounded cursor-pointer"
                title="Selector de color"
              />
              <input
                type="text"
                value={color}
                onChange={(e) => setColor(e.target.value.toUpperCase())}
                maxLength={7}
                className={`flex-1 px-4 py-2 bg-dark-700 border ${errors.color ? 'border-red-500' : 'border-dark-600'} rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500`}
                placeholder="#RRGGBB"
                title="Color en hexadecimal"
              />
            </div>
            {errors.color && <p className="text-red-500 text-xs mt-1">{errors.color}</p>}
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
            {errors.brightness && <p className="text-red-500 text-xs mt-1">{errors.brightness}</p>}
          </div>
        </div>

        {/* Sistema de Tags */}
        <div>
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
          <p className="text-xs text-gray-400 mt-1">Las etiquetas ayudan a categorizar y filtrar balizas</p>
        </div>

        <div className="flex gap-3 pt-4">
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="flex-1 flex items-center justify-center gap-2 py-3 bg-dark-700 hover:bg-dark-600 text-white font-semibold rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
              Cancelar
            </button>
          )}
          <button
            type="submit"
            disabled={saving || !zone.trim()}
            className="flex-1 flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition-colors"
          >
            <Save className="w-5 h-5" />
            {saving ? "Guardando..." : "Guardar Configuración"}
          </button>
        </div>
      </form>
    </div>
  );
};
