# ✅ Checklist de Integración WPF - Sistema Completo de Beacons

## 🎯 Objetivo

Verificar que **TODOS los campos y funcionalidades** configurados desde el panel web se implementen correctamente en las aplicaciones WPF de las beacons.

---

## 💡 RESUMEN EJECUTIVO - ARQUITECTURA DEL SISTEMA

### 🌐 **LA WEB HACE TODO EL TRABAJO**

**⚠️ CONCEPTO CLAVE**: El panel web es responsable de **generar todos los textos** en todos los idiomas. WPF solo tiene que **leer y mostrar**.

### 📖 Flujo de Trabajo Simplificado:

1. **Usuario en el Panel Web**:
   - Selecciona modo (NORMAL, CONGESTION, etc.)
   - Selecciona idioma (ES, CA, EN, etc.)
   - Selecciona dirección de flecha (si aplica)
   - Opcionalmente escribe un texto personalizado

2. **Panel Web Automáticamente**:
   - Si el usuario escribió texto → guarda ese texto
   - Si el usuario NO escribió nada → **auto-rellena con el texto predefinido** correspondiente (modo + idioma + flecha)
   - **Siempre guarda el campo `message` con contenido**

3. **Tu Aplicación WPF**:
   - Lee el campo `message` de Firestore
   - Lo muestra en el display
   - **FIN**

### ✅ Lo que NO tienes que hacer:

- ❌ NO implementar traducción de textos
- ❌ NO generar mensajes predefinidos
- ❌ NO implementar lógica de selección de textos según modo/idioma/flecha
- ❌ NO verificar si message está vacío

### ✅ Lo que SÍ tienes que hacer:

- ✅ Leer `beacon.Message` de Firestore
- ✅ Mostrarlo en el display: `display.ShowText(beacon.Message)`
- ✅ Implementar los otros campos (modo, color, brillo, flecha, etc.)

---

## 📋 Campos a Implementar en WPF

### 🔴 Campos OBLIGATORIOS (Esenciales)

Estos campos **DEBEN estar implementados** para el funcionamiento básico:

#### 1. ✅ `beaconId` (string)
- **Qué es**: Identificador único de la beacon
- **Ejemplo**: `"BEACON_001"`, `"BEACON_PADDOCK_05"`
- **Uso en WPF**: Identificar qué beacon procesar
- **Implementación**:
```csharp
[FirestoreProperty("beaconId")]
public string BeaconId { get; set; }
```

#### 2. ✅ `zone` (string)
- **Qué es**: Zona del circuito donde está la beacon
- **Ejemplo**: `"Paddock"`, `"Curva 1"`, `"Recta Principal"`
- **Uso en WPF**: Organización y logging
- **Implementación**:
```csharp
[FirestoreProperty("zone")]
public string Zone { get; set; }
```

#### 3. ✅ `mode` (string)
- **Qué es**: Modo de operación de la beacon
- **Valores posibles**:
  - `"UNCONFIGURED"` - Sin configurar (inicial)
  - `"NORMAL"` - Funcionamiento normal
  - `"CONGESTION"` - Alerta de tráfico denso
  - `"EMERGENCY"` - Emergencia activa
  - `"EVACUATION"` - Evacuación en curso
  - `"MAINTENANCE"` - Mantenimiento
- **Uso en WPF**: Determinar qué mostrar en pantalla
- **Implementación**:
```csharp
[FirestoreProperty("mode")]
public string Mode { get; set; }

// Procesar según modo
switch (beacon.Mode)
{
    case "NORMAL":
        // Mostrar información normal
        break;
    case "EMERGENCY":
        // Activar alerta visual/sonora
        break;
    case "EVACUATION":
        // Mostrar rutas de evacuación
        break;
    // ... etc
}
```

#### 4. ✅ `arrow` (string)
- **Qué es**: Dirección de la flecha a mostrar
- **Valores posibles**:
  - `"NONE"` - Sin flecha
  - `"UP"` - ↑
  - `"DOWN"` - ↓
  - `"LEFT"` - ←
  - `"RIGHT"` - →
  - `"UP_LEFT"` - ↖
  - `"UP_RIGHT"` - ↗
  - `"DOWN_LEFT"` - ↙
  - `"DOWN_RIGHT"` - ↘
