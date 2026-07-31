import { Injectable } from '@angular/core';
import type { WinLineDetail, WinPoint, ActiveWinLine } from '../../domain/slot-machine.models';
import { Xoshiro256pp } from '../../domain/xoshiro256pp';

@Injectable({ providedIn: 'root' })
export class SlotMachineService {
  readonly CELL_SIZE = 90;

  readonly SYMBOLS = [
    'assets/img/slot-machine/slowpoke.gif',
    'assets/img/slot-machine/noctowl.gif',
    'assets/img/slot-machine/snorlax.gif',
    'assets/img/slot-machine/pikachu.gif',
    'assets/img/slot-machine/gengar.gif',
    'assets/img/slot-machine/charizard.gif',
    'assets/img/slot-machine/greninja-ash.gif',
    'assets/img/slot-machine/mewtwo.gif',
  ];

  readonly SYMBOL_NAMES = [
    'SLOWPOKE', 'NOCTOWL', 'SNORLAX', 'PIKACHU',
    'GENGAR', 'CHARIZARD', 'GRENINJA', 'MEWTWO',
  ];

  readonly PAYOUTS = [5, 7, 10, 15, 20, 30, 50, 100];

  readonly WEIGHTS: number[][] = [
    [7, 7, 6, 5, 4, 3, 2, 1],
    [7, 7, 6, 5, 4, 3, 2, 1],
    [7, 6, 5, 5, 4, 3, 2, 1],
  ];

  readonly PAYLINES = [
    { label: 'FILA 1', cells: [[0, 0], [1, 0], [2, 0]] as [number, number][] },
    { label: 'FILA 2', cells: [[0, 1], [1, 1], [2, 1]] as [number, number][] },
    { label: 'FILA 3', cells: [[0, 2], [1, 2], [2, 2]] as [number, number][] },
    { label: 'DIAG ↘', cells: [[0, 0], [1, 1], [2, 2]] as [number, number][] },
    { label: 'DIAG ↗', cells: [[0, 2], [1, 1], [2, 0]] as [number, number][] },
  ];

  readonly LINE_COLORS = ['#00ff88', '#ffb300', '#ff6a00', '#ff00ff', '#00ccff'];

  private rng = new Xoshiro256pp();

  private ws(reelIndex: number): number {
    const weights = this.WEIGHTS[reelIndex];
    const total = weights.reduce((a, b) => a + b, 0);
    let r = this.rng.nextDouble() * total;
    for (let i = 0; i < weights.length; i++) {
      r -= weights[i];
      if (r <= 0) return i;
    }
    return weights.length - 1;
  }

  buildStrip(col: number, targetRowIndices: number[]): number[] {
    const strip: number[] = [];
    for (let i = 0; i < 22; i++) {
      strip.push(this.ws(col));
    }
    for (const idx of targetRowIndices) {
      strip.push(idx);
    }
    strip.push(this.ws(col));
    return strip;
  }

  generateTargetIndices(): number[][] {
    const grid: number[][] = [[], [], []];
    for (let col = 0; col < 3; col++) {
      for (let row = 0; row < 3; row++) {
        grid[col].push(this.ws(col));
      }
    }
    return grid;
  }

  generateInitialGrid(): number[][] {
    const grid: number[][] = [[], [], []];
    for (let col = 0; col < 3; col++) {
      for (let row = 0; row < 3; row++) {
        grid[col].push(this.ws(col));
      }
    }
    return grid;
  }

  checkLines(grid: number[][], bet: number): { details: WinLineDetail[]; totalWin: number } {
    const details: WinLineDetail[] = [];

    for (let li = 0; li < this.PAYLINES.length; li++) {
      const line = this.PAYLINES[li];
      const syms = line.cells.map(([c, r]) => grid[c][r]);
      if (syms[0] === syms[1] && syms[1] === syms[2]) {
        details.push({
          lineIndex: li,
          label: line.label,
          multiplier: this.PAYOUTS[syms[0]],
          symbolIdx: syms[0],
          isJackpot: syms[0] === 7,
        });
      }
    }

    const totalWin = details.reduce((sum, d) => sum + bet * d.multiplier, 0);
    return { details, totalWin };
  }

  buildWinLines(
    wins: WinLineDetail[],
    frameRect: DOMRect,
    outerRects: DOMRect[],
  ): ActiveWinLine[] {
    const lines: ActiveWinLine[] = [];

    for (let wi = 0; wi < wins.length; wi++) {
      const w = wins[wi];
      const line = this.PAYLINES[w.lineIndex];
      const color = w.isJackpot ? '#ffb300' : this.LINE_COLORS[wi % this.LINE_COLORS.length];
      const pts: WinPoint[] = [];

      for (const [col, row] of line.cells) {
        const outerRect = outerRects[col];
        if (!outerRect) continue;
        const cx = (outerRect.left + outerRect.width / 2) - frameRect.left;
        const cy = (outerRect.top - frameRect.top) + row * this.CELL_SIZE + this.CELL_SIZE / 2;
        pts.push({ x: Math.round(cx), y: Math.round(cy) });
      }

      lines.push({
        points: pts.map(p => `${p.x},${p.y}`).join(' '),
        pts,
        color,
      });
    }

    return lines;
  }
}
