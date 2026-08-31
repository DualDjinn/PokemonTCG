import { Component, output } from '@angular/core';
import { ShinyTitle } from '../../shared/shiny-title/shiny-title';
import { EnergyIcon } from '../../shared/energy-icon/energy-icon';

export interface PlaceholderDeck {
  name: string;
  type: string;
  price: number;
  winRate: string;
  description: string;
  accentColor: string;
}

export const PLACEHOLDER_DECKS: PlaceholderDeck[] = [
  {
    name: 'Deck Dragón Carmesí',
    type: 'fire',
    price: 3500,
    winRate: '61%',
    description: '60 cartas listas para competir. Ataque explosivo y aceleración de energía.',
    accentColor: '#ff6b4a',
  },
  {
    name: 'Deck Tidal Force',
    type: 'water',
    price: 3200,
    winRate: '57%',
    description: 'Control de tablero con marea alta y disrupción constante.',
    accentColor: '#46c2f0',
  },
  {
    name: 'Deck Volt Combo',
    type: 'lightning',
    price: 3600,
    winRate: '63%',
    description: 'Combos rápidos y remates eléctricos de alto voltaje.',
    accentColor: '#ffe066',
  },
  {
    name: 'Deck Psique Astral',
    type: 'psychic',
    price: 3400,
    winRate: '59%',
    description: 'Estrategia de bloqueo y manipulación de contadores de daño.',
    accentColor: '#c77dff',
  },
];

@Component({
  standalone: true,
  selector: 'app-decks-section',
  imports: [ShinyTitle, EnergyIcon],
  templateUrl: './decks-section.html',
  styleUrl: './decks-section.scss',
})
export class DecksSection {
  readonly back = output<void>();
  readonly decks = PLACEHOLDER_DECKS;
}
