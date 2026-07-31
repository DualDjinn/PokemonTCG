import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { SlotMachineService } from '../../data-access/services/slot-machine.service';

@Component({
  selector: 'app-px-pay-table',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
  <div class="px-pay-wrap">
    <div class="px-pay-title">{{ 'SLOT_MACHINE.PAY_TABLE' | translate }}</div>
    @for (i of [0,1,2,3,4,5,6,7]; track i) {
      <div class="px-pay-row" [class.px-pay-jackpot]="i === 7">
        <div class="px-pay-cell">
          <img [src]="service.SYMBOLS[i]" alt="" class="px-pay-img" />
        </div>
        <span class="px-pay-name">{{ service.SYMBOL_NAMES[i] }}</span>
        <span class="px-pay-mult">×{{ service.PAYOUTS[i] }}</span>
      </div>
    }
  </div>
  `,
  styles: [`
    .px-pay-wrap {
      background: var(--px-dark);
      border: 4px solid var(--px-shadow);
      outline: 2px solid var(--px-border);
      clip-path: polygon(8px 0%, calc(100% - 8px) 0%, 100% 8px, 100% calc(100% - 8px), calc(100% - 8px) 100%, 8px 100%, 0% calc(100% - 8px), 0% 8px);
      padding: 14px 14px;
      height: 100%;
      box-sizing: border-box;
      min-width: 198px;
    }
    .px-pay-title {
      font-size: 8px;
      color: var(--px-border);
      text-shadow: 1px 1px 0 var(--px-shadow);
      letter-spacing: 2px;
      text-align: center;
      margin-bottom: 10px;
      padding-bottom: 6px;
      border-bottom: 2px solid var(--px-shadow);
    }
    .px-pay-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 5px 0;
      border-bottom: 1px solid rgba(255,106,0,0.1);
    }
    .px-pay-row:last-child {
      border-bottom: none;
    }
    .px-pay-cell {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--px-reel);
      border: 2px solid var(--px-shadow);
    }
    .px-pay-img {
      width: 25px;
      height: 25px;
      object-fit: contain;
      image-rendering: pixelated;
      display: block;
    }
    .px-pay-name {
      font-size: 7px;
      color: #ccc;
      letter-spacing: 1px;
      flex: 1;
    }
    .px-pay-mult {
      font-size: 8px;
      color: var(--px-gold);
      text-shadow: 0 0 4px var(--px-gold);
      min-width: 24px;
      text-align: right;
    }
    .px-pay-jackpot .px-pay-name {
      color: var(--px-gold2);
    }
    .px-pay-jackpot .px-pay-mult {
      color: var(--px-gold2);
      text-shadow: 0 0 8px var(--px-gold2);
    }
    .px-pay-jackpot .px-pay-cell {
      border-color: var(--px-gold);
    }
  `],
})
export class PxPayTable {
  protected service = inject(SlotMachineService);
}
