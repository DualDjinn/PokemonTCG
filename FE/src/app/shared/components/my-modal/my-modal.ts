import { ChangeDetectionStrategy, Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { MyText } from '../../ui/my-text/my-text';
import { MyIconButton } from '../../ui/my-icon-button/my-icon-button';

/**
 * Generic modal dialog with a backdrop, title bar and content slot.
 *
 * Used as the single dialog component for the whole project: callers
 * toggle the `open` input, set the title and color, and project the
 * body through <ng-content>. Closing is signaled via the `close` output,
 * which fires for the X button, the Escape key and the backdrop click.
 */
@Component({
  selector: 'my-modal',
  standalone: true,
  imports: [MyText, MyIconButton],
  templateUrl: './my-modal.html',
  styleUrl: './my-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyModal {
  @Input() open = false;
  @Input() title = '';
  @Input() variant: 'solid' | 'pixel' | 'glass' = 'solid';
  @Input() color = 'var(--neon-cyan)';
  @Input() background = 'var(--bg-secondary)';
  @Input() backdropStyle: 'dark' | 'blur' = 'dark';
  @Input() titleColor = 'var(--text-primary)';
  @Input() width = '32rem';
  @Input() closeOnBackdrop = true;
  @Input() showClose = true;
  @Input() backdropOpacity: number = 0.6;
  @Input() bodyOverflow: string = 'auto';
  @Input() bodyPadding: string = 'p-4';
  @Input() panelOpacity: number = 1;
  @Input() entranceAnimated = true;

  @Output() close = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.close.emit();
    }
  }

  onBackdropClick(): void {
    if (this.closeOnBackdrop) {
      this.close.emit();
    }
  }

  onCloseClick(): void {
    this.close.emit();
  }
}
