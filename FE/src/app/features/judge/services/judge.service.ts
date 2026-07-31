import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { JudgeQueryParams, JudgeResponse } from '../models/judge.types';

@Injectable({ providedIn: 'root' })
export class JudgeService {
  private readonly http = inject(HttpClient);

  query(matchId: string, params: JudgeQueryParams): Observable<JudgeResponse> {
    let p = new HttpParams().set('category', params.category);
    if (params.targetAction) p = p.set('targetAction', params.targetAction);
    if (params.targetCardId) p = p.set('targetCardId', params.targetCardId);
    if (params.targetPokemonId) p = p.set('targetPokemonId', params.targetPokemonId);
    if (params.targetTurnNumber !== undefined) p = p.set('targetTurnNumber', String(params.targetTurnNumber));
    if (params.lang) p = p.set('lang', params.lang);

    return this.http.get<JudgeResponse>(
      `/api/matches/${matchId}/judge/query`,
      { params: p, withCredentials: true },
    );
  }
}
