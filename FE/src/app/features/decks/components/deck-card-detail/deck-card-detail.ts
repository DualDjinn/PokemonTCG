import { ChangeDetectionStrategy, Component, signal, input, computed } from '@angular/core';
import { CardSummary } from '../../domain/models/deck.models';

@Component({
  selector: 'app-deck-card-detail',
  standalone: true,
  template: `
    <div class="flex flex-col items-center gap-2">
      <div
        class="card-3d-wrapper w-full max-w-[260px]"
        style="perspective: 600px;"
        (mousemove)="onMouseMove($event)"
        (mouseleave)="onMouseLeave()"
      >
        <div
          class="card-3d-inner w-full rounded-xl shadow-[0_0_20px_rgba(0,170,255,0.15),0_8px_32px_rgba(0,0,0,0.4)]"
          [style.transform]="cardTransform()"
        >
          <div
            class="card-face relative w-full aspect-[2.5/3.5] overflow-hidden rounded-xl border flex items-center justify-center"
            [style.borderColor]="'rgba(255,255,255,0.2)'"
            [class.card-holo]="isHolo()"
          >
            @if (card().imageUrl) {
              <img
                [src]="card().imageUrl"
                [alt]="card().name"
                class="w-full h-full object-contain block"
                (error)="onImgError($event)"
              />
            } @else {
              <div class="w-full h-full flex items-center justify-center text-5xl text-[#00ccaa] bg-gradient-to-br from-[rgba(0,204,170,0.1)] to-[rgba(0,0,0,0.4)]">?</div>
            }
          </div>
        </div>
      </div>
      @if (count() >= 1 && showCount()) {
        <div class="bg-[#0284c7] text-white text-sm font-bold px-3 py-1 rounded-full leading-tight">x{{ count() }}</div>
      }
    </div>
  `,
  styleUrl: './deck-card-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckCardDetail {
  readonly card = input.required<CardSummary>();
  readonly count = input(0);
  readonly showCount = input(true);
  readonly cardTransform = signal('rotateX(0deg) rotateY(0deg) scale(1)');
  readonly isHolo = computed(() => {
    const r = this.card().rarity;
    return r === 'Rare Holo EX' || r === 'Rare Ultra';
  });

  onMouseMove(event: MouseEvent): void {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width;
    const y = (event.clientY - rect.top) / rect.height;
    const rotateX = (y - 0.5) * -15;
    const rotateY = (x - 0.5) * 15;
    this.cardTransform.set(
      `rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.03)`
    );
  }

  onMouseLeave(): void {
    this.cardTransform.set('rotateX(0deg) rotateY(0deg) scale(1)');
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    const parent = img.parentElement;
    if (parent) {
      const placeholder = document.createElement('div');
      placeholder.className = 'card-placeholder';
      placeholder.textContent = '?';
      parent.appendChild(placeholder);
    }
  }
}
