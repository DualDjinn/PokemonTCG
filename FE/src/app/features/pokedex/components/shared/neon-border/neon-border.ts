import {
  afterNextRender,
  Component,
  computed,
  input,
  signal,
  viewChild,
  type ElementRef,
  type OnDestroy,
} from '@angular/core';

type Movement = 'continuous' | 'step';

const EDGE_COPIES = 2;
const GLOW_LAYERS = [
  { blur: 4, opacity: 0.8, reach: 0.2 },
  { blur: 10, opacity: 0.5, reach: 0.5 },
  { blur: 22, opacity: 0.25, reach: 0.8 },
];
const MAX_GLOW_BLUR = Math.max(...GLOW_LAYERS.map((l) => l.blur));
const MAX_GLOW_REACH = 16;
const SLOWEST_CYCLE = 30;
const FASTEST_CYCLE = 4;
const SLOWEST_STEP = 3;
const FASTEST_STEP = 0.35;
const STEP_EASE = [0.72, 0.16, 0.18, 1.05];
const GLIDE_EASE = [0.65, 0, 0.35, 1];
const ARC_SAMPLES = 24;
const MIN_ARC = 0.015;

function withAlpha(input: string, alpha: number): string {
  const a = Math.max(0, Math.min(1, alpha));
  if (typeof input !== 'string') return `rgba(0,0,0,${a})`;
  const s = input.trim();
  const hex = s.match(/^#([0-9a-f]{3,8})$/i);
  if (hex) {
    let h = hex[1];
    if (h.length === 3 || h.length === 4) {
      h = h
        .split('')
        .map((c) => c + c)
        .join('');
    }
    const n = parseInt(h.slice(0, 6), 16);
    if (!Number.isFinite(n)) return `rgba(0,0,0,${a})`;
    return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`;
  }
  const rgb = s.match(/^rgba?\(([^)]+)\)/i);
  if (rgb) {
    const parts = rgb[1].split(',').map((v) => parseFloat(v));
    if (parts.length >= 3 && parts.slice(0, 3).every(Number.isFinite)) {
      return `rgba(${parts[0]},${parts[1]},${parts[2]},${a})`;
    }
  }
  return `rgba(0,0,0,${a})`;
}

function perimeterPoint(u: number, w: number, h: number): [number, number] {
  const d = (((u % 1) + 1) % 1) * 2 * (w + h);
  if (d < w) return [d, 0];
  if (d < w + h) return [w, d - w];
  if (d < w * 2 + h) return [w - (d - w - h), h];
  return [0, h - (d - w * 2 - h)];
}

function cornerLap(k: number, w: number, h: number): number {
  const p = 2 * (w + h);
  const at = [0, w / p, (w + h) / p, (w * 2 + h) / p];
  return Math.floor(k / 4) + at[((k % 4) + 4) % 4];
}

function perimeterAngle(u: number, w: number, h: number): number {
  const [x, y] = perimeterPoint(u, w, h);
  return (Math.atan2(x - w / 2, h / 2 - y) * 180) / Math.PI;
}

function buildArc(
  lap: number,
  lengthPct: number,
  w: number,
  h: number,
  color: string,
): string {
  const fw = w > 0 ? w : 100;
  const fh = h > 0 ? h : 100;
  const len = Math.max(0, Math.min(100, lengthPct));
  const span = Math.max(MIN_ARC, (len / 100) * 0.5);
  const solidT = len / 100;
  const stops: string[] = [];
  let base = 0;
  let prev = 0;
  let acc = 0;
  for (let i = 0; i <= ARC_SAMPLES; i++) {
    const f = i / ARC_SAMPLES;
    const angle = perimeterAngle(lap + (f - 0.5) * span, fw, fh);
    if (i === 0) {
      base = angle;
    } else {
      let d = angle - prev;
      while (d > 180) d -= 360;
      while (d < -180) d += 360;
      acc += d;
    }
    prev = angle;
    const t = Math.abs(f - 0.5) * 2;
    const k = solidT >= 1 ? 1 : t <= solidT ? 1 : 1 - (t - solidT) / (1 - solidT);
    stops.push(
      `${withAlpha(color, k * k * (3 - 2 * k))} ${acc.toFixed(2)}deg`,
    );
  }
  const end = acc.toFixed(2);
  stops.push(`${withAlpha(color, 0)} ${end}deg`);
  stops.push(`${withAlpha(color, 0)} 360deg`);
  return `conic-gradient(from ${base.toFixed(2)}deg at 50% 50%, ${stops.join(', ')})`;
}

function makeEaseFn(pts: number[]): (t: number) => number {
  const [x1, y1, x2, y2] = pts;
  if (x1 === y1 && x2 === y2) return (t) => t;
  const bez = (a: number, b: number, t: number) => {
    const u = 1 - t;
    return 3 * u * u * t * a + 3 * u * t * t * b + t * t * t;
  };
  return (t: number) => {
    const x = Math.max(0, Math.min(1, t));
    let s = x;
    for (let i = 0; i < 8; i++) {
      const cx = bez(x1, x2, s) - x;
      const u = 1 - s;
      const dx = 3 * u * u * x1 + 6 * u * s * (x2 - x1) + 3 * s * s * (1 - x2);
      if (Math.abs(dx) < 1e-6) break;
      s -= cx / dx;
      s = Math.max(0, Math.min(1, s));
    }
    return bez(y1, y2, s);
  };
}

const stepEase = makeEaseFn(STEP_EASE);
const glideEase = makeEaseFn(GLIDE_EASE);

@Component({
  standalone: true,
  selector: 'app-neon-border',
  imports: [],
  templateUrl: './neon-border.html',
  styleUrl: './neon-border.scss',
})
export class NeonBorder implements OnDestroy {
  readonly color = input('#7c4dff');
  readonly borderSize = input(65);
  readonly glow = input(30);
  readonly thickness = input(3);
  readonly speed = input(12);
  readonly movement = input<Movement>('continuous');

  private readonly root = viewChild<ElementRef<HTMLDivElement>>('root');
  private readonly groupA = viewChild<ElementRef<HTMLDivElement>>('groupA');
  private readonly groupB = viewChild<ElementRef<HTMLDivElement>>('groupB');

  private readonly size = signal({ w: 0, h: 0 });
  protected readonly radius = signal(16);

  protected readonly glowLayers = GLOW_LAYERS;
  protected readonly edgeCopies = Array.from({ length: EDGE_COPIES }, (_, i) => i);
  protected readonly amount = computed(() => Math.max(0, Math.min(100, this.glow())) / 100);
  protected readonly thick = computed(() => Math.max(1, Math.min(10, this.thickness())));
  protected readonly glowOuter = computed(() => 10 + MAX_GLOW_REACH + MAX_GLOW_BLUR * 2);
  protected readonly ringAt = (share: number) =>
    this.thick() + this.amount() * MAX_GLOW_REACH * share;

  private ro: ResizeObserver | null = null;
  private rafId = 0;

  constructor() {
    afterNextRender(() => {
      this.observe();
      this.animate();
    });
  }

  ngOnDestroy(): void {
    this.ro?.disconnect();
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
    }
  }

  private observe(): void {
    const el = this.root()?.nativeElement;
    if (!el || typeof ResizeObserver === 'undefined') return;
    this.ro = new ResizeObserver(() => {
      const r = el.getBoundingClientRect();
      this.size.set({ w: r.width, h: r.height });
      const radiusPx = parseFloat(getComputedStyle(el).borderRadius);
      this.radius.set(Number.isFinite(radiusPx) ? radiusPx : 16);
    });
    this.ro.observe(el);
  }

  private setArc(el: HTMLElement | undefined, lap: number): void {
    if (!el) return;
    const { w, h } = this.size();
    el.style.setProperty(
      '--arc',
      buildArc(lap, this.borderSize(), w, h, this.color()),
    );
  }

  private animate(): void {
    if (typeof requestAnimationFrame === 'undefined') return;
    this.setArc(this.groupA()?.nativeElement, 0);
    this.setArc(this.groupB()?.nativeElement, 0.5);

    let last = performance.now();
    let lap = 0;
    let corner = 0;
    let stepT = 0;
    const frame = (now: number) => {
      const dt = Math.min(0.05, Math.max(0, (now - last) / 1000));
      last = now;
      const s = Math.max(0, Math.min(20, this.speed()));
      if (s > 0) {
        const step = this.movement() === 'step';
        const beat = step
          ? SLOWEST_STEP + ((FASTEST_STEP - SLOWEST_STEP) * (s - 1)) / 19
          : (SLOWEST_CYCLE + ((FASTEST_CYCLE - SLOWEST_CYCLE) * (s - 1)) / 19) / 4;
        stepT += dt / beat;
        while (stepT >= 1) {
          stepT -= 1;
          corner += 1;
        }
        const eased = step ? stepEase(Math.min(1, stepT * 2)) : glideEase(stepT);
        const { w, h } = this.size();
        const fw = w > 0 ? w : 100;
        const fh = h > 0 ? h : 100;
        const from = cornerLap(corner, fw, fh);
        const to = cornerLap(corner + 1, fw, fh);
        lap = from + (to - from) * eased;
        this.setArc(this.groupA()?.nativeElement, lap);
        this.setArc(this.groupB()?.nativeElement, lap + 0.5);
      }
      this.rafId = requestAnimationFrame(frame);
    };
    this.rafId = requestAnimationFrame(frame);
  }
}