- **Uso en WPF**: Renderizar flecha direccional en pantalla
- **Implementación**:
```csharp
[FirestoreProperty("arrow")]
public string Arrow { get; set; }

// Renderizar flecha
private void ShowArrow(string direction)
{
    switch (direction)
    {
        case "UP":
            display.DrawArrow(ArrowDirection.Up);
            break;
        case "DOWN":
            display.DrawArrow(ArrowDirection.Down);
            break;
        case "LEFT":
            display.DrawArrow(ArrowDirection.Left);
            break;
        case "RIGHT":
            display.DrawArrow(ArrowDirection.Right);
            break;
        case "UP_LEFT":
            display.DrawArrow(ArrowDirection.UpLeft);
            break;
        case "UP_RIGHT":
            display.DrawArrow(ArrowDirection.UpRight);
            break;
        case "DOWN_LEFT":
            display.DrawArrow(ArrowDirection.DownLeft);
            break;
        case "DOWN_RIGHT":
            display.DrawArrow(ArrowDirection.DownRight);
            break;
        case "NONE":
        default:
            display.HideArrow();
            break;
    }
}
```

#### 5. ✅ `color` (string)
- **Qué es**: Color de fondo de la pantalla en formato hexadecimal
- **Ejemplo**: `"#00FFAA"`, `"#FF0000"`, `"#0000FF"`
- **Uso en WPF**: Cambiar color de fondo de la pantalla
- **Implementación**:
```csharp
[FirestoreProperty("color")]
public string Color { get; set; }

// Convertir hex a Color
private Color ColorFromHex(string hexColor)
{
    // Remover # si existe
    hexColor = hexColor.TrimStart('#');
    
    // Convertir
    int r = Convert.ToInt32(hexColor.Substring(0, 2), 16);
    int g = Convert.ToInt32(hexColor.Substring(2, 2), 16);
    int b = Convert.ToInt32(hexColor.Substring(4, 2), 16);
    
    return Color.FromArgb(r, g, b);
}

// Aplicar color
display.BackgroundColor = ColorFromHex(beacon.Color);
```

#### 6. ✅ `brightness` (int)
- **Qué es**: Nivel de brillo de la pantalla (0-100)
- **Ejemplo**: `90`, `50`, `100`
- **Uso en WPF**: Ajustar brillo de la pantalla LED/LCD
- **Implementación**:
```csharp
[FirestoreProperty("brightness")]
public int Brightness { get; set; }

// Aplicar brillo (0-100 -> 0.0-1.0)
display.Brightness = beacon.Brightness / 100.0;
```

#### 7. ✅ `message` (string)
- **Qué es**: Texto a mostrar en la beacon
- **Ejemplo**: `"Bienvenido al Circuit de Catalunya"` o `"⚠️ Congestión\nReduzca Velocidad"`
- **⚠️ IMPORTANTE**: Este campo **SIEMPRE existirá** en Firestore. Si el usuario no especifica mensaje personalizado, el panel web guardará automáticamente el texto predefinido según modo, idioma y dirección de flecha.
- **Uso en WPF**: Leer y mostrar directamente el contenido de este campo
- **Implementación**:
```csharp
[FirestoreProperty("message")]
public string Message { get; set; }

// Mostrar texto directamente
display.ShowText(beacon.Message);

// NO es necesario implementar GetDefaultMessage() en WPF
// El panel web ya lo hace automáticamente
```

#### 8. ✅ `language` (string)
- **Qué es**: Idioma configurado para la beacon
- **Valores posibles**: `"ES"`, `"CA"`, `"EN"`, `"FR"`, `"DE"`, `"IT"`, `"PT"`
- **Uso en WPF**: Referencia informativa (el texto en `message` ya está en el idioma correcto)
- **Implementación**:
```csharp
[FirestoreProperty("language")]
public string Language { get; set; }

// Solo para logging o información
// El campo 'message' ya contiene el texto en el idioma correcto
```

---

### 🟡 Campos OPCIONALES (Recomendados)

Estos campos mejoran la funcionalidad pero no son estrictamente necesarios:

