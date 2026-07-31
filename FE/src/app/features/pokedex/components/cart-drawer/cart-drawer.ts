import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { forkJoin, Observable, of, switchMap } from 'rxjs';
import { CartService } from '../../data-access/services/cart.service';
import { ShopService } from '../../data-access/services/shop.service';
import { CosmeticsService } from '../../../profile/data-access/cosmetics.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyIconButton } from '../../../../shared/ui/my-icon-button/my-icon-button';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-cart-drawer',
  imports: [MyText, MyButton, MyIconButton, MyBadge, TranslateModule],
  template: `
    @if (open()) {
      <div class="fixed inset-0 z-[1000]">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" (click)="closed.emit()"></div>

        <aside class="absolute right-0 top-0 h-full w-full max-w-[420px] bg-[var(--bg-secondary)] border-l-2 border-[var(--neon-cyan)] flex flex-col shadow-[−8px_0_32px_rgba(0,170,255,0.15)] animate-[slideIn_0.25s_ease-out]">

          <header class="flex items-center justify-between px-5 py-4 border-b border-white/10">
            <my-text [text]="'CART.TITLE' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--neon-cyan)" />
            <my-icon-button icon="✕" color="var(--text-secondary)" size="small" (clicked)="closed.emit()" [ariaLabel]="'CART.CLOSE' | translate" />
          </header>

          <div class="flex-1 overflow-y-auto scrollbar-none p-4 flex flex-col gap-3">
            @if (cartService.items().length === 0) {
              <div class="flex flex-col items-center justify-center flex-1 gap-3 opacity-60">
                <span class="text-4xl">🛒</span>
                <my-text [text]="'CART.EMPTY' | translate" tag="p" size="0.855rem" color="var(--text-secondary)" align="center" />
              </div>
            } @else {
              @for (item of cartService.items(); track item.id) {
                <div class="flex gap-3 p-3 rounded-lg border border-white/10 bg-white/[0.03]">
                  <img [src]="item.imageUrl" [alt]="item.name" class="w-14 h-14 rounded object-contain bg-white/5 flex-shrink-0" />

                  <div class="flex-1 min-w-0 flex flex-col gap-1">
                    <my-text [text]="item.name" tag="span" size="0.765rem" weight="bold" color="var(--text-primary)" />
                    <div class="flex items-center gap-2">
                      <my-badge [text]="item.type === 'card' ? ('CART.CARD' | translate) : item.type === 'cardBack' ? ('CART.CARD_BACK' | translate) : ('CART.SPLASH' | translate)" color="var(--neon-blue)" />
                      <my-text [text]="'₽ ' + item.unitPrice" tag="span" size="0.675rem" color="var(--text-secondary)" />
                    </div>

                    <div class="flex items-center gap-2 mt-1">
                      <my-icon-button icon="−" size="small" color="var(--neon-cyan)" background="rgba(0,170,255,0.1)" (clicked)="cartService.updateQuantity(item.id, item.quantity - 1)" [ariaLabel]="'CART.DECREASE' | translate" />
                      <my-text [text]="'' + item.quantity" tag="span" size="0.765rem" weight="bold" color="var(--text-primary)" align="center" />
                      <my-icon-button icon="+" size="small" color="var(--neon-cyan)" background="rgba(0,170,255,0.1)" [disabled]="(item.type === 'cardBack' && item.quantity >= 1) || (item.type === 'card' && isCardAtMax(item))" (clicked)="cartService.updateQuantity(item.id, item.quantity + 1)" [ariaLabel]="'CART.INCREASE' | translate" />
                      <div class="flex-1"></div>
                      <my-text [text]="'₽ ' + (item.unitPrice * item.quantity)" tag="span" size="0.765rem" weight="bold" color="var(--neon-cyan)" />
                    </div>
                  </div>

                  <my-icon-button icon="🗑" size="small" color="var(--neon-magenta)" (clicked)="cartService.removeItem(item.id)" [ariaLabel]="'CART.REMOVE' | translate" />
                </div>
              }
            }
          </div>

          <footer class="px-5 py-4 border-t border-white/10 flex flex-col gap-3">
            <div class="flex items-center justify-between">
              <my-text [text]="'CART.TOTAL' | translate" tag="span" size="0.9rem" weight="bold" color="var(--text-primary)" />
              <my-text [text]="'₽ ' + cartService.totalPrice()" tag="span" size="1.035rem" weight="bold" color="var(--neon-cyan)" />
            </div>
            <div class="flex gap-2">
              <my-button [text]="'CART.CLEAR' | translate" variant="outline" color="var(--neon-magenta)" textColor="var(--neon-magenta)" size="small" [disabled]="cartService.items().length === 0" (clicked)="onClear()" class="flex-1" />
              <my-button [text]="checkoutLoading() ? ('PAYMENT.PROCESSING' | translate) : ('CART.BUY' | translate)" color="var(--neon-cyan)" textColor="var(--bg-dark)" size="medium" [disabled]="cartService.items().length === 0 || checkoutLoading()" (clicked)="onCheckout()" class="flex-[2]" />
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
  private readonly cosmeticsService = inject(CosmeticsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly sfx = inject(AudioSfxChannel);

  readonly open = input(false);
  readonly closed = output<void>();
  readonly checkoutLoading = signal(false);

  isCardAtMax(item: { id: string; quantity: number }): boolean {
    const owned = this.authService.currentUser()?.collectionQuantities?.[item.id] ?? 0;
    return owned + item.quantity >= 4;
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

    const cardItems     = allItems.filter(i => i.type === 'card');
    const cardBackItems = allItems.filter(i => i.type === 'cardBack');
    const splashQty     = allItems
      .filter(i => i.type === 'splashArt')
      .reduce((sum, i) => sum + i.quantity, 0);

    this.checkoutLoading.set(true);

    // Todas las compras se descuentan y persisten en el servidor; se encadenan
    // en orden (reversos -> cartas -> splash arts) y el último saldo devuelto por
    // el servidor es la fuente de verdad. Así el saldo nunca se "resetea" al
    // re-sincronizar con /api/players/me.
    const cardBackPurchases$: Observable<unknown> = cardBackItems.length > 0
      ? forkJoin(cardBackItems.map(i => this.cosmeticsService.purchaseCardBack(i.id)))
      : of(null);

    const cards$: Observable<{ remainingCoins: number } | null> = cardItems.length > 0
      ? this.shopService.buyCards(cardItems.map(i => ({ cardId: i.id, quantity: i.quantity })))
      : of(null);

    const splash$ = (prev: { remainingCoins: number } | null): Observable<{ remainingCoins: number } | null> =>
      splashQty > 0 ? this.shopService.buySplashArts(splashQty) : of(prev);

    cardBackPurchases$.pipe(
      switchMap(() => cards$),
      switchMap(cardsResult => splash$(cardsResult))
    ).subscribe({
      next: (result: { remainingCoins: number } | null) => {
        this.checkoutLoading.set(false);
        this.authService.getCurrentPlayer().subscribe();
        this.cartService.clearCart();
        this.closed.emit();
        this.sfx.play('CompraExitosa.mp3');
        this.toast.show(this.translateService.instant('CART.BUY_SUCCESS'), 'success');
      },
      error: (e: { status?: number }) => {
        this.checkoutLoading.set(false);
        const key = e?.status === 402 ? 'PACKS_SHOP.NOT_ENOUGH_COINS'
                  : e?.status === 409 ? 'CART.MAX_COPIES_ERROR'
                  : 'PAYMENT.ERROR';
        this.toast.show(this.translateService.instant(key), 'error');
      }
    });
  }
}
