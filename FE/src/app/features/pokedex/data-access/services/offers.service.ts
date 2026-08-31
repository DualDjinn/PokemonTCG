import { Injectable, inject, signal } from '@angular/core';
import { CardService } from './card.service';
import { getPrice } from '../../domain/models/cart.model';
import { Card } from '../../domain/models/card.model';
import { PackType } from '../../domain/models/pack.model';

export interface ShopMarqueeItem {
  id: string;
  name: string;
  itemType: 'pack' | 'card' | 'splashArt' | 'cardBack';
  oldPrice: number;
  price: number;
  discountPercent: number;
  type: string;
  highlight: string;
  imageUrl?: string;
  rarity?: string;
  cardData?: Card;
  packDescription?: string;
}

export interface EffectivePriceInfo {
  originalPrice: number;
  price: number;
  discountPercent: number;
  highlight: string;
  hasDiscount: boolean;
}

const INITIAL_OFFERS: ShopMarqueeItem[] = [
  { id: 'pack-bronze', name: 'Sobre Base', itemType: 'pack', oldPrice: 100, price: 85, discountPercent: 15, type: 'colorless', highlight: '-15%', imageUrl: '/assets/animation_store/base_16bit.png', packDescription: '5 cartas · Común 60% · Infrecuente 30% · Rara 10%', rarity: 'BRONZE' },
  { id: 'pack-silver', name: 'Sobre Expansión', itemType: 'pack', oldPrice: 250, price: 200, discountPercent: 20, type: 'metal', highlight: '-20%', imageUrl: '/assets/animation_store/plata_16bit.png', packDescription: '5 cartas · Común 50% · Infrecuente 35% · Rara 15%', rarity: 'SILVER' },
  { id: 'pack-gold', name: 'Sobre Premium', itemType: 'pack', oldPrice: 500, price: 400, discountPercent: 20, type: 'lightning', highlight: '-20%', imageUrl: '/assets/animation_store/oro_16bit.png', packDescription: '5 cartas · Común 40% · Infrecuente 35% · Rara 25%', rarity: 'GOLD' },
  { id: 'pack-diamond', name: 'Sobre Legendario EX', itemType: 'pack', oldPrice: 2000, price: 1600, discountPercent: 20, type: 'psychic', highlight: '-20%', imageUrl: '/assets/animation_store/diamante_16bit.png', packDescription: '1 carta Holo EX legendaria garantizada.', rarity: 'DIAMOND' },
  { id: 'xy1-1', name: 'Venusaur-EX', itemType: 'card', oldPrice: 1200, price: 960, discountPercent: 20, type: 'grass', highlight: '-20%', rarity: 'Rare Holo EX' },
  { id: 'xy1-29', name: 'Blastoise-EX', itemType: 'card', oldPrice: 1200, price: 1020, discountPercent: 15, type: 'water', highlight: '-15%', rarity: 'Rare Holo EX' },
  { id: 'xy1-42', name: 'Pikachu', itemType: 'card', oldPrice: 300, price: 255, discountPercent: 15, type: 'lightning', highlight: '-15%', rarity: 'Common' },
  { id: 'xy1-79', name: 'Yveltal-EX', itemType: 'card', oldPrice: 1200, price: 900, discountPercent: 25, type: 'darkness', highlight: '-25%', rarity: 'Rare Holo EX' },
  { id: 'xy1-96', name: 'Xerneas-EX', itemType: 'card', oldPrice: 1200, price: 900, discountPercent: 25, type: 'fairy', highlight: '-25%', rarity: 'Rare Holo EX' },
  { id: 'xy1-80', name: 'Skarmory-EX', itemType: 'card', oldPrice: 1200, price: 1080, discountPercent: 10, type: 'metal', highlight: '-10%', rarity: 'Rare Holo EX' },
];

@Injectable({ providedIn: 'root' })
export class StoreOffersService {
  private readonly cardService = inject(CardService);