#### 9. ⚠️ `evacuationExit` (string, opcional)
- **Qué es**: Salida de evacuación específica (solo en modo EVACUATION)
- **Ejemplo**: `"SALIDA NORTE"`, `"EXIT A"`, `"PADDOCK EXIT"`
- **Uso en WPF**: Mostrar junto al mensaje de evacuación
- **Implementación**:
```csharp
[FirestoreProperty("evacuationExit")]
public string EvacuationExit { get; set; }

// Usar en modo EVACUATION
if (beacon.Mode == "EVACUATION")
{
    // El texto ya viene traducido en beacon.Message
    string text = beacon.Message;
    
    if (!string.IsNullOrEmpty(beacon.EvacuationExit))
    {
        text += "\n" + beacon.EvacuationExit;
    }
    
    display.ShowText(text);
}
```

---

### 🔵 Campos de CONTROL (Sistema de Comandos)

Estos campos permiten control remoto desde el panel web:

#### 10. 🚨 `command` (string, opcional)
- **Qué es**: Comando a ejecutar
- **Valores posibles**: `"RESTART"`, `"STATUS"`, `"CONFIG"`, etc.
- **Uso en WPF**: Ejecutar acciones remotas
- **⚠️ IMPORTANTE**: El comando `"RESTART"` reinicia **TODO EL SISTEMA WINDOWS**, no solo la app
- **Implementación**:
```csharp
[FirestoreProperty("command")]
public string Command { get; set; }

// Procesar comandos
if (!string.IsNullOrEmpty(beacon.Command))
{
    await ProcessCommandAsync(beacon);
}

private async Task ProcessCommandAsync(Beacon beacon)
{
    switch (beacon.Command?.ToUpper())
    {
        case "RESTART":
            // ⚠️ REINICIA TODO EL SISTEMA WINDOWS
            Process.Start("shutdown", "/r /t 10 /f /c \"Reinicio remoto desde panel\"");
            break;
            
        case "STATUS":
            // Reportar estado actual
            await ReportStatusAsync(beacon.BeaconId);
            break;
            
        default:
            Console.WriteLine($"Comando desconocido: {beacon.Command}");
            break;
    }
}
```

#### 11. 🕐 `commandTimestamp` (string, opcional)
- **Qué es**: Timestamp del comando (ISO 8601)
- **Ejemplo**: `"2024-11-16T10:30:45.123Z"`
- **Uso en WPF**: Evitar ejecutar el mismo comando múltiples veces
- **⚠️ IMPORTANTE**: Los comandos se auto-limpian a los 7 segundos desde el web
- **Implementación**:
```csharp
[FirestoreProperty("commandTimestamp")]
public string CommandTimestamp { get; set; }

// Deduplicación de comandos
private string lastProcessedCommandTimestamp = "";

private async Task ProcessCommandAsync(Beacon beacon)
{
    // Evitar ejecutar el mismo comando dos veces
    if (beacon.CommandTimestamp == lastProcessedCommandTimestamp)
    {
        return; // Ya procesado
    }
    
    // Ejecutar comando
    switch (beacon.Command)
    {
        case "RESTART":
            Process.Start("shutdown", "/r /t 10 /f");
            break;
    }
    
    // Guardar timestamp procesado
    lastProcessedCommandTimestamp = beacon.CommandTimestamp;
}
```

---

### 🟢 Campos de ESTADO (Feedback al Panel Web)

Estos campos deben **actualizarse desde WPF** para informar al panel web:

#### 12. ✅ `online` (bool)
- **Qué es**: Indica si la beacon está online
- **Uso**: El panel web muestra el estado
- **Implementación**:
```csharp
[FirestoreProperty("online")]
public bool Online { get; set; }

// Actualizar cada 5 segundos (heartbeat)
await beaconRef.UpdateAsync(new Dictionary<string, object>
{
    { "online", true },
    { "lastSeen", DateTime.UtcNow }
});
```

#### 13. ⏰ `lastSeen` (timestamp)
- **Qué es**: Última vez que la beacon envió heartbeat
- **Uso**: Detectar beacons inactivas
- **Implementación**:
```csharp
[FirestoreProperty("lastSeen")]
public DateTime LastSeen { get; set; }

// Heartbeat cada 5 segundos
private async Task SendHeartbeatAsync()
{
    while (true)
    {
        await beaconRef.UpdateAsync(new Dictionary<string, object>
        {
            { "online", true },
            { "lastSeen", FieldValue.ServerTimestamp }
        });
        
        await Task.Delay(5000); // 5 segundos
    }
}
```

