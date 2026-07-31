/**
 * Decoraciones animadas (fuente + flores) que se dibujan sobre el mapa
 * estático sin recargar la imagen del mapa. Los sprites se hornean en
 * canvases offscreen una sola vez y se ciclan por frame; el render solo
 * compone bitmaps ya generados, así que es barato.
 *
 * Las posiciones son world-anchored (no screen-anchored): viajan con el
 * mundo via `shift()` igual que `AutumnLeavesEffect`, para que sigan
 * pegadas a su tile cuando el jugador se mueve.
 */

import { AnimatedDecorationSpawn } from './locations.config';

const TILE_PX = 28;

const FOUNTAIN_SRC = 'assets/maps/Decorations/fountain.png';
const FLOWER_SRC = 'assets/maps/Decorations/flower.png';

/** Desplazamiento máximo en píxeles de la imagen fuente (antes de escalar). */
const BOB_AMPLITUDE_PX = 2;

/**
 * Rect (en píxeles de la imagen fuente 168x196) que cubre la columna de
 * agua de la fuente. Detectado a partir de los píxeles cyan más brillantes
 * con un pequeño margen para incluir el sombreado del contorno.
 */
const FOUNTAIN_WATER_RECT = { x: 60, y: 30, w: 50, h: 85 };
/**
 * Sólo cabeceo vertical sutil: el efecto "vivo" de la fuente lo aportan las
 * partículas, no la columna. Frames múltiples y FPS lentos = respiración
 * apenas perceptible, no un balanceo mecánico.
 */
const FOUNTAIN_FRAME_COUNT = 8;
const FOUNTAIN_FPS = 3;
const FOUNTAIN_BOB_PX = 1;

/**
 * Spray de gotas que sale de arriba de la columna de agua. Coordenadas y
 * velocidades en píxeles de la imagen fuente (168x196), antes de escalar.
 */
const PARTICLE_WATER_TOP_X = 85; // centro horizontal del chorro
const PARTICLE_WATER_TOP_Y = 28; // borde superior del chorro
const PARTICLE_SPAWN_X_SPREAD = 8;
const PARTICLE_SPAWN_RATE = 14; // gotas por segundo y por fuente
const PARTICLE_VY0 = -90;       // velocidad inicial hacia arriba
const PARTICLE_VY_VAR = 30;     // variación de impulso vertical
const PARTICLE_VX_VAR = 22;     // ancho del abanico horizontal
const PARTICLE_GRAVITY = 260;   // px/seg² hacia abajo
const PARTICLE_BASIN_Y = 138;   // y a la que se considera que la gota cae al agua
const PARTICLE_MAX_LIFE = 2.5;
const PARTICLE_COLOR = '#dceeff';

/**
 * Coreografía circular para los pétalos: arriba → derecha → abajo →
 * izquierda → arriba. Cada par (dx, dy) es un offset entero en píxeles de
 * la imagen fuente, así la flor "baila" en un cuadrado/diamante alrededor
 * de su posición de reposo sin perder la estética pixelada.
 */
const FLOWER_DANCE: Array<[number, number]> = [
  [0, -BOB_AMPLITUDE_PX], // arriba
  [1, -1],                // arriba-derecha
  [BOB_AMPLITUDE_PX, 0],  // derecha
  [1, 1],                 // abajo-derecha
  [0, BOB_AMPLITUDE_PX],  // abajo
  [-1, 1],                // abajo-izquierda
  [-BOB_AMPLITUDE_PX, 0], // izquierda
  [-1, -1],               // arriba-izquierda
];
const FLOWER_FPS = 3.5;

interface FountainParticle {
  /** Offsets en píxeles de la imagen fuente, relativos a (worldX, worldY). */
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  size: number;
}

interface DecorationInstance {
  kind: 'fountain' | 'flower';
  worldX: number;
  worldY: number;
  phase: number;
  particles: FountainParticle[];
  spawnAcc: number;
}

export class AnimatedDecorationsEffect {
  private instances: DecorationInstance[] = [];
  private fountainFrames: HTMLCanvasElement[] = [];
  private flowerFrames: HTMLCanvasElement[] = [];
  private scale: number;
  private elapsed = 0;
  private lastTime: number | null = null;
  private fountainImg: HTMLImageElement;
  private flowerImg: HTMLImageElement;
  private framesReady = false;

  constructor(
    spawns: AnimatedDecorationSpawn[],
    startOffset: { x: number; y: number },
    scale: number,
  ) {
    this.scale = scale;
    const tilePx = TILE_PX * scale;
    for (const s of spawns) {
      this.instances.push({
        kind: s.kind,
        worldX: s.col * tilePx + startOffset.x,
        worldY: s.row * tilePx + startOffset.y,
        phase: Math.random() * Math.PI * 2,
        particles: [],
        // Acumulador desfasado para que distintas fuentes no spawneen
        // la primera gota en el mismo instante.
        spawnAcc: Math.random(),
      });
    }

    this.fountainImg = new Image();
    this.flowerImg = new Image();
    this.fountainImg.src = FOUNTAIN_SRC;
    this.flowerImg.src = FLOWER_SRC;

    let loaded = 0;
    const onload = () => {
      loaded++;
      if (loaded === 2) {
        this.bakeFrames();
        this.framesReady = true;
      }
    };
    if (this.fountainImg.complete && this.fountainImg.naturalWidth > 0) onload();
    else this.fountainImg.addEventListener('load', onload);
    if (this.flowerImg.complete && this.flowerImg.naturalWidth > 0) onload();
    else this.flowerImg.addEventListener('load', onload);
  }

  /** Desplaza todas las decoraciones junto al mundo (lo llama `moveAll`). */
  shift(dx: number, dy: number) {
    for (const d of this.instances) {
      d.worldX += dx;
      d.worldY += dy;
    }
  }

