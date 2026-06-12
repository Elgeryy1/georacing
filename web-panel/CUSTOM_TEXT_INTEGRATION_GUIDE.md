# 📝 Guía de Integración - Texto Personalizado en Balizas

## 🎯 Objetivo

Integrar el sistema de **textos personalizados** que ya existe en el panel web con las aplicaciones WPF de las balizas, para que los mensajes configurados desde la web se muestren en las pantallas físicas de las balizas.

---

## 📊 Estado Actual

### ✅ Panel Web (YA IMPLEMENTADO)

El panel web **YA tiene implementado** el sistema completo de textos personalizados:

**Campos disponibles en Firestore** (`beacons` collection):
```typescript
interface Beacon {
  beaconId: string;
  zone: string;
  
  // ✅ Texto personalizado - YA EXISTE EN FIRESTORE
  message: string;              // Mensaje personalizado para la baliza
  
  // Otros campos de configuración
  mode: BeaconMode;             // NORMAL, EMERGENCY, EVACUATION, etc.
  arrow: ArrowDirection;        // UP, DOWN, LEFT, RIGHT, etc.
  color: string;                // Color hex: "#00FFAA"
  brightness: number;           // 0-100
  language: Language;           // ES, CA, EN, FR, DE, IT, PT
  evacuationExit?: string;      // Solo para modo EVACUATION
}
```

**Componentes web que gestionan el texto**:
- ✅ `BeaconEditModal` - Permite editar el mensaje
- ✅ `BeaconDetail` - Editor completo con textarea
- ✅ `BeaconPreview` - Vista previa del mensaje
- ✅ Dashboard - Gestión masiva de mensajes

**Ejemplo de datos en Firestore**:
```json
{
  "beaconId": "BEACON_001",
  "zone": "Paddock",
  "message": "Bienvenido al Circuit de Catalunya",
  "mode": "NORMAL",
  "arrow": "RIGHT",
  "color": "#00FFAA",
  "brightness": 90,
  "language": "ES"
}
```

---

## 🔧 ¿Qué Necesitas Implementar en WPF?

### 1. Leer el campo `message` desde Firestore

Tu aplicación WPF **ya está haciendo polling** cada 300ms a Firestore para detectar cambios. Solo necesitas **leer un campo adicional**.

#### Código C# Actual (Ejemplo)
```csharp
// Tu polling actual (ya existente)
private async Task PollFirestoreAsync()
{
    while (true)
    {
        var snapshot = await beaconsRef.GetSnapshotAsync();
        
        foreach (var doc in snapshot.Documents)
        {
            var beacon = doc.ConvertTo<Beacon>();
            
            // Ya estás procesando estos campos:
            // beacon.Mode
            // beacon.Arrow
            // beacon.Color
            // beacon.Brightness
            
            // ✅ AGREGAR: Procesar el campo message
            if (!string.IsNullOrEmpty(beacon.Message))
            {
                await UpdateBeaconTextAsync(beacon.BeaconId, beacon.Message);
            }
        }
        
        await Task.Delay(300);
    }
}
```

#### Clase Beacon en C# (Actualizar)

**Agregar la propiedad `Message` a tu clase Beacon**:

```csharp
public class Beacon
{
    [FirestoreProperty("beaconId")]
    public string BeaconId { get; set; }
    
    [FirestoreProperty("zone")]
    public string Zone { get; set; }
    
    [FirestoreProperty("mode")]
    public string Mode { get; set; }
    
    [FirestoreProperty("arrow")]
    public string Arrow { get; set; }
    
    [FirestoreProperty("color")]
    public string Color { get; set; }
    
    [FirestoreProperty("brightness")]
    public int Brightness { get; set; }
    
    [FirestoreProperty("language")]
    public string Language { get; set; }
    
    // ✅ AGREGAR ESTA PROPIEDAD
    [FirestoreProperty("message")]
    public string Message { get; set; }
    
    [FirestoreProperty("evacuationExit")]
    public string EvacuationExit { get; set; }
    
    [FirestoreProperty("command")]
    public string Command { get; set; }
    
    [FirestoreProperty("commandTimestamp")]
    public string CommandTimestamp { get; set; }
}
```

---

### 2. Mostrar el Texto en la Pantalla de la Baliza

Necesitas **renderizar el texto personalizado** en la pantalla LED/LCD de la baliza.

#### Lógica de Visualización

