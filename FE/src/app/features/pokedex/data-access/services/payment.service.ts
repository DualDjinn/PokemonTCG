import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PaymentItem {
  id: string;
  title: string;
  quantity: number;
  unitPrice: number;
}

export interface CreatePaymentRequest {
  playerId: string;
  items: PaymentItem[];
  coinsToAdd?: number;
}

export interface PaymentPreferenceResult {
  purchaseId: string;
  preferenceId: string;
  checkoutUrl: string;
}

export interface PurchaseStatusResponse {
  approved: boolean;
  rejected: boolean;
  coinsToAdd: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/payments';

  createCheckout(request: CreatePaymentRequest): Observable<PaymentPreferenceResult> {
    // Idempotency-Key: prevents creating duplicate preferences/purchases on double submit;
    // the backend forwards it to MercadoPago as X-Idempotency-Key.
    return this.http.post<PaymentPreferenceResult>(`${this.apiUrl}/create`, request, {
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      withCredentials: true,
    });
  }

  getPurchaseStatus(purchaseId: string): Observable<PurchaseStatusResponse> {
    return this.http.get<PurchaseStatusResponse>(`${this.apiUrl}/${purchaseId}/status`);
  }

  /**
   * Pull-based confirmation after the MercadoPago redirect: credits the coins for an approved
   * payment without depending on the webhook reaching the backend. Idempotent on the server.
   */
  confirmPayment(paymentId: string): Observable<PurchaseStatusResponse> {
    return this.http.post<PurchaseStatusResponse>(`${this.apiUrl}/confirm`, null, {
      params: { paymentId },
      withCredentials: true,
    });
  }

  redirectToCheckout(checkoutUrl: string): void {
    window.location.href = checkoutUrl;
  }
}
