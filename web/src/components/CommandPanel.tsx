import React, { useState } from "react";
import { Terminal, Send } from "lucide-react";
import { beaconsService } from "../services/beaconService";

interface CommandPanelProps {
  beaconId?: string;
  onCommandSent?: () => void;
}

export const CommandPanel: React.FC<CommandPanelProps> = ({ beaconId, onCommandSent }) => {
  const [command, setCommand] = useState("");
  const [sending, setSending] = useState(false);

  const handleSendCommand = async () => {
    if (!command.trim() || !beaconId) return;

    setSending(true);
    try {
      await beaconsService.sendCommand(beaconId, command.trim());
      alert(`✅ Comando "${command}" enviado a ${beaconId}`);
      setCommand("");
      if (onCommandSent) onCommandSent();
    } catch (error) {
      console.error("Error enviando comando:", error);
      alert("❌ Error al enviar comando");
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="bg-dark-700 rounded-lg p-4 border border-dark-600">
      <div className="flex items-center gap-2 mb-3">
        <Terminal className="w-5 h-5 text-blue-400" />
        <h3 className="text-lg font-semibold text-white">Ejecutar Comando</h3>
      </div>
      
      <div className="flex gap-2">
        <input
          type="text"
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !sending) {
              handleSendCommand();
            }
          }}
          placeholder="RESTART, STATUS, CONFIG..."
          className="flex-1 px-4 py-2 bg-dark-800 border border-dark-600 rounded text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={!beaconId || sending}
        />
        <button
          onClick={handleSendCommand}
          disabled={!command.trim() || !beaconId || sending}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded transition-colors"
          title="Enviar comando"
        >
          <Send className="w-4 h-4" />
          {sending ? "Enviando..." : "Enviar"}
        </button>
      </div>
      
      {!beaconId && (
        <p className="text-sm text-gray-500 mt-2">
          Selecciona una baliza para enviar comandos
        </p>
      )}
    </div>
  );
};
