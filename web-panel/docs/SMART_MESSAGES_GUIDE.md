# 💬 Guía de Mensajes Inteligentes - Sistema Automático

## 🎯 ¿Qué es esto?

El sistema de mensajes inteligentes genera **automáticamente textos apropiados** para las balizas cuando no se especifica un mensaje personalizado. Los textos varían según:

1. **Modo de operación** (NORMAL, EMERGENCY, etc.)
2. **Dirección de la flecha** (solo en modo NORMAL)
3. **Idioma seleccionado** (7 idiomas disponibles)

---

## ✨ Características

### 🧠 Inteligencia Direccional (Modo NORMAL)

En modo NORMAL, el mensaje cambia automáticamente según la dirección de la flecha para **guiar al usuario**:

| Flecha | Español | Catalán | Inglés |
|--------|---------|---------|--------|
| NONE | Circulación Normal | Circulació Normal | Normal Traffic |
| UP ↑ | Continúe Recto | Continuï Recte | Continue Straight |
| DOWN ↓ | Retroceda | Retrocedeixi | Go Back |
| LEFT ← | Gire a la Izquierda | Giri a l'Esquerra | Turn Left |
| RIGHT → | Gire a la Derecha | Giri a la Dreta | Turn Right |
| UP_LEFT ↖ | Diagonal Izquierda | Diagonal Esquerra | Diagonal Left |
| UP_RIGHT ↗ | Diagonal Derecha | Diagonal Dreta | Diagonal Right |
| DOWN_LEFT ↙ | Retroceda Izquierda | Retrocedeixi Esquerra | Back Left |
| DOWN_RIGHT ↘ | Retroceda Derecha | Retrocedeixi Dreta | Back Right |

### 🌐 Soporte Multiidioma

Todos los textos disponibles en **7 idiomas**:

- 🇪🇸 **ES** - Español
- 🇪🇸 **CA** - Catalán
- 🇬🇧 **EN** - Inglés
- 🇫🇷 **FR** - Francés
- 🇩🇪 **DE** - Alemán
- 🇮🇹 **IT** - Italiano
- 🇵🇹 **PT** - Portugués

### 📊 Mensajes por Modo

#### UNCONFIGURED
```
ES: "Sistema en Configuración"
CA: "Sistema en Configuració"
EN: "System in Configuration"
FR: "Système en Configuration"
DE: "System in Konfiguration"
IT: "Sistema in Configurazione"
PT: "Sistema em Configuração"
```

#### NORMAL
Varía según flecha (ver tabla arriba)

#### CONGESTION
```
ES: "⚠️ Congestión\nReduzca Velocidad"
CA: "⚠️ Congestió\nRedueixi Velocitat"
EN: "⚠️ Congestion\nReduce Speed"
FR: "⚠️ Congestion\nRalentir"
DE: "⚠️ Stau\nGeschwindigkeit Reduzieren"
IT: "⚠️ Congestione\nRidurre Velocità"
PT: "⚠️ Congestionamento\nReduza Velocidade"
```

#### EMERGENCY
```
ES: "⚠️ EMERGENCIA\nPRECAUCIÓN"
CA: "⚠️ EMERGÈNCIA\nPRECAUCIÓ"
EN: "⚠️ EMERGENCY\nCAUTION"
FR: "⚠️ URGENCE\nPRUDENCE"
DE: "⚠️ NOTFALL\nVORSICHT"
IT: "⚠️ EMERGENZA\nATTENZIONE"
PT: "⚠️ EMERGÊNCIA\nCUIDADO"
```

#### EVACUATION
```
ES: "🚨 EVACUACIÓN\nSiga las Flechas"
CA: "🚨 EVACUACIÓ\nSegueixi les Fletxes"
EN: "🚨 EVACUATION\nFollow the Arrows"
FR: "🚨 ÉVACUATION\nSuivez les Flèches"
DE: "🚨 EVAKUIERUNG\nFolgen Sie den Pfeilen"
IT: "🚨 EVACUAZIONE\nSegui le Frecce"
PT: "🚨 EVACUAÇÃO\nSiga as Setas"
```

#### MAINTENANCE
```
ES: "🔧 Mantenimiento\nFuera de Servicio"
CA: "🔧 Manteniment\nFora de Servei"
EN: "🔧 Maintenance\nOut of Service"
FR: "🔧 Maintenance\nHors Service"
DE: "🔧 Wartung\nAußer Betrieb"
IT: "🔧 Manutenzione\nFuori Servizio"
PT: "🔧 Manutenção\nFora de Serviço"
```

---

## 💻 Uso en el Código

### Importar la Función

