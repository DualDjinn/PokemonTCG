import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatchCardView, MatchPhase } from '../../services/game-state.service';

@Component({
  selector: 'app-table-cards',
  standalone: true,
  templateUrl: './table-cards.component.html',
  styleUrl: './table-cards.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableCardsComponent {
  readonly cards = input.required<MatchCardView[]>();
  readonly currentPhase = input.required<MatchPhase>();
  readonly showCardBacks = input(false);
  readonly cardBackUrl = input<string | undefined>(undefined);
  readonly isOpponentZone = input(false);
  readonly hpMap = input(new Map<string, number>());
  readonly hoverEnabled = input(true);
  readonly highlightedCardIds = input<Set<string>>(new Set());
  readonly hoverTargetIds = input<Set<string>>(new Set());
  readonly evolvingCardId = input<string | null>(null);
  readonly energyMap = input(new Map<string, string[]>());
  readonly damageMap = input(new Map<string, number>());

  readonly cardSelected = output<MatchCardView>();
  readonly openDetail = output<MatchCardView>();

  onCardContextMenu(card: MatchCardView, event: Event): void {
    event.preventDefault();
    this.openDetail.emit(card);
  }

  readonly disabledCardIds = computed<Set<string>>(() => {
    const phase = this.currentPhase();
    const disabled = new Set<string>();

    if (phase !== 'MAIN') {
      for (const card of this.cards()) {
        disabled.add(card.id);
      }
      return disabled;
    }

    for (const card of this.cards()) {
      const supertype = (card.supertype ?? '').toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      if (supertype === 'trainer' || supertype === 'energy') {
        continue;
      }
      if (supertype === 'pokemon') {
        const subtypes = (card.subtypes ?? []).map(s =>
          s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        );
        if (subtypes.includes('basic')) {
          continue;
        }
      }
      disabled.add(card.id);
    }
    return disabled;
  });

  onCardClick(card: MatchCardView): void {
    if (this.disabledCardIds().has(card.id)) {
      return;
    }
    this.cardSelected.emit(card);
  }

  isCardDisabled(card: MatchCardView): boolean {
    return this.disabledCardIds().has(card.id);
  }

  groupEnergies(energies: string[]): { type: string; count: number }[] {
    const map = new Map<string, number>();
    for (const e of energies) {
      map.set(e, (map.get(e) ?? 0) + 1);
    }
    return [...map.entries()].map(([type, count]) => ({ type, count }));
  }

  energyImage(type: string): string {
    const name = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
    return `assets/energies/${name}.png`;
  }

  /** Porcentaje de HP de una carta del banco (máximo = HP actual + daño). */
  hpBarPercent(cardId: string): number {
    const hp = this.hpMap().get(cardId);
    if (hp === undefined) {
      return 0;
    }
    const max = hp + (this.damageMap().get(cardId) ?? 0);
    if (max <= 0) {
      return 0;
    }
    return Math.max(0, Math.min(100, Math.round((hp / max) * 100)));
  }

  /** Clase de color de la barra: alto (>50%), medio (20–50%), bajo (<20%). */
  hpBarClass(cardId: string): string {
    const p = this.hpBarPercent(cardId);
    return p > 50 ? 'hp-high' : p > 20 ? 'hp-medium' : 'hp-low';
  }

  /** Etiqueta de daño acumulado (`-N`) o vacío si no hay daño. */
  damageLabel(cardId: string): string {
    const d = this.damageMap().get(cardId) ?? 0;
    return d > 0 ? `-${d}` : '';
  }
}
