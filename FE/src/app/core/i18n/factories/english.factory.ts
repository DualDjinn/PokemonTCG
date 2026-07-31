import { LanguageFactory } from '../interfaces/language-factory.interface';
import { LanguageConfig } from '../interfaces/language-config.interface';
import { LocaleFormatter } from '../interfaces/locale-formatter.interface';

/**
 * Formateador concreto para el idioma inglés (locale en-US).
 * Usa la API Intl del navegador para formatear números y fechas.
 */
class EnglishLocaleFormatter implements LocaleFormatter {
  readonly locale = 'en-US';

  formatNumber(n: number): string {
    // Ejemplo: 1234567 → "1,234,567"
    return new Intl.NumberFormat(this.locale).format(n);
  }

  formatDate(d: Date): string {
    // Ejemplo: "6/7/2026"
    return new Intl.DateTimeFormat(this.locale).format(d);
  }
}

/**
 * Fábrica Concreta — Inglés.
 *
 * Crea los productos específicos del idioma inglés:
 * su configuración de metadatos y su formateador de locale.
 */
export class EnglishLanguageFactory implements LanguageFactory {
  createConfig(): LanguageConfig {
    return {
      code: 'en',
      nativeName: 'English',
      translateKey: 'GAME_OPTIONS.ENGLISH',
      locale: 'en-US',
    };
  }

  createFormatter(): LocaleFormatter {
    return new EnglishLocaleFormatter();
  }
}
