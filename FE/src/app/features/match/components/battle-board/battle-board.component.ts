import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { InPlayPokemonView, MatchCardView } from '../../services/game-state.service';
import { ActivePokemonSlotComponent } from '../active-pokemon-slot/active-pokemon-slot.component';

interface Particle {
  id: number;
  x: number;
  y: number;
  size: number;
  dur: number;
  delay: number;
  dx: number;
  dy: number;
}

const PARTICLE_COLORS: Record<string, string> = {
  FIRE: '#ff5522', WATER: '#2299ff', GRASS: '#55dd55',
  LIGHTNING: '#ffcc00', PSYCHIC: '#cc55ff', FIGHTING: '#cc8844',
  DARKNESS: '#7744aa', METAL: '#aaaacc', FAIRY: '#ff88cc',
  DRAGON: '#8844ff',
};

function colorForType(type: string | null): string {
  if (!type) return '#aabbcc';
  return PARTICLE_COLORS[type.toUpperCase()] ?? '#aabbcc';
}

function generateParticles(count: number): Particle[] {
  const particles: Particle[] = [];
  for (let i = 0; i < count; i++) {
    const angle = Math.random() * Math.PI * 2;
    const dist = 80 + Math.random() * 120;
    particles.push({
      id: i,
      x: 5 + Math.random() * 90,
      y: 5 + Math.random() * 90,
      size: 3 + Math.random() * 4,
      dur: 6 + Math.random() * 6,
      delay: Math.random() * 12,
      dx: Math.cos(angle) * dist,
      dy: Math.sin(angle) * dist,
    });
  }
  return particles;
}

@Component({
  selector: 'app-battle-board',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActivePokemonSlotComponent],
  styleUrl: './battle-board.component.css',
  template: `
    <div class="battle-board">
      <div class="board-bg" [class.board-bg--live]="!isInicialBg()" [style.backgroundImage]="'url(' + bgImage() + ')'"></div>
      @if (!isInicialBg()) {
        @for (p of particles(); track p.id) {
          <div class="board-particle"
            [style.left.%]="p.x"
            [style.top.%]="p.y"
            [style.width.px]="p.size"
            [style.height.px]="p.size"
            [style.background]="particleColor()"
            [style.animationDuration.s]="p.dur"
            [style.animationDelay.s]="p.delay"
            [style.--dx.px]="p.dx"
            [style.--dy.px]="p.dy">
          </div>
        }
      }
      <div class="board-zone board-zone--opponent"
        [class.opponent-hover]="opponentHover()"
        [class.attack-lunge]="attackLunge() === 'opponent'"
        [class.defender-shake]="defenderShake() === 'opponent'">
        <div class="opponent-float" [class.opponent-float--active]="opponentHover()">
          @if (opponentActive(); as mon) {
            <app-active-pokemon-slot 
              [pokemon]="mon" 
              [animateEntry]="opponentAnimateEntry()"
              [showCardBack]="showOpponentCardBack()"
              [cardBackUrl]="opponentCardBackUrl()"
              (openDetail)="openDetail.emit($event)"
              (openToolDetail)="openToolDetail.emit($event)" />
          }
        </div>
      </div>
      <div class="board-divider"></div>
      @if (activeStadium(); as stadium) {
        <div class="board-trainer-card board-trainer-card--stadium" (click)="trainerCardClicked.emit(stadium)">
          <img [src]="stadium.imageUrl" [alt]="stadium.name" class="board-trainer-card__img" />
        </div>
      }
      @if (trainerDisplayCard(); as tdc) {
        <div class="board-trainer-card board-trainer-card--played"
             [class.board-trainer-card--fade-out]="trainerDisplayFading()"
             [class.board-trainer-card--glow]="trainerSlotGlow()"
             data-drop-zone="trainer"
             (click)="trainerCardClicked.emit(tdc.card)">
          <img [src]="tdc.card.imageUrl" [alt]="tdc.card.name" class="board-trainer-card__img" />
        </div>
      } @else if (trainerSlotGlow()) {
        <div class="board-trainer-card board-trainer-card--empty" [class.board-trainer-card--glow]="trainerSlotGlow()" data-drop-zone="trainer">
          <span style="color:#555577;font-size:0.6rem;text-align:center;padding:0.25rem">Entrenador</span>
        </div>
      }
      <div
        class="board-zone board-zone--player"
        [class.can-receive]="canReceive()"
        [class.attack-lunge]="attackLunge() === 'player'"
        [class.defender-shake]="defenderShake() === 'player'"
        [style.opacity]="hidePlayerActive() ? 0 : ''"
        (click)="onPlayerZoneClick()"
      >
        @if (myActive(); as mon) {
          <app-active-pokemon-slot 
            [pokemon]="mon" 
            [highlight]="highlightPlayerZone()"
            [animateEntry]="myAnimateEntry()"
            [isOwn]="true"
            [cardBackUrl]="playerCardBackUrl()"
            [class.is-drop-target]="isDropTarget()"
            (openDetail)="openDetail.emit($event)"
            (openToolDetail)="openToolDetail.emit($event)" />
        } @else if (highlightPlayerZone()) {
          <div class="active-slot-placeholder setup-glow" [class.is-drop-target]="isDropTarget()"></div>
        }
      </div>
    </div>
  `,
})
export class BattleBoardComponent {
  readonly opponentActive = input<InPlayPokemonView | null>(null);
  readonly myActive = input<InPlayPokemonView | null>(null);
  readonly highlightPlayerZone = input(false);
  readonly canReceive = input(false);
  readonly isDropTarget = input(false);
  readonly showOpponentCardBack = input(false);
  readonly opponentCardBackUrl = input<string | undefined>(undefined);
  readonly playerCardBackUrl = input<string | undefined>(undefined);
  readonly opponentHover = input(false);
  readonly attackLunge = input<'player' | 'opponent' | null>(null);
  readonly defenderShake = input<'player' | 'opponent' | null>(null);
  readonly boardBg = input<string | null>(null);
  readonly hidePlayerActive = input(false);
  readonly activeStadium = input<MatchCardView | null>(null);
  readonly trainerDisplayCard = input<{ card: MatchCardView; playerId: string } | null>(null);
  readonly trainerDisplayFading = input(false);
  readonly trainerSlotGlow = input(false);
  readonly zoneClicked = output<void>();
  readonly openDetail = output<InPlayPokemonView>();

