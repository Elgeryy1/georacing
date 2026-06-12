# Sistema de Comandos - GeoRacing Balizas

## 📋 Descripción General

El sistema de comandos permite enviar instrucciones remotas a las balizas desde el panel web. Los comandos se sincronizan a través de Firestore y son procesados por las aplicaciones WPF que ejecutan polling cada 300ms.

## 🔧 Arquitectura

### Flujo de Comandos

```
Panel Web → Firestore → WPF App (Polling 300ms) → Baliza Hardware
```

1. **Panel Web**: Escribe `command` y `commandTimestamp` en Firestore
2. **Firestore**: Base de datos en tiempo real
3. **WPF App**: Detecta nuevo comando via polling
4. **Baliza Hardware**: Ejecuta el comando

### Campos en Firestore

```typescript
interface Beacon {
  // ... otros campos ...
  command?: string;              // Comando a ejecutar
  commandTimestamp?: string;     // Timestamp ISO del comando
}
```

## 🎯 Comandos Disponibles

### RESTART
**⚠️ REINICIA TODO EL SISTEMA WINDOWS** del ordenador que controla la baliza.

**IMPORTANTE**: Este comando **NO reinicia solo la aplicación**, reinicia el **ordenador completo**. El sistema Windows se apagará y volverá a encender.

**Uso desde código:**
```typescript
// ⚠️ Esto reiniciará el sistema Windows completo
await beaconsService.restartBeacon("BEACON_001");
```

**Uso desde UI:**
- Botón "Reiniciar" en el modal de edición
- Botón "Reiniciar Todas" en el Dashboard
- Panel de comandos personalizado

### Comandos Personalizados

Puedes enviar cualquier comando personalizado usando el panel de comandos:

```typescript
await beaconsService.sendCommand("BEACON_001", "STATUS");
await beaconsService.sendCommand("BEACON_001", "CONFIG");
await beaconsService.sendCommand("BEACON_001", "TEST");
```

## 🚀 Funciones de Servicio

### `restartBeacon(beaconId: string)`

Reinicia una baliza específica.

```typescript
try {
  await beaconsService.restartBeacon("BEACON_001");
  console.log("✅ Comando de reinicio enviado");
} catch (error) {
  console.error("❌ Error:", error);
}
```

### `restartAllBeacons()`

**⚠️ REINICIA TODOS LOS SISTEMAS WINDOWS** de todas las balizas usando operaciones batch.

**IMPORTANTE**: Esto reiniciará **todos los ordenadores completos**, no solo las aplicaciones.

```typescript
try {
  // ⚠️ Esto reiniciará TODOS los sistemas Windows
  const count = await beaconsService.restartAllBeacons();
  console.log(`✅ ${count} sistemas Windows reiniciados`);
} catch (error) {
  console.error("❌ Error:", error);
}
```

**Implementación interna:**
```typescript
const batch = writeBatch(db);
const beaconsRef = collection(db, "beacons");
const snapshot = await getDocs(beaconsRef);
const timestamp = new Date().toISOString();

snapshot.forEach((doc) => {
  batch.update(doc.ref, {
    command: "RESTART",
    commandTimestamp: timestamp
  });
});

await batch.commit();
return snapshot.size;
```

### `sendCommand(beaconId: string, command: string)`

Envía un comando personalizado a una baliza.

```typescript
await beaconsService.sendCommand("BEACON_001", "CUSTOM_CMD");
```

## 🎨 Componentes UI

### CommandPanel

Panel para ejecutar comandos personalizados.

**Props:**
- `beaconId?: string` - ID de la baliza (opcional)
- `onCommandSent?: () => void` - Callback al enviar comando

**Uso:**
```tsx
import { CommandPanel } from "../components/CommandPanel";

<CommandPanel 
  beaconId="BEACON_001"
  onCommandSent={() => console.log("Comando enviado")}
/>
```

**Características:**
- ✅ Input de texto para comando personalizado
- ✅ Botón de envío con estado de carga
- ✅ Soporte para Enter key
- ✅ Validación de entrada
- ✅ Feedback visual

### BeaconEditModal - Botón Reiniciar

El modal de edición incluye un botón naranja para reiniciar:

```tsx
<button onClick={handleRestart}>
  🔄 Reiniciar
</button>
```

**Características:**
- ⚠️ Confirmación antes de reiniciar
- 🔒 Disabled durante operaciones
- ✅ Feedback de éxito/error

