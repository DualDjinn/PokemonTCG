import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-px-hud',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="px-led">
      <span class="px-led-label credits">{{ 'SLOT_MACHINE.COINS' | translate }}</span>
      <span class="px-led-value credits">{{ coins() }}</span>
    </div>
    <div class="px-led">
      <span class="px-led-label win">{{ 'SLOT_MACHINE.WIN_LABEL' | translate }}</span>
      <span class="px-led-value win">{{ winAmount() }}</span>
    </div>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: row;
      gap: 16px;
    }
    .px-led {
      background: var(--px-dark);
      border: 3px solid var(--px-shadow);
      outline: 1px solid rgba(255,106,0,0.267);
      padding: 10px 20px;
      text-align: center;
      min-width: 162px;
    }
    .px-led-label {
      font-size: 7px;
      display: block;
      margin-bottom: 4px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .px-led-label.credits {
      color: var(--px-gold);
      text-shadow: 0 0 6px var(--px-gold);
    }
    .px-led-label.win {
      color: var(--px-win);
    }
    .px-led-value {
      font-size: 16px;
      display: block;
    }
    .px-led-value.credits {
      color: var(--px-gold);
      text-shadow: 0 0 6px var(--px-gold);
    }
    .px-led-value.win {
      color: var(--px-win);
      text-shadow: 0 0 6px var(--px-win);
    }
  `],
})
export class PxHud {
  readonly coins = input(0);
  readonly winAmount = input(0);
}
