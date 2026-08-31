import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  inject,
  NgZone,
  OnInit,
  AfterViewInit,
  OnDestroy,
  signal,
  HostListener,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Card } from '../../domain/models/card.model';
import { CardService } from '../../data-access/services/card.service';
import { CartService } from '../../data-access/services/cart.service';
import { PaymentService } from '../../data-access/services/payment.service';
import { PackOpeningAnimationComponent } from '../../components/pack-opening-animation/pack-opening-animation';
import { OpenedPack } from '../../domain/models/pack.model';
import { SplashArtService } from '../../data-access/services/splash-art.service';
import { SplashArt } from '../../domain/models/splash-art.model';
import {
  CosmeticsService,
  CardBack,
} from '../../../profile/data-access/cosmetics.service';
import { CartItem, getPrice } from '../../domain/models/cart.model';
import { CardSummary } from '../../../decks/domain/models/deck.models';
import { DeckCardDetail } from '../../../decks/components/deck-card-detail/deck-card-detail';
import { ToastService } from '../../../../shared/services/toast.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CartDrawer } from '../../components/cart-drawer/cart-drawer';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';

import { ShopNavbar } from '../../components/navbar/navbar';
import { ShopHero } from '../../components/hero/hero';
import { StoreOffersService } from '../../data-access/services/offers.service';

interface CoinPackage {
  coins: number;
  price: number;
  badge?: string;
  badgeColor?: string;
}

