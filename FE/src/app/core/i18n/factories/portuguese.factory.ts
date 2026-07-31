import { LanguageFactory } from '../interfaces/language-factory.interface';
import { LanguageConfig } from '../interfaces/language-config.interface';
import { LocaleFormatter } from '../interfaces/locale-formatter.interface';

/**
 * Formateador concreto para el idioma portugués (locale pt-BR).
 * Usa la API Intl del navegador para formatear números y fechas.
 */
class PortugueseLocaleFormatter implements LocaleFormatter {
  readonly locale = 'pt-BR';

  formatNumber(n: number): string {
    // Ejemplo: 1234567 → "1.234.567"
    return new Intl.NumberFormat(this.locale).format(n);
  }

  formatDate(d: Date): string {
    // Ejemplo: "07/06/2026"
    return new Intl.DateTimeFormat(this.locale).format(d);
  }
}

/**
 * Fábrica Concreta — Portugués.
 *
 * Crea los productos específicos del idioma portugués:
 * su configuración de metadatos y su formateador de locale.
 */
export class PortugueseLanguageFactory implements LanguageFactory {
  createConfig(): LanguageConfig {
    return {
      code: 'pt',
      nativeName: 'Português',
      translateKey: 'GAME_OPTIONS.PORTUGUESE',
      locale: 'pt-BR',
    };
  }

  createFormatter(): LocaleFormatter {
    return new PortugueseLocaleFormatter();
  }
}