```csharp
private async Task UpdateBeaconTextAsync(string beaconId, string customMessage)
{
    // 1. Obtener la baliza/pantalla correspondiente
    var display = GetBeaconDisplay(beaconId);
    
    // 2. Configurar el texto
    display.Text = customMessage;
    
    // 3. Aplicar configuración adicional (opcional)
    // - Tamaño de fuente
    // - Alineación
    // - Word wrap
    // - Scroll si el texto es muy largo
    
    // 4. Actualizar la pantalla
    await display.RefreshAsync();
}
```

#### Ejemplo Completo

```csharp
private async Task ProcessBeaconUpdate(Beacon beacon)
{
    var display = GetBeaconDisplay(beacon.BeaconId);
    
    // Configurar color de fondo (según beacon.Color)
    display.BackgroundColor = ColorFromHex(beacon.Color);
    
    // Configurar brillo
    display.Brightness = beacon.Brightness;
    
    // ✅ MOSTRAR MENSAJE PERSONALIZADO
    if (!string.IsNullOrEmpty(beacon.Message))
    {
        display.ShowCustomText(beacon.Message);
    }
    else
    {
        // Si no hay mensaje personalizado, mostrar texto por defecto según modo
        display.ShowDefaultText(beacon.Mode);
    }
    
    // Mostrar flecha (si aplica)
    if (beacon.Arrow != "NONE")
    {
        display.ShowArrow(beacon.Arrow);
    }
    
    // Aplicar todo
    await display.UpdateAsync();
}
```

---

### 3. Manejo de Idiomas

El campo `language` indica en qué idioma debería mostrarse el contenido.

#### Textos Predeterminados por Modo

Si `message` está **vacío**, debes mostrar textos predeterminados según el **modo** y el **idioma**:

```csharp
private string GetDefaultMessage(string mode, string language)
{
    // Diccionario de traducciones
    var translations = new Dictionary<string, Dictionary<string, string>>
    {
        ["NORMAL"] = new Dictionary<string, string>
        {
            ["ES"] = "Circulación Normal",
            ["CA"] = "Circulació Normal",
            ["EN"] = "Normal Traffic",
            ["FR"] = "Circulation Normale",
            ["DE"] = "Normaler Verkehr",
            ["IT"] = "Traffico Normale",
            ["PT"] = "Tráfego Normal"
        },
        ["EMERGENCY"] = new Dictionary<string, string>
        {
            ["ES"] = "⚠️ EMERGENCIA",
            ["CA"] = "⚠️ EMERGÈNCIA",
            ["EN"] = "⚠️ EMERGENCY",
            ["FR"] = "⚠️ URGENCE",
            ["DE"] = "⚠️ NOTFALL",
            ["IT"] = "⚠️ EMERGENZA",
            ["PT"] = "⚠️ EMERGÊNCIA"
        },
        ["EVACUATION"] = new Dictionary<string, string>
        {
            ["ES"] = "🚨 EVACUACIÓN",
            ["CA"] = "🚨 EVACUACIÓ",
            ["EN"] = "🚨 EVACUATION",
            ["FR"] = "🚨 ÉVACUATION",
            ["DE"] = "🚨 EVAKUIERUNG",
            ["IT"] = "🚨 EVACUAZIONE",
            ["PT"] = "🚨 EVACUAÇÃO"
        },
        ["CONGESTION"] = new Dictionary<string, string>
        {
            ["ES"] = "⚠️ Congestión - Reduzca Velocidad",
            ["CA"] = "⚠️ Congestió - Redueixi Velocitat",
            ["EN"] = "⚠️ Congestion - Reduce Speed",
            ["FR"] = "⚠️ Congestion - Ralentir",
            ["DE"] = "⚠️ Stau - Geschwindigkeit Reduzieren",
            ["IT"] = "⚠️ Congestione - Ridurre Velocità",
            ["PT"] = "⚠️ Congestionamento - Reduza Velocidade"
        },
        ["MAINTENANCE"] = new Dictionary<string, string>
        {
            ["ES"] = "🔧 Mantenimiento",
            ["CA"] = "🔧 Manteniment",
            ["EN"] = "🔧 Maintenance",
            ["FR"] = "🔧 Maintenance",
            ["DE"] = "🔧 Wartung",
            ["IT"] = "🔧 Manutenzione",
            ["PT"] = "🔧 Manutenção"
        }
    };
    
    if (translations.ContainsKey(mode) && 
        translations[mode].ContainsKey(language))
    {
        return translations[mode][language];
    }
    
    // Fallback a español
    return translations[mode]["ES"];
}
```

