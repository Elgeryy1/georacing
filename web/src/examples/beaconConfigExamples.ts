/**
 * GUÍA DE USO - Sistema de Configuración de Balizas
 * 
 * Este archivo contiene ejemplos de código para usar todas las funcionalidades
 * del sistema de configuración de balizas.
 */

import { beaconsService } from "../services/beaconService";
import { BeaconUpdate, Language } from "../types";
import { validateBeaconConfig, parseTags, stringifyTags } from "../utils/beaconValidation";

// ========================================
// EJEMPLO 1: Configurar una baliza nueva
// ========================================
export const ejemploConfigurarBaliza = async (beaconId: string) => {
  const config: BeaconUpdate = {
    mode: "NORMAL",
    arrow: "RIGHT",
    message: "Acceso Principal",
    color: "#00FFAA",
    brightness: 90,
    language: "ES",
    zone: "GRADA-G",
    tags: ["acceso", "principal"]
  };

  try {
    // Validar antes de enviar
    const errors = validateBeaconConfig(config);
    if (errors.length > 0) {
      console.error("Errores de validación:", errors);
      return;
    }

    // Enviar configuración
    await beaconsService.configureBeacon(beaconId, config);
    console.log("✅ Baliza configurada correctamente");
  } catch (error) {
    console.error("❌ Error al configurar baliza:", error);
  }
};

// ========================================
// EJEMPLO 2: Actualizar una baliza existente
// ========================================
export const ejemploActualizarBaliza = async (beaconId: string) => {
  const updates: BeaconUpdate = {
    message: "Nuevo mensaje",
    brightness: 75,
    color: "#FF6600"
  };

  try {
    await beaconsService.updateBeacon(beaconId, updates);
    console.log("✅ Baliza actualizada");
  } catch (error) {
    console.error("❌ Error al actualizar:", error);
  }
};

// ========================================
// EJEMPLO 3: Activar modo evacuación en una zona
// ========================================
export const ejemploEvacuacionZonal = async (zona: string) => {
  const config: BeaconUpdate = {
    mode: "EVACUATION",
    message: "EVACUACIÓN - Siga las flechas",
    evacuationExit: "SALIDA NORTE",
    arrow: "UP",
    color: "#FF0000",
    brightness: 100,
    language: "ES"
  };

  try {
    // Obtener todas las balizas de la zona
    // (En tu código real, filtra las balizas por zona)
    const beaconIds = ["beacon1", "beacon2"]; // IDs de ejemplo
    
    await beaconsService.updateMultipleBeacons(beaconIds, config);
    console.log(`✅ Evacuación activada en zona ${zona}`);
  } catch (error) {
    console.error("❌ Error en evacuación:", error);
  }
};

// ========================================
// EJEMPLO 4: Configurar mensaje multiidioma
// ========================================
export const ejemploMensajeMultiidioma = async () => {
  const mensajes: Record<Language, string> = {
    ES: "Bienvenido al circuito",
    CA: "Benvingut al circuit",
    EN: "Welcome to the circuit",
    FR: "Bienvenue au circuit",
    DE: "Willkommen auf der Rennstrecke",
    IT: "Benvenuto al circuito",
    PT: "Bem-vindo ao circuito"
  };

  // Configurar diferentes balizas con diferentes idiomas
  // Ejemplo comentado de uso:
  // for (const [language, message] of Object.entries(mensajes)) {
  //   await beaconsService.updateBeacon(`beacon-${language}`, {
  //     mode: "NORMAL",
  //     message,
  //     language: language as Language,
  //     color: "#00FFAA",
  //     brightness: 90
  //   });
  // }
  console.log("Mensajes multilenguaje preparados:", mensajes);
};

// ========================================
// EJEMPLO 5: Sistema de tags
// ========================================
export const ejemploGestionTags = () => {
  // Parsear tags desde JSON (desde base de datos)
  const tagsJson = '["vip", "acceso", "principal"]';
  const tags = parseTags(tagsJson);
  console.log("Tags parseados:", tags); // ["vip", "acceso", "principal"]

  // Agregar nuevo tag
  tags.push("prioritario");

  // Convertir a JSON para guardar
  const tagsToSave = stringifyTags(tags);
  console.log("Tags para guardar:", tagsToSave); // '["vip", "acceso", "principal", "prioritario"]'
};

