import {
  Component,
  computed,
  inject,
  input,
  OnInit,
  output,
  signal,
  type OnDestroy,
} from '@angular/core';
import type { BentoCard as BentoCardModel } from '../../../../domain/models/bento-card.model';
import { CardService } from '../../../../data-access/services/card.service';

@Component({
  standalone: true,
  selector: 'app-bento-card',
  imports: [],
  templateUrl: './bento-card.html',
  styleUrl: './bento-card.scss',
})
export class BentoCard implements OnInit, OnDestroy {
  readonly card = input.required<BentoCardModel>();
  readonly open = output<BentoCardModel>();

  private readonly cardService = inject(CardService);
  readonly randomSinglesCards = signal<{ name: string; imageUrl: string }[]>([
    { name: 'Charizard EX', imageUrl: 'https://images.pokemontcg.io/xy1/1.png' },
    { name: 'Blastoise EX', imageUrl: 'https://images.pokemontcg.io/xy1/29.png' },
    { name: 'Pikachu', imageUrl: 'https://images.pokemontcg.io/xy1/42.png' },
  ]);

  private readonly wordIndex = signal(0);
  private wordTimer: ReturnType<typeof setInterval> | null = null;

  protected readonly currentWord = computed(() => {
    const words = this.card().words;
    if (!words || words.length === 0) return '';
    return words[this.wordIndex() % words.length];
  });

  constructor() {
    this.wordTimer = setInterval(() => {
      const words = this.card().words;
      if (words && words.length > 1) {
        this.wordIndex.update((i) => (i + 1) % words.length);
      }
    }, 2400);
  }

  ngOnInit(): void {
    if (this.card().sectionKey === 'singles') {
      this.cardService.getCards().subscribe({
        next: (cards) => {
          if (cards && cards.length >= 3) {
            // Mezclar aleatoriamente las cartas del catálogo y seleccionar 3
            const shuffled = [...cards].sort(() => Math.random() - 0.5);
            const picked = shuffled.slice(0, 3).map((c) => ({
              name: c.name,
              imageUrl:
                c.images?.small ||
                c.images?.large ||
                `https://images.pokemontcg.io/${c.id.replace('-', '/')}.png`,
            }));
            this.randomSinglesCards.set(picked);
          }
        },
        error: () => {
          // Mantener defaults en caso de error de red
        },
      });
    }
  }

  ngOnDestroy(): void {
    if (this.wordTimer) {
      clearInterval(this.wordTimer);
    }
  }

  protected navegar(e: MouseEvent): void {
    e.preventDefault();
    this.open.emit(this.card());
  }
}
