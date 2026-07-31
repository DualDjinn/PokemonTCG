import { isObservable, firstValueFrom } from 'rxjs';
import { routes } from './app.routes';

describe('app routes', () => {
  it('should register the protected profile route as a lazy-loaded component', async () => {
    const profileRoute = routes.find(route => route.path === 'profile');

    expect(profileRoute).toBeDefined();
    expect(profileRoute?.canActivate?.length).toBe(1);
    expect(profileRoute?.loadComponent).toBeDefined();

    const loadedComponent = await profileRoute?.loadComponent?.();
    expect(loadedComponent).toBeDefined();
  });

  it('should lazy load the login, character creation, and game routes', async () => {
    const loginRoute = routes.find(route => route.path === 'login');
    const characterRoute = routes.find(route => route.path === 'crear-personaje');
    const gameRoute = routes.find(route => route.path === 'juego/:location');

    await expectAsync(resolveLoader(loginRoute?.loadComponent?.())).toBeResolved();
    await expectAsync(resolveLoader(characterRoute?.loadComponent?.())).toBeResolved();
    await expectAsync(resolveLoader(gameRoute?.loadComponent?.())).toBeResolved();
  });

  it('should keep the expected redirect routes', () => {
    const rootRedirect = routes.find(route => route.path === '');
    const wildcard = routes.find(route => route.path === '**');

    expect(rootRedirect?.redirectTo).toBe('login');
    expect(rootRedirect?.pathMatch).toBe('full');
    // El comodín ahora carga una página 404 dedicada en vez de redirigir a login.
    expect(wildcard?.loadComponent).toBeDefined();
    expect(wildcard?.redirectTo).toBeUndefined();
  });

  function resolveLoader(
    value: ReturnType<NonNullable<(typeof routes)[number]['loadComponent']>> | undefined,
  ): Promise<unknown> {
    if (isObservable(value)) {
      return firstValueFrom(value);
    }

    return Promise.resolve(value);
  }
});