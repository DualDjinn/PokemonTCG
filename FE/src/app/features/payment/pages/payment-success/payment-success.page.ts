import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { PaymentService } from '../../../pokedex/data-access/services/payment.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [TranslateModule, RouterLink, MyText, MyButton],
  template: `
    <div class="min-h-screen bg-[var(--bg-dark)] flex flex-col items-center justify-center gap-6 p-6 text-center">
      <div class="text-6xl select-none">✅</div>

      <my-text
        [text]="'PAYMENT.SUCCESS_TITLE' | translate"
        tag="h1"
        size="1.5rem"
        weight="bold"
        color="var(--neon-cyan)"
      />

      <my-text
        [text]="'PAYMENT.SUCCESS_MESSAGE' | translate"
        tag="p"
        size="0.9rem"
        color="var(--text-secondary)"
      />

      @if (paymentId()) {
        <my-text
          [text]="('PAYMENT.PAYMENT_ID' | translate) + ': ' + paymentId()"
          tag="p"
          size="0.75rem"
          color="var(--text-secondary)"
        />
      }

      <!-- Coin balance section -->
      <div class="flex flex-col items-center gap-2 px-6 py-4 rounded-xl border"
           style="border-color: rgba(0,170,255,0.3); background: rgba(0,170,255,0.06);">
        @if (refreshingCoins()) {
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 border-2 border-[var(--neon-cyan)]/30 border-t-[var(--neon-cyan)] rounded-full animate-spin"></div>
            <my-text
              [text]="'PAYMENT.COINS_CREDITED' | translate"
              tag="p"
              size="0.8rem"
              color="var(--text-secondary)"
            />
          </div>
        } @else {
          <my-text
            [text]="'PAYMENT.COINS_CREDITED' | translate"
            tag="p"
            size="0.8rem"
            color="var(--neon-cyan)"
          />
          <div class="flex items-center gap-2">
            <span class="text-2xl">🪙</span>
            <my-text
              [text]="currentCoins() + ''"
              tag="span"
              size="1.5rem"
              weight="bold"
              color="var(--text-primary)"
            />
            <my-text
              [text]="'PAYMENT.CURRENT_COINS' | translate"
              tag="span"
              size="0.75rem"
              color="var(--text-secondary)"
            />
          </div>
        }
      </div>

      <a routerLink="/pokeshop">
        <my-button
          [text]="'PAYMENT.GO_TO_SHOP' | translate"
          color="var(--neon-cyan)"
          textColor="var(--bg-dark)"
          size="medium"
        />
      </a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentSuccessPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly paymentService = inject(PaymentService);
  private readonly sfx = inject(AudioSfxChannel);

  readonly paymentId = signal('');
  readonly refreshingCoins = signal(true);
  readonly currentCoins = computed(() => this.authService.currentUser()?.coins ?? 0);

  private pollAttempt = 0;
  private readonly maxPollAttempts = 10;

  ngOnInit(): void {
    // On arriving at the "payment success" redirect, play the successful-purchase sound effect.
    this.sfx.play('CompraExitosa.mp3');

    const params = this.route.snapshot.queryParamMap;
    const paymentId = params.get('payment_id') ?? '';
    const purchaseId = params.get('external_reference');

    this.paymentId.set(paymentId);

    if (paymentId) {
      // Pull-based: confirm + credit directly against MercadoPago, independent of the webhook.
      this.paymentService.confirmPayment(paymentId).subscribe({
        next: () => this.refreshUser(),
        error: () => (purchaseId ? this.pollPurchaseStatus(purchaseId) : this.refreshUser()),
      });
    } else if (purchaseId) {
      this.pollPurchaseStatus(purchaseId);
    } else {
      this.refreshUser();
    }
  }

  private pollPurchaseStatus(purchaseId: string): void {
    this.pollAttempt++;
    this.paymentService.getPurchaseStatus(purchaseId).subscribe({
      next: (status) => {
        if (status.approved || status.rejected) {
          this.refreshUser();
        } else if (this.pollAttempt < this.maxPollAttempts) {
          setTimeout(() => this.pollPurchaseStatus(purchaseId), 2000);
        } else {
          this.refreshUser();
        }
      },
      error: () => this.refreshUser(),
    });
  }

  private refreshUser(): void {
    this.authService.getCurrentPlayer().subscribe({
      next: () => this.refreshingCoins.set(false),
      error: () => this.refreshingCoins.set(false),
    });
  }
}
