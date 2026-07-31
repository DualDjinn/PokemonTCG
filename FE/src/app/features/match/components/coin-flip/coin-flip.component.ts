import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-coin-flip',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './coin-flip.component.html',
  styleUrl: './coin-flip.component.css',
})
export class CoinFlipComponent implements OnChanges {
  @ViewChild('coin') coinRef!: ElementRef<HTMLDivElement>;
  @ViewChild('shadow') shadowRef!: ElementRef<HTMLDivElement>;

  @Input() playerRole: 'chooser' | 'waiter' = 'chooser';
  @Input() wasCoinChooser: boolean = false;
  @Input() canChooseStarter: boolean = false;
  @Input() openingState: string | null = null;
  @Input() openingCoinResult: string | null = null;
  @Input() openingStarterPlayerId: string | null = null;
  @Input() chooserChoice: string | null = null;
  @Output() sequenceComplete = new EventEmitter<'me' | 'opponent'>();
  @Output() coinSideChosen = new EventEmitter<'heads' | 'tails'>();
  @Output() starterChosen = new EventEmitter<'me' | 'opponent'>();

  readonly state = signal<'choosing' | 'waiting' | 'flipping' | 'result' | 'chooseTurn' | 'done'>('choosing');

  flipping = false;
  result: 'heads' | 'tails' | null = null;
  correctGuess = false;
  pick: 'heads' | 'tails' | null = null;
  firstTurn: 'me' | 'opponent' | null = null;
  hadChanceToChoose = false;
  private currentDeg = 0;
  private chooseTurnTimeout: ReturnType<typeof setTimeout> | null = null;
  private animationPlaying = false;
  history: ('heads' | 'tails')[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['openingState'] || changes['openingCoinResult'] || changes['openingStarterPlayerId'] || changes['canChooseStarter'] || changes['chooserChoice']) {
      this.syncFromOpeningState();
    }
  }

  choose(side: 'heads' | 'tails'): void {
    if (this.flipping || this.playerRole !== 'chooser') return;
    this.hadChanceToChoose = true;
    this.pick = side;
    this.flipping = true;
    this.state.set('flipping');
    this.animationPlaying = true;
    setTimeout(() => this.spinCoin(), 0);
    this.coinSideChosen.emit(side);
  }

  chooseTurn(who: 'me' | 'opponent'): void {
    this.firstTurn = who;
    this.starterChosen.emit(who);
  }

  private syncFromOpeningState(): void {
    if (this.chooseTurnTimeout !== null) {
      clearTimeout(this.chooseTurnTimeout);
      this.chooseTurnTimeout = null;
    }

    if (this.animationPlaying) {
      return;
    }

    if (this.openingState === 'COIN_CHOICE') {
      this.flipping = false;
      if (this.playerRole === 'chooser') {
        this.state.set('choosing');
      } else {
        this.state.set('waiting');
      }
      return;
    }

    if (this.openingCoinResult && this.openingState === 'COIN_RESULT' && (this.state() === 'waiting' || this.state() === 'choosing')) {
      this.state.set('flipping');
      this.animationPlaying = true;
      requestAnimationFrame(() => requestAnimationFrame(() => this.spinCoin()));
      return;
    }

    if (this.openingCoinResult && (this.openingState === 'COIN_RESULT' || this.state() === 'flipping')) {
      this.flipping = false;
      this.result = this.openingCoinResult === 'heads' ? 'heads' : 'tails';

      const resultDeg = this.openingCoinResult === 'tails' ? -180 : 0;
      this.currentDeg = resultDeg;
      if (this.coinRef?.nativeElement) {
        this.coinRef.nativeElement.style.transform = `translateY(0px) rotateX(${resultDeg}deg)`;
      }

      if (this.shadowRef?.nativeElement) {
        this.shadowRef.nativeElement.style.transform = 'translateX(-50%) scale(1)';
        this.shadowRef.nativeElement.style.opacity = '1';
      }

      this.updateStats(this.result);

      const playerPick = this.wasCoinChooser ? this.pick : this.chooserChoice;
      this.correctGuess = !!playerPick && playerPick === this.result;

      this.state.set('result');

      this.chooseTurnTimeout = setTimeout(() => {
        if (this.state() === 'result') {
          this.state.set(this.canChooseStarter ? 'chooseTurn' : 'waiting');
        }
      }, 2000);
      return;
    }

    if (this.openingState === 'DEALING') {
      return;
    }

    if (this.openingState === 'COMPLETE') {
      this.flipping = false;
      this.state.set('done');
      this.sequenceComplete.emit('opponent');
      return;
    }

    this.flipping = false;
    this.state.set(this.playerRole === 'chooser' ? 'choosing' : 'waiting');
  }

  private spinCoin(): void {
    if (!this.coinRef?.nativeElement) {
      return;
    }

    const extraSpins = (3 + Math.floor(Math.random() * 3)) * 360;
    const targetOffset = this.openingCoinResult === 'tails' ? 180 : 0;
    const targetDeg = this.currentDeg - extraSpins - targetOffset;
    const duration = 1800 + Math.random() * 600;
    const startTime = performance.now();
    const startDeg = this.currentDeg;
    const jumpHeight = 90;

    const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);

    const animate = (now: number) => {
      if (!this.coinRef?.nativeElement || this.state() !== 'flipping') {
        return;
      }

      const t = Math.min((now - startTime) / duration, 1);
      const deg = startDeg + easeOut(t) * (targetDeg - startDeg);
      const jumpT = 4 * t * (1 - t);
      const jump = jumpHeight * jumpT;

      this.coinRef.nativeElement.style.transform = `translateY(${-jump}px) rotateX(${deg}deg)`;

      if (this.shadowRef?.nativeElement) {
        const scale = 1 - jumpT * 0.5;
        const opacity = 1 - jumpT * 0.6;
        this.shadowRef.nativeElement.style.transform = `translateX(-50%) scale(${scale})`;
        this.shadowRef.nativeElement.style.opacity = String(opacity);
      }

      if (t < 1) {
        requestAnimationFrame(animate);
      } else {
        this.currentDeg = this.openingCoinResult === 'heads' ? 0 : -180;
        this.coinRef.nativeElement.style.transform = `translateY(0px) rotateX(${this.currentDeg}deg)`;
        if (this.shadowRef?.nativeElement) {
          this.shadowRef.nativeElement.style.transform = 'translateX(-50%) scale(1)';
          this.shadowRef.nativeElement.style.opacity = '1';
        }
        this.animationPlaying = false;
        this.syncFromOpeningState();
      }
    };

    requestAnimationFrame(animate);
  }

  private updateStats(outcome: 'heads' | 'tails'): void {
    this.history = [...this.history.slice(-19), outcome];
  }
}