// ========================================
// EJEMPLO 6: Configuración según hora del día
// ========================================
export const ejemploConfiguracionDinamica = async (beaconId: string) => {
  const hora = new Date().getHours();
  
  let config: BeaconUpdate;
  
  if (hora >= 6 && hora < 12) {
    // Mañana: modo normal, mensaje de bienvenida
    config = {
      mode: "NORMAL",
      message: "Buenos días - Acceso Abierto",
      color: "#00FFAA",
      brightness: 80
    };
  } else if (hora >= 12 && hora < 20) {
    // Tarde: modo normal, alta visibilidad
    config = {
      mode: "NORMAL",
      message: "Acceso Principal",
      color: "#00FFAA",
      brightness: 100
    };
  } else {
    // Noche: modo mantenimiento o baja intensidad
    config = {
      mode: "MAINTENANCE",
      message: "Cerrado - Fuera de Horario",
      color: "#808080",
      brightness: 30
    };
  }
  
  await beaconsService.updateBeacon(beaconId, config);
};

// ========================================
// EJEMPLO 7: Configuración según capacidad
// ========================================
export const ejemploConfiguracionPorCapacidad = async (
  beaconId: string,
  ocupacionPorcentaje: number
) => {
  let config: BeaconUpdate;
  
  if (ocupacionPorcentaje < 50) {
    // Baja ocupación: verde, acceso normal
    config = {
      mode: "NORMAL",
      message: "Acceso Libre",
      color: "#00FF00",
      arrow: "RIGHT",
      brightness: 90
    };
  } else if (ocupacionPorcentaje < 80) {
    // Media ocupación: amarillo, precaución
    config = {
      mode: "CONGESTION",
      message: "Afluencia Media - Precaución",
      color: "#FFA500",
      arrow: "DOWN",
      brightness: 100
    };
  } else {
    // Alta ocupación: rojo, acceso cerrado
    config = {
      mode: "EMERGENCY",
      message: "Aforo Completo - Acceso Cerrado",
      color: "#FF0000",
      arrow: "NONE",
      brightness: 100
    };
  }
  
  await beaconsService.updateBeacon(beaconId, config);
};

// ========================================
// EJEMPLO 8: Configuración masiva por lotes
// ========================================
export const ejemploConfiguracionMasiva = async (zonas: string[]) => {
  const configPorZona: Record<string, BeaconUpdate> = {
    "PADDOCK": {
      mode: "NORMAL",
      message: "Zona Paddock - Solo Personal Autorizado",
      color: "#0066FF",
      brightness: 85,
      zone: "PADDOCK",
      tags: ["paddock", "restringido"]
    },
    "GRADA-A": {
      mode: "NORMAL",
      message: "Grada A - Sección Premium",
      color: "#FFD700",
      brightness: 90,
      zone: "GRADA-A",
      tags: ["grada", "premium"]
    },
    "PARKING": {
      mode: "NORMAL",
      message: "Parking - Siga las flechas",
      color: "#00FFAA",
      arrow: "LEFT",
      brightness: 80,
      zone: "PARKING",
      tags: ["parking", "acceso"]
    }
  };

  for (const zona of zonas) {
    const config = configPorZona[zona];
    if (config) {
      // En tu código real, obtener IDs de balizas por zona
      const beaconIds = [`beacon-${zona}-1`, `beacon-${zona}-2`];
      await beaconsService.updateMultipleBeacons(beaconIds, config);
      console.log(`✅ Configurada zona ${zona}`);
    }
  }
};

