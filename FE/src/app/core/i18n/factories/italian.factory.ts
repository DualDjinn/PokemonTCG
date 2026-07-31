import { LanguageFactory } from '../interfaces/language-factory.interface';
import { LanguageConfig } from '../interfaces/language-config.interface';
import { LocaleFormatter } from '../interfaces/locale-formatter.interface';

/**
 * Formateador concreto para el idioma italiano (locale it-IT).
 * Usa la API Intl del navegador para formatear números y fechas.
 */
class ItalianLocaleFormatter implements LocaleFormatter {
  readonly locale = 'it-IT';

  formatNumber(n: number): string {
    // Ejemplo: 1234567 → "1.234.567"
    return new Intl.NumberFormat(this.locale).format(n);
  }

  formatDate(d: Date): string {
    // Ejemplo: "7/6/2026"
    return new Intl.DateTimeFormat(this.locale).format(d);
  }
}

/**
 * Fábrica Concreta — Italiano.
 *
 * Crea los productos específicos del idioma italiano:
 * su configuración de metadatos y su formateador de locale.
 */
export class ItalianLanguageFactory implements LanguageFactory {
  createConfig(): LanguageConfig {
    return {
      code: 'it',
      nativeName: 'Italiano',
      translateKey: 'GAME_OPTIONS.ITALIAN',
      locale: 'it-IT',
    };
  }

  createFormatter(): LocaleFormatter {
    return new ItalianLocaleFormatter();
  }
}
