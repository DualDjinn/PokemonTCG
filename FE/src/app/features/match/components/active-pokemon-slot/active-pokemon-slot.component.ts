import { ChangeDetectionStrategy, Component, HostListener, computed, input, output } from '@angular/core';
import { InPlayPokemonView } from '../../services/game-state.service';

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

const AURA_COLORS: Record<string, string> = {
  FIRE: '#8b0000',
  GRASS: '#046307',
  WATER: '#5bc0eb',
  LIGHTNING: '#d4a017',
  PSYCHIC: '#9b2d86',
  FIGHTING: '#8b4513',
  DARKNESS: '#4a2d6b',
  METAL: '#7a7a9e',
  DRAGON: '#5b21b6',
  FAIRY: '#d4618c',
  COLORLESS: '#8a8a6e',
};

const STATUS_ICONS: Record<string, string> = {
  POISONED: '☠️',
  BURNED: '🔥',
  ASLEEP: '💤',
  PARALYZED: '⚡',
  CONFUSED: '😵',
};

interface EnergyGroup {
  type: string;
  count: number;
}

@Component({
  selector: 'app-active-pokemon-slot',
  templateUrl: './active-pokemon-slot.component.html',
  styleUrl: './active-pokemon-slot.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivePokemonSlotComponent {
  readonly pokemon = input.required<InPlayPokemonView>();
  readonly highlight = input(false);
  readonly animateEntry = input(false);
  readonly showCardBack = input(false);
  readonly cardBackUrl = input<string | undefined>(undefined);
  readonly isOwn = input(false);

  readonly openDetail = output<InPlayPokemonView>();
  readonly openToolDetail = output<{ name: string; imageUrl: string; rules?: string[] }>();

  @HostListener('contextmenu', ['$event'])
  onContextMenu(event: Event): void {
    event.preventDefault();
    this.openDetail.emit(this.pokemon());
  }

  onToolRightClick(event: MouseEvent, name: string, imageUrl: string, rules?: string[]): void {
    event.preventDefault();
    event.stopPropagation();
    this.openToolDetail.emit({ name, imageUrl, rules });
  }

  readonly damageTaken = computed(() => {
    const mon = this.pokemon();
    if (!mon || mon.maxHp <= mon.currentHp) return 0;
    return mon.maxHp - mon.currentHp;
  });

  /** Porcentaje de HP actual (0–100) para el ancho de la barra. */
  readonly hpPct = computed(() => this.hpPercent(this.pokemon()));

  /** Clase de color de la barra: alto (>50%), medio (20–50%), bajo (<20%). */
  readonly hpClass = computed(() => {
    const p = this.hpPct();
    return p > 50 ? 'hp-high' : p > 20 ? 'hp-medium' : 'hp-low';
  });

  readonly groupedEnergies = computed<EnergyGroup[]>(() => {
    const energies = this.pokemon().attachedEnergies;
    const map = new Map<string, number>();
    for (const e of energies) {
      map.set(e, (map.get(e) ?? 0) + 1);
    }
    return [...map.entries()].map(([type, count]) => ({ type, count }));
  });

  readonly hasTool = computed(() => {
    const name = this.pokemon().toolCardName;
    return !!name && name.length > 0;
  });

  readonly gifPath = computed(() => {
    const gif = this.pokemon().gifUrl;
    if (!gif) return null;
    return `assets/gifs/${gif}`;
  });

  readonly auraColor = computed(() => {
    const type = this.pokemon().pokemonType;
    if (!type) return AURA_COLORS['COLORLESS'];
    return AURA_COLORS[type.toUpperCase()] ?? AURA_COLORS['COLORLESS'];
  });

  energyImage(type: string): string {
    const name = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
    return `assets/energies/${name}.png`;
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
    if (pct >= 25) {
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