// ========================================
// EJEMPLO 9: Monitoreo y actualización automática
// ========================================
export const ejemploMonitoreoAutomatico = () => {
  // Suscribirse a cambios de balizas
  const unsubscribe = beaconsService.subscribeToBeacons((beacons) => {
    console.log("📡 Balizas actualizadas:", beacons.length);
    
    // Detectar balizas con batería baja
    const bateriasBajas = beacons.filter(b => b.battery && b.battery < 20);
    if (bateriasBajas.length > 0) {
      console.warn("⚠️ Balizas con batería baja:", bateriasBajas.map(b => b.beaconId));
    }
    
    // Detectar balizas offline
    const offline = beacons.filter(b => !b.online);
    if (offline.length > 0) {
      console.warn("🔴 Balizas offline:", offline.map(b => b.beaconId));
    }
    
    // Detectar balizas sin configurar
    const sinConfigurar = beacons.filter(b => !b.configured);
    if (sinConfigurar.length > 0) {
      console.warn("⚙️ Balizas sin configurar:", sinConfigurar.map(b => b.beaconId));
    }
  }, 5000); // Poll cada 5 segundos
  
  // Limpiar al desmontar
  return unsubscribe;
};

// ========================================
// EJEMPLO 10: Validación completa antes de guardar
// ========================================
export const ejemploValidacionCompleta = (config: BeaconUpdate) => {
  // Validar toda la configuración
  const errors = validateBeaconConfig(config);
  
  if (errors.length > 0) {
    console.error("❌ Errores de validación encontrados:");
    errors.forEach(error => {
      console.error(`  - ${error.field}: ${error.message}`);
    });
    return false;
  }
  
  console.log("✅ Configuración válida");
  return true;
};

// ========================================
// EJEMPLO 11: Resetear baliza a valores por defecto
// ========================================
export const ejemploResetBaliza = async (beaconId: string) => {
  const defaultConfig: BeaconUpdate = {
    mode: "NORMAL",
    arrow: "NONE",
    message: "Sistema Operativo",
    color: "#00FFAA",
    brightness: 90,
    language: "ES",
    evacuationExit: undefined,
    zone: "GENERAL",
    tags: []
  };
  
  await beaconsService.updateBeacon(beaconId, defaultConfig);
  console.log("✅ Baliza reseteada a valores por defecto");
};

// ========================================
// EJEMPLO 12: Configuración por evento especial
// ========================================
export const ejemploEventoEspecial = async (nombreEvento: string) => {
  const configsEvento: Record<string, BeaconUpdate> = {
    "CARRERA_F1": {
      mode: "NORMAL",
      message: "Gran Premio F1 - Siga las indicaciones",
      color: "#FF0000",
      brightness: 100,
      tags: ["evento", "f1", "carrera"]
    },
    "CONCIERTO": {
      mode: "NORMAL",
      message: "Concierto en Vivo - Zona de Espectadores",
      color: "#9B59B6",
      brightness: 95,
      tags: ["evento", "concierto", "entretenimiento"]
    },
    "TOUR_GUIADO": {
      mode: "NORMAL",
      message: "Tour del Circuito - Punto de Encuentro",
      color: "#3498DB",
      brightness: 85,
      tags: ["evento", "tour", "visita"]
    }
  };
  
  const config = configsEvento[nombreEvento];
  if (config) {
    // Aplicar a todas las balizas relevantes
    console.log(`🎉 Configurando evento: ${nombreEvento}`);
  }
};

/**
 * NOTAS IMPORTANTES:
 * 
 * 1. Siempre valida la configuración antes de enviarla
 * 2. El campo 'configured' se marca automáticamente como true al guardar
 * 3. Tags se almacenan como JSON en la base de datos
 * 4. Usa evacuationExit solo en modo EVACUATION
 * 5. El mensaje puede estar vacío (usará predeterminado según modo/idioma)
 * 6. El brillo debe estar entre 0-100
 * 7. Color debe ser hexadecimal válido (#RRGGBB)
 * 8. Zone es importante para evacuaciones zonales
 * 9. Los cambios se envían tanto a la baliza como a la base de datos
 * 10. Polling automático cada 4 segundos por defecto
 */

export default {
  ejemploConfigurarBaliza,
  ejemploActualizarBaliza,
  ejemploEvacuacionZonal,
  ejemploMensajeMultiidioma,
  ejemploGestionTags,
  ejemploConfiguracionDinamica,
  ejemploConfiguracionPorCapacidad,
  ejemploConfiguracionMasiva,
  ejemploMonitoreoAutomatico,
  ejemploValidacionCompleta,
  ejemploResetBaliza,
  ejemploEventoEspecial
};
