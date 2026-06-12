
import type { Beacon } from "../types";
import { getModeColor, BeaconModeStrict } from "../utils/beaconHelpers";

interface BeaconMetricsCardProps {
  beacon: Beacon;
  onClick?: () => void;
  selected?: boolean;
  onSelect?: () => void;
}

export function BeaconMetricsCard({ beacon, onClick, selected, onSelect }: BeaconMetricsCardProps) {
  const {
    beaconId,
    name,
    battery,
    brightness,
    mode,
    online,
    lastSeen,
    lastUpdate,
    zone,
    arrow,
    message,
    configured,
    tags,
  } = beacon;

  const displayName = name || beaconId;
  const modeClass = getModeColor(mode as BeaconModeStrict | null);

  return (
    <section
      className="rounded-2xl border border-slate-800 bg-slate-900/60 p-4 shadow-sm"
      aria-label={`Métricas de la baliza ${displayName}`}
      tabIndex={0}
      role="button"
      onClick={onClick}
      onKeyDown={e => {
        if (e.key === "Enter" || e.key === " ") onClick?.();
      }}
    >
      <div className="absolute top-4 right-4 z-10">
        {onSelect && (
          <input
            type="checkbox"
            checked={selected}
            onChange={(e) => {
              e.stopPropagation();
              onSelect();
            }}
            className="w-5 h-5 rounded border-gray-600 bg-dark-700 text-blue-600 focus:ring-blue-500 focus:ring-offset-dark-800"
          />
        )}
      </div>
      <header className="mb-3 flex items-center justify-between gap-2 pr-8">
        <div className="min-w-0 flex-1">
          <h2 className="truncate text-sm font-semibold text-slate-100">{displayName}</h2>
          <p className="truncate text-xs text-slate-400">{beaconId}</p>
          {zone && <p className="truncate text-xs text-blue-400 mt-0.5">📍 {zone}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${online
              ? "bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/30"
              : "bg-red-500/10 text-red-400 ring-1 ring-red-500/30"
              }`}
            role="status"
            aria-label={online ? "Baliza en línea" : "Baliza desconectada"}
          >
            <span
              className={`h-1.5 w-1.5 rounded-full ${online ? "bg-emerald-400" : "bg-red-400"
                }`}
              aria-hidden="true"
            />
            {online ? "Online" : "Offline"}
          </span>
          {!configured && (
            <span
              className="inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium bg-yellow-500/10 text-yellow-400 ring-1 ring-yellow-500/30"
              title="Baliza sin configurar"
            >
              ⚠️ Sin configurar
            </span>
          )}
          <span
            className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ring-1 ${modeClass}`}
            title={mode ?? "Modo desconocido"}
          >
            {mode ?? "Modo desconocido"}
          </span>
        </div>
      </header>
      <div className="grid gap-3 text-xs text-slate-300 sm:grid-cols-2">
        <div>
          <p className="mb-1 text-slate-400">Batería</p>
          <p className="font-medium">
            {battery != null ? `${battery}%` : "Desconocida"}
          </p>
        </div>
        <div>
          <p className="mb-1 text-slate-400">Brillo</p>
          <p className="font-medium">
            {brightness != null ? `${brightness}%` : "Desconocido"}
          </p>
        </div>
        {arrow && arrow !== "NONE" && (
          <div>
            <p className="mb-1 text-slate-400">Flecha</p>
            <p className="font-medium">
              {arrow === "UP" && "↑ Arriba"}
              {arrow === "DOWN" && "↓ Abajo"}
              {arrow === "LEFT" && "← Izquierda"}
              {arrow === "RIGHT" && "→ Derecha"}
              {arrow === "UP_LEFT" && "↖ Arriba-Izq"}
              {arrow === "UP_RIGHT" && "↗ Arriba-Der"}
              {arrow === "DOWN_LEFT" && "↙ Abajo-Izq"}
              {arrow === "DOWN_RIGHT" && "↘ Abajo-Der"}
            </p>
          </div>
        )}
        {message && (
          <div className="col-span-2">
            <p className="mb-1 text-slate-400">Mensaje</p>
            <p className="font-medium truncate" title={message}>
              {message}
            </p>
          </div>
        )}
        <div>
          <p className="mb-1 text-slate-400">Última señal</p>
          <p className="font-medium">
            {lastSeen ? new Date(lastSeen).toLocaleString() : "Sin datos"}
          </p>
        </div>
        <div>
          <p className="mb-1 text-slate-400">Última actualización</p>
          <p className="font-medium">
            {lastUpdate ? new Date(lastUpdate).toLocaleString() : "Sin datos"}
          </p>
        </div>
      </div>

      {/* Tags */}
      {tags && tags.length > 0 && (
        <div className="mt-3 pt-3 border-t border-slate-800">
          <div className="flex flex-wrap gap-1">
            {tags.map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center px-2 py-0.5 rounded text-xs bg-blue-500/10 text-blue-400 ring-1 ring-blue-500/30"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
