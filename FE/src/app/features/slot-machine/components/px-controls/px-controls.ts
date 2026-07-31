import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import type { MachineState } from '../../domain/slot-machine.models';

@Component({
  selector: 'app-px-controls',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="px-bet-group">
      <button class="px-bet-btn" (click)="decreaseBet.emit()" [disabled]="machineState() === 'spinning'">−</button>
      <span class="px-bet-value">{{ bet() }}</span>
      <button class="px-bet-btn" (click)="increaseBet.emit()" [disabled]="machineState() === 'spinning'">+</button>
    </div>
    <div class="px-spin-area">
      <div class="px-spin-border" [class.on]="machineState() === 'spinning'"></div>
      <button class="px-spin-btn" (click)="spin.emit()" [disabled]="machineState() === 'spinning' || credits() < bet()">
        <span>{{ 'SLOT_MACHINE.SPIN' | translate }}</span>
      </button>
    </div>
  `,
  styles: [`
    :host {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    .px-bet-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .px-bet-btn {
      width: 26px;
      height: 26px;
      background: var(--px-bg2);
      clip-path: polygon(4px 0%, calc(100% - 4px) 0%, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0% calc(100% - 4px), 0% 4px);
      border: 3px solid var(--px-border);
      cursor: pointer;
      color: var(--px-gold);
      font-family: 'Press Start 2P', monospace;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0;
      line-height: 1;
    }
    .px-bet-btn:hover:not(:disabled) {
      background: var(--px-border);
      color: #fff;
    }
    .px-bet-btn:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
    .px-bet-value {
      font-size: 13px;
      color: var(--px-gold);
      text-shadow: 0 0 6px var(--px-gold);
      min-width: 36px;
      text-align: center;
    }
    .px-spin-area {
      position: relative;
      width: 108px;
      height: 108px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .px-spin-border {
      position: absolute;
      inset: -4px;
      border: 3px solid transparent;
    }
    .px-spin-border.on {
      border-color: var(--px-border);
      animation: spinborder .3s step-end infinite;
    }
    @keyframes spinborder {
      50% { opacity: 0; }
    }
    .px-spin-btn {
      width: 108px;
      height: 108px;
      background: var(--px-btn);
      clip-path: polygon(14px 0%, calc(100% - 14px) 0%, 100% 14px, 100% calc(100% - 14px), calc(100% - 14px) 100%, 14px 100%, 0% calc(100% - 14px), 0% 14px);
      border: none;
      cursor: pointer;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: 'Press Start 2P', monospace;
      font-size: 14px;
      color: #fff;
      text-shadow: 2px 2px 0 rgba(0,0,0,0.5);
      user-select: none;
    }
    .px-spin-btn::before {
      content: '';
      position: absolute;
      inset: 3px;
      background: var(--px-btn2);
      clip-path: polygon(14px 0%, calc(100% - 14px) 0%, 100% 14px, 100% calc(100% - 14px), calc(100% - 14px) 100%, 14px 100%, 0% calc(100% - 14px), 0% 14px);
    }
    .px-spin-btn span {
      position: relative;
      z-index: 1;
    }
    .px-spin-btn:hover:not(:disabled) {
      filter: brightness(1.1);
    }
    .px-spin-btn:active:not(:disabled) {
      transform: translate(2px, 2px);
    }
    .px-spin-btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  `],
})
export class PxControls {
  readonly credits = input(0);
  readonly bet = input(0);
  readonly machineState = input<MachineState>('idle');
  readonly increaseBet = output<void>();
  readonly decreaseBet = output<void>();
  readonly spin = output<void>();
}