### Dashboard - Botón Reiniciar Todas

El dashboard incluye un botón global para reiniciar todas las balizas:

```tsx
<button onClick={handleRestartAll}>
  🔄 Reiniciar Todas
</button>
```

**Características:**
- ⚠️ Confirmación con contador de balizas
- 📊 Muestra cantidad de balizas afectadas
- ✅ Usa operaciones batch para eficiencia

## 💻 Integración con WPF

### Detección de Comandos (C# .NET 8)

#### Reinicio de Windows
```csharp
private async Task RestartWindowsSystemAsync(string beaconId)
{
    try
    {
        Console.WriteLine($"⚠️ REINICIANDO SISTEMA WINDOWS para {beaconId}");
        
        // Guardar estado antes de reiniciar
        await SaveStateAsync();
        
        // Ejecutar comando de reinicio de Windows
        // -r: reiniciar, -t 10: esperar 10 segundos, -f: forzar cierre de apps
        Process.Start("shutdown", "/r /t 10 /f /c \"Reinicio remoto desde panel de control\"");
        
        Console.WriteLine($"✅ Sistema Windows se reiniciará en 10 segundos");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"❌ Error al reiniciar sistema: {ex.Message}");
    }
}
```

#### Polling de Comandos
```csharp
private async Task PollFirestoreAsync()
{
    while (true)
    {
        var snapshot = await beaconsRef.GetSnapshotAsync();
        
        foreach (var doc in snapshot.Documents)
        {
            var beacon = doc.ConvertTo<Beacon>();
            
            // Detectar nuevo comando
            if (!string.IsNullOrEmpty(beacon.Command))
            {
                await ProcessCommandAsync(beacon);
                
                // Limpiar comando después de procesar
                await doc.Reference.UpdateAsync(new Dictionary<string, object>
                {
                    { "command", FieldValue.Delete },
                    { "commandTimestamp", FieldValue.Delete }
                });
            }
        }
        
        await Task.Delay(300); // Polling cada 300ms
    }
}

private async Task ProcessCommandAsync(Beacon beacon)
{
    switch (beacon.Command)
    {
        case "RESTART":
            // ⚠️ REINICIA TODO EL SISTEMA WINDOWS
            await RestartWindowsSystemAsync(beacon.BeaconId);
            break;
        case "STATUS":
            await GetBeaconStatusAsync(beacon.BeaconId);
            break;
        default:
            Console.WriteLine($"Comando desconocido: {beacon.Command}");
            break;
    }
}
```

### Limpieza de Comandos

**Importante**: Los comandos se **auto-limpian automáticamente después de 7 segundos** desde el panel web.

#### Limpieza Automática desde Web
El panel web usa `setTimeout` para eliminar los campos después de 7 segundos:

```typescript
// El comando permanece en Firestore por 7 segundos
await beaconsService.restartBeacon("BEACON_001");

// Después de 7 segundos, se auto-limpia:
setTimeout(async () => {
  await updateDoc(beaconRef, {
    command: deleteField(),
    commandTimestamp: deleteField()
  });
}, 7000);
```

#### Limpieza Manual desde WPF (Opcional)
Las aplicaciones WPF pueden limpiar inmediatamente después de procesar:

```csharp
await doc.Reference.UpdateAsync(new Dictionary<string, object>
{
    { "command", FieldValue.Delete },
    { "commandTimestamp", FieldValue.Delete }
});
```

**Recomendación**: Como el web ya limpia a los 7 segundos, WPF solo necesita:
1. Detectar el comando en polling (300ms)
2. Ejecutarlo inmediatamente
3. (Opcional) Limpiar si se procesa antes de 7s

## 🔐 Seguridad

### Firestore Rules

```javascript
match /beacons/{beaconId} {
  // Solo usuarios autenticados pueden enviar comandos
  allow update: if request.auth != null 
    && request.resource.data.keys().hasAny(['command', 'commandTimestamp']);
}
```

### Validación de Comandos

**En el cliente (TypeScript):**
```typescript
const ALLOWED_COMMANDS = ['RESTART', 'STATUS', 'CONFIG', 'TEST'];

function validateCommand(command: string): boolean {
  return ALLOWED_COMMANDS.includes(command.toUpperCase());
}
```

