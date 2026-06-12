# 🎯 GeoRacing Control Panel - Sistema de Balizas

Panel de control web para gestionar las balizas inteligentes del Circuit de Barcelona-Catalunya en tiempo real.

## ✨ Características

- 🔐 Autenticación con Firebase Auth
- 📊 Dashboard en tiempo real de todas las balizas
- 🎯 Control individual y masivo de balizas
- 🚨 Sistema de emergencias y evacuación global
- 🎨 Vista previa en tiempo real de las pantallas
- 🌐 Soporte multiidioma (ES, CA, EN, FR, DE, IT, PT)
- 💬 **Textos predefinidos inteligentes según modo y dirección de flecha**
- 🔄 Sincronización en tiempo real con aplicaciones WPF
- 📍 Gestión por zonas del circuito
- 📈 Estadísticas y métricas del sistema
- 🔔 Detección automática de nuevas balizas
- ⚡ Sistema de comandos remotos y reinicio de balizas
- 🖱️ Edición click-to-edit con modal interactivo

## 🚀 Stack Tecnológico

- **Frontend**: React + TypeScript + Vite
- **Estilos**: TailwindCSS
- **Base de datos**: Firebase Firestore (tiempo real)
- **Autenticación**: Firebase Auth
- **Iconos**: Lucide React
- **Integración**: Sistema WPF .NET 8

## Instalación

1. Instalar dependencias:
```bash
npm install
```

2. Configurar Firebase:
   - Copiar `.env.example` a `.env`
   - Rellenar las credenciales de Firebase

3. Ejecutar en desarrollo:
```bash
npm run dev
```

4. Compilar para producción:
```bash
npm run build
```

## 📁 Estructura del Proyecto

```
src/
├── components/         # Componentes reutilizables
│   ├── BeaconConfigForm.tsx      # Formulario de configuración
│   ├── BeaconMetricsCard.tsx     # Tarjeta de métricas
│   ├── BeaconPreview.tsx         # Vista previa de baliza
│   ├── Layout.tsx                # Layout principal
│   ├── NewBeaconModal.tsx        # Modal nueva baliza
│   └── ProtectedRoute.tsx        # Rutas protegidas
├── context/           # Contextos de React
│   └── AuthContext.tsx           # Autenticación
├── firebase/          # Configuración de Firebase
│   ├── config.ts                 # Credenciales
│   └── firebaseApp.ts            # Inicialización
├── hooks/             # Hooks personalizados
│   ├── useBeacons.ts             # Hook de balizas
│   ├── useNewBeaconDetection.ts  # Detección nuevas balizas
│   └── useZones.ts               # Gestión de zonas
├── pages/             # Páginas de la aplicación
│   ├── BeaconDetail.tsx          # Detalle de baliza
│   ├── Config.tsx                # Configuración
│   ├── ConfigAdvanced.tsx        # Panel avanzado
│   ├── Dashboard.tsx             # Dashboard principal
│   ├── Emergencies.tsx           # Control emergencias
│   ├── Login.tsx                 # Login
│   ├── Routes.tsx                # Rutas del circuito
│   ├── Statistics.tsx            # Estadísticas
│   └── ZonesMap.tsx              # Mapa de zonas
├── services/          # Servicios de backend
│   ├── beaconDetectionService.ts # Detección balizas
│   └── beaconService.ts          # CRUD de balizas
├── types/             # Tipos de TypeScript
│   └── index.ts                  # Definiciones
├── utils/             # Utilidades
│   ├── beaconMessages.ts         # Mensajes predefinidos multiidioma
│   └── beaconUtils.ts            # 15+ funciones auxiliares
```

## 🔥 Configuración de Firebase

### Firestore - Colección `beacons`