#### 14. 📊 `metrics` (object, opcional)
- **Qué es**: Métricas de rendimiento
- **Ejemplo**:
```json
{
  "cpuUsage": 45.2,
  "memoryUsage": 62.8,
  "temperature": 42.0,
  "uptime": 86400
}
```
- **Uso**: Monitoreo desde el panel web
- **Implementación**:
```csharp
// Enviar métricas cada minuto
await beaconRef.UpdateAsync(new Dictionary<string, object>
{
    { "metrics", new Dictionary<string, object>
        {
            { "cpuUsage", GetCpuUsage() },
            { "memoryUsage", GetMemoryUsage() },
            { "temperature", GetTemperature() },
            { "uptime", GetUptime() }
        }
    }
});
```

---

## 🔄 Flujo de Datos Completo

### Panel Web → Firestore → WPF

```
┌─────────────────┐
│   Panel Web     │
│   (React)       │
└────────┬────────┘
         │ 1. Usuario configura beacon
         ▼
┌─────────────────┐
│   Firestore     │
│  (Real-time DB) │
└────────┬────────┘
         │ 2. Datos guardados
         ▼
┌─────────────────┐
│   WPF App       │
│  (Polling 300ms)│
└────────┬────────┘
         │ 3. Detecta cambios
         ▼
┌─────────────────┐
│   Pantalla      │
│   Beacon        │
└─────────────────┘
```

### WPF → Firestore → Panel Web

```
┌─────────────────┐
│   WPF App       │
│  (Heartbeat)    │
└────────┬────────┘
         │ 1. Envía estado cada 5s
         ▼
┌─────────────────┐
│   Firestore     │
│  (Real-time DB) │
└────────┬────────┘
         │ 2. Sync automático
         ▼
┌─────────────────┐
│   Panel Web     │
│   (React)       │
└─────────────────┘
```

---

## 📝 Clase Beacon Completa en C#

```csharp
using Google.Cloud.Firestore;
using System;

[FirestoreData]
public class Beacon
{
    // === IDENTIFICACIÓN ===
    [FirestoreProperty("beaconId")]
    public string BeaconId { get; set; }
    
    [FirestoreProperty("zone")]
    public string Zone { get; set; }
    
    // === CONFIGURACIÓN VISUAL ===
    [FirestoreProperty("mode")]
    public string Mode { get; set; }
    
    [FirestoreProperty("arrow")]
    public string Arrow { get; set; }
    
    [FirestoreProperty("color")]
    public string Color { get; set; }
    
    [FirestoreProperty("brightness")]
    public int Brightness { get; set; }
    
    // === CONTENIDO ===
    [FirestoreProperty("message")]
    public string Message { get; set; }
    
    [FirestoreProperty("language")]
    public string Language { get; set; }
    
    [FirestoreProperty("evacuationExit")]
    public string EvacuationExit { get; set; }
    
    // === COMANDOS ===
    [FirestoreProperty("command")]
    public string Command { get; set; }
    
    [FirestoreProperty("commandTimestamp")]
    public string CommandTimestamp { get; set; }
    
    // === ESTADO ===
    [FirestoreProperty("online")]
    public bool Online { get; set; }
    
    [FirestoreProperty("lastSeen")]
    public DateTime? LastSeen { get; set; }
    
    [FirestoreProperty("metrics")]
    public Dictionary<string, object> Metrics { get; set; }
}
```

---

## ✅ Checklist de Verificación

### Configuración Inicial

- [ ] **Proyecto WPF configurado** con Google.Cloud.Firestore NuGet
- [ ] **Credenciales Firebase** correctamente configuradas
- [ ] **Clase Beacon** con todas las propiedades implementadas
- [ ] **Polling activo** cada 300ms
- [ ] **Heartbeat activo** cada 5 segundos

### Campos Visuales (CRÍTICO)

- [ ] **`mode`** - Todos los modos procesados correctamente
  - [ ] UNCONFIGURED
  - [ ] NORMAL
  - [ ] CONGESTION
  - [ ] EMERGENCY
  - [ ] EVACUATION
  - [ ] MAINTENANCE