**En el servidor (WPF):**
```csharp
private bool IsCommandValid(string command)
{
    var allowedCommands = new[] { "RESTART", "STATUS", "CONFIG", "TEST" };
    return allowedCommands.Contains(command.ToUpper());
}
```

## 📊 Monitoreo

### Logs de Comandos

**Agregar logging en WPF:**
```csharp
private async Task LogCommandAsync(string beaconId, string command)
{
    await Firestore.Collection("command_logs").AddAsync(new
    {
        BeaconId = beaconId,
        Command = command,
        Timestamp = DateTime.UtcNow,
        Status = "executed"
    });
}
```

**Consultar logs desde la web:**
```typescript
async function getCommandLogs(beaconId: string) {
  const logsRef = collection(db, "command_logs");
  const q = query(
    logsRef, 
    where("beaconId", "==", beaconId),
    orderBy("timestamp", "desc"),
    limit(10)
  );
  
  const snapshot = await getDocs(q);
  return snapshot.docs.map(doc => doc.data());
}
```

## 🎯 Casos de Uso

### 1. Reinicio de Sistema Windows
```typescript
// Usuario hace clic en "Reiniciar Windows" en el modal
await beaconsService.restartBeacon("BEACON_001");
// ✅ Comando enviado
// ⏱️ WPF detecta en <300ms
// 🔄 Sistema Windows completo se reinicia (shutdown -r)
// ⏳ Tiempo de reinicio: ~1-2 minutos
```

### 2. Reinicio Masivo de Sistemas
```typescript
// Usuario hace clic en "Reiniciar Todos (Windows)"
const count = await beaconsService.restartAllBeacons();
// ✅ 150 sistemas Windows procesados
// 📦 Usando batch operations
// ⚡ Todos los ordenadores se reiniciarán simultáneamente
// ⏳ Downtime total: ~1-2 minutos por sistema
```

### 3. Comando Personalizado
```typescript
// Usuario escribe "STATUS" en el panel
await beaconsService.sendCommand("BEACON_001", "STATUS");
// ✅ Comando personalizado enviado
// 📊 WPF procesa y responde
```

### 4. Diagnóstico Remoto
```typescript
// Enviar serie de comandos de diagnóstico
await beaconsService.sendCommand("BEACON_001", "DIAGNOSTIC");
await beaconsService.sendCommand("BEACON_001", "REPORT");
await beaconsService.sendCommand("BEACON_001", "CLEAR_CACHE");
```

## ⚠️ Consideraciones

### Timing
- Polling WPF: 300ms
- Latencia Firestore: ~100-500ms
- **Persistencia del comando**: 7 segundos (auto-limpieza)
- **Total**: Comando ejecutado en <1 segundo, visible por 7s

### Errores Comunes

**1. Comando no procesado**
- ✅ Verificar que WPF esté ejecutando polling
- ✅ Comprobar conectividad con Firestore
- ✅ Validar permisos de Firestore

**2. Ejecución duplicada**
- ✅ Panel web auto-limpia comandos a los 7 segundos
- ✅ Usar `commandTimestamp` para deduplicación si WPF ejecuta múltiples veces
- 💡 Tip: Guardar último timestamp procesado para evitar duplicados

**3. Comandos perdidos**
- 📝 Implementar sistema de logs
- 🔄 Agregar retry logic en WPF

### Best Practices

1. **Siempre usar confirmación** para comandos destructivos
2. **Auto-limpieza a 7 segundos** implementada en panel web
3. **Validar comandos** en cliente y servidor
4. **Implementar logging** para auditoría
5. **Usar batch operations** para operaciones masivas
6. **Deduplicación con timestamp** en WPF para evitar ejecuciones múltiples
7. **No reintentar RESTART** - el comando persiste 7s, suficiente para detectar

## 🚀 Próximas Mejoras

- [ ] Sistema de cola de comandos
- [ ] Comandos programados (schedule)
- [ ] Historial de comandos en UI
- [ ] Rollback de comandos
- [ ] Comandos condicionales
- [ ] Dashboard de monitoreo de comandos
- [ ] Notificaciones push al completar comandos

## 📚 Referencias

- [Firestore Batch Operations](https://firebase.google.com/docs/firestore/manage-data/transactions)
- [React Component Best Practices](https://react.dev/learn)
- [TypeScript Type Safety](https://www.typescriptlang.org/docs/)
- [WPF Async Patterns](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/)
