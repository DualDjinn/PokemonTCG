import { Component, input, ChangeDetectionStrategy, ViewChild, ElementRef } from '@angular/core';

@Component({
  selector: 'app-px-reel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="px-reel-wrap">
      <div class="px-reel-top"></div>
      <div class="px-reel-outer">
        <div class="px-reel-inner" #reelInner>
          @for (cell of symbols(); track $index) {
            <div class="px-cell" [class.px-win-cell]="isWinCell($index)">
              <img [src]="cell" alt="" class="px-cell-img" />
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .px-reel-wrap {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .px-reel-top {
      width: 100%;
      max-width: 100%;
      height: 6px;
      background: var(--px-border2);
      clip-path: polygon(0% 0%, 100% 0%, 96% 100%, 4% 100%);
    }
    .px-reel-outer {
      position: relative;
      width: 100%;
      height: 270px;
      overflow: hidden;
      border: 3px solid var(--px-shadow);
      outline: 1px solid var(--px-border);
    }
    .px-reel-outer::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 47px;
      background: linear-gradient(to bottom, var(--px-dark), transparent);
      z-index: 5;
      pointer-events: none;
    }
    .px-reel-outer::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 47px;
      background: linear-gradient(to top, var(--px-dark), transparent);
      z-index: 5;
      pointer-events: none;
    }
    .px-reel-inner {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      will-change: top;
    }
    .px-cell {
      height: 90px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36px;
      border-bottom: 2px solid #1a0800;
      background: var(--px-reel);
      position: relative;
      user-select: none;
    }
    .px-cell::after {
      content: '';
      position: absolute;
      inset: 0;
      background: repeating-linear-gradient(90deg, transparent 6px, rgba(255,106,0,0.04) 1px);
      pointer-events: none;
    }
    .px-cell.px-win-cell {
      background: #001a0a;
      outline: 2px solid var(--px-win);
      outline-offset: -2px;
    }
    .px-cell-img {
      width: 68px;
      height: 68px;
      object-fit: contain;
      image-rendering: pixelated;
      display: block;
    }
  `],
  host: {
    'style': 'display: contents',
  },
})
export class PxReel {
  readonly symbols = input<string[]>([]);
  readonly colIndex = input(0);
  readonly winCellSet = input<Set<string>>(new Set());

  @ViewChild('reelInner', { read: ElementRef }) reelInnerEl!: ElementRef<HTMLDivElement>;

  isWinCell(idx: number): boolean {
    return this.winCellSet().has(this.colIndex() + '-' + idx);
  }
}
