import { Directive, Input } from '@angular/core';

@Directive({
  selector: '[appFullPage]',
  standalone: true,
  host: {
    '[style.display]': '"block"',
    '[style.width]': '"100%"',
    '[style.min-height]': 'scroll ? "100dvh" : null',
    '[style.height]': 'scroll ? null : "100vh"',
    '[style.overflow]': 'scroll ? null : "hidden"',
    '[style.color]': '"var(--text-primary)"',
  },
})
export class FullPageDirective {
  @Input() scroll = false;
}