```typescript
import { getDefaultBeaconMessage } from "./utils/beaconMessages";
```

### Ejemplos de Uso

```typescript
// Modo NORMAL con diferentes flechas
getDefaultBeaconMessage("NORMAL", "ES", "UP");
// → "Continúe Recto"

getDefaultBeaconMessage("NORMAL", "EN", "LEFT");
// → "Turn Left"

getDefaultBeaconMessage("NORMAL", "FR", "RIGHT");
// → "Tournez à Droite"

// Modo NORMAL sin flecha
getDefaultBeaconMessage("NORMAL", "ES", "NONE");
// → "Circulación Normal"

// Otros modos (no necesitan arrow)
getDefaultBeaconMessage("EMERGENCY", "CA");
// → "⚠️ EMERGÈNCIA\nPRECAUCIÓ"

getDefaultBeaconMessage("EVACUATION", "DE");
// → "🚨 EVAKUIERUNG\nFolgen Sie den Pfeilen"

getDefaultBeaconMessage("CONGESTION", "IT");
// → "⚠️ Congestione\nRidurre Velocità"
```

### Integración en Componentes

```typescript
const MyBeaconComponent: React.FC<Props> = ({ beacon }) => {
  // Obtener mensaje apropiado
  const displayMessage = beacon.message 
    ? beacon.message  // Mensaje personalizado
    : getDefaultBeaconMessage(beacon.mode, beacon.language, beacon.arrow);
  
  return (
    <div>
      <h1>{displayMessage}</h1>
    </div>
  );
};
```

---

## 📐 Arquitectura del Sistema

### Archivo Principal
```
src/utils/beaconMessages.ts
```

### Estructura del Código

```typescript
// Función principal exportada
export const getDefaultBeaconMessage = (
  mode: BeaconMode,
  language: Language,
  arrow: ArrowDirection = "NONE"
): string => {
  // Si es NORMAL, usar lógica direccional
  if (mode === "NORMAL") {
    return getNormalModeMessage(arrow, language);
  }
  
  // Otros modos: mensajes estándar
  return standardMessages[mode][language];
};

// Función auxiliar para NORMAL mode
const getNormalModeMessage = (
  arrow: ArrowDirection,
  language: Language
): string => {
  return normalMessages[arrow][language];
};
```

### Datos

**Mensajes estándar**: 6 modos × 7 idiomas = **42 textos**

**Mensajes direccionales**: 9 direcciones × 7 idiomas = **63 textos**

**TOTAL**: **105 variaciones de texto**

---

## 🎨 Uso en el Panel Web

### Vista Previa Automática

El componente `BeaconPreview` usa automáticamente los mensajes predefinidos:

```typescript
// src/components/BeaconPreview.tsx
const getDisplayMessage = (): string => {
  // Si hay mensaje personalizado, usarlo
  if (message && message.trim()) {
    return message;
  }
  
  // Si no, usar texto predefinido
  return getDefaultBeaconMessage(mode, language, arrow);
};
```

### Edición Click-to-Edit

En `BeaconEditModal`, al cambiar el modo o la flecha sin mensaje personalizado, la vista previa actualiza automáticamente el texto sugerido.

**Flujo**:
1. Usuario selecciona modo "NORMAL"
2. Usuario selecciona flecha "RIGHT" →
3. Usuario selecciona idioma "ES"
4. Vista previa muestra: **"Gire a la Derecha"**

Si el usuario escribe un mensaje personalizado, **ese tiene prioridad**.

---

## 🔄 Integración con WPF

### Implementación C#

El mismo sistema debe implementarse en las aplicaciones WPF. Ver:
- **[WPF_INTEGRATION_CHECKLIST.md](./WPF_INTEGRATION_CHECKLIST.md)** - Código completo en C#
- **[CUSTOM_TEXT_INTEGRATION_GUIDE.md](./CUSTOM_TEXT_INTEGRATION_GUIDE.md)** - Guía de integración

### Sincronización

```
Panel Web → Firestore → WPF

Usuario configura:
- mode: "NORMAL"
- arrow: "LEFT"
- language: "EN"
- message: "" (vacío)

WPF detecta y muestra:
"Turn Left"
```

---

## 📊 Estadísticas del Sistema

| Métrica | Valor |
|---------|-------|
| Modos | 6 |
| Idiomas | 7 |
| Direcciones de flecha | 9 |
| Mensajes estándar | 42 |
| Mensajes direccionales (NORMAL) | 63 |
| **Total de variaciones** | **105** |

---

## 🧪 Casos de Prueba

