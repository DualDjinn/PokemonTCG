import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

interface Spark {
  ang: number;
  dist: number;
  len: number;
  dur: number;
  delay: number;
}

interface Ember {
  ex: number;
  ey: number;
  dur: number;
  delay: number;
}

interface Palette {
  core: string;
  bright: string;
  mid: string;
}

type Tier = 'weak' | 'normal' | 'strong';

/** Paleta por tipo TCG (núcleo claro → brillante → medio). */
const PALETTES: Record<string, Palette> = {
  FIRE: { core: '#fff6cc', bright: '#ff8a1e', mid: '#ff3b00' },
  WATER: { core: '#e8fbff', bright: '#5cc8ff', mid: '#1f8bff' },
  LIGHTNING: { core: '#ffffff', bright: '#fff07a', mid: '#ffd60a' },
  GRASS: { core: '#f0ffd6', bright: '#8ef26a', mid: '#3fbf3f' },
  PSYCHIC: { core: '#ffe6fb', bright: '#ff7ae0', mid: '#c850ff' },
  FIGHTING: { core: '#ffe6cc', bright: '#ff9a52', mid: '#d2691e' },
  DARKNESS: { core: '#d9c8ff', bright: '#9a6ad0', mid: '#5b2a86' },
  METAL: { core: '#eef2ff', bright: '#bfcad8', mid: '#8a93a8' },
  DRAGON: { core: '#e0e6ff', bright: '#7c8cff', mid: '#4b3fd0' },
  FAIRY: { core: '#ffe6f3', bright: '#ff9ed6', mid: '#ff5ea8' },
  COLORLESS: { core: '#fbfbff', bright: '#dfe2ea', mid: '#aab0c0' },
};

/** Umbrales de poder (daño del ataque) que definen el nivel del efecto. */
const NORMAL_MIN = 50;
const STRONG_MIN = 100;

/**
 * Efecto de impacto de ataque (CSS, blend aditivo). Se monta en el momento del
 * golpe y se reproduce una sola vez; el contenedor padre lo desmonta al limpiar.
 *
 * El **nivel** depende del `power` (daño del ataque):
 * - `weak` (<50): burst pequeño y sobrio.
 * - `normal` (50–99): burst completo.
 * - `strong` (≥100): espectacular — escala mayor, onda de choque extra,
 *   destello en estrella y más chispas/brasas.
 *
 * El color sale del `type` (pokemonType del atacante).
 */
