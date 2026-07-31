import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { InPlayPokemonView } from '../../services/game-state.service';
import { TranslateModule } from '@ngx-translate/core';

const ENERGY_COLORS: Record<string, string> = {
  Colorless: '#a8a878',
  Fire: '#f08030',
  Water: '#6890f0',
  Lightning: '#f8d030',
  Grass: '#78c850',
  Psychic: '#f85888',
  Fighting: '#c03028',
  Darkness: '#705848',
  Metal: '#b8b8d0',
  Dragon: '#7038f8',
  Fairy: '#ee99ac',
};

const STATUS_ICONS: Record<string, string> = {
  POISONED: '☠️',
  BURNED: '🔥',
  ASLEEP: '💤',
  PARALYZED: '⚡',
  CONFUSED: '😵',
};

const BENCH_SIZE = 5;

@Component({
  selector: 'app-bench',
  imports: [TranslateModule],
  templateUrl: './bench.component.html',
  styleUrl: './bench.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenchComponent {
  readonly pokemon = input<ReadonlyArray<InPlayPokemonView | null>>([]);
  readonly isOwnBench = input(false);

  readonly benchPokemonSelected = output<number>();
  readonly openToolDetail = output<{ name: string; imageUrl: string; rules?: string[] }>();

  readonly slots = computed(() => {
    const arr = this.pokemon();
    const result: (InPlayPokemonView | null)[] = [];
    for (let i = 0; i < BENCH_SIZE; i++) {
      result.push(i < arr.length ? arr[i] : null);
    }
    return result;
  });

  onSlotClick(index: number): void {
    if (!this.isOwnBench()) {
      return;
    }
    const slot = this.slots()[index];
    if (!slot) {
      return;
    }
    this.benchPokemonSelected.emit(index);
  }

  onToolRightClick(event: MouseEvent, name: string, imageUrl: string, rules?: string[]): void {
    event.preventDefault();
    event.stopPropagation();
    this.openToolDetail.emit({ name, imageUrl, rules });
  }

  hpPercent(mon: InPlayPokemonView): number {
    if (mon.maxHp <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((mon.currentHp / mon.maxHp) * 100));
  }

  hpColor(mon: InPlayPokemonView): string {
    const pct = this.hpPercent(mon);
    if (pct > 50) {
      return '#22c55e';
    }
    if (pct > 20) {
      return '#eab308';
    }
    return '#ef4444';
  }

  energyBg(type: string): string {
    return ENERGY_COLORS[type] ?? '#a8a878';
  }

  statusIcon(status: string): string {
    return STATUS_ICONS[status] ?? '❓';
  }
}
