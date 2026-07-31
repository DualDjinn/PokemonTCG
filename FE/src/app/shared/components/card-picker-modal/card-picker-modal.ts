import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { MyModal } from '../my-modal/my-modal';
import { MatchCardView } from '../../../features/match/services/game-state.service';

/**
 * Generic picker modal: shows a grid of cards, lets the user pick between
 * {@code minPick} and {@code maxPick} of them, and (optionally) drag-reorder
 * the full set. Emits the picked card ids in pick order on confirm.
 */
@Component({
  selector: 'app-card-picker-modal',
  standalone: true,
  imports: [CommonModule, TranslateModule, MyModal],
  templateUrl: './card-picker-modal.html',
  styleUrl: './card-picker-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardPickerModal {
  @Input() open = false;
  @Input() title = '';
  @Input() cards: MatchCardView[] = [];
  @Input() minPick = 0;
  @Input() maxPick = 1;
  @Input() allowReorder = false;
  @Input() prompt: string | null = null;

  @Output() confirm = new EventEmitter<string[]>();
  @Output() cancel = new EventEmitter<void>();

  protected readonly pickedIndices = signal<number[]>([]);
  protected readonly orderedCards = signal<MatchCardView[]>([]);

  protected readonly canConfirm = computed(() => {
    const n = this.allowReorder ? this.cards.length : this.pickedIndices().length;
    return n >= this.minPick && n <= this.maxPick;
  });

  protected readonly pickedCount = computed(() => this.pickedIndices().length);

  ngOnChanges(changes: SimpleChanges): void {
    const justOpened =
      changes['open']?.currentValue === true && changes['open']?.previousValue !== true;
    const cardsChanged =
      changes['cards'] && !this.sameCardIds(changes['cards'].previousValue, changes['cards'].currentValue);
    if (justOpened || cardsChanged) {
      this.pickedIndices.set([]);
      this.orderedCards.set([...this.cards]);
    }
  }

  private sameCardIds(a: MatchCardView[] | undefined, b: MatchCardView[] | undefined): boolean {
    if (!a || !b || a.length !== b.length) return false;
    for (let i = 0; i < a.length; i++) {
      if (a[i]?.id !== b[i]?.id) return false;
    }
    return true;
  }

  protected onCardClick(card: MatchCardView, index: number): void {
    if (!card.id) return;
    if (this.allowReorder) return;
    const ids = this.pickedIndices();
    const idx = ids.indexOf(index);
    if (idx >= 0) {
      this.pickedIndices.set(ids.filter((_, i) => i !== idx));
    } else if (ids.length < this.maxPick) {
      this.pickedIndices.set([...ids, index]);
    }
  }

  protected isPicked(index: number): boolean {
    return this.pickedIndices().includes(index);
  }

  protected moveUp(index: number): void {
    if (index <= 0) return;
    const arr = [...this.orderedCards()];
    [arr[index - 1], arr[index]] = [arr[index], arr[index - 1]];
    this.orderedCards.set(arr);
  }

  protected moveDown(index: number): void {
    const arr = [...this.orderedCards()];
    if (index >= arr.length - 1) return;
    [arr[index], arr[index + 1]] = [arr[index + 1], arr[index]];
    this.orderedCards.set(arr);
  }

  protected onConfirm(): void {
    if (!this.canConfirm()) return;
    const result = this.allowReorder
      ? this.orderedCards().map(c => c.id!).filter(Boolean)
      : this.pickedIndices().map(i => this.cards[i]?.id).filter(Boolean);
    this.confirm.emit(result);
  }

  protected onCancel(): void {
    this.cancel.emit();
  }
}
