import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, inject, NgZone, OnInit, AfterViewInit, signal, computed, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Card, CardFilters } from '../../domain/models/card.model';
import { CardService } from '../../data-access/services/card.service';
import { CartService } from '../../data-access/services/cart.service';
import { PaymentService } from '../../data-access/services/payment.service';
import { PackShopComponent } from '../../components/pack-shop/pack-shop';
import { PackOpeningAnimationComponent } from '../../components/pack-opening-animation/pack-opening-animation';
import { OpenedPack } from '../../domain/models/pack.model';
import { SplashArtService } from '../../data-access/services/splash-art.service';
import { SplashArt } from '../../domain/models/splash-art.model';
import { CosmeticsService, CardBack } from '../../../profile/data-access/cosmetics.service';
import { CartItem, getPrice } from '../../domain/models/cart.model';
import { CardSummary } from '../../../decks/domain/models/deck.models';
import { DeckCardDetail } from '../../../decks/components/deck-card-detail/deck-card-detail';
import { ToastService } from '../../../../shared/services/toast.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CartDrawer } from '../../components/cart-drawer/cart-drawer';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyInput } from '../../../../shared/ui/my-input/my-input';
import { MySelect, MySelectOption } from '../../../../shared/ui/my-select/my-select';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MyIconButton } from '../../../../shared/ui/my-icon-button/my-icon-button';
import { PixelPanelDirective, PixelBorderDirective } from '../../../../shared/directives';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/services/language.service';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';

interface CoinPackage {
  coins: number;
  price: number;
  badge?: string;
  badgeColor?: string;
}

const ENERGY_TYPES = [
  'Grass', 'Fire', 'Water', 'Lightning', 'Psychic',
  'Fighting', 'Darkness', 'Metal', 'Fairy', 'Dragon', 'Colorless',
];

const RARITIES = [
  'Common', 'Uncommon', 'Rare', 'Rare Holo', 'Rare Holo EX', 'Rare Ace',
];

const SUBTYPES = [
  'Basic', 'Stage 1', 'Stage 2', 'MEGA', 'EX',
  'Item', 'Supporter', 'Stadium', 'Pokemon Tool',
];

type ShopTab = 'cards' | 'splashArt' | 'cardBacks' | 'packs';

