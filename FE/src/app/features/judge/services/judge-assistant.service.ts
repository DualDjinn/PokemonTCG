import { Injectable, inject } from '@angular/core';
import { JudgeService } from './judge.service';
import { JudgeMewService } from './judge-mew.service';
import { LanguageService } from '../../../core/services/language.service';
import { GameActionType, JudgeQueryCategory } from '../models/judge.types';

@Injectable({ providedIn: 'root' })
export class JudgeAssistantService {
  private readonly judgeService = inject(JudgeService);
  private readonly mewService = inject(JudgeMewService);
  private readonly languageService = inject(LanguageService);

  explainBlockedAction(
    matchId: string,
    action: GameActionType,
    ctx: { cardId?: string; pokemonId?: string } = {},
  ): void {
    if (!matchId) return;
    if (!this.mewService.enabled() || !this.mewService.botSessionActive()) return;

    const lang = this.languageService.currentLang();
    const judgeLang: 'es' | 'en' = lang === 'en' ? 'en' : 'es';

    this.mewService.triggerThinking();
    this.judgeService.query(matchId, {
      category: JudgeQueryCategory.WHY_CANT_I,
      targetAction: action,
      targetCardId: ctx.cardId,
      targetPokemonId: ctx.pokemonId,
      lang: judgeLang,
    }).subscribe({
      next: (res) => {
        this.mewService.showSpeech(res.summary, 8000);
        this.mewService.triggerApproved();
      },
      error: () => {
        this.mewService.backToIdle();
      },
    });
  }

  say(text: string, durationMs = 7000): void {
    if (!this.mewService.enabled() || !this.mewService.botSessionActive()) return;
    this.mewService.showSpeech(text, durationMs);
  }
}
