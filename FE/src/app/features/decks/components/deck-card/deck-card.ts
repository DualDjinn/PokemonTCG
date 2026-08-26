import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DeckListItem } from '../../domain/models/deck.models';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MyIconButton } from '../../../../shared/ui/my-icon-button/my-icon-button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-deck-card',
  standalone: true,
  imports: [MyText, MyBadge, MyIconButton, TranslateModule],
  template: `
    @if (isCreateCard()) {
      <button type="button" class="deck-card-btn flex flex-col gap-2 items-center justify-center w-full h-full p-4 rounded-2xl border-2 border-dashed border-white/20 cursor-pointer text-left font-inherit box-border min-h-[100px] bg-white/5 backdrop-blur hover:border-[#0284c7] hover:bg-white/10 transition-all duration-200 group" (click)="create.emit()">
        <span class="text-2xl text-[#0284c7] leading-none transition-transform duration-200 group-hover:scale-125">+</span>
        <my-text [text]="'DECK_CARD.CREATE_DECK' | translate" tag="span" size="0.9rem" weight="bold" color="#ffffff" align="center"></my-text>
      </button>
    } @else {
      <button
        type="button"
        class="deck-card-btn relative flex flex-col gap-2 w-full h-full p-4 rounded-2xl border bg-white/5 backdrop-blur cursor-pointer text-left font-inherit box-border"
        [class.border-white/20]="!selected()"
        [class.border-[#0284c7]]="selected()"
        [class.shadow-[0_0_20px_rgba(2,132,199,0.35)]]="selected()"
        (click)="selectDeck()"
        [attr.aria-label]="'DECK_CARD.SELECT_DECK' | translate:{name: deck().name}"
      >
        <div class="flex justify-between items-center gap-2">
          <my-text [text]="deck().name" tag="span" size="0.9rem" weight="bold" color="var(--text-primary)"></my-text>
          <my-icon-button
            [icon]="isFavorite() ? '★' : '☆'"
            [color]="isFavorite() ? 'var(--neon-magenta)' : '#cbd5e1'"
            variant="modern"
            size="small"
            [ariaLabel]="isFavorite() ? ('DECK_CARD.REMOVE_FAVORITE' | translate) : ('DECK_CARD.ADD_FAVORITE' | translate)"
            (clicked)="toggleFav($event)"
          ></my-icon-button>
        </div>

        <div class="flex justify-between items-center gap-2">
          <span
            class="px-2 py-0.5 rounded-full text-xs font-bold border leading-tight"
            [style.background]="deck().valid ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)'"
            [style.color]="deck().valid ? '#4ade80' : '#f87171'"
            [style.borderColor]="deck().valid ? 'rgba(34,197,94,0.4)' : 'rgba(239,68,68,0.4)'"
          >{{ deck().cardCount }} / 60</span>
          @if (deck().valid) {
            <my-badge [text]="'DECK_CARD.VALID' | translate" color="var(--success-color, #22c55e)"></my-badge>
          } @else {
            <my-badge [text]="'DECK_CARD.INVALID' | translate" color="var(--error-color, #ef4444)"></my-badge>
          }
        </div>
      </button>
    }
  `,
  styleUrl: './deck-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckCard {
  readonly deck = input.required<DeckListItem>();
  readonly selected = input(false);
  readonly isCreateCard = input(false);
  readonly isFavorite = input(false);

  readonly selectedChange = output<string>();
  readonly toggleFavorite = output<string>();
  readonly create = output<void>();

  selectDeck(): void {
    this.selectedChange.emit(this.deck().id);
  }

  toggleFav(event: MouseEvent): void {
    event.stopPropagation();
    this.toggleFavorite.emit(this.deck().id);
  }
}
