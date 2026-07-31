import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../data-access/services/auth.service';
import { GameEngineService } from '../../data-access/services/game-engine.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/services/language.service';

@Component({
  selector: 'app-game-character-bar',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <div id="character-bar">
      <div class="bar-left">
        <span class="bar-username">{{ authService.currentUser()?.trainerName ?? engineService.getUsername() }}</span>
      </div>
      <div class="bar-center">
        <span class="bar-deck">{{ 'CHARACTER_BAR.SELECTED_DECK' | translate }}{{ activeDeckName() ?? ('CHARACTER_BAR.NO_DECK' | translate) }}</span>
      </div>
      <div class="bar-right">
        <div class="bar-stats">
          <img src="/assets/img/Moneda.png" alt="" class="coin-icon" />
          <span class="bar-coins">{{ authService.currentUser()?.coins ?? 0 }}</span>
          <span class="stat-separator"></span>
          <span class="bar-level">{{ levelText() }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    #character-bar {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 56px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.5rem;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(0,170,255,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(0,170,255,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      border-bottom: none;
      box-shadow:
        0 -2px 0 0 var(--pixel-border-hover-bottom-right),
        0 2px 0 0 var(--pixel-border-top-left),
        0 4px 0 0 #050510,
        0 2px 12px rgba(0, 0, 0, 0.5);
      z-index: 500;
      box-sizing: border-box;
    }
    .bar-left, .bar-center, .bar-right {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      height: 100%;
    }
    .bar-username {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.7rem;
      line-height: 1.6;
      color: var(--neon-cyan);
    }
    .bar-deck {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.65rem;
      line-height: 1.6;
      color: var(--neon-cyan);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .coin-icon {
      width: 20px;
      height: 20px;
      object-fit: contain;
    }
    .bar-stats {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      height: 100%;
    }
    .stat-separator {
      width: 2px;
      height: 20px;
      background: rgba(0,170,255,0.25);
    }
    .bar-coins {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.65rem;
      line-height: 1.6;
      color: #f5c842;
    }
    .bar-level {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.65rem;
      line-height: 1;
      color: #f5c842;
    }
  `],
})
export class GameCharacterBarComponent {
  authService = inject(AuthService);
  engineService = inject(GameEngineService);
  readonly langService = inject(LanguageService);
  readonly translateService = inject(TranslateService);

  activeDeckName = computed(() => {
    const user = this.authService.currentUser();
    if (!user?.activeDeckId || !user.decks) return null;
    return user.decks.find(d => d.id === user.activeDeckId)?.name ?? null;
  });

  levelText = computed(() => {
    this.langService.currentLang();
    return `${this.translateService.instant('CHARACTER_BAR.LEVEL')} ${this.authService.currentUser()?.level ?? 0}`;
  });
}
