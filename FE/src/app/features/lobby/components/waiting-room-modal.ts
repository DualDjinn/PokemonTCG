import { CUSTOM_ELEMENTS_SCHEMA, ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild, ElementRef, OnInit, OnDestroy, inject, signal, computed, effect } from '@angular/core';
import { DatePipe } from '@angular/common';
import { interval, of, Subscription, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MyButton } from '../../../shared/ui/my-button/my-button';
import { AudioSfxChannel } from '../../../core/services/audio-sfx-channel.service';
import { AudioMusicChannel } from '../../../core/services/audio-music-channel.service';
import { MatchLobbyService } from '../services/match-lobby.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ChatService, LobbyEvent } from '../services/chat.service';
import { getCharacterPortrait } from '../../../shared/utils/characters';

@Component({
  selector: 'app-waiting-room-modal',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [MyButton, DatePipe, TranslateModule],
  templateUrl: './waiting-room-modal.html',
  styleUrl: './waiting-room-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WaitingRoomModal implements OnInit, OnDestroy {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLElement>;

  @Input() matchId = '';
  @Input() joinCode = '';
  @Input() roomName = '';
  @Input() hostTrainerName = '';
  @Input() hostCharacter = '';
  @Input() set guestTrainerName(value: string | null) {
    this._guestTrainerName.set(value);
  }
  @Input() set guestCharacter(value: string | null) {
    this._guestCharacter.set(value);
  }
  @Input() set hostReady(value: boolean) {
    this._hostReady.set(value);
  }
  @Input() set guestReady(value: boolean) {
    this._guestReady.set(value);
  }
  @Input() isHost = true;
  @Input() currentUserId = '';
  @Input() currentTrainerName = '';

  @Output() close = new EventEmitter<string>();
  @Output() matchStarted = new EventEmitter<string>();
  @Output() countdownStarted = new EventEmitter<void>();
  @Output() countdownCancelled = new EventEmitter<void>();

  private readonly lobbyService = inject(MatchLobbyService);
  private readonly toastService = inject(ToastService);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly music = inject(AudioMusicChannel);
  private readonly chatService = inject(ChatService);
  private readonly translate = inject(TranslateService);

  readonly _guestTrainerName = signal<string | null>(null);
  readonly _guestCharacter = signal<string | null>(null);
  readonly _hostReady = signal(false);
  readonly _guestReady = signal(false);

  loading = signal(false);
  countdownPhase = signal<'idle' | 'cancelable' | 'preparing'>('idle');
  countdownSeconds = signal(0);
  error = signal('');
  chatOpen = signal(false);
  chatInput = signal('');
  showEmojiPicker = signal(false);

  private pollSub: Subscription | null = null;
  private previousGuestTrainerName: string | null = null;
  private previousStatus: string = 'WAITING';
  private closed = false;
  private started = false;
  private countdownTimer: ReturnType<typeof setInterval> | null = null;
  private countdownHostPhase: 'cancelable' | 'preparing' | null = null;

  readonly chatMessages = this.chatService.messages;
  readonly unreadCount = this.chatService.unreadCount;

  readonly portraitUrl = getCharacterPortrait;

  readonly opponentName = computed(() => {
    return this.isHost
      ? (this._guestTrainerName() ?? this.translate.instant('WAITING_ROOM.OPPONENT'))
      : this.hostTrainerName;
  });

  onOverlayClick(_event: MouseEvent): void {
  }

  onCancel(): void {
    this.sfx.play('Click.mp3');
    this.loading.set(true);
    this.lobbyService.cancelMatch(this.matchId).subscribe({
      next: () => {
        this.loading.set(false);
        this.close.emit('self_cancelled');
      },
      error: () => {
        this.loading.set(false);
        this.close.emit('self_cancelled');
      },
    });
  }

  onCancelCountdown(): void {
    this.sfx.play('Click.mp3');
    this.clearCountdownTimer();
    this.lobbyService.cancelCountdown(this.matchId).subscribe({
      next: () => {
        this.countdownPhase.set('idle');
        this.countdownSeconds.set(0);
        this.countdownHostPhase = null;
        this.countdownCancelled.emit();
      },
      error: () => {
        this.countdownPhase.set('idle');
        this.countdownSeconds.set(0);
        this.countdownHostPhase = null;
        this.countdownCancelled.emit();
      },
    });
  }

  onToggleReady(): void {
    this.sfx.play('Click.mp3');
    this.loading.set(true);
    this.error.set('');
    const currentReady = this.isHost ? this._hostReady() : this._guestReady();
    this.lobbyService.setReady(this.matchId, !currentReady).subscribe({
      next: room => {
        this._hostReady.set(room.hostReady);
        this._guestReady.set(room.guestReady);
        this._guestTrainerName.set(room.guestTrainerName);
        this._guestCharacter.set(room.guestCharacter);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('WAITING_ROOM.TOAST_READY_ERROR'));
        this.loading.set(false);
      },
    });
  }

  onStart(): void {
    this.sfx.play('Click.mp3');
    this.countdownHostPhase = 'cancelable';
    this.lobbyService.startCountdown(this.matchId, 'cancelable', 5).subscribe({
      next: () => {
        this.countdownPhase.set('cancelable');
        this.countdownSeconds.set(5);
        this.startLocalCountdown();
        this.countdownStarted.emit();
      },
      error: () => {
        this.countdownHostPhase = null;
        this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_START_ERROR'), 'error');
      },
    });
  }

  async onCopyCode(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.joinCode);
      this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_COPY_CODE'), 'success', 1500);
      this.sfx.play('Click.mp3');
    } catch {
      /* fallback silencioso */
    }
  }

  onChatInput(event: Event): void {
    this.chatInput.set((event.target as HTMLInputElement).value);
  }

  toggleChat(): void {
    const next = !this.chatOpen();
    this.chatOpen.set(next);
    this.chatService.setChatOpen(next);
    if (next) {
      this.showEmojiPicker.set(false);
      this.scrollToBottom();
    }
  }

  sendChatMessage(): void {
    const text = this.chatInput().trim();
    if (!text) return;
    const sent = this.chatService.sendMessage(text);
    if (!sent) {
      this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_CANT_SEND'), 'error');
      return;
    }
    this.chatInput.set('');
    this.showEmojiPicker.set(false);
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }

  onChatInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChatMessage();
    }
  }

  toggleEmojiPicker(): void {
    this.showEmojiPicker.update(v => !v);
  }

  onEmojiClick(event: Event): void {
    const customEvent = event as CustomEvent;
    const emoji = customEvent.detail?.emoji?.native || customEvent.detail?.unicode || '';
    if (emoji) {
      this.chatInput.update(v => v + emoji);
    }
    this.showEmojiPicker.set(false);
  }

  ngOnInit(): void {
    this.startPolling();
    this.chatService.connect(this.matchId, this.currentUserId, this.currentTrainerName);
  }

  ngOnDestroy(): void {
    this.clearCountdownTimer();
    this.stopPolling();
    this.chatService.disconnect();
  }

  private readonly lobbyEventListener = effect(() => {
    const event = this.chatService.lobbyEvent();
    if (!event || this.closed) return;
    this.processLobbyEvent(event);
  });

  private startLocalCountdown(): void {
    this.clearCountdownTimer();
    this.countdownTimer = setInterval(() => {
      this.countdownSeconds.update(s => {
        const next = s - 1;
        if (next <= 0) {
          this.onCountdownTickComplete();
          return 0;
        }
        return next;
      });
    }, 1000);
  }

  private clearCountdownTimer(): void {
    if (this.countdownTimer !== null) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
  }

  private onCountdownTickComplete(): void {
    this.clearCountdownTimer();
    const phase = this.countdownPhase();

    if (phase === 'cancelable' && this.isHost) {
      this.countdownHostPhase = 'preparing';
      this.lobbyService.startCountdown(this.matchId, 'preparing', 10).subscribe({
        next: () => {
          this.countdownPhase.set('preparing');
          this.countdownSeconds.set(10);
          this.music.play('ConteoInicialPartida.mp3');
          this.startLocalCountdown();
        },
        error: () => {
          this.countdownPhase.set('idle');
          this.countdownSeconds.set(0);
          this.countdownHostPhase = null;
        },
      });
    } else if (phase === 'preparing' && this.isHost) {
      this.countdownHostPhase = null;
      this.stopPolling();
      this.started = true;
      this.matchStarted.emit(this.matchId);
      this.lobbyService.startMatch(this.matchId).subscribe({
        error: () => {
          this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_START_ERROR'), 'error');
        },
      });
    }
  }

  private processLobbyEvent(event: LobbyEvent): void {
    switch (event.type) {
      case 'GUEST_JOINED': {
        const name = event.payload.guestTrainerName;
        if (name && !this.previousGuestTrainerName) {
          this.chatService.addSystemMessage(
            this.translate.instant('WAITING_ROOM.CHAT_GUEST_JOINED', { name })
          );
        }
        this._guestTrainerName.set(event.payload.guestTrainerName);
        this._guestCharacter.set(event.payload.guestCharacter);
        this.previousGuestTrainerName = event.payload.guestTrainerName;
        break;
      }
      case 'GUEST_LEFT': {
        if (this.previousGuestTrainerName) {
          this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_GUEST_LEFT'), 'info');
          this.chatService.addSystemMessage(
            this.translate.instant('WAITING_ROOM.CHAT_GUEST_LEFT', { name: this.previousGuestTrainerName })
          );
        }
        this._guestTrainerName.set(null);
        this._guestCharacter.set(null);
        this._hostReady.set(false);
        this.previousGuestTrainerName = null;
        this.clearCountdownTimer();
        this.countdownPhase.set('idle');
        this.countdownSeconds.set(0);
        this.countdownHostPhase = null;
        this.countdownCancelled.emit();
        break;
      }
      case 'READY_CHANGED': {
        this._hostReady.set(event.payload.hostReady);
        this._guestReady.set(event.payload.guestReady);
        break;
      }
      case 'COUNTDOWN_UPDATE': {
        const phase = (event.payload as any).phase;
        const totalSeconds = (event.payload as any).totalSeconds;
        this.countdownPhase.set(phase);
        this.countdownSeconds.set(totalSeconds);
        if (phase === 'preparing') {
          this.music.play('ConteoInicialPartida.mp3');
        }
        this.startLocalCountdown();
        break;
      }
      case 'COUNTDOWN_CANCELLED': {
        this.clearCountdownTimer();
        this.countdownPhase.set('idle');
        this.countdownSeconds.set(0);
        this.countdownHostPhase = null;
        this.countdownCancelled.emit();
        break;
      }
      case 'MATCH_STARTED': {
        if (!this.started) {
          this.countdownHostPhase = null;
          this.stopPolling();
          this.started = true;
          this.matchStarted.emit(this.matchId);
        }
        break;
      }
      case 'MATCH_CANCELLED': {
        if (!this.isHost && !this.closed) {
          this.closed = true;
          this.clearCountdownTimer();
          this.stopPolling();
          this.close.emit('opponent_cancelled');
        }
        break;
      }
    }
  }

  private startPolling(): void {
    this.pollSub = interval(5000)
      .pipe(
        switchMap(() => {
          if (!this.matchId) return of(null);
          return this.lobbyService.getLobbyState(this.matchId).pipe(
            catchError(err => {
              if (err.status === 404) {
                if (this.previousStatus === 'WAITING') {
                  return of(null);
                }
                return of({ __deleted: true } as any);
              }
              return of(null);
            }),
          );
        }),
      )
      .subscribe({
        next: room => {
          if (!room) return;

          if ((room as any).__deleted) {
            if (!this.isHost && !this.closed) {
              this.closed = true;
              this.close.emit('opponent_cancelled');
            }
            return;
          }

          this._hostReady.set(room.hostReady);
          this._guestReady.set(room.guestReady);
          this._guestTrainerName.set(room.guestTrainerName);
          this._guestCharacter.set(room.guestCharacter);
          this.previousGuestTrainerName = room.guestTrainerName;

          if (room.status !== this.previousStatus) {
            this.previousStatus = room.status;
            if (room.status !== 'WAITING' && !this.started) {
              this.started = true;
              this.toastService.show(this.translate.instant('WAITING_ROOM.TOAST_STATUS_CHANGED'), 'info');
              this.stopPolling();
              this.matchStarted.emit(this.matchId);
            }
          }
        },
      });
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }
}