#### Lógica Final de Texto

```csharp
private string GetBeaconText(Beacon beacon)
{
    // 1. Si hay mensaje personalizado, usarlo SIEMPRE
    if (!string.IsNullOrEmpty(beacon.Message))
    {
        return beacon.Message;
    }
    
    // 2. Si no hay mensaje personalizado, usar texto por defecto según modo e idioma
    return GetDefaultMessage(beacon.Mode, beacon.Language);
}
```

---

### 4. Caso Especial: Modo EVACUATION

En modo **EVACUATION**, además del mensaje, puede haber un campo `evacuationExit`:

```csharp
private string GetEvacuationText(Beacon beacon)
{
    string text = "";
    
    // Mensaje principal
    if (!string.IsNullOrEmpty(beacon.Message))
    {
        text = beacon.Message;
    }
    else
    {
        text = GetDefaultMessage("EVACUATION", beacon.Language);
    }
    
    // Agregar salida de evacuación si existe
    if (!string.IsNullOrEmpty(beacon.EvacuationExit))
    {
        text += "\n" + beacon.EvacuationExit;
    }
    
    return text;
}
```

**Ejemplo de salida**:
```
🚨 EVACUACIÓN
SALIDA NORTE
```

---

### 5. Formateo y Renderizado del Texto

#### Consideraciones Técnicas

**Longitud del Texto**:
- El campo `message` puede contener **hasta ~500 caracteres** (sin límite estricto)
- Implementar **scroll horizontal** o **vertical** si el texto es muy largo
- Considerar **word wrap** para líneas largas

**Líneas Múltiples**:
- El texto puede contener saltos de línea (`\n`)
- Ejemplo: `"Bienvenido\nal Circuit de\nCatalunya"`
- Renderizar cada línea por separado

**Caracteres Especiales**:
- Emojis: `⚠️ 🚨 🔧 ↑ ↓ ← →`
- Acentos: `á é í ó ú ñ ç`
- Asegurarse que tu pantalla/fuente soporta UTF-8

#### Ejemplo de Renderizado

```csharp
private void RenderTextOnDisplay(Display display, string text)
{
    // Limpiar pantalla
    display.Clear();
    
    // Dividir en líneas
    string[] lines = text.Split('\n');
    
    // Configurar fuente
    display.Font = new Font("Arial", 24, FontStyle.Bold);
    display.ForegroundColor = Color.White;
    
    // Calcular posición inicial (centrado vertical)
    int lineHeight = 30;
    int totalHeight = lines.Length * lineHeight;
    int startY = (display.Height - totalHeight) / 2;
    
    // Renderizar cada línea
    for (int i = 0; i < lines.Length; i++)
    {
        int y = startY + (i * lineHeight);
        
        // Centrar horizontalmente
        var textSize = display.MeasureText(lines[i]);
        int x = (display.Width - textSize.Width) / 2;
        
        display.DrawText(lines[i], x, y);
    }
    
    // Actualizar pantalla física
    display.Refresh();
}
```

---

## 📋 Checklist de Implementación

### Paso 1: Actualizar Modelo de Datos
- [ ] Agregar propiedad `Message` a la clase `Beacon`
- [ ] Agregar atributo `[FirestoreProperty("message")]`
- [ ] Compilar y verificar que no hay errores

### Paso 2: Leer el Campo desde Firestore
- [ ] Modificar método de polling para leer `beacon.Message`
- [ ] Loggear el valor en consola para verificar
- [ ] Probar con una baliza de prueba

### Paso 3: Implementar Renderizado
- [ ] Crear método `GetBeaconText(Beacon beacon)`
- [ ] Implementar lógica: mensaje personalizado > texto por defecto
- [ ] Manejar textos multilínea (`\n`)
- [ ] Implementar word wrap si es necesario

### Paso 4: Integrar con Display
- [ ] Conectar el texto al sistema de pantalla actual
- [ ] Configurar fuente, tamaño y alineación
- [ ] Implementar scroll si el texto es muy largo

