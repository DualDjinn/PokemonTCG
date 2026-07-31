import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  GameActionType,
  JudgeQueryCategory,
  JudgeQueryParams,
  JudgeResponse,
} from '../../models/judge.types';
import { JudgeService } from '../../services/judge.service';
import { JudgeMewService } from '../../services/judge-mew.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { LanguageService } from '../../../../core/services/language.service';

type PanelView = 'categories' | 'action-select' | 'loading' | 'response';

const CATEGORY_META: Record<JudgeQueryCategory, { icon: string; key: string }> = {
  [JudgeQueryCategory.WHY_CANT_I]:           { icon: '❓', key: 'JUDGE.CATEGORIES.WHY_CANT_I' },
  [JudgeQueryCategory.EXPLAIN_DAMAGE]:       { icon: '⚔️', key: 'JUDGE.CATEGORIES.EXPLAIN_DAMAGE' },
  [JudgeQueryCategory.EXPLAIN_STATUS]:       { icon: '💫', key: 'JUDGE.CATEGORIES.EXPLAIN_STATUS' },
  [JudgeQueryCategory.VICTORY_CONDITIONS]:   { icon: '🏆', key: 'JUDGE.CATEGORIES.VICTORY_CONDITIONS' },
  [JudgeQueryCategory.CURRENT_RESTRICTIONS]: { icon: '🚫', key: 'JUDGE.CATEGORIES.CURRENT_RESTRICTIONS' },
};

const ACTION_META: Record<GameActionType, { icon: string; key: string }> = {
  [GameActionType.PLAY_BASIC]:       { icon: '🃏', key: 'JUDGE.ACTIONS.PLAY_BASIC' },
  [GameActionType.EVOLVE]:           { icon: '🔄', key: 'JUDGE.ACTIONS.EVOLVE' },
  [GameActionType.ATTACH_ENERGY]:    { icon: '⚡', key: 'JUDGE.ACTIONS.ATTACH_ENERGY' },
  [GameActionType.PLAY_ITEM]:        { icon: '🎒', key: 'JUDGE.ACTIONS.PLAY_ITEM' },
  [GameActionType.PLAY_SUPPORTER]:   { icon: '👤', key: 'JUDGE.ACTIONS.PLAY_SUPPORTER' },
  [GameActionType.RETREAT]:          { icon: '🔙', key: 'JUDGE.ACTIONS.RETREAT' },
  [GameActionType.ADVANCE_TO_ATTACK]:{ icon: '▶️', key: 'JUDGE.ACTIONS.ADVANCE_TO_ATTACK' },
  [GameActionType.ATTACK]:           { icon: '💥', key: 'JUDGE.ACTIONS.ATTACK' },
  [GameActionType.END_TURN]:         { icon: '⏭️', key: 'JUDGE.ACTIONS.END_TURN' },
};

