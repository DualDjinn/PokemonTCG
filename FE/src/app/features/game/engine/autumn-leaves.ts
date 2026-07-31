/**
 * Efecto ambiental de otoño para mapas exteriores: hojas pixel-art que
 * vuelan empujadas por el viento, con ráfagas visibles que cruzan la
 * pantalla.
 *
 * No usa assets externos: cada hoja se "hornea" una sola vez en canvases
 * offscreen a partir de una matriz de píxeles y se pre-rota en pasos fijos,
 * para que la rotación mantenga la estética pixelada del juego en lugar de
 * verse como un sprite girado suavemente.
 */

// 0 = transparente, 1 = color base, 2 = claro, 3 = oscuro (nervadura/sombra)
const LEAF_SHAPES: number[][][] = [
  // Hoja redondeada
  [
    [0, 1, 1, 0, 0],
    [1, 2, 1, 1, 0],
    [1, 1, 3, 1, 1],
    [0, 1, 1, 2, 1],
    [0, 0, 1, 1, 0],
  ],
  // Hoja alargada
  [
    [0, 1, 1, 0, 0, 0],
    [1, 2, 3, 1, 1, 0],
    [0, 1, 1, 3, 2, 1],
    [0, 0, 0, 1, 1, 0],
  ],
  // Hoja chica
  [
    [0, 1, 1, 0],
    [1, 2, 3, 1],
    [1, 3, 2, 1],
    [0, 1, 1, 0],
  ],
];

const LEAF_PALETTES: string[][] = [
  ['#e07b39', '#f2a65a', '#a34f1f'], // naranja
  ['#c0392b', '#e06b50', '#8e2a1d'], // rojo
  ['#e3a92b', '#f4c95d', '#b07d18'], // amarillo
  ['#9c6b30', '#bd8a4e', '#6e4a1f'], // marrón
];

/** Tamaño en pantalla de cada "píxel" del arte (capa cercana). */
const PIXEL_SCALE = 3;
/** Rotaciones pre-horneadas por sprite: más pasos = giro más suave. */
const ROTATION_STEPS = 16;
/** Margen fuera de pantalla para spawn/respawn de partículas. */
const MARGIN = 48;
const MAX_STREAKS = 4;
const STREAK_TRAIL_LENGTH = 16;

interface LeafVariant {
  frames: HTMLCanvasElement[];
  pixelScale: number;
}

interface Leaf {
  x: number;
  y: number;
  variant: number;
  rot: number;
  rotSpeed: number;
  /** Cuánto lo empuja el viento global (las hojas "lejanas" responden menos). */
  windFactor: number;
  fall: number;
  flutterAmp: number;
  flutterFreq: number;
  phase: number;
  drift: number;
  alpha: number;
}

interface Streak {
  x: number;
  baseY: number;
  speed: number;
  wobbleAmp: number;
  wobbleFreq: number;
  phase: number;
  trail: Array<{ x: number; y: number }>;
}

export class AutumnLeavesEffect {
  private variants: LeafVariant[] = [];
  private farVariants: number[] = [];
  private nearVariants: number[] = [];
  private leaves: Leaf[] = [];
  private streaks: Streak[] = [];
  private lastTime: number | null = null;
  private elapsed = 0;

  constructor(width: number, height: number) {
    this.bakeSprites();
    const count = Math.min(30, Math.max(14, Math.round((width * height) / 90000)));
    for (let i = 0; i < count; i++) {
      this.leaves.push(this.spawnLeaf(width, height, true));
    }
  }

  /** Desplaza todas las partículas junto con el mundo (lo llama moveAll). */
  shift(dx: number, dy: number) {
    for (const leaf of this.leaves) {
      leaf.x += dx;
      leaf.y += dy;
    }
    for (const streak of this.streaks) {
      streak.x += dx;
      streak.baseY += dy;
      for (const p of streak.trail) {
        p.x += dx;
        p.y += dy;
      }
    }
  }

