import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MyModal } from '../my-modal/my-modal';
import { MyText } from '../../ui/my-text/my-text';
import { MyButton } from '../../ui/my-button/my-button';

@Component({
  selector: 'my-confirm-modal',
  standalone: true,
  imports: [MyModal, MyText, MyButton],
  templateUrl: './my-confirm-modal.html',
  styleUrl: './my-confirm-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyConfirmModal {
  @Input() open = false;
  @Input() title = '';
  @Input() message = '';
  @Input() confirmText = 'Confirm';
  @Input() cancelText = 'Cancel';
  @Input() confirmColor = 'var(--neon-cyan, #00ccaa)';
  @Input() confirmTextColor = '#050508';
  @Input() destructive = false;
  @Input() loading = false;
  @Input() width = '24rem';
  @Input() variant: 'solid' | 'pixel' | 'glass' = 'pixel';
  @Input() backdropStyle: 'dark' | 'blur' = 'dark';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  get resolvedConfirmColor(): string {
    return this.destructive ? 'var(--error-color, #ef4444)' : this.confirmColor;
  }

  onConfirm(): void {
    if (!this.loading) {
      this.confirm.emit();
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
