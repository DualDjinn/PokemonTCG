import { Injectable, inject, signal, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { PlayerResponse } from '../../domain/models/player-response.model';

const USER_STORAGE_KEY = 'currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly apiUrl = '/api/players';

  currentUser = signal<PlayerResponse | null>(this.restoreUser());

  constructor() {
    // Persiste cualquier cambio a currentUser en localStorage automáticamente,
    // incluyendo actualizaciones directas de coins desde PackShop y CartDrawer.
    effect(() => {
      const user = this.currentUser();
      try {
        if (user) {
          localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
        } else {
          localStorage.removeItem(USER_STORAGE_KEY);
        }
      } catch {}
    });
  }

  private loginActions: ((r: PlayerResponse) => void)[] = [];

  login(username: string, password: string): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(`${this.apiUrl}/login`, { username, password }).pipe(
      tap(response => this.setUser(response))
    );
  }

  register(username: string, email: string, password: string): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(`${this.apiUrl}/register`, {
      username,
      email,
      password,
      trainerName: username,
      dateOfBirth: null
    }).pipe(
      tap(response => this.setUser(response))
    );
  }

  getCurrentPlayer(): Observable<PlayerResponse> {
    return this.http.get<PlayerResponse>(`${this.apiUrl}/me`).pipe(
      tap(response => this.setUser(response))
    );
  }

  updateTrainerName(trainerName: string): Observable<PlayerResponse> {
    return this.http.put<PlayerResponse>(`${this.apiUrl}/trainer-name`, { trainerName }).pipe(
      tap(response => this.setUser(response))
    );
  }

  updateCharacter(character: string, color: string | null): Observable<PlayerResponse> {
    return this.http.put<PlayerResponse>(`${this.apiUrl}/character`, { character, color }).pipe(
      tap(response => this.setUser(response))
    );
  }

  setActiveDeck(deckId: string | null): Observable<PlayerResponse> {
    return this.http.put<PlayerResponse>(`${this.apiUrl}/me/active-deck`, { deckId }).pipe(
      tap(response => this.setUser(response))
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
      complete: () => {
        this.clearUser();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.clearUser();
        this.router.navigate(['/login']);
      }
    });
  }

  googleLogin(credential: string): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(`${this.apiUrl}/google-login`, { credential }).pipe(
      tap(response => this.setUser(response))
    );
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  /**
   * Solicita el envío de un email de cambio de contraseña al jugador autenticado.
   * El backend toma el email desde la sesión y reutiliza el flujo de reset-password.
   */
  requestPasswordChange(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/me/request-password-change`, {});
  }

  isEmailValid(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  private setUser(response: PlayerResponse): void {
    this.currentUser.set(response);
    try {
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response));
    } catch {}
  }

  private restoreUser(): PlayerResponse | null {
    try {
      const stored = localStorage.getItem(USER_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as PlayerResponse;
        if (parsed?.id && parsed?.username) {
          return parsed;
        }
      }
    } catch {}
    return null;
  }

  private clearUser(): void {
    this.currentUser.set(null);
    try {
      localStorage.removeItem(USER_STORAGE_KEY);
    } catch {}
  }
}
