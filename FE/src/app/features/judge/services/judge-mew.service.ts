import { Injectable, signal } from '@angular/core';
import { JudgeMewState } from '../models/judge.types';

const STORAGE_KEY = 'judge_mew_enabled';

@Injectable({ providedIn: 'root' })
export class JudgeMewService {
  private readonly _enabled = signal<boolean>(this.loadEnabled());
  private readonly _state = signal<JudgeMewState>('idle');
  private readonly _botSessionActive = signal<boolean>(false);
  private readonly _speech = signal<string | null>(null);
  private stateTimeout: ReturnType<typeof setTimeout> | null = null;
  private speechTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly enabled = this._enabled.asReadonly();
  readonly state = this._state.asReadonly();
  readonly botSessionActive = this._botSessionActive.asReadonly();
  readonly speech = this._speech.asReadonly();

  toggleEnabled(): void {
    const next = !this._enabled();
    this._enabled.set(next);
    localStorage.setItem(STORAGE_KEY, String(next));
  }

  startBotSession(): void {
    this._botSessionActive.set(true);
  }

  endBotSession(): void {
    this._botSessionActive.set(false);
    this.backToIdle();
    this.clearSpeech();
  }

  showSpeech(text: string, durationMs = 7000): void {
    if (this.speechTimeout) {
      clearTimeout(this.speechTimeout);
      this.speechTimeout = null;
    }
    this._speech.set(text);
    // Los avisos de juego se muestran sin voz; sólo el modal de presentación habla.
    if (durationMs > 0) {
      this.speechTimeout = setTimeout(() => this.clearSpeech(), durationMs);
    }
  }

  clearSpeech(): void {
    if (this.speechTimeout) {
      clearTimeout(this.speechTimeout);
      this.speechTimeout = null;
    }
    this._speech.set(null);
  }

  triggerThinking(): void {
    this.clearTimeout();
    this._state.set('thinking');
    this.playSound('move');
  }

  triggerApproved(): void {
    this.playSound('move');
    this.transitionFor('approved', 900);
  }

  triggerRejected(): void {
    this.playSound('move');
    this.transitionFor('rejected', 900);
  }

  triggerTeleport(): void {
    this.playSound('move');
    this.transitionFor('teleport', 700);
  }

  triggerCelebrate(): void {
    this.playSound('move');
    this.transitionFor('celebrate', 3100);
  }

  backToIdle(): void {
    this.clearTimeout();
    this._state.set('idle');
  }

  private transitionFor(state: JudgeMewState, duration: number): void {
    this.clearTimeout();
    this._state.set(state);
    this.stateTimeout = setTimeout(() => this._state.set('idle'), duration);
  }

  private clearTimeout(): void {
    if (this.stateTimeout) {
      clearTimeout(this.stateTimeout);
      this.stateTimeout = null;
    }
  }

  private playSound(type: 'speak' | 'move'): void {
    try {
      const src = type === 'speak'
        ? '/assets/img/character/judge-mew/voice/hablar.mpeg'
        : '/assets/img/character/judge-mew/voice/mover.mpeg';
      const audio = new Audio(src);
      audio.volume = 0.5;
      audio.play().catch(() => {});
    } catch {
    }
  }

  private loadEnabled(): boolean {
    return localStorage.getItem(STORAGE_KEY) !== 'false';
  }
}
