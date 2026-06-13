import React, { useState, useEffect } from "react";
import { X, Save, RotateCcw, Power, LogOut } from "lucide-react";
import { Beacon, BeaconMode, ArrowDirection, Language } from "../types";
import { beaconsService } from "../services/beaconService";
import { BeaconPreview } from "./BeaconPreview";
import { CommandPanel } from "./CommandPanel";
import { getDefaultBeaconMessage } from "../utils/beaconMessages";
import { useToast } from "../context/ToastContext";

interface BeaconEditModalProps {
  beacon: Beacon;
  onClose: () => void;
  onSaved?: () => void;
}

export const BeaconEditModal: React.FC<BeaconEditModalProps> = ({ beacon, onClose, onSaved }) => {
  const { showToast } = useToast();
  const [mode, setMode] = useState<BeaconMode>((beacon.mode as BeaconMode) || "UNCONFIGURED");
  const [arrow, setArrow] = useState<ArrowDirection>(beacon.arrow || "NONE");
  const [message, setMessage] = useState(beacon.message || "");
  const [color, setColor] = useState(beacon.color || "#00FFAA");
  const [brightness, setBrightness] = useState(beacon.brightness || 90);
  const [language, setLanguage] = useState<Language>(beacon.language || "ES");
  const [evacuationExit, setEvacuationExit] = useState(beacon.evacuationExit || "");
  const [zone, setZone] = useState(beacon.zone || "");
  const [saving, setSaving] = useState(false);
  const [restarting, setRestarting] = useState(false);

  // ✨ NUEVO: Trackear si el usuario ha escrito un mensaje personalizado
  const [hasCustomMessage, setHasCustomMessage] = useState(false);

  // Initialise the form ONLY when the beacon being edited changes (by id).
  // The parent polls the beacon list every few seconds; if we also depended on
  // the individual fields, a backend refresh mid-edit would overwrite the
  // operator's unsaved changes. Keying on the id initialises once per beacon.
  useEffect(() => {
    setMode((beacon.mode as BeaconMode) || "UNCONFIGURED");
    setArrow(beacon.arrow || "NONE");
    setMessage(beacon.message || "");
    setColor(beacon.color || "#00FFAA");
    setBrightness(beacon.brightness || 90);
    setLanguage(beacon.language || "ES");
    setEvacuationExit(beacon.evacuationExit || "");
    setZone(beacon.zone || "");
    setHasCustomMessage(false); // Resetear al abrir el modal
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beacon.beaconId]);

  // ✨ Actualizar automáticamente el mensaje cuando cambias modo/idioma/flecha
  // SOLO si el usuario NO ha escrito un mensaje personalizado
  useEffect(() => {
    if (!hasCustomMessage) {
      setMessage(getDefaultBeaconMessage(mode, language, arrow));
    }
  }, [mode, language, arrow, hasCustomMessage]);

  // ✨ Detectar cuando el usuario escribe en el campo de mensaje
  const handleMessageChange = (newMessage: string) => {
    setMessage(newMessage);
    // Si el usuario borra todo el texto, volver a modo automático
    if (newMessage.trim() === "") {
      setHasCustomMessage(false);
    } else {
      // Si escribe algo, marcar como personalizado
      setHasCustomMessage(true);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      showToast("Enviando configuración a la baliza...", "info");

      await beaconsService.updateBeacon(beacon.beaconId, {
        mode,
        arrow,
        message, // Ya siempre tiene contenido (automático o personalizado)
        color,
        brightness,
        language,
        evacuationExit,
        zone: zone.trim() || undefined
      });

      showToast("✅ Configuración enviada correctamente", "success");

      if (onSaved) onSaved();
      onClose();
    } catch (error) {
      console.error("Error guardando baliza:", error);
      showToast("❌ Error al contactar con la baliza", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleRestart = async () => {
    if (!confirm(`⚠️⚠️⚠️ ¿Seguro que quieres REINICIAR EL SISTEMA WINDOWS de ${beacon.beaconId}?\n\n🔴 ESTO REINICIARÁ EL ORDENADOR COMPLETO, NO SOLO LA APLICACIÓN.\n\nEl sistema se apagará y volverá a encender.`)) {
      return;
    }

    setRestarting(true);
    try {
      await beaconsService.restartBeacon(beacon.beaconId);
      alert(`✅ Comando de reinicio enviado a ${beacon.beaconId}`);
    } catch (error) {
      console.error("Error reiniciando baliza:", error);
      alert("❌ Error al enviar comando de reinicio");
    } finally {
      setRestarting(false);
    }
  };

  const handleShutdown = async () => {
    if (!confirm(`⛔⛔⛔ ¿Seguro que quieres APAGAR EL SISTEMA WINDOWS de ${beacon.beaconId}?\n\n🔴 EL ORDENADOR SE APAGARÁ COMPLETAMENTE Y NO PODRÁS VOLVER A ENCENDERLO REMOTAMENTE.\n\n¿Continuar?`)) {
      return;
    }

    setRestarting(true); // Reusamos estado para bloquear botones
    try {
      await beaconsService.shutdownBeacon(beacon.beaconId);
      alert(`✅ Comando de APAGADO enviado a ${beacon.beaconId}`);
    } catch (error) {
      console.error("Error apagando baliza:", error);
      alert("❌ Error al enviar comando de apagado");
    } finally {
      setRestarting(false);
    }
  };

  const handleCloseApp = async () => {
    if (!confirm(`¿Seguro que quieres CERRAR LA APLICACIÓN en ${beacon.beaconId}?\n\nLa aplicación se cerrará y volverá al escritorio de Windows.`)) {
      return;
    }

    setRestarting(true);
    try {
      await beaconsService.closeAppBeacon(beacon.beaconId);
      alert(`✅ Comando de cerrar aplicación enviado a ${beacon.beaconId}`);
    } catch (error) {
      console.error("Error cerrando app:", error);
      alert("❌ Error al enviar comando");
    } finally {
      setRestarting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-dark-800 rounded-lg max-w-5xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-dark-800 border-b border-dark-700 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-white">Editar Baliza</h2>
            <p className="text-sm text-gray-400">{beacon.beaconId} - {beacon.zone}</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-dark-700 rounded-lg transition-colors"
            aria-label="Cerrar"
            title="Cerrar"
          >
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        <div className="p-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Formulario */}
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-white mb-4">Configuración</h3>

              {/* Zona */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Zona <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={zone}
                  onChange={(e) => setZone(e.target.value)}
                  maxLength={50}
                  className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ej: GRADA-G, PADDOCK, VIP"
                />
                <p className="text-xs text-gray-400 mt-1">Ubicación física de la baliza (máx. 50 caracteres)</p>
              </div>

              {/* Modo */}
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

              {/* Flecha */}
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

              {/* Mensaje */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Mensaje Personalizado (opcional)
                </label>
                <div className="space-y-2">
                  <div className="px-3 py-2 bg-blue-900/30 border border-blue-700/50 rounded text-sm">
                    <div className="flex items-start gap-2">
                      <span className="text-blue-400 font-semibold shrink-0">💡 Predefinido:</span>
                      <span className="text-blue-200 italic">"{getDefaultBeaconMessage(mode, language, arrow)}"</span>
                    </div>
                    <p className="text-xs text-blue-300/70 mt-1 ml-6">
                      Este texto se mostrará si dejas el campo vacío
                    </p>
                  </div>
                  <textarea
                    value={message}
                    onChange={(e) => handleMessageChange(e.target.value)}
                    rows={3}
                    className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Escribe aquí tu mensaje personalizado (déjalo vacío para usar el predefinido)"
                  />
                  {hasCustomMessage && (
                    <p className="text-xs text-green-400 flex items-center gap-1">
                      <span>✓</span> Se usará tu mensaje personalizado
                    </p>
                  )}
                  {!hasCustomMessage && (
                    <p className="text-xs text-blue-400 flex items-center gap-1">
                      <span>🔄</span> Actualizándose automáticamente
                    </p>
                  )}
                </div>
              </div>

              {/* Salida de evacuación (solo si modo es EVACUATION) */}
              {mode === "EVACUATION" && (
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">Salida de Evacuación</label>
                  <input
                    type="text"
                    value={evacuationExit}
                    onChange={(e) => setEvacuationExit(e.target.value)}
                    className="w-full px-4 py-2 bg-dark-700 border border-dark-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Ej: SALIDA NORTE, EXIT A"
                  />
                </div>
              )}

              {/* Color y Brillo */}
              <div className="grid grid-cols-2 gap-4">
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
              </div>

              {/* Idioma */}
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

            {/* Vista Previa */}
            <div className="space-y-6">
              <div>
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

              <CommandPanel beaconId={beacon.beaconId} />
            </div>
          </div>
        </div>

        {/* Footer con botones */}
        <div className="sticky bottom-0 bg-dark-800 border-t border-dark-700 px-6 py-4 flex gap-3">
          <button
            onClick={onClose}
            className="px-4 py-3 bg-dark-700 hover:bg-dark-600 text-white font-semibold rounded-lg transition-colors"
          >
            Cancelar
          </button>

          <div className="flex-1 flex gap-2">
            <button
              onClick={handleCloseApp}
              disabled={restarting || saving}
              className="flex-1 flex items-center justify-center gap-2 py-3 bg-gray-600 hover:bg-gray-700 disabled:opacity-50 text-white font-semibold rounded-lg transition-colors"
              title="Cerrar Aplicación"
            >
              <LogOut className="w-5 h-5" />
              Cerrar App
            </button>

            <button
              onClick={handleRestart}
              disabled={restarting || saving}
              className="flex-1 flex items-center justify-center gap-2 py-3 bg-orange-600 hover:bg-orange-700 disabled:opacity-50 text-white font-semibold rounded-lg transition-colors"
              title="Reiniciar Windows"
            >
              <RotateCcw className="w-5 h-5" />
              Reiniciar
            </button>

            <button
              onClick={handleShutdown}
              disabled={restarting || saving}
              className="flex-1 flex items-center justify-center gap-2 py-3 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-semibold rounded-lg transition-colors"
              title="Apagar Windows"
            >
              <Power className="w-5 h-5" />
              Apagar
            </button>
          </div>

          <button
            onClick={handleSave}
            disabled={saving || restarting}
            className="px-8 flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold rounded-lg transition-colors"
          >
            <Save className="w-5 h-5" />
            {saving ? "Guardando..." : "Guardar"}
          </button>
        </div>
      </div>
    </div>
  );
};

