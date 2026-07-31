import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Horizontal or vertical visual separator.
 *
 * Used between grouped sections of a panel or menu. Color and thickness
 * are inputs so callers can match the surrounding palette without
 * needing a new variant per use site.
 */
export type MyDividerOrientation = 'horizontal' | 'vertical';

@Component({
  selector: 'my-divider',
  standalone: true,
  templateUrl: './my-divider.html',
  styleUrl: './my-divider.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyDivider {
  @Input() color = 'var(--neon-cyan)';
  @Input() orientation: MyDividerOrientation = 'horizontal';
  @Input() thickness = '1px';
  @Input() spacing = '0.75rem';
}
