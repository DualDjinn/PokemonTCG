import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
  signal,
} from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { JudgeMewComponent } from '../../../judge/components/judge-mew/judge-mew.component';
import { JudgeWidgetComponent } from '../../../judge/components/judge-widget/judge-widget.component';
import { JudgeIntroModalComponent } from '../../../judge/components/judge-intro-modal/judge-intro-modal.component';

import { JudgeMewService } from '../../../judge/services/judge-mew.service';
import { JudgeService } from '../../../judge/services/judge.service';

import {
  JudgeMewState,
  JudgeQueryCategory,
  JudgeResponse,
} from '../../../judge/models/judge.types';

@Component({
  selector: 'app-mew-playground',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslateModule,
    JsonPipe,
    JudgeMewComponent,
    JudgeWidgetComponent,
    JudgeIntroModalComponent,
  ],
  styleUrl: './mew-playground.page.css',
  template: `
    <div class="playground">
      <header class="playground__header">
        <h1>Mew Playground</h1>
        <p>Pestaña de prueba aislada del feature Judge Mew.</p>
      </header>

      <div class="playground__grid">

        <section class="card card--preview">
          <h2>Preview</h2>
          <div class="preview-stage">
            <app-judge-mew />
          </div>
          <p class="state-readout">
            Estado actual:
            <strong>{{ mewService.state() }}</strong>
          </p>
        </section>

        <section class="card">
          <h2>Estados del sprite</h2>
          <p class="hint">Dispara cada animación para verla en el preview.</p>
          <div class="btn-grid">
            @for (s of states; track s) {
              <button class="btn" (click)="setState(s)">{{ s }}</button>
            }
            <button class="btn btn--reset" (click)="mewService.backToIdle()">backToIdle</button>
          </div>
        </section>

        <section class="card">
          <h2>Intro modal</h2>
          <p class="hint">Renderiza el modal con typewriter y Mew2 hablando.</p>
          <button class="btn btn--primary" (click)="openIntro()">Abrir intro modal</button>
        </section>

        <section class="card">
          <h2>Bocadillo (speech)</h2>
          <p class="hint">Muestra texto en el bocadillo del widget. Auto-desaparece a los 8s.</p>
          <textarea
            class="ta"
            rows="3"
            [(ngModel)]="speechText"
            placeholder="Texto a decir..."
          ></textarea>
          <div class="row">
            <button class="btn btn--primary" (click)="say()">Decir</button>
            <button class="btn" (click)="mewService.clearSpeech()">Limpiar</button>
          </div>
        </section>

        <section class="card">
          <h2>Sesión Judge</h2>
          <p class="hint">
            Cuando está activa, el <code>app-judge-widget</code> se renderiza fijo
            abajo a la derecha (arrastrable). Sin sesión activa, el widget no aparece.
          </p>
          <p class="state-readout">
            Sesión: <strong>{{ mewService.botSessionActive() ? 'ACTIVA' : 'inactiva' }}</strong>
          </p>
          <div class="row">
            <button
              class="btn btn--primary"
              (click)="mewService.startBotSession()"
              [disabled]="mewService.botSessionActive()"
            >Activar</button>
            <button
              class="btn"
              (click)="mewService.endBotSession()"
              [disabled]="!mewService.botSessionActive()"
            >Desactivar</button>
          </div>
          <p class="state-readout">
            Enabled (toggle global): <strong>{{ mewService.enabled() ? 'sí' : 'no' }}</strong>
          </p>
          <button class="btn" (click)="mewService.toggleEnabled()">Alternar enabled</button>
        </section>

        <section class="card card--be">
          <h2>Llamada real al BE</h2>
          <p class="hint">
            Llama <code>GET /api/matches/&#123;matchId&#125;/judge/query</code> con la sesión actual.
            Requiere un <code>matchId</code> válido y el BE corriendo.
          </p>
          <label>matchId
            <input class="input" type="text" [(ngModel)]="beMatchId" autocomplete="off" placeholder="uuid del match" />
          </label>
          <label>Categoría
            <select class="input" [(ngModel)]="beCategory">
              @for (cat of categories; track cat) {
                <option [value]="cat">{{ cat }}</option>
              }
            </select>
          </label>
          <button class="btn btn--primary" (click)="callBE()" [disabled]="beLoading()">
            {{ beLoading() ? 'Llamando...' : 'Consultar al BE' }}
          </button>
          @if (beError(); as err) {
            <p class="error">❌ {{ err }}</p>
          }
          @if (beResponse(); as res) {
            <pre class="response">{{ res | json }}</pre>
          }
        </section>

      </div>

      <app-judge-widget [matchId]="beMatchId || 'playground'" />

      <app-judge-intro-modal
        [open]="introOpen()"
        [text]="introText"
        [durationMs]="null"
        (closed)="onIntroClosed()"
      />
    </div>
  `,
})
export class MewPlaygroundPage {
  protected readonly mewService = inject(JudgeMewService);
  private readonly judgeService = inject(JudgeService);

  readonly states: Exclude<JudgeMewState, 'idle'>[] = [
    'thinking', 'approved', 'rejected', 'teleport', 'celebrate',
  ];

  readonly categories: JudgeQueryCategory[] = Object.values(JudgeQueryCategory);

  readonly introOpen = signal(false);
  readonly introText =
    '¡Hola entrenador! Soy Mew, tu asistente virtual. Esta es una pestaña de prueba ' +
    'para visualizar mis animaciones, el modal con typewriter, el bocadillo y el drag. ' +
    'Hacé click en ACEPTAR para cerrar.';

  speechText = '¿Por qué no puedo atacar con Pikachu?';

  beMatchId = '';
  beCategory: JudgeQueryCategory = JudgeQueryCategory.VICTORY_CONDITIONS;
  readonly beLoading = signal(false);
  readonly beResponse = signal<JudgeResponse | null>(null);
  readonly beError = signal<string | null>(null);

  setState(state: Exclude<JudgeMewState, 'idle'>): void {
    const method =
      `trigger${state.charAt(0).toUpperCase()}${state.slice(1)}` as
      'triggerThinking' | 'triggerApproved' | 'triggerRejected' | 'triggerTeleport' | 'triggerCelebrate';
    this.mewService[method]();
  }

  openIntro(): void {
    this.introOpen.set(true);
  }

  onIntroClosed(): void {
    this.introOpen.set(false);
  }

  say(): void {
    const text = (this.speechText ?? '').trim();
    if (!text) return;
    if (!this.mewService.botSessionActive()) {
      this.mewService.startBotSession();
    }
    this.mewService.showSpeech(text, 8000);
  }

  callBE(): void {
    const matchId = this.beMatchId.trim();
    if (!matchId) {
      this.beError.set('Ingresá un matchId.');
      return;
    }
    this.beError.set(null);
    this.beResponse.set(null);
    this.beLoading.set(true);

    this.judgeService.query(matchId, { category: this.beCategory, lang: 'es' }).subscribe({
      next: (res) => {
        this.beResponse.set(res);
        this.beLoading.set(false);
        if (this.mewService.botSessionActive()) {
          this.mewService.showSpeech(res.summary, 10000);
        }
      },
      error: (err) => {
        const status = err?.status ?? '???';
        const msg = err?.error?.message ?? err?.message ?? 'Error desconocido';
        this.beError.set(`HTTP ${status} — ${msg}`);
        this.beLoading.set(false);
      },
    });
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    if (this.introOpen()) this.introOpen.set(false);
  }
}
