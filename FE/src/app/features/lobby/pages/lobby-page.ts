import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, AfterViewInit, inject, signal, computed, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { interval, of, Subscription, switchMap } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MyText } from '../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../shared/ui/my-button/my-button';
import { MyInput } from '../../../shared/ui/my-input/my-input';
import { MySelect, MySelectOption } from '../../../shared/ui/my-select/my-select';
import { AuthService } from '../../game/data-access/services/auth.service';
import { GameStateService } from '../../game/data-access/services/game-state.service';
import { MatchLobbyService, LobbyRoom } from '../services/match-lobby.service';
import { ToastService } from '../../../shared/services/toast.service';
import { CircleTransitionService } from '../../../shared/services/circle-transition.service';
import { getCharacterPortrait } from '../../../shared/utils/characters';
import { AudioSfxChannel } from '../../../core/services/audio-sfx-channel.service';
import { AudioMusicChannel } from '../../../core/services/audio-music-channel.service';
import { LanguageService } from '../../../core/services/language.service';
import { CreateRoomModal } from '../components/create-room-modal';
import { WaitingRoomModal } from '../components/waiting-room-modal';
import { MyModal } from '../../../shared/components/my-modal/my-modal';
import { PixelPanelDirective } from '../../../shared/directives';
import { MyPageBackground } from '../../../shared/components/my-page-background/my-page-background';
import { PokenavDeviceComponent } from '../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../game/components/game-options/game-options';