- [ ] **`arrow`** - Todas las direcciones renderizadas
  - [ ] NONE (sin flecha)
  - [ ] UP ↑
  - [ ] DOWN ↓
  - [ ] LEFT ←
  - [ ] RIGHT →
  - [ ] UP_LEFT ↖
  - [ ] UP_RIGHT ↗
  - [ ] DOWN_LEFT ↙
  - [ ] DOWN_RIGHT ↘

- [ ] **`color`** - Conversión hex → RGB funcionando
  - [ ] Rojo #FF0000
  - [ ] Verde #00FF00
  - [ ] Azul #0000FF
  - [ ] Personalizado #00FFAA

- [ ] **`brightness`** - Brillo aplicado correctamente (0-100)

- [ ] **`message`** - Texto personalizado mostrado
  - [ ] Mensaje corto
  - [ ] Mensaje largo (scroll si necesario)
  - [ ] Mensaje multilínea (\n)
  - [ ] Emojis (⚠️ 🚨)
  - [ ] Acentos (á é í ó ú ñ)

- [ ] **`language`** - Textos por defecto en idioma correcto
  - [ ] ES - Español
  - [ ] CA - Catalán
  - [ ] EN - Inglés
  - [ ] FR - Francés
  - [ ] DE - Alemán
  - [ ] IT - Italiano
  - [ ] PT - Portugués

- [ ] **`evacuationExit`** - Mostrado en modo EVACUATION

### Sistema de Comandos

- [ ] **`command`** - Comandos procesados correctamente
  - [ ] RESTART (reinicia Windows completo)
  - [ ] STATUS (reporta estado)
  - [ ] Comandos personalizados

- [ ] **Deduplicación** - No ejecutar comando dos veces
  - [ ] Usar `commandTimestamp` para evitar duplicados
  - [ ] Guardar último timestamp procesado

- [ ] **Auto-limpieza** - Web limpia comandos a los 7s
  - [ ] WPF detecta comando en <7 segundos
  - [ ] No requiere limpieza manual desde WPF

### Feedback al Panel Web

- [ ] **`online`** - Estado actualizado cada 5s
- [ ] **`lastSeen`** - Timestamp actualizado en heartbeat
- [ ] **`metrics`** - Métricas enviadas (opcional)

### Casos Especiales

- [ ] **Sin mensaje personalizado** - Texto por defecto según modo+idioma
- [ ] **Texto muy largo** - Scroll implementado
- [ ] **Caracteres especiales** - UTF-8 soportado
- [ ] **Modo UNCONFIGURED** - Comportamiento apropiado
- [ ] **Cambios en tiempo real** - Pantalla actualiza inmediatamente

---

## 🧪 Escenarios de Testing

### Test 1: Configuración Normal
```json
{
  "beaconId": "TEST_001",
  "mode": "NORMAL",
  "arrow": "RIGHT",
  "color": "#00FFAA",
  "brightness": 90,
  "message": "Bienvenido al Circuit",
  "language": "ES"
}
```
**Verificar**: Pantalla muestra texto, flecha →, color verde, brillo 90%

### Test 2: Emergencia
```json
{
  "beaconId": "TEST_002",
  "mode": "EMERGENCY",
  "arrow": "LEFT",
  "color": "#FF0000",
  "brightness": 100,
  "message": "⚠️ PELIGRO",
  "language": "ES"
}
```
**Verificar**: Pantalla roja, flecha ←, texto de emergencia, brillo máximo

### Test 3: Evacuación con Salida
```json
{
  "beaconId": "TEST_003",
  "mode": "EVACUATION",
  "arrow": "UP",
  "color": "#FF6600",
  "brightness": 100,
  "message": "🚨 EVACUACIÓN",
  "evacuationExit": "SALIDA NORTE",
  "language": "ES"
}
```
**Verificar**: Mensaje + salida, flecha ↑

### Test 4: Texto Predefinido Automático
**⚠️ IMPORTANTE**: El panel web **automáticamente rellena** el campo `message` cuando el usuario lo deja vacío, según modo/idioma/flecha.

**Aunque el usuario no escriba nada, el campo `message` SIEMPRE existirá en Firestore con el texto predefinido correspondiente.**

