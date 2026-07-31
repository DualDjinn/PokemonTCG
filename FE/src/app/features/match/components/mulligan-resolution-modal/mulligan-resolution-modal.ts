import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-mulligan-resolution-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MyModal, MyButton, TranslateModule],
  template: `
    @if (isBeneficiary()) {
      <my-modal
        [open]="open()"
        [title]="'MATCH.MULLIGAN_TITLE' | translate"
        variant="pixel"
        [closeOnBackdrop]="false"
        [showClose]="false"
        width="24rem"
      >
        <div class="flex flex-col gap-4 items-center">
          <p class="text-white text-sm text-center">
            {{ 'MATCH.MULLIGAN_DID' | translate:{ name: opponentName(), count: maxCount() } }}<br />
            {{ 'MATCH.MULLIGAN_HOW_MANY' | translate }}
          </p>
          <div class="flex items-center gap-4">
            <my-button
              text="-"
              variant="pixel"
              size="small"
              [disabled]="currentCount() <= 0"
              (clicked)="decrement()"
            />
            <span class="text-white text-2xl font-bold min-w-[3rem] text-center">{{ currentCount() }}</span>
            <my-button
              text="+"
              variant="pixel"
              size="small"
              [disabled]="currentCount() >= maxCount()"
              (clicked)="increment()"
            />
          </div>
          <my-button
            [text]="'MATCH.MULLIGAN_ACCEPT' | translate"
            variant="pixel"
            size="medium"
            (clicked)="confirm()"
          />
        </div>
      </my-modal>
    } @else {
      <my-modal
        [open]="open()"
        [title]="'MATCH.MULLIGAN_TITLE' | translate"
        variant="pixel"
        [closeOnBackdrop]="false"
        [showClose]="false"
        width="24rem"
      >
        <div class="flex flex-col gap-4 items-center">
          <p class="text-white text-sm text-center">
            {{ 'MATCH.MULLIGAN_WAITING' | translate }}
          </p>
        </div>
      </my-modal>
    }
  `,
})
export class MulliganResolutionModal {
  readonly open = input.required<boolean>();
  readonly isBeneficiary = input.required<boolean>();
  readonly maxCount = input.required<number>();
  readonly opponentName = input('');

  readonly countChosen = output<number>();

  readonly currentCount = signal(0);

  increment(): void {
    this.currentCount.update(v => Math.min(v + 1, this.maxCount()));
  }

  decrement(): void {
    this.currentCount.update(v => Math.max(v - 1, 0));
  }

  confirm(): void {
    this.countChosen.emit(this.currentCount());
  }
}