  readonly openToolDetail = output<{ name: string; imageUrl: string; rules?: string[] }>();
  readonly trainerCardClicked = output<MatchCardView>();

  readonly myAnimateEntry = signal(false);
  readonly opponentAnimateEntry = signal(false);
  readonly bgImage = signal<string>('/assets/img/board/Inicial.png');
  readonly isInicialBg = computed(() => this.bgImage().includes('Inicial'));
  readonly particleColor = computed(() => colorForType(this.boardBg()));
  readonly particles = computed(() => generateParticles(40));

  private bgImageUrl(type: string | null): string {
    if (!type) return '/assets/img/board/Inicial.png';
    const name = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
    if (name === 'Colorless' || name === 'Psychic') return '/assets/img/board/Inicial.png';
    const jpgTypes = ['Grass', 'Water'];
    const ext = jpgTypes.includes(name) ? '.jpg' : '.png';
    return `/assets/img/board/${name}${ext}`;
  }

  constructor() {
    let prevMyActive: InPlayPokemonView | null | undefined = undefined;
    let prevOpponentActive: InPlayPokemonView | null | undefined = undefined;

    effect(() => {
      const current = this.myActive();
      if (current && (prevMyActive === null || prevMyActive === undefined)) {
        this.myAnimateEntry.set(true);
        setTimeout(() => this.myAnimateEntry.set(false), 650);
      }
      prevMyActive = current;
    });

    effect(() => {
      const current = this.opponentActive();
      if (current && (prevOpponentActive === null || prevOpponentActive === undefined)) {
        this.opponentAnimateEntry.set(true);
        setTimeout(() => this.opponentAnimateEntry.set(false), 650);
      }
      prevOpponentActive = current;
    });

    effect(() => {
      const newType = this.boardBg();
      this.bgImage.set(this.bgImageUrl(newType));
    });
  }

  onPlayerZoneClick(): void {
    this.zoneClicked.emit();
  }
}
