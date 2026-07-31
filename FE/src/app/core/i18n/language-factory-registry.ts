import { AppLang, LanguageConfig } from './interfaces/language-config.interface';
import { LanguageFactory } from './interfaces/language-factory.interface';
import { SpanishLanguageFactory } from './factories/spanish.factory';
import { EnglishLanguageFactory } from './factories/english.factory';
import { ItalianLanguageFactory } from './factories/italian.factory';
import { PortugueseLanguageFactory } from './factories/portuguese.factory';

/**
 * Registro de fábricas de idioma.
 *
 * Actúa como punto de entrada único para obtener la fábrica concreta
 * correspondiente a cada código de idioma. Para agregar un idioma nuevo
 * solo hay que: crear su fábrica concreta y agregarla aquí.
 */
export class LanguageFactoryRegistry {
  /**
   * Mapa estático: AppLang → LanguageFactory concreta.
   * Se inicializa una sola vez cuando se carga el módulo.
   */
  private static readonly registry: Record<AppLang, LanguageFactory> = {
    es: new SpanishLanguageFactory(),
    en: new EnglishLanguageFactory(),
    it: new ItalianLanguageFactory(),
    pt: new PortugueseLanguageFactory(),
  };

  /** Devuelve la fábrica concreta para el idioma indicado */
  static getFactory(lang: AppLang): LanguageFactory {
    return this.registry[lang];
  }

  /** Devuelve la configuración de todos los idiomas registrados (para la UI) */
  static getAllConfigs(): LanguageConfig[] {
    return (Object.keys(this.registry) as AppLang[]).map(
      (lang) => this.registry[lang].createConfig()
    );
  }
}
