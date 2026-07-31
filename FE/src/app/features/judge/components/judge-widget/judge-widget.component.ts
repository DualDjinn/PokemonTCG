import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  HostListener,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule } from '@ngx-translate/core';
import { JudgeMewService } from '../../services/judge-mew.service';
import { JudgeService } from '../../services/judge.service';
import { JudgeMewComponent } from '../judge-mew/judge-mew.component';
import { JudgeIntroModalComponent } from '../judge-intro-modal/judge-intro-modal.component';
import { GameActionType, JudgeQueryCategory } from '../../models/judge.types';

/** Un tema de ayuda del menú del juez: etiqueta visible + consulta al backend. */
interface HelpTopic {
  label: string;
  category: JudgeQueryCategory;
  targetAction?: GameActionType;
}

@Component({
  selector: 'app-judge-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [JudgeMewComponent, TranslateModule, JudgeIntroModalComponent],
  styleUrl: './judge-widget.component.css',
  template: `
    @if (mewService.enabled() && mewService.botSessionActive()) {
      <div
        class="judge-widget"
        [class.judge-widget--dragging]="isDragging()"
        [class.judge-widget--positioned]="position() !== null"
        [style.left.px]="position()?.x"
        [style.top.px]="position()?.y"
        title="Hacé click para pedirme ayuda sobre las reglas"
        (mousedown)="onDragStart($event)"
        (click)="onMewClick()"
      >
        <!-- Menú de ayuda sobre las reglas (se abre al hacer click en el Mew). -->
        @if (helpOpen()) {
          <div
            class="judge-menu"
            (mousedown)="$event.stopPropagation()"
            (click)="$event.stopPropagation()"
          >
            <div class="judge-menu__title">¿En qué te ayudo?</div>
            @for (topic of helpTopics; track topic.label) {
              <button class="judge-menu__item" type="button" (click)="askJudge(topic)">
                {{ topic.label }}
              </button>
            }
          </div>
        }

        @if (mewService.speech(); as text) {
          <div class="judge-bubble">
            <p class="judge-bubble__text">{{ text }}</p>
            <button
              class="judge-bubble__close"
              type="button"
              (click)="onDismissBubble($event)"
              [attr.aria-label]="'JUDGE.CLOSE' | translate"
            >✕</button>
            <div class="judge-bubble__tail"></div>
          </div>
        }
        <app-judge-mew />
      </div>
    }

    <!-- Presentación del juez: se abre SÓLO la primera vez que se hace click en el Mew. -->
    <app-judge-intro-modal
      [open]="introOpen()"
      [text]="introText"
      (closed)="introOpen.set(false)"
    />
  `,
})
export class JudgeWidgetComponent {
  readonly matchId = input.required<string>();
  readonly mewService = inject(JudgeMewService);
  private readonly judgeService = inject(JudgeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly position = signal<{ x: number; y: number } | null>(null);
  readonly isDragging = signal(false);
  private dragOffset = { x: 0, y: 0 };
  private dragMoved = false;

  /** Estado del modal de presentación; se abre una única vez al hacer click en el Mew. */
  readonly introOpen = signal(false);
  /** El saludo es sólo inicial: una vez mostrado, los clicks abren el menú de ayuda. */
  private introShown = false;
  readonly introText =
    '¡Hola, entrenador! Soy Mew, tu juez asistente. Durante la batalla te aviso ' +
    'cuándo una jugada no está permitida y te explico por qué según las reglas. ' +
    'Si tenés una duda, hacé click sobre mí y elegí un tema. ¡Mucha suerte!';

  /** Menú de ayuda sobre las reglas; se abre/cierra al hacer click en el Mew. */
  readonly helpOpen = signal(false);
  readonly helpTopics: ReadonlyArray<HelpTopic> = [
    { label: '¿Por qué no puedo atacar?', category: JudgeQueryCategory.WHY_CANT_I, targetAction: GameActionType.ATTACK },
    { label: '¿Qué me limita ahora?', category: JudgeQueryCategory.CURRENT_RESTRICTIONS },
    { label: '¿Cómo se calcula el daño?', category: JudgeQueryCategory.EXPLAIN_DAMAGE },
    { label: 'Estados especiales', category: JudgeQueryCategory.EXPLAIN_STATUS },
    { label: '¿Cómo se gana la partida?', category: JudgeQueryCategory.VICTORY_CONDITIONS },
  ];

  onDragStart(event: MouseEvent): void {
    if ((event.target as HTMLElement).closest('.judge-bubble__close')) return;
    event.preventDefault();

    try {
      const a = new Audio('/assets/img/character/judge-mew/voice/mover.mpeg');
      a.volume = 0.4;
      a.play().catch(() => {});
    } catch {
    }

    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    if (this.position() === null) {
      this.position.set({ x: rect.left, y: rect.top });
    }
    this.dragOffset = {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
    };
    this.isDragging.set(true);
    this.dragMoved = false;
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.isDragging()) return;
    this.dragMoved = true;
    const width = 150;
    const height = 200;
    const newX = event.clientX - this.dragOffset.x;
    const newY = event.clientY - this.dragOffset.y;
    const clampedX = Math.max(0, Math.min(window.innerWidth - width, newX));
    const clampedY = Math.max(0, Math.min(window.innerHeight - height, newY));
    this.position.set({ x: clampedX, y: clampedY });
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp(): void {
    if (this.isDragging()) {
      this.isDragging.set(false);
    }
  }

  /**
   * Click en el Mew (sin arrastrar). La primera vez muestra la presentación inicial;
   * a partir de ahí abre/cierra el menú de ayuda sobre las reglas.
   */
  onMewClick(): void {
    if (this.dragMoved) {
      return;
    }
    if (!this.introShown) {
      this.introShown = true;
      this.mewService.triggerTeleport();
      this.introOpen.set(true);
      return;
    }
    const next = !this.helpOpen();
    this.helpOpen.set(next);
    if (next) {
      this.mewService.clearSpeech();
    }
  }

  /**
   * Consulta al juez del backend sobre la regla elegida y muestra la explicación en
   * el bocadillo de Mew. El backend ya conoce las reglas del set XY1.
   */
  askJudge(topic: HelpTopic): void {
    this.helpOpen.set(false);
    const matchId = this.matchId();
    if (!matchId) {
      return;
    }
    this.mewService.triggerThinking();
    this.judgeService.query(matchId, {
      category: topic.category,
      targetAction: topic.targetAction,
      lang: 'es',
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.mewService.triggerApproved();
          const text = res.suggestion ? `${res.summary} ${res.suggestion}` : res.summary;
          this.mewService.showSpeech(text, 11000);
        },
        error: () => {
          this.mewService.backToIdle();
          this.mewService.showSpeech('No pude consultar las reglas en este momento. Probá de nuevo.', 6000);
        },
      });
  }

  onDismissBubble(event: MouseEvent): void {
    event.stopPropagation();
    this.mewService.clearSpeech();
  }
}
