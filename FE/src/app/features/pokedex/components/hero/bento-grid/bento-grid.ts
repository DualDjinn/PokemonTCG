import { Component, output } from '@angular/core';
import { BentoCard } from './bento-card/bento-card';
import type { BentoCard as BentoCardModel } from '../../../domain/models/bento-card.model';

export const POKESHOP_BENTO_CARDS: BentoCardModel[] = [
  {
    title: 'Sobres',
    description: 'Abrí sobres booster y descubrí cartas raras y holográficas.',
    icon: 'pack',
    span: 'normal',
    badge: 'Nuevos',
    glow: 'from-violet-500/40',
    sectionKey: 'sobres',
  },
  {
    title: 'Decks',
    description: 'Mazos de 60 cartas listos para competir en batallas.',
    icon: 'deckbox',
    span: 'normal',
    badge: 'Próximamente',
    glow: 'from-emerald-500/40',
    sectionKey: 'decks',
  },
  {
    title: 'Singles',
    description: 'Cartas individuales de todos los tipos y rarezas.',
    icon: 'sleeves',
    span: 'normal',
    badge: 'Catálogo',
    glow: 'from-cyan-500/40',
    sectionKey: 'singles',
  },
  {
    title: 'Accesorios',
    description: 'Sleeves personalizados, reversos de cartas, tapetes y Full Arts exclusivos.',
    words: ['Full Arts Exclusivos', 'Reversos de Cartas', 'Sleeves Personalizados'],
    icon: 'star',
    span: 'full',
    glow: 'from-amber-500/40',
    sectionKey: 'accesorios',
  },
];

@Component({
  standalone: true,
  selector: 'app-bento-grid',
  imports: [BentoCard],
  templateUrl: './bento-grid.html',
  styleUrl: './bento-grid.scss',
})
export class BentoGrid {
  readonly cards = POKESHOP_BENTO_CARDS;
  readonly openSection = output<'sobres' | 'decks' | 'singles' | 'accesorios'>();

  protected onCardClick(card: BentoCardModel): void {
    if (card.sectionKey) {
      this.openSection.emit(card.sectionKey);
    }
  }
}
