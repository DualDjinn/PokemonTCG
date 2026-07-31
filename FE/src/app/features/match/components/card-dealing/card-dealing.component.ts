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
import { MatchCardView } from '../../services/game-state.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';

interface FlyingCard {
  id: string;
  startX: number;
  startY: number;
  endX: number;
  endY: number;
  delay: number;
  flip: boolean;
  imageUrl?: string;
}

interface TargetRect {
  left: number;
  right: number;
  y: number;
}

@Component({
  selector: 'app-card-dealing',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./card-dealing.component.css'],
  template: `
    <div class="dealing-overlay">
      @if (phase() === 'shuffle') {
        <div class="shuffle-stage">
          <div class="shuffle-deck">
            @for (_ of [1,2,3,4,5]; track $index) {
              <div class="shuffle-card" [style.animation-delay.ms]="$index * 70">
                @if (cardBackUrl()) {
                  <img [src]="cardBackUrl()" alt="" class="shuffle-card-img" />
                } @else {
                  <div class="card-back"></div>
                }
              </div>
            }
          </div>
        </div>
      }

      @if (phase() === 'deal') {
        <div class="deal-stage">
          @for (card of flyingCards(); track card.id) {
            <div
              class="flying-card"
              [class.flying-card--player]="card.flip"
              [class.flying-card--fading]="fading()"
              [style.--start-x]="card.startX + 'px'"
              [style.--start-y]="card.startY + 'px'"
              [style.--end-x]="card.endX + 'px'"
              [style.--end-y]="card.endY + 'px'"
              [style.animation-delay.ms]="card.delay"
            >
              @if (card.flip && card.imageUrl) {
                <img [src]="card.imageUrl" alt="" class="flying-card__image" />
              } @else if (cardBackUrl()) {
                <img [src]="cardBackUrl()" alt="" class="flying-card__image" />
              } @else {
                <div class="flying-card__back"></div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class CardDealingComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly sfx = inject(AudioSfxChannel);

  readonly myCards = input.required<MatchCardView[]>();
  readonly opponentCount = input.required<number>();
  readonly cardBackUrl = input<string | undefined>(undefined);

  readonly dealt = output<void>();
  readonly firstDealDone = output<void>();

  readonly phase = signal<'idle' | 'shuffle' | 'deal' | 'done'>('idle');
  readonly flyingCards = signal<FlyingCard[]>([]);
  readonly fading = signal(false);

  private started = false;
  private firstDealEmitted = false;
  private cardDuration = 600;
  private stagger = 200;

  constructor() {
    effect(() => {
      const myCards = this.myCards();
      const oppCount = this.opponentCount();
      if (myCards.length === 0 && oppCount === 0) return;
      if (this.started) return;

      this.started = true;
      this.firstDealEmitted = false;

      this.showShuffle();
    });
  }

  private showShuffle(): void {
    this.phase.set('shuffle');
    this.fading.set(false);
    const timer = setTimeout(() => this.showDeal(), 900);
    this.destroyRef.onDestroy(() => clearTimeout(timer));
  }

  private showDeal(): void {
    this.phase.set('deal');
    const cards = this.buildFlyingCards();
    this.flyingCards.set(cards);

    for (const card of cards) {
      const t = setTimeout(() => {
        this.sfx.play('TomarCarta.mp3');
      }, card.delay);
      this.destroyRef.onDestroy(() => clearTimeout(t));
    }

    const maxDelay = cards.reduce((m, c) => Math.max(m, c.delay), 0);
    const total = maxDelay + this.cardDuration + 150;
    const fadeDelay = total - 300;
    if (fadeDelay > 0) {
      const fadeTimer = setTimeout(() => this.fading.set(true), fadeDelay);
      this.destroyRef.onDestroy(() => clearTimeout(fadeTimer));
    }
    const timer = setTimeout(() => {
      if (!this.firstDealEmitted) {
        this.firstDealEmitted = true;
        this.firstDealDone.emit();
      }
      this.phase.set('done');
      this.dealt.emit();
    }, total);
    this.destroyRef.onDestroy(() => clearTimeout(timer));
  }

  private buildFlyingCards(): FlyingCard[] {
    const cards: FlyingCard[] = [];
    const oppStart = this.getCenter('.battle-row--opponent .deck-area');
    const ownStart = this.getCenter('.battle-row--player .deck-area');
    const oppTarget = this.getTargetRect('.hand-cards-row--opponent');
    const ownTarget = this.getTargetRect('.hand-cards-row--player');

    const oppCount = this.opponentCount();
    if (oppStart && oppTarget) {
      for (let i = 0; i < oppCount; i++) {
        cards.push({
          id: `opp-deal-${i}`,
          startX: oppStart.x,
          startY: oppStart.y,
          endX: this.calculateSpreadX(oppTarget, i, oppCount),
          endY: oppTarget.y,
          delay: i * this.stagger,
          flip: false,
        });
      }
    }

    const myCards = this.myCards();
    const myCount = myCards.length;
    if (ownStart && ownTarget) {
      for (let i = 0; i < myCount; i++) {
        cards.push({
          id: `my-deal-${myCards[i].id}`,
          startX: ownStart.x,
          startY: ownStart.y,
          endX: this.calculateSpreadX(ownTarget, i, myCount),
          endY: ownTarget.y,
          delay: i * this.stagger,
          flip: true,
          imageUrl: myCards[i].imageUrl,
        });
      }
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
