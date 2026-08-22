import { Component, HostListener, effect, inject, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { GameEngineService } from '../../data-access/services/game-engine.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { SecretCardRevealComponent } from '../secret-card-reveal/secret-card-reveal';
import { QuestService } from '../../data-access/services/quest.service';
import { MatchLobbyService, BotDifficulty } from '../../../lobby/services/match-lobby.service';
import { AuthService } from '../../data-access/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-npc-interaction',
  standalone: true,
  imports: [TranslateModule, MyButton, MyText, SecretCardRevealComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (engine.nearNpcIndex() !== null && !engine.interactionOpen() && !engine.interactionDialog()) {
      <div
        id="npc-hint"
        [style.left.px]="hintX()"
        [style.top.px]="hintY()"
        (click)="onHintClick()"
        (contextmenu)="$event.preventDefault()"
      >
        {{ 'NPC_INTERACTION.TALK' | translate }}
      </div>
    }

    @if (engine.nearChair() && !engine.interactionOpen() && !engine.interactionDialog()) {
      <div
        id="jugar-hint"
        [style.left.px]="jugarX()"
        [style.top.px]="jugarY()"
        (click)="onJugarClick()"
        (contextmenu)="$event.preventDefault()"
      >
        {{ 'NPC_INTERACTION.PLAY' | translate }}
      </div>
    }

    @if (engine.nearSecret() && !engine.secretCodeOpen() && !engine.interactionDialog()) {
      <div
        id="secret-hint"
        [style.left.px]="secretX()"
        [style.top.px]="secretY()"
        (click)="onSecretHintClick()"
        (contextmenu)="$event.preventDefault()"
      >
        {{ 'NPC_INTERACTION.ENTER_CODE' | translate }} (E)
      </div>
    }

    @if (engine.interactionOpen()) {
      <div id="npc-overlay" (click)="onOverlayClick($event)">
        <div
          id="npc-menu"
          class="p-6 min-w-[200px] w-fit text-center flex flex-col gap-3"
          [style.left.px]="menuX()"
          [style.top.px]="menuY()"
          (click)="$event.stopPropagation()"
        >
          <my-text [text]="npcTitle" tag="h3" size="1.08rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <div class="menu-options flex flex-col gap-3 mx-auto">
            @for (opt of menuOptions; track opt; let i = $index) {
              <div
                class="menu-option-wrapper"
                [class.menu-option-selected]="i === selectedIndex()"
                (mouseenter)="selectedIndex.set(i)"
              >
                <my-button
                  [text]="opt | translate"
                  variant="pixel"
                  size="medium"
                  [color]="opt === 'NPC_INTERACTION.LEAVE' ? 'var(--neon-magenta)' : 'var(--neon-cyan)'"
                  (clicked)="onMenuAction(opt)"
                ></my-button>
              </div>
            }
          </div>
        </div>
      </div>
    }

    @if (engine.interactionDialog()) {
      <div id="npc-dialog" class="p-6">
        <div class="flex flex-col items-center gap-4">
          <p class="dialog-text">{{ displayText() }}</p>
          <my-button [text]="(isLastDialogue() ? 'NPC_INTERACTION.CLOSE' : 'NPC_INTERACTION.NEXT') | translate" variant="pixel" size="small" (clicked)="onVolver()"></my-button>
        </div>
      </div>
    }

    @if (tutorialState() === 'video') {
      <div id="tutorial-video-overlay" class="fixed inset-0 z-[2000] bg-black/85 flex flex-col items-center justify-center gap-4" (click)="closeVideo()">
        <video #tutorialVideo
          [src]="tutorialVideoSrc()"
          autoplay playsinline
          class="max-w-[90vw] max-h-[75vh] rounded"
          (ended)="onVideoEnded()"
          (timeupdate)="updateVideoProgress()"
          (click)="$event.stopPropagation()"
        ></video>
        <div class="w-full max-w-[90vw] h-3 bg-white/20 rounded cursor-pointer"
             (click)="$event.stopPropagation()"
             (mousedown)="startDrag($event); $event.stopPropagation()"
             (mousemove)="onProgressDrag($event)"
             (mouseup)="stopDrag()"
             (mouseleave)="stopDrag()">
          <div class="h-full bg-[var(--neon-cyan)] rounded transition-[width] duration-150" [style.width.%]="videoProgress()"></div>
        </div>
        <div class="flex items-center gap-3" (click)="$event.stopPropagation()">
          <my-button [text]="videoPaused() ? '▶' : '⏸'" variant="pixel" size="small" (clicked)="toggleVideoPlay()"></my-button>
          <my-button [text]="videoMuted() ? '🔇' : '🔊'" variant="pixel" size="small" (clicked)="toggleVideoMute()"></my-button>
          <my-button [text]="'NPC_INTERACTION.CLOSE' | translate" variant="pixel" size="small" color="var(--neon-magenta)" (clicked)="closeVideo()"></my-button>
        </div>
      </div>
    }

    @if (tutorialState() === 'deckSelection') {
      <div id="deck-selection-overlay" class="fixed inset-0 z-[2000] flex flex-col items-center justify-center" style="background-color: rgba(0,0,0,0.95);">
        <div class="absolute top-[10vh] left-0 right-0 flex justify-center pointer-events-none">
          <my-text [text]="'Elige tu mazo inicial'" tag="h2" size="1.2rem" weight="bold" color="var(--neon-cyan)" />
        </div>
        <div class="flex gap-6">
          @for (deck of starterDecks(); track deck.name; let idx = $index) {
            <div class="flex flex-col items-center gap-3 p-4 rounded-lg transition-transform"
                 [class.cursor-pointer]="!deckSelecting()"
                 [class.hover:scale-105]="!deckSelecting()"
                 [class.hover:bg-white/5]="!deckSelecting()"
                 [class.pointer-events-none]="deckSelecting() !== null"
                 [class.opacity-60]="deckSelecting() !== null && deckSelecting() !== deck.name"
                 (click)="selectStarterDeck(deck.name)">
              <div class="relative">
                <div class="deck-glow absolute -inset-6 rounded-full blur-3xl pointer-events-none"
                     [style.background]="'radial-gradient(circle, ' + deck.glow + ', transparent 70%)'"
                     [style.animation-delay]="(idx * 0.35) + 's'"></div>
                <div class="deck-particles pointer-events-none" aria-hidden="true">
                  @for (p of [0,1,2,3,4,5]; track p) {
                    <span class="deck-particle"
                          [style.left.px]="(p - 2.5) * 40"
                          [style.animation-delay]="(idx * 0.15) + (p * 0.45) + 's'"
                          [style.animation-duration]="(2.2 + (p % 3) * 0.5) + 's'"
                          [style.background]="deck.glow"></span>
                  }
                </div>
                @if (deckSelecting() === deck.name) {
                  <div class="w-[26rem] aspect-[2816/1536] max-w-full relative flex items-center justify-center">
                    <div class="w-10 h-10 border-4 border-white/10 border-t-[var(--neon-cyan)] rounded-full animate-spin"></div>
                  </div>
                } @else {
                  <img [src]="deck.image" [alt]="deck.name" class="w-[26rem] aspect-[2816/1536] max-w-full object-contain rounded relative" />
                }
              </div>
              <my-text [text]="deck.label" tag="span" size="0.72rem" weight="bold" color="var(--text-primary)" />
            </div>
          }
        </div>
      </div>
    }

    @if (tutorialState() === 'deckConfirmed' && confirmedDeck(); as deck) {
      <div id="deck-confirmed-overlay" class="fixed inset-0 z-[2000] flex flex-col items-center justify-center gap-6" style="background-color: rgba(0,0,0,0.95);">
        <my-text [text]="'¡Felicitaciones, elegiste ' + deck.label + '!'" tag="h2" size="1.1rem" weight="bold" color="var(--neon-cyan)" />
        <div class="relative">
          <div class="deck-glow absolute -inset-6 rounded-full blur-3xl pointer-events-none"
               [style.background]="'radial-gradient(circle, ' + deck.glow + ', transparent 70%)'"></div>
          <div class="deck-particles pointer-events-none" aria-hidden="true">
            @for (p of [0,1,2,3,4,5]; track p) {
              <span class="deck-particle"
                    [style.left.px]="(p - 2.5) * 46"
                    [style.animation-delay]="(p * 0.45) + 's'"
                    [style.animation-duration]="(2.2 + (p % 3) * 0.5) + 's'"
                    [style.background]="deck.glow"></span>
            }
          </div>
          <img [src]="deck.image" [alt]="deck.name" class="w-[32rem] aspect-[2816/1536] max-w-full object-contain rounded relative" />
        </div>
        <my-button [text]="'SECRET_REVEAL.CONTINUE' | translate" variant="pixel" color="var(--neon-cyan)" textColor="var(--bg-dark)" size="medium" (clicked)="confirmDeckSelection()" />
      </div>
    }

    @if (engine.secretCodeOpen()) {
      <div id="code-overlay" (click)="onCodeOverlayClick($event)">
        <div id="code-menu" (click)="$event.stopPropagation()">
          <my-text [text]="'NPC_INTERACTION.ENTER_CODE' | translate" tag="h3" size="1.08rem" weight="bold" color="#9b59b6" align="center"></my-text>
          <div class="code-display">{{ codeInput() }}</div>
          @if (showError()) {
            <div class="code-error">{{ 'NPC_INTERACTION.WRONG_CODE' | translate }}</div>
          }
          @for (row of letterRows; track $index) {
            <div class="code-keys">
              @for (key of row; track key) {
                <my-button [text]="key" variant="pixel" size="medium" color="var(--neon-cyan)" (clicked)="onKeyPress(key)"></my-button>
              }
            </div>
          }
          <div class="code-actions">
            <my-button [text]="'NPC_INTERACTION.DELETE' | translate" variant="pixel" size="medium" color="var(--neon-cyan)" (clicked)="onBackspace()"></my-button>
            <my-button [text]="'NPC_INTERACTION.ENTER' | translate" variant="pixel" size="medium" color="var(--neon-cyan)" (clicked)="onIngresarClick()"></my-button>
            <my-button [text]="'NPC_INTERACTION.EXIT' | translate" variant="pixel" size="medium" color="var(--neon-magenta)" (clicked)="onExitCodeMenu()"></my-button>
          </div>
        </div>
      </div>
    }

    @if (secretReveal(); as reward) {
      <app-secret-card-reveal
        [cardName]="reward.name"
        [imageUrl]="reward.imageUrl"
        (closed)="onCardRevealClosed()"
      />
    }
  `,
  styles: [`
    .deck-glow {
      animation: deck-glow-pulse 2.6s ease-in-out infinite;
    }
    @keyframes deck-glow-pulse {
      0%, 100% { opacity: 0.45; transform: scale(1); }
      50% { opacity: 0.95; transform: scale(1.08); }
    }

    .deck-particles {
      position: absolute;
      left: 50%;
      bottom: -4px;
      width: 0;
      height: 0;
      pointer-events: none;
    }
    .deck-particle {
      position: absolute;
      bottom: 0;
      width: 6px;
      height: 6px;
      border-radius: 9999px;
      opacity: 0;
      pointer-events: none;
      animation: deck-particle-rise 2.6s ease-out infinite;
    }
    @keyframes deck-particle-rise {
      0% { opacity: 0; transform: translateY(0) translateX(0) scale(1); }
      15% { opacity: 0.85; }
      100% { opacity: 0; transform: translateY(-110px) translateX(12px) scale(0.35); }
    }

    #npc-hint {
      position: fixed;
      z-index: 900;
      padding: 0.5rem 1.5rem;
      background: var(--neon-cyan);
      user-select: none;
      border: 3px solid;
      border-color: var(--pixel-border-top-left) #3a8abe #3a8abe var(--pixel-border-top-left);
      color: #0a0a1a;
      font-family: 'Press Start 2P', monospace;
      font-size: 0.6rem;
      font-weight: 700;
      letter-spacing: 2px;
      cursor: pointer;
      text-transform: uppercase;
      transition: all 0.3s;
      transform: translateX(-50%);
      animation: hintPulse 2s ease-in-out infinite;
    }
    #npc-hint:hover {
      brightness: 1.2;
      transform: translateX(-50%) translateY(-2px);
    }
    @keyframes hintPulse {
      0%, 100% { opacity: 0.85; }
      50% { opacity: 1; }
    }

    #jugar-hint {
      position: fixed;
      z-index: 900;
      padding: 0.5rem 1.5rem;
      background: var(--neon-magenta);
      user-select: none;
      border: 3px solid;
      border-color: var(--pixel-border-top-left) #be3a3a #be3a3a var(--pixel-border-top-left);
      color: #0a0a1a;
      font-family: 'Press Start 2P', monospace;
      font-size: 0.6rem;
      font-weight: 700;
      letter-spacing: 2px;
      cursor: pointer;
      text-transform: uppercase;
      transition: all 0.3s;
      transform: translateX(-50%);
      animation: hintPulse 2s ease-in-out infinite;
    }
    #jugar-hint:hover {
      brightness: 1.2;
      transform: translateX(-50%) translateY(-2px);
    }

    #secret-hint {
      position: fixed;
      z-index: 900;
      padding: 0.35rem 1rem;
      background: #9b59b6;
      user-select: none;
      border: 3px solid;
      border-color: #af7ac5 #5b2c6f #5b2c6f #af7ac5;
      box-shadow: 0 0 20px rgba(255, 215, 0, 0.6);
      color: #0a0a1a;
      font-family: 'Press Start 2P', monospace;
      font-size: 0.6rem;
      font-weight: 700;
      letter-spacing: 2px;
      cursor: pointer;
      text-transform: uppercase;
      transition: all 0.3s;
      transform: translateX(-50%);
      animation: hintPulse 2s ease-in-out infinite;
    }
    #secret-hint:hover {
      brightness: 1.2;
      transform: translateX(-50%) translateY(-2px);
    }

    #npc-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      z-index: 1000;
    }

    /* Ancho fijado por el botón más ancho, no por el título del panel. */
    .menu-options {
      width: fit-content;
    }

    .menu-option-wrapper {
      position: relative;
      padding: 4px;
      border: 2px solid transparent;
      transition: all 0.12s ease-out;
      /* Estira el botón interno al ancho del contenedor para igualarlos. */
      display: flex;
      flex-direction: column;
    }
    .menu-option-selected {
      border-color: var(--neon-cyan);
      box-shadow: 0 0 12px rgba(0, 170, 255, 0.55);
      transform: translateX(4px);
    }

    #npc-menu {
      position: fixed;
      z-index: 1001;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(0,170,255,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(0,170,255,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      border: 4px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      box-shadow: 0 0 30px rgba(0,170,255,0.2);
    }

    #npc-dialog {
      position: fixed;
      bottom: 60px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 1000;
      width: 500px;
      max-width: 90vw;
      text-align: center;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(0,170,255,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(0,170,255,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      border: 4px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      box-shadow: 0 0 30px rgba(0,170,255,0.2);
    }
    .dialog-text {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.72rem;
      color: var(--text-primary);
      line-height: 1.8;
      margin: 0;
      padding: 0.5rem 1rem;
      border-left: 3px solid;
      border-color: var(--pixel-border-top-left);
      text-align: left;
      width: 100%;
    }

    #code-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      z-index: 1000;
    }

    #code-menu {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 1001;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(155,89,182,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(155,89,182,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      border: 4px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      box-shadow: 0 0 30px rgba(255, 215, 0, 0.3);
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      max-width: 700px;
      width: 90vw;
    }

    .code-display {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.8rem;
      color: var(--neon-cyan);
      background: rgba(0, 0, 0, 0.4);
      border: 2px solid var(--pixel-border-top-left);
      padding: 0.5rem 1rem;
      min-height: 2rem;
      width: 100%;
      text-align: center;
      letter-spacing: 4px;
    }

    .code-error {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.6rem;
      color: var(--neon-magenta);
      animation: hintPulse 0.5s ease-in-out infinite;
    }

    .code-keys {
      display: flex;
      justify-content: center;
      gap: 0.25rem;
    }

    .code-actions {
      display: flex;
      gap: 0.75rem;
      margin-top: 0.25rem;
    }

    #tutorial-video-overlay video {
      border: 3px solid var(--pixel-border-top-left);
      box-shadow: 0 0 30px var(--neon-cyan);
    }
  `],
})
export class NpcInteractionComponent {
  engine = inject(GameEngineService);
  private http = inject(HttpClient);
  private router = inject(Router);
  private translateService = inject(TranslateService);
  private circleTransition = inject(CircleTransitionService);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly musicChannel = inject(AudioMusicChannel);
  private readonly quests = inject(QuestService);
  private readonly lobbyService = inject(MatchLobbyService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

  readonly fightLoading = signal(false);

  readonly tutorialState = signal<'idle' | 'dialogue1' | 'video' | 'dialogue2' | 'dialogue3' | 'deckSelection' | 'deckConfirmed' | 'reward1' | 'reward2' | 'done'>('idle');
  readonly videoPaused = signal(false);
  readonly videoMuted = signal(false);
  readonly tutorialVideoSrc = signal('');
  readonly videoProgress = signal(0);

  readonly starterDecks = signal([
    { name: 'Chespin Deck', label: 'Planta - Chesnaught', image: 'assets/img/Decks/Grass Deck - Chesnaught.png', glow: 'rgba(34, 197, 94, 0.65)' },
    { name: 'Fennekin Deck', label: 'Fuego - Delphox', image: 'assets/img/Decks/Fire Deck - Delphos.png', glow: 'rgba(239, 68, 68, 0.65)' },
    { name: 'Froakie Deck', label: 'Agua - Greninja', image: 'assets/img/Decks/Water Deck - Greninja.png', glow: 'rgba(34, 211, 238, 0.65)' },
  ]);

  readonly confirmedDeck = signal<{ name: string; label: string; image: string; glow: string } | null>(null);

  readonly deckSelecting = signal<string | null>(null);

  private draggingVideo = false;

  private navigating = false;
  private typewriterTimer: ReturnType<typeof setInterval> | null = null;
  private typewriterAudio: { stop: () => void } | null = null;
  readonly displayText = signal('');
  readonly dialogueIndex = signal(0);

  /** Índice de la opción del menú resaltada por teclado. */
  readonly selectedIndex = signal(0);

  /**
   * Carta a revelar tras cerrar el diálogo de un NPC con recompensa.
   * Se setea con la respuesta del reclamo al backend; null = sin animación.
   */
  readonly secretReveal = signal<{ name: string; imageUrl: string; npcId: string } | null>(null);

  private previousMusicTrack: string | null = null;

  private readonly STARTER_PENDING_KEY = 'starterDeckPending';

  constructor() {
    this.restoreStarterDeckSelection();

    // Cada vez que se abre el menú, resetear la selección al primer botón y
    // limpiar el estado de teclado del engine para que el personaje no quede
    // caminando solo si el jugador presionó una flecha justo al abrir.
    effect(() => {
      if (this.engine.interactionOpen()) {
        this.selectedIndex.set(0);
        this.engine.releaseAllKeys();
      }
    });

    // Cuando se revela una carta secreta de Don Alfajor, cambiar la musica a numb.mp3
    effect(() => {
      const reward = this.secretReveal();
      if (reward && reward.npcId === 'Don Alfajor') {
        this.previousMusicTrack = this.musicChannel.getCurrentTrack();
        this.musicChannel.playOnce('numb.mp3');
      }
    });
  }

  private markStarterDeckPending(): void {
    const user = this.authService.currentUser()?.username;
    if (user) {
      try {
        localStorage.setItem(this.STARTER_PENDING_KEY, user);
      } catch {}
    }
  }

  private clearStarterDeckPending(): void {
    try {
      localStorage.removeItem(this.STARTER_PENDING_KEY);
    } catch {}
  }

  private restoreStarterDeckSelection(): void {
    try {
      const user = this.authService.currentUser()?.username;
      const pending = localStorage.getItem(this.STARTER_PENDING_KEY);
      const hasDeck = (this.authService.currentUser()?.decks?.length ?? 0) > 0;
      if (pending && pending === user && !hasDeck) {
        this.tutorialState.set('deckSelection');
      } else if (pending) {
        this.clearStarterDeckPending();
      }
    } catch {}
  }

  /**
   * Captura las flechas y Enter mientras hay un menú u un diálogo abierto.
   * Las flechas mueven la selección, Enter ejecuta la opción resaltada.
   */
  @HostListener('document:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (this.engine.secretCodeOpen()) {
      if (event.key === 'Escape') {
        event.preventDefault();
        this.onExitCodeMenu();
        return;
      }
      if (event.key === 'Enter') {
        event.preventDefault();
        this.onIngresarClick();
        return;
      }
      if (event.key === 'Backspace') {
        event.preventDefault();
        this.onBackspace();
        return;
      }
      if (event.key.length === 1 && /[a-zA-Z0-9]/.test(event.key)) {
        event.preventDefault();
        this.onKeyPress(event.key.toUpperCase());
        return;
      }
      event.preventDefault();
      return;
    }

    if (this.engine.interactionDialog()) {
      if (event.key === 'Enter') {
        event.preventDefault();
        this.onVolver();
      }
      return;
    }

    if (!this.engine.interactionOpen()) {
      return;
    }

    const options = this.menuOptions;
    if (options.length === 0) return;

    switch (event.key) {
      case 'ArrowDown':
      case 's':
      case 'S':
        event.preventDefault();
        this.selectedIndex.update(i => (i + 1) % options.length);
        this.engine.releaseAllKeys();
        break;
      case 'ArrowUp':
      case 'w':
      case 'W':
        event.preventDefault();
        this.selectedIndex.update(i => (i - 1 + options.length) % options.length);
        this.engine.releaseAllKeys();
        break;
      case 'Enter':
        event.preventDefault();
        this.onMenuAction(options[this.selectedIndex()]);
        break;
    }
  }

  hintX = computed(() => this.engine.nearNpcPosition().x + 28);
  hintY = computed(() => this.engine.nearNpcPosition().y + 84);
  jugarX = computed(() => this.engine.slotMachinePosition().x + 28);
  jugarY = computed(() => this.engine.slotMachinePosition().y + 108);
  secretX = computed(() => this.engine.secretPosition().x + 28);
  secretY = computed(() => this.engine.secretPosition().y + 84);
  menuX = computed(() => this.engine.nearNpcPosition().x + 80);
  menuY = computed(() => this.engine.nearNpcPosition().y - 20);

  readonly codeInput = signal('');
  readonly showError = signal(false);
  readonly letterRows: string[][] = [
    ['1','2','3','4','5','6','7','8','9','0'],
    ['Q','W','E','R','T','Y','U','I','O','P'],
    ['A','S','D','F','G','H','J','K','L'],
    ['Z','X','C','V','B','N','M'],
  ];

  currentDialogueText = computed(() => {
    const cfg = this.engine.interactionNpcConfig();
    if (!cfg?.name) return '';
    if (cfg.dialogues && cfg.dialogues.length > 0) {
      const key = 'NPC_DIALOGUE.' + cfg.name.toUpperCase().replace(/ /g, '_').replace(/Ñ/g, 'N') + '_' + (this.dialogueIndex() + 1);
      return this.translateService.instant(key);
    }
    const key = 'NPC_DIALOGUE.' + cfg.name.toUpperCase().replace(/ /g, '_').replace(/Ñ/g, 'N');
    return this.translateService.instant(key);
  });

  isLastDialogue = computed(() => {
    const cfg = this.engine.interactionNpcConfig();
    if (!cfg?.dialogues) return true;
    if (cfg.randomDialogue) return true;
    return this.dialogueIndex() >= cfg.dialogues.length - 1;
  });

  get npcTitle(): string {
    const cfg = this.engine.interactionNpcConfig();
    return cfg?.name.toUpperCase() ?? '';
  }

  get menuOptions(): string[] {
    const cfg = this.engine.interactionNpcConfig();
    if (!cfg?.menuOptions) return [];
    const opts = cfg.menuOptions.map(opt => this.translateMenuOption(opt));
    if (this.hasTutorial()) {
      opts.unshift('NPC_INTERACTION.TUTORIAL');
    }
    return opts;
  }

  private translateMenuOption(opt: string): string {
    const key = opt.toLowerCase();
    switch (key) {
      case 'hablar': return 'NPC_INTERACTION.SPEAK';
      case 'salir': return 'NPC_INTERACTION.LEAVE';
      case 'comprar': return 'NPC_INTERACTION.BUY';
      case 'enfrentamiento': return 'NPC_INTERACTION.FIGHT';
      case 'tutorial': return 'NPC_INTERACTION.TUTORIAL';
      default: return opt;
    }
  }

  onHintClick() {
    this.sfx.play('NpcBoton.mp3');
    this.engine.openInteraction();
  }

  async onJugarClick() {
    if (this.navigating) return;
    this.navigating = true;
    this.sfx.play('NpcBoton.mp3');
    this.engine.saveGame();
    await this.circleTransition.close();
    this.router.navigate(['/slot-machine']);
  }

  onSecretHintClick() {
    this.sfx.play('NpcBoton.mp3');
    this.engine.openSecretCodeMenu();
  }

  onCodeOverlayClick(event: MouseEvent) {
    if ((event.target as HTMLElement).id === 'code-overlay') {
      this.sfx.play('NpcBoton.mp3');
      this.onExitCodeMenu();
    }
  }

  onKeyPress(key: string) {
    this.sfx.play('NpcBoton.mp3');
    this.showError.set(false);
    this.codeInput.update(val => val + key);
  }

  onIngresarClick() {
    this.sfx.play('NpcBoton.mp3');
    if (this.codeInput() === 'CORNUDO666') {
      this.onCorrectCode();
    } else {
      this.showError.set(true);
      setTimeout(() => this.showError.set(false), 2000);
    }
  }

  onBackspace() {
    this.sfx.play('NpcBoton.mp3');
    this.showError.set(false);
    this.codeInput.update(val => val.slice(0, -1));
  }

  onExitCodeMenu() {
    this.sfx.play('NpcBoton.mp3');
    this.codeInput.set('');
    this.showError.set(false);
    this.engine.closeSecretCodeMenu();
  }

  private async onCorrectCode() {
    this.engine.saveGame();
    await this.circleTransition.close();
    this.engine.closeSecretCodeMenu();
    this.codeInput.set('');
    this.router.navigate(['/juego', 'secret'], {
      queryParams: { dcol: 0, drow: 0, user: this.engine.getUsername() },
    });
  }

  onOverlayClick(event: MouseEvent) {
    if ((event.target as HTMLElement).id === 'npc-overlay') {
      this.sfx.play('NpcBoton.mp3');
      this.engine.closeInteraction();
    }
  }

  hasTutorial(): boolean {
    return !!this.engine.interactionNpcConfig()?.tutorial;
  }

  async onMenuAction(action: string) {
    this.sfx.play('NpcBoton.mp3');
    switch (action) {
      case 'TUTORIAL':
      case 'NPC_INTERACTION.TUTORIAL':
        this.tutorialVideoSrc.set(this.engine.interactionNpcConfig()?.tutorial?.videoSrc ?? '');
        this.engine.closeInteraction();
        this.tutorialState.set('dialogue1');
        this.displayText.set('');
        this.engine.showDialog('show');
        setTimeout(() => this.startTypewriter(this.translateService.instant('NPC_DIALOGUE.FABIO_Y_JOACO_3')), 100);
        break;
      case 'NPC_INTERACTION.SPEAK':
        {
    if (this.tutorialState() === 'dialogue2') {
      this.engine.hideDialog();
      this.tutorialState.set('dialogue3');
      this.displayText.set('');
      this.engine.showDialog('show');
      setTimeout(() => this.startTypewriter(this.translateService.instant('NPC_DIALOGUE.FABIO_Y_JOACO_5')), 100);
      return;
    }
    if (this.tutorialState() === 'dialogue3') {
      this.engine.hideDialog();
      if ((this.authService.currentUser()?.decks?.length ?? 0) > 0) {
        this.tutorialState.set('reward1');
        this.claimTutorialReward('Fabio y Joaco');
      } else {
        this.tutorialState.set('deckSelection');
        this.markStarterDeckPending();
      }
      return;
    }

    const cfg = this.engine.interactionNpcConfig();
          if (cfg?.dialogues && cfg.dialogues.length > 0) {
            this.dialogueIndex.set(cfg.randomDialogue ? Math.floor(Math.random() * cfg.dialogues.length) : 0);
          } else {
            this.dialogueIndex.set(0);
          }
        }
        this.displayText.set('');
        this.engine.showDialog('show');
        setTimeout(() => this.startTypewriter(this.currentDialogueText()), 100);
        break;
      case 'NPC_INTERACTION.LEAVE':
        this.engine.closeInteraction();
        break;
      case 'NPC_INTERACTION.BUY':
        console.log('[NPC-Blanca] Botón "Comprar" presionado → navegando a /pokeshop');
        this.engine.saveGame();
        await this.circleTransition.close();
        this.router.navigate(['/pokeshop']);
        break;
      case 'NPC_INTERACTION.FIGHT': {
        const cfg = this.engine.interactionNpcConfig();
        if (cfg?.botDifficulty) {
          await this.startNpcBotMatch(cfg.botDifficulty);
        } else {
          this.engine.saveGame();
          await this.circleTransition.close();
          this.router.navigate(['/lobby']);
        }
        break;
      }
    }
  }

  private async startNpcBotMatch(difficulty: BotDifficulty): Promise<void> {
    if (this.fightLoading()) return;
    this.fightLoading.set(true);

    const deckId = this.authService.currentUser()?.activeDeckId;
    if (!deckId) {
      this.toastService.show(
        this.translateService.instant('NPC_INTERACTION.NO_DECK_ERROR'),
        'error'
      );
      this.fightLoading.set(false);
      this.engine.closeInteraction();
      this.engine.saveGame();
      await this.circleTransition.close();
      this.router.navigate(['/decks']);
      return;
    }

    this.engine.closeInteraction();
    this.engine.saveGame();
    await this.circleTransition.close();

    this.lobbyService.createBotMatch(deckId, difficulty).subscribe({
      next: (room) => {
        this.fightLoading.set(false);
        this.router.navigate(['/batalla', room.matchId]);
      },
      error: () => {
        this.fightLoading.set(false);
        this.toastService.show(
          this.translateService.instant('NPC_INTERACTION.FIGHT_ERROR'),
          'error'
        );
        this.router.navigate(['/juego', 'lan']);
      },
    });
  }

  onVolver() {
    this.sfx.play('NpcBoton.mp3');
    this.stopTypewriter();

    if (this.tutorialState() === 'dialogue1') {
      this.previousMusicTrack = this.musicChannel.getCurrentTrack();
      this.musicChannel.stop();
      this.engine.hideDialog();
      this.tutorialState.set('video');
      return;
    }
    if (this.tutorialState() === 'dialogue2') {
      this.engine.hideDialog();
      this.tutorialState.set('dialogue3');
      this.displayText.set('');
      this.engine.showDialog('show');
      setTimeout(() => this.startTypewriter(this.translateService.instant('NPC_DIALOGUE.FABIO_Y_JOACO_5')), 100);
      return;
    }
    if (this.tutorialState() === 'dialogue3') {
      this.stopTypewriter();
      this.engine.hideDialog();
      this.displayText.set('');
      setTimeout(() => {
        if ((this.authService.currentUser()?.decks?.length ?? 0) > 0) {
          this.tutorialState.set('reward1');
          this.claimTutorialReward('Fabio y Joaco');
        } else {
          this.tutorialState.set('deckSelection');
          this.markStarterDeckPending();
        }
      }, 50);
      return;
    }

    const cfg = this.engine.interactionNpcConfig();
    if (cfg?.dialogues && !cfg.randomDialogue && this.dialogueIndex() < cfg.dialogues.length - 1) {
      this.dialogueIndex.update(i => i + 1);
      setTimeout(() => this.startTypewriter(this.currentDialogueText()), 100);
    } else {
      this.engine.hideDialog();
      if (cfg?.secretRewardCardId) {
        this.claimSecretReward(cfg.name);
      }
    }
  }

  onCardRevealClosed(): void {
    this.secretReveal.set(null);
    if (this.previousMusicTrack) {
      this.musicChannel.play(this.previousMusicTrack);
      this.previousMusicTrack = null;
    }
    if (this.tutorialState() === 'reward1') {
      this.tutorialState.set('reward2');
      this.claimTutorialReward('Fabio y Joaco-2');
    } else if (this.tutorialState() === 'reward2') {
      this.tutorialState.set('done');
    }
  }

  /**
   * Reclama la recompensa del NPC y, si es la primera vez, dispara la
   * animación de descubrimiento con la carta que devuelve el backend.
   */
  private getVideoEl(): HTMLVideoElement | null {
    return document.querySelector('#tutorial-video-overlay video') as HTMLVideoElement | null;
  }

  updateVideoProgress(): void {
    const video = this.getVideoEl();
    if (video && video.duration) {
      this.videoProgress.set((video.currentTime / video.duration) * 100);
    }
  }

  seekVideo(event: MouseEvent): void {
    const bar = event.currentTarget as HTMLElement;
    const rect = bar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
    const video = this.getVideoEl();
    if (video && video.duration) {
      video.currentTime = ratio * video.duration;
    }
  }

  startDrag(event: MouseEvent): void {
    this.draggingVideo = true;
    this.seekVideo(event);
  }

  onProgressDrag(event: MouseEvent): void {
    if (!this.draggingVideo) return;
    this.seekVideo(event);
  }

  stopDrag(): void {
    this.draggingVideo = false;
  }

  toggleVideoPlay(): void {
    const video = this.getVideoEl();
    if (!video) return;
    if (video.paused) {
      video.play();
      this.videoPaused.set(false);
    } else {
      video.pause();
      this.videoPaused.set(true);
    }
  }

  toggleVideoMute(): void {
    const video = this.getVideoEl();
    if (!video) return;
    video.muted = !video.muted;
    this.videoMuted.set(video.muted);
  }

  onVideoEnded(): void {
    this.tutorialState.set('dialogue2');
    this.restoreMusic();
    this.displayText.set('');
    this.engine.showDialog('show');
    setTimeout(() => this.startTypewriter(this.translateService.instant('NPC_DIALOGUE.FABIO_Y_JOACO_4')), 100);
  }

  closeVideo(): void {
    const video = this.getVideoEl();
    if (video) video.pause();
    this.tutorialState.set('dialogue2');
    this.restoreMusic();
    this.displayText.set('');
    this.engine.showDialog('show');
    setTimeout(() => this.startTypewriter(this.translateService.instant('NPC_DIALOGUE.FABIO_Y_JOACO_4')), 100);
  }

  selectStarterDeck(deckName: string): void {
    this.deckSelecting.set(deckName);
    this.http.post('/api/players/me/starter-deck', { deckName }, { withCredentials: true }).subscribe({
      next: () => {
        this.deckSelecting.set(null);
        const deck = this.starterDecks().find(d => d.name === deckName);
        this.confirmedDeck.set(deck ?? null);
        this.tutorialState.set('deckConfirmed');
        this.authService.getCurrentPlayer().subscribe();
      },
      error: () => {
        this.deckSelecting.set(null);
        this.toastService.show('Error al reclamar el mazo', 'error');
      },
    });
  }

  confirmDeckSelection(): void {
    this.clearStarterDeckPending();
    this.confirmedDeck.set(null);
    this.tutorialState.set('reward1');
    this.claimTutorialReward('Fabio y Joaco');
  }

  private restoreMusic(): void {
    if (this.previousMusicTrack) {
      this.musicChannel.play(this.previousMusicTrack);
      this.previousMusicTrack = null;
    }
  }

  private claimTutorialReward(npcId: string): void {
    if (this.quests.isClaimed(npcId)) {
      return;
    }
    this.quests.claimNpcReward(npcId).subscribe({
      next: res => {
        if (!res.alreadyClaimed && res.card) {
          this.engine.releaseAllKeys();
          this.secretReveal.set({ name: res.card.name, imageUrl: res.card.imageUrl, npcId });
        } else {
          this.onCardRevealClosed();
        }
      },
      error: () => this.onCardRevealClosed(),
    });
  }

  private claimSecretReward(npcId: string): void {
    if (this.quests.isClaimed(npcId)) {
      return;
    }
    this.quests.claimNpcReward(npcId).subscribe({
      next: res => {
        if (!res.alreadyClaimed && res.card) {
          this.engine.releaseAllKeys();
          this.secretReveal.set({ name: res.card.name, imageUrl: res.card.imageUrl, npcId });
        }
      },
      // Backend caído o sin sesión: no rompemos el flujo del juego.
      error: () => {},
    });
  }

  private startTypewriter(text: string): void {
    this.stopTypewriter();
    this.displayText.set('');
    this.typewriterAudio = this.sfx.playLooped('NpcSpeak.mp3');
    let index = 0;
    const speak = () => {
      if (index < text.length) {
        this.displayText.set(text.slice(0, index + 1));
        index++;
      }
      if (index >= text.length) {
        this.stopTypewriter();
      }
    };
    speak();
    this.typewriterTimer = setInterval(speak, 40);
  }

  private stopTypewriter(): void {
    if (this.typewriterTimer) {
      clearInterval(this.typewriterTimer);
      this.typewriterTimer = null;
    }
    if (this.typewriterAudio) {
      this.typewriterAudio.stop();
      this.typewriterAudio = null;
    }
  }
}
