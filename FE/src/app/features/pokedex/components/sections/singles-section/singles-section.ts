import {
  Component,
  computed,
  inject,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { Card, CardFilters } from '../../../domain/models/card.model';
import { CardService } from '../../../data-access/services/card.service';
import { CartService } from '../../../data-access/services/cart.service';
import { getPrice } from '../../../domain/models/cart.model';
import { ShinyTitle } from '../../shared/shiny-title/shiny-title';
import { EnergyIcon } from '../../shared/energy-icon/energy-icon';
import { ToastService } from '../../../../../shared/services/toast.service';
import { StoreOffersService } from '../../../data-access/services/offers.service';
import { AuthService } from '../../../../game/data-access/services/auth.service';

const ENERGY_TYPES = [
  { key: 'Fire', label: 'Fuego', icon: 'fire' },
  { key: 'Water', label: 'Agua', icon: 'water' },
  { key: 'Grass', label: 'Planta', icon: 'grass' },
  { key: 'Lightning', label: 'Rayo', icon: 'lightning' },
  { key: 'Psychic', label: 'Psíquico', icon: 'psychic' },
  { key: 'Fighting', label: 'Lucha', icon: 'fighting' },
  { key: 'Darkness', label: 'Oscuro', icon: 'darkness' },
  { key: 'Metal', label: 'Metal', icon: 'metal' },
  { key: 'Fairy', label: 'Hada', icon: 'fairy' },
  { key: 'Dragon', label: 'Dragón', icon: 'dragon' },
  { key: 'Colorless', label: 'Incoloro', icon: 'colorless' },
];

const RARITIES = [
  'Common',
  'Uncommon',
  'Rare',
  'Rare Holo',
  'Rare Holo EX',
  'Rare Ace',
];

@Component({
  standalone: true,
  selector: 'app-singles-section',
  imports: [ReactiveFormsModule, ShinyTitle, EnergyIcon],
  templateUrl: './singles-section.html',
  styleUrl: './singles-section.scss',
})
export class SinglesSection implements OnInit {
  private readonly cardService = inject(CardService);
  protected readonly cartService = inject(CartService);
  private readonly offersService = inject(StoreOffersService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly back = output<void>();
  readonly cardDetail = output<Card>();

  readonly allCards = signal<Card[]>([]);
  readonly loading = signal(true);
  readonly selectedType = signal<string | null>(null);
  readonly selectedRarity = signal<string | null>(null);
  readonly searchTerm = signal<string>('');

  readonly energyTypes = ENERGY_TYPES;
  readonly rarities = RARITIES;

  readonly searchControl = new FormControl<string>('', { nonNullable: true });

  readonly cards = computed(() => {
    let list = this.allCards();
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.selectedType();
    const rarity = this.selectedRarity();

    if (term) {
      list = list.filter((c) =>
        c.name.toLowerCase().includes(term) ||
        (c.supertype && c.supertype.toLowerCase().includes(term)) ||
        (c.subtypes && c.subtypes.some((s) => s.toLowerCase().includes(term)))
      );
    }

    if (type) {
      list = list.filter((c) =>
        (c.types && c.types.some((t) => t.toLowerCase() === type.toLowerCase())) ||
        (c.supertype?.toLowerCase() === 'energy' && c.name.toLowerCase().includes(type.toLowerCase()))
      );
    }

    if (rarity) {
      list = list.filter((c) =>
        c.rarity && c.rarity.toLowerCase() === rarity.toLowerCase()
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.cardService.getCards().subscribe({
      next: (data) => {
        this.allCards.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.show('Error al cargar cartas', 'error');
      },
    });

    this.searchControl.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged())
      .subscribe((val) => {
        this.searchTerm.set(val || '');
      });
  }

  toggleType(typeKey: string): void {
    if (this.selectedType() === typeKey) {
      this.selectedType.set(null);
    } else {
      this.selectedType.set(typeKey);
    }
  }

  toggleRarity(rarity: string): void {
    if (this.selectedRarity() === rarity) {
      this.selectedRarity.set(null);
    } else {
      this.selectedRarity.set(rarity);
    }
  }

  resetFilters(): void {
    this.searchControl.setValue('');
    this.searchTerm.set('');
    this.selectedType.set(null);
    this.selectedRarity.set(null);
  }

  getCardPriceInfo(card: Card) {
    return this.offersService.getEffectiveCardPrice(card);
  }

  getCardPrice(card: Card): number {
    return this.offersService.getEffectiveCardPrice(card).price;
  }

  getOwnedCopies(cardId: string): number {
    return this.authService.currentUser()?.collectionQuantities?.[cardId] ?? 0;
  }

  isCardAtMax(cardId: string): boolean {
    const owned = this.getOwnedCopies(cardId);
    const inCart = this.cartService.getItemQuantity(cardId);
    return (owned + inCart) >= 4;
  }

  getQtyInCart(cardId: string): number {
    return this.cartService.getItemQuantity(cardId);
  }

  addToCart(card: Card, event?: Event): void {
    event?.stopPropagation();
    if (this.isCardAtMax(card.id)) {
      this.toast.show(
        `Ya posees el límite de 4 copias de ${card.name} en tu colección`,
        'error',
      );
      return;
    }
    const priceInfo = this.getCardPriceInfo(card);
    this.cartService.addItem({
      id: card.id,
      name: card.name,
      imageUrl: card.images?.small || card.images?.large || '',
      type: 'card',
      rarity: card.rarity,
      quantity: 1,
      unitPrice: priceInfo.price,
    });
    const toastMsg = priceInfo.hasDiscount
      ? `¡${card.name} (${priceInfo.highlight}) añadida al carrito!`
      : `${card.name} añadida al carrito`;
    this.toast.show(toastMsg, 'info');
  }

  openDetail(card: Card): void {
    this.cardDetail.emit(card);
  }
}
