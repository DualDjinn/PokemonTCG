import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';

type RevealStage = 'spin' | 'modal';

/**
 * Animación de descubrimiento de una carta secreta otorgada por un NPC.
 *
 * Secuencia:
 *  1. `spin` (~1.2s): la carta aparece como silueta girando sobre su eje Y
 *     entre rayos de luz, y se "revela" con un flash + onda expansiva.
 *  2. `modal`: la carta vuela a su lugar dentro del panel de misión
 *     completada mientras llueve confeti por toda la pantalla.
 *
 * La carta a mostrar (nombre + imagen) llega por inputs, así el mismo
 * componente sirve para cualquier NPC que entregue una recompensa.
 */
@Component({
  selector: 'app-secret-card-reveal',
  standalone: true,
  imports: [TranslateModule, MyButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="reveal-overlay" [class.stage-modal]="stage() === 'modal'" [class.stage-spin]="stage() === 'spin'">
      <div class="reveal-backdrop"></div>

      <div class="shake-zone">
        <div class="god-rays"></div>
        <div class="god-rays rays-reverse"></div>
        <div class="shockwave"></div>
        <div class="shockwave shockwave-cyan"></div>
        <div class="flash"></div>

        <div class="modal-panel">
          <h2 class="reveal-title">{{ 'SECRET_REVEAL.TITLE' | translate }}</h2>
          <div class="card-slot"></div>
          <p class="reveal-line line-discovery">{{ 'SECRET_REVEAL.DISCOVERY' | translate }}</p>
          <p class="reveal-line card-name">{{ cardName() }}</p>
          <p class="reveal-line line-belongs">{{ 'SECRET_REVEAL.BELONGS' | translate }}</p>
          <div class="continue-wrapper">
            <my-button
              [text]="'SECRET_REVEAL.CONTINUE' | translate"
              variant="pixel"
              size="medium"
              color="#ffd700"
              (clicked)="onContinue()"
            ></my-button>
          </div>
        </div>

        <div class="card-fly">
          <div class="card-inner">
            <img [src]="imageUrl()" [alt]="cardName()" draggable="false" />
          </div>
          @for (s of sparkles; track $index) {
            <div
              class="card-sparkle"
              [style.--sx]="s.x + 'px'"
              [style.--sy]="s.y + 'px'"
              [style.--sdelay]="s.delay + 'ms'"
            ></div>
          }
        </div>
      </div>

      @if (stage() === 'modal') {
        <div class="confetti-layer layer-back">
          @for (c of confettiBack; track $index) {
            <div
              class="confetti-piece"
              [style.--x]="c.x + 'vw'"
              [style.--c]="c.color"
              [style.--w]="c.w + 'px'"
              [style.--h]="c.h + 'px'"
              [style.--br]="c.round ? '50%' : '0'"
              [style.--sway]="c.sway + 'px'"
              [style.--rot]="c.rot + 'deg'"
              [style.--dur]="c.dur + 'ms'"
              [style.--delay]="c.delay + 'ms'"
            ></div>
          }
        </div>
        <div class="confetti-layer layer-front">
          @for (c of confetti; track $index) {
            <div
              class="confetti-piece"
              [style.--x]="c.x + 'vw'"
              [style.--c]="c.color"
              [style.--w]="c.w + 'px'"
              [style.--h]="c.h + 'px'"
              [style.--br]="c.round ? '50%' : '0'"
              [style.--sway]="c.sway + 'px'"
              [style.--rot]="c.rot + 'deg'"
              [style.--dur]="c.dur + 'ms'"
              [style.--delay]="c.delay + 'ms'"
            ></div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .reveal-overlay {
      position: fixed;
      inset: 0;
      z-index: 2000;
    }

    .reveal-backdrop {
      position: fixed;
      inset: 0;
      background: radial-gradient(circle at 50% 42%, rgba(20, 16, 60, 0.88) 0%, rgba(4, 2, 18, 0.96) 70%);
      opacity: 0;
      animation: backdrop-in 0.3s ease-out forwards;
    }
    @keyframes backdrop-in {
      to { opacity: 1; }
    }

    .shake-zone {
      position: fixed;
      inset: 0;
      perspective: 1200px;
      animation: impact-shake 0.4s linear 1.12s;
    }
    @keyframes impact-shake {
      0%, 100% { transform: translate(0, 0); }
      20% { transform: translate(-7px, 4px); }
      40% { transform: translate(6px, -5px); }
      60% { transform: translate(-4px, 3px); }
      80% { transform: translate(3px, -2px); }
    }

    /* ── Rayos de luz tras la carta ─────────────────────────────── */
    .god-rays {
      position: fixed;
      top: 50%;
      left: 50%;
      width: 160vmax;
      height: 160vmax;
      background: repeating-conic-gradient(from 0deg, rgba(255, 215, 0, 0.16) 0deg 9deg, transparent 9deg 22deg);
      -webkit-mask-image: radial-gradient(circle, rgba(0, 0, 0, 0.9) 0%, transparent 62%);
      mask-image: radial-gradient(circle, rgba(0, 0, 0, 0.9) 0%, transparent 62%);
      animation: rays-rotate 14s linear infinite;
      transition: opacity 0.6s ease-out;
      pointer-events: none;
    }
    .rays-reverse {
      background: repeating-conic-gradient(from 5deg, rgba(80, 200, 255, 0.1) 0deg 7deg, transparent 7deg 26deg);
      animation-direction: reverse;
      animation-duration: 22s;
    }
    .stage-modal .god-rays { opacity: 0.45; }
    @keyframes rays-rotate {
      from { transform: translate(-50%, -50%) rotate(0deg); }
      to   { transform: translate(-50%, -50%) rotate(360deg); }
    }

    /* ── Onda expansiva + flash al terminar el giro ─────────────── */
    .shockwave {
      position: fixed;
      top: 50%;
      left: 50%;
      width: 120px;
      height: 120px;
      border-radius: 50%;
      border: 4px solid rgba(255, 215, 0, 0.9);
      box-shadow: 0 0 40px rgba(255, 215, 0, 0.8), inset 0 0 30px rgba(255, 255, 255, 0.6);
      transform: translate(-50%, -50%) scale(0);
      opacity: 0;
      animation: shockwave-burst 0.8s cubic-bezier(0.16, 1, 0.3, 1) 1.05s forwards;
      pointer-events: none;
    }
    .shockwave-cyan {
      border-color: rgba(80, 200, 255, 0.8);
      box-shadow: 0 0 40px rgba(80, 200, 255, 0.7);
      animation-delay: 1.18s;
    }
    @keyframes shockwave-burst {
      0%   { transform: translate(-50%, -50%) scale(0.2); opacity: 1; }
      100% { transform: translate(-50%, -50%) scale(9); opacity: 0; }
    }

    .flash {
      position: fixed;
      inset: 0;
      background: radial-gradient(circle at 50% 50%, #fff 0%, rgba(255, 255, 255, 0.6) 35%, transparent 75%);
      opacity: 0;
      animation: flash-pop 0.5s ease-out 1.08s;
      pointer-events: none;
    }
    @keyframes flash-pop {
      0%   { opacity: 0; }
      25%  { opacity: 0.95; }
      100% { opacity: 0; }
    }

    /* ── La carta: giro de revelación y vuelo al panel ──────────── */
    .card-fly {
      position: fixed;
      top: 50%;
      left: 50%;
      width: 240px;
      height: 335px;
      transform: translate(-50%, -50%) rotateY(0deg) scale(1.04);
      transform-style: preserve-3d;
      transition: transform 0.7s cubic-bezier(0.34, 1.56, 0.64, 1);
      filter: drop-shadow(0 0 28px rgba(255, 215, 0, 0.65));
      z-index: 5;
    }
    .stage-spin .card-fly {
      animation: card-spin-in 1.2s cubic-bezier(0.33, 1, 0.68, 1) forwards;
    }
    .stage-modal .card-fly {
      transform: translate(-50%, calc(-50% - 62px)) rotateY(0deg) scale(0.76);
    }
    @keyframes card-spin-in {
      0% {
        transform: translate(-50%, -50%) rotateY(0deg) scale(0.04);
        filter: brightness(0) drop-shadow(0 0 30px rgba(255, 215, 0, 0.9));
      }
      40% {
        transform: translate(-50%, -50%) rotateY(540deg) scale(1.22);
        filter: brightness(0) drop-shadow(0 0 50px rgba(255, 215, 0, 1));
      }
      60% {
        filter: brightness(2.6) drop-shadow(0 0 60px rgba(255, 255, 255, 1));
      }
      80% {
        transform: translate(-50%, -50%) rotateY(1010deg) scale(0.94);
      }
      100% {
        transform: translate(-50%, -50%) rotateY(1080deg) scale(1.04);
        filter: brightness(1) drop-shadow(0 0 28px rgba(255, 215, 0, 0.65));
      }
    }

    .card-inner {
      position: relative;
      width: 100%;
      height: 100%;
      border-radius: 12px;
      overflow: hidden;
    }
    .stage-modal .card-inner {
      animation: card-float 3.2s ease-in-out 0.8s infinite;
    }
    @keyframes card-float {
      0%, 100% { transform: translateY(0) rotateZ(0deg); }
      50%      { transform: translateY(-7px) rotateZ(0.6deg); }
    }
    .card-inner img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 12px;
      display: block;
    }
    /* Brillo holográfico que barre la carta una vez asentada. */
    .card-inner::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(
        120deg,
        transparent 30%,
        rgba(255, 255, 255, 0.35) 48%,
        rgba(255, 215, 0, 0.25) 52%,
        transparent 70%
      );
      transform: translateX(-160%);
      pointer-events: none;
    }
    .stage-modal .card-inner::after {
      animation: holo-sweep 2.8s ease-in-out 1s infinite;
    }
    @keyframes holo-sweep {
      0%       { transform: translateX(-160%); }
      45%, 100% { transform: translateX(160%); }
    }

    .card-sparkle {
      position: absolute;
      top: 50%;
      left: 50%;
      width: 8px;
      height: 8px;
      background: #fff;
      border-radius: 50%;
      box-shadow: 0 0 12px #fff, 0 0 22px #ffd700;
      transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy))) scale(0.3);
      opacity: 0;
      animation: sparkle-twinkle 1.5s ease-out var(--sdelay) infinite;
      pointer-events: none;
    }
    @keyframes sparkle-twinkle {
      0%, 100% { opacity: 0; transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy))) scale(0.3); }
      50%      { opacity: 1; transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy))) scale(1.3); }
    }

    /* ── Panel de misión completada ─────────────────────────────── */
    .modal-panel {
      position: fixed;
      top: 50%;
      left: 50%;
      width: min(560px, 94vw);
      height: 580px;
      transform: translate(-50%, -50%) scale(0.8);
      opacity: 0;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(255, 215, 0, 0.05) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(255, 215, 0, 0.05) 25%, transparent 25%);
      background-size: 4px 4px;
      border: 4px solid;
      border-color: #ffe07c #8a6d00 #8a6d00 #ffe07c;
      box-shadow: 0 0 50px rgba(255, 215, 0, 0.35), 0 0 120px rgba(255, 215, 0, 0.15);
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 26px 30px;
      transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.4s ease-out;
      pointer-events: none;
      z-index: 3;
    }
    .stage-modal .modal-panel {
      transform: translate(-50%, -50%) scale(1);
      opacity: 1;
      pointer-events: auto;
    }

    .reveal-title {
      font-family: 'Press Start 2P', monospace;
      font-size: 1rem;
      line-height: 1.7;
      text-align: center;
      min-height: 58px;
      margin: 0;
      background: linear-gradient(180deg, #fff6c8 0%, #ffd700 45%, #b8860b 100%);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
      filter: drop-shadow(0 0 10px rgba(255, 215, 0, 0.55));
      transform: scale(2.4);
      opacity: 0;
    }
    .stage-modal .reveal-title {
      animation:
        title-stamp 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) 0.25s forwards,
        title-glow 2.2s ease-in-out 0.9s infinite;
    }
    @keyframes title-stamp {
      0%   { transform: scale(2.4); opacity: 0; }
      60%  { transform: scale(0.92); opacity: 1; }
      100% { transform: scale(1); opacity: 1; }
    }
    @keyframes title-glow {
      0%, 100% { filter: drop-shadow(0 0 10px rgba(255, 215, 0, 0.55)); }
      50%      { filter: drop-shadow(0 0 22px rgba(255, 215, 0, 0.95)); }
    }

    /* Espacio reservado donde aterriza la carta (.card-fly vuela hasta aquí). */
    .card-slot {
      height: 272px;
      width: 100%;
      margin-top: 8px;
      flex-shrink: 0;
    }

    .reveal-line {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.62rem;
      color: var(--text-primary);
      line-height: 1.8;
      text-align: center;
      margin: 0;
      opacity: 0;
      transform: translateY(14px);
      transition: opacity 0.4s ease-out, transform 0.4s ease-out;
    }
    .stage-modal .reveal-line {
      opacity: 1;
      transform: translateY(0);
    }
    .line-discovery {
      margin-top: 14px;
      transition-delay: 0.55s;
    }
    .card-name {
      margin-top: 10px;
      font-size: 0.72rem;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: var(--neon-cyan);
      text-shadow: 0 0 12px rgba(0, 170, 255, 0.7);
      transition-delay: 0.7s;
    }
    .line-belongs {
      margin-top: 10px;
      font-size: 0.7rem;
      color: #ffd700;
      text-shadow: 0 0 12px rgba(255, 215, 0, 0.6);
      transition-delay: 0.85s;
    }

    .continue-wrapper {
      margin-top: auto;
      opacity: 0;
      transform: translateY(14px) scale(0.85);
      transition: opacity 0.4s ease-out 1.05s, transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) 1.05s;
    }
    .stage-modal .continue-wrapper {
      opacity: 1;
      transform: translateY(0) scale(1);
    }

    /* ── Confeti a pantalla completa ────────────────────────────── */
    .confetti-layer {
      position: fixed;
      inset: 0;
      overflow: hidden;
      pointer-events: none;
    }
    .layer-back { z-index: 2001; }
    .layer-front { z-index: 2010; }
    .confetti-piece {
      position: absolute;
      top: -6vh;
      left: var(--x);
      width: var(--w);
      height: var(--h);
      background: var(--c);
      border-radius: var(--br);
      opacity: 0;
      animation: confetti-rain var(--dur) cubic-bezier(0.3, 0.1, 0.6, 1) var(--delay) infinite;
      will-change: transform;
    }
    .layer-back .confetti-piece { filter: brightness(0.6); }
    @keyframes confetti-rain {
      0% {
        transform: translate3d(0, 0, 0) rotateZ(0deg) rotateX(0deg);
        opacity: 1;
      }
      90% { opacity: 1; }
      100% {
        transform: translate3d(var(--sway), 112vh, 0) rotateZ(var(--rot)) rotateX(900deg);
        opacity: 0.6;
      }
    }

    /* Pantallas bajas: panel y carta más compactos (misma geometría relativa). */
    @media (max-height: 720px) {
      .modal-panel {
        height: 500px;
        padding: 22px 26px;
      }
      .reveal-title {
        font-size: 0.85rem;
        min-height: 52px;
      }
      .card-slot { height: 230px; }
      .stage-modal .card-fly {
        transform: translate(-50%, calc(-50% - 53px)) rotateY(0deg) scale(0.65);
      }
    }
  `],
})
export class SecretCardRevealComponent implements OnInit, OnDestroy {
  private readonly sfx = inject(AudioSfxChannel);

  /** Nombre de la carta, mostrado en mayúsculas dentro del panel. */
  readonly cardName = input.required<string>();
  /** URL de la imagen de la carta (bucket público de MinIO vía backend). */
  readonly imageUrl = input.required<string>();

  readonly closed = output<void>();

  readonly stage = signal<RevealStage>('spin');

  /** Duración del giro (1.2s) + flash/onda antes de armar el modal. */
  private static readonly SPIN_DURATION_MS = 1450;
  private stageTimer: ReturnType<typeof setTimeout> | null = null;

  private static readonly CONFETTI_COLORS = ['#ffd700', '#ff50c8', '#50c8ff', '#a0ff50', '#ff8c50', '#ffffff', '#9b59b6'];

  readonly sparkles = Array.from({ length: 12 }, () => ({
    x: (Math.random() - 0.5) * 300,
    y: (Math.random() - 0.5) * 380,
    delay: Math.random() * 1400,
  }));

  readonly confetti = SecretCardRevealComponent.makeConfetti(110);
  readonly confettiBack = SecretCardRevealComponent.makeConfetti(40);

  private static makeConfetti(count: number) {
    return Array.from({ length: count }, () => {
      const round = Math.random() < 0.25;
      const w = 6 + Math.random() * 8;
      return {
        x: Math.random() * 100,
        color: this.CONFETTI_COLORS[Math.floor(Math.random() * this.CONFETTI_COLORS.length)],
        w,
        h: round ? w : w * (1.2 + Math.random()),
        round,
        sway: (Math.random() - 0.5) * 240,
        rot: (Math.random() - 0.5) * 1440,
        dur: 2400 + Math.random() * 1800,
        delay: Math.random() * 1200,
      };
    });
  }

  ngOnInit(): void {
    this.stageTimer = setTimeout(
      () => this.stage.set('modal'),
      SecretCardRevealComponent.SPIN_DURATION_MS,
    );
  }

  ngOnDestroy(): void {
    if (this.stageTimer) {
      clearTimeout(this.stageTimer);
      this.stageTimer = null;
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (this.stage() !== 'modal') return;
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.onContinue();
    }
  }

  onContinue(): void {
    this.sfx.play('NpcBoton.mp3');
    this.closed.emit();
  }
}
