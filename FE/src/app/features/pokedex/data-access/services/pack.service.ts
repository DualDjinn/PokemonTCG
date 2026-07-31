import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OpenedPack, PackInfo, PackType } from '../../domain/models/pack.model';

@Injectable({ providedIn: 'root' })
export class PackService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/packs';

  /** Catálogo público de sobres (no requiere auth). */
  listPacks(): Observable<PackInfo[]> {
    return this.http.get<PackInfo[]>(this.apiUrl);
  }

  /**
   * Compra y abre un sobre. La cookie de sesión va automáticamente con
   * `withCredentials` (configurado en el HTTP interceptor del proyecto).
   *
   * Errores esperados:
   *  - 400 → tipo inválido
   *  - 402 → monedas insuficientes
   *  - 409 → catálogo vacío u otro estado inválido
   */
  openPack(packType: PackType): Observable<OpenedPack> {
    // Idempotency-Key: a retry / double-click with the same key returns the
    // same pack (same cards) without charging again.
    return this.http.post<OpenedPack>(`${this.apiUrl}/open`, { packType }, {
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      withCredentials: true,
    });
  }
}
