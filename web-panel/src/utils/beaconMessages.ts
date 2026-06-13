import { BeaconMode, ArrowDirection, Language } from "../types";

/**
 * Obtiene el mensaje predefinido según modo, idioma y dirección de flecha
 */
export const getDefaultBeaconMessage = (
  mode: BeaconMode,
  language: Language,
  arrow: ArrowDirection = "NONE"
): string => {
  // MODO NORMAL: Texto varía según la dirección de la flecha
  if (mode === "NORMAL") {
    return getNormalModeMessage(arrow, language);
  }

  // Otros modos: Mensajes estándar
  const messages: Record<BeaconMode, Record<Language, string>> = {
    UNCONFIGURED: {
      ES: "Sistema en Configuración",
      CA: "Sistema en Configuració",
      EN: "System in Configuration",
      FR: "Système en Configuration",
      DE: "System in Konfiguration",
      IT: "Sistema in Configurazione",
      PT: "Sistema em Configuração"
    },
    NORMAL: {
      ES: "Circulación Normal",
      CA: "Circulació Normal",
      EN: "Normal Traffic",
      FR: "Circulation Normale",
      DE: "Normaler Verkehr",
      IT: "Traffico Normale",
      PT: "Tráfego Normal"
    },
    CONGESTION: {
      ES: "⚠️ Congestión\nReduzca Velocidad",
      CA: "⚠️ Congestió\nRedueixi Velocitat",
      EN: "⚠️ Congestion\nReduce Speed",
      FR: "⚠️ Congestion\nRalentir",
      DE: "⚠️ Stau\nGeschwindigkeit Reduzieren",
      IT: "⚠️ Congestione\nRidurre Velocità",
      PT: "⚠️ Congestionamento\nReduza Velocidade"
    },
    EMERGENCY: {
      ES: "⚠️ EMERGENCIA\nPRECAUCIÓN",
      CA: "⚠️ EMERGÈNCIA\nPRECAUCIÓ",
      EN: "⚠️ EMERGENCY\nCAUTION",
      FR: "⚠️ URGENCE\nPRUDENCE",
      DE: "⚠️ NOTFALL\nVORSICHT",
      IT: "⚠️ EMERGENZA\nATTENZIONE",
      PT: "⚠️ EMERGÊNCIA\nCUIDADO"
    },
    EVACUATION: {
      ES: "🚨 EVACUACIÓN\nSiga las Flechas",
      CA: "🚨 EVACUACIÓ\nSegueixi les Fletxes",
      EN: "🚨 EVACUATION\nFollow the Arrows",
      FR: "🚨 ÉVACUATION\nSuivez les Flèches",
      DE: "🚨 EVAKUIERUNG\nFolgen Sie den Pfeilen",
      IT: "🚨 EVACUAZIONE\nSegui le Frecce",
      PT: "🚨 EVACUAÇÃO\nSiga as Setas"
    },
    MAINTENANCE: {
      ES: "🔧 Mantenimiento\nFuera de Servicio",
      CA: "🔧 Manteniment\nFora de Servei",
      EN: "🔧 Maintenance\nOut of Service",
      FR: "🔧 Maintenance\nHors Service",
      DE: "🔧 Wartung\nAußer Betrieb",
      IT: "🔧 Manutenzione\nFuori Servizio",
      PT: "🔧 Manutenção\nFora de Serviço"
    }
  };

  return messages[mode]?.[language] || messages[mode]?.["ES"] || "Sistema Activo";
};

/**
 * Mensajes específicos para modo NORMAL según dirección de flecha
 */
