import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MyText } from '../my-text/my-text';

/**
 * Reusable button used across the app.
 *
 * The same component covers every visual variant in the project: callers
 * change the background color, text color and label rather than creating
 * a new button class per use site. Renders its label through MyText so
 * typography stays consistent with the rest of the app.
 */
export type MyButtonVariant = 'solid' | 'outline' | 'pixel' | 'modern';
export type MyButtonSize = 'small' | 'medium' | 'large';

@Component({
  selector: 'my-button',
  standalone: true,
  imports: [MyText],
  templateUrl: './my-button.html',
  styleUrl: './my-button.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyButton {
  @Input() text = '';
  @Input() color = '#0284c7';
  @Input() textColor = 'var(--text-primary)';
  @Input() variant: MyButtonVariant = 'solid';
  @Input() size: MyButtonSize = 'medium';
  @Input() disabled = false;
  @Input() type: 'button' | 'submit' | 'reset' = 'button';

  @Output() clicked = new EventEmitter<MouseEvent>();

  get fontSize(): string {
    if (this.variant === 'pixel') {
      switch (this.size) {
        case 'small': return '0.55rem';
        case 'medium': return '0.65rem';
        case 'large': return '0.75rem';
      }
    }
    switch (this.size) {
      case 'small':
        return '0.765rem';
      case 'large':
        return '1.08rem';
      default:
        return '0.9rem';
    }
  }

  get padding(): string {
    if (this.variant === 'pixel') {
      switch (this.size) {
        case 'small':
          return '0.35rem 0.9rem';
        case 'large':
          return '0.9rem 1rem';
        default:
          return '0.6rem 1.4rem';
      }
    }
    switch (this.size) {
      case 'small':
        return '0.35rem 0.9rem';
      case 'large':
        return '0.85rem 2rem';
      default:
        return '0.6rem 1.4rem';
    }
  }

  get background(): string {
    if (this.variant === 'pixel') return 'var(--pixel-card-bg)';
    return this.variant === 'solid' ? this.color : 'transparent';
  }

  get resolvedTextColor(): string {
    if (this.variant === 'pixel') return this.color;
    return this.variant === 'solid' ? this.textColor : this.color;
  }

  onClick(event: MouseEvent): void {
    if (this.disabled) {
      return;
    }
    this.clicked.emit(event);
  }
}
