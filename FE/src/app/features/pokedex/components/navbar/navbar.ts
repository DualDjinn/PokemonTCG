import { Component, computed, inject, input, output } from '@angular/core';
import { SparkleText } from '../shared/sparkle-text/sparkle-text';
import { CartService } from '../../data-access/services/cart.service';
import { AuthService } from '../../../game/data-access/services/auth.service';

@Component({
  standalone: true,
  selector: 'app-shop-navbar',
  imports: [SparkleText],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class ShopNavbar {
  private readonly cartService = inject(CartService);
  protected readonly authService = inject(AuthService);

  readonly activeSection = input<string | null>(null);

  readonly navigateSection = output<'sobres' | 'decks' | 'singles' | 'accesorios' | null>();
  readonly openCart = output<void>();
  readonly openCoinsModal = output<void>();
  readonly exit = output<void>();

  protected readonly cartCount = computed(() => this.cartService.totalItems());
  protected readonly userCoins = computed(() => this.authService.currentUser()?.coins ?? 0);

  protected onSectionClick(section: 'sobres' | 'decks' | 'singles' | 'accesorios' | null, event?: Event): void {
    event?.preventDefault();
    this.navigateSection.emit(section);
  }
}
