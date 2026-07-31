import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  NgZone,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatchCardView } from '../../services/game-state.service';

@Component({
  selector: 'app-hand-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './hand-card.component.css',
  template: `
    <div
      class="hand-card-wrapper"
      [class.is-drag-source]="isDragging()"
      [class.is-selected]="isSelected()"
      [class.is-stack]="count() > 1"
      [class.has-setup-arrow]="showSetupArrow()"
      [class.setup-glow]="glow()"
    >
      @if (showSetupArrow()) {
        <div class="setup-arrow">▲</div>
      }
      @if (displayCount() <= 1) {
        <div class="hand-card">
          @if (card().imageUrl) {
            <img
              [src]="card().imageUrl"
              [alt]="card().name"
              loading="lazy"
              class="hand-card__img"
              draggable="false"
            />
          } @else {
            <span class="hand-card__placeholder">?</span>
          }
        </div>
      } @else {
        <div class="hand-card-stack">
          @for (_ of [1,2,3]; track $index) {
            @if ($index < displayCount() && $index < 3) {
              <div
                class="hand-card-stacked"
                [style.top.px]="$index * -2"
                [style.left.px]="$index * 2"
                [style.zIndex]="3 - $index"
              >
                @if (card().imageUrl) {
                  <img
                    [src]="card().imageUrl"
                    [alt]="card().name"
                    loading="lazy"
                    class="hand-card__img"
                    draggable="false"
                  />
                } @else {
                  <span class="hand-card__placeholder">?</span>
                }
              </div>
            }
          }
          <span class="stack-badge">{{ displayCount() }}</span>
        </div>
      }
    </div>
  `,
})
export class HandCardComponent implements OnInit {
  private readonly ngZone = inject(NgZone);

  readonly card = input.required<MatchCardView>();
  readonly count = input(0);
  readonly isSelected = input(false);
  readonly showSetupArrow = input(false);
  readonly glow = input(false);

  readonly select = output<MatchCardView>();
  readonly openDetail = output<MatchCardView>();
  readonly dragStarted = output<{
    card: MatchCardView;
    clientX: number;
    clientY: number;
    offsetX: number;
    offsetY: number;
  }>();
  readonly dragMoved = output<{ clientX: number; clientY: number }>();
  readonly dragEnded = output<void>();

  readonly isDragging = signal(false);
  readonly stackDragOffset = signal(0);
  readonly displayCount = computed(() => this.count() + this.stackDragOffset());

  private startX = 0;
  private startY = 0;
  private dragThreshold = 6;
  private isPointerDown = false;
  private hostEl: HTMLElement | null = null;
  private boundTouchMove: ((e: TouchEvent) => void) | null = null;

  ngOnInit(): void {
    this.boundTouchMove = (e: TouchEvent) => {
      if (!this.isPointerDown) return;
      const touch = e.touches[0];
      if (!touch) return;
      this.handleMove(touch.clientX, touch.clientY);
    };
    this.ngZone.runOutsideAngular(() => {
      document.addEventListener('touchmove', this.boundTouchMove!, { passive: true });
    });
  }

  @HostListener('mousedown', ['$event'])
  onPointerDown(event: MouseEvent): void {
    if (event.button !== 0) return;
    this.hostEl = event.currentTarget as HTMLElement;
    this.isPointerDown = true;
    this.startX = event.clientX;
    this.startY = event.clientY;
  }

  @HostListener('touchstart', ['$event'])
  onTouchStart(event: TouchEvent): void {
    const touch = event.touches[0];
    if (!touch) return;
    this.hostEl = event.currentTarget as HTMLElement;
    this.isPointerDown = true;
    this.startX = touch.clientX;
    this.startY = touch.clientY;
  }

  @HostListener('document:mousemove', ['$event'])
  onPointerMove(event: MouseEvent): void {
    if (!this.isPointerDown) return;
    this.handleMove(event.clientX, event.clientY);
  }

  private handleMove(clientX: number, clientY: number): void {
    const dx = clientX - this.startX;
    const dy = clientY - this.startY;

    if (!this.isDragging()) {
      if (Math.abs(dx) < this.dragThreshold && Math.abs(dy) < this.dragThreshold) return;

      const el = (this.hostEl?.querySelector('.hand-card-wrapper') as HTMLElement) ?? this.hostEl;
      const rect = el!.getBoundingClientRect();
      const offsetX = clientX - rect.left;
      const offsetY = clientY - rect.top;

      this.isDragging.set(true);
      if (this.count() > 1) this.stackDragOffset.set(-1);

      this.dragStarted.emit({
        card: this.card(),
        clientX,
        clientY,
        offsetX,
        offsetY,
      });
      return;
    }

    this.dragMoved.emit({ clientX, clientY });
  }

  @HostListener('document:mouseup')
  onPointerUp(): void {
    if (!this.isPointerDown) return;
    this.isPointerDown = false;
    this.stackDragOffset.set(0);

    if (this.isDragging()) {
      this.isDragging.set(false);
      this.dragEnded.emit();
    } else {
      this.select.emit(this.card());
    }
  }

  @HostListener('document:touchend')
  onTouchEnd(): void {
    if (!this.isPointerDown) return;
    this.isPointerDown = false;
    this.stackDragOffset.set(0);

    if (this.isDragging()) {
      this.isDragging.set(false);
      this.dragEnded.emit();
    }
  }

  @HostListener('contextmenu', ['$event'])
  onContextMenu(event: Event): void {
    event.preventDefault();
    this.openDetail.emit(this.card());
  }

  ngOnDestroy(): void {
    if (this.boundTouchMove) {
      document.removeEventListener('touchmove', this.boundTouchMove);
    }
  }
}