@Component({
  selector: 'app-attack-effect',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[style.--core]': 'palette().core',
    '[style.--bright]': 'palette().bright',
    '[style.--mid]': 'palette().mid',
  },
  template: `
    <div class="fx-root" [style.--scale]="scale()">
      <div class="scrim"></div>
      <div class="flash"></div>
      <div class="core"></div>
      <div class="shock" style="--dur:0.58s"></div>
      <div class="shock" style="--dur:0.66s;--delay:0.07s"></div>

      @if (tier() === 'strong') {
        <div class="shock shock-outer" style="--dur:0.85s;--delay:0.04s"></div>
        <div class="flare"></div>
      }

      @for (s of sparks(); track $index) {
        <div
          class="spark"
          [style.--ang]="s.ang + 'deg'"
          [style.--dist]="s.dist + 'px'"
          [style.--len]="s.len + 'px'"
          [style.--dur]="s.dur + 's'"
          [style.--delay]="s.delay + 's'"
        ></div>
      }
      @for (e of embers(); track $index) {
        <div
          class="ember"
          [style.--ex]="e.ex + 'px'"
          [style.--ey]="e.ey + 'px'"
          [style.--dur]="e.dur + 's'"
          [style.--delay]="e.delay + 's'"
        ></div>
      }
    </div>
  `,
  styles: [`
    :host {
      position: absolute;
      inset: 0;
      display: block;
      pointer-events: none;
    }
    .fx-root {
      position: absolute;
      inset: 0;
      transform: scale(var(--scale, 1));
      transform-origin: center;
    }
    .fx-root > div {
      position: absolute;
      left: 50%;
      top: 50%;
    }
    .scrim {
      width: 240px;
      height: 240px;
      margin: -120px 0 0 -120px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(0,0,0,0.5) 0%, transparent 62%);
      animation: fx-scrim 0.5s ease-out forwards;
    }
    @keyframes fx-scrim {
      0% { transform: scale(0.4); opacity: 0.85; }
      100% { transform: scale(2.4); opacity: 0; }
    }
    .flash {
      width: 132px; height: 132px; margin: -66px 0 0 -66px; border-radius: 50%;
      background: radial-gradient(circle, var(--core) 0%, var(--bright) 32%, transparent 66%);
      mix-blend-mode: screen; filter: blur(3px);
      animation: fx-flash 0.3s ease-out forwards;
    }
    @keyframes fx-flash {
      0% { transform: scale(0.3); opacity: 1; }
      100% { transform: scale(2.7); opacity: 0; }
    }
    .core {
      width: 104px; height: 104px; margin: -52px 0 0 -52px; border-radius: 50%;
      background: radial-gradient(circle, var(--core), var(--mid) 45%, transparent 70%);
      mix-blend-mode: screen; filter: blur(6px);
      animation: fx-core 0.62s ease-out forwards;
    }
    @keyframes fx-core {
      0% { transform: scale(0.4); opacity: 0.95; }
      60% { opacity: 0.6; }
      100% { transform: scale(1.75); opacity: 0; }
    }
    .shock {
      width: 76px; height: 76px; margin: -38px 0 0 -38px; border-radius: 50%;
      border: 4px solid var(--bright);
      box-shadow: 0 0 18px var(--mid), inset 0 0 12px var(--core);
      mix-blend-mode: screen;
      animation: fx-shock var(--dur, 0.58s) cubic-bezier(0.1, 0.7, 0.25, 1) forwards;
      animation-delay: var(--delay, 0s);
    }
    @keyframes fx-shock {
      0% { transform: scale(0.2); opacity: 0.9; }
      100% { transform: scale(3.5); opacity: 0; }
    }
    .shock-outer {
      width: 110px; height: 110px; margin: -55px 0 0 -55px;
      border-width: 5px;
    }
    .flare {
      width: 210px; height: 210px; margin: -105px 0 0 -105px;
      background: var(--core);
      clip-path: polygon(50% 0%, 60% 40%, 100% 50%, 60% 60%, 50% 100%, 40% 60%, 0% 50%, 40% 40%);
      mix-blend-mode: screen; filter: blur(2px);
      animation: fx-flare 0.55s ease-out forwards;
    }
    @keyframes fx-flare {
      0% { transform: scale(0.15) rotate(0deg); opacity: 0; }
      25% { opacity: 1; }
      100% { transform: scale(1.35) rotate(40deg); opacity: 0; }
    }
    .spark {
      width: 3px; height: var(--len, 26px);
      margin-left: -1.5px; margin-top: calc(var(--len, 26px) / -2);
      border-radius: 2px; transform-origin: center center;
      background: linear-gradient(to top, transparent, var(--mid), var(--bright) 70%, var(--core));
      mix-blend-mode: screen;
      animation: fx-spark var(--dur, 0.5s) cubic-bezier(0.12, 0.72, 0.25, 1) forwards;
      animation-delay: var(--delay, 0s);
    }
    @keyframes fx-spark {
      0% { transform: rotate(var(--ang)) translateY(-5px) scaleY(0.5); opacity: 0; }
      14% { opacity: 1; }
      70% { opacity: 1; }
      100% { transform: rotate(var(--ang)) translateY(calc(-1 * var(--dist))) scaleY(1.25); opacity: 0; }
    }
    .ember {
      width: 5px; height: 5px; margin: -2.5px 0 0 -2.5px; border-radius: 50%;
      background: var(--bright); box-shadow: 0 0 7px var(--bright);
      mix-blend-mode: screen;
      animation: fx-ember var(--dur, 0.9s) cubic-bezier(0.2, 0.6, 0.4, 1) forwards;
      animation-delay: var(--delay, 0s);
    }
    @keyframes fx-ember {
      0% { transform: translate(0, 0) scale(1); opacity: 0; }
      16% { opacity: 1; }
      100% { transform: translate(var(--ex), var(--ey)) scale(0.3); opacity: 0; }
    }
    @media (prefers-reduced-motion: reduce) {
      .spark, .ember, .flare { display: none; }
    }
  `],
})
export class AttackEffectComponent {
  /** Tipo del atacante (pokemonType); determina la paleta. */
  readonly type = input<string | null>(null);

  /** Poder del ataque (daño); determina el nivel del efecto. */
  readonly power = input<number>(0);

  readonly palette = computed<Palette>(() =>
    PALETTES[(this.type() ?? 'COLORLESS').toUpperCase()] ?? PALETTES['COLORLESS']);

  readonly tier = computed<Tier>(() => {
    const p = this.power();
    if (p >= STRONG_MIN) return 'strong';
    if (p >= NORMAL_MIN) return 'normal';
    return 'weak';
  });

  /** Factor de escala global del efecto según el nivel. */
  readonly scale = computed(() => {
    const t = this.tier();
    return t === 'strong' ? 1.5 : t === 'normal' ? 1.1 : 0.8;
  });

  private readonly sparkCount = computed(() => {
    const t = this.tier();
    return t === 'strong' ? 28 : t === 'normal' ? 18 : 12;
  });

  private readonly emberCount = computed(() => {
    const t = this.tier();
    return t === 'strong' ? 14 : t === 'normal' ? 9 : 6;
  });

  /** Chispas generadas según el nivel (la instancia se monta nueva en cada golpe). */
  readonly sparks = computed<Spark[]>(() => {
    const n = this.sparkCount();
    return Array.from({ length: n }, (_, i) => ({
      ang: (360 / n) * i + (Math.random() * 24 - 12),
      dist: 85 + Math.random() * 90,
      len: 28 + Math.random() * 22,
      dur: 0.44 + Math.random() * 0.24,
      delay: Math.random() * 0.08,
    }));
  });

  readonly embers = computed<Ember[]>(() => {
    const n = this.emberCount();
    return Array.from({ length: n }, () => {
      const a = Math.random() * Math.PI * 2;
      const d = 55 + Math.random() * 100;
      return {
        ex: Math.cos(a) * d,
        ey: Math.sin(a) * d + (12 + Math.random() * 55),
        dur: 0.75 + Math.random() * 0.45,
        delay: Math.random() * 0.13,
      };
    });
  });
}