@Component({
  selector: 'app-lobby-page',
  standalone: true,
  imports: [
    TranslateModule,
    MyModal, MyText, MyButton, MyInput, MySelect, MyPageBackground,
    CreateRoomModal, WaitingRoomModal,
    PixelPanelDirective, PokenavDeviceComponent, GameOptionsComponent,
  ],
  templateUrl: './lobby-page.html',
  styleUrl: './lobby-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LobbyPage implements OnInit, OnDestroy, AfterViewInit {
  private readonly ACTIVE_MATCH_KEY = 'activeMatchId';
  private readonly ACTIVE_MATCH_HOST_KEY = 'activeMatchHost';

  private readonly lobbyService = inject(MatchLobbyService);
  private readonly authService = inject(AuthService);
  private readonly gameStateService = inject(GameStateService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly audioMusic = inject(AudioMusicChannel);
  private readonly circleTransition = inject(CircleTransitionService);
  private readonly translate = inject(TranslateService);
  private readonly langService = inject(LanguageService);

  rooms = signal<LobbyRoom[]>([]);
  loading = signal(true);
  showCreateModal = signal(false);
  joinCodeInput = '';

  // Code password modal
  showCodePasswordModal = signal(false);
  pendingCode = '';
  codePassword = '';

  // Room password modal
  showRoomPasswordModal = signal(false);
  pendingRoomId = '';
  pendingRoomPassword = '';

  // Waiting room state (shown as overlay)
  showWaitingRoom = signal(false);
  waitingMatchId = signal('');
  waitingJoinCode = signal('');
  waitingRoomName = signal('');
  waitingHostTrainerName = signal('');
  waitingHostCharacter = signal('');
  waitingGuestTrainerName = signal<string | null>(null);
  waitingGuestCharacter = signal<string | null>(null);
  waitingHostReady = signal(false);
  waitingGuestReady = signal(false);
  waitingIsHost = signal(true);

  readonly showMenu = signal(false);
  readonly showOptions = signal(false);
  readonly showPendingMatchModal = signal(false);
  readonly pendingMatch = signal<LobbyRoom | null>(null);

  private pollSub: Subscription | null = null;

  // Filtros
  searchText = signal('');
  filterType = signal<'all' | 'public' | 'private'>('all');
  filterStatus = signal<'all' | 'waiting' | 'playing'>('all');
  filterAvailability = signal<'all' | 'available' | 'full'>('all');

  readonly filterTypeOptions = computed((): MySelectOption[] => {
    this.langService.currentLang();
    return [
      { label: this.translate.instant('LOBBY.FILTER_ALL'), value: 'all' },
      { label: this.translate.instant('LOBBY.FILTER_PUBLIC'), value: 'public' },
      { label: this.translate.instant('LOBBY.FILTER_PRIVATE'), value: 'private' },
    ];
  });

  readonly filterStatusOptions = computed((): MySelectOption[] => {
    this.langService.currentLang();
    return [
      { label: this.translate.instant('LOBBY.FILTER_ALL_STATUS'), value: 'all' },
      { label: this.translate.instant('LOBBY.FILTER_WAITING'), value: 'waiting' },
      { label: this.translate.instant('LOBBY.FILTER_PLAYING'), value: 'playing' },
    ];
  });

  readonly filterAvailabilityOptions = computed((): MySelectOption[] => {
    this.langService.currentLang();
    return [
      { label: this.translate.instant('LOBBY.FILTER_ANY_SPACE'), value: 'all' },
      { label: this.translate.instant('LOBBY.FILTER_AVAILABLE'), value: 'available' },
      { label: this.translate.instant('LOBBY.FILTER_FULL'), value: 'full' },
    ];
  });

  readonly filteredRooms = computed(() => {
    let list = this.rooms();

    const text = this.searchText().toLowerCase();
    if (text) {
      list = list.filter(r =>
        r.roomName?.toLowerCase().includes(text) ||
        r.hostTrainerName?.toLowerCase().includes(text) ||
        r.description?.toLowerCase().includes(text)
      );
    }

    const type = this.filterType();
    if (type === 'public') list = list.filter(r => !r.isPrivate);
    else if (type === 'private') list = list.filter(r => r.isPrivate);

    const status = this.filterStatus();
    if (status === 'waiting') list = list.filter(r => r.status === 'WAITING');
    else if (status === 'playing') list = list.filter(r => r.status !== 'WAITING');

    const avail = this.filterAvailability();
    if (avail === 'available') list = list.filter(r => !r.guestTrainerName);
    else if (avail === 'full') list = list.filter(r => r.guestTrainerName);

    return list;
  });

  onFilterTypeChange(value: string): void {
    this.filterType.set(value as 'all' | 'public' | 'private');
  }

  onFilterStatusChange(value: string): void {
    this.filterStatus.set(value as 'all' | 'waiting' | 'playing');
  }

  onFilterAvailabilityChange(value: string): void {
    this.filterAvailability.set(value as 'all' | 'available' | 'full');
  }

  // Player info
  readonly navigating = signal(false);
  readonly currentUserId = computed(() => this.authService.currentUser()?.username ?? '');
  readonly currentTrainerName = computed(() => this.authService.currentUser()?.trainerName ?? '');
  readonly playerTrainerName = computed(() => this.authService.currentUser()?.trainerName ?? '');
  readonly playerLevel = computed(() => this.authService.currentUser()?.level ?? 1);
  readonly playerExp = computed(() => this.authService.currentUser()?.xpIntoCurrentLevel ?? 0);
  readonly playerExpForNext = computed(() => this.authService.currentUser()?.xpForNextLevel ?? 1);
  readonly expPercent = computed(() => {
    const total = this.playerExpForNext();
    return total > 0 ? (this.playerExp() / total) * 100 : 0;
  });

  readonly activeDeckName = computed(() => {
    this.langService.currentLang();
    const user = this.authService.currentUser();
    const deck = user?.decks?.find(d => d.id === user.activeDeckId);
    return deck?.name ?? this.translate.instant('LOBBY.NO_DECK');
  });

  readonly activeDeckId = () => this.authService.currentUser()?.activeDeckId ?? null;

  readonly getCharacterPortrait = getCharacterPortrait;

  private readonly lobbyTracks = ['LobbySound1.mp3'];

  ngAfterViewInit(): void {
    const pick = this.lobbyTracks[Math.floor(Math.random() * this.lobbyTracks.length)];
    this.audioMusic.play(pick);
  }

  ngOnInit(): void {
    this.loadRooms();
    this.checkPendingMatches();
    this.restoreActiveRoom();
    this.pollSub = interval(5000)
      .pipe(
        switchMap(() => this.lobbyService.getRooms().pipe(
          catchError(() => of([])),
        )),
      )
      .subscribe({
        next: rooms => this.rooms.set(rooms),
      });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.showOptions()) { this.showOptions.set(false); this.showMenu.set(true); return; }
    this.showMenu.update(v => !v);
  }

  onMenuExit(): void { this.showMenu.set(false); this.goBack(); }

  async goBack(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.audioMusic.stop();
    this.sfx.play('Click.mp3');
    const user = this.authService.currentUser();
    await this.circleTransition.close();
    if (user?.username) {
      const saved = this.gameStateService.getSavedPosition(user.username);
      if (saved) {
        this.router.navigate(['/juego', saved.location], { queryParams: { user: user.username } });
        return;
      }
      this.router.navigate(['/juego', 'pueblo_paleta'], { queryParams: { user: user.username } });
      return;
    }
    this.router.navigate(['/juego', 'pueblo_paleta']);
  }

  private loadRooms(): void {
    this.loading.set(true);
    this.lobbyService.getRooms()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: rooms => {
          this.rooms.set(rooms);
          if (this.circleTransition.pendingOpen) {
            setTimeout(() => this.circleTransition.open());
          }
        },
        error: () => this.toastService.show(this.translate.instant('LOBBY.TOAST_ERROR_LOADING'), 'error'),
      });
  }

  onJoinByCode(): void {
    this.sfx.play('Click.mp3');
    const code = this.joinCodeInput.trim();
    if (!code) return;

    const deckId = this.activeDeckId();
    if (!deckId) {
      this.toastService.show(this.translate.instant('LOBBY.TOAST_NEED_DECK'), 'error');
      return;
    }

    this.lobbyService.joinByCode(code, deckId).subscribe({
      next: room => {
        this.toastService.show(this.translate.instant('LOBBY.TOAST_JOINED'), 'success');
        this.joinCodeInput = '';
        this.openWaitingRoom(room, false);
      },
      error: err => {
        if (err.status === 428) {
          this.pendingCode = code;
          this.codePassword = '';
          this.showCodePasswordModal.set(true);
        } else {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_INVALID_CODE'), 'error');
        }
      },
    });
  }

  onConfirmCodePassword(): void {
    this.sfx.play('Click.mp3');
    if (!this.pendingCode) return;
    const deckId = this.activeDeckId();
    if (!deckId) return;

    this.lobbyService.joinByCode(this.pendingCode, deckId, this.codePassword).subscribe({
      next: room => {
        this.toastService.show(this.translate.instant('LOBBY.TOAST_JOINED'), 'success');
        this.joinCodeInput = '';
        this.pendingCode = '';
        this.codePassword = '';
        this.showCodePasswordModal.set(false);
        this.openWaitingRoom(room, false);
      },
      error: err => {
        if (err.status === 403) {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_WRONG_PASSWORD'), 'error');
        } else {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_INVALID_CODE'), 'error');
        }
      },
    });
  }

  onCancelCodePassword(): void {
    this.sfx.play('Click.mp3');
    this.showCodePasswordModal.set(false);
    this.pendingCode = '';
    this.codePassword = '';
  }

  onJoinRoom(matchId: string): void {
    this.sfx.play('Click.mp3');
    const deckId = this.activeDeckId();
    if (!deckId) {
      this.toastService.show(this.translate.instant('LOBBY.TOAST_NEED_DECK'), 'error');
      return;
    }

    this.lobbyService.joinMatch(matchId, deckId).subscribe({
      next: room => {
        this.toastService.show(this.translate.instant('LOBBY.TOAST_JOINED'), 'success');
        this.openWaitingRoom(room, false);
      },
      error: err => {
        if (err.status === 428) {
          this.pendingRoomId = matchId;
          this.pendingRoomPassword = '';
          this.showRoomPasswordModal.set(true);
        } else if (err.status === 403) {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_WRONG_PASSWORD'), 'error');
        } else {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_CANT_JOIN'), 'error');
        }
      },
    });
  }

  onConfirmRoomPassword(): void {
    this.sfx.play('Click.mp3');
    if (!this.pendingRoomId) return;
    const deckId = this.activeDeckId();
    if (!deckId) return;
    this.doJoinRoom(this.pendingRoomId, deckId, this.pendingRoomPassword);
    this.showRoomPasswordModal.set(false);
    this.pendingRoomId = '';
    this.pendingRoomPassword = '';
  }

  onCancelRoomPassword(): void {
    this.sfx.play('Click.mp3');
    this.showRoomPasswordModal.set(false);
    this.pendingRoomId = '';
    this.pendingRoomPassword = '';
  }

  private doJoinRoom(matchId: string, deckId: string, password?: string): void {
    this.lobbyService.joinMatch(matchId, deckId, password).subscribe({
      next: room => {
        this.toastService.show(this.translate.instant('LOBBY.TOAST_JOINED'), 'success');
        this.openWaitingRoom(room, false);
      },
      error: err => {
        if (err.status === 428) {
          this.pendingRoomId = matchId;
          this.pendingRoomPassword = '';
          this.showRoomPasswordModal.set(true);
        } else if (err.status === 403) {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_WRONG_PASSWORD'), 'error');
        } else {
          this.toastService.show(this.translate.instant('LOBBY.TOAST_CANT_JOIN'), 'error');
        }
      },
    });
  }

  private checkPendingMatches(): void {
    this.lobbyService.getPendingMatches().subscribe({
      next: matches => {
        if (matches.length > 0) {
          this.pendingMatch.set(matches[0]);
          this.showPendingMatchModal.set(true);
        }
      },
    });
  }

  onContinuePendingMatch(pm: LobbyRoom): void {
    this.showPendingMatchModal.set(false);
    this.pendingMatch.set(null);
    this.router.navigate(['/batalla', pm.matchId], { queryParams: { reconnect: true } });
  }

  onSurrenderPendingMatch(pm: LobbyRoom): void {
    this.showPendingMatchModal.set(false);
    this.pendingMatch.set(null);
    // Surrender via REST or Websocket — send POST to cancel
    this.lobbyService.surrenderMatch(pm.matchId).subscribe({
      next: () => this.toastService.show('Te has rendido. Partida finalizada.', 'info'),
      error: () => this.toastService.show('Error al rendirse', 'error'),
    });
  }

  onRefreshRooms(): void {
    this.sfx.play('Click.mp3');
    this.loadRooms();
  }

  onChangeDeck(): void {
    this.sfx.play('Click.mp3');
    this.router.navigate(['/decks'], { queryParams: { from: 'lobby' } });
  }

  onRoomCreated(room: LobbyRoom): void {
    this.showCreateModal.set(false);
    this.openWaitingRoom(room, true);
  }

  onCloseWaitingRoom(reason?: string): void {
    this.sfx.play('Click.mp3');
    this.showWaitingRoom.set(false);
    this.clearActiveRoom();
    this.loadRooms();
    if (reason === 'self_cancelled') {
      const msg = this.waitingIsHost()
        ? this.translate.instant('LOBBY.TOAST_MATCH_CANCELLED')
        : this.translate.instant('LOBBY.TOAST_LEFT_ROOM');
      this.toastService.show(msg, 'success');
    } else if (reason === 'opponent_cancelled') {
      this.toastService.show(this.translate.instant('LOBBY.TOAST_HOST_CANCELLED'), 'info');
    }
  }

  onMatchStarted(matchId: string): void {
    this.showWaitingRoom.set(false);
    this.clearActiveRoom();
    this.router.navigate(['/batalla', matchId]);
  }

  onCountdownStarted(): void {
    this.audioMusic.fadeOut(2000, true);
  }

  onCountdownCancelled(): void {
    this.audioMusic.fadeIn(2000);
  }

  private openWaitingRoom(room: LobbyRoom, isHost: boolean): void {
    this.waitingMatchId.set(room.matchId);
    this.waitingJoinCode.set(room.joinCode);
    this.waitingRoomName.set(room.roomName || this.translate.instant('LOBBY.UNNAMED_ROOM'));
    this.waitingHostTrainerName.set(room.hostTrainerName);
    this.waitingHostCharacter.set(room.hostCharacter);
    this.waitingGuestTrainerName.set(room.guestTrainerName);
    this.waitingGuestCharacter.set(room.guestCharacter);
    this.waitingHostReady.set(room.hostReady);
    this.waitingGuestReady.set(room.guestReady);
    this.waitingIsHost.set(isHost);
    this.showWaitingRoom.set(true);
    this.saveActiveRoom(room.matchId, isHost);
  }

  private saveActiveRoom(matchId: string, isHost: boolean): void {
    try {
      localStorage.setItem(this.ACTIVE_MATCH_KEY, matchId);
      localStorage.setItem(this.ACTIVE_MATCH_HOST_KEY, String(isHost));
    } catch {}
  }

  private clearActiveRoom(): void {
    try {
      localStorage.removeItem(this.ACTIVE_MATCH_KEY);
      localStorage.removeItem(this.ACTIVE_MATCH_HOST_KEY);
    } catch {}
  }

  private restoreActiveRoom(): void {
    let matchId: string | null = null;
    let isHost = true;
    try {
      matchId = localStorage.getItem(this.ACTIVE_MATCH_KEY);
      const stored = localStorage.getItem(this.ACTIVE_MATCH_HOST_KEY);
      isHost = stored === 'true';
    } catch {}

    if (!matchId) return;

    this.lobbyService.getLobbyState(matchId).subscribe({
      next: room => {
        if (room.started) {
          this.clearActiveRoom();
          this.router.navigate(['/batalla', matchId]);
        } else if (room.status !== 'WAITING') {
          this.clearActiveRoom();
          this.toastService.show(this.translate.instant('LOBBY.TOAST_ROOM_UNAVAILABLE'), 'info');
        } else {
          this.openWaitingRoom(room, isHost);
        }
      },
      error: () => this.clearActiveRoom(),
    });
  }
}
