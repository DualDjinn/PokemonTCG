import { Injectable, signal, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  AppLang,
  LanguageConfig,
  LocaleFormatter,
  LanguageFactoryRegistry,
} from '../i18n';

// Clave usada para guardar el idioma elegido entre sesiones
const LANG_KEY = 'app_language';

// Re-exporta el tipo para que el resto de la app lo siga importando desde aquí
export type { AppLang };

/**
 * Servicio de idioma.
 *
 * Actúa como cliente del patrón Abstract Factory: delega en
 * LanguageFactoryRegistry la creación de los productos de cada idioma
 * (configuración y formateador) sin conocer las fábricas concretas.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);

  // Signal reactivo con el idioma activo
  readonly currentLang = signal<AppLang>(this.getSavedLang());

  /**
   * Cambia el idioma activo.
   * Obtiene la configuración a través de la fábrica, actualiza ngx-translate,
   * el signal reactivo y persiste la elección en localStorage.
   */
  setLanguage(lang: AppLang): void {
    const config = LanguageFactoryRegistry.getFactory(lang).createConfig();
    this.translate.use(config.code);
    this.currentLang.set(config.code);
    localStorage.setItem(LANG_KEY, config.code);
  }

  /**
   * Devuelve la lista de configuraciones de todos los idiomas registrados.
   * Usado por la UI para renderizar las opciones sin hardcodear strings.
   */
  getAvailableLanguages(): LanguageConfig[] {
    return LanguageFactoryRegistry.getAllConfigs();
  }

  /**
   * Devuelve el formateador de fechas/números del idioma activo.
   * Útil para componentes que formatean valores según el locale actual.
   */
  getCurrentFormatter(): LocaleFormatter {
    return LanguageFactoryRegistry.getFactory(this.currentLang()).createFormatter();
  }

  private getSavedLang(): AppLang {
    const saved = localStorage.getItem(LANG_KEY);
    const valid = LanguageFactoryRegistry.getAllConfigs().map((c) => c.code);
    return valid.includes(saved as AppLang) ? (saved as AppLang) : 'es';
  }
}
