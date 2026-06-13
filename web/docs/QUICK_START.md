# 🚀 Quick Start - Sistema de Beacons

## 📦 Importaciones Principales

```typescript
// Servicios
import { beaconsService, emergencyService } from "../services/beaconService";

// Hooks
import { useBeacons } from "../hooks/useBeacons";

// Utilidades
import { 
  isBeaconOnline, 
  getBeaconStats, 
  getBeaconStatus,
  formatLastSeen,
  filterBeaconsByZone
} from "../utils/beaconUtils";

// Componentes
import { BeaconPreview } from "../components/BeaconPreview";
import { BeaconConfigForm } from "../components/BeaconConfigForm";
import { BeaconMetricsCard } from "../components/BeaconMetricsCard";
```

---

## 🎯 Casos de Uso Comunes

### 1. Listar Beacons en Tiempo Real

```tsx
function MyComponent() {
  const { beacons, loading } = useBeacons();
  
  if (loading) return <div>Cargando...</div>;
  
  return (
    <div>
      <h2>Beacons: {beacons.length}</h2>
      {beacons.map(beacon => (
        <div key={beacon.beaconId}>
          {beacon.beaconId} - {beacon.zone}
        </div>
      ))}
    </div>
  );
}
```

### 2. Verificar Estado Online/Offline

```tsx
function BeaconStatus({ beacon }) {
  const online = isBeaconOnline(beacon);
  const status = getBeaconStatus(beacon);
  
  return (
    <div className={status.color}>
      <span>{status.emoji}</span>
      <span>{status.text}</span>
      <span>{online ? "Conectada" : "Desconectada"}</span>
    </div>
  );
}
```

### 3. Mostrar Estadísticas del Sistema

```tsx
function SystemStats() {
  const { beacons } = useBeacons();
  const stats = getBeaconStats(beacons);
  
  return (
    <div className="stats-grid">
      <div>Total: {stats.total}</div>
      <div>Online: {stats.online}</div>
      <div>Offline: {stats.offline}</div>
      <div>Uptime: {stats.uptime}%</div>
      <div>Emergencias: {stats.emergency}</div>
    </div>
  );
}
```

### 4. Configurar una Beacon

```tsx
function ConfigureBeacon({ beacon }) {
  const handleSave = async () => {
    await beaconsService.configureBeacon(beacon.beaconId, {
      mode: "NORMAL",
      arrow: "RIGHT",
      message: "Entrada Principal",
      color: "#00FF00",
      brightness: 80,
      language: "ES"
    });
    alert("Beacon configurada");
  };
  
  return <button onClick={handleSave}>Configurar</button>;
}
```

### 5. Vista Previa de Beacon

```tsx
function PreviewBeacon({ beacon }) {
  return (
    <BeaconPreview
      mode={beacon.mode}
      arrow={beacon.arrow}
      message={beacon.message}
      color={beacon.color}
      language={beacon.language}
      evacuationExit={beacon.evacuationExit}
    />
  );
}
```

### 6. Activar Emergencia Global

```tsx
function EmergencyButton() {
  const { beacons } = useBeacons();
  const { user } = useAuth();
  
  const handleEmergency = async () => {
    if (!user) return;
    
    if (confirm("¿Activar emergencia global?")) {
      await emergencyService.activateGlobalEvacuation(
        beacons,
        user.uid,
        "¡EMERGENCIA! Evacuar inmediatamente",
        "SALIDA NORTE"
      );
    }
  };
  
  return (
    <button onClick={handleEmergency} className="emergency-btn">
      🚨 ACTIVAR EMERGENCIA
    </button>
  );
}
```

### 7. Filtrar Beacons por Zona

```tsx
function ZoneBeacons({ zone }) {
  const { beacons } = useBeacons();
  const zoneBeacons = filterBeaconsByZone(beacons, zone);
  
  return (
    <div>
      <h2>{zone}</h2>
      <p>{zoneBeacons.length} beacons</p>
      {zoneBeacons.map(beacon => (
        <BeaconMetricsCard key={beacon.beaconId} beacon={beacon} />
      ))}
    </div>
  );
}
```

### 8. Formatear Última Conexión

```tsx
function LastSeenInfo({ beacon }) {
  const lastSeen = formatLastSeen(beacon);
  
  return (
    <div>
      Última conexión: {lastSeen}
    </div>
  );
}
```

### 9. Formulario Completo de Configuración

```tsx
function BeaconEditor({ beacon }) {
  return (
    <BeaconConfigForm
      beacon={beacon}
      onSave={() => {
        console.log("Guardado exitoso");
        // Navegar o cerrar modal
      }}
      onCancel={() => {
        // Cerrar sin guardar
      }}
    />
  );
}
```

### 10. Actualizar Modo de Beacon

```tsx
function ChangeModeButton({ beaconId, newMode }) {
  const handleChange = async () => {
    await beaconsService.setBeaconMode(beaconId, newMode);
    console.log(`Modo cambiado a ${newMode}`);
  };
  
  return (
    <button onClick={handleChange}>
      Cambiar a {newMode}
    </button>
  );
}
```

