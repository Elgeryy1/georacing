# 🔧 Guía de Integración del Sistema de Balizas

## ✅ Cambios Implementados

### 1. Tipos Actualizados (`src/types/index.ts`)

Se han expandido los tipos para soportar la especificación completa:

- **BeaconMode**: Ahora incluye `UNCONFIGURED` (nuevo)
- **ArrowDirection**: Añadidas 4 direcciones diagonales: `DOWN`, `UP_LEFT`, `UP_RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT`
- **Language**: Añadidos idiomas: `FR` (Francés), `DE` (Alemán), `IT` (Italiano), `PT` (Portugués)

### 2. Componentes Actualizados

#### `BeaconPreview.tsx`
- ✅ Soporta todas las 9 direcciones de flecha
- ✅ Añadido modo `UNCONFIGURED` con UI especial
- ✅ Traducciones para 7 idiomas en modo evacuación
- ✅ Colores actualizados según especificación

#### `NewBeaconModal.tsx`
- ✅ Selector de modo incluye `UNCONFIGURED`
- ✅ Selector de flecha con 9 opciones
- ✅ Selector de idioma con 7 opciones

#### `BeaconConfigForm.tsx` (NUEVO)
- ✅ Formulario completo de configuración individual
- ✅ Vista previa en tiempo real
- ✅ Campo de salida de evacuación
- ✅ Validaciones de zona obligatoria

#### `BeaconMetricsCard.tsx`
- ✅ Usa la nueva función `isBeaconOnline()`
- ✅ Usa la nueva función `formatLastSeen()`

### 3. Servicios Mejorados

#### `beaconService.ts`
Nuevas funciones añadidas:

```typescript
// Configurar baliza
beaconsService.configureBeacon(beaconId, config)

// Activar emergencia global
beaconsService.activateEmergencyAll(message, arrow)

// Crear baliza de prueba
beaconsService.createTestBeacon(beaconId)
```

Función movida a `utils`:
```typescript
// Ahora se importa desde utils/beaconUtils
import { isBeaconOnline } from "../utils/beaconUtils"
```

### 4. Utilidades Nuevas (`src/utils/beaconUtils.ts`)

#### Funciones de Estado
```typescript
// Verificar si está online (heartbeat < 15s)
isBeaconOnline(beacon): boolean

// Formatear última conexión
formatLastSeen(beacon): string // "Hace 5s", "Hace 2m", etc.

// Obtener estado con emoji
getBeaconStatus(beacon): { emoji, text, color }
```

#### Funciones de Traducción
```typescript
// Nombre del modo en español
getModeName(mode): string

// Nombre de dirección con emoji
getArrowName(arrow): string

// Color por defecto del modo
getModeColor(mode): string
```

#### Funciones de Filtrado
```typescript
// Filtrar por zona
filterBeaconsByZone(beacons, zone): Beacon[]

// Filtrar por estado online/offline
filterBeaconsByOnlineStatus(beacons, online): Beacon[]
```

#### Función de Estadísticas
```typescript
getBeaconStats(beacons): {
  total: number,
  online: number,
  offline: number,
  configured: number,
  unconfigured: number,
  emergency: number,
  uptime: number // porcentaje
}
```

---

## 📚 Cómo Usar las Nuevas Funcionalidades

### Ejemplo 1: Detectar Estado Online

```tsx
import { isBeaconOnline, getBeaconStatus } from "../utils/beaconUtils";

function BeaconList({ beacons }) {
  return (
    <div>
      {beacons.map(beacon => {
        const online = isBeaconOnline(beacon);
        const status = getBeaconStatus(beacon);
        
        return (
          <div key={beacon.beaconId}>
            <span>{status.emoji}</span>
            <span className={status.color}>{status.text}</span>
            {!online && <span>⚠️ Sin conexión</span>}
          </div>
        );
      })}
    </div>
  );
}
```

### Ejemplo 2: Configurar Baliza Individual

```tsx
import { BeaconConfigForm } from "../components/BeaconConfigForm";

function BeaconDetailPage({ beacon }) {
  return (
    <BeaconConfigForm 
      beacon={beacon}
      onSave={() => alert("Configuración guardada")}
      onCancel={() => navigate("/beacons")}
    />
  );
}
```

### Ejemplo 3: Activar Emergencia Global

```tsx
import { beaconsService } from "../services/beaconService";

async function activateGlobalEmergency() {
  const count = await beaconsService.activateEmergencyAll(
    "¡EMERGENCIA! Evacuar inmediatamente",
    "RIGHT" // Flecha derecha
  );
  
  console.log(`${count} balizas actualizadas`);
}
```

