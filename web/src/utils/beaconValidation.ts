import { BeaconMode, ArrowDirection, Language } from "../types";

/**
 * Utilidades de validación para configuración de balizas
 */

export const VALID_MODES: BeaconMode[] = [
  "UNCONFIGURED",
  "NORMAL",
  "CONGESTION",
  "EMERGENCY",
  "EVACUATION",
  "MAINTENANCE"
];

export const VALID_ARROWS: ArrowDirection[] = [
  "NONE",
  "UP",
  "DOWN",
  "LEFT",
  "RIGHT",
  "UP_LEFT",
  "UP_RIGHT",
  "DOWN_LEFT",
  "DOWN_RIGHT"
];

export const VALID_LANGUAGES: Language[] = [
  "ES",
  "CA",
  "EN",
  "FR",
  "DE",
  "IT",
  "PT"
];

export interface ValidationError {
  field: string;
  message: string;
}

/**
 * Valida que un color esté en formato hexadecimal válido
 */
export const isValidHexColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
};

/**
 * Valida que el brillo esté en el rango válido (0-100)
 */
export const isValidBrightness = (brightness: number): boolean => {
  return brightness >= 0 && brightness <= 100;
};

/**
 * Valida que un mensaje no exceda el límite de caracteres
 */
export const isValidMessage = (message: string): boolean => {
  return message.length <= 255;
};

/**
 * Valida que una zona sea válida (no vacía, máx 50 caracteres)
 */
export const isValidZone = (zone: string): boolean => {
  return zone.trim().length > 0 && zone.length <= 50;
};

/**
 * Valida que la salida de evacuación sea válida (máx 100 caracteres)
 */
export const isValidEvacuationExit = (exit: string): boolean => {
  return exit.length <= 100;
};

/**
 * Valida una configuración completa de baliza
 */
export const validateBeaconConfig = (config: {
  mode?: BeaconMode;
  arrow?: ArrowDirection;
  message?: string;
  color?: string;
  brightness?: number;
  language?: Language;
  evacuationExit?: string;
  zone?: string;
}): ValidationError[] => {
  const errors: ValidationError[] = [];

  // Validar modo
  if (config.mode && !VALID_MODES.includes(config.mode)) {
    errors.push({ field: "mode", message: "Modo no válido" });
  }

  // Validar flecha
  if (config.arrow && !VALID_ARROWS.includes(config.arrow)) {
    errors.push({ field: "arrow", message: "Dirección de flecha no válida" });
  }

  // Validar idioma
  if (config.language && !VALID_LANGUAGES.includes(config.language)) {
    errors.push({ field: "language", message: "Idioma no válido" });
  }

  // Validar mensaje
  if (config.message && !isValidMessage(config.message)) {
    errors.push({ field: "message", message: "El mensaje no puede exceder 255 caracteres" });
  }

  // Validar color
  if (config.color && !isValidHexColor(config.color)) {
    errors.push({ field: "color", message: "Color debe ser un valor hexadecimal válido (#RRGGBB)" });
  }

  // Validar brillo
  if (config.brightness !== undefined && !isValidBrightness(config.brightness)) {
    errors.push({ field: "brightness", message: "El brillo debe estar entre 0 y 100" });
  }

  // Validar zona
  if (config.zone && !isValidZone(config.zone)) {
    errors.push({ field: "zone", message: "La zona es obligatoria y no puede exceder 50 caracteres" });
  }

  // Validar salida de evacuación
  if (config.evacuationExit && !isValidEvacuationExit(config.evacuationExit)) {
    errors.push({ field: "evacuationExit", message: "La salida de evacuación no puede exceder 100 caracteres" });
  }

  // Validación especial: si el modo es EVACUATION, la salida de evacuación es obligatoria
  if (config.mode === "EVACUATION" && (!config.evacuationExit || !config.evacuationExit.trim())) {
    errors.push({ field: "evacuationExit", message: "La salida de evacuación es obligatoria en modo EVACUATION" });
  }

  return errors;
};

/**
 * Normaliza un color para asegurar que esté en mayúsculas
 */
export const normalizeColor = (color: string): string => {
  return color.toUpperCase();
};

/**
 * Limpia y normaliza un mensaje eliminando espacios extra
 */
export const normalizeMessage = (message: string): string => {
  return message.trim().replace(/\s+/g, ' ');
};

/**
 * Parsea tags desde un string JSON o devuelve array vacío si falla
 */
export const parseTags = (tagsJson: string | null): string[] => {
  if (!tagsJson) return [];
  try {
    const parsed = JSON.parse(tagsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

/**
 * Convierte tags a string JSON para almacenar en la base de datos
 */
export const stringifyTags = (tags: string[]): string => {
  return JSON.stringify(tags);
};
