import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { NpcRewardClaimResponse } from '../../domain/models/npc-reward.model';

/**
 * Sidequests de NPCs: reclamo de cartas secretas contra el backend.
 *
 * Mantiene en memoria el set de NPCs ya reclamados para decidir al instante
 * (sin round-trip) si corresponde reproducir la animación de descubrimiento.
 * El backend valida igual: el reclamo es idempotente del lado servidor.
 */
@Injectable({ providedIn: 'root' })
export class QuestService {
  private http = inject(HttpClient);

  private readonly apiUrl = '/api/quests';

  /** NPCs cuya recompensa ya reclamó el jugador actual. */
  readonly claimedNpcIds = signal<ReadonlySet<string>>(new Set());

  /** Precarga el set de reclamados; llamar al entrar a la página del juego. */
  loadClaimedRewards(): void {
    this.http.get<string[]>(`${this.apiUrl}/npc/claimed`).subscribe({
      next: ids => this.claimedNpcIds.set(new Set(ids)),
      // Sin sesión o backend caído: dejamos el set vacío, el POST de
      // reclamo decide igual del lado servidor.
      error: () => {},
    });
  }

  isClaimed(npcId: string): boolean {
    return this.claimedNpcIds().has(npcId);
  }

  claimNpcReward(npcId: string): Observable<NpcRewardClaimResponse> {
    return this.http
      .post<NpcRewardClaimResponse>(`${this.apiUrl}/npc/${encodeURIComponent(npcId)}/claim`, {})
      .pipe(tap(() => this.claimedNpcIds.update(set => new Set(set).add(npcId))));
  }
}
