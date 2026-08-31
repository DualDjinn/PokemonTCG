import { Component, input, output } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-add-to-cart-button',
  templateUrl: './add-to-cart-button.html',
  styleUrl: './add-to-cart-button.scss',
})
export class AddToCartButton {
  readonly text = input<string>('Agregar');
  readonly disabled = input<boolean>(false);
  readonly added = output<void>();

  protected onClick(): void {
    if (!this.disabled()) {
      this.added.emit();
    }
  }
}