Ejemplo - Usuario selecciona modo CONGESTION + idioma CA y guarda SIN escribir texto:
```json
{
  "beaconId": "TEST_004",
  "mode": "CONGESTION",
  "message": "⚠️ Congestió\nRedueixi Velocitat",  // ✅ Auto-rellenado por la web
  "language": "CA"
}
```
**Verificar en WPF**: Simplemente leer `beacon.Message` y mostrar → "⚠️ Congestió\nRedueixi Velocitat"

### Test 4b: Modo NORMAL - Mensajes según Dirección de Flecha
**⚠️ IMPORTANTE**: En modo NORMAL, el mensaje predefinido varía según la dirección de la flecha.

Ejemplos - Usuario selecciona modo NORMAL + diferentes flechas y guarda SIN escribir texto:
```json
// Flecha arriba
{ 
  "beaconId": "TEST_004B_1", 
  "mode": "NORMAL", 
  "arrow": "UP", 
  "message": "Continúe Recto",  // ✅ Auto-rellenado
  "language": "ES" 
}

// Flecha izquierda
{ 
  "beaconId": "TEST_004B_2", 
  "mode": "NORMAL", 
  "arrow": "LEFT", 
  "message": "Gire a la Izquierda",  // ✅ Auto-rellenado
  "language": "ES" 
}

// Flecha derecha
{ 
  "beaconId": "TEST_004B_3", 
  "mode": "NORMAL", 
  "arrow": "RIGHT", 
  "message": "Gire a la Derecha",  // ✅ Auto-rellenado
  "language": "ES" 
}

// Sin flecha
{ 
  "beaconId": "TEST_004B_4", 
  "mode": "NORMAL", 
  "arrow": "NONE", 
  "message": "Circulación Normal",  // ✅ Auto-rellenado
  "language": "ES" 
}
```
**Verificar en WPF**: Simplemente leer `beacon.Message` para cada caso

### Test 5: Comando de Reinicio
```json
{
  "beaconId": "TEST_005",
  "command": "RESTART",
  "commandTimestamp": "2024-11-16T10:30:45.123Z"
}
```
**Verificar**: Sistema Windows se reinicia en 10 segundos

### Test 6: Multiidioma
**Probar mismo modo en todos los idiomas**:

El panel web auto-rellena según el idioma seleccionado. Ejemplo para modo NORMAL:
```json
{ "mode": "NORMAL", "message": "Circulación Normal", "language": "ES" }
{ "mode": "NORMAL", "message": "Circulació Normal", "language": "CA" }
{ "mode": "NORMAL", "message": "Normal Traffic", "language": "EN" }
{ "mode": "NORMAL", "message": "Circulation Normale", "language": "FR" }
{ "mode": "NORMAL", "message": "Normaler Verkehr", "language": "DE" }
{ "mode": "NORMAL", "message": "Traffico Normale", "language": "IT" }
{ "mode": "NORMAL", "message": "", "language": "PT" }
```
**Verificar**: Texto en idioma correspondiente

### Test 7: Actualización en Tiempo Real
1. Configurar beacon con mensaje "Texto 1"
2. Cambiar a "Texto 2" desde panel web
3. **Verificar**: Pantalla actualiza en <1 segundo

### Test 8: Heartbeat
1. Iniciar WPF
2. Verificar en Firestore: `online = true`, `lastSeen` actualizado
3. Detener WPF
4. Esperar >15 segundos
5. **Verificar**: Panel web muestra beacon como offline

---

## 🚨 Errores Comunes

### Error 1: Flecha No Aparece
**Causa**: Valor `arrow` no procesado o case incorrecto
**Solución**: Implementar switch para todas las direcciones + NONE

### Error 2: Color No Cambia
**Causa**: Conversión hex→RGB incorrecta
**Solución**: Verificar que se remueve `#` y se convierte correctamente

### Error 3: Texto No Actualiza
**Causa**: Polling no detecta cambios en `message`
**Solución**: Verificar que propiedad `Message` existe y se lee en polling

### Error 4: Comando Ejecuta Múltiples Veces
**Causa**: No hay deduplicación con `commandTimestamp`
**Solución**: Guardar último timestamp procesado

### Error 5: Panel Web Muestra Beacon Offline
**Causa**: Heartbeat no se envía o `online` no se actualiza
**Solución**: Implementar loop con `await Task.Delay(5000)` enviando heartbeat