Estructura de documento:
```typescript
{
  // Identificación
  beaconId: string,                    // ID único "BALIZA-XXX"
  
  // Estado de conexión
  online: boolean,                     // ¿Conectada?
  lastSeen: Timestamp,                 // Última conexión (heartbeat cada 5s)
  
  // Configuración
  configured: boolean,                 // ¿Configurada?
  mode: BeaconMode,                   // Ver modos abajo
  arrow: ArrowDirection,              // Ver direcciones abajo
  message: string,                    // Mensaje personalizado
  color: string,                      // Color hex (#RRGGBB)
  brightness: number,                 // Brillo 0-100
  language: Language,                 // Ver idiomas abajo
  
  // Ubicación
  zone: string,                       // Zona del circuito
  evacuationExit?: string,            // Salida evacuación
  
  // Metadata
  tags: string[],                     // Etiquetas
  lastUpdatedAt: Timestamp,           // Última actualización
  firstSeen?: Timestamp               // Primera conexión
}
```

### Modos Disponibles (BeaconMode)

```typescript
type BeaconMode = 
  | "UNCONFIGURED"  // Sin configurar (gris)
  | "NORMAL"        // Operación normal (verde)
  | "CONGESTION"    // Tráfico/congestión (amarillo)
  | "EMERGENCY"     // Emergencia (rojo parpadeante)
  | "EVACUATION"    // Evacuación (rojo + flecha)
  | "MAINTENANCE"   // Mantenimiento (azul)
```

### Direcciones de Flecha (ArrowDirection)

```typescript
type ArrowDirection = 
  | "NONE"         // Sin flecha
  | "UP"           // ↑ Arriba
  | "DOWN"         // ↓ Abajo
  | "LEFT"         // ← Izquierda
  | "RIGHT"        // → Derecha
  | "UP_LEFT"      // ↖ Arriba-Izquierda
  | "UP_RIGHT"     // ↗ Arriba-Derecha
  | "DOWN_LEFT"    // ↙ Abajo-Izquierda
  | "DOWN_RIGHT"   // ↘ Abajo-Derecha
```

### Idiomas Soportados (Language)

```typescript
type Language = 
  | "ES"  // Español
  | "CA"  // Catalán
  | "EN"  // Inglés
  | "FR"  // Francés
  | "DE"  // Alemán
  | "IT"  // Italiano
  | "PT"  // Portugués
```

### Firestore - Colección `emergency_logs`

Estructura de documento:
```typescript
{
  type: "GLOBAL_EVACUATION_ON" | "GLOBAL_EVACUATION_OFF" | "ZONE_EVACUATION_ON" | "ZONE_EVACUATION_OFF",
  zone?: string,
  triggeredByUid: string,
  triggeredAt: Timestamp,
  payload: object
}
```

## 🎯 Funcionalidades

### 📊 Dashboard
- Listado completo de balizas en tiempo real
- Filtros por zona, modo y estado online/offline
- Selección múltiple para acciones masivas
- Detección automática de nuevas balizas
- Estadísticas globales del sistema

### 🔧 Configuración de Balizas
- Vista previa en tiempo real de la baliza
- Edición de todos los parámetros:
  - Modo de operación (6 modos)
  - Dirección de flecha (9 direcciones)
  - Mensaje personalizado
  - Color y brillo
  - Idioma (7 idiomas)
  - Zona y salida de evacuación
- Guardado instantáneo en Firestore

### 🚨 Sistema de Emergencias
- Activación global de evacuación (todas las balizas)
- Control por zonas específicas
- Mensajes personalizados multiidioma
- Registro de acciones críticas con logs
- Desactivación controlada

### 📈 Monitoreo y Métricas
- Estado online/offline (heartbeat < 15s)
- Tiempo desde última conexión
- Estadísticas del sistema:
  - Total de balizas
  - Balizas online/offline
  - Balizas configuradas/sin configurar
  - Balizas en emergencia
  - Porcentaje de uptime
- Métricas por baliza:
  - Batería y voltaje
  - Señal WiFi/Red
  - Temperatura
  - Conexiones activas

