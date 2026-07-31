import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Card, CardFilters } from '../../domain/models/card.model';

/**
 * Service responsible for fetching Pokémon TCG card data from the backend API.
 * All calls target the `/cards` endpoint exposed by the Spring Boot server.
 */
@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/cards';

  /**
   * Fetches cards from the backend, optionally applying the given filters.
   * Each defined filter property maps to the matching query parameter.
   *
   * @param filters Optional object with name, supertype, type, rarity and/or subtype
   * @returns Observable that emits the list of matching cards
   */
  getCards(filters?: CardFilters): Observable<Card[]> {
    let params = new HttpParams();

    if (filters?.name) {
      params = params.set('name', filters.name);
    }
    if (filters?.supertype) {
      params = params.set('supertype', filters.supertype);
    }
    if (filters?.type) {
      params = params.set('type', filters.type);
    }
    if (filters?.rarity) {
      params = params.set('rarity', filters.rarity);
    }
    if (filters?.subtype) {
      params = params.set('subtype', filters.subtype);
    }

    return this.http.get<Card[]>(this.baseUrl, { params });
  }

  /**
   * Fetches a single card by its unique identifier (e.g., "xy1-1").
   *
   * @param id The card identifier
   * @returns Observable that emits the card, or throws a 404 error if not found
   */
  getCardById(id: string): Observable<Card> {
    return this.http.get<Card>(`${this.baseUrl}/${id}`);
  }
}