const getNormalModeMessage = (arrow: ArrowDirection, language: Language): string => {
  const normalMessages: Record<ArrowDirection, Record<Language, string>> = {
    NONE: {
      ES: "Circulación Normal",
      CA: "Circulació Normal",
      EN: "Normal Traffic",
      FR: "Circulation Normale",
      DE: "Normaler Verkehr",
      IT: "Traffico Normale",
      PT: "Tráfego Normal"
    },
    UP: {
      ES: "Continúe Recto",
      CA: "Continuï Recte",
      EN: "Continue Straight",
      FR: "Continuez Tout Droit",
      DE: "Geradeaus Weiter",
      IT: "Proseguire Dritto",
      PT: "Continue em Frente"
    },
    DOWN: {
      ES: "Retroceda",
      CA: "Retrocedeixi",
      EN: "Go Back",
      FR: "Reculez",
      DE: "Zurück",
      IT: "Tornare Indietro",
      PT: "Volte"
    },
    LEFT: {
      ES: "Gire a la Izquierda",
      CA: "Giri a l'Esquerra",
      EN: "Turn Left",
      FR: "Tournez à Gauche",
      DE: "Links Abbiegen",
      IT: "Svoltare a Sinistra",
      PT: "Vire à Esquerda"
    },
    RIGHT: {
      ES: "Gire a la Derecha",
      CA: "Giri a la Dreta",
      EN: "Turn Right",
      FR: "Tournez à Droite",
      DE: "Rechts Abbiegen",
      IT: "Svoltare a Destra",
      PT: "Vire à Direita"
    },
    UP_LEFT: {
      ES: "Diagonal Izquierda",
      CA: "Diagonal Esquerra",
      EN: "Diagonal Left",
      FR: "Diagonale Gauche",
      DE: "Diagonal Links",
      IT: "Diagonale Sinistra",
      PT: "Diagonal Esquerda"
    },
    UP_RIGHT: {
      ES: "Diagonal Derecha",
      CA: "Diagonal Dreta",
      EN: "Diagonal Right",
      FR: "Diagonale Droite",
      DE: "Diagonal Rechts",
      IT: "Diagonale Destra",
      PT: "Diagonal Direita"
    },
    DOWN_LEFT: {
      ES: "Retroceda Izquierda",
      CA: "Retrocedeixi Esquerra",
      EN: "Back Left",
      FR: "Reculez à Gauche",
      DE: "Zurück Links",
      IT: "Indietro Sinistra",
      PT: "Volte Esquerda"
    },
    DOWN_RIGHT: {
      ES: "Retroceda Derecha",
      CA: "Retrocedeixi Dreta",
      EN: "Back Right",
      FR: "Reculez à Droite",
      DE: "Zurück Rechts",
      IT: "Indietro Destra",
      PT: "Volte Direita"
    },
    FORWARD: {
      ES: "Continúe Recto",
      CA: "Continuï Recte",
      EN: "Continue Straight",
      FR: "Continuez Tout Droit",
      DE: "Geradeaus Weiter",
      IT: "Proseguire Dritto",
      PT: "Continue em Frente"
    },
    BACKWARD: {
      ES: "Retroceda",
      CA: "Retrocedeixi",
      EN: "Go Back",
      FR: "Reculez",
      DE: "Zurück",
      IT: "Tornare Indietro",
      PT: "Volte"
    },
    FORWARD_LEFT: {
      ES: "Diagonal Izquierda",
      CA: "Diagonal Esquerra",
      EN: "Diagonal Left",
      FR: "Diagonale Gauche",
      DE: "Diagonal Links",
      IT: "Diagonale Sinistra",
      PT: "Diagonal Esquerda"
    },
    FORWARD_RIGHT: {
      ES: "Diagonal Derecha",
      CA: "Diagonal Dreta",
      EN: "Diagonal Right",
      FR: "Diagonale Droite",
      DE: "Diagonal Rechts",
      IT: "Diagonale Destra",
      PT: "Diagonal Direita"
    },
    BACKWARD_LEFT: {
      ES: "Retroceda Izquierda",
      CA: "Retrocedeixi Esquerra",
      EN: "Back Left",
      FR: "Reculez à Gauche",
      DE: "Zurück Links",
      IT: "Indietro Sinistra",
      PT: "Volte Esquerda"
    },
    BACKWARD_RIGHT: {
      ES: "Retroceda Derecha",
      CA: "Retrocedeixi Dreta",
      EN: "Back Right",
      FR: "Reculez à Droite",
      DE: "Zurück Rechts",
      IT: "Indietro Destra",
      PT: "Volte Direita"
    }
  };

  return normalMessages[arrow]?.[language] || normalMessages["NONE"]?.[language] || "Circulación Normal";
};