@Component({
  selector: 'app-pokedex-page',
  standalone: true,
  imports: [
    TranslateModule,
    ShopNavbar,
    ShopHero,
    CartDrawer,
    DeckCardDetail,
    PackOpeningAnimationComponent,
    PokenavDeviceComponent,
    GameOptionsComponent,
  ],
  templateUrl: './pokedex-page.html',
  styleUrl: './pokedex-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokedexPage implements OnInit, AfterViewInit, OnDestroy {
  private readonly cardService = inject(CardService);
  private readonly splashArtService = inject(SplashArtService);
  private readonly cosmeticsService = inject(CosmeticsService);
  private readonly toast = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly paymentService = inject(PaymentService);
  protected readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly elRef = inject(ElementRef);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly circleTransition = inject(CircleTransitionService);
  private readonly storageService = inject(StorageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly audioMusic = inject(AudioMusicChannel);

  readonly activeSection = signal<'sobres' | 'decks' | 'singles' | 'accesorios' | null>(null);
  readonly openedPack = signal<OpenedPack | null>(null);
  readonly cartOpen = signal(false);
  readonly selectedCard = signal<Card | null>(null);
  readonly selectedSplash = signal<SplashArt | null>(null);
  readonly selectedCardBack = signal<CardBack | null>(null);
  readonly coinsModalOpen = signal(false);
  readonly navigating = signal(false);
  readonly showMenu = signal(false);
  readonly showOptions = signal(false);
  readonly ownedCardBackIds = signal<Set<string>>(new Set());
  readonly coinsBuyLoading = signal<number | null>(null);

  readonly COIN_PACKAGES: CoinPackage[] = [
    { coins: 100, price: 1 },
    { coins: 500, price: 2, badge: 'COINS_SHOP.POPULAR', badgeColor: 'var(--neon-cyan)' },
    { coins: 1000, price: 3 },
    { coins: 5000, price: 4, badge: 'COINS_SHOP.BEST_VALUE', badgeColor: 'var(--neon-magenta)' },
  ];

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (
      this.openedPack() ||
      this.cartOpen() ||
      this.coinsModalOpen() ||
      this.selectedCard() ||
      this.selectedSplash() ||
      this.selectedCardBack()
    ) {
      this.closeAllModals();
      return;
    }
    if (this.showOptions()) {
      this.showOptions.set(false);
      this.showMenu.set(true);
      return;
    }
    this.showMenu.update((v) => !v);
  }

  ngOnInit(): void {
    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    if (tabParam === 'packs') {
      this.activeSection.set('sobres');
    } else if (tabParam === 'cards') {
      this.activeSection.set('singles');
    } else if (tabParam === 'splashArt' || tabParam === 'cardBacks') {
      this.activeSection.set('accesorios');
    } else if (tabParam === 'decks') {
      this.activeSection.set('decks');
    }

    this.authService.getCurrentPlayer().subscribe();
    this.refreshOwnedCardBacks();
  }

  ngAfterViewInit(): void {
    if (this.circleTransition.pendingOpen) {
      setTimeout(() => this.circleTransition.open());
    }
    this.audioMusic.play('pokeShopMusic.opus');
  }

  ngOnDestroy(): void {
    this.cartService.clearCart();
    this.audioMusic.stop();
  }

  readonly packQueue = signal<OpenedPack[]>([]);

  onSectionChange(section: 'sobres' | 'decks' | 'singles' | 'accesorios' | null): void {
    this.activeSection.set(section);
  }

  onPackOpened(opened: OpenedPack): void {
    this.onPacksOpened([opened]);
  }

  onPacksOpened(packs: OpenedPack[]): void {
    if (!packs || packs.length === 0) return;
    this.packQueue.set(packs);
    this.openedPack.set(packs[0]);
  }

  onPackAnimationClosed(): void {
    const queue = this.packQueue();
    if (queue.length > 1) {
      const remaining = queue.slice(1);
      this.packQueue.set(remaining);
      this.openedPack.set(remaining[0]);
    } else {
      this.packQueue.set([]);
      this.openedPack.set(null);
    }
  }

  openCardDetail(card: Card): void {
    this.selectedCard.set(card);
  }

  closeCardDetail(): void {
    this.selectedCard.set(null);
  }

  openSplashDetail(sa: SplashArt): void {
    this.selectedSplash.set(sa);
  }

  closeSplashDetail(): void {
    this.selectedSplash.set(null);
  }

  openCardBackDetail(cb: CardBack): void {
    this.selectedCardBack.set(cb);
  }

  closeCardBackDetail(): void {
    this.selectedCardBack.set(null);
  }

  closeAllModals(): void {
    this.selectedCard.set(null);
    this.selectedSplash.set(null);
    this.selectedCardBack.set(null);
    this.coinsModalOpen.set(false);
    this.cartOpen.set(false);
  }

  private readonly offersService = inject(StoreOffersService);

  isCardAtMax(cardId: string): boolean {
    const owned = this.authService.currentUser()?.collectionQuantities?.[cardId] ?? 0;
    const inCart = this.cartService.getItemQuantity(cardId);
    return (owned + inCart) >= 4;
  }

  confirmAddToCart(card: Card): void {
    if (this.isCardAtMax(card.id)) {
      this.toast.show(
        `Ya posees el límite de 4 copias de ${card.name} en tu colección`,
        'error',
      );
      return;
    }
    const priceInfo = this.getCardPriceInfo(card);
    const item: CartItem = {
      id: card.id,
      name: card.name,
      imageUrl: card.images.small,
      type: 'card',
      rarity: card.rarity,
      quantity: 1,
      unitPrice: priceInfo.price,
    };
    this.cartService.addItem(item);
    const toastMsg = priceInfo.hasDiscount
      ? `¡${card.name} (${priceInfo.highlight}) añadida al carrito!`
      : `${card.name} añadida al carrito`;
    this.toast.show(toastMsg, 'info');
    this.closeCardDetail();
  }

  addSplashToCart(sa: SplashArt): void {
    const item: CartItem = {
      id: sa.id,
      name: sa.name,
      imageUrl: sa.imageUrl,
      type: 'splashArt',
      rarity: sa.rarity,
      quantity: 1,
      unitPrice: sa.kind === 'original' ? 2000 : 1500,
    };
    this.cartService.addItem(item);
    this.toast.show(`${sa.name} añadido al carrito`, 'info');
    this.closeSplashDetail();
  }

  addCardBackToCart(cb: CardBack): void {
    if (this.isCardBackOwned(cb.id) || !cb.price) return;
    const item: CartItem = {
      id: cb.id,
      name: cb.name,
      imageUrl: cb.imageUrl,
      type: 'cardBack',
      rarity: 'Cosmetic',
      quantity: 1,
      unitPrice: cb.price,
    };
    this.cartService.addItem(item);
    this.toast.show(`Reverso ${cb.name} añadido al carrito`, 'info');
    this.closeCardBackDetail();
  }

  isCardBackOwned(id: string): boolean {
    return this.ownedCardBackIds().has(id);
  }

  getCardPriceInfo(card: Card) {
    return this.offersService.getEffectiveCardPrice(card);
  }

  getCardPrice(card: Card): number {
    return this.offersService.getEffectiveCardPrice(card).price;
  }

  getSplashPrice(sa: SplashArt): number {
    return sa.kind === 'original' ? 2000 : 1500;
  }

  toCardSummary(card: Card): CardSummary {
    return {
      id: card.id,
      name: card.name,
      supertype: card.supertype,
      imageUrl: card.images.small,
      imageUrlLarge: card.images.large,
      rarity: card.rarity,
    };
  }

  buyCoinPackage(pkg: CoinPackage): void {
    if (this.coinsBuyLoading() !== null) return;
    const user = this.authService.currentUser();
    if (!user) {
      this.toast.show(
        this.translateService.instant('PAYMENT.LOGIN_REQUIRED') || 'Debes iniciar sesión',
        'error',
      );
      return;
    }

    const payload = {
      playerId: user.id,
      coinsToAdd: pkg.coins,
      items: [
        {
          id: `coins-${pkg.coins}`,
          title: `${pkg.coins} Coins — PokeShop`,
          quantity: 1,
          unitPrice: pkg.price,
        },
      ],
    };
    this.coinsBuyLoading.set(pkg.coins);
    this.paymentService.createCheckout(payload).subscribe({
      next: (result) => {
        this.coinsBuyLoading.set(null);
        this.paymentService.redirectToCheckout(result.checkoutUrl);
      },
      error: () => {
        this.coinsBuyLoading.set(null);
        this.toast.show(
          this.translateService.instant('PAYMENT.ERROR') || 'Error al procesar el pago',
          'error',
        );
      },
    });
  }

  refreshOwnedCardBacks(): void {
    this.cosmeticsService.getPlayerCardBacks().subscribe({
      next: (data) => this.ownedCardBackIds.set(new Set(data.owned.map((b) => b.id))),
      error: () => {},
    });
  }

  async onExit(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.cartService.clearCart();
    await this.circleTransition.close();
    const user = this.authService.currentUser();
    if (user) {
      const saved = this.storageService.getSavedPosition(user.username);
      const location = saved?.location ?? 'preLan';
      this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
    } else {
      this.router.navigate(['/login']);
    }
  }

  onMenuProfile(): void {
    this.showMenu.set(false);
    this.cartService.clearCart();
    this.router.navigate(['/profile']);
  }

  onMenuDecks(): void {
    this.showMenu.set(false);
    this.cartService.clearCart();
    this.router.navigate(['/decks']);
  }

  onMenuExit(): void {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.showMenu.set(false);
    this.cartService.clearCart();
    const user = this.authService.currentUser();
    if (user) {
      const saved = this.storageService.getSavedPosition(user.username);
      const location = saved?.location ?? 'preLan';
      this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
    } else {
      this.router.navigate(['/login']);
    }
  }
}