  /** Actualiza y dibuja el efecto. Calcula su propio dt para ser independiente del framerate. */
  render(ctx: CanvasRenderingContext2D, width: number, height: number) {
    const now = performance.now();
    // Clamp del dt: evita saltos enormes al volver de una pausa de menú/pestaña.
    const dt = this.lastTime === null ? 0.016 : Math.min((now - this.lastTime) / 1000, 0.05);
    this.lastTime = now;
    this.elapsed += dt;

    const gust = this.gustStrength(this.elapsed);
    const wind = 21 + gust * 105;

    this.updateStreaks(dt, gust, width, height);
    this.updateLeaves(dt, wind, gust, width, height);

    ctx.save();
    this.drawStreaks(ctx);
    this.drawLeaves(ctx);
    ctx.restore();
  }

  /**
   * Intensidad de ráfaga 0..1: dos senos lentos desfasados, recortados y
   * elevados al cuadrado para que haya calmas largas y ráfagas marcadas.
   */
  private gustStrength(t: number): number {
    const slow = Math.sin(t * 0.28 + 1.1) * 0.6 + Math.sin(t * 0.71 + 0.3) * 0.4;
    return Math.max(0, slow) ** 2;
  }

  private updateLeaves(dt: number, wind: number, gust: number, width: number, height: number) {
    for (const leaf of this.leaves) {
      leaf.x += (wind * leaf.windFactor + leaf.drift) * dt;
      const flutter = Math.sin(this.elapsed * leaf.flutterFreq + leaf.phase) * leaf.flutterAmp;
      const lift = gust * 24;
      leaf.y += (leaf.fall + flutter - lift) * dt;
      leaf.rot += leaf.rotSpeed * (0.5 + gust) * dt;

      if (
        leaf.x > width + MARGIN ||
        leaf.y > height + MARGIN ||
        leaf.x < -MARGIN * 2 ||
        leaf.y < -MARGIN * 2
      ) {
        Object.assign(leaf, this.spawnLeaf(width, height, false));
      }
    }
  }

  private drawLeaves(ctx: CanvasRenderingContext2D) {
    for (const leaf of this.leaves) {
      const { frames } = this.variants[leaf.variant];
      const steps = frames.length;
      let idx = Math.round((leaf.rot / (Math.PI * 2)) * steps) % steps;
      if (idx < 0) idx += steps;
      const frame = frames[idx];
      ctx.globalAlpha = leaf.alpha;
      ctx.drawImage(frame, Math.round(leaf.x - frame.width / 2), Math.round(leaf.y - frame.height / 2));
    }
    ctx.globalAlpha = 1;
  }

  private updateStreaks(dt: number, gust: number, width: number, height: number) {
    // Las ráfagas visibles aparecen seguido, y todavía más cuando sopla fuerte.
    if (this.streaks.length < MAX_STREAKS && Math.random() < (0.35 + gust * 2.2) * dt) {
      this.streaks.push({
        x: -MARGIN,
        baseY: height * (0.15 + Math.random() * 0.7),
        speed: 240 + Math.random() * 160 + gust * 120,
        wobbleAmp: 10 + Math.random() * 14,
        wobbleFreq: 0.015 + Math.random() * 0.015,
        phase: Math.random() * Math.PI * 2,
        trail: [],
      });
    }

    for (const streak of this.streaks) {
      streak.x += streak.speed * dt;
      // Ondulación espacial (en función de x): dibuja la línea con forma de onda.
      const y = streak.baseY + Math.sin(streak.x * streak.wobbleFreq + streak.phase) * streak.wobbleAmp;
      streak.trail.push({ x: streak.x, y });
      if (streak.trail.length > STREAK_TRAIL_LENGTH) streak.trail.shift();
    }

    this.streaks = this.streaks.filter((s) => s.trail.length === 0 || s.trail[0].x < width + PIXEL_SCALE);
  }