@Component({
  selector: 'app-judge-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslateModule],
  styleUrl: './judge-panel.component.css',
  template: `
    <div class="judge-panel" role="dialog" aria-modal="true" [attr.aria-label]="'JUDGE.TITLE' | translate">

      <div class="panel-header" [class.has-restriction]="response()?.hasActiveRestrictions">
        <span class="header-icon">⚖️</span>
        <span class="header-title">{{ 'JUDGE.TITLE' | translate }}</span>
        <button class="close-btn" (click)="onClose()" [attr.aria-label]="'JUDGE.CLOSE' | translate">✕</button>
      </div>

      @if (view() === 'categories') {
        <div class="panel-body">
          <p class="panel-subtitle">{{ 'JUDGE.SUBTITLE' | translate }}</p>
          <div class="category-list">
            @for (cat of categories; track cat) {
              <button class="category-btn" (click)="selectCategory(cat)">
                <span class="cat-icon">{{ categoryMeta[cat].icon }}</span>
                <span class="cat-label">{{ categoryMeta[cat].key | translate }}</span>
                <span class="cat-arrow">›</span>
              </button>
            }
          </div>
        </div>
      }

      @if (view() === 'action-select') {
        <div class="panel-body">
          <button class="back-btn" (click)="backToCategories()">
            ‹ {{ 'JUDGE.BACK' | translate }}
          </button>
          <p class="panel-subtitle">{{ 'JUDGE.ACTION_SUBTITLE' | translate }}</p>
          <div class="category-list">
            @for (action of actions; track action) {
              <button class="category-btn" (click)="selectAction(action)">
                <span class="cat-icon">{{ actionMeta[action].icon }}</span>
                <span class="cat-label">{{ actionMeta[action].key | translate }}</span>
                <span class="cat-arrow">›</span>
              </button>
            }
          </div>
        </div>
      }

      @if (view() === 'loading') {
        <div class="panel-body panel-body--center">
          <div class="loading-spinner"></div>
          <p class="loading-text">{{ 'JUDGE.LOADING' | translate }}</p>
        </div>
      }

      @if (view() === 'response') {
        <div class="panel-body">
          <button class="back-btn" (click)="backToCategories()">
            ‹ {{ 'JUDGE.BACK' | translate }}
          </button>

          @if (response(); as r) {
            @if (r.hasActiveRestrictions) {
              <div class="restriction-badge restriction-badge--active">
                🚫 {{ 'JUDGE.RESPONSE.RESTRICTION' | translate }}
              </div>
            } @else {
              <div class="restriction-badge restriction-badge--clear">
                ✅ {{ 'JUDGE.RESPONSE.NO_RESTRICTION' | translate }}
              </div>
            }

            <p class="response-summary">{{ r.summary }}</p>

            @if (r.reasons.length > 0) {
              <div class="response-section">
                <span class="section-label">{{ 'JUDGE.RESPONSE.REASONS' | translate }}</span>
                <ul class="reasons-list">
                  @for (reason of r.reasons; track $index) {
                    <li class="reason-item">{{ reason }}</li>
                  }
                </ul>
              </div>
            }

            @if (r.suggestion) {
              <div class="suggestion-box">
                <span class="suggestion-icon">💡</span>
                <span class="suggestion-text">{{ r.suggestion }}</span>
              </div>
            }

            @if (r.ruleReferences.length > 0) {
              <div class="rule-refs">
                <span class="rule-icon">📖</span>
                {{ r.ruleReferences.join(' · ') }}
              </div>
            }
          }
        </div>
      }
    </div>
  `,
})
export class JudgePanelComponent {
  readonly matchId = input.required<string>();
  readonly close = output<void>();

  private readonly judgeService = inject(JudgeService);
  private readonly judgeMewService = inject(JudgeMewService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly translate = inject(TranslateService);

  readonly view = signal<PanelView>('categories');
  readonly response = signal<JudgeResponse | null>(null);

  readonly categoryMeta = CATEGORY_META;
  readonly actionMeta = ACTION_META;
  readonly categories = Object.values(JudgeQueryCategory);
  readonly actions = Object.values(GameActionType);

  private selectedCategory: JudgeQueryCategory | null = null;

  selectCategory(category: JudgeQueryCategory): void {
    if (category === JudgeQueryCategory.WHY_CANT_I) {
      this.selectedCategory = category;
      this.view.set('action-select');
    } else {
      this.query({ category });
    }
  }

  selectAction(action: GameActionType): void {
    if (!this.selectedCategory) return;
    this.query({ category: this.selectedCategory, targetAction: action });
  }

  backToCategories(): void {
    this.view.set('categories');
    this.selectedCategory = null;
    this.response.set(null);
    this.judgeMewService.backToIdle();
  }

  onClose(): void {
    this.judgeMewService.backToIdle();
    this.close.emit();
  }

  private query(params: JudgeQueryParams): void {
    this.view.set('loading');
    this.judgeMewService.triggerThinking();

    const lang = this.languageService.currentLang();
    const judgeLang: 'es' | 'en' = lang === 'en' ? 'en' : 'es';

    this.judgeService.query(this.matchId(), {
      ...params,
      lang: judgeLang,
    }).subscribe({
      next: (res) => {
        this.response.set(res);
        this.view.set('response');
        this.judgeMewService.triggerApproved();
      },
      error: (err: HttpErrorResponse) => {
        this.view.set('categories');
        this.judgeMewService.triggerRejected();
        const msgKey = err.status === 404
          ? 'JUDGE.ERROR.NOT_STARTED'
          : err.status === 403
          ? 'JUDGE.ERROR.FORBIDDEN'
          : 'JUDGE.ERROR.GENERIC';
        this.toastService.show(this.translate.instant(msgKey), 'error');
      },
    });
  }
}