### Ejemplo 4: Estadísticas del Sistema

```tsx
import { getBeaconStats } from "../utils/beaconUtils";

function DashboardStats({ beacons }) {
  const stats = getBeaconStats(beacons);
  
  return (
    <div className="grid grid-cols-3 gap-4">
      <Card>
        <h3>Online</h3>
        <p>{stats.online} / {stats.total}</p>
      </Card>
      <Card>
        <h3>Uptime</h3>
        <p>{stats.uptime}%</p>
      </Card>
      <Card>
        <h3>Emergencias</h3>
        <p className="text-red-500">{stats.emergency}</p>
      </Card>
    </div>
  );
}
```

### Ejemplo 5: Filtrar por Zona

```tsx
import { filterBeaconsByZone } from "../utils/beaconUtils";

function ZoneBeacons({ beacons, zone }) {
  const zoneBeacons = filterBeaconsByZone(beacons, zone);
  
  return (
    <div>
      <h2>{zone} - {zoneBeacons.length} balizas</h2>
      {/* Renderizar balizas de la zona */}
    </div>
  );
}
```

---

## 🎯 Próximos Pasos Recomendados

### 1. Página de Detalle de Baliza
Crear `src/pages/BeaconDetail.tsx` que muestre:
- Información completa de la baliza
- Formulario de configuración (usando `BeaconConfigForm`)
- Histórico de cambios
- Métricas en tiempo real

### 2. Panel de Control Global
Mejorar `src/pages/Config.tsx` con:
- Estadísticas globales de balizas
- Botón de emergencia global
- Lista de zonas con conteo de balizas
- Configuración por lotes

### 3. Dashboard Mejorado
Añadir a `src/pages/Dashboard.tsx`:
- Widget de estado de balizas usando `getBeaconStats()`
- Alertas de balizas offline
- Mapa interactivo de balizas

### 4. Sistema de Alertas
Crear `src/hooks/useBeaconAlerts.ts`:
- Detectar cuando una baliza se desconecta
- Notificar cuando hay emergencias activas
- Alertar balizas sin configurar

### 5. Histórico de Eventos
Implementar logging de:
- Cambios de configuración
- Activaciones de emergencia
- Conexiones/desconexiones
- Cambios de modo

---

## 🧪 Testing

### Crear Baliza de Prueba

```typescript
import { beaconsService } from "./services/beaconService";

// Crear baliza de prueba
await beaconsService.createTestBeacon("BALIZA-TEST-01");
```

### Simular Estados

```typescript
// Simular baliza offline
await beaconsService.updateBeacon("BALIZA-TEST-01", {
  mode: "NORMAL",
  // No actualizar lastSeen para simular desconexión
});

// Simular emergencia
await beaconsService.updateBeacon("BALIZA-TEST-01", {
  mode: "EMERGENCY",
  message: "Accidente en Curva 3",
  color: "#FF0000",
  brightness: 100
});
```

---

## 📖 Documentación de Referencia

- **Especificación Completa**: Ver documento original de integración
- **Firestore Rules**: `firestore.rules`
- **Tipos TypeScript**: `src/types/index.ts`
- **Utilidades**: `src/utils/beaconUtils.ts`
- **Servicios**: `src/services/beaconService.ts`

---

## ⚠️ Consideraciones Importantes

1. **Heartbeat**: Las balizas se consideran offline si `lastSeen` > 15 segundos
2. **Auto-registro**: Las balizas nuevas crean su documento automáticamente en modo `UNCONFIGURED`
3. **Polling**: Las balizas consultan Firestore cada 300ms
4. **Batch Updates**: Usar `updateMultipleBeacons()` para cambios masivos
5. **Tiempo Real**: Usar `subscribeToBeacons()` para actualizaciones en vivo

---

## 🔐 Seguridad

Las reglas de Firestore actuales permiten:
- ✅ Lectura pública de balizas (para que las WPF funcionen)
- ✅ Auto-registro de balizas nuevas
- ✅ Heartbeat sin autenticación (campos `online` y `lastSeen`)
- ✅ Escritura completa solo para usuarios autenticados
- ❌ Eliminación solo para usuarios autenticados

---

## 🚀 ¡Sistema Listo!

El sistema de balizas está completamente integrado y listo para usar. Todas las funcionalidades de la especificación han sido implementadas.

Para cualquier duda, consulta los comentarios JSDoc en el código o la documentación original.
