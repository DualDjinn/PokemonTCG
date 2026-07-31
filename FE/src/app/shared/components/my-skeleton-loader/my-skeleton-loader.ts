import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Animated placeholder shown while real content is loading.
 *
 * Renders one or more shimmering bars at the requested size and color.
 * Use a single line for a button-shaped placeholder, or multiple lines
 * to stand in for a paragraph of text. Color and dimensions are inputs
 * so the same component covers every loading state in the project.
 */
@Component({
  selector: 'my-skeleton-loader',
  standalone: true,
  templateUrl: './my-skeleton-loader.html',
  styleUrl: './my-skeleton-loader.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MySkeletonLoader {
  @Input() lines = 2;
  @Input() width = '100%';
  @Input() height = '0.9rem';
  @Input() color = 'var(--bg-secondary)';
  @Input() highlightColor = 'var(--text-secondary)';
  @Input() borderRadius = '4px';
  @Input() gap = '0.5rem';

  get lineList(): number[] {
    const safeLines = Math.max(1, Math.floor(this.lines));
    return Array.from({ length: safeLines }, (_, index) => index);
  }

  lineWidth(index: number): string {
    const isLast = index === this.lineList.length - 1;
    return isLast && this.lineList.length > 1 ? '70%' : this.width;
  }
}
