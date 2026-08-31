import { computed, Injectable, signal } from '@angular/core';
import { CartItem } from '../../domain/models/cart.model';

const STORAGE_KEY = 'pokedex-cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<CartItem[]>(this.load());

  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, i) => sum + i.quantity, 0));
  readonly totalPrice = computed(() => this._items().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0));

  addItem(item: CartItem): void {
    this._items.update(items => {
      const existing = items.find(i => i.id === item.id);
      if (existing) {
        return items.map(i => i.id === item.id ? { ...i, quantity: i.quantity + item.quantity } : i);
      }
      return [...items, item];
    });
    this.persist();
  }

  getItemQuantity(id: string): number {
    const item = this._items().find(i => i.id === id);
    return item ? item.quantity : 0;
  }

  removeItem(id: string): void {
    this._items.update(items => items.filter(i => i.id !== id));
    this.persist();
  }

  updateQuantity(id: string, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(id);
      return;
    }
    this._items.update(items => items.map(i => i.id === id ? { ...i, quantity } : i));
    this.persist();
  }

  clearCart(): void {
    this._items.set([]);
    this.persist();
  }

  private persist(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this._items()));
  }

  private load(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) as CartItem[] : [];
    } catch {
      return [];
    }
  }
}
