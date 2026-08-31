import { Component, input, output } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-checkout-button',
  templateUrl: './checkout-button.html',
  styleUrl: './checkout-button.scss',
})
export class CheckoutButton {
  readonly text = input<string>('Pagar');
  readonly disabled = input<boolean>(false);
  readonly clicked = output<void>();
}