### 🗺️ Gestión por Zonas
- Filtrado de balizas por zona
- Activación de emergencias zonales
- Vista de mapa interactivo
- Estadísticas por zona

### 💬 Sistema de Textos Predefinidos Inteligentes

El sistema genera automáticamente mensajes apropiados cuando no se especifica un mensaje personalizado:

#### Modo NORMAL - Direcciones Inteligentes
En modo NORMAL, el texto varía según la dirección de la flecha:
- **NONE**: "Circulación Normal"
- **UP** ↑: "Continúe Recto"
- **LEFT** ←: "Gire a la Izquierda"
- **RIGHT** →: "Gire a la Derecha"
- **UP_LEFT** ↖: "Diagonal Izquierda"
- **UP_RIGHT** ↗: "Diagonal Derecha"
- **DOWN_LEFT** ↙: "Retroceda Izquierda"
- **DOWN_RIGHT** ↘: "Retroceda Derecha"
- **DOWN** ↓: "Retroceda"

#### Otros Modos
Cada modo tiene su mensaje predefinido:
- **UNCONFIGURED**: "Sistema en Configuración"
- **CONGESTION**: "⚠️ Congestión - Reduzca Velocidad"
- **EMERGENCY**: "⚠️ EMERGENCIA - PRECAUCIÓN"
- **EVACUATION**: "🚨 EVACUACIÓN - Siga las Flechas"
- **MAINTENANCE**: "🔧 Mantenimiento - Fuera de Servicio"

#### Multiidioma
Todos los mensajes disponibles en 7 idiomas:
- 🇪🇸 Español (ES)
- 🇪🇸 Catalán (CA)
- 🇬🇧 Inglés (EN)
- 🇫🇷 Francés (FR)
- 🇩🇪 Alemán (DE)
- 🇮🇹 Italiano (IT)
- 🇵🇹 Portugués (PT)

**Total**: 105 variaciones de texto (6 modos × 7 idiomas + 9 direcciones × 7 idiomas para NORMAL)

```typescript
import { getDefaultBeaconMessage } from "./utils/beaconMessages";

// Ejemplos
getDefaultBeaconMessage("NORMAL", "ES", "RIGHT");     // "Gire a la Derecha"
getDefaultBeaconMessage("NORMAL", "EN", "UP");        // "Continue Straight"
getDefaultBeaconMessage("EMERGENCY", "FR");           // "⚠️ URGENCE - PRUDENCE"
getDefaultBeaconMessage("EVACUATION", "CA");          // "🚨 EVACUACIÓ - Segueixi les Fletxes"
```

## 📚 Documentación

- **[QUICK_START.md](./QUICK_START.md)** - Guía de inicio rápido con ejemplos
- **[SMART_MESSAGES_GUIDE.md](./SMART_MESSAGES_GUIDE.md)** - 💬 Sistema de mensajes inteligentes multiidioma
- **[BEACON_INTEGRATION_GUIDE.md](./BEACON_INTEGRATION_GUIDE.md)** - Guía completa de integración
- **[INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md)** - Resumen de cambios implementados
- **[COMMAND_SYSTEM_GUIDE.md](./COMMAND_SYSTEM_GUIDE.md)** - Sistema de comandos y reinicio remoto
- **[CUSTOM_TEXT_INTEGRATION_GUIDE.md](./CUSTOM_TEXT_INTEGRATION_GUIDE.md)** - Integración de textos personalizados en WPF
- **[WPF_INTEGRATION_CHECKLIST.md](./WPF_INTEGRATION_CHECKLIST.md)** - Checklist completo para verificar integración WPF
- **[AUTH_GUIDE.md](./AUTH_GUIDE.md)** - Guía de autenticación
- **[BEACON_METRICS_GUIDE.md](./BEACON_METRICS_GUIDE.md)** - Guía de métricas
- **[FIRESTORE_SETUP.md](./FIRESTORE_SETUP.md)** - Configuración de Firestore

## 🚀 Uso Rápido

### Importar Funciones