  readonly activeOffers = signal<ShopMarqueeItem[]>(INITIAL_OFFERS);

  constructor() {
    this.initOffers();
  }

  private initOffers(): void {
    this.cardService.getCards().subscribe({
      next: (cards) => {
        this.generateOffers(cards);
      },
      error: () => {
        // Keep initial offers
      },
    });
  }

  generateOffers(cards: Card[]): void {
    const candidatePool: Array<{
      id: string;
      name: string;
      itemType: 'pack' | 'card' | 'splashArt' | 'cardBack';
      oldPrice: number;
      type: string;
      imageUrl?: string;
      rarity?: string;
      cardData?: Card;
      packDescription?: string;
    }> = [];

    // Add booster pack candidates
    candidatePool.push(...INITIAL_OFFERS.filter((o) => o.itemType === 'pack'));

    // Add real single card candidates
    if (cards && cards.length > 0) {
      for (const card of cards) {
        const rawType = card.types?.[0]?.toLowerCase() || (card.supertype === 'Energy' ? 'colorless' : 'colorless');
        const oldPrice = getPrice(card.rarity);
        candidatePool.push({
          id: card.id,
          name: card.name,
          itemType: 'card',
          oldPrice,
          type: rawType,
          imageUrl: card.images?.large || card.images?.small || '',
          rarity: card.rarity,
          cardData: card,
        });
      }
    }

    // Shuffle and pick 10 items
    const shuffled = [...candidatePool].sort(() => 0.5 - Math.random());
    const selected = shuffled.slice(0, 10);

    const generatedOffers: ShopMarqueeItem[] = selected.map((item) => {
      // Random discount between 5% and 25%
      const discountPercent = Math.floor(Math.random() * 21) + 5; // 5 to 25
      const price = Math.max(1, Math.round(item.oldPrice * (1 - discountPercent / 100)));
      return {
        id: item.id,
        name: item.name,
        itemType: item.itemType,
        oldPrice: item.oldPrice,
        price,
        discountPercent,
        type: item.type,
        highlight: `-${discountPercent}%`,
        imageUrl: item.imageUrl,
        rarity: item.rarity,
        cardData: item.cardData,
        packDescription: item.packDescription,
      };
    });

    this.activeOffers.set(generatedOffers);
  }

  getOfferForCard(cardId: string): ShopMarqueeItem | undefined {
    return this.activeOffers().find((o) => o.itemType === 'card' && o.id === cardId);
  }

  getOfferForPack(packType: PackType): ShopMarqueeItem | undefined {
    const rawType = packType.toUpperCase();
    return this.activeOffers().find(
      (o) => o.itemType === 'pack' && (o.rarity?.toUpperCase() === rawType || o.id === `pack-${packType.toLowerCase()}`),
    );
  }

  getEffectiveCardPrice(card: { id: string; rarity: string }): EffectivePriceInfo {
    const offer = this.getOfferForCard(card.id);
    const originalPrice = getPrice(card.rarity);
    if (offer) {
      return {
        originalPrice: offer.oldPrice || originalPrice,
        price: offer.price,
        discountPercent: offer.discountPercent,
        highlight: offer.highlight,
        hasDiscount: true,
      };
    }
    return {
      originalPrice,
      price: originalPrice,
      discountPercent: 0,
      highlight: '',
      hasDiscount: false,
    };
  }

  getEffectivePackPrice(packType: PackType, basePrice: number): EffectivePriceInfo {
    const offer = this.getOfferForPack(packType);
    if (offer) {
      return {
        originalPrice: offer.oldPrice || basePrice,
        price: offer.price,
        discountPercent: offer.discountPercent,
        highlight: offer.highlight,
        hasDiscount: true,
      };
    }
    return {
      originalPrice: basePrice,
      price: basePrice,
      discountPercent: 0,
      highlight: '',
      hasDiscount: false,
    };
  }
}
