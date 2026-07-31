import { Injectable } from '@angular/core';

const OVERLAY_ID = 'circle-transition-overlay';
const DURATION = 900;

@Injectable({ providedIn: 'root' })
export class CircleTransitionService {
  private canvas: HTMLCanvasElement | null = null;
  private ctx: CanvasRenderingContext2D | null = null;
  pendingOpen = false;

  async close(): Promise<void> {
    this.pendingOpen = true;

    const cvs = this.ensureCanvas();
    if (!cvs) return;

    const ctx = this.ctx!;
    const rmax = this.getRmax(cvs);

    ctx.clearRect(0, 0, cvs.width, cvs.height);
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, cvs.width, cvs.height);
    ctx.save();
    ctx.globalCompositeOperation = 'destination-out';
    ctx.beginPath();
    ctx.arc(cvs.width / 2, cvs.height / 2, rmax, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    await this.animate(rmax, 0);
  }

  async open(): Promise<void> {
    this.pendingOpen = false;

    let cvs = document.getElementById(OVERLAY_ID) as HTMLCanvasElement | null;
    if (!cvs) return;

    this.canvas = cvs;
    this.ctx = cvs.getContext('2d')!;

    const ctx = this.ctx;
    ctx.clearRect(0, 0, cvs.width, cvs.height);
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, cvs.width, cvs.height);

    await this.animate(0, this.getRmax(cvs));

    this.cleanup();
  }

  cleanup(): void {
    const el = document.getElementById(OVERLAY_ID);
    if (el) el.remove();
    this.canvas = null;
    this.ctx = null;
  }

  private ensureCanvas(): HTMLCanvasElement | null {
    if (this.canvas && this.ctx) return this.canvas;

    let cvs = document.getElementById(OVERLAY_ID) as HTMLCanvasElement | null;
    if (!cvs) {
      cvs = document.createElement('canvas');
      cvs.id = OVERLAY_ID;
      cvs.style.position = 'fixed';
      cvs.style.inset = '0';
      cvs.style.width = '100%';
      cvs.style.height = '100%';
      cvs.style.zIndex = '9999';
      cvs.style.pointerEvents = 'none';
      cvs.width = window.innerWidth;
      cvs.height = window.innerHeight;
      document.body.appendChild(cvs);
    }

    this.canvas = cvs;
    this.ctx = cvs.getContext('2d')!;
    return cvs;
  }

  private getRmax(cvs: HTMLCanvasElement): number {
    const cx = cvs.width / 2;
    const cy = cvs.height / 2;
    return Math.sqrt(cx * cx + cy * cy) * 1.1;
  }

  private animate(fromRadius: number, toRadius: number): Promise<void> {
    return new Promise(resolve => {
      const ctx = this.ctx!;
      const cvs = this.canvas!;
      const cx = cvs.width / 2;
      const cy = cvs.height / 2;
      const start = performance.now();

      const tick = (now: number) => {
        const elapsed = now - start;
        const t = Math.min(elapsed / DURATION, 1);
        const eased = -(Math.cos(Math.PI * t) - 1) / 2;
        const radius = fromRadius + (toRadius - fromRadius) * eased;

        ctx.clearRect(0, 0, cvs.width, cvs.height);
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, cvs.width, cvs.height);
        ctx.save();
        ctx.globalCompositeOperation = 'destination-out';
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        if (t < 1) {
          requestAnimationFrame(tick);
        } else {
          resolve();
        }
      };

      requestAnimationFrame(tick);
    });
  }
}
