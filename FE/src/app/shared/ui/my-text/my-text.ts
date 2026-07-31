import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Single text primitive used everywhere in the app.
 *
 * Every other UI component that renders visible text routes through this one
 * so that the project font, fallbacks, and color tokens live in a single
 * place. Renders as the HTML tag passed in `as` (h1, h2, h3, p, span, label)
 * so semantics stay correct while the styling stays uniform.
 *
 * Font loading is configured in the component's stylesheet. If the Google
 * font fails to load the browser falls back to `Montserrat` and then to the
 * generic `serif` family without any JS detection.
 */
export type MyTextTag = 'h1' | 'h2' | 'h3' | 'h4' | 'p' | 'span' | 'label';
export type MyTextWeight = 'normal' | 'bold' | 'lighter' | number;

@Component({
  selector: 'my-text',
  standalone: true,
  templateUrl: './my-text.html',
  styleUrl: './my-text.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyText {
  @Input() text = '';
  @Input() tag: MyTextTag = 'span';
  @Input() color = 'var(--text-primary)';
  @Input() size = '.8rem';
  @Input() weight: MyTextWeight = 'normal';
  @Input() align: 'left' | 'center' | 'right' = 'left';
}
