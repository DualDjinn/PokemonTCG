import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LobbyRoom {
  matchId: string;
  joinCode: string;
  status: string;
  roomName: string | null;
  description: string | null;
  isPrivate: boolean;
  showInList: boolean;
  hasPassword: boolean;
  allowSpectators: boolean;
  started: boolean;
  hostReady: boolean;
  guestReady: boolean;
  hostTrainerName: string;
  hostCharacter: string;
  guestTrainerName: string | null;
  guestCharacter: string | null;
}

export interface CreateRoomRequest {
  deckId: string;
  roomName: string;
  description: string;
  isPrivate: boolean;
  showInList: boolean;
  password: string;
  allowSpectators: boolean;
  isBotMatch?: boolean;
  botDifficulty?: BotDifficulty;
}

export type BotDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface JoinByCodeRequest {
  code: string;
  deckId: string;
  password?: string;
}

export interface JoinMatchRequest {
  deckId: string;
  password?: string;
}

export interface SetReadyRequest {
  ready: boolean;
}

@Injectable({ providedIn: 'root' })
export class MatchLobbyService {
  private readonly http = inject(HttpClient);

  getRooms(): Observable<LobbyRoom[]> {
    return this.http.get<LobbyRoom[]>('/api/matches/lobby');
  }

  createRoom(request: CreateRoomRequest): Observable<LobbyRoom> {
    return this.http.post<LobbyRoom>('/api/matches', request);
  }

  /**
   * Crea una partida contra un bot. El backend resuelve el bot/deck según la
   * dificultad y deja el match en estado SETUP listo para jugar.
   */
  createBotMatch(deckId: string, difficulty: BotDifficulty): Observable<LobbyRoom> {
    const body: CreateRoomRequest = {
      deckId,
      roomName: `Bot Match (${difficulty})`,
      description: '',
      isPrivate: true,
      showInList: false,
      password: '',
      allowSpectators: false,
      isBotMatch: true,
      botDifficulty: difficulty,
    };
    return this.http.post<LobbyRoom>('/api/matches', body);
  }

  joinByCode(code: string, deckId: string, password?: string): Observable<LobbyRoom> {
    const body: JoinByCodeRequest = { code, deckId };
    if (password) body.password = password;
    return this.http.post<LobbyRoom>('/api/matches/join-by-code', body);
  }

  setReady(matchId: string, ready: boolean): Observable<LobbyRoom> {
    return this.http.post<LobbyRoom>(`/api/matches/${matchId}/ready`, { ready } satisfies SetReadyRequest);
  }

  joinMatch(matchId: string, deckId: string, password?: string): Observable<LobbyRoom> {
    const body: JoinMatchRequest = { deckId };
    if (password) body.password = password;
    return this.http.post<LobbyRoom>(`/api/matches/${matchId}/join`, body);
  }

  startMatch(matchId: string): Observable<LobbyRoom> {
    return this.http.post<LobbyRoom>(`/api/matches/${matchId}/start`, {});
  }

  getLobbyState(matchId: string): Observable<LobbyRoom> {
    return this.http.get<LobbyRoom>(`/api/matches/${matchId}/lobby-state`);
  }

  cancelMatch(matchId: string): Observable<void> {
    return this.http.post<void>(`/api/matches/${matchId}/cancel`, {});
  }

  startCountdown(matchId: string, phase: string, totalSeconds: number): Observable<void> {
    return this.http.post<void>(`/api/matches/${matchId}/start-countdown`, { phase, totalSeconds });
  }

  cancelCountdown(matchId: string): Observable<void> {
    return this.http.post<void>(`/api/matches/${matchId}/cancel-countdown`, {});
  }

  getPendingMatches(): Observable<LobbyRoom[]> {
    return this.http.get<LobbyRoom[]>('/api/matches/pending-matches');
  }

  surrenderMatch(matchId: string): Observable<void> {
    return this.http.post<void>(`/api/matches/${matchId}/surrender`, {});
  }
}
