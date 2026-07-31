import { Directive, ElementRef, Input, OnInit } from '@angular/core';

@Directive({
  selector: '[appPixelBorder]',
  standalone: true,
  host: {
    '[style.border-style]': '"solid"',
    '[style.border-width]': 'borderWidth',
    '[style.border-color]':
      '"var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left)"',
    '[style.border-radius]': '"0"',
  },
})
export class PixelBorderDirective implements OnInit {
  @Input('appPixelBorder') width: string | '' = '2px';

  borderWidth = '2px';

  ngOnInit(): void {
    this.borderWidth = this.width || '2px';
  }
}
