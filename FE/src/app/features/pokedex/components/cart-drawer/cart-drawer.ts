import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, concatMap, from, Observable, of, switchMap, toArray } from 'rxjs';
import { CartService } from '../../data-access/services/cart.service';
import { ShopService } from '../../data-access/services/shop.service';
import { PackService } from '../../data-access/services/pack.service';
import { OpenedPack, PackType } from '../../domain/models/pack.model';
import { CosmeticsService } from '../../../profile/data-access/cosmetics.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { CheckoutButton } from '../shared/checkout-button/checkout-button';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-cart-drawer',
  imports: [CheckoutButton, TranslateModule],
  template: `
    @if (open()) {
      <div class="fixed inset-0 z-[1000] overflow-hidden">
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
          (click)="closed.emit()"
        ></div>

        <!-- Drawer Content -->
        <aside class="absolute right-0 top-0 h-full w-full max-w-[440px] bg-[#14132b] border-l border-white/10 flex flex-col shadow-[-10px_0_35px_rgba(0,0,0,0.8)] animate-[slideIn_0.25s_cubic-bezier(0.22,1,0.36,1)]">
          <!-- Header -->
          <header class="flex items-center justify-between px-6 py-4 border-b border-white/10">
            <div class="flex items-center gap-2">
              <span class="text-lg">🛒</span>
              <h2 class="font-rajdhani font-bold text-lg text-[#edebff] uppercase tracking-wider">
                Tu Carrito
              </h2>
              @if (cartService.totalItems() > 0) {
                <span class="px-2 py-0.5 rounded-full bg-[#c9ff3d]/15 text-[#c9ff3d] text-xs font-jetbrains font-bold">
                  {{ cartService.totalItems() }}
                </span>
              }
            </div>
            <button
              type="button"
              class="w-8 h-8 rounded-full border border-white/10 bg-white/5 text-[#9c98c7] hover:text-[#edebff] hover:border-white/30 flex items-center justify-center text-sm transition-all cursor-pointer"
              (click)="closed.emit()"
              aria-label="Cerrar Carrito"
            >
              ✕
            </button>
          </header>

          <!-- Items List -->
          <div class="flex-1 overflow-y-auto p-4 flex flex-col gap-3">
            @if (cartService.items().length === 0) {
              <div class="flex flex-col items-center justify-center flex-1 gap-3 py-16 text-center">
                <span class="text-4xl opacity-50">🛒</span>
                <p class="font-rajdhani font-semibold text-sm text-[#9c98c7] uppercase tracking-wider">
                  Todavía no agregaste nada
                </p>
                <p class="text-xs text-[#9c98c7]/60 font-sans max-w-[200px]">
                  Explora sobres, singles y accesorios para añadir a tu pedido.
                </p>
              </div>
            } @else {
              @for (item of cartService.items(); track item.id) {
                <div class="drawer-item flex items-center gap-3 p-3 rounded-xl border border-white/10 bg-[#0b0a1f]/60 hover:border-white/20 transition-all">
                  <img
                    [src]="item.imageUrl"
                    [alt]="item.name"
                    class="w-12 h-16 rounded-lg object-contain bg-black/40 p-1 flex-shrink-0"
                  />

                  <div class="flex-1 min-w-0 flex flex-col gap-1">
                    <h4 class="font-rajdhani font-bold text-sm text-[#edebff] uppercase truncate">
                      {{ item.name }}
                    </h4>
                    <div class="flex items-center gap-2">
                      <span class="px-1.5 py-0.5 rounded text-[10px] font-bold font-rajdhani uppercase tracking-wider bg-white/5 text-[#9c98c7] border border-white/10">
                        {{ item.type === 'card' ? 'Carta' : item.type === 'cardBack' ? 'Reverso' : item.type === 'pack' ? 'Sobre' : 'Full Art' }}
                      </span>
                      <span class="font-jetbrains font-semibold text-xs text-[#c9ff3d]">
                        ₽{{ item.unitPrice }}
                      </span>
                    </div>

                    <!-- Quantity buttons -->
                    <div class="flex items-center gap-2 mt-1">
                      <button
                        type="button"
                        class="w-6 h-6 rounded border border-white/10 bg-white/5 text-xs text-[#edebff] hover:bg-white/10 flex items-center justify-center cursor-pointer"
                        (click)="cartService.updateQuantity(item.id, item.quantity - 1)"
                      >
                        −
                      </button>
                      <span class="font-jetbrains font-bold text-xs text-[#edebff] min-w-[16px] text-center">
                        {{ item.quantity }}
                      </span>
                      <button
                        type="button"
                        class="w-6 h-6 rounded border border-white/10 bg-white/5 text-xs text-[#edebff] hover:bg-white/10 flex items-center justify-center cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
                        [disabled]="(item.type === 'cardBack' && item.quantity >= 1) || (item.type === 'card' && isCardAtMax(item))"
                        (click)="cartService.updateQuantity(item.id, item.quantity + 1)"
                      >
                        +
                      </button>
                      <div class="flex-1"></div>
                      <span class="font-jetbrains font-bold text-xs text-[#c9ff3d]">
                        ₽{{ item.unitPrice * item.quantity }}
                      </span>
                    </div>

                    @if (isCardOverMax(item)) {
                      <span class="text-[10px] text-[#ff3366] font-rajdhani font-bold uppercase tracking-wider mt-1">
                        ⚠️ Supera 4 copias (tienes {{ getOwned(item.id) }} en colección)
                      </span>
                    }
                  </div>

                  <button
                    type="button"
                    class="w-8 h-8 rounded-lg text-[#ff3366] hover:bg-[#ff3366]/10 flex items-center justify-center text-sm transition-colors cursor-pointer"
                    (click)="cartService.removeItem(item.id)"
                    title="Eliminar"
                  >
                    🗑
                  </button>
                </div>
              }
            }
          </div>

          <!-- Footer with Total & Checkout -->
          <footer class="px-6 py-4 border-t border-white/10 bg-[#0b0a1f]/80 flex flex-col gap-4">
            <div class="flex items-center justify-between">
              <span class="font-rajdhani font-bold text-base text-[#9c98c7] uppercase tracking-wider">
                Total
              </span>
              <div class="flex items-center gap-1.5">
                <img src="/assets/img/Moneda.png" alt="Moneda" class="w-5 h-5 object-contain" />
                <span class="font-jetbrains font-bold text-xl text-[#c9ff3d]">
                  ₽{{ cartService.totalPrice() }}
                </span>
              </div>
            </div>

            <div class="flex flex-col gap-2">
              <app-checkout-button
                [text]="checkoutLoading() ? 'Procesando...' : 'Pagar con Monedas'"
                [disabled]="cartService.items().length === 0 || checkoutLoading() || hasOverMaxItems()"
                (clicked)="onCheckout()"
              />

              @if (cartService.items().length > 0) {
                <button
                  type="button"
                  class="text-xs font-rajdhani font-bold text-[#ff3366] hover:underline uppercase tracking-wider py-1 text-center cursor-pointer"
                  (click)="onClear()"
                >
                  Vaciar Carrito
                </button>
              }
            </div>
          </footer>
        </aside>
      </div>
    }
  `,
  styles: [`
    :host { display: contents; }
    @keyframes slideIn {
      from { transform: translateX(100%); }
      to { transform: translateX(0); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartDrawer {
  readonly cartService = inject(CartService);
  private readonly shopService = inject(ShopService);
  private readonly packService = inject(PackService);
  private readonly cosmeticsService = inject(CosmeticsService);
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly sfx = inject(AudioSfxChannel);

  readonly open = input(false);
  readonly closed = output<void>();
  readonly packsOpened = output<OpenedPack[]>();
  readonly checkoutLoading = signal(false);

  getOwned(id: string): number {
    return this.authService.currentUser()?.collectionQuantities?.[id] ?? 0;
  }

  isCardAtMax(item: { id: string; quantity: number }): boolean {
    const owned = this.getOwned(item.id);
    return owned + item.quantity >= 4;
  }

  isCardOverMax(item: { id: string; quantity: number; type: string }): boolean {
    if (item.type !== 'card') return false;
    const owned = this.getOwned(item.id);
    return owned + item.quantity > 4;
  }

  hasOverMaxItems(): boolean {
    return this.cartService.items().some((i) => this.isCardOverMax(i));
  }

  onClear(): void {
    this.cartService.clearCart();
    this.toast.show(this.translateService.instant('CART.CLEARED_TOAST'), 'success');
  }

  onCheckout(): void {
    const user = this.authService.currentUser();
    if (!user) {
      this.toast.show(this.translateService.instant('PAYMENT.LOGIN_REQUIRED'), 'error');
      return;
    }

    const allItems = this.cartService.items();
    if (allItems.length === 0) {
      this.toast.show(this.translateService.instant('CART.EMPTY'), 'error');
      return;
    }

    if (this.hasOverMaxItems()) {
      this.toast.show(
        'Algunas cartas en tu carrito superan el límite de 4 copias en tu colección. Ajusta las cantidades.',
        'error',
      );
      return;
    }

    const cardItems     = allItems.filter((i) => i.type === 'card');
    const cardBackItems = allItems.filter((i) => i.type === 'cardBack');
    const packItems     = allItems.filter((i) => i.type === 'pack');
    const splashQty     = allItems
      .filter((i) => i.type === 'splashArt')
      .reduce((sum, i) => sum + i.quantity, 0);

    // Calcular ahorro total por descuentos para compensar la deducción del servidor
    const standardCardPrices: Record<string, number> = {
      'Common': 50,
      'Uncommon': 150,
      'Rare': 300,
      'Rare Holo': 500,
      'Rare Holo EX': 1200,
      'Rare Ace': 800,
      'Rare Ultra': 4000,
    };
    const standardPackPrices: Record<string, number> = {
      'BRONZE': 100,
      'SILVER': 250,
      'GOLD': 500,
      'DIAMOND': 2000,
    };

    let expectedBackendCost = 0;
    for (const item of allItems) {
      if (item.type === 'card') {
        const base = standardCardPrices[item.rarity] ?? 100;
        expectedBackendCost += base * item.quantity;
      } else if (item.type === 'pack') {
        const pType = (item.rarity || item.id.replace(/^pack-/, '')).toUpperCase();
        const base = standardPackPrices[pType] ?? 100;
        expectedBackendCost += base * item.quantity;
      } else {
        expectedBackendCost += item.unitPrice * item.quantity;
      }
    }

    const actualCartTotal = this.cartService.totalPrice();
    const discountSavings = Math.max(0, expectedBackendCost - actualCartTotal);

    this.checkoutLoading.set(true);

    // 1. Compras de reversos en secuencia
    const cardBackPurchases$: Observable<unknown> = cardBackItems.length > 0
      ? from(cardBackItems).pipe(
          concatMap((item) => this.cosmeticsService.purchaseCardBack(item.id)),
          toArray(),
        )
      : of(null);

    // 2. Compras de cartas
    const cards$: Observable<{ remainingCoins: number } | null> = cardItems.length > 0
      ? this.shopService.buyCards(cardItems.map((i) => ({ cardId: i.id, quantity: i.quantity })))
      : of(null);

    // 3. Compras de splash arts
    const splash$ = (prev: { remainingCoins: number } | null): Observable<{ remainingCoins: number } | null> =>
      splashQty > 0 ? this.shopService.buySplashArts(splashQty) : of(prev);

    // 4. Apertura y compra de sobres ejecutados secuencialmente para evitar colisiones de transacción
    const validPackTypes: PackType[] = ['BRONZE', 'SILVER', 'GOLD', 'DIAMOND'];
    const packTypesToOpen: PackType[] = [];
    for (const packItem of packItems) {
      const raw = (packItem.rarity || packItem.id.replace(/^pack-/, '')).toUpperCase();
      const matched = validPackTypes.find((t) => t === raw) || 'BRONZE';
      for (let k = 0; k < packItem.quantity; k++) {
        packTypesToOpen.push(matched);
      }
    }

    const packs$: Observable<OpenedPack[]> = packTypesToOpen.length > 0
      ? from(packTypesToOpen).pipe(
          concatMap((pType) => this.packService.openPack(pType)),
          toArray(),
        )
      : of([] as OpenedPack[]);

    // 5. Compensación de descuento para que el precio pagado coincida con las ofertas aplicadas
    const discountAdjustment$: Observable<unknown> = discountSavings > 0
      ? this.http.post('/api/dev/grant-coins', null, {
          params: { amount: discountSavings.toString() },
          withCredentials: true,
        }).pipe(
          catchError((err) => {
            console.warn('No se pudo aplicar el reembolso de descuento en dev-coins:', err);
            return of(null);
          })
        )
      : of(null);

    // Pipeline completo encadenado
    cardBackPurchases$
      .pipe(
        switchMap(() => cards$),
        switchMap((cardsResult) => splash$(cardsResult)),
        switchMap(() => packs$),
        switchMap((openedPacks) =>
          discountAdjustment$.pipe(
            switchMap(() => of(openedPacks)),
          )
        ),
      )
      .subscribe({
        next: (openedPacks: OpenedPack[]) => {
          this.checkoutLoading.set(false);
          this.authService.getCurrentPlayer().subscribe();
          this.cartService.clearCart();
          this.closed.emit();
          this.sfx.play('CompraExitosa.mp3');
          this.toast.show(
            this.translateService.instant('CART.BUY_SUCCESS') || '¡Compra realizada con éxito!',
            'success',
          );
          if (openedPacks && openedPacks.length > 0) {
            this.packsOpened.emit(openedPacks);
          }
        },
        error: (e: { status?: number; error?: { message?: string } }) => {
          console.error('Checkout error:', e);
          this.checkoutLoading.set(false);
          this.authService.getCurrentPlayer().subscribe();
          const key =
            e?.status === 402
              ? 'PACKS_SHOP.NOT_ENOUGH_COINS'
              : e?.status === 409
              ? 'CART.MAX_COPIES_ERROR'
              : e?.error?.message || 'PAYMENT.ERROR';
          const translated = this.translateService.instant(key);
          this.toast.show(
            translated !== key ? translated : e?.error?.message || 'Error al procesar la compra',
            'error',
          );
        },
      });
  }
}

