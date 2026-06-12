# Configuración de Firestore para GeoRacing

## Estructura de Base de Datos

### Colección: `beacons`

Cada documento representa una baliza física. El ID del documento debe coincidir con el `beaconId` de la baliza.

**Ruta:** `beacons/{beaconId}`

**Ejemplo de documento:**
```javascript
{
  beaconId: "BALIZA-GRADA-G-ENTRADA",
  zone: "GRADA-G",
  mode: "NORMAL",
  arrow: "RIGHT",
  message: "Acceso Principal",
  color: "#00FFAA",
  brightness: 90,
  language: "ES",
  evacuationExit: "",
  lastUpdatedAt: timestamp,
  lastSeen: timestamp,
  online: true,
  tags: ["entrada", "grada", "principal"]
}
```

**Campos:**

| Campo | Tipo | Valores | Descripción |
|-------|------|---------|-------------|
| `beaconId` | string | - | ID único de la baliza |
| `zone` | string | - | Zona del circuito (ej: "GRADA-G") |
| `mode` | string | NORMAL, CONGESTION, EMERGENCY, EVACUATION, MAINTENANCE | Modo actual de operación |
| `arrow` | string | NONE, LEFT, RIGHT, UP | Dirección de la flecha |
| `message` | string | - | Mensaje a mostrar |
| `color` | string | hex color | Color de fondo (#RRGGBB) |
| `brightness` | number | 0-100 | Nivel de brillo |
| `language` | string | ES, CAT, EN | Idioma |
| `evacuationExit` | string | - | Salida de evacuación |
| `lastUpdatedAt` | timestamp | - | Última actualización desde panel |
| `lastSeen` | timestamp | - | Último heartbeat de la baliza |
| `online` | boolean | - | Estado de conexión |
| `tags` | array | - | Etiquetas para filtrado |

### Colección: `emergency_logs`

Registro de todas las acciones críticas de emergencia.

**Ruta:** `emergency_logs/{autoId}`

**Ejemplo de documento:**
```javascript
{
  type: "GLOBAL_EVACUATION_ON",
  zone: null,
  triggeredByUid: "user123",
  triggeredAt: timestamp,
  payload: {
    message: "EVACUACIÓN EN CURSO",
    evacuationExit: "SALIDA 3",
    beaconCount: 45
  }
}
```

**Tipos de eventos:**
- `GLOBAL_EVACUATION_ON`: Evacuación global activada
- `GLOBAL_EVACUATION_OFF`: Evacuación global desactivada
- `ZONE_EVACUATION_ON`: Evacuación de zona activada
- `ZONE_EVACUATION_OFF`: Evacuación de zona desactivada

## Reglas de Seguridad

### Para Balizas (BeaconDisplay en Windows)

Las balizas pueden:
- ✅ **Leer** su propio documento para obtener configuración actualizada
- ✅ **Actualizar** campos de heartbeat (`lastSeen`, `online`)
- ❌ No pueden crear ni eliminar documentos

### Para Panel de Control (Usuarios Autenticados)

Los usuarios autenticados pueden:
- ✅ **Leer** todas las balizas
- ✅ **Crear** nuevas balizas
- ✅ **Actualizar** cualquier campo de cualquier baliza
- ✅ **Eliminar** balizas
- ✅ **Leer y crear** logs de emergencia

## Integración con BeaconDisplay (Windows)

### 1. Suscripción en Tiempo Real

La aplicación Windows debe suscribirse a su documento:

```javascript
// Ejemplo en JavaScript/TypeScript
const beaconRef = doc(db, "beacons", "BALIZA-GRADA-G-ENTRADA");

onSnapshot(beaconRef, (doc) => {
  if (doc.exists()) {
    const data = doc.data();
    // Actualizar UI según:
    // - data.mode
    // - data.arrow
    // - data.message
    // - data.color
    // - data.brightness
    // - data.language
    // - data.evacuationExit
  }
});
```

### 2. Heartbeat

La baliza debe actualizar su estado cada 30-60 segundos:

```javascript
setInterval(async () => {
  await updateDoc(beaconRef, {
    lastSeen: serverTimestamp(),
    online: true
  });
}, 30000); // Cada 30 segundos
```

### 3. Estados de la Baliza

#### NORMAL
- Mostrar `message` con `color` de fondo
- Mostrar `arrow` si no es NONE
- Brillo según `brightness`

#### CONGESTION
- Fondo naranja (#FFA500)
- Mostrar "AFORO COMPLETO"
- Mostrar `arrow`

#### EMERGENCY
- Fondo naranja fuerte (#FF6600)
- Mostrar `message`
- Brillo al 100%

#### EVACUATION
- Fondo rojo (#FF0000)
- Mensaje según idioma:
  - ES: "EVACUACIÓN EN CURSO. SIGA LAS FLECHAS."
  - CAT: "EVACUACIÓ EN CURS. SEGUEIX LES FLETXES."
  - EN: "EVACUATION IN PROGRESS. FOLLOW THE ARROWS."
- Mostrar `evacuationExit` en grande
- Brillo al 100%

#### MAINTENANCE
- Fondo gris (#808080)
- Solo mostrar hora actual en grande
- Mostrar "MANTENIMIENTO"

## Crear Balizas Iniciales

Desde Firebase Console o mediante script:

```javascript
import { collection, doc, setDoc, serverTimestamp } from "firebase/firestore";

const beacons = [
  {
    beaconId: "BALIZA-GRADA-G-ENTRADA",
    zone: "GRADA-G",
    mode: "NORMAL",
    arrow: "RIGHT",
    message: "Acceso Principal",
    color: "#00FFAA",
    brightness: 90,
    language: "ES",
    evacuationExit: "",
    lastUpdatedAt: serverTimestamp(),
    lastSeen: serverTimestamp(),
    online: false,
    tags: ["entrada", "grada"]
  },
  {
    beaconId: "BALIZA-GRADA-A-SECTOR-1",
    zone: "GRADA-A",
    mode: "NORMAL",
    arrow: "UP",
    message: "Sector 1 - Planta Superior",
    color: "#00FFAA",
    brightness: 90,
    language: "ES",
    evacuationExit: "",
    lastUpdatedAt: serverTimestamp(),
    lastSeen: serverTimestamp(),
    online: false,
    tags: ["grada", "sector1"]
  }
  // ... más balizas
];

// Crear documentos
for (const beacon of beacons) {
  await setDoc(doc(db, "beacons", beacon.beaconId), beacon);
}
```

## Desplegar Configuración

Para desplegar las reglas e índices:

```bash
firebase deploy --only firestore
```

Para desplegar solo las reglas:

```bash
firebase deploy --only firestore:rules
```

Para desplegar solo los índices:

```bash
firebase deploy --only firestore:indexes
```

## Monitoreo

### Panel de Control
- Dashboard muestra estado online/offline de cada baliza
- Última conexión (`lastSeen`)
- Modo actual

### Detección de Balizas Offline

Las balizas que no envían heartbeat en más de 2 minutos deben marcarse como offline. Esto puede hacerse con:

1. **Cloud Function** (recomendado):
```javascript
// Ejecutar cada minuto
const cutoff = Date.now() - (2 * 60 * 1000); // 2 minutos
const beaconsRef = collection(db, "beacons");
const q = query(beaconsRef, where("lastSeen", "<", cutoff), where("online", "==", true));

const snapshot = await getDocs(q);
snapshot.forEach(async (doc) => {
  await updateDoc(doc.ref, { online: false });
});
```

2. **Desde el Panel** (alternativa simple):
Verificar en el hook `useBeacons` y actualizar localmente la visualización.

## Seguridad Adicional

### Validación de Datos (Opcional)

Puedes agregar validación en las reglas:

```javascript
match /beacons/{beaconId} {
  allow update: if request.resource.data.mode in ["NORMAL", "CONGESTION", "EMERGENCY", "EVACUATION", "MAINTENANCE"]
    && request.resource.data.arrow in ["NONE", "LEFT", "RIGHT", "UP"]
    && request.resource.data.language in ["ES", "CAT", "EN"]
    && request.resource.data.brightness >= 0 
    && request.resource.data.brightness <= 100;
}
```

### Rate Limiting

Para evitar abuso, considera implementar rate limiting en Cloud Functions o usar App Check.

## Backup

Configura backups automáticos desde Firebase Console:
- Firestore → Backups
- Programar backups diarios
