import { Injectable, NgZone, inject, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BackendMatchStateDTO, BackendMulliganEventDTO, GameEvent, GameStateService } from './game-state.service';

export interface GameEventEnvelope {
  type: 'COIN_FLIP' | 'STATUS_APPLIED' | 'TRAINER_EFFECT' | string;
  payload: Record<string, unknown>;
}

export enum ConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly gameStateService = inject(GameStateService);
  private readonly ngZone = inject(NgZone);

  readonly connectionStatus = signal<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  readonly lastGameEvent = signal<GameEventEnvelope | null>(null);

  private stompClient: Client | null = null;
  private currentMatchId: string | null = null;
  private currentPlayerId: string | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private isActiveMatch = false;
  private intentionalClose = false;
  private pendingMessages: Array<{ destination: string; body: unknown }> = [];

  connect(matchId: string, playerId: string): void {
    this.intentionalClose = false;
    this.cleanup();

    this.currentMatchId = matchId;
    this.currentPlayerId = playerId;
    this.isActiveMatch = true;
    this.connectionStatus.set(ConnectionStatus.CONNECTING);

    this.stompClient = new Client({
      webSocketFactory: () => new WebSocket('/ws'),
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => this.onConnected(),
      onWebSocketClose: () => this.onWebSocketClose(),
      onStompError: () => this.onError(),
    });

    this.stompClient.activate();
  }

  /**
   * Publica un mensaje STOMP hacia el destino indicado.
   * Usado por los componentes de juego para enviar acciones al backend.
   * TODO: mover la lógica de publicación a TurnActionService cuando FE-14 esté implementado.
   *
   * @param destination destino STOMP (ej. `/app/matches/{matchId}/setup`)
   * @param body        objeto a serializar como JSON en el cuerpo del mensaje
   */
  publish(destination: string, body: unknown): void {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body),
      });
      return;
    }
    if (this.currentMatchId) {
      this.pendingMessages.push({ destination, body });
    }
  }

  disconnect(): void {
    this.isActiveMatch = false;
    this.currentMatchId = null;
    this.currentPlayerId = null;
    this.cleanup();
    this.connectionStatus.set(ConnectionStatus.DISCONNECTED);
  }

  private onConnected(): void {
    this.connectionStatus.set(ConnectionStatus.CONNECTED);

    if (!this.currentMatchId || !this.currentPlayerId || !this.stompClient) {
      return;
    }

    this.stompClient.subscribe(
      `/topic/matches/${this.currentMatchId}/state/${this.currentPlayerId}`,
      (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body);
          console.log('[WebSocket] Estado recibido por topic:', parsed);
          if (parsed.type === 'MATCH_FINISHED') {
            const winnerId = parsed.payload?.winnerId;
            this.gameStateService.matchState.update(s => s ? { ...s, winnerId, phase: 'FINISHED' } : s);
            return;
          }
          if (parsed.type === 'OPPONENT_HOVER') {
            this.gameStateService.applyEvent(parsed as GameEvent);
            return;
          }
          if (parsed.type === 'OPPONENT_UNHOVER') {
            this.gameStateService.applyEvent(parsed as GameEvent);
            return;
          }
          if (parsed.type === 'ACTION_ERROR') {
            this.gameStateService.actionError.set(parsed.payload?.message ?? 'Error desconocido');
            return;
          }
          if (parsed.type === 'OPPONENT_DISCONNECTED') {
            this.ngZone.run(() => {
              this.gameStateService.opponentDisconnected.set({
                secondsRemaining: parsed.payload?.secondsRemaining ?? 120,
                disconnectedPlayerId: parsed.payload?.disconnectedPlayerId ?? '',
              });
            });
            return;
          }
          if (parsed.type === 'DISCONNECT_TICK') {
            this.ngZone.run(() => {
              this.gameStateService.opponentDisconnected.update(d =>
                d ? { ...d, secondsRemaining: parsed.payload?.secondsRemaining ?? 0 } : d);
            });
            return;
          }
          if (parsed.type === 'OPPONENT_FORFEITED') {
            this.ngZone.run(() => {
              const winnerId = parsed.payload?.winnerId;
              this.gameStateService.matchState.update(s => s ? { ...s, winnerId, phase: 'FINISHED' } : s);
              this.gameStateService.opponentDisconnected.set(null);
            });
            return;
          }
          if (parsed.type === 'OPPONENT_RECONNECTED') {
            this.ngZone.run(() => {
              this.gameStateService.opponentDisconnected.set(null);
            });
            return;
          }
          this.gameStateService.processRawState(parsed as BackendMatchStateDTO, this.currentMatchId ?? '');
        } catch {
          console.error('Error al deserializar evento de juego:', message.body);
        }
      },
    );

    this.stompClient.subscribe(
      '/user/queue/match',
      (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body);
          console.log('[WebSocket] Estado recibido por user queue:', parsed);
          this.gameStateService.processRawState(parsed as BackendMatchStateDTO, this.currentMatchId ?? '');
        } catch {
          console.error('Error al deserializar evento de juego:', message.body);
        }
      },
    );

    this.stompClient.subscribe(
      `/topic/matches/${this.currentMatchId}/mulligan`,
      (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body);
          console.log('[WebSocket] Mulligan event:', parsed);
          this.gameStateService.processMulliganEvent(parsed as BackendMulliganEventDTO);
        } catch {
          console.error('Error al deserializar evento de mulligan:', message.body);
        }
      },
    );

    this.stompClient.subscribe(
      `/topic/matches/${this.currentMatchId}/events/${this.currentPlayerId}`,
      (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body) as GameEventEnvelope;
          console.log('[WebSocket] Game event:', parsed);
          this.lastGameEvent.set(parsed);
        } catch {
          console.error('Error al deserializar game event:', message.body);
        }
      },
    );

    for (const msg of this.pendingMessages) {
      this.stompClient.publish({
        destination: msg.destination,
        body: JSON.stringify(msg.body),
      });
    }
    this.pendingMessages = [];
  }

  private onWebSocketClose(): void {
    if (this.intentionalClose) {
      this.intentionalClose = false;
      return;
    }

    if (this.isActiveMatch && this.currentMatchId && this.currentPlayerId) {
      this.connectionStatus.set(ConnectionStatus.CONNECTING);
      this.reconnectTimeout = setTimeout(() => {
        if (this.isActiveMatch && this.currentMatchId && this.currentPlayerId) {
          this.connect(this.currentMatchId, this.currentPlayerId);
        }
      }, 3000);
    } else {
      this.connectionStatus.set(ConnectionStatus.DISCONNECTED);
    }
  }

  private onError(): void {
    this.connectionStatus.set(ConnectionStatus.ERROR);
  }

  private cancelReconnect(): void {
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private cleanup(): void {
    this.cancelReconnect();
    this.intentionalClose = true;
    this.pendingMessages = [];

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}
