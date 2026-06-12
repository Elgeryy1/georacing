# 📋 REFERENCIA RÁPIDA - PANEL → BALIZA

## 🎯 Campos que Lee la Baliza

### **ESTRUCTURA COMPLETA del estado de baliza:**

```json
{
  "id": "MINI-PC-01",
  "name": "Baliza Sector A",
  "mode": "NORMAL",
  "brightness": 80,
  "online": true,
  "configured": true,
  "zone": "Sector A",
  "arrow": "UP_RIGHT",
  "message": "Texto personalizado",
  "color": "#2E7D32",
  "language": "ES",
  "evacuationExit": "Salida 3 - Tribuna Principal",
  "lastSeen": "2024-01-20T10:30:00Z",
  "lastUpdate": "2024-01-20T10:29:55Z"
}
```

---

## 📝 CAMPOS OBLIGATORIOS

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `mode` | string | Modo operativo | `"NORMAL"` |
| `zone` | string | Nombre de la zona | `"Sector A"` |
| `arrow` | string | Dirección de flecha | `"UP"` |
| `brightness` | int | Brillo 0-100 | `80` |
| `configured` | bool | Estado configurado | `true` |

---

## 🎨 CAMPOS OPCIONALES (Personalizables)

### **1. Mensaje Personalizado**
```json
"message": "¡Bienvenido al circuito!"
```
- **Efecto**: Reemplaza el texto por defecto del modo
- **Formato**: Cualquier string (ya traducido)
- **Límite**: 100 caracteres recomendados
- **Si no se envía**: Usa texto por defecto del modo

### **2. Color Personalizado**
```json
"color": "#FF5722"
```
- **Efecto**: Reemplaza el color por defecto del modo
- **Formato**: Hexadecimal (`#RRGGBB`)
- **Ejemplos válidos**: `"#FF0000"`, `"#00FF00"`, `"#0000FF"`
- **Si no se envía**: Usa color por defecto del modo

### **3. Salida de Evacuación**
```json
"evacuationExit": "Salida 3 - Tribuna Principal"
```
- **Efecto**: Muestra la salida en modo EVACUATION
- **Visible solo cuando**: `mode == "EVACUATION"`
- **Formato**: String descriptivo
- **Ejemplo**: `"Salida 3 - Tribuna Norte"`

### **4. Idioma**
```json
"language": "ES"
```
- **Valores**: `ES`, `CA`, `EN`, `FR`, `DE`, `IT`, `PT`
- **Uso**: Informativo (el texto ya viene traducido en `message`)
- **NO traduce automáticamente**: El panel debe enviar el texto ya traducido

---

## 🧭 VALORES VÁLIDOS - ARROW

### **Cardinales**
```json
"arrow": "UP"       // ⬆
"arrow": "DOWN"     // ⬇
"arrow": "LEFT"     // ⬅
"arrow": "RIGHT"    // ➡
```

### **Diagonales**
```json
"arrow": "UP_LEFT"      // ↖
"arrow": "UP_RIGHT"     // ↗
"arrow": "DOWN_LEFT"    // ↙
"arrow": "DOWN_RIGHT"   // ↘
```

### **Sin flecha**
```json
"arrow": "NONE"     // Oculta la flecha
```

⚠️ **IMPORTANTE**: Usar MAYÚSCULAS siempre

---

## 🎭 MODOS OPERATIVOS

