import React from "react";
import { BeaconMode, ArrowDirection, Language } from "../types";
import { ArrowLeft, ArrowRight, ArrowUp, ArrowDown, ArrowUpLeft, ArrowUpRight, ArrowDownLeft, ArrowDownRight } from "lucide-react";
import { getDefaultBeaconMessage } from "../utils/beaconMessages";

export interface BeaconPreviewProps {
  mode: BeaconMode;
  arrow: ArrowDirection;
  message?: string; // Opcional porque puede no existir en Firestore
  color: string;
  language: Language;
  evacuationExit?: string;
}

export const BeaconPreview: React.FC<BeaconPreviewProps> = ({
  mode,
  arrow,
  message,
  color,
  language,
  evacuationExit
}) => {
  const getBackgroundColor = () => {
    switch (mode) {
      case "UNCONFIGURED":
        return "#333333";
      case "NORMAL":
        return color;
      case "CONGESTION":
        return "#FFA500";
      case "EMERGENCY":
        return "#FF6600";
      case "EVACUATION":
        return "#FF0000";
      case "MAINTENANCE":
        return "#808080";
      default:
        return color;
    }
  };

  // Obtener mensaje a mostrar: personalizado o predefinido según modo/idioma/flecha
  const getDisplayMessage = (): string => {
    // Si hay mensaje personalizado (no vacío), usarlo
    if (message && message.trim()) {
      return message;
    }

    // Si no hay mensaje o está vacío, usar texto predefinido según modo, idioma y dirección
    return getDefaultBeaconMessage(mode, language, arrow);
  };

  const renderArrow = () => {
    const arrowClass = "w-32 h-32 text-white";

    switch (arrow) {
      case "LEFT":
        return <ArrowLeft className={arrowClass} />;
      case "RIGHT":
        return <ArrowRight className={arrowClass} />;
      case "FORWARD":
      case "UP": // Backwards compatibility
        return <ArrowUp className={arrowClass} />;
      case "BACKWARD":
      case "DOWN": // Backwards compatibility
        return <ArrowDown className={arrowClass} />;
      case "FORWARD_LEFT":
      case "UP_LEFT":
        return <ArrowUpLeft className={arrowClass} />;
      case "FORWARD_RIGHT":
      case "UP_RIGHT":
        return <ArrowUpRight className={arrowClass} />;
      case "BACKWARD_LEFT":
      case "DOWN_LEFT":
        return <ArrowDownLeft className={arrowClass} />;
      case "BACKWARD_RIGHT":
      case "DOWN_RIGHT":
        return <ArrowDownRight className={arrowClass} />;
      default:
        return null;
    }
  };

  const displayMessage = getDisplayMessage();

  if (mode === "UNCONFIGURED") {
    return (
      <div
        className="w-full aspect-video rounded-lg flex items-center justify-center bg-gray-800"
      >
        <div className="text-center text-gray-400">
          <div className="text-4xl font-bold mb-2">⚠️</div>
          <div className="text-2xl font-semibold">
            BALIZA SIN CONFIGURAR
          </div>
          <div className="text-sm mt-2">
            Configure esta baliza desde el panel de control
          </div>
        </div>
      </div>
    );
  }

  if (mode === "MAINTENANCE") {
    return (
      <div
        className="w-full aspect-video rounded-lg flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor() }}
      >
        <div className="text-center text-white">
          <div className="text-6xl font-bold mb-4">
            {new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}
          </div>
          <div className="text-2xl">
            MANTENIMIENTO
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="w-full aspect-video rounded-lg flex flex-col items-center justify-center p-8"
      style={{ backgroundColor: getBackgroundColor() }}
    >
      {arrow !== "NONE" && (
        <div className="mb-6">
          {renderArrow()}
        </div>
      )}

      <div className="text-center text-white">
        <div className="text-3xl font-bold mb-4 whitespace-pre-line">
          {displayMessage}
        </div>

        {mode === "EVACUATION" && evacuationExit && (
          <div className="text-2xl font-semibold mt-4 px-6 py-3 bg-black/30 rounded-lg">
            {evacuationExit}
          </div>
        )}
      </div>
    </div>
  );
};