  private drawStreaks(ctx: CanvasRenderingContext2D) {
    const ps = PIXEL_SCALE;
    ctx.fillStyle = '#fff7e8';
    for (const streak of this.streaks) {
      for (let i = 0; i < streak.trail.length; i++) {
        const p = streak.trail[i];
        // La cabeza es la más visible y la cola se desvanece.
        ctx.globalAlpha = ((i + 1) / streak.trail.length) * 0.35;
        ctx.fillRect(Math.round(p.x / ps) * ps, Math.round(p.y / ps) * ps, ps, ps);
      }
    }
    ctx.globalAlpha = 1;
  }

  private spawnLeaf(width: number, height: number, initial: boolean): Leaf {
    const depth = Math.random(); // 0 = lejos (chica, lenta), 1 = cerca (grande, rápida)
    const pool = depth < 0.45 ? this.farVariants : this.nearVariants;
    const fromTop = !initial && Math.random() < 0.35;

    let x: number;
    let y: number;
    if (initial) {
      // Arranque: repartidas por toda la pantalla para no empezar en vacío.
      x = Math.random() * width;
      y = Math.random() * height;
    } else if (fromTop) {
      x = Math.random() * width;
      y = -MARGIN / 2;
    } else {
      // El viento sopla hacia la derecha: la mayoría entra por la izquierda.
      x = -MARGIN + Math.random() * (MARGIN - 10);
      y = Math.random() * height * 0.9;
    }

    return {
      x,
      y,
      variant: pool[Math.floor(Math.random() * pool.length)],
      rot: Math.random() * Math.PI * 2,
      rotSpeed: (1.0 + Math.random() * 2.9) * (Math.random() < 0.5 ? -1 : 1),
      windFactor: 0.55 + depth * 0.65,
      fall: 13 + Math.random() * 18,
      flutterAmp: 16 + Math.random() * 21,
      flutterFreq: 1.2 + Math.random() * 1.8,
      phase: Math.random() * Math.PI * 2,
      drift: 5 + Math.random() * 18,
      alpha: 0.7 + depth * 0.3,
    };
  }

  private bakeSprites() {
    for (const pixelScale of [PIXEL_SCALE - 1, PIXEL_SCALE]) {
      for (const palette of LEAF_PALETTES) {
        for (const shape of LEAF_SHAPES) {
          const h = shape.length;
          const w = shape[0].length;

          const base = document.createElement('canvas');
          base.width = w;
          base.height = h;
          const baseCtx = base.getContext('2d')!;
          shape.forEach((row, j) =>
            row.forEach((cell, i) => {
              if (!cell) return;
              baseCtx.fillStyle = palette[cell - 1];
              baseCtx.fillRect(i, j, 1, 1);
            })
          );

          const size = Math.ceil(Math.hypot(w, h)) + 2;
          const frames: HTMLCanvasElement[] = [];
          for (let step = 0; step < ROTATION_STEPS; step++) {
            // Rotar a resolución nativa (re-rasteriza al grid de 1px) y recién
            // después escalar sin suavizado: los "píxeles" quedan siempre enteros.
            const small = document.createElement('canvas');
            small.width = size;
            small.height = size;
            const smallCtx = small.getContext('2d')!;
            smallCtx.imageSmoothingEnabled = false;
            smallCtx.translate(size / 2, size / 2);
            smallCtx.rotate((step / ROTATION_STEPS) * Math.PI * 2);
            smallCtx.drawImage(base, -w / 2, -h / 2);

            const final = document.createElement('canvas');
            final.width = size * pixelScale;
            final.height = size * pixelScale;
            const finalCtx = final.getContext('2d')!;
            finalCtx.imageSmoothingEnabled = false;
            finalCtx.drawImage(small, 0, 0, final.width, final.height);
            frames.push(final);
          }

          const idx = this.variants.length;
          this.variants.push({ frames, pixelScale });
          (pixelScale < PIXEL_SCALE ? this.farVariants : this.nearVariants).push(idx);
        }
      }
    }
  }
}
