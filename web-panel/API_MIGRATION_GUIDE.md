# Sistema de Configuración de Balizas - API Local

## 🎯 Resumen

El panel de control ahora utiliza **100% API local** (Windows Server + SQL Server) para configurar balizas. No depende de Firebase para la configuración de balizas.

---

## 🔌 Endpoints API

### Base URLs

- **API Base (SQL Server):** `http://192.168.1.99:3000/api`
- **API Comandos:** `http://192.168.1.99:4000/api`

### Crear Comando de Configuración

```http
POST http://192.168.1.99:4000/api/commands
Content-Type: application/json

{
  "beaconId": "B-001",
  "command": "UPDATE_CONFIG",
  "value": {
    "mode": "NORMAL",
    "arrow": "UP",
    "zone": "A",
    "brightness": 80,
    "message": "VÍA LIBRE",
    "language": "ES",
    "color": "#00FFAA",
    "evacuationExit": ""
  }
}
```

### Consultar Balizas

```http
GET http://192.168.1.99:3000/api/beacons
GET http://192.168.1.99:3000/api/beacons/:id
```

---

## 📝 Flujo de Configuración

### 1. Usuario Edita Baliza en el Panel

- Abre modal de edición
- Cambia modo, flecha, brillo, mensaje, etc.
- Click en "Guardar Cambios"

### 2. Sistema Envía Comando

```typescript
await api.sendUpdateConfig(beaconId, {
  mode: "NORMAL",
  arrow: "UP",
  brightness: 80,
  message: "VÍA LIBRE",
  language: "ES",
  color: "#00FFAA"
});
```

### 3. Backend Registra Comando

- Se guarda en tabla `Commands`
- Baliza WPF lo recogerá en próximo polling

### 4. Actualización de Base de Datos

```typescript
await api.patchBeacon(beaconId, updates);
```

- Se actualiza tabla `Beacons` con nueva configuración
- Panel refleja cambios en 300ms (polling rápido)

---

## 🎨 Sistema de Notificaciones

### Toast Messages

El sistema muestra notificaciones automáticas:

✅ **Success:** "Configuración enviada correctamente"  
❌ **Error:** "Error al contactar con la baliza"  
⚠️ **Warning:** "Enviando configuración..."  
ℹ️ **Info:** "Procesando solicitud..."

### Uso en Componentes

```typescript
import { useToast } from "../context/ToastContext";

const { showToast } = useToast();

// Mostrar notificación
showToast("Configuración aplicada", "success");
showToast("Error al guardar", "error");
showToast("Procesando...", "info");
showToast("Atención requerida", "warning");
```

---

## 🔄 Acciones Implementadas

### Edición Individual

- **Componente:** `BeaconEditModal`
- **Acción:** Cambiar configuración de una baliza
- **API:** `POST /api/commands` + `PATCH /api/beacons/:id`

### Acciones Masivas

- **Componente:** `Dashboard` (Panel lateral)
- **Acción:** Aplicar configuración a múltiples balizas seleccionadas
- **API:** `POST /api/commands` (múltiple) + `PATCH /api/beacons/:id` (batch)

### Reinicio de Sistema

- **Componente:** `Dashboard`, `BeaconEditModal`
- **Acción:** Reiniciar Windows de la baliza
- **API:** `POST /api/commands` con `command: "RESTART"`

### Nueva Baliza Detectada

- **Componente:** `NewBeaconModal`
- **Acción:** Configurar baliza nueva no configurada
- **API:** `POST /api/commands` + `POST /api/beacons` (upsert)

---

## ⚙️ Configuración

### Variables de Entorno

Crear archivo `.env` en la raíz:

```env
# API Endpoints
VITE_API_BASE_URL=http://192.168.1.99:3000/api
VITE_COMMAND_API_URL=http://192.168.1.99:4000/api

# Firebase (solo autenticación)
VITE_FIREBASE_API_KEY=your_key
VITE_FIREBASE_AUTH_DOMAIN=your_domain
VITE_FIREBASE_PROJECT_ID=your_project
VITE_FIREBASE_STORAGE_BUCKET=your_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender
VITE_FIREBASE_APP_ID=your_app_id
```

### Polling Interval

El sistema consulta cambios cada **300ms**:

