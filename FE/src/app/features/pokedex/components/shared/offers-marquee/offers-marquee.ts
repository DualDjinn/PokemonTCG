import { Component, computed, inject, signal } from '@angular/core';
import { EnergyIcon } from '../energy-icon/energy-icon';
import { CartService } from '../../../data-access/services/cart.service';
import { AuthService } from '../../../../game/data-access/services/auth.service';
import { ToastService } from '../../../../../shared/services/toast.service';
import { Card } from '../../../domain/models/card.model';
import { DeckCardDetail } from '../../../../decks/components/deck-card-detail/deck-card-detail';
import { CardSummary } from '../../../../decks/domain/models/deck.models';
import { ShopMarqueeItem, StoreOffersService } from '../../../data-access/services/offers.service';

@Component({
  standalone: true,
  selector: 'app-offers-marquee',
  imports: [EnergyIcon, DeckCardDetail],
  templateUrl: './offers-marquee.html',
  styleUrl: './offers-marquee.scss',
})
export class OffersMarquee {
  private readonly offersService = inject(StoreOffersService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly offers = this.offersService.activeOffers;
  readonly selectedOffer = signal<ShopMarqueeItem | null>(null);
  readonly quantity = signal<number>(1);

  toCardSummary(card: Card): CardSummary {
    return {
      id: card.id,
      name: card.name,
      supertype: card.supertype,
      imageUrl: card.images?.small || '',
      imageUrlLarge: card.images?.large || card.images?.small || '',
      rarity: card.rarity,
    };
  }

  getOwnedCount(cardId?: string): number {
    if (!cardId) return 0;
    return this.authService.currentUser()?.collectionQuantities?.[cardId] ?? 0;
  }

  isCardMaxed(item: ShopMarqueeItem): boolean {
    if (item.itemType !== 'card') return false;
    return this.getOwnedCount(item.id) >= 4;
  }

  maxSelectableQty(item: ShopMarqueeItem): number {
    if (item.itemType !== 'card') return 10;
    const owned = this.getOwnedCount(item.id);
    const inCart = this.cartService.getItemQuantity(item.id);
    return Math.max(0, 4 - owned - inCart);
  }

  openOfferModal(item: ShopMarqueeItem, event?: Event): void {
    event?.stopPropagation();
    this.selectedOffer.set(item);
    const max = this.maxSelectableQty(item);
    this.quantity.set(max > 0 ? 1 : 0);
  }

  closeOfferModal(): void {
    this.selectedOffer.set(null);
    this.quantity.set(1);
  }

  incrementQty(): void {
    const item = this.selectedOffer();
    const max = item ? this.maxSelectableQty(item) : 10;
    this.quantity.update((q) => Math.min(max, q + 1));
  }

  decrementQty(): void {
    this.quantity.update((q) => Math.max(1, q - 1));
  }

  confirmAddToCart(): void {
    const item = this.selectedOffer();
    if (!item) return;

    if (item.itemType === 'card') {
      const maxAllowed = this.maxSelectableQty(item);
      if (maxAllowed <= 0) {
        this.toast.show(
          `Ya tienes el máximo de 4 copias de ${item.name} en tu colección`,
          'error',
        );
        return;
      }
    }

    const qty = Math.max(1, this.quantity());
    this.cartService.addItem({
      id: item.id || `offer-${item.name}`,
      name: item.name,
      imageUrl: item.imageUrl || '',
      type: item.itemType,
      rarity: item.rarity || 'Common',
      quantity: qty,
      unitPrice: item.price,
    });

    this.toast.show(
      `¡${qty}x ${item.name} (${item.highlight}) añadido al carrito!`,
      'info',
    );
    this.closeOfferModal();
  }
}
