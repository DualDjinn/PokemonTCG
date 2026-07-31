import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';

@Component({
  selector: 'app-payment-failure',
  standalone: true,
  imports: [TranslateModule, RouterLink, MyText, MyButton],
  template: `
    <div class="min-h-screen bg-[var(--bg-dark)] flex flex-col items-center justify-center gap-6 p-6 text-center">
      <div class="text-6xl select-none">❌</div>

      <my-text
        [text]="'PAYMENT.FAILURE_TITLE' | translate"
        tag="h1"
        size="1.5rem"
        weight="bold"
        color="var(--neon-magenta)"
      />

      <my-text
        [text]="'PAYMENT.FAILURE_MESSAGE' | translate"
        tag="p"
        size="0.9rem"
        color="var(--text-secondary)"
      />

      @if (status()) {
        <my-text
          [text]="('PAYMENT.STATUS' | translate) + ': ' + status()"
          tag="p"
          size="0.75rem"
          color="var(--text-secondary)"
        />
      }

      <div class="flex gap-3 flex-wrap justify-center">
        <a routerLink="/pokeshop">
          <my-button
            [text]="'PAYMENT.RETRY' | translate"
            color="var(--neon-magenta)"
            textColor="var(--bg-dark)"
            size="medium"
          />
        </a>
        <a routerLink="/pokeshop">
          <my-button
            [text]="'PAYMENT.GO_TO_SHOP' | translate"
            variant="outline"
            color="var(--neon-cyan)"
            textColor="var(--neon-cyan)"
            size="medium"
          />
        </a>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentFailurePage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly status = signal('');

  ngOnInit(): void {
    this.status.set(this.route.snapshot.queryParamMap.get('status') ?? '');
  }
}
