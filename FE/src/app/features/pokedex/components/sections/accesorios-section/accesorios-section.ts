import {
  Component,
  computed,
  inject,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { SplashArtService } from '../../../data-access/services/splash-art.service';
import { SplashArt } from '../../../domain/models/splash-art.model';
import {
  CardBack,
  CosmeticsService,
  PlayerCardBacks,
} from '../../../../profile/data-access/cosmetics.service';
import { CartService } from '../../../data-access/services/cart.service';
import { ShinyTitle } from '../../shared/shiny-title/shiny-title';
import { ToastService } from '../../../../../shared/services/toast.service';

type SubTab = 'fullArts' | 'cardBacks';

@Component({
  standalone: true,
  selector: 'app-accesorios-section',
  imports: [ReactiveFormsModule, ShinyTitle],
  templateUrl: './accesorios-section.html',
  styleUrl: './accesorios-section.scss',
})
export class AccesoriosSection implements OnInit {
  private readonly splashArtService = inject(SplashArtService);
  private readonly cosmeticsService = inject(CosmeticsService);
  protected readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);

  readonly back = output<void>();
  readonly splashDetail = output<SplashArt>();
  readonly cardBackDetail = output<CardBack>();

  readonly activeSubTab = signal<SubTab>('fullArts');
  readonly loading = signal(true);

  readonly splashArts = signal<SplashArt[]>([]);
  readonly cardBacks = signal<CardBack[]>([]);
  readonly playerCardBacks = signal<PlayerCardBacks | null>(null);

  readonly searchControl = new FormControl<string>('');

  protected readonly filteredSplashArts = computed(() => {
    const term = (this.searchControl.value || '').toLowerCase().trim();
    if (!term) return this.splashArts();
    return this.splashArts().filter(
      (sa) =>
        sa.name.toLowerCase().includes(term) ||
        sa.pokemonName.toLowerCase().includes(term),
    );
  });

  protected readonly filteredCardBacks = computed(() => {
    const term = (this.searchControl.value || '').toLowerCase().trim();
    if (!term) return this.cardBacks();
    return this.cardBacks().filter((cb) =>
      cb.name.toLowerCase().includes(term),
    );
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.splashArtService.getSplashArts().subscribe({
      next: (list) => {
        this.splashArts.set(list);
        this.checkFinishedLoading();
      },
      error: () => this.checkFinishedLoading(),
    });

    this.cosmeticsService.getCardBackCatalog().subscribe({
      next: (backs) => {
        this.cardBacks.set(
          backs.filter((cb) => cb.origin !== 'DEFAULT' && cb.id !== 'pixel'),
        );
        this.checkFinishedLoading();
      },
      error: () => this.checkFinishedLoading(),
    });

    this.cosmeticsService.getPlayerCardBacks().subscribe({
      next: (pcb) => this.playerCardBacks.set(pcb),
      error: () => {},
    });
  }

  private checkFinishedLoading(): void {
    this.loading.set(false);
  }

  setSubTab(tab: SubTab): void {
    this.activeSubTab.set(tab);
    this.searchControl.setValue('');
  }

  isCardBackOwned(id: string): boolean {
    const pcb = this.playerCardBacks();
    return pcb?.owned?.some((cb) => cb.id === id) ?? false;
  }

  getSplashPrice(sa: SplashArt): number {
    return sa.kind === 'original' ? 2000 : 1500;
  }

  getQtyInCart(id: string): number {
    return this.cartService.getItemQuantity(id);
  }

  addSplashToCart(sa: SplashArt, event?: Event): void {
    event?.stopPropagation();
    this.cartService.addItem({
      id: sa.id,
      name: sa.name,
      imageUrl: sa.imageUrl,
      type: 'splashArt',
      rarity: sa.rarity,
      quantity: 1,
      unitPrice: this.getSplashPrice(sa),
    });
    this.toast.show(`${sa.name} añadido al carrito`, 'info');
  }

  addCardBackToCart(cb: CardBack, event?: Event): void {
    event?.stopPropagation();
    if (this.isCardBackOwned(cb.id) || !cb.price) return;
    this.cartService.addItem({
      id: cb.id,
      name: cb.name,
      imageUrl: cb.imageUrl,
      type: 'cardBack',
      rarity: 'Cosmetic',
      quantity: 1,
      unitPrice: cb.price,
    });
    this.toast.show(`Reverso ${cb.name} añadido al carrito`, 'info');
  }
}