### **UNCONFIGURED**
```json
{
  "mode": "UNCONFIGURED",
  "configured": false
}
```
- Color: Gris (#90A4AE)
- Texto: "SIN CONFIGURAR"
- Uso: Baliza recién instalada

### **NORMAL**
```json
{
  "mode": "NORMAL",
  "zone": "Sector A",
  "arrow": "UP"
}
```
- Color: Verde (#2E7D32)
- Texto: Nombre de la zona o mensaje personalizado
- Uso: Operación estándar

### **CONGESTION**
```json
{
  "mode": "CONGESTION",
  "message": "Evite esta zona",
  "arrow": "LEFT"
}
```
- Color: Naranja (#F57C00)
- Texto: "⚠️ CONGESTIÓN" o mensaje personalizado
- Uso: Tráfico pesado

### **EMERGENCY**
```json
{
  "mode": "EMERGENCY",
  "message": "Permanezca en su ubicación",
  "color": "#FF0000"
}
```
- Color: Rojo oscuro (#C62828) o personalizado
- Texto: "🚨 EMERGENCIA" o mensaje personalizado
- Uso: Incidente activo

### **EVACUATION**
```json
{
  "mode": "EVACUATION",
  "evacuationExit": "Salida 3 - Tribuna Principal",
  "arrow": "DOWN_RIGHT"
}
```
- Color: Rojo brillante (#D32F2F)
- Texto: "🚨 EVACUACIÓN"
- Muestra: Salida de evacuación + flecha
- Uso: Evacuación en curso

### **MAINTENANCE**
```json
{
  "mode": "MAINTENANCE",
  "message": "Zona en mantenimiento"
}
```
- Color: Morado (#7B1FA2)
- Texto: "🔧 MANTENIMIENTO" o mensaje personalizado
- Uso: Trabajos de mantenimiento

---

## 🔄 PRIORIDAD DE VALORES

```
┌─────────────────────────────────────┐
│ PRIORIDAD ALTA (Personalizado)      │
├─────────────────────────────────────┤
│ message → Reemplaza DisplayText     │
│ color   → Reemplaza BackgroundColor │
└─────────────────────────────────────┘
              ↓ Si no existen ↓
┌─────────────────────────────────────┐
│ PRIORIDAD BAJA (Por defecto)        │
├─────────────────────────────────────┤
│ Texto del modo (ej: "🚨 EMERGENCIA")│
│ Color del modo (ej: "#C62828")      │
└─────────────────────────────────────┘
```

---

## 🚀 EJEMPLOS DE USO

### **Ejemplo 1: Dirección Simple**
```json
{
  "mode": "NORMAL",
  "zone": "Sector A",
  "arrow": "UP",
  "brightness": 80,
  "configured": true
}
```
**Resultado**: Fondo verde, texto "SECTOR A", flecha ⬆

### **Ejemplo 2: Mensaje Personalizado**
```json
{
  "mode": "NORMAL",
  "message": "¡Bienvenido al circuito!",
  "color": "#4CAF50",
  "arrow": "NONE",
  "brightness": 100,
  "configured": true
}
```
**Resultado**: Fondo verde claro, texto "¡Bienvenido al circuito!", sin flecha

### **Ejemplo 3: Evacuación con Salida**
```json
{
  "mode": "EVACUATION",
  "evacuationExit": "Salida 3 - Tribuna Norte",
  "arrow": "LEFT",
  "brightness": 100,
  "configured": true
}
```
**Resultado**: Fondo rojo, texto "🚨 EVACUACIÓN", salida "Salida 3 - Tribuna Norte", flecha ⬅

### **Ejemplo 4: Congestión con Dirección Alternativa**
```json
{
  "mode": "CONGESTION",
  "message": "Use ruta alternativa",
  "arrow": "UP_RIGHT",
  "brightness": 90,
  "configured": true
}
```
**Resultado**: Fondo naranja, texto "Use ruta alternativa", flecha ↗

### **Ejemplo 5: Emergencia Multiidioma**
```json
{
  "mode": "EMERGENCY",
  "message": "Stay in your location",
  "language": "EN",
  "arrow": "NONE",
  "brightness": 100,
  "configured": true
}
```
**Resultado**: Fondo rojo, texto "Stay in your location", sin flecha

---

## ⚡ COMPORTAMIENTO DE LA BALIZA

### **Polling**
- Frecuencia: **2 segundos**
- Endpoint: `GET /api/beacons/{id}`
- La baliza lee constantemente el estado

### **Sin Escritura**
- ❌ NO envía heartbeats
- ❌ NO marca comandos ejecutados
- ✅ SOLO lee y muestra

### **Sin Lógica de Negocio**
- ❌ NO recalcula mensajes
- ❌ NO traduce textos
- ❌ NO modifica colores
- ✅ Muestra exactamente lo que llega

---

## 📊 CHECKLIST DE VALIDACIÓN

Antes de enviar datos al endpoint:

- [ ] `mode` es válido (UNCONFIGURED, NORMAL, CONGESTION, EMERGENCY, EVACUATION, MAINTENANCE)
- [ ] `arrow` está en MAYÚSCULAS (UP, DOWN, LEFT, RIGHT, UP_LEFT, etc.)
- [ ] `color` tiene formato hexadecimal (`#RRGGBB`)
- [ ] `brightness` está entre 0 y 100
- [ ] `message` está traducido (si aplica)
- [ ] `evacuationExit` solo se envía si `mode == "EVACUATION"`
- [ ] `configured` es `true` (si la baliza está lista)

---

## 🔗 ENDPOINT DE ACTUALIZACIÓN

**Método**: `PUT /api/beacons/{id}`

**Body**:
```json
{
  "mode": "NORMAL",
  "zone": "Sector A",
  "arrow": "UP",
  "message": "Texto personalizado",
  "color": "#2E7D32",
  "brightness": 80,
  "language": "ES",
  "evacuationExit": "Salida 3",
  "configured": true
}
```

---

## 📞 TROUBLESHOOTING

### **El mensaje personalizado no aparece**
✅ Verificar que `message` no es `null` ni `""`  
✅ El texto debe estar traducido antes de enviarlo

### **El color no cambia**
✅ Usar formato hexadecimal: `"#FF0000"` (no `"rgb(255,0,0)"`)  
✅ Incluir `#` al inicio

### **La flecha no se muestra**
✅ Usar MAYÚSCULAS: `"UP"` (no `"up"`)  
✅ Verificar que no es `"NONE"`

### **La salida de evacuación no aparece**
✅ Verificar que `mode == "EVACUATION"`  
✅ Verificar que `evacuationExit` no es `null`

---

**📘 Para más información técnica, ver `IMPLEMENTACION-COMPLETA.md`**
