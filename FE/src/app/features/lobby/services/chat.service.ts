import { Injectable, inject, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { TranslateService } from '@ngx-translate/core';
import { AudioSfxChannel } from '../../../core/services/audio-sfx-channel.service';

export interface ChatMessage {
  matchId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: string;
}

export interface LobbyEventPayload {
  status: string;
  hostReady: boolean;
  guestReady: boolean;
  guestTrainerName: string | null;
  guestCharacter: string | null;
  started: boolean;
}

export interface LobbyEvent {
  type: string;
  matchId: string;
  payload: LobbyEventPayload;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly sfx = inject(AudioSfxChannel);
  private readonly translate = inject(TranslateService);

  readonly messages = signal<ChatMessage[]>([]);
  readonly unreadCount = signal(0);
  readonly connected = signal(false);
  readonly error = signal<string | null>(null);
  readonly lobbyEvent = signal<LobbyEvent | null>(null);

  private stompClient: Client | null = null;
  private currentMatchId: string | null = null;
  private currentUserId: string | null = null;
  private currentTrainerName: string | null = null;
  private chatOpen = false;

  connect(matchId: string, userId: string, trainerName: string): void {
    this.disconnect();
    this.currentMatchId = matchId;
    this.currentUserId = userId;
    this.currentTrainerName = trainerName;
    this.messages.set([]);
    this.unreadCount.set(0);
    this.lobbyEvent.set(null);

    this.stompClient = new Client({
      webSocketFactory: () => new WebSocket('/ws'),
      reconnectDelay: 3000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
      onConnect: () => this.onConnected(),
      onWebSocketClose: () => this.connected.set(false),
      onStompError: () => this.connected.set(false),
    });

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.currentMatchId = null;
    this.currentUserId = null;
    this.currentTrainerName = null;
    this.messages.set([]);
    this.unreadCount.set(0);
    this.lobbyEvent.set(null);
    this.connected.set(false);
    this.error.set(null);
    this.chatOpen = false;
  }

  sendMessage(content: string): boolean {
    if (!this.stompClient?.connected || !this.currentMatchId || !this.currentUserId) {
      this.error.set(this.translate.instant('WAITING_ROOM.CHAT_NO_CONNECTION'));
      return false;
    }
    this.stompClient.publish({
      destination: `/app/chat/${this.currentMatchId}/send`,
      body: JSON.stringify({
        matchId: this.currentMatchId,
        senderId: this.currentUserId,
        senderName: this.currentTrainerName,
        content,
        timestamp: null,
      }),
    });
    return true;
  }

  addSystemMessage(content: string): void {
    this.messages.update(list => [...list, {
      matchId: this.currentMatchId ?? '',
      senderId: 'system',
      senderName: '',
      content,
      timestamp: new Date().toISOString(),
    }]);
  }

  setChatOpen(open: boolean): void {
    this.chatOpen = open;
    if (open) this.unreadCount.set(0);
  }

  private onConnected(): void {
    if (!this.currentMatchId || !this.stompClient) return;

    this.connected.set(true);
    this.error.set(null);

    this.stompClient.subscribe(
      `/topic/matches/${this.currentMatchId}/chat`,
      message => {
        try {
          const msg: ChatMessage = JSON.parse(message.body);
          this.messages.update(list => [...list, msg]);
          if (msg.senderId !== this.currentUserId) {
            this.sfx.play('Plop.mp3');
            if (!this.chatOpen) {
              this.unreadCount.update(c => c + 1);
            }
          }
        } catch {
          console.error('[ChatService] Error al deserializar mensaje:', message.body);
        }
      },
    );

    this.stompClient.subscribe(
      `/topic/matches/${this.currentMatchId}/lobby`,
      message => {
        try {
          const event: LobbyEvent = JSON.parse(message.body);
          console.log('[ChatService] Evento de sala:', event);
          this.lobbyEvent.set(event);
        } catch {
          console.error('[ChatService] Error al deserializar evento de sala:', message.body);
        }
      },
    );
  }
}
