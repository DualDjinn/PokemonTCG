import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Generic container box.
 *
 * Used as the visual frame for menu cards, pokedex entries, modal bodies
 * and any other grouped content. Background and border colors are inputs
 * so the same component covers every panel style in the project.
 */
@Component({
  selector: 'my-card',
  standalone: true,
  templateUrl: './my-card.html',
  styleUrl: './my-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyCard {
  @Input() color = 'var(--bg-secondary)';
  @Input() borderColor = 'var(--neon-cyan)';
  @Input() padding = '1rem';
  @Input() borderRadius = '0px';
  @Input() borderWidth = '2px';
  @Input() shadow = true;
}
