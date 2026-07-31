/**
 * Producto Abstracto 1 — Configuración de idioma.
 *
 * Define los metadatos estáticos que describe cada idioma soportado.
 * Cada fábrica concreta devuelve una instancia específica de esta interfaz.
 */

// Unión de todos los códigos de idioma soportados por la aplicación
export type AppLang = 'es' | 'en' | 'it' | 'pt';

export interface LanguageConfig {
  /** Código BCP-47 corto del idioma (coincide con el archivo assets/i18n/*.json) */
  code: AppLang;

  /** Nombre del idioma escrito en el propio idioma: "Español", "English"… */
  nativeName: string;

  /** Clave de traducción usada para renderizar el label en la UI */
  translateKey: string;

  /** Locale BCP-47 completo para la API Intl (formateo de fechas y números) */
  locale: string;
}
