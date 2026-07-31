import {
  ChangeDetectionStrategy, Component, EventEmitter, inject, OnInit,
  Output, signal,
} from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { PixelPanelDirective, PixelBorderDirective } from '../../../../shared/directives';
import { ToastService } from '../../../../shared/services/toast.service';
import { AuthService } from '../../../game/data-access/services/auth.service';

import { PackService } from '../../data-access/services/pack.service';
import { OpenedPack, PackInfo, PackType } from '../../domain/models/pack.model';

@Component({
  selector: 'app-pack-shop',
  standalone: true,
  imports: [TranslateModule, MyText, MyButton, PixelPanelDirective, PixelBorderDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section appPixelPanel class="flex flex-col gap-6 p-6 opacity-80">
      <header class="flex items-center justify-between flex-wrap gap-4">
        <div class="flex flex-col gap-1">
          <my-text
            [text]="'PACKS_SHOP.TITLE' | translate" tag="h2"
            size="1.2rem" weight="bold" color="var(--neon-cyan)" />
          <my-text
            [text]="'PACKS_SHOP.SUBTITLE' | translate" tag="p"
            size="0.72rem" color="var(--text-secondary)" />
        </div>
        <div appPixelBorder class="flex items-center gap-2 px-3 py-1.5 bg-black/30">
          <img src="/assets/img/Moneda.png" alt="Moneda" class="w-5 h-5 inline-block align-middle" />
          <my-text
            [text]="(authService.currentUser()?.coins ?? 0) + ''" tag="span"
            size="0.9rem" weight="bold" color="var(--neon-cyan)" />
          <my-text
            [text]="'PACKS_SHOP.COINS' | translate" tag="span"
            size="0.6rem" color="var(--text-secondary)" />
        </div>
      </header>

      @if (loading()) {
        <div class="flex items-center justify-center p-12">
          <div class="w-10 h-10 border-4 border-white/10 border-t-[var(--neon-cyan)]
                      rounded-full animate-spin"></div>
        </div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
          @for (pack of packs(); track pack.type) {
            <div appPixelPanel="card" class="pack-card flex flex-col items-center gap-4 p-6 transition-transform"
                 [class]="'pack-card--' + pack.type.toLowerCase()">

              <!-- Visual del sobre: imagen 16-bit -->
              <div class="pack-visual">
                <div class="pack-shine-clip">
                  <img [src]="packImage(pack.type)" [alt]="pack.type" class="pack-img" />
                  <div class="pack-body-shine"></div>
                </div>
              </div>

              <!-- Precio -->
              <div class="flex items-center gap-2">
                <img src="/assets/img/Moneda.png" alt="Moneda" class="w-6 h-6 inline-block align-middle" />
                <my-text
                  [text]="pack.price + ''" tag="span"
                  size="1rem" weight="bold" color="var(--neon-cyan)" />
              </div>

              <!-- Distribución -->
              <div class="flex flex-col gap-1 w-full text-center">
                <my-text
                  [text]="cardsLabel(pack)" tag="p"
                  size="0.66rem" color="var(--text-secondary)" />
                <my-text
                  [text]="probabilitySummary(pack)" tag="p"
                  size="0.54rem" color="var(--text-secondary)" />
              </div>

              <!-- Botón comprar -->
              <my-button
                [text]="purchasing() === pack.type
                  ? ('PACKS_SHOP.OPENING' | translate)
                  : ('PACKS_SHOP.BUY' | translate)"
                variant="pixel" size="medium"
                color="var(--neon-cyan)"
                [disabled]="purchasing() !== null || !canAfford(pack)"
                (clicked)="onBuy(pack)" />

              @if (!canAfford(pack)) {
                <my-text
                  [text]="'PACKS_SHOP.NOT_ENOUGH_COINS' | translate" tag="p"
                  size="0.54rem" color="var(--neon-magenta)" />
              }
            </div>
          }
        </div>
      }
    </section>
  `,
  styles: [`
    .pack-card { transition: transform 0.35s ease; }
    .pack-card:hover { transform: translateY(-6px); }

    /* Glow ambiental pulsante por rareza */
    /* Bronze usa azul (color del sobre base) */
    @keyframes pulse-bronze  { 0%,100% { box-shadow: 0 0 18px rgba(40,100,230,0.2); }  50% { box-shadow: 0 0 48px rgba(40,100,230,0.6); } }
    @keyframes pulse-silver  { 0%,100% { box-shadow: 0 0 18px rgba(192,192,192,0.2); } 50% { box-shadow: 0 0 48px rgba(192,192,192,0.6); } }
    @keyframes pulse-gold    { 0%,100% { box-shadow: 0 0 20px rgba(255,215,0,0.25); }  50% { box-shadow: 0 0 56px rgba(255,215,0,0.75); } }
    @keyframes pulse-diamond { 0%,100% { box-shadow: 0 0 24px rgba(125,224,255,0.3); } 50% { box-shadow: 0 0 64px rgba(125,224,255,0.85); } }

    .pack-card--bronze  { animation: pulse-bronze  3.5s ease-in-out infinite; }
    .pack-card--silver  { animation: pulse-silver  3.5s ease-in-out infinite; }
    .pack-card--gold    { animation: pulse-gold    3s   ease-in-out infinite; }
    .pack-card--diamond { animation: pulse-diamond 2.5s ease-in-out infinite; }

    /* Contenedor externo: el transform va aquí para no clipear con overflow:hidden interno */
    .pack-visual {
      display: flex;
      align-items: center;
      justify-content: center;
      transition: filter 0.35s ease, transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    .pack-visual:hover {
      transform: translateY(-10px) scale(1.07);
    }

    /* Glow hover por rareza */
    .pack-card--bronze  .pack-visual:hover { filter: drop-shadow(0 0 14px rgba(40,120,255,0.95))  drop-shadow(0 10px 22px rgba(40,120,255,0.5)); }
    .pack-card--silver  .pack-visual:hover { filter: drop-shadow(0 0 14px rgba(192,192,192,0.95)) drop-shadow(0 10px 22px rgba(192,192,192,0.5)); }
    .pack-card--gold    .pack-visual:hover { filter: drop-shadow(0 0 18px rgba(255,215,0,1.0))    drop-shadow(0 10px 26px rgba(255,215,0,0.7)); }
    .pack-card--diamond .pack-visual:hover { filter: drop-shadow(0 0 20px rgba(125,224,255,1.0))  drop-shadow(0 10px 30px rgba(125,224,255,0.8)); }

    /* Wrapper interno: contiene SOLO el barrido de brillo */
    .pack-shine-clip {
      position: relative;
      display: inline-flex;
      overflow: hidden;
    }

    .pack-img {
      display: block;
      height: 215px;
      width: auto;
      image-rendering: pixelated;
    }

    /* Barrido de brillo contenido dentro de pack-shine-clip */
    .pack-body-shine {
      position: absolute; inset: 0;
      background: linear-gradient(-110deg,
        transparent 20%,
        rgba(255,255,255,0.5) 50%,
        transparent 80%);
      transform: translateX(120%);
      transition: transform 0.65s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      pointer-events: none;
      mix-blend-mode: overlay;
    }
    .pack-visual:hover .pack-body-shine {
      transform: translateX(-120%);
    }
  `],
})
export class PackShopComponent implements OnInit {
  private readonly packService = inject(PackService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);
  readonly authService = inject(AuthService);

  @Output() readonly packOpened = new EventEmitter<OpenedPack>();

  readonly packs = signal<PackInfo[]>([]);
  readonly loading = signal(true);
  readonly purchasing = signal<PackType | null>(null);

  ngOnInit(): void {
    this.packService.listPacks().subscribe({
      next: data => {
        this.packs.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.show(this.translate.instant('PACKS_SHOP.ERROR_LOAD'), 'error');
      }
    });
  }

  canAfford(pack: PackInfo): boolean {
    const coins = this.authService.currentUser()?.coins ?? 0;
    return coins >= pack.price;
  }

  packImage(type: PackType): string {
    const names: Record<PackType, string> = {
      BRONZE: 'base', SILVER: 'plata', GOLD: 'oro', DIAMOND: 'diamante',
    };
    return `/assets/animation_store/${names[type]}_16bit.png`;
  }

  cardsLabel(pack: PackInfo): string {
    if (pack.type === 'DIAMOND') {
      return this.translate.instant('PACKS_SHOP.DIAMOND_CARDS');
    }
    return `${pack.cardsPerPack} ${this.translate.instant('PACKS_SHOP.CARDS')}`;
  }

  probabilitySummary(pack: PackInfo): string {
    if (pack.type === 'DIAMOND') {
      return this.translate.instant('PACKS_SHOP.DIAMOND_GUARANTEED');
    }
    const c = Math.round((pack.probabilities.HIGH ?? 0) * 100);
    const u = Math.round((pack.probabilities.MEDIUM ?? 0) * 100);
    const r = Math.round((pack.probabilities.LOW ?? 0) * 100);
    return `C ${c}% · U ${u}% · R ${r}%`;
  }

  onBuy(pack: PackInfo): void {
    if (this.purchasing() || !this.canAfford(pack)) return;
    this.purchasing.set(pack.type);

    this.packService.openPack(pack.type).subscribe({
      next: opened => {
        this.purchasing.set(null);
        this.authService.getCurrentPlayer().subscribe();
        this.packOpened.emit(opened);
      },
      error: err => {
        this.purchasing.set(null);
        const key = err?.status === 402
          ? 'PACKS_SHOP.NOT_ENOUGH_COINS'
          : 'PACKS_SHOP.ERROR_OPEN';
        this.toast.show(this.translate.instant(key), 'error');
      }
    });
  }
}