  render(ctx: CanvasRenderingContext2D) {
    if (!this.framesReady || !this.instances.length) return;

    const now = performance.now();
    const dt = this.lastTime === null ? 0.016 : Math.min((now - this.lastTime) / 1000, 0.05);
    this.lastTime = now;
    this.elapsed += dt;

    ctx.save();
    ctx.imageSmoothingEnabled = false;
    for (const d of this.instances) {
      const frames = d.kind === 'fountain' ? this.fountainFrames : this.flowerFrames;
      if (!frames.length) continue;
      const fps = d.kind === 'fountain' ? FOUNTAIN_FPS : FLOWER_FPS;
      const rawIdx = Math.floor(this.elapsed * fps + d.phase * frames.length / (Math.PI * 2));
      const idx = ((rawIdx % frames.length) + frames.length) % frames.length;
      const frame = frames[idx];
      const dw = frame.width * this.scale;
      const dh = frame.height * this.scale;
      ctx.drawImage(frame, Math.round(d.worldX), Math.round(d.worldY), dw, dh);

      if (d.kind === 'fountain') {
        this.updateFountainParticles(d, dt);
        this.drawFountainParticles(ctx, d);
      }
    }
    ctx.restore();
  }

  private updateFountainParticles(inst: DecorationInstance, dt: number) {
    inst.spawnAcc += PARTICLE_SPAWN_RATE * dt;
    while (inst.spawnAcc >= 1) {
      inst.spawnAcc -= 1;
      inst.particles.push({
        x: PARTICLE_WATER_TOP_X + (Math.random() - 0.5) * PARTICLE_SPAWN_X_SPREAD,
        y: PARTICLE_WATER_TOP_Y,
        vx: (Math.random() - 0.5) * PARTICLE_VX_VAR * 2,
        vy: PARTICLE_VY0 - Math.random() * PARTICLE_VY_VAR,
        life: PARTICLE_MAX_LIFE,
        // 70% gotas de 1px, 30% de 2px — un poco de variedad visual.
        size: Math.random() < 0.7 ? 1 : 2,
      });
    }
    for (let i = inst.particles.length - 1; i >= 0; i--) {
      const p = inst.particles[i];
      p.vy += PARTICLE_GRAVITY * dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.life -= dt;
      // Retirar al caer en la pileta o al expirar.
      if (p.life <= 0 || p.y > PARTICLE_BASIN_Y) {
        inst.particles.splice(i, 1);
      }
    }
  }

  private drawFountainParticles(ctx: CanvasRenderingContext2D, inst: DecorationInstance) {
    ctx.fillStyle = PARTICLE_COLOR;
    for (const p of inst.particles) {
      // Fade-out al final de la vida para que no desaparezcan abruptamente.
      const fade = Math.min(1, p.life * 2);
      ctx.globalAlpha = fade;
      const sz = Math.max(1, Math.round(p.size * this.scale));
      const screenX = Math.round(inst.worldX + p.x * this.scale);
      const screenY = Math.round(inst.worldY + p.y * this.scale);
      ctx.fillRect(screenX, screenY, sz, sz);
    }
    ctx.globalAlpha = 1;
  }

  private bakeFrames() {
    this.fountainFrames = this.bakeFountain();
    this.flowerFrames = this.bakeFlower();
  }

  private bakeFountain(): HTMLCanvasElement[] {
    const img = this.fountainImg;
    const W = img.naturalWidth;
    const H = img.naturalHeight;
    const wr = FOUNTAIN_WATER_RECT;
    const frames: HTMLCanvasElement[] = [];

    for (let i = 0; i < FOUNTAIN_FRAME_COUNT; i++) {
      const canvas = document.createElement('canvas');
      canvas.width = W;
      canvas.height = H;
      const c = canvas.getContext('2d')!;
      c.imageSmoothingEnabled = false;
      // Base: fuente completa en posición de reposo.
      c.drawImage(img, 0, 0);

      const t = (i / FOUNTAIN_FRAME_COUNT) * Math.PI * 2;
      // Sólo respiración vertical: la "vida" de la fuente viene del spray.
      const bobY = Math.round(Math.sin(t) * FOUNTAIN_BOB_PX);

      if (bobY !== 0) {
        // Borramos el rect del agua y lo redibujamos desplazado en Y: el
        // contorno dorado de la fuente queda quieto, solo se mueve el chorro.
        c.clearRect(wr.x, wr.y, wr.w, wr.h);
        c.drawImage(
          img,
          wr.x, wr.y, wr.w, wr.h,
          wr.x, wr.y + bobY, wr.w, wr.h,
        );
      }
      frames.push(canvas);
    }
    return frames;
  }

  private bakeFlower(): HTMLCanvasElement[] {
    const img = this.flowerImg;
    const W = img.naturalWidth;
    const H = img.naturalHeight;
    // Las hojas/tallo (mitad inferior) se quedan fijos; los pétalos (mitad
    // superior) bailan en círculo.
    const petalH = Math.floor(H / 2);
    const frames: HTMLCanvasElement[] = [];

    for (const [dx, dy] of FLOWER_DANCE) {
      const canvas = document.createElement('canvas');
      canvas.width = W;
      canvas.height = H;
      const c = canvas.getContext('2d')!;
      c.imageSmoothingEnabled = false;
      // Mitad inferior fija (tallo y hojas).
      c.drawImage(img, 0, petalH, W, H - petalH, 0, petalH, W, H - petalH);
      // Mitad superior desplazada (pétalos) en (dx, dy).
      c.drawImage(img, 0, 0, W, petalH, dx, dy, W, petalH);
      frames.push(canvas);
    }
    return frames;
  }
}
