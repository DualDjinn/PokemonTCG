import { Component, signal, computed, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, ViewChild, ElementRef, QueryList, inject, OnDestroy, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../game/data-access/services/auth.service';
import { StorageService } from '../../core/services/storage.service';
import { AudioSfxChannel } from '../../core/services/audio-sfx-channel.service';
import { AudioMusicChannel } from '../../core/services/audio-music-channel.service';
import { CircleTransitionService } from '../../shared/services/circle-transition.service';
import { SlotMachineService } from './data-access/services/slot-machine.service';
import { SlotMachineApiService } from './data-access/services/slot-machine-api.service';
import { PxReel } from './components/px-reel/px-reel';
import { PxHud } from './components/px-hud/px-hud';
import { PxControls } from './components/px-controls/px-controls';
import { PxPayTable } from './components/px-pay-table/px-pay-table';
import type { MachineState, MessageType, WinLineDetail, ActiveWinLine } from './domain/slot-machine.models';

@Component({
  selector: 'app-slot-machine',
  standalone: true,
  imports: [TranslateModule, PxReel, PxHud, PxControls, PxPayTable],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './slot-machine.page.css',
  template: `
  <div class="px-wrap">
    <div class="px-layout-container">
    <div class="px-top-row">
      <button class="px-back-btn" (click)="goBack()" [disabled]="navigating()"><span>{{ 'SLOT_MACHINE.BACK' | translate }}</span></button>
      <div class="px-title">
        ★ LUCKY 777 ★
        <span class="px-subtitle">PIXEL SLOTS · 3×3 · MULTILINE</span>
      </div>
    </div>

    <div class="px-body">
      <div class="px-lights">
        @for (l of [0,1,2,3,4,5,6]; track l) {
          <div class="px-light"
            [class.px-light-1]="l%3===0"
            [class.px-light-2]="l%3===1"
            [class.px-light-3]="l%3===2"
            [style.animation-delay]="[0,.3,.6,.9,.15][l%5]+'s'">
          </div>
        }
      </div>

      <div class="px-reels-row">
      <div class="px-left-col">
      <div class="px-reels-frame" #reelsFrame>
        <div class="px-reels">
          @for (col of [0,1,2]; track col) {
            <app-px-reel [symbols]="reelStrips()[col]" [colIndex]="col" [winCellSet]="winCellSet()" />
          }
        </div>

        <svg class="px-win-lines-svg">
          @for (line of activeWinLines(); track $index) {
            <polyline [attr.points]="line.points" [attr.stroke]="line.color" class="px-win-halo" fill="none" />
            <polyline [attr.points]="line.points" [attr.stroke]="line.color" class="px-win-line" fill="none" />
            @for (pt of line.pts; track $index) {
              <rect [attr.x]="pt.x - 6" [attr.y]="pt.y - 6" width="12" height="12" [attr.fill]="line.color" class="px-win-rect" />
            }
          }
        </svg>

        <div class="px-sparkles" #sparklesContainer></div>
      </div>

      <app-px-hud [coins]="coins()" [winAmount]="winAmount()" />
      </div>

      <app-px-pay-table />
      </div>

      <app-px-controls
        [credits]="coins()"
        [bet]="bet()"
        [machineState]="machineState()"
        (increaseBet)="increaseBet()"
        (decreaseBet)="decreaseBet()"
        (spin)="spin()"
      />

      <div class="px-msg"
        [class.win]="messageType() === 'win'"
        [class.lose]="messageType() === 'lose'"
        [class.jackpot]="messageType() === 'jackpot'"
        [style.visibility]="resultText() ? 'visible' : 'hidden'">
        {{ resultText() }}
      </div>

    </div>
    </div>

  </div>
  `,
})
export class SlotMachinePage implements OnDestroy, AfterViewInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private storageService = inject(StorageService);
  private sfx = inject(AudioSfxChannel);
  private audioMusic = inject(AudioMusicChannel);
  private circleTransition = inject(CircleTransitionService);
  private translateService = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private service = inject(SlotMachineService);
  private slotApi = inject(SlotMachineApiService);

  readonly coins = computed(() => this.authService.currentUser()?.coins ?? 0);
  readonly bet = signal(10);
  readonly machineState = signal<MachineState>('idle');
  readonly messageType = signal<MessageType>('none');
  readonly winAmount = signal(0);
  readonly lastWin = signal(0);
  readonly resultText = signal('');
  readonly reelStrips = signal<string[][]>([[], [], []]);
  readonly activeWinLines = signal<ActiveWinLine[]>([]);
  readonly winCellSet = signal<Set<string>>(new Set());
  readonly navigating = signal(false);

  private reelStripIndices: number[][] = [[], [], []];
  private reelTargetIndices: number[][] = [[], [], []];
  private destroySub = false;
  private pendingTimeouts: ReturnType<typeof setTimeout>[] = [];
  private animationId: number | null = null;

  @ViewChildren(PxReel) reelComponents!: QueryList<PxReel>;
  @ViewChild('reelsFrame', { read: ElementRef }) reelsFrameRef!: ElementRef<HTMLDivElement>;
  @ViewChild('sparklesContainer', { read: ElementRef }) sparklesContainerRef!: ElementRef<HTMLDivElement>;

  ngOnDestroy() {
    this.audioMusic.stop();
    this.destroySub = true;
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId);
      this.animationId = null;
    }
    this.pendingTimeouts.forEach(clearTimeout);
    this.pendingTimeouts = [];
  }

  ngAfterViewInit(): void {
    this.initReels();
    this.updateUI();
    this.audioMusic.play('../audio/SlotMachine.mp3');
    if (this.circleTransition.pendingOpen) {
      setTimeout(() => this.circleTransition.open());
    }
  }

  private initReels() {
    const strips: string[][] = [[], [], []];
    this.reelStripIndices = [[], [], []];
    const grid = this.service.generateInitialGrid();
    for (let col = 0; col < 3; col++) {
      for (let row = 0; row < 3; row++) {
        this.reelStripIndices[col].push(grid[col][row]);
        strips[col].push(this.service.SYMBOLS[grid[col][row]]);
      }
    }
    this.reelStrips.set(strips);
    this.cdr.detectChanges();
    this.reelComponents?.forEach(comp => {
      comp.reelInnerEl.nativeElement.style.top = '0px';
    });
  }

  private animateReel(col: number, delay: number): Promise<void> {
    return new Promise(resolve => {
      const timeout = setTimeout(() => {
        if (this.destroySub) { resolve(); return; }

        const comps = this.reelComponents.toArray();
        if (col >= comps.length) { resolve(); return; }
        const inner = comps[col].reelInnerEl.nativeElement;
        const stripIdx = this.reelStripIndices[col];
        const targetIdx = stripIdx.length - 4;
        const targetPx = targetIdx * this.service.CELL_SIZE;

        inner.style.transition = 'none';
        inner.style.top = '0px';

        let currentTop = 0;
        const baseSpeed = 14 + Math.random() * 8;

        const step = () => {
          if (this.destroySub) { resolve(); return; }
          currentTop -= baseSpeed;

          if (-currentTop >= targetPx) {
            const overshootPx = targetPx + 8;
            inner.style.transition = 'top .1s steps(2,end)';
            inner.style.top = `-${overshootPx}px`;

            const snapTimeout = setTimeout(() => {
              inner.style.transition = 'top .1s steps(2,end)';
              inner.style.top = `-${targetPx}px`;
              resolve();
            }, 100);
            this.pendingTimeouts.push(snapTimeout);
            return;
          }

          inner.style.top = `${currentTop}px`;
          this.animationId = requestAnimationFrame(step);
        };

        this.animationId = requestAnimationFrame(step);
      }, delay);

      this.pendingTimeouts.push(timeout);
    });
  }

  private checkLines(): { details: WinLineDetail[]; totalWin: number } {
    return this.service.checkLines(this.reelTargetIndices, this.bet());
  }

  private highlightCells(wins: WinLineDetail[]) {
    const comps = this.reelComponents.toArray();
    comps.forEach((comp, col) => {
      const el = comp.reelInnerEl.nativeElement;
      const children = el.children as HTMLCollectionOf<HTMLElement>;
      const offset = children.length > 3 ? children.length - 4 : 0;

      const activeRows = new Set<number>();
      for (const w of wins) {
        for (const [c, row] of this.service.PAYLINES[w.lineIndex].cells) {
          if (c === col) activeRows.add(row);
        }
      }
      for (const row of activeRows) {
        const cell = children[offset + row];
        if (cell) {
          cell.classList.add('px-win-cell');
        }
      }
    });
  }

  private clearWinCells() {
    this.winCellSet.set(new Set());
    this.reelComponents?.forEach(comp => {
      const children = comp.reelInnerEl.nativeElement.children as HTMLCollectionOf<HTMLElement>;
      for (let i = 0; i < children.length; i++) {
        children[i].classList.remove('px-win-cell');
      }
    });
  }

  private drawWinLines(wins: WinLineDetail[]) {
    const frameEl = this.reelsFrameRef?.nativeElement;
    if (!frameEl) return;
    const frameRect = frameEl.getBoundingClientRect();
    const comps = this.reelComponents.toArray();
    const outerRects = comps.map(comp => {
      const outer = comp.reelInnerEl.nativeElement.parentElement;
      return outer!.getBoundingClientRect();
    });
    this.activeWinLines.set(this.service.buildWinLines(wins, frameRect, outerRects));
  }

  private showMsg(txt: string, type: 'win' | 'lose' | 'jackpot') {
    this.resultText.set(txt);
    this.messageType.set(type);
  }

  private spawnPixelSparks(wins: WinLineDetail[]) {
    const container = this.sparklesContainerRef?.nativeElement;
    if (!container) return;

    const hasJackpot = wins.some(w => w.isJackpot);
    const count = hasJackpot ? 16 : wins.length * 4;
    const syms = ['★', '✦', '◆', '▲'];
    const colors = ['#ffb300', '#ff6a00'];

    for (let i = 0; i < count; i++) {
      const el = document.createElement('div');
      el.className = 'px-spark';
      el.textContent = syms[Math.floor(Math.random() * syms.length)];
      el.style.color = colors[Math.floor(Math.random() * colors.length)];
      el.style.left = (10 + Math.random() * 80) + '%';
      el.style.top = (10 + Math.random() * 80) + '%';
      el.style.animationDelay = (Math.random() * 0.3) + 's';
      container.appendChild(el);
      const rmTimeout = setTimeout(() => el.remove(), 800);
      this.pendingTimeouts.push(rmTimeout);
    }
  }

  private updateUI() {
    this.cdr.markForCheck();
  }

  private resolveResult() {
    const { details, totalWin } = this.checkLines();

    this.slotApi.spin({ bet: this.bet(), winAmount: totalWin }).subscribe({
      next: (response) => {
        const user = this.authService.currentUser();
        if (user) {
          this.authService.currentUser.set({ ...user, coins: response.remainingCoins });
        }

        if (totalWin > 0) {
          this.winAmount.set(totalWin);
          this.lastWin.set(totalWin);
          this.highlightCells(details);
          this.drawWinLines(details);

          const hasJackpot = details.some(d => d.isJackpot);

          if (hasJackpot) {
            this.showMsg('★ ' + this.translateService.instant('SLOT_MACHINE.JACKPOT') + totalWin + ' ★', 'jackpot');
            this.spawnPixelSparks(details);
            this.sfx.play('jackPot.mp3');
          } else {
            const msg = this.translateService.instant('SLOT_MACHINE.WIN_MESSAGE', { totalWin });
            this.showMsg(msg, 'win');
            this.spawnPixelSparks(details);
            this.sfx.play('jackPot.mp3');
          }
        } else {
          this.showMsg(this.translateService.instant('SLOT_MACHINE.GOOD_LUCK'), 'lose');
        }

        this.machineState.set('idle');
      },
      error: (err) => {
        if (err.status === 402) {
          this.showMsg(this.translateService.instant('SLOT_MACHINE.NO_COINS'), 'lose');
        } else {
          this.showMsg('Error', 'lose');
        }
        this.machineState.set('idle');
      }
    });
  }

  async spin() {
    if (this.machineState() === 'spinning' || this.coins() < this.bet()) return;

    this.sfx.play('tombola.mp3');
    this.lastWin.set(0);
    this.winAmount.set(0);
    this.resultText.set('');
    this.messageType.set('none');
    this.activeWinLines.set([]);
    this.winCellSet.set(new Set());
    this.clearWinCells();
    this.machineState.set('spinning');

    const comps = this.reelComponents.toArray();
    if (comps.length !== 3) {
      this.machineState.set('idle');
      return;
    }

    this.reelTargetIndices = this.service.generateTargetIndices();

    for (let col = 0; col < 3; col++) {
      this.reelStripIndices[col] = this.service.buildStrip(col, this.reelTargetIndices[col]);
    }

    this.reelStrips.set(
      this.reelStripIndices.map(col => col.map(i => this.service.SYMBOLS[i]))
    );
    this.cdr.detectChanges();

    comps.forEach(comp => {
      comp.reelInnerEl.nativeElement.style.transition = 'none';
      comp.reelInnerEl.nativeElement.style.top = '0px';
    });

    const promises = [0, 1, 2].map(col =>
      this.animateReel(col, col * 350)
    );

    await Promise.all(promises);

    if (this.destroySub) return;
    this.resolveResult();
    this.cdr.markForCheck();
  }

  increaseBet() {
    this.bet.update(b => Math.min(b + 5, 50));
  }

  decreaseBet() {
    this.bet.update(b => Math.max(b - 5, 5));
  }

  async goBack(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.sfx.play('/assets/audio/Click.mp3');
    const user = this.authService.currentUser();
    if (!user) return;
    await this.circleTransition.close();
    const saved = this.storageService.getSavedPosition(user.username);
    const location = saved?.location ?? 'preLan';
    this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
  }
}