@Component({
  selector: 'app-pokedex-page',
  // TranslateModule para usar el pipe 'translate' en pokedex-page.html
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    CartDrawer, DeckCardDetail, MyModal, PokenavDeviceComponent, GameOptionsComponent,
    MyText, MyButton, MyInput, MySelect, MyBadge, MyIconButton,
    PixelPanelDirective, PixelBorderDirective,
    PackShopComponent, PackOpeningAnimationComponent,
  ],
  templateUrl: './pokedex-page.html',
  styleUrl: './pokedex-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokedexPage implements OnInit{
  private readonly cardService = inject(CardService);
  private readonly splashArtService = inject(SplashArtService);
  private readonly cosmeticsService = inject(CosmeticsService);
  private readonly toast = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly langService = inject(LanguageService);
  private readonly paymentService = inject(PaymentService);
  private readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly elRef = inject(ElementRef);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly circleTransition = inject(CircleTransitionService);
  private readonly storageService = inject(StorageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly audioMusic = inject(AudioMusicChannel);

  private heroSourceCardId: string | null = null;
  private isAnimating = false;

  readonly activeTab = signal<ShopTab>('cards');
  readonly openedPack = signal<OpenedPack | null>(null);
  readonly cartOpen = signal(false);
  readonly selectedCard = signal<Card | null>(null);
  readonly selectedSplash = signal<SplashArt | null>(null);
  readonly selectedCardBack = signal<CardBack | null>(null);
  readonly coinsModalOpen = signal(false);
  readonly navigating = signal(false);
  readonly showMenu = signal(false);
  readonly showOptions = signal(false);
  readonly cardBacks = signal<CardBack[]>([]);
  readonly ownedCardBackIds = signal<Set<string>>(new Set());
  readonly cardBacksLoading = signal(false);

  readonly pendingQuantities = signal<Map<string, number>>(new Map());

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.openedPack() || this.cartOpen() || this.coinsModalOpen() || this.selectedCard() || this.selectedSplash()) return;
    if (this.showOptions()) { this.showOptions.set(false); this.showMenu.set(true); return; }
    this.showMenu.update(v => !v);
  }

  onMenuProfile(): void { this.showMenu.set(false); this.router.navigate(['/profile']); }
  onMenuDecks(): void { this.showMenu.set(false); this.router.navigate(['/decks']); }
  onMenuExit(): void {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.showMenu.set(false);
    const user = this.authService.currentUser();
    if (user) {
      const saved = this.storageService.getSavedPosition(user.username);
      const location = saved?.location ?? 'preLan';
      this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
    } else {
      this.router.navigate(['/login']);
    }
  }
  readonly coinsBuyLoading = signal<number | null>(null);

  readonly COIN_PACKAGES: CoinPackage[] = [
    { coins: 100, price: 1 },
    { coins: 500, price: 2, badge: 'COINS_SHOP.POPULAR', badgeColor: 'var(--neon-cyan)' },
    { coins: 1000, price: 3 },
    { coins: 5000, price: 4, badge: 'COINS_SHOP.BEST_VALUE', badgeColor: 'var(--neon-magenta)' },
  ];

  filterForm = new FormGroup({
    name: new FormControl(''),
    supertype: new FormControl(''),
    type: new FormControl(''),
    rarity: new FormControl(''),
    subtype: new FormControl(''),
  });

  splashFilterForm = new FormGroup({
    name: new FormControl(''),
  });

  cards = signal<Card[]>([]);
  splashArts = signal<SplashArt[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  readonly supertypeOptions = computed<MySelectOption[]>(() => {
    this.langService.currentLang();
    return [
      { label: this.translateService.instant('POKEDEX.ALL_OPTION'), value: '' },
      { label: 'Pokémon', value: 'Pokemon' },
      { label: 'Trainer', value: 'Trainer' },
      { label: 'Energy', value: 'Energy' },
    ];
  });

  readonly typeOptions = computed<MySelectOption[]>(() => {
    this.langService.currentLang();
    return [
      { label: this.translateService.instant('POKEDEX.ALL_OPTION'), value: '' },
      ...ENERGY_TYPES.map(t => ({ label: t, value: t })),
    ];
  });

  readonly rarityOptions = computed<MySelectOption[]>(() => {
    this.langService.currentLang();
    return [
      { label: this.translateService.instant('POKEDEX.ALL_OPTION'), value: '' },
      ...RARITIES.map(r => ({ label: r, value: r })),
    ];
  });

  readonly subtypeOptions = computed<MySelectOption[]>(() => {
    this.langService.currentLang();
    return [
      { label: this.translateService.instant('POKEDEX.ALL_OPTION'), value: '' },
      ...SUBTYPES.map(s => ({ label: s, value: s })),
    ];
  });

  readonly typeColorMap: { [key: string]: string | undefined } = {
    grass: '#4ade80',
    fire: '#f97316',
    water: '#38bdf8',
    lightning: '#facc15',
    psychic: '#e879f9',
    fighting: '#a16207',
    darkness: '#374151',
    metal: '#9ca3af',
    fairy: '#f472b6',
    dragon: '#7c3aed',
    colorless: '#d1d5db',
  };

  ngOnInit(): void {
    this.loadCards();
    this.loadSplashArts();
    this.loadCardBackCatalog();
    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    if (tabParam === 'packs' || tabParam === 'splashArt' || tabParam === 'cards' || tabParam === 'cardBacks') {
      this.activeTab.set(tabParam);
    }
  }

  onPackOpened(opened: OpenedPack): void {
    this.openedPack.set(opened);
  }

  onPackAnimationClosed(): void {
    this.openedPack.set(null);
  }

  async onExit(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
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

  ngAfterViewInit(): void {
    if (this.circleTransition.pendingOpen) {
      setTimeout(() => this.circleTransition.open());
    }
    this.audioMusic.play('pokeShopMusic.opus');
  }

  ngOnDestroy(): void {
    this.audioMusic.stop();
  }

  switchTab(tab: ShopTab): void {
    this.activeTab.set(tab);
  }

  onFilter(): void {
    const raw = this.filterForm.value;
    const filters: CardFilters = {};

    if (raw.name?.trim()) filters.name = raw.name.trim();
    if (raw.supertype) filters.supertype = raw.supertype;
    if (raw.type) filters.type = raw.type;
    if (raw.rarity) filters.rarity = raw.rarity;
    if (raw.subtype) filters.subtype = raw.subtype;

    this.loadCards(Object.keys(filters).length ? filters : undefined);
  }

  onReset(): void {
    this.filterForm.reset();
    this.loadCards();
  }

  onFilterSplash(): void {
    const raw = this.splashFilterForm.value;
    this.loadSplashArts(raw.name?.trim() ? { name: raw.name.trim() } : undefined);
  }

  onResetSplash(): void {
    this.splashFilterForm.reset();
    this.loadSplashArts();
  }

  getCardPrice(card: Card): number {
    return getPrice(card.rarity);
  }

  getSplashPrice(sa: SplashArt): number {
    return getPrice(sa.rarity);
  }

  ownedCount(cardId: string): number {
    return this.authService.currentUser()?.collectionQuantities?.[cardId] ?? 0;
  }

  isMaxedOut(cardId: string): boolean {
    return this.ownedCount(cardId) >= 4;
  }

  maxBuyable(cardId: string): number {
    const owned = this.ownedCount(cardId);
    const inCart = this.getCardInCart(cardId) ?? 0;
    return Math.max(0, 4 - owned - inCart);
  }

  getPendingQty(cardId: string): number {
    return this.pendingQuantities().get(cardId) ?? 1;
  }

  changePendingQty(cardId: string, delta: number): void {
    this.pendingQuantities.update(map => {
      const copy = new Map(map);
      const current = copy.get(cardId) ?? 1;
      const max = this.maxBuyable(cardId) + (this.getCardInCart(cardId) ?? 0);
      const newQty = Math.max(1, Math.min(current + delta, max));
      copy.set(cardId, newQty);
      return copy;
    });
  }

  confirmAddToCart(card: Card): void {
    const qty = this.getPendingQty(card.id);
    if (qty <= 0 || this.maxBuyable(card.id) <= 0) return;
    const isFirstAdd = !this.getCardInCart(card.id);
    const item: CartItem = {
      id: card.id,
      name: card.name,
      imageUrl: card.images.small,
      type: 'card',
      rarity: card.rarity,
      quantity: qty,
      unitPrice: getPrice(card.rarity),
    };
    this.cartService.addItem(item);
    this.toast.show(this.translateService.instant('POKEDEX.ADDED_TO_CART', { name: card.name }), 'success');
    if (isFirstAdd) {
      this.playFlyToCart(card.images.small, card.id);
    }
  }

  getCardInCart(id: string): number | null {
    const item = this.cartService.items().find(i => i.id === id);
    return item ? item.quantity : null;
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

  openCardDetail(card: Card, event: MouseEvent): void {
    if (this.isAnimating) return;

    this.pendingQuantities.update(map => {
      const copy = new Map(map);
      copy.delete(card.id);
      return copy;
    });

    const wrapper = event.currentTarget as HTMLElement;
    const imgEl = wrapper.querySelector('img') as HTMLImageElement | null;

    if (!imgEl) {
      this.selectedCard.set(card);
      return;
    }

    this.heroSourceCardId = card.id;
    this.isAnimating = true;
    this.playHeroOpen(card, this.getContainRect(imgEl));
  }

  closeCardDetail(): void {
    if (this.isAnimating) return;

    const card = this.selectedCard();
    if (!card) return;

    const modalImg = document.querySelector('my-modal app-deck-card-detail img') as HTMLImageElement | null;
    const gridCard = this.heroSourceCardId
      ? (this.elRef.nativeElement as HTMLElement).querySelector(`[data-card-id="${this.heroSourceCardId}"] img`) as HTMLImageElement | null
      : null;

    if (!modalImg || !gridCard) {
      this.selectedCard.set(null);
      this.heroSourceCardId = null;
      return;
    }

    this.isAnimating = true;
    this.playHeroClose(card.images.small, this.getContainRect(modalImg), this.getContainRect(gridCard));
  }

  /** CardSummary para alimentar el `app-deck-card-detail` desde un Full Art. */
  toSplashSummary(sa: SplashArt): CardSummary {
    return {
      id: sa.id,
      name: sa.name,
      supertype: 'Pokemon',
      imageUrl: sa.imageUrl,
      imageUrlLarge: sa.imageUrl,
      rarity: sa.rarity,
    };
  }

  /** Abre el modal de detalle de un Full Art con la misma animación hero. */
  openSplashDetail(sa: SplashArt, event: MouseEvent): void {
    if (this.isAnimating) return;

    const wrapper = event.currentTarget as HTMLElement;
    const imgEl = wrapper.querySelector('img') as HTMLImageElement | null;

    if (!imgEl) {
      this.selectedSplash.set(sa);
      return;
    }

    this.heroSourceCardId = sa.id;
    this.isAnimating = true;
    this.playHeroOpenGeneric(sa.imageUrl, this.getContainRect(imgEl),
      () => this.selectedSplash.set(sa));
  }

  closeSplashDetail(): void {
    if (this.isAnimating) return;

    const sa = this.selectedSplash();
    if (!sa) return;

    const modalImg = document.querySelector('my-modal app-deck-card-detail img') as HTMLImageElement | null;
    const gridCard = this.heroSourceCardId
      ? (this.elRef.nativeElement as HTMLElement).querySelector(`[data-card-id="${this.heroSourceCardId}"] img`) as HTMLImageElement | null
      : null;

    if (!modalImg || !gridCard) {
      this.selectedSplash.set(null);
      this.heroSourceCardId = null;
      return;
    }

    this.isAnimating = true;
    this.playHeroClose(sa.imageUrl, this.getContainRect(modalImg), this.getContainRect(gridCard),
      () => this.selectedSplash.set(null));
  }

  incrementItem(id: string, name: string, imageUrl: string, type: 'card' | 'splashArt', rarity: string): void {
    if (type === 'card' && this.maxBuyable(id) <= 0) return;
    const item: CartItem = { id, name, imageUrl, type, rarity, quantity: 1, unitPrice: getPrice(rarity) };
    this.cartService.addItem(item);
  }

  decrementItem(id: string): void {
    const item = this.cartService.items().find(i => i.id === id);
    if (item) {
      this.cartService.updateQuantity(id, item.quantity - 1);
    }
  }

  addCardToCart(card: Card): void {
    if (this.maxBuyable(card.id) <= 0) return;
    const isFirstAdd = !this.getCardInCart(card.id);
    const item: CartItem = {
      id: card.id,
      name: card.name,
      imageUrl: card.images.small,
      type: 'card',
      rarity: card.rarity,
      quantity: 1,
      unitPrice: getPrice(card.rarity),
    };
    this.cartService.addItem(item);
    this.toast.show(this.translateService.instant('POKEDEX.ADDED_TO_CART', { name: card.name }), 'success');
    if (isFirstAdd) {
      this.playFlyToCart(card.images.small, card.id);
    }
  }

  addSplashToCart(sa: SplashArt): void {
    const isFirstAdd = !this.getCardInCart(sa.id);

    const item: CartItem = {
      id: sa.id,
      name: sa.name,
      imageUrl: sa.imageUrl,
      type: 'splashArt',
      rarity: sa.rarity,
      quantity: 1,
      unitPrice: getPrice(sa.rarity),
    };
    this.cartService.addItem(item);
    this.toast.show(this.translateService.instant('POKEDEX.ADDED_TO_CART', { name: sa.name }), 'success');

    if (isFirstAdd) {
      this.playFlyToCart(sa.imageUrl, sa.id);
    }
  }

  /** Returns the actual rendered content rect of an img with object-fit:contain,
   *  stripping the transparent letterbox area on the sides or top/bottom. */
  private getContainRect(el: HTMLImageElement): DOMRect {
    const box = el.getBoundingClientRect();
    const nW = el.naturalWidth;
    const nH = el.naturalHeight;
    if (!nW || !nH) return box;
    const containerAR = box.width / box.height;
    const imageAR = nW / nH;
    if (imageAR > containerAR) {
      const h = box.width / imageAR;
      return new DOMRect(box.left, box.top + (box.height - h) / 2, box.width, h);
    } else {
      const w = box.height * imageAR;
      return new DOMRect(box.left + (box.width - w) / 2, box.top, w, box.height);
    }
  }

  private playHeroOpen(card: Card, sourceRect: DOMRect): void {
    this.playHeroOpenGeneric(card.images.small, sourceRect, () => this.selectedCard.set(card));
  }

  /**
   * Animación "hero" genérica: lleva la imagen de la carta desde su posición en
   * la grilla hasta el centro del modal con un giro 3D. Reutilizada por la
   * sección "Cartas" ({@link openCardDetail}) y "Full Art" ({@link openSplashDetail}).
   *
   * @param imageSmall  URL de la imagen a animar
   * @param sourceRect  rect de origen (la imagen en la grilla)
   * @param renderModal callback que abre el modal correspondiente (set del signal)
   */
  private playHeroOpenGeneric(
    imageSmall: string,
    sourceRect: DOMRect,
    renderModal: () => void,
    targetImgSelector = 'my-modal app-deck-card-detail img',
  ): void {
    renderModal();
    this.cdr.detectChanges();

    const modalImg = document.querySelector(targetImgSelector) as HTMLImageElement | null;
    const modalEl = modalImg?.closest('my-modal') as HTMLElement | null;
    // my-modal uses `display: contents` (see my-modal.css), so hiding the
    // host has no effect. Hide its actual rendered children (backdrop +
    // panel) instead.
    const modalParts = modalEl
      ? Array.from(modalEl.children).filter((c): c is HTMLElement => c instanceof HTMLElement)
      : [];
    modalParts.forEach(el => { el.style.visibility = 'hidden'; });

    // Use actual rendered content rect (strips letterbox transparent area).
    const targetRect = modalImg
      ? this.getContainRect(modalImg)
      : (() => {
          const w = 240;
          const h = w * (sourceRect.height / sourceRect.width);
          return new DOMRect((window.innerWidth - w) / 2, (window.innerHeight - h) / 2, w, h);
        })();

    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:1100;';

    const backdrop = document.createElement('div');
    backdrop.style.cssText = 'position:fixed;inset:0;background:black;opacity:0;transition:opacity 0.35s ease;';
    overlay.appendChild(backdrop);

    const img = document.createElement('img');
    img.src = imageSmall;
    img.style.cssText = `position:fixed;left:${sourceRect.left}px;top:${sourceRect.top}px;width:${sourceRect.width}px;height:${sourceRect.height}px;border-radius:12px;pointer-events:none;box-shadow:0 0 30px rgba(0,204,170,0.4);transform-origin:center center;`;
    overlay.appendChild(img);
    document.body.appendChild(overlay);

    requestAnimationFrame(() => { backdrop.style.opacity = '0.6'; });

    // Center-to-center translation so the rotation pivots around the
    // card's center (with transform-origin:center) without drifting.
    const sourceCx = sourceRect.left + sourceRect.width / 2;
    const sourceCy = sourceRect.top + sourceRect.height / 2;
    const targetCx = targetRect.left + targetRect.width / 2;
    const targetCy = targetRect.top + targetRect.height / 2;
    const dx = targetCx - sourceCx;
    const dy = targetCy - sourceCy;
    // Escalado UNIFORME (un solo factor para X e Y) para que la carta no se
    // distorsione horizontalmente cuando la miniatura y el modal tienen distinta
    // proporción (caso Full Art). Con object-contain la imagen mantiene su aspecto.
    const scale = targetRect.height / sourceRect.height;

    img.animate([
      { transform: 'translate(0,0) scale(1) rotateY(0deg)' },
      { transform: `translate(${dx}px,${dy}px) scale(${scale}) rotateY(720deg)` },
    ], { duration: 800, easing: 'cubic-bezier(0.23,1,0.32,1)', fill: 'forwards' });

    setTimeout(() => {
      modalParts.forEach(el => { el.style.visibility = ''; });
      overlay.remove();
      this.isAnimating = false;
    }, 810);
  }

  private playHeroClose(imageUrl: string, sourceRect: DOMRect, targetRect: DOMRect,
                        clearModal: () => void = () => this.selectedCard.set(null)): void {
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:1100;';

    const backdrop = document.createElement('div');
    backdrop.style.cssText = 'position:fixed;inset:0;background:black;opacity:0.6;';
    overlay.appendChild(backdrop);

    const img = document.createElement('img');
    img.src = imageUrl;
    img.style.cssText = `position:fixed;left:${sourceRect.left}px;top:${sourceRect.top}px;width:${sourceRect.width}px;height:${sourceRect.height}px;border-radius:12px;pointer-events:none;box-shadow:0 0 30px rgba(0,204,170,0.4);transform-origin:center center;`;
    overlay.appendChild(img);
    document.body.appendChild(overlay);

    this.ngZone.run(clearModal);

    backdrop.animate([{ opacity: '0.6' }, { opacity: '0' }], { duration: 800, easing: 'ease-out', fill: 'forwards' });

    const sourceCx = sourceRect.left + sourceRect.width / 2;
    const sourceCy = sourceRect.top + sourceRect.height / 2;
    const targetCx = targetRect.left + targetRect.width / 2;
    const targetCy = targetRect.top + targetRect.height / 2;
    const dx = targetCx - sourceCx;
    const dy = targetCy - sourceCy;
    // Escalado uniforme (ver playHeroOpenGeneric): evita la distorsión horizontal.
    const scale = targetRect.height / sourceRect.height;

    img.animate([
      { transform: 'translate(0,0) scale(1) rotateY(0deg)' },
      { transform: `translate(${dx}px,${dy}px) scale(${scale}) rotateY(-720deg)` },
    ], { duration: 800, easing: 'cubic-bezier(0.23,1,0.32,1)', fill: 'forwards' });

    setTimeout(() => {
      overlay.remove();
      this.heroSourceCardId = null;
      this.isAnimating = false;
    }, 810);
  }

  private playFlyToCart(imageUrl: string, cardId: string): void {
    let sourceEl: HTMLElement | null = null;

    if (this.selectedCard()) {
      sourceEl = document.querySelector('my-modal app-deck-card-detail img') as HTMLElement;
    }
    if (!sourceEl) {
      sourceEl = (this.elRef.nativeElement as HTMLElement).querySelector(`[data-card-id="${cardId}"] img`) as HTMLElement;
    }

    const cartBtn = document.querySelector('[data-cart-btn]') as HTMLElement;
    if (!sourceEl || !cartBtn) return;

    const srcR = sourceEl.getBoundingClientRect();
    const cartR = cartBtn.getBoundingClientRect();

    const flyImg = document.createElement('img');
    flyImg.src = imageUrl;
    flyImg.style.cssText = `position:fixed;left:${srcR.left}px;top:${srcR.top}px;width:${srcR.width}px;height:${srcR.height}px;z-index:2000;border-radius:8px;object-fit:contain;pointer-events:none;box-shadow:0 0 20px rgba(0,204,170,0.4);`;
    document.body.appendChild(flyImg);

    const endCx = cartR.left + cartR.width / 2;
    const endCy = cartR.top + cartR.height / 2;
    const endScale = 30 / srcR.width;
    const dx = endCx - srcR.left - srcR.width / 2;
    const dy = endCy - srcR.top - srcR.height / 2;
    const midDx = dx * 0.5;
    const midDy = Math.min(0, dy) - 80;
    const midScale = 0.5;

    flyImg.style.transformOrigin = 'center center';
    flyImg.animate([
      { transform: 'translate(0,0) scale(1) rotate(0deg)', opacity: '1' },
      { transform: `translate(${midDx}px,${midDy}px) scale(${midScale}) rotate(-10deg)`, opacity: '0.8', offset: 0.5 },
      { transform: `translate(${dx}px,${dy}px) scale(${endScale}) rotate(-25deg)`, opacity: '0.3' },
    ], { duration: 650, easing: 'cubic-bezier(0.4,0,0.2,1)', fill: 'forwards' });

    setTimeout(() => {
      flyImg.remove();
      cartBtn.animate([
        { transform: 'scale(1)' },
        { transform: 'scale(1.3)' },
        { transform: 'scale(1)' },
      ], { duration: 300, easing: 'ease-out' });
    }, 660);
  }

  buyCoinPackage(pkg: CoinPackage): void {
    // Double-click guard: if a checkout is already in progress, don't fire another.
    if (this.coinsBuyLoading() !== null) {
      return;
    }
    const user = this.authService.currentUser();
    if (!user) {
      this.toast.show(this.translateService.instant('PAYMENT.LOGIN_REQUIRED'), 'error');
      return;
    }

    const payload = {
      playerId: user.id,
      coinsToAdd: pkg.coins,
      items: [{
        id: `coins-${pkg.coins}`,
        title: `${pkg.coins} Coins — PokeShop`,
        quantity: 1,
        unitPrice: pkg.price
      }]
    };
    this.coinsBuyLoading.set(pkg.coins);
    this.paymentService.createCheckout(payload).subscribe({
      next: result => {
        this.coinsBuyLoading.set(null);
        this.paymentService.redirectToCheckout(result.checkoutUrl);
      },
      error: () => {
        this.coinsBuyLoading.set(null);
        this.toast.show(this.translateService.instant('PAYMENT.ERROR'), 'error');
      }
    });
  }

  private loadCardBackCatalog(): void {
    this.cardBacksLoading.set(true);
    this.cosmeticsService.getCardBackCatalog().subscribe({
      next: (catalog) => {
        this.cardBacks.set(catalog.filter(cb => cb.origin !== 'DEFAULT'));
        this.cardBacksLoading.set(false);
      },
      error: () => this.cardBacksLoading.set(false),
    });
    this.cosmeticsService.getPlayerCardBacks().subscribe({
      next: (data) => this.ownedCardBackIds.set(new Set(data.owned.map(cb => cb.id))),
    });
  }

  isCardBackOwned(id: string): boolean {
    return this.ownedCardBackIds().has(id);
  }

  getCardBackOriginColor(origin: CardBack['origin']): string {
    const map: Record<CardBack['origin'], string> = {
      DEFAULT: 'var(--text-secondary)',
      SHOP:    'var(--neon-cyan)',
      PACK:    'var(--neon-blue)',
      BATTLE:  'var(--neon-magenta)',
      LEVEL:   '#facc15',
    };
    return map[origin] ?? 'var(--text-secondary)';
  }

  getCardBackOriginKey(origin: CardBack['origin']): string {
    const map: Record<CardBack['origin'], string> = {
      DEFAULT: 'POKEDEX.CARD_BACKS_ORIGIN_DEFAULT',
      SHOP:    'POKEDEX.CARD_BACKS_ORIGIN_SHOP',
      PACK:    'POKEDEX.CARD_BACKS_ORIGIN_PACK',
      BATTLE:  'POKEDEX.CARD_BACKS_ORIGIN_BATTLE',
      LEVEL:   'POKEDEX.CARD_BACKS_ORIGIN_LEVEL',
    };
    return map[origin] ?? origin;
  }

  openCardBackDetail(cb: CardBack, event: MouseEvent): void {
    if (this.isAnimating) return;
    const wrapper = event.currentTarget as HTMLElement;
    const imgEl = wrapper.querySelector('img') as HTMLImageElement | null;
    if (!imgEl) { this.selectedCardBack.set(cb); return; }
    this.heroSourceCardId = cb.id;
    this.isAnimating = true;
    this.playHeroOpenGeneric(cb.imageUrl, this.getContainRect(imgEl),
      () => this.selectedCardBack.set(cb), 'my-modal [data-hero-img]');
  }

  closeCardBackDetail(): void {
    if (this.isAnimating) return;
    const cb = this.selectedCardBack();
    if (!cb) return;
    const modalImg = document.querySelector('my-modal [data-hero-img]') as HTMLImageElement | null;
    const gridCard = this.heroSourceCardId
      ? (this.elRef.nativeElement as HTMLElement).querySelector(`[data-card-id="${this.heroSourceCardId}"] img`) as HTMLImageElement | null
      : null;
    if (!modalImg || !gridCard) {
      this.selectedCardBack.set(null);
      this.heroSourceCardId = null;
      return;
    }
    this.isAnimating = true;
    this.playHeroClose(cb.imageUrl, this.getContainRect(modalImg), this.getContainRect(gridCard),
      () => this.selectedCardBack.set(null));
  }

  addCardBackToCart(cb: CardBack): void {
    if (this.isCardBackOwned(cb.id)) return;
    if (this.getCardInCart(cb.id)) return; // ya está en el carrito
    const item: CartItem = {
      id: cb.id,
      name: cb.name,
      imageUrl: cb.imageUrl,
      type: 'cardBack',
      rarity: 'cardBack',
      quantity: 1,
      unitPrice: cb.price ?? 0,
    };
    this.cartService.addItem(item);
    this.toast.show(`${cb.name} agregado al carrito`, 'success');
    this.playFlyToCart(cb.imageUrl, cb.id);
  }

  refreshOwnedCardBacks(): void {
    this.cosmeticsService.getPlayerCardBacks().subscribe({
      next: (data) => this.ownedCardBackIds.set(new Set(data.owned.map(b => b.id))),
    });
  }

  private loadCards(filters?: CardFilters): void {
    this.loading.set(true);
    this.error.set(null);

    this.cardService.getCards(filters).subscribe({
      next: (data) => {
        this.cards.set(data.filter(c => !c.id.startsWith('pokesecret-')));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translateService.instant('POKEDEX.CONNECTION_ERROR'));
        this.loading.set(false);
      },
    });
  }

  private loadSplashArts(filters?: { name?: string }): void {
    this.splashArtService.getSplashArts(filters).subscribe({
      next: (data) => this.splashArts.set(data),
    });
  }
}
