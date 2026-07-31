import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  DeckListItem,
  DeckDetail,
  CreateDeckRequest,
  UpdateDeckRequest,
  DeckApiDTO,
  CardSummary,
  CardSummaryApiDTO,
} from '../../domain/models/deck.models';

@Injectable({ providedIn: 'root' })
export class DeckService {
  private readonly http = inject(HttpClient);

  getDecks(): Observable<{ items: DeckListItem[]; details: DeckDetail[] }> {
    return this.http.get<DeckApiDTO[]>('/api/players/me/decks').pipe(
      map(decks => {
        const details: DeckDetail[] = [];
        const items = decks.map(dto => {
          const detail = this.normalizeDeckDetail(dto);
          details.push(detail);
          return this.deckDetailToListItem(detail);
        });
        return { items, details };
      })
    );
  }

  private deckDetailToListItem(detail: DeckDetail): DeckListItem {
    return {
      id: detail.id,
      name: detail.name,
      cardCount: detail.cardCount,
      valid: detail.valid,
      favorite: detail.favorite,
      validationMessage: detail.validationMessage,
    };
  }

  getDeck(id: string): Observable<DeckDetail> {
    return this.http.get<DeckApiDTO>(`/api/decks/${id}`).pipe(
      map(dto => this.normalizeDeckDetail(dto))
    );
  }

  createDeck(name: string): Observable<DeckListItem> {
    const body: CreateDeckRequest = { name };
    return this.http.post<DeckApiDTO>('/api/decks', body).pipe(
      map(dto => this.normalizeDeckListItem(dto))
    );
  }

  updateDeck(id: string, cardIds: string[]): Observable<void> {
    const body: UpdateDeckRequest = { cardIds };
    return this.http.put<void>(`/api/decks/${id}`, body);
  }

  deleteDeck(id: string): Observable<void> {
    return this.http.delete<void>(`/api/decks/${id}`);
  }

  renameDeck(id: string, name: string): Observable<DeckListItem> {
    return this.http.patch<DeckApiDTO>(`/api/decks/${id}/name`, { name }).pipe(
      map(dto => this.normalizeDeckListItem(dto))
    );
  }

  toggleFavorite(id: string): Observable<DeckListItem> {
    return this.http.put<DeckApiDTO>(`/api/decks/${id}/favorite`, {}).pipe(
      map(dto => this.normalizeDeckListItem(dto))
    );
  }

  private normalizeDeckListItem(dto: DeckApiDTO): DeckListItem {
    const cardCount = dto.cardCount ?? 0;
    const errors = dto.errors ?? [];
    const validationMessage = cardCount === 0 ? null : (errors[0] ?? dto.message ?? dto.error ?? null);

    return {
      id: dto.id ?? crypto.randomUUID(),
      name: dto.name ?? 'Mazo sin nombre',
      cardCount,
      valid: dto.valid ?? dto.isValid ?? false,
      favorite: dto.favorite ?? false,
      validationMessage,
    };
  }

  normalizeDeckDetail(dto: DeckApiDTO): DeckDetail {
    const cardCount = dto.cardCount ?? 0;
    const errors = dto.errors ?? [];
    const validationMessage = cardCount === 0 ? null : (errors[0] ?? dto.message ?? dto.error ?? null);

    return {
      id: dto.id ?? crypto.randomUUID(),
      name: dto.name ?? 'Mazo sin nombre',
      cardCount,
      cards: (dto.cards ?? []).map(c => this.normalizeCardSummary(c)),
      valid: dto.valid ?? dto.isValid ?? false,
      favorite: dto.favorite ?? false,
      validationMessage,
    };
  }

  private normalizeCardSummary(card: CardSummaryApiDTO): CardSummary {
    return {
      id: card.id ?? crypto.randomUUID(),
      name: card.name ?? 'Carta sin nombre',
      supertype: this.normalizeSupertype(card.supertype),
      imageUrl: card.imageUrl ?? card.images?.small ?? card.images?.large ?? '',
      imageUrlLarge: card.images?.large ?? card.imageUrl ?? undefined,
      rarity: card.rarity ?? '',
    };
  }

  private normalizeSupertype(supertype: string | undefined): string {
    if (!supertype) return 'Pokemon';
    const upper = supertype.toUpperCase();
    if (upper === 'POKEMON') return 'Pokemon';
    if (upper === 'TRAINER') return 'Trainer';
    if (upper === 'ENERGY') return 'Energy';
    return 'Pokemon';
  }
}
