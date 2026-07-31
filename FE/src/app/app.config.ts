import {
  ApplicationConfig,
  importProvidersFrom,
  provideAppInitializer,
  inject,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { sessionInterceptor } from './core/interceptors/session.interceptor';

const LANG_KEY = 'app_language';
const SUPPORTED_LANGS = ['es', 'en', 'it', 'pt'];

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([sessionInterceptor])),
    importProvidersFrom(
      TranslateModule.forRoot({})
    ),
    ...provideTranslateHttpLoader({ prefix: 'assets/i18n/', suffix: '.json' }),
    provideAppInitializer(() => {
      const translate = inject(TranslateService);
      const saved = localStorage.getItem(LANG_KEY);
      const lang = saved !== null && SUPPORTED_LANGS.includes(saved) ? saved : 'es';
      translate.addLangs(SUPPORTED_LANGS);
      translate.setDefaultLang('es');
      return firstValueFrom(translate.use(lang));
    }),
  ],
};