---

## 🎨 Ejemplos de UI Completos

### Dashboard con Todo

```tsx
import { useBeacons } from "../hooks/useBeacons";
import { getBeaconStats, getBeaconStatus } from "../utils/beaconUtils";

export function BeaconDashboard() {
  const { beacons, loading } = useBeacons();
  const stats = getBeaconStats(beacons);
  
  if (loading) return <div>Cargando...</div>;
  
  return (
    <div className="dashboard">
      {/* Estadísticas */}
      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total</h3>
          <p>{stats.total}</p>
        </div>
        <div className="stat-card">
          <h3>Online</h3>
          <p>{stats.online}</p>
        </div>
        <div className="stat-card">
          <h3>Uptime</h3>
          <p>{stats.uptime}%</p>
        </div>
        <div className="stat-card">
          <h3>Emergencias</h3>
          <p className="text-red">{stats.emergency}</p>
        </div>
      </div>
      
      {/* Lista de Beacons */}
      <div className="beacon-list">
        <h2>Estado de Beacons</h2>
        <table>
          <thead>
            <tr>
              <th>Estado</th>
              <th>ID</th>
              <th>Zona</th>
              <th>Modo</th>
            </tr>
          </thead>
          <tbody>
            {beacons.map(beacon => {
              const status = getBeaconStatus(beacon);
              return (
                <tr key={beacon.beaconId}>
                  <td>
                    <span className={status.color}>
                      {status.emoji} {status.text}
                    </span>
                  </td>
                  <td>{beacon.beaconId}</td>
                  <td>{beacon.zone}</td>
                  <td>{beacon.mode}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

### Panel de Control de Emergencias

```tsx
import { useBeacons } from "../hooks/useBeacons";
import { useAuth } from "../context/AuthContext";
import { emergencyService } from "../services/beaconService";
import { getBeaconStats } from "../utils/beaconUtils";

export function EmergencyControl() {
  const { beacons } = useBeacons();
  const { user } = useAuth();
  const stats = getBeaconStats(beacons);
  
  const activateEmergency = async () => {
    if (!user) return;
    
    await emergencyService.activateGlobalEvacuation(
      beacons,
      user.uid,
      "¡EMERGENCIA! Evacuar zona",
      "SALIDA PRINCIPAL"
    );
  };
  
  const deactivateEmergency = async () => {
    if (!user) return;
    
    await emergencyService.deactivateGlobalEvacuation(
      beacons,
      user.uid
    );
  };
  
  return (
    <div className="emergency-panel">
      <div className="alert">
        {stats.emergency > 0 && (
          <p>⚠️ {stats.emergency} beacon(s) en emergencia</p>
        )}
      </div>
      
      <div className="controls">
        <button 
          onClick={activateEmergency}
          className="btn-danger"
        >
          🚨 ACTIVAR EMERGENCIA GLOBAL
        </button>
        
        <button 
          onClick={deactivateEmergency}
          className="btn-success"
          disabled={stats.emergency === 0}
        >
          ✅ DESACTIVAR EMERGENCIAS
        </button>
      </div>
    </div>
  );
}
```

---

## 🧪 Testing Rápido

### Crear Beacon de Prueba

```typescript
import { beaconsService } from "../services/beaconService";

// En la consola del navegador:
await beaconsService.createTestBeacon("BEACON-DEMO-01");
```

### Verificar Estado

```typescript
import { isBeaconOnline } from "../utils/beaconUtils";

const beacon = beacons[0];
console.log("Online:", isBeaconOnline(beacon));
```

### Activar Emergencia de Prueba

```typescript
await beaconsService.activateEmergencyAll(
  "PRUEBA DE EMERGENCIA",
  "RIGHT"
);
```

---

## 📖 Referencia Rápida

### Funciones Principales

| Función | Uso |
|---------|-----|
| `useBeacons()` | Hook para obtener beacons en tiempo real |
| `isBeaconOnline(beacon)` | ¿Está online? (< 15s) |
| `getBeaconStats(beacons)` | Estadísticas del sistema |
| `getBeaconStatus(beacon)` | Estado visual (emoji, texto, color) |
| `formatLastSeen(beacon)` | "Hace 5s", "Hace 2m", etc. |
| `filterBeaconsByZone(beacons, zone)` | Filtrar por zona |
| `beaconsService.configureBeacon()` | Configurar beacon |
| `beaconsService.activateEmergencyAll()` | Emergencia global |
| `emergencyService.activateGlobalEvacuation()` | Evacuación global |

### Componentes

| Componente | Props |
|------------|-------|
| `<BeaconPreview>` | mode, arrow, message, color, language, evacuationExit |
| `<BeaconConfigForm>` | beacon, onSave, onCancel |
| `<BeaconMetricsCard>` | beacon |

---

## 🎯 Próximos Pasos

1. ✅ Importar componentes necesarios
2. ✅ Usar `useBeacons()` para obtener datos
3. ✅ Aplicar funciones de utilidad
4. ✅ Renderizar con componentes
5. ✅ Configurar y gestionar beacons

**¡Todo listo para usar! 🚀**
