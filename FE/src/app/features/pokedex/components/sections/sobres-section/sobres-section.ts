import {
  Component,
  computed,
  inject,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { PackService } from '../../../data-access/services/pack.service';
import { PackInfo, PackType } from '../../../domain/models/pack.model';
import { CartService } from '../../../data-access/services/cart.service';
import { AuthService } from '../../../../game/data-access/services/auth.service';
import { ToastService } from '../../../../../shared/services/toast.service';
import { TranslateService } from '@ngx-translate/core';
import { ShinyTitle } from '../../shared/shiny-title/shiny-title';
import { StoreOffersService } from '../../../data-access/services/offers.service';

const CARD_WIDTH = 340;
const CARD_HEIGHT = 480;
const SCALE_STEP = 0.18;
const MAX_VISIBLE = 1;
const DEPTH = 180;
const GAP = 8;
const TILT = 9;
const SIDE_TILT = 5;
const OPACITY = 25;
const MOVE_MS = 500;
const EASE = 'cubic-bezier(0.22, 1, 0.36, 1)';

@Component({
  standalone: true,
  selector: 'app-sobres-section',
  imports: [ShinyTitle],
  templateUrl: './sobres-section.html',
  styleUrl: './sobres-section.scss',
})
export class SobresSection implements OnInit {
  private readonly packService = inject(PackService);
  private readonly cartService = inject(CartService);
  private readonly offersService = inject(StoreOffersService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);
  protected readonly authService = inject(AuthService);

  readonly back = output<void>();

  readonly packs = signal<PackInfo[]>([]);
  readonly loading = signal(true);
  readonly active = signal(0);
  readonly cantidad = signal(1);

  readonly cardWidth = CARD_WIDTH;
  readonly cardHeight = CARD_HEIGHT;

  protected readonly activePack = computed(() => {
    const list = this.packs();
    if (list.length === 0) return null;
    return list[this.active() % list.length];
  });

  protected readonly transitionCss = computed(
    () => `transform ${MOVE_MS}ms ${EASE}, opacity ${MOVE_MS}ms ${EASE}`,
  );
  protected readonly dim = 1 - OPACITY / 100;

  private locked = false;

  ngOnInit(): void {
    this.packService.listPacks().subscribe({
      next: (data) => {
        this.packs.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.show(
          this.translate.instant('PACKS_SHOP.ERROR_LOAD') || 'Error al cargar sobres',
          'error',
        );
      },
    });
  }

  getPackPriceInfo(pack: PackInfo) {
    return this.offersService.getEffectivePackPrice(pack.type, pack.price);
  }

  packImage(type: PackType): string {
    const names: Record<PackType, string> = {
      BRONZE: 'base',
      SILVER: 'plata',
      GOLD: 'oro',
      DIAMOND: 'diamante',
    };
    return `/assets/animation_store/${names[type]}_16bit.png`;
  }

  packTitle(type: PackType): string {
    const titles: Record<PackType, string> = {
      BRONZE: 'Sobre Base',
      SILVER: 'Sobre Expansión',
      GOLD: 'Sobre Premium',
      DIAMOND: 'Sobre Legendario EX',
    };
    return titles[type] ?? type;
  }

  packDescription(pack: PackInfo): string {
    if (pack.type === 'DIAMOND') {
      return '1 carta Holo EX legendaria garantizada.';
    }
    const c = Math.round((pack.probabilities?.HIGH ?? 0.6) * 100);
    const u = Math.round((pack.probabilities?.MEDIUM ?? 0.3) * 100);
    const r = Math.round((pack.probabilities?.LOW ?? 0.1) * 100);
    return `${pack.cardsPerPack} cartas · Común ${c}% · Infrecuente ${u}% · Rara ${r}%`;
  }

  protected increment(): void {
    this.cantidad.update((c) => Math.min(10, c + 1));
  }

  protected decrement(): void {
    this.cantidad.update((c) => Math.max(1, c - 1));
  }

  step(dir: number): void {
    if (this.locked) return;
    this.lock();
    const n = this.packs().length;
    if (n === 0) return;
    this.active.update((a) => (((a + dir) % n) + n) % n);
    this.cantidad.set(1);
  }

  select(i: number): void {
    if (this.locked) return;
    this.lock();
    const n = this.packs().length;
    if (n === 0) return;
    if (i === this.active()) {
      this.active.update((a) => (a + 1) % n);
    } else {
      this.active.set(i);
    }
    this.cantidad.set(1);
  }

  private lock(): void {
    this.locked = true;
    setTimeout(() => {
      this.locked = false;
    }, Math.max(50, MOVE_MS));
  }

  protected rel(i: number): number {
    const n = this.packs().length;
    if (n === 0) return 0;
    let rel = i - this.active();
    if (rel > n / 2) rel -= n;
    if (rel < -n / 2) rel += n;
    return rel;
  }

  protected transform(i: number): string {
    const rel = this.rel(i);
    const ax = Math.abs(rel);
    const sc = Math.max(0.72, 1 - ax * SCALE_STEP);
    const tx = rel * (GAP * 42);
    const tz = -ax * DEPTH;
    const ry = -rel * TILT;
    const rz = rel * SIDE_TILT;
    return `translate(-50%, -50%) translateX(${tx}px) translateZ(${tz}px) rotateY(${ry}deg) rotateZ(${rz}deg) scale(${sc})`;
  }

  protected visible(i: number): boolean {
    return Math.abs(this.rel(i)) <= MAX_VISIBLE;
  }

  addToCart(): void {
    const pack = this.activePack();
    if (!pack) return;
    const priceInfo = this.getPackPriceInfo(pack);

    this.cartService.addItem({
      id: 'pack-' + pack.type,
      name: this.packTitle(pack.type),
      imageUrl: this.packImage(pack.type),
      type: 'pack',
      rarity: pack.type,
      quantity: this.cantidad(),
      unitPrice: priceInfo.price,
    });

    const toastMsg = priceInfo.hasDiscount
      ? `¡${this.cantidad()}x ${this.packTitle(pack.type)} (${priceInfo.highlight}) añadido al carrito!`
      : `${this.cantidad()}x ${this.packTitle(pack.type)} añadido al carrito`;

    this.toast.show(toastMsg, 'info');
  }
}