### Paso 5: Testing
- [ ] Probar con mensaje corto: `"Hola"`
- [ ] Probar con mensaje largo: `"Este es un mensaje muy largo que podría necesitar scroll o word wrap"`
- [ ] Probar con mensaje multilínea: `"Línea 1\nLínea 2\nLínea 3"`
- [ ] Probar sin mensaje (debe mostrar texto por defecto)
- [ ] Probar con diferentes idiomas
- [ ] Probar con emojis: `"⚠️ Atención"`
- [ ] Probar modo EVACUATION con `evacuationExit`

### Paso 6: Manejo de Idiomas
- [ ] Implementar diccionario de traducciones
- [ ] Usar `beacon.Language` cuando no hay mensaje personalizado
- [ ] Fallback a español si idioma no disponible

---

## 🔍 Ejemplos de Flujo Completo

### Ejemplo 1: Mensaje Personalizado Simple

**Desde el Panel Web**:
```typescript
// Usuario configura baliza BEACON_001
await beaconsService.updateBeacon("BEACON_001", {
  message: "Bienvenido al Circuit de Catalunya"
});
```

**En Firestore** (se guarda automáticamente):
```json
{
  "beaconId": "BEACON_001",
  "message": "Bienvenido al Circuit de Catalunya",
  "mode": "NORMAL",
  "language": "ES"
}
```

**En WPF** (tu código):
```csharp
// Polling detecta el cambio (en <300ms)
var beacon = doc.ConvertTo<Beacon>();

// Lees el mensaje
string text = beacon.Message; // "Bienvenido al Circuit de Catalunya"

// Lo muestras en la pantalla
display.ShowText(text);
display.Refresh();
```

**En la Pantalla Física**:
```
╔════════════════════════════════╗
║                                ║
║   Bienvenido al Circuit de     ║
║         Catalunya              ║
║                                ║
╚════════════════════════════════╝
```

---

### Ejemplo 2: Mensaje de Emergencia con Flecha

**Desde el Panel Web**:
```typescript
await beaconsService.updateBeacon("BEACON_005", {
  mode: "EMERGENCY",
  message: "⚠️ PELIGRO\nEvite esta zona",
  arrow: "LEFT",
  color: "#FF0000", // Rojo
  language: "ES"
});
```

**En WPF**:
```csharp
var beacon = doc.ConvertTo<Beacon>();

string text = beacon.Message; // "⚠️ PELIGRO\nEvite esta zona"
string arrow = beacon.Arrow;  // "LEFT"
string color = beacon.Color;  // "#FF0000"

display.BackgroundColor = ColorFromHex(color);
display.ShowText(text);
display.ShowArrow(arrow); // ←
display.Refresh();
```

**En la Pantalla**:
```
╔════════════════════════════════╗
║      ⚠️ PELIGRO        ←       ║
║    Evite esta zona             ║
╚════════════════════════════════╝
   (Fondo rojo)
```

---

### Ejemplo 3: Evacuación con Salida Específica

**Desde el Panel Web**:
```typescript
await beaconsService.updateBeacon("BEACON_010", {
  mode: "EVACUATION",
  message: "🚨 EVACUACIÓN INMEDIATA",
  evacuationExit: "SALIDA NORTE",
  arrow: "UP",
  language: "ES"
});
```

**En WPF**:
```csharp
var beacon = doc.ConvertTo<Beacon>();

string mainText = beacon.Message;           // "🚨 EVACUACIÓN INMEDIATA"
string exit = beacon.EvacuationExit;        // "SALIDA NORTE"
string fullText = mainText + "\n" + exit;   // Combinar

display.ShowText(fullText);
display.ShowArrow("UP"); // ↑
display.Refresh();
```

**En la Pantalla**:
```
╔════════════════════════════════╗
║           ↑                    ║
║   🚨 EVACUACIÓN INMEDIATA       ║
║      SALIDA NORTE              ║
╚════════════════════════════════╝
```

---

### Ejemplo 4: Sin Mensaje Personalizado (Texto por Defecto)

**Desde el Panel Web**:
```typescript
await beaconsService.updateBeacon("BEACON_020", {
  mode: "CONGESTION",
  message: "", // Vacío - usar texto por defecto
  language: "CA" // Catalán
});
```

**En WPF**:
```csharp
var beacon = doc.ConvertTo<Beacon>();

string text;
if (string.IsNullOrEmpty(beacon.Message))
{
    // Usar texto por defecto en catalán
    text = GetDefaultMessage(beacon.Mode, beacon.Language);
    // text = "⚠️ Congestió - Redueixi Velocitat"
}
else
{
    text = beacon.Message;
}

display.ShowText(text);
display.Refresh();
```

