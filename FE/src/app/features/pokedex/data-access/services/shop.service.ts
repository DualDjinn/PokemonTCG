import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BuyCartItem {
  cardId: string;
  quantity: number;
}

export interface BuyCartResponse {
  remainingCoins: number;
}

@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly http = inject(HttpClient);

  buyCards(items: BuyCartItem[]): Observable<BuyCartResponse> {
    return this.http.post<BuyCartResponse>('/api/shop/buy', { items }, this.idempotentOptions());
  }

  /** Compra splash arts (full-art) con coins. El servidor descuenta y persiste el saldo. */
  buySplashArts(quantity: number): Observable<BuyCartResponse> {
    return this.http.post<BuyCartResponse>('/api/shop/splash-arts/buy', { quantity }, this.idempotentOptions());
  }

  /**
   * HTTP options with a unique `Idempotency-Key` header per purchase, so the
   * backend deduplicates retries / double-submit and does not deduct coins twice.
   */
  private idempotentOptions() {
    return { headers: { 'Idempotency-Key': crypto.randomUUID() }, withCredentials: true };
  }
}