### Error 6: Caracteres Raros en Texto
**Causa**: Encoding incorrecto (no UTF-8)
**Solución**: Asegurar UTF-8 en toda la aplicación

### Error 7: Modo EVACUATION Sin Salida
**Causa**: Campo `evacuationExit` no procesado
**Solución**: Combinar `message + "\n" + evacuationExit`

---

## 📊 Rendimiento Esperado

| Métrica | Valor Esperado |
|---------|----------------|
| **Latencia de actualización** | < 1 segundo |
| **Intervalo de polling** | 300ms |
| **Intervalo de heartbeat** | 5 segundos |
| **Tiempo detección offline** | ~15 segundos |
| **Persistencia comando** | 7 segundos |
| **Uso CPU (idle)** | < 5% |
| **Uso memoria** | < 100 MB |

---

## 📞 Preguntas Frecuentes

**P: ¿Qué pasa si no implemento `message`?**
R: Las beacons no mostrarán textos personalizados, solo podrán mostrar textos por defecto.

**P: ¿Es obligatorio implementar todos los idiomas?**
R: Recomendado pero no obligatorio. Como mínimo implementar ES (español).

**P: ¿Qué pasa si no implemento el sistema de comandos?**
R: No podrás reiniciar ni controlar las beacons remotamente desde el panel web.

**P: ¿Cada cuánto debo enviar el heartbeat?**
R: Cada 5 segundos. El panel web considera offline si no hay heartbeat en 15s.

**P: ¿Debo limpiar los comandos después de procesarlos?**
R: NO es necesario. El panel web auto-limpia comandos a los 7 segundos.

**P: ¿Qué hace el comando RESTART exactamente?**
R: Reinicia TODO EL SISTEMA WINDOWS usando `shutdown /r`, no solo la aplicación.

**P: ¿Puedo usar un polling más lento?**
R: No recomendado. 300ms asegura detección rápida de cambios y comandos.

---

## 🎯 Resumen de Prioridades

### 🔴 PRIORIDAD ALTA (Implementar primero)
1. Clase `Beacon` con todas las propiedades
2. Polling cada 300ms
3. Procesamiento de `mode`, `arrow`, `color`, `brightness`
4. Renderizado de `message` en pantalla
5. Heartbeat cada 5 segundos

### 🟡 PRIORIDAD MEDIA (Implementar después)
1. Sistema de comandos (`command`, `commandTimestamp`)
2. Manejo de idiomas (`language` + textos por defecto)
3. Modo EVACUATION con `evacuationExit`
4. Deduplicación de comandos

### 🟢 PRIORIDAD BAJA (Nice to have)
1. Métricas de rendimiento (`metrics`)
2. Logging detallado
3. Manejo de caracteres especiales/emojis
4. Scroll para textos largos

---

## ✅ Validación Final

Antes de dar por completada la integración, verificar:

- [ ] **Todas las propiedades de `Beacon`** están implementadas
- [ ] **Polling funciona** y detecta cambios en <1 segundo
- [ ] **Heartbeat funciona** y panel web muestra estado correcto
- [ ] **Todos los modos** se procesan correctamente
- [ ] **Todas las flechas** se renderizan correctamente
- [ ] **Colores personalizados** funcionan
- [ ] **Brillo ajustable** funciona
- [ ] **Textos personalizados** se muestran
- [ ] **Multiidioma** funciona (al menos ES)
- [ ] **Comandos** se ejecutan correctamente
- [ ] **RESTART** reinicia el sistema Windows
- [ ] **Deduplicación** evita ejecuciones múltiples
- [ ] **EVACUATION** muestra salida si existe
- [ ] **Testing completo** con todos los escenarios

---

**Si todos los checkboxes están marcados, la integración está completa. 🎉**

---

## 📚 Documentación de Referencia

- **[CUSTOM_TEXT_INTEGRATION_GUIDE.md](./CUSTOM_TEXT_INTEGRATION_GUIDE.md)** - Guía detallada de textos personalizados
- **[COMMAND_SYSTEM_GUIDE.md](./COMMAND_SYSTEM_GUIDE.md)** - Sistema de comandos remotos
- **[BEACON_INTEGRATION_GUIDE.md](./BEACON_INTEGRATION_GUIDE.md)** - Guía completa de integración

---

**Última actualización**: 16 de noviembre de 2025