### Test 1: NORMAL sin flecha
```json
{ "mode": "NORMAL", "arrow": "NONE", "language": "ES", "message": "" }
```
**Resultado**: "Circulación Normal"

### Test 2: NORMAL con flecha arriba
```json
{ "mode": "NORMAL", "arrow": "UP", "language": "EN", "message": "" }
```
**Resultado**: "Continue Straight"

### Test 3: NORMAL con flecha izquierda en francés
```json
{ "mode": "NORMAL", "arrow": "LEFT", "language": "FR", "message": "" }
```
**Resultado**: "Tournez à Gauche"

### Test 4: EMERGENCY en alemán
```json
{ "mode": "EMERGENCY", "language": "DE", "message": "" }
```
**Resultado**: "⚠️ NOTFALL\nVORSICHT"

### Test 5: Mensaje personalizado (ignora predefinidos)
```json
{ "mode": "NORMAL", "arrow": "UP", "language": "ES", "message": "Hola Mundo" }
```
**Resultado**: "Hola Mundo" (mensaje personalizado tiene prioridad)

### Test 6: EVACUATION en catalán
```json
{ "mode": "EVACUATION", "language": "CA", "message": "" }
```
**Resultado**: "🚨 EVACUACIÓ\nSegueixi les Fletxes"

---

## 🚀 Beneficios

### Para Operadores
- ✅ No necesitan escribir textos para configuraciones básicas
- ✅ Textos consistentes y profesionales
- ✅ Soporte multiidioma automático
- ✅ Menos errores de escritura

### Para Desarrolladores
- ✅ Sistema centralizado y mantenible
- ✅ Fácil añadir nuevos idiomas o modos
- ✅ TypeScript asegura tipos correctos
- ✅ Reutilizable en web y WPF

### Para Usuarios Finales
- ✅ Mensajes claros y comprensibles
- ✅ Instrucciones específicas según dirección
- ✅ Idioma nativo automático
- ✅ Consistencia visual

---

## 🔧 Mantenimiento

### Añadir un Nuevo Idioma

1. Actualizar tipo `Language` en `src/types/index.ts`:
```typescript
export type Language = "ES" | "EN" | "FR" | "DE" | "IT" | "PT" | "CA" | "JP"; // Añadir JP
```

2. Añadir traducciones en `src/utils/beaconMessages.ts`:
```typescript
NORMAL: {
  // ... otros idiomas
  JP: "通常の交通"
}
```

### Añadir un Nuevo Modo

1. Actualizar tipo `BeaconMode` en `src/types/index.ts`:
```typescript
export type BeaconMode = "UNCONFIGURED" | "NORMAL" | "CONGESTION" | "EMERGENCY" | "EVACUATION" | "MAINTENANCE" | "RACE"; // Nuevo
```

2. Añadir traducciones en `src/utils/beaconMessages.ts`:
```typescript
RACE: {
  ES: "🏁 CARRERA EN CURSO",
  EN: "🏁 RACE IN PROGRESS",
  // ... otros idiomas
}
```

---

## ❓ Preguntas Frecuentes

**P: ¿Puedo sobrescribir un mensaje predefinido?**
R: Sí, solo escribe un mensaje personalizado en el campo `message`. Siempre tiene prioridad sobre los predefinidos.

**P: ¿Por qué solo NORMAL tiene direcciones?**
R: Es el modo donde más sentido tiene guiar al usuario. Otros modos son más informativos (emergencia, congestión).

**P: ¿Puedo añadir direcciones a otros modos?**
R: Sí, replica la lógica de `getNormalModeMessage()` para otros modos en `beaconMessages.ts`.

**P: ¿Los textos se guardan en Firestore?**
R: No, se generan dinámicamente. Solo se guarda `mode`, `arrow` y `language`. Esto ahorra espacio y centraliza las traducciones.

**P: ¿Qué pasa si un idioma no está disponible?**
R: Fallback automático a español (ES).

**P: ¿Las balizas WPF usan los mismos textos?**
R: Sí, deben implementar la misma lógica. Ver guías WPF.

---

## 📚 Documentación Relacionada

- **[README.md](../README.md)** - Descripción general del proyecto
- **[CUSTOM_TEXT_INTEGRATION_GUIDE.md](./CUSTOM_TEXT_INTEGRATION_GUIDE.md)** - Integración en WPF
- **[WPF_INTEGRATION_CHECKLIST.md](./WPF_INTEGRATION_CHECKLIST.md)** - Checklist completo
- **[BEACON_INTEGRATION_GUIDE.md](./BEACON_INTEGRATION_GUIDE.md)** - Guía de integración general

---

**Última actualización**: 16 de noviembre de 2024