**En la Pantalla**:
```
╔════════════════════════════════╗
║   ⚠️ Congestió                 ║
║   Redueixi Velocitat           ║
╚════════════════════════════════╝
```

---

## 🚨 Casos Especiales y Edge Cases

### 1. Mensaje Muy Largo

```csharp
if (text.Length > 100)
{
    // Opción A: Truncar con "..."
    text = text.Substring(0, 97) + "...";
    
    // Opción B: Implementar scroll horizontal
    display.EnableHorizontalScroll(text, speed: 2);
    
    // Opción C: Word wrap y scroll vertical
    display.EnableWordWrap = true;
    display.EnableVerticalScroll(text);
}
```

### 2. Caracteres No Soportados

```csharp
// Si tu pantalla no soporta ciertos caracteres
text = text.Replace("🚨", "!!");
text = text.Replace("⚠️", "!");
text = RemoveUnsupportedChars(text);
```

### 3. Mensaje Vacío en Modo UNCONFIGURED

```csharp
if (beacon.Mode == "UNCONFIGURED")
{
    // No mostrar nada, o mostrar mensaje de configuración pendiente
    display.Clear();
    display.ShowText("Sistema en configuración...");
    return;
}
```

### 4. Actualización en Tiempo Real

```csharp
// Tu polling ya maneja esto
// Cada 300ms detectarás cambios en beacon.Message
// Solo asegúrate de actualizar la pantalla inmediatamente

if (lastMessage != beacon.Message)
{
    lastMessage = beacon.Message;
    await UpdateDisplayAsync(beacon);
}
```

---

## 📊 Datos de Ejemplo para Testing

Puedes usar estos datos de prueba desde el panel web:

```typescript
// Test 1: Mensaje corto
{ message: "Hola" }

// Test 2: Mensaje largo
{ message: "Este es un mensaje muy largo que podría necesitar scroll o word wrap en la pantalla de la baliza" }

// Test 3: Multilínea
{ message: "Línea 1\nLínea 2\nLínea 3" }

// Test 4: Con emojis
{ message: "⚠️ Atención\n🚨 Emergencia" }

// Test 5: Diferentes idiomas
{ message: "", mode: "NORMAL", language: "CA" } // Texto por defecto en catalán
{ message: "", mode: "EMERGENCY", language: "EN" } // Texto por defecto en inglés

// Test 6: Evacuación completa
{ 
  mode: "EVACUATION", 
  message: "🚨 EVACUACIÓN", 
  evacuationExit: "SALIDA SUR",
  arrow: "DOWN"
}

// Test 7: Vacío (debe mostrar texto por defecto)
{ message: "", mode: "CONGESTION", language: "ES" }
```

---

## 🔗 Integración con Sistema Existente

### Ya Tienes Implementado:
- ✅ Polling a Firestore (300ms)
- ✅ Procesamiento de `mode`, `arrow`, `color`, `brightness`
- ✅ Sistema de comandos (`command`, `commandTimestamp`)
- ✅ Heartbeat cada 5 segundos

### Solo Necesitas Agregar:
1. **Leer** `beacon.Message` en tu polling
2. **Renderizar** el texto en la pantalla
3. **Implementar** lógica de texto por defecto (opcional pero recomendado)

---

## 🎯 Resumen Ejecutivo

### ¿Qué es `message`?
Un campo de texto libre en Firestore que contiene el mensaje personalizado para mostrar en la baliza.

### ¿Cuándo se usa?
- Siempre que quieras mostrar un mensaje específico en una baliza
- Ejemplos: "Bienvenido", "⚠️ Precaución", "Zona de boxes", etc.

### ¿Qué hacer si está vacío?
Mostrar texto predeterminado según `mode` y `language`.

### ¿Cómo se actualiza?
Automáticamente via tu polling (ya existente). Solo leer el campo adicional.

### ¿Dónde se renderiza?
En la pantalla física de la baliza, junto con la flecha y el color de fondo.

---

## 📞 Contacto y Soporte

Si tienes dudas durante la implementación:
1. Revisa los ejemplos de código en esta guía
2. Consulta el panel web para ver cómo se configuran los mensajes
3. Prueba con una baliza individual antes de desplegar masivamente

**El sistema en el panel web ya funciona perfectamente**. Solo necesitas **leer y mostrar** el campo `message` en tu aplicación WPF.

---

**¡Éxito con la implementación! 🚀**
