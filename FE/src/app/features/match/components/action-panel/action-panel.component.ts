import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MatchAttackView, MatchPhase, MatchPlayerStateView } from '../../services/game-state.service';
import { TranslateModule } from '@ngx-translate/core';

export type ActionType = 'ATTACH_ENERGY' | 'END_TURN' | 'RETREAT' | 'ATTACK' | 'PASS_ATTACK';

export interface ActionEvent {
  type: ActionType;
  attackIndex?: number;
}

const ENERGY_COLORS: Record<string, string> = {
  Colorless: '#a8a878',
  Fire: '#f08030',
  Water: '#6890f0',
  Lightning: '#f8d030',
  Grass: '#78c850',
  Psychic: '#f85888',
  Fighting: '#c03028',
  Darkness: '#705848',
  Metal: '#b8b8d0',
  Dragon: '#7038f8',
  Fairy: '#ee99ac',
};

@Component({
  selector: 'app-action-panel',
  imports: [MyButton, TranslateModule],
  templateUrl: './action-panel.component.html',
  styleUrl: './action-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionPanelComponent {
  readonly currentPhase = input.required<MatchPhase>();
  readonly playerState = input.required<MatchPlayerStateView>();

  readonly actionSelected = output<ActionEvent>();

  readonly isMainPhase = computed(() => this.currentPhase() === 'MAIN');
  readonly isAttackPhase = computed(() => this.currentPhase() === 'ATTACK');

  readonly activePokemon = computed(() => this.playerState()?.activePokemon ?? null);
  readonly attacks = computed<MatchAttackView[]>(() => this.activePokemon()?.attacks ?? []);

  readonly hasEnergyInHand = computed(() =>
    (this.playerState()?.hand ?? []).some(c => c.supertype === 'Energy'),
  );

  readonly canAttachEnergy = computed(() =>
    this.isMainPhase() &&
    this.hasEnergyInHand() &&
    !this.playerState()?.hasAttachedEnergyThisTurn,
  );

  readonly canRetreat = computed(() =>
    this.isMainPhase() && !!this.playerState()?.canRetreat,
  );

  readonly attacksWithDisabled = computed(() =>
    this.attacks().map(attack => ({
      attack,
      disabled: !this.canPayEnergyCost(attack.cost ?? [], this.activePokemon()?.attachedEnergies ?? []),
    })),
  );

  emitAction(type: ActionType, attackIndex?: number): void {
    this.actionSelected.emit({ type, attackIndex });
  }

  canPayEnergyCost(cost: string[], attached: string[]): boolean {
    const remaining = [...attached];
    for (const required of cost.filter(e => e !== 'Colorless')) {
      const idx = remaining.indexOf(required);
      if (idx === -1) {
        return false;
      }
      remaining.splice(idx, 1);
    }
    const colorlessNeeded = cost.filter(e => e === 'Colorless').length;
    return remaining.length >= colorlessNeeded;
  }

  energyBg(type: string): string {
    return ENERGY_COLORS[type] ?? '#a8a878';
  }
}
