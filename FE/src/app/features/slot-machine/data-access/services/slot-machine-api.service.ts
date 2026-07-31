import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SlotSpinRequest {
  bet: number;
  winAmount: number;
}

export interface SlotSpinResponse {
  remainingCoins: number;
}

@Injectable({ providedIn: 'root' })
export class SlotMachineApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/slot-machine';

  spin(request: SlotSpinRequest): Observable<SlotSpinResponse> {
    return this.http.post<SlotSpinResponse>(`${this.apiUrl}/spin`, request);
  }
}
