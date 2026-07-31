import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Dorso de carta cosmético del catálogo. */
export interface CardBack {
  id: string;
  name: string;
  imageUrl: string;
  origin: 'DEFAULT' | 'SHOP' | 'PACK' | 'BATTLE' | 'LEVEL';
  price: number | null;
  unlockValue: number | null;
}

/** Dorsos que posee el jugador y cuál tiene equipado (null = clásico). */
export interface PlayerCardBacks {
  activeCardBackId: string | null;
  owned: CardBack[];
}

/** Skin full-art que posee el jugador. */
export interface PlayerSkin {
  skinId: string;
  skinName: string;
  baseCardId: string;
  baseCardName: string;
  origin: string;
  imageUrl: string;
  imageUrlLarge: string;
  quantity: number;
  equipped: boolean;
}

/**
 * Cosméticos del jugador: dorsos de carta (uno equipado globalmente) y
 * skins full-art (se equipan por carta, sobre su carta base).
 */
@Injectable({ providedIn: 'root' })
export class CosmeticsService {
  private readonly http = inject(HttpClient);

  /** Catálogo público de dorsos (sin autenticación). */
  getCardBackCatalog(): Observable<CardBack[]> {
    return this.http.get<CardBack[]>('/api/card-backs');
  }

  getPlayerCardBacks(): Observable<PlayerCardBacks> {
    return this.http.get<PlayerCardBacks>('/api/card-backs/me');
  }

  /** Compra un dorso con coins. Devuelve el estado actualizado del jugador. */
  purchaseCardBack(cardBackId: string): Observable<PlayerCardBacks> {
    // Idempotency-Key: a retry with the same key does not charge for the card back again.
    return this.http.post<PlayerCardBacks>('/api/card-backs/me/purchase', { cardBackId }, {
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      withCredentials: true,
    });
  }

  /** Equipa un dorso; null restaura el clásico. Devuelve el estado actualizado. */
  equipCardBack(cardBackId: string | null): Observable<PlayerCardBacks> {
    return this.http.put<PlayerCardBacks>('/api/card-backs/me/active', { cardBackId });
  }

  getPlayerSkins(): Observable<PlayerSkin[]> {
    return this.http.get<PlayerSkin[]>('/api/skins/me');
  }

  equipSkin(cardId: string, skinId: string): Observable<void> {
    return this.http.post<void>('/api/skins/me/equip', { cardId, skinId });
  }

  unequipSkin(cardId: string): Observable<void> {
    return this.http.post<void>('/api/skins/me/unequip', { cardId, skinId: null });
  }
}
