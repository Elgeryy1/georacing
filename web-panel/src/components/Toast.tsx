import React, { useEffect } from "react";
import { CheckCircle, XCircle, Info, AlertCircle } from "lucide-react";

export type ToastType = "success" | "error" | "info" | "warning";

interface ToastProps {
  id?: string; // Opcional, usado solo para key en el parent
  message: string;
  type: ToastType;
  onClose: () => void;
  duration?: number;
}

export const Toast: React.FC<ToastProps> = ({ message, type, onClose, duration = 3000 }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const getIcon = () => {
    switch (type) {
      case "success":
        return <CheckCircle className="w-5 h-5 text-green-400" />;
      case "error":
        return <XCircle className="w-5 h-5 text-red-400" />;
      case "warning":
        return <AlertCircle className="w-5 h-5 text-orange-400" />;
      case "info":
        return <Info className="w-5 h-5 text-blue-400" />;
    }
  };

  const getBgColor = () => {
    switch (type) {
      case "success":
        return "bg-black/90 border-neon-green/50 shadow-[0_0_10px_rgba(57,255,20,0.3)]";
      case "error":
        return "bg-black/90 border-neon-red/50 shadow-[0_0_10px_rgba(255,0,0,0.3)]";
      case "warning":
        return "bg-black/90 border-neon-yellow/50 shadow-[0_0_10px_rgba(255,255,0,0.3)]";
      case "info":
        return "bg-black/90 border-neon-blue/50 shadow-[0_0_10px_rgba(0,255,255,0.3)]";
    }
  };

  return (
    <div className={`relative flex items-center gap-3 px-4 py-3 rounded-sm border-l-4 ${getBgColor()} backdrop-blur-sm animate-slide-up font-mono`}>
      {getIcon()}
      <span className="text-white text-sm font-bold tracking-wide">{message}</span>
      <button
        onClick={onClose}
        className="ml-2 text-gray-500 hover:text-white transition-colors font-bold"
        aria-label="Cerrar notificación"
      >
        ×
      </button>
    </div>
  );
};
