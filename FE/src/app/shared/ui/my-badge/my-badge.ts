import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MyText } from '../my-text/my-text';

/**
 * Small status pill.
 *
 * Used for short pieces of metadata that need visual emphasis: HP value,
 * energy count, card type label, status condition. Background and text
 * colors are inputs so the same component covers every badge style.
 */
@Component({
  selector: 'my-badge',
  standalone: true,
  imports: [MyText],
  templateUrl: './my-badge.html',
  styleUrl: './my-badge.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyBadge {
  @Input() text = '';
  @Input() color = 'var(--neon-magenta)';
  @Input() textColor = 'var(--text-primary)';
  @Input() borderColor: string | null = null;

  get resolvedBorderColor(): string {
    return this.borderColor ?? this.color;
  }
}