```typescript
// Servicios
import { beaconsService, emergencyService } from "./services/beaconService";

// Hooks
import { useBeacons } from "./hooks/useBeacons";

// Utilidades
import { 
  isBeaconOnline, 
  getBeaconStats, 
  getBeaconStatus 
} from "./utils/beaconUtils";
```

### Listar Balizas

```typescript
function MyComponent() {
  const { beacons, loading } = useBeacons();
  
  return (
    <div>
      {beacons.map(beacon => (
        <div key={beacon.beaconId}>
          {beacon.beaconId} - {beacon.zone}
        </div>
      ))}
    </div>
  );
}
```

### Configurar Baliza

```typescript
await beaconsService.configureBeacon("BALIZA-01", {
  mode: "NORMAL",
  arrow: "RIGHT",
  message: "Entrada Principal",
  color: "#00FF00",
  brightness: 80,
  language: "ES"
});
```

### Activar Emergencia Global

```typescript
await emergencyService.activateGlobalEvacuation(
  beacons,
  user.uid,
  "¡EMERGENCIA! Evacuar zona",
  "SALIDA NORTE"
);
```

## 🔄 Integración con Sistema WPF

El panel web se sincroniza en tiempo real con las aplicaciones WPF de las balizas:

### Comportamiento de las Balizas
1. **Polling**: Las balizas consultan Firestore cada **300ms**
2. **Heartbeat**: Envían señal de vida cada **5 segundos**
3. **Auto-registro**: Se crean automáticamente en modo `UNCONFIGURED`
4. **Actualización**: Cambios instantáneos desde el panel web

### Detección de Estado
- **Online**: Si `lastSeen` < 15 segundos
- **Offline**: Si `lastSeen` > 15 segundos
- **Sin configurar**: Si `configured = false`

## 🧪 Testing

### Crear Baliza de Prueba

```typescript
// En la consola del navegador
await beaconsService.createTestBeacon("BALIZA-TEST-01");
```

### Activar Emergencia de Prueba

```typescript
await beaconsService.activateEmergencyAll(
  "PRUEBA DE EMERGENCIA",
  "RIGHT"
);
```

### Verificar Estadísticas

```typescript
const stats = getBeaconStats(beacons);
console.log(`Uptime: ${stats.uptime}%`);
console.log(`Online: ${stats.online}/${stats.total}`);
```

## 🛠️ Desarrollo

### Scripts Disponibles

```bash
# Desarrollo
npm run dev

# Compilar
npm run build

# Vista previa de producción
npm run preview

# Linter
npm run lint

# Type checking
npm run type-check
```

### Crear Usuario Administrador

```bash
npm run create-admin
```

### Crear Balizas de Ejemplo

```bash
node scripts/create-beacons.js
```

## 🔐 Seguridad

Las reglas de Firestore permiten:
- ✅ Lectura pública de balizas (para aplicaciones WPF)
- ✅ Auto-registro de nuevas balizas
- ✅ Heartbeat sin autenticación (`online`, `lastSeen`)
- ✅ Escritura completa para usuarios autenticados
- ❌ Eliminación solo para usuarios autenticados

## 📦 Dependencias Principales

```json
{
  "react": "^18.x",
  "react-router-dom": "^6.x",
  "firebase": "^10.x",
  "lucide-react": "^0.x",
  "tailwindcss": "^3.x",
  "typescript": "^5.x",
  "vite": "^5.x"
}
```

## 🌐 Deploy

### Firebase Hosting

```bash
npm run build
firebase deploy --only hosting
```

### Vercel / Netlify

El proyecto está configurado para deploy automático con Vite.

## 🤝 Contribución

Este es un proyecto interno de GeoRacing para el Circuit de Barcelona-Catalunya.

## 📄 Licencia

Propiedad de GeoRacing - Circuit de Barcelona-Catalunya

---

**Versión**: 2.0.0  
**Fecha**: Noviembre 2025  
**Estado**: ✅ Producción
