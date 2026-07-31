import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Compact icon-only button.
 *
 * Used for actions whose meaning is fully carried by the icon (close X,
 * settings gear, expand arrow). Accepts the icon glyph as a plain string
 * so callers can pass either a text character or an icon-font ligature
 * without coupling this component to a specific icon library.
 */
export type MyIconButtonSize = 'small' | 'medium' | 'large';
export type MyIconButtonVariant = 'solid' | 'pixel';

@Component({
  selector: 'my-icon-button',
  standalone: true,
  templateUrl: './my-icon-button.html',
  styleUrl: './my-icon-button.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyIconButton {
  @Input() icon = '';
  @Input() color = 'var(--neon-cyan)';
  @Input() background = 'transparent';
  @Input() size: MyIconButtonSize = 'medium';
  @Input() variant: MyIconButtonVariant = 'solid';
  @Input() disabled = false;
  @Input() ariaLabel = '';

  @Output() clicked = new EventEmitter<MouseEvent>();

  get dimension(): string {
    switch (this.size) {
      case 'small':
        return '1.75rem';
      case 'large':
        return '3rem';
      default:
        return '2.25rem';
    }
  }

  get fontSize(): string {
    switch (this.size) {
      case 'small':
        return '0.855rem';
      case 'large':
        return '1.35rem';
      default:
        return '1.035rem';
    }
  }

  onClick(event: MouseEvent): void {
    if (this.disabled) {
      return;
    }
    this.clicked.emit(event);
  }
}
