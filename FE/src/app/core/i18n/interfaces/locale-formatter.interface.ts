/**
 * Producto Abstracto 2 — Formateador de locale.
 *
 * Define el comportamiento de formateo específico para un idioma.
 * Cada fábrica concreta produce su propia implementación usando la API Intl.
 */
export interface LocaleFormatter {
  /** Locale BCP-47 que usa este formateador internamente */
  readonly locale: string;

  /** Formatea un número según las convenciones del idioma */
  formatNumber(n: number): string;

  /** Formatea una fecha según las convenciones del idioma */
  formatDate(d: Date): string;
}