```typescript
// src/hooks/useBeacons.ts
const unsubscribe = beaconsService.subscribeToBeacons((beaconsData) => {
  setBeacons(beaconsData);
  setLoading(false);
}, 300); // ← 300ms polling
```

Para cambiar el intervalo, modificar el segundo parámetro.

---

## 📊 Estructura de Datos

### BeaconUpdate

```typescript
interface BeaconUpdate {
  mode?: "UNCONFIGURED" | "NORMAL" | "CONGESTION" | "EMERGENCY" | "EVACUATION" | "MAINTENANCE";
  arrow?: "NONE" | "UP" | "DOWN" | "LEFT" | "RIGHT" | "UP_LEFT" | "UP_RIGHT" | "DOWN_LEFT" | "DOWN_RIGHT";
  message?: string;
  color?: string; // Hexadecimal: "#RRGGBB"
  brightness?: number; // 0-100
  language?: "ES" | "EN" | "FR" | "DE" | "IT" | "PT" | "CA";
  evacuationExit?: string;
}
```

### Comando Completo

```json
{
  "beaconId": "B-001",
  "command": "UPDATE_CONFIG",
  "value": {
    "mode": "NORMAL",
    "arrow": "NONE",
    "zone": "GRADA-A",
    "brightness": 90,
    "message": "ACCESO PRINCIPAL",
    "language": "ES",
    "color": "#00FFAA",
    "evacuationExit": ""
  }
}
```

**⚠️ IMPORTANTE:** Siempre enviar JSON completo en `value`, nunca incompleto.

---

## 🚀 Funciones Principales

### `api.sendUpdateConfig()`

```typescript
await api.sendUpdateConfig(beaconId, {
  mode: "NORMAL",
  arrow: "UP",
  brightness: 80,
  message: "VÍA LIBRE",
  language: "ES"
});
```

### `beaconsService.updateBeacon()`

```typescript
// Envía comando + actualiza BD
await beaconsService.updateBeacon(beaconId, updates);
```

### `beaconsService.updateMultipleBeacons()`

```typescript
// Aplica a múltiples balizas
await beaconsService.updateMultipleBeacons(
  ["B-001", "B-002", "B-003"],
  {
    mode: "EMERGENCY",
    brightness: 100,
    color: "#FF0000"
  }
);
```

### `beaconsService.configureBeacon()`

```typescript
// Primera configuración de baliza nueva
await beaconsService.configureBeacon(beaconId, {
  mode: "NORMAL",
  arrow: "NONE",
  message: "Bienvenido",
  brightness: 90,
  language: "ES"
});
```

---

## 🔍 Debugging

### Verificar Comandos Enviados

```bash
# Ver logs del servidor
tail -f /path/to/server/logs

# Consultar comandos pendientes
GET http://192.168.1.99:4000/api/commands/pending/:beaconId
```

### Verificar Estado de Baliza

```bash
# Consultar baliza específica
GET http://192.168.1.99:3000/api/beacons/B-001
```

### Consola del Navegador

```javascript
// Ver notificaciones en consola
console.log("Comando enviado:", beaconId, updates);
```

---

## ✅ Checklist de Implementación

- [x] Crear función `sendUpdateConfig()` en `apiClient.ts`
- [x] Actualizar `beaconService.ts` para enviar comandos
- [x] Adaptar `BeaconEditModal` con notificaciones
- [x] Adaptar `Dashboard` (acciones masivas)
- [x] Adaptar `NewBeaconModal` (configuración inicial)
- [x] Implementar sistema de notificaciones (Toast)
- [x] Integrar `ToastProvider` en `App.tsx`
- [x] Añadir animaciones CSS para toasts
- [x] Configurar variables de entorno (vite-env.d.ts)
- [x] Documentación completa

---

## 📚 Recursos

- **Backend API:** `server/beacons.ts`, `server/commands.ts`
- **Cliente API:** `src/services/apiClient.ts`
- **Servicio Balizas:** `src/services/beaconService.ts`
- **Notificaciones:** `src/context/ToastContext.tsx`
- **Hook Balizas:** `src/hooks/useBeacons.ts`

---

## 🆘 Soporte

Para problemas o dudas:

1. Verificar logs del servidor Windows
2. Revisar consola del navegador (F12)
3. Comprobar conectividad a API (`http://192.168.1.99:4000`)
4. Verificar que SQL Server esté activo

---

**Última actualización:** 17 de noviembre de 2025
