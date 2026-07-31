import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-judge-intro-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslateModule],
  styleUrl: './judge-intro-modal.component.css',
  template: `
    @if (open()) {
      <div class="intro-overlay">
        <div class="intro-modal">

          <div class="intro-header">
            <span class="intro-icon">⚖</span>
            <span class="intro-title">{{ 'JUDGE.INTRO_TITLE' | translate }}</span>
            <span class="intro-icon">⚖</span>
          </div>

          <div class="intro-body">

            <div class="intro-speaker">
              <div class="speaker-glow"></div>
              <img
                class="speaker-img"
                src="/assets/img/character/judge-mew/mew2.gif"
                alt=""
                draggable="false"
              />
              <div class="speak-bubbles" [class.silent]="canAccept()">
                <span class="bub b1"></span>
                <span class="bub b2"></span>
                <span class="bub b3"></span>
              </div>
            </div>

            <div class="intro-text-wrap">
              <p class="intro-text">{{ displayedText() }}<span class="cursor" [class.hidden]="canAccept()">&#9612;</span></p>
            </div>
          </div>

          <div class="intro-footer">
            @if (canAccept()) {
              <button class="accept-btn" (click)="onAccept()">
                {{ 'JUDGE.ACCEPT' | translate }}
              </button>
            } @else {
              <button class="skip-btn" (click)="onSkip()" [title]="'JUDGE.SKIP' | translate">
                ▶▶
              </button>
            }
          </div>
        </div>
      </div>
    }
  `,
})
export class JudgeIntroModalComponent implements OnDestroy {
  readonly open = input.required<boolean>();
  readonly text = input.required<string>();
  readonly durationMs = input<number | null>(null);
  readonly closed = output<void>();

  readonly displayedText = signal('');
  readonly canAccept = signal(false);

  private interval: ReturnType<typeof setInterval> | null = null;
  private speakAudio: HTMLAudioElement | null = null;

  constructor() {
    effect(() => {
      if (this.open()) {
        this.startTypewriter();
      } else {
        this.reset();
      }
    });
  }

  private startTypewriter(): void {
    this.clear();
    this.displayedText.set('');
    this.canAccept.set(false);
    this.playSpeakSound();
    const full = this.text();
    const target = this.durationMs();
    const delay = (target && target > 0 && full.length > 0)
      ? Math.max(12, target / full.length)
      : 35;
    let i = 0;
    this.interval = setInterval(() => {
      if (i >= full.length) {
        this.clear();
        this.canAccept.set(true);
        return;
      }
      this.displayedText.update(t => t + full.charAt(i));
      i++;
    }, delay);
  }

  private reset(): void {
    this.clear();
    this.stopSpeakSound();
    this.displayedText.set('');
    this.canAccept.set(false);
  }

  private clear(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  private playSpeakSound(): void {
    this.stopSpeakSound();
    try {
      const audio = new Audio('/assets/img/character/judge-mew/voice/hablar.mpeg');
      audio.volume = 0.5;
      this.speakAudio = audio;
      audio.play().catch(() => {});
    } catch {
    }
  }

  /** Detiene y descarta la voz si está sonando (al cerrar, reabrir o destruir). */
  private stopSpeakSound(): void {
    if (this.speakAudio) {
      this.speakAudio.pause();
      this.speakAudio.currentTime = 0;
      this.speakAudio = null;
    }
  }

  ngOnDestroy(): void {
    this.clear();
    this.stopSpeakSound();
  }

  onAccept(): void {
    this.closed.emit();
  }

  onSkip(): void {
    this.clear();
    this.displayedText.set(this.text());
    this.canAccept.set(true);
  }
}
