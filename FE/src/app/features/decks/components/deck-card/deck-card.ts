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
      <button type="button" class="deck-card-btn flex flex-col gap-2 items-center justify-center w-full h-full p-4 border-4 border-[var(--pixel-border-bottom-right)] border-t-[var(--pixel-border-top-left)] border-l-[var(--pixel-border-top-left)] cursor-pointer text-left font-inherit box-border min-h-[100px] opacity-70 bg-bg-secondary/50 hover:opacity-100" (click)="create.emit()">
        <span class="text-2xl text-[#00ccaa] leading-none">+</span>
        <my-text [text]="'DECK_CARD.CREATE_DECK' | translate" tag="span" size="0.9rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
      </button>
    } @else {
      <button
        type="button"
        class="deck-card-btn relative flex flex-col gap-2 w-full h-full p-4 border-4 border-[var(--pixel-border-bottom-right)] border-t-[var(--pixel-border-top-left)] border-l-[var(--pixel-border-top-left)] cursor-pointer text-left font-inherit box-border bg-bg-secondary/85 backdrop-blur"
        [class.shadow-[0_0_16px_rgba(0,204,170,0.3)]]="selected()"
        (click)="selectDeck()"
        [attr.aria-label]="'DECK_CARD.SELECT_DECK' | translate:{name: deck().name}"
      >
        <div class="flex justify-between items-center gap-2">
          <my-text [text]="deck().name" tag="span" size="0.9rem" weight="bold" color="var(--text-primary)"></my-text>
          <my-icon-button
            [icon]="isFavorite() ? '★' : '☆'"
            [color]="isFavorite() ? 'var(--neon-magenta)' : 'var(--text-secondary)'"
            variant="pixel"
            size="small"
            [ariaLabel]="isFavorite() ? ('DECK_CARD.REMOVE_FAVORITE' | translate) : ('DECK_CARD.ADD_FAVORITE' | translate)"
            (clicked)="toggleFav($event)"
          ></my-icon-button>
        </div>

        <div class="flex justify-between items-center gap-2">
          <my-text [text]="deck().cardCount + ' / 60'" tag="span" size="0.765rem" weight="normal" color="var(--text-secondary)"></my-text>
          @if (deck().valid) {
            <my-badge [text]="'DECK_CARD.VALID' | translate" color="var(--success-color, #22c55e)"></my-badge>
          } @else {
            <my-badge [text]="deck().validationMessage || ('DECK_CARD.INVALID' | translate)" color="var(--error-color, #ef4444)"></my-badge>
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
