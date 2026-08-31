export type BentoCardSpan = 'normal' | 'wide' | 'tall' | 'full';

export interface BentoCard {
  title: string;
  description?: string;
  words?: string[];
  icon: 'sleeves' | 'star' | 'pack' | 'deckbox';
  span: BentoCardSpan;
  badge?: string;
  glow: string;
  link?: string;
  sectionKey?: 'sobres' | 'decks' | 'singles' | 'accesorios';
}
