import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';

interface AnimCard {
  id: string;
  startX: number;
  startY: number;
  endX: number;
  endY: number;
  delay: number;
}

interface TargetRect {
  left: number;
  right: number;
  y: number;
}

@Component({
  selector: 'app-mulligan-re-deal',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./mulligan-re-deal.component.css'],
  template: `
    @if (phase() === 'return') {
      <div class="mulligan-overlay">
        <div class="mulligan-badge">{{ 'MATCH.MULLIGAN' | translate }}</div>
          @for (card of animCards(); track card.id) {
          <div
            class="amu-card amu-card--return"
            [style.--start-x]="card.startX + 'px'"
            [style.--start-y]="card.startY + 'px'"
            [style.--end-x]="card.endX + 'px'"
            [style.--end-y]="card.endY + 'px'"
            [style.animation-delay.ms]="card.delay"
          >
            @if (cardBackUrl()) {
              <img [src]="cardBackUrl()" alt="" class="card-back-img" />
            } @else {
              <div class="card-back"></div>
            }
          </div>
        }
      </div>
    }

    @if (phase() === 'shuffle') {
      <div class="mulligan-overlay">
        <div class="shuffle-stage">
          <div class="shuffle-deck">
            @for (_ of [1,2,3,4,5]; track $index) {
              <div class="shuffle-card" [style.animation-delay.ms]="$index * 70">
                @if (cardBackUrl()) {
                  <img [src]="cardBackUrl()" alt="" class="card-back-img" />
                } @else {
                  <div class="card-back"></div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }

    @if (phase() === 'deal') {
      <div class="mulligan-overlay">
        <div class="mulligan-badge">{{ 'MATCH.MULLIGAN' | translate }}</div>
        @for (card of animCards(); track card.id) {
          <div
            class="amu-card amu-card--deal"
            [style.--start-x]="card.startX + 'px'"
            [style.--start-y]="card.startY + 'px'"
            [style.--end-x]="card.endX + 'px'"
            [style.--end-y]="card.endY + 'px'"
            [style.animation-delay.ms]="card.delay"
          >
            @if (cardBackUrl()) {
              <img [src]="cardBackUrl()" alt="" class="card-back-img" />
            } @else {
              <div class="card-back"></div>
            }
          </div>
        }
      </div>
    }
  `,
})
export class MulliganReDealComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly sfx = inject(AudioSfxChannel);

  readonly iterations = input(0);
  readonly handSize = input(0);
  readonly cardBackUrl = input<string | undefined>(undefined);

  readonly done = output<void>();

  readonly phase = signal<'idle' | 'return' | 'shuffle' | 'deal' | 'done'>('idle');
  readonly animCards = signal<AnimCard[]>([]);

  private started = false;
  private stagger = 200;

  constructor() {
    effect(() => {
      const iter = this.iterations();
      if (iter <= 0) return;
      if (this.started) return;
      this.started = true;
      this.runCycle(iter);
    });
  }

  private runCycle(remaining: number): void {
    this.showReturn(remaining);
  }

  private showReturn(remaining: number): void {
    this.phase.set('return');
    const cards = this.buildReturnCards();
    this.animCards.set(cards);
    const timer = setTimeout(() => {
      this.showShuffle(remaining);
    }, 600);
    this.destroyRef.onDestroy(() => clearTimeout(timer));
  }

  private showShuffle(remaining: number): void {
    this.phase.set('shuffle');
    const timer = setTimeout(() => {
      this.showDeal(remaining);
    }, 900);
    this.destroyRef.onDestroy(() => clearTimeout(timer));
  }

  private showDeal(remaining: number): void {
    this.phase.set('deal');
    const cards = this.buildDealCards();
    this.animCards.set(cards);

    for (const card of cards) {
      const t = setTimeout(() => {
        this.sfx.play('TomarCarta.mp3');
      }, card.delay);
      this.destroyRef.onDestroy(() => clearTimeout(t));
    }

    const maxDelay = cards.reduce((m, c) => Math.max(m, c.delay), 0);
    const total = maxDelay + 600 + 150;
    const timer = setTimeout(() => {
      const next = remaining - 1;
      if (next > 0) {
        this.runCycle(next);
      } else {
        this.phase.set('done');
        this.done.emit();
      }
    }, total);
    this.destroyRef.onDestroy(() => clearTimeout(timer));
  }

  private buildReturnCards(): AnimCard[] {
    const startTarget = this.getTargetRect('.hand-cards-row--player');
    const end = this.getCenter('.battle-row--player .deck-area');
    if (!startTarget || !end) return [];
    const cards: AnimCard[] = [];
    const count = this.handSize();
    for (let i = 0; i < count; i++) {
      cards.push({
        id: `return-${i}`,
        startX: this.calculateSpreadX(startTarget, i, count),
        startY: startTarget.y,
        endX: end.x,
        endY: end.y,
        delay: 0,
      });
    }
    return cards;
  }

  private buildDealCards(): AnimCard[] {
    const start = this.getCenter('.battle-row--player .deck-area');
    const endTarget = this.getTargetRect('.hand-cards-row--player');
    if (!start || !endTarget) return [];
    const cards: AnimCard[] = [];
    const count = this.handSize();
    for (let i = 0; i < count; i++) {
      cards.push({
        id: `deal-${i}`,
        startX: start.x,
        startY: start.y,
        endX: this.calculateSpreadX(endTarget, i, count),
        endY: endTarget.y,
        delay: i * this.stagger,
      });
    }
    return cards;
  }

  private getTargetRect(selector: string): TargetRect | null {
    const el = document.querySelector(selector);
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    return {
      left: rect.left,
      right: rect.right,
      y: rect.top + rect.height / 2,
    };
  }

  private calculateSpreadX(target: TargetRect, index: number, total: number): number {
    const cardWidth = 80;
    const gap = 70;
    if (total <= 1) {
      return target.left + (target.right - target.left) / 2;
    }
    const totalSpread = (total - 1) * gap;
    const startX = target.left + (target.right - target.left) / 2 - totalSpread / 2;
    return startX + index * gap;
  }

  private getCenter(selector: string): { x: number; y: number } | null {
    const el = document.querySelector(selector);
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
  }
}
