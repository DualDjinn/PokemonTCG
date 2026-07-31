import { LanguageConfig } from './language-config.interface';
import { LocaleFormatter } from './locale-formatter.interface';

/**
 * Fábrica Abstracta — define el contrato para crear los productos de un idioma.
 *
 * Cada idioma soportado tiene su propia fábrica concreta que implementa
 * esta interfaz y es responsable de instanciar sus propios productos.
 *
 * Para agregar un nuevo idioma: crear una nueva clase que implemente
 * LanguageFactory y registrarla en LanguageFactoryRegistry.
 */
export interface LanguageFactory {
  /** Crea la configuración (metadatos) del idioma */
  createConfig(): LanguageConfig;

  /** Crea el formateador de fechas y números del idioma */
  createFormatter(): LocaleFormatter;
}
